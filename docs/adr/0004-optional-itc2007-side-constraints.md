# ADR 0004: Optional ITC-2007 side constraints as hard cost components

- Status: accepted
- Date: 2026-10-04

## Context

The synthetic benchmark was the main threat to validity. The standard external benchmark for
post-enrolment timetabling is track 2 of the Second International Timetabling Competition
(ITC-2007). It uses the same 45-slot grid and the same three soft constraints as this engine,
but adds three hard constraints the engine did not model:

1. **room features**: an event may only use a room that offers every feature it requires;
2. **slot availability**: each event may only be held in a given subset of the 45 slots;
3. **precedence**: some events must be held in a strictly earlier slot than others.

Three requirements constrained the design. The exact equality between the incremental
`TimetableState` and the reference `CostModel` ([ADR 0003](0003-count-based-incremental-evaluation.md))
had to survive. Every published synthetic result had to reproduce bit for bit. And instances
without side constraints should not become slower.

## Decision

- `Room` gains a feature set and `Event` gains a required-feature set. Both default to empty
  through the original constructors. `Precedence(before, after)` is a new record.
  `TimetablingProblem` gets a second constructor that takes per-event available slots (events
  not listed may use every slot) and a list of precedences. The original constructor delegates
  to it with "no constraints".
- The problem precomputes an event × room feature matrix, sorted available-slot arrays and,
  per event, the indices of the precedences it takes part in. `suitableRoomsOf(e)` now requires
  both capacity and features. Without features it returns exactly the previous list.
- `CostBreakdown` gains three **hard** components, each a plain violation count:
  `featureViolations` (placed events in a room missing a required feature),
  `unavailableSlots` (placed events in a slot not available to them) and
  `precedenceViolations` (precedences whose two events are placed with
  `slot(before) >= slot(after)`; a pair with an unplaced endpoint does not count, as in the
  official checker).
- `TimetableState.assign` updates the first two in its existing `add`/`remove` of the moved
  event. For precedence it subtracts the contribution of the event's own constraints before
  the move and adds it back after. The cost of a move grows by `O(|precedences(e)|)`.
  `CostModel` recomputes all three naively. The feature check reads the `Room`/`Event` records
  directly, not the precomputed matrix. The property test now also runs on instances with
  random features, availability and precedences. It requires each new component to be
  non-zero at some point, so the equality cannot hold vacuously.
- Solvers: greedy construction only tries available slots. Its room choice keeps best fit
  among suitable rooms. When all of those are taken, it falls back to the largest free room
  that has the features, then to any free room, and only then to double booking. Annealing's
  relocate move samples only available slots. Swaps, and precedence for all solvers, are
  handled through the hard penalty. When an event has no available slot at all, every slot is
  tried and the violation is counted. Each change uses the same random draws when the
  constraint is absent: `availableSlotsOf(e)` is then `0..T-1`, so `slots[rng.nextInt(T)]`
  equals `rng.nextInt(T)`.
- The competition does not score hard violations. It ranks by *distance to feasibility*
  (students in unplaced events) and then by soft cost. `ItcScore` unplaces events until no
  hard violation other than "unassigned" remains, re-inserts any event that then fits, and
  reports `(DtF, soft)`. It is a post-processing step for reporting and does not change what
  the solvers optimise.

## Consequences

- Existing callers, the REST API and the synthetic generator compile unchanged. A solver test
  checks that vacuous side constraints leave every solver's output identical. A re-run of the
  committed synthetic benchmark reproduces `docs/research/results/main/runs.csv` exactly,
  except for the timing column.
- `CostBreakdown` has 11 components instead of 8, so code that calls its canonical
  constructor positionally had to be updated (only tests did).
- Hard-violation **counts** are not the competition's counts. The official checker counts
  student clashes per pair of events, while this engine counts `events − 1` per (student,
  slot). The checker also counts a room with several defects more than once. The two
  definitions are zero on exactly the same timetables, so feasibility and every soft
  component agree, but the hard totals must not be compared with checker output.
- Precedence is enforced only through the penalty. A construction heuristic that orders
  events topologically would probably do better on the ITC instances. That is left as a
  solver improvement and is not part of the model.
