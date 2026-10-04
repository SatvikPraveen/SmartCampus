package scheduling.experiment;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.ToDoubleFunction;

import scheduling.eval.CostBreakdown;
import scheduling.eval.ItcScore;
import scheduling.eval.TimetableState;
import scheduling.io.Itc2007Loader;
import scheduling.model.TimetablingProblem;
import scheduling.solver.GreedySolver;
import scheduling.solver.SolverResult;
import scheduling.solver.TimetableSolver;

/**
 * Benchmark of all solvers on the ITC-2007 post-enrolment (track 2) instances.
 *
 * <p>The design mirrors the synthetic benchmark: every solver runs on every instance with seeds
 * {@code 1..seeds}, local search has a fixed iteration budget, and comparisons are paired by
 * (instance, seed). Each run is reported twice:</p>
 * <ul>
 *   <li>in the engine's terms: hard-violation count and soft cost of the complete timetable the
 *       solver returned, so "feasible" means every event is placed without violating a hard
 *       constraint;</li>
 *   <li>in the competition's terms ({@link ItcScore}): events are unplaced until no hard
 *       violation remains, and the timetable is scored by distance to feasibility (students in
 *       unplaced events) and then by soft cost.</li>
 * </ul>
 */
public final class ItcExperiment {

    private ItcExperiment() {
    }

    /** One solver run on one instance. */
    record Run(String instance, int seed, SolverResult result, ItcScore itc) {

        double objective() {
            return result.cost().weighted(TimetableSolver.HARD_WEIGHT);
        }

        double itcObjective() {
            return itc.distanceToFeasibility() * (double) TimetableSolver.HARD_WEIGHT + itc.soft();
        }
    }

