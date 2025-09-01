// File location: src/main/java/concurrent/EnrollmentProcessor.java

package concurrent;

import models.*;
import services.EnrollmentService;
import services.NotificationService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Handles multithreaded enrollment processing operations
 * Provides thread-safe enrollment management with concurrent access control
 */
public class EnrollmentProcessor {
    
    private final EnrollmentService enrollmentService;
    private final NotificationService notificationService;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final ReentrantLock enrollmentLock;
    private final AtomicInteger activeProcesses;
    private final BlockingQueue<EnrollmentRequest> enrollmentQueue;
    private volatile boolean isProcessing;
    
    public EnrollmentProcessor(EnrollmentService enrollmentService, 
                             NotificationService notificationService) {
        this.enrollmentService = enrollmentService;
        this.notificationService = notificationService;
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.enrollmentLock = new ReentrantLock();
        this.activeProcesses = new AtomicInteger(0);
        this.enrollmentQueue = new LinkedBlockingQueue<>();
        this.isProcessing = false;
        
        startQueueProcessor();
    }
    
    /**
     * Process single enrollment request asynchronously
     */
    public CompletableFuture<EnrollmentResult> processEnrollmentAsync(
            Student student, Course course) {
        
        return CompletableFuture.supplyAsync(() -> {
            activeProcesses.incrementAndGet();
            try {
                return processEnrollment(student, course);
            } finally {
                activeProcesses.decrementAndGet();
            }
        }, executorService);
    }
    
