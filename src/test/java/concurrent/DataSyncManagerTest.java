package concurrent;

import concurrent.DataSyncManager.BackupData;
import concurrent.DataSyncManager.BackupResult;
import concurrent.DataSyncManager.ExternalDataSource;
import concurrent.DataSyncManager.SyncResult;
import concurrent.DataSyncManager.ValidationResult;
import models.Course;
import models.Department;
import models.Enrollment;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import repositories.CourseRepository;
import repositories.DepartmentRepository;
import repositories.EnrollmentRepository;
import repositories.ProfessorRepository;
import repositories.StudentRepository;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(60)
class DataSyncManagerTest {

    /** Department repository whose findAll can be made to block or fail on demand. */
    static class ControllableDepartmentRepository extends DepartmentRepository {
        volatile CountDownLatch entered;
        volatile CountDownLatch release;
        volatile RuntimeException failure;

        @Override
        public List<Department> findAll() {
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
            return super.findAll();
        }
    }

    private StudentRepository students;
    private ProfessorRepository professors;
    private CourseRepository courses;
    private ControllableDepartmentRepository departments;
    private EnrollmentRepository enrollments;
    private DataSyncManager manager;

    @BeforeEach
    void setUp() {
        students = new StudentRepository();
        professors = new ProfessorRepository();
        courses = new CourseRepository();
        departments = new ControllableDepartmentRepository();
        enrollments = new EnrollmentRepository();
        manager = new DataSyncManager(students, professors, courses, departments, enrollments);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    private void seedValidData() {
        departments.save(new Department("D-CS", "CS", "Computer Science", "", "Hall A"));
        for (String id : List.of("S1", "S2", "S3")) {
            Student s = new Student("U" + id, "First", "Last", id.toLowerCase() + "@u.edu", null, id, "CS",
                    Student.AcademicYear.SOPHOMORE);
            s.setDepartmentId(id.equals("S3") ? "D-MATH" : "D-CS");
            students.save(s);
        }
        professors.save(new Professor("UP1", "Pat", "Prof", "p1@u.edu", null, "P1", "D-CS",
                Professor.AcademicRank.FULL, "AI"));
        Course c1 = new Course("C1", "CS101", "Intro", "", 3, "D-CS");
        c1.setProfessorId("P1");
        courses.save(c1);
        enrollments.save(new Enrollment("E1", "S1", "C1", "Fall", 2024));
        enrollments.save(new Enrollment("E2", "S2", "C1", "Fall", 2024));
    }

    @Nested
    class FullSync {

        @Test
        void countsEveryRepositoryAndRecordsSyncTime() throws Exception {
            seedValidData();
            long before = System.currentTimeMillis();

            SyncResult result = await(manager.syncAllDataAsync());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getMessage()).startsWith("Sync completed in ")
                    .contains("departments=1", "students=3", "professors=1", "courses=1", "enrollments=2");
            assertThat(manager.getLastSyncTime()).isGreaterThanOrEqualTo(before);
            assertThat(manager.isSyncInProgress()).isFalse();
        }

        @Test
        void secondSyncIsRefusedWhileOneIsRunning() throws Exception {
            departments.entered = new CountDownLatch(1);
            departments.release = new CountDownLatch(1);

            CompletableFuture<SyncResult> first = manager.syncAllDataAsync();
            assertThat(departments.entered.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(manager.isSyncInProgress()).isTrue();
            SyncResult refused = await(manager.syncAllDataAsync());
            assertThat(refused.isSuccess()).isFalse();
            assertThat(refused.getMessage()).isEqualTo("Sync already in progress");

            departments.release.countDown();
            assertThat(await(first).isSuccess()).isTrue();
            assertThat(manager.isSyncInProgress()).isFalse();
        }

        @Test
        void failureIsReportedAndReleasesTheInProgressFlag() throws Exception {
            departments.failure = new IllegalStateException("db down");

            SyncResult result = await(manager.syncAllDataAsync());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Sync failed: db down");
            assertThat(manager.isSyncInProgress()).isFalse();
            assertThat(manager.getLastSyncTime()).isZero();

            departments.failure = null;
            assertThat(await(manager.syncAllDataAsync()).isSuccess()).isTrue();
        }
    }

    @Test
    void externalSyncProcessesWhatTheSourceProvides() throws Exception {
        ExternalDataSource source = new ExternalDataSource("SIS", "jdbc:sis");

        SyncResult result = await(manager.syncFromExternalSourceAsync(source));

        assertThat(source.getSourceName()).isEqualTo("SIS");
        assertThat(source.getConnectionString()).isEqualTo("jdbc:sis");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("External sync completed. Processed 0 records");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshCachePopulatesCountsAndGroupings() throws Exception {
        seedValidData();

        await(manager.refreshCacheAsync());

        assertThat(manager.getCachedData("students_count")).isEqualTo(3);
        assertThat(manager.getCachedData("courses_count")).isEqualTo(1);
        assertThat(manager.getCachedData("departments_count")).isEqualTo(1);
        assertThat(manager.getCachedData("enrollments_count")).isEqualTo(2);
        assertThat((Map<String, List<Student>>) manager.getCachedData("students_by_department"))
                .containsOnlyKeys("D-CS", "D-MATH");
        assertThat((Map<String, List<Course>>) manager.getCachedData("courses_by_department"))
                .containsOnlyKeys("D-CS");
        assertThat(manager.getCachedData("department_stats")).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) manager.getCachedData("enrollment_stats"))
                .containsEntry("totalEnrollments", 2L);

        students.deleteById("S1");
        await(manager.refreshCacheAsync());
        assertThat(manager.getCachedData("students_count")).isEqualTo(2);
    }

