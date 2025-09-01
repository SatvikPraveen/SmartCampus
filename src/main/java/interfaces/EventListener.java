// File: src/main/java/interfaces/EventListener.java
package interfaces;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * EventListener interface defining event handling operations.
 * This interface provides a contract for handling various system events.
 * 
 * Key Java concepts demonstrated:
 * - Generic interface with event types
 * - Default methods for common functionality
 * - CompletableFuture for async operations
 * - Functional interface concepts
 * - Nested classes and enums
 * - Method overloading for different event types
 */
public interface EventListener<T> {
    
    /**
     * Event priority enumeration for ordering event processing.
     */
    enum EventPriority {
        HIGHEST(1, "Highest"),
        HIGH(2, "High"),
        NORMAL(3, "Normal"),
        LOW(4, "Low"),
        LOWEST(5, "Lowest");
        
        private final int level;
        private final String displayName;
        
        EventPriority(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
        
        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Event processing mode enumeration.
     */
    enum ProcessingMode {
        SYNCHRONOUS("Synchronous"),
        ASYNCHRONOUS("Asynchronous"),
        BATCH("Batch"),
        IMMEDIATE("Immediate");
        
        private final String displayName;
        
        ProcessingMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Handle an event synchronously.
     * 
     * @param event The event to handle
     * @throws EventHandlingException if event handling fails
     */
    void handleEvent(Event<T> event) throws EventHandlingException;
    
    /**
     * Handle an event asynchronously.
     * 
     * @param event The event to handle
     * @return CompletableFuture that completes when event is handled
     */
    CompletableFuture<Void> handleEventAsync(Event<T> event);
    
    /**
     * Check if this listener can handle a specific event type.
     * 
     * @param eventType The event type to check
     * @return true if this listener can handle the event type, false otherwise
     */
    boolean canHandle(String eventType);
    
    /**
     * Check if this listener can handle a specific event.
     * 
     * @param event The event to check
     * @return true if this listener can handle the event, false otherwise
     */
    boolean canHandle(Event<T> event);
    
    /**
     * Get the priority of this event listener.
     * 
     * @return The priority level
     */
    EventPriority getPriority();
    
    /**
     * Get the preferred processing mode for this listener.
     * 
     * @return The preferred processing mode
     */
    ProcessingMode getProcessingMode();
    
    /**
     * Get the supported event types for this listener.
     * 
     * @return Array of supported event type names
     */
    String[] getSupportedEventTypes();
    
    /**
     * Called before event handling begins.
     * 
     * @param event The event about to be handled
     * @throws EventHandlingException if pre-processing fails
     */
    default void beforeEventHandling(Event<T> event) throws EventHandlingException {
        // Default implementation does nothing
    }
    
    /**
     * Called after event handling completes successfully.
     * 
     * @param event The event that was handled
     * @param result Optional result from event handling
     */
    default void afterEventHandling(Event<T> event, Object result) {
        // Default implementation does nothing
    }
    
    /**
     * Called when event handling fails.
     * 
     * @param event The event that failed to be handled
     * @param exception The exception that occurred
     */
    default void onEventHandlingError(Event<T> event, Exception exception) {
        // Default implementation does nothing
    }
    
    /**
     * Called when event handling is cancelled.
     * 
     * @param event The event that was cancelled
     */
    default void onEventHandlingCancelled(Event<T> event) {
        // Default implementation does nothing
    }
    
    /**
     * Get the unique identifier for this listener.
     * 
     * @return Unique identifier string
     */
    default String getListenerId() {
        return this.getClass().getSimpleName() + "@" + System.identityHashCode(this);
    }
    
    /**
     * Check if this listener is enabled.
     * 
     * @return true if listener is enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Get timeout for event handling (in milliseconds).
     * 
     * @return Timeout in milliseconds, or -1 for no timeout
     */
    default long getTimeoutMillis() {
        return 30000; // 30 seconds default
    }
    
    /**
     * Handle event timeout.
     * 
     * @param event The event that timed out
     */
    default void onEventTimeout(Event<T> event) {
        throw new EventHandlingException("Event handling timed out for event: " + event.getEventType());
    }
    
    /**
     * Inner class representing an event.
     */
    class Event<T> {
        private final String eventId;
        private final String eventType;
        private final T payload;
        private final String source;
        private final LocalDateTime timestamp;
        private final Map<String, Object> metadata;
        private final EventPriority priority;
        private final String correlationId;
        private final String userId;
        private volatile boolean cancelled;
        private volatile boolean processed;
        
        public Event(String eventType, T payload, String source) {
            this.eventId = generateEventId();
            this.eventType = eventType;
            this.payload = payload;
            this.source = source;
            this.timestamp = LocalDateTime.now();
            this.metadata = Map.of();
            this.priority = EventPriority.NORMAL;
            this.correlationId = null;
            this.userId = null;
            this.cancelled = false;
            this.processed = false;
        }
        
        public Event(String eventType, T payload, String source, EventPriority priority) {
            this.eventId = generateEventId();
            this.eventType = eventType;
            this.payload = payload;
            this.source = source;
            this.timestamp = LocalDateTime.now();
            this.metadata = Map.of();
            this.priority = priority;
            this.correlationId = null;
            this.userId = null;
            this.cancelled = false;
            this.processed = false;
        }
        
        public Event(String eventType, T payload, String source, Map<String, Object> metadata,
                    EventPriority priority, String correlationId, String userId) {
            this.eventId = generateEventId();
            this.eventType = eventType;
            this.payload = payload;
            this.source = source;
            this.timestamp = LocalDateTime.now();
            this.metadata = metadata != null ? metadata : Map.of();
            this.priority = priority != null ? priority : EventPriority.NORMAL;
            this.correlationId = correlationId;
            this.userId = userId;
            this.cancelled = false;
            this.processed = false;
        }
        
        // Getters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public T getPayload() { return payload; }
        public String getSource() { return source; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
        public EventPriority getPriority() { return priority; }
        public String getCorrelationId() { return correlationId; }
        public String getUserId() { return userId; }
        public boolean isCancelled() { return cancelled; }
        public boolean isProcessed() { return processed; }
        
        /**
         * Cancel the event processing.
         */
        public void cancel() {
            this.cancelled = true;
        }
        
        /**
         * Mark the event as processed.
         */
        public void markProcessed() {
            this.processed = true;
        }
        
        /**
         * Get metadata value by key.
         * 
         * @param key The metadata key
         * @return The metadata value, or null if not found
         */
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
        
        /**
         * Get typed metadata value.
         * 
         * @param key The metadata key
         * @param type The expected type
         * @return The typed metadata value, or null if not found or wrong type
         */
        @SuppressWarnings("unchecked")
        public <V> V getMetadata(String key, Class<V> type) {
            Object value = metadata.get(key);
            if (value != null && type.isAssignableFrom(value.getClass())) {
                return (V) value;
            }
            return null;
        }
        
        /**
         * Check if event has specific metadata.
         * 
         * @param key The metadata key to check
         * @return true if metadata exists, false otherwise
         */
        public boolean hasMetadata(String key) {
            return metadata.containsKey(key);
        }
        
        /**
         * Get event age in milliseconds.
         * 
         * @return Age of the event in milliseconds
         */
        public long getAgeMillis() {
            return java.time.Duration.between(timestamp, LocalDateTime.now()).toMillis();
        }
        
        /**
         * Check if event is expired based on a timeout.
         * 
         * @param timeoutMillis Timeout in milliseconds
         * @return true if event is expired, false otherwise
         */
        public boolean isExpired(long timeoutMillis) {
            return getAgeMillis() > timeoutMillis;
        }
        
        /**
         * Create a copy of this event with different payload.
         * 
         * @param newPayload The new payload
         * @return New event with the same properties but different payload
         */
        public <U> Event<U> withPayload(U newPayload) {
            return new Event<>(eventType, newPayload, source, metadata, priority, correlationId, userId);
        }
        
        /**
         * Generate a unique event ID.
         * 
         * @return Unique event ID
         */
        private String generateEventId() {
            return "EVT_" + System.currentTimeMillis() + "_" + System.nanoTime() % 100000;
        }
        
        @Override
        public String toString() {
            return String.format("Event{id='%s', type='%s', source='%s', priority=%s, timestamp=%s, cancelled=%s, processed=%s}",
                eventId, eventType, source, priority, timestamp, cancelled, processed);
        }
    }
    
    /**
     * Custom exception for event handling errors.
     */
    class EventHandlingException extends Exception {
        private final String eventType;
        private final String eventId;
        private final String listenerId;
        
        public EventHandlingException(String message) {
            super(message);
            this.eventType = null;
            this.eventId = null;
            this.listenerId = null;
        }
        
        public EventHandlingException(String message, Throwable cause) {
            super(message, cause);
            this.eventType = null;
            this.eventId = null;
            this.listenerId = null;
        }
        
        public EventHandlingException(String eventType, String eventId, String listenerId, String message) {
            super(String.format("Event handling failed for event '%s' (ID: %s) in listener '%s': %s", 
                eventType, eventId, listenerId, message));
            this.eventType = eventType;
            this.eventId = eventId;
            this.listenerId = listenerId;
        }
        
        public EventHandlingException(String eventType, String eventId, String listenerId, String message, Throwable cause) {
            super(String.format("Event handling failed for event '%s' (ID: %s) in listener '%s': %s", 
                eventType, eventId, listenerId, message), cause);
            this.eventType = eventType;
            this.eventId = eventId;
            this.listenerId = listenerId;
        }
        
        public String getEventType() { return eventType; }
        public String getEventId() { return eventId; }
        public String getListenerId() { return listenerId; }
    }
}