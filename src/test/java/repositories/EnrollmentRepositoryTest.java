package repositories;

import models.Course;
import models.Department;
import models.Enrollment;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentRepositoryTest {

    private EnrollmentRepository repo;
    private Enrollment e1;
    private Enrollment e2;
    private Enrollment e3;
    private Enrollment e4;
    private Student s1;
    private Course c1;

    @BeforeEach
    void setUp() {
        repo = new EnrollmentRepository();
        e1 = enrollment("S1", "C1", "Fall", 2024, LocalDateTime.of(2024, 8, 20, 9, 0));
        e2 = enrollment("S1", "C2", "Spring", 2025, LocalDateTime.of(2025, 1, 10, 9, 0));
        e3 = enrollment("S2", "C1", "Fall", 2024, LocalDateTime.of(2024, 8, 25, 9, 0));
        e4 = enrollment("S3", "C3", "Summer", 2025, LocalDateTime.of(2025, 6, 1, 9, 0));
        s1 = new Student("U1", "Ann", "Lee", "a@u.edu", null, "S1", "CS", Student.AcademicYear.FRESHMAN);
        c1 = new Course("C1", "CS101", "Intro", "", 3, "D-CS");
    }

    private Enrollment enrollment(String student, String course, String semester, int year, LocalDateTime when) {
        Enrollment e = new Enrollment(null, student, course, semester, year);
        e.setEnrollmentDate(when);
        return repo.save(e);
    }

    private static Date date(LocalDateTime t) {
        return Date.from(t.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void generatesIds() {
        assertThat(e1.getEnrollmentId()).isEqualTo("ENR000001");
        assertThat(e4.getEnrollmentId()).isEqualTo("ENR000004");
    }

    @Test
    void studentAndCourseLookups() {
        Course other = new Course("C9", "CS999", "Other", "", 3, "D-CS");

        assertThat(repo.findByStudent(s1)).containsExactlyInAnyOrder(e1, e2);
        assertThat(repo.findByCourse(c1)).containsExactlyInAnyOrder(e1, e3);
        assertThat(repo.findByStudentAndCourse(s1, c1)).containsSame(e1);
        assertThat(repo.findByStudentAndCourse(s1, other)).isEmpty();
    }

    @Test
    void departmentLookupUsesDepartmentCourses() {
        Department cs = new Department("D-CS", "CS", "Computer Science", "", "A");
        cs.addCourse("C1");
        cs.addCourse("C3");

        assertThat(repo.findByDepartment(cs)).containsExactlyInAnyOrder(e1, e3, e4);
    }

    @Test
    void dateQueries() {
        assertThat(repo.findByEnrollmentDateBetween(date(LocalDateTime.of(2024, 8, 20, 9, 0)),
                date(LocalDateTime.of(2025, 1, 10, 9, 0)))).containsExactlyInAnyOrder(e1, e2, e3);
        assertThat(repo.findByEnrollmentYear(2025)).containsExactlyInAnyOrder(e2, e4);
        assertThat(repo.getEnrollmentTrendsByMonth())
                .isEqualTo(Map.of("2024-08", 2L, "2025-01", 1L, "2025-06", 1L));
    }

    @Test
    void recentEnrollmentsAreRelativeToNow() {
        Enrollment fresh = enrollment("S4", "C1", "Fall", 2024, LocalDateTime.now().minusDays(1));

        assertThat(repo.findRecentEnrollments(7)).containsExactly(fresh);
    }

    @Test
    void semesterQueriesIgnoreCase() {
        assertThat(repo.findBySemester("FALL")).containsExactlyInAnyOrder(e1, e3);
        assertThat(repo.groupBySemester()).containsOnlyKeys("Fall", "Spring", "Summer");
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({"Fall, 2024, 2024-2025", "Fall 2024, 2024, 2024-2025", "Spring, 2025, 2024-2025",
            "Summer, 2025, 2024-2025", "Spring, 2024, 2023-2024"})
    void academicYearIsDerivedFromSemesterAndYear(String semester, int year, String academicYear) {
        EnrollmentRepository fresh = new EnrollmentRepository();
        Enrollment e = fresh.save(new Enrollment(null, "S", "C", semester, year));

        assertThat(fresh.findByAcademicYear(academicYear)).containsExactly(e);
    }

    @Test
    void groupByAcademicYear() {
        assertThat(repo.groupByAcademicYear()).containsOnlyKeys("2024-2025");
        assertThat(repo.findByAcademicYear("2024-2025")).hasSize(4);
    }

    @Test
    void countsAndThresholds() {
        assertThat(repo.getEnrollmentCountByCourse()).isEqualTo(Map.of("C1", 2L, "C2", 1L, "C3", 1L));
        assertThat(repo.getStudentCourseCount()).isEqualTo(Map.of("S1", 2L, "S2", 1L, "S3", 1L));
        assertThat(repo.findStudentsWithMultipleCourses()).containsExactly("S1");
        assertThat(repo.findCoursesWithLowEnrollment(2)).containsExactlyInAnyOrder("C2", "C3");
        assertThat(repo.findCoursesWithHighEnrollment(2)).containsExactly("C1");
        assertThat(repo.groupByStudent().get("S1")).containsExactlyInAnyOrder(e1, e2);
        assertThat(repo.groupByCourse().get("C1")).containsExactlyInAnyOrder(e1, e3);
    }

    @Test
    void statistics() {
        assertThat(repo.getEnrollmentStatistics())
                .containsEntry("totalEnrollments", 4L)
                .containsEntry("uniqueStudents", 3L)
                .containsEntry("uniqueCourses", 3L)
                .containsEntry("averageCoursesPerStudent", 4.0 / 3)
                .containsEntry("averageStudentsPerCourse", 4.0 / 3);
    }

    @Test
    void searchCombinesCriteria() {
        assertThat(repo.searchEnrollments(s1, null, "fall", null, null, null)).containsExactly(e1);
        assertThat(repo.searchEnrollments(null, c1, null, "2024-2025", null, null)).containsExactlyInAnyOrder(e1, e3);
        assertThat(repo.searchEnrollments(null, null, null, null,
                date(LocalDateTime.of(2025, 1, 1, 0, 0)), null)).containsExactlyInAnyOrder(e2, e4);
        assertThat(repo.searchEnrollments(null, null, " ", "", null,
                date(LocalDateTime.of(2024, 8, 21, 0, 0)))).containsExactly(e1);
    }
}
