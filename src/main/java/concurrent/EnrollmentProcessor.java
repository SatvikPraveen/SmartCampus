// File location: src/main/java/concurrent/EnrollmentProcessor.java

package concurrent;

import models.*;
import services.EnrollmentService;
import services.NotificationService;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private Thread queueProcessor;
    private volatile long retryBackoffMillis = 1000;
    
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
        return processEnrollmentAsync(student, course, null);
    }
    
    private CompletableFuture<EnrollmentResult> processEnrollmentAsync(
            Student student, Course course, AtomicBoolean commitClaim) {
        
        return CompletableFuture.supplyAsync(() -> {
            activeProcesses.incrementAndGet();
            try {
                return processEnrollment(student, course, commitClaim);
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
        if (!isProcessing) {
            throw new IllegalStateException("EnrollmentProcessor has been shut down");
        }
        EnrollmentRequest request = new EnrollmentRequest(student, course, priority);
        try {
            enrollmentQueue.put(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to queue enrollment request", e);
        }
    }
    
    /**
     * Process enrollment with thread safety.
     *
     * @param commitClaim when non-null, the enrollment is only committed if this flag can be
     *                    claimed first; a caller that has already claimed it (for example after
     *                    reporting a timeout) thereby cancels the enrollment
     */
    private EnrollmentResult processEnrollment(Student student, Course course, AtomicBoolean commitClaim) {
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
            
            if (commitClaim != null && !commitClaim.compareAndSet(false, true)) {
                return new EnrollmentResult(false, "Enrollment cancelled after timeout",
                                          student, course, new Date());
            }
            
            // Process enrollment
            boolean success = enrollmentService.enrollStudent(student.getStudentId(), course.getCourseId(),
                                                            course.getSemester(), course.getYear());
            
            if (success) {
                // Send notification asynchronously
                CompletableFuture.runAsync(() -> 
                    notificationService.sendNotification(
                        student.getUserId(),
                        NotificationService.NotificationType.ACADEMIC,
                        "Enrollment Confirmed",
                        "You have been enrolled in " + course.getCourseCode() + " - " + course.getCourseName(),
                        NotificationService.Priority.NORMAL),
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
        
        Thread processor = new Thread(() -> {
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
        
        processor.setDaemon(true);
        processor.setName("EnrollmentQueueProcessor");
        processor.start();
        queueProcessor = processor;
    }
    
    /**
     * Process enrollment requests with timeout.
     *
     * <p>A reported timeout is final: the enrollment is cancelled and will not be committed
     * later. If the enrollment had already started committing when the timeout fired, the
     * real outcome is returned instead of a timeout.</p>
     */
    public CompletableFuture<EnrollmentResult> processEnrollmentWithTimeout(
            Student student, Course course, long timeout, TimeUnit unit) {
        
        AtomicBoolean commitClaim = new AtomicBoolean(false);
        CompletableFuture<EnrollmentResult> work = processEnrollmentAsync(student, course, commitClaim);
        
        return work.copy().orTimeout(timeout, unit)
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                            ? throwable.getCause() : throwable;
                    if (cause instanceof TimeoutException) {
                        if (commitClaim.compareAndSet(false, true)) {
                            return CompletableFuture.completedFuture(new EnrollmentResult(
                                    false, "Enrollment timed out", student, course, new Date()));
                        }
                        return work; // already committing: report the real outcome
                    }
                    return CompletableFuture.completedFuture(new EnrollmentResult(false,
                            "Enrollment error: " + cause.getMessage(), student, course, new Date()));
                })
                .thenCompose(f -> f);
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
                result = processEnrollment(student, course, null);
                if (result.isSuccess()) {
                    break;
                }
                
                attempts++;
                if (attempts <= maxRetries) {
                    try {
                        Thread.sleep(retryBackoffMillis * attempts); // Linear backoff
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
     * Set the base delay between retries of {@link #processEnrollmentWithRetry}
     * (attempt n waits n * base). Defaults to one second.
     */
    void setRetryBackoffMillis(long retryBackoffMillis) {
        this.retryBackoffMillis = retryBackoffMillis;
    }
    
    /**
     * Schedule enrollment for future processing
     */
    public ScheduledFuture<EnrollmentResult> scheduleEnrollment(
            Student student, Course course, long delay, TimeUnit unit) {
        
        return scheduledExecutor.schedule(() -> 
            processEnrollment(student, course, null), delay, unit);
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
        
        // Hand anything still queued to the executor before it stops accepting work, then stop
        // the processor thread (it may be waiting in poll) and wait for its last hand-off.
        List<EnrollmentRequest> pending = new ArrayList<>();
        enrollmentQueue.drainTo(pending);
        pending.forEach(r -> processEnrollmentAsync(r.getStudent(), r.getCourse()));
        queueProcessor.interrupt();
        try {
            queueProcessor.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
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
        // Enrollments made through this processor are recorded by the enrollment
        // service, not on the Course object, so both must be counted
        int enrolled = Math.max(course.getEnrolledStudentIds().size(),
                                enrollmentService.getCurrentEnrollmentCount(course.getCourseId()));
        return enrolled < course.getMaxEnrollment();
    }
    
    private boolean isStudentEnrolled(Student student, Course course) {
        return enrollmentService.isStudentEnrolled(student.getStudentId(), course.getCourseId());
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
                               success, message, student.getFullName(), course.getCourseName());
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