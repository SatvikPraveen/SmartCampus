package models;

import models.Grade.GradeComponent;
import models.Grade.GradeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GradeTest {

    // Due dates far enough from "now" that the wall clock never matters.
    private static final LocalDateTime PAST_DUE = LocalDateTime.of(2000, 1, 1, 23, 59);
    private static final LocalDateTime FUTURE_DUE = LocalDateTime.now().plusYears(50);

    private static Grade assignment(double pointsPossible, LocalDateTime due) {
        return new Grade("G1", "E1", "S1", "C1", "A1", "Homework 1",
                GradeComponent.HOMEWORK, pointsPossible, due);
    }

    private static Grade submitted(double pointsPossible) {
        Grade g = assignment(pointsPossible, FUTURE_DUE);
        assertThat(g.submitAssignment()).isTrue();
        return g;
    }

    @Test
    void newGradeIsUngradedDraft() {
        Grade g = assignment(50, FUTURE_DUE);
        assertThat(g.getStatus()).isEqualTo(GradeStatus.DRAFT);
        assertThat(g.getPointsEarned()).isEqualTo(-1);
        assertThat(g.getLetterGrade()).isNull();
        assertThat(g.countsTowardFinalGrade()).isFalse();
        assertThat(g.getWeightedPoints()).isZero();
        assertThat(g.getAttemptNumber()).isEqualTo(1);
    }

    @Nested
    @DisplayName("submission")
    class Submission {

        @Test
        void onTimeSubmission() {
            Grade g = assignment(50, FUTURE_DUE);
            assertThat(g.submitAssignment()).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.SUBMITTED);
            assertThat(g.getDateSubmitted()).isNotNull();
            assertThat(g.submitAssignment()).isFalse();
        }

        @Test
        void lateSubmissionIsFlagged() {
            Grade g = assignment(50, PAST_DUE);
            assertThat(g.isOverdue()).isTrue();
            assertThat(g.submitAssignment()).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.LATE);
            assertThat(g.isOverdue()).isFalse();
        }

        @Test
        void missingAssignmentScoresZeroAndCanStillBeSubmitted() {
            Grade g = assignment(50, PAST_DUE);
            assertThat(g.markAsMissing()).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.MISSING);
            assertThat(g.getPointsEarned()).isZero();
            assertThat(g.getPercentage()).isZero();
            assertThat(g.getLetterGrade()).isEqualTo("F");
            assertThat(g.getWeightedPossiblePoints()).isEqualTo(50.0);
            assertThat(g.submitAssignment()).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.LATE);
        }

        @Test
        void cannotMarkMissingBeforeDueDateOrWithoutOne() {
            assertThat(assignment(50, FUTURE_DUE).markAsMissing()).isFalse();
            assertThat(assignment(50, null).markAsMissing()).isFalse();
        }
    }

    @Nested
    @DisplayName("grading")
    class Grading {

        @Test
        void gradingComputesPercentageAndLetter() {
            Grade g = submitted(50);
            assertThat(g.gradeAssignment(45, "P1", "Nice work")).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.GRADED);
            assertThat(g.getPercentage()).isEqualTo(90.0);
            assertThat(g.getLetterGrade()).isEqualTo("A-");
            assertThat(g.getGradedBy()).isEqualTo("P1");
            assertThat(g.getFeedback()).isEqualTo("Nice work");
            assertThat(g.countsTowardFinalGrade()).isTrue();
        }

        @Test
        void cannotGradeBeforeSubmission() {
            Grade g = assignment(50, FUTURE_DUE);
            assertThat(g.gradeAssignment(40, "P1", null)).isFalse();
            assertThat(g.getPointsEarned()).isEqualTo(-1);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1, 50.5, 1000})
        void rejectsPointsOutsideZeroToPossible(double points) {
            Grade g = submitted(50);
            assertThat(g.gradeAssignment(points, "P1", null)).isFalse();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.SUBMITTED);
        }

        @Test
        void regradingAndReturning() {
            Grade g = submitted(100);
            g.gradeAssignment(70, "P1", null);
            assertThat(g.gradeAssignment(85, "P1", "regrade")).isTrue();
            assertThat(g.getLetterGrade()).isEqualTo("B");
            assertThat(g.returnToStudent()).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.RETURNED);
            assertThat(g.countsTowardFinalGrade()).isTrue();
            assertThat(g.returnToStudent()).isFalse();
            assertThat(g.gradeAssignment(90, "P1", null)).isFalse();
        }

        @ParameterizedTest(name = "{0}% -> {1}")
        @CsvSource({
                "100, A+", "97, A+", "96.99, A", "93, A", "92.99, A-", "90, A-", "87, B+", "83, B",
                "80, B-", "77, C+", "73, C", "70, C-", "67, D+", "63, D", "60, D-", "59.99, F", "0, F"
        })
        void letterGradeBoundaries(double points, String letter) {
            Grade g = assignment(100, FUTURE_DUE);
            g.setPointsEarned(points);
            assertThat(g.getPercentage()).isEqualTo(points);
            assertThat(g.getLetterGrade()).isEqualTo(letter);
        }

        @Test
        void fullConstructorComputesImmediately() {
            Grade g = new Grade("G2", "E1", "S1", "C1", "A2", "Quiz", GradeComponent.QUIZ,
                    8, 10, FUTURE_DUE, "P1", "Fall", 2024);
            assertThat(g.getPercentage()).isEqualTo(80.0);
            assertThat(g.getLetterGrade()).isEqualTo("B-");
        }

        @Test
        void changingPointsPossibleRecomputesPercentage() {
            Grade g = assignment(100, FUTURE_DUE);
            g.setPointsEarned(40);
            g.setPointsPossible(50);
            assertThat(g.getPercentage()).isEqualTo(80.0);
            g.setPointsPossible(0);
            assertThat(g.getPointsPossible()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("final-grade contribution")
    class Contribution {

        @Test
        void weightScalesEarnedAndPossiblePoints() {
            Grade g = submitted(10);
            g.setWeight(2.0);
            g.gradeAssignment(7, "P1", null);
            assertThat(g.getWeightedPoints()).isEqualTo(14.0);
            assertThat(g.getWeightedPossiblePoints()).isEqualTo(20.0);
            g.setWeight(-1);
            assertThat(g.getWeight()).isEqualTo(2.0);
        }

        @Test
        void droppedGradesContributeNothingUntilIncludedAgain() {
            Grade g = submitted(10);
            g.gradeAssignment(7, "P1", null);
            g.dropGrade();
            assertThat(g.countsTowardFinalGrade()).isFalse();
            assertThat(g.getWeightedPoints()).isZero();
            assertThat(g.getWeightedPossiblePoints()).isZero();
            g.includeGrade();
            assertThat(g.getWeightedPoints()).isEqualTo(7.0);
        }

        @Test
        void excusedGradesContributeNothing() {
            Grade g = submitted(10);
            g.gradeAssignment(7, "P1", null);
            assertThat(g.excuseAssignment("illness")).isTrue();
            assertThat(g.getStatus()).isEqualTo(GradeStatus.EXCUSED);
            assertThat(g.countsTowardFinalGrade()).isFalse();
            assertThat(g.getWeightedPossiblePoints()).isZero();
            assertThat(g.getNotes()).isEqualTo("Excused: illness");
        }

        @Test
        @DisplayName("extra credit adds earned points without raising the possible total (regression)")
        void extraCreditDoesNotInflateTheDenominator() {
            Grade g = Grade.createExtraCreditGrade("E1", "S1", "C1", "Bonus", 10);
            assertThat(g.isExtraCredit()).isTrue();
            assertThat(g.getComponent()).isEqualTo(GradeComponent.OTHER);
            g.submitAssignment();
            g.gradeAssignment(5, "P1", null);
            assertThat(g.getWeightedPoints()).isEqualTo(5.0);
            assertThat(g.getWeightedPossiblePoints()).isZero();
        }
    }

    @Test
    void factoryBuildsIdFromAssignmentName() {
        Grade g = Grade.createGrade("E1", "S1", "C1", "Midterm  Exam", GradeComponent.MIDTERM, 100);
        assertThat(g.getGradeId()).startsWith("GRD_S1_C1_Midterm_Exam_");
        assertThat(g.getPointsPossible()).isEqualTo(100);
    }

    @Test
    void attemptNumberMustBePositive() {
        Grade g = assignment(10, FUTURE_DUE);
        g.setAttemptNumber(0);
        assertThat(g.getAttemptNumber()).isEqualTo(1);
        g.setAttemptNumber(2);
        assertThat(g.getAttemptNumber()).isEqualTo(2);
    }

    @Test
    void equalityIsByGradeId() {
        Grade a = assignment(10, FUTURE_DUE);
        Grade b = assignment(99, null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
