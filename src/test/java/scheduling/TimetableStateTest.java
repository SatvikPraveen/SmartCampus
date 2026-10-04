package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.SplittableRandom;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
