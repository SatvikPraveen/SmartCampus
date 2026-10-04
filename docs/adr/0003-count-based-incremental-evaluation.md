# ADR 0003: Count-based incremental cost evaluation, checked against a reference model

- Status: accepted
- Date: 2026-10-04

## Context

Local search evaluates millions of moves. Recomputing the full cost costs
`O(students × slots)` per move, which is too slow. Hand-written delta formulas are fast
but a common source of subtle bugs.

## Decision

Every cost component is defined as a function of occupancy counts: (student, slot),
(instructor, slot) and (room, slot), plus room capacity. `TimetableState.assign` removes the
contributions of the rows a move touches, updates the counts and adds the contributions
back, at a cost of `O(|students(e)| × periodsPerDay)`. `CostModel` is an independent,
deliberately naive implementation of the same definitions. Property tests apply thousands
of random moves and require exact equality between the two.

## Consequences

- Delta evaluation is exact by construction, not approximate.
- Moves are applied and undone instead of being scored speculatively, which costs about
  2× a pure delta computation but keeps one code path.
- New cost terms must be expressible from the counts, or must extend both implementations
  and the property test.
