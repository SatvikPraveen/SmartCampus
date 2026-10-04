package scheduling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import scheduling.model.Event;
import scheduling.model.Precedence;
import scheduling.model.Room;
import scheduling.model.TimetablingProblem;

/** Hand-built instances with known structure. */
final class Fixtures {

    private Fixtures() {
    }

    /**
     * Four events, two rooms, one day with three periods.
     * <ul>
     *   <li>e0 {a,b}, instructor X</li>
     *   <li>e1 {b,c}, instructor Y &ndash; shares student b with e0</li>
     *   <li>e2 {d}, instructor X &ndash; shares instructor with e0</li>
     *   <li>e3 {e,f,g}, no instructor &ndash; only fits the big room</li>
     * </ul>
     */
    static TimetablingProblem tiny() {
        return new TimetablingProblem(
                List.of(
                        new Event("e0", "X", Set.of("a", "b")),
                        new Event("e1", "Y", Set.of("b", "c")),
                        new Event("e2", "X", Set.of("d")),
                        new Event("e3", null, Set.of("e", "f", "g"))),
                List.of(new Room("small", 2), new Room("big", 3)),
                1, 3);
    }

    /**
     * Copy of {@code base} with random side constraints of the ITC-2007 kind: each room offers
     * each of three features with probability 1/2, each event requires each feature with
     * probability {@code featureProb}, each event is unavailable in each slot with probability
     * {@code unavailableProb}, and {@code precedences} random ordered pairs are added.
     */
    static TimetablingProblem withSideConstraints(TimetablingProblem base, long seed, double featureProb,
                                                  double unavailableProb, int precedences) {
        SplittableRandom rng = new SplittableRandom(seed);
        String[] features = {"projector", "lab", "computers"};
        List<Room> rooms = new ArrayList<>();
        for (Room r : base.rooms()) {
            Set<String> offered = new HashSet<>();
            for (String f : features) {
                if (rng.nextBoolean()) {
                    offered.add(f);
                }
            }
            rooms.add(new Room(r.id(), r.capacity(), offered));
        }
        List<Event> events = new ArrayList<>();
        Map<String, Set<Integer>> availability = new HashMap<>();
        for (Event e : base.events()) {
            Set<String> required = new HashSet<>();
            for (String f : features) {
                if (rng.nextDouble() < featureProb) {
                    required.add(f);
                }
            }
            events.add(new Event(e.id(), e.instructorId(), e.studentIds(), required));
            Set<Integer> allowed = new HashSet<>();
            for (int t = 0; t < base.slotCount(); t++) {
                if (rng.nextDouble() >= unavailableProb) {
                    allowed.add(t);
                }
            }
            availability.put(e.id(), allowed);
        }
        List<Precedence> order = new ArrayList<>();
        int n = events.size();
        while (order.size() < precedences) {
            int a = rng.nextInt(n);
            int b = rng.nextInt(n);
            if (a != b) {
                order.add(new Precedence(events.get(a).id(), events.get(b).id()));
            }
        }
        return new TimetablingProblem(events, rooms, base.days(), base.periodsPerDay(), availability, order);
    }
}
