package scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import scheduling.eval.CostModel;
import scheduling.experiment.InstanceGenerator;
import scheduling.model.TimetablingProblem;
import scheduling.solver.GreedySolver;
import scheduling.solver.RandomSolver;
import scheduling.solver.SimulatedAnnealingSolver;
import scheduling.solver.SolverResult;
import scheduling.solver.TimetableSolver;

class SolverTest {

    private static final InstanceGenerator.Params EASY =
            new InstanceGenerator.Params("easy", 60, 200, 3, 30, 6, 5, 6, 0.8, 1.2);

    static Stream<TimetableSolver> solvers() {
        GreedySolver dsatur = new GreedySolver(GreedySolver.Ordering.DSATUR);
        return Stream.of(
                new RandomSolver(),
                new GreedySolver(GreedySolver.Ordering.INPUT),
                new GreedySolver(GreedySolver.Ordering.RANDOM),
                new GreedySolver(GreedySolver.Ordering.LARGEST_DEGREE),
                dsatur,
                new SimulatedAnnealingSolver(dsatur, SimulatedAnnealingSolver.Config.defaults(5_000)));
    }

    @ParameterizedTest
    @MethodSource("solvers")
    void everySolverAssignsEveryEventAndIsDeterministic(TimetableSolver solver) {
        TimetablingProblem p = InstanceGenerator.generate(EASY, 7);
        SolverResult a = solver.solve(p, 42);
        SolverResult b = solver.solve(p, 42);
        assertEquals(0, a.cost().unassigned());
        assertArrayEquals(a.slots(), b.slots());
        assertArrayEquals(a.rooms(), b.rooms());
        assertEquals(CostModel.evaluate(p, a.slots(), a.rooms()), a.cost());
    }

    @Test
    void greedySolversFindFeasibleTimetablesOnTheTinyInstance() {
        TimetablingProblem p = Fixtures.tiny();
        for (GreedySolver.Ordering o : GreedySolver.Ordering.values()) {
            assertTrue(new GreedySolver(o).solve(p, 1).cost().feasible(), o::name);
        }
    }

    @Test
    void dsaturIsFeasibleOnEasyInstancesAndBeatsRandomBaseline() {
        TimetableSolver dsatur = new GreedySolver(GreedySolver.Ordering.DSATUR);
        TimetableSolver random = new RandomSolver();
        for (long seed = 1; seed <= 5; seed++) {
            TimetablingProblem p = InstanceGenerator.generate(EASY, seed);
            SolverResult d = dsatur.solve(p, seed);
            SolverResult r = random.solve(p, seed);
            assertTrue(d.cost().feasible(), () -> "seed " + d.cost());
            assertTrue(d.cost().weighted(TimetableSolver.HARD_WEIGHT) < r.cost().weighted(TimetableSolver.HARD_WEIGHT));
        }
    }

    @Test
    void annealingNeverWorsensItsInitialSolution() {
        GreedySolver base = new GreedySolver(GreedySolver.Ordering.INPUT);
        TimetableSolver sa = new SimulatedAnnealingSolver(base, SimulatedAnnealingSolver.Config.defaults(20_000));
        for (long seed : List.of(1L, 2L, 3L)) {
            TimetablingProblem p = InstanceGenerator.generate(EASY, seed);
            long start = base.solve(p, seed).cost().weighted(TimetableSolver.HARD_WEIGHT);
            long end = sa.solve(p, seed).cost().weighted(TimetableSolver.HARD_WEIGHT);
            assertTrue(end <= start, () -> "SA " + end + " > initial " + start);
        }
    }

    @Test
    void annealingImprovesWeakStartOnAverage() {
        GreedySolver base = new GreedySolver(GreedySolver.Ordering.INPUT);
        TimetableSolver sa = new SimulatedAnnealingSolver(base, SimulatedAnnealingSolver.Config.defaults(50_000));
        long improved = Stream.of(1L, 2L, 3L, 4L, 5L).filter(seed -> {
            TimetablingProblem p = InstanceGenerator.generate(EASY, seed);
            return sa.solve(p, seed).cost().soft() < base.solve(p, seed).cost().soft();
        }).count();
        assertTrue(improved >= 4, "SA improved only " + improved + "/5 instances");
    }
}
