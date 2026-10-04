package scheduling.model;

/**
 * Ordering constraint: event {@code before} must be held in a strictly earlier time slot of the
 * week than event {@code after}. Slots are ordered day-major, so every slot of day 0 precedes
 * every slot of day 1.
 *
 * @param before id of the event that must come first
 * @param after  id of the event that must come later
 */
public record Precedence(String before, String after) {

    public Precedence {
        if (before == null || after == null) {
            throw new IllegalArgumentException("Precedence endpoints must be non-null");
        }
        if (before.equals(after)) {
            throw new IllegalArgumentException("An event cannot precede itself: " + before);
        }
    }
}
