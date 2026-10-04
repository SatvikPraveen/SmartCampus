package scheduling.solver;

import scheduling.eval.CostModel;
import scheduling.eval.TimetableState;
import scheduling.model.TimetablingProblem;

/**
 * A timetabling algorithm. Implementations must be deterministic for a fixed {@code seed}.
 */
public interface TimetableSolver {

    /** Weight that makes one hard violation dominate any realistic soft penalty. */
    long HARD_WEIGHT = 1_000_000L;

    /** Short, stable name used in experiment output. */
    String name();

    /** Builds the timetable and returns the final state. */
    TimetableState construct(TimetablingProblem problem, long seed);

    /** Runs the solver, timing it and scoring the result with the reference cost model. */
    default SolverResult solve(TimetablingProblem problem, long seed) {
        long start = System.nanoTime();
        TimetableState state = construct(problem, seed);
        long elapsed = System.nanoTime() - start;
        int[] slots = state.slots();
        int[] rooms = state.rooms();
        return new SolverResult(name(), slots, rooms, CostModel.evaluate(problem, slots, rooms),
                elapsed, iterationsOf(problem));
    }

    /** Number of search steps reported in {@link SolverResult#iterations()}. */
    default long iterationsOf(TimetablingProblem problem) {
        return problem.eventCount();
    }
}
