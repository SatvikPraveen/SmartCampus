package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import scheduling.eval.CostBreakdown;
import scheduling.eval.CostModel;
import scheduling.model.Event;
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
}
