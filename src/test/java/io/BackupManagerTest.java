package io;

import models.Course;
import models.Department;
import models.Enrollment;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.CourseRepository;
import repositories.DepartmentRepository;
import repositories.EnrollmentRepository;
import repositories.ProfessorRepository;
import repositories.StudentRepository;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BackupManagerTest {

    private static final long TIMEOUT_SECONDS = 30;

    @TempDir
    Path dir;

    private Path base;
    private StudentRepository students;
    private ProfessorRepository professors;
    private CourseRepository courses;
    private DepartmentRepository departments;
    private EnrollmentRepository enrollments;
    private BackupManager manager;

    @BeforeEach
    void setUp() {
        base = dir.resolve("backup");
        students = new StudentRepository();
        professors = new ProfessorRepository();
        courses = new CourseRepository();
        departments = new DepartmentRepository();
        enrollments = new EnrollmentRepository();
        manager = managerWith(null);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    private BackupManager managerWith(DatabaseManager db) {
        return new BackupManager(students, professors, courses, departments, enrollments, db, base);
    }

    private void populate() {
        Student s1 = new Student("S1", "Ann", "Lee", "ann@uni.edu", null, "S1", "CS", Student.AcademicYear.JUNIOR);
        s1.setEnrollmentDate(LocalDate.now());
        Student s2 = new Student("S2", "Bob", "Ray", "bob@uni.edu", null, "S2", "Math", Student.AcademicYear.SENIOR);
        s2.setEnrollmentDate(LocalDate.now().minusYears(3));
        students.save(s1);
        students.save(s2);
        professors.save(new Professor("P1", "Grace", "Hopper", "grace@uni.edu", null, "P1", "D1",
                Professor.AcademicRank.ASSOCIATE, "Compilers"));
        courses.save(new Course("C1", "CS101", "Intro", "Desc", 3, "D1", "P1",
                Course.DifficultyLevel.BEGINNER, "Fall", 2024));
        departments.save(new Department("D1", "CS", "Computer Science", "Desc", "Bldg"));
        Enrollment recent = new Enrollment("E1", "S1", "C1", "Fall", 2024);
        recent.setEnrollmentDate(LocalDateTime.now().minusHours(1));
        Enrollment old = new Enrollment("E2", "S2", "C1", "Fall", 2020);
        old.setEnrollmentDate(LocalDateTime.now().minusYears(3));
        enrollments.save(recent);
        enrollments.save(old);
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static List<String> zipEntries(Path zip) throws Exception {
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            List<String> names = new ArrayList<>();
            zf.stream().forEach(e -> names.add(e.getName()));
            return names;
        }
    }

    private Path zip(String name, Map<String, String> entries) throws Exception {
        Path zip = dir.resolve(name);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zip;
    }

    @Test
    void constructorCreatesBackupDirectoriesUnderBase() {
        assertThat(base.resolve("full")).isDirectory();
        assertThat(base.resolve("incremental")).isDirectory();
        assertThat(base.resolve("database")).isDirectory();
    }

    @Nested
    class FullBackup {

        @Test
        void archiveContainsAllEntitiesAndMetadata() throws Exception {
            populate();

            BackupManager.BackupResult result = await(manager.createFullBackup());

            assertThat(result.isSuccess()).as(result.getMessage()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Full backup completed successfully");
            Path archive = result.getBackupPath();
            assertThat(archive.getParent()).isEqualTo(base.resolve("full"));
            assertThat(archive.getFileName().toString()).matches("full_backup_\\d{8}_\\d{6}(_\\d+)?\\.zip");
            assertThat(zipEntries(archive)).containsExactlyInAnyOrder("students.json", "professors.json",
                    "courses.json", "departments.json", "enrollments.json", "backup_metadata.json");
            try (var files = Files.list(base.resolve("full"))) {
                assertThat(files).containsExactly(archive); // working directory removed
            }

            BackupManager.BackupMetadata metadata = result.getMetadata();
            assertThat(metadata.getType()).isEqualTo(BackupManager.BackupType.FULL);
            assertThat(metadata.getName() + ".zip").isEqualTo(archive.getFileName().toString());
            assertThat(metadata.getDuration()).isNotNegative();
            assertThat(metadata.getEntityResults()).hasSize(5);
            assertThat(metadata.getEntityResults().get("students").getRecordCount()).isEqualTo(2);
            assertThat(metadata.getEntityResults().get("enrollments").getRecordCount()).isEqualTo(2);
            assertThat(metadata.getEntityResults().get("courses").isSuccess()).isTrue();
            assertThat(metadata.getEntityResults().get("courses").getFileSize()).isPositive();
            assertThat(metadata.getEntityResults().get("courses").getError()).isNull();
        }

        @Test
        void failingRepositoryIsReportedPerEntity() throws Exception {
            manager.shutdown();
            manager = new BackupManager(new StudentRepository() {
                @Override
                public List<Student> findAll() {
                    throw new IllegalStateException("store offline");
                }
            }, professors, courses, departments, enrollments, null, base);

            BackupManager.BackupResult result = await(manager.createFullBackup());

            assertThat(result.isSuccess()).isTrue();
            BackupManager.BackupEntityResult studentsResult = result.getMetadata().getEntityResults().get("students");
            assertThat(studentsResult.isSuccess()).isFalse();
            assertThat(studentsResult.getError()).isEqualTo("store offline");
            assertThat(studentsResult.getRecordCount()).isZero();
        }

        // Regression: backup jobs and their per-entity subtasks shared one 4-thread pool, so four
        // concurrent backups occupied every thread while waiting for subtasks that could never run.
        // Also, backups started within the same second shared (and clobbered) one directory/archive.
        @Test
        void concurrentBackupsNeitherDeadlockNorOverwriteEachOther() throws Exception {
            populate();

            List<CompletableFuture<BackupManager.BackupResult>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                futures.add(manager.createFullBackup());
            }
            List<BackupManager.BackupResult> results = new ArrayList<>();
            for (CompletableFuture<BackupManager.BackupResult> f : futures) {
                results.add(await(f));
            }

            assertThat(results).allSatisfy(r -> assertThat(r.isSuccess()).as(r.getMessage()).isTrue());
            assertThat(results).extracting(BackupManager.BackupResult::getBackupPath).doesNotHaveDuplicates();
            for (BackupManager.BackupResult r : results) {
                assertThat(manager.verifyBackup(r.getBackupPath()).isValid()).isTrue();
            }
        }
    }

    @Nested
    class IncrementalBackup {

        @Test
        void includesOnlyRecentStudentsAndEnrollments() throws Exception {
            populate();
            Date since = Date.from(LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());

            BackupManager.BackupResult result = await(manager.createIncrementalBackup(since));

            assertThat(result.isSuccess()).as(result.getMessage()).isTrue();
            assertThat(result.getBackupPath().getParent()).isEqualTo(base.resolve("incremental"));
            assertThat(zipEntries(result.getBackupPath()))
                    .containsExactlyInAnyOrder("students.json", "enrollments.json", "backup_metadata.json");
            BackupManager.BackupMetadata metadata = result.getMetadata();
            assertThat(metadata.getType()).isEqualTo(BackupManager.BackupType.INCREMENTAL);
            assertThat(metadata.getBasedOnDate()).isEqualTo(since);
            assertThat(metadata.getEntityResults().get("students").getRecordCount()).isEqualTo(1);
            assertThat(metadata.getEntityResults().get("enrollments").getRecordCount()).isEqualTo(1);
        }

        @Test
        void verificationFlagsMissingEntityFiles() throws Exception {
            BackupManager.BackupResult result = await(manager.createIncrementalBackup(new Date(0)));

            BackupManager.BackupVerificationResult verification = manager.verifyBackup(result.getBackupPath());

            assertThat(verification.isValid()).isFalse();
            assertThat(verification.hasIssues()).isTrue();
            assertThat(verification.getIssues()).containsExactlyInAnyOrder("Missing data file: professors.json",
                    "Missing data file: courses.json", "Missing data file: departments.json");
        }
    }

    @Nested
    class Restore {

        // Regression: BackupMetadata/BackupEntityResult had no deserializable constructor, so reading
        // backup_metadata.json failed and every restore (and verification) of a real backup failed.
        @Test
        void fullBackupRestoresIntoEmptyRepositories() throws Exception {
            populate();
            Path archive = await(manager.createFullBackup()).getBackupPath();
            StudentRepository freshStudents = new StudentRepository();
            CourseRepository freshCourses = new CourseRepository();
            DepartmentRepository freshDepartments = new DepartmentRepository();
            ProfessorRepository freshProfessors = new ProfessorRepository();
            EnrollmentRepository freshEnrollments = new EnrollmentRepository();
            BackupManager restorer = new BackupManager(freshStudents, freshProfessors, freshCourses,
                    freshDepartments, freshEnrollments, null, dir.resolve("other"));
            try {
                BackupManager.RestoreResult result =
                        await(restorer.restoreFromBackup(archive, new BackupManager.RestoreOptions()));

                assertThat(result.isSuccess()).as(result.getMessage()).isTrue();
                assertThat(result.getMessage()).isEqualTo("Restore completed successfully");
                assertThat(result.getDuration()).isNotNegative();
                assertThat(result.getOriginalMetadata().getType()).isEqualTo(BackupManager.BackupType.FULL);
                assertThat(result.getOriginalMetadata().getEntityResults().get("students").getRecordCount())
                        .isEqualTo(2);
                assertThat(result.getEntityResults()).hasSize(5)
                        .allSatisfy((type, r) -> assertThat(r.isSuccess()).as(type + ": " + r.getError()).isTrue());
                assertThat(result.getEntityResults().get("students").getRecordCount()).isEqualTo(2);
                assertThat(result.getEntityResults().get("students").getEntityType()).isEqualTo("students");
                assertThat(freshStudents.findAll()).extracting(Student::getStudentId).containsExactlyInAnyOrder("S1", "S2");
                assertThat(freshCourses.findAll()).extracting(Course::getCourseCode).containsExactly("CS101");
                assertThat(freshDepartments.findAll()).hasSize(1);
                assertThat(freshProfessors.findAll()).hasSize(1);
                assertThat(freshEnrollments.findAll()).hasSize(2);
            } finally {
                restorer.shutdown();
            }
            assertThat(manager.verifyBackup(archive).isValid()).isTrue();
        }

        @Test
        void optionsSelectEntitiesAndMissingFilesAreReported() throws Exception {
            populate();
            Path archive = await(manager.createIncrementalBackup(new Date(0))).getBackupPath();
            BackupManager.RestoreOptions options = new BackupManager.RestoreOptions();
            options.setRestoreStudents(false);
            options.setRestoreEnrollments(false);
            options.setOverwriteExisting(true);

            BackupManager.RestoreResult result = await(manager.restoreFromBackup(archive, options));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getEntityResults()).containsOnlyKeys("professors", "courses", "departments");
            assertThat(result.getEntityResults().values())
                    .allSatisfy(r -> assertThat(r.isSuccess()).isFalse())
                    .extracting(BackupManager.RestoreEntityResult::getError)
                    .containsExactlyInAnyOrder("Professors file not found in backup",
                            "Courses file not found in backup", "Departments file not found in backup");
        }

        @Test
        void corruptEntityFileIsReportedPerEntity() throws Exception {
            Path archive = zip("corrupt.zip", Map.of(
                    "backup_metadata.json", "{\"name\":\"x\",\"type\":\"FULL\",\"timestamp\":\"t\",\"entityResults\":{},\"duration\":1}",
                    "students.json", "{broken",
                    "professors.json", "[]", "courses.json", "[]", "departments.json", "[]",
                    "enrollments.json", "nope"));

            BackupManager.RestoreResult result = await(manager.restoreFromBackup(archive, new BackupManager.RestoreOptions()));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getEntityResults().get("students").isSuccess()).isFalse();
            assertThat(result.getEntityResults().get("enrollments").isSuccess()).isFalse();
            assertThat(result.getEntityResults().get("courses").isSuccess()).isTrue();
            assertThat(result.getEntityResults().get("courses").getRecordCount()).isZero();
        }

        @Test
        void missingArchiveFails() throws Exception {
            BackupManager.RestoreResult result =
                    await(manager.restoreFromBackup(dir.resolve("none.zip"), new BackupManager.RestoreOptions()));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).startsWith("Restore failed");
            assertThat(result.getEntityResults()).isEmpty();
            assertThat(result.getOriginalMetadata()).isNull();
        }

        // Regression: archive entries were extracted unchecked, so "../" entries escaped the
        // extraction directory ("zip slip").
        @Test
        void entriesEscapingExtractionDirectoryAreRejected() throws Exception {
            // archives are extracted into a fresh directory under java.io.tmpdir
            String fileName = "io-zip-slip-" + System.nanoTime() + ".txt";
            Path archive = zip("evil.zip", Map.of("../" + fileName, "pwned"));
            Path escapeTarget = Path.of(System.getProperty("java.io.tmpdir")).resolve(fileName);

            BackupManager.RestoreResult result = await(manager.restoreFromBackup(archive, new BackupManager.RestoreOptions()));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("outside");
            assertThat(escapeTarget).doesNotExist();
        }
    }

    @Nested
    class Verification {

        @Test
        void missingFileIsInvalid() {
            BackupManager.BackupVerificationResult r = manager.verifyBackup(dir.resolve("none.zip"));
            assertThat(r.isValid()).isFalse();
            assertThat(r.getIssues()).containsExactly("Backup file does not exist");
        }

        @Test
        void invalidMetadataAndJsonAreReported() throws Exception {
            Path archive = zip("bad.zip", Map.of(
                    "backup_metadata.json", "{oops",
                    "students.json", "[", "professors.json", "[]", "courses.json", "[]",
                    "departments.json", "[]", "enrollments.json", "[]"));

            BackupManager.BackupVerificationResult r = manager.verifyBackup(archive);

            assertThat(r.isValid()).isFalse();
            assertThat(r.getIssues()).hasSize(2)
                    .anySatisfy(i -> assertThat(i).startsWith("Invalid backup metadata"))
                    .anySatisfy(i -> assertThat(i).startsWith("Invalid JSON in file students.json"));
        }

        @Test
        void archiveWithoutMetadataIsReported() throws Exception {
            Path archive = zip("nometa.zip", Map.of("students.json", "[]"));
            assertThat(manager.verifyBackup(archive).getIssues()).contains("Backup metadata file missing");
        }

        @Test
        void unreadableArchiveFailsVerification() throws Exception {
            Path archive = zip("evil.zip", Map.of("../escape.txt", "x"));
            BackupManager.BackupVerificationResult r = manager.verifyBackup(archive);
            assertThat(r.isValid()).isFalse();
            assertThat(r.getIssues()).singleElement().asString().startsWith("Verification failed");
        }
    }

    @Nested
    class Housekeeping {

        @Test
        void listsBackupsOfAllTypesNewestFirst() throws Exception {
            Path full = await(manager.createFullBackup()).getBackupPath();
            Path incremental = await(manager.createIncrementalBackup(new Date(0))).getBackupPath();
            Path db = Files.writeString(base.resolve("database/db.sql"), "script");
            Files.setLastModifiedTime(full, FileTime.from(Instant.now().minus(2, ChronoUnit.HOURS)));
            Files.setLastModifiedTime(incremental, FileTime.from(Instant.now().minus(1, ChronoUnit.HOURS)));

            List<BackupManager.BackupInfo> backups = manager.listAvailableBackups();

            assertThat(backups).extracting(BackupManager.BackupInfo::getPath).containsExactly(db, incremental, full);
            assertThat(backups).extracting(BackupManager.BackupInfo::getType).containsExactly(
                    BackupManager.BackupType.DATABASE, BackupManager.BackupType.INCREMENTAL, BackupManager.BackupType.FULL);
            assertThat(backups.get(0).getName()).isEqualTo("db.sql");
            assertThat(backups.get(0).getSize()).isEqualTo(6);
        }

        @Test
        void listingToleratesMissingDirectories() throws Exception {
            FileUtil.deleteDirectoryRecursively(base);
            assertThat(manager.listAvailableBackups()).isEmpty();
        }

        @Test
        void cleanupDeletesOnlyBackupsOlderThanRetention() throws Exception {
            Path old = Files.writeString(base.resolve("full/old.zip"), "12345");
            Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(40, ChronoUnit.DAYS)));
            Path fresh = Files.writeString(base.resolve("incremental/fresh.zip"), "abc");

            BackupManager.BackupCleanupResult result = manager.cleanupOldBackups(30);

            assertThat(result.getDeletedCount()).isEqualTo(1);
            assertThat(result.getFreedSpace()).isEqualTo(5);
            assertThat(result.getFormattedFreedSpace()).isEqualTo("5 B");
            assertThat(result.getErrors()).isEmpty();
            assertThat(old).doesNotExist();
            assertThat(fresh).exists();
        }

        @Test
        void databaseBackupWritesScriptThroughDatabaseManager() throws Exception {
            Path originalConfigDir = ConfigManager.getConfigDirectory();
            DatabaseManagerTest.configureIsolatedDatabase(Files.createDirectory(dir.resolve("cfg")), 2);
            Constructor<DatabaseManager> ctor = DatabaseManager.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            DatabaseManager db = ctor.newInstance();
            BackupManager withDb = managerWith(db);
            try {
                db.executeUpdate("CREATE TABLE t (id INT)");

                BackupManager.BackupResult result = await(withDb.createDatabaseBackup());

                assertThat(result.isSuccess()).as(result.getMessage()).isTrue();
                assertThat(result.getBackupPath().getParent()).isEqualTo(base.resolve("database"));
                assertThat(Files.readString(result.getBackupPath())).contains("CREATE");
                assertThat(result.getMetadata().getType()).isEqualTo(BackupManager.BackupType.DATABASE);
            } finally {
                withDb.shutdown();
                db.close();
                ConfigManager.setConfigDirectory(originalConfigDir);
            }
        }

        @Test
        void databaseBackupFailureIsReported() throws Exception {
            BackupManager.BackupResult result = await(manager.createDatabaseBackup()); // no DatabaseManager

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).startsWith("Database backup failed");
            assertThat(result.getBackupPath()).isNull();
        }
    }

    @Nested
    class ValueObjects {

        @Test
        void sizesAreFormattedWithBinaryUnits() {
            assertThat(info(100).getFormattedSize()).isEqualTo("100 B");
            assertThat(info(2048).getFormattedSize()).isEqualTo(String.format("%.1f KB", 2.0));
            assertThat(info(5L << 20).getFormattedSize()).isEqualTo(String.format("%.1f MB", 5.0));
            assertThat(info(3L << 30).getFormattedSize()).isEqualTo(String.format("%.1f GB", 3.0));

            assertThat(cleanup(2048).getFormattedFreedSpace()).isEqualTo(String.format("%.1f KB", 2.0));
            assertThat(cleanup(5L << 20).getFormattedFreedSpace()).isEqualTo(String.format("%.1f MB", 5.0));
            assertThat(cleanup(3L << 30).getFormattedFreedSpace()).isEqualTo(String.format("%.1f GB", 3.0));
        }

        private BackupManager.BackupInfo info(long size) {
            return new BackupManager.BackupInfo("n", Path.of("n"), BackupManager.BackupType.FULL, new Date(), size);
        }

        private BackupManager.BackupCleanupResult cleanup(long freed) {
            return new BackupManager.BackupCleanupResult(0, freed, List.of());
        }

        @Test
        void restoreOptionsDefaultToEverythingWithoutOverwrite() {
            BackupManager.RestoreOptions o = new BackupManager.RestoreOptions();
            assertThat(o.isRestoreStudents()).isTrue();
            assertThat(o.isRestoreProfessors()).isTrue();
            assertThat(o.isRestoreCourses()).isTrue();
            assertThat(o.isRestoreDepartments()).isTrue();
            assertThat(o.isRestoreEnrollments()).isTrue();
            assertThat(o.isOverwriteExisting()).isFalse();
            o.setRestoreProfessors(false);
            o.setRestoreCourses(false);
            o.setRestoreDepartments(false);
            assertThat(o.isRestoreProfessors() || o.isRestoreCourses() || o.isRestoreDepartments()).isFalse();
        }

        @Test
        void metadataSetters() {
            BackupManager.BackupMetadata m = new BackupManager.BackupMetadata("a", BackupManager.BackupType.FULL, "t", Map.of(), 1);
            m.setName("b");
            m.setType(BackupManager.BackupType.DATABASE);
            m.setTimestamp("u");
            m.setEntityResults(null);
            m.setDuration(9);
            assertThat(m.getName()).isEqualTo("b");
            assertThat(m.getType()).isEqualTo(BackupManager.BackupType.DATABASE);
            assertThat(m.getTimestamp()).isEqualTo("u");
            assertThat(m.getEntityResults()).isNull();
            assertThat(m.getDuration()).isEqualTo(9);
        }
    }
}
