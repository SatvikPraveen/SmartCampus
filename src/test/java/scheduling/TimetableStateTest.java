package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import scheduling.eval.CostBreakdown;
import scheduling.eval.CostModel;
import scheduling.eval.TimetableState;
import scheduling.experiment.InstanceGenerator;
import scheduling.model.TimetablingProblem;

/**
 * Property test: after any sequence of assign/move/unassign operations, the incrementally
 * maintained cost must equal the reference cost model evaluated from scratch.
 */
class TimetableStateTest {

    private static final InstanceGenerator.Params DENSE =
            new InstanceGenerator.Params("dense", 40, 120, 4, 10, 4, 3, 5, 1.0, 1.0);

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8})
    void incrementalCostEqualsFullRecomputation(long seed) {
        TimetablingProblem p = InstanceGenerator.generate(DENSE, seed);
        TimetableState state = new TimetableState(p);
        SplittableRandom rng = new SplittableRandom(seed);
        for (int step = 0; step < 3_000; step++) {
            int e = rng.nextInt(p.eventCount());
            if (rng.nextInt(10) == 0) {
                state.unassign(e);
            } else {
                state.assign(e, rng.nextInt(p.slotCount()), rng.nextInt(p.roomCount()));
            }
            if (step % 50 == 0) {
                assertEquals(CostModel.evaluate(p, state.slots(), state.rooms()), state.cost(), "step " + step);
            }
        }
        assertEquals(CostModel.evaluate(p, state.slots(), state.rooms()), state.cost());
    }

    /**
     * The same property with room features, slot availability and precedence constraints active.
     * The test also requires every new component to be non-zero at some point, so the equality is
     * not satisfied vacuously.
     */
    @ParameterizedTest
    @ValueSource(longs = {21, 22, 23, 24, 25, 26})
    void incrementalCostEqualsFullRecomputationWithSideConstraints(long seed) {
        TimetablingProblem p = Fixtures.withSideConstraints(InstanceGenerator.generate(DENSE, seed), seed, 0.3, 0.4, 60);
        TimetableState state = new TimetableState(p);
        SplittableRandom rng = new SplittableRandom(seed);
        long maxFeature = 0;
        long maxUnavailable = 0;
        long maxPrecedence = 0;
        for (int step = 0; step < 3_000; step++) {
            int e = rng.nextInt(p.eventCount());
            int choice = rng.nextInt(10);
            if (choice == 0) {
                state.unassign(e);
            } else if (choice == 1) {
                // Room change only, same slot.
                int slot = state.slotOf(e) == CostModel.UNASSIGNED ? rng.nextInt(p.slotCount()) : state.slotOf(e);
                state.assign(e, slot, rng.nextInt(p.roomCount()));
            } else {
                state.assign(e, rng.nextInt(p.slotCount()), rng.nextInt(p.roomCount()));
            }
            CostBreakdown c = state.cost();
            maxFeature = Math.max(maxFeature, c.featureViolations());
            maxUnavailable = Math.max(maxUnavailable, c.unavailableSlots());
            maxPrecedence = Math.max(maxPrecedence, c.precedenceViolations());
            if (step % 25 == 0) {
                assertEquals(CostModel.evaluate(p, state.slots(), state.rooms()), c, "step " + step);
            }
        }
        assertEquals(CostModel.evaluate(p, state.slots(), state.rooms()), state.cost());
        assertTrue(maxFeature > 0 && maxUnavailable > 0 && maxPrecedence > 0,
                () -> "side constraints never violated: " + state.cost());
        TimetableState rebuilt = TimetableState.of(p, state.slots(), state.rooms());
        assertEquals(state.cost(), rebuilt.cost());
    }

    @ParameterizedTest
    @ValueSource(longs = {11, 12, 13})
    void copyIsIndependent(long seed) {
        TimetablingProblem p = InstanceGenerator.generate(DENSE, seed);
        TimetableState a = new TimetableState(p);
        for (int e = 0; e < p.eventCount(); e++) {
            a.assign(e, e % p.slotCount(), e % p.roomCount());
        }
        TimetableState b = a.copy();
        var before = a.cost();
        b.assign(0, (b.slotOf(0) + 1) % p.slotCount(), 0);
        assertEquals(before, a.cost());
        assertEquals(CostModel.evaluate(p, b.slots(), b.rooms()), b.cost());
    }
}
