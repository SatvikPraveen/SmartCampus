package scheduling;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import scheduling.eval.CostModel;
import scheduling.eval.CostBreakdown;
import scheduling.experiment.InstanceGenerator;
import scheduling.model.Event;
import scheduling.model.Room;
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
    void descentAblationIsNamedAndNeverWorsensItsStart() {
        GreedySolver base = new GreedySolver(GreedySolver.Ordering.INPUT);
        SimulatedAnnealingSolver descent = SimulatedAnnealingSolver.descent(base, 20_000);
        assertEquals("descent(greedy-input)", descent.name());
        TimetablingProblem p = InstanceGenerator.generate(EASY, 4);
        assertTrue(descent.solve(p, 4).cost().weighted(TimetableSolver.HARD_WEIGHT)
                <= base.solve(p, 4).cost().weighted(TimetableSolver.HARD_WEIGHT));
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

    /**
     * Side constraints that exclude nothing (every slot listed as available, every room offering a
     * feature no event needs) must leave every solver's output bit-identical. This is what keeps
     * the published synthetic results reproducible after the model extension.
     */
    @ParameterizedTest
    @MethodSource("solvers")
    void vacuousSideConstraintsLeaveResultsUnchanged(TimetableSolver solver) {
        TimetablingProblem p = InstanceGenerator.generate(EASY, 3);
        Map<String, Set<Integer>> all = new HashMap<>();
        Set<Integer> slots = IntStream.range(0, p.slotCount()).boxed().collect(Collectors.toSet());
        p.events().forEach(e -> all.put(e.id(), slots));
        List<Room> rooms = p.rooms().stream().map(r -> new Room(r.id(), r.capacity(), Set.of("board"))).toList();
        TimetablingProblem q = new TimetablingProblem(p.events(), rooms, p.days(), p.periodsPerDay(), all, List.of());
        SolverResult a = solver.solve(p, 9);
        SolverResult b = solver.solve(q, 9);
        assertArrayEquals(a.slots(), b.slots());
        assertArrayEquals(a.rooms(), b.rooms());
        assertEquals(a.cost(), b.cost());
    }

    @ParameterizedTest
    @MethodSource("solvers")
    void solversRespectSlotAvailability(TimetableSolver solver) {
        TimetablingProblem p = Fixtures.withSideConstraints(InstanceGenerator.generate(EASY, 5), 5, 0.0, 0.5, 0);
        SolverResult r = solver.solve(p, 5);
        // Construction only proposes available slots; annealing could only reach an unavailable
        // slot through a swap that pays the hard penalty, which it does not accept here.
        assertEquals(0, r.cost().unavailableSlots(), r.cost()::toString);
        for (int e = 0; e < p.eventCount(); e++) {
            assertTrue(p.isAvailable(e, r.slots()[e]));
        }
    }

    @Test
    void dsaturAndAnnealingFindFeasibleTimetablesUnderAvailabilityAndPrecedence() {
        GreedySolver dsatur = new GreedySolver(GreedySolver.Ordering.DSATUR);
        TimetableSolver sa = new SimulatedAnnealingSolver(dsatur, SimulatedAnnealingSolver.Config.defaults(20_000));
        for (long seed = 1; seed <= 3; seed++) {
            TimetablingProblem p = Fixtures.withSideConstraints(InstanceGenerator.generate(EASY, seed), seed, 0.0, 0.3, 15);
            CostBreakdown start = dsatur.solve(p, seed).cost();
            CostBreakdown end = sa.solve(p, seed).cost();
            assertTrue(end.weighted(TimetableSolver.HARD_WEIGHT) <= start.weighted(TimetableSolver.HARD_WEIGHT));
            assertTrue(end.feasible(), end::toString);
        }
    }

    @Test
    void eventWithoutAvailableSlotsIsStillPlaced() {
        TimetablingProblem base = Fixtures.tiny();
        TimetablingProblem p = new TimetablingProblem(base.events(), base.rooms(), base.days(), base.periodsPerDay(),
                Map.of("e2", Set.of()), List.of());
        for (TimetableSolver solver : solvers().toList()) {
            SolverResult r = solver.solve(p, 1);
            assertEquals(0, r.cost().unassigned(), solver::name);
            assertEquals(1, r.cost().unavailableSlots(), solver::name);
        }
    }

    @Test
    void greedyPrefersAFeaturelessFreeRoomOverDoubleBooking() {
        List<Event> events = new ArrayList<>();
        events.add(new Event("x", null, Set.of("s1"), Set.of("lab")));
        events.add(new Event("y", null, Set.of("s2"), Set.of("lab")));
        TimetablingProblem p = new TimetablingProblem(events,
                List.of(new Room("lab", 5, Set.of("lab")), new Room("plain", 5)), 1, 1);
        CostBreakdown c = new GreedySolver(GreedySolver.Ordering.INPUT).solve(p, 1).cost();
        assertEquals(1, c.featureViolations());
        assertEquals(0, c.roomClashes());
    }
}
