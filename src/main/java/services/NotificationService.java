// File: src/main/java/services/NotificationService.java
package services;

import models.*;
import interfaces.EventListener;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.concurrent.*;

/**
 * NotificationService class providing comprehensive notification functionality.
 * This service manages notifications, alerts, and communication across the Smart Campus system.
 * 
 * Key Java concepts demonstrated:
 * - Event-driven architecture
 * - Observer pattern implementation
 * - Concurrent programming with ExecutorService
 * - CompletableFuture for async operations
 * - Scheduled tasks and timing
 * - Template pattern for notification formatting
 * - Strategy pattern for delivery methods
 * - Chain of responsibility for notification processing
 */
public class NotificationService implements EventListener<Object> {
    
    // Notification types
    public enum NotificationType {
        INFO("Information", "📢"),
        WARNING("Warning", "⚠️"),
        ERROR("Error", "❌"),
        SUCCESS("Success", "✅"),
        REMINDER("Reminder", "⏰"),
        ALERT("Alert", "🚨"),
        ACADEMIC("Academic", "🎓"),
        ADMINISTRATIVE("Administrative", "📋"),
        SYSTEM("System", "⚙️"),
        EMERGENCY("Emergency", "🚨");
        
        private final String displayName;
        private final String icon;
        
        NotificationType(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
    }
    
    // Delivery channels
    public enum DeliveryChannel {
        EMAIL("Email"),
        SMS("SMS"),
        PUSH("Push Notification"),
        IN_APP("In-App"),
        PORTAL("Portal Message"),
        DASHBOARD("Dashboard Alert");
        
        private final String displayName;
        
        DeliveryChannel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Notification priority
    public enum Priority {
        LOW(1, "Low"),
        NORMAL(2, "Normal"),
        HIGH(3, "High"),
        URGENT(4, "Urgent"),
        CRITICAL(5, "Critical");
        
        private final int level;
        private final String displayName;
        
        Priority(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
        
        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }
    }
    
    // Instance fields
    private final Map<String, Notification> notifications;
    private final Map<String, List<String>> userNotifications; // userId -> notificationIds
    private final Map<String, NotificationPreferences> userPreferences;
    private final Set<NotificationListener> listeners;
    private final Map<String, NotificationTemplate> templates;
    private final Queue<Notification> pendingNotifications;
    
    // Concurrent processing
    private final ExecutorService notificationExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final CompletableFuture<Void> processingTask;
    
    // Configuration
    private final int maxNotificationsPerUser = 1000;
    private final int maxRetryAttempts = 3;
    private final long retryDelaySeconds = 5;
    
    /**
     * Constructor initializing the notification service.
     */
    public NotificationService() {
        this.notifications = new ConcurrentHashMap<>();
        this.userNotifications = new ConcurrentHashMap<>();
        this.userPreferences = new ConcurrentHashMap<>();
        this.listeners = ConcurrentHashMap.newKeySet();
        this.templates = new ConcurrentHashMap<>();
        this.pendingNotifications = new ConcurrentLinkedQueue<>();
        
        // Initialize thread pools
        this.notificationExecutor = Executors.newFixedThreadPool(10);
        this.scheduledExecutor = Executors.newScheduledThreadPool(5);
        
        // Start async processing
        this.processingTask = startNotificationProcessing();
        
        // Initialize default templates and preferences
        initializeDefaultTemplates();
        initializeSystemNotifications();
    }
    
    // Core notification methods
    
    /**
     * Send a notification to a user.
     * 
     * @param userId The recipient user ID
     * @param type The notification type
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority
     * @return The created notification
     */
    public Notification sendNotification(String userId, NotificationType type, String title, 
                                       String message, Priority priority) {
        return sendNotification(userId, type, title, message, priority, Set.of(DeliveryChannel.IN_APP));
    }
    
