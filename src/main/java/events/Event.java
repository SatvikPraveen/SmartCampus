// File location: src/main/java/events/Event.java
package events;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all events in the campus management system
 * Provides common event properties and functionality for the event-driven architecture
 */
public abstract class Event {
    
    // Event properties
    private final String eventId;
    private final String eventType;
    private final LocalDateTime timestamp;
    private final String sourceSystem;
    private final String correlationId;
    private final int version;
    private final Map<String, Object> metadata;
    private final Priority priority;
    private final String aggregateId;
    private final String aggregateType;
    private final Long aggregateVersion;
    
    // Event priority levels
    public enum Priority {
        LOW(1, "Low Priority"),
        NORMAL(2, "Normal Priority"),
        HIGH(3, "High Priority"),
        CRITICAL(4, "Critical Priority");
        
        private final int level;
        private final String description;
        
        Priority(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() { return level; }
        public String getDescription() { return description; }
    }
    
    // Event categories for classification
    public enum Category {
        DOMAIN("Domain Event", "Core business domain events"),
        INTEGRATION("Integration Event", "External system integration events"),
        SYSTEM("System Event", "Infrastructure and system events"),
        AUDIT("Audit Event", "Audit trail and compliance events"),
        NOTIFICATION("Notification Event", "User notification events"),
        WORKFLOW("Workflow Event", "Business process workflow events"),
        ANALYTICS("Analytics Event", "Data analytics and reporting events");
        
        private final String displayName;
        private final String description;
        
        Category(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new Event with basic properties
     */
    protected Event(String eventType) {
        this(eventType, Priority.NORMAL, null, null, null);
    }
    
    /**
     * Creates a new Event with priority
     */
    protected Event(String eventType, Priority priority) {
        this(eventType, priority, null, null, null);
    }
    
    /**
     * Creates a new Event with aggregate information
     */
    protected Event(String eventType, String aggregateId, String aggregateType, Long aggregateVersion) {
        this(eventType, Priority.NORMAL, aggregateId, aggregateType, aggregateVersion);
    }
    
    /**
     * Creates a new Event with full context
     */
    protected Event(String eventType, Priority priority, String aggregateId, 
                   String aggregateType, Long aggregateVersion) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.sourceSystem = getSystemIdentifier();
        this.correlationId = generateCorrelationId();
        this.version = 1;
        this.metadata = new HashMap<>();
        this.priority = priority != null ? priority : Priority.NORMAL;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.aggregateVersion = aggregateVersion;
        
        // Add default metadata
        initializeMetadata();
    }
    
