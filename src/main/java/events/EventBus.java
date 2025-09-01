// File location: src/main/java/events/EventBus.java
package events;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Event bus implementation for managing event publishing and subscription
 * Supports synchronous and asynchronous event processing with error handling
 */
public class EventBus {
    
    // Event bus configuration
    private final String busName;
    private final boolean asyncByDefault;
    private final ExecutorService executorService;
    private final int maxRetries;
    private final long retryDelayMs;
    
    // Event handling
    private final Map<String, List<EventHandler>> eventHandlers;
    private final Map<Class<? extends Event>, List<EventHandler>> typeHandlers;
    private final List<EventHandler> globalHandlers;
    private final Map<String, EventFilter> eventFilters;
    
    // Dead letter queue
    private final Queue<DeadLetterEvent> deadLetterQueue;
    private final int maxDeadLetterSize;
    
    // Statistics and monitoring
    private final AtomicLong eventsPublished;
    private final AtomicLong eventsProcessed;
    private final AtomicLong eventsFailed;
    private final Map<String, AtomicLong> eventTypeStats;
    private final Map<String, AtomicLong> handlerStats;
    
    // Lifecycle management
    private volatile boolean isRunning;
    private final Object lifecycleLock = new Object();
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Event handler wrapper with metadata
     */
    public static class EventHandler {
        private final String handlerId;
        private final String name;
        private final Consumer<Event> handler;
        private final Predicate<Event> filter;
        private final boolean isAsync;
        private final int priority;
        private final LocalDateTime registeredAt;
        private final AtomicLong processedCount;
        private final AtomicLong failedCount;
        
        public EventHandler(String handlerId, String name, Consumer<Event> handler,
                           Predicate<Event> filter, boolean isAsync, int priority) {
            this.handlerId = handlerId;
            this.name = name;
            this.handler = handler;
            this.filter = filter;
            this.isAsync = isAsync;
            this.priority = priority;
            this.registeredAt = LocalDateTime.now();
            this.processedCount = new AtomicLong(0);
            this.failedCount = new AtomicLong(0);
        }
        
        // Getters
        public String getHandlerId() { return handlerId; }
        public String getName() { return name; }
        public Consumer<Event> getHandler() { return handler; }
        public Predicate<Event> getFilter() { return filter; }
        public boolean isAsync() { return isAsync; }
        public int getPriority() { return priority; }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public long getProcessedCount() { return processedCount.get(); }
        public long getFailedCount() { return failedCount.get(); }
        
        void incrementProcessed() { processedCount.incrementAndGet(); }
        void incrementFailed() { failedCount.incrementAndGet(); }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            EventHandler that = (EventHandler) obj;
            return Objects.equals(handlerId, that.handlerId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(handlerId);
        }
    }
    
    /**
     * Event filter for advanced event routing
     */
    public static class EventFilter {
        private final String filterId;
        private final Predicate<Event> predicate;
        private final String description;
        
        public EventFilter(String filterId, Predicate<Event> predicate, String description) {
            this.filterId = filterId;
            this.predicate = predicate;
            this.description = description;
        }
        
        public String getFilterId() { return filterId; }
        public Predicate<Event> getPredicate() { return predicate; }
        public String getDescription() { return description; }
    }
    
    /**
     * Dead letter event wrapper
     */
    public static class DeadLetterEvent {
        private final Event originalEvent;
        private final String reason;
        private final Throwable cause;
        private final LocalDateTime deadLetterTime;
        private final int attemptCount;
        
        public DeadLetterEvent(Event originalEvent, String reason, Throwable cause, int attemptCount) {
            this.originalEvent = originalEvent;
            this.reason = reason;
            this.cause = cause;
            this.deadLetterTime = LocalDateTime.now();
            this.attemptCount = attemptCount;
        }
        
        // Getters
        public Event getOriginalEvent() { return originalEvent; }
        public String getReason() { return reason; }
        public Throwable getCause() { return cause; }
        public LocalDateTime getDeadLetterTime() { return deadLetterTime; }
        public int getAttemptCount() { return attemptCount; }
    }
    
