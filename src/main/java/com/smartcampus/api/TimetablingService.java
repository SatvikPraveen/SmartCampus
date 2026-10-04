package com.smartcampus.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;

import org.springframework.stereotype.Service;

import com.smartcampus.api.TimetablingDtos.AssignmentDto;
import com.smartcampus.api.TimetablingDtos.CostDto;
import com.smartcampus.api.TimetablingDtos.EventDto;
import com.smartcampus.api.TimetablingDtos.ProblemDto;
import com.smartcampus.api.TimetablingDtos.RoomDto;
import com.smartcampus.api.TimetablingDtos.SolveRequest;
import com.smartcampus.api.TimetablingDtos.SolveResponse;
import com.smartcampus.api.TimetablingDtos.SolverInfo;

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

/**
 * Translates API payloads to the {@link scheduling} engine and back.
 */
@Service
public class TimetablingService {

    static final long DEFAULT_SEED = 1L;
    static final long DEFAULT_ITERATIONS = 200_000L;
    static final String DEFAULT_SOLVER = "sa";

    private record Entry(String description, LongFunction<TimetableSolver> factory) {
    }

    private final Map<String, Entry> solvers = new LinkedHashMap<>();

    public TimetablingService() {
        solvers.put("random", new Entry("Uniform random slot and room (baseline)", it -> new RandomSolver()));
        solvers.put("greedy", new Entry("Greedy insertion in input order",
                it -> new GreedySolver(GreedySolver.Ordering.INPUT)));
        solvers.put("largest-degree", new Entry("Greedy insertion, largest conflict degree first",
                it -> new GreedySolver(GreedySolver.Ordering.LARGEST_DEGREE)));
        solvers.put("dsatur", new Entry("Greedy insertion with dynamic saturation ordering (DSATUR)",
                it -> new GreedySolver(GreedySolver.Ordering.DSATUR)));
        solvers.put("sa", new Entry("Simulated annealing started from DSATUR",
                it -> new SimulatedAnnealingSolver(new GreedySolver(GreedySolver.Ordering.DSATUR),
                        SimulatedAnnealingSolver.Config.defaults(it))));
    }

    public List<SolverInfo> solvers() {
        List<SolverInfo> result = new ArrayList<>();
        solvers.forEach((name, e) -> result.add(new SolverInfo(name, e.description())));
        return result;
    }

    public SolveResponse solve(SolveRequest request) {
        String name = request.solver() == null ? DEFAULT_SOLVER : request.solver();
        Entry entry = solvers.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown solver '" + name + "'; available: " + solvers.keySet());
        }
        long seed = request.seed() == null ? DEFAULT_SEED : request.seed();
        long iterations = request.iterations() == null ? DEFAULT_ITERATIONS : request.iterations();
        TimetablingProblem problem = toProblem(request.problem());
        SolverResult result = entry.factory().apply(iterations).solve(problem, seed);
        return new SolveResponse(name, seed, toDto(result.cost()), result.elapsedMillis(),
                assignments(problem, result));
    }

    /** Generates a benchmark instance in API format, convenient for trying the solve endpoint. */
    public ProblemDto generate(String family, long seed) {
        InstanceGenerator.Params params = switch (family) {
            case "small" -> InstanceGenerator.SMALL;
            case "medium" -> InstanceGenerator.MEDIUM;
            case "large" -> InstanceGenerator.LARGE;
            default -> throw new IllegalArgumentException("Unknown instance family '" + family
                    + "'; available: [small, medium, large]");
        };
        TimetablingProblem p = InstanceGenerator.generate(params, seed);
        return new ProblemDto(p.days(), p.periodsPerDay(),
                p.rooms().stream().map(r -> new RoomDto(r.id(), r.capacity())).toList(),
                p.events().stream().map(e -> new EventDto(e.id(), e.instructorId(), e.studentIds())).toList());
    }

    static TimetablingProblem toProblem(ProblemDto dto) {
        return new TimetablingProblem(
                dto.events().stream().map(e -> new Event(e.id(), e.instructorId(), e.studentIds())).toList(),
                dto.rooms().stream().map(r -> new Room(r.id(), r.capacity())).toList(),
                dto.days(), dto.periodsPerDay());
    }

    private static List<AssignmentDto> assignments(TimetablingProblem p, SolverResult r) {
        List<AssignmentDto> out = new ArrayList<>(p.eventCount());
        for (int e = 0; e < p.eventCount(); e++) {
            int slot = r.slots()[e];
            out.add(slot < 0
                    ? new AssignmentDto(p.events().get(e).id(), -1, -1, null)
                    : new AssignmentDto(p.events().get(e).id(), p.dayOf(slot), p.periodOf(slot),
                            p.rooms().get(r.rooms()[e]).id()));
        }
        return out;
    }

    private static CostDto toDto(CostBreakdown c) {
        return new CostDto(c.hard(), c.soft(), c.feasible(), c.unassigned(), c.studentClashes(),
                c.instructorClashes(), c.roomClashes(), c.capacityViolations(), c.lastPeriod(),
                c.consecutive(), c.singleClassDays());
    }
}
