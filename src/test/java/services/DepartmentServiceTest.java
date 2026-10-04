package services;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SearchResult;
import interfaces.Searchable.SortOrder;
import models.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DepartmentServiceTest {

    private DepartmentService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentService();
    }

    private static Department department(String id, String code, String name, String location) {
        return new Department(id, code, name, "About " + name, location);
    }

    private Department addDepartment(String id, String code, String name, String location) {
        Department d = department(id, code, name, location);
        assertThat(service.addDepartment(d)).isTrue();
        return d;
    }

    private void addStudents(String departmentId, int count) {
        IntStream.rangeClosed(1, count)
                .forEach(i -> assertThat(service.addStudent(departmentId, departmentId + "_S" + i)).isTrue());
    }

    private void addProfessors(String departmentId, int count) {
        IntStream.rangeClosed(1, count)
                .forEach(i -> assertThat(service.addProfessor(departmentId, departmentId + "_P" + i)).isTrue());
    }

    @Nested
    class Crud {

        @Test
        void addAndLookup() {
            Department cs = addDepartment("D1", "CS", "Computer Science", "Building A");

            assertThat(service.getDepartmentById("D1")).containsSame(cs);
            assertThat(service.getDepartmentById("NOPE")).isEmpty();
            assertThat(service.getAllDepartments()).containsExactly(cs);
            assertThat(service.getDepartmentStudents("D1")).isEmpty();
            assertThat(service.getDepartmentProfessors("D1")).isEmpty();
            assertThat(service.getDepartmentCourses("D1")).isEmpty();
            assertThat(service.getDepartmentBudget("D1")).hasValueSatisfying(budget ->
                    assertThat(budget).containsEntry("totalBudget", 0.0)
                            .containsEntry("fiscalYear", LocalDateTime.now().getYear())
                            .containsKeys("allocations", "lastUpdated"));
        }

        @Test
        void rejectsNullDuplicateAndBlankIds() {
            addDepartment("D1", "CS", "Computer Science", "A");

            assertThat(service.addDepartment(null)).isFalse();
            assertThat(service.addDepartment(department("D1", "MA", "Maths", "B"))).isFalse();
            assertThat(service.addDepartment(new Department())).isFalse();
            assertThat(service.getAllDepartments()).hasSize(1);
        }

        @Test
        void updateOnlyExistingDepartment() {
            addDepartment("D1", "CS", "Computer Science", "A");
            Department renamed = department("D1", "CS", "Computing", "B");

            assertThat(service.updateDepartment(renamed)).isTrue();
            assertThat(service.getDepartmentById("D1")).containsSame(renamed);
            assertThat(service.updateDepartment(department("NOPE", "X", "X", "X"))).isFalse();
            assertThat(service.updateDepartment(null)).isFalse();
        }

        @Test
        void updateWithoutIdReturnsFalse() {
            // Regression: updateDepartment looked up a null ID in a ConcurrentHashMap and threw
            // a NullPointerException instead of reporting failure.
            assertThat(service.updateDepartment(new Department())).isFalse();
        }

        @Test
        void updateKeepsMembershipOfReplacedDepartment() {
            // Regression: updateDepartment swapped in the new object without re-syncing the
            // service's student/professor/course indices, so the stored department reported
            // zero enrollment while the service still listed its members.
            addDepartment("D1", "CS", "Computer Science", "A");
            addStudents("D1", 2);
            addProfessors("D1", 1);
            service.addCourse("D1", "CS101");

            Department renamed = department("D1", "CS", "Computing", "A");
            assertThat(service.updateDepartment(renamed)).isTrue();

            assertThat(renamed.getTotalEnrollment()).isEqualTo(2);
            assertThat(renamed.getStudentIds()).containsExactly("D1_S1", "D1_S2");
            assertThat(renamed.getProfessorIds()).containsExactly("D1_P1");
            assertThat(renamed.getCourseIds()).containsExactly("CS101");
            assertThat(service.getDepartmentsWithEnrollmentAbove(2)).containsExactly(renamed);
        }

        @Test
        void removeClearsAllState() {
            addDepartment("D1", "CS", "Computer Science", "A");
            addStudents("D1", 1);
            service.addProfessor("D1", "P1");
            service.addCourse("D1", "CS101");

            assertThat(service.removeDepartment("D1")).isTrue();

            assertThat(service.getDepartmentById("D1")).isEmpty();
            assertThat(service.getDepartmentStudents("D1")).isEmpty();
            assertThat(service.getDepartmentProfessors("D1")).isEmpty();
            assertThat(service.getDepartmentCourses("D1")).isEmpty();
            assertThat(service.getDepartmentBudget("D1")).isEmpty();
            assertThat(service.removeDepartment("D1")).isFalse();
            assertThat(service.removeDepartment(" ")).isFalse();
            assertThat(service.removeDepartment(null)).isFalse();
        }
    }

    @Nested
    class Membership {

        private Department cs;

        @BeforeEach
        void seed() {
            cs = addDepartment("D1", "CS", "Computer Science", "A");
        }

        @Test
        void studentsAreSyncedToTheDepartment() {
            assertThat(service.addStudent("D1", "S1")).isTrue();
            assertThat(service.addStudent("D1", "S2")).isTrue();
            assertThat(service.addStudent("D1", "S1")).as("duplicate").isFalse();
            assertThat(service.addStudent("D1", "")).isFalse();
            assertThat(service.addStudent("NOPE", "S3")).isFalse();

            assertThat(service.getDepartmentStudents("D1")).containsExactly("S1", "S2");
            assertThat(cs.getStudentIds()).containsExactly("S1", "S2");
            assertThat(cs.getTotalEnrollment()).isEqualTo(2);

            assertThat(service.removeStudent("D1", "S1")).isTrue();
            assertThat(service.removeStudent("D1", "S1")).isFalse();
            assertThat(service.removeStudent("NOPE", "S2")).isFalse();
            assertThat(cs.getStudentIds()).containsExactly("S2");
            assertThat(cs.getTotalEnrollment()).isEqualTo(1);
        }

        @Test
        void professorsAreSyncedToTheDepartment() {
            // Regression: the service cleared and refilled Department#getProfessorIds(), which is a
            // defensive copy, so the department object never saw its professors.
            assertThat(service.addProfessor("D1", "P1")).isTrue();
            assertThat(service.addProfessor("D1", "P2")).isTrue();
            assertThat(service.addProfessor("D1", "P1")).isFalse();
            assertThat(service.addProfessor("D1", null)).isFalse();
            assertThat(service.addProfessor("NOPE", "P3")).isFalse();

            assertThat(service.getDepartmentProfessors("D1")).containsExactly("P1", "P2");
            assertThat(cs.getProfessorIds()).containsExactly("P1", "P2");
            assertThat(cs.getProfessorCount()).isEqualTo(2);

            assertThat(service.removeProfessor("D1", "P1")).isTrue();
            assertThat(service.removeProfessor("D1", "P1")).isFalse();
            assertThat(service.removeProfessor("NOPE", "P2")).isFalse();
            assertThat(cs.getProfessorIds()).containsExactly("P2");
        }

        @Test
        void coursesAreSyncedToTheDepartment() {
            // Regression: same defensive-copy problem as professors, for course IDs.
            assertThat(service.addCourse("D1", "CS101")).isTrue();
            assertThat(service.addCourse("D1", "CS201")).isTrue();
            assertThat(service.addCourse("D1", "CS101")).isFalse();
            assertThat(service.addCourse("D1", " ")).isFalse();
            assertThat(service.addCourse("NOPE", "CS301")).isFalse();

            assertThat(service.getDepartmentCourses("D1")).containsExactly("CS101", "CS201");
            assertThat(cs.getCourseIds()).containsExactly("CS101", "CS201");
            assertThat(cs.hasCourse("CS201")).isTrue();

            assertThat(service.removeCourse("D1", "CS101")).isTrue();
            assertThat(service.removeCourse("D1", "CS101")).isFalse();
            assertThat(service.removeCourse("NOPE", "CS201")).isFalse();
            assertThat(cs.getCourseIds()).containsExactly("CS201");
        }

        @Test
        void unknownDepartmentHasNoMembers() {
            assertThat(service.getDepartmentStudents("NOPE")).isEmpty();
            assertThat(service.getDepartmentProfessors("NOPE")).isEmpty();
            assertThat(service.getDepartmentCourses("NOPE")).isEmpty();
        }
    }

    @Nested
    class Budgets {

        @BeforeEach
        void seed() {
            addDepartment("D1", "CS", "Computer Science", "A");
        }

        @Test
        void setBudgetValidatesDepartmentAndAmount() {
            assertThat(service.setDepartmentBudget("D1", 50_000, 2026)).isTrue();
            assertThat(service.getDepartmentBudget("D1")).hasValueSatisfying(b ->
                    assertThat(b).containsEntry("totalBudget", 50_000.0).containsEntry("fiscalYear", 2026));

            assertThat(service.setDepartmentBudget("D1", -1, 2026)).isFalse();
            assertThat(service.setDepartmentBudget("NOPE", 10, 2026)).isFalse();
            assertThat(service.getDepartmentBudget("NOPE")).isEmpty();
            assertThat(service.getDepartmentBudget("D1").orElseThrow()).containsEntry("totalBudget", 50_000.0);
        }

        @Test
        void allocationsAreRecordedPerCategory() {
            assertThat(service.allocateBudget("D1", "labs", 1_000)).isTrue();
            assertThat(service.allocateBudget("D1", "travel", 250)).isTrue();
            assertThat(service.allocateBudget("D1", "labs", 1_500)).isTrue();
            assertThat(service.allocateBudget("D1", "labs", -1)).isFalse();
            assertThat(service.allocateBudget("NOPE", "labs", 1)).isFalse();

            @SuppressWarnings("unchecked")
            Map<String, Double> allocations =
                    (Map<String, Double>) service.getDepartmentBudget("D1").orElseThrow().get("allocations");
            assertThat(allocations).containsOnly(Map.entry("labs", 1_500.0), Map.entry("travel", 250.0));
        }

        @Test
        void totalsFlowIntoStatisticsAndFinancialReport() {
            addDepartment("D2", "MA", "Mathematics", "B");
            service.setDepartmentBudget("D1", 30_000, 2026);
            service.setDepartmentBudget("D2", 10_000, 2026);
            addStudents("D1", 3);

            Map<String, Object> stats = service.calculateOverallStatistics();
            assertThat(stats).containsEntry("totalBudget", 40_000.0).containsEntry("averageBudget", 20_000.0);

            ReportData report = service.generateReport(ReportType.FINANCIAL_REPORT);
            assertThat(report.getReportType()).isEqualTo(ReportType.FINANCIAL_REPORT);
            assertThat(report.getMetadata()).containsEntry("totalBudget", 40_000.0);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Department", "Computer Science")
                    .containsEntry("Total Budget", "$30000.00")
                    .containsEntry("Per Student", "$10000.00")
                    .containsEntry("Fiscal Year", 2026));
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Department", "Mathematics")
                    .containsEntry("Per Student", "$0.00"));
        }
    }

    @Nested
    class Queries {

        private Department cs;
        private Department math;
        private Department art;

        @BeforeEach
        void seed() {
            cs = addDepartment("D1", "CS", "Computer Science", "Building A");
            math = addDepartment("D2", "MA", "Mathematics", "Building B");
            art = addDepartment("D3", "ART", "Art", "Building A");
            addStudents("D1", 60);
            addStudents("D2", 10);
            addProfessors("D1", 3);
            addProfessors("D2", 2);
            art.setActive(false);
            math.addGraduateProgram("MSc Mathematics");
            cs.addMajorProgram("BSc CS");
            cs.addMajorProgram("BSc SE");
            cs.addGraduateProgram("MSc CS");
        }

        @Test
        void activeDepartmentsSortedByName() {
            assertThat(service.getActiveDepartments()).containsExactly(cs, math);
        }

        @Test
        void enrollmentThresholdsAndRanking() {
            assertThat(service.getDepartmentsWithEnrollmentAbove(10)).containsExactly(cs, math);
            assertThat(service.getDepartmentsWithEnrollmentAbove(11)).containsExactly(cs);
            assertThat(service.getLargestDepartments(2)).containsExactly(cs, math);
            assertThat(service.getLargestDepartments(0)).isEmpty();
        }

        @Test
        void graduateProgramsAndProgramStatistics() {
            assertThat(service.getDepartmentsWithGraduatePrograms()).containsExactlyInAnyOrder(cs, math);

            Map<String, Object> stats = service.getProgramOfferingStatistics();
            assertThat(stats).containsEntry("totalUndergraduatePrograms", 2L)
                    .containsEntry("totalGraduatePrograms", 2L)
                    .containsEntry("programsByDepartment", Map.of("CS", 3L, "MA", 1L, "ART", 0L));
        }

        @Test
        void facultyToStudentRatios() {
            assertThat(service.getFacultyToStudentRatios())
                    .containsEntry("D1", 20.0)
                    .containsEntry("D2", 5.0)
                    .containsEntry("D3", 0.0);
        }

        @Test
        void departmentsNeedingAttention() {
            // CS: 60 students, 3 professors -> healthy; Maths: low enrollment; Art: inactive.
            assertThat(service.getDepartmentsNeedingAttention()).containsExactlyInAnyOrder(math, art);
        }

        @Test
        void filteringAndCriteria() {
            assertThat(service.filterDepartments(d -> d.getDepartmentCode().length() == 2))
                    .containsExactlyInAnyOrder(cs, math);
            assertThat(service.filter(d -> !d.isActive())).containsExactly(art);
            assertThat(service.search(d -> d.getTotalEnrollment() == 60)).containsExactly(cs);

            assertThat(service.findDepartmentsByCriteria(Map.of("location", "Building A", "active", true)))
                    .containsExactly(cs);
            assertThat(service.findDepartmentsByCriteria(Map.of("totalEnrollment", 10))).containsExactly(math);
            assertThat(service.findDepartmentsByCriteria(Map.of())).hasSize(3);
            assertThat(service.findDepartmentsByCriteria(Map.of("unknown", "x"))).isEmpty();
        }

        @Test
        void overallStatistics() {
            Map<String, Object> stats = service.calculateOverallStatistics();

            assertThat(stats).containsEntry("totalDepartments", 3)
                    .containsEntry("activeDepartments", 2)
                    .containsEntry("totalEnrollment", 70L)
                    .containsEntry("averageEnrollment", 35.0)
                    .containsEntry("maxEnrollment", 60)
                    .containsEntry("minEnrollment", 10)
                    .containsKeys("facultyToStudentRatios", "programStatistics");
            assertThat(service.getSummaryStatistics()).isEqualTo(stats);
        }

        @Test
        void statisticsRefreshAfterMutation() {
            assertThat(service.calculateOverallStatistics()).containsEntry("totalEnrollment", 70L);
            service.addStudent("D2", "NEW");
            assertThat(service.calculateOverallStatistics()).containsEntry("totalEnrollment", 71L);
            service.removeDepartment("D1");
            assertThat(service.calculateOverallStatistics()).containsEntry("totalDepartments", 2);
        }
    }

    @Nested
    class Statistics {

        @Test
        void freshServiceStatisticsArePopulated() {
            // Regression: the cache timestamp was initialised in the constructor, so a fresh
            // service returned an empty (cached) statistics map for five minutes.
            Map<String, Object> stats = service.calculateOverallStatistics();

            assertThat(stats).containsEntry("totalDepartments", 0)
                    .containsEntry("activeDepartments", 0)
                    .containsEntry("totalBudget", 0.0)
                    .containsEntry("averageBudget", 0.0);
        }

        @Test
        void enrollmentExtremesAreZeroWithoutActiveDepartments() {
            // Regression: IntSummaryStatistics reports Integer.MAX_VALUE/MIN_VALUE as min/max
            // of an empty set, which leaked into the statistics.
            addDepartment("D1", "CS", "Computer Science", "A").setActive(false);

            assertThat(service.calculateOverallStatistics())
                    .containsEntry("minEnrollment", 0)
                    .containsEntry("maxEnrollment", 0)
                    .containsEntry("averageEnrollment", 0.0);
        }

        @Test
        void cachedStatisticsAreReturnedAsCopies() {
            addDepartment("D1", "CS", "Computer Science", "A");
            Map<String, Object> first = service.calculateOverallStatistics();
            first.clear();

            assertThat(service.calculateOverallStatistics()).containsEntry("totalDepartments", 1);
        }
    }

    @Nested
    class Searching {

        private Department cs;
        private Department math;
        private Department stats;

        @BeforeEach
        void seed() {
            cs = addDepartment("D1", "CS", "Computer Science", "Engineering Hall");
            math = addDepartment("D2", "MA", "Mathematics", "Science Hall");
            stats = addDepartment("D3", "ST", "Statistics", null);
            addStudents("D2", 2);
            addStudents("D3", 1);
        }

        @Test
        void keywordMatchesNameCodeOrIdCaseInsensitively() {
            assertThat(service.search("SCIENCE")).containsExactly(cs);
            assertThat(service.search("ma")).containsExactly(math);
            assertThat(service.search("d3")).containsExactly(stats);
            assertThat(service.countSearchResults("hall")).as("location is not a keyword field").isZero();
            assertThat(service.countSearchResults("")).isEqualTo(3);
        }

        @ParameterizedTest
        @CsvSource({
                "departmentName,EXACT_MATCH,mathematics,D2",
                "departmentName,CONTAINS,stat,D3",
                "departmentCode,STARTS_WITH,c,D1",
                "location,ENDS_WITH,science hall,D2",
                "departmentId,EXACT_MATCH,d1,D1",
                "totalEnrollment,EXACT_MATCH,2,D2",
                "active,EXACT_MATCH,true,D1;D2;D3",
                "departmentName,REGEX,.*,''",
                "unknownField,CONTAINS,x,''"})
        void criteriaSearch(String field, SearchCriteria criteria, String value, String expectedIds) {
            List<String> ids = service.search(Map.of(field, new SearchCriterion(criteria, value))).stream()
                    .map(Department::getDepartmentId).sorted().toList();
            assertThat(String.join(";", ids)).isEqualTo(expectedIds);
        }

        @Test
        void countWithCriteria() {
            assertThat(service.countSearchResults(Map.of("location", new SearchCriterion(SearchCriteria.CONTAINS, "hall"))))
                    .as("null location never matches").isEqualTo(2);
        }

        @ParameterizedTest
        @CsvSource({
                "departmentName,ASC,D1;D2;D3",
                "departmentName,DESC,D3;D2;D1",
                "departmentCode,ASC,D1;D2;D3",
                "totalEnrollment,DESC,D2;D3;D1",
                "unknown,ASC,D1;D2;D3"})
        void searchAndSort(String sortBy, SortOrder order, String expectedIds) {
            List<String> ids = service.searchAndSort("", sortBy, order).stream()
                    .map(Department::getDepartmentId).toList();
            assertThat(String.join(";", ids)).isEqualTo(expectedIds);
        }

        @Test
        void sortByActivePutsInactiveFirstAscending() {
            math.setActive(false);
            assertThat(service.searchAndSort("", "active", SortOrder.ASC).get(0)).isSameAs(math);
        }

        @Test
        void pagination() {
            SearchResult<Department> page = service.searchWithPagination("", 1, 2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getResults()).hasSize(1);

            SearchResult<Department> advanced = service.advancedSearchWithPagination(
                    Map.of("location", new SearchCriterion(SearchCriteria.CONTAINS, "hall")),
                    "departmentName", SortOrder.DESC, 0, 1);
            assertThat(advanced.getTotalElements()).isEqualTo(2);
            assertThat(advanced.getResults()).containsExactly(math);
            assertThat(advanced.getSortBy()).isEqualTo("departmentName");
            assertThat(advanced.getSortOrder()).isEqualTo(SortOrder.DESC);

            assertThat(service.advancedSearchWithPagination(Map.of(), "departmentCode", SortOrder.ASC, 0, 10)
                    .getResults()).containsExactly(cs, math, stats);
        }

        @Test
        void suggestionsAreDistinctLimitedAndSkipNulls() {
            assertThat(service.getSearchSuggestions("hall", 10))
                    .containsExactlyInAnyOrder("Engineering Hall", "Science Hall");
            assertThat(service.getSearchSuggestions("s", 2)).hasSize(2);
            assertThat(service.getSearchSuggestions("zzz", 5)).isEmpty();
        }

        @Test
        void advertisedFields() {
            assertThat(service.getSearchableFields()).contains("departmentName", "location", "active");
            assertThat(service.getSortableFields()).contains("departmentName", "totalEnrollment");
        }
    }

    @Nested
    class Reports {

        @BeforeEach
        void seed() {
            addDepartment("D1", "CS", "Computer Science", "Building A");
            addDepartment("D2", "MA", "Mathematics", "Building A");
            addDepartment("D3", "ART", "Art", "Building B").setActive(false);
            addStudents("D1", 4);
            addProfessors("D1", 2);
            service.addCourse("D1", "CS101");
        }

        @Test
        void enrollmentReportHasOneRowPerDepartment() {
            ReportData report = service.generateReport(ReportType.ENROLLMENT_REPORT);

            assertThat(report.getTitle()).isEqualTo("Department Enrollment Report");
            assertThat(report.getRows()).hasSize(3);
            assertThat(report.getMetadata()).containsEntry("totalDepartments", 3);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Code", "CS")
                    .containsEntry("Enrollment", 4)
                    .containsEntry("Professors", 2)
                    .containsEntry("Courses", 1)
                    .containsEntry("Ratio", "2.0:1"));
        }

        @Test
        void demographicReportGroupsByLocationAndStatus() {
            ReportData report = service.generateReport(ReportType.DEMOGRAPHIC_REPORT);

            assertThat(report.getRows()).hasSize(4);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Category", "Location").containsEntry("Value", "Building A")
                    .containsEntry("Count", 2L).containsEntry("Percentage", "66.7%"));
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Category", "Status").containsEntry("Value", "Inactive")
                    .containsEntry("Count", 1L).containsEntry("Percentage", "33.3%"));
        }

        @Test
        void statisticalSummaryListsStatistics() {
            ReportData report = service.generateReportForDateRange(ReportType.STATISTICAL_SUMMARY,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now());

            assertThat(report.getTitle()).isEqualTo("Department Statistical Summary");
            assertThat(report.getContent()).contains("totalDepartments: 3", "activeDepartments: 2");
        }

        @Test
        void unsupportedReportType() {
            ReportData report = service.generateReport(ReportType.GRADE_REPORT);
            assertThat(report.getTitle()).isEqualTo("Unsupported Report Type");
            assertThat(report.getContent()).isEqualTo("Report type not supported");
        }

        @Test
        void reportingMetadata() {
            assertThat(service.getAvailableReportTypes()).containsExactly(ReportType.ENROLLMENT_REPORT,
                    ReportType.DEMOGRAPHIC_REPORT, ReportType.FINANCIAL_REPORT, ReportType.STATISTICAL_SUMMARY);
            assertThat(service.getSupportedFormats()).contains(ReportFormat.PDF, ReportFormat.CSV);
            assertThat(service.scheduleRecurringReport(ReportType.ENROLLMENT_REPORT, "weekly", List.of("a@b.c")))
                    .startsWith("SCHED_DEPT_");
            assertThat(service.cancelScheduledReport("x")).isTrue();
            assertThat(service.getReportHistory(ReportType.ENROLLMENT_REPORT, 5)).isEmpty();
            assertThat(service.exportReport(service.generateReport(ReportType.ENROLLMENT_REPORT),
                    ReportFormat.CSV, "unused.csv")).isTrue();
        }
    }

    @Test
    void demographicReportOfEmptyServiceHasNoNaNPercentages() {
        // Regression: with no departments the active/inactive rows divided by zero and
        // reported "NaN%".
        ReportData report = service.generateReport(ReportType.DEMOGRAPHIC_REPORT);

        assertThat(report.getRows()).allSatisfy(row -> assertThat(row).containsEntry("Percentage", "0.0%"));
        assertThat(report.getMetadata()).containsEntry("totalDepartments", 0L);
    }

    @Test
    void ratioIsDividedByProfessorCount() {
        addDepartment("D1", "CS", "Computer Science", "A");
        addStudents("D1", 5);
        addProfessors("D1", 2);

        assertThat(service.getFacultyToStudentRatios().get("D1")).isCloseTo(2.5, within(1e-9));
    }
}
