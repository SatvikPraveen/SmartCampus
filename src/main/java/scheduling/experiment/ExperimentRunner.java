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

import scheduling.model.TimetablingProblem;
import scheduling.solver.GreedySolver;
import scheduling.solver.RandomSolver;
import scheduling.solver.SimulatedAnnealingSolver;
import scheduling.solver.SolverResult;
import scheduling.solver.TimetableSolver;

/**
 * Reproducible benchmark of all timetabling solvers.
 *
 * <p>For every instance family and every seed, one instance is generated and every solver is run
 * on that same instance with the same seed, so comparisons are <em>paired</em>. Results are written
 * as one CSV row per run ({@code runs.csv}) and summarised in Markdown ({@code summary.md}) with
 * means, standard deviations, 95% percentile-bootstrap confidence intervals and a paired comparison
 * of simulated annealing against its own initial solution.</p>
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.mainClass=scheduling.experiment.ExperimentRunner \
 *     -Dexec.args="--seeds 10 --iterations 200000 --out results"
 * </pre>
 */
public final class ExperimentRunner {

    private static final int BOOTSTRAP_RESAMPLES = 10_000;
    private static final long BOOTSTRAP_SEED = 20251004L;

    private ExperimentRunner() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> opts = parse(args);
        int seeds = Integer.parseInt(opts.getOrDefault("seeds", "10"));
        long iterations = Long.parseLong(opts.getOrDefault("iterations", "200000"));
        Path out = Path.of(opts.getOrDefault("out", "results"));
        List<InstanceGenerator.Params> families = families(opts.getOrDefault("sizes", "small,medium,large"));

        GreedySolver dsatur = new GreedySolver(GreedySolver.Ordering.DSATUR);
        List<TimetableSolver> solvers = List.of(
                new RandomSolver(),
                new GreedySolver(GreedySolver.Ordering.INPUT),
                new GreedySolver(GreedySolver.Ordering.RANDOM),
                new GreedySolver(GreedySolver.Ordering.LARGEST_DEGREE),
                dsatur,
                SimulatedAnnealingSolver.descent(dsatur, iterations),
                new SimulatedAnnealingSolver(dsatur, SimulatedAnnealingSolver.Config.defaults(iterations)));

