package services;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
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

import java.time.LocalDateTime;
import java.util.List;
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

    @Nested
    class SearchingAndSorting {

        @BeforeEach
        void seed() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            add("S2", "Alan", "Turing", "cs", AcademicYear.SENIOR);
            add("S3", "Grace", "Hopper", "MATH", AcademicYear.JUNIOR);
        }

        private List<String> ids(List<Student> students) {
            return students.stream().map(Student::getStudentId).toList();
        }

        @ParameterizedTest
        @CsvSource({"firstName,S1;S2;S3", "lastName,S3;S1;S2", "email,S1;S2;S3", "studentId,S1;S2;S3",
                    "major,S1;S3;S2", "unknownField,S3;S1;S2"})
        void sortsAscendingByField(String field, String expected) {
            assertThat(ids(service.searchAndSort("", field, SortOrder.ASC))).containsExactly(expected.split(";"));
        }

        @Test
        void sortsByAcademicYearAndGpa() {
            service.addGrade("S1", graded("S1", 95)); // 4.0
            service.addGrade("S2", graded("S2", 85)); // 3.0

            assertThat(ids(service.searchAndSort("", "academicYear", SortOrder.DESC)).get(0)).isEqualTo("S2");
            assertThat(ids(service.searchAndSort("", "gpa", SortOrder.ASC))).containsExactly("S3", "S2", "S1");
        }

        @Test
        void criteriaOperatorsAndFields() {
            assertThat(ids(service.search(Map.of("lastName", new SearchCriterion(SearchCriteria.EXACT_MATCH, "TURING")))))
                    .containsExactly("S2");
            assertThat(ids(service.search(Map.of("email", new SearchCriterion(SearchCriteria.ENDS_WITH, "ce@campus.edu")))))
                    .containsExactly("S3");
            assertThat(service.search(Map.of("studentId", new SearchCriterion(SearchCriteria.CONTAINS, "s")))).hasSize(3);
            assertThat(service.search(Map.of("gpa", new SearchCriterion(SearchCriteria.EXACT_MATCH, "0.0")))).hasSize(3);
            assertThat(service.search(Map.of("firstName", new SearchCriterion(SearchCriteria.REGEX, "A.*")))).isEmpty();
            assertThat(service.search(Map.of("nickname", new SearchCriterion(SearchCriteria.CONTAINS, "a")))).isEmpty();
            assertThat(service.countSearchResults(
                    Map.of("academicYear", new SearchCriterion(SearchCriteria.EXACT_MATCH, "junior")))).isEqualTo(2);
            assertThat(service.countSearchResults("a")).isEqualTo(3);
        }

        @Test
        void exactValueCriteria() {
            assertThat(ids(service.findStudentsByCriteria(Map.of("email", "ada@campus.edu")))).containsExactly("S1");
            assertThat(ids(service.findStudentsByCriteria(Map.of("studentId", "S3", "gpa", 0.0)))).containsExactly("S3");
            assertThat(service.findStudentsByCriteria(Map.of("nickname", "x"))).isEmpty();
        }

        @Test
        void predicateFilters() {
            assertThat(ids(service.filterStudents(s -> s.getFirstName().startsWith("A"))))
                    .containsExactlyInAnyOrder("S1", "S2");
            assertThat(ids(service.filter(s -> s.getMajor().equals("MATH")))).containsExactly("S3");
            assertThat(ids(service.search((Student s) -> s.getLastName().equals("Turing")))).containsExactly("S2");
        }

        @Test
        void advancedPagination() {
            SearchResult<Student> page = service.advancedSearchWithPagination(
                    Map.of("major", new SearchCriterion(SearchCriteria.EXACT_MATCH, "cs")),
                    "firstName", SortOrder.DESC, 0, 1);

            assertThat(ids(page.getResults())).containsExactly("S2");
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.getSortBy()).isEqualTo("firstName");
            assertThat(page.getSortOrder()).isEqualTo(SortOrder.DESC);

            SearchResult<Student> second = service.advancedSearchWithPagination(
                    Map.of(), "lastName", SortOrder.ASC, 1, 2);
            assertThat(ids(second.getResults())).containsExactly("S2");
            assertThat(second.hasPrevious()).isTrue();
        }

        @Test
        void fieldListsAndMajorStatistics() {
            assertThat(service.getSearchableFields()).contains("major", "academicYear").doesNotContain("gpa");
            assertThat(service.getSortableFields()).contains("gpa");
            assertThat(service.getEnrollmentStatisticsByMajor())
                    .containsEntry("CS", 1L).containsEntry("cs", 1L).containsEntry("MATH", 1L);
            assertThat(service.getSearchSuggestions("a", 2)).hasSize(2);
        }
    }

    @Nested
    class MoreStatistics {

        @Test
        void emptyServiceReportsZeroMinAndMaxGpa() {
            // Regression: DoubleSummaryStatistics over no students reported +/-Infinity.
            assertThat(service.calculateOverallStatistics())
                    .containsEntry("minGPA", 0.0)
                    .containsEntry("maxGPA", 0.0)
                    .containsEntry("averageGPA", 0.0)
                    .containsEntry("standardDeviationGPA", 0.0);
        }

        @Test
        void evenCountMedianAndStandardDeviation() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            add("S2", "Bob", "B", "CS", AcademicYear.JUNIOR);
            service.addGrade("S1", graded("S1", 95)); // 4.0
            service.addGrade("S2", graded("S2", 85)); // 3.0
            service.addEnrollment("S1", Enrollment.createEnrollment("S1", "CS101", "Fall", 2025));

            Map<String, Object> stats = service.getSummaryStatistics();
            assertThat(stats).containsEntry("medianGPA", 3.5)
                    .containsEntry("standardDeviationGPA", 0.5)
                    .containsEntry("activeStudents", 2L)
                    .containsEntry("totalEnrollments", 1L);
        }

        @Test
        void statisticsAreCachedUntilAMutation() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            Map<String, Object> first = service.calculateOverallStatistics();
            assertThat(service.calculateOverallStatistics()).isEqualTo(first);

            Student s1 = service.getStudentById("S1").orElseThrow();
            s1.setActive(false); // out-of-band change: cached snapshot is still served
            assertThat(service.calculateOverallStatistics()).containsEntry("activeStudents", 1L);

            service.updateStudent(s1);
            assertThat(service.calculateOverallStatistics()).containsEntry("activeStudents", 0L);
        }

        @Test
        void gradeDistributionCoversMiddleBands() {
            add("S1", "A", "A", "CS", AcademicYear.JUNIOR);
            add("S2", "B", "B", "CS", AcademicYear.JUNIOR);
            add("S3", "C", "C", "CS", AcademicYear.JUNIOR);
            service.addGrade("S1", graded("S1", 80)); // B- 2.7
            service.addGrade("S2", graded("S2", 73)); // C 2.0
            service.addGrade("S3", graded("S3", 63)); // D 1.0

            assertThat(service.getGradeDistribution())
                    .containsEntry("B (2.7-3.2)", 1L)
                    .containsEntry("C (2.0-2.6)", 1L)
                    .containsEntry("D (1.0-1.9)", 1L);
        }
    }

    @Nested
    class MoreReports {

        @BeforeEach
        void seed() {
            add("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            add("S2", "Bob", "Byron", "CS", AcademicYear.SENIOR);
            add("S3", "Cy", "Cyclops", "MATH", AcademicYear.JUNIOR);
            service.addGrade("S1", graded("S1", 95));
            service.addGrade("S1", graded("S1", 91));
        }

        @Test
        void gradeReport() {
            ReportData report = service.generateReport(ReportType.GRADE_REPORT);

            assertThat(report.getReportType()).isEqualTo(ReportType.GRADE_REPORT);
            Map<String, Object> ada = report.getRows().stream()
                    .filter(r -> r.get("Student ID").equals("S1")).findFirst().orElseThrow();
            assertThat(ada).containsEntry("Name", "Ada Lovelace")
                    .containsEntry("GPA", String.format("%.2f", 3.85))
                    .containsEntry("Total Credits", 2)
                    .containsEntry("Grade Level", "A (3.8-4.0)");
            assertThat(report.getMetadata()).containsEntry("totalStudents", 3);
        }

        @Test
        void performanceReportListsEveryStatistic() {
            ReportData report = service.generateReport(ReportType.PERFORMANCE_REPORT);

            assertThat(report.getColumns()).containsExactly("Metric", "Value");
            assertThat(report.getRows()).contains(Map.of("Metric", "totalStudents", "Value", "3"));
            assertThat(report.getRowCount()).isEqualTo(report.getMetadata().size());
        }

        @Test
        void demographicReportPercentages() {
            ReportData report = service.generateReport(ReportType.DEMOGRAPHIC_REPORT);

            assertThat(report.getRows()).hasSize(4)
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Major").containsEntry("Value", "CS")
                            .containsEntry("Count", 2L).containsEntry("Percentage", String.format("%.1f%%", 200.0 / 3)))
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Academic Year")
                            .containsEntry("Value", "Senior").containsEntry("Count", 1L));
            assertThat(report.getMetadata()).containsEntry("totalStudents", 3L);
        }

        @Test
        void statisticalSummaryDateRangeAndUnsupportedTypes() {
            assertThat(service.generateReport(ReportType.STATISTICAL_SUMMARY).getContent()).contains("totalStudents: 3");
            assertThat(service.generateReportForDateRange(ReportType.ENROLLMENT_REPORT,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now()).getRows()).hasSize(3);
            assertThat(service.generateReport(ReportType.FINANCIAL_REPORT).getTitle()).isEqualTo("Unsupported Report Type");
        }

        @Test
        void reportableStubs() {
            assertThat(service.getAvailableReportTypes()).hasSize(5).doesNotContain(ReportType.FINANCIAL_REPORT);
            assertThat(service.getSupportedFormats()).contains(ReportFormat.CSV);
            assertThat(service.exportReport(null, ReportFormat.PDF, "unused")).isTrue();
            assertThat(service.scheduleRecurringReport(ReportType.GRADE_REPORT, "daily", List.of())).startsWith("SCHED_");
            assertThat(service.cancelScheduledReport("x")).isTrue();
            assertThat(service.getReportHistory(ReportType.GRADE_REPORT, 3)).isEmpty();
        }
    }
}
