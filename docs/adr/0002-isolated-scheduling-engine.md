# ADR 0002: The scheduling engine is an isolated, dependency-free package

- Status: accepted
- Date: 2026-10-04

## Context

Timetabling research needs fast iteration, deterministic experiments and the ability to
benchmark solvers without starting a web container or a database.

## Decision

`scheduling` depends only on the JDK. It defines its own minimal problem model (`Event`,
`Room`) rather than reusing `models.Course`. The Spring layer (`com.smartcampus.api`)
translates DTOs to this model, and the domain library can do the same.

## Consequences

- Solvers, cost models and the experiment harness run with `java -cp` and nothing else.
- Unit and property tests run in milliseconds.
- Some data is duplicated between `Course` and `Event`; adapters own that translation.
