package services;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportMetadata;
import interfaces.Reportable.ReportType;
import models.Course;
import models.Department;
import models.Grade;
import models.Grade.GradeComponent;
import models.Professor;
import models.Professor.AcademicRank;
import models.Student;
import models.Student.AcademicYear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    private StudentService students;
    private ProfessorService professors;
    private CourseService courses;
    private DepartmentService departments;
    private EnrollmentService enrollments;
    private GradeService grades;
    private ReportService service;

    @BeforeEach
    void setUp() {
        students = new StudentService();
        professors = new ProfessorService();
        courses = new CourseService();
        departments = new DepartmentService();
        enrollments = new EnrollmentService();
        grades = new GradeService();
        service = new ReportService(students, professors, courses, departments, enrollments, grades);
    }

    private Student addStudent(String id, String first, String last, String major, AcademicYear year) {
        Student s = new Student("U" + id, first, last, first.toLowerCase() + "@campus.edu", null, id, major, year);
        assertThat(students.addStudent(s)).isTrue();
        return s;
    }

    private Professor addProfessor(String id, String first, String last, AcademicRank rank, double rating) {
        Professor p = new Professor("U" + id, first, last, first.toLowerCase() + "@campus.edu", null, id, "CS", rank, "x");
        p.setTeachingRating(rating);
        assertThat(professors.addProfessor(p)).isTrue();
        return p;
    }

    private Course addCourse(String id, String name, int capacity) {
        Course c = new Course(id, id, name, "About " + name, 3, "CS");
        c.setMaxEnrollment(capacity);
        assertThat(courses.addCourse(c)).isTrue();
        return c;
    }

    private static Grade graded(String id, String student, String course, double points) {
        Grade g = new Grade(id, "E1", student, course, "Task " + id, GradeComponent.HOMEWORK);
        g.setPointsPossible(100);
        g.submitAssignment();
        assertThat(g.gradeAssignment(points, "P1", "")).isTrue();
        return g;
    }

    private static Map<String, Object> rowWith(List<Map<String, Object>> rows, String key, Object value) {
        return rows.stream().filter(r -> value.equals(r.get(key))).findFirst().orElseThrow();
    }

    @Nested
    class EnrollmentReports {

        @BeforeEach
        void seed() {
            addStudent("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            addCourse("CS101", "Compilers", 10);
            assertThat(enrollments.enrollStudent("S2", "CS999", "Fall", 2025)).isTrue();
            assertThat(enrollments.enrollStudent("S1", "CS101", "Fall", 2025)).isTrue();
        }

        @Test
        void rowsResolveNamesAndAreSortedByStudent() {
            ReportData report = service.generateReport(ReportType.ENROLLMENT_REPORT);

            assertThat(report.getReportType()).isEqualTo(ReportType.ENROLLMENT_REPORT);
            assertThat(report.getTitle()).startsWith("Enrollment Report - Generated on");
            assertThat(report.getRows()).extracting(r -> r.get("Student ID")).containsExactly("S1", "S2");
            assertThat(report.getRows().get(0))
                    .containsEntry("Student Name", "Ada Lovelace")
                    .containsEntry("Course", "Compilers")
                    .containsEntry("Status", "Enrolled");
            assertThat(report.getRows().get(1))
                    .containsEntry("Student Name", "Unknown Student")
                    .containsEntry("Course", "Unknown Course");
            assertThat(report.getMetadata()).containsEntry("totalEnrollments", 2);
        }

        @Test
        void dateRangeIsHonoured() {
            // Regression: generateReportForDateRange ignored startDate/endDate and always
            // reported the last six months.
            LocalDateTime now = LocalDateTime.now();

            assertThat(service.generateReportForDateRange(ReportType.ENROLLMENT_REPORT,
                    now.minusDays(10), now.minusDays(5)).getRows()).isEmpty();
            assertThat(service.generateReportForDateRange(ReportType.ENROLLMENT_REPORT,
                    now.minusDays(1), now.plusDays(1)).getRows()).hasSize(2);
            assertThat(service.generateReport(ReportType.ENROLLMENT_REPORT,
                    Map.of("startDate", now.plusDays(1))).getRows()).isEmpty();
            assertThat(service.generateReport(ReportType.ENROLLMENT_REPORT,
                    Map.of("endDate", now.minusDays(1))).getRows()).isEmpty();
        }
    }

    @Nested
    class GradeReports {

        @Test
        void onlyCountedGradesAppear() {
            addStudent("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            addCourse("CS101", "Compilers", 10);
            grades.addGrade(graded("G1", "S1", "CS101", 95));
            Grade dropped = graded("G2", "S1", "CS101", 10);
            dropped.dropGrade();
            grades.addGrade(dropped);

            ReportData report = service.generateReport(ReportType.GRADE_REPORT);

            assertThat(report.getRows()).singleElement().satisfies(row -> assertThat(row)
                    .containsEntry("Student", "Ada Lovelace")
                    .containsEntry("Course", "Compilers")
                    .containsEntry("Assignment", "Task G1")
                    .containsEntry("Grade", "A")
                    .containsEntry("Percentage", String.format("%.1f%%", 95.0)));
            assertThat(report.getMetadata()).isNotEmpty();
        }
    }

    @Nested
    class PerformanceReports {

        @BeforeEach
        void seed() {
            Student ada = addStudent("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            students.addGrade("S1", graded("G1", "S1", "CS101", 95)); // GPA 4.0
            Student inactive = addStudent("S2", "Bob", "Byron", "CS", AcademicYear.SENIOR);
            inactive.setActive(false);
            addProfessor("P1", "Alan", "Turing", AcademicRank.FULL, 4.5);
            addProfessor("P2", "Grace", "Hopper", AcademicRank.ASSISTANT, 1.0);
            addCourse("CS101", "Compilers", 4);
            courses.enrollStudent("S1", "CS101", "Fall", 2025);
            courses.enrollStudent("S3", "CS101", "Fall", 2025);
            assertThat(ada.isActive()).isTrue();
        }

        @Test
        void rowsCoverActiveStudentsProfessorsAndCoursesSortedByEntity() {
            ReportData report = service.generateReport(ReportType.PERFORMANCE_REPORT);

            assertThat(report.getRows()).extracting(r -> r.get("Entity")).containsExactly(
                    "Course: Compilers", "Professor: Alan Turing", "Professor: Grace Hopper", "Student: Ada Lovelace");
            assertThat(rowWith(report.getRows(), "Entity", "Student: Ada Lovelace"))
                    .containsEntry("Metric", "GPA").containsEntry("Value", "4.00").containsEntry("Benchmark", "4.0");
            assertThat(rowWith(report.getRows(), "Entity", "Course: Compilers"))
                    .containsEntry("Metric", "Enrollment Rate").containsEntry("Value", "50.00");
            assertThat(report.getMetadata())
                    .containsEntry("studentsAnalyzed", 2)
                    .containsEntry("professorsAnalyzed", 2)
                    .containsEntry("coursesAnalyzed", 1);
        }

        @Test
        void trendIsJudgedRelativeToTheBenchmark() {
            // Regression: percentage thresholds (>50 positive, >30 stable) were applied to raw
            // GPA / rating values, so a perfect 4.0 GPA was reported as a "Negative" trend.
            List<Map<String, Object>> rows = service.generateReport(ReportType.PERFORMANCE_REPORT).getRows();

            assertThat(rowWith(rows, "Entity", "Student: Ada Lovelace")).containsEntry("Trend", "Positive");
            assertThat(rowWith(rows, "Entity", "Professor: Alan Turing")).containsEntry("Trend", "Positive");
            assertThat(rowWith(rows, "Entity", "Professor: Grace Hopper")).containsEntry("Trend", "Negative");
            assertThat(rowWith(rows, "Entity", "Course: Compilers")).containsEntry("Trend", "Stable");
        }
    }

    @Nested
    class DemographicAndFinancialReports {

        @Test
        void demographicRowsPerCategory() {
            addStudent("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            addStudent("S2", "Bob", "Byron", "CS", AcademicYear.JUNIOR);
            addStudent("S3", "Cy", "Cyclops", "MATH", AcademicYear.SENIOR);
            addProfessor("P1", "Alan", "Turing", AcademicRank.FULL, 4.0);

            ReportData report = service.generateReport(ReportType.DEMOGRAPHIC_REPORT);

            assertThat(report.getRows()).hasSize(5);
            assertThat(report.getRows())
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Student Major")
                            .containsEntry("Subcategory", "CS").containsEntry("Count", 2L)
                            .containsEntry("Percentage", String.format("%.1f%%", 200.0 / 3)))
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Student Year")
                            .containsEntry("Subcategory", "Senior").containsEntry("Count", 1L))
                    .anySatisfy(row -> assertThat(row).containsEntry("Category", "Professor Rank")
                            .containsEntry("Subcategory", "Full Professor")
                            .containsEntry("Percentage", String.format("%.1f%%", 100.0)));
            assertThat(report.getMetadata()).containsEntry("totalStudents", 3).containsEntry("totalProfessors", 1)
                    .containsEntry("totalDepartments", 0);
        }

        @Test
        void financialRowsForActiveDepartmentsOnly() {
            departments.addDepartment(new Department("D2", "MATH", "Mathematics", "d", "B2"));
            departments.addDepartment(new Department("D1", "CS", "Computer Science", "d", "B1"));
            Department closed = new Department("D3", "ART", "Art", "d", "B3");
            closed.setActive(false);
            departments.addDepartment(closed);

            ReportData report = service.generateReport(ReportType.FINANCIAL_REPORT);

            assertThat(report.getRows()).extracting(r -> r.get("Department"))
                    .containsExactly("Computer Science", "Mathematics");
            assertThat(report.getRows().get(0)).containsEntry("Utilization %", "75%");
            assertThat(report.getMetadata()).containsEntry("departmentsAnalyzed", 3);
        }
    }

    @Nested
    class StatisticalSummary {

        @Test
        void otherTypesFallBackToCombinedStatistics() {
            addStudent("S1", "Ada", "Lovelace", "CS", AcademicYear.JUNIOR);
            addProfessor("P1", "Alan", "Turing", AcademicRank.FULL, 4.0);

            ReportData report = service.generateReport(ReportType.ATTENDANCE_REPORT);

            assertThat(report.getReportType()).isEqualTo(ReportType.STATISTICAL_SUMMARY);
            assertThat(report.getTitle()).startsWith("Attendance Report");
            assertThat(report.getContent()).contains("totalStudents: 1", "totalProfessors: 1");
        }
    }

    @Nested
    class AsyncGeneration {

        @Test
        void asyncReportIsGeneratedAndValidated() throws Exception {
            ReportData report = service.generateReportAsync(ReportType.GRADE_REPORT, Map.of()).get(5, TimeUnit.SECONDS);

            assertThat(report.getReportType()).isEqualTo(ReportType.GRADE_REPORT);
            assertThat(service.getReportHistory(null, 10)).extracting(ReportMetadata::getReportId)
                    .containsExactly(report.getReportId());
        }

        @Test
        void failuresBecomeErrorReports() throws Exception {
            ReportData report = service.generateReportAsync(null, Map.of()).get(5, TimeUnit.SECONDS);

            assertThat(report.getTitle()).isEqualTo("Error Report");
            assertThat(report.getContent()).startsWith("Error generating report:");
        }

        @Test
        void multipleReportsAreReturnedInTypeOrder() throws Exception {
            List<ReportData> reports = service.generateMultipleReportsAsync(List.of(
                    ReportType.STATISTICAL_SUMMARY, ReportType.GRADE_REPORT, ReportType.ENROLLMENT_REPORT))
                    .get(10, TimeUnit.SECONDS);

            assertThat(reports).extracting(ReportData::getReportType).containsExactly(
                    ReportType.ENROLLMENT_REPORT, ReportType.GRADE_REPORT, ReportType.STATISTICAL_SUMMARY);
            assertThat(service.getSummaryStatistics()).containsEntry("totalReports", 3);
        }
    }

    @Nested
    class HistoryAndScheduling {

        @Test
        void reportIdsAreUniqueSoHistoryKeepsEveryReport() {
            // Regression: ids were "millis_nanoTime%1000"; reports generated within the same
            // millisecond collided and overwrote each other in the cache and history.
            List<String> ids = IntStream.range(0, 300)
                    .mapToObj(i -> service.generateReport(ReportType.GRADE_REPORT).getReportId())
                    .toList();

            assertThat(ids).doesNotHaveDuplicates();
            assertThat(service.getSummaryStatistics())
                    .containsEntry("totalReports", 300)
                    .containsEntry("cachedReports", 300);
        }

        @Test
        void historyIsFilteredByTypeNewestFirstAndLimited() {
            ReportData first = service.generateReport(ReportType.GRADE_REPORT);
            service.generateReport(ReportType.ENROLLMENT_REPORT);
            ReportData last = service.generateReport(ReportType.GRADE_REPORT);

            List<ReportMetadata> gradeHistory = service.getReportHistory(ReportType.GRADE_REPORT, 10);
            assertThat(gradeHistory).extracting(ReportMetadata::getReportId)
                    .containsExactlyInAnyOrder(first.getReportId(), last.getReportId());
            assertThat(gradeHistory.get(0).getGeneratedAt()).isAfterOrEqualTo(gradeHistory.get(1).getGeneratedAt());
            assertThat(gradeHistory.get(0).getGeneratedBy()).isEqualTo("SYSTEM");
            assertThat(gradeHistory.get(0).getFormat()).isEqualTo(ReportFormat.JSON);
            assertThat(gradeHistory.get(0).getFileSize()).isPositive();
            assertThat(service.getReportHistory(null, 2)).hasSize(2);
        }

        @Test
        void summaryStatisticsBeforeAndAfterGeneration() {
            assertThat(service.getSummaryStatistics())
                    .containsEntry("totalReports", 0)
                    .containsEntry("lastGenerated", "N/A");

            service.generateReport(ReportType.GRADE_REPORT);
            assertThat((String) service.getSummaryStatistics().get("lastGenerated")).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
        }

        @Test
        void scheduleAndCancel() {
            String id = service.scheduleRecurringReport(ReportType.GRADE_REPORT, "weekly", List.of("dean@campus.edu"));

            assertThat(id).startsWith("SCHED_RPT_");
            assertThat(service.getSummaryStatistics()).containsEntry("scheduledReports", 1);
            assertThat(service.cancelScheduledReport(id)).isTrue();
            assertThat(service.cancelScheduledReport(id)).isFalse();
            assertThat(service.getSummaryStatistics()).containsEntry("scheduledReports", 0);
        }

        @Test
        void exportRequiresAReportWithAnId() {
            ReportData report = service.generateReport(ReportType.GRADE_REPORT);

            assertThat(service.exportReport(report, ReportFormat.CSV, "ignored.csv")).isTrue();
            assertThat(service.exportReport(null, ReportFormat.CSV, "ignored.csv")).isFalse();
            assertThat(service.exportReport(new ReportData(" ", ReportType.GRADE_REPORT, "t", "c"),
                    ReportFormat.CSV, "ignored.csv")).isFalse();
        }

        @Test
        void advertisesAllTypesAndFormats() {
            assertThat(service.getAvailableReportTypes()).containsExactly(ReportType.values());
            assertThat(service.getSupportedFormats()).containsExactly(ReportFormat.values());
        }

        @Test
        void nullParametersFallBackToDefaultWindow() {
            addCourse("CS101", "Compilers", 10);
            enrollments.enrollStudent("S1", "CS101", "Fall", 2025);
            Map<String, Object> params = new HashMap<>();
            params.put("startDate", null);

            assertThat(service.generateReport(ReportType.ENROLLMENT_REPORT, params).getRows()).hasSize(1);
            assertThat(service.generateReport(ReportType.ENROLLMENT_REPORT, null).getRows()).hasSize(1);
        }
    }
}
