package services;

import interfaces.Enrollable.EnrollmentStatistics;
import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SearchResult;
import interfaces.Searchable.SortOrder;
import models.Enrollment;
import models.Enrollment.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnrollmentServiceTest {

    private static final String SEM = "Fall";
    private static final int YEAR = 2025;

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService();
    }

    private boolean enroll(String student, String course) {
        return service.enrollStudent(student, course, SEM, YEAR);
    }

    private Enrollment enrollmentOf(String student, String course) {
        return service.getStudentEnrollments(student).stream()
                .filter(e -> e.getCourseId().equals(course))
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .findFirst().orElseThrow();
    }

    private void complete(String student, String course) {
        assertThat(enroll(student, course)).isTrue();
        service.updateEnrollmentStatus(enrollmentOf(student, course).getEnrollmentId(), EnrollmentStatus.COMPLETED);
    }

    @Nested
    class Enrolling {

        @Test
        void enrollsStudentAndIndexesEnrollment() {
            assertThat(enroll("S1", "CS101")).isTrue();

            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
            assertThat(service.getStudentEnrollments("S1")).singleElement()
                    .satisfies(e -> {
                        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
                        assertThat(e.getEnrolledBy()).isEqualTo("SYSTEM");
                        assertThat(e.getSemester()).isEqualTo(SEM);
                        assertThat(e.getYear()).isEqualTo(YEAR);
                    });
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsBlankIds(String blank) {
            assertThat(service.enrollStudent(blank, "CS101", SEM, YEAR)).isFalse();
            assertThat(service.enrollStudent("S1", blank, SEM, YEAR)).isFalse();
            assertThat(service.getAllEnrollments()).isEmpty();
        }

        @Test
        void rejectsDuplicateEnrollment() {
            assertThat(enroll("S1", "CS101")).isTrue();
            assertThat(enroll("S1", "CS101")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
        }

        @Test
        void defaultCapacityIsThirty() {
            assertThat(service.getMaxEnrollmentCapacity("ANY")).isEqualTo(30);
        }

        @Test
        void respectsCourseLimit() {
            service.setCourseLimit("CS101", 2);

            assertThat(enroll("S1", "CS101")).isTrue();
            assertThat(enroll("S2", "CS101")).isTrue();
            assertThat(service.hasAvailableSpots("CS101")).isFalse();
            assertThat(enroll("S3", "CS101")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(2);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void rejectsNonPositiveCourseLimit(int limit) {
            assertThat(service.setCourseLimit("CS101", limit)).isFalse();
            assertThat(service.getMaxEnrollmentCapacity("CS101")).isEqualTo(30);
        }

        @Test
        void capsStudentAtSixActiveEnrollments() {
            IntStream.rangeClosed(1, 6).forEach(i -> assertThat(enroll("S1", "C" + i)).isTrue());

            assertThat(enroll("S1", "C7")).isFalse();
            assertThat(service.getStudentsWithMaxEnrollments()).containsExactly("S1");

            // dropping one frees a slot again
            assertThat(service.dropStudent("S1", "C1", "too many")).isTrue();
            assertThat(enroll("S1", "C7")).isTrue();
        }

        @Test
        void bulkEnrollCountsOnlySuccessfulEnrollments() {
            service.setCourseLimit("CS101", 2);
            int enrolled = service.bulkEnrollStudents(List.of("S1", "S2", "S3", ""), "CS101", SEM, YEAR);

            assertThat(enrolled).isEqualTo(2);
            assertThat(service.getCourseEnrollments("CS101"))
                    .extracting(Enrollment::getStudentId).containsExactlyInAnyOrder("S1", "S2");
        }

        @Test
        void reEnrollingAfterDropKeepsBothRecordsDistinct() {
            // Regression: enrollment IDs derive from millis % 10000, so a quick re-enrollment
            // reused the dropped record's ID, overwrote it and double-indexed the new one.
            assertThat(enroll("S1", "CS101")).isTrue();
            assertThat(service.dropStudent("S1", "CS101", "changed mind")).isTrue();
            assertThat(enroll("S1", "CS101")).isTrue();

            List<Enrollment> records = service.getStudentEnrollments("S1");
            assertThat(records).hasSize(2).doesNotHaveDuplicates();
            assertThat(records).extracting(Enrollment::getStatus)
                    .containsExactlyInAnyOrder(EnrollmentStatus.DROPPED, EnrollmentStatus.ENROLLED);
            assertThat(service.getAllEnrollments()).hasSize(2);
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
        }
    }

    @Nested
    class Prerequisites {

        @Test
        void blocksEnrollmentUntilPrerequisiteCompleted() {
            service.addPrerequisite("CS201", "CS101");

            assertThat(service.checkPrerequisites("S1", "CS201")).isFalse();
            assertThat(enroll("S1", "CS201")).isFalse();

            complete("S1", "CS101");

            assertThat(service.getCompletedCourses("S1")).containsExactly("CS101");
            assertThat(service.checkPrerequisites("S1", "CS201")).isTrue();
            assertThat(enroll("S1", "CS201")).isTrue();
        }

        @Test
        void requiresAllPrerequisites() {
            service.addPrerequisite("CS301", "CS101");
            service.addPrerequisite("CS301", "MATH101");
            complete("S1", "CS101");

            assertThat(service.checkPrerequisites("S1", "CS301")).isFalse();
            complete("S1", "MATH101");
            assertThat(service.checkPrerequisites("S1", "CS301")).isTrue();
        }

        @Test
        void addAndRemovePrerequisites() {
            assertThat(service.addPrerequisite("CS201", "CS101")).isTrue();
            assertThat(service.addPrerequisite("CS201", "")).isFalse();
            assertThat(service.getCoursePrerequisites("CS201")).containsExactly("CS101");

            assertThat(service.removePrerequisite("CS201", "CS101")).isTrue();
            assertThat(service.removePrerequisite("CS201", "CS101")).isFalse();
            assertThat(service.removePrerequisite("NOPE", "CS101")).isFalse();
            assertThat(service.checkPrerequisites("S1", "CS201")).isTrue();
        }

        @Test
        void returnedPrerequisiteSetIsDefensiveCopy() {
            service.addPrerequisite("CS201", "CS101");
            service.getCoursePrerequisites("CS201").clear();
            assertThat(service.getCoursePrerequisites("CS201")).containsExactly("CS101");
        }
    }

    @Nested
    class DroppingAndWaitlist {

        @Test
        void dropMarksEnrollmentDroppedAndFreesSeat() {
            enroll("S1", "CS101");

            assertThat(service.dropStudent("S1", "CS101", "schedule")).isTrue();

            assertThat(service.isStudentEnrolled("S1", "CS101")).isFalse();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isZero();
            assertThat(service.getStudentEnrollments("S1")).singleElement()
                    .satisfies(e -> {
                        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
                        assertThat(e.getNotes()).contains("schedule");
                    });
        }

        @Test
        void dropWithoutEnrollmentFails() {
            assertThat(service.dropStudent("S1", "CS101", "x")).isFalse();
            enroll("S1", "CS101");
            service.dropStudent("S1", "CS101", "x");
            assertThat(service.dropStudent("S1", "CS101", "again")).isFalse();
        }

        @Test
        void waitlistRejectsDuplicatesAndEnrolledStudents() {
            enroll("S1", "CS101");

            assertThat(service.addToWaitlist("S1", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.addToWaitlist("S2", "CS101", SEM, YEAR)).isTrue();
            assertThat(service.addToWaitlist("S2", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.addToWaitlist("", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.isStudentWaitlisted("S2", "CS101")).isTrue();
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(1);
        }

        @Test
        void waitlistIsCappedAtTwenty() {
            IntStream.rangeClosed(1, 20)
                    .forEach(i -> assertThat(service.addToWaitlist("W" + i, "CS101", SEM, YEAR)).isTrue());

            assertThat(service.addToWaitlist("W21", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(20);
        }

        @Test
        void dropPromotesFirstWaitlistedStudentInOrder() {
            service.setCourseLimit("CS101", 1);
            enroll("S1", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            service.addToWaitlist("W2", "CS101", SEM, YEAR);

            service.dropStudent("S1", "CS101", "leaving");

            assertThat(service.isStudentEnrolled("W1", "CS101")).isTrue();
            assertThat(service.isStudentWaitlisted("W1", "CS101")).isFalse();
            assertThat(service.isStudentWaitlisted("W2", "CS101")).isTrue();
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(1);
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(1);
        }

        @Test
        void processWaitlistNeverExceedsCapacity() {
            service.setCourseLimit("CS101", 2);
            enroll("S1", "CS101");
            IntStream.rangeClosed(1, 3).forEach(i -> service.addToWaitlist("W" + i, "CS101", SEM, YEAR));

            assertThat(service.processWaitlist("CS101", 10)).isEqualTo(1);
            assertThat(service.getCurrentEnrollmentCount("CS101")).isEqualTo(2);
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(2);
            assertThat(service.processWaitlist("CS101", 10)).isZero();
        }

        @Test
        void processWaitlistHonoursRequestedNumber() {
            IntStream.rangeClosed(1, 3).forEach(i -> service.addToWaitlist("W" + i, "CS101", SEM, YEAR));

            assertThat(service.processWaitlist("CS101", 2)).isEqualTo(2);
            assertThat(service.isStudentEnrolled("W1", "CS101")).isTrue();
            assertThat(service.isStudentEnrolled("W2", "CS101")).isTrue();
            assertThat(service.isStudentWaitlisted("W3", "CS101")).isTrue();
            assertThat(service.processWaitlist("EMPTY", 5)).isZero();
        }

        @Test
        void removeFromWaitlist() {
            service.addToWaitlist("W1", "CS101", SEM, YEAR);

            assertThat(service.removeFromWaitlist("W1", "CS101")).isTrue();
            assertThat(service.isStudentWaitlisted("W1", "CS101")).isFalse();
            assertThat(service.getCurrentWaitlistCount("CS101")).isZero();
            assertThat(service.removeFromWaitlist("W1", "CS101")).isFalse();
        }

        @Test
        void withdrawMarksEnrollmentWithdrawn() {
            enroll("S1", "CS101");

            assertThat(service.withdrawStudent("S1", "CS101", "medical")).isTrue();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isFalse();
            assertThat(service.getEnrollmentsByStatus(EnrollmentStatus.WITHDRAWN)).hasSize(1);
            assertThat(service.withdrawStudent("S1", "CS101", "again")).isFalse();
        }
    }

    @Nested
    class Transfers {

        @Test
        void transfersBetweenCourses() {
            enroll("S1", "CS101");

            assertThat(service.transferStudent("S1", "CS101", "CS102", SEM, YEAR)).isTrue();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isFalse();
            assertThat(service.isStudentEnrolled("S1", "CS102")).isTrue();
        }

        @Test
        void transferRequiresSourceEnrollmentAndTargetCapacity() {
            assertThat(service.transferStudent("S1", "CS101", "CS102", SEM, YEAR)).isFalse();

            enroll("S1", "CS101");
            service.setCourseLimit("CS102", 1);
            enroll("S2", "CS102");

            assertThat(service.transferStudent("S1", "CS101", "CS102", SEM, YEAR)).isFalse();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
        }

        @Test
        void failedTransferDoesNotGiveAwayTheStudentsSeat() {
            // Regression: transfer dropped the student (promoting the waitlist) before
            // discovering that prerequisites for the target were unmet, and the rollback
            // re-enrollment then failed because the seat was gone.
            service.setCourseLimit("CS101", 1);
            enroll("S1", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            service.addPrerequisite("CS301", "CS201");

            assertThat(service.transferStudent("S1", "CS101", "CS301", SEM, YEAR)).isFalse();

            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
            assertThat(service.isStudentWaitlisted("W1", "CS101")).isTrue();
            assertThat(service.isStudentEnrolled("W1", "CS101")).isFalse();
        }

        @Test
        void transferToSameOrAlreadyEnrolledCourseIsRejected() {
            enroll("S1", "CS101");
            enroll("S1", "CS102");

            assertThat(service.transferStudent("S1", "CS101", "CS101", SEM, YEAR)).isFalse();
            assertThat(service.transferStudent("S1", "CS101", "CS102", SEM, YEAR)).isFalse();
            assertThat(service.isStudentEnrolled("S1", "CS101")).isTrue();
            assertThat(service.isStudentEnrolled("S1", "CS102")).isTrue();
        }
    }

    @Nested
    class Statistics {

        @Test
        void courseStatisticsCountDroppedAndCompletedRecords() {
            // Regression: dropped records were removed from the course index, so the
            // statistics always reported zero drops.
            service.setCourseLimit("CS101", 2);
            enroll("S1", "CS101");
            enroll("S2", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            service.addToWaitlist("W2", "CS101", SEM, YEAR);
            service.dropStudent("S1", "CS101", "x"); // promotes W1
            service.updateEnrollmentStatus(enrollmentOf("S2", "CS101").getEnrollmentId(), EnrollmentStatus.COMPLETED);

            EnrollmentStatistics stats = service.getEnrollmentStatistics("CS101");

            assertThat(stats.getEnrolled()).isEqualTo(1);
            assertThat(stats.getWaitlisted()).isEqualTo(1);
            assertThat(stats.getDropped()).isEqualTo(1);
            assertThat(stats.getCompleted()).isEqualTo(1);
            assertThat(stats.getCapacity()).isEqualTo(2);
        }

        @Test
        void overallStatisticsOnFreshServiceArePopulated() {
            // Regression: the cache timestamp was initialised in the constructor, so a
            // fresh service returned an empty (cached) map for five minutes.
            Map<String, Object> stats = service.calculateOverallStatistics();

            assertThat(stats).containsEntry("totalEnrollments", 0)
                    .containsEntry("activeEnrollments", 0L)
                    .containsEntry("completionRate", 0.0)
                    .containsEntry("dropRate", 0.0);
        }

        @Test
        void overallStatisticsReflectMutations() {
            enroll("S1", "CS101");
            enroll("S2", "CS101");
            assertThat(service.calculateOverallStatistics()).containsEntry("totalEnrollments", 2);

            service.dropStudent("S1", "CS101", "x");
            Map<String, Object> stats = service.calculateOverallStatistics();

            assertThat(stats).containsEntry("totalEnrollments", 2)
                    .containsEntry("activeEnrollments", 1L)
                    .containsEntry("dropRate", 50.0);
        }

        @Test
        void enrollmentRateAndOverenrollment() {
            service.setCourseLimit("CS101", 4);
            enroll("S1", "CS101");
            assertThat(service.getEnrollmentRateByCourse()).containsEntry("CS101", 25.0);

            enroll("S2", "CS101");
            service.setCourseLimit("CS101", 1); // shrink below current enrollment
            assertThat(service.getOverenrolledCourses()).containsExactly("CS101");
            assertThat(service.calculateOverallStatistics()).containsEntry("overenrolledCourses", 1);
        }

        @Test
        void groupingsByStatusAndSemester() {
            enroll("S1", "CS101");
            service.enrollStudent("S2", "CS101", "Spring", 2026);
            service.addToWaitlist("S3", "CS101", SEM, YEAR);

            assertThat(service.getEnrollmentStatisticsByStatus())
                    .containsEntry(EnrollmentStatus.ENROLLED, 2L)
                    .containsEntry(EnrollmentStatus.WAITLISTED, 1L);
            assertThat(service.getEnrollmentStatisticsBySemester())
                    .containsEntry("Fall 2025", 2L)
                    .containsEntry("Spring 2026", 1L);
            assertThat(service.getEnrollmentsBySemester("Spring", 2026)).hasSize(1);
        }
    }

    @Nested
    class Searching {

        @BeforeEach
        void seed() {
            enroll("ALICE", "CS101");
            enroll("BOB", "CS101");
            enroll("ALICE", "MATH200");
        }

        @Test
        void keywordSearchIsCaseInsensitive() {
            assertThat(service.search("alice")).hasSize(2);
            assertThat(service.search("math")).singleElement()
                    .extracting(Enrollment::getStudentId).isEqualTo("ALICE");
            assertThat(service.countSearchResults("zzz")).isZero();
        }

        @Test
        void criteriaSearch() {
            Map<String, SearchCriterion> criteria = Map.of(
                    "courseId", new SearchCriterion(SearchCriteria.EXACT_MATCH, "cs101"),
                    "studentId", new SearchCriterion(SearchCriteria.STARTS_WITH, "b"));

            assertThat(service.search(criteria)).singleElement()
                    .extracting(Enrollment::getStudentId).isEqualTo("BOB");
            assertThat(service.search(Map.of("unknown", new SearchCriterion(SearchCriteria.CONTAINS, "x")))).isEmpty();
        }

        @Test
        void sortedSearchAndPagination() {
            assertThat(service.searchAndSort("cs101", "studentId", SortOrder.DESC))
                    .extracting(Enrollment::getStudentId).containsExactly("BOB", "ALICE");

            SearchResult<Enrollment> page = service.searchWithPagination("", 1, 2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getResults()).hasSize(1);
        }

        @Test
        void suggestionsAreDistinctAndLimited() {
            assertThat(service.getSearchSuggestions("cs", 10)).containsExactly("CS101");
            assertThat(service.getSearchSuggestions("a", 1)).hasSize(1);
        }
    }

    @Test
    void reportsCoverAllEnrollments() {
        enroll("S1", "CS101");
        enroll("S2", "CS101");

        ReportData report = service.generateReport(ReportType.ENROLLMENT_REPORT);
        assertThat(report.getRows()).hasSize(2);
        assertThat(report.getRows()).allSatisfy(row -> assertThat(row).containsEntry("Grade", "N/A"));

        ReportData unsupported = service.generateReport(ReportType.GRADE_REPORT);
        assertThat(unsupported.getTitle()).isEqualTo("Unsupported Report Type");
    }

    @Nested
    class SearchingByField {

        @BeforeEach
        void seed() {
            enroll("ALICE", "CS101");
            service.enrollStudent("BOB", "MATH200", "Spring", 2026);
            service.addToWaitlist("CAROL", "CS101", SEM, YEAR);
            enrollmentOf("ALICE", "CS101").setGrade(Enrollment.Grade.B_PLUS);
        }

        @ParameterizedTest
        @CsvSource({
                "semester,EXACT_MATCH,spring,BOB",
                "year,EXACT_MATCH,2025,ALICE;CAROL",
                "status,EXACT_MATCH,waitlisted,CAROL",
                "enrollmentType,CONTAINS,regular,ALICE;BOB;CAROL",
                "grade,EXACT_MATCH,b_plus,ALICE",
                "courseId,ENDS_WITH,200,BOB",
                "enrollmentDate,CONTAINS,T,ALICE;BOB;CAROL",
                "studentId,REGEX,.*,''"})
        void criteriaOnEveryField(String field, SearchCriteria criteria, String value, String expected) {
            Map<String, SearchCriterion> query = Map.of(field, new SearchCriterion(criteria, value));
            List<String> students = service.search(query).stream().map(Enrollment::getStudentId).sorted().toList();

            assertThat(String.join(";", students)).isEqualTo(expected);
            assertThat(service.countSearchResults(query)).isEqualTo(students.size());
        }

        @ParameterizedTest
        @CsvSource({
                "studentId,ASC,ALICE;BOB;CAROL",
                "courseId,DESC,BOB",
                "semester,DESC,BOB",
                "year,DESC,BOB",
                "status,DESC,CAROL"})
        void sortByField(String sortBy, SortOrder order, String expectedPrefix) {
            String students = String.join(";", service.searchAndSort("", sortBy, order).stream()
                    .map(Enrollment::getStudentId).toList());
            assertThat(students).startsWith(expectedPrefix);
        }

        @ParameterizedTest
        @CsvSource({"enrollmentDate", "unknown"})
        void sortByEnrollmentDateIsTheDefault(String sortBy) {
            assertThat(service.searchAndSort("", sortBy, SortOrder.ASC))
                    .hasSize(3)
                    .isSortedAccordingTo(Comparator.comparing(Enrollment::getEnrollmentDate));
        }

        @Test
        void sortByAdvertisedGradeField() {
            // Regression: "grade" is advertised by getSortableFields() but the comparator
            // silently fell back to sorting by enrollment date.
            enrollmentOf("BOB", "MATH200").setGrade(Enrollment.Grade.A);
            assertThat(service.getSortableFields()).contains("grade");

            assertThat(service.searchAndSort("", "grade", SortOrder.ASC))
                    .extracting(Enrollment::getStudentId).containsExactly("BOB", "ALICE", "CAROL");
            assertThat(service.searchAndSort("", "grade", SortOrder.DESC))
                    .extracting(Enrollment::getStudentId).containsExactly("CAROL", "ALICE", "BOB");
        }

        @Test
        void advancedPagination() {
            SearchResult<Enrollment> result = service.advancedSearchWithPagination(
                    Map.of("enrollmentType", new SearchCriterion(SearchCriteria.EXACT_MATCH, "regular")),
                    "studentId", SortOrder.DESC, 0, 2);

            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getResults()).extracting(Enrollment::getStudentId).containsExactly("CAROL", "BOB");
            assertThat(result.getSortBy()).isEqualTo("studentId");
            assertThat(service.advancedSearchWithPagination(Map.of(), "studentId", SortOrder.ASC, 1, 2).getResults())
                    .extracting(Enrollment::getStudentId).containsExactly("CAROL");
        }

        @Test
        void predicateSearchAndFilter() {
            assertThat(service.search(e -> e.getYear() == 2026)).extracting(Enrollment::getStudentId).containsExactly("BOB");
            assertThat(service.filter(e -> e.getStatus() == EnrollmentStatus.WAITLISTED)).hasSize(1);
            assertThat(service.getEnrollmentsByType(Enrollment.EnrollmentType.REGULAR)).hasSize(3);
            assertThat(service.getEnrollmentsByType(Enrollment.EnrollmentType.AUDIT)).isEmpty();
            assertThat(service.getSearchableFields()).contains("grade", "year");
        }

        @Test
        void reportShowsLetterGrade() {
            ReportData report = service.generateReport(ReportType.ENROLLMENT_REPORT);

            assertThat(report.getMetadata()).containsEntry("totalEnrollments", 3);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Student ID", "ALICE")
                    .containsEntry("Grade", "B+")
                    .containsEntry("Semester", "Fall 2025")
                    .containsEntry("Status", "ENROLLED"));
        }
    }

    @Nested
    class Housekeeping {

        @Test
        void lookupAndStatusUpdates() {
            enroll("S1", "CS101");
            Enrollment e = enrollmentOf("S1", "CS101");

            assertThat(service.getEnrollmentById(e.getEnrollmentId())).containsSame(e);
            assertThat(service.getEnrollmentById("NOPE")).isEmpty();
            assertThat(service.updateEnrollmentStatus("NOPE", EnrollmentStatus.COMPLETED)).isFalse();
            assertThat(service.withdrawStudent("S1", "NOPE", "x")).isFalse();
        }

        @Test
        void completedEnrollmentsNoLongerOccupySeats() {
            service.setCourseLimit("CS101", 1);
            complete("S1", "CS101");

            assertThat(service.getCourseEnrollments("CS101")).isEmpty();
            assertThat(service.hasAvailableSpots("CS101")).isTrue();
            assertThat(enroll("S2", "CS101")).isTrue();
        }

        @Test
        void processWaitlistSkipsWhenCourseIsFull() {
            service.setCourseLimit("CS101", 1);
            enroll("S1", "CS101");
            service.addToWaitlist("W1", "CS101", SEM, YEAR);
            service.addToWaitlist("W2", "CS101", SEM, YEAR);

            assertThat(service.processWaitlist("CS101", 2)).isZero();
            assertThat(service.getCurrentWaitlistCount("CS101")).isEqualTo(2);
        }

        @Test
        void statisticsAreCachedAndCopied() {
            enroll("S1", "CS101");
            Map<String, Object> first = service.calculateOverallStatistics();
            first.clear();

            assertThat(service.getSummaryStatistics()).containsEntry("totalEnrollments", 1)
                    .containsEntry("totalWaitlistCount", 0)
                    .containsEntry("averageEnrollmentRate", 0.0);
        }

        @Test
        void completionRateAndAverageEnrollmentRate() {
            service.setCourseLimit("CS101", 4);
            service.setCourseLimit("CS102", 2);
            complete("S1", "CS101");
            enroll("S2", "CS101");
            enroll("S3", "CS102");

            Map<String, Object> stats = service.calculateOverallStatistics();
            assertThat((double) stats.get("completionRate")).isCloseTo(100.0 / 3, within(1e-9));
            assertThat(stats).containsEntry("averageEnrollmentRate", (25.0 + 50.0) / 2);
        }

        @Test
        void statisticalSummaryAndReportingMetadata() {
            enroll("S1", "CS101");

            ReportData summary = service.generateReportForDateRange(ReportType.STATISTICAL_SUMMARY,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now());
            assertThat(summary.getTitle()).isEqualTo("Enrollment Statistical Summary");
            assertThat(summary.getContent()).contains("totalEnrollments: 1");

            assertThat(service.getAvailableReportTypes())
                    .containsExactly(ReportType.ENROLLMENT_REPORT, ReportType.STATISTICAL_SUMMARY);
            assertThat(service.getSupportedFormats()).contains(ReportFormat.PDF);
            assertThat(service.scheduleRecurringReport(ReportType.ENROLLMENT_REPORT, "weekly", List.of()))
                    .startsWith("SCHED_ENR_");
            assertThat(service.cancelScheduledReport("x")).isTrue();
            assertThat(service.getReportHistory(ReportType.ENROLLMENT_REPORT, 2)).isEmpty();
            assertThat(service.exportReport(summary, ReportFormat.JSON, "x.json")).isTrue();
        }
    }
}
