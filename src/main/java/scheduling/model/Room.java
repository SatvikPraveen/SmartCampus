package scheduling.model;

/**
 * A teaching room.
 *
 * @param id       stable identifier
 * @param capacity number of seats, must be positive
 */
public record Room(String id, int capacity) {

    public Room {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Room id must be non-blank");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive: " + capacity);
        }
    }
}
