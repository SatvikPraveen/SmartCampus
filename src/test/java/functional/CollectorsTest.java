package functional;

import enums.GradeLevel;
import models.Course;
import models.Department;
import models.Enrollment;
import models.Professor;
import models.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CollectorsTest {

    private static Student student(String id, Student.AcademicYear year, double gpa, String major, int credits) {
        Student s = new Student("U" + id, "F" + id, "L" + id, id + "@u.edu", null, id, major, year);
        s.setGpa(gpa);
        s.setTotalCredits(credits);
        return s;
    }

    private static Course course(String code, int credits, int capacity, int enrolled) {
        Course c = new Course("C-" + code, code, code, "", credits, "D1");
        c.setMaxEnrollment(capacity);
        c.setStatus(Course.CourseStatus.OPEN);
        for (int i = 0; i < enrolled; i++) c.enrollStudent("S" + i);
        return c;
    }

    private static Enrollment enrollment(Enrollment.Grade grade, int credits) {
        Enrollment e = new Enrollment("E", "S", "C", "Fall", 2024);
        e.setGrade(grade);
        e.setCreditHours(credits);
        return e;
    }

    @Test
    void toTopNKeepsTheLargestElementsInDescendingOrder() {
        // Regression: the heap evicted the largest element, so "top N" returned the N smallest.
        assertThat(Stream.of(5, 1, 4, 2, 3).collect(Collectors.toTopN(2, Comparator.<Integer>naturalOrder())))
                .containsExactly(5, 4);
        assertThat(Stream.of(5, 1, 4).collect(Collectors.toTopN(10, Comparator.<Integer>naturalOrder())))
                .containsExactly(5, 4, 1);
        assertThat(Stream.<Integer>empty().collect(Collectors.toTopN(3, Comparator.<Integer>naturalOrder()))).isEmpty();
    }

    @Test
    void toTopNAgreesWithSortingInParallel() {
        List<Integer> data = IntStream.range(0, 2_000).map(i -> (i * 7919) % 2_003).boxed().toList();

        List<Integer> top = data.parallelStream().collect(Collectors.toTopN(25, Comparator.<Integer>naturalOrder()));

        assertThat(top).isEqualTo(data.stream().sorted(Comparator.reverseOrder()).limit(25).toList());
    }

    @Test
    void topStudentsByGpa() {
        Student a = student("a", Student.AcademicYear.SENIOR, 3.9, "CS", 0);
        Student b = student("b", Student.AcademicYear.SENIOR, 2.1, "CS", 0);
        Student c = student("c", Student.AcademicYear.SENIOR, 3.4, "CS", 0);

        assertThat(Stream.of(a, b, c).collect(Collectors.toTopN(2, Comparator.comparingDouble(Student::getGpa))))
                .containsExactly(a, c);
    }

    @Test
    void genericCollectors() {
        assertThat(Stream.of(3, 1, 2).collect(Collectors.toSortedList(Comparator.<Integer>naturalOrder()))).containsExactly(1, 2, 3);
        assertThat(Stream.of("a", "b", "a").collect(Collectors.toFrequencyMap())).isEqualTo(Map.of("a", 2L, "b", 1L));
        assertThat(Stream.of("b", "a", "b", "c", "a").collect(Collectors.toUniqueList())).containsExactly("b", "a", "c");
        assertThat(Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i > 2)))
                .isEqualTo(Map.of(true, List.of(3, 4), false, List.of(1, 2)));

        Map<String, Double> pct = Stream.of("a", "a", "a", "b").collect(Collectors.toPercentageMap());
        assertThat(pct.get("a")).isCloseTo(75.0, within(1e-9));
        assertThat(pct.get("b")).isCloseTo(25.0, within(1e-9));
        assertThat(Stream.<String>empty().collect(Collectors.toPercentageMap())).isEmpty();
    }

    @Test
    void studentGroupings() {
        Student fr = student("a", Student.AcademicYear.FRESHMAN, 3.8, "CS", 0);
        Student sr = student("b", Student.AcademicYear.SENIOR, 2.5, null, 0);
        Student gr = student("c", Student.AcademicYear.GRADUATE, 0.5, "CS", 10);

        assertThat(Stream.of(fr, sr, gr).collect(Collectors.byClassification()))
                .containsOnlyKeys("Freshman", "Senior", "Graduate");
        assertThat(Stream.of(fr, sr, gr).collect(Collectors.byMajor()))
                .containsOnlyKeys("CS", "Undeclared");
        assertThat(Stream.of(fr, sr, gr).collect(Collectors.honorsStudents())).containsExactly(fr);
        assertThat(Stream.of(fr, sr, gr).collect(Collectors.averageGpa())).isCloseTo(6.8 / 3, within(1e-9));
    }

    @ParameterizedTest
    @CsvSource({"4.0, Excellent (3.7-4.0)", "3.7, Excellent (3.7-4.0)", "3.69, Good (3.0-3.69)",
            "2.0, Satisfactory (2.0-2.99)", "1.0, Below Average (1.0-1.99)", "0.99, Poor (0.0-0.99)"})
    void gpaRangeBoundaries(double gpa, String bucket) {
        Student s = student("a", Student.AcademicYear.JUNIOR, gpa, "CS", 0);

        assertThat(Stream.of(s).collect(Collectors.byGpaRange())).containsOnlyKeys(bucket);
    }

    @Test
    void academicStatistics() {
        Collectors.AcademicStatistics stats = Stream.of(
                student("a", Student.AcademicYear.JUNIOR, 3.0, "CS", 60),
                student("b", Student.AcademicYear.JUNIOR, 1.0, "CS", 30),
                student("c", Student.AcademicYear.JUNIOR, 2.0, "CS", 0)
        ).collect(Collectors.academicStatistics());

        assertThat(stats.getStudentCount()).isEqualTo(3);
        assertThat(stats.getAverageGpa()).isCloseTo(2.0, within(1e-9));
        assertThat(stats.getMinGpa()).isEqualTo(1.0);
        assertThat(stats.getMaxGpa()).isEqualTo(3.0);
        assertThat(stats.getTotalCredits()).isEqualTo(90);
        assertThat(stats.getAverageCredits()).isEqualTo(30.0);

        Collectors.AcademicStatistics empty = Stream.<Student>empty().collect(Collectors.academicStatistics());
        assertThat(empty.getAverageGpa()).isZero();
        assertThat(empty.getMinGpa()).isZero();
        assertThat(empty.getMaxGpa()).isZero();
    }

    @Test
    void courseCollectors() {
        Course empty = course("CS101", 3, 10, 0);
        Course low = course("CS102", 3, 10, 2);
        Course moderate = course("CS103", 4, 10, 5);
        Course nearly = course("CS504", 4, 10, 8);
        Course full = course("CS505", 1, 2, 2);

        Map<String, List<Course>> byStatus = Stream.of(empty, low, moderate, nearly, full)
                .collect(Collectors.coursesByEnrollmentStatus());
        assertThat(byStatus.get("Empty")).containsExactly(empty);
        assertThat(byStatus.get("Low")).containsExactly(low);
        assertThat(byStatus.get("Moderate")).containsExactly(moderate);
        assertThat(byStatus.get("Nearly Full")).containsExactly(nearly);
        assertThat(byStatus.get("Full")).containsExactly(full);

        assertThat(Stream.of(empty, nearly).collect(Collectors.coursesByLevel()))
                .isEqualTo(Map.of("Undergraduate", List.of(empty), "Graduate", List.of(nearly)));
        assertThat(Stream.of(empty, full).collect(Collectors.availableCourses())).containsExactly(empty);
        assertThat(Stream.of(empty, low, moderate).collect(Collectors.coursesByCredits())).containsOnlyKeys(3, 4);

        Collectors.CourseStatistics stats = Stream.of(empty, low, moderate, nearly, full)
                .collect(Collectors.courseStatistics());
        assertThat(stats.getCourseCount()).isEqualTo(5);
        assertThat(stats.getTotalEnrollment()).isEqualTo(17);
        assertThat(stats.getMinEnrollment()).isZero();
        assertThat(stats.getMaxEnrollment()).isEqualTo(8);
        assertThat(stats.getAverageCredits()).isEqualTo(3.0);
        assertThat(Stream.<Course>empty().collect(Collectors.courseStatistics()).getMinEnrollment()).isZero();
    }

    @Test
    void enrollmentCollectors() {
        Enrollment a = enrollment(Enrollment.Grade.A, 4);
        Enrollment c = enrollment(Enrollment.Grade.C, 2);
        Enrollment f = enrollment(Enrollment.Grade.F, 3);
        Enrollment pass = enrollment(Enrollment.Grade.PASS, 3);
        Enrollment ungraded = enrollment(null, 1);

        assertThat(Stream.of(a, c, f, pass, ungraded).collect(Collectors.gpaFromEnrollments()))
                .isCloseTo((4 * 4.0 + 2 * 2.0) / 9, within(1e-9));
        assertThat(Stream.of(pass, ungraded).collect(Collectors.gpaFromEnrollments())).isZero();
        assertThat(Stream.of(a, c, f, pass).collect(Collectors.passingEnrollments())).containsExactly(a, c, pass);
        assertThat(Stream.of(a, c, f, pass).collect(Collectors.failingEnrollments())).containsExactly(f);
        assertThat(Stream.of(a, c, f).collect(Collectors.totalCredits())).isEqualTo(9);
        Map<GradeLevel, List<Enrollment>> byGrade = Stream.of(a, f, ungraded).collect(Collectors.enrollmentsByGrade());
        assertThat(byGrade).containsOnlyKeys(GradeLevel.A, GradeLevel.F, GradeLevel.INCOMPLETE);
        assertThat(byGrade.get(GradeLevel.INCOMPLETE)).containsExactly(ungraded);
    }

    @Test
    void professorCollectors() {
        Professor tenured = new Professor("U1", "A", "B", "a@u.edu", null, "P1", "D1", Professor.AcademicRank.FULL, "AI");
        tenured.setTenured(true);
        Professor adjunct = new Professor("U2", "C", "D", "c@u.edu", null, "P2", "D2", Professor.AcademicRank.ADJUNCT, "DB");

        assertThat(Stream.of(tenured, adjunct).collect(Collectors.tenuredProfessors())).containsExactly(tenured);
        assertThat(Stream.of(tenured, adjunct).collect(Collectors.professorsByEmploymentStatus()))
                .containsOnlyKeys("Tenured", "Adjunct");
        assertThat(Stream.of(tenured, adjunct).collect(Collectors.professorsByRank())).hasSize(2);
        assertThat(Stream.of(tenured, adjunct).collect(Collectors.professorsByDepartment())).containsOnlyKeys("D1", "D2");
    }

    @Test
    void departmentCollectors() {
        Department big = new Department("D1", "A", "A", "", "X", null, new BigDecimal("12000000"));
        Department mid = new Department("D2", "B", "B", "", "X", null, new BigDecimal("1000000"));
        Department none = new Department("D3", "C", "C", "", "X");
        big.addProfessor("P1");
        big.addStudent("S1", "2024");
        mid.addStudent("S2", "2024");

        assertThat(Stream.of(big, mid, none).collect(Collectors.departmentsByBudgetRange()))
                .containsOnlyKeys("Very High (>$10M)", "Medium ($1M-$5M)", "No Budget");
        assertThat(Stream.of(big, mid, none).collect(Collectors.totalBudget())).isEqualTo(13_000_000.0);
        assertThat(Stream.of(big, mid, none).collect(Collectors.totalFacultyCount())).isEqualTo(1);
        assertThat(Stream.of(big, mid, none).collect(Collectors.totalStudentCount())).isEqualTo(2);
    }
}
