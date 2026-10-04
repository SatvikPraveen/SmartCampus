package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import scheduling.eval.CostBreakdown;
import scheduling.eval.CostModel;
import scheduling.model.Event;
import scheduling.model.Precedence;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;

class CostModelTest {

    private static final int U = CostModel.UNASSIGNED;

    @Test
    void emptyTimetableCountsOnlyUnassigned() {
        CostBreakdown c = CostModel.evaluate(Fixtures.tiny(), new int[] {U, U, U, U}, new int[] {U, U, U, U});
        assertEquals(new CostBreakdown(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), c);
    }

    @Test
    void detectsEachHardViolationType() {
        TimetablingProblem p = Fixtures.tiny();
        // e0,e1,e2 all in slot 0: student b clashes once, instructor X clashes once.
        // e0,e1 share room 0 and e2 room 0 too -> room 0 holds 3 events -> 2 room clashes.
        // e3 (3 students) in the small room (cap 2) -> 1 capacity violation.
        CostBreakdown c = CostModel.evaluate(p, new int[] {0, 0, 0, 1}, new int[] {0, 0, 0, 0});
        assertEquals(0, c.unassigned());
        assertEquals(1, c.studentClashes());
        assertEquals(1, c.instructorClashes());
        assertEquals(2, c.roomClashes());
        assertEquals(1, c.capacityViolations());
        assertFalse(c.feasible());
    }

    @Test
    void feasibleTimetableHasZeroHardCost() {
        CostBreakdown c = CostModel.evaluate(Fixtures.tiny(), new int[] {0, 1, 1, 0}, new int[] {0, 0, 1, 1});
        assertTrue(c.feasible(), c::toString);
    }

    @Test
    void softConstraintsMatchHandComputation() {
        // One student, one day of five periods.
        List<Event> events = List.of(
                new Event("p0", null, Set.of("s")),
                new Event("p1", null, Set.of("s")),
                new Event("p2", null, Set.of("s")),
                new Event("p3", null, Set.of("s")),
                new Event("p4", null, Set.of("s")));
        TimetablingProblem p = new TimetablingProblem(events, List.of(new Room("r", 1)), 2, 5);
        int[] rooms = {0, 0, 0, 0, 0};
        // All five on day 0: run of 5 -> 3 consecutive, last period used -> 1, day 1 empty.
        CostBreakdown all = CostModel.evaluate(p, new int[] {0, 1, 2, 3, 4}, rooms);
        assertEquals(3, all.consecutive());
        assertEquals(1, all.lastPeriod());
        assertEquals(0, all.singleClassDays());
        // Day 0: periods 0,1,3,4 (two runs of 2, no penalty, last period used); day 1: period 0 only.
        CostBreakdown split = CostModel.evaluate(p, new int[] {0, 1, 3, 4, 5}, rooms);
        assertEquals(0, split.consecutive());
        assertEquals(1, split.lastPeriod());
        assertEquals(1, split.singleClassDays());
    }

    @Test
    void weightedObjectiveIsLexicographicForLargeWeight() {
        CostBreakdown a = new CostBreakdown(0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        CostBreakdown b = new CostBreakdown(0, 0, 0, 0, 0, 0, 0, 0, 900, 0, 0);
        assertTrue(a.weighted(1_000_000) > b.weighted(1_000_000));
    }

    /**
     * Three single-student events on one day of three periods. {@code a} needs a lab and may only
     * use slots 0-1, {@code c} may only use slot 2, and a &lt; b &lt; c must hold in slot order.
     */
    private static TimetablingProblem sideConstrained() {
        return new TimetablingProblem(
                List.of(new Event("a", null, Set.of("s1"), Set.of("lab")),
                        new Event("b", null, Set.of("s2")),
                        new Event("c", null, Set.of("s3"))),
                List.of(new Room("plain", 5), new Room("lab", 5, Set.of("lab"))),
                1, 3,
                Map.of("a", Set.of(0, 1), "c", Set.of(2)),
                List.of(new Precedence("a", "b"), new Precedence("b", "c")));
    }

    @Test
    void detectsFeatureAvailabilityAndPrecedenceViolations() {
        TimetablingProblem p = sideConstrained();
        // a: slot 2, plain room -> missing feature and unavailable slot.
        // b: slot 1, lab room   -> fine (b needs no feature).
        // c: slot 0             -> unavailable slot. Both precedences are reversed.
        CostBreakdown c = CostModel.evaluate(p, new int[] {2, 1, 0}, new int[] {0, 1, 0});
        assertEquals(1, c.featureViolations());
        assertEquals(2, c.unavailableSlots());
        assertEquals(2, c.precedenceViolations());
        assertEquals(5, c.hard());
        assertEquals(new CostBreakdown(0, 0, 0, 0, 0, 1, 2, 2, 1, 0, 3), c);
    }

    @Test
    void sideConstraintsAreSatisfiableAndPrecedenceIsStrict() {
        TimetablingProblem p = sideConstrained();
        assertTrue(CostModel.evaluate(p, new int[] {0, 1, 2}, new int[] {1, 0, 0}).feasible());
        // Same slot does not satisfy "before".
        assertEquals(1, CostModel.evaluate(p, new int[] {1, 1, 2}, new int[] {1, 0, 0}).precedenceViolations());
        // A precedence with an unassigned endpoint is not counted.
        CostBreakdown partial = CostModel.evaluate(p, new int[] {1, U, 0}, new int[] {1, U, 0});
        assertEquals(0, partial.precedenceViolations());
        assertEquals(1, partial.unassigned());
    }
}
