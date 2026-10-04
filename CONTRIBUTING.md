# Contributing

Thanks for your interest in SmartCampus. This project holds itself to research-software
standards: every behavioural claim should be backed by a test, and every performance claim
by a reproducible benchmark.

## Development setup

Requirements: JDK 21. Maven is provided by the wrapper.

```bash
./mvnw verify          # compile, run all tests, enforce the coverage gate
./mvnw spring-boot:run # start the REST service on :8080
```

## Workflow

1. Open an issue first for non-trivial changes. Solver or cost-model ideas should use the
   *Research proposal* template: state the hypothesis, the falsification criterion and the
   baselines it has to beat.
2. Create a branch, keep commits small and use
   [Conventional Commits](https://www.conventionalcommits.org/) (`feat(scheduling): ...`,
   `fix(services): ...`, `test: ...`, `docs: ...`).
3. Run `./mvnw verify` before opening a pull request.

## Code standards

- Follow the surrounding style (`.editorconfig`: 4-space indent, 120 columns).
- Public types and non-obvious methods get Javadoc that explains *why*, not just *what*.
- No new dependency without a concrete use in the same pull request.
- Randomised code takes an explicit seed and must be deterministic for that seed.

## Changes to the scheduling engine

- **Cost model.** `CostModel` is the specification; `TimetableState` must stay exactly
  equal to it. Extend `TimetableStateTest` for any new cost component.
- **Solvers.** Add the solver to `ExperimentRunner`, run at least 10 seeds per instance
  family, and include the generated `summary.md` in the pull request. Report negative
  results too: a solver that does not beat DSATUR is still informative.
- **Claims.** Compare against the strongest existing baseline under an equal time or
  iteration budget, and report paired differences with confidence intervals rather than
  single runs.

## Reporting bugs

Use the *Bug report* template and include the exact command or request, seed and commit.
Security issues: see [SECURITY.md](SECURITY.md).
