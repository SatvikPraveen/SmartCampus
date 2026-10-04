package services;

import interfaces.EventListener.Event;
import interfaces.EventListener.EventHandlingException;
import interfaces.EventListener.EventPriority;
import interfaces.EventListener.ProcessingMode;
import models.Course;
import models.Enrollment;
import models.Grade;
import models.Grade.GradeComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import services.NotificationService.DeliveryChannel;
import services.NotificationService.Notification;
import services.NotificationService.NotificationEvent;
import services.NotificationService.NotificationListener;
import services.NotificationService.NotificationPreferences;
import services.NotificationService.NotificationStatistics;
import services.NotificationService.NotificationTemplate;
import services.NotificationService.NotificationType;
import services.NotificationService.Priority;
import services.NotificationService.ScheduledNotification;
import services.NotificationService.SystemNotificationStatistics;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    private Notification info(String user, String title) {
        return service.sendNotification(user, NotificationType.INFO, title, "msg " + title, Priority.NORMAL);
    }

    private static Event<Object> event(String type, Object payload) {
        return new Event<>(type, payload, "test");
    }

    /** Records every callback; invoked synchronously on the sending thread. */
    private static final class RecordingListener implements NotificationListener {
        final List<Notification> sent = new CopyOnWriteArrayList<>();
        final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void onNotificationSent(Notification notification) {
            sent.add(notification);
        }

        @Override
        public void onNotificationEvent(NotificationEvent event) {
            events.add(event.getEventType() + ":" + event.getNotification().getId());
            assertThat(event.getTimestamp()).isNotNull();
        }
    }

    @Nested
    class Sending {

        @Test
        void storesNotificationForUserWithInAppChannelByDefault() {
            Notification n = info("U1", "Hello");

            assertThat(n.getId()).startsWith("NOTIF_");
            assertThat(n.getUserId()).isEqualTo("U1");
            assertThat(n.getType()).isEqualTo(NotificationType.INFO);
            assertThat(n.getMessage()).isEqualTo("msg Hello");
            assertThat(n.getPriority()).isEqualTo(Priority.NORMAL);
            assertThat(n.getChannels()).containsExactly(DeliveryChannel.IN_APP);
            assertThat(n.isRead()).isFalse();
            assertThat(n.getReadAt()).isNull();
            assertThat(n.getMetadata()).isEmpty();
            assertThat(n.toString()).contains("Hello", "NORMAL");
            assertThat(service.getUserNotifications("U1")).containsExactly(n);
            assertThat(service.getUserNotifications("nobody")).isEmpty();
        }

        @Test
        void rejectsBlankUserOrTitle() {
            assertThatThrownBy(() -> info(" ", "t")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> info("U1", null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void userNotificationsAreNewestFirst() {
            info("U1", "a");
            info("U1", "b");
            info("U1", "c");

            assertThat(service.getUserNotifications("U1"))
                    .hasSize(3)
                    .isSortedAccordingTo(Comparator.comparing(Notification::getCreatedAt).reversed());
        }

        @Test
        void templatedNotificationSubstitutesParameters() {
            Notification n = service.sendTemplatedNotification("S1", "ENROLLMENT_CONFIRMATION",
                    Map.of("courseId", "CS101", "semester", "Fall"), Priority.HIGH);

            assertThat(n.getType()).isEqualTo(NotificationType.ACADEMIC);
            assertThat(n.getTitle()).isEqualTo("Enrollment Confirmed");
            assertThat(n.getMessage()).isEqualTo("You have been successfully enrolled in CS101 for Fall.");
            assertThat(n.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(n.getChannels()).containsExactlyInAnyOrder(DeliveryChannel.EMAIL, DeliveryChannel.IN_APP);
        }

        @Test
        void unknownTemplateIsRejected() {
            assertThatThrownBy(() -> service.sendTemplatedNotification("S1", "NOPE", Map.of(), Priority.LOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("NOPE");
        }

        @Test
        void bulkSendSkipsInvalidUserIds() {
            List<String> users = new ArrayList<>(List.of("U1", "U2", " "));
            users.add(null);
            List<Notification> sent = service.sendBulkNotification(users, NotificationType.ALERT, "Fire drill",
                    "Leave now", Priority.URGENT);

            assertThat(sent).extracting(Notification::getUserId).containsExactlyInAnyOrder("U1", "U2");
        }

        @Test
        void bulkSendProducesUniqueIdsSoNoNotificationIsLost() {
            // Regression: ids were "millis_nanoTime%10000", which collided under load and
            // silently overwrote earlier notifications in the store.
            List<String> users = IntStream.range(0, 500).mapToObj(i -> "U" + i).toList();
            List<Notification> sent = service.sendBulkNotification(users, NotificationType.INFO, "t", "m", Priority.LOW);

            assertThat(sent).extracting(Notification::getId).doesNotHaveDuplicates().hasSize(500);
            assertThat(service.getSystemStatistics().getTotalNotifications()).isEqualTo(501); // + startup notice
        }

        @Test
        void concurrentSendsToTheSameUserAreAllRecorded() throws Exception {
            // Regression: the per-user id list was a plain ArrayList mutated from many threads.
            int n = 200;
            CountDownLatch start = new CountDownLatch(1);
            List<CompletableFuture<Notification>> futures = IntStream.range(0, n)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try {
                            start.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return info("SAME", "t" + i);
                    }))
                    .toList();
            start.countDown();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

            assertThat(service.getUserNotifications("SAME")).hasSize(n);
        }

        @Test
        void asyncSendCompletesWithStoredNotification() throws Exception {
            Notification n = service.sendNotificationAsync("U1", NotificationType.SUCCESS, "Done", "ok", Priority.LOW)
                    .get(5, TimeUnit.SECONDS);

            assertThat(service.getUserNotifications("U1")).containsExactly(n);
        }
    }

    @Nested
    class Scheduling {

        @Test
        void dueNotificationIsDeliveredByTheScheduler() throws Exception {
            CountDownLatch delivered = new CountDownLatch(1);
            service.addListener(new NotificationListener() {
                @Override
                public void onNotificationSent(Notification notification) {
                    if (notification.getUserId().equals("U1")) {
                        delivered.countDown();
                    }
                }

                @Override
                public void onNotificationEvent(NotificationEvent event) {
                }
            });

            ScheduledNotification scheduled = service.scheduleNotification("U1", NotificationType.REMINDER,
                    "Due", "now", Priority.HIGH, LocalDateTime.now());

            assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(service.getUserNotifications("U1")).singleElement()
                    .satisfies(n -> assertThat(n.getType()).isEqualTo(NotificationType.REMINDER));
            assertThat(scheduled.getUserId()).isEqualTo("U1");
            assertThat(scheduled.getTitle()).isEqualTo("Due");
            assertThat(scheduled.getMessage()).isEqualTo("now");
            assertThat(scheduled.getType()).isEqualTo(NotificationType.REMINDER);
            assertThat(scheduled.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(scheduled.getDeliveryTime()).isNotNull();
        }

        @Test
        void futureNotificationCanBeCancelled() {
            ScheduledNotification scheduled = service.scheduleNotification("U1", NotificationType.REMINDER,
                    "Later", "m", Priority.LOW, LocalDateTime.now().plusHours(1));

            assertThat(scheduled.isCancelled()).isFalse();
            assertThat(scheduled.cancel()).isTrue();
            assertThat(scheduled.isCancelled()).isTrue();
            assertThat(service.getUserNotifications("U1")).isEmpty();
        }

        @Test
        void unscheduledNotificationCannotBeCancelled() {
            ScheduledNotification detached = new ScheduledNotification("U1", NotificationType.INFO, "t", "m",
                    Priority.LOW, LocalDateTime.now());
            assertThat(detached.cancel()).isFalse();
            assertThat(detached.isCancelled()).isFalse();
        }
    }

    @Nested
    class ReadingAndDeleting {

        private RecordingListener listener;

        @BeforeEach
        void register() {
            listener = new RecordingListener();
            service.addListener(listener);
        }

        @Test
        void listenerSeesSendsReadsAndDeletes() {
            Notification n = info("U1", "a");
            assertThat(listener.sent).containsExactly(n);

            assertThat(service.markAsRead(n.getId())).isTrue();
            assertThat(n.isRead()).isTrue();
            assertThat(n.getReadAt()).isNotNull();
            assertThat(service.markAsRead(n.getId())).isFalse();
            assertThat(service.markAsRead("missing")).isFalse();

            assertThat(service.deleteNotification(n.getId())).isTrue();
            assertThat(service.deleteNotification(n.getId())).isFalse();
            assertThat(service.getUserNotifications("U1")).isEmpty();
            assertThat(listener.events).containsExactly("NOTIFICATION_READ:" + n.getId(),
                    "NOTIFICATION_DELETED:" + n.getId());

            service.removeListener(listener);
            info("U1", "b");
            assertThat(listener.sent).hasSize(1);
        }

        @Test
        void markAllAsReadOnlyTouchesUnread() {
            Notification a = info("U1", "a");
            info("U1", "b");
            info("U1", "c");
            service.markAsRead(a.getId());

            assertThat(service.getUnreadNotifications("U1")).hasSize(2);
            assertThat(service.markAllAsRead("U1")).isEqualTo(2);
            assertThat(service.getUnreadNotifications("U1")).isEmpty();
            assertThat(service.markAllAsRead("U1")).isZero();
        }

        @Test
        void failingListenerDoesNotBreakSending() {
            service.addListener(new NotificationListener() {
                @Override
                public void onNotificationSent(Notification notification) {
                    throw new IllegalStateException("boom");
                }

                @Override
                public void onNotificationEvent(NotificationEvent event) {
                    throw new IllegalStateException("boom");
                }
            });

            Notification n = info("U1", "a");
            assertThat(service.markAsRead(n.getId())).isTrue();
            assertThat(listener.sent).containsExactly(n);
        }

        @Test
        void filtersByTypeAndPriority() {
            info("U1", "a");
            service.sendNotification("U1", NotificationType.WARNING, "w", "m", Priority.HIGH);

            assertThat(service.getNotificationsByType("U1", NotificationType.WARNING)).extracting(Notification::getTitle)
                    .containsExactly("w");
            assertThat(service.getNotificationsByPriority("U1", Priority.NORMAL)).extracting(Notification::getTitle)
                    .containsExactly("a");
        }

        @Test
        void cleanupRemovesNotificationsOlderThanCutoff() {
            info("U1", "a");
            info("U2", "b");

            assertThat(service.cleanupOldNotifications(LocalDateTime.now().minusDays(1))).isZero();
            assertThat(service.cleanupOldNotifications(LocalDateTime.now().plusSeconds(1))).isEqualTo(3);
            assertThat(service.getSystemStatistics().getTotalNotifications()).isZero();
        }
    }

    @Nested
    class Preferences {

        @Test
        void defaultsEnableInAppEverywhereAndEmailSmsSelectively() {
            NotificationPreferences prefs = service.getUserPreferences("U1");

            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.IN_APP)).isTrue();
            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.EMAIL)).isFalse();
            assertThat(prefs.isChannelEnabled(NotificationType.ACADEMIC, DeliveryChannel.EMAIL)).isTrue();
            assertThat(prefs.isChannelEnabled(NotificationType.ALERT, DeliveryChannel.EMAIL)).isTrue();
            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.SMS)).isFalse();
            assertThat(prefs.isChannelEnabled(NotificationType.EMERGENCY, DeliveryChannel.SMS)).isTrue();
        }

        @Test
        void updatePreferencePersistsForUser() {
            service.updatePreference("U1", NotificationType.INFO, DeliveryChannel.EMAIL, true);

            assertThat(service.getUserPreferences("U1").isChannelEnabled(NotificationType.INFO, DeliveryChannel.EMAIL))
                    .isTrue();
            assertThat(service.getUserPreferences("U2").isChannelEnabled(NotificationType.INFO, DeliveryChannel.EMAIL))
                    .isFalse();
        }

        @Test
        void globalSwitchAndMutingOverrideChannels() {
            NotificationPreferences prefs = new NotificationPreferences();
            assertThat(prefs.isGlobalEnabled()).isTrue();
            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.PUSH)).isTrue();

            prefs.muteType(NotificationType.INFO);
            assertThat(prefs.getMutedTypes()).containsExactly(NotificationType.INFO);
            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.PUSH)).isFalse();
            prefs.unmuteType(NotificationType.INFO);
            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.PUSH)).isTrue();

            prefs.setGlobalEnabled(false);
            assertThat(prefs.isChannelEnabled(NotificationType.INFO, DeliveryChannel.PUSH)).isFalse();

            service.setUserPreferences("U1", prefs);
            assertThat(service.getUserPreferences("U1")).isSameAs(prefs);
        }
    }

    @Nested
    class Templates {

        @Test
        void createGetAndDelete() {
            NotificationTemplate t = new NotificationTemplate("T1", NotificationType.REMINDER, "Reminder",
                    "Hi {{name}}", "Details for {{name}}", Set.of(DeliveryChannel.PUSH));
            service.createTemplate("T1", t);

            assertThat(service.getTemplate("T1")).isSameAs(t);
            assertThat(t.getTemplateId()).isEqualTo("T1");
            assertThat(t.getTitleTemplate()).isEqualTo("Reminder");
            assertThat(t.getDetailTemplate()).isEqualTo("Details for {{name}}");
            assertThat(service.getTemplate("GRADE_NOTIFICATION")).isNotNull();

            Notification n = service.sendTemplatedNotification("U1", "T1", Map.of("name", "Ada"), Priority.LOW);
            assertThat(n.getTitle()).isEqualTo("Reminder");
            assertThat(n.getMessage()).isEqualTo("Hi Ada");
            assertThat(n.getType()).isEqualTo(NotificationType.REMINDER);
            assertThat(n.getChannels()).containsExactly(DeliveryChannel.PUSH);

            assertThat(service.deleteTemplate("T1")).isTrue();
            assertThat(service.deleteTemplate("T1")).isFalse();
            assertThat(service.getTemplate("T1")).isNull();
        }
    }

    @Nested
    class EventHandling {

        @Test
        void studentEnrolledSendsEnrollmentConfirmation() throws Exception {
            service.handleEvent(event("STUDENT_ENROLLED", Enrollment.createEnrollment("S1", "CS101", "Fall", 2025)));

            assertThat(service.getUserNotifications("S1")).singleElement()
                    .satisfies(n -> assertThat(n.getMessage()).contains("CS101", "Fall"));
        }

        @Test
        void gradeAssignedSendsGradeNotification() throws Exception {
            Grade g = new Grade("G1", "E1", "S1", "CS101", "Midterm", GradeComponent.EXAM);
            g.setPointsPossible(100);
            g.submitAssignment();
            g.gradeAssignment(95, "P1", "");

            service.handleEvent(event("GRADE_ASSIGNED", g));

            assertThat(service.getUserNotifications("S1")).singleElement()
                    .satisfies(n -> {
                        assertThat(n.getTitle()).isEqualTo("Grade Posted");
                        assertThat(n.getMessage()).isEqualTo("Grade posted for Midterm");
                    });
        }

        @Test
        void courseCancelledAndMaintenanceBroadcast() throws Exception {
            service.handleEvent(event("COURSE_CANCELLED", new Course("C1", "C1", "Compilers", "d", 3, "CS")));
            service.handleEvent(event("SYSTEM_MAINTENANCE", null));

            assertThat(service.getUserNotifications("ALL_STUDENTS")).singleElement()
                    .satisfies(n -> {
                        assertThat(n.getMessage()).contains("Compilers");
                        assertThat(n.getPriority()).isEqualTo(Priority.HIGH);
                    });
            assertThat(service.getUserNotifications("ALL_USERS")).hasSize(1);
        }

        @Test
        void wrongPayloadTypesAndGenericEventsSendNothing() throws Exception {
            service.handleEvent(event("STUDENT_ENROLLED", "not an enrollment"));
            service.handleEvent(event("GRADE_ASSIGNED", 42));
            service.handleEvent(event("COURSE_CANCELLED", null));
            service.handleEvent(event("STUDENT_DROPPED", null));

            assertThat(service.getSystemStatistics().getTotalNotifications()).isEqualTo(1); // startup notice only
        }

        @Test
        void processingFailureIsWrapped() {
            Grade ungraded = new Grade("G1", "E1", "S1", "CS101", "Draft", GradeComponent.QUIZ); // no letter grade

            assertThatThrownBy(() -> service.handleEvent(event("GRADE_ASSIGNED", ungraded)))
                    .isInstanceOf(EventHandlingException.class)
                    .hasMessage("Failed to process event");
        }

        @Test
        void asyncHandlingCompletesAndSwallowsFailures() throws Exception {
            service.handleEventAsync(event("SYSTEM_MAINTENANCE", null)).get(5, TimeUnit.SECONDS);
            assertThat(service.getUserNotifications("ALL_USERS")).hasSize(1);

            Grade ungraded = new Grade("G1", "E1", "S1", "CS101", "Draft", GradeComponent.QUIZ);
            assertThat(service.handleEventAsync(event("GRADE_ASSIGNED", ungraded)).get(5, TimeUnit.SECONDS)).isNull();
        }

        @Test
        void listenerContract() {
            assertThat(service.canHandle("STUDENT_ENROLLED")).isTrue();
            assertThat(service.canHandle("SYSTEM_MAINTENANCE")).isTrue();
            assertThat(service.canHandle("ENROLLMENT_WAITLISTED")).isTrue();
            assertThat(service.canHandle("PROFESSOR_HIRED")).isTrue();
            assertThat(service.canHandle("COURSE_X")).isTrue();
            assertThat(service.canHandle("GRADE_X")).isTrue();
            assertThat(service.canHandle("PAYMENT_RECEIVED")).isFalse();
            assertThat(service.canHandle(event("GRADE_UPDATED", null))).isTrue();
            assertThat(service.getPriority()).isEqualTo(EventPriority.NORMAL);
            assertThat(service.getProcessingMode()).isEqualTo(ProcessingMode.ASYNCHRONOUS);
            assertThat(service.getSupportedEventTypes()).contains("STUDENT_ENROLLED", "SYSTEM_MAINTENANCE").hasSize(8);
        }
    }

    @Nested
    class Delivery {

        @Test
        void deliversOnEveryChannelTheUserHasEnabled() {
            Notification n = service.sendNotification("U1", NotificationType.EMERGENCY, "Evacuate", "now",
                    Priority.CRITICAL, Set.of(DeliveryChannel.values()));

            // Defaults for EMERGENCY: email off, every other channel on
            await().atMost(Duration.ofSeconds(5)).until(() -> n.getDeliveryRecords().size() == 5);
            assertThat(n.getDeliveryRecords())
                    .allSatisfy(r -> {
                        assertThat(r.isSuccessful()).isTrue();
                        assertThat(r.getErrorMessage()).isNull();
                        assertThat(r.getDeliveredAt()).isNotNull();
                    })
                    .extracting(r -> r.getChannel())
                    .containsExactlyInAnyOrder(DeliveryChannel.IN_APP, DeliveryChannel.SMS, DeliveryChannel.PUSH,
                            DeliveryChannel.PORTAL, DeliveryChannel.DASHBOARD);
        }

        @Test
        void emailIsDeliveredForAcademicNotifications() {
            Notification n = service.sendNotification("U1", NotificationType.ACADEMIC, "Grades", "posted",
                    Priority.NORMAL, Set.of(DeliveryChannel.EMAIL));

            await().atMost(Duration.ofSeconds(5)).until(() -> n.getDeliveryRecords().size() == 1);
            assertThat(n.getDeliveryRecords().get(0).getChannel()).isEqualTo(DeliveryChannel.EMAIL);
        }

        @Test
        void disabledUserReceivesNothing() {
            NotificationPreferences off = new NotificationPreferences();
            off.setGlobalEnabled(false);
            service.setUserPreferences("MUTED", off);

            Notification muted = info("MUTED", "quiet");
            Notification sentinel = info("U1", "after");

            // Single FIFO consumer: once the later notification is delivered, the muted one was processed.
            await().atMost(Duration.ofSeconds(5)).until(() -> !sentinel.getDeliveryRecords().isEmpty());
            assertThat(muted.getDeliveryRecords()).isEmpty();
        }
    }

    @Nested
    class Statistics {

        @Test
        void userStatistics() {
            Notification a = info("U1", "a");
            service.sendNotification("U1", NotificationType.WARNING, "w", "m", Priority.HIGH);
            service.markAsRead(a.getId());

            NotificationStatistics stats = service.getUserStatistics("U1");
            assertThat(stats.getUserId()).isEqualTo("U1");
            assertThat(stats.getTotalNotifications()).isEqualTo(2);
            assertThat(stats.getReadCount()).isEqualTo(1);
            assertThat(stats.getUnreadCount()).isEqualTo(1);
            assertThat(stats.getReadPercentage()).isEqualTo(50.0);
            assertThat(stats.getByType()).containsEntry(NotificationType.INFO, 1L).containsEntry(NotificationType.WARNING, 1L);
            assertThat(stats.getByPriority()).containsEntry(Priority.HIGH, 1L);

            assertThat(service.getUserStatistics("nobody").getReadPercentage()).isZero();
        }

        @Test
        void systemStatistics() {
            info("U1", "a");
            info("U1", "b");
            info("U2", "c");

            SystemNotificationStatistics stats = service.getSystemStatistics();
            assertThat(stats.getTotalNotifications()).isEqualTo(4); // includes startup notice for SYSTEM
            assertThat(stats.getTotalUsers()).isEqualTo(3);
            assertThat(stats.getAverageNotificationsPerUser()).isCloseTo(4.0 / 3, within(1e-9));
            assertThat(stats.getPendingNotifications()).isBetween(0L, 4L);
            assertThat(stats.getByType()).containsEntry(NotificationType.SYSTEM, 1L).containsEntry(NotificationType.INFO, 3L);
            assertThat(stats.getByPriority()).containsEntry(Priority.LOW, 1L);
        }

        @Test
        void systemStatisticsAfterCleanup() {
            service.cleanupOldNotifications(LocalDateTime.now().plusSeconds(1));

            SystemNotificationStatistics stats = service.getSystemStatistics();
            assertThat(stats.getTotalNotifications()).isZero();
            assertThat(stats.getAverageNotificationsPerUser()).isZero();
            assertThat(stats.getByType()).isEmpty();
        }

        @Test
        void enumsExposeDisplayMetadata() {
            assertThat(NotificationType.EMERGENCY.getDisplayName()).isEqualTo("Emergency");
            assertThat(NotificationType.INFO.getIcon()).isNotBlank();
            assertThat(DeliveryChannel.PORTAL.getDisplayName()).isEqualTo("Portal Message");
            assertThat(Priority.CRITICAL.getLevel()).isEqualTo(5);
            assertThat(Priority.LOW.getDisplayName()).isEqualTo("Low");
            assertThat(Set.of(NotificationType.values()).stream().map(NotificationType::getDisplayName)
                    .collect(Collectors.toSet())).hasSize(NotificationType.values().length);
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void shutdownReturnsPromptly() {
            // Regression: the delivery loop never observed shutdown, so awaitTermination
            // always blocked for its full 30-second timeout.
            assertTimeoutPreemptively(Duration.ofSeconds(10), service::shutdown);
        }
    }
}
