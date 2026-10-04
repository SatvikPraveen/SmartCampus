# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-10-04

This release turns a code base that had never compiled into a building, tested and
documented project, and adds a course-timetabling research engine.

### Added
- `scheduling` package: post-enrolment course timetabling engine
  - immutable problem model with conflict graph and best-fit room lists
  - reference cost model (5 hard, 3 soft constraint families) and an incremental
    `TimetableState` verified against it by property tests
  - random baseline, greedy construction (input, random, largest-degree, DSATUR orderings)
    and simulated annealing with calibrated temperature and best-so-far tracking
  - seeded Zipf-popularity instance generator, bootstrap CIs, sign test and a paired
    benchmark runner (`ExperimentRunner`)
- REST API (`/api/v1/timetabling/*`) with validation, RFC 7807 errors, OpenAPI docs and
  Actuator health probes
- Benchmark results and methodology in `docs/research/`, architecture decision records in
  `docs/adr/`, `CITATION.cff`, `SECURITY.md`
- Maven wrapper, layered non-root Docker image, CI/CodeQL/release workflow drafts,
  Dependabot, issue and PR templates
- Test suite for the scheduling engine, the API and the domain packages, with a JaCoCo
  coverage gate on the engine

### Fixed
- ~830 compilation errors across the domain library: consumer code assumed an object graph
  (`enrollment.getCourse()`, `student.getDepartment()`) while models reference each other by ID
- `io.DatabaseManager` source corrupted by a mis-spliced method block
- `getCurrentSemester()` in three classes always returned `"Fall"` (`||` instead of `&&`)
- Application class could not start (JPA repositories enabled for a nonexistent package,
  undefined auditor bean)

### Removed
- Dependencies with no code using them: Spring Data JPA, Security, Redis, Mail, Flyway,
  PostgreSQL driver, JJWT, MapStruct, Testcontainers, REST-assured
- Legacy test suite (16.9k lines) written against packages and services that never existed
- Scripts and guides documenting infrastructure that was never implemented (PostgreSQL
  setup, JWT endpoints, Kubernetes deployment)

### Changed
- Target Java 21 (LTS); `reflection.DynamicProxy` uses virtual threads
- Artifact renamed to `smartcampus`; version 2.0.0

## [1.0.0] - 2025-08-31

Initial import of the SmartCampus domain library (models, services, repositories, design
patterns, concurrency, reflection, functional utilities). This version did not compile.
