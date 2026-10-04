# Architecture

SmartCampus consists of three layers with strictly one-way dependencies:

```
com.smartcampus (Spring Boot)          REST API, validation, OpenAPI, Actuator
        │
        ▼
scheduling (plain Java, no dependencies) timetabling engine + experiment harness
        
domain library (plain Java)            models, services, repositories, patterns, ...
```

The scheduling engine does not depend on the domain library or on Spring. That keeps it
testable and benchmarkable in isolation, and the HTTP layer is a thin adapter over it
([ADR 0002](adr/0002-isolated-scheduling-engine.md)).

## Repository layout

```
src/main/java
├── com/smartcampus/          Spring Boot application and REST API (api/)
├── scheduling/               Course timetabling engine
│   ├── model/                Event, Room, TimetablingProblem (conflict graph, room lists)
│   ├── eval/                 CostModel (reference), TimetableState (incremental), CostBreakdown
│   ├── solver/               Random, Greedy (4 orderings), SimulatedAnnealing
│   └── experiment/           InstanceGenerator, Statistics, ExperimentRunner
├── models/ enums/            University domain entities (ID-referenced) and enumerations
├── services/                 In-memory business services (enrollment, grading, reports, ...)
├── repositories/             Generic in-memory repositories keyed by entity ID
├── patterns/                 Builder, factory, adapter, command, observer, singleton examples
├── concurrent/               Batch/async processing, concurrent grade calculation
├── functional/               Predicates, functions and collectors over domain types
├── security/                 Password hashing, tokens, role-based access
├── io/ cache/ reflection/    File/JSON/CSV/JDBC I/O, LRU cache, annotation-driven proxies
└── utils/ exceptions/ ...    Shared helpers
src/test/java                 Tests mirror the main package layout
docs/                         Architecture, ADRs, research write-up, design-pattern notes
```

## Scheduling engine data flow

1. `TimetablingProblem` maps string IDs to dense integer indices and precomputes the
   conflict graph (events sharing a student or an instructor) and, for each event, the rooms
   large enough to hold it, ordered best fit first.
2. A solver mutates a `TimetableState`, which holds occupancy counts per (student, slot),
   (instructor, slot) and (room, slot). Every cost component is a function of these counts,
   so a move only touches the rows of the moved event
   ([ADR 0003](adr/0003-count-based-incremental-evaluation.md)).
3. `TimetableSolver.solve` re-scores the final assignment with the reference `CostModel`,
   so reported numbers never rely on the incremental bookkeeping.
4. `ExperimentRunner` runs every solver on the same seeded instances and writes per-run CSV
   files plus a Markdown summary with bootstrap confidence intervals.

## Domain library

The domain library models a university: users (students, professors, admins),
departments, courses, enrollments and grades. Entities reference one another **by ID**
rather than by object reference ([ADR 0001](adr/0001-id-referenced-domain-model.md)).
Services keep state in memory. `io.DatabaseManager` provides optional JDBC persistence
against an embedded H2 database.