    /**
     * Event bus statistics
     */
    public static class EventBusStats {
        private final String busName;
        private final LocalDateTime snapshotTime;
        private final long eventsPublished;
        private final long eventsProcessed;
        private final long eventsFailed;
        private final int registeredHandlers;
        private final int deadLetterCount;
        private final Map<String, Long> eventTypeStats;
        private final Map<String, Long> handlerStats;
        private final boolean isRunning;
        
        public EventBusStats(String busName, long eventsPublished, long eventsProcessed,
                           long eventsFailed, int registeredHandlers, int deadLetterCount,
                           Map<String, Long> eventTypeStats, Map<String, Long> handlerStats,
                           boolean isRunning) {
            this.busName = busName;
            this.snapshotTime = LocalDateTime.now();
            this.eventsPublished = eventsPublished;
            this.eventsProcessed = eventsProcessed;
            this.eventsFailed = eventsFailed;
            this.registeredHandlers = registeredHandlers;
            this.deadLetterCount = deadLetterCount;
            this.eventTypeStats = new HashMap<>(eventTypeStats);
            this.handlerStats = new HashMap<>(handlerStats);
            this.isRunning = isRunning;
        }
        
        // Getters
        public String getBusName() { return busName; }
        public LocalDateTime getSnapshotTime() { return snapshotTime; }
        public long getEventsPublished() { return eventsPublished; }
        public long getEventsProcessed() { return eventsProcessed; }
        public long getEventsFailed() { return eventsFailed; }
        public int getRegisteredHandlers() { return registeredHandlers; }
        public int getDeadLetterCount() { return deadLetterCount; }
        public Map<String, Long> getEventTypeStats() { return new HashMap<>(eventTypeStats); }
        public Map<String, Long> getHandlerStats() { return new HashMap<>(handlerStats); }
        public boolean isRunning() { return isRunning; }
        
        public double getSuccessRate() {
            long total = eventsPublished;
            return total > 0 ? ((double) (eventsProcessed - eventsFailed) / total) * 100.0 : 0.0;
        }
        
        public double getFailureRate() {
            long total = eventsPublished;
            return total > 0 ? ((double) eventsFailed / total) * 100.0 : 0.0;
        }
    }
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new EventBus with default configuration
     */
    public EventBus(String busName) {
        this(busName, true, 10, 3, 1000, 1000);
    }
    
    /**
     * Creates a new EventBus with custom configuration
     */
    public EventBus(String busName, boolean asyncByDefault, int threadPoolSize,
                   int maxRetries, long retryDelayMs, int maxDeadLetterSize) {
        this.busName = busName;
        this.asyncByDefault = asyncByDefault;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize,
            r -> {
                Thread t = new Thread(r, "EventBus-" + busName + "-Thread");
                t.setDaemon(true);
                return t;
            });
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.maxDeadLetterSize = maxDeadLetterSize;
        
        // Initialize data structures
        this.eventHandlers = new ConcurrentHashMap<>();
        this.typeHandlers = new ConcurrentHashMap<>();
        this.globalHandlers = new CopyOnWriteArrayList<>();
        this.eventFilters = new ConcurrentHashMap<>();
        this.deadLetterQueue = new ConcurrentLinkedQueue<>();
        
        // Initialize statistics
        this.eventsPublished = new AtomicLong(0);
        this.eventsProcessed = new AtomicLong(0);
        this.eventsFailed = new AtomicLong(0);
        this.eventTypeStats = new ConcurrentHashMap<>();
        this.handlerStats = new ConcurrentHashMap<>();
        
