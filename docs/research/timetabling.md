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
| features *(optional)* | events placed in a room that lacks a feature they require |
| availability *(optional)* | events placed in a slot that is not available to them |
| precedence *(optional)* | ordered pairs (a before b) with both events placed and slot(a) ≥ slot(b) |

The three optional constraints come from the ITC-2007 post-enrolment track (§4.3). The
synthetic instances do not use them. Without them, the model, the solvers and every result
in §4.1–4.2 are bit-identical to the version that lacked them: the committed `main` benchmark
was re-run after the extension and reproduced `runs.csv` exactly, apart from timings
([ADR 0004](../adr/0004-optional-itc2007-side-constraints.md)).

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

### 4.3 External validation: ITC-2007 post-enrolment instances

Source: [`results/itc2007/summary.md`](results/itc2007/summary.md) and
[`runs.csv`](results/itc2007/runs.csv). 24 instances, 10 seeds each, 10⁶ local-search
iterations, all seven solvers (1,680 runs).

**Data.** The 24 public instances of track 2 of the Second International Timetabling
Competition (`comp-2007-2-1.tim` … `-24.tim`) are still served by the organisers at
`https://www.eeecs.qub.ac.uk/itc2007/postenrolcourse/initialdatasets/`. The Internet Archive
holds byte-identical 2010 copies. The competition published no licence for the files, so they
are not committed. [`scripts/fetch-itc2007.sh`](../../scripts/fetch-itc2007.sh) downloads them
into the gitignored `data/itc2007/` and checks recorded SHA-256 sums. The instances have
100–600 events, 10–20 rooms, 10–30 room features and 300–1,000 students on the same
5 × 9 grid. Every event has at least one suitable room, and on average 17–35 of the 45 slots
are available to it. There are 11–197 precedence pairs per instance and no instructors.

**Definitions: what matches and what does not.** These were checked against the
competition's problem description and its official validator, `checksln3b.cpp`.

- *Soft constraints match exactly.* The competition counts, per student, classes in the last
  slot of a day, every class that is the third or later of an unbroken run within a day
  (3 in a row = 1, 4 = 2, …; runs do not wrap across days), and days with exactly one class.
  These are the engine's three soft terms. The validator computes them from a per-student
  "is busy in this slot" bit, while the engine uses occupancy counts. The two differ only when
  a student has a clash, so they agree on every timetable the competition accepts.
- *Hard constraints match, their counts do not.* Rooms (size and features), one event per
  room and slot, availability and precedence are the same constraints. Precedence is strict
  (the same slot violates it). Following the validator, matrix entry (a, b) = 1 means a
  before b, and a pair with an unplaced event is not checked. The validator counts student
  clashes per pair of events and can count a room with several defects more than once, so
  hard-violation *totals* are not comparable. Zero is zero in both.
- *Distance to feasibility is a different convention.* The competition rejects any
  timetable with a hard violation. Events may be left unplaced instead, and solutions are
  ranked by **distance to feasibility (DtF)**, the total number of students in unplaced events,
  and then by soft cost. The engine places every event and counts violations. `ItcScore`
  converts between the two. It repeatedly unplaces the event that removes the most violations
  per attending student, then re-inserts any unplaced event that fits without a violation,
  and scores the result. This is a reporting step only. The solvers still minimise
  `10⁶·hard + soft`, not DtF, so their DtF is not what they optimise.
- *Validation.* Every one of the 1,680 timetables was written in the competition's `.sln`
  format and checked with the official validator (compiled from the published source). The
  validator reported zero hard violations for all of them, and its DtF and soft cost matched
  `ItcScore` exactly on all 1,680.

**Feasibility.** The engine rarely produces a timetable that places every event.

