package patterns;

import models.Course;
import models.Grade;
import models.Grade.GradeComponent;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import patterns.CommandProcessor.Command;
import patterns.CommandProcessor.CommandResult;
import patterns.CommandProcessor.CommandStatistics;
import patterns.CommandProcessor.CriticalCommand;
import patterns.CommandProcessor.UndoableCommand;
import patterns.ServiceFactory.CachedServiceFactory;
import patterns.ServiceFactory.ServiceType;
import services.CourseService;
import services.EnrollmentService;
import services.GradeService;
import services.StudentService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The built-in commands obtain their services from {@link ServiceFactory#getDefaultFactory()}. Each test installs a
 * fresh {@link CachedServiceFactory} as the default so that all commands in a test share the same (empty) services,
 * and restores the original registration afterwards so no state leaks between tests.
 */
class CommandProcessorTest {

    private CommandProcessor processor;
    private ServiceFactory originalCached;
    private ServiceFactory services;

    @BeforeEach
    void setUp() {
        originalCached = ServiceFactory.getFactory(ServiceType.CACHED);
        ServiceFactory.registerFactory(ServiceType.CACHED, new CachedServiceFactory());
        ServiceFactory.setDefaultFactory(ServiceType.CACHED);
        services = ServiceFactory.getDefaultFactory();
        processor = new CommandProcessor();
    }

    @AfterEach
    void tearDown() {
        processor.shutdown();
        ServiceFactory.registerFactory(ServiceType.CACHED, originalCached);
        ServiceFactory.setDefaultFactory(ServiceType.STANDARD);
    }

    private static Student student(String id) {
        return new Student(id, "Ada", "Lovelace", id.toLowerCase() + "@university.edu", null, id, "CS",
                           Student.AcademicYear.FRESHMAN);
    }

    private static Course course(String id) {
        return new Course(id, id, "Intro " + id, "desc", 3, "DEPT_CS", "PROF00001",
                          Course.DifficultyLevel.BEGINNER, "Fall", 2025);
    }

    private static Grade gradedAssignment(String id, String studentId, String courseId, double points) {
        Grade g = new Grade(id, "E_" + studentId + "_" + courseId, studentId, courseId, "Exam", GradeComponent.EXAM);
        g.setPointsPossible(100);
        g.submitAssignment();
        assertThat(g.gradeAssignment(points, "P1", "")).isTrue();
        return g;
    }

    /** Simple undoable command that increments/decrements a shared counter. */
    private static final class Counter implements UndoableCommand {
        private final AtomicInteger value;
        private final String description;
        private final Date timestamp = new Date();

        Counter(AtomicInteger value) {
            this(value, "increment");
        }

        Counter(AtomicInteger value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override public Object execute() { return value.incrementAndGet(); }
        @Override public Object undo() { return value.decrementAndGet(); }
        @Override public String getDescription() { return description; }
        @Override public Date getTimestamp() { return timestamp; }
    }

    private static Command plain(String description, Object result) {
        return new Command() {
            @Override public Object execute() { return result; }
            @Override public String getDescription() { return description; }
            @Override public Date getTimestamp() { return new Date(); }
        };
    }

    private static final class FailingCritical implements CriticalCommand {
        @Override public Object execute() throws Exception { throw new IllegalStateException("disk full"); }
        @Override public String getDescription() { return "critical"; }
        @Override public Date getTimestamp() { return new Date(); }
    }

    @Nested
    class Execution {

        @Test
        void executeRecordsHistoryAndReturnsResult() {
            AtomicInteger counter = new AtomicInteger();

            CommandResult result = processor.execute(new Counter(counter));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResult()).isEqualTo(1);
            assertThat(result.getMessage()).isEqualTo("Command executed successfully");
            assertThat(result.toString()).contains("success=true");
            assertThat(processor.getCommandHistory()).hasSize(1);
            assertThat(processor.getStatistics().getUndoStackSize()).isEqualTo(1);
        }

        @Test
        void invalidCommandsAreRejectedWithoutExecuting() {
            AtomicInteger counter = new AtomicInteger();
            Command noTimestamp = new Command() {
                @Override public Object execute() { return counter.incrementAndGet(); }
                @Override public String getDescription() { return " "; }
                @Override public Date getTimestamp() { return null; }
            };

            CommandResult nullResult = processor.execute(null);
            CommandResult invalid = processor.execute(noTimestamp);

            assertThat(nullResult.isSuccess()).isFalse();
            assertThat(nullResult.getMessage()).contains("Command cannot be null");
            assertThat(invalid.getMessage())
                .contains("Command must have a description")
                .contains("Command must have a timestamp");
            assertThat(counter).hasValue(0);
            assertThat(processor.getCommandHistory()).isEmpty();
        }

        @Test
        void exceptionsBecomeFailedResultsAndAreNotRecorded() {
            CommandResult result = processor.execute(new FailingCritical());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Command execution failed: disk full");
            assertThat(processor.getCommandHistory()).isEmpty();
        }

        @Test
        void nonUndoableCommandsGoToHistoryButNotUndoStack() {
            processor.execute(plain("report", "ok"));

            assertThat(processor.getCommandHistory()).hasSize(1);
            assertThat(processor.undo().getMessage()).isEqualTo("No commands to undo");
        }

        @Test
        void historyIsCappedAtMaxSize() {
            CommandProcessor small = new CommandProcessor(2);
            try {
                AtomicInteger counter = new AtomicInteger();
                Command first = new Counter(counter, "first");
                small.execute(first);
                small.execute(new Counter(counter, "second"));
                small.execute(new Counter(counter, "third"));

                assertThat(small.getCommandHistory()).extracting(Command::getDescription)
                    .containsExactly("second", "third");
                // undo stack is not trimmed by the history cap
                assertThat(small.getStatistics().getUndoStackSize()).isEqualTo(3);
            } finally {
                small.shutdown();
            }
        }

        @Test
        void batchStopsAtFirstFailingCriticalCommand() {
            AtomicInteger counter = new AtomicInteger();

            List<CommandResult> results = processor.executeBatch(List.of(
                new Counter(counter), new FailingCritical(), new Counter(counter)));

            assertThat(results).extracting(CommandResult::isSuccess).containsExactly(true, false);
            assertThat(counter).hasValue(1);
        }

        @Test
        void batchContinuesPastNonCriticalFailures() {
            AtomicInteger counter = new AtomicInteger();

            List<CommandResult> results = processor.executeBatch(List.of(
                new Counter(counter), plain("", null), new Counter(counter)));

            assertThat(results).extracting(CommandResult::isSuccess).containsExactly(true, false, true);
            assertThat(counter).hasValue(2);
        }

        @Test
        @Timeout(10)
        void asyncExecutionCompletes() throws Exception {
            AtomicInteger counter = new AtomicInteger();

            CommandResult result = processor.executeAsync(new Counter(counter)).get(5, TimeUnit.SECONDS);

            assertThat(result.isSuccess()).isTrue();
            assertThat(counter).hasValue(1);
        }

        // Regression: history/undo/redo bookkeeping used an unsynchronized LinkedList and a non-atomic
        // push+clear, so commands executed concurrently on the processor's pool could be lost or corrupt it.
        @Test
        @Timeout(30)
        void concurrentAsyncBatchRecordsEveryCommand() throws Exception {
            AtomicInteger counter = new AtomicInteger();
            List<Command> commands = new ArrayList<>();
            for (int i = 0; i < 2000; i++) {
                commands.add(new Counter(counter));
            }

            List<CommandResult> results = processor.executeBatchAsync(commands).get(20, TimeUnit.SECONDS);

            assertThat(results).hasSize(2000).allMatch(CommandResult::isSuccess);
            assertThat(counter).hasValue(2000);
            assertThat(processor.getCommandHistory()).hasSize(1000); // default history cap
            assertThat(processor.getStatistics().getUndoStackSize()).isEqualTo(2000);
        }
    }

    @Nested
    class UndoRedo {

        @Test
        void undoAndRedoMoveCommandsBetweenStacks() {
            AtomicInteger counter = new AtomicInteger();
            processor.execute(new Counter(counter));
            processor.execute(new Counter(counter));

            CommandResult undone = processor.undo();
            assertThat(undone.isSuccess()).isTrue();
            assertThat(undone.getResult()).isEqualTo(1);
            assertThat(counter).hasValue(1);

            CommandStatistics stats = processor.getStatistics();
            assertThat(stats.getUndoStackSize()).isEqualTo(1);
            assertThat(stats.getRedoStackSize()).isEqualTo(1);

            CommandResult redone = processor.redo();
            assertThat(redone.isSuccess()).isTrue();
            assertThat(redone.getMessage()).isEqualTo("Command redone successfully");
            assertThat(counter).hasValue(2);
            assertThat(processor.getStatistics().getRedoStackSize()).isZero();
        }

        @Test
        void emptyStacksReportNothingToDo() {
            assertThat(processor.undo().isSuccess()).isFalse();
            assertThat(processor.redo().getMessage()).isEqualTo("No commands to redo");
        }

        @Test
        void newCommandClearsRedoStack() {
            AtomicInteger counter = new AtomicInteger();
            processor.execute(new Counter(counter));
            processor.undo();

            processor.execute(new Counter(counter));

            assertThat(processor.redo().isSuccess()).isFalse();
        }

        @Test
        void failedUndoPutsCommandBack() {
            AtomicInteger attempts = new AtomicInteger();
            UndoableCommand stubborn = new UndoableCommand() {
                @Override public Object execute() { return null; }
                @Override public Object undo() throws Exception {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("locked");
                    }
                    return "undone";
                }
                @Override public String getDescription() { return "stubborn"; }
                @Override public Date getTimestamp() { return new Date(); }
            };
            processor.execute(stubborn);

            assertThat(processor.undo().getMessage()).isEqualTo("Undo failed: locked");
            assertThat(processor.undo().getResult()).isEqualTo("undone");
        }

        @Test
        void failedRedoPutsCommandBack() {
            AtomicInteger executions = new AtomicInteger();
            UndoableCommand flaky = new UndoableCommand() {
                @Override public Object execute() throws Exception {
                    if (executions.incrementAndGet() == 2) {
                        throw new IllegalStateException("busy");
                    }
                    return executions.get();
                }
                @Override public Object undo() { return null; }
                @Override public String getDescription() { return "flaky"; }
                @Override public Date getTimestamp() { return new Date(); }
            };
            processor.execute(flaky);
            processor.undo();

            assertThat(processor.redo().getMessage()).isEqualTo("Redo failed: busy");
            assertThat(processor.redo().getResult()).isEqualTo(3);
        }

        @Test
        void clearHistoryEmptiesEverything() {
            AtomicInteger counter = new AtomicInteger();
            processor.execute(new Counter(counter));
            processor.execute(new Counter(counter));
            processor.undo();

            processor.clearHistory();

            CommandStatistics stats = processor.getStatistics();
            assertThat(stats.getTotalCommands()).isZero();
            assertThat(stats.getUndoStackSize()).isZero();
            assertThat(stats.getRedoStackSize()).isZero();
        }
    }

    @Nested
    class StudentCommands {

        @Test
        void createThenUndoRemovesStudent() {
            StudentService students = services.createStudentService();
            Command create = processor.createCommand("CREATE_STUDENT", Map.of("student", student("STU000001")));

            assertThat(processor.execute(create).getResult()).isEqualTo(true);
            assertThat(create.getDescription()).isEqualTo("Create student: Ada Lovelace");
            assertThat(students.getStudentById("STU000001")).isPresent();

            processor.undo();
            assertThat(students.getStudentById("STU000001")).isEmpty();

            processor.redo();
            assertThat(students.getStudentById("STU000001")).isPresent();
        }

        // Regression: a CREATE that failed (duplicate id) was still undoable, and undoing it deleted the
        // pre-existing student that the command never created.
        @Test
        void undoOfFailedCreateLeavesExistingStudent() {
            StudentService students = services.createStudentService();
            Student original = student("STU000001");
            students.addStudent(original);

            CommandResult result = processor.execute(processor.new CreateStudentCommand(student("STU000001")));
            assertThat(result.getResult()).isEqualTo(false);

            processor.undo();

            assertThat(students.getStudentById("STU000001")).containsSame(original);
        }

        @Test
        void updateThenUndoRestoresOriginal() {
            StudentService students = services.createStudentService();
            Student original = student("STU000001");
            students.addStudent(original);
            Student changed = student("STU000001");
            changed.setLastName("Byron");

            Command update = processor.createCommand("UPDATE_STUDENT", Map.of("student", changed));
            assertThat(processor.execute(update).getResult()).isEqualTo(true);
            assertThat(update.getDescription()).isEqualTo("Update student: Ada Byron");
            assertThat(students.getStudentById("STU000001")).containsSame(changed);

            assertThat(processor.undo().getResult()).isSameAs(original);
            assertThat(students.getStudentById("STU000001")).containsSame(original);
        }

        @Test
        void updateOfUnknownStudentFailsAndUndoIsNoOp() {
            StudentService students = services.createStudentService();
            Command update = processor.new UpdateStudentCommand(student("STU000404"));

            assertThat(processor.execute(update).getResult()).isEqualTo(false);
            assertThat(((CommandProcessor.StatefulCommand) update).wasSuccessful()).isFalse();
            assertThat(processor.undo().getResult()).isNull();
            assertThat(students.getStudentById("STU000404")).isEmpty();
        }

        @Test
        void deleteThenUndoRestoresStudent() {
            StudentService students = services.createStudentService();
            Student original = student("STU000001");
            students.addStudent(original);

            Command delete = processor.createCommand("DELETE_STUDENT", Map.of("studentId", "STU000001"));
            processor.execute(delete);
            assertThat(delete.getDescription()).isEqualTo("Delete student: STU000001");
            assertThat(students.getStudentById("STU000001")).isEmpty();

            assertThat(processor.undo().getResult()).isSameAs(original);
            assertThat(students.getStudentById("STU000001")).containsSame(original);
        }

        @Test
        void commandThatThrowsRecordsError() {
            CommandProcessor.UpdateStudentCommand update = processor.new UpdateStudentCommand(null);

            assertThatThrownBy(update::execute).isInstanceOf(NullPointerException.class);
            assertThat(update.wasSuccessful()).isFalse();
            assertThat(update.getLastError()).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class CourseCommands {

        @Test
        void createUpdateDeleteWithUndo() {
            CourseService courses = services.createCourseService();
            Course original = course("CS101");

            Command create = processor.createCommand("CREATE_COURSE", Map.of("course", original));
            processor.execute(create);
            assertThat(create.getDescription()).isEqualTo("Create course: Intro CS101");
            assertThat(courses.getCourseById("CS101")).containsSame(original);

            Course renamed = course("CS101");
            renamed.setCourseName("Programming I");
            Command update = processor.createCommand("UPDATE_COURSE", Map.of("course", renamed));
            processor.execute(update);
            assertThat(update.getDescription()).isEqualTo("Update course: Programming I");
            assertThat(courses.getCourseById("CS101")).containsSame(renamed);
            processor.undo();
            assertThat(courses.getCourseById("CS101")).containsSame(original);

            Command delete = processor.createCommand("DELETE_COURSE", Map.of("courseCode", "CS101"));
            processor.execute(delete);
            assertThat(delete.getDescription()).isEqualTo("Delete course: CS101");
            assertThat(courses.getCourseById("CS101")).isEmpty();
            assertThat(processor.undo().getResult()).isSameAs(original);
            assertThat(courses.getCourseById("CS101")).containsSame(original);

            processor.undo(); // undo create
            assertThat(courses.getCourseById("CS101")).isEmpty();
        }

        // Regression: undoing a CREATE_COURSE that failed (duplicate id) removed the existing course.
        @Test
        void undoOfFailedCreateLeavesExistingCourse() {
            CourseService courses = services.createCourseService();
            Course original = course("CS101");
            courses.addCourse(original);

            processor.execute(processor.new CreateCourseCommand(course("CS101")));
            processor.undo();

            assertThat(courses.getCourseById("CS101")).containsSame(original);
        }

        @Test
        void deleteOfUnknownCourseIsUnsuccessfulAndUndoIsNoOp() {
            CommandProcessor.DeleteCourseCommand delete = processor.new DeleteCourseCommand("NOPE999");

            processor.execute(delete);

            assertThat(delete.wasSuccessful()).isFalse();
            assertThat(processor.undo().getResult()).isNull();
        }
    }

    @Nested
    class EnrollmentCommands {

        @Test
        void enrollThenUndoDropsAndUnenrollThenUndoReenrolls() {
            EnrollmentService enrollments = services.createEnrollmentService();
            Student s = student("STU000001");
            Course c = course("CS101");

            Command enroll = processor.createCommand("ENROLL_STUDENT", Map.of("student", s, "course", c));
            assertThat(processor.execute(enroll).getResult()).isEqualTo(true);
            assertThat(enroll.getDescription()).isEqualTo("Enroll Ada Lovelace in Intro CS101");
            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isTrue();

            processor.undo();
            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isFalse();
            processor.redo();
            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isTrue();

            Command unenroll = processor.createCommand("UNENROLL_STUDENT", Map.of("student", s, "course", c));
            assertThat(processor.execute(unenroll).getResult()).isEqualTo(true);
            assertThat(unenroll.getDescription()).isEqualTo("Unenroll Ada Lovelace from Intro CS101");
            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isFalse();

            processor.undo();
            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isTrue();
        }

        // Regression: a duplicate ENROLL (returns false) was still undone, dropping the existing enrollment.
        @Test
        void undoOfFailedEnrollKeepsExistingEnrollment() {
            EnrollmentService enrollments = services.createEnrollmentService();
            Student s = student("STU000001");
            Course c = course("CS101");
            processor.execute(processor.new EnrollStudentCommand(s, c));

            CommandResult duplicate = processor.execute(processor.new EnrollStudentCommand(s, c));
            assertThat(duplicate.getResult()).isEqualTo(false);
            processor.undo();

            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isTrue();
        }

        // Regression: undoing an UNENROLL that failed (student was not enrolled) enrolled the student.
        @Test
        void undoOfFailedUnenrollDoesNotEnrollStudent() {
            EnrollmentService enrollments = services.createEnrollmentService();

            CommandResult result = processor.execute(
                processor.new UnenrollStudentCommand(student("STU000001"), course("CS101")));
            assertThat(result.getResult()).isEqualTo(false);
            processor.undo();

            assertThat(enrollments.isStudentEnrolled("STU000001", "CS101")).isFalse();
        }
    }

    @Nested
    class GradeCommands {

        @Test
        void assignNewGradeThenUndoRemovesIt() {
            GradeService grades = services.createGradeService();
            Grade g = gradedAssignment("G1", "STU000001", "CS101", 95);

            Command assign = processor.createCommand("ASSIGN_GRADE",
                Map.of("student", student("STU000001"), "course", course("CS101"), "grade", g));
            assertThat(processor.execute(assign).getResult()).isEqualTo(true);
            assertThat(assign.getDescription()).isEqualTo("Assign grade A to Ada Lovelace for Intro CS101");
            assertThat(grades.getGradeById("G1")).containsSame(g);

            assertThat(processor.undo().getResult()).isNull();
            assertThat(grades.getGradeById("G1")).isEmpty();
        }

        @Test
        void reassignGradeThenUndoRestoresPrevious() {
            GradeService grades = services.createGradeService();
            Grade previous = gradedAssignment("G1", "STU000001", "CS101", 70);
            grades.addGrade(previous);
            Grade improved = gradedAssignment("G1", "STU000001", "CS101", 92);

            processor.execute(processor.new AssignGradeCommand(student("STU000001"), course("CS101"), improved));
            assertThat(grades.getGradeById("G1")).containsSame(improved);

            assertThat(processor.undo().getResult()).isSameAs(previous);
            assertThat(grades.getGradeById("G1")).containsSame(previous);
        }
    }

    @Nested
    class SystemCommands {

        @Test
        @Timeout(15)
        void backupAndRestoreRunAsCriticalCommands() throws Exception {
            Command backup = processor.createCommand("BACKUP_DATA", Map.of());
            Command restore = processor.createCommand("RESTORE_DATA", Map.of("backupPath", "/tmp/b1"));

            assertThat(backup).isInstanceOf(CriticalCommand.class);
            assertThat(backup.getDescription()).isEqualTo("Backup system data");
            assertThat(restore.getDescription()).isEqualTo("Restore system data from /tmp/b1");

            List<CommandResult> results = processor.executeBatchAsync(List.of(backup, restore)).get(10, TimeUnit.SECONDS);

            assertThat(results).extracting(CommandResult::getResult).containsExactly(
                "Backup completed successfully", "Restore completed successfully from /tmp/b1");
            assertThat(processor.undo().isSuccess()).isFalse(); // neither is undoable
        }

        @Test
        void unknownCommandTypeIsRejected() {
            assertThatThrownBy(() -> processor.createCommand("LAUNCH_ROCKET", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LAUNCH_ROCKET");
        }

        @Test
        void customFactoriesCanBeRegistered() {
            processor.registerCommandFactory("PING", params -> plain("ping", params.get("payload")));

            CommandResult result = processor.execute(processor.createCommand("PING", Map.of("payload", "pong")));

            assertThat(result.getResult()).isEqualTo("pong");
        }
    }

    @Nested
    class Statistics {

        @Test
        void statisticsCountByTypeAndOutcome() {
            StudentService students = services.createStudentService();
            students.addStudent(student("STU000002"));
            processor.execute(processor.new CreateStudentCommand(student("STU000001"))); // succeeds
            processor.execute(processor.new CreateStudentCommand(student("STU000002"))); // duplicate -> unsuccessful
            processor.execute(plain("report", "ok")); // not stateful
            processor.undo();

            CommandStatistics stats = processor.getStatistics();

            assertThat(stats.getTotalCommands()).isEqualTo(3);
            assertThat(stats.getSuccessfulCommands()).isEqualTo(1);
            assertThat(stats.getFailedCommands()).isEqualTo(1);
            assertThat(stats.getUndoStackSize()).isEqualTo(1);
            assertThat(stats.getRedoStackSize()).isEqualTo(1);
            assertThat(stats.getCommandCounts()).containsEntry("CreateStudentCommand", 2);
            assertThat(stats.getSuccessRate()).isCloseTo(100.0 / 3, org.assertj.core.data.Offset.offset(1e-9));
            assertThat(stats.toString()).contains("total=3").contains("undo=1").contains("redo=1");
        }

        @Test
        void emptyStatisticsHaveZeroSuccessRate() {
            assertThat(processor.getStatistics().getSuccessRate()).isZero();
        }
    }
}
