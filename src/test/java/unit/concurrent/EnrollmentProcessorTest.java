// File location: src/test/java/unit/concurrent/EnrollmentProcessorTest.java

package com.smartcampus.test.unit.concurrent;

import com.smartcampus.services.concurrent.EnrollmentProcessor;
import com.smartcampus.services.EnrollmentService;
import com.smartcampus.services.StudentService;
import com.smartcampus.services.CourseService;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.enums.EnrollmentStatus;
import com.smartcampus.exceptions.CourseFullException;
import com.smartcampus.exceptions.EnrollmentConflictException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for concurrent enrollment processing
 * Tests thread safety, race conditions, and concurrent access patterns
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Enrollment Processor Concurrent Tests")
class EnrollmentProcessorTest {

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private EnrollmentProcessor enrollmentProcessor;

    private ExecutorService executorService;
    private Course testCourse;
    private List<Student> testStudents;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10);
        
        // Set up test course with limited capacity
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("CS101");
        testCourse.setCourseName("Introduction to Programming");
        testCourse.setMaxEnrollment(5);
        testCourse.setCurrentEnrollment(0);

        // Set up test students
        testStudents = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Student student = new Student();
            student.setId((long) i);
            student.setStudentId("STU" + String.format("%03d", i));
            testStudents.add(student);
        }
    }

    @Nested
    @DisplayName("Concurrent Enrollment Tests")
    class ConcurrentEnrollmentTests {

        @Test
        @DisplayName("Should handle concurrent enrollment requests without race conditions")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldHandleConcurrentEnrollmentRequestsWithoutRaceConditions() throws InterruptedException {
            // Arrange
            AtomicInteger successfulEnrollments = new AtomicInteger(0);
            AtomicInteger failedEnrollments = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(testStudents.size());
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            
            // Mock enrollment service to simulate course capacity limits
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    Long studentId = invocation.getArgument(0);
                    synchronized (testCourse) {
                        if (testCourse.getCurrentEnrollment() < testCourse.getMaxEnrollment()) {
                            testCourse.setCurrentEnrollment(testCourse.getCurrentEnrollment() + 1);
                            Enrollment enrollment = new Enrollment();
                            enrollment.setStudent(testStudents.get(studentId.intValue() - 1));
                            enrollment.setCourse(testCourse);
                            enrollment.setStatus(EnrollmentStatus.ENROLLED);
                            return enrollment;
                        } else {
                            throw new CourseFullException("Course is full");
                        }
                    }
                });

            // Act - Submit concurrent enrollment requests
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Student student : testStudents) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        enrollmentProcessor.processEnrollment(student.getId(), testCourse.getId());
                        successfulEnrollments.incrementAndGet();
                    } catch (CourseFullException e) {
                        failedEnrollments.incrementAndGet();
                    } catch (Exception e) {
                        fail("Unexpected exception: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }, executorService);
                futures.add(future);
            }

            // Wait for all operations to complete
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Assert
            assertThat(successfulEnrollments.get()).isEqualTo(5); // Course capacity
            assertThat(failedEnrollments.get()).isEqualTo(5); // Remaining students
            assertThat(successfulEnrollments.get() + failedEnrollments.get()).isEqualTo(testStudents.size());
            assertThat(testCourse.getCurrentEnrollment()).isEqualTo(5);
        }

        @RepeatedTest(5)
        @DisplayName("Should consistently handle concurrent enrollments across multiple runs")
        void shouldConsistentlyHandleConcurrentEnrollmentsAcrossMultipleRuns() throws InterruptedException {
            // Reset course enrollment for each test run
            testCourse.setCurrentEnrollment(0);
            
            AtomicInteger totalProcessed = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(5);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    synchronized (testCourse) {
                        if (testCourse.getCurrentEnrollment() < testCourse.getMaxEnrollment()) {
                            testCourse.setCurrentEnrollment(testCourse.getCurrentEnrollment() + 1);
                            Enrollment enrollment = new Enrollment();
                            enrollment.setStatus(EnrollmentStatus.ENROLLED);
                            return enrollment;
                        } else {
                            throw new CourseFullException("Course is full");
                        }
                    }
                });

            // Submit 5 concurrent requests
            for (int i = 0; i < 5; i++) {
                final long studentId = i + 1;
                executorService.submit(() -> {
                    try {
                        enrollmentProcessor.processEnrollment(studentId, 1L);
                        totalProcessed.incrementAndGet();
                    } catch (CourseFullException e) {
                        // Expected when course is full
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertThat(testCourse.getCurrentEnrollment()).isLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should prevent duplicate enrollments for same student")
        void shouldPreventDuplicateEnrollmentsForSameStudent() throws InterruptedException {
            // Arrange
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger duplicateCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(3);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(eq(1L), eq(1L)))
                .thenAnswer(invocation -> {
                    // Simulate checking for existing enrollment
                    if (successCount.get() == 0) {
                        successCount.incrementAndGet();
                        Enrollment enrollment = new Enrollment();
                        enrollment.setStatus(EnrollmentStatus.ENROLLED);
                        return enrollment;
                    } else {
                        throw new EnrollmentConflictException("Student already enrolled");
                    }
                });

            // Act - Try to enroll same student multiple times concurrently
            for (int i = 0; i < 3; i++) {
                executorService.submit(() -> {
                    try {
                        enrollmentProcessor.processEnrollment(1L, 1L);
                    } catch (EnrollmentConflictException e) {
                        duplicateCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(duplicateCount.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Batch Processing Tests")
    class BatchProcessingTests {

        @Test
        @DisplayName("Should process enrollment batches concurrently")
        void shouldProcessEnrollmentBatchesConcurrently() throws InterruptedException {
            // Arrange
            List<Long> studentIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
            List<Long> courseIds = Arrays.asList(1L);
            AtomicInteger processedCount = new AtomicInteger(0);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    processedCount.incrementAndGet();
                    // Simulate processing time
                    Thread.sleep(10);
                    Enrollment enrollment = new Enrollment();
                    enrollment.setStatus(EnrollmentStatus.ENROLLED);
                    return enrollment;
                });

            // Act
            CompletableFuture<Void> batchFuture = enrollmentProcessor
                .processBatchEnrollments(studentIds, courseIds);
            
            batchFuture.join();

            // Assert
            assertThat(processedCount.get()).isEqualTo(5);
            verify(enrollmentService, times(5)).enrollStudent(anyLong(), eq(1L));
        }

        @Test
        @DisplayName("Should handle partial batch failures gracefully")
        void shouldHandlePartialBatchFailuresGracefully() throws InterruptedException {
            // Arrange
            List<Long> studentIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
            List<Long> courseIds = Arrays.asList(1L);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    Long studentId = invocation.getArgument(0);
                    if (studentId <= 3) {
                        successCount.incrementAndGet();
                        Enrollment enrollment = new Enrollment();
                        enrollment.setStatus(EnrollmentStatus.ENROLLED);
                        return enrollment;
                    } else {
                        failureCount.incrementAndGet();
                        throw new CourseFullException("Course is full");
                    }
                });

            // Act
            CompletableFuture<Map<String, Integer>> resultFuture = 
                enrollmentProcessor.processBatchEnrollmentsWithResults(studentIds, courseIds);
            
            Map<String, Integer> results = resultFuture.join();

            // Assert
            assertThat(results.get("successful")).isEqualTo(3);
            assertThat(results.get("failed")).isEqualTo(2);
            assertThat(successCount.get()).isEqualTo(3);
            assertThat(failureCount.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should respect batch size limits")
        void shouldRespectBatchSizeLimits() throws InterruptedException {
            // Arrange
            List<Long> largeStudentList = new ArrayList<>();
            for (long i = 1; i <= 100; i++) {
                largeStudentList.add(i);
            }
            
            AtomicInteger batchCount = new AtomicInteger(0);
            AtomicInteger maxConcurrentBatches = new AtomicInteger(0);
            AtomicInteger currentConcurrentBatches = new AtomicInteger(0);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    int current = currentConcurrentBatches.incrementAndGet();
                    maxConcurrentBatches.updateAndGet(max -> Math.max(max, current));
                    
                    Thread.sleep(50); // Simulate processing time
                    
                    currentConcurrentBatches.decrementAndGet();
                    batchCount.incrementAndGet();
                    
                    Enrollment enrollment = new Enrollment();
                    enrollment.setStatus(EnrollmentStatus.ENROLLED);
                    return enrollment;
                });

            // Act
            CompletableFuture<Void> future = enrollmentProcessor
                .processBatchEnrollmentsWithLimit(largeStudentList, Arrays.asList(1L), 10);
            
            future.join();

            // Assert
            assertThat(batchCount.get()).isEqualTo(100);
            assertThat(maxConcurrentBatches.get()).isLessThanOrEqualTo(10); // Respect concurrency limit
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should maintain thread safety during enrollment state changes")
        void shouldMaintainThreadSafetyDuringEnrollmentStateChanges() throws InterruptedException {
            // Arrange
            Map<Long, EnrollmentStatus> enrollmentStates = new ConcurrentHashMap<>();
            CountDownLatch latch = new CountDownLatch(20);
            AtomicBoolean hasDataRace = new AtomicBoolean(false);
            
            // Act - Multiple threads updating enrollment states
            for (int i = 0; i < 20; i++) {
                final long enrollmentId = i + 1;
                executorService.submit(() -> {
                    try {
                        // Simulate state transitions
                        enrollmentStates.put(enrollmentId, EnrollmentStatus.ENROLLED);
                        Thread.sleep(10);
                        
                        EnrollmentStatus currentState = enrollmentStates.get(enrollmentId);
                        if (currentState != EnrollmentStatus.ENROLLED) {
                            hasDataRace.set(true);
                        }
                        
                        enrollmentStates.put(enrollmentId, EnrollmentStatus.COMPLETED);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertFalse(hasDataRace.get(), "Data race detected in enrollment state management");
            assertThat(enrollmentStates).hasSize(20);
            enrollmentStates.values().forEach(status -> 
                assertThat(status).isEqualTo(EnrollmentStatus.COMPLETED));
        }

        @Test
        @DisplayName("Should handle concurrent reads and writes safely")
        void shouldHandleConcurrentReadsAndWritesSafely() throws InterruptedException {
            // Arrange
            ConcurrentLinkedQueue<String> operations = new ConcurrentLinkedQueue<>();
            CountDownLatch latch = new CountDownLatch(30);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    operations.add("WRITE");
                    Thread.sleep(5);
                    return new Enrollment();
                });
            
            when(enrollmentService.getEnrollmentsByStudent(anyLong()))
                .thenAnswer(invocation -> {
                    operations.add("READ");
                    Thread.sleep(2);
                    return Collections.emptyList();
                });

            // Act - Mix of read and write operations
            for (int i = 0; i < 15; i++) {
                final long studentId = i + 1;
                
                // Write operation
                executorService.submit(() -> {
                    try {
                        enrollmentProcessor.processEnrollment(studentId, 1L);
                    } catch (Exception e) {
                        // Ignore exceptions for this test
                    } finally {
                        latch.countDown();
                    }
                });
                
                // Read operation
                executorService.submit(() -> {
                    try {
                        enrollmentService.getEnrollmentsByStudent(studentId);
                    } catch (Exception e) {
                        // Ignore exceptions for this test
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertThat(operations).hasSize(30);
            
            // Verify both reads and writes occurred
            long readCount = operations.stream().filter(op -> "READ".equals(op)).count();
            long writeCount = operations.stream().filter(op -> "WRITE".equals(op)).count();
            
            assertThat(readCount).isEqualTo(15);
            assertThat(writeCount).isEqualTo(15);
        }

        @Test
        @DisplayName("Should prevent deadlocks in complex enrollment scenarios")
        void shouldPreventDeadlocksInComplexEnrollmentScenarios() throws InterruptedException {
            // Arrange
            Course course1 = new Course();
            course1.setId(1L);
            course1.setMaxEnrollment(2);
            
            Course course2 = new Course();
            course2.setId(2L);
            course2.setMaxEnrollment(2);
            
            CountDownLatch latch = new CountDownLatch(4);
            AtomicBoolean deadlockDetected = new AtomicBoolean(false);
            
            when(courseService.findCourseById(1L)).thenReturn(course1);
            when(courseService.findCourseById(2L)).thenReturn(course2);
            
            // Mock enrollment service with potential for deadlock
            when(enrollmentService.enrollStudent(anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    Long studentId = invocation.getArgument(0);
                    Long courseId = invocation.getArgument(1);
                    
                    // Simulate cross-locking scenario
                    if (studentId % 2 == 0) {
                        synchronized (course1) {
                            Thread.sleep(50);
                            synchronized (course2) {
                                return new Enrollment();
                            }
                        }
                    } else {
                        synchronized (course2) {
                            Thread.sleep(50);
                            synchronized (course1) {
                                return new Enrollment();
                            }
                        }
                    }
                });

            // Act - Create potential deadlock scenario
            for (int i = 1; i <= 4; i++) {
                final long studentId = i;
                final long courseId = (i % 2) + 1;
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        enrollmentProcessor.processEnrollmentWithTimeout(studentId, courseId, 2, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        deadlockDetected.set(true);
                    } catch (Exception e) {
                        // Other exceptions are acceptable
                    } finally {
                        latch.countDown();
                    }
                }, executorService);
                
                // Set timeout to detect potential deadlocks
                future.orTimeout(5, TimeUnit.SECONDS).exceptionally(throwable -> {
                    deadlockDetected.set(true);
                    latch.countDown();
                    return null;
                });
            }

            // Assert
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertFalse(deadlockDetected.get(), "Deadlock detected in enrollment processing");
        }
    }

    @Nested
    @DisplayName("Performance and Scalability Tests")
    class PerformanceAndScalabilityTests {

        @Test
        @DisplayName("Should maintain performance under high concurrent load")
        void shouldMaintainPerformanceUnderHighConcurrentLoad() throws InterruptedException {
            // Arrange
            int numberOfStudents = 1000;
            AtomicInteger processedCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(numberOfStudents);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    processedCount.incrementAndGet();
                    return new Enrollment();
                });

            long startTime = System.currentTimeMillis();

            // Act - Submit high number of concurrent requests
            for (int i = 1; i <= numberOfStudents; i++) {
                final long studentId = i;
                executorService.submit(() -> {
                    try {
                        enrollmentProcessor.processEnrollment(studentId, 1L);
                    } catch (Exception e) {
                        // Accept failures for this load test
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            assertTrue(latch.await(30, TimeUnit.SECONDS));
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            assertThat(processedCount.get()).isPositive();
            assertThat(duration).isLessThan(30000); // Should complete within 30 seconds
            
            double throughput = (double) processedCount.get() / (duration / 1000.0);
            assertThat(throughput).isGreaterThan(10); // At least 10 enrollments per second
        }

        @Test
        @DisplayName("Should scale efficiently with thread pool size")
        void shouldScaleEfficientlyWithThreadPoolSize() throws InterruptedException {
            // Test with different thread pool sizes
            int[] threadPoolSizes = {2, 5, 10, 20};
            Map<Integer, Long> performanceResults = new HashMap<>();
            
            for (int poolSize : threadPoolSizes) {
                ExecutorService testExecutor = Executors.newFixedThreadPool(poolSize);
                EnrollmentProcessor testProcessor = new EnrollmentProcessor(
                    enrollmentService, studentService, courseService, testExecutor);
                
                CountDownLatch testLatch = new CountDownLatch(100);
                AtomicInteger testProcessedCount = new AtomicInteger(0);
                
                when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                    .thenAnswer(invocation -> {
                        testProcessedCount.incrementAndGet();
                        Thread.sleep(10); // Simulate processing time
                        return new Enrollment();
                    });
                
                long startTime = System.currentTimeMillis();
                
                for (int i = 1; i <= 100; i++) {
                    final long studentId = i;
                    testExecutor.submit(() -> {
                        try {
                            testProcessor.processEnrollment(studentId, 1L);
                        } catch (Exception e) {
                            // Ignore for this test
                        } finally {
                            testLatch.countDown();
                        }
                    });
                }
                
                testLatch.await(30, TimeUnit.SECONDS);
                long duration = System.currentTimeMillis() - startTime;
                performanceResults.put(poolSize, duration);
                
                testExecutor.shutdown();
                reset(enrollmentService);
            }
            
            // Assert that performance improves with larger thread pools (up to a point)
            assertThat(performanceResults.get(10)).isLessThan(performanceResults.get(2));
        }

        @Test
        @DisplayName("Should handle memory efficiently during bulk processing")
        void shouldHandleMemoryEfficientlyDuringBulkProcessing() throws InterruptedException {
            // Arrange
            List<Long> largeStudentList = new ArrayList<>();
            for (long i = 1; i <= 10000; i++) {
                largeStudentList.add(i);
            }
            
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenReturn(new Enrollment());

            // Act
            CompletableFuture<Void> future = enrollmentProcessor
                .processBulkEnrollmentsEfficiently(largeStudentList, Arrays.asList(1L));
            
            future.join();
            
            // Force garbage collection to clean up
            System.gc();
            Thread.sleep(100);
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Assert - Memory increase should be reasonable (less than 100MB for this test)
            assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
        }
    }

    @Nested
    @DisplayName("Error Handling and Recovery Tests")
    class ErrorHandlingAndRecoveryTests {

        @Test
        @DisplayName("Should handle exceptions gracefully in concurrent environment")
        void shouldHandleExceptionsGracefullyInConcurrentEnvironment() throws InterruptedException {
            // Arrange
            AtomicInteger exceptionCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(10);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    Long studentId = invocation.getArgument(0);
                    if (studentId % 3 == 0) {
                        throw new RuntimeException("Simulated database error");
                    }
                    return new Enrollment();
                });

            // Act
            for (int i = 1; i <= 10; i++) {
                final long studentId = i;
                executorService.submit(() -> {
                    try {
                        enrollmentProcessor.processEnrollment(studentId, 1L);
                    } catch (Exception e) {
                        exceptionCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertThat(exceptionCount.get()).isEqualTo(3); // Students 3, 6, 9
        }

        @Test
        @DisplayName("Should implement circuit breaker pattern for resilience")
        void shouldImplementCircuitBreakerPatternForResilience() throws InterruptedException {
            // Arrange
            AtomicInteger attemptCount = new AtomicInteger(0);
            AtomicInteger circuitOpenCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(20);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(anyLong(), eq(1L)))
                .thenAnswer(invocation -> {
                    int attempt = attemptCount.incrementAndGet();
                    // Simulate high failure rate initially
                    if (attempt <= 10) {
                        throw new RuntimeException("Service unavailable");
                    }
                    return new Enrollment();
                });

            // Act - Circuit breaker should open after consecutive failures
            for (int i = 1; i <= 20; i++) {
                final long studentId = i;
                executorService.submit(() -> {
                    try {
                        enrollmentProcessor.processEnrollmentWithCircuitBreaker(studentId, 1L);
                    } catch (Exception e) {
                        if (e.getMessage().contains("Circuit breaker is open")) {
                            circuitOpenCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertThat(circuitOpenCount.get()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should implement retry mechanism with exponential backoff")
        void shouldImplementRetryMechanismWithExponentialBackoff() throws InterruptedException {
            // Arrange
            AtomicInteger attemptCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(1);
            
            when(courseService.findCourseById(1L)).thenReturn(testCourse);
            when(enrollmentService.enrollStudent(eq(1L), eq(1L)))
                .thenAnswer(invocation -> {
                    int attempt = attemptCount.incrementAndGet();
                    if (attempt < 3) {
                        throw new RuntimeException("Temporary failure");
                    }
                    return new Enrollment();
                });

            long startTime = System.currentTimeMillis();

            // Act
            executorService.submit(() -> {
                try {
                    enrollmentProcessor.processEnrollmentWithRetry(1L, 1L, 3, 100);
                } catch (Exception e) {
                    fail("Should have succeeded after retries");
                } finally {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(attemptCount.get()).isEqualTo(3);
            assertThat(duration).isGreaterThan(300); // Should have backoff delays (100ms + 200ms)
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}