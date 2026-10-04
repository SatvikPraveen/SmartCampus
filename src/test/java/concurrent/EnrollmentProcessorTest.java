package concurrent;

import concurrent.EnrollmentProcessor.EnrollmentPriority;
import concurrent.EnrollmentProcessor.EnrollmentRequest;
import concurrent.EnrollmentProcessor.EnrollmentResult;
import concurrent.EnrollmentProcessor.ProcessingStats;
import models.Course;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import services.EnrollmentService;
import services.NotificationService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Timeout(30)
class EnrollmentProcessorTest {

    /**
     * Enrollment service that can block inside enrollStudent, refuse selected attempts
     * (1-based attempt number) or throw.
     */
    static class ControllableEnrollmentService extends EnrollmentService {
        final AtomicInteger attempts = new AtomicInteger();
        volatile IntPredicate refuseAttempt = n -> false;
        volatile CountDownLatch entered;
        volatile CountDownLatch release;
        volatile RuntimeException failure;

        @Override
        public boolean enrollStudent(String studentId, String courseId, String semester, int year) {
            int attempt = attempts.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            if (release != null) {
                entered.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (refuseAttempt.test(attempt)) {
                return false;
            }
            return super.enrollStudent(studentId, courseId, semester, year);
        }
    }

    private ControllableEnrollmentService enrollmentService;
    private NotificationService notificationService;
    private EnrollmentProcessor processor;

    @BeforeEach
    void setUp() {
        enrollmentService = new ControllableEnrollmentService();
        notificationService = mock(NotificationService.class);
        processor = new EnrollmentProcessor(enrollmentService, notificationService);
        processor.setRetryBackoffMillis(1);
    }

    @AfterEach
    void tearDown() {
        if (enrollmentService.release != null) {
            enrollmentService.release.countDown();
        }
        processor.shutdown();
    }

    private static Student student(int i) {
        return new Student("U" + i, "First", "Last" + i, "s" + i + "@u.edu", null, "S" + i, "CS",
                Student.AcademicYear.JUNIOR);
    }

