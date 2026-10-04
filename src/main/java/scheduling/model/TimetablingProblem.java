package scheduling.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable description of a post-enrolment course timetabling instance.
 *
 * <p>Students and instructors are mapped to dense integer indices at construction time so that
 * the evaluator and solvers can work on primitive arrays. The <em>conflict graph</em> has one
 * vertex per event and an edge between two events that share at least one student or the same
 * instructor; any two adjacent events placed in the same slot violate a hard constraint.</p>
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

    public TimetablingProblem(List<Event> events, List<Room> rooms, int days, int periodsPerDay) {
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
        suitableRooms = buildSuitableRooms(n);
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
            result[e] = java.util.stream.IntStream.range(0, rooms.size())
                    .filter(r -> rooms.get(r).capacity() >= size)
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

    /** Rooms large enough for {@code e}, ordered by ascending capacity (best fit first). */
    public int[] suitableRoomsOf(int e) { return suitableRooms[e]; }

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