    /**
     * Send a notification with specific delivery channels.
     * 
     * @param userId The recipient user ID
     * @param type The notification type
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority
     * @param channels The delivery channels to use
     * @return The created notification
     */
    public Notification sendNotification(String userId, NotificationType type, String title, 
                                       String message, Priority priority, Set<DeliveryChannel> channels) {
        if (!ValidationUtil.isValidString(userId) || !ValidationUtil.isValidString(title)) {
            throw new IllegalArgumentException("Invalid notification parameters");
        }
        
        Notification notification = createNotification(userId, type, title, message, priority, channels);
        
        // Store notification
        notifications.put(notification.getId(), notification);
        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification.getId());
        
        // Add to processing queue
        pendingNotifications.offer(notification);
        
        // Notify listeners
        notifyListeners(notification);
        
        return notification;
    }
    
    /**
     * Send notification using template.
     * 
     * @param userId The recipient user ID
     * @param templateId The template ID
     * @param parameters Template parameters
     * @param priority The notification priority
     * @return The created notification
     */
    public Notification sendTemplatedNotification(String userId, String templateId, 
                                                 Map<String, String> parameters, Priority priority) {
        NotificationTemplate template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        
        String title = processTemplate(template.getTitleTemplate(), parameters);
        String message = processTemplate(template.getMessageTemplate(), parameters);
        
        return sendNotification(userId, template.getType(), title, message, priority, template.getChannels());
    }
    
    /**
     * Send notification to multiple users.
     * 
     * @param userIds The recipient user IDs
     * @param type The notification type
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority
     * @return List of created notifications
     */
    public List<Notification> sendBulkNotification(List<String> userIds, NotificationType type, 
                                                  String title, String message, Priority priority) {
        return userIds.parallelStream()
                .filter(ValidationUtil::isValidString)
                .map(userId -> sendNotification(userId, type, title, message, priority))
                .collect(Collectors.toList());
    }
    
    /**
     * Send notification asynchronously.
     * 
     * @param userId The recipient user ID
     * @param type The notification type
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority
     * @return CompletableFuture with the notification
     */
    public CompletableFuture<Notification> sendNotificationAsync(String userId, NotificationType type, 
                                                               String title, String message, Priority priority) {
        return CompletableFuture.supplyAsync(() -> 
            sendNotification(userId, type, title, message, priority), notificationExecutor);
    }
    
    /**
     * Schedule a notification for future delivery.
     * 
     * @param userId The recipient user ID
     * @param type The notification type
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority
     * @param deliveryTime When to deliver the notification
     * @return The scheduled notification
     */
    public ScheduledNotification scheduleNotification(String userId, NotificationType type, String title, 
                                                    String message, Priority priority, LocalDateTime deliveryTime) {
        ScheduledNotification scheduledNotification = new ScheduledNotification(
            userId, type, title, message, priority, deliveryTime);
        
        long delaySeconds = java.time.Duration.between(LocalDateTime.now(), deliveryTime).getSeconds();
        
        ScheduledFuture<?> scheduledTask = scheduledExecutor.schedule(() -> {
            sendNotification(userId, type, title, message, priority);
        }, delaySeconds, TimeUnit.SECONDS);
        
        scheduledNotification.setScheduledTask(scheduledTask);
        return scheduledNotification;
    }
    
    // Notification retrieval and management
    
    /**
     * Get notifications for a user.
     * 
     * @param userId The user ID
     * @return List of notifications for the user
     */
    public List<Notification> getUserNotifications(String userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>()).stream()
                .map(notifications::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get unread notifications for a user.
     * 
     * @param userId The user ID
     * @return List of unread notifications
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return getUserNotifications(userId).stream()
                .filter(notification -> !notification.isRead())
                .collect(Collectors.toList());
    }
    
