package models;

import models.Course.CourseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTest {

    private static Course draftCourse() {
        return new Course("C1", " cs101 ", " Intro to CS ", "Basics", 3, "D1");
    }

    private static Course openCourse(int capacity) {
        Course course = draftCourse();
        course.setProfessorId("P1");
        course.setMaxEnrollment(capacity);
        course.openEnrollment();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
        return course;
    }

    private static void assertCapacityInvariants(Course course) {
        List<String> enrolled = course.getEnrolledStudentIds();
        List<String> waitlist = course.getWaitlistedStudentIds();
        assertThat(enrolled).doesNotHaveDuplicates();
        assertThat(waitlist).doesNotHaveDuplicates();
        assertThat(enrolled).noneMatch(waitlist::contains);
        assertThat(course.getAvailableSeats()).isGreaterThanOrEqualTo(0);
    }

    @Nested
    @DisplayName("construction and validation")
    class Validation {

        @Test
        void constructorNormalisesAndAppliesDefaults() {
            Course course = draftCourse();
            assertThat(course.getCourseCode()).isEqualTo("CS101");
            assertThat(course.getCourseName()).isEqualTo("Intro to CS");
            assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
            assertThat(course.getMaxEnrollment()).isEqualTo(Course.MAX_STUDENTS);
            assertThat(course.getPassingGrade()).isEqualTo(60.0);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 0, 7})
        void rejectsCreditsOutsideOneToSix(int credits) {
            Course course = draftCourse();
            assertThatThrownBy(() -> course.setCredits(credits)).isInstanceOf(IllegalArgumentException.class);
            assertThat(course.getCredits()).isEqualTo(3);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 6})
        void acceptsCreditBoundaries(int credits) {
            Course course = draftCourse();
            course.setCredits(credits);
            assertThat(course.getCredits()).isEqualTo(credits);
        }

        @Test
        void rejectsInvalidScalarFields() {
            Course course = draftCourse();
            assertThatThrownBy(() -> course.setMaxEnrollment(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> course.setPassingGrade(100.1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> course.setPassingGrade(-0.1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> course.setYear(2019)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> course.setYear(2031)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> course.setCourseId("  ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> course.setCourseName(null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void validForEnrollmentRequiresAProfessor() {
            Course course = draftCourse();
            assertThat(course.isValidForEnrollment()).isFalse();
            course.openEnrollment();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
            course.setProfessorId("P1");
            assertThat(course.isValidForEnrollment()).isTrue();
        }

        @Test
        void equalityIsByCourseId() {
            Course a = draftCourse();
            Course b = new Course("C1", "MATH200", "Other", null, 4, "D2");
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            assertThat(a).isNotEqualTo(new Course("C2", "CS101", "Intro", null, 3, "D1"));
        }
    }

    @Nested
    @DisplayName("enrollment capacity")
    class Capacity {

        @Test
        void enrollsUntilFullThenCloses() {
            Course course = openCourse(3);
            assertThat(course.enrollStudent("S1")).isTrue();
            assertThat(course.enrollStudent("S2")).isTrue();
            assertThat(course.isEnrollmentOpen()).isTrue();
            assertThat(course.enrollStudent("S3")).isTrue();

            assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
            assertThat(course.getAvailableSeats()).isZero();
            assertThat(course.getEnrollmentPercentage()).isEqualTo(100.0);
            assertThat(course.isEnrollmentOpen()).isFalse();
            assertThat(course.enrollStudent("S4")).isFalse();
            assertThat(course.getEnrolledStudentIds()).containsExactly("S1", "S2", "S3");
        }

        @Test
        void enrolmentNeverExceedsCapacity() {
            Course course = openCourse(3);
            for (int i = 0; i < 10; i++) {
                course.enrollStudent("S" + i);
                assertThat(course.getEnrolledStudentIds().size()).isLessThanOrEqualTo(3);
            }
            assertCapacityInvariants(course);
        }

        @Test
        void rejectsInvalidDuplicateAndNotOpen() {
            Course course = openCourse(5);
            assertThat(course.enrollStudent(null)).isFalse();
            assertThat(course.enrollStudent("   ")).isFalse();
            assertThat(course.enrollStudent("S1")).isTrue();
            assertThat(course.enrollStudent("S1")).isFalse();
            assertThat(course.getEnrolledStudentIds()).containsExactly("S1");

            Course draft = draftCourse();
            assertThat(draft.enrollStudent("S1")).isFalse();
        }

        @Test
        void fullButOpenCourseWaitlistsInsteadOfEnrolling() {
            Course course = openCourse(1);
            course.enrollStudent("S1");
            course.setStatus(CourseStatus.OPEN);

            assertThat(course.enrollStudent("S2")).isFalse();
            assertThat(course.enrollStudent("S2")).isFalse();
            assertThat(course.getWaitlistedStudentIds()).containsExactly("S2");
            assertThat(course.getEnrolledStudentIds()).containsExactly("S1");
        }

        @Test
        void percentageAndSeats() {
            Course course = openCourse(4);
            course.enrollStudent("S1");
            assertThat(course.getEnrollmentPercentage()).isEqualTo(25.0);
            assertThat(course.getAvailableSeats()).isEqualTo(3);
        }

        @Test
        void availableSeatsNeverNegativeWhenCapacityIsLowered() {
            Course course = openCourse(3);
            course.enrollStudent("S1");
            course.enrollStudent("S2");
            course.setMaxEnrollment(1);
            assertThat(course.getAvailableSeats()).isZero();
            assertThat(course.isEnrollmentOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("waitlist")
    class Waitlist {

        @Test
        void addToWaitlistRejectsInvalidDuplicateAndEnrolledStudents() {
            Course course = openCourse(2);
            course.enrollStudent("S1");
            assertThat(course.addToWaitlist("S1")).isFalse();
            assertThat(course.addToWaitlist("")).isFalse();
            assertThat(course.addToWaitlist(null)).isFalse();
            assertThat(course.addToWaitlist("W1")).isTrue();
            assertThat(course.addToWaitlist("W1")).isFalse();
            assertThat(course.hasWaitlist()).isTrue();
            assertCapacityInvariants(course);
        }

        @Test
        void droppingFromAFullCoursePromotesTheWaitlistInFifoOrder() {
            Course course = openCourse(2);
            course.enrollStudent("S1");
            course.enrollStudent("S2");
            course.addToWaitlist("W1");
            course.addToWaitlist("W2");

            assertThat(course.dropStudent("S1")).isTrue();

            assertThat(course.getEnrolledStudentIds()).containsExactlyInAnyOrder("S2", "W1");
            assertThat(course.getWaitlistedStudentIds()).containsExactly("W2");
            assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
            assertCapacityInvariants(course);
        }

        @Test
        void droppingFromAFullCourseWithoutWaitlistReopensIt() {
            Course course = openCourse(2);
            course.enrollStudent("S1");
            course.enrollStudent("S2");
            assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);

            assertThat(course.dropStudent("S2")).isTrue();

            assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);
            assertThat(course.enrollStudent("S3")).isTrue();
        }

        @Test
        void droppingAStudentWhoIsNotEnrolledChangesNothing() {
            Course course = openCourse(2);
            course.enrollStudent("S1");
            course.addToWaitlist("W1");

            assertThat(course.dropStudent("W1")).isFalse();
            assertThat(course.dropStudent("ghost")).isFalse();
            assertThat(course.getEnrolledStudentIds()).containsExactly("S1");
            assertThat(course.getWaitlistedStudentIds()).containsExactly("W1");
        }

        @Test
        @DisplayName("waitlist promotion respects a capacity that was lowered (regression)")
        void promotionNeverOverfillsALoweredCapacity() {
            Course course = openCourse(3);
            course.enrollStudent("S1");
            course.enrollStudent("S2");
            course.enrollStudent("S3");
            course.addToWaitlist("W1");
            course.setMaxEnrollment(2);

            course.dropStudent("S1");

            assertThat(course.getEnrolledStudentIds()).containsExactlyInAnyOrder("S2", "S3");
            assertThat(course.getWaitlistedStudentIds()).containsExactly("W1");

            course.dropStudent("S2");
            assertThat(course.getEnrolledStudentIds()).containsExactlyInAnyOrder("S3", "W1");
            assertThat(course.getWaitlistedStudentIds()).isEmpty();
            assertCapacityInvariants(course);
        }

        @Test
        void enrollingAWaitlistedStudentRemovesThemFromTheWaitlist() {
            Course course = openCourse(3);
            course.addToWaitlist("S1");
            assertThat(course.enrollStudent("S1")).isTrue();
            assertThat(course.getWaitlistedStudentIds()).isEmpty();
            assertCapacityInvariants(course);
        }
    }

    @Nested
    @DisplayName("status lifecycle")
    class Lifecycle {

        @Test
        void startRequiresMinimumEnrollment() {
            Course course = openCourse(10);
            for (int i = 1; i < Course.MIN_STUDENTS; i++) {
                course.enrollStudent("S" + i);
            }
            course.startCourse();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.OPEN);

            course.enrollStudent("S" + Course.MIN_STUDENTS);
            course.startCourse();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.IN_PROGRESS);

            course.completeCourse();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.COMPLETED);
        }

        @Test
        void completedCoursesCannotBeCancelledOrRestarted() {
            Course course = openCourse(10);
            for (int i = 0; i < 5; i++) {
                course.enrollStudent("S" + i);
            }
            course.startCourse();
            course.completeCourse();
            course.cancelCourse();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.COMPLETED);
            course.openEnrollment();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.COMPLETED);
        }

        @Test
        void completeOnlyFromInProgressAndCloseOnlyFromOpen() {
            Course course = draftCourse();
            course.completeCourse();
            course.closeEnrollment();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
            course.cancelCourse();
            assertThat(course.getStatus()).isEqualTo(CourseStatus.CANCELLED);

            Course open = openCourse(5);
            open.closeEnrollment();
            assertThat(open.getStatus()).isEqualTo(CourseStatus.CLOSED);
            assertThat(open.enrollStudent("S1")).isFalse();
        }
    }

    @Nested
    @DisplayName("curriculum details")
    class Details {

        @Test
        void gradingComponentsIgnoreNonPositiveWeightsAndSum() {
            Course course = draftCourse();
            course.addGradingComponent("Midterm", 30);
            course.addGradingComponent("Final", 40);
            course.addGradingComponent("Bogus", 0);
            course.addGradingComponent("Negative", -5);
            course.addGradingComponent(" ", 10);
            course.addGradingComponent("Final", 50);
            assertThat(course.getGradingComponents()).containsOnlyKeys("Midterm", "Final");
            assertThat(course.getTotalGradingWeight()).isEqualTo(80.0);
            course.removeGradingComponent("Midterm");
            assertThat(course.getTotalGradingWeight()).isEqualTo(50.0);
        }

        @Test
        void prerequisitesAreDeduplicated() {
            Course course = draftCourse();
            assertThat(course.hasPrerequisites()).isFalse();
            course.addPrerequisite("C0");
            course.addPrerequisite("C0");
            course.addPrerequisite("");
            assertThat(course.getPrerequisiteCourseIds()).containsExactly("C0");
            course.removePrerequisite("C0");
            assertThat(course.hasPrerequisites()).isFalse();
        }

        @Test
        void scheduleAndResources() {
            Course course = draftCourse();
            course.setSchedule(List.of("Monday", "Wednesday"), LocalTime.of(9, 0), LocalTime.of(10, 15));
            course.addMeetingDay("Monday");
            course.addMeetingDay("Friday");
            assertThat(course.getMeetingDays()).containsExactly("Monday", "Wednesday", "Friday");
            course.addTextbook("SICP");
            course.addTextbook("SICP");
            course.addOnlineResource("https://example.org");
            assertThat(course.getTextbooks()).containsExactly("SICP");
            assertThat(course.getOnlineResources()).hasSize(1);
        }

        @Test
        void gettersReturnDefensiveCopies() {
            Course course = openCourse(5);
            course.enrollStudent("S1");
            course.getEnrolledStudentIds().add("intruder");
            course.getWaitlistedStudentIds().add("intruder");
            course.getGradingComponents().put("x", 100.0);
            assertThat(course.getEnrolledStudentIds()).containsExactly("S1");
            assertThat(course.getWaitlistedStudentIds()).isEmpty();
            assertThat(course.getGradingComponents()).isEmpty();
        }
    }
}
