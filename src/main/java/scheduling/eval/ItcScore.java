package scheduling.eval;

import java.util.Arrays;
import java.util.stream.IntStream;

import scheduling.model.TimetablingProblem;

/**
 * Score of a timetable under the ITC-2007 post-enrolment rules.
 *
 * <p>The competition does not accept timetables that violate a hard constraint. Instead, events
 * may be left <em>unplaced</em>, and a solution is ranked first by its <em>distance to
 * feasibility</em> (the total number of students attending unplaced events) and then by its soft
 * cost. This engine instead counts hard violations and always places every event, so its
 * {@link CostBreakdown#hard()} is not the competition's measure. {@link #of} bridges the two: it
 * unplaces events until no hard violation other than "unassigned" remains, greedily re-inserts
 * any unplaced event that fits without creating a violation, and scores the result.</p>
 *
 * <p>A timetable with {@code hard() == 0} is returned unchanged, so for feasible timetables the
 * distance to feasibility is 0 and the soft cost equals {@link CostBreakdown#soft()}.</p>
 *
 * @param distanceToFeasibility total enrolment of the unplaced events
 * @param soft                  soft cost of the repaired timetable (the competition's definition)
 * @param unplaced              number of unplaced events
 */
public record ItcScore(long distanceToFeasibility, long soft, int unplaced) {

    /** Repairs a copy of {@code state} with {@link #repair} and scores it; the argument is not modified. */
    public static ItcScore of(TimetableState state) {
        return score(repair(state));
    }

    /**
     * Scores a timetable whose only hard violations are unassigned events, e.g. the output of
     * {@link #repair}.
     *
     * @throws IllegalArgumentException if any other hard constraint is violated
     */
    public static ItcScore score(TimetableState repaired) {
        if (violations(repaired) != 0) {
            throw new IllegalArgumentException("Timetable violates hard constraints: " + repaired.cost());
        }
        TimetablingProblem p = repaired.problem();
        int[] slots = repaired.slots();
        CostBreakdown cost = CostModel.evaluate(p, slots, repaired.rooms());
        int unplaced = 0;
        long dtf = 0;
        for (int e = 0; e < slots.length; e++) {
            if (slots[e] == CostModel.UNASSIGNED) {
                unplaced++;
                dtf += p.studentsOf(e).length;
            }
        }
        return new ItcScore(dtf, cost.soft(), unplaced);
    }

    /**
     * Returns a copy of {@code state} in which the only hard violations are unassigned events.
     *
     * <p>Step 1 repeatedly unplaces the event whose removal eliminates the most violations per
     * attending student (ties: lower index), which keeps the distance to feasibility small. Step 2
     * visits the unplaced events, largest first, and puts each back at the (available slot,
     * room) pair with the lowest soft cost among those that create no hard violation, if any.</p>
     */
    public static TimetableState repair(TimetableState state) {
        TimetableState s = state.copy();
        TimetablingProblem p = s.problem();
        int n = p.eventCount();
        // Events left unassigned by the solver are unplaced events too: they count towards
        // distance to feasibility, not towards the violations removed here.
        while (violations(s) > 0) {
            long current = violations(s);
            int best = -1;
            double bestRatio = 0;
            for (int e = 0; e < n; e++) {
                int slot = s.slotOf(e);
                if (slot == CostModel.UNASSIGNED) {
                    continue;
                }
                int room = s.roomOf(e);
                s.unassign(e);
                long gain = current - violations(s);
                s.assign(e, slot, room);
                double ratio = gain / (double) Math.max(1, p.studentsOf(e).length);
                if (gain > 0 && ratio > bestRatio) {
                    bestRatio = ratio;
                    best = e;
                }
            }
            s.unassign(best);
        }
        Integer[] unplaced = IntStream.range(0, n).filter(e -> s.slotOf(e) == CostModel.UNASSIGNED)
                .boxed().toArray(Integer[]::new);
        Arrays.sort(unplaced, (a, b) -> Integer.compare(p.studentsOf(b).length, p.studentsOf(a).length));
        for (int e : unplaced) {
            long bestSoft = Long.MAX_VALUE;
            int bestSlot = CostModel.UNASSIGNED;
            int bestRoom = CostModel.UNASSIGNED;
            for (int slot : p.availableSlotsOf(e)) {
                for (int room : p.suitableRoomsOf(e)) {
                    if (s.roomOccupancy(room, slot) > 0) {
                        continue;
                    }
                    s.assign(e, slot, room);
                    if (violations(s) == 0 && s.soft() < bestSoft) {
                        bestSoft = s.soft();
                        bestSlot = slot;
                        bestRoom = room;
                    }
                    s.unassign(e);
                }
            }
            if (bestSlot != CostModel.UNASSIGNED) {
                s.assign(e, bestSlot, bestRoom);
            }
        }
        return s;
    }

    /** Hard violations other than unassigned events. */
    private static long violations(TimetableState s) {
        CostBreakdown c = s.cost();
        return c.hard() - c.unassigned();
    }
}
