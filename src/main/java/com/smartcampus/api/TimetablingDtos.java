package com.smartcampus.api;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request and response payloads of the timetabling API.
 */
public final class TimetablingDtos {

    /** Upper bounds that keep a single request within a few seconds of CPU time. */
    public static final int MAX_EVENTS = 2_000;
    public static final int MAX_ROOMS = 500;
    public static final long MAX_ITERATIONS = 2_000_000;

    private TimetablingDtos() {
    }

    public record RoomDto(@NotBlank String id, @Min(1) int capacity) {
    }

    public record EventDto(@NotBlank String id, String instructorId, @NotNull Set<@NotBlank String> studentIds) {
    }

    /** A complete problem instance. */
    public record ProblemDto(
            @Min(1) @Max(7) int days,
            @Min(1) @Max(24) int periodsPerDay,
            @NotEmpty @Size(max = MAX_ROOMS) List<@Valid RoomDto> rooms,
            @NotEmpty @Size(max = MAX_EVENTS) List<@Valid EventDto> events) {
    }

    /**
     * Solve request.
     *
     * @param problem    the instance
     * @param solver     solver name, see {@code GET /api/v1/timetabling/solvers}; defaults to {@code sa}
     * @param seed       RNG seed; identical requests return identical timetables
     * @param iterations simulated-annealing budget (ignored by constructive solvers)
     */
    public record SolveRequest(
            @NotNull @Valid ProblemDto problem,
            String solver,
            Long seed,
            @Min(0) @Max(MAX_ITERATIONS) Long iterations) {
    }

    public record AssignmentDto(String eventId, int day, int period, String roomId) {
    }

    public record CostDto(long hard, long soft, boolean feasible, long unassigned, long studentClashes,
                          long instructorClashes, long roomClashes, long capacityViolations,
                          long lastPeriod, long consecutive, long singleClassDays) {
    }

    public record SolveResponse(String solver, long seed, CostDto cost, double elapsedMillis,
                                List<AssignmentDto> assignments) {
    }

    public record SolverInfo(String name, String description) {
    }
}
