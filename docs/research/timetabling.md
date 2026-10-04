# Course timetabling: methodology and results

This document describes the timetabling problem SmartCampus solves, the algorithms it
implements, how they were evaluated and what the evidence does and does not support. Raw
data for every number below is in [`results/`](results/). Each run can be regenerated with
the command shown in [Reproducing](#reproducing).

## 1. Problem

We study **post-enrolment course timetabling**: student enrolments are known, and every
event (a course meeting) must be assigned one time slot from a `days × periodsPerDay` grid
and one room.

**Hard constraints** (each violation counted):

| Component | Definition |
|---|---|
| unassigned | events without a slot |
| student clashes | Σ over (student, slot) of max(0, events − 1) |
| instructor clashes | Σ over (instructor, slot) of max(0, events − 1) |
| room clashes | Σ over (room, slot) of max(0, events − 1) |
| capacity | events placed in a room smaller than their enrolment |

**Soft constraints**, which are student-centred and follow the structure of the first
International Timetabling Competition's post-enrolment track:

| Component | Definition |
|---|---|
| last period | student-events held in the last period of a day |
| consecutive | for every student-day, Σ over maximal runs of length L > 2 of (L − 2) |
| single-class days | student-days with exactly one class |

A timetable is *feasible* when the hard cost is 0. Solvers minimise
`10⁶ · hard + soft`. That weight is larger than any soft cost reached in these experiments,
so in practice the objective is lexicographic.

The incremental evaluator (`TimetableState`) and the reference implementation (`CostModel`)
are independent implementations of these definitions. Property tests require them to agree
exactly after thousands of random moves ([ADR 0003](../adr/0003-count-based-incremental-evaluation.md)).
Every reported cost is re-scored with `CostModel`.

## 2. Algorithms

| Solver | Description |
|---|---|
| `random` | Uniform random slot and a random room that fits. Zero-information baseline. |
| `greedy-input` | Insert events in input order. Each event goes to the (slot, best-fit free room) pair with the smallest objective increase. |
| `greedy-random` | Same placement rule, seeded random order. |
| `greedy-largest-degree` | Same placement rule, static order by conflict-graph degree (Welsh–Powell). |
| `greedy-dsatur` | Same placement rule, dynamic order by saturation (number of distinct slots used by placed neighbours), ties broken by degree (Brélaz's DSATUR). |
| `sa(greedy-dsatur)` | Simulated annealing from the DSATUR timetable. Relocate and swap moves, Metropolis acceptance, geometric cooling from T₀ to 10⁻³·T₀. T₀ is calibrated so an average worsening soft move is initially accepted with p = 0.5. Returns the best state seen. |
| `descent(greedy-dsatur)` | **Ablation control.** Identical to SA except T₀ is calibrated to p = 10⁻⁹, so worsening moves are essentially never accepted, while sideways (Δ = 0) moves still are. |

SA and descent receive the same seed as their DSATUR start, so their starting point is
exactly the DSATUR timetable reported in the same row. Because both return the best state
seen, they can never be worse than that start. This makes the paired comparisons below exact.

## 3. Experimental design

**Instances.** `InstanceGenerator` draws students' course selections from a Zipf
distribution over events (exponent 0.8), which produces a heavy-tailed size distribution
with a few large courses and many small ones. Instructors are assigned with balanced
load. Room capacities are quantiles of the realised event sizes ×1.1, and the largest event
always fits. The grid is 5 days × 9 periods = 45 slots throughout.

| Family | Events | Students | Courses/student | Instructors | Rooms | Events per room-slot |
|---|---:|---:|---:|---:|---:|---:|
| small | 100 | 400 | 4 | 40 | 5 | 0.44 |
| medium | 200 | 1000 | 5 | 80 | 8 | 0.56 |
| large | 400 | 2000 | 5 | 150 | 12 | 0.74 |

**Protocol.** For each family, seeds 1–10 each generate one instance, and every solver runs
on that same instance with the same seed (a paired design). Local search gets a fixed
**iteration** budget (10⁶ unless stated), so solution quality does not depend on machine
load; wall-clock times are reported but were measured on a shared machine and are only
indicative.

**Statistics.** Means with 95% percentile-bootstrap CIs (10 000 resamples, fixed seed).
Paired comparisons report the mean difference of the weighted objective with a bootstrap CI,
win/loss/tie counts and an exact two-sided sign test. With n = 10 the smallest attainable
sign-test p is 0.002 (10/0). Fifteen paired comparisons are reported, so the Bonferroni
threshold at family-wise α = 0.05 is 0.0033. Results below say explicitly whether they
survive it.

## 4. Results

Source: [`results/main/summary.md`](results/main/summary.md) (10 seeds per family, 10⁶ iterations).

### 4.1 Mean soft cost (all listed solvers feasible on 10/10 seeds unless noted)

| Solver | small | medium | large |
|---|---:|---:|---:|
| random | infeasible (0/10) | infeasible (0/10) | infeasible (0/10) |
| greedy-input | 687.0 [661.7, 712.9] | 2120.7 [2048.7, 2187.7] | feasible 1/10 |
| greedy-random | 644.9 [620.8, 670.9] | 2098.9 [1982.5, 2230.2] | feasible 0/10 |
| greedy-largest-degree | 447.0 [432.4, 462.6] | 1381.7 [1359.3, 1398.6] | 3262.1 [3224.9, 3296.2] |
| greedy-dsatur | 444.8 [433.6, 457.6] | 1388.6 [1365.6, 1413.6] | 3270.8 [3236.1, 3302.7] |
| sa(greedy-dsatur) | 316.3 [310.8, 323.3] | 1238.8 [1211.9, 1267.6] | 3114.9 [3075.0, 3157.3] |
| descent(greedy-dsatur) | **319.0** [313.6, 324.2] | **1179.5** [1169.7, 1190.0] | **3006.0** [2988.2, 3023.5] |

### 4.2 Findings

**F1. Insertion order is the largest single effect among the constructive heuristics.**
Ordering by conflict degree instead of input order lowers the soft cost by 35% (small, Δ = −240,
CI [−267, −212]) and 35% (medium, Δ = −739, CI [−822, −655]), winning on all 10 seeds in both
families (p = 0.002, survives Bonferroni). On large instances, input and random orderings reach
feasibility on only 1/10 and 0/10 seeds, while both degree-based orderings are feasible on 10/10.
This matches the graph-colouring rationale: placing the most constrained events first avoids
dead ends.

**F2. DSATUR is not distinguishable from static largest-degree ordering.** The paired
differences are −2.2 [−13.7, 8.3], +6.9 [−11.9, 26.4] and +8.7 [−30.2, 47.9], with 6/4, 5/5
and 5/5 wins. This is a failure to find a difference, not evidence of equivalence. With 10
seeds, effects of roughly ±15–40 points (about 1–3%) cannot be excluded.

**F3. Local search helps, but only above a budget threshold.** SA improves on its DSATUR start
on every seed in every family (p = 0.002 each). On medium instances the improvement depends
strongly on the budget ([`results/sweep-medium/`](results/sweep-medium/)):

| SA iterations | 10⁴ | 10⁵ | 10⁶ | 4·10⁶ |
|---|---:|---:|---:|---:|
| SA − DSATUR (mean, 95% CI) | −0.7 [−2.1, 0.0] | −9.5 [−21.8, 0.0] | −149.8 [−180.7, −114.5] | −207.9 [−251.4, −153.7] |
| sign-test p | 1.0 | 0.25 | 0.002 | 0.002 |

**F4. The annealing component does not explain the improvement (negative result).** The
hypothesis that SA's benefit comes from accepting worsening moves predicts SA < descent at
equal budget. The data contradict this:

| SA − descent | small | medium | large |
|---|---:|---:|---:|
| mean diff (95% CI) | −2.7 [−8.6, 3.9] | +59.3 [32.2, 87.6] | +108.9 [75.1, 144.3] |
| wins/losses | 7/3 | 1/9 | 0/10 |
| sign-test p | 0.34 | 0.022 | 0.002 |

At 10⁶ iterations, descent with sideways moves is at least as good as SA on small instances
and better on medium and large ones. The large-instance result survives Bonferroni. The
medium result has a CI that excludes zero but p = 0.022 does not survive the correction.
The sweep is consistent with SA losing budget at high temperature: SA needs 4·10⁶ iterations
(1180.7) to match what descent reaches in 10⁶ (1179.5). That comparison crosses two separate
runs and is an inference, not a direct test.

**F5. Local search removes the "last period" penalty but barely touches single-class days.**

| medium, mean penalty | last period | >2 consecutive | single-class days |
|---|---:|---:|---:|
| greedy-dsatur | 253.7 | 63.9 | 1071.0 |
| descent(greedy-dsatur) | 3.4 | 46.6 | 1129.5 |
| sa(greedy-dsatur) | 28.2 | 29.5 | 1181.1 |

The same pattern holds in all three families (see the summary). Single-class days account
for 77–97% of the remaining soft cost and *increase* slightly under local search, which
trades them for last-period and consecutive-class reductions. Relocate/swap moves change one
or two events at a time, but removing a single-class day requires moving an event next to
another event of the *same student* without creating clashes for any other student. These
moves rarely leave the cost unchanged or lower it, so they are rarely accepted.

## 5. Threats to validity

- **Synthetic instances.** The generator controls size and skew, but real enrolment data has
  structure it lacks: curricula, co-requisites, cohort blocks. That structure creates
  cliques in the conflict graph and could change F1–F5. The ITC-2007 post-enrolment
  instances are the obvious external validation.
- **One SA configuration.** F4 concerns *this* calibration (p₀ = 0.5, T_end/T₀ = 10⁻³,
  geometric cooling). A cooler start or reheating could reverse it. F4 rejects
  "annealing as configured helps", not "annealing cannot help".
- **Budget in iterations, not time.** Descent and SA perform the same number of proposals at
  about the same cost per move, so iteration and time budgets coincide here. Solvers with
  different per-move costs would need time-based comparisons.
- **Sample size.** Ten seeds per family detect large paired effects reliably but cannot
  establish equivalence (F2).
- **Penalty weights.** All three soft terms have weight 1, and different weights would
  change which term dominates. F5 depends on that weighting.

## 6. Open hypotheses and next experiments

Ranked by expected information per unit of compute:

1. **Temperature calibration (tests the explanation for F4).** Sweep p₀ ∈ {10⁻⁹, 10⁻⁴, 10⁻², 0.1, 0.5}
   at 10⁶ iterations on medium. If SA is losing budget at high temperature, cost should fall
   monotonically as p₀ decreases toward descent. A U-shape would instead show that some
   annealing helps.
2. **Single-class-day neighbourhood (tests the mechanism behind F5).** Add a Kempe-chain move,
   which swaps two slots for a connected component of the conflict graph and preserves
   feasibility. Prediction: single-class days fall, while plain relocate/swap leaves them unchanged.
3. **External validity.** Load the ITC-2007 post-enrolment instances and check whether F1, F2
   and F4 persist.
4. **Equivalence of DSATUR and largest-degree (F2).** A two one-sided tests (TOST) design
   with ±2% bounds would need roughly 50–100 seeds per family. That is cheap, because
   construction takes milliseconds.

## Reproducing

```bash
./mvnw -q compile
# Main table (about 10 minutes on one core)
./mvnw -q exec:java -Dexec.args="--seeds 10 --iterations 1000000 --out docs/research/results/main"
# Budget sweep
for it in 10000 100000 1000000 4000000; do
  ./mvnw -q exec:java -Dexec.args="--seeds 10 --iterations $it --sizes medium --out docs/research/results/sweep-medium/iter-$it"
done
```

Instances, solvers and bootstrap resampling are all seeded, so solution costs reproduce
exactly. Timings do not. The sweep results were produced before the descent control was
added to the runner. SA and DSATUR behave identically in both versions; only solver labels
differ.
