// File location: src/main/java/concurrent/AsyncNotificationSender.java

package concurrent;

import models.*;
import services.NotificationService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Handles asynchronous notification operations
 * Provides efficient notification delivery with retry mechanisms and rate limiting
 */
public class AsyncNotificationSender {
    
    private final NotificationService notificationService;
    private final ExecutorService notificationExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final BlockingQueue<NotificationTask> notificationQueue;
    private final AtomicInteger activeTasks;
    private final AtomicLong totalNotificationsSent;
    private final AtomicLong failedNotifications;
    private final RateLimiter rateLimiter;
    private volatile boolean isRunning;
    
    public AsyncNotificationSender(NotificationService notificationService, 
                                 int threadPoolSize, int maxNotificationsPerSecond) {
        this.notificationService = notificationService;
        this.notificationExecutor = Executors.newFixedThreadPool(threadPoolSize);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.notificationQueue = new LinkedBlockingQueue<>();
        this.activeTasks = new AtomicInteger(0);
        this.totalNotificationsSent = new AtomicLong(0);
        this.failedNotifications = new AtomicLong(0);
        this.rateLimiter = new RateLimiter(maxNotificationsPerSecond);
        this.isRunning = true;
        
        startNotificationProcessor();
    }
    
    /**
     * Send enrollment confirmation asynchronously
     */
    public CompletableFuture<NotificationResult> sendEnrollmentConfirmationAsync(
            Student student, Course course) {
        
        NotificationTask task = new NotificationTask(
            NotificationType.ENROLLMENT_CONFIRMATION,
            student.getEmail(),
            "Enrollment Confirmation",
            createEnrollmentMessage(student, course),
            Priority.NORMAL,
            0
        );
        
        return executeNotificationTask(task);
    }
    
    /**
     * Send grade notification asynchronously
     */
    public CompletableFuture<NotificationResult> sendGradeNotificationAsync(
            Student student, Course course, Grade grade) {
        
        NotificationTask task = new NotificationTask(
            NotificationType.GRADE_NOTIFICATION,
            student.getEmail(),
            "Grade Posted",
            createGradeMessage(student, course, grade),
            Priority.HIGH,
            0
        );
        
        return executeNotificationTask(task);
    }
    