    /**
     * Runs the benchmark and writes {@code runs.csv} and {@code summary.md} to {@code out}.
     *
     * @param threads   number of instances solved concurrently; costs do not depend on it (every run
     *                  is seeded and independent), only the measured wall-clock times do
     * @param solutions directory receiving every repaired timetable in the competition's
     *                  {@code .sln} format, or {@code null} to skip writing them
     */
    static void run(Path dir, String instanceSpec, int seeds, long iterations, int threads, Path out,
                    Path solutions) throws IOException {
        List<Integer> ids = parseInstances(instanceSpec);
        GreedySolver dsatur = new GreedySolver(GreedySolver.Ordering.DSATUR);
        List<TimetableSolver> solvers = ExperimentRunner.solvers(dsatur, iterations);
        List<String> instances = new ArrayList<>();
        Map<String, TimetablingProblem> problems = new LinkedHashMap<>();
        for (int id : ids) {
            String name = "comp-2007-2-" + id;
            Path file = dir.resolve(name + ".tim");
            if (!Files.isRegularFile(file)) {
                throw new IOException("Missing instance " + file
                        + "; run scripts/fetch-itc2007.sh to download the ITC-2007 data set");
            }
            problems.put(name, Itc2007Loader.load(file));
            instances.add(name);
        }
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
        List<Future<List<Run>>> futures = new ArrayList<>();
        for (Map.Entry<String, TimetablingProblem> entry : problems.entrySet()) {
            futures.add(pool.submit(() -> solveInstance(entry.getKey(), entry.getValue(), solvers, seeds, solutions)));
        }
        List<Run> runs = new ArrayList<>();
        try {
            for (Future<List<Run>> f : futures) {
                runs.addAll(f.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("Solver run failed", e.getCause());
        } finally {
            pool.shutdownNow();
        }
        Files.createDirectories(out);
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out.resolve("runs.csv")))) {
            csv.println("instance,seed,events,students,rooms,precedences,conflict_density,solver,"
                    + "hard,soft,unassigned,student_clashes,room_clashes,capacity_violations,"
                    + "feature_violations,unavailable_slots,precedence_violations,"
                    + "last_period,consecutive,single_class_days,itc_unplaced,itc_distance_to_feasibility,"
                    + "itc_soft,iterations,millis");
            for (Run run : runs) {
                TimetablingProblem problem = problems.get(run.instance());
                SolverResult r = run.result();
                ItcScore itc = run.itc();
                CostBreakdown c = r.cost();
                csv.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%.4f,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f%n",
                        run.instance(), run.seed(), problem.eventCount(), problem.studentCount(),
                        problem.roomCount(), problem.precedenceCount(), problem.conflictDensity(), r.solver(),
                        c.hard(), c.soft(), c.unassigned(), c.studentClashes(), c.roomClashes(),
                        c.capacityViolations(), c.featureViolations(), c.unavailableSlots(),
                        c.precedenceViolations(), c.lastPeriod(), c.consecutive(), c.singleClassDays(),
                        itc.unplaced(), itc.distanceToFeasibility(), itc.soft(), r.iterations(),
                        r.elapsedMillis());
            }
        }
        String summary = summarise(runs, instances, solvers, seeds, iterations, dsatur.name());
        Files.writeString(out.resolve("summary.md"), summary);
        System.out.println();
        System.out.println(summary);
    }

    private static List<Run> solveInstance(String name, TimetablingProblem problem, List<TimetableSolver> solvers,
                                           int seeds, Path solutions) throws IOException {
        List<Run> runs = new ArrayList<>();
        if (solutions != null) {
            Files.createDirectories(solutions);
        }
        for (int seed = 1; seed <= seeds; seed++) {
            for (TimetableSolver solver : solvers) {
                SolverResult r = solver.solve(problem, seed);
                TimetableState repaired = ItcScore.repair(TimetableState.of(problem, r.slots(), r.rooms()));
                ItcScore itc = ItcScore.score(repaired);
                runs.add(new Run(name, seed, r, itc));
                if (solutions != null) {
                    String file = name + "_" + r.solver().replaceAll("[^A-Za-z0-9-]+", "_") + "_s" + seed + ".sln";
                    Files.writeString(solutions.resolve(file),
                            Itc2007Loader.toSolution(repaired.slots(), repaired.rooms()));
                }
                System.out.printf(Locale.ROOT, "%-14s seed=%-2d %-24s hard=%-4d soft=%-5d dtf=%-4d itc-soft=%-5d %8.1f ms%n",
                        name, seed, r.solver(), r.cost().hard(), r.cost().soft(), itc.distanceToFeasibility(),
                        itc.soft(), r.elapsedMillis());
            }
        }
        return runs;
    }

    static List<Integer> parseInstances(String spec) {
        List<Integer> ids = new ArrayList<>();
        for (String part : spec.split(",")) {
            String p = part.trim();
            int dash = p.indexOf('-');
            if (dash > 0) {
                int from = Integer.parseInt(p.substring(0, dash));
                int to = Integer.parseInt(p.substring(dash + 1));
                for (int i = from; i <= to; i++) {
                    ids.add(i);
                }
            } else {
                ids.add(Integer.parseInt(p));
            }
        }
        return ids;
    }

    static String summarise(List<Run> runs, List<String> instances, List<TimetableSolver> solvers,
                            int seeds, long iterations, String saBase) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ITC-2007 post-enrolment benchmark summary\n\n");
        sb.append(String.format(Locale.ROOT, "Instances: %d. Seeds per instance: %d. Local-search iterations: %d. "
                + "Intervals are 95%% percentile-bootstrap CIs of the mean (%d resamples).%n%n",
                instances.size(), seeds, iterations, ExperimentRunner.BOOTSTRAP_RESAMPLES));
        sb.append("*Feasible* means every event placed with no hard violation (the engine's definition). "
                + "*DtF* and *ITC soft* are the competition's measures after unplacing events until no hard "
                + "violation remains (`ItcScore`).\n\n");

        sb.append("## Overall\n\n");
        sb.append("| solver | feasible runs | instances feasible at least once | mean DtF | "
                + "soft, feasible runs (mean) | time ms (median) |\n|---|---:|---:|---:|---:|---:|\n");
        for (TimetableSolver solver : solvers) {
            List<Run> rs = select(runs, null, solver.name());
            long feasible = rs.stream().filter(r -> r.result().cost().feasible()).count();
            long instancesFeasible = instances.stream()
                    .filter(i -> select(runs, i, solver.name()).stream().anyMatch(r -> r.result().cost().feasible()))
                    .count();
            double[] softFeasible = rs.stream().filter(r -> r.result().cost().feasible())
                    .mapToDouble(r -> r.result().cost().soft()).toArray();
            sb.append(String.format(Locale.ROOT, "| %s | %d/%d | %d/%d | %.1f | %s | %.1f |%n",
                    solver.name(), feasible, rs.size(), instancesFeasible, instances.size(),
                    rs.stream().mapToLong(r -> r.itc().distanceToFeasibility()).average().orElse(0),
                    softFeasible.length == 0 ? "-" : String.format(Locale.ROOT, "%.1f", Statistics.mean(softFeasible)),
                    Statistics.median(rs.stream().mapToDouble(r -> r.result().elapsedMillis()).toArray())));
        }

        sb.append("\n## Paired comparisons over all (instance, seed) pairs\n\n");
        sb.append("Lexicographic objective (hard violations, then soft cost) as used by the solvers, and the "
                + "competition's (DtF, then soft). Wins/losses/ties count runs where the first solver is "
                + "better/worse/equal; exact two-sided sign test. Runs on the same instance share structure, "
                + "so the pairs are not fully independent and p-values are optimistic. The soft difference "
                + "is restricted to pairs where both runs are feasible.\n\n");
        sb.append("| comparison | engine W/L/T | sign-test p | ITC W/L/T | sign-test p | "
                + "DtF diff (mean, 95% CI) | soft diff, both feasible (mean, 95% CI) | pairs |\n"
                + "|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (String[] pair : ExperimentRunner.comparisons(saBase)) {
            List<Run> a = select(runs, null, pair[0]);
            List<Run> b = select(runs, null, pair[1]);
            if (a.isEmpty() || a.size() != b.size()) {
                continue;
            }
            List<double[]> soft = new ArrayList<>();
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i).result().cost().feasible() && b.get(i).result().cost().feasible()) {
                    soft.add(new double[] {a.get(i).result().cost().soft(), b.get(i).result().cost().soft()});
                }
            }
            String softCell = "-";
            if (!soft.isEmpty()) {
                var d = Statistics.pairedDifference(soft.stream().mapToDouble(x -> x[0]).toArray(),
                        soft.stream().mapToDouble(x -> x[1]).toArray(), 0.95,
                        ExperimentRunner.BOOTSTRAP_RESAMPLES, ExperimentRunner.BOOTSTRAP_SEED);
                softCell = String.format(Locale.ROOT, "%.1f [%.1f, %.1f]", d.estimate(), d.lower(), d.upper());
            }
            var dtf = Statistics.pairedDifference(
                    a.stream().mapToDouble(r -> r.itc().distanceToFeasibility()).toArray(),
                    b.stream().mapToDouble(r -> r.itc().distanceToFeasibility()).toArray(), 0.95,
                    ExperimentRunner.BOOTSTRAP_RESAMPLES, ExperimentRunner.BOOTSTRAP_SEED);
            sb.append(String.format(Locale.ROOT, "| %s - %s | %s | %s | %.1f [%.1f, %.1f] | %s | %d |%n",
                    pair[0], pair[1], paired(a, b, Run::objective), paired(a, b, Run::itcObjective),
                    dtf.estimate(), dtf.lower(), dtf.upper(), softCell, soft.size()));
        }

        sb.append("\n## Feasible runs per instance\n\n");
        appendInstanceTable(sb, runs, instances, solvers, rs -> {
            long k = rs.stream().filter(r -> r.result().cost().feasible()).count();
            return k + "/" + rs.size();
        });
        sb.append("\n## Mean distance to feasibility per instance (ITC; 0 = every event placed)\n\n");
        appendInstanceTable(sb, runs, instances, solvers, rs -> String.format(Locale.ROOT, "%.0f",
                rs.stream().mapToLong(r -> r.itc().distanceToFeasibility()).average().orElse(0)));
        sb.append("\n## Mean hard violations per instance (engine, complete timetable)\n\n");
        appendInstanceTable(sb, runs, instances, solvers, rs -> String.format(Locale.ROOT, "%.1f",
                rs.stream().mapToLong(r -> r.result().cost().hard()).average().orElse(0)));
        sb.append("\n## Mean soft cost per instance (engine; feasible runs only, `-` if none)\n\n");
        appendInstanceTable(sb, runs, instances, solvers, rs -> {
            double[] soft = rs.stream().filter(r -> r.result().cost().feasible())
                    .mapToDouble(r -> r.result().cost().soft()).toArray();
            return soft.length == 0 ? "-" : String.format(Locale.ROOT, "%.0f", Statistics.mean(soft));
        });
        sb.append("\n## Best ITC score per instance (DtF / soft over the seeds, lexicographic)\n\n");
        appendInstanceTable(sb, runs, instances, solvers, rs -> {
            Run best = rs.stream().min((x, y) -> Double.compare(x.itcObjective(), y.itcObjective())).orElseThrow();
            return best.itc().distanceToFeasibility() + " / " + best.itc().soft();
        });
        sb.append("\n## Median ITC score per instance (DtF / soft, lexicographic median run)\n\n");
        appendInstanceTable(sb, runs, instances, solvers, rs -> {
            List<Run> sorted = rs.stream().sorted((x, y) -> Double.compare(x.itcObjective(), y.itcObjective())).toList();
            Run median = sorted.get((sorted.size() - 1) / 2);
            return median.itc().distanceToFeasibility() + " / " + median.itc().soft();
        });

        sb.append("\n## Soft-penalty decomposition (means over feasible runs)\n\n");
        sb.append("| solver | last period | >2 consecutive | single-class days |\n|---|---:|---:|---:|\n");
        for (TimetableSolver solver : solvers) {
            List<Run> rs = select(runs, null, solver.name()).stream()
                    .filter(r -> r.result().cost().feasible()).toList();
            if (rs.isEmpty()) {
                sb.append("| ").append(solver.name()).append(" | - | - | - |\n");
                continue;
            }
            sb.append(String.format(Locale.ROOT, "| %s | %.1f | %.1f | %.1f |%n", solver.name(),
                    rs.stream().mapToLong(r -> r.result().cost().lastPeriod()).average().orElse(0),
                    rs.stream().mapToLong(r -> r.result().cost().consecutive()).average().orElse(0),
                    rs.stream().mapToLong(r -> r.result().cost().singleClassDays()).average().orElse(0)));
        }
        return sb.toString();
    }

    private static String paired(List<Run> a, List<Run> b, ToDoubleFunction<Run> key) {
        double[] x = a.stream().mapToDouble(key).toArray();
        double[] y = b.stream().mapToDouble(key).toArray();
        int wins = 0;
        int losses = 0;
        for (int i = 0; i < x.length; i++) {
            wins += x[i] < y[i] ? 1 : 0;
            losses += x[i] > y[i] ? 1 : 0;
        }
        return String.format(Locale.ROOT, "%d/%d/%d | %.2g", wins, losses, x.length - wins - losses,
                Statistics.signTestPValue(x, y));
    }

    private interface Cell {
        String of(List<Run> runs);
    }

    private static void appendInstanceTable(StringBuilder sb, List<Run> runs, List<String> instances,
                                            List<TimetableSolver> solvers, Cell cell) {
        sb.append("| instance |");
        for (TimetableSolver solver : solvers) {
            sb.append(' ').append(solver.name()).append(" |");
        }
        sb.append("\n|---|");
        sb.append("---:|".repeat(solvers.size()));
        sb.append('\n');
        for (String instance : instances) {
            sb.append("| ").append(instance.replace("comp-2007-2-", "")).append(" |");
            for (TimetableSolver solver : solvers) {
                sb.append(' ').append(cell.of(select(runs, instance, solver.name()))).append(" |");
            }
            sb.append('\n');
        }
    }

    private static List<Run> select(List<Run> runs, String instance, String solver) {
        return runs.stream()
                .filter(r -> (instance == null || r.instance().equals(instance)) && r.result().solver().equals(solver))
                .toList();
    }
}