    @Nested
    class Validation {

        @Test
        void consistentDataIsValid() throws Exception {
            seedValidData();

            ValidationResult result = await(manager.validateDataIntegrityAsync());

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        void reportsEveryMissingReference() throws Exception {
            Student student = new Student();
            student.setStudentId("S9");
            students.save(student);
            Student otherValid = new Student("U8", "B", "E", "x@u.edu", null, "S8", "CS",
                    Student.AcademicYear.FRESHMAN);
            otherValid.setDepartmentId("D-CS");
            students.save(otherValid);
            Professor professor = new Professor();
            professor.setProfessorId("P9");
            professors.save(professor);
            courses.save(new Course("C9", "CS999", "Orphan", "", 3, null));
            enrollments.save(new Enrollment("E9", null, null, "Fall", 2024));

            ValidationResult result = await(manager.validateDataIntegrityAsync());

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactlyInAnyOrder(
                    "Student S9 has no email",
                    "Student S9 has no department",
                    "Professor P9 has no email",
                    "Professor P9 has no department",
                    "Course CS999 has no department",
                    "Course CS999 has no professor",
                    "Enrollment E9 has no student",
                    "Enrollment E9 has no course");
        }
    }

    @Nested
    class Backup {

        @Test
        void snapshotsEveryRepositoryIndependentlyOfLaterChanges() throws Exception {
            seedValidData();

            BackupResult result = await(manager.createBackupAsync());
            students.deleteById("S1");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).startsWith("Backup completed in ");
            BackupData data = result.getBackupData();
            assertThat(data.getStudents()).extracting(Student::getStudentId)
                    .containsExactlyInAnyOrder("S1", "S2", "S3");
            assertThat(data.getProfessors()).extracting(Professor::getProfessorId).containsExactly("P1");
            assertThat(data.getCourses()).extracting(Course::getCourseId).containsExactly("C1");
            assertThat(data.getDepartments()).extracting(Department::getDepartmentId).containsExactly("D-CS");
            assertThat(data.getEnrollments()).hasSize(2);
            assertThat(data.getBackupTime()).isNotNull();
        }

        @Test
        void repositoryFailureFailsTheBackup() throws Exception {
            departments.failure = new IllegalStateException("disk full");

            BackupResult result = await(manager.createBackupAsync());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).startsWith("Backup failed: ").contains("disk full");
            assertThat(result.getBackupData()).isNull();
        }
    }

    // Regression: every public operation ran its coordinating task on the worker pool and then
    // blocked joining sub-tasks queued on that same pool. Once the pool was busy (for example
    // with other concurrent operations), the coordinator held the last free thread and waited
    // forever for sub-tasks that could never be scheduled.
    @Test
    void operationsCompleteWhileAllButOneWorkerThreadIsBusy() throws Exception {
        seedValidData();
        Field field = DataSyncManager.class.getDeclaredField("executorService");
        field.setAccessible(true);
        ExecutorService workers = (ExecutorService) field.get(manager);
        int poolSize = ((ThreadPoolExecutor) workers).getCorePoolSize();
        CountDownLatch busy = new CountDownLatch(poolSize - 1);
        CountDownLatch release = new CountDownLatch(1);
        for (int i = 0; i < poolSize - 1; i++) {
            workers.submit(() -> {
                busy.countDown();
                return release.await(30, TimeUnit.SECONDS);
            });
        }
        try {
            assertThat(busy.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(await(manager.validateDataIntegrityAsync()).isValid()).isTrue();
            assertThat(await(manager.createBackupAsync()).isSuccess()).isTrue();
            assertThat(await(manager.syncAllDataAsync()).isSuccess()).isTrue();
            assertThat(await(manager.syncFromExternalSourceAsync(new ExternalDataSource("x", "y"))).isSuccess())
                    .isTrue();
            await(manager.refreshCacheAsync());
            assertThat(manager.getCachedData("students_count")).isEqualTo(3);
        } finally {
            release.countDown();
        }
    }

    @Test
    void shutdownRejectsNewOperations() {
        manager.shutdown();

        assertThatThrownBy(() -> manager.validateDataIntegrityAsync())
                .isInstanceOf(RejectedExecutionException.class);
        assertThatThrownBy(() -> manager.createBackupAsync())
                .isInstanceOf(RejectedExecutionException.class);
    }
}
