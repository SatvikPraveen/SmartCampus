package concurrent;

import concurrent.ConcurrentGradeCalculator.CourseStatistics;
import concurrent.ConcurrentGradeCalculator.DepartmentStatistics;
import concurrent.ConcurrentGradeCalculator.GPAResult;
import concurrent.ConcurrentGradeCalculator.GradeTrends;
import concurrent.ConcurrentGradeCalculator.SemesterStatistics;
import models.Course;
import models.Department;
import models.Enrollment;
import models.Enrollment.EnrollmentStatus;
import models.Enrollment.EnrollmentType;
import models.Enrollment.Grade;
import models.Student;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import repositories.CourseRepository;
import repositories.EnrollmentRepository;
import repositories.StudentRepository;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;

@Timeout(30)
class ConcurrentGradeCalculatorTest {

    private static final double EPS = 1e-9;

    private EnrollmentRepository enrollments;
    private StudentRepository students;
    private CourseRepository courses;
    private ConcurrentGradeCalculator calculator;
    private int nextId;

    @BeforeEach
    void setUp() {
        enrollments = new EnrollmentRepository();
        students = new StudentRepository();
        courses = new CourseRepository();
        calculator = new ConcurrentGradeCalculator(enrollments, students, courses);
    }

    @AfterEach
    void tearDown() {
        calculator.shutdown();
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    private Student student(String id) {
        return students.save(new Student("U" + id, "First", "Last", id.toLowerCase() + "@u.edu",
                null, id, "CS", Student.AcademicYear.JUNIOR));
    }

    private Course course(String id, String departmentId) {
        return courses.save(new Course(id, "CODE-" + id, "Course " + id, "", 3, departmentId));
    }

    private static Department department(String... courseIds) {
        Department d = new Department("D-CS", "CS", "Computer Science", "", "Hall A");
        for (String c : courseIds) {
            d.addCourse(c);
        }
        return d;
    }

    private Enrollment grade(String studentId, String courseId, String semester, int year,
                             Grade grade, double numeric, int credits) {
        Enrollment e = new Enrollment("E" + (++nextId), studentId, courseId, semester, year,
                EnrollmentStatus.COMPLETED, EnrollmentType.REGULAR, "001", credits);
        e.setGrade(grade);
        e.setNumericGrade(numeric);
        return enrollments.save(e);
    }

    private Enrollment grade(String studentId, String courseId, Grade grade, double numeric) {
        return grade(studentId, courseId, "Fall", 2024, grade, numeric, 3);
    }

    /** Sequential reference GPA: credit-weighted grade points over GPA-counting enrollments. */
    private static double referenceGpa(List<Enrollment> list) {
        double points = 0;
        int credits = 0;
        for (Enrollment e : list) {
            if (e.getGrade() != null && e.getGrade() != Grade.NOT_GRADED && e.countsTowardGpa()) {
                points += e.getGrade().getPoints() * e.getCreditHours();
                credits += e.getCreditHours();
            }
        }
        return credits > 0 ? points / credits : 0.0;
    }

    @Nested
    class StudentGpa {

        @Test
        void weightsByCreditsAndExcludesNonGpaGrades() throws Exception {
            Student s = student("S1");
            grade("S1", "C1", "Fall", 2024, Grade.A, 95, 3);
            grade("S1", "C2", "Fall", 2024, Grade.B, 85, 4);
            grade("S1", "C3", "Fall", 2024, Grade.PASS, 70, 2);
            grade("S1", "C4", "Fall", 2024, Grade.NOT_GRADED, -1, 3);
            grade("S2", "C1", "Fall", 2024, Grade.F, 10, 3);

            GPAResult result = await(calculator.calculateStudentGPAAsync(s));

            assertThat(result.getStudent()).isSameAs(s);
            assertThat(result.getGpa()).isCloseTo((4.0 * 3 + 3.0 * 4) / 7, within(EPS));
            assertThat(result.getTotalCredits()).isEqualTo(7);
            assertThat(result.getGradeDistribution())
                    .containsOnly(entry("A", 1), entry("B", 1), entry("P", 1));
        }

        @Test
        void studentWithoutGradesHasZeroGpa() throws Exception {
            Student s = student("S1");
            grade("S1", "C1", "Fall", 2024, Grade.NOT_GRADED, -1, 3);

            GPAResult result = await(calculator.calculateStudentGPAAsync(s));

            assertThat(result.getGpa()).isZero();
            assertThat(result.getTotalCredits()).isZero();
            assertThat(result.getGradeDistribution()).isEmpty();
        }

        @Test
        void onlyPassFailGradesGiveZeroGpaButKeepDistribution() throws Exception {
            Student s = student("S1");
            grade("S1", "C1", "Fall", 2024, Grade.PASS, 80, 3);

            GPAResult result = await(calculator.calculateStudentGPAAsync(s));

            assertThat(result.getGpa()).isZero();
            assertThat(result.getGradeDistribution()).containsOnly(entry("P", 1));
        }

        @Test
        void batchMatchesSequentialReferenceInInputOrder() throws Exception {
            Random random = new Random(42);
            Grade[] pool = {Grade.A, Grade.A_MINUS, Grade.B_PLUS, Grade.B, Grade.C, Grade.D, Grade.F, Grade.PASS};
            List<Student> roster = IntStream.range(0, 40).mapToObj(i -> student("S" + i)).toList();
            for (Student s : roster) {
                for (int c = 0; c < 6; c++) {
                    grade(s.getStudentId(), "C" + c, "Fall", 2024,
                            pool[random.nextInt(pool.length)], random.nextInt(101), 1 + random.nextInt(4));
                }
            }

            List<GPAResult> results = await(calculator.calculateBatchStudentGPAs(roster));

            assertThat(results).extracting(GPAResult::getStudent).containsExactlyElementsOf(roster);
            for (GPAResult r : results) {
                assertThat(r.getGpa()).isCloseTo(referenceGpa(enrollments.findByStudent(r.getStudent())), within(EPS));
            }
        }
    }

    @Nested
    class CourseStats {

        @Test
        void summarisesGradedEnrollmentsOfTheCourse() throws Exception {
            Course c1 = course("C1", "D-CS");
            grade("S1", "C1", Grade.A, 95);
            grade("S2", "C1", Grade.B, 82);
            grade("S3", "C1", Grade.B, 80);
            grade("S4", "C1", Grade.NOT_GRADED, -1);
            grade("S1", "C2", Grade.F, 10);

            CourseStatistics stats = await(calculator.calculateCourseStatisticsAsync(c1));

            assertThat(stats.getCourse()).isSameAs(c1);
            assertThat(stats.getStudentCount()).isEqualTo(3);
            assertThat(stats.getAverageGrade()).isCloseTo((95 + 82 + 80) / 3.0, within(EPS));
            assertThat(stats.getMinGrade()).isEqualTo(80);
            assertThat(stats.getMaxGrade()).isEqualTo(95);
            assertThat(stats.getGradeDistribution()).containsOnly(entry("A", 1L), entry("B", 2L));
        }

        @Test
        void courseWithoutGradesYieldsZeroes() throws Exception {
            CourseStatistics stats = await(calculator.calculateCourseStatisticsAsync(course("C1", "D-CS")));

            assertThat(stats.getStudentCount()).isZero();
            assertThat(stats.getAverageGrade()).isZero();
            assertThat(stats.getMinGrade()).isZero();
            assertThat(stats.getMaxGrade()).isZero();
            assertThat(stats.getGradeDistribution()).isEmpty();
        }
    }

    @Nested
    class DepartmentStats {

        @Test
        void averagesPerKnownCourseAndOverall() throws Exception {
            Course c1 = course("C1", "D-CS");
            Course c2 = course("C2", "D-CS");
            grade("S1", "C1", Grade.A, 90);
            grade("S2", "C1", Grade.B, 80);
            grade("S1", "C2", Grade.C, 70);
            grade("S1", "C9", Grade.F, 40); // course in department but not in the repository
            grade("S1", "OTHER", Grade.A, 100); // course outside the department

            DepartmentStatistics stats = await(calculator.calculateDepartmentStatisticsAsync(
                    department("C1", "C2", "C9")));

            assertThat(stats.getDepartment().getDepartmentId()).isEqualTo("D-CS");
            assertThat(stats.getCourseAverages()).containsOnly(entry(c1, 85.0), entry(c2, 70.0));
            assertThat(stats.getOverallAverage()).isCloseTo((90 + 80 + 70 + 40) / 4.0, within(EPS));
            assertThat(stats.getGradeDistribution())
                    .containsOnly(entry("A", 1L), entry("B", 1L), entry("C", 1L), entry("F", 1L));
        }

        @Test
        void departmentWithoutGradesYieldsEmptyStatistics() throws Exception {
            DepartmentStatistics stats = await(calculator.calculateDepartmentStatisticsAsync(department("C1")));

            assertThat(stats.getCourseAverages()).isEmpty();
            assertThat(stats.getOverallAverage()).isZero();
            assertThat(stats.getGradeDistribution()).isEmpty();
        }

        // Regression: above the fork/join threshold (1000 grades) partial results were merged by
        // averaging the two halves' averages, ignoring how many grades each half held, so
        // per-course and overall averages diverged from the true (count-weighted) means.
        @Test
        void forkJoinAveragesMatchSequentialReferenceAboveSplitThreshold() throws Exception {
            Random random = new Random(7);
            List<Course> deptCourses = IntStream.range(0, 5).mapToObj(i -> course("C" + i, "D-CS")).toList();
            Grade[] letters = Grade.values();
            for (int i = 0; i < 2501; i++) {
                // skew grades per course so that unequal course counts per half matter
                int c = random.nextInt(5);
                grade("S" + random.nextInt(300), "C" + c, letters[random.nextInt(13)],
                        Math.min(100, c * 15 + random.nextInt(40)));
            }
            List<Enrollment> all = enrollments.findAll();
            Map<String, Double> expectedCourseAverages = all.stream().collect(Collectors.groupingBy(
                    Enrollment::getCourseId, Collectors.averagingDouble(Enrollment::getNumericGrade)));
            double expectedOverall = all.stream().mapToDouble(Enrollment::getNumericGrade).average().orElseThrow();
            Map<String, Long> expectedDistribution = all.stream().collect(Collectors.groupingBy(
                    e -> e.getGrade().getLetter(), Collectors.counting()));

            DepartmentStatistics stats = await(calculator.calculateDepartmentStatisticsAsync(
                    department("C0", "C1", "C2", "C3", "C4")));

            assertThat(stats.getOverallAverage()).isCloseTo(expectedOverall, within(1e-9));
            assertThat(stats.getCourseAverages()).hasSize(5);
            for (Course c : deptCourses) {
                assertThat(stats.getCourseAverages().get(c))
                        .as("average of %s", c.getCourseId())
                        .isCloseTo(expectedCourseAverages.get(c.getCourseId()), within(1e-9));
            }
            assertThat(stats.getGradeDistribution()).isEqualTo(expectedDistribution);
        }
    }

    @Nested
    class SemesterStats {

        @Test
        void aggregatesTheMatchingTermOfTheAcademicYear() throws Exception {
            course("C1", "D-CS");
            course("C2", "D-MATH");
            grade("S1", "C1", "Fall", 2024, Grade.A, 90, 3);
            grade("S2", "C1", "fall", 2024, Grade.B, 80, 3);
            grade("S1", "C2", "Fall", 2024, Grade.A, 94, 3);
            grade("S3", "UNKNOWN", "Fall", 2024, Grade.C, 72, 3); // not in course repository
            grade("S4", "C1", "Spring", 2025, Grade.F, 10, 3);    // same academic year, other term
            grade("S5", "C1", "Fall", 2023, Grade.F, 10, 3);      // other academic year

            SemesterStatistics stats = await(calculator.calculateSemesterStatisticsAsync("Fall", "2024-2025"));

            assertThat(stats.getSemester()).isEqualTo("Fall");
            assertThat(stats.getAcademicYear()).isEqualTo("2024-2025");
            assertThat(stats.getTotalStudents()).isEqualTo(3);
            assertThat(stats.getAverageGrade()).isCloseTo((90 + 80 + 94 + 72) / 4.0, within(EPS));
            assertThat(stats.getDepartmentDistribution()).isEqualTo(Map.of(
                    "D-CS", Map.of("A", 1L, "B", 1L),
                    "D-MATH", Map.of("A", 1L)));
            assertThat(stats.getOverallDistribution())
                    .containsOnly(entry("A", 2L), entry("B", 1L), entry("C", 1L));
        }

        @Test
        void emptySemesterYieldsZeroes() throws Exception {
            SemesterStatistics stats = await(calculator.calculateSemesterStatisticsAsync("Fall", "1999-2000"));

            assertThat(stats.getTotalStudents()).isZero();
            assertThat(stats.getAverageGrade()).isZero();
            assertThat(stats.getDepartmentDistribution()).isEmpty();
            assertThat(stats.getOverallDistribution()).isEmpty();
        }
    }

    @Nested
    class Trends {

        private Department seedFourTerms() {
            grade("S1", "C1", "Fall", 2023, Grade.C, 60, 3);
            grade("S2", "C1", "Fall", 2023, Grade.C, 70, 3);
            grade("S1", "C1", "Spring", 2024, Grade.B, 75, 3);
            grade("S1", "C1", "Summer", 2024, Grade.B, 80, 3);
            grade("S1", "C1", "Fall", 2024, Grade.A, 95, 3);
            grade("S1", "OTHER", "Fall", 2024, Grade.F, 0, 3);
            return department("C1");
        }

        @Test
        void ordersTermsChronologicallyAndAveragesConsecutiveChanges() throws Exception {
            GradeTrends trends = await(calculator.calculateGradeTrendsAsync(seedFourTerms(), 10));

            assertThat(trends.getSemesterAverages()).containsExactly(
                    entry("Fall 2023", 65.0), entry("Spring 2024", 75.0),
                    entry("Summer 2024", 80.0), entry("Fall 2024", 95.0));
            assertThat(trends.getOverallTrend()).isCloseTo((95.0 - 65.0) / 3, within(EPS));
            assertThat(trends.getDepartment().getDepartmentId()).isEqualTo("D-CS");
        }

        @Test
        void keepsOnlyTheMostRecentTerms() throws Exception {
            GradeTrends trends = await(calculator.calculateGradeTrendsAsync(seedFourTerms(), 2));

            assertThat(trends.getSemesterAverages()).containsExactly(
                    entry("Summer 2024", 80.0), entry("Fall 2024", 95.0));
            assertThat(trends.getOverallTrend()).isCloseTo(15.0, within(EPS));
        }

        @Test
        void singleTermHasNoTrend() throws Exception {
            grade("S1", "C1", "Fall", 2024, Grade.A, 95, 3);

            GradeTrends trends = await(calculator.calculateGradeTrendsAsync(department("C1"), 5));

            assertThat(trends.getSemesterAverages()).containsExactly(entry("Fall 2024", 95.0));
            assertThat(trends.getOverallTrend()).isZero();
        }
    }

    @Nested
    class StudentLists {

        @Test
        void failingStudentsAreThoseBelowThresholdAndKnown() throws Exception {
            Student good = student("S1");
            Student bad = student("S2");
            grade("S1", "C1", Grade.A, 95);
            grade("S2", "C1", Grade.D, 62);
            grade("S2", "C2", Grade.F, 30);
            grade("GHOST", "C1", Grade.F, 0); // not in student repository

            assertThat(await(calculator.findFailingStudentsAsync(2.0))).containsExactly(bad);
            assertThat(await(calculator.findFailingStudentsAsync(5.0))).containsExactlyInAnyOrder(good, bad);
        }

        @Test
        void honorRollConsidersOnlyTheRequestedTerm() throws Exception {
            Student s1 = student("S1");
            Student s2 = student("S2");
            student("S3");
            grade("S1", "C1", "Fall", 2024, Grade.A, 95, 3);
            grade("S1", "C2", "Fall", 2024, Grade.A_MINUS, 91, 3);
            grade("S2", "C1", "Fall", 2024, Grade.B, 85, 3);
            grade("S2", "C2", "Spring", 2025, Grade.A, 99, 3);
            grade("S3", "C1", "Fall", 2023, Grade.A, 99, 3);

            assertThat(await(calculator.calculateHonorRollAsync(3.5, "Fall", "2024-2025"))).containsExactly(s1);
            assertThat(await(calculator.calculateHonorRollAsync(3.0, "Fall", "2024-2025")))
                    .containsExactlyInAnyOrder(s1, s2);
        }
    }

    @Test
    void shutdownStopsAcceptingWork() {
        Course c1 = course("C1", "D-CS");
        calculator.shutdown();

        assertThatThrownBy(() -> calculator.calculateCourseStatisticsAsync(c1))
                .isInstanceOf(RejectedExecutionException.class);
    }
}