    /**
     * Send bulk notifications to multiple recipients
     */
    public CompletableFuture<BulkNotificationResult> sendBulkNotificationsAsync(
            List<Student> students, String subject, String message, Priority priority) {
        
        List<CompletableFuture<NotificationResult>> notificationFutures = 
            students.stream()
                .map(student -> {
                    NotificationTask task = new NotificationTask(
                        NotificationType.BULK_NOTIFICATION,
                        student.getEmail(),
                        subject,
                        message,
                        priority,
                        0
                    );
                    return executeNotificationTask(task);
                })
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(notificationFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<NotificationResult> results = notificationFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                long successCount = results.stream()
                    .mapToLong(result -> result.isSuccess() ? 1 : 0)
                    .sum();
                
                return new BulkNotificationResult(
                    results.size(),
                    (int) successCount,
                    results.size() - (int) successCount,
                    results
                );
            });
    }
    
    /**
     * Schedule notification for future delivery
     */
    public ScheduledFuture<NotificationResult> scheduleNotification(
            NotificationTask task, long delay, TimeUnit unit) {
        
        return scheduledExecutor.schedule(() -> {
            try {
                return executeNotificationTask(task).get();
            } catch (Exception e) {
                return new NotificationResult(false, 
                    "Scheduled notification failed: " + e.getMessage(), 
                    task, new Date());
            }
        }, delay, unit);
    }
    
    /**
     * Schedule recurring notifications
     */
    public ScheduledFuture<?> scheduleRecurringNotification(
            NotificationTask task, long initialDelay, long period, TimeUnit unit) {
        
        return scheduledExecutor.scheduleAtFixedRate(() -> {
            executeNotificationTask(task).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    System.err.println("Recurring notification failed: " + 
                                     throwable.getMessage());
                }
            });
        }, initialDelay, period, unit);
    }
    
    /**
     * Send notification with retry mechanism
     */
    public CompletableFuture<NotificationResult> sendNotificationWithRetry(
            NotificationTask task, int maxRetries, long retryDelay, TimeUnit unit) {
        
        return CompletableFuture.supplyAsync(() -> {
            NotificationResult result = null;
            int attempts = 0;
            
            while (attempts <= maxRetries) {
                try {
                    result = executeNotificationTask(task).get();
                    if (result.isSuccess()) {
                        break;
                    }
                } catch (Exception e) {
                    result = new NotificationResult(false, 
                        "Notification attempt failed: " + e.getMessage(), 
                        task, new Date());
                }
                
                attempts++;
                if (attempts <= maxRetries) {
                    try {
                        unit.sleep(retryDelay * attempts); // Exponential backoff
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            return result;
        }, notificationExecutor);
    }
    
    /**
     * Execute notification task asynchronously
     */
    private CompletableFuture<NotificationResult> executeNotificationTask(
            NotificationTask task) {
        
        return CompletableFuture.supplyAsync(() -> {
            activeTasks.incrementAndGet();
            
            try {
                // Apply rate limiting
                rateLimiter.acquire();
                
                // Send notification
                boolean success = sendNotification(task);
                
                if (success) {
                    totalNotificationsSent.incrementAndGet();
                    return new NotificationResult(true, "Notification sent successfully", 
                                                task, new Date());
                } else {
                    failedNotifications.incrementAndGet();
                    return new NotificationResult(false, "Notification failed to send", 
                                                task, new Date());
                }
                
            } catch (Exception e) {
                failedNotifications.incrementAndGet();
                return new NotificationResult(false, 
                    "Notification error: " + e.getMessage(), task, new Date());
            } finally {
                activeTasks.decrementAndGet();
            }
        }, notificationExecutor);
    }
    
    /**
     * Queue notification for processing
     */
    public void queueNotification(NotificationTask task) {
        try {
            notificationQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to queue notification", e);
        }
    }
    
    /**
     * Start the notification processor thread
     */
    private void startNotificationProcessor() {
        Thread processor = new Thread(() -> {
            while (isRunning || !notificationQueue.isEmpty()) {
                try {
                    NotificationTask task = notificationQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        executeNotificationTask(task).whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                System.err.println("Queued notification failed: " + 
                                                 throwable.getMessage());
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        processor.setDaemon(true);
        processor.setName("NotificationProcessor");
        processor.start();
    }
    
    /**
     * Send notification with different delivery methods
     */
    private boolean sendNotification(NotificationTask task) {
        switch (task.getType()) {
            case EMAIL:
                return notificationService.sendEmail(
                    task.getRecipient(), task.getSubject(), task.getMessage());
            case SMS:
                return notificationService.sendSMS(
                    task.getRecipient(), task.getMessage());
            case PUSH:
                return notificationService.sendPushNotification(
                    task.getRecipient(), task.getSubject(), task.getMessage());
            case ENROLLMENT_CONFIRMATION:
                return notificationService.sendEnrollmentConfirmation(
                    getStudentByEmail(task.getRecipient()), 
                    getCourseFromMessage(task.getMessage()));
            case GRADE_NOTIFICATION:
                return notificationService.sendGradeNotification(
                    getStudentByEmail(task.getRecipient()),
                    getCourseFromMessage(task.getMessage()),
                    getGradeFromMessage(task.getMessage()));
            case BULK_NOTIFICATION:
                return notificationService.sendEmail(
                    task.getRecipient(), task.getSubject(), task.getMessage());
            default:
                return false;
        }
    }
    
    /**
     * Get notification statistics
     */
    public NotificationStatistics getStatistics() {
        return new NotificationStatistics(
            totalNotificationsSent.get(),
            failedNotifications.get(),
            activeTasks.get(),
            notificationQueue.size(),
            rateLimiter.getCurrentRate()
        );
    }
    
    /**
     * Create enrollment confirmation message
     */
    private String createEnrollmentMessage(Student student, Course course) {
        return String.format(
            "Dear %s,\n\nYou have been successfully enrolled in %s (%s).\n\n" +
            "Course Details:\n" +
            "- Professor: %s\n" +
            "- Credits: %d\n" +
            "- Semester: %s\n\n" +
            "Best regards,\nRegistrar's Office",
            student.getName(),
            course.getName(),
            course.getCourseCode(),
            course.getProfessor().getName(),
            course.getCredits(),
            course.getSemester()
        );
    }
    
    /**
     * Create grade notification message
     */
    private String createGradeMessage(Student student, Course course, Grade grade) {
        return String.format(
            "Dear %s,\n\nA new grade has been posted for %s (%s).\n\n" +
            "Grade: %s (%.1f)\n" +
            "Comments: %s\n\n" +
            "Best regards,\nAcademic Affairs",
            student.getName(),
            course.getName(),
            course.getCourseCode(),
            grade.getLetterGrade(),
            grade.getNumericGrade(),
            grade.getComments() != null ? grade.getComments() : "No comments"
        );
    }
    
    // Helper methods (these would need proper implementation)
    private Student getStudentByEmail(String email) {
        // Implementation would look up student by email
        return null;
    }
    
    private Course getCourseFromMessage(String message) {
        // Implementation would extract course from message
        return null;
    }
    
    private Grade getGradeFromMessage(String message) {
        // Implementation would extract grade from message
        return null;
    }
    
    /**
     * Shutdown notification sender
     */
    public void shutdown() {
        isRunning = false;
        
        notificationExecutor.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!notificationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                notificationExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            notificationExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes
    
    public static class NotificationTask {
        private final NotificationType type;
        private final String recipient;
        private final String subject;
        private final String message;
        private final Priority priority;
        private final int retryCount;
        private final Date createdAt;
        
        public NotificationTask(NotificationType type, String recipient, 
                              String subject, String message, Priority priority, 
                              int retryCount) {
            this.type = type;
            this.recipient = recipient;
            this.subject = subject;
            this.message = message;
            this.priority = priority;
            this.retryCount = retryCount;
            this.createdAt = new Date();
        }
        
        // Getters
        public NotificationType getType() { return type; }
        public String getRecipient() { return recipient; }
        public String getSubject() { return subject; }
        public String getMessage() { return message; }
        public Priority getPriority() { return priority; }
        public int getRetryCount() { return retryCount; }
        public Date getCreatedAt() { return createdAt; }
    }
    
    public static class NotificationResult {
        private final boolean success;
        private final String message;
        private final NotificationTask task;
        private final Date sentAt;
        
        public NotificationResult(boolean success, String message, 
                                NotificationTask task, Date sentAt) {
            this.success = success;
            this.message = message;
            this.task = task;
            this.sentAt = sentAt;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public NotificationTask getTask() { return task; }
        public Date getSentAt() { return sentAt; }
    }
    
    public static class BulkNotificationResult {
        private final int totalSent;
        private final int successCount;
        private final int failureCount;
        private final List<NotificationResult> results;
        
        public BulkNotificationResult(int totalSent, int successCount, 
                                    int failureCount, List<NotificationResult> results) {
            this.totalSent = totalSent;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.results = results;
        }
        
        // Getters
        public int getTotalSent() { return totalSent; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<NotificationResult> getResults() { return results; }
        public double getSuccessRate() { 
            return totalSent > 0 ? (double) successCount / totalSent * 100 : 0; 
        }
    }
    
    public static class NotificationStatistics {
        private final long totalSent;
        private final long totalFailed;
        private final int activeTasks;
        private final int queueSize;
        private final double currentRate;
        
        public NotificationStatistics(long totalSent, long totalFailed, 
                                    int activeTasks, int queueSize, double currentRate) {
            this.totalSent = totalSent;
            this.totalFailed = totalFailed;
            this.activeTasks = activeTasks;
            this.queueSize = queueSize;
            this.currentRate = currentRate;
        }
        
        // Getters
        public long getTotalSent() { return totalSent; }
        public long getTotalFailed() { return totalFailed; }
        public int getActiveTasks() { return activeTasks; }
        public int getQueueSize() { return queueSize; }
        public double getCurrentRate() { return currentRate; }
        public double getSuccessRate() { 
            long total = totalSent + totalFailed;
            return total > 0 ? (double) totalSent / total * 100 : 0; 
        }
    }
    
    public enum NotificationType {
        EMAIL, SMS, PUSH, ENROLLMENT_CONFIRMATION, GRADE_NOTIFICATION, BULK_NOTIFICATION
    }
    
    public enum Priority {
        LOW(1), NORMAL(2), HIGH(3), URGENT(4);
        
        private final int level;
        
        Priority(int level) {
            this.level = level;
        }
        
        public int getLevel() { return level; }
    }
    
    // Simple rate limiter implementation
    private static class RateLimiter {
        private final int maxRequestsPerSecond;
        private final Queue<Long> requestTimes;
        private final Object lock = new Object();
        
        public RateLimiter(int maxRequestsPerSecond) {
            this.maxRequestsPerSecond = maxRequestsPerSecond;
            this.requestTimes = new LinkedList<>();
        }
        
        public void acquire() throws InterruptedException {
            synchronized (lock) {
                long now = System.currentTimeMillis();
                
                // Remove old timestamps
                while (!requestTimes.isEmpty() && 
                       now - requestTimes.peek() > 1000) {
                    requestTimes.poll();
                }
                
                // Check if we need to wait
                if (requestTimes.size() >= maxRequestsPerSecond) {
                    long oldestTime = requestTimes.peek();
                    long waitTime = 1000 - (now - oldestTime);
                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                        now = System.currentTimeMillis();
                    }
                }
                
                requestTimes.offer(now);
            }
        }
        
        public double getCurrentRate() {
            synchronized (lock) {
                long now = System.currentTimeMillis();
                // Remove old timestamps
                while (!requestTimes.isEmpty() && 
                       now - requestTimes.peek() > 1000) {
                    requestTimes.poll();
                }
                return requestTimes.size();
            }
        }
    }
}