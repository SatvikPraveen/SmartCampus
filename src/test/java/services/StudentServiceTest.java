package services;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SearchResult;
import interfaces.Searchable.SortOrder;
import models.Enrollment;
import models.Enrollment.EnrollmentStatus;
import models.Grade;
import models.Grade.GradeComponent;
import models.Student;
import models.Student.AcademicYear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StudentServiceTest {

    private StudentService service;
    private int gradeSeq;

    @BeforeEach
    void setUp() {
        service = new StudentService();
        gradeSeq = 0;
    }

    private static Student student(String id, String first, String last, String major, AcademicYear year) {
        return new Student("U" + id, first, last, first.toLowerCase() + "@campus.edu", null, id, major, year);
    }

    private Student add(String id, String first, String last, String major, AcademicYear year) {
        Student s = student(id, first, last, major, year);
        assertThat(service.addStudent(s)).isTrue();
        return s;
    }

    /** A graded assignment worth 100 points with the given score. */
    private Grade graded(String studentId, double points) {
        Grade g = new Grade("G" + (++gradeSeq), "E1", studentId, "CS101", "HW" + gradeSeq, GradeComponent.HOMEWORK);
        g.setPointsPossible(100);
        g.submitAssignment();
        assertThat(g.gradeAssignment(points, "P1", "ok")).isTrue();
        return g;
    }

    @Nested
    class Crud {

        @Test
        void addGetAndRemove() {
            Student s = add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);

            assertThat(service.getStudentById("S1")).containsSame(s);
            assertThat(service.getAllStudents()).containsExactly(s);
            assertThat(service.getStudentEnrollments("S1")).isEmpty();

            assertThat(service.removeStudent("S1")).isTrue();
            assertThat(service.getStudentById("S1")).isEmpty();
            assertThat(service.removeStudent("S1")).isFalse();
            assertThat(service.removeStudent("  ")).isFalse();
        }

        @Test
        void rejectsNullDuplicateAndBlankIds() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);

            assertThat(service.addStudent(null)).isFalse();
            assertThat(service.addStudent(student("S1", "Bob", "B", "CS", AcademicYear.SENIOR))).isFalse();
            assertThat(service.addStudent(student("", "Cy", "C", "CS", AcademicYear.SENIOR))).isFalse();
            assertThat(service.getAllStudents()).hasSize(1);
        }

        @Test
        void updateOnlyExisting() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            Student updated = student("S1", "Ada", "Lovelace", "MATH", AcademicYear.SENIOR);

            assertThat(service.updateStudent(updated)).isTrue();
            assertThat(service.getStudentById("S1")).containsSame(updated);
            assertThat(service.updateStudent(student("S9", "X", "Y", "CS", AcademicYear.SENIOR))).isFalse();
            assertThat(service.updateStudent(null)).isFalse();
        }

        @Test
        void enrollmentsAndGradesRequireKnownStudent() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            Enrollment e = Enrollment.createEnrollment("S1", "CS101", "Fall", 2025);

            assertThat(service.addEnrollment("S1", e)).isTrue();
            assertThat(service.addEnrollment("S1", null)).isFalse();
            assertThat(service.addEnrollment("NOPE", e)).isFalse();
            assertThat(service.addGrade("NOPE", graded("NOPE", 90))).isFalse();
            assertThat(service.addGrade("S1", null)).isFalse();
            assertThat(service.getStudentEnrollments("S1")).containsExactly(e);
            assertThat(service.getStudentGrades("NOPE")).isEmpty();
        }
    }

    @Nested
    class Gpa {

        @BeforeEach
        void seed() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
        }

        @Test
        void noGradesMeansZero() {
            assertThat(service.calculateGPA("S1")).isZero();
        }

        @ParameterizedTest
        @CsvSource({"100,4.0", "97,4.0", "93,4.0", "90,3.7", "87,3.3", "83,3.0", "80,2.7",
                    "77,2.3", "73,2.0", "70,1.7", "67,1.3", "63,1.0", "60,0.7", "59.9,0.0", "0,0.0"})
        void mapsPercentageToGradePoints(double points, double expectedGpa) {
            service.addGrade("S1", graded("S1", points));
            assertThat(service.calculateGPA("S1")).isCloseTo(expectedGpa, within(1e-9));
        }

        @Test
        void averagesGradePointsAndRefreshesWhenGradesAdded() {
            service.addGrade("S1", graded("S1", 95)); // 4.0
            assertThat(service.calculateGPA("S1")).isEqualTo(4.0);

            service.addGrade("S1", graded("S1", 85)); // 3.0
            assertThat(service.calculateGPA("S1")).isCloseTo(3.5, within(1e-9));
        }

        @Test
        void ignoresGradesThatDoNotCount() {
            service.addGrade("S1", graded("S1", 95));
            Grade dropped = graded("S1", 10);
            dropped.dropGrade();
            service.addGrade("S1", dropped);
            Grade ungraded = new Grade("GX", "E1", "S1", "CS101", "Draft", GradeComponent.QUIZ);
            service.addGrade("S1", ungraded);

            assertThat(service.calculateGPA("S1")).isEqualTo(4.0);
        }

        @Test
        void recalculatePicksUpOutOfBandChanges() {
            Grade g = graded("S1", 95);
            service.addGrade("S1", g);
            assertThat(service.calculateGPA("S1")).isEqualTo(4.0);

            g.dropGrade(); // mutated directly, cache unaware
            assertThat(service.recalculateGPA("S1")).isZero();
            assertThat(service.calculateGPA("S1")).isZero();
        }

        @Test
        void honorsAndProbation() {
            add("S2", "Bob", "Builder", "ART", AcademicYear.FRESHMAN);
            add("S3", "Cy", "Cyclops", "CS", AcademicYear.SENIOR);
            service.addGrade("S1", graded("S1", 95)); // 4.0
            service.addGrade("S2", graded("S2", 50)); // 0.0
            service.addGrade("S3", graded("S3", 88)); // 3.3

            assertThat(service.getHonorsStudents()).extracting(Student::getStudentId).containsExactly("S1");
            assertThat(service.getStudentsOnProbation()).extracting(Student::getStudentId).containsExactly("S2");
            assertThat(service.getTopStudentsByGPA(2)).extracting(Student::getStudentId).containsExactly("S1", "S3");
            assertThat(service.getStudentsWithGPAAbove(3.3)).extracting(Student::getStudentId).containsExactly("S1", "S3");
            assertThat(service.getAverageGPAByMajor().get("CS")).isCloseTo(3.65, within(1e-9));
            assertThat(service.getGradeDistribution())
                    .containsEntry("A (3.8-4.0)", 1L)
                    .containsEntry("B+ (3.3-3.7)", 1L)
                    .containsEntry("F (0.0-0.9)", 1L);
        }
    }

    @Nested
    class Queries {

        @BeforeEach
        void seed() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            add("S2", "Alan", "Turing", "cs", AcademicYear.SENIOR);
            add("S3", "Grace", "Hopper", "MATH", AcademicYear.JUNIOR);
        }

        @Test
        void byMajorIsCaseInsensitiveAndSortedByName() {
            assertThat(service.getStudentsByMajor("Cs")).extracting(Student::getStudentId).containsExactly("S1", "S2");
        }

        @Test
        void byAcademicYear() {
            assertThat(service.getStudentsByAcademicYear(AcademicYear.JUNIOR))
                    .extracting(Student::getStudentId).containsExactlyInAnyOrder("S1", "S3");
            assertThat(service.getEnrollmentStatisticsByAcademicYear())
                    .containsEntry(AcademicYear.JUNIOR, 2L).containsEntry(AcademicYear.SENIOR, 1L);
        }

        @Test
        void byEnrollmentAndCourse() {
            Enrollment active = Enrollment.createEnrollment("S1", "CS101", "Fall", 2025);
            Enrollment dropped = Enrollment.createEnrollment("S2", "CS101", "Fall", 2025);
            dropped.dropEnrollment("x");
            service.addEnrollment("S1", active);
            service.addEnrollment("S2", dropped);

            assertThat(service.getStudentsInCourse("CS101")).extracting(Student::getStudentId).containsExactly("S1");
            assertThat(service.getStudentsByEnrollmentStatus(EnrollmentStatus.DROPPED))
                    .extracting(Student::getStudentId).containsExactly("S2");
        }

        @Test
        void criteriaAndKeywordSearch() {
            assertThat(service.findStudentsByCriteria(Map.of("major", "CS", "academicYear", AcademicYear.JUNIOR)))
                    .extracting(Student::getStudentId).containsExactly("S1");
            assertThat(service.search("hopper")).extracting(Student::getStudentId).containsExactly("S3");
            assertThat(service.search("SENIOR")).extracting(Student::getStudentId).containsExactly("S2");
            assertThat(service.search(Map.of("firstName", new SearchCriterion(SearchCriteria.STARTS_WITH, "a"))))
                    .hasSize(2);
        }

        @Test
        void sortingAndPagination() {
            assertThat(service.searchAndSort("", "firstName", SortOrder.DESC))
                    .extracting(Student::getFirstName).containsExactly("Grace", "Alan", "Ada");

            SearchResult<Student> page = service.searchWithPagination("", 0, 2);
            assertThat(page.getResults()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        void suggestions() {
            assertThat(service.getSearchSuggestions("al", 10)).contains("Alan").doesNotContain("Grace");
        }
    }

    @Nested
    class StatisticsAndReports {

        @Test
        void freshServiceStatisticsArePopulated() {
            // Regression: cache timestamp initialised in the constructor made a fresh
            // service return an empty cached map.
            assertThat(service.calculateOverallStatistics())
                    .containsEntry("totalStudents", 0)
                    .containsEntry("medianGPA", 0.0);
        }

        @Test
        void statisticsReflectAddedStudents() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            add("S2", "Bob", "B", "CS", AcademicYear.JUNIOR);
            add("S3", "Cy", "C", "CS", AcademicYear.JUNIOR);
            service.addGrade("S1", graded("S1", 95)); // 4.0
            service.addGrade("S2", graded("S2", 85)); // 3.0
            service.addGrade("S3", graded("S3", 50)); // 0.0

            Map<String, Object> stats = service.calculateOverallStatistics();
            assertThat(stats).containsEntry("totalStudents", 3)
                    .containsEntry("medianGPA", 3.0)
                    .containsEntry("maxGPA", 4.0)
                    .containsEntry("minGPA", 0.0);
            assertThat((double) stats.get("averageGPA")).isCloseTo(7.0 / 3, within(1e-9));
        }

        @Test
        void enrollmentReport() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            service.addEnrollment("S1", Enrollment.createEnrollment("S1", "CS101", "Fall", 2025));

            ReportData report = service.generateReport(ReportType.ENROLLMENT_REPORT);
            assertThat(report.getRows()).singleElement().satisfies(row ->
                    assertThat(row).containsEntry("Enrollments", 1).containsEntry("Status", "Active"));
        }
    }
}
