package scheduling.model;

import java.util.Set;

/**
 * A single meeting that must be placed into exactly one (time slot, room) pair, e.g. one
 * lecture of a course section.
 *
 * @param id               stable identifier
 * @param instructorId     identifier of the teaching instructor, or {@code null} when unknown
 * @param studentIds       identifiers of the enrolled students (defensively copied)
 * @param requiredFeatures room features the event needs; a room is only suitable if it offers all
 *                         of them (defensively copied, empty when features are not modelled)
 */
public record Event(String id, String instructorId, Set<String> studentIds, Set<String> requiredFeatures) {

    public Event {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Event id must be non-blank");
        }
        studentIds = Set.copyOf(studentIds);
        requiredFeatures = Set.copyOf(requiredFeatures);
    }

    /** An event without feature requirements. */
    public Event(String id, String instructorId, Set<String> studentIds) {
        this(id, instructorId, studentIds, Set.of());
    }

    /** Number of seats this event needs. */
    public int size() {
        return studentIds.size();
    }
}