        this.isRunning = true;
    }
    
    // ==================== EVENT PUBLISHING ====================
    
    /**
     * Publishes an event to all registered handlers
     */
    public CompletableFuture<Void> publish(Event event) {
        if (!isRunning) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("EventBus is not running"));
        }
        
        if (event == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Event cannot be null"));
        }
        
        if (!event.isValid()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Event is not valid"));
        }
        
        eventsPublished.incrementAndGet();
        updateEventTypeStats(event.getEventType());
        
        return CompletableFuture.runAsync(() -> processEvent(event), executorService)
            .exceptionally(throwable -> {
                handleEventProcessingError(event, throwable, 0);
                return null;
            });
    }
    
    /**
     * Publishes an event synchronously
     */
    public void publishSync(Event event) {
        if (!isRunning) {
            throw new IllegalStateException("EventBus is not running");
        }
        
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        if (!event.isValid()) {
            throw new IllegalArgumentException("Event is not valid");
        }
        
        eventsPublished.incrementAndGet();
        updateEventTypeStats(event.getEventType());
        
        try {
            processEvent(event);
        } catch (Exception e) {
            handleEventProcessingError(event, e, 0);
            throw new RuntimeException("Failed to process event synchronously", e);
        }
    }
    
    /**
     * Publishes multiple events
     */
    public CompletableFuture<Void> publishAll(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        List<CompletableFuture<Void>> futures = events.stream()
            .map(this::publish)
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    // ==================== EVENT HANDLER REGISTRATION ====================
    
    /**
     * Registers an event handler for a specific event type
     */
    public String subscribe(String eventType, Consumer<Event> handler) {
        return subscribe(eventType, handler, null, asyncByDefault, 0, null);
    }
    
    /**
     * Registers an event handler with custom configuration
     */
    public String subscribe(String eventType, Consumer<Event> handler, String handlerName,
                          boolean isAsync, int priority, Predicate<Event> filter) {
        if (!isRunning) {
            throw new IllegalStateException("EventBus is not running");
        }
        
        String handlerId = UUID.randomUUID().toString();
        String name = handlerName != null ? handlerName : "Handler-" + handlerId.substring(0, 8);
        
        EventHandler eventHandler = new EventHandler(handlerId, name, handler, filter, isAsync, priority);
        
        eventHandlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(eventHandler);
        handlerStats.put(handlerId, new AtomicLong(0));
        
        return handlerId;
    }
    
    /**
     * Registers an event handler for a specific event class
     */
    public <T extends Event> String subscribe(Class<T> eventClass, Consumer<T> handler) {
        return subscribe(eventClass, handler, null, asyncByDefault, 0, null);
    }
    
    /**
     * Registers an event handler for a specific event class with configuration
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> String subscribe(Class<T> eventClass, Consumer<T> handler,
                                            String handlerName, boolean isAsync, int priority,
                                            Predicate<T> filter) {
        if (!isRunning) {
            throw new IllegalStateException("EventBus is not running");
        }
        
        String handlerId = UUID.randomUUID().toString();
        String name = handlerName != null ? handlerName : "Handler-" + handlerId.substring(0, 8);
        
        // Wrap the typed handler
        Consumer<Event> wrappedHandler = event -> {
            if (eventClass.isInstance(event)) {
                handler.accept((T) event);
            }
        };
        
        // Wrap the typed filter
        Predicate<Event> wrappedFilter = filter != null ? 
            event -> eventClass.isInstance(event) && filter.test((T) event) :
            eventClass::isInstance;
        
        EventHandler eventHandler = new EventHandler(handlerId, name, wrappedHandler, 
                                                    wrappedFilter, isAsync, priority);
        
        typeHandlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(eventHandler);
        handlerStats.put(handlerId, new AtomicLong(0));
        
        return handlerId;
    }
    
    /**
     * Registers a global event handler (receives all events)
     */
    public String subscribeGlobal(Consumer<Event> handler) {
        return subscribeGlobal(handler, null, asyncByDefault, 0, null);
    }
    
    /**
     * Registers a global event handler with configuration
     */
    public String subscribeGlobal(Consumer<Event> handler, String handlerName,
                                boolean isAsync, int priority, Predicate<Event> filter) {
        if (!isRunning) {
            throw new IllegalStateException("EventBus is not running");
        }
        
        String handlerId = UUID.randomUUID().toString();
        String name = handlerName != null ? handlerName : "GlobalHandler-" + handlerId.substring(0, 8);
        
        EventHandler eventHandler = new EventHandler(handlerId, name, handler, filter, isAsync, priority);
        globalHandlers.add(eventHandler);
        handlerStats.put(handlerId, new AtomicLong(0));
        
        return handlerId;
    }
    
    /**
     * Unregisters an event handler
     */
    public boolean unsubscribe(String handlerId) {
        if (handlerId == null) return false;
        
        // Remove from event type handlers
        for (List<EventHandler> handlers : eventHandlers.values()) {
            handlers.removeIf(handler -> handlerId.equals(handler.getHandlerId()));
        }
        
        // Remove from type handlers
        for (List<EventHandler> handlers : typeHandlers.values()) {
            handlers.removeIf(handler -> handlerId.equals(handler.getHandlerId()));
        }
        
        // Remove from global handlers
        globalHandlers.removeIf(handler -> handlerId.equals(handler.getHandlerId()));
        
        // Remove stats
        handlerStats.remove(handlerId);
        
        return true;
    }
    
    // ==================== EVENT FILTERS ====================
    
    /**
     * Adds a global event filter
     */
    public void addFilter(String filterId, Predicate<Event> filter, String description) {
        eventFilters.put(filterId, new EventFilter(filterId, filter, description));
    }
    
    /**
     * Removes an event filter
     */
    public boolean removeFilter(String filterId) {
        return eventFilters.remove(filterId) != null;
    }
    
    /**
     * Gets all registered filters
     */
    public List<EventFilter> getFilters() {
        return new ArrayList<>(eventFilters.values());
    }
    
    // ==================== EVENT PROCESSING ====================
    
    /**
     * Processes an event by sending it to all matching handlers
     */
    private void processEvent(Event event) {
        // Apply global filters
        for (EventFilter filter : eventFilters.values()) {
            if (!filter.getPredicate().test(event)) {
                return; // Event filtered out
            }
        }
        
        List<EventHandler> handlersToRun = new ArrayList<>();
        
        // Collect handlers by event type
        List<EventHandler> typeHandlers = eventHandlers.get(event.getEventType());
        if (typeHandlers != null) {
            handlersToRun.addAll(typeHandlers);
        }
        
        // Collect handlers by event class
        for (Map.Entry<Class<? extends Event>, List<EventHandler>> entry : this.typeHandlers.entrySet()) {
            if (entry.getKey().isInstance(event)) {
                handlersToRun.addAll(entry.getValue());
            }
        }
        
        // Add global handlers
        handlersToRun.addAll(globalHandlers);
        
        // Sort by priority (higher priority first)
        handlersToRun.sort((h1, h2) -> Integer.compare(h2.getPriority(), h1.getPriority()));
        
        // Execute handlers
        List<CompletableFuture<Void>> asyncTasks = new ArrayList<>();
        
        for (EventHandler handler : handlersToRun) {
            // Apply handler-specific filter
            if (handler.getFilter() != null && !handler.getFilter().test(event)) {
                continue;
            }
            
            if (handler.isAsync()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> executeHandler(handler, event), executorService);
                asyncTasks.add(future);
            } else {
                executeHandler(handler, event);
            }
        }
        
        // Wait for async tasks to complete
        if (!asyncTasks.isEmpty()) {
            CompletableFuture.allOf(asyncTasks.toArray(new CompletableFuture[0])).join();
        }
        
        eventsProcessed.incrementAndGet();
    }
    
    /**
     * Executes a single event handler
     */
    private void executeHandler(EventHandler handler, Event event) {
        try {
            handler.getHandler().accept(event);
            handler.incrementProcessed();
            
            AtomicLong handlerCount = handlerStats.get(handler.getHandlerId());
            if (handlerCount != null) {
                handlerCount.incrementAndGet();
            }
        } catch (Exception e) {
            handler.incrementFailed();
            eventsFailed.incrementAndGet();
            
            // Log error and potentially retry
            handleHandlerError(handler, event, e, 1);
        }
    }
    
    /**
     * Handles handler execution errors with retry logic
     */
    private void handleHandlerError(EventHandler handler, Event event, Exception error, int attemptCount) {
        if (attemptCount <= maxRetries) {
            // Schedule retry
            CompletableFuture.delayedExecutor(retryDelayMs, TimeUnit.MILLISECONDS, executorService)
                .execute(() -> {
                    try {
                        handler.getHandler().accept(event);
                        handler.incrementProcessed();
                    } catch (Exception retryError) {
                        handleHandlerError(handler, event, retryError, attemptCount + 1);
                    }
                });
        } else {
            // Send to dead letter queue
            addToDeadLetterQueue(event, "Handler execution failed after " + maxRetries + " retries", 
                               error, attemptCount);
        }
    }
    
    /**
     * Handles event processing errors
     */
    private void handleEventProcessingError(Event event, Throwable error, int attemptCount) {
        eventsFailed.incrementAndGet();
        addToDeadLetterQueue(event, "Event processing failed", error, attemptCount);
    }
    
    // ==================== DEAD LETTER QUEUE ====================
    
    /**
     * Adds an event to the dead letter queue
     */
    private void addToDeadLetterQueue(Event event, String reason, Throwable cause, int attemptCount) {
        DeadLetterEvent deadLetterEvent = new DeadLetterEvent(event, reason, cause, attemptCount);
        
        // Add to queue, removing oldest if at capacity
        if (deadLetterQueue.size() >= maxDeadLetterSize) {
            deadLetterQueue.poll();
        }
        deadLetterQueue.offer(deadLetterEvent);
    }
    
    /**
     * Gets all dead letter events
     */
    public List<DeadLetterEvent> getDeadLetterEvents() {
        return new ArrayList<>(deadLetterQueue);
    }
    
    /**
     * Clears the dead letter queue
     */
    public void clearDeadLetterQueue() {
        deadLetterQueue.clear();
    }
    
    /**
     * Reprocesses a dead letter event
     */
    public CompletableFuture<Void> reprocessDeadLetterEvent(DeadLetterEvent deadLetterEvent) {
        return publish(deadLetterEvent.getOriginalEvent());
    }
    
    // ==================== STATISTICS AND MONITORING ====================
    
    /**
     * Gets current event bus statistics
     */
    public EventBusStats getStats() {
        Map<String, Long> eventTypeStatsSnapshot = eventTypeStats.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get()
            ));
        
        Map<String, Long> handlerStatsSnapshot = handlerStats.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get()
            ));
        
        int totalHandlers = eventHandlers.values().stream().mapToInt(List::size).sum() +
                          typeHandlers.values().stream().mapToInt(List::size).sum() +
                          globalHandlers.size();
        
        return new EventBusStats(
            busName,
            eventsPublished.get(),
            eventsProcessed.get(),
            eventsFailed.get(),
            totalHandlers,
            deadLetterQueue.size(),
            eventTypeStatsSnapshot,
            handlerStatsSnapshot,
            isRunning
        );
    }
    
    /**
     * Gets all registered handlers
     */
    public List<EventHandler> getAllHandlers() {
        List<EventHandler> allHandlers = new ArrayList<>();
        
        eventHandlers.values().forEach(allHandlers::addAll);
        typeHandlers.values().forEach(allHandlers::addAll);
        allHandlers.addAll(globalHandlers);
        
        return allHandlers;
    }
    
    /**
     * Gets handlers for a specific event type
     */
    public List<EventHandler> getHandlers(String eventType) {
        List<EventHandler> handlers = eventHandlers.get(eventType);
        return handlers != null ? new ArrayList<>(handlers) : new ArrayList<>();
    }
    
    /**
     * Updates event type statistics
     */
    private void updateEventTypeStats(String eventType) {
        eventTypeStats.computeIfAbsent(eventType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    // ==================== LIFECYCLE MANAGEMENT ====================
    
    /**
     * Starts the event bus
     */
    public void start() {
        synchronized (lifecycleLock) {
            if (!isRunning) {
                isRunning = true;
            }
        }
    }
    
    /**
     * Stops the event bus
     */
    public void stop() {
        synchronized (lifecycleLock) {
            if (isRunning) {
                isRunning = false;
                
                // Shutdown executor service gracefully
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * Checks if the event bus is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Gets the bus name
     */
    public String getBusName() {
        return busName;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Waits for all pending events to be processed
     */
    public boolean waitForCompletion(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (eventsPublished.get() == eventsProcessed.get() + eventsFailed.get()) {
                return true;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Resets all statistics
     */
    public void resetStats() {
        eventsPublished.set(0);
        eventsProcessed.set(0);
        eventsFailed.set(0);
        eventTypeStats.clear();
        
        for (AtomicLong count : handlerStats.values()) {
            count.set(0);
        }
        
        clearDeadLetterQueue();
    }
    
    @Override
    public String toString() {
        return String.format("EventBus{name='%s', running=%s, handlers=%d, published=%d, processed=%d, failed=%d}",
                           busName, isRunning, getAllHandlers().size(), 
                           eventsPublished.get(), eventsProcessed.get(), eventsFailed.get());
    }
}