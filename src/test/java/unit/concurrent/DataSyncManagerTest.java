// File location: src/test/java/unit/concurrent/DataSyncManagerTest.java

package com.smartcampus.test.unit.concurrent;

import com.smartcampus.services.concurrent.DataSyncManager;
import com.smartcampus.services.StudentService;
import com.smartcampus.services.CourseService;
import com.smartcampus.services.EnrollmentService;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.sync.SyncResult;
import com.smartcampus.models.sync.ConflictResolutionStrategy;

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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for asynchronous data synchronization
 * Tests async operations, data consistency, and conflict resolution
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Data Sync Manager Async Tests")
class DataSyncManagerTest {

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private DataSyncManager dataSyncManager;

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newCachedThreadPool();
        scheduledExecutorService = Executors.newScheduledThreadPool(5);
        dataSyncManager.setExecutorService(executorService);
        dataSyncManager.setScheduledExecutorService(scheduledExecutorService);
    }

    @Nested
    @DisplayName("Asynchronous Data Synchronization Tests")
    class AsyncDataSynchronizationTests {

        @Test
        @DisplayName("Should synchronize student data asynchronously")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldSynchronizeStudentDataAsynchronously() throws Exception {
            // Arrange
            List<Student> sourceStudents = createTestStudents(5);
            List<Student> targetStudents = createTestStudents(3);
            
            when(studentService.getAllStudents()).thenReturn(targetStudents);
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(studentService.updateStudent(anyLong(), any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeStudentsAsync(sourceStudents);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getProcessedCount()).isEqualTo(5);
            assertThat(result.getCreatedCount()).isEqualTo(2); // 5 source - 3 existing
            assertThat(result.getUpdatedCount()).isEqualTo(3); // 3 existing updated

            verify(studentService, times(2)).createStudent(any(Student.class));
            verify(studentService, times(3)).updateStudent(anyLong(), any(Student.class));
        }

        @Test
        @DisplayName("Should handle sync failures gracefully")
        void shouldHandleSyncFailuresGracefully() throws Exception {
            // Arrange
            List<Student> sourceStudents = createTestStudents(3);
            
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());
            when(studentService.createStudent(any(Student.class)))
                .thenThrow(new RuntimeException("Database error"))
                .thenReturn(sourceStudents.get(1))
                .thenReturn(sourceStudents.get(2));

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeStudentsAsync(sourceStudents);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getProcessedCount()).isEqualTo(3);
            assertThat(result.getFailedCount()).isEqualTo(1);
            assertThat(result.getErrorMessages()).hasSize(1);
            assertThat(result.getErrorMessages().get(0)).contains("Database error");
        }

        @Test
        @DisplayName("Should process large datasets efficiently")
        void shouldProcessLargeDatasetsEfficiently() throws Exception {
            // Arrange
            int datasetSize = 1000;
            List<Student> largeDataset = createTestStudents(datasetSize);
            AtomicInteger processedCount = new AtomicInteger(0);
            
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> {
                    processedCount.incrementAndGet();
                    // Simulate processing time
                    Thread.sleep(1);
                    return invocation.getArgument(0);
                });

            long startTime = System.currentTimeMillis();

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeStudentsAsyncBatched(largeDataset, 50);

            SyncResult result = syncFuture.get(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getProcessedCount()).isEqualTo(datasetSize);
            assertThat(processedCount.get()).isEqualTo(datasetSize);
            assertThat(duration).isLessThan(20000); // Should complete within 20 seconds
            
            // Verify batching occurred
            verify(studentService, times(datasetSize)).createStudent(any(Student.class));
        }

        @Test
        @DisplayName("Should maintain data consistency during parallel sync")
        void shouldMaintainDataConsistencyDuringParallelSync() throws Exception {
            // Arrange
            List<Student> dataset1 = createTestStudents(5, "Dataset1");
            List<Student> dataset2 = createTestStudents(5, "Dataset2");
            
            Map<String, Student> syncedStudents = new ConcurrentHashMap<>();
            
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> {
                    Student student = invocation.getArgument(0);
                    syncedStudents.put(student.getStudentId(), student);
                    return student;
                });

            // Act - Run parallel synchronization
            CompletableFuture<SyncResult> sync1 = dataSyncManager
                .synchronizeStudentsAsync(dataset1);
            CompletableFuture<SyncResult> sync2 = dataSyncManager
                .synchronizeStudentsAsync(dataset2);

            CompletableFuture<Void> allSyncs = CompletableFuture.allOf(sync1, sync2);
            allSyncs.get(10, TimeUnit.SECONDS);

            // Assert
            SyncResult result1 = sync1.get();
            SyncResult result2 = sync2.get();
            
            assertThat(result1.isSuccessful()).isTrue();
            assertThat(result2.isSuccessful()).isTrue();
            assertThat(syncedStudents).hasSize(10);
            
            // Verify no data corruption occurred
            dataset1.forEach(student -> 
                assertThat(syncedStudents.get(student.getStudentId()).getFirstName())
                    .startsWith("Dataset1"));
            dataset2.forEach(student -> 
                assertThat(syncedStudents.get(student.getStudentId()).getFirstName())
                    .startsWith("Dataset2"));
        }
    }

    // Helper methods for creating test data
    private List<Student> createTestStudents(int count) {
        return createTestStudents(count, "TestStudent");
    }

    private List<Student> createTestStudents(int count, String namePrefix) {
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            students.add(createTestStudent((long) i, namePrefix + i));
        }
        return students;
    }

    private Student createTestStudent(Long id, String namePrefix) {
        Student student = new Student();
        student.setId(id);
        student.setStudentId("STU" + String.format("%03d", id));
        student.setFirstName(namePrefix);
        student.setLastName("LastName" + id);
        student.setEmail(namePrefix.toLowerCase() + id + "@test.com");
        student.setPhone("555-" + String.format("%04d", id));
        student.setLastModified(LocalDateTime.now());
        return student;
    }

    private Student createConflictingStudent(Long id, String firstName, String email, String phone) {
        Student student = new Student();
        student.setId(id);
        student.setStudentId("STU" + String.format("%03d", id));
        student.setFirstName(firstName);
        student.setLastName("LastName" + id);
        student.setEmail(email);
        student.setPhone(phone);
        student.setLastModified(LocalDateTime.now());
        return student;
    }

    private Student createInvalidStudent(Long id, String firstName, String email) {
        Student student = new Student();
        student.setId(id);
        student.setStudentId("STU" + String.format("%03d", id));
        student.setFirstName(firstName);
        student.setLastName("LastName" + id);
        student.setEmail(email);
        student.setLastModified(LocalDateTime.now());
        return student;
    }

    private List<Course> createTestCourses(int count) {
        List<Course> courses = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Course course = new Course();
            course.setId((long) i);
            course.setCourseCode("CS" + (100 + i));
            course.setCourseName("Course " + i);
            course.setCredits(3);
            courses.add(course);
        }
        return courses;
    }

    private Enrollment createTestEnrollment(Long id, Long studentId, Long courseId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudent(createTestStudent(studentId, "Student"));
        enrollment.setCourse(createTestCourses(1).get(0));
        enrollment.setEnrollmentDate(LocalDateTime.now());
        return enrollment;
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
        
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

    @Nested
    @DisplayName("Conflict Resolution Tests")
    class ConflictResolutionTests {

        @Test
        @DisplayName("Should resolve conflicts using last-write-wins strategy")
        void shouldResolveConflictsUsingLastWriteWinsStrategy() throws Exception {
            // Arrange
            Student sourceStudent = createTestStudent(1L, "Source");
            sourceStudent.setLastModified(LocalDateTime.now());
            
            Student existingStudent = createTestStudent(1L, "Existing");
            existingStudent.setLastModified(LocalDateTime.now().minusHours(1));
            
            when(studentService.findStudentById(1L)).thenReturn(existingStudent);
            when(studentService.updateStudent(eq(1L), any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithConflictResolution(
                    Arrays.asList(sourceStudent),
                    ConflictResolutionStrategy.LAST_WRITE_WINS
                );

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getConflictsResolved()).isEqualTo(1);
            
            verify(studentService).updateStudent(eq(1L), argThat(student -> 
                student.getFirstName().equals("Source")));
        }

        @Test
        @DisplayName("Should resolve conflicts using manual merge strategy")
        void shouldResolveConflictsUsingManualMergeStrategy() throws Exception {
            // Arrange
            Student sourceStudent = createTestStudent(1L, "Source");
            sourceStudent.setEmail("source@example.com");
            
            Student existingStudent = createTestStudent(1L, "Existing");
            existingStudent.setEmail("existing@example.com");
            existingStudent.setPhone("555-1234");
            
            when(studentService.findStudentById(1L)).thenReturn(existingStudent);
            when(studentService.updateStudent(eq(1L), any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

            // Custom merger that combines data
            dataSyncManager.setConflictMerger((source, existing) -> {
                Student merged = new Student();
                merged.setId(existing.getId());
                merged.setFirstName(source.getFirstName()); // Take from source
                merged.setEmail(source.getEmail()); // Take from source
                merged.setPhone(existing.getPhone()); // Keep existing
                return merged;
            });

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithConflictResolution(
                    Arrays.asList(sourceStudent),
                    ConflictResolutionStrategy.MANUAL_MERGE
                );

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            verify(studentService).updateStudent(eq(1L), argThat(student -> 
                student.getFirstName().equals("Source") && 
                student.getEmail().equals("source@example.com") &&
                "555-1234".equals(student.getPhone())));
        }

        @Test
        @DisplayName("Should skip conflicts when using ignore strategy")
        void shouldSkipConflictsWhenUsingIgnoreStrategy() throws Exception {
            // Arrange
            Student sourceStudent = createTestStudent(1L, "Source");
            Student existingStudent = createTestStudent(1L, "Existing");
            
            when(studentService.findStudentById(1L)).thenReturn(existingStudent);

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithConflictResolution(
                    Arrays.asList(sourceStudent),
                    ConflictResolutionStrategy.IGNORE_CONFLICTS
                );

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getSkippedCount()).isEqualTo(1);
            
            verify(studentService, never()).updateStudent(anyLong(), any(Student.class));
        }

        @Test
        @DisplayName("Should handle complex multi-field conflicts")
        void shouldHandleComplexMultiFieldConflicts() throws Exception {
            // Arrange
            List<Student> sourceStudents = Arrays.asList(
                createConflictingStudent(1L, "John", "john.new@test.com", "555-0001"),
                createConflictingStudent(2L, "Jane", "jane.new@test.com", "555-0002")
            );
            
            List<Student> existingStudents = Arrays.asList(
                createConflictingStudent(1L, "John", "john.old@test.com", "555-9999"),
                createConflictingStudent(2L, "Janet", "janet.old@test.com", "555-8888")
            );
            
            when(studentService.findStudentById(1L)).thenReturn(existingStudents.get(0));
            when(studentService.findStudentById(2L)).thenReturn(existingStudents.get(1));
            when(studentService.updateStudent(anyLong(), any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithAdvancedConflictResolution(sourceStudents);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getConflictsResolved()).isEqualTo(2);
            
            // Verify complex conflict resolution occurred
            verify(studentService, times(2)).updateStudent(anyLong(), any(Student.class));
        }
    }

    @Nested
    @DisplayName("Scheduled Synchronization Tests")
    class ScheduledSynchronizationTests {

        @Test
        @DisplayName("Should execute scheduled sync tasks")
        void shouldExecuteScheduledSyncTasks() throws Exception {
            // Arrange
            AtomicInteger syncCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(3);
            
            when(studentService.getAllStudents()).thenReturn(createTestStudents(2));
            
            dataSyncManager.setSyncCallback(() -> {
                syncCount.incrementAndGet();
                latch.countDown();
            });

            // Act - Schedule sync every 100ms
            ScheduledFuture<?> scheduledSync = dataSyncManager
                .schedulePeriodicSync(100, TimeUnit.MILLISECONDS);

            // Wait for multiple executions
            assertTrue(latch.await(2, TimeUnit.SECONDS));
            scheduledSync.cancel(true);

            // Assert
            assertThat(syncCount.get()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Should handle scheduled sync failures gracefully")
        void shouldHandleScheduledSyncFailuresGracefully() throws Exception {
            // Arrange
            AtomicInteger attemptCount = new AtomicInteger(0);
            AtomicBoolean hadFailure = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(5);
            
            when(studentService.getAllStudents())
                .thenAnswer(invocation -> {
                    int attempt = attemptCount.incrementAndGet();
                    latch.countDown();
                    
                    if (attempt == 2 || attempt == 3) {
                        hadFailure.set(true);
                        throw new RuntimeException("Sync failure");
                    }
                    return createTestStudents(1);
                });

            // Act
            ScheduledFuture<?> scheduledSync = dataSyncManager
                .schedulePeriodicSyncWithErrorHandling(50, TimeUnit.MILLISECONDS);

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            scheduledSync.cancel(true);

            // Assert
            assertTrue(hadFailure.get());
            assertThat(attemptCount.get()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should implement exponential backoff for failed syncs")
        void shouldImplementExponentialBackoffForFailedSyncs() throws Exception {
            // Arrange
            List<Long> executionTimes = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger failureCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(4);
            
            when(studentService.getAllStudents())
                .thenAnswer(invocation -> {
                    executionTimes.add(System.currentTimeMillis());
                    int failures = failureCount.get();
                    latch.countDown();
                    
                    if (failures < 3) {
                        failureCount.incrementAndGet();
                        throw new RuntimeException("Sync failure #" + (failures + 1));
                    }
                    return createTestStudents(1);
                });

            long startTime = System.currentTimeMillis();

            // Act
            ScheduledFuture<?> scheduledSync = dataSyncManager
                .schedulePeriodicSyncWithBackoff(50, TimeUnit.MILLISECONDS);

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            scheduledSync.cancel(true);

            // Assert
            assertThat(executionTimes).hasSizeGreaterThanOrEqualTo(4);
            
            // Verify exponential backoff - intervals should increase
            if (executionTimes.size() >= 3) {
                long interval1 = executionTimes.get(1) - executionTimes.get(0);
                long interval2 = executionTimes.get(2) - executionTimes.get(1);
                assertThat(interval2).isGreaterThan(interval1);
            }
        }

        @Test
        @DisplayName("Should coordinate multiple scheduled sync tasks")
        void shouldCoordinateMultipleScheduledSyncTasks() throws Exception {
            // Arrange
            AtomicInteger studentSyncCount = new AtomicInteger(0);
            AtomicInteger courseSyncCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(6); // 3 of each type
            
            when(studentService.getAllStudents()).thenReturn(createTestStudents(1));
            when(courseService.getAllCourses()).thenReturn(createTestCourses(1));

            // Act - Schedule different sync tasks
            ScheduledFuture<?> studentSync = scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    dataSyncManager.syncStudents();
                    studentSyncCount.incrementAndGet();
                    latch.countDown();
                } catch (Exception e) {
                    // Ignore for this test
                }
            }, 0, 100, TimeUnit.MILLISECONDS);

            ScheduledFuture<?> courseSync = scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    dataSyncManager.syncCourses();
                    courseSyncCount.incrementAndGet();
                    latch.countDown();
                } catch (Exception e) {
                    // Ignore for this test
                }
            }, 50, 100, TimeUnit.MILLISECONDS); // Offset by 50ms

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            studentSync.cancel(true);
            courseSync.cancel(true);

            // Assert
            assertThat(studentSyncCount.get()).isGreaterThanOrEqualTo(3);
            assertThat(courseSyncCount.get()).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Data Validation and Integrity Tests")
    class DataValidationAndIntegrityTests {

        @Test
        @DisplayName("Should validate data before synchronization")
        void shouldValidateDataBeforeSynchronization() throws Exception {
            // Arrange
            List<Student> invalidStudents = Arrays.asList(
                createInvalidStudent(1L, "", "invalid-email"), // Invalid name and email
                createInvalidStudent(2L, "Valid Name", "valid@test.com"), // Valid
                createInvalidStudent(3L, "Another Valid", "") // Invalid email
            );
            
            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithValidation(invalidStudents);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.getProcessedCount()).isEqualTo(3);
            assertThat(result.getValidationErrors()).hasSize(2); // Two invalid records
            assertThat(result.getSuccessfulCount()).isEqualTo(1); // One valid record
        }

        @Test
        @DisplayName("Should maintain referential integrity during sync")
        void shouldMaintainReferentialIntegrityDuringSync() throws Exception {
            // Arrange
            List<Course> courses = createTestCourses(2);
            List<Enrollment> enrollments = Arrays.asList(
                createTestEnrollment(1L, 1L, 1L), // Valid
                createTestEnrollment(2L, 2L, 2L), // Valid
                createTestEnrollment(3L, 999L, 1L) // Invalid student ID
            );
            
            when(studentService.findStudentById(1L)).thenReturn(createTestStudent(1L, "Student1"));
            when(studentService.findStudentById(2L)).thenReturn(createTestStudent(2L, "Student2"));
            when(studentService.findStudentById(999L)).thenReturn(null);
            when(courseService.findCourseById(anyLong())).thenReturn(courses.get(0));

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeEnrollmentsWithIntegrityCheck(enrollments);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.getProcessedCount()).isEqualTo(3);
            assertThat(result.getIntegrityViolations()).hasSize(1);
            assertThat(result.getSuccessfulCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should detect and report data inconsistencies")
        void shouldDetectAndReportDataInconsistencies() throws Exception {
            // Arrange
            List<Student> sourceData = createTestStudents(3);
            List<Student> corruptedData = createTestStudents(3);
            
            // Simulate data corruption
            corruptedData.get(1).setEmail("corrupted@invalid");
            corruptedData.get(2).setStudentId(null);
            
            when(studentService.getAllStudents()).thenReturn(corruptedData);

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .performConsistencyCheck(sourceData);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.getInconsistencies()).hasSize(2);
            assertThat(result.getInconsistencies().get(0)).contains("email");
            assertThat(result.getInconsistencies().get(1)).contains("studentId");
        }

        @Test
        @DisplayName("Should rollback failed synchronization transactions")
        void shouldRollbackFailedSynchronizationTransactions() throws Exception {
            // Arrange
            List<Student> students = createTestStudents(3);
            AtomicInteger saveCount = new AtomicInteger(0);
            AtomicInteger rollbackCount = new AtomicInteger(0);
            
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> {
                    int count = saveCount.incrementAndGet();
                    if (count == 2) {
                        throw new RuntimeException("Database constraint violation");
                    }
                    return invocation.getArgument(0);
                });
            
            dataSyncManager.setRollbackHandler(() -> rollbackCount.incrementAndGet());

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithTransaction(students);

            SyncResult result = syncFuture.get(5, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isFalse();
            assertThat(rollbackCount.get()).isEqualTo(1);
            assertThat(result.getRollbackPerformed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Performance and Monitoring Tests")
    class PerformanceAndMonitoringTests {

        @Test
        @DisplayName("Should monitor synchronization performance")
        void shouldMonitorSynchronizationPerformance() throws Exception {
            // Arrange
            List<Student> largeDataset = createTestStudents(500);
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(2); // Simulate processing time
                    return invocation.getArgument(0);
                });

            // Act
            long startTime = System.currentTimeMillis();
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithPerformanceMonitoring(largeDataset);

            SyncResult result = syncFuture.get(30, TimeUnit.SECONDS);
            long totalTime = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getPerformanceMetrics()).isNotNull();
            assertThat(result.getPerformanceMetrics().getTotalTimeMs()).isGreaterThan(0);
            assertThat(result.getPerformanceMetrics().getAverageTimePerRecord()).isGreaterThan(0);
            assertThat(result.getPerformanceMetrics().getThroughputPerSecond()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should provide real-time sync progress updates")
        void shouldProvideRealTimeSyncProgressUpdates() throws Exception {
            // Arrange
            List<Student> dataset = createTestStudents(10);
            List<Integer> progressUpdates = Collections.synchronizedList(new ArrayList<>());
            
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(50); // Simulate processing time
                    return invocation.getArgument(0);
                });

            dataSyncManager.setProgressCallback(progress -> progressUpdates.add(progress));

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithProgressTracking(dataset);

            SyncResult result = syncFuture.get(10, TimeUnit.SECONDS);

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(progressUpdates).isNotEmpty();
            assertThat(progressUpdates).contains(100); // Should reach 100%
            
            // Verify progress updates are in ascending order
            for (int i = 1; i < progressUpdates.size(); i++) {
                assertThat(progressUpdates.get(i)).isGreaterThanOrEqualTo(progressUpdates.get(i - 1));
            }
        }

        @Test
        @DisplayName("Should handle memory efficiently during large syncs")
        void shouldHandleMemoryEfficientlyDuringLargeSyncs() throws Exception {
            // Arrange
            List<Student> largeDataset = createTestStudents(5000);
            Runtime runtime = Runtime.getRuntime();
            
            when(studentService.getAllStudents()).thenReturn(Collections.emptyList());
            when(studentService.createStudent(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Measure initial memory
            System.gc();
            Thread.sleep(100);
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            // Act
            CompletableFuture<SyncResult> syncFuture = dataSyncManager
                .synchronizeWithMemoryOptimization(largeDataset, 100);

            SyncResult result = syncFuture.get(30, TimeUnit.SECONDS);

            // Measure final memory
            System.gc();
            Thread.sleep(100);
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            // Assert
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getProcessedCount()).isEqualTo(5000);
            
            // Memory increase should be reasonable (less than 200MB for this test)
            assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024);
        }
    }