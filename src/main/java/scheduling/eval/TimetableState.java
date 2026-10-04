package scheduling.eval;

import java.util.Arrays;

import scheduling.model.TimetablingProblem;

/**
 * Mutable timetable with exact incremental cost bookkeeping.
 *
 * <p>The state stores occupancy counts per (student, slot), (instructor, slot) and (room, slot).
 * Every cost component in {@link CostBreakdown} is a function of these counts or of the moved
 * event's own placement, so moving one event only requires re-evaluating the rows touched by that
 * event: its students on the source and target days, its instructor, its rooms, its slot
 * availability and the precedence constraints it takes part in. The cost of {@link #assign} is therefore
 * {@code O(|students(e)| * periodsPerDay)} instead of the {@code O(students * slots)} of a full
 * {@link CostModel#evaluate} call. Equality with the reference model is enforced by property
 * tests.</p>
 */
public final class TimetableState {

    private final TimetablingProblem p;
    private final int slots;
    private final int[] slotOf;
    private final int[] roomOf;
    private final int[] studentSlot;
    private final int[] instructorSlot;
    private final int[] roomSlot;

    private long unassigned;
    private long studentClashes;
    private long instructorClashes;
    private long roomClashes;
    private long capacityViolations;
    private long featureViolations;
    private long unavailableSlots;
    private long precedenceViolations;
    private long lastPeriod;
    private long consecutive;
    private long singleClassDays;

    /** Creates a state in which every event is unassigned. */
    public TimetableState(TimetablingProblem problem) {
        this.p = problem;
        this.slots = problem.slotCount();
        this.slotOf = new int[problem.eventCount()];
        this.roomOf = new int[problem.eventCount()];
        Arrays.fill(slotOf, CostModel.UNASSIGNED);
        Arrays.fill(roomOf, CostModel.UNASSIGNED);
        this.studentSlot = new int[problem.studentCount() * slots];
        this.instructorSlot = new int[problem.instructorCount() * slots];
        this.roomSlot = new int[problem.roomCount() * slots];
        this.unassigned = problem.eventCount();
    }

    private TimetableState(TimetableState o) {
        this.p = o.p;
        this.slots = o.slots;
        this.slotOf = o.slotOf.clone();
        this.roomOf = o.roomOf.clone();
        this.studentSlot = o.studentSlot.clone();
        this.instructorSlot = o.instructorSlot.clone();
        this.roomSlot = o.roomSlot.clone();
        this.unassigned = o.unassigned;
        this.studentClashes = o.studentClashes;
        this.instructorClashes = o.instructorClashes;
        this.roomClashes = o.roomClashes;
        this.capacityViolations = o.capacityViolations;
        this.featureViolations = o.featureViolations;
        this.unavailableSlots = o.unavailableSlots;
        this.precedenceViolations = o.precedenceViolations;
        this.lastPeriod = o.lastPeriod;
        this.consecutive = o.consecutive;
        this.singleClassDays = o.singleClassDays;
    }

    /** Builds a state from explicit assignment arrays. */
    public static TimetableState of(TimetablingProblem problem, int[] slots, int[] rooms) {
        TimetableState state = new TimetableState(problem);
        for (int e = 0; e < slots.length; e++) {
            if (slots[e] != CostModel.UNASSIGNED) {
                state.assign(e, slots[e], rooms[e]);
            }
        }
        return state;
    }

    /** Deep copy. */
    public TimetableState copy() {
        return new TimetableState(this);
    }

    public TimetablingProblem problem() { return p; }
    public int slotOf(int e) { return slotOf[e]; }
    public int roomOf(int e) { return roomOf[e]; }
    public int[] slots() { return slotOf.clone(); }
    public int[] rooms() { return roomOf.clone(); }

    /** Number of events currently held in {@code room} during {@code slot}. */
    public int roomOccupancy(int room, int slot) {
        return roomSlot[room * slots + slot];
    }

    /** Number of events adjacent to {@code e} in the conflict graph that are placed in {@code slot}. */
    public int conflictsInSlot(int e, int slot) {
        int c = 0;
        for (int f : p.neighboursOf(e)) {
            if (slotOf[f] == slot) {
                c++;
            }
        }
        return c;
    }

    public long hard() {
        return unassigned + studentClashes + instructorClashes + roomClashes + capacityViolations
                + featureViolations + unavailableSlots + precedenceViolations;
    }

    public long soft() {
        return lastPeriod + consecutive + singleClassDays;
    }

    public long weighted(long hardWeight) {
        return hardWeight * hard() + soft();
    }

    public CostBreakdown cost() {
        return new CostBreakdown(unassigned, studentClashes, instructorClashes, roomClashes,
                capacityViolations, featureViolations, unavailableSlots, precedenceViolations,
                lastPeriod, consecutive, singleClassDays);
    }

