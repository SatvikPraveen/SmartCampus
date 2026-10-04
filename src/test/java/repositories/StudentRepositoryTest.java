package repositories;

import models.Department;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StudentRepositoryTest {

    private StudentRepository repo;
    private Department cs;
    private Department math;

    @BeforeEach
    void setUp() {
        repo = new StudentRepository();
        cs = new Department("D-CS", "CS", "Computer Science", "", "Building A, Floor 2");
        math = new Department("D-MA", "MA", "Mathematics", "", "Building B");
    }

    private Student student(String id, String first, String last, String email, String deptId, LocalDate enrolled) {
        Student s = new Student("U-" + first, first, last, email, null, id, "Major", Student.AcademicYear.SOPHOMORE);
        s.setDepartmentId(deptId);
        s.setEnrollmentDate(enrolled);
        return repo.save(s);
    }

    private static Date date(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void generatesSequentialPaddedIds() {
        Student a = repo.save(new Student());
        Student b = repo.save(new Student());

        assertThat(a.getStudentId()).isEqualTo("STU000001");
        assertThat(b.getStudentId()).isEqualTo("STU000002");
    }

    @Test
    void departmentQueries() {
        Student a = student("S1", "Ann", "Lee", "ann@uni.edu", "D-CS", LocalDate.of(2020, 9, 1));
        Student b = student("S2", "Bo", "Kim", "bo@uni.edu", "D-MA", LocalDate.of(2021, 9, 1));
        Student c = student("S3", "Cy", "Ng", "cy@uni.edu", null, LocalDate.of(2021, 1, 15));

        assertThat(repo.findByDepartment(cs)).containsExactly(a);
        assertThat(repo.findByDepartments(List.of(cs, math))).containsExactlyInAnyOrder(a, b);
        assertThat(repo.groupByDepartment()).containsOnlyKeys("D-CS", "D-MA");
        assertThat(repo.getStudentCountByDepartment()).isEqualTo(Map.of("D-CS", 1L, "D-MA", 1L));
        assertThat(c).isNotNull();
    }

    @Test
    void emailAndNameQueries() {
        Student a = student("S1", "Ann", "Lee", "Ann@Uni.edu", "D-CS", LocalDate.of(2020, 9, 1));
        Student b = student("S2", "Joanna", "Smith", "jo@other.org", "D-CS", LocalDate.of(2020, 9, 1));

        assertThat(repo.findByEmail("ann@uni.edu")).containsSame(a); // emails are normalised to lower case
        assertThat(repo.findByEmail("nobody@uni.edu")).isEmpty();
        assertThat(repo.findByEmailDomain("UNI.EDU")).containsExactly(a);
        assertThat(repo.findByNameContaining("ANN")).containsExactlyInAnyOrder(a, b);
        assertThat(repo.findByNameContaining("smi")).containsExactly(b);
    }

    @Test
    void enrollmentDateQueriesAreInclusive() {
        Student a = student("S1", "Ann", "Lee", "a@u.edu", "D-CS", LocalDate.of(2020, 9, 1));
        Student b = student("S2", "Bo", "Kim", "b@u.edu", "D-CS", LocalDate.of(2021, 1, 31));
        Student c = student("S3", "Cy", "Ng", "c@u.edu", "D-CS", LocalDate.of(2021, 9, 1));

        assertThat(repo.findByEnrollmentYear(2021)).containsExactlyInAnyOrder(b, c);
        assertThat(repo.findByEnrollmentDateBetween(date(LocalDate.of(2020, 9, 1)), date(LocalDate.of(2021, 1, 31))))
                .containsExactlyInAnyOrder(a, b);
        assertThat(repo.getEnrollmentStatsByYear()).isEqualTo(Map.of(2020, 1L, 2021, 2L));
    }

    @Test
    void searchCombinesCriteria() {
        Student a = student("S1", "Ann", "Lee", "a@u.edu", "D-CS", LocalDate.of(2020, 9, 1));
        Student b = student("S2", "Anna", "Bell", "b@u.edu", "D-MA", LocalDate.of(2022, 9, 1));
        student("S3", "Cy", "Ng", "c@u.edu", "D-CS", LocalDate.of(2022, 9, 1));

        assertThat(repo.searchStudents("ann", null, null, null)).containsExactlyInAnyOrder(a, b);
        assertThat(repo.searchStudents("ann", cs, null, null)).containsExactly(a);
        assertThat(repo.searchStudents("  ", null, date(LocalDate.of(2022, 1, 1)), null)).hasSize(2);
        assertThat(repo.searchStudents(null, null, null, date(LocalDate.of(2021, 1, 1)))).containsExactly(a);
        assertThat(repo.searchStudents(null, null, null, null)).hasSize(3);
    }

    @Test
    void recentAndActiveStudentsAreRelativeToToday() {
        LocalDate today = LocalDate.now();
        Student fresh = student("S1", "Ann", "Lee", "a@u.edu", "D-CS", today.minusDays(3));
        student("S2", "Bo", "Kim", "b@u.edu", "D-CS", today.minusYears(3));

        assertThat(repo.getRecentEnrollments(7)).containsExactly(fresh);
        assertThat(repo.findActiveStudents()).containsExactly(fresh);
    }

    @Test
    void currentAcademicYearStartsOnAugustFirst() {
        // Regression: the academic year was always taken to start on 1 August of the *calendar* year,
        // so from January to July nobody counted as a current-year student, and 1 August itself was excluded.
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1;
        LocalDate start = LocalDate.of(startYear, 8, 1);

        Student first = student("S1", "Ann", "Lee", "a@u.edu", "D-CS", start);
        Student recent = student("S2", "Bo", "Kim", "b@u.edu", "D-CS", today);
        student("S3", "Cy", "Ng", "c@u.edu", "D-CS", start.minusDays(1));

        assertThat(repo.findCurrentYearStudents()).containsExactlyInAnyOrder(first, recent);
    }
}