    /**
     * Copy constructor for event replay/reconstruction
     */
    protected Event(String eventId, String eventType, LocalDateTime timestamp, 
                   String sourceSystem, String correlationId, int version,
                   Priority priority, String aggregateId, String aggregateType, 
                   Long aggregateVersion, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.sourceSystem = sourceSystem;
        this.correlationId = correlationId;
        this.version = version;
        this.priority = priority;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.aggregateVersion = aggregateVersion;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // ==================== ABSTRACT METHODS ====================
    
    /**
     * Gets the event category for classification
     */
    public abstract Category getCategory();
    
    /**
     * Gets the event payload/data
     */
    public abstract Object getPayload();
    
    /**
     * Validates the event data
     */
    public abstract boolean isValid();
    
    /**
     * Gets a human-readable description of the event
     */
    public abstract String getDescription();
    
    // ==================== GETTERS ====================
    
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSourceSystem() { return sourceSystem; }
    public String getCorrelationId() { return correlationId; }
    public int getVersion() { return version; }
    public Priority getPriority() { return priority; }
    public String getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateVersion() { return aggregateVersion; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Gets specific metadata value
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Gets metadata value as specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        return (value != null && type.isInstance(value)) ? (T) value : null;
    }
    
    // ==================== METADATA METHODS ====================
    
    /**
     * Adds metadata to the event
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
    }
    
    /**
     * Adds multiple metadata entries
     */
    public void addMetadata(Map<String, Object> additionalMetadata) {
        if (additionalMetadata != null) {
            metadata.putAll(additionalMetadata);
        }
    }
    
    /**
     * Removes metadata
     */
    public Object removeMetadata(String key) {
        return metadata.remove(key);
    }
    
    /**
     * Checks if metadata exists
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Clears all metadata
     */
    protected void clearMetadata() {
        metadata.clear();
        initializeMetadata();
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if this event is related to an aggregate
     */
    public boolean hasAggregate() {
        return aggregateId != null && aggregateType != null;
    }
    
    /**
     * Checks if this event has a specific aggregate type
     */
    public boolean isAggregateType(String type) {
        return Objects.equals(aggregateType, type);
    }
    
    /**
     * Checks if this event is for a specific aggregate instance
     */
    public boolean isForAggregate(String type, String id) {
        return Objects.equals(aggregateType, type) && Objects.equals(aggregateId, id);
    }
    
    /**
     * Gets the age of the event in milliseconds
     */
    public long getAgeInMillis() {
        return java.time.Duration.between(timestamp, LocalDateTime.now()).toMillis();
    }
    
    /**
     * Checks if the event is recent (within specified seconds)
     */
    public boolean isRecent(int seconds) {
        return getAgeInMillis() < (seconds * 1000L);
    }
    
    /**
     * Checks if the event is stale (older than specified seconds)
     */
    public boolean isStale(int seconds) {
        return getAgeInMillis() > (seconds * 1000L);
    }
    
    /**
     * Creates a new event with updated version
     */
    public Event withVersion(int newVersion) {
        return createCopy(eventId, eventType, timestamp, sourceSystem, correlationId, 
                         newVersion, priority, aggregateId, aggregateType, aggregateVersion, metadata);
    }
    
    /**
     * Creates a new event with updated correlation ID
     */
    public Event withCorrelationId(String newCorrelationId) {
        return createCopy(eventId, eventType, timestamp, sourceSystem, newCorrelationId, 
                         version, priority, aggregateId, aggregateType, aggregateVersion, metadata);
    }
    
    /**
     * Creates a new event with updated priority
     */
    public Event withPriority(Priority newPriority) {
        return createCopy(eventId, eventType, timestamp, sourceSystem, correlationId, 
                         version, newPriority, aggregateId, aggregateType, aggregateVersion, metadata);
    }
    
    // ==================== SERIALIZATION SUPPORT ====================
    
    /**
     * Converts event to a map for serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> eventMap = new HashMap<>();
        
        eventMap.put("eventId", eventId);
        eventMap.put("eventType", eventType);
        eventMap.put("timestamp", timestamp.toString());
        eventMap.put("sourceSystem", sourceSystem);
        eventMap.put("correlationId", correlationId);
        eventMap.put("version", version);
        eventMap.put("priority", priority.toString());
        eventMap.put("category", getCategory().toString());
        
        if (aggregateId != null) eventMap.put("aggregateId", aggregateId);
        if (aggregateType != null) eventMap.put("aggregateType", aggregateType);
        if (aggregateVersion != null) eventMap.put("aggregateVersion", aggregateVersion);
        
        eventMap.put("payload", getPayload());
        eventMap.put("metadata", metadata);
        eventMap.put("description", getDescription());
        eventMap.put("valid", isValid());
        
        return eventMap;
    }
    
    /**
     * Converts event to JSON-like structure for logging
     */
    public Map<String, Object> toLogEntry() {
        Map<String, Object> logEntry = new HashMap<>();
        
        logEntry.put("@timestamp", timestamp.toString());
        logEntry.put("event.id", eventId);
        logEntry.put("event.type", eventType);
        logEntry.put("event.category", getCategory().toString());
        logEntry.put("event.priority", priority.toString());
        logEntry.put("event.source", sourceSystem);
        logEntry.put("event.correlation_id", correlationId);
        logEntry.put("event.version", version);
        logEntry.put("event.description", getDescription());
        
        if (hasAggregate()) {
            logEntry.put("aggregate.type", aggregateType);
            logEntry.put("aggregate.id", aggregateId);
            logEntry.put("aggregate.version", aggregateVersion);
        }
        
        // Add payload as nested object
        Object payload = getPayload();
        if (payload != null) {
            logEntry.put("event.payload", payload);
        }
        
        // Add metadata
        if (!metadata.isEmpty()) {
            logEntry.put("event.metadata", metadata);
        }
        
        return logEntry;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Initializes default metadata
     */
    private void initializeMetadata() {
        metadata.put("createdBy", "system");
        metadata.put("environment", System.getProperty("application.environment", "unknown"));
        metadata.put("version", System.getProperty("application.version", "unknown"));
        metadata.put("hostname", getHostname());
    }
    
    /**
     * Gets system identifier
     */
    private String getSystemIdentifier() {
        return System.getProperty("application.name", "campus-management-system");
    }
    
    /**
     * Generates correlation ID
     */
    private String generateCorrelationId() {
        // Try to get correlation ID from thread context first
        String threadCorrelationId = getThreadCorrelationId();
        return threadCorrelationId != null ? threadCorrelationId : UUID.randomUUID().toString();
    }
    
    /**
     * Gets correlation ID from thread context (placeholder implementation)
     */
    private String getThreadCorrelationId() {
        // In a real application, this would extract correlation ID from MDC or similar
        return null;
    }
    
    /**
     * Gets hostname
     */
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Creates a copy of this event with modified properties
     */
    protected abstract Event createCopy(String eventId, String eventType, LocalDateTime timestamp,
                                       String sourceSystem, String correlationId, int version,
                                       Priority priority, String aggregateId, String aggregateType,
                                       Long aggregateVersion, Map<String, Object> metadata);
    
    // ==================== EQUALITY AND HASH CODE ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Event event = (Event) obj;
        return Objects.equals(eventId, event.eventId) &&
               Objects.equals(eventType, event.eventType) &&
               Objects.equals(timestamp, event.timestamp) &&
               Objects.equals(aggregateId, event.aggregateId) &&
               Objects.equals(aggregateVersion, event.aggregateVersion);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, timestamp, aggregateId, aggregateVersion);
    }
    
    // ==================== STRING REPRESENTATION ====================
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("{");
        sb.append("eventId='").append(eventId).append("'");
        sb.append(", eventType='").append(eventType).append("'");
        sb.append(", timestamp=").append(timestamp);
        sb.append(", priority=").append(priority);
        sb.append(", category=").append(getCategory());
        
        if (hasAggregate()) {
            sb.append(", aggregateType='").append(aggregateType).append("'");
            sb.append(", aggregateId='").append(aggregateId).append("'");
            sb.append(", aggregateVersion=").append(aggregateVersion);
        }
        
        sb.append(", correlationId='").append(correlationId).append("'");
        sb.append(", sourceSystem='").append(sourceSystem).append("'");
        sb.append(", valid=").append(isValid());
        sb.append(", description='").append(getDescription()).append("'");
        sb.append("}");
        
        return sb.toString();
    }
    
    // ==================== BUILDER SUPPORT ====================
    
    /**
     * Base builder class for events
     */
    public abstract static class Builder<T extends Event, B extends Builder<T, B>> {
        protected Priority priority = Priority.NORMAL;
        protected String aggregateId;
        protected String aggregateType;
        protected Long aggregateVersion;
        protected final Map<String, Object> metadata = new HashMap<>();
        protected String correlationId;
        
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
        
        public B priority(Priority priority) {
            this.priority = priority;
            return self();
        }
        
        public B aggregate(String type, String id, Long version) {
            this.aggregateType = type;
            this.aggregateId = id;
            this.aggregateVersion = version;
            return self();
        }
        
        public B aggregate(String type, String id) {
            return aggregate(type, id, null);
        }
        
        public B correlationId(String correlationId) {
            this.correlationId = correlationId;
            return self();
        }
        
        public B metadata(String key, Object value) {
            this.metadata.put(key, value);
            return self();
        }
        
        public B metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return self();
        }
        
        public abstract T build();
    }
}