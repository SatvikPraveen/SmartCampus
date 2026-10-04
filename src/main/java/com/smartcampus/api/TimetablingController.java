package com.smartcampus.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcampus.api.TimetablingDtos.ProblemDto;
import com.smartcampus.api.TimetablingDtos.SolveRequest;
import com.smartcampus.api.TimetablingDtos.SolveResponse;
import com.smartcampus.api.TimetablingDtos.SolverInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoints for course timetabling.
 */
@RestController
@Validated
@RequestMapping("/api/v1/timetabling")
@Tag(name = "Timetabling", description = "Course timetabling solvers")
public class TimetablingController {

    private final TimetablingService service;

    public TimetablingController(TimetablingService service) {
        this.service = service;
    }

    @GetMapping("/solvers")
    @Operation(summary = "List available solvers")
    public List<SolverInfo> solvers() {
        return service.solvers();
    }

    @PostMapping("/solve")
    @Operation(summary = "Solve a timetabling instance",
            description = "Deterministic for a fixed seed. Returns every event's (day, period, room) "
                    + "and the itemised constraint cost.")
    public SolveResponse solve(@Valid @RequestBody SolveRequest request) {
        return service.solve(request);
    }

    @GetMapping("/instances/{family}")
    @Operation(summary = "Generate a synthetic benchmark instance (small, medium or large)")
    public ProblemDto instance(@PathVariable String family,
                               @RequestParam(defaultValue = "1") @Min(0) @Max(Integer.MAX_VALUE) long seed) {
        return service.generate(family, seed);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
