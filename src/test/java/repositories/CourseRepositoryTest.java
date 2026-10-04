package repositories;

import models.Course;
import models.Department;
import models.Professor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CourseRepositoryTest {

    private CourseRepository repo;
    private Course cs101;
    private Course cs201;
    private Course ma501;

    @BeforeEach
    void setUp() {
        repo = new CourseRepository();
        cs101 = course("CS101", "Intro to Programming", "Learn Java basics", 3, "D-CS", "P1", "Fall", 2024, 4, 2);
        cs201 = course("CS201", "Data Structures", "Trees, graphs and java collections", 4, "D-CS", "P2", "Spring", 2025, 2, 2);
        ma501 = course("MA501", "Real Analysis", "Measure theory", 3, "D-MA", null, "fall", 2024, 10, 0);
        cs201.addPrerequisite(cs101.getCourseId());
    }

    private Course course(String code, String name, String desc, int credits, String dept, String prof,
                          String semester, int year, int capacity, int enrolled) {
        Course c = new Course();
        c.setCourseCode(code);
        c.setCourseName(name);
        c.setDescription(desc);
        c.setCredits(credits);
        c.setDepartmentId(dept);
        if (prof != null) c.setProfessorId(prof);
        c.setSemester(semester);
        c.setYear(year);
        c.setMaxEnrollment(capacity);
        repo.save(c);
        c.setStatus(Course.CourseStatus.OPEN);
        for (int i = 0; i < enrolled; i++) c.enrollStudent(code + "-S" + i);
        return c;
    }

    @Test
    void generatesIds() {
        assertThat(cs101.getCourseId()).isEqualTo("COURSE0001");
        assertThat(ma501.getCourseId()).isEqualTo("COURSE0003");
    }

    @Test
    void lookupQueries() {
        Department cs = new Department("D-CS", "CS", "Computer Science", "", "A");
        Professor p1 = new Professor("U1", "Pat", "Kim", "p@u.edu", null, "P1", "D-CS", Professor.AcademicRank.FULL, "AI");

        assertThat(repo.findByCourseCode("CS201")).containsSame(cs201);
        assertThat(repo.findByCourseCode("XX999")).isEmpty();
        assertThat(repo.findByDepartment(cs)).containsExactlyInAnyOrder(cs101, cs201);
        assertThat(repo.findByProfessor(p1)).containsExactly(cs101);
        assertThat(repo.findByCredits(3)).containsExactlyInAnyOrder(cs101, ma501);
        assertThat(repo.findByCreditRange(4, 6)).containsExactly(cs201);
        assertThat(repo.findByNameContaining("DATA")).containsExactly(cs201);
        assertThat(repo.findByDescriptionContaining("JAVA")).containsExactlyInAnyOrder(cs101, cs201);
        assertThat(repo.findBySemester("FALL")).containsExactlyInAnyOrder(cs101, ma501);
        assertThat(repo.findByAcademicYear("2025")).containsExactly(cs201);
        assertThat(repo.findByCapacityRange(2, 4)).containsExactlyInAnyOrder(cs101, cs201);
    }

    @Test
    void capacityQueries() {
        assertThat(repo.findCoursesWithAvailableSpots()).containsExactlyInAnyOrder(cs101, ma501);
        assertThat(repo.findFullCourses()).containsExactly(cs201);
        assertThat(repo.findByMinEnrollment(2)).containsExactlyInAnyOrder(cs101, cs201);
    }

    @ParameterizedTest
    @CsvSource({"50, 2", "100, 1", "0, 3"})
    void popularCoursesByUtilisation(double minRate, int expectedCount) {
        assertThat(repo.findPopularCourses(minRate)).hasSize(expectedCount);
    }

    @Test
    void underutilisedCourses() {
        assertThat(repo.findUnderutilizedCourses(50)).containsExactlyInAnyOrder(cs101, ma501);
        assertThat(repo.findUnderutilizedCourses(0)).containsExactly(ma501);
    }

    @Test
    void groupingAndAggregates() {
        assertThat(repo.groupByProfessor()).containsOnlyKeys("P1", "P2");
        assertThat(repo.groupBySemester()).containsOnlyKeys("Fall", "Spring", "fall");
        assertThat(repo.getCourseCountByDepartment()).isEqualTo(Map.of("D-CS", 2L, "D-MA", 1L));
        assertThat(repo.getTotalCreditsByDepartment()).isEqualTo(Map.of("D-CS", 7, "D-MA", 3));
        assertThat(repo.getAverageCapacityByDepartment().get("D-CS")).isCloseTo(3.0, within(1e-9));
        assertThat(repo.groupByDepartment().get("D-MA")).containsExactly(ma501);
    }

    @Test
    void enrollmentStatistics() {
        Map<String, Object> stats = repo.getEnrollmentStatistics();

        assertThat(stats).containsEntry("totalCourses", 3)
                .containsEntry("totalCapacity", 16)
                .containsEntry("totalEnrolled", 4)
                .containsEntry("availableSpots", 12);
        assertThat((double) stats.get("utilizationRate")).isCloseTo(25.0, within(1e-9));
        assertThat(new CourseRepository().getEnrollmentStatistics()).containsEntry("utilizationRate", 0.0);
    }

    @Test
    void prerequisites() {
        assertThat(repo.findPrerequisites(cs201)).containsExactly(cs101);
        assertThat(repo.findPrerequisites(cs101)).isEmpty();
        assertThat(repo.findCoursesWithPrerequisite(cs101)).containsExactly(cs201);
    }

    @Test
    void searchCombinesCriteria() {
        Department cs = new Department("D-CS", "CS", "Computer Science", "", "A");

        assertThat(repo.searchCourses(null, cs, null, null, null, null, null)).hasSize(2);
        assertThat(repo.searchCourses("intro", cs, null, "fall", "2024", 3, 3)).containsExactly(cs101);
        assertThat(repo.searchCourses(null, null, null, null, null, 4, null)).containsExactly(cs201);
        assertThat(repo.searchCourses("", null, null, " ", "", null, 3)).containsExactlyInAnyOrder(cs101, ma501);
    }
}
