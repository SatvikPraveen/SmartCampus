package scheduling.eval;

/**
 * Itemised cost of a (possibly partial) timetable.
 *
 * <p>Every component is a non-negative violation count. Hard components must all be zero for a
 * timetable to be <em>feasible</em>; soft components measure quality among feasible timetables.</p>
 *
 * @param unassigned         events without a slot
 * @param studentClashes     sum over (student, slot) of {@code max(0, events - 1)}
 * @param instructorClashes  sum over (instructor, slot) of {@code max(0, events - 1)}
 * @param roomClashes        sum over (room, slot) of {@code max(0, events - 1)}
 * @param capacityViolations assigned events whose room is smaller than their enrolment
 * @param featureViolations  assigned events whose room lacks a feature the event requires
 * @param unavailableSlots   assigned events placed in a slot that is not available to them
 * @param precedenceViolations precedence constraints whose events are both assigned but not in
 *                           strictly increasing slot order
 * @param lastPeriod         student-events held in the last period of a day
 * @param consecutive        for each student-day, sum over maximal runs of length L&gt;2 of (L-2)
 * @param singleClassDays    student-days with exactly one class
 */
public record CostBreakdown(
        long unassigned,
        long studentClashes,
        long instructorClashes,
        long roomClashes,
        long capacityViolations,
        long featureViolations,
        long unavailableSlots,
        long precedenceViolations,
        long lastPeriod,
        long consecutive,
        long singleClassDays) {

    /** Total hard-constraint violations. */
    public long hard() {
        return unassigned + studentClashes + instructorClashes + roomClashes + capacityViolations
                + featureViolations + unavailableSlots + precedenceViolations;
    }

    /** Total soft-constraint penalty. */
    public long soft() {
        return lastPeriod + consecutive + singleClassDays;
    }

    /** True when no hard constraint is violated. */
    public boolean feasible() {
        return hard() == 0;
    }

    /** Scalarised objective {@code hardWeight * hard + soft}. */
    public long weighted(long hardWeight) {
        return hardWeight * hard() + soft();
    }
}