    private static Course course(int capacity) {
        Course c = new Course("C1", "CS101", "Intro", "", 3, "D-CS", "P1", Course.DifficultyLevel.BEGINNER,
                "Fall", 2024);
        c.setMaxEnrollment(capacity);
        c.setStatus(Course.CourseStatus.OPEN);
        return c;
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    @Nested
    class SingleEnrollment {

        @Test
        void enrollsAndSendsConfirmation() throws Exception {
            Student s = student(1);
            Course c = course(10);

            EnrollmentResult result = await(processor.processEnrollmentAsync(s, c));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Enrollment successful");
            assertThat(result.getStudent()).isSameAs(s);
            assertThat(result.getCourse()).isSameAs(c);
            assertThat(result.getProcessedTime()).isNotNull();
            assertThat(result).hasToString(
                    "EnrollmentResult{success=true, message='Enrollment successful', student=First Last1, course=Intro}");
            assertThat(enrollmentService.isStudentEnrolled("S1", "C1")).isTrue();
            verify(notificationService, timeout(5000)).sendNotification(eq("U1"),
                    eq(NotificationService.NotificationType.ACADEMIC), eq("Enrollment Confirmed"),
                    contains("CS101 - Intro"), eq(NotificationService.Priority.NORMAL));
        }

        @Test
        void refusesDuplicateEnrollment() throws Exception {
            await(processor.processEnrollmentAsync(student(1), course(10)));

            EnrollmentResult again = await(processor.processEnrollmentAsync(student(1), course(10)));

            assertThat(again.isSuccess()).isFalse();
            assertThat(again.getMessage()).isEqualTo("Student already enrolled");
        }

        @Test
        void refusesWhenCourseObjectIsAlreadyFull() throws Exception {
            Course c = course(1);
            assertThat(c.enrollStudent("OTHER")).isTrue();

            EnrollmentResult result = await(processor.processEnrollmentAsync(student(1), c));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Course is full");
        }

        @Test
        void reportsFailureWhenServiceRefuses() throws Exception {
            enrollmentService.refuseAttempt = n -> true;

            EnrollmentResult result = await(processor.processEnrollmentAsync(student(1), course(10)));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Enrollment failed");
        }
    }

    @Nested
    class Capacity {

        // Regression: capacity was checked against Course#getEnrolledStudentIds, which the
        // processor never updates (enrollments are recorded by the EnrollmentService), so
        // any number of students could be enrolled into a course of any capacity.
        @Test
        void concurrentRequestsNeverExceedCourseCapacity() throws Exception {
            Course c = course(3);
            List<EnrollmentRequest> requests = IntStream.range(0, 12)
                    .mapToObj(i -> new EnrollmentRequest(student(i), c, EnrollmentPriority.NORMAL))
                    .toList();

            List<EnrollmentResult> results = await(processor.processBatchEnrollments(requests));

            assertThat(results).extracting(EnrollmentResult::getStudent)
                    .containsExactlyElementsOf(requests.stream().map(EnrollmentRequest::getStudent).toList());
            assertThat(results).filteredOn(EnrollmentResult::isSuccess).hasSize(3);
            assertThat(results).filteredOn(r -> !r.isSuccess())
                    .extracting(EnrollmentResult::getMessage).containsOnly("Course is full");
            assertThat(enrollmentService.getCurrentEnrollmentCount("C1")).isEqualTo(3);
        }

        @Test
        void batchOfDistinctStudentsWithinCapacityAllSucceed() throws Exception {
            Course c = course(20);
            List<EnrollmentRequest> requests = IntStream.range(0, 10)
                    .mapToObj(i -> new EnrollmentRequest(student(i), c, EnrollmentPriority.HIGH))
                    .toList();

            List<EnrollmentResult> results = await(processor.processBatchEnrollments(requests));

            assertThat(results).allMatch(EnrollmentResult::isSuccess);
            assertThat(enrollmentService.getCurrentEnrollmentCount("C1")).isEqualTo(10);
        }
    }

    @Nested
    class TimeoutsAndRetries {

        @Test
        void completesNormallyWithinTimeout() throws Exception {
            EnrollmentResult result = await(processor.processEnrollmentWithTimeout(
                    student(1), course(5), 10, TimeUnit.SECONDS));

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        void timedOutEnrollmentIsCancelledAndNeverCommittedLater() throws Exception {
            // A first enrollment holds the processor's lock inside enrollStudent ...
            enrollmentService.entered = new CountDownLatch(1);
            enrollmentService.release = new CountDownLatch(1);
            Course c = course(5);
            CompletableFuture<EnrollmentResult> first = processor.processEnrollmentAsync(student(1), c);
            assertThat(enrollmentService.entered.await(10, TimeUnit.SECONDS)).isTrue();

            // ... so the second one is still waiting for it when its timeout fires.
            CompletableFuture<EnrollmentResult> second = processor.processEnrollmentWithTimeout(
                    student(2), c, 20, TimeUnit.MILLISECONDS);
            EnrollmentResult result = await(second);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Enrollment timed out");

            enrollmentService.release.countDown();
            assertThat(await(first).isSuccess()).isTrue();
            processor.waitForCompletion(10, TimeUnit.SECONDS);

            assertThat(enrollmentService.isStudentEnrolled("S2", "C1")).isFalse();
            assertThat(enrollmentService.attempts.get()).isEqualTo(1);
        }

        @Test
        void timeoutDuringCommitReportsTheRealOutcome() throws Exception {
            enrollmentService.entered = new CountDownLatch(1);
            enrollmentService.release = new CountDownLatch(1);

            CompletableFuture<EnrollmentResult> pending = processor.processEnrollmentWithTimeout(
                    student(1), course(5), 20, TimeUnit.MILLISECONDS);
            assertThat(enrollmentService.entered.await(10, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(50); // let the timeout fire while the commit is in progress
            enrollmentService.release.countDown();

            EnrollmentResult result = await(pending);
            assertThat(result.isSuccess()).isTrue();
            assertThat(enrollmentService.isStudentEnrolled("S1", "C1")).isTrue();
        }

        @Test
        void serviceExceptionIsReportedAsError() throws Exception {
            enrollmentService.failure = new IllegalStateException("registry offline");

            EnrollmentResult result = await(processor.processEnrollmentWithTimeout(
                    student(1), course(5), 10, TimeUnit.SECONDS));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).startsWith("Enrollment error: ").contains("registry offline");
        }

        @Test
        void retriesUntilEnrollmentSucceeds() throws Exception {
            enrollmentService.refuseAttempt = n -> n < 3;

            EnrollmentResult result = await(processor.processEnrollmentWithRetry(student(1), course(5), 5));

            assertThat(result.isSuccess()).isTrue();
            assertThat(enrollmentService.attempts.get()).isEqualTo(3);
        }

        @Test
        void returnsLastFailureWhenRetriesAreExhausted() throws Exception {
            enrollmentService.refuseAttempt = n -> true;

            EnrollmentResult result = await(processor.processEnrollmentWithRetry(student(1), course(5), 2));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Enrollment failed");
            assertThat(enrollmentService.attempts.get()).isEqualTo(3);
        }
    }

    @Test
    void scheduledEnrollmentRunsAfterDelay() throws Exception {
        EnrollmentResult result = processor.scheduleEnrollment(student(1), course(5), 1, TimeUnit.MILLISECONDS)
                .get(10, TimeUnit.SECONDS);

        assertThat(result.isSuccess()).isTrue();
        assertThat(enrollmentService.isStudentEnrolled("S1", "C1")).isTrue();
    }

    @Test
    void queuedRequestsAreProcessedInTheBackground() throws Exception {
        CountDownLatch confirmed = new CountDownLatch(3);
        doAnswer(inv -> {
            confirmed.countDown();
            return null;
        }).when(notificationService).sendNotification(any(), any(), any(), any(), any());
        Course c = course(10);

        for (int i = 0; i < 3; i++) {
            processor.queueEnrollmentRequest(student(i), c, EnrollmentPriority.URGENT);
        }

        assertThat(confirmed.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(enrollmentService.getCurrentEnrollmentCount("C1")).isEqualTo(3);
    }

    @Test
    void statsReflectActiveWorkAndWaitForCompletionReturnsOnceIdle() throws Exception {
        enrollmentService.entered = new CountDownLatch(1);
        enrollmentService.release = new CountDownLatch(1);

        CompletableFuture<EnrollmentResult> pending = processor.processEnrollmentAsync(student(1), course(5));
        assertThat(enrollmentService.entered.await(10, TimeUnit.SECONDS)).isTrue();

        ProcessingStats busy = processor.getProcessingStats();
        assertThat(busy.getActiveProcesses()).isEqualTo(1);
        assertThat(busy.getQueueSize()).isZero();
        assertThat(busy.isProcessing()).isTrue();
        assertThat(busy).hasToString("ProcessingStats{active=1, queued=0, processing=true}");

        enrollmentService.release.countDown();
        assertThat(await(pending).isSuccess()).isTrue();
        processor.waitForCompletion(10, TimeUnit.SECONDS);
        assertThat(processor.getProcessingStats().getActiveProcesses()).isZero();
    }

    @Test
    void requestCarriesItsMetadata() {
        Student s = student(1);
        Course c = course(5);
        EnrollmentRequest request = new EnrollmentRequest(s, c, EnrollmentPriority.LOW);

        assertThat(request.getStudent()).isSameAs(s);
        assertThat(request.getCourse()).isSameAs(c);
        assertThat(request.getPriority()).isEqualTo(EnrollmentPriority.LOW);
        assertThat(request.getRequestTime()).isNotNull();
    }

    @Test
    void shutdownProcessesRequestsStillWaitingInTheQueue() {
        Course c = course(25); // within EnrollmentService's default per-course limit of 30
        for (int i = 0; i < 25; i++) {
            processor.queueEnrollmentRequest(student(i), c, EnrollmentPriority.NORMAL);
        }

        processor.shutdown();

        assertThat(enrollmentService.getCurrentEnrollmentCount("C1")).isEqualTo(25);
        assertThatThrownBy(() -> processor.queueEnrollmentRequest(student(99), c, EnrollmentPriority.NORMAL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shutdownStopsProcessingAndRejectsNewWork() {
        processor.shutdown();

        assertThat(processor.getProcessingStats().isProcessing()).isFalse();
        assertThatThrownBy(() -> processor.processEnrollmentAsync(student(1), course(5)))
                .isInstanceOf(RejectedExecutionException.class);
    }
}
