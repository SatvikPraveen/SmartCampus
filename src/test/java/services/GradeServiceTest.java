package services;

import interfaces.Reportable.ReportData;
import interfaces.Reportable.ReportFormat;
import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SearchResult;
import interfaces.Searchable.SortOrder;
import models.Grade;
import models.Grade.GradeComponent;
import models.Grade.GradeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GradeServiceTest {

    private GradeService service;
    private int seq;

    @BeforeEach
    void setUp() {
        service = new GradeService();
        seq = 0;
    }

    /** An ungraded (DRAFT) assignment worth 100 points. */
    private Grade draft(String student, String course, GradeComponent component) {
        seq++;
        Grade g = new Grade("G" + seq, "E_" + student + "_" + course, student, course, "Task" + seq, component);
        g.setPointsPossible(100);
        return g;
    }

    /** A graded assignment worth 100 points, added to the service. */
    private Grade addGraded(String student, String course, GradeComponent component, double points) {
        Grade g = draft(student, course, component);
        g.submitAssignment();
        assertThat(g.gradeAssignment(points, "P1", "")).isTrue();
        assertThat(service.addGrade(g)).isTrue();
        return g;
    }

    @Nested
    class Crud {

        @Test
        void addIndexesByStudentCourseAndRejectsDuplicates() {
            Grade g = addGraded("S1", "CS101", GradeComponent.EXAM, 90);

            assertThat(service.getGradeById(g.getGradeId())).containsSame(g);
            assertThat(service.getStudentGrades("S1")).containsExactly(g);
            assertThat(service.getCourseGrades("CS101")).containsExactly(g);
            assertThat(service.addGrade(g)).isFalse();
            assertThat(service.addGrade(null)).isFalse();
            assertThat(service.addGrade(new Grade())).isFalse();
        }

        @Test
        void removeCleansIndicesAndGpa() {
            Grade g = addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(4.0);

            assertThat(service.removeGrade(g.getGradeId())).isTrue();
            assertThat(service.getStudentGrades("S1")).isEmpty();
            assertThat(service.getCourseGrades("CS101")).isEmpty();
            assertThat(service.calculateStudentGPA("S1")).isZero();
            assertThat(service.removeGrade(g.getGradeId())).isFalse();
            assertThat(service.removeGrade("")).isFalse();
        }

        @Test
        void updateReplacesGradeAndRefreshesGpa() {
            Grade g = addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(4.0);

            Grade replacement = new Grade(g.getGradeId(), g.getEnrollmentId(), "S1", "CS101", "Redo", GradeComponent.EXAM);
            replacement.setPointsPossible(100);
            replacement.submitAssignment();
            replacement.gradeAssignment(75, "P1", "");

            assertThat(service.updateGrade(replacement)).isTrue();
            assertThat(service.getGradeById(g.getGradeId())).containsSame(replacement);
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(2.0);
            assertThat(service.updateGrade(draft("S1", "CS101", GradeComponent.EXAM))).isFalse();
        }

        @Test
        void updateThatChangesStudentMovesTheGradeBetweenStudents() {
            // Regression: updateGrade left the student/course indices pointing at the old owner.
            Grade g = addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            assertThat(service.calculateStudentGPA("S2")).isZero();

            Grade moved = new Grade(g.getGradeId(), "E_S2", "S2", "CS102", "Task", GradeComponent.EXAM);
            moved.setPointsPossible(100);
            moved.submitAssignment();
            moved.gradeAssignment(95, "P1", "");
            service.updateGrade(moved);

            assertThat(service.getStudentGrades("S1")).isEmpty();
            assertThat(service.getStudentGrades("S2")).containsExactly(moved);
            assertThat(service.getCourseGrades("CS101")).isEmpty();
            assertThat(service.getCourseGrades("CS102")).containsExactly(moved);
            assertThat(service.calculateStudentGPA("S1")).isZero();
            assertThat(service.calculateStudentGPA("S2")).isEqualTo(4.0);
        }
    }

    @Nested
    class Assigning {

        @Test
        void assignGradeProducesAGradedEntry() {
            // Regression: assignGrade created a DRAFT grade, which Grade.gradeAssignment
            // refuses, so the stored grade had no points, letter or GRADED status.
            assertThat(service.assignGrade("S1", "CS101", "Midterm", GradeComponent.MIDTERM, 88, 100, "P1", "good"))
                    .isTrue();

            Grade g = service.getStudentGrades("S1").get(0);
            assertThat(g.getStatus()).isEqualTo(GradeStatus.GRADED);
            assertThat(g.getPointsEarned()).isEqualTo(88);
            assertThat(g.getPercentage()).isEqualTo(88.0);
            assertThat(g.getLetterGrade()).isEqualTo("B+");
            assertThat(g.getGradedBy()).isEqualTo("P1");
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(3.3);
        }

        @ParameterizedTest
        @CsvSource({"-1,100", "101,100", "10,0", "10,-5"})
        void assignGradeRejectsInvalidPoints(double earned, double possible) {
            assertThat(service.assignGrade("S1", "CS101", "HW", GradeComponent.HOMEWORK, earned, possible, "P1", ""))
                    .isFalse();
            assertThat(service.getAllGrades()).isEmpty();
        }

        @Test
        void assignGradeRejectsBlankIds() {
            assertThat(service.assignGrade("", "CS101", "HW", GradeComponent.HOMEWORK, 5, 10, "P1", "")).isFalse();
            assertThat(service.assignGrade("S1", null, "HW", GradeComponent.HOMEWORK, 5, 10, "P1", "")).isFalse();
        }

        @Test
        void updateGradePointsReportsFailureForInvalidPoints() {
            // Regression: updateGradePoints returned true even when the grade rejected the points.
            Grade g = addGraded("S1", "CS101", GradeComponent.EXAM, 50);

            assertThat(service.updateGradePoints(g.getGradeId(), 150, "P1", "")).isFalse();
            assertThat(g.getPointsEarned()).isEqualTo(50);

            assertThat(service.updateGradePoints(g.getGradeId(), 95, "P1", "regrade")).isTrue();
            assertThat(g.getLetterGrade()).isEqualTo("A");
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(4.0);
            assertThat(service.updateGradePoints("NOPE", 10, "P1", "")).isFalse();
        }

        @Test
        void updateGradePointsOnDraftFails() {
            Grade g = draft("S1", "CS101", GradeComponent.EXAM);
            service.addGrade(g);

            assertThat(service.updateGradePoints(g.getGradeId(), 80, "P1", "")).isFalse();
        }

        @Test
        void workflowSubmitGradeReturn() {
            Grade g = draft("S1", "CS101", GradeComponent.HOMEWORK);
            service.addGrade(g);

            assertThat(service.getGradesPendingReview()).isEmpty();
            assertThat(service.submitAssignment(g.getGradeId())).isTrue();
            assertThat(service.submitAssignment(g.getGradeId())).isFalse();
            assertThat(service.getGradesPendingReview()).containsExactly(g);

            assertThat(service.returnGradedAssignment(g.getGradeId())).isFalse();
            assertThat(service.updateGradePoints(g.getGradeId(), 70, "P1", "")).isTrue();
            assertThat(service.returnGradedAssignment(g.getGradeId())).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.RETURNED);
            assertThat(service.submitAssignment("NOPE")).isFalse();
        }

        @Test
        void excusingAGradeRefreshesGpa() {
            // Regression: excuseAssignment changed whether the grade counts without
            // invalidating the cached GPA.
            addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            Grade bad = addGraded("S1", "CS102", GradeComponent.EXAM, 40);
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(2.0);

            assertThat(service.excuseAssignment(bad.getGradeId(), "illness")).isTrue();
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(4.0);
        }
    }

    @Nested
    class Calculations {

        @Test
        void gpaAveragesPerCourseNotPerAssignment() {
            addGraded("S1", "CS101", GradeComponent.HOMEWORK, 100);
            addGraded("S1", "CS101", GradeComponent.HOMEWORK, 80); // CS101 avg 90 -> 3.7
            addGraded("S1", "CS102", GradeComponent.EXAM, 50);     // CS102 -> 0.0

            assertThat(service.calculateStudentGPA("S1")).isCloseTo(1.85, within(1e-9));
            assertThat(service.calculateStudentGPA("UNKNOWN")).isZero();
        }

        @Test
        void courseAverageIgnoresUngradedEntries() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 80);
            addGraded("S2", "CS101", GradeComponent.EXAM, 60);
            service.addGrade(draft("S3", "CS101", GradeComponent.EXAM));

            assertThat(service.calculateCourseAverage("CS101")).hasValue(70.0);
            assertThat(service.calculateCourseAverage("NONE")).isEmpty();
        }

        @Test
        void weightedCourseGradeUsesDefaultWeights() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 90);     // 0.4
            addGraded("S1", "CS101", GradeComponent.HOMEWORK, 60); // 0.3

            double expected = (90 * 0.4 + 60 * 0.3) / 0.7;
            assertThat(service.calculateWeightedCourseGrade("S1", "CS101").getAsDouble())
                    .isCloseTo(expected, within(1e-9));
        }

        @Test
        void weightedCourseGradeWithCustomWeightsAndUnweightedComponents() {
            Map<GradeComponent, Double> weights = new EnumMap<>(GradeComponent.class);
            weights.put(GradeComponent.EXAM, 0.25);
            weights.put(GradeComponent.LAB, 0.75);
            assertThat(service.setGradeWeights("CS101", weights)).isTrue();

            addGraded("S1", "CS101", GradeComponent.EXAM, 40);
            addGraded("S1", "CS101", GradeComponent.LAB, 80);
            assertThat(service.calculateWeightedCourseGrade("S1", "CS101")).hasValue(70.0);

            // only a component with no weight -> no meaningful grade
            addGraded("S2", "CS101", GradeComponent.QUIZ, 90);
            assertThat(service.calculateWeightedCourseGrade("S2", "CS101")).isEmpty();
            assertThat(service.calculateWeightedCourseGrade("S3", "CS101")).isEmpty();
        }

        @Test
        void gradeWeightsMustSumToOne() {
            assertThat(service.setGradeWeights("CS101", Map.of(GradeComponent.EXAM, 0.5))).isFalse();
            assertThat(service.setGradeWeights("CS101", Map.of(GradeComponent.EXAM, 0.995))).isTrue();
            assertThat(service.setGradeWeights("CS101", Map.of())).isFalse();
            assertThat(service.setGradeWeights("", Map.of(GradeComponent.EXAM, 1.0))).isFalse();
            assertThat(service.setGradeWeights("CS101", null)).isFalse();
        }

        @Test
        void failingGradesExcludeUngradedEntries() {
            // Regression: ungraded entries have percentage 0.0 by default and were
            // reported as failing.
            Grade fail = addGraded("S1", "CS101", GradeComponent.EXAM, 59);
            addGraded("S2", "CS101", GradeComponent.EXAM, 60);
            service.addGrade(draft("S3", "CS101", GradeComponent.EXAM));

            assertThat(service.getFailingGrades()).containsExactly(fail);
        }

        @Test
        void highPerformersSortedDescending() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 91);
            addGraded("S2", "CS101", GradeComponent.EXAM, 99);
            addGraded("S3", "CS101", GradeComponent.EXAM, 70);

            assertThat(service.getHighPerformingGrades(90))
                    .extracting(Grade::getStudentId).containsExactly("S2", "S1");
        }

        @Test
        void atRiskAndTopPerformers() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            addGraded("S2", "CS101", GradeComponent.EXAM, 55);
            addGraded("S3", "CS101", GradeComponent.EXAM, 85);
            service.addGrade(draft("S4", "CS101", GradeComponent.EXAM)); // nothing graded yet

            assertThat(service.getStudentsAtRisk(60)).containsExactly("S2");
            assertThat(service.getTopPerformers(2)).containsExactly("S1", "S3");
        }

        @Test
        void distributionAndCourseStatistics() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            addGraded("S2", "CS101", GradeComponent.EXAM, 94);
            addGraded("S3", "CS101", GradeComponent.EXAM, 81);

            assertThat(service.getGradeDistribution("CS101")).containsEntry("A", 2L).containsEntry("B-", 1L);

            Map<String, Object> stats = service.getCourseGradeStatistics("CS101");
            assertThat(stats).containsEntry("count", 3)
                    .containsEntry("median", 94.0)
                    .containsEntry("min", 81.0)
                    .containsEntry("max", 95.0);
            assertThat(service.getCourseGradeStatistics("EMPTY")).isEmpty();
        }

        @Test
        void overallStatistics() {
            assertThat(service.calculateOverallStatistics()).isEmpty();

            addGraded("S1", "CS101", GradeComponent.EXAM, 90);
            service.addGrade(draft("S2", "CS101", GradeComponent.EXAM));

            Map<String, Object> stats = service.calculateOverallStatistics();
            assertThat(stats).containsEntry("totalGrades", 2)
                    .containsEntry("validGrades", 1)
                    .containsEntry("averageGrade", 90.0)
                    .containsEntry("failingGradeCount", 0)
                    .containsEntry("completionRate", 50.0);
        }
    }

    @Nested
    class Searching {

        @BeforeEach
        void seed() {
            addGraded("ALICE", "CS101", GradeComponent.EXAM, 95);
            addGraded("BOB", "CS101", GradeComponent.HOMEWORK, 70);
            addGraded("ALICE", "MATH1", GradeComponent.QUIZ, 80);
        }

        @Test
        void keywordAndCriteria() {
            assertThat(service.search("alice")).hasSize(2);
            assertThat(service.search(Map.of("component", new SearchCriterion(SearchCriteria.EXACT_MATCH, "homework"))))
                    .extracting(Grade::getStudentId).containsExactly("BOB");
            assertThat(service.getGradesByComponent(GradeComponent.QUIZ)).hasSize(1);
            assertThat(service.getGradesByStatus(GradeStatus.GRADED)).hasSize(3);
        }

        @Test
        void sorted() {
            List<Grade> sorted = service.searchAndSort("", "percentage", SortOrder.DESC);
            assertThat(sorted).extracting(Grade::getPercentage).containsExactly(95.0, 80.0, 70.0);
        }

        @Test
        void reportRowsPerGrade() {
            assertThat(service.generateReport(ReportType.GRADE_REPORT).getRows()).hasSize(3);
        }

        @Test
        void keywordMatchesEveryTextualField() {
            assertThat(service.search("MATH")).extracting(Grade::getCourseId).containsExactly("MATH1");
            assertThat(service.search("task3")).extracting(Grade::getCourseId).containsExactly("MATH1");
            assertThat(service.search("quiz")).extracting(Grade::getComponent).containsExactly(GradeComponent.QUIZ);
            assertThat(service.search("graded")).hasSize(3);
            assertThat(service.search("c-")).as("letter grade").extracting(Grade::getStudentId).containsExactly("BOB");
            assertThat(service.countSearchResults("zzz")).isZero();
        }

        @ParameterizedTest
        @CsvSource({
                "studentId,EXACT_MATCH,bob,BOB/CS101",
                "courseId,STARTS_WITH,math,ALICE/MATH1",
                "assignmentName,CONTAINS,task2,BOB/CS101",
                "status,EXACT_MATCH,graded,ALICE/CS101;ALICE/MATH1;BOB/CS101",
                "letterGrade,EXACT_MATCH,a,ALICE/CS101",
                "percentage,STARTS_WITH,9,ALICE/CS101",
                "pointsEarned,ENDS_WITH,0.0,BOB/CS101;ALICE/MATH1",
                "pointsPossible,EXACT_MATCH,100.0,ALICE/CS101;ALICE/MATH1;BOB/CS101",
                "dateAssigned,CONTAINS,-,ALICE/CS101;ALICE/MATH1;BOB/CS101",
                "dateGraded,CONTAINS,:,ALICE/CS101;ALICE/MATH1;BOB/CS101",
                "dateDue,CONTAINS,2,''",
                "studentId,REGEX,.*,''",
                "unknown,CONTAINS,x,''"})
        void criteriaOnEveryField(String field, SearchCriteria criteria, String value, String expected) {
            List<String> keys = service.search(Map.of(field, new SearchCriterion(criteria, value))).stream()
                    .map(g -> g.getStudentId() + "/" + g.getCourseId()).sorted().toList();
            assertThat(keys).containsExactlyInAnyOrder(expected.isEmpty() ? new String[0] : expected.split(";"));
            assertThat(service.countSearchResults(Map.of(field, new SearchCriterion(criteria, value))))
                    .isEqualTo(keys.size());
        }

        @ParameterizedTest
        @CsvSource({
                "studentId,ASC,ALICE",
                "studentId,DESC,BOB",
                "courseId,DESC,MATH1",
                "assignmentName,DESC,MATH1",
                "component,DESC,CS101",
                "pointsEarned,ASC,CS101",
                "percentage,DESC,CS101"})
        void sortByField(String sortBy, SortOrder order, String expectedFirstCourseOrStudent) {
            Grade first = service.searchAndSort("", sortBy, order).get(0);
            assertThat(List.of(first.getStudentId(), first.getCourseId())).contains(expectedFirstCourseOrStudent);
        }

        @Test
        void sortByComponentFollowsEnumOrder() {
            assertThat(service.searchAndSort("", "component", SortOrder.ASC))
                    .extracting(Grade::getComponent)
                    .containsExactly(GradeComponent.EXAM, GradeComponent.QUIZ, GradeComponent.HOMEWORK);
            assertThat(service.searchAndSort("", "pointsEarned", SortOrder.ASC))
                    .extracting(Grade::getPointsEarned).containsExactly(70.0, 80.0, 95.0);
        }

        @ParameterizedTest
        @CsvSource({"dateAssigned", "dateDue", "dateGraded", "status", "unknown"})
        void sortByDateLikeFieldsKeepsAllResults(String sortBy) {
            assertThat(service.searchAndSort("", sortBy, SortOrder.ASC)).hasSize(3);
        }

        @Test
        void pagination() {
            SearchResult<Grade> page = service.searchWithPagination("alice", 0, 1);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getResults()).hasSize(1);

            SearchResult<Grade> advanced = service.advancedSearchWithPagination(
                    Map.of("status", new SearchCriterion(SearchCriteria.EXACT_MATCH, "graded")),
                    "percentage", SortOrder.DESC, 1, 2);
            assertThat(advanced.getTotalElements()).isEqualTo(3);
            assertThat(advanced.getResults()).extracting(Grade::getPercentage).containsExactly(70.0);
            assertThat(advanced.getSortBy()).isEqualTo("percentage");

            assertThat(service.advancedSearchWithPagination(Map.of(), "percentage", SortOrder.ASC, 0, 2)
                    .getResults()).extracting(Grade::getPercentage).containsExactly(70.0, 80.0);
        }

        @Test
        void predicateSearchAndFilter() {
            assertThat(service.search(g -> g.getPercentage() > 75)).hasSize(2);
            assertThat(service.filter(g -> "BOB".equals(g.getStudentId()))).hasSize(1);
        }

        @Test
        void suggestionsAreDistinctAndLimited() {
            assertThat(service.getSearchSuggestions("ali", 10)).containsExactly("ALICE");
            assertThat(service.getSearchSuggestions("A", 2)).hasSize(2);
            assertThat(service.getSearchSuggestions("b-", 10)).containsExactly("B-");
        }

        @Test
        void advertisedFields() {
            assertThat(service.getSearchableFields()).contains("studentId", "letterGrade", "pointsEarned");
            assertThat(service.getSortableFields()).contains("dateDue", "percentage", "component");
        }
    }

    @Nested
    class GpaScale {

        @ParameterizedTest
        @CsvSource({
                "100,4.0", "97,4.0", "93,4.0", "90,3.7", "87,3.3", "83,3.0", "80,2.7",
                "77,2.3", "73,2.0", "70,1.7", "67,1.3", "63,1.0", "60,0.7", "59,0.0"})
        void percentageMapsToGpaPoints(double points, double expectedGpa) {
            addGraded("S1", "CS101", GradeComponent.EXAM, points);
            assertThat(service.calculateStudentGPA("S1")).isEqualTo(expectedGpa);
        }

        @Test
        void droppedGradesDoNotCount() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            Grade dropped = addGraded("S1", "CS102", GradeComponent.EXAM, 10);
            dropped.dropGrade();

            assertThat(service.calculateStudentGPA("S1")).isEqualTo(4.0);
            assertThat(service.calculateCourseAverage("CS102")).isEmpty();
            assertThat(service.getGradeDistribution("CS102")).isEmpty();
        }

        @Test
        void studentWithOnlyUngradedWorkHasZeroGpa() {
            service.addGrade(draft("S1", "CS101", GradeComponent.EXAM));
            assertThat(service.calculateStudentGPA("S1")).isZero();
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void lateSubmissionIsPendingReview() {
            Grade late = new Grade("L1", "E1", "S1", "CS101", "A1", "Essay", GradeComponent.HOMEWORK, 100,
                    LocalDateTime.now().minusDays(1));
            service.addGrade(late);

            assertThat(service.getOverdueAssignments()).containsExactly(late);
            assertThat(service.submitAssignment("L1")).isTrue();
            assertThat(late.getStatus()).isEqualTo(GradeStatus.LATE);
            assertThat(service.getGradesPendingReview()).containsExactly(late);
            assertThat(service.getOverdueAssignments()).isEmpty();
        }

        @Test
        void excuseAndReturnOfUnknownGradeFail() {
            assertThat(service.excuseAssignment("NOPE", "x")).isFalse();
            assertThat(service.returnGradedAssignment("NOPE")).isFalse();
        }

        @Test
        void removeGradeWithoutEnrollmentId() {
            Grade g = new Grade("G1", null, "S1", "CS101", "HW", GradeComponent.HOMEWORK);
            assertThat(service.addGrade(g)).isTrue();
            assertThat(service.removeGrade("G1")).isTrue();
            assertThat(service.getAllGrades()).isEmpty();
        }

        @Test
        void updateGradeWithoutIdReturnsFalse() {
            // Regression: updateGrade looked up a null ID in a ConcurrentHashMap and threw a
            // NullPointerException instead of reporting failure.
            assertThat(service.updateGrade(new Grade())).isFalse();
            assertThat(service.updateGrade(null)).isFalse();
        }

        @Test
        void assigningTheSameAssignmentTwiceKeepsBothGrades() {
            // Regression: generated grade IDs derive from millis % 10000, so assigning a second
            // grade for the same student/course/assignment name within the same millisecond (or
            // a multiple of ten seconds later) collided with the first and was rejected.
            for (int i = 0; i < 5; i++) {
                assertThat(service.assignGrade("S1", "CS101", "Weekly Quiz", GradeComponent.QUIZ, 8 + i % 2, 10, "P1", ""))
                        .as("attempt %d", i).isTrue();
            }

            assertThat(service.getStudentGrades("S1")).hasSize(5)
                    .extracting(Grade::getGradeId).doesNotHaveDuplicates();
        }
    }

    @Nested
    class Aggregates {

        @Test
        void statusComponentAndOverallDistributions() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            addGraded("S2", "CS101", GradeComponent.EXAM, 96);
            addGraded("S2", "CS102", GradeComponent.LAB, 50);
            service.addGrade(draft("S3", "CS101", GradeComponent.LAB));

            assertThat(service.getGradeStatisticsByStatus())
                    .containsEntry(GradeStatus.GRADED, 3L).containsEntry(GradeStatus.DRAFT, 1L);
            assertThat(service.getGradeStatisticsByComponent())
                    .containsEntry(GradeComponent.EXAM, 2L).containsEntry(GradeComponent.LAB, 2L);
            assertThat(service.getOverallGradeDistribution())
                    .containsOnly(Map.entry("A", 2L), Map.entry("F", 1L));
        }

        @Test
        void courseStatisticsWithEvenCountUseMeanOfMiddleValues() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 80);
            addGraded("S2", "CS101", GradeComponent.EXAM, 90);

            Map<String, Object> stats = service.getCourseGradeStatistics("CS101");
            assertThat(stats).containsEntry("median", 85.0)
                    .containsEntry("average", 85.0)
                    .containsEntry("standardDeviation", 5.0);
            assertThat(service.calculateCourseAverage("CS101")).hasValue(85.0);
        }

        @Test
        void singleGradeHasZeroStandardDeviation() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 80);
            assertThat(service.getCourseGradeStatistics("CS101")).containsEntry("standardDeviation", 0.0);
        }

        @Test
        void overallStatisticsAreCachedAndInvalidatedByChanges() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 90);
            Map<String, Object> first = service.calculateOverallStatistics();
            first.clear();
            assertThat(service.getSummaryStatistics()).containsEntry("totalGrades", 1);

            addGraded("S2", "CS101", GradeComponent.EXAM, 70);
            assertThat(service.calculateOverallStatistics())
                    .containsEntry("totalGrades", 2)
                    .containsEntry("averageGrade", 80.0)
                    .containsEntry("minGrade", 70.0)
                    .containsEntry("maxGrade", 90.0)
                    .containsEntry("pendingReviewCount", 0)
                    .containsEntry("overdueAssignmentCount", 0);
        }

        @Test
        void completionRateCountsReturnedGrades() {
            Grade g = addGraded("S1", "CS101", GradeComponent.EXAM, 90);
            service.addGrade(draft("S2", "CS101", GradeComponent.EXAM));
            service.returnGradedAssignment(g.getGradeId());

            assertThat(service.calculateOverallStatistics()).containsEntry("completionRate", 50.0);
        }
    }

    @Nested
    class Reports {

        @BeforeEach
        void seed() {
            addGraded("S1", "CS101", GradeComponent.EXAM, 95);
            addGraded("S1", "CS102", GradeComponent.EXAM, 85);
            addGraded("S2", "CS101", GradeComponent.EXAM, 40);
            service.addGrade(draft("S3", "CS101", GradeComponent.EXAM));
        }

        @Test
        void gradeReportFormatsRows() {
            ReportData report = service.generateReport(ReportType.GRADE_REPORT);

            assertThat(report.getMetadata()).containsEntry("totalGrades", 4);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Student ID", "S1")
                    .containsEntry("Points", "95.0/100.0")
                    .containsEntry("Percentage", "95.0%")
                    .containsEntry("Letter Grade", "A")
                    .containsEntry("Status", "GRADED"));
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Student ID", "S3").containsEntry("Letter Grade", "N/A"));
        }

        @Test
        void performanceReportSummarisesEachStudent() {
            ReportData report = service.generateReport(ReportType.PERFORMANCE_REPORT);

            assertThat(report.getTitle()).isEqualTo("Student Performance Report");
            assertThat(report.getRows()).hasSize(3);
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Student ID", "S1")
                    .containsEntry("Course Count", 2L)
                    .containsEntry("Average Grade", "90.0%")
                    .containsEntry("GPA", "3.50")
                    .containsEntry("Status", "Passing"));
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Student ID", "S2").containsEntry("Status", "At Risk"));
            assertThat(report.getRows()).anySatisfy(row -> assertThat(row)
                    .containsEntry("Student ID", "S3")
                    .containsEntry("Course Count", 0L)
                    .containsEntry("Average Grade", "0.0%"));
            assertThat(report.getMetadata()).containsEntry("totalGrades", 4);
        }

        @Test
        void statisticalSummaryAndUnsupportedTypes() {
            ReportData summary = service.generateReportForDateRange(ReportType.STATISTICAL_SUMMARY,
                    LocalDateTime.now().minusDays(7), LocalDateTime.now());
            assertThat(summary.getTitle()).isEqualTo("Grade Statistical Summary");
            assertThat(summary.getContent()).contains("totalGrades: 4", "validGrades: 3");

            ReportData unsupported = service.generateReport(ReportType.ENROLLMENT_REPORT);
            assertThat(unsupported.getTitle()).isEqualTo("Unsupported Report Type");
        }

        @Test
        void reportingMetadata() {
            assertThat(service.getAvailableReportTypes())
                    .containsExactly(ReportType.GRADE_REPORT, ReportType.PERFORMANCE_REPORT, ReportType.STATISTICAL_SUMMARY);
            assertThat(service.getSupportedFormats()).contains(ReportFormat.JSON);
            assertThat(service.scheduleRecurringReport(ReportType.GRADE_REPORT, "daily", List.of())).startsWith("SCHED_GRADE_");
            assertThat(service.cancelScheduledReport("x")).isTrue();
            assertThat(service.getReportHistory(ReportType.GRADE_REPORT, 3)).isEmpty();
            assertThat(service.exportReport(service.generateReport(ReportType.GRADE_REPORT), ReportFormat.PDF, "x.pdf"))
                    .isTrue();
        }
    }
}
