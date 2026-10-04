package scheduling.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Immutable description of a post-enrolment course timetabling instance.
 *
 * <p>Students and instructors are mapped to dense integer indices at construction time so that
 * the evaluator and solvers can work on primitive arrays. The <em>conflict graph</em> has one
 * vertex per event and an edge between two events that share at least one student or the same
 * instructor; any two adjacent events placed in the same slot violate a hard constraint.</p>
 *
 * <p>Three optional side constraints model the ITC-2007 post-enrolment track: room
 * <em>features</em> (an event may only use rooms offering all features it requires), per-event
 * slot <em>availability</em>, and <em>precedence</em> between events. When none is given, every
 * room that is large enough is suitable, every slot is available and no ordering is imposed, so
 * the problem is exactly the original model.</p>
 */
public final class TimetablingProblem {

    private final List<Event> events;
    private final List<Room> rooms;
    private final int days;
    private final int periodsPerDay;

    private final int studentCount;
    private final int instructorCount;
    private final int[][] eventStudents;
    private final int[] eventInstructor;
    private final int[][] conflictNeighbours;
    private final int[][] suitableRooms;

    private final boolean[][] featureOk;
    private final int[][] featureRooms;
    private final boolean[] available;
    private final int[][] availableSlots;
    private final int[] precedenceBefore;
    private final int[] precedenceAfter;
    private final int[][] precedencesOfEvent;

    /** A problem without availability or precedence constraints. */
    public TimetablingProblem(List<Event> events, List<Room> rooms, int days, int periodsPerDay) {
        this(events, rooms, days, periodsPerDay, Map.of(), List.of());
    }