    /**
     * Get notifications by type.
     * 
     * @param userId The user ID
     * @param type The notification type
     * @return List of notifications of the specified type
     */
    public List<Notification> getNotificationsByType(String userId, NotificationType type) {
        return getUserNotifications(userId).stream()
                .filter(notification -> notification.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Get notifications by priority.
     * 
     * @param userId The user ID
     * @param priority The notification priority
     * @return List of notifications with the specified priority
     */
    public List<Notification> getNotificationsByPriority(String userId, Priority priority) {
        return getUserNotifications(userId).stream()
                .filter(notification -> notification.getPriority() == priority)
                .collect(Collectors.toList());
    }
    
    /**
     * Mark notification as read.
     * 
     * @param notificationId The notification ID
     * @return true if successfully marked as read
     */
    public boolean markAsRead(String notificationId) {
        Notification notification = notifications.get(notificationId);
        if (notification != null && !notification.isRead()) {
            notification.markAsRead();
            notifyListeners(new NotificationEvent("NOTIFICATION_READ", notification));
            return true;
        }
        return false;
    }
    
    /**
     * Mark all notifications as read for a user.
     * 
     * @param userId The user ID
     * @return Number of notifications marked as read
     */
    public int markAllAsRead(String userId) {
        return (int) getUnreadNotifications(userId).stream()
                .mapToLong(notification -> markAsRead(notification.getId()) ? 1 : 0)
                .sum();
    }
    
    /**
     * Delete notification.
     * 
     * @param notificationId The notification ID
     * @return true if successfully deleted
     */
    public boolean deleteNotification(String notificationId) {
        Notification notification = notifications.remove(notificationId);
        if (notification != null) {
            // Remove from user's notification list
            List<String> userNotifs = userNotifications.get(notification.getUserId());
            if (userNotifs != null) {
                userNotifs.remove(notificationId);
            }
            
            notifyListeners(new NotificationEvent("NOTIFICATION_DELETED", notification));
            return true;
        }
        return false;
    }
    
    // User preferences management
    
    /**
     * Set user notification preferences.
     * 
     * @param userId The user ID
     * @param preferences The notification preferences
     */
    public void setUserPreferences(String userId, NotificationPreferences preferences) {
        userPreferences.put(userId, preferences);
    }
    
    /**
     * Get user notification preferences.
     * 
     * @param userId The user ID
     * @return The user's notification preferences
     */
    public NotificationPreferences getUserPreferences(String userId) {
        return userPreferences.getOrDefault(userId, createDefaultPreferences());
    }
    
    /**
     * Update specific preference for user.
     * 
     * @param userId The user ID
     * @param type The notification type
     * @param channel The delivery channel
     * @param enabled Whether notifications of this type/channel are enabled
     */
    public void updatePreference(String userId, NotificationType type, DeliveryChannel channel, boolean enabled) {
        NotificationPreferences preferences = getUserPreferences(userId);
        preferences.setChannelEnabled(type, channel, enabled);
        userPreferences.put(userId, preferences);
    }
    
    // Template management
    
    /**
     * Create notification template.
     * 
     * @param templateId The template ID
     * @param template The notification template
     */
    public void createTemplate(String templateId, NotificationTemplate template) {
        templates.put(templateId, template);
    }
    
    /**
     * Get notification template.
     * 
     * @param templateId The template ID
     * @return The notification template
     */
    public NotificationTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }
    
    /**
     * Delete notification template.
     * 
     * @param templateId The template ID
     * @return true if successfully deleted
     */
    public boolean deleteTemplate(String templateId) {
        return templates.remove(templateId) != null;
    }
    
    // Event listener methods
    
    /**
     * Add notification listener.
     * 
     * @param listener The notification listener
     */
    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove notification listener.
     * 
     * @param listener The notification listener
     */
    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }
    
    // EventListener interface implementation
    
    @Override
    public void handleEvent(Event<Object> event) throws EventHandlingException {
        try {
            processSystemEvent(event);
        } catch (Exception e) {
            throw new EventHandlingException("Failed to process event", e);
        }
    }
    
    @Override
    public CompletableFuture<Void> handleEventAsync(Event<Object> event) {
        return CompletableFuture.runAsync(() -> {
            try {
                handleEvent(event);
            } catch (EventHandlingException e) {
                // Log error but don't fail the future
                System.err.println("Failed to handle event: " + e.getMessage());
            }
        }, notificationExecutor);
    }
    
    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("STUDENT_") || 
               eventType.startsWith("PROFESSOR_") || 
               eventType.startsWith("COURSE_") || 
               eventType.startsWith("GRADE_") ||
               eventType.startsWith("ENROLLMENT_") ||
               eventType.startsWith("SYSTEM_");
    }
    
    @Override
    public boolean canHandle(Event<Object> event) {
        return canHandle(event.getEventType());
    }
    
