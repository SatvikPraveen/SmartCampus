package scheduling.solver;

import java.util.SplittableRandom;

import scheduling.eval.TimetableState;
import scheduling.model.TimetablingProblem;

/**
 * Zero-information baseline: every event receives a uniformly random slot among those available to
 * it and a uniformly random room among the suitable ones (any room if none is). Any useful solver must beat it.
 */
public final class RandomSolver implements TimetableSolver {

    @Override
    public String name() {
        return "random";
    }

    @Override
    public TimetableState construct(TimetablingProblem p, long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        TimetableState state = new TimetableState(p);
        for (int e = 0; e < p.eventCount(); e++) {
            int[] suitable = p.suitableRoomsOf(e);
            int room = suitable.length > 0
                    ? suitable[rng.nextInt(suitable.length)]
                    : rng.nextInt(p.roomCount());
            state.assign(e, GreedySolver.randomSlot(p, e, rng), room);
        }
        return state;
    }
}
