<div align="center">

# SmartCampus

**A course-timetabling research engine and university-management domain library, served through a Spring Boot REST API.**

[![CI](https://github.com/SatvikPraveen/SmartCampus/actions/workflows/ci.yml/badge.svg)](https://github.com/SatvikPraveen/SmartCampus/actions/workflows/ci.yml)
[![CodeQL](https://github.com/SatvikPraveen/SmartCampus/actions/workflows/codeql.yml/badge.svg)](https://github.com/SatvikPraveen/SmartCampus/actions/workflows/codeql.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/tests-1%2C980_passing-25A162.svg)](#testing-and-quality)
[![Reproducible](https://img.shields.io/badge/benchmarks-seeded_%26_paired-8A2BE2.svg)](docs/research/timetabling.md)
[![Cite](https://img.shields.io/badge/cite-CITATION.cff-lightgrey.svg)](CITATION.cff)

[Overview](#overview) •
[Key results](#key-results) •
[Quick start](#quick-start) •
[REST API](#rest-api) •
[The engine](#the-timetabling-engine) •
[Reproducing](#reproducing-the-experiments) •
[Architecture](#architecture) •
[Citation](#citation)

</div>

---

## Overview

Every term, a university has to place hundreds of course meetings into a weekly grid of time
slots and rooms. No student or instructor can be in two places at once, and every room must
be large enough. Within those limits the timetable should also be humane: few classes in the
last period of the day, no long runs of back-to-back classes, and no days on campus for a
single lecture. This is the **post-enrolment course timetabling problem**, a constrained
combinatorial optimisation problem closely related to graph colouring.

SmartCampus contains:

| Component | What it is |
|---|---|
| **`scheduling`**: timetabling engine | Dependency-free Java package with an exact constraint model, incremental cost evaluation verified against a reference implementation, constructive and local-search solvers, a seeded instance generator and a paired benchmarking harness with bootstrap confidence intervals. |
| **REST service** | Stateless Spring Boot 3 application exposing the engine (`/api/v1/timetabling`), with request validation, RFC 7807 errors, OpenAPI/Swagger UI and Actuator health probes. Needs no database. |
| **Domain library** | University model (students, professors, departments, courses, enrollments, grades) with in-memory services and repositories, plus worked examples of design patterns, concurrency, reflection and functional programming in modern Java. |
| **Research write-up** | Methodology, results, ablations, threats to validity and follow-up experiments in [`docs/research/timetabling.md`](docs/research/timetabling.md). |

## Key results

Ten paired seeds per instance family, 10⁶ local-search iterations, 5 days × 9 periods.
Values are mean soft-constraint penalty (lower is better) with 95% bootstrap CIs. Full
tables, decomposition and statistics are in [`docs/research/results/main/summary.md`](docs/research/results/main/summary.md).

| Solver | small (100 events) | medium (200 events) | large (400 events) |
|---|---:|---:|---:|
| Random (baseline) | infeasible | infeasible | infeasible |
| Greedy, input order | 687 [662, 713] | 2121 [2049, 2188] | feasible on 1/10 |
| Greedy, largest degree first | 447 [432, 463] | 1382 [1359, 1399] | 3262 [3225, 3296] |
| Greedy, DSATUR | 445 [434, 458] | 1389 [1366, 1414] | 3271 [3236, 3303] |
| Simulated annealing (from DSATUR) | 316 [311, 323] | 1239 [1212, 1268] | 3115 [3075, 3157] |
| Descent ablation (from DSATUR) | **319** [314, 324] | **1180** [1170, 1190] | **3006** [2988, 3024] |

What the evidence supports:

1. **Insertion order dominates construction.** Degree-based ordering lowers the soft cost by
   about 35% compared with input order, and it is the difference between feasible and infeasible
   timetables on large instances (10/10 vs 1/10 seeds).
2. **DSATUR shows no detectable advantage over static largest-degree ordering** on these
   instances. The point estimates differ by less than 1%, and every 95% CI includes 0.
3. **Local search needs a budget threshold.** The SA gain is negligible at 10⁴–10⁵ iterations and
   large at 10⁶ (−150 on medium, 10/10 wins).
4. **Negative result: annealing is not what makes local search work here.** At equal budget, a
   near-zero-temperature descent control matches SA on small instances and beats it on medium
   (+59 [32, 88]) and large (+109 [75, 144]; 0/10 wins for SA).
5. **Single-class days are the bottleneck.** They make up 77–97% of the remaining penalty and are
   not reduced by relocate/swap moves. That points to compound neighbourhoods such as Kempe chains
   as the next step.

These conclusions come from synthetic instances. The scope and the planned validation on
ITC-2007 data are described in [threats to validity](docs/research/timetabling.md#5-threats-to-validity).

## Quick start

Requirements: **JDK 21**. Maven comes with the wrapper.

```bash
git clone https://github.com/SatvikPraveen/SmartCampus.git
cd SmartCampus

./mvnw verify                 # compile, run the full test suite, enforce coverage gate
./mvnw spring-boot:run        # start the API on http://localhost:8080
```

Or with Docker:

```bash
docker compose up --build     # same service, non-root layered JDK 21 image
```

Then open **http://localhost:8080/swagger-ui.html**, or try it from the shell:

```bash
# 1. Generate a benchmark instance (100 events, 400 students, 5 rooms)
curl -s 'localhost:8080/api/v1/timetabling/instances/small?seed=1' > problem.json

# 2. Solve it with simulated annealing
jq '{problem: ., solver: "sa", seed: 1, iterations: 200000}' problem.json \
  | curl -s -X POST localhost:8080/api/v1/timetabling/solve \
         -H 'Content-Type: application/json' -d @- \
  | jq '{solver, cost, first: .assignments[0]}'
```

```json
{
  "solver": "sa",
  "cost": { "hard": 0, "soft": 383, "feasible": true, "lastPeriod": 3,
            "consecutive": 1, "singleClassDays": 379, "...": "..." },
  "first": { "eventId": "E0", "day": 0, "period": 8, "roomId": "R1" }
}
```

## REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/timetabling/solvers` | Available solvers and their descriptions |
| `POST` | `/api/v1/timetabling/solve` | Solve an instance. Deterministic for a given `seed`. |
| `GET` | `/api/v1/timetabling/instances/{small\|medium\|large}?seed=` | Generate a synthetic benchmark instance in request format |
| `GET` | `/actuator/health`, `/actuator/health/{liveness,readiness}` | Health probes |
| `GET` | `/swagger-ui.html`, `/v3/api-docs` | Interactive documentation and OpenAPI spec |

<details>
<summary><b>Solve request schema</b></summary>

```jsonc
{
  "problem": {
    "days": 5,                 // 1–7
    "periodsPerDay": 9,        // 1–24
    "rooms":  [{ "id": "R1", "capacity": 40 }],
    "events": [{ "id": "CS101", "instructorId": "P7", "studentIds": ["S1", "S2"] }]
  },
  "solver": "sa",              // random | greedy | largest-degree | dsatur | sa   (default: sa)
  "seed": 1,                   // default: 1
  "iterations": 200000         // SA budget, ≤ 2,000,000 (default: 200,000)
}
```

Invalid input returns `400` with an RFC 7807 `application/problem+json` body. Request size is
bounded (≤ 2,000 events, ≤ 500 rooms) to keep each request within seconds of CPU time.

</details>

## The timetabling engine

**Formulation.** Each event *e* gets a slot *t(e)* and a room *r(e)*. The conflict graph
links events that share a student or an instructor. Solvers minimise
`10⁶ · hard + soft`, which is lexicographic in practice:

| Hard constraints (must be 0) | Soft constraints (minimised) |
|---|---|
| Unassigned events | Student-events in the last period of a day |
| Student clashes, Σ max(0, events − 1) per (student, slot) | Runs of more than 2 consecutive classes, Σ (L − 2) |
| Instructor clashes | Student-days with exactly one class |
| Room double-bookings | |
| Room capacity violations | |

**Exact incremental evaluation.** `TimetableState` keeps occupancy counts per (student, slot),
(instructor, slot) and (room, slot). Every cost term is a function of these counts, so a move
costs **O(|students(e)| · periodsPerDay)** rather than O(students · slots). A property test
applies thousands of random moves and asserts **exact equality** with the independent,
deliberately naive `CostModel` ([ADR 0003](docs/adr/0003-count-based-incremental-evaluation.md)).

**Solvers.**

| Solver | Idea |
|---|---|
| `RandomSolver` | Zero-information baseline |
| `GreedySolver` (`INPUT`, `RANDOM`, `LARGEST_DEGREE`, `DSATUR`) | Insert events one at a time at the cheapest (slot, best-fit room). The orderings follow the graph-colouring literature (Welsh–Powell, Brélaz). |
| `SimulatedAnnealingSolver` | Relocate and swap moves, Metropolis acceptance, geometric cooling, T₀ calibrated from sampled soft deltas, best-so-far tracking. Never worse than its seeded start. |
| `SimulatedAnnealingSolver.descent` | Ablation control: same moves and budget at near-zero temperature |

**Using it as a library:**

```java
TimetablingProblem problem = new TimetablingProblem(events, rooms, /* days */ 5, /* periods */ 9);
TimetableSolver solver = new SimulatedAnnealingSolver(
        new GreedySolver(GreedySolver.Ordering.DSATUR),
        SimulatedAnnealingSolver.Config.defaults(1_000_000));
SolverResult result = solver.solve(problem, /* seed */ 42);
System.out.println(result.cost());   // CostBreakdown[unassigned=0, studentClashes=0, ...]
```

## Reproducing the experiments

Instances, solvers and bootstrap resampling are all seeded, so every cost in this README is
reproduced exactly. Budgets are counted in iterations, not seconds.

```bash
./mvnw -q compile
./mvnw -q exec:java -Dexec.args="--seeds 10 --iterations 1000000 --out results"
# options: --sizes small,medium,large   --seeds N   --iterations N   --out DIR
```

This produces `results/runs.csv` (one row per solver run, with every cost component and the
timing) and `results/summary.md` (CIs, decomposition and paired tests). The committed
results and the budget sweep are in [`docs/research/results/`](docs/research/results/).

## Architecture

```
com.smartcampus.api  ──►  scheduling (JDK only)        domain library (JDK only)
  REST, validation         model · eval · solver ·        models · services · repositories ·
  OpenAPI, Actuator        experiment                     patterns · concurrent · functional ·
                                                          security · io · cache · reflection
```

The engine does not depend on Spring or on the domain library, so it can be tested and
benchmarked in isolation. Domain entities reference one another by ID
([ADR 0001](docs/adr/0001-id-referenced-domain-model.md)). See
[`docs/architecture.md`](docs/architecture.md) for the package-by-package layout and
[`docs/design/design-patterns.md`](docs/design/design-patterns.md) for the pattern catalogue.

<details>
<summary><b>Repository layout</b></summary>

```
SmartCampus/
├── src/main/java/
│   ├── com/smartcampus/        Spring Boot application, REST API
│   ├── scheduling/             Timetabling engine (model, eval, solver, experiment)
│   ├── models/ enums/          Domain entities and enumerations
│   ├── services/ repositories/ In-memory business logic and storage
│   ├── patterns/ concurrent/ functional/ reflection/   Language and design showcases
│   └── security/ io/ cache/ utils/ exceptions/ ...
├── src/test/java/              Tests mirroring the main packages
├── docs/
│   ├── research/               Methodology, results, raw CSVs
│   ├── adr/                    Architecture decision records
│   └── architecture.md
├── Dockerfile, docker-compose.yml
└── CITATION.cff, CHANGELOG.md, CONTRIBUTING.md, SECURITY.md
```

</details>

## Testing and quality

| Layer | Approach |
|---|---|
| Cost model | Hand-computed fixtures for every constraint type, plus a property test (incremental ≡ reference) |
| Solvers | Determinism per seed, feasibility on easy instances, beats the random baseline, local search never worse than its start |
| Statistics | Analytic checks: bootstrap reproducibility, sign-test binomial tails |
| REST API | `@SpringBootTest` + MockMvc covering the HTTP contract, validation errors and health |
| Domain library | Unit tests for services, models, utilities, security and repositories, written as regression tests for the bugs they exposed |

Current suite: **1,980 tests, 0 failures**. Line coverage by package (JaCoCo):

| ≥ 90% | 70–90% | < 50% (not yet tested) |
|---|---|---|
| scheduling.model 96%, scheduling.solver 97%, scheduling.eval 93%, api 96%, utils 96%, repositories 95%, cache 94% | functional 87%, enums 86%, security 81%, models 73% | services 40%, io, patterns, concurrent, reflection, events, app: 0% |

Overall line coverage is 41%. The untested packages are the clearest next step for
contributors. Writing the existing tests uncovered and fixed more than 70 defects, including
an always-failing token manager, a forged-token revocation path, a deadlock in the LRU cache
views and grade conversions that returned the wrong scale. Each fix is documented in its
commit message and pinned by a regression test.

`./mvnw verify` runs everything and enforces an **85% line-coverage floor** on the engine's
model, evaluation and solver packages through JaCoCo. The report is written to
`target/site/jacoco/index.html`.

Every push and pull request runs [CI](.github/workflows/ci.yml): the full `verify` build with the
coverage gate, a benchmark smoke run, and a Docker image build with a container health check.
[CodeQL](.github/workflows/codeql.yml) scans the code weekly and on every PR, and tagging `v*`
publishes the jar through the [release workflow](.github/workflows/release.yml).

## Roadmap

- [ ] Temperature-calibration sweep to test *why* descent beats SA ([research §6](docs/research/timetabling.md#6-open-hypotheses-and-next-experiments))
- [ ] Kempe-chain neighbourhood aimed at single-class days
- [ ] ITC-2007 post-enrolment instance loader for external validation
- [ ] Adapter from `models.Course` enrollments to `scheduling.TimetablingProblem`
- [ ] Room features and per-event time-window constraints

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow and the
reproducibility requirements for solver changes. Report security issues as described in
[SECURITY.md](SECURITY.md).

## Citation

If you use SmartCampus or its benchmark results, please cite it ([`CITATION.cff`](CITATION.cff)):

```bibtex
@software{praveen_smartcampus_2026,
  author  = {Praveen, Satvik},
  title   = {SmartCampus: a course-timetabling engine with reproducible benchmarks},
  year    = {2026},
  version = {2.0.0},
  url     = {https://github.com/SatvikPraveen/SmartCampus}
}
```

## License

Released under the [MIT License](LICENSE) © 2025 Satvik Praveen.