    /**
     * Process multiple enrollment requests concurrently
     */
    public CompletableFuture<List<EnrollmentResult>> processBatchEnrollments(
            List<EnrollmentRequest> requests) {
        
        List<CompletableFuture<EnrollmentResult>> futures = requests.stream()
                .map(request -> processEnrollmentAsync(request.getStudent(), 
                                                     request.getCourse()))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * Add enrollment request to processing queue
     */
    public void queueEnrollmentRequest(Student student, Course course, 
                                     EnrollmentPriority priority) {
        EnrollmentRequest request = new EnrollmentRequest(student, course, priority);
        try {
            enrollmentQueue.put(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to queue enrollment request", e);
        }
    }
    
    /**
     * Process enrollment with thread safety
     */
    private EnrollmentResult processEnrollment(Student student, Course course) {
        enrollmentLock.lock();
        try {
            // Check course capacity
            if (!hasAvailableCapacity(course)) {
                return new EnrollmentResult(false, "Course is full", 
                                          student, course, new Date());
            }
            
            // Check if student is already enrolled
            if (isStudentEnrolled(student, course)) {
                return new EnrollmentResult(false, "Student already enrolled", 
                                          student, course, new Date());
            }
            
            // Process enrollment
            boolean success = enrollmentService.enrollStudent(student, course);
            
            if (success) {
                // Send notification asynchronously
                CompletableFuture.runAsync(() -> 
                    notificationService.sendEnrollmentConfirmation(student, course),
                    executorService);
                
                return new EnrollmentResult(true, "Enrollment successful", 
                                          student, course, new Date());
            } else {
                return new EnrollmentResult(false, "Enrollment failed", 
                                          student, course, new Date());
            }
            
        } finally {
            enrollmentLock.unlock();
        }
    }
    
    /**
     * Start the queue processor thread
     */
    private void startQueueProcessor() {
        isProcessing = true;
        
        Thread queueProcessor = new Thread(() -> {
            while (isProcessing || !enrollmentQueue.isEmpty()) {
                try {
                    EnrollmentRequest request = enrollmentQueue.poll(1, TimeUnit.SECONDS);
                    if (request != null) {
                        processEnrollmentAsync(request.getStudent(), request.getCourse())
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        System.err.println("Enrollment processing failed: " + 
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
        
        queueProcessor.setDaemon(true);
        queueProcessor.setName("EnrollmentQueueProcessor");
        queueProcessor.start();
    }
    
    /**
     * Process enrollment requests with timeout
     */
    public CompletableFuture<EnrollmentResult> processEnrollmentWithTimeout(
            Student student, Course course, long timeout, TimeUnit unit) {
        
        CompletableFuture<EnrollmentResult> future = processEnrollmentAsync(student, course);
        
        return future.orTimeout(timeout, unit)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        return new EnrollmentResult(false, "Enrollment timed out", 
                                                  student, course, new Date());
                    }
                    return new EnrollmentResult(false, "Enrollment error: " + 
                                              throwable.getMessage(), 
                                              student, course, new Date());
                });
    }
    
    /**
     * Process enrollments with retry mechanism
     */
    public CompletableFuture<EnrollmentResult> processEnrollmentWithRetry(
            Student student, Course course, int maxRetries) {
        
        return CompletableFuture.supplyAsync(() -> {
            EnrollmentResult result = null;
            int attempts = 0;
            
            while (attempts <= maxRetries) {
                result = processEnrollment(student, course);
                if (result.isSuccess()) {
                    break;
                }
                
                attempts++;
                if (attempts <= maxRetries) {
                    try {
                        Thread.sleep(1000 * attempts); // Exponential backoff
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            return result;
        }, executorService);
    }
    
    /**
     * Schedule enrollment for future processing
     */
    public ScheduledFuture<EnrollmentResult> scheduleEnrollment(
            Student student, Course course, long delay, TimeUnit unit) {
        
        return scheduledExecutor.schedule(() -> 
            processEnrollment(student, course), delay, unit);
    }
    
    /**
     * Get current processing statistics
     */
    public ProcessingStats getProcessingStats() {
        return new ProcessingStats(
            activeProcesses.get(),
            enrollmentQueue.size(),
            isProcessing
        );
    }
    
    /**
     * Wait for all active processes to complete
     */
    public void waitForCompletion(long timeout, TimeUnit unit) 
            throws InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        
        while (activeProcesses.get() > 0 && System.currentTimeMillis() < endTime) {
            Thread.sleep(100);
        }
    }
    
    /**
     * Shutdown the processor gracefully
     */
    public void shutdown() {
        isProcessing = false;
        
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Helper methods
    
    private boolean hasAvailableCapacity(Course course) {
        return course.getEnrolledStudents() < course.getCapacity();
    }
    
    private boolean isStudentEnrolled(Student student, Course course) {
        return enrollmentService.isStudentEnrolled(student, course);
    }
    
    // Inner classes
    
    public static class EnrollmentRequest {
        private final Student student;
        private final Course course;
        private final EnrollmentPriority priority;
        private final Date requestTime;
        
        public EnrollmentRequest(Student student, Course course, 
                               EnrollmentPriority priority) {
            this.student = student;
            this.course = course;
            this.priority = priority;
            this.requestTime = new Date();
        }
        
        // Getters
        public Student getStudent() { return student; }
        public Course getCourse() { return course; }
        public EnrollmentPriority getPriority() { return priority; }
        public Date getRequestTime() { return requestTime; }
    }
    
    public static class EnrollmentResult {
        private final boolean success;
        private final String message;
        private final Student student;
        private final Course course;
        private final Date processedTime;
        
        public EnrollmentResult(boolean success, String message, 
                              Student student, Course course, Date processedTime) {
            this.success = success;
            this.message = message;
            this.student = student;
            this.course = course;
            this.processedTime = processedTime;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Student getStudent() { return student; }
        public Course getCourse() { return course; }
        public Date getProcessedTime() { return processedTime; }
        
        @Override
        public String toString() {
            return String.format("EnrollmentResult{success=%s, message='%s', student=%s, course=%s}",
                               success, message, student.getName(), course.getName());
        }
    }
    
    public static class ProcessingStats {
        private final int activeProcesses;
        private final int queueSize;
        private final boolean isProcessing;
        
        public ProcessingStats(int activeProcesses, int queueSize, boolean isProcessing) {
            this.activeProcesses = activeProcesses;
            this.queueSize = queueSize;
            this.isProcessing = isProcessing;
        }
        
        // Getters
        public int getActiveProcesses() { return activeProcesses; }
        public int getQueueSize() { return queueSize; }
        public boolean isProcessing() { return isProcessing; }
        
        @Override
        public String toString() {
            return String.format("ProcessingStats{active=%d, queued=%d, processing=%s}",
                               activeProcesses, queueSize, isProcessing);
        }
    }
    
    public enum EnrollmentPriority {
        LOW, NORMAL, HIGH, URGENT
    }
}