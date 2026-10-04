package concurrent;

import concurrent.AsyncNotificationSender.BulkNotificationResult;
import concurrent.AsyncNotificationSender.NotificationResult;
import concurrent.AsyncNotificationSender.NotificationStatistics;
import concurrent.AsyncNotificationSender.NotificationTask;
import concurrent.AsyncNotificationSender.NotificationType;
import concurrent.AsyncNotificationSender.Priority;
import models.Course;
import models.Grade;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import services.NotificationService;
import services.NotificationService.DeliveryChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@Timeout(30)
class AsyncNotificationSenderTest {

    /** One recorded call to {@code NotificationService#sendNotification(..., channels)}. */
    record Sent(String recipient, NotificationService.NotificationType type, String title, String message,
                NotificationService.Priority priority, Set<DeliveryChannel> channels, long atNanos) {
    }

    private final ConcurrentLinkedQueue<Sent> sent = new ConcurrentLinkedQueue<>();
    private final List<AsyncNotificationSender> started = new ArrayList<>();

    @AfterEach
    void tearDown() {
        started.forEach(AsyncNotificationSender::shutdown);
    }

    /**
     * A notification service mock that records every delivery and succeeds unless the
     * recipient matches {@code fails}.
     */
    @SuppressWarnings("unchecked")
    private NotificationService service(Predicate<String> fails) {
        NotificationService service = mock(NotificationService.class);
        NotificationService.Notification delivered = mock(NotificationService.Notification.class);
        doAnswer(inv -> {
            sent.add(new Sent(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2), inv.getArgument(3),
                    inv.getArgument(4), (Set<DeliveryChannel>) inv.getArgument(5), System.nanoTime()));
            return fails.test(inv.getArgument(0)) ? null : delivered;
        }).when(service).sendNotification(any(), any(), any(), any(), any(), anySet());
        return service;
    }

    private AsyncNotificationSender sender(NotificationService service, int threads, int perSecond) {
        AsyncNotificationSender s = new AsyncNotificationSender(service, threads, perSecond);
        started.add(s);
        return s;
    }

    private AsyncNotificationSender sender() {
        return sender(service(r -> false), 4, 10_000);
    }

    private static Student student(String id) {
        return new Student("U" + id, "Ann", "Lee" + id, id.toLowerCase() + "@u.edu", null, id, "CS",
                Student.AcademicYear.SENIOR);
    }

    private static Course course() {
        return new Course("C1", "CS101", "Intro to CS", "", 4, "D-CS", null, Course.DifficultyLevel.BEGINNER,
                "Fall", 2024);
    }

    private static NotificationTask task(NotificationType type, String recipient) {
        return new NotificationTask(type, recipient, "Subject", "Body", Priority.LOW, 0);
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    @Nested
    class TypedNotifications {

        @Test
        void enrollmentConfirmationIsAnAcademicEmailWithCourseDetails() throws Exception {
            AsyncNotificationSender sender = sender();

            NotificationResult result = await(sender.sendEnrollmentConfirmationAsync(student("S1"), course()));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Notification sent successfully");
            assertThat(result.getSentAt()).isNotNull();
            assertThat(result.getTask().getType()).isEqualTo(NotificationType.ENROLLMENT_CONFIRMATION);
            assertThat(sent).singleElement().satisfies(s -> {
                assertThat(s.recipient()).isEqualTo("s1@u.edu");
                assertThat(s.type()).isEqualTo(NotificationService.NotificationType.ACADEMIC);
                assertThat(s.title()).isEqualTo("Enrollment Confirmation");
                assertThat(s.priority()).isEqualTo(NotificationService.Priority.NORMAL);
                assertThat(s.channels()).containsExactly(DeliveryChannel.EMAIL);
                assertThat(s.message()).contains("Dear Ann LeeS1", "Intro to CS (CS101)",
                        "Professor ID: TBA", "Credits: 4", "Semester: Fall");
            });
            assertThat(sender.getStatistics().getTotalSent()).isEqualTo(1);
        }

        @Test
        void gradeNotificationIsHighPriorityAndIncludesGradeAndFeedback() throws Exception {
            Grade grade = new Grade("G1", "E1", "S1", "C1", "A1", "Midterm", Grade.GradeComponent.EXAM,
                    90, 100, null, "P1", "Fall", 2024);
            Grade ungraded = new Grade("G2", "E1", "S1", "C1", "A2", "Final", Grade.GradeComponent.EXAM,
                    50, 100, null, "P1", "Fall", 2024);
            grade.setFeedback("Well done");
            AsyncNotificationSender sender = sender();

            assertThat(await(sender.sendGradeNotificationAsync(student("S1"), course(), grade)).isSuccess()).isTrue();
            assertThat(await(sender.sendGradeNotificationAsync(student("S1"), course(), ungraded)).isSuccess()).isTrue();

            assertThat(sent).hasSize(2).allSatisfy(s -> {
                assertThat(s.title()).isEqualTo("Grade Posted");
                assertThat(s.type()).isEqualTo(NotificationService.NotificationType.ACADEMIC);
                assertThat(s.priority()).isEqualTo(NotificationService.Priority.HIGH);
            });
            assertThat(sent).extracting(Sent::message).anySatisfy(m -> assertThat(m)
                    .contains("Grade: " + grade.getLetterGrade() + " (90.0%)", "Comments: Well done"));
            assertThat(sent).extracting(Sent::message).anySatisfy(m -> assertThat(m)
                    .contains("(50.0%)", "Comments: No comments"));
        }

        @ParameterizedTest
        @CsvSource({"EMAIL, EMAIL", "BULK_NOTIFICATION, EMAIL", "SMS, SMS", "PUSH, PUSH"})
        void genericTypesAreInformationalOnTheirChannel(NotificationType type, DeliveryChannel channel)
                throws Exception {
            NotificationResult result = await(sender().sendNotificationWithRetry(
                    task(type, "x@u.edu"), 0, 0, TimeUnit.MILLISECONDS));

            assertThat(result.isSuccess()).isTrue();
            assertThat(sent).singleElement().satisfies(s -> {
                assertThat(s.type()).isEqualTo(NotificationService.NotificationType.INFO);
                assertThat(s.channels()).containsExactly(channel);
                assertThat(s.title()).isEqualTo("Subject");
                assertThat(s.message()).isEqualTo("Body");
                assertThat(s.priority()).isEqualTo(NotificationService.Priority.LOW);
            });
        }

        @Test
        void missingSubjectFallsBackToTheNotificationTypeName() throws Exception {
            NotificationTask noSubject = new NotificationTask(NotificationType.SMS, "x@u.edu", null, "Body",
                    Priority.URGENT, 0);

            await(sender().sendNotificationWithRetry(noSubject, 0, 0, TimeUnit.MILLISECONDS));

            assertThat(sent).singleElement().satisfies(s -> {
                assertThat(s.title()).isEqualTo(NotificationService.NotificationType.INFO.getDisplayName());
                assertThat(s.priority()).isEqualTo(NotificationService.Priority.URGENT);
            });
        }
    }

    @Nested
    class Failures {

        @Test
        void undeliveredNotificationIsReportedAndCounted() throws Exception {
            AsyncNotificationSender sender = sender(service(r -> true), 2, 1000);

            NotificationResult result = await(sender.sendEnrollmentConfirmationAsync(student("S1"), course()));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Notification failed to send");
            NotificationStatistics stats = sender.getStatistics();
            assertThat(stats.getTotalSent()).isZero();
            assertThat(stats.getTotalFailed()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isZero();
        }

        @Test
        void serviceExceptionBecomesAFailedResult() throws Exception {
            NotificationService service = mock(NotificationService.class);
            doAnswer(inv -> { throw new IllegalArgumentException("Invalid notification parameters"); })
                    .when(service).sendNotification(any(), any(), any(), any(), any(), anySet());
            AsyncNotificationSender sender = sender(service, 2, 1000);

            NotificationResult result = await(sender.sendEnrollmentConfirmationAsync(student("S1"), course()));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Notification error: Invalid notification parameters");
            assertThat(sender.getStatistics().getTotalFailed()).isEqualTo(1);
        }
    }

    @Nested
    class Bulk {

        @Test
        void aggregatesPerRecipientOutcomes() throws Exception {
            AsyncNotificationSender sender = sender(service(r -> r.equals("s3@u.edu")), 4, 10_000);
            List<Student> students = IntStream.range(0, 5).mapToObj(i -> student("S" + i)).toList();

            BulkNotificationResult result = await(sender.sendBulkNotificationsAsync(
                    students, "Closure", "Campus closed", Priority.URGENT));

            assertThat(result.getTotalSent()).isEqualTo(5);
            assertThat(result.getSuccessCount()).isEqualTo(4);
            assertThat(result.getFailureCount()).isEqualTo(1);
            assertThat(result.getSuccessRate()).isCloseTo(80.0, within(1e-9));
            assertThat(result.getResults()).extracting(r -> r.getTask().getRecipient())
                    .containsExactly("s0@u.edu", "s1@u.edu", "s2@u.edu", "s3@u.edu", "s4@u.edu");
            assertThat(sent).extracting(Sent::recipient).containsExactlyInAnyOrder(
                    "s0@u.edu", "s1@u.edu", "s2@u.edu", "s3@u.edu", "s4@u.edu");
            NotificationStatistics stats = sender.getStatistics();
            assertThat(stats.getTotalSent()).isEqualTo(4);
            assertThat(stats.getTotalFailed()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isCloseTo(80.0, within(1e-9));
            assertThat(stats.getActiveTasks()).isZero();
            assertThat(stats.getQueueSize()).isZero();
        }

        @Test
        void emptyRecipientListYieldsEmptyResult() throws Exception {
            BulkNotificationResult result = await(sender().sendBulkNotificationsAsync(
                    List.of(), "S", "M", Priority.LOW));

            assertThat(result.getTotalSent()).isZero();
            assertThat(result.getSuccessRate()).isZero();
            assertThat(result.getResults()).isEmpty();
        }
    }

    @Nested
    class Retry {

        @Test
        void retriesUntilDeliverySucceeds() throws Exception {
            AtomicInteger attempts = new AtomicInteger();
            AsyncNotificationSender sender = sender(service(r -> attempts.incrementAndGet() < 3), 2, 1000);

            NotificationResult result = await(sender.sendNotificationWithRetry(
                    task(NotificationType.EMAIL, "x@u.edu"), 5, 1, TimeUnit.MILLISECONDS));

            assertThat(result.isSuccess()).isTrue();
            assertThat(attempts.get()).isEqualTo(3);
            assertThat(sender.getStatistics().getTotalFailed()).isEqualTo(2);
            assertThat(sender.getStatistics().getTotalSent()).isEqualTo(1);
        }

        @Test
        void givesUpAfterMaxRetriesWithLastFailure() throws Exception {
            AsyncNotificationSender sender = sender(service(r -> true), 2, 1000);

            NotificationResult result = await(sender.sendNotificationWithRetry(
                    task(NotificationType.SMS, "x@u.edu"), 2, 1, TimeUnit.MILLISECONDS));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Notification failed to send");
            assertThat(sent).hasSize(3);
        }

        // Regression: the retry loop ran on the notification pool and blocked on a send queued
        // to that same pool, so with a single worker thread it deadlocked forever.
        @Test
        void singleWorkerThreadDoesNotDeadlock() throws Exception {
            AsyncNotificationSender sender = sender(service(r -> false), 1, 1000);

            NotificationResult result = await(sender.sendNotificationWithRetry(
                    task(NotificationType.EMAIL, "x@u.edu"), 1, 1, TimeUnit.MILLISECONDS));

            assertThat(result.isSuccess()).isTrue();
        }

        // Regression: as many concurrent retries as worker threads starved every send.
        @Test
        void concurrentRetriesSaturatingThePoolAllComplete() throws Exception {
            AsyncNotificationSender sender = sender(service(r -> false), 2, 10_000);

            List<CompletableFuture<NotificationResult>> results = IntStream.range(0, 8)
                    .mapToObj(i -> sender.sendNotificationWithRetry(
                            task(NotificationType.PUSH, "u" + i + "@u.edu"), 1, 1, TimeUnit.MILLISECONDS))
                    .toList();

            for (CompletableFuture<NotificationResult> r : results) {
                assertThat(await(r).isSuccess()).isTrue();
            }
            assertThat(sent).hasSize(8);
        }
    }

    @Nested
    class Scheduling {

        @Test
        void scheduledNotificationIsDeliveredAfterDelay() throws Exception {
            ScheduledFuture<NotificationResult> future = sender().scheduleNotification(
                    task(NotificationType.EMAIL, "x@u.edu"), 1, TimeUnit.MILLISECONDS);

            NotificationResult result = future.get(10, TimeUnit.SECONDS);

            assertThat(result.isSuccess()).isTrue();
            assertThat(sent).extracting(Sent::recipient).containsExactly("x@u.edu");
        }

        @Test
        void delayedNotificationFiringAfterShutdownReportsFailure() throws Exception {
            AsyncNotificationSender sender = sender();
            ScheduledFuture<NotificationResult> future = sender.scheduleNotification(
                    task(NotificationType.EMAIL, "x@u.edu"), 300, TimeUnit.MILLISECONDS);

            // the scheduler still runs already-delayed tasks after shutdown, but sends are rejected
            sender.shutdown();

            NotificationResult result = future.get(10, TimeUnit.SECONDS);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).startsWith("Scheduled notification failed: ");
            assertThat(sent).isEmpty();
        }

        @Test
        void recurringNotificationRepeatsUntilCancelled() throws Exception {
            CountDownLatch threeDeliveries = new CountDownLatch(3);
            AsyncNotificationSender sender = sender(service(r -> {
                threeDeliveries.countDown();
                return false;
            }), 2, 10_000);

            ScheduledFuture<?> recurring = sender.scheduleRecurringNotification(
                    new NotificationTask(NotificationType.PUSH, "x@u.edu", "Tick", "tock", Priority.NORMAL, 0),
                    0, 5, TimeUnit.MILLISECONDS);

            assertThat(threeDeliveries.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(recurring.cancel(false)).isTrue();
            assertThat(sent).hasSizeGreaterThanOrEqualTo(3)
                    .allSatisfy(s -> assertThat(s.title()).isEqualTo("Tick"));
        }
    }

    @Test
    void queuedNotificationsAreDeliveredByTheBackgroundProcessor() throws Exception {
        CountDownLatch delivered = new CountDownLatch(3);
        NotificationService service = service(r -> {
            delivered.countDown();
            return false;
        });
        AsyncNotificationSender sender = sender(service, 2, 10_000);

        for (int i = 0; i < 3; i++) {
            sender.queueNotification(task(NotificationType.EMAIL, "q" + i + "@u.edu"));
        }

        assertThat(delivered.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(sent).extracting(Sent::recipient).containsExactlyInAnyOrder("q0@u.edu", "q1@u.edu", "q2@u.edu");
    }

    @Test
    void rateLimiterSpreadsSendsBeyondTheLimitOverOneSecond() throws Exception {
        AsyncNotificationSender sender = sender(service(r -> false), 3, 2);

        List<CompletableFuture<NotificationResult>> results = IntStream.range(0, 3)
                .mapToObj(i -> sender.sendNotificationWithRetry(
                        task(NotificationType.EMAIL, "r" + i + "@u.edu"), 0, 0, TimeUnit.MILLISECONDS))
                .toList();
        for (CompletableFuture<NotificationResult> r : results) {
            assertThat(await(r).isSuccess()).isTrue();
        }

        List<Long> times = sent.stream().map(Sent::atNanos).sorted().toList();
        assertThat(times).hasSize(3);
        assertThat(TimeUnit.NANOSECONDS.toMillis(times.get(2) - times.get(0))).isGreaterThanOrEqualTo(900);
        assertThat(sender.getStatistics().getCurrentRate()).isLessThanOrEqualTo(3.0);
    }

    @Test
    void valueTypesExposeTheirFields() {
        NotificationTask t = new NotificationTask(NotificationType.SMS, "a@u.edu", "S", "M", Priority.HIGH, 2);

        assertThat(t.getRetryCount()).isEqualTo(2);
        assertThat(t.getCreatedAt()).isNotNull();
        assertThat(Priority.LOW.getLevel()).isLessThan(Priority.NORMAL.getLevel());
        assertThat(Priority.HIGH.getLevel()).isLessThan(Priority.URGENT.getLevel());
        assertThat(new NotificationStatistics(0, 0, 0, 0, 0).getSuccessRate()).isZero();
        assertThat(new BulkNotificationResult(0, 0, 0, List.of()).getSuccessRate()).isZero();
    }

    @Test
    void shutdownDeliversNotificationsStillWaitingInTheQueue() {
        AsyncNotificationSender sender = sender(service(r -> false), 2, 10_000);

        for (int i = 0; i < 50; i++) {
            sender.queueNotification(task(NotificationType.EMAIL, "pending" + i + "@u.edu"));
        }
        sender.shutdown();

        assertThat(sent).hasSize(50);
        assertThat(sender.getStatistics().getTotalSent()).isEqualTo(50);
    }

    @Test
    void queueingAfterShutdownIsRejectedInsteadOfSilentlyDropped() {
        AsyncNotificationSender sender = sender();
        sender.shutdown();

        assertThatThrownBy(() -> sender.queueNotification(task(NotificationType.EMAIL, "late@u.edu")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shutdownRejectsNewNotificationsAndIsIdempotent() {
        AsyncNotificationSender sender = sender();
        sender.shutdown();
        sender.shutdown();

        assertThatThrownBy(() -> sender.sendEnrollmentConfirmationAsync(student("S1"), course()))
                .isInstanceOf(RejectedExecutionException.class);
        assertThatThrownBy(() -> sender.sendNotificationWithRetry(task(NotificationType.SMS, "x@u.edu"),
                0, 0, TimeUnit.MILLISECONDS)).isInstanceOf(RejectedExecutionException.class);
    }
}