        Files.createDirectories(out);
        List<Row> rows = new ArrayList<>();
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(out.resolve("runs.csv")))) {
            csv.println("instance,seed,events,students,rooms,slots,conflict_density,solver,"
                    + "hard,soft,unassigned,student_clashes,instructor_clashes,room_clashes,"
                    + "capacity_violations,last_period,consecutive,single_class_days,millis");
            for (InstanceGenerator.Params family : families) {
                for (int seed = 1; seed <= seeds; seed++) {
                    TimetablingProblem problem = InstanceGenerator.generate(family, seed);
                    for (TimetableSolver solver : solvers) {
                        SolverResult r = solver.solve(problem, seed);
                        rows.add(new Row(family.name(), seed, r));
                        var c = r.cost();
                        csv.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%.4f,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f%n",
                                family.name(), seed, problem.eventCount(), problem.studentCount(),
                                problem.roomCount(), problem.slotCount(), problem.conflictDensity(),
                                r.solver(), c.hard(), c.soft(), c.unassigned(), c.studentClashes(),
                                c.instructorClashes(), c.roomClashes(), c.capacityViolations(),
                                c.lastPeriod(), c.consecutive(), c.singleClassDays(), r.elapsedMillis());
                        System.out.printf(Locale.ROOT, "%-7s seed=%-3d %-24s hard=%-5d soft=%-6d %8.1f ms%n",
                                family.name(), seed, r.solver(), c.hard(), c.soft(), r.elapsedMillis());
                    }
                }
            }
        }
        String summary = summarise(rows, families, solvers, seeds, iterations, dsatur.name());
        Files.writeString(out.resolve("summary.md"), summary);
        System.out.println();
        System.out.println(summary);
    }

    private record Row(String instance, int seed, SolverResult result) {
    }

    static String summarise(List<Row> rows, List<InstanceGenerator.Params> families,
                            List<TimetableSolver> solvers, int seeds, long iterations, String saBase) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Timetabling benchmark summary\n\n");
        sb.append(String.format(Locale.ROOT, "Seeds per instance family: %d. SA iterations: %d. "
                + "Intervals are 95%% percentile-bootstrap CIs of the mean (%d resamples).%n%n",
                seeds, iterations, BOOTSTRAP_RESAMPLES));
        for (InstanceGenerator.Params family : families) {
            sb.append("## ").append(family.name()).append(String.format(Locale.ROOT,
                    " (%d events, %d students, %d rooms, %d slots)%n%n", family.events(), family.students(),
                    family.rooms(), family.days() * family.periodsPerDay()));
            sb.append("| solver | feasible | hard (mean, 95% CI) | soft (mean, 95% CI) | soft sd | time ms (median) |\n");
            sb.append("|---|---:|---:|---:|---:|---:|\n");
            for (TimetableSolver solver : solvers) {
                List<SolverResult> rs = select(rows, family.name(), solver.name());
                double[] hard = rs.stream().mapToDouble(r -> r.cost().hard()).toArray();
                double[] soft = rs.stream().mapToDouble(r -> r.cost().soft()).toArray();
                double[] ms = rs.stream().mapToDouble(SolverResult::elapsedMillis).toArray();
                long feasible = rs.stream().filter(r -> r.cost().feasible()).count();
                var h = Statistics.bootstrapMean(hard, 0.95, BOOTSTRAP_RESAMPLES, BOOTSTRAP_SEED);
                var s = Statistics.bootstrapMean(soft, 0.95, BOOTSTRAP_RESAMPLES, BOOTSTRAP_SEED);
                sb.append(String.format(Locale.ROOT, "| %s | %d/%d | %.1f [%.1f, %.1f] | %.1f [%.1f, %.1f] | %.1f | %.1f |%n",
                        solver.name(), feasible, rs.size(), h.estimate(), h.lower(), h.upper(),
                        s.estimate(), s.lower(), s.upper(), Statistics.standardDeviation(soft),
                        Statistics.median(ms)));
            }
            sb.append(String.format(Locale.ROOT, "%nSoft-penalty decomposition (means):%n%n"));
            sb.append("| solver | last period | >2 consecutive | single-class days |\n|---|---:|---:|---:|\n");
            for (TimetableSolver solver : solvers) {
                List<SolverResult> rs = select(rows, family.name(), solver.name());
                sb.append(String.format(Locale.ROOT, "| %s | %.1f | %.1f | %.1f |%n", solver.name(),
                        rs.stream().mapToLong(r -> r.cost().lastPeriod()).average().orElse(0),
                        rs.stream().mapToLong(r -> r.cost().consecutive()).average().orElse(0),
                        rs.stream().mapToLong(r -> r.cost().singleClassDays()).average().orElse(0)));
            }
            sb.append(String.format(Locale.ROOT, "%nPaired weighted-objective differences "
                    + "(negative = first solver better; 95%% bootstrap CI; exact two-sided sign test):%n%n"));
            sb.append("| comparison | mean diff | 95% CI | wins/losses/ties | sign-test p |\n|---|---:|---:|---:|---:|\n");
            String sa = "sa(" + saBase + ")";
            String descent = "descent(" + saBase + ")";
            String[][] pairs = {
                {sa, saBase}, {descent, saBase}, {sa, descent}, {saBase, "greedy-largest-degree"},
                {"greedy-largest-degree", "greedy-input"}};
            for (String[] pair : pairs) {
                double[] a = objective(select(rows, family.name(), pair[0]));
                double[] b = objective(select(rows, family.name(), pair[1]));
                if (a.length == 0 || a.length != b.length) {
                    continue;
                }
                var diff = Statistics.pairedDifference(a, b, 0.95, BOOTSTRAP_RESAMPLES, BOOTSTRAP_SEED);
                int wins = 0;
                int losses = 0;
                for (int i = 0; i < a.length; i++) {
                    wins += a[i] < b[i] ? 1 : 0;
                    losses += a[i] > b[i] ? 1 : 0;
                }
                sb.append(String.format(Locale.ROOT, "| %s - %s | %.1f | [%.1f, %.1f] | %d/%d/%d | %.4f |%n",
                        pair[0], pair[1], diff.estimate(), diff.lower(), diff.upper(),
                        wins, losses, a.length - wins - losses, Statistics.signTestPValue(a, b)));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static double[] objective(List<SolverResult> rs) {
        return rs.stream().mapToDouble(r -> r.cost().weighted(TimetableSolver.HARD_WEIGHT)).toArray();
    }

    private static List<SolverResult> select(List<Row> rows, String instance, String solver) {
        return rows.stream()
                .filter(r -> r.instance().equals(instance) && r.result().solver().equals(solver))
                .map(Row::result)
                .toList();
    }

    private static List<InstanceGenerator.Params> families(String spec) {
        Map<String, InstanceGenerator.Params> known = new LinkedHashMap<>();
        known.put("small", InstanceGenerator.SMALL);
        known.put("medium", InstanceGenerator.MEDIUM);
        known.put("large", InstanceGenerator.LARGE);
        List<InstanceGenerator.Params> result = new ArrayList<>();
        for (String name : spec.split(",")) {
            InstanceGenerator.Params p = known.get(name.trim());
            if (p == null) {
                throw new IllegalArgumentException("Unknown instance family: " + name + " (known: " + known.keySet() + ")");
            }
            result.add(p);
        }
        return result;
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--") || i + 1 >= args.length) {
                throw new IllegalArgumentException("Expected --key value pairs, got: " + String.join(" ", args));
            }
            m.put(args[i].substring(2), args[++i]);
        }
        return m;
    }
}
