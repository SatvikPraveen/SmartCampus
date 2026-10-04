package scheduling.solver;

import scheduling.eval.CostBreakdown;

/**
 * Outcome of one solver run.
 *
 * @param solver        solver name
 * @param slots         slot per event
 * @param rooms         room per event
 * @param cost          cost as scored by the reference {@link scheduling.eval.CostModel}
 * @param elapsedNanos  wall-clock time spent inside the solver
 * @param iterations    number of search steps performed (construction steps for greedy solvers)
 */
public record SolverResult(
        String solver,
        int[] slots,
        int[] rooms,
        CostBreakdown cost,
        long elapsedNanos,
        long iterations) {

    public double elapsedMillis() {
        return elapsedNanos / 1e6;
    }
}
