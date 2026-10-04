package models;

import models.Student.AcademicYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class StudentTest {

    private static Student freshman() {
        return new Student(" U1 ", " Ada ", " Lovelace ", " Ada@Example.EDU ", " 555-0100 ",
                "S1", "CS", AcademicYear.FRESHMAN);
    }

    @Nested
    @DisplayName("User validation (inherited)")
    class UserValidation {

        @Test
        void constructorTrimsAndNormalises() {
            Student s = freshman();
            assertThat(s.getUserId()).isEqualTo("U1");
            assertThat(s.getFullName()).isEqualTo("Ada Lovelace");
            assertThat(s.getEmail()).isEqualTo("ada@example.edu");
            assertThat(s.getPhoneNumber()).isEqualTo("555-0100");
            assertThat(s.isActive()).isTrue();
            assertThat(s.isValidUser()).isTrue();
            assertThat(s.getRole()).isEqualTo("STUDENT");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void rejectsBlankNamesAndIds(String blank) {
            Student s = freshman();
            assertThatThrownBy(() -> s.setFirstName(blank)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> s.setLastName(blank)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> s.setUserId(blank)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> s.setStudentId(blank)).isInstanceOf(IllegalArgumentException.class);
            assertThat(s.getFullName()).isEqualTo("Ada Lovelace");
        }

        @Test
        void rejectsEmailWithoutAt() {
            Student s = freshman();
            assertThatThrownBy(() -> s.setEmail("not-an-email")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> s.setEmail(null)).isInstanceOf(IllegalArgumentException.class);
            assertThat(s.getEmail()).isEqualTo("ada@example.edu");
        }

        @Test
        void nullPhoneIsIgnoredAndInactiveUsersAreInvalid() {
            Student s = freshman();
            s.setPhoneNumber(null);
            assertThat(s.getPhoneNumber()).isEqualTo("555-0100");
            s.setActive(false);
            assertThat(s.isValidUser()).isFalse();
        }

        @Test
        void equalityUsesUserIdStudentIdAndConcreteType() {
            Student a = freshman();
            Student b = freshman();
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            b.setStudentId("S2");
            assertThat(a).isNotEqualTo(b);
            Professor p = new Professor("U1", "Ada", "Lovelace", "ada@example.edu", null,
                    "S1", "D1", Professor.AcademicRank.ASSISTANT, "Math");
            assertThat(a).isNotEqualTo(p);
        }
    }

    @Nested
    @DisplayName("GPA and probation")
    class Gpa {

        @ParameterizedTest
        @ValueSource(doubles = {-0.01, 4.01, 10})
        void setGpaRejectsOutOfRange(double gpa) {
            Student s = freshman();
            assertThatThrownBy(() -> s.setGpa(gpa)).isInstanceOf(IllegalArgumentException.class);
            assertThat(s.getGpa()).isZero();
        }

        @ParameterizedTest(name = "gpa {0} -> probation {1}")
        @CsvSource({"0.0, true", "1.99, true", "2.0, false", "4.0, false"})
        void probationThresholdIsTwoPointZero(double gpa, boolean probation) {
            Student s = freshman();
            s.setGpa(gpa);
            assertThat(s.isOnProbation()).isEqualTo(probation);
        }

        @Test
        void completingCoursesMaintainsARunningAverage() {
            Student s = freshman();
            s.enrollInCourse("C1");
            s.enrollInCourse("C2");
            s.enrollInCourse("C3");

            s.completeCourse("C1", 4.0);
            assertThat(s.getGpa()).isEqualTo(4.0);
            s.completeCourse("C2", 2.0);
            assertThat(s.getGpa()).isCloseTo(3.0, within(1e-9));
            s.completeCourse("C3", 0.0);
            assertThat(s.getGpa()).isCloseTo(2.0, within(1e-9));
            assertThat(s.isOnProbation()).isFalse();

            assertThat(s.getEnrolledCourseIds()).isEmpty();
            assertThat(s.getCompletedCourseIds()).containsExactly("C1", "C2", "C3");

            s.enrollInCourse("C4");
            s.completeCourse("C4", 0.0);
            assertThat(s.getGpa()).isCloseTo(1.5, within(1e-9));
            assertThat(s.isOnProbation()).isTrue();
        }

        @Test
        void completingACourseThatIsNotEnrolledIsANoOp() {
            Student s = freshman();
            s.completeCourse("C9", 4.0);
            assertThat(s.getCompletedCourseIds()).isEmpty();
            assertThat(s.getGpa()).isZero();
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1.0, 4.5, 85.0})
        @DisplayName("grade points outside 0-4 are rejected without side effects (regression)")
        void completingWithOutOfRangeGradeIsRejected(double grade) {
            Student s = freshman();
            s.enrollInCourse("C1");
            assertThatThrownBy(() -> s.completeCourse("C1", grade)).isInstanceOf(IllegalArgumentException.class);
            assertThat(s.getEnrolledCourseIds()).containsExactly("C1");
            assertThat(s.getCompletedCourseIds()).isEmpty();
            assertThat(s.getGpa()).isBetween(0.0, 4.0);
        }
    }

    @Nested
    @DisplayName("course enrollment rules")
    class Enrollment {

        @Test
        void enrollIgnoresBlankAndDuplicateIds() {
            Student s = freshman();
            s.enrollInCourse("C1");
            s.enrollInCourse("C1");
            s.enrollInCourse(" ");
            s.enrollInCourse(null);
            assertThat(s.getEnrolledCourseIds()).containsExactly("C1");
            s.dropCourse("C1");
            assertThat(s.getEnrolledCourseIds()).isEmpty();
        }

        @Test
        void canEnrollChecksStatusHistoryAndLoad() {
            Student s = freshman();
            assertThat(s.canEnrollInCourse("C1")).isTrue();

            s.enrollInCourse("C1");
            assertThat(s.canEnrollInCourse("C1")).isFalse();
            s.completeCourse("C1", 3.0);
            assertThat(s.canEnrollInCourse("C1")).isFalse();

            for (int i = 0; i < 6; i++) {
                s.enrollInCourse("X" + i);
            }
            assertThat(s.canEnrollInCourse("C7")).isFalse();
            s.dropCourse("X0");
            assertThat(s.canEnrollInCourse("C7")).isTrue();
        }

        @Test
        void probationOrInactiveBlocksEnrollment() {
            Student s = freshman();
            s.setGpa(1.5);
            assertThat(s.canEnrollInCourse("C1")).isFalse();
            s.setGpa(3.0);
            s.setActive(false);
            assertThat(s.canEnrollInCourse("C1")).isFalse();
        }

        @Test
        void gettersReturnDefensiveCopies() {
            Student s = freshman();
            s.getEnrolledCourseIds().add("intruder");
            s.getCompletedCourseIds().add("intruder");
            assertThat(s.getEnrolledCourseIds()).isEmpty();
            assertThat(s.getCompletedCourseIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("academic year promotion")
    class Promotion {

        @ParameterizedTest(name = "{0} credits: {1} -> {2}")
        @CsvSource({
                "0, FRESHMAN, FRESHMAN",
                "29, FRESHMAN, FRESHMAN",
                "30, FRESHMAN, SOPHOMORE",
                "59, SOPHOMORE, SOPHOMORE",
                "60, SOPHOMORE, JUNIOR",
                "90, JUNIOR, SENIOR",
                "60, FRESHMAN, JUNIOR",
                "95, FRESHMAN, SENIOR",
                "200, SENIOR, SENIOR",
                "200, GRADUATE, GRADUATE"
        })
        void promotesAsFarAsCreditsAllow(int credits, AcademicYear start, AcademicYear expected) {
            Student s = freshman();
            s.setAcademicYear(start);
            s.setTotalCredits(credits);
            assertThat(s.getAcademicYear()).isEqualTo(expected);
        }

        @Test
        void negativeCreditsAreRejected() {
            Student s = freshman();
            assertThatThrownBy(() -> s.setTotalCredits(-1)).isInstanceOf(IllegalArgumentException.class);
            assertThat(s.getTotalCredits()).isZero();
        }

        @Test
        void yearsAreNeverDemoted() {
            Student s = freshman();
            s.setTotalCredits(95);
            s.setTotalCredits(10);
            assertThat(s.getAcademicYear()).isEqualTo(AcademicYear.SENIOR);
        }
    }
}