    @Override
    public EventPriority getPriority() {
        return EventPriority.NORMAL;
    }
    
    @Override
    public ProcessingMode getProcessingMode() {
        return ProcessingMode.ASYNCHRONOUS;
    }
    
    @Override
    public String[] getSupportedEventTypes() {
        return new String[]{
            "STUDENT_ENROLLED", "STUDENT_DROPPED", "GRADE_ASSIGNED", "GRADE_UPDATED",
            "COURSE_CREATED", "COURSE_CANCELLED", "ENROLLMENT_WAITLISTED", "SYSTEM_MAINTENANCE"
        };
    }
    
    // Statistics and monitoring
    
    /**
     * Get notification statistics for a user.
     * 
     * @param userId The user ID
     * @return Notification statistics
     */
    public NotificationStatistics getUserStatistics(String userId) {
        List<Notification> userNotifs = getUserNotifications(userId);
        
        long totalNotifications = userNotifs.size();
        long unreadCount = userNotifs.stream().filter(n -> !n.isRead()).count();
        long readCount = totalNotifications - unreadCount;
        
        Map<NotificationType, Long> byType = userNotifs.stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));
        
        Map<Priority, Long> byPriority = userNotifs.stream()
                .collect(Collectors.groupingBy(Notification::getPriority, Collectors.counting()));
        
        return new NotificationStatistics(userId, totalNotifications, readCount, unreadCount, byType, byPriority);
    }
    
    /**
     * Get system-wide notification statistics.
     * 
     * @return System notification statistics
     */
    public SystemNotificationStatistics getSystemStatistics() {
        long totalNotifications = notifications.size();
        long totalUsers = userNotifications.size();
        long pendingCount = pendingNotifications.size();
        
        Map<NotificationType, Long> byType = notifications.values().stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));
        
        Map<Priority, Long> byPriority = notifications.values().stream()
                .collect(Collectors.groupingBy(Notification::getPriority, Collectors.counting()));
        
        double averageNotificationsPerUser = totalUsers > 0 ? (double) totalNotifications / totalUsers : 0;
        
        return new SystemNotificationStatistics(totalNotifications, totalUsers, pendingCount, 
                                              averageNotificationsPerUser, byType, byPriority);
    }
    
    // Cleanup and maintenance
    
    /**
     * Clean up old notifications.
     * 
     * @param olderThan Delete notifications older than this date
     * @return Number of notifications deleted
     */
    public int cleanupOldNotifications(LocalDateTime olderThan) {
        List<String> toDelete = notifications.values().stream()
                .filter(notification -> notification.getCreatedAt().isBefore(olderThan))
                .map(Notification::getId)
                .collect(Collectors.toList());
        
        toDelete.forEach(this::deleteNotification);
        return toDelete.size();
    }
    
    /**
     * Shutdown the notification service.
     */
    public void shutdown() {
        try {
            notificationExecutor.shutdown();
            scheduledExecutor.shutdown();
            
            if (!notificationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                notificationExecutor.shutdownNow();
            }
            
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            
            processingTask.cancel(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Helper methods
    
    private Notification createNotification(String userId, NotificationType type, String title, 
                                          String message, Priority priority, Set<DeliveryChannel> channels) {
        String notificationId = generateNotificationId();
        
        return new Notification(notificationId, userId, type, title, message, priority, 
                              channels, LocalDateTime.now());
    }
    
    private String generateNotificationId() {
        return "NOTIF_" + System.currentTimeMillis() + "_" + System.nanoTime() % 10000;
    }
    
    private String processTemplate(String template, Map<String, String> parameters) {
        String processed = template;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            processed = processed.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return processed;
    }
    
    private void processSystemEvent(Event<Object> event) {
        String eventType = event.getEventType();
        Object payload = event.getPayload();
        
        switch (eventType) {
            case "STUDENT_ENROLLED":
                handleStudentEnrolledEvent(payload);
                break;
            case "GRADE_ASSIGNED":
                handleGradeAssignedEvent(payload);
                break;
            case "COURSE_CANCELLED":
                handleCourseCancelledEvent(payload);
                break;
            case "SYSTEM_MAINTENANCE":
                handleSystemMaintenanceEvent(payload);
                break;
            default:
                // Handle generic event
                handleGenericEvent(event);
        }
    }
    
    private void handleStudentEnrolledEvent(Object payload) {
        if (payload instanceof Enrollment) {
            Enrollment enrollment = (Enrollment) payload;
            sendTemplatedNotification(enrollment.getStudentId(), "ENROLLMENT_CONFIRMATION",
                    Map.of("courseId", enrollment.getCourseId(), "semester", enrollment.getSemester()),
                    Priority.NORMAL);
        }
    }
    
    private void handleGradeAssignedEvent(Object payload) {
        if (payload instanceof Grade) {
            Grade grade = (Grade) payload;
            sendTemplatedNotification(grade.getStudentId(), "GRADE_NOTIFICATION",
                    Map.of("assignment", grade.getAssignmentName(), "grade", grade.getLetterGrade()),
                    Priority.NORMAL);
        }
    }
    
    private void handleCourseCancelledEvent(Object payload) {
        if (payload instanceof Course) {
            Course course = (Course) payload;
            // Send to all enrolled students
            // This would require integration with enrollment service
            sendNotification("ALL_STUDENTS", NotificationType.WARNING, "Course Cancelled",
                    "Course " + course.getCourseName() + " has been cancelled.", Priority.HIGH);
        }
    }
    
    private void handleSystemMaintenanceEvent(Object payload) {
        sendNotification("ALL_USERS", NotificationType.INFO, "System Maintenance",
                "Scheduled system maintenance will occur tonight.", Priority.NORMAL);
    }
    
    private void handleGenericEvent(Event<Object> event) {
        // Log or process generic events
        System.out.println("Processed generic event: " + event.getEventType());
    }
    
    private CompletableFuture<Void> startNotificationProcessing() {
        return CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Notification notification = pendingNotifications.poll();
                    if (notification != null) {
                        processNotificationDelivery(notification);
                    } else {
                        Thread.sleep(100); // Brief pause when queue is empty
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error processing notification: " + e.getMessage());
                }
            }
        }, notificationExecutor);
    }
    
    private void processNotificationDelivery(Notification notification) {
        NotificationPreferences userPrefs = getUserPreferences(notification.getUserId());
        
        for (DeliveryChannel channel : notification.getChannels()) {
            if (userPrefs.isChannelEnabled(notification.getType(), channel)) {
                deliverNotification(notification, channel);
            }
        }
    }
    
    private void deliverNotification(Notification notification, DeliveryChannel channel) {
        // Simulate delivery to different channels
        switch (channel) {
            case EMAIL:
                deliverEmail(notification);
                break;
            case SMS:
                deliverSMS(notification);
                break;
            case PUSH:
                deliverPushNotification(notification);
                break;
            case IN_APP:
                deliverInApp(notification);
                break;
            case PORTAL:
                deliverPortalMessage(notification);
                break;
            case DASHBOARD:
                deliverDashboardAlert(notification);
                break;
        }
    }
    
    private void deliverEmail(Notification notification) {
        // Simulate email delivery
        System.out.println("📧 Email sent: " + notification.getTitle());
        notification.addDeliveryRecord(DeliveryChannel.EMAIL, LocalDateTime.now(), true, null);
    }
    
    private void deliverSMS(Notification notification) {
        // Simulate SMS delivery
        System.out.println("📱 SMS sent: " + notification.getTitle());
        notification.addDeliveryRecord(DeliveryChannel.SMS, LocalDateTime.now(), true, null);
    }
    
    private void deliverPushNotification(Notification notification) {
        // Simulate push notification delivery
        System.out.println("🔔 Push notification sent: " + notification.getTitle());
        notification.addDeliveryRecord(DeliveryChannel.PUSH, LocalDateTime.now(), true, null);
    }
    
    private void deliverInApp(Notification notification) {
        // In-app notifications are stored and displayed in the UI
        System.out.println("📱 In-app notification: " + notification.getTitle());
        notification.addDeliveryRecord(DeliveryChannel.IN_APP, LocalDateTime.now(), true, null);
    }
    
    private void deliverPortalMessage(Notification notification) {
        // Simulate portal message delivery
        System.out.println("🌐 Portal message: " + notification.getTitle());
        notification.addDeliveryRecord(DeliveryChannel.PORTAL, LocalDateTime.now(), true, null);
    }
    
    private void deliverDashboardAlert(Notification notification) {
        // Simulate dashboard alert
        System.out.println("📊 Dashboard alert: " + notification.getTitle());
        notification.addDeliveryRecord(DeliveryChannel.DASHBOARD, LocalDateTime.now(), true, null);
    }
    
    private void notifyListeners(Object event) {
        listeners.forEach(listener -> {
            try {
                if (event instanceof Notification) {
                    listener.onNotificationSent((Notification) event);
                } else if (event instanceof NotificationEvent) {
                    listener.onNotificationEvent((NotificationEvent) event);
                }
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        });
    }
    
    private NotificationPreferences createDefaultPreferences() {
        NotificationPreferences preferences = new NotificationPreferences();
        
        // Set default preferences for all notification types and channels
        for (NotificationType type : NotificationType.values()) {
            preferences.setChannelEnabled(type, DeliveryChannel.IN_APP, true);
            preferences.setChannelEnabled(type, DeliveryChannel.EMAIL, type == NotificationType.ACADEMIC || type == NotificationType.ALERT);
            preferences.setChannelEnabled(type, DeliveryChannel.SMS, type == NotificationType.EMERGENCY || type == NotificationType.CRITICAL);
        }
        
        return preferences;
    }
    
    private void initializeDefaultTemplates() {
        // Enrollment confirmation template
        createTemplate("ENROLLMENT_CONFIRMATION", new NotificationTemplate(
                "ENROLLMENT_CONFIRMATION",
                NotificationType.ACADEMIC,
                "Enrollment Confirmed",
                "You have been successfully enrolled in {{courseId}} for {{semester}}.",
                "Your enrollment in course {{courseId}} for {{semester}} has been confirmed. Welcome to the class!",
                Set.of(DeliveryChannel.EMAIL, DeliveryChannel.IN_APP)
        ));
        
        // Grade notification template
        createTemplate("GRADE_NOTIFICATION", new NotificationTemplate(
                "GRADE_NOTIFICATION",
                NotificationType.ACADEMIC,
                "Grade Posted",
                "Grade posted for {{assignment}}",
                "Your grade for {{assignment}} has been posted: {{grade}}. Check your student portal for details.",
                Set.of(DeliveryChannel.EMAIL, DeliveryChannel.IN_APP)
        ));
        
        // System maintenance template
        createTemplate("SYSTEM_MAINTENANCE", new NotificationTemplate(
                "SYSTEM_MAINTENANCE",
                NotificationType.SYSTEM,
                "System Maintenance Notice",
                "Scheduled maintenance",
                "The system will undergo scheduled maintenance. Please save your work and log out before the maintenance window.",
                Set.of(DeliveryChannel.EMAIL, DeliveryChannel.IN_APP, DeliveryChannel.PORTAL)
        ));
    }
    
    private void initializeSystemNotifications() {
        // Send welcome notification to system startup
        sendNotification("SYSTEM", NotificationType.SYSTEM, "System Started",
                "Smart Campus notification system is now online.", Priority.LOW);
    }
    
    // Inner classes and interfaces
    
    /**
     * Notification listener interface.
     */
    public interface NotificationListener {
        void onNotificationSent(Notification notification);
        void onNotificationEvent(NotificationEvent event);
    }
    
    /**
     * Notification event for system events.
     */
    public static class NotificationEvent {
        private final String eventType;
        private final Notification notification;
        private final LocalDateTime timestamp;
        
        public NotificationEvent(String eventType, Notification notification) {
            this.eventType = eventType;
            this.notification = notification;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getEventType() { return eventType; }
        public Notification getNotification() { return notification; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Main notification class.
     */
    public static class Notification {
        private final String id;
        private final String userId;
        private final NotificationType type;
        private final String title;
        private final String message;
        private final Priority priority;
        private final Set<DeliveryChannel> channels;
        private final LocalDateTime createdAt;
        private final List<DeliveryRecord> deliveryRecords;
        
        private boolean read = false;
        private LocalDateTime readAt;
        private Map<String, String> metadata;
        
        public Notification(String id, String userId, NotificationType type, String title, 
                          String message, Priority priority, Set<DeliveryChannel> channels, LocalDateTime createdAt) {
            this.id = id;
            this.userId = userId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.priority = priority;
            this.channels = new HashSet<>(channels);
            this.createdAt = createdAt;
            this.deliveryRecords = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        public void markAsRead() {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
        
        public void addDeliveryRecord(DeliveryChannel channel, LocalDateTime deliveredAt, 
                                    boolean successful, String errorMessage) {
            deliveryRecords.add(new DeliveryRecord(channel, deliveredAt, successful, errorMessage));
        }
        
        // Getters
        public String getId() { return id; }
        public String getUserId() { return userId; }
        public NotificationType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public Priority getPriority() { return priority; }
        public Set<DeliveryChannel> getChannels() { return channels; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public boolean isRead() { return read; }
        public LocalDateTime getReadAt() { return readAt; }
        public Map<String, String> getMetadata() { return metadata; }
        public List<DeliveryRecord> getDeliveryRecords() { return deliveryRecords; }
        
        @Override
        public String toString() {
            return String.format("Notification{id='%s', type=%s, title='%s', priority=%s, read=%s}",
                    id, type, title, priority, read);
        }
    }
    
    /**
     * Delivery record for tracking notification delivery.
     */
    public static class DeliveryRecord {
        private final DeliveryChannel channel;
        private final LocalDateTime deliveredAt;
        private final boolean successful;
        private final String errorMessage;
        
        public DeliveryRecord(DeliveryChannel channel, LocalDateTime deliveredAt, 
                            boolean successful, String errorMessage) {
            this.channel = channel;
            this.deliveredAt = deliveredAt;
            this.successful = successful;
            this.errorMessage = errorMessage;
        }
        
        public DeliveryChannel getChannel() { return channel; }
        public LocalDateTime getDeliveredAt() { return deliveredAt; }
        public boolean isSuccessful() { return successful; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * User notification preferences.
     */
    public static class NotificationPreferences {
        private final Map<NotificationType, Map<DeliveryChannel, Boolean>> preferences;
        private boolean globalEnabled = true;
        private Set<NotificationType> mutedTypes = new HashSet<>();
        
        public NotificationPreferences() {
            this.preferences = new HashMap<>();
            initializeDefaults();
        }
        
        private void initializeDefaults() {
            for (NotificationType type : NotificationType.values()) {
                preferences.put(type, new HashMap<>());
                for (DeliveryChannel channel : DeliveryChannel.values()) {
                    preferences.get(type).put(channel, true);
                }
            }
        }
        
        public void setChannelEnabled(NotificationType type, DeliveryChannel channel, boolean enabled) {
            preferences.computeIfAbsent(type, k -> new HashMap<>()).put(channel, enabled);
        }
        
        public boolean isChannelEnabled(NotificationType type, DeliveryChannel channel) {
            if (!globalEnabled || mutedTypes.contains(type)) {
                return false;
            }
            return preferences.getOrDefault(type, Map.of()).getOrDefault(channel, false);
        }
        
        public void setGlobalEnabled(boolean enabled) {
            this.globalEnabled = enabled;
        }
        
        public void muteType(NotificationType type) {
            mutedTypes.add(type);
        }
        
        public void unmuteType(NotificationType type) {
            mutedTypes.remove(type);
        }
        
        public boolean isGlobalEnabled() { return globalEnabled; }
        public Set<NotificationType> getMutedTypes() { return mutedTypes; }
    }
    
    /**
     * Notification template for templated messages.
     */
    public static class NotificationTemplate {
        private final String templateId;
        private final NotificationType type;
        private final String titleTemplate;
        private final String messageTemplate;
        private final String detailTemplate;
        private final Set<DeliveryChannel> channels;
        
        public NotificationTemplate(String templateId, NotificationType type, String titleTemplate, 
                                  String messageTemplate, String detailTemplate, Set<DeliveryChannel> channels) {
            this.templateId = templateId;
            this.type = type;
            this.titleTemplate = titleTemplate;
            this.messageTemplate = messageTemplate;
            this.detailTemplate = detailTemplate;
            this.channels = new HashSet<>(channels);
        }
        
        public String getTemplateId() { return templateId; }
        public NotificationType getType() { return type; }
        public String getTitleTemplate() { return titleTemplate; }
        public String getMessageTemplate() { return messageTemplate; }
        public String getDetailTemplate() { return detailTemplate; }
        public Set<DeliveryChannel> getChannels() { return channels; }
    }
    
    /**
     * Scheduled notification for future delivery.
     */
    public static class ScheduledNotification {
        private final String userId;
        private final NotificationType type;
        private final String title;
        private final String message;
        private final Priority priority;
        private final LocalDateTime deliveryTime;
        private ScheduledFuture<?> scheduledTask;
        
        public ScheduledNotification(String userId, NotificationType type, String title, 
                                   String message, Priority priority, LocalDateTime deliveryTime) {
            this.userId = userId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.priority = priority;
            this.deliveryTime = deliveryTime;
        }
        
        public void setScheduledTask(ScheduledFuture<?> scheduledTask) {
            this.scheduledTask = scheduledTask;
        }
        
        public boolean cancel() {
            return scheduledTask != null && scheduledTask.cancel(false);
        }
        
        public String getUserId() { return userId; }
        public NotificationType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public Priority getPriority() { return priority; }
        public LocalDateTime getDeliveryTime() { return deliveryTime; }
        public boolean isCancelled() { return scheduledTask != null && scheduledTask.isCancelled(); }
    }
    
    /**
     * Notification statistics for a user.
     */
    public static class NotificationStatistics {
        private final String userId;
        private final long totalNotifications;
        private final long readCount;
        private final long unreadCount;
        private final Map<NotificationType, Long> byType;
        private final Map<Priority, Long> byPriority;
        
        public NotificationStatistics(String userId, long totalNotifications, long readCount, long unreadCount,
                                    Map<NotificationType, Long> byType, Map<Priority, Long> byPriority) {
            this.userId = userId;
            this.totalNotifications = totalNotifications;
            this.readCount = readCount;
            this.unreadCount = unreadCount;
            this.byType = new HashMap<>(byType);
            this.byPriority = new HashMap<>(byPriority);
        }
        
        public String getUserId() { return userId; }
        public long getTotalNotifications() { return totalNotifications; }
        public long getReadCount() { return readCount; }
        public long getUnreadCount() { return unreadCount; }
        public Map<NotificationType, Long> getByType() { return byType; }
        public Map<Priority, Long> getByPriority() { return byPriority; }
        public double getReadPercentage() { return totalNotifications > 0 ? (readCount * 100.0) / totalNotifications : 0; }
    }
    
    /**
     * System-wide notification statistics.
     */
    public static class SystemNotificationStatistics {
        private final long totalNotifications;
        private final long totalUsers;
        private final long pendingNotifications;
        private final double averageNotificationsPerUser;
        private final Map<NotificationType, Long> byType;
        private final Map<Priority, Long> byPriority;
        
        public SystemNotificationStatistics(long totalNotifications, long totalUsers, long pendingNotifications,
                                          double averageNotificationsPerUser, Map<NotificationType, Long> byType,
                                          Map<Priority, Long> byPriority) {
            this.totalNotifications = totalNotifications;
            this.totalUsers = totalUsers;
            this.pendingNotifications = pendingNotifications;
            this.averageNotificationsPerUser = averageNotificationsPerUser;
            this.byType = new HashMap<>(byType);
            this.byPriority = new HashMap<>(byPriority);
        }
        
        public long getTotalNotifications() { return totalNotifications; }
        public long getTotalUsers() { return totalUsers; }
        public long getPendingNotifications() { return pendingNotifications; }
        public double getAverageNotificationsPerUser() { return averageNotificationsPerUser; }
        public Map<NotificationType, Long> getByType() { return byType; }
        public Map<Priority, Long> getByPriority() { return byPriority; }
    }
}