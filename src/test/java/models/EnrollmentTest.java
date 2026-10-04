package models;

import models.Enrollment.EnrollmentStatus;
import models.Enrollment.EnrollmentType;
import models.Enrollment.Grade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentTest {

    private static Enrollment regular() {
        return new Enrollment("E1", "S1", "C1", "Fall", 2024,
                EnrollmentStatus.ENROLLED, EnrollmentType.REGULAR, "01", 3);
    }

    @Test
    void defaults() {
        Enrollment e = new Enrollment();
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(e.getEnrollmentType()).isEqualTo(EnrollmentType.REGULAR);
        assertThat(e.getNumericGrade()).isEqualTo(-1);
        assertThat(e.isActive()).isTrue();
        assertThat(e.isCurrentlyActive()).isTrue();
        assertThat(e.getGrade()).isNull();
        assertThat(e.toString()).contains("Not Graded");
    }

    @Nested
    @DisplayName("factories")
    class Factories {

        @Test
        void createEnrollmentBuildsADescriptiveId() {
            Enrollment e = Enrollment.createEnrollment("S1", "C1", "Fall", 2024);
            assertThat(e.getEnrollmentId()).startsWith("ENR_S1_C1_Fall_2024_");
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        }

        @Test
        void waitlistedEnrollmentCannotBeGradedUntilPromoted() {
            Enrollment e = Enrollment.createWaitlistedEnrollment("S1", "C1", "Fall", 2024);
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
            assertThat(e.isCurrentlyActive()).isTrue();
            assertThat(e.assignGrade(Grade.A)).isFalse();

            assertThat(e.enrollFromWaitlist()).isTrue();
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(e.enrollFromWaitlist()).isFalse();
            assertThat(e.assignGrade(Grade.A)).isTrue();
        }

        @Test
        void auditEnrollmentIsNeverGraded() {
            Enrollment e = Enrollment.createAuditEnrollment("S1", "C1", "Fall", 2024);
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.AUDIT);
            assertThat(e.getEnrollmentType()).isEqualTo(EnrollmentType.AUDIT);
            assertThat(e.canBeGraded()).isFalse();
            e.setStatus(EnrollmentStatus.ENROLLED);
            assertThat(e.canBeGraded()).isFalse();
            assertThat(e.assignNumericGrade(90)).isFalse();
        }
    }

    @Nested
    @DisplayName("grading")
    class Grading {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "A_PLUS, COMPLETED", "B, COMPLETED", "D_MINUS, COMPLETED", "PASS, COMPLETED",
                "F, FAILED", "WITHDRAW, WITHDRAWN", "INCOMPLETE, ENROLLED", "NOT_GRADED, ENROLLED"
        })
        void assignGradeMovesStatus(Grade grade, EnrollmentStatus expected) {
            Enrollment e = regular();
            assertThat(e.assignGrade(grade)).isTrue();
            assertThat(e.getGrade()).isEqualTo(grade);
            assertThat(e.getStatus()).isEqualTo(expected);
        }

        @Test
        void finalGradesCannotBeOverwritten() {
            Enrollment e = regular();
            e.assignGrade(Grade.B);
            assertThat(e.assignGrade(Grade.A)).isFalse();
            assertThat(e.getGrade()).isEqualTo(Grade.B);
        }

        @Test
        void incompleteCanLaterBeResolved() {
            Enrollment e = regular();
            e.assignGrade(Grade.INCOMPLETE);
            assertThat(e.assignGrade(Grade.C)).isTrue();
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        }

        @ParameterizedTest(name = "{0} -> {1} / {2}")
        @CsvSource({
                "100, A_PLUS, COMPLETED", "97, A_PLUS, COMPLETED", "96.99, A, COMPLETED", "93, A, COMPLETED",
                "90, A_MINUS, COMPLETED", "87, B_PLUS, COMPLETED", "83, B, COMPLETED", "80, B_MINUS, COMPLETED",
                "77, C_PLUS, COMPLETED", "73, C, COMPLETED", "70, C_MINUS, COMPLETED", "67, D_PLUS, COMPLETED",
                "63, D, COMPLETED", "60, D_MINUS, COMPLETED", "59.99, F, FAILED", "0, F, FAILED"
        })
        @DisplayName("numeric grades map to letters and finalise status like letter grades (regression)")
        void assignNumericGrade(double numeric, Grade letter, EnrollmentStatus status) {
            Enrollment e = regular();
            assertThat(e.assignNumericGrade(numeric)).isTrue();
            assertThat(e.getNumericGrade()).isEqualTo(numeric);
            assertThat(e.getGrade()).isEqualTo(letter);
            assertThat(e.getStatus()).isEqualTo(status);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.01, 100.01, -1})
        void assignNumericGradeRejectsOutOfRange(double numeric) {
            Enrollment e = regular();
            assertThat(e.assignNumericGrade(numeric)).isFalse();
            assertThat(e.getNumericGrade()).isEqualTo(-1);
            assertThat(e.getGrade()).isNull();
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        }

        @Test
        void setNumericGradeAcceptsMinusOneToHundred() {
            Enrollment e = regular();
            e.setNumericGrade(101);
            assertThat(e.getNumericGrade()).isEqualTo(-1);
            e.setNumericGrade(100);
            assertThat(e.getNumericGrade()).isEqualTo(100);
            e.setNumericGrade(-1);
            assertThat(e.getNumericGrade()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("GPA contribution")
    class GpaContribution {

        @Test
        void gpaPointsAreGradePointsTimesCredits() {
            Enrollment e = regular();
            e.assignGrade(Grade.B_PLUS);
            assertThat(e.getGpaPoints()).isCloseTo(9.9, org.assertj.core.api.Assertions.within(1e-9));
            assertThat(e.countsTowardGpa()).isTrue();
        }

        @Test
        void ungradedOrNonRegularEnrollmentsContributeNothing() {
            assertThat(regular().getGpaPoints()).isZero();
            assertThat(regular().countsTowardGpa()).isFalse();

            Enrollment cnc = regular();
            cnc.setEnrollmentType(EnrollmentType.CREDIT_NO_CREDIT);
            cnc.assignGrade(Grade.A);
            assertThat(cnc.getGpaPoints()).isZero();
            assertThat(cnc.countsTowardGpa()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = Grade.class, names = {"PASS", "NO_PASS", "WITHDRAW", "INCOMPLETE", "NOT_GRADED"})
        void nonGpaGradesDoNotCount(Grade grade) {
            Enrollment e = regular();
            e.setGrade(grade);
            assertThat(e.countsTowardGpa()).isFalse();
        }

        @Test
        void failingGradeCountsAsZero() {
            Enrollment e = regular();
            e.assignGrade(Grade.F);
            assertThat(e.countsTowardGpa()).isTrue();
            assertThat(e.getGpaPoints()).isZero();
        }
    }

    @Nested
    @DisplayName("drop and withdraw")
    class DropWithdraw {

        @Test
        void dropDeactivatesAndRecordsReason() {
            Enrollment e = regular();
            assertThat(e.dropEnrollment("schedule conflict")).isTrue();
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
            assertThat(e.isActive()).isFalse();
            assertThat(e.isCurrentlyActive()).isFalse();
            assertThat(e.getNotes()).isEqualTo("Dropped: schedule conflict");
            assertThat(e.dropEnrollment("again")).isFalse();
        }

        @Test
        void dropAppendsToExistingNotesAndIgnoresBlankReason() {
            Enrollment e = regular();
            e.setNotes("Advisor approved");
            e.dropEnrollment("moving");
            assertThat(e.getNotes()).isEqualTo("Advisor approved; Dropped: moving");

            Enrollment quiet = regular();
            quiet.dropEnrollment("  ");
            assertThat(quiet.getNotes()).isNull();
        }

        @Test
        void waitlistedEnrollmentCanBeDroppedButNotWithdrawn() {
            Enrollment e = Enrollment.createWaitlistedEnrollment("S1", "C1", "Fall", 2024);
            assertThat(e.withdrawFromCourse("x")).isFalse();
            assertThat(e.dropEnrollment(null)).isTrue();
        }

        @Test
        void withdrawAssignsWGradeOnlyFromEnrolled() {
            Enrollment e = regular();
            assertThat(e.withdrawFromCourse("medical")).isTrue();
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
            assertThat(e.getGrade()).isEqualTo(Grade.WITHDRAW);
            assertThat(e.getNotes()).isEqualTo("Withdrawn: medical");
            assertThat(e.withdrawFromCourse("again")).isFalse();
            assertThat(e.dropEnrollment("late")).isFalse();
        }

        @Test
        void completedEnrollmentCannotBeDropped() {
            Enrollment e = regular();
            e.assignGrade(Grade.A);
            assertThat(e.dropEnrollment("too late")).isFalse();
            assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        }
    }

    @Test
    void attendanceAndCreditBounds() {
        Enrollment e = regular();
        assertThat(e.updateAttendance(101)).isFalse();
        assertThat(e.updateAttendance(-1)).isFalse();
        assertThat(e.updateAttendance(100)).isTrue();
        assertThat(e.getAttendancePercentage()).isEqualTo(100);
        e.setAttendancePercentage(150);
        assertThat(e.getAttendancePercentage()).isEqualTo(100);
        e.setCreditHours(-2);
        assertThat(e.getCreditHours()).isEqualTo(3);
    }

    @Test
    void equalityIsByEnrollmentId() {
        Enrollment a = regular();
        Enrollment b = new Enrollment("E1", "other", "other", "Spring", 2025);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(new Enrollment("E2", "S1", "C1", "Fall", 2024));
    }
}
