package scheduling.model;

import java.util.Set;

/**
 * A teaching room.
 *
 * @param id       stable identifier
 * @param capacity number of seats, must be positive
 * @param features features the room offers (e.g. a projector or a lab bench); defensively
 *                 copied, empty when the instance does not model room features
 */
public record Room(String id, int capacity, Set<String> features) {

    public Room {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Room id must be non-blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive: " + capacity);
        }
        features = Set.copyOf(features);
    }

    /** A room without features. */
    public Room(String id, int capacity) {
        this(id, capacity, Set.of());
    }
}