| solver | feasible runs | instances with ≥ 1 feasible run | mean DtF |
|---|---:|---:|---:|
| random | 0/240 | 0/24 | 2077 |
| greedy-input | 0/240 | 0/24 | 1772 |
| greedy-random | 0/240 | 0/24 | 1802 |
| greedy-largest-degree | 0/240 | 0/24 | 1410 |
| greedy-dsatur | 4/240 | 1/24 | 1249 |
| descent(greedy-dsatur) | 10/240 | 1/24 | 966 |
| sa(greedy-dsatur) | **21/240** | **3/24** (3, 16, 17) | **826** |

Soft cost can therefore be compared on only three instances, and between all local-search
solvers only on instance 17 (all 10 seeds feasible for both): SA 379 vs descent 1060 vs DSATUR
1603 (4 of 10 seeds feasible). The other per-instance tables in the summary
(mean DtF, mean hard violations, best and median (DtF, soft)) carry the comparison.

**Comparison with the competition (reported because the source is the organisers' own).**
The organisers published every finalist's ten runs per instance as
[`track2-Results.csv`](https://www.eeecs.qub.ac.uk/itc2007/winner/post/track2-Results.csv)
(also archived by the Internet Archive in May 2008). Selected columns, as (DtF / soft):

| # | finalists' feasible runs | best of all finalists | winner's median (Cambazard et al.) | ours: best of 70 runs | SA median |
|---:|---:|---:|---:|---:|---:|
| 1 | 31/50 | 0 / 15 | 0 / 821 | 1016 / 2212 | 1262 / 2306 |
| 3 | 50/50 | 0 / 164 | 0 / 235 | 0 / 1251 | 0 / 1426 |
| 8 | 50/50 | 0 / 0 | 0 / 0 | 68 / 1158 | 218 / 1152 |
| 16 | 50/50 | 0 / 1 | 0 / 11 | 0 / 625 | 0 / 898 |
| 17 | 50/50 | 0 / 0 | 0 / 1 | 0 / 195 | 0 / 362 |
| 20 | 34/50 | 0 / 445 | 0 / 500 | 103 / 1845 | 128 / 2004 |
| 23 | 39/50 | 0 / 238 | 0 / 319 | 2875 / 3601 | 3492 / 3467 |
| all 24 | 1028/1200 | DtF 0 on 24/24 | — | DtF 0 on 3/24 | — |

"Median" is the 5th-best of 10 runs. The finalists were run by the organisers under a
wall-clock limit set by the competition's machine-calibrated benchmark program, not an
iteration budget, on 2007 hardware. Our runs use 10⁶ iterations, a median of about 10 s per
local-search run (6 runs in parallel in Docker on a 10-core laptop). The budgets are therefore
not equal, and nothing here ranks the engine against the finalists in a controlled way. The
qualitative gap is still unambiguous. The finalists place every event on all 24 instances
(1,028 of their 1,200 runs), with soft costs often below 50. This engine places every event on
3 instances. Where both are feasible, its soft cost is several times higher (instance 3: best
1251 vs 164) or more (instances 16 and 17: 625 vs 1 and 195 vs 0). The engine's solvers are
general-purpose baselines without a feasibility phase, while the finalists' algorithms were
built for this competition. A large gap is therefore expected, and it does not indicate a
defect in the evaluation.

**What carries over from the synthetic findings, and what does not.**

| synthetic finding | ITC-2007 | verdict |
|---|---|---|
| F1: degree-based order beats input order and decides feasibility | largest-degree beats input order on 24/24 instances in mean DtF and hard violations (run level: 240/0 on the engine objective, mean DtF −362 [−397, −328]) | **replicates** |
| F2: DSATUR ties largest-degree | DSATUR is **better**: lower mean DtF on 22/24 instances (p = 3.6·10⁻⁵, instance-level sign test), fewer hard violations on 24/24, 215/25 run-level wins, mean DtF −161 [−190, −132] | **does not replicate** |
| F3: local search improves on its start | descent and SA beat DSATUR on 24/24 instances and on all 240 paired runs on the engine objective | **replicates** |
| F4: descent ≥ SA at equal budget | SA is **better**: lower mean DtF on 22/24 instances (1 tie, p = 5.7·10⁻⁶), fewer hard violations on 23/24 (p = 2.4·10⁻⁷), 213/27 run-level wins, mean DtF −141 [−165, −116]. More feasible runs (21 vs 10) and lower soft cost on instance 17 (379 vs 1060) | **reverses** |
| F5: single-class days dominate the remaining soft cost | on the feasible ITC runs, last-period and consecutive-class penalties dominate (e.g. SA: 321 / 271 / 160) | **does not replicate** (but see below) |

How to read this:

- **F2 and F4 are sensitive to instance structure, not general facts about the algorithms.**
  The synthetic instances are easy to make feasible: every degree-based greedy reaches
  feasibility, and local search only optimises soft cost. ITC-2007 is tight. Most runs end
  infeasible, and the objective is dominated by the hard term. In that regime, dynamic
  saturation ordering helps construction, as the graph-colouring literature predicts. SA's
  soft-calibrated temperature lets it drift across hard-neutral plateaus, accepting
  soft-worsening moves that keep the hard count, and this reaches lower-violation states
  that descent does not. On synthetic instances the same mobility only wasted budget. Both
  explanations are hypotheses consistent with the data and have not been tested directly.
- **The ITC evidence is about the hard objective.** Only F1 and F3 can be tested on the
  quantity they were about. F4 is reversed on hard violations and DtF. Its soft-cost version
  rests on a single instance (17). F5 cannot be tested properly: the decomposition pools
  different instances for different solvers (descent is feasible only on 17, SA on 3, 16 and
  17), and ITC students attend many more events than synthetic ones, which makes single-class
  days rare by construction.
- **Dependence.** The ten seeds of an instance share its structure, so run-level p-values are
  optimistic. The instance-level sign tests above (n = 24) are the conservative ones, and all
  quoted verdicts hold at that level. `greedy-input` is deterministic, so its ten seeds per
  instance are identical copies.

## 5. Threats to validity

- **Synthetic instances.** The generator controls size and skew, but real enrolment data has
  structure it lacks: curricula, co-requisites, cohort blocks. On the ITC-2007 post-enrolment
  instances (§4.3), F1 and F3 replicate, F2 and F4 do not, and F5 cannot be tested. The
  synthetic F2/F4 conclusions should therefore be read as holding for *easy-to-satisfy*
  instances only. ITC-2007 itself is synthetic, generated by the organisers to resemble real
  data, so it is still not data from a real institution.
- **Not the competition's objective.** The solvers minimise weighted hard violations, not
  distance to feasibility. ITC scores are obtained by a post-hoc repair (§4.3), and solvers
  that optimise DtF directly could rank differently.
- **One SA configuration.** F4 concerns *this* calibration (p₀ = 0.5, T_end/T₀ = 10⁻³,
  geometric cooling). A cooler start or reheating could reverse it. F4 rejects
  "annealing as configured helps", not "annealing cannot help".
- **Budget in iterations, not time.** Descent and SA perform the same number of proposals at
  about the same cost per move, so iteration and time budgets coincide here. Solvers with
  different per-move costs would need time-based comparisons, and so does any comparison with
  the ITC-2007 finalists, who ran under a wall-clock limit.
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
3. **Feasibility on ITC-2007 (follows from §4.3).** Add a feasibility phase that optimises
   distance to feasibility directly, by searching over partial timetables. Then repeat the
   temperature sweep of item 1 on ITC instances to test whether SA's advantage there comes
   from plateau mobility under a dominant hard term.
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
# ITC-2007 (about 1 hour with 6 threads; --threads changes timings only, not costs)
scripts/fetch-itc2007.sh
./mvnw -q exec:java -Dexec.args="--itc data/itc2007 --seeds 10 --iterations 1000000 --threads 6 --out docs/research/results/itc2007"
# optional: --solutions DIR writes every timetable as .sln for the official checksln3b validator
```

Instances, solvers and bootstrap resampling are all seeded, so solution costs reproduce
exactly. Timings do not. The sweep results were produced before the descent control was
added to the runner. SA and DSATUR behave identically in both versions; only solver labels
differ.