    /**
     * Places event {@code e} into ({@code slot}, {@code room}), moving it if already placed.
     * Passing {@link CostModel#UNASSIGNED} as slot removes the event.
     */
    public void assign(int e, int slot, int room) {
        int oldSlot = slotOf[e];
        if (oldSlot == slot && (slot == CostModel.UNASSIGNED || roomOf[e] == room)) {
            return;
        }
        int oldDay = oldSlot == CostModel.UNASSIGNED ? -1 : p.dayOf(oldSlot);
        int newDay = slot == CostModel.UNASSIGNED ? -1 : p.dayOf(slot);
        int[] students = p.studentsOf(e);

        for (int s : students) {
            subtractDays(s, oldDay, newDay);
        }
        int[] precedences = p.precedencesOf(e);
        for (int k : precedences) {
            precedenceViolations -= precedenceViolated(k);
        }
        if (oldSlot != CostModel.UNASSIGNED) {
            remove(e, oldSlot, roomOf[e]);
        } else {
            unassigned--;
        }
        if (slot != CostModel.UNASSIGNED) {
            add(e, slot, room);
        } else {
            unassigned++;
        }
        slotOf[e] = slot;
        roomOf[e] = slot == CostModel.UNASSIGNED ? CostModel.UNASSIGNED : room;
        for (int s : students) {
            addDays(s, oldDay, newDay);
        }
        for (int k : precedences) {
            precedenceViolations += precedenceViolated(k);
        }
    }

    private int precedenceViolated(int k) {
        int a = slotOf[p.precedenceBefore(k)];
        int b = slotOf[p.precedenceAfter(k)];
        return a != CostModel.UNASSIGNED && b != CostModel.UNASSIGNED && a >= b ? 1 : 0;
    }

    /** Convenience for {@code assign(e, UNASSIGNED, UNASSIGNED)}. */
    public void unassign(int e) {
        assign(e, CostModel.UNASSIGNED, CostModel.UNASSIGNED);
    }

    private void remove(int e, int slot, int room) {
        for (int s : p.studentsOf(e)) {
            if (--studentSlot[s * slots + slot] >= 1) {
                studentClashes--;
            }
        }
        int i = p.instructorOf(e);
        if (i >= 0 && --instructorSlot[i * slots + slot] >= 1) {
            instructorClashes--;
        }
        if (--roomSlot[room * slots + slot] >= 1) {
            roomClashes--;
        }
        if (p.capacityOf(room) < p.studentsOf(e).length) {
            capacityViolations--;
        }
        if (!p.hasRequiredFeatures(e, room)) {
            featureViolations--;
        }
        if (!p.isAvailable(e, slot)) {
            unavailableSlots--;
        }
    }

    private void add(int e, int slot, int room) {
        for (int s : p.studentsOf(e)) {
            if (studentSlot[s * slots + slot]++ >= 1) {
                studentClashes++;
            }
        }
        int i = p.instructorOf(e);
        if (i >= 0 && instructorSlot[i * slots + slot]++ >= 1) {
            instructorClashes++;
        }
        if (roomSlot[room * slots + slot]++ >= 1) {
            roomClashes++;
        }
        if (p.capacityOf(room) < p.studentsOf(e).length) {
            capacityViolations++;
        }
        if (!p.hasRequiredFeatures(e, room)) {
            featureViolations++;
        }
        if (!p.isAvailable(e, slot)) {
            unavailableSlots++;
        }
    }

    private void subtractDays(int s, int dayA, int dayB) {
        if (dayA >= 0) {
            applyDay(s, dayA, -1);
        }
        if (dayB >= 0 && dayB != dayA) {
            applyDay(s, dayB, -1);
        }
    }

    private void addDays(int s, int dayA, int dayB) {
        if (dayA >= 0) {
            applyDay(s, dayA, +1);
        }
        if (dayB >= 0 && dayB != dayA) {
            applyDay(s, dayB, +1);
        }
    }

    private void applyDay(int s, int day, int sign) {
        int base = s * slots + day * p.periodsPerDay();
        int periods = p.periodsPerDay();
        int classes = 0;
        int run = 0;
        long consec = 0;
        for (int q = 0; q < periods; q++) {
            if (studentSlot[base + q] > 0) {
                classes++;
                run++;
            } else {
                consec += Math.max(0, run - 2);
                run = 0;
            }
        }
        consec += Math.max(0, run - 2);
        consecutive += sign * consec;
        lastPeriod += (long) sign * studentSlot[base + periods - 1];
        if (classes == 1) {
            singleClassDays += sign;
        }
    }
}