    /**
     * A problem with optional side constraints.
     *
     * @param availableSlots for each event id listed, the slot indices ({@code day * periodsPerDay +
     *                       period}) the event may use; events not listed may use every slot
     * @param precedences    ordering constraints between events; duplicates are ignored
     */
    public TimetablingProblem(List<Event> events, List<Room> rooms, int days, int periodsPerDay,
                              Map<String, Set<Integer>> availableSlots, List<Precedence> precedences) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("At least one event is required");
        }
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("At least one room is required");
        }
        if (days <= 0 || periodsPerDay <= 0) {
            throw new IllegalArgumentException("days and periodsPerDay must be positive");
        }
        this.events = List.copyOf(events);
        this.rooms = List.copyOf(rooms);
        this.days = days;
        this.periodsPerDay = periodsPerDay;

        Map<String, Integer> studentIndex = new HashMap<>();
        Map<String, Integer> instructorIndex = new HashMap<>();
        Map<String, Integer> eventIndex = new HashMap<>();
        int n = this.events.size();
        eventStudents = new int[n][];
        eventInstructor = new int[n];
        for (int e = 0; e < n; e++) {
            Event event = this.events.get(e);
            if (eventIndex.put(event.id(), e) != null) {
                throw new IllegalArgumentException("Duplicate event id: " + event.id());
            }
            eventStudents[e] = event.studentIds().stream()
                    .sorted()
                    .mapToInt(s -> studentIndex.computeIfAbsent(s, k -> studentIndex.size()))
                    .toArray();
            eventInstructor[e] = event.instructorId() == null
                    ? -1
                    : instructorIndex.computeIfAbsent(event.instructorId(), k -> instructorIndex.size());
        }
        studentCount = studentIndex.size();
        instructorCount = instructorIndex.size();
        conflictNeighbours = buildConflictGraph(n);
        featureOk = buildFeatureMatrix(n);
        featureRooms = new int[n][];
        for (int e = 0; e < n; e++) {
            final int ev = e;
            featureRooms[e] = IntStream.range(0, this.rooms.size()).filter(r -> featureOk[ev][r]).toArray();
        }
        suitableRooms = buildSuitableRooms(n);

        int t = slotCount();
        available = new boolean[n * t];
        Arrays.fill(available, true);
        for (Map.Entry<String, Set<Integer>> entry : availableSlots.entrySet()) {
            int e = indexOf(eventIndex, entry.getKey());
            Arrays.fill(available, e * t, (e + 1) * t, false);
            for (int slot : entry.getValue()) {
                if (slot < 0 || slot >= t) {
                    throw new IllegalArgumentException("Slot out of range for event " + entry.getKey() + ": " + slot);
                }
                available[e * t + slot] = true;
            }
        }
        this.availableSlots = new int[n][];
        for (int e = 0; e < n; e++) {
            final int base = e * t;
            this.availableSlots[e] = IntStream.range(0, t).filter(s -> available[base + s]).toArray();
        }

        Set<List<Integer>> seen = new LinkedHashSet<>();
        for (Precedence pr : precedences) {
            seen.add(List.of(indexOf(eventIndex, pr.before()), indexOf(eventIndex, pr.after())));
        }
        precedenceBefore = seen.stream().mapToInt(l -> l.get(0)).toArray();
        precedenceAfter = seen.stream().mapToInt(l -> l.get(1)).toArray();
        List<List<Integer>> byEvent = new ArrayList<>(n);
        for (int e = 0; e < n; e++) {
            byEvent.add(new ArrayList<>());
        }
        for (int k = 0; k < precedenceBefore.length; k++) {
            byEvent.get(precedenceBefore[k]).add(k);
            byEvent.get(precedenceAfter[k]).add(k);
        }
        precedencesOfEvent = new int[n][];
        for (int e = 0; e < n; e++) {
            precedencesOfEvent[e] = byEvent.get(e).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private static int indexOf(Map<String, Integer> eventIndex, String id) {
        Integer e = eventIndex.get(id);
        if (e == null) {
            throw new IllegalArgumentException("Unknown event id: " + id);
        }
        return e;
    }

    private boolean[][] buildFeatureMatrix(int n) {
        boolean[][] ok = new boolean[n][rooms.size()];
        for (int e = 0; e < n; e++) {
            Set<String> required = events.get(e).requiredFeatures();
            for (int r = 0; r < rooms.size(); r++) {
                ok[e][r] = rooms.get(r).features().containsAll(required);
            }
        }
        return ok;
    }

    private int[][] buildConflictGraph(int n) {
        List<List<Integer>> eventsOfStudent = new ArrayList<>(studentCount);
        for (int s = 0; s < studentCount; s++) {
            eventsOfStudent.add(new ArrayList<>());
        }
        List<List<Integer>> eventsOfInstructor = new ArrayList<>(instructorCount);
        for (int i = 0; i < instructorCount; i++) {
            eventsOfInstructor.add(new ArrayList<>());
        }
        for (int e = 0; e < n; e++) {
            for (int s : eventStudents[e]) {
                eventsOfStudent.get(s).add(e);
            }
            if (eventInstructor[e] >= 0) {
                eventsOfInstructor.get(eventInstructor[e]).add(e);
            }
        }
        BitSet[] adjacency = new BitSet[n];
        for (int e = 0; e < n; e++) {
            adjacency[e] = new BitSet(n);
        }
        List<List<Integer>> groups = new ArrayList<>(eventsOfStudent);
        groups.addAll(eventsOfInstructor);
        for (List<Integer> group : groups) {
            for (int a = 0; a < group.size(); a++) {
                for (int b = a + 1; b < group.size(); b++) {
                    adjacency[group.get(a)].set(group.get(b));
                    adjacency[group.get(b)].set(group.get(a));
                }
            }
        }
        int[][] neighbours = new int[n][];
        for (int e = 0; e < n; e++) {
            neighbours[e] = adjacency[e].stream().toArray();
        }
        return neighbours;
    }

    private int[][] buildSuitableRooms(int n) {
        int[][] result = new int[n][];
        for (int e = 0; e < n; e++) {
            int size = eventStudents[e].length;
            boolean[] ok = featureOk[e];
            result[e] = IntStream.range(0, rooms.size())
                    .filter(r -> rooms.get(r).capacity() >= size && ok[r])
                    .boxed()
                    .sorted((a, b) -> Integer.compare(rooms.get(a).capacity(), rooms.get(b).capacity()))
                    .mapToInt(Integer::intValue)
                    .toArray();
        }
        return result;
    }

    public List<Event> events() { return events; }
    public List<Room> rooms() { return rooms; }
    public int days() { return days; }
    public int periodsPerDay() { return periodsPerDay; }
    public int eventCount() { return events.size(); }
    public int roomCount() { return rooms.size(); }
    public int slotCount() { return days * periodsPerDay; }
    public int studentCount() { return studentCount; }
    public int instructorCount() { return instructorCount; }

    public int dayOf(int slot) { return slot / periodsPerDay; }
    public int periodOf(int slot) { return slot % periodsPerDay; }
    public int slot(int day, int period) { return day * periodsPerDay + period; }

    /** Dense student indices of event {@code e}; callers must not modify the array. */
    public int[] studentsOf(int e) { return eventStudents[e]; }

    /** Dense instructor index of event {@code e}, or {@code -1} if none. */
    public int instructorOf(int e) { return eventInstructor[e]; }

    /** Events sharing a student or instructor with {@code e}; callers must not modify the array. */
    public int[] neighboursOf(int e) { return conflictNeighbours[e]; }

    /** Degree of {@code e} in the conflict graph. */
    public int degreeOf(int e) { return conflictNeighbours[e].length; }

    /**
     * Rooms large enough for {@code e} that offer all its required features, ordered by ascending
     * capacity (best fit first).
     */
    public int[] suitableRoomsOf(int e) { return suitableRooms[e]; }

    /** True when {@code room} offers every feature event {@code e} requires (regardless of size). */
    public boolean hasRequiredFeatures(int e, int room) { return featureOk[e][room]; }

    /** Rooms offering every feature {@code e} requires, in input order; callers must not modify. */
    public int[] featureRoomsOf(int e) { return featureRooms[e]; }

    /** True when event {@code e} may be held in {@code slot}. */
    public boolean isAvailable(int e, int slot) { return available[e * slotCount() + slot]; }

    /** Slots event {@code e} may use, ascending; callers must not modify the array. */
    public int[] availableSlotsOf(int e) { return availableSlots[e]; }

    /** Number of distinct precedence constraints. */
    public int precedenceCount() { return precedenceBefore.length; }

    /** Event that must come first in precedence constraint {@code k}. */
    public int precedenceBefore(int k) { return precedenceBefore[k]; }

    /** Event that must come later in precedence constraint {@code k}. */
    public int precedenceAfter(int k) { return precedenceAfter[k]; }

    /** Indices of the precedence constraints involving {@code e}; callers must not modify. */
    public int[] precedencesOf(int e) { return precedencesOfEvent[e]; }

    public int capacityOf(int room) { return rooms.get(room).capacity(); }

    /** Number of edges in the conflict graph. */
    public long conflictEdgeCount() {
        return Arrays.stream(conflictNeighbours).mapToLong(a -> a.length).sum() / 2;
    }

    /** Edge density of the conflict graph in [0, 1]. */
    public double conflictDensity() {
        long n = eventCount();
        return n < 2 ? 0.0 : (2.0 * conflictEdgeCount()) / (n * (n - 1));
    }
}
