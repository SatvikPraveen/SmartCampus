package scheduling.experiment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import scheduling.model.Event;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;

/**
 * Seeded generator of synthetic post-enrolment timetabling instances.
 *
 * <p>Each student enrols in {@code coursesPerStudent} distinct events drawn without replacement
 * from a Zipf distribution over events (exponent {@code zipfExponent}; 0 gives uniform
 * popularity). Real enrolment data is typically heavy-tailed: a few large introductory courses
 * and many small electives, which is what the Zipf law models. Each event is taught by one of
 * {@code instructors} instructors, assigned round-robin over a seeded permutation so that teaching
 * loads differ by at most one. Room capacities are set to evenly spaced quantiles of the realised
 * event-size distribution, inflated by {@code roomSlack}, with the largest room always able to
 * hold the largest event, so capacity is never infeasible by construction.</p>
 *
 * <p>The generator makes no claim that the instances are representative of a specific
 * institution; it provides controlled, reproducible variation of size, density and tightness.</p>
 */
public final class InstanceGenerator {

    /**
     * Generator parameters.
     *
     * @param name              label used in reports
     * @param events            number of events
     * @param students          number of students
     * @param coursesPerStudent events per student (at most {@code events})
     * @param instructors       number of instructors
     * @param rooms             number of rooms
     * @param days              days per week
     * @param periodsPerDay     periods per day
     * @param zipfExponent      popularity skew, {@code >= 0}
     * @param roomSlack         multiplicative capacity slack, {@code >= 1}
     */
    public record Params(String name, int events, int students, int coursesPerStudent, int instructors,
                         int rooms, int days, int periodsPerDay, double zipfExponent, double roomSlack) {

        public Params {
            if (coursesPerStudent > events) {
                throw new IllegalArgumentException("coursesPerStudent cannot exceed events");
            }
            if (instructors <= 0 || rooms <= 0 || students <= 0 || events <= 0) {
                throw new IllegalArgumentException("counts must be positive");
            }
            if (zipfExponent < 0 || roomSlack < 1) {
                throw new IllegalArgumentException("zipfExponent must be >= 0 and roomSlack >= 1");
            }
        }

        /** Copy with a different name and room count, used to vary tightness. */
        public Params withRooms(String newName, int newRooms) {
            return new Params(newName, events, students, coursesPerStudent, instructors, newRooms, days,
                    periodsPerDay, zipfExponent, roomSlack);
        }
    }

    /** Small instance: 100 events, 45 slots. */
    public static final Params SMALL = new Params("small", 100, 400, 4, 40, 5, 5, 9, 0.8, 1.1);
    /** Medium instance: 200 events, 45 slots. */
    public static final Params MEDIUM = new Params("medium", 200, 1000, 5, 80, 8, 5, 9, 0.8, 1.1);
    /** Large instance: 400 events, 45 slots. */
    public static final Params LARGE = new Params("large", 400, 2000, 5, 150, 12, 5, 9, 0.8, 1.1);

    private InstanceGenerator() {
    }

    public static TimetablingProblem generate(Params params, long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        int n = params.events();
        double[] cdf = zipfCdf(n, params.zipfExponent());
        int[] popularityRank = permutation(n, rng);

        List<Set<String>> enrolled = new ArrayList<>(n);
        for (int e = 0; e < n; e++) {
            enrolled.add(new HashSet<>());
        }
        for (int s = 0; s < params.students(); s++) {
            Set<Integer> chosen = new HashSet<>();
            while (chosen.size() < params.coursesPerStudent()) {
                chosen.add(popularityRank[sample(cdf, rng.nextDouble())]);
            }
            String studentId = "S" + s;
            for (int e : chosen) {
                enrolled.get(e).add(studentId);
            }
        }

        int[] instructorOrder = permutation(n, rng);
        List<Event> events = new ArrayList<>(n);
        for (int e = 0; e < n; e++) {
            String instructor = "I" + (instructorOrder[e] % params.instructors());
            events.add(new Event("E" + e, instructor, enrolled.get(e)));
        }
        return new TimetablingProblem(events, buildRooms(params, events), params.days(), params.periodsPerDay());
    }

    private static List<Room> buildRooms(Params params, List<Event> events) {
        int[] sizes = events.stream().mapToInt(Event::size).sorted().toArray();
        int r = params.rooms();
        List<Room> rooms = new ArrayList<>(r);
        for (int i = 0; i < r; i++) {
            double q = r == 1 ? 1.0 : (double) i / (r - 1);
            int size = sizes[(int) Math.round(q * (sizes.length - 1))];
            int capacity = Math.max(1, (int) Math.ceil(size * params.roomSlack()));
            if (i == r - 1) {
                capacity = Math.max(capacity, sizes[sizes.length - 1]);
            }
            rooms.add(new Room("R" + i, capacity));
        }
        return rooms;
    }

    private static double[] zipfCdf(int n, double exponent) {
        double[] cdf = new double[n];
        double total = 0;
        for (int k = 0; k < n; k++) {
            total += 1.0 / Math.pow(k + 1, exponent);
            cdf[k] = total;
        }
        for (int k = 0; k < n; k++) {
            cdf[k] /= total;
        }
        return cdf;
    }

    private static int sample(double[] cdf, double u) {
        int lo = 0;
        int hi = cdf.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cdf[mid] < u) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    private static int[] permutation(int n, SplittableRandom rng) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        return p;
    }
}
