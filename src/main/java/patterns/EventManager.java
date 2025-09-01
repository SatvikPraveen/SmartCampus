// File location: src/main/java/patterns/EventManager.java

package patterns;

import models.*;
import events.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Observer pattern implementation for managing system events
 * Provides event publishing and subscription capabilities for the SmartCampus system
 */
public class EventManager {
    
    private static EventManager instance;
    private static final Object instanceLock = new Object();
    
    private final Map<Class<? extends Event>, Set<EventListener<?>>> listeners;
    private final ExecutorService eventExecutor;
    private final boolean asyncProcessing;
    private final Queue<EventExecution> eventQueue;
    private final Map<String, EventStatistics> eventStats;
    
    private EventManager(boolean asyncProcessing) {
        this.listeners = new ConcurrentHashMap<>();
        this.asyncProcessing = asyncProcessing;
        this.eventExecutor = asyncProcessing ? 
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "EventManager-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }) : null;
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.eventStats = new ConcurrentHashMap<>();
    }
    
    /**
     * Get singleton instance with default async processing
     */
    public static EventManager getInstance() {
        return getInstance(true);
    }
    
    /**
     * Get singleton instance with specified async processing mode
     */
    public static EventManager getInstance(boolean asyncProcessing) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new EventManager(asyncProcessing);
                }
            }
        }
        return instance;
    }
    
    /**
     * Subscribe to events of a specific type
     */
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }
    
    /**
     * Subscribe to events with a simple consumer function
     */
    public <T extends Event> void subscribe(Class<T> eventType, Consumer<T> consumer) {
        subscribe(eventType, new EventListener<T>() {
            @Override
            public void handleEvent(T event) {
                consumer.accept(event);
            }
            
            @Override
            public Class<T> getEventType() {
                return eventType;
            }
        });
    }
    
    /**
     * Subscribe to events with priority
     */
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener, EventPriority priority) {
        PriorityEventListener<T> priorityListener = new PriorityEventListener<>(listener, priority);
        listeners.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet()).add(priorityListener);
    }
    
    /**
     * Unsubscribe from events
     */
    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        Set<EventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }
    
    /**
     * Publish an event to all subscribers
     */
    public <T extends Event> void publish(T event) {
        if (event == null) {
            return;
        }
        
        updateEventStatistics(event);
        
        Set<EventListener<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }
        
        // Sort listeners by priority if any have priority
        List<EventListener<?>> sortedListeners = new ArrayList<>(eventListeners);
        sortedListeners.sort((l1, l2) -> {
            int priority1 = (l1 instanceof PriorityEventListener) ? 
                ((PriorityEventListener<?>) l1).getPriority().ordinal() : EventPriority.NORMAL.ordinal();
            int priority2 = (l2 instanceof PriorityEventListener) ? 
                ((PriorityEventListener<?>) l2).getPriority().ordinal() : EventPriority.NORMAL.ordinal();
            return Integer.compare(priority1, priority2);
        });
        
        if (asyncProcessing) {
            // Process events asynchronously
            for (EventListener<?> listener : sortedListeners) {
                eventExecutor.submit(() -> processEvent(event, listener));
            }
        } else {
            // Process events synchronously
            for (EventListener<?> listener : sortedListeners) {
                processEvent(event, listener);
            }
        }
    }
    
    /**
     * Publish event with delay
     */
    public <T extends Event> void publishDelayed(T event, long delay, TimeUnit unit) {
        if (eventExecutor != null) {
            CompletableFuture.delayedExecutor(delay, unit, eventExecutor)
                .execute(() -> publish(event));
        } else {
            // For synchronous processing, use a timer
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    publish(event);
                }
            }, unit.toMillis(delay));
        }
    }
    
    /**
     * Process a single event with a listener
     */
    @SuppressWarnings("unchecked")
    private <T extends Event> void processEvent(T event, EventListener<?> listener) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Cast is safe because we only add listeners of the correct type
            ((EventListener<T>) listener).handleEvent(event);
            
            long processingTime = System.currentTimeMillis() - startTime;
            recordEventExecution(event, listener, processingTime, null);
            
        } catch (Exception e) {
            recordEventExecution(event, listener, 0, e);
            handleEventProcessingError(event, listener, e);
        }
    }
    
    /**
     * Handle event processing errors
     */
    private void handleEventProcessingError(Event event, EventListener<?> listener, Exception error) {
        System.err.println("Error processing event " + event.getClass().getSimpleName() + 
                          " with listener " + listener.getClass().getSimpleName() + ": " + error.getMessage());
        
        // Publish error event
        ErrorEvent errorEvent = new ErrorEvent(event, listener, error, new Date());
        
        // Avoid infinite recursion by checking if this is already an error event
        if (!(event instanceof ErrorEvent)) {
            publish(errorEvent);
        }
    }
    
    /**
     * Record event execution for statistics
     */
    private void recordEventExecution(Event event, EventListener<?> listener, 
                                    long processingTime, Exception error) {
        EventExecution execution = new EventExecution(event, listener, processingTime, error, new Date());
        eventQueue.offer(execution);
        
        // Keep queue size manageable
        while (eventQueue.size() > 1000) {
            eventQueue.poll();
        }
    }
    
    /**
     * Update event statistics
     */
    private void updateEventStatistics(Event event) {
        String eventType = event.getClass().getSimpleName();
        eventStats.computeIfAbsent(eventType, k -> new EventStatistics())
                 .incrementCount();
    }
    
    /**
     * Get event statistics
     */
    public Map<String, EventStatistics> getEventStatistics() {
        return new HashMap<>(eventStats);
    }
    
    /**
     * Get recent event executions
     */
    public List<EventExecution> getRecentEventExecutions(int count) {
        return eventQueue.stream()
                .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
                .limit(count)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Clear all listeners
     */
    public void clearAllListeners() {
        listeners.clear();
    }
    
    /**
     * Clear listeners for specific event type
     */
    public void clearListeners(Class<? extends Event> eventType) {
        listeners.remove(eventType);
    }
    
    /**
     * Get number of listeners for event type
     */
    public int getListenerCount(Class<? extends Event> eventType) {
        Set<EventListener<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * Get all registered event types
     */
    public Set<Class<? extends Event>> getRegisteredEventTypes() {
        return new HashSet<>(listeners.keySet());
    }
    
    /**
     * Shutdown event manager
     */
    public void shutdown() {
        if (eventExecutor != null) {
            eventExecutor.shutdown();
            try {
                if (!eventExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    eventExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                eventExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        clearAllListeners();
    }
    
    // Convenience methods for common SmartCampus events
    
    /**
     * Subscribe to student enrollment events
     */
    public void onStudentEnrolled(Consumer<StudentEnrolledEvent> handler) {
        subscribe(StudentEnrolledEvent.class, handler);
    }
    
    /**
     * Subscribe to grade update events
     */
    public void onGradeUpdated(Consumer<GradeUpdatedEvent> handler) {
        subscribe(GradeUpdatedEvent.class, handler);
    }
    
    /**
     * Subscribe to course creation events
     */
    public void onCourseCreated(Consumer<CourseCreatedEvent> handler) {
        subscribe(CourseCreatedEvent.class, handler);
    }
    
    /**
     * Subscribe to user login events
     */
    public void onUserLogin(Consumer<UserLoginEvent> handler) {
        subscribe(UserLoginEvent.class, handler);
    }
    
    /**
     * Subscribe to system error events
     */
    public void onSystemError(Consumer<ErrorEvent> handler) {
        subscribe(ErrorEvent.class, handler);
    }
    
    // Helper classes and interfaces
    
    public enum EventPriority {
        HIGHEST, HIGH, NORMAL, LOW, LOWEST
    }
    
    private static class PriorityEventListener<T extends Event> implements EventListener<T> {
        private final EventListener<T> delegate;
        private final EventPriority priority;
        
        public PriorityEventListener(EventListener<T> delegate, EventPriority priority) {
            this.delegate = delegate;
            this.priority = priority;
        }
        
        @Override
        public void handleEvent(T event) {
            delegate.handleEvent(event);
        }
        
        @Override
        public Class<T> getEventType() {
            return delegate.getEventType();
        }
        
        public EventPriority getPriority() {
            return priority;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PriorityEventListener<?> that = (PriorityEventListener<?>) obj;
            return Objects.equals(delegate, that.delegate);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(delegate);
        }
    }
    
    public static class EventExecution {
        private final Event event;
        private final EventListener<?> listener;
        private final long processingTime;
        private final Exception error;
        private final Date timestamp;
        
        public EventExecution(Event event, EventListener<?> listener, long processingTime, 
                            Exception error, Date timestamp) {
            this.event = event;
            this.listener = listener;
            this.processingTime = processingTime;
            this.error = error;
            this.timestamp = timestamp;
        }
        
        // Getters
        public Event getEvent() { return event; }
        public EventListener<?> getListener() { return listener; }
        public long getProcessingTime() { return processingTime; }
        public Exception getError() { return error; }
        public Date getTimestamp() { return timestamp; }
        public boolean hasError() { return error != null; }
        
        @Override
        public String toString() {
            return String.format("EventExecution{event=%s, listener=%s, time=%dms, error=%s}",
                               event.getClass().getSimpleName(),
                               listener.getClass().getSimpleName(),
                               processingTime,
                               error != null ? error.getMessage() : "none");
        }
    }
    
    public static class EventStatistics {
        private long totalCount = 0;
        private long errorCount = 0;
        private Date firstOccurrence;
        private Date lastOccurrence;
        
        public synchronized void incrementCount() {
            totalCount++;
            Date now = new Date();
            if (firstOccurrence == null) {
                firstOccurrence = now;
            }
            lastOccurrence = now;
        }
        
        public synchronized void incrementErrorCount() {
            errorCount++;
        }
        
        // Getters
        public long getTotalCount() { return totalCount; }
        public long getErrorCount() { return errorCount; }
        public Date getFirstOccurrence() { return firstOccurrence; }
        public Date getLastOccurrence() { return lastOccurrence; }
        public double getErrorRate() { 
            return totalCount > 0 ? (double) errorCount / totalCount * 100 : 0; 
        }
        
        @Override
        public String toString() {
            return String.format("EventStats{total=%d, errors=%d, errorRate=%.2f%%}",
                               totalCount, errorCount, getErrorRate());
        }
    }
    
    // Specific event classes for SmartCampus system
    
    public static class StudentEnrolledEvent extends Event {
        private final Student student;
        private final Course course;
        private final Date enrollmentDate;
        
        public StudentEnrolledEvent(Student student, Course course, Date enrollmentDate) {
            super("StudentEnrolled", enrollmentDate);
            this.student = student;
            this.course = course;
            this.enrollmentDate = enrollmentDate;
        }
        
        public Student getStudent() { return student; }
        public Course getCourse() { return course; }
        public Date getEnrollmentDate() { return enrollmentDate; }
    }
    
    public static class GradeUpdatedEvent extends Event {
        private final Student student;
        private final Course course;
        private final Grade newGrade;
        private final Grade oldGrade;
        
        public GradeUpdatedEvent(Student student, Course course, Grade newGrade, Grade oldGrade) {
            super("GradeUpdated", new Date());
            this.student = student;
            this.course = course;
            this.newGrade = newGrade;
            this.oldGrade = oldGrade;
        }
        
        public Student getStudent() { return student; }
        public Course getCourse() { return course; }
        public Grade getNewGrade() { return newGrade; }
        public Grade getOldGrade() { return oldGrade; }
    }
    
    public static class CourseCreatedEvent extends Event {
        private final Course course;
        private final Professor professor;
        
        public CourseCreatedEvent(Course course, Professor professor) {
            super("CourseCreated", new Date());
            this.course = course;
            this.professor = professor;
        }
        
        public Course getCourse() { return course; }
        public Professor getProfessor() { return professor; }
    }
    
    public static class UserLoginEvent extends Event {
        private final User user;
        private final String ipAddress;
        private final boolean successful;
        
        public UserLoginEvent(User user, String ipAddress, boolean successful) {
            super("UserLogin", new Date());
            this.user = user;
            this.ipAddress = ipAddress;
            this.successful = successful;
        }
        
        public User getUser() { return user; }
        public String getIpAddress() { return ipAddress; }
        public boolean isSuccessful() { return successful; }
    }
    
    public static class ErrorEvent extends Event {
        private final Event originalEvent;
        private final EventListener<?> failedListener;
        private final Exception error;
        
        public ErrorEvent(Event originalEvent, EventListener<?> failedListener, Exception error, Date timestamp) {
            super("Error", timestamp);
            this.originalEvent = originalEvent;
            this.failedListener = failedListener;
            this.error = error;
        }
        
        public Event getOriginalEvent() { return originalEvent; }
        public EventListener<?> getFailedListener() { return failedListener; }
        public Exception getError() { return error; }
    }
    
    /**
     * Factory for creating pre-configured event managers
     */
    public static class EventManagerFactory {
        
        /**
         * Create event manager for testing with synchronous processing
         */
        public static EventManager createTestEventManager() {
            return new EventManager(false);
        }
        
        /**
         * Create event manager for production with async processing
         */
        public static EventManager createProductionEventManager() {
            return new EventManager(true);
        }
        
        /**
         * Create event manager with common SmartCampus listeners
         */
        public static EventManager createSmartCampusEventManager() {
            EventManager manager = new EventManager(true);
            
            // Add default listeners for common events
            manager.onStudentEnrolled(event -> 
                System.out.println("Student " + event.getStudent().getName() + 
                                 " enrolled in " + event.getCourse().getName()));
            
            manager.onGradeUpdated(event -> 
                System.out.println("Grade updated for " + event.getStudent().getName() + 
                                 " in " + event.getCourse().getName() + 
                                 ": " + event.getNewGrade().getLetterGrade()));
            
            manager.onCourseCreated(event -> 
                System.out.println("New course created: " + event.getCourse().getName() + 
                                 " by " + event.getProfessor().getName()));
            
            manager.onUserLogin(event -> 
                System.out.println("User login: " + event.getUser().getName() + 
                                 " from " + event.getIpAddress() + 
                                 " - " + (event.isSuccessful() ? "SUCCESS" : "FAILED")));
            
            manager.onSystemError(event -> 
                System.err.println("System error in event processing: " + 
                                 event.getError().getMessage()));
            
            return manager;
        }
    }
}