package services;

import interfaces.Reportable.ReportType;
import interfaces.Searchable.SearchCriteria;
import interfaces.Searchable.SearchCriterion;
import interfaces.Searchable.SortOrder;
import models.Grade;
import models.Grade.GradeComponent;
import models.Grade.GradeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    }
}
