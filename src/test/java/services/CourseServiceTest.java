package services;

import interfaces.Enrollable.EnrollmentStatistics;
import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SortOrder;
import models.Course;
import models.Course.CourseStatus;
import models.Course.DifficultyLevel;
import models.Enrollment.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CourseServiceTest {

    private static final String SEM = "Fall";
    private static final int YEAR = 2025;

    private CourseService service;

    @BeforeEach
    void setUp() {
        service = new CourseService();
    }

    private static Course course(String id, String dept, int credits, int capacity) {
        Course c = new Course(id, id, "Course " + id, "About " + id, credits, dept);
        c.setMaxEnrollment(capacity);
        return c;
    }

    private Course addCourse(String id, int capacity) {
        Course c = course(id, "CS", 3, capacity);
        assertThat(service.addCourse(c)).isTrue();
        return c;
    }

    private boolean enroll(String student, String courseId) {
        return service.enrollStudent(student, courseId, SEM, YEAR);
    }

    @Nested
    class Crud {

        @Test
        void addAndLookup() {
            Course c = addCourse("CS101", 10);

            assertThat(service.getCourseById("CS101")).containsSame(c);
            assertThat(service.getAllCourses()).containsExactly(c);
            assertThat(service.getCurrentEnrollmentCount("CS101")).isZero();
        }

        @Test
        void rejectsNullDuplicateAndBlankIds() {
            addCourse("CS101", 10);

            assertThat(service.addCourse(null)).isFalse();
            assertThat(service.addCourse(course("CS101", "CS", 3, 5))).isFalse();
            Course blank = new Course();
            assertThat(service.addCourse(blank)).isFalse();
            assertThat(service.getAllCourses()).hasSize(1);
        }

        @Test
        void updateOnlyExistingCourse() {
            addCourse("CS101", 10);
            Course replacement = course("CS101", "MATH", 4, 20);

            assertThat(service.updateCourse(replacement)).isTrue();
            assertThat(service.getCourseById("CS101")).containsSame(replacement);
            assertThat(service.updateCourse(course("NOPE", "CS", 3, 5))).isFalse();
            assertThat(service.updateCourse(null)).isFalse();
        }

        @Test
        void removeClearsAllState() {
            addCourse("CS101", 10);
            enroll("S1", "CS101");
            service.assignInstructor("CS101", "P1");

            assertThat(service.removeCourse("CS101")).isTrue();

            assertThat(service.getCourseById("CS101")).isEmpty();
            assertThat(service.getCourseEnrollments("CS101")).isEmpty();
            assertThat(service.getCoursesByInstructor("P1")).isEmpty();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isZero();
            assertThat(service.removeCourse("CS101")).isFalse();
            assertThat(service.removeCourse(null)).isFalse();
        }
    }

    @Nested
    class Enrolling {

        @Test
        void enrollsUntilCapacity() {
            addCourse("CS101", 2);

            assertThat(enroll("S1", "CS101")).isTrue();
            assertThat(enroll("S2", "CS101")).isTrue();
            assertThat(service.hasAvailableSpots("CS101")).isFalse();
            assertThat(enroll("S3", "CS101")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(2);
        }

        @Test
        void cannotEnrollInUnknownOrInactiveCourse() {
            Course c = addCourse("CS101", 10);
            c.setActive(false);

            assertThat(enroll("S1", "CS101")).isFalse();
            assertThat(enroll("S1", "NOPE")).isFalse();
            assertThat(service.hasAvailableSpots("NOPE")).isFalse();
            assertThat(service.getMaxEnrollmentCapacity("NOPE")).isZero();
        }

        @Test
        void sameStudentCannotTakeTwoSeats() {
            // Regression: enrollStudent did not check for an existing enrollment.
            addCourse("CS101", 2);

            assertThat(enroll("S1", "CS101")).isTrue();
            assertThat(enroll("S1", "CS101")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
            assertThat(enroll("S2", "CS101")).isTrue();
        }

        @Test
        void dropReleasesSeatOnce() {
            // Regression: dropStudent matched any record of the student (including already
            // dropped ones) and decremented the counter even when nothing was dropped.
            addCourse("CS101", 1);
            enroll("S1", "CS101");

            assertThat(service.dropStudent("S1", "CS101", "x")).isTrue();
            assertThat(service.dropStudent("S1", "CS101", "again")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isZero();

            assertThat(enroll("S2", "CS101")).isTrue();
            assertThat(enroll("S3", "CS101")).isFalse();
        }

        @Test
        void reEnrollAfterDropThenDropAgain() {
            addCourse("CS101", 5);
            enroll("S1", "CS101");
            service.dropStudent("S1", "CS101", "x");

            assertThat(enroll("S1", "CS101")).isTrue();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
            assertThat(service.dropStudent("S1", "CS101", "y")).isTrue();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isZero();
        }

        @Test
        void droppingWaitlistedStudentDoesNotTouchEnrollmentCount() {
            addCourse("CS101", 1);
            enroll("S1", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);

            assertThat(service.dropStudent("W1", "CS101", "x")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
            assertThat(service.isStudentWaitlisted("W1", "CS101")).isTrue();
        }

        @Test
        void waitlistRejectsDuplicatesAndEnrolledStudents() {
            addCourse("CS101", 1);
            enroll("S1", "CS101");

            assertThat(service.addToWaitlist("S1", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.addToWaitlist("W1", "CS101", SEM, YEAR)).isTrue();
            assertThat(service.addToWaitlist("W1", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(1);
        }

        @Test
        void processWaitlistFillsFreedSeatsOnly() {
            addCourse("CS101", 2);
            enroll("S1", "CS101");
            enroll("S2", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            service.addToWaitlist("W2", "CS101", SEM, YEAR);

            assertThat(service.processWaitlist("CS101", 5)).isZero();

            service.dropStudent("S1", "CS101", "x");
            assertThat(service.processWaitlist("CS101", 5)).isEqualTo(1);

            assertThat(service.isStudentEnrolled("W1", "CS101")).isTrue();
            assertThat(service.isStudentWaitlisted("W2", "CS101")).isTrue();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(2);
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(1);
        }

        @Test
        void removeFromWaitlist() {
            addCourse("CS101", 1);
            service.addToWaitlist("W1", "CS101", SEM, YEAR);

            assertThat(service.removeFromWaitlist("W1", "CS101")).isTrue();
            assertThat(service.removeFromWaitlist("W1", "CS101")).isFalse();
            assertThat(service.getCurrentWaitlistCount("CS101")).isZero();
        }

        @Test
        void transferMovesStudent() {
            addCourse("CS101", 5);
            addCourse("CS102", 5);
            enroll("S1", "CS101");

            assertThat(service.transferStudent("S1", "CS101", "CS102", SEM, YEAR)).isTrue();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isFalse();
            assertThat(service.isStudentEnrolled("S1", "CS102")).isTrue();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isZero();
            assertThat(service.getCurrentEnrollmentCount("CS102")).isEqualTo(1);
        }

        @Test
        void transferToFullCourseKeepsOriginalSeat() {
            // Regression: transfer dropped the student before checking the target, leaving
            // them enrolled nowhere when the target was full.
            addCourse("CS101", 5);
            addCourse("CS102", 1);
            enroll("S1", "CS101");
            enroll("S2", "CS102");

            assertThat(service.transferStudent("S1", "CS101", "CS102", SEM, YEAR)).isFalse();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
            assertThat(service.transferStudent("S1", "CS101", "NOPE", SEM, YEAR)).isFalse();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
        }

        @Test
        void bulkEnrollAndStudentView() {
            addCourse("CS101", 2);
            addCourse("CS102", 2);

            assertThat(service.bulkEnrollStudents(List.of("S1", "S2", "S3"), "CS101", SEM, YEAR)).isEqualTo(2);
            enroll("S1", "CS102");
            assertThat(service.getStudentEnrollments("S1")).hasSize(2);
        }

        @Test
        void enrollmentStatistics() {
            addCourse("CS101", 3);
            enroll("S1", "CS101");
            enroll("S2", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            service.dropStudent("S2", "CS101", "x");

            EnrollmentStatistics stats = service.getEnrollmentStatistics("CS101");
            assertThat(stats.getEnrolled()).isEqualTo(1);
            assertThat(stats.getWaitlisted()).isEqualTo(1);
            assertThat(stats.getDropped()).isEqualTo(1);
            assertThat(stats.getCapacity()).isEqualTo(3);

            assertThat(service.getEnrollmentStatistics("NOPE").getCapacity()).isZero();
            assertThat(service.getCourseEnrollments("CS101"))
                    .extracting(e -> e.getStatus())
                    .contains(EnrollmentStatus.DROPPED);
        }
    }

    @Nested
    class Prerequisites {

        @Test
        void prerequisitesMustExistAndBeSatisfied() {
            addCourse("CS101", 5);
            addCourse("CS201", 5);

            assertThat(service.addPrerequisite("CS201", "CS101")).isTrue();
            assertThat(service.addPrerequisite("CS201", "NOPE")).isFalse();
            assertThat(service.addPrerequisite("NOPE", "CS101")).isFalse();

            assertThat(service.getCoursePrerequisites("CS201")).containsExactly("CS101");
            assertThat(service.checkPrerequisites("CS201", List.of())).isFalse();
            assertThat(service.checkPrerequisites("CS201", List.of("CS101"))).isTrue();
            assertThat(service.checkPrerequisites("CS101", List.of())).isTrue();
        }

        @Test
        void courseCannotBeItsOwnPrerequisite() {
            addCourse("CS101", 5);

            assertThat(service.addPrerequisite("CS101", "CS101")).isFalse();
            assertThat(service.getCoursePrerequisites("CS101")).isEmpty();
        }

        @Test
        void recommendationsRespectPrerequisitesDepartmentsAndCompletion() {
            addCourse("CS101", 5);
            addCourse("CS201", 5);
            service.addCourse(course("ART1", "ART", 3, 5));
            Course closed = addCourse("CS300", 5);
            closed.setActive(false);
            service.addPrerequisite("CS201", "CS101");

            assertThat(service.getRecommendedCourses("FRESHMAN", List.of(), List.of("CS")))
                    .extracting(Course::getCourseId).containsExactly("CS101");
            assertThat(service.getRecommendedCourses("FRESHMAN", List.of("CS101"), List.of("CS")))
                    .extracting(Course::getCourseId).containsExactly("CS201");
        }
    }

    @Nested
    class Queries {

        @BeforeEach
        void seed() {
            service.addCourse(course("CS101", "CS", 3, 10));
            service.addCourse(course("CS201", "CS", 4, 4));
            service.addCourse(course("MA101", "MATH", 2, 10));
            enroll("S1", "CS201");
            enroll("S2", "CS201");
            enroll("S3", "CS201");
        }

        @Test
        void byDepartmentSortedByCode() {
            assertThat(service.getCoursesByDepartment("CS"))
                    .extracting(Course::getCourseId).containsExactly("CS101", "CS201");
        }

        @ParameterizedTest
        @CsvSource({"1,2,MA101", "3,4,CS101;CS201", "2,3,MA101;CS101", "5,6,''"})
        void byCreditRangeInclusiveAndOrdered(int min, int max, String expected) {
            List<String> ids = service.getCoursesByCreditRange(min, max).stream().map(Course::getCourseId).toList();
            assertThat(String.join(";", ids)).isEqualTo(expected);
        }

        @Test
        void popularityAndAvailability() {
            assertThat(service.getPopularCourses(75.0)).extracting(Course::getCourseId).containsExactly("CS201");
            enroll("S4", "CS201");
            assertThat(service.getCoursesWithAvailableSeats())
                    .extracting(Course::getCourseId).doesNotContain("CS201").hasSize(2);
        }

        @Test
        void statusDifficultyAndInstructorFilters() {
            service.getCourseById("CS101").orElseThrow().setStatus(CourseStatus.OPEN);
            service.getCourseById("MA101").orElseThrow().setDifficultyLevel(DifficultyLevel.ADVANCED);
            assertThat(service.assignInstructor("CS101", "P1")).isTrue();
            assertThat(service.assignInstructor("NOPE", "P1")).isFalse();

            assertThat(service.getCoursesByStatus(CourseStatus.OPEN)).extracting(Course::getCourseId).containsExactly("CS101");
            assertThat(service.getCoursesByDifficulty(DifficultyLevel.ADVANCED)).extracting(Course::getCourseId).containsExactly("MA101");
            assertThat(service.getCoursesByInstructor("P1")).extracting(Course::getCourseId).containsExactly("CS101");
        }

        @Test
        void timeSlotOverlapIsInclusive() {
            Course cs101 = service.getCourseById("CS101").orElseThrow();
            cs101.setStartTime(LocalTime.of(9, 0));
            cs101.setEndTime(LocalTime.of(10, 0));

            assertThat(service.getCoursesInTimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0))).containsExactly(cs101);
            assertThat(service.getCoursesInTimeSlot(LocalTime.of(10, 1), LocalTime.of(11, 0))).isEmpty();
        }

        @Test
        void aggregatesByDepartment() {
            assertThat(service.getEnrollmentStatisticsByDepartment()).containsEntry("CS", 3).containsEntry("MATH", 0);
            assertThat(service.getAverageEnrollmentRateByDepartment()).containsEntry("CS", 37.5);
            assertThat(service.getCreditHourDistribution()).containsEntry(3, 1L).containsEntry(4, 1L).containsEntry(2, 1L);
        }

        @Test
        void criteriaAndSearch() {
            assertThat(service.findCoursesByCriteria(Map.of("departmentId", "CS", "creditHours", 4)))
                    .extracting(Course::getCourseId).containsExactly("CS201");
            assertThat(service.search("about ma")).extracting(Course::getCourseId).containsExactly("MA101");
            assertThat(service.search(Map.of("courseCode", new SearchCriterion(SearchCriteria.STARTS_WITH, "cs"))))
                    .hasSize(2);
            assertThat(service.searchAndSort("", "creditHours", SortOrder.DESC))
                    .extracting(Course::getCourseId).containsExactly("CS201", "CS101", "MA101");
        }

        @Test
        void statisticsStayFreshAfterEnrollmentChanges() {
            // Regression: the stats cache was valid from construction and enroll/drop did
            // not invalidate it, so totals were stale for up to five minutes.
            Map<String, Object> before = service.calculateOverallStatistics();
            assertThat(before).containsEntry("totalCourses", 3).containsEntry("totalEnrollments", 3);

            enroll("S9", "CS101");
            assertThat(service.calculateOverallStatistics()).containsEntry("totalEnrollments", 4)
                    .containsEntry("availableSeats", 10 + 4 + 10 - 4);

            service.dropStudent("S9", "CS101", "x");
            assertThat(service.calculateOverallStatistics()).containsEntry("totalEnrollments", 3);
        }

        @Test
        void reports() {
            ReportData report = service.generateReport(ReportType.ENROLLMENT_REPORT);
            assertThat(report.getRows()).hasSize(3);
            assertThat(report.getRows()).anySatisfy(row ->
                    assertThat(row).containsEntry("Course Code", "CS201").containsEntry("Enrolled", 3));
            assertThat(service.generateReport(ReportType.STATISTICAL_SUMMARY).getContent()).contains("totalCourses: 3");
        }
    }

    @Test
    void freshServiceStatisticsArePopulated() {
        assertThat(service.calculateOverallStatistics())
                .containsEntry("totalCourses", 0)
                .containsEntry("totalEnrollments", 0);
    }

    @Test
    void enrollmentRateExtremesAreZeroWithoutActiveCourses() {
        // Regression: DoubleSummaryStatistics reports +/-Infinity as min/max of an empty set,
        // which leaked into the statistics.
        addCourse("CS101", 10).setActive(false);

        assertThat(service.calculateOverallStatistics())
                .containsEntry("activeCourses", 0)
                .containsEntry("minEnrollmentRate", 0.0)
                .containsEntry("maxEnrollmentRate", 0.0)
                .containsEntry("averageEnrollmentRate", 0.0);
    }

    @Test
    void updateWithoutIdReturnsFalse() {
        // Regression: updateCourse looked up a null ID in a ConcurrentHashMap and threw a
        // NullPointerException instead of reporting failure.
        assertThat(service.updateCourse(new Course())).isFalse();
    }

    @Nested
    class Searching {

        private Course algorithms;
        private Course calculus;
        private Course seminar;

        @BeforeEach
        void seed() {
            algorithms = course("CS301", "CS", 4, 2);
            algorithms.setCourseName("Algorithms");
            calculus = course("MA201", "MATH", 3, 4);
            calculus.setCourseName("Calculus");
            calculus.setDifficultyLevel(DifficultyLevel.ADVANCED);
            seminar = course("CS399", "CS", 1, 10);
            seminar.setCourseName("Seminar");
            seminar.setStatus(CourseStatus.OPEN);
            service.addCourse(algorithms);
            service.addCourse(calculus);
            service.addCourse(seminar);
            enroll("S1", "CS301");       // 50%
            enroll("S1", "MA201");       // 25%
        }

        @Test
        void keywordSearchToleratesCoursesWithoutDescription() {
            // Regression: search dereferenced the optional description and threw a
            // NullPointerException as soon as any course had none.
            seminar.setDescription(null);

            assertThat(service.search("seminar")).containsExactly(seminar);
            assertThat(service.search("algo")).containsExactly(algorithms);
            assertThat(service.countSearchResults("about")).isEqualTo(2);
        }

        @ParameterizedTest
        @CsvSource({
                "courseName,EXACT_MATCH,calculus,MA201",
                "courseCode,ENDS_WITH,99,CS399",
                "description,CONTAINS,about cs3,CS301;CS399",
                "description,STARTS_WITH,about,CS301;CS399;MA201",
                "departmentId,EXACT_MATCH,math,MA201",
                "status,CONTAINS,open,CS399",
                "difficultyLevel,STARTS_WITH,adv,MA201",
                "creditHours,EXACT_MATCH,1,CS399",
                "maxEnrollment,EXACT_MATCH,10,CS399",
                "enrollmentRate,EXACT_MATCH,50.0,CS301",
                "courseName,REGEX,.*,''",
                "unknown,CONTAINS,x,''"})
        void criteriaOnEveryField(String field, SearchCriteria criteria, String value, String expectedIds) {
            Map<String, SearchCriterion> query = Map.of(field, new SearchCriterion(criteria, value));
            List<String> ids = service.search(query).stream().map(Course::getCourseId).sorted().toList();

            assertThat(String.join(";", ids)).isEqualTo(expectedIds);
            assertThat(service.countSearchResults(query)).isEqualTo(ids.size());
        }

        @Test
        void emptyCriteriaMatchEverything() {
            assertThat(service.search(Map.<String, SearchCriterion>of())).hasSize(3);
            assertThat(service.findCoursesByCriteria(Map.of())).hasSize(3);
        }

        @ParameterizedTest
        @CsvSource({
                "courseName,ASC,CS301;MA201;CS399",
                "courseCode,DESC,MA201;CS399;CS301",
                "creditHours,ASC,CS399;MA201;CS301",
                "enrollmentRate,DESC,CS301;MA201;CS399",
                "departmentId,DESC,MA201",
                "maxEnrollment,ASC,CS301;MA201;CS399",
                "status,DESC,CS399",
                "unknown,ASC,CS301;CS399;MA201"})
        void sortByField(String sortBy, SortOrder order, String expectedPrefix) {
            String ids = String.join(";", service.searchAndSort("", sortBy, order).stream()
                    .map(Course::getCourseId).toList());
            assertThat(ids).startsWith(expectedPrefix);
        }

        @Test
        void pagination() {
            var page = service.searchWithPagination("cs", 0, 2);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getResults()).hasSize(2);

            var advanced = service.advancedSearchWithPagination(
                    Map.of("departmentId", new SearchCriterion(SearchCriteria.EXACT_MATCH, "cs")),
                    "courseName", SortOrder.DESC, 0, 1);
            assertThat(advanced.getTotalElements()).isEqualTo(2);
            assertThat(advanced.getResults()).containsExactly(seminar);
            assertThat(advanced.getSortOrder()).isEqualTo(SortOrder.DESC);

            assertThat(service.advancedSearchWithPagination(Map.of(), "courseCode", SortOrder.ASC, 1, 2)
                    .getResults()).containsExactly(calculus);
        }

        @Test
        void predicateSearchFilterAndSuggestions() {
            assertThat(service.search(c -> c.getCredits() > 3)).containsExactly(algorithms);
            assertThat(service.filter(c -> c.getDifficultyLevel() == DifficultyLevel.ADVANCED)).containsExactly(calculus);
            assertThat(service.filterCourses(c -> true)).hasSize(3);

            assertThat(service.getSearchSuggestions("cs3", 10)).containsExactlyInAnyOrder("CS301", "CS399");
            assertThat(service.getSearchSuggestions("c", 2)).hasSize(2);
            assertThat(service.getSearchableFields()).contains("courseName", "creditHours");
            assertThat(service.getSortableFields()).contains("enrollmentRate", "status");
        }
    }

    @Nested
    class Reporting {

        @BeforeEach
        void seed() {
            addCourse("CS101", 4);
            Course hard = addCourse("CS401", 2);
            hard.setDifficultyLevel(DifficultyLevel.ADVANCED);
            enroll("S1", "CS401");
        }

        @Test
        void courseEvaluationReport() {
            ReportData report = service.generateReport(ReportType.COURSE_EVALUATION_REPORT);

            assertThat(report.getTitle()).isEqualTo("Course Evaluation Report");
            assertThat(report.getRows()).hasSize(2);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Course Code", "CS401")
                    .containsEntry("Difficulty", DifficultyLevel.ADVANCED.toString())
                    .containsEntry("Credits", 3)
                    .containsEntry("Enrollment Rate", "50.0%"));
            assertThat(report.getMetadata()).containsEntry("totalCourses", 2);
        }

        @Test
        void statisticsAreCachedUntilAMutation() {
            Map<String, Object> first = service.calculateOverallStatistics();
            first.clear();
            assertThat(service.getSummaryStatistics())
                    .containsEntry("totalCourses", 2)
                    .containsEntry("maxEnrollmentRate", 50.0)
                    .containsEntry("minEnrollmentRate", 0.0)
                    .containsEntry("averageCreditHours", 3.0)
                    .containsEntry("totalCreditHours", 6L)
                    .containsEntry("waitlistTotal", 0);

            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            assertThat(service.calculateOverallStatistics()).containsEntry("waitlistTotal", 1);
        }

        @Test
        void unsupportedTypeAndMetadata() {
            assertThat(service.generateReport(ReportType.GRADE_REPORT).getContent()).isEqualTo("Report type not supported");
            assertThat(service.generateReportForDateRange(ReportType.ENROLLMENT_REPORT,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now()).getRows()).hasSize(2);
            assertThat(service.getAvailableReportTypes()).containsExactly(ReportType.ENROLLMENT_REPORT,
                    ReportType.COURSE_EVALUATION_REPORT, ReportType.STATISTICAL_SUMMARY);
            assertThat(service.getSupportedFormats()).contains(ReportFormat.EXCEL);
            assertThat(service.scheduleRecurringReport(ReportType.ENROLLMENT_REPORT, "daily", List.of()))
                    .startsWith("SCHED_COURSE_");
            assertThat(service.cancelScheduledReport("x")).isTrue();
            assertThat(service.getReportHistory(ReportType.ENROLLMENT_REPORT, 1)).isEmpty();
            assertThat(service.exportReport(null, ReportFormat.CSV, "x.csv")).isTrue();
        }

        @Test
        void completedEnrollmentsAreCountedInStatistics() {
            service.getCourseEnrollments("CS401").get(0).setStatus(EnrollmentStatus.COMPLETED);
            assertThat(service.getEnrollmentStatistics("CS401").getCompleted()).isEqualTo(1);
        }
    }
}
