package patterns;

import events.Event;
import models.Course;
import models.Grade;
import models.Grade.GradeComponent;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import patterns.EventManager.CourseCreatedEvent;
import patterns.EventManager.ErrorEvent;
import patterns.EventManager.EventExecution;
import patterns.EventManager.EventListener;
import patterns.EventManager.EventManagerFactory;
import patterns.EventManager.EventPriority;
import patterns.EventManager.EventStatistics;
import patterns.EventManager.GradeUpdatedEvent;
import patterns.EventManager.StudentEnrolledEvent;
import patterns.EventManager.UserLoginEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests use factory-created managers. The {@link EventManager#getInstance()} singleton has no reset hook, so
 * its tests only check identity and leave no listeners behind.
 */
class EventManagerTest {

    private EventManager events;

    @BeforeEach
    void setUp() {
        events = EventManagerFactory.createTestEventManager(); // synchronous
    }

    @AfterEach
    void tearDown() {
        events.shutdown();
    }

    private static Student student() {
        return new Student("STU000001", "Ada", "Lovelace", "ada@university.edu", null, "STU000001", "CS",
                           Student.AcademicYear.FRESHMAN);
    }

    private static Course course() {
        return new Course("CS101", "CS101", "Intro", "desc", 3, "DEPT_CS");
    }

    private static Professor professor() {
        return new Professor("PROF00001", "Alan", "Turing", "alan@university.edu", null, "PROF00001",
                             "DEPT_CS", Professor.AcademicRank.ASSISTANT, "Computation");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(Event event) {
        return (Map<String, Object>) event.getPayload();
    }

    private static StudentEnrolledEvent enrolled() {
        return new StudentEnrolledEvent(student(), course(), new Date());
    }

    /** A named listener that appends its name to a shared log. */
    private static EventListener<StudentEnrolledEvent> recorder(String name, List<String> log) {
        return new EventListener<>() {
            @Override public void handleEvent(StudentEnrolledEvent event) { log.add(name); }
            @Override public Class<StudentEnrolledEvent> getEventType() { return StudentEnrolledEvent.class; }
        };
    }

    @Nested
    class Subscriptions {

        @Test
        void publishDeliversToSubscribersOfExactType() {
            List<StudentEnrolledEvent> received = new ArrayList<>();
            List<UserLoginEvent> logins = new ArrayList<>();
            events.onStudentEnrolled(received::add);
            events.onUserLogin(logins::add);
            StudentEnrolledEvent event = enrolled();

            events.publish(event);

            assertThat(received).containsExactly(event);
            assertThat(logins).isEmpty();
        }

        @Test
        void publishNullOrWithoutListenersIsHarmless() {
            events.publish(null);
            events.publish(enrolled());

            assertThat(events.getEventStatistics()).containsOnlyKeys("StudentEnrolledEvent");
        }

        @Test
        void listenerBookkeeping() {
            List<String> log = new ArrayList<>();
            EventListener<StudentEnrolledEvent> a = recorder("a", log);
            events.subscribe(StudentEnrolledEvent.class, a);
            events.onGradeUpdated(e -> { });
            events.onCourseCreated(e -> { });

            assertThat(events.getListenerCount(StudentEnrolledEvent.class)).isEqualTo(1);
            assertThat(events.getRegisteredEventTypes())
                .containsExactlyInAnyOrder(StudentEnrolledEvent.class, GradeUpdatedEvent.class, CourseCreatedEvent.class);

            events.clearListeners(GradeUpdatedEvent.class);
            assertThat(events.getListenerCount(GradeUpdatedEvent.class)).isZero();

            events.unsubscribe(StudentEnrolledEvent.class, a);
            assertThat(events.getRegisteredEventTypes()).containsExactly(CourseCreatedEvent.class);
            events.publish(enrolled());
            assertThat(log).isEmpty();

            events.clearAllListeners();
            assertThat(events.getRegisteredEventTypes()).isEmpty();
        }

        @Test
        void unsubscribeOfUnknownTypeOrListenerIsNoOp() {
            List<String> log = new ArrayList<>();
            events.subscribe(StudentEnrolledEvent.class, recorder("a", log));

            events.unsubscribe(UserLoginEvent.class, new EventListener<UserLoginEvent>() {
                @Override public void handleEvent(UserLoginEvent event) { }
                @Override public Class<UserLoginEvent> getEventType() { return UserLoginEvent.class; }
            });
            events.unsubscribe(StudentEnrolledEvent.class, recorder("other", log));

            assertThat(events.getListenerCount(StudentEnrolledEvent.class)).isEqualTo(1);
        }

        @Test
        void priorityListenersRunHighestFirst() {
            List<String> log = new ArrayList<>();
            events.subscribe(StudentEnrolledEvent.class, recorder("low", log), EventPriority.LOW);
            events.subscribe(StudentEnrolledEvent.class, recorder("normal", log));
            events.subscribe(StudentEnrolledEvent.class, recorder("highest", log), EventPriority.HIGHEST);
            events.subscribe(StudentEnrolledEvent.class, recorder("lowest", log), EventPriority.LOWEST);

            events.publish(enrolled());

            assertThat(log).containsExactly("highest", "normal", "low", "lowest");
        }

        // Regression: listeners subscribed with a priority are stored wrapped, and unsubscribe() compared
        // against the wrapper, so such listeners could never be removed.
        @Test
        void priorityListenerCanBeUnsubscribed() {
            List<String> log = new ArrayList<>();
            EventListener<StudentEnrolledEvent> listener = recorder("p", log);
            events.subscribe(StudentEnrolledEvent.class, listener, EventPriority.HIGH);

            events.unsubscribe(StudentEnrolledEvent.class, listener);
            events.publish(enrolled());

            assertThat(log).isEmpty();
            assertThat(events.getListenerCount(StudentEnrolledEvent.class)).isZero();
            assertThat(events.getRegisteredEventTypes()).doesNotContain(StudentEnrolledEvent.class);
        }

        @Test
        void consumerListenerReportsItsEventType() {
            AtomicReference<EventListener<?>> captured = new AtomicReference<>();
            events.onUserLogin(e -> { });
            events.publish(new UserLoginEvent(student(), "10.0.0.1", true));

            captured.set(events.getRecentEventExecutions(1).get(0).getListener());

            assertThat(captured.get().getEventType()).isEqualTo(UserLoginEvent.class);
        }
    }

    @Nested
    class ErrorsAndStatistics {

        @Test
        void failingListenerDoesNotStopOthersAndPublishesErrorEvent() {
            List<String> log = new ArrayList<>();
            List<ErrorEvent> errors = new ArrayList<>();
            events.subscribe(StudentEnrolledEvent.class, (StudentEnrolledEvent e) -> {
                throw new IllegalStateException("boom");
            });
            events.subscribe(StudentEnrolledEvent.class, recorder("ok", log), EventPriority.LOWEST);
            events.onSystemError(errors::add);
            StudentEnrolledEvent event = enrolled();

            events.publish(event);

            assertThat(log).containsExactly("ok");
            assertThat(errors).singleElement().satisfies(err -> {
                assertThat(err.getOriginalEvent()).isSameAs(event);
                assertThat(err.getError()).hasMessage("boom");
                assertThat(err.getFailedListener()).isNotNull();
                assertThat(err.getErrorTime()).isNotNull();
                assertThat(err.isValid()).isTrue();
                assertThat(err.getDescription()).isEqualTo("Error processing StudentEnrolledEvent: boom");
            });
        }

        @Test
        void failingErrorListenerDoesNotRecurse() {
            events.onSystemError(e -> { throw new IllegalStateException("again"); });
            events.subscribe(StudentEnrolledEvent.class, (StudentEnrolledEvent e) -> {
                throw new IllegalStateException("boom");
            });

            events.publish(enrolled());

            assertThat(events.getEventStatistics().get("ErrorEvent").getTotalCount()).isEqualTo(1);
        }

        // Regression: EventStatistics.errorCount was never incremented, so error rates were always 0%.
        @Test
        void statisticsCountListenerFailures() {
            events.subscribe(StudentEnrolledEvent.class, (StudentEnrolledEvent e) -> {
                throw new IllegalStateException("boom");
            });
            events.subscribe(StudentEnrolledEvent.class, recorder("ok", new ArrayList<>()));

            events.publish(enrolled());
            events.publish(enrolled());

            EventStatistics stats = events.getEventStatistics().get("StudentEnrolledEvent");
            assertThat(stats.getTotalCount()).isEqualTo(2);
            assertThat(stats.getErrorCount()).isEqualTo(2);
            assertThat(stats.getErrorRate()).isEqualTo(100.0);
            assertThat(stats.getFirstOccurrence()).isNotNull();
            assertThat(stats.getLastOccurrence()).isAfterOrEqualTo(stats.getFirstOccurrence());
            assertThat(stats.toString()).contains("total=2").contains("errors=2");
        }

        @Test
        void recentExecutionsRecordOutcome() {
            events.subscribe(StudentEnrolledEvent.class, recorder("ok", new ArrayList<>()));
            events.publish(enrolled());
            events.subscribe(StudentEnrolledEvent.class, (StudentEnrolledEvent e) -> {
                throw new IllegalStateException("bad");
            });
            events.publish(enrolled());

            List<EventExecution> recent = events.getRecentEventExecutions(10);

            assertThat(recent).hasSize(3);
            assertThat(recent).filteredOn(EventExecution::hasError).singleElement().satisfies(x -> {
                assertThat(x.getError()).hasMessage("bad");
                assertThat(x.getProcessingTime()).isZero();
                assertThat(x.getEvent()).isInstanceOf(StudentEnrolledEvent.class);
                assertThat(x.toString()).contains("event=StudentEnrolledEvent").contains("error=bad");
            });
            assertThat(recent).filteredOn(x -> !x.hasError()).allSatisfy(x ->
                assertThat(x.toString()).contains("error=none"));
            assertThat(events.getRecentEventExecutions(1)).hasSize(1);
        }

        @Test
        void emptyStatisticsHaveZeroErrorRate() {
            assertThat(new EventStatistics().getErrorRate()).isZero();
        }
    }

    @Nested
    class Async {

        @Test
        @Timeout(10)
        void productionManagerDeliversOnWorkerThreads() throws InterruptedException {
            EventManager async = EventManagerFactory.createProductionEventManager();
            try {
                CountDownLatch delivered = new CountDownLatch(2);
                List<String> threads = Collections.synchronizedList(new ArrayList<>());
                async.onStudentEnrolled(e -> { threads.add(Thread.currentThread().getName()); delivered.countDown(); });
                async.onStudentEnrolled(e -> { threads.add(Thread.currentThread().getName()); delivered.countDown(); });

                async.publish(enrolled());

                assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(threads).hasSize(2).allMatch(name -> name.startsWith("EventManager-"));
            } finally {
                async.shutdown();
            }
            assertThat(async.getRegisteredEventTypes()).isEmpty();
        }

        @Test
        @Timeout(10)
        void delayedPublishOnAsyncManager() throws InterruptedException {
            EventManager async = EventManagerFactory.createProductionEventManager();
            try {
                CountDownLatch delivered = new CountDownLatch(1);
                async.onStudentEnrolled(e -> delivered.countDown());

                async.publishDelayed(enrolled(), 10, TimeUnit.MILLISECONDS);

                assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                async.shutdown();
            }
        }

        @Test
        @Timeout(10)
        void delayedPublishOnSyncManagerUsesTimer() throws InterruptedException {
            CountDownLatch delivered = new CountDownLatch(1);
            events.onStudentEnrolled(e -> delivered.countDown());

            events.publishDelayed(enrolled(), 10, TimeUnit.MILLISECONDS);

            assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        }

        @Test
        @Timeout(10)
        void smartCampusManagerHasDefaultListeners() {
            EventManager campus = EventManagerFactory.createSmartCampusEventManager();
            try {
                assertThat(campus.getRegisteredEventTypes()).containsExactlyInAnyOrder(
                    StudentEnrolledEvent.class, GradeUpdatedEvent.class, CourseCreatedEvent.class,
                    UserLoginEvent.class, ErrorEvent.class);
                campus.publish(enrolled());
                campus.publish(new UserLoginEvent(student(), "1.2.3.4", false));
                campus.publish(new CourseCreatedEvent(course(), professor()));
            } finally {
                campus.shutdown(); // waits for the queued default listeners to finish
            }
            assertThat(campus.getEventStatistics()).containsKeys("StudentEnrolledEvent", "UserLoginEvent", "CourseCreatedEvent");
        }

        @Test
        void singletonReturnsSameInstance() {
            assertThat(EventManager.getInstance()).isSameAs(EventManager.getInstance(false));
        }
    }

    @Nested
    class EventTypes {

        @Test
        void studentEnrolledEvent() {
            Date when = new Date();
            StudentEnrolledEvent e = new StudentEnrolledEvent(student(), course(), when);

            assertThat(e.getStudent().getStudentId()).isEqualTo("STU000001");
            assertThat(e.getCourse().getCourseId()).isEqualTo("CS101");
            assertThat(e.getEnrollmentDate()).isSameAs(when);
            assertThat(e.getCategory()).isEqualTo(Event.Category.DOMAIN);
            assertThat(e.isValid()).isTrue();
            assertThat(e.getDescription()).isEqualTo("Student Ada Lovelace enrolled in Intro");
            assertThat(payload(e))
                .containsEntry("studentId", "STU000001").containsEntry("courseId", "CS101");

            StudentEnrolledEvent empty = new StudentEnrolledEvent(null, null, null);
            assertThat(empty.isValid()).isFalse();
            assertThat(empty.getDescription()).isEqualTo("Student unknown enrolled in unknown");
            assertThat(payload(empty)).containsEntry("studentId", null);
        }

        @Test
        void gradeUpdatedEvent() {
            Grade grade = new Grade("G1", "E1", "STU000001", "CS101", "Exam", GradeComponent.EXAM);
            GradeUpdatedEvent e = new GradeUpdatedEvent(student(), course(), grade, null);

            assertThat(e.getNewGrade()).isSameAs(grade);
            assertThat(e.getOldGrade()).isNull();
            assertThat(e.getStudent()).isNotNull();
            assertThat(e.getCourse()).isNotNull();
            assertThat(e.getCategory()).isEqualTo(Event.Category.DOMAIN);
            assertThat(e.isValid()).isTrue();
            assertThat(e.getDescription()).startsWith("Grade updated for Ada Lovelace in Intro: ");
            assertThat(payload(e)).containsEntry("oldGrade", null).containsKey("newGrade");

            GradeUpdatedEvent empty = new GradeUpdatedEvent(null, null, null, grade);
            assertThat(empty.isValid()).isFalse();
            assertThat(empty.getDescription()).isEqualTo("Grade updated for unknown in unknown: N/A");
            assertThat(payload(empty)).containsEntry("studentId", null).containsKey("oldGrade");
        }

        @Test
        void courseCreatedEvent() {
            CourseCreatedEvent e = new CourseCreatedEvent(course(), professor());

            assertThat(e.getCourse().getCourseId()).isEqualTo("CS101");
            assertThat(e.getProfessor().getProfessorId()).isEqualTo("PROF00001");
            assertThat(e.getCategory()).isEqualTo(Event.Category.DOMAIN);
            assertThat(e.isValid()).isTrue();
            assertThat(e.getDescription()).isEqualTo("New course created: Intro by Alan Turing");
            assertThat(payload(e)).containsEntry("courseId", "CS101").containsEntry("professorId", "PROF00001");

            CourseCreatedEvent empty = new CourseCreatedEvent(null, null);
            assertThat(empty.isValid()).isFalse();
            assertThat(empty.getDescription()).isEqualTo("New course created: unknown by unknown");
            assertThat(payload(empty)).containsEntry("courseId", null);
        }

        @Test
        void userLoginEvent() {
            UserLoginEvent ok = new UserLoginEvent(student(), "10.0.0.1", true);
            UserLoginEvent failed = new UserLoginEvent(null, "10.0.0.2", false);

            assertThat(ok.getUser().getUserId()).isEqualTo("STU000001");
            assertThat(ok.getIpAddress()).isEqualTo("10.0.0.1");
            assertThat(ok.isSuccessful()).isTrue();
            assertThat(ok.getCategory()).isEqualTo(Event.Category.AUDIT);
            assertThat(ok.isValid()).isTrue();
            assertThat(ok.getDescription()).isEqualTo("User login: Ada Lovelace from 10.0.0.1 - SUCCESS");
            assertThat(payload(ok)).containsEntry("userId", "STU000001").containsEntry("successful", true);
            assertThat(failed.isValid()).isFalse();
            assertThat(failed.getDescription()).isEqualTo("User login: unknown from 10.0.0.2 - FAILED");
            assertThat(payload(failed)).containsEntry("userId", null);
        }

        @Test
        void copiesKeepTypeSpecificFields() {
            Grade grade = new Grade("G1", "E1", "STU000001", "CS101", "Exam", GradeComponent.EXAM);
            StudentEnrolledEvent enrolled = enrolled();
            GradeUpdatedEvent graded = new GradeUpdatedEvent(student(), course(), grade, null);
            CourseCreatedEvent created = new CourseCreatedEvent(course(), professor());
            UserLoginEvent login = new UserLoginEvent(student(), "10.0.0.1", true);
            ErrorEvent error = new ErrorEvent(enrolled, null, new IllegalStateException("x"), new Date());

            StudentEnrolledEvent enrolledCopy = (StudentEnrolledEvent) enrolled.withCorrelationId("corr-1");
            GradeUpdatedEvent gradedCopy = (GradeUpdatedEvent) graded.withCorrelationId("corr-2");
            CourseCreatedEvent createdCopy = (CourseCreatedEvent) created.withCorrelationId("corr-3");
            UserLoginEvent loginCopy = (UserLoginEvent) login.withCorrelationId("corr-4");
            ErrorEvent errorCopy = (ErrorEvent) error.withCorrelationId("corr-5");

            assertThat(enrolledCopy.getCorrelationId()).isEqualTo("corr-1");
            assertThat(enrolledCopy.getEventId()).isEqualTo(enrolled.getEventId());
            assertThat(enrolledCopy.getStudent()).isSameAs(enrolled.getStudent());
            assertThat(gradedCopy.getNewGrade()).isSameAs(grade);
            assertThat(createdCopy.getProfessor()).isSameAs(created.getProfessor());
            assertThat(loginCopy.getIpAddress()).isEqualTo("10.0.0.1");
            assertThat(errorCopy.getOriginalEvent()).isSameAs(enrolled);
            assertThat(errorCopy.getError()).hasMessage("x");
        }

        @Test
        void errorEvent() {
            StudentEnrolledEvent original = enrolled();
            ErrorEvent e = new ErrorEvent(original, null, new IllegalStateException("x"), new Date());

            assertThat(e.getCategory()).isEqualTo(Event.Category.SYSTEM);
            assertThat(payload(e))
                .containsEntry("originalEventId", original.getEventId())
                .containsEntry("listener", null)
                .containsEntry("error", "x");

            ErrorEvent empty = new ErrorEvent(null, null, null, null);
            assertThat(empty.isValid()).isFalse();
            assertThat(empty.getDescription()).isEqualTo("Error processing event: unknown");
            assertThat(payload(empty)).containsEntry("originalEventId", null).containsEntry("error", null);
        }
    }
}
