package scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import scheduling.eval.CostBreakdown;
import scheduling.eval.CostModel;
import scheduling.eval.ItcScore;
import scheduling.eval.TimetableState;
import scheduling.experiment.InstanceGenerator;
import scheduling.model.Event;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;
import scheduling.solver.RandomSolver;

class ItcScoreTest {

    private static final int U = CostModel.UNASSIGNED;

    /** Events a {s1, s2} and b {s2} share a student; the grid has {@code periods} slots on one day. */
    private static TimetablingProblem clash(int periods) {
        return new TimetablingProblem(
                List.of(new Event("a", null, Set.of("s1", "s2")), new Event("b", null, Set.of("s2"))),
                List.of(new Room("r0", 5), new Room("r1", 5)), 1, periods);
    }

    @Test
    void feasibleTimetableIsScoredUnchanged() {
        TimetablingProblem p = clash(2);
        TimetableState s = TimetableState.of(p, new int[] {0, 1}, new int[] {0, 0});
        assertTrue(s.cost().feasible());
        assertEquals(new ItcScore(0, s.soft(), 0), ItcScore.of(s));
        assertArrayEquals(s.slots(), ItcScore.repair(s).slots());
    }

    @Test
    void unplacesTheEventWithFewestStudentsPerRemovedViolation() {
        TimetablingProblem p = clash(1);
        TimetableState s = TimetableState.of(p, new int[] {0, 0}, new int[] {0, 1});
        assertEquals(1, s.cost().studentClashes());
        TimetableState repaired = ItcScore.repair(s);
        // Removing b (1 student) clears the clash at half the cost of removing a (2 students),
        // and b cannot be re-inserted in the single slot without clashing again.
        assertArrayEquals(new int[] {0, U}, repaired.slots());
        // a alone in the only (hence last) period of the day: 2 last-period + 2 single-class days.
        assertEquals(new ItcScore(1, 4, 1), ItcScore.of(s));
        assertEquals(1, s.cost().studentClashes(), "the argument is not modified");
    }

    @Test
    void reinsertsUnplacedEventsWhereTheyFit() {
        TimetablingProblem p = clash(2);
        TimetableState s = TimetableState.of(p, new int[] {0, U}, new int[] {0, U});
        ItcScore score = ItcScore.of(s);
        assertEquals(0, score.distanceToFeasibility());
        assertEquals(0, score.unplaced());
    }

    @Test
    void scoreRejectsTimetablesWithHardViolations() {
        TimetablingProblem p = clash(1);
        TimetableState s = TimetableState.of(p, new int[] {0, 0}, new int[] {0, 1});
        assertThrows(IllegalArgumentException.class, () -> ItcScore.score(s));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3})
    void repairRemovesEveryViolationOfARandomTimetable(long seed) {
        TimetablingProblem p = Fixtures.withSideConstraints(
                InstanceGenerator.generate(new InstanceGenerator.Params("t", 40, 120, 4, 10, 4, 3, 5, 1.0, 1.0), seed),
                seed, 0.3, 0.3, 20);
        TimetableState s = new RandomSolver().construct(p, seed);
        TimetableState repaired = ItcScore.repair(s);
        CostBreakdown c = CostModel.evaluate(p, repaired.slots(), repaired.rooms());
        assertEquals(c.unassigned(), c.hard(), c::toString);
        ItcScore score = ItcScore.score(repaired);
        long dtf = 0;
        for (int e = 0; e < p.eventCount(); e++) {
            if (repaired.slotOf(e) == U) {
                dtf += p.studentsOf(e).length;
            }
        }
        assertEquals(dtf, score.distanceToFeasibility());
        assertEquals(c.unassigned(), score.unplaced());
        assertEquals(c.soft(), score.soft());
        assertTrue(score.unplaced() > 0);
    }
}
