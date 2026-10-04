package scheduling.eval;

import scheduling.model.TimetablingProblem;

/**
 * Reference (non-incremental) implementation of the cost model.
 *
 * <p>This class recomputes every component from scratch in a deliberately direct style. It is
 * the specification against which {@link TimetableState}'s incremental bookkeeping is tested and
 * the function used to score final solutions in experiments.</p>
 */
public final class CostModel {

    /** Marker for "no slot" / "no room". */
    public static final int UNASSIGNED = -1;

    private CostModel() {
    }

    /**
     * Evaluates a timetable.
     *
     * @param p     the problem
     * @param slots slot index per event, or {@link #UNASSIGNED}
     * @param rooms room index per event, or {@link #UNASSIGNED}; ignored for unassigned events
     */
    public static CostBreakdown evaluate(TimetablingProblem p, int[] slots, int[] rooms) {
        int n = p.eventCount();
        int t = p.slotCount();
        if (slots.length != n || rooms.length != n) {
            throw new IllegalArgumentException("Assignment arrays must have one entry per event");
        }
        int[][] studentSlot = new int[p.studentCount()][t];
        int[][] instructorSlot = new int[p.instructorCount()][t];
        int[][] roomSlot = new int[p.roomCount()][t];
        long unassigned = 0;
        long capacity = 0;
        for (int e = 0; e < n; e++) {
            if (slots[e] == UNASSIGNED) {
                unassigned++;
                continue;
            }
            for (int s : p.studentsOf(e)) {
                studentSlot[s][slots[e]]++;
            }
            if (p.instructorOf(e) >= 0) {
                instructorSlot[p.instructorOf(e)][slots[e]]++;
            }
            roomSlot[rooms[e]][slots[e]]++;
            if (p.capacityOf(rooms[e]) < p.studentsOf(e).length) {
                capacity++;
            }
        }
        long lastPeriod = 0;
        long consecutive = 0;
        long single = 0;
        for (int[] row : studentSlot) {
            for (int d = 0; d < p.days(); d++) {
                int classes = 0;
                int run = 0;
                for (int q = 0; q < p.periodsPerDay(); q++) {
                    int count = row[p.slot(d, q)];
                    if (count > 0) {
                        classes++;
                        run++;
                    } else {
                        consecutive += Math.max(0, run - 2);
                        run = 0;
                    }
                }
                consecutive += Math.max(0, run - 2);
                lastPeriod += row[p.slot(d, p.periodsPerDay() - 1)];
                if (classes == 1) {
                    single++;
                }
            }
        }
        return new CostBreakdown(unassigned, excess(studentSlot), excess(instructorSlot),
                excess(roomSlot), capacity, lastPeriod, consecutive, single);
    }

    private static long excess(int[][] counts) {
        long total = 0;
        for (int[] row : counts) {
            for (int c : row) {
                total += Math.max(0, c - 1);
            }
        }
        return total;
    }
}
