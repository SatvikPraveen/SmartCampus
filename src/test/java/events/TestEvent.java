package events;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Minimal concrete {@link Event} used to exercise the abstract base class and the {@link EventBus}
 * without dragging in a domain payload.
 */
class TestEvent extends Event {

    private final String label;
    private final boolean valid;

    TestEvent(String eventType, String label) {
        this(eventType, label, true);
    }

    TestEvent(String eventType, String label, boolean valid) {
        super(eventType);
        this.label = label;
        this.valid = valid;
    }

    TestEvent(String eventType, Priority priority, String label) {
        super(eventType, priority);
        this.label = label;
        this.valid = true;
    }

    TestEvent(String eventType, String aggregateId, String aggregateType, Long aggregateVersion) {
        super(eventType, aggregateId, aggregateType, aggregateVersion);
        this.label = "aggregate";
        this.valid = true;
    }

    /** Reconstruction constructor (same shape the concrete events use for createCopy). */
    TestEvent(String eventId, String eventType, LocalDateTime timestamp, String sourceSystem,
              String correlationId, int version, Priority priority, String aggregateId,
              String aggregateType, Long aggregateVersion, Map<String, Object> metadata,
              String label, boolean valid) {
        super(eventId, eventType, timestamp, sourceSystem, correlationId, version, priority,
              aggregateId, aggregateType, aggregateVersion, metadata);
        this.label = label;
        this.valid = valid;
    }

    String getLabel() { return label; }

    void resetMetadata() { clearMetadata(); }

    @Override
    public Category getCategory() { return Category.SYSTEM; }

    @Override
    public Object getPayload() { return label; }

    @Override
    public boolean isValid() { return valid; }

    @Override
    public String getDescription() { return "Test event " + label; }

    @Override
    protected Event createCopy(String eventId, String eventType, LocalDateTime timestamp,
                               String sourceSystem, String correlationId, int version,
                               Priority priority, String aggregateId, String aggregateType,
                               Long aggregateVersion, Map<String, Object> metadata) {
        return new TestEvent(eventId, eventType, timestamp, sourceSystem, correlationId, version,
                priority, aggregateId, aggregateType, aggregateVersion, metadata, label, valid);
    }

    /** Subclass used to check that class-based subscriptions also match subtypes. */
    static class Special extends TestEvent {
        Special(String eventType, String label) {
            super(eventType, label);
        }
    }
}
