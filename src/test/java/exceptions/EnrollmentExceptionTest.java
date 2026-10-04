package exceptions;

import enums.EnrollmentStatus;
import exceptions.EnrollmentException.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentExceptionTest {

    private static final Set<ErrorCode> RECOVERABLE = EnumSet.of(
            ErrorCode.SYSTEM_ERROR, ErrorCode.COURSE_FULL, ErrorCode.WAITLIST_FULL);
    private static final Set<ErrorCode> MANUAL = EnumSet.of(
            ErrorCode.INSTRUCTOR_APPROVAL_REQUIRED, ErrorCode.DEPARTMENT_PERMISSION_REQUIRED,
            ErrorCode.HOLD_ON_ACCOUNT, ErrorCode.PAYMENT_REQUIRED, ErrorCode.ACADEMIC_PROBATION_RESTRICTION);

    private static EnrollmentException of(ErrorCode code) {
        return new EnrollmentException(code, null);
    }

    @Nested
    class Constructors {

        @Test
        void codesAreUnique() {
            assertThat(Stream.of(ErrorCode.values()).map(ErrorCode::getCode)).doesNotHaveDuplicates()
                    .allMatch(c -> c.startsWith("ENROLL_"));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void nullMessageFallsBackToDescription(ErrorCode code) {
            assertThat(of(code).getMessage()).isEqualTo(code.getDescription());
        }

        @Test
        void simpleConstructorsLeaveContextEmpty() {
            IllegalStateException cause = new IllegalStateException();
            EnrollmentException e = new EnrollmentException(ErrorCode.COURSE_FULL, "full", cause);

            assertThat(e).hasMessage("full").hasCause(cause);
            assertThat(e.getStudentId()).isNull();
            assertThat(e.getCourseId()).isNull();
            assertThat(e.getSemester()).isNull();
            assertThat(e.getCurrentStatus()).isNull();
            assertThat(e.getAttemptedStatus()).isNull();
            assertThat(e.getAdditionalData()).isEmpty();
            assertThat(e.getTimestamp()).isNotNull();
            assertThat(new EnrollmentException(ErrorCode.COURSE_FULL, null, cause).getMessage())
                    .isEqualTo(ErrorCode.COURSE_FULL.getDescription());
        }

        @Test
        void contextConstructorFormatsAllParts() {
            EnrollmentException e = new EnrollmentException(ErrorCode.INVALID_SEMESTER, "S1", "CS101", "FALL",
                    "bad term");

            assertThat(e.getMessage()).isEqualTo("bad term [Student: S1, Course: CS101, Semester: FALL]");
            assertThat(e.getSemester()).isEqualTo("FALL");
        }

        @ParameterizedTest(name = "{0}/{1}/{2} -> {3}")
        @CsvSource(nullValues = "null", delimiter = '|', value = {
                "null | C1 | null | Invalid semester for enrollment [Course: C1]",
                "null | null | F | Invalid semester for enrollment [Semester: F]",
                "S1 | null | F | Invalid semester for enrollment [Student: S1, Semester: F]",
                "null | null | null | Invalid semester for enrollment"
        })
        void contextMessageOnlyIncludesPresentParts(String student, String course, String semester, String expected) {
            assertThat(new EnrollmentException(ErrorCode.INVALID_SEMESTER, student, course, semester, " ")
                    .getMessage()).isEqualTo(expected);
        }

        @Test
        void fullConstructorKeepsStatuses() {
            EnrollmentException e = new EnrollmentException(ErrorCode.INVALID_ENROLLMENT_STATUS, "S1", "C1", null,
                    EnrollmentStatus.ENROLLED, EnrollmentStatus.APPLIED, "nope", null);

            assertThat(e.getCurrentStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(e.getAttemptedStatus()).isEqualTo(EnrollmentStatus.APPLIED);
            assertThat(e.getMessage()).isEqualTo("nope [Student: S1, Course: C1]");
        }
    }

    @Nested
    class BuilderAndData {

        @Test
        void builderProducesConsistentState() {
            RuntimeException cause = new RuntimeException("x");
            EnrollmentException e = EnrollmentException.builder(ErrorCode.WAITLIST_FULL)
                    .studentId("S1").courseId("C1").semester("SPRING")
                    .currentStatus(EnrollmentStatus.WAITLISTED).attemptedStatus(EnrollmentStatus.ENROLLED)
                    .addData("position", 12).addData(Map.of("max", 10))
                    .message("wl full").cause(cause).build();

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WAITLIST_FULL);
            assertThat(e.getMessage()).isEqualTo("wl full [Student: S1, Course: C1, Semester: SPRING]");
            assertThat(e).hasCause(cause);
            assertThat(e.getAdditionalData()).containsOnly(Map.entry("position", 12), Map.entry("max", 10));
        }

        @Test
        void additionalDataIsDefensivelyCopied() {
            EnrollmentException e = EnrollmentException.builder(ErrorCode.COURSE_FULL).addData("k", 1).build();

            e.getAdditionalData().clear();

            assertThat(e.getAdditionalData("k")).isEqualTo(1);
            assertThat(e.getAdditionalData("k", Integer.class)).isEqualTo(1);
            assertThat(e.getAdditionalData("k", String.class)).isNull();
            assertThat(e.getAdditionalData("nope", String.class)).isNull();
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void courseFull() {
            EnrollmentException e = EnrollmentException.courseFull("S1", "CS101", 30);

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COURSE_FULL);
            assertThat(e.getMessage()).isEqualTo("Course CS101 is at maximum capacity (30 students) [Student: S1, Course: CS101]");
            assertThat(e.getAdditionalData("maxEnrollment", Integer.class)).isEqualTo(30);
            assertThat(e.isRecoverable()).isTrue();
        }

        @Test
        void prerequisiteNotMet() {
            EnrollmentException e = EnrollmentException.prerequisiteNotMet("S1", "CS201", "CS101");

            assertThat(e.getMessage()).startsWith("Student S1 has not completed prerequisite: CS101");
            assertThat(e.getUserFriendlyMessage()).isEqualTo("You need to complete CS101 before enrolling in this course.");
        }

        @Test
        void scheduleConflict() {
            EnrollmentException e = EnrollmentException.scheduleConflict("S1", "C1", "C2", "MWF 9am");

            assertThat(e.getMessage()).startsWith("Course C1 conflicts with C2 at MWF 9am");
            assertThat(e.getAdditionalData()).containsEntry("timeSlot", "MWF 9am");
            assertThat(e.getUserFriendlyMessage()).isEqualTo("This course conflicts with your enrollment in C2.");
        }

        @Test
        void duplicateEnrollment() {
            EnrollmentException e = EnrollmentException.duplicateEnrollment("S1", "C1", EnrollmentStatus.ENROLLED);

            assertThat(e.getCurrentStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(e.getMessage()).contains("already enrolled in course C1 with status: ");
        }

        @Test
        void enrollmentClosed() {
            LocalDateTime closed = LocalDateTime.of(2024, 1, 15, 0, 0);
            EnrollmentException e = EnrollmentException.enrollmentClosed("C1", closed);

            assertThat(e.getMessage()).isEqualTo("Enrollment for course C1 closed on 2024-01-15T00:00 [Course: C1]");
            assertThat(e.getAdditionalData("closedDate", LocalDateTime.class)).isEqualTo(closed);
        }

        @Test
        void creditLimitExceeded() {
            EnrollmentException e = EnrollmentException.creditLimitExceeded("S1", 16, 4, 18);

            assertThat(e.getMessage()).startsWith("Enrollment would exceed credit limit. Current: 16, Course: 4, Max: 18");
            assertThat(e.getUserFriendlyMessage()).contains("maximum credit limit of 18 hours");
        }

        @Test
        void paymentRequired() {
            Locale previous = Locale.getDefault();
            Locale.setDefault(Locale.US);
            try {
                EnrollmentException e = EnrollmentException.paymentRequired("S1", 125.5);

                assertThat(e.getMessage()).startsWith("Payment of $125.50 required for enrollment");
                assertThat(e.getUserFriendlyMessage()).isEqualTo("Please make a payment of $125.50 to complete enrollment.");
                assertThat(e.requiresManualIntervention()).isTrue();
            } finally {
                Locale.setDefault(previous);
            }
        }

        @Test
        void holdOnAccount() {
            EnrollmentException e = EnrollmentException.holdOnAccount("S1", "financial", "unpaid fees");

            assertThat(e.getMessage()).startsWith("Student S1 has a financial hold: unpaid fees");
            assertThat(e.getUserFriendlyMessage()).isEqualTo("You have a financial hold on your account that prevents enrollment.");
        }

        @Test
        void invalidStatusTransition() {
            EnrollmentException e = EnrollmentException.invalidStatusTransition("S1", "C1",
                    EnrollmentStatus.GRADUATED, EnrollmentStatus.APPLIED);

            assertThat(e.getMessage()).startsWith("Invalid status transition from ");
            assertThat(e.getCurrentStatus()).isEqualTo(EnrollmentStatus.GRADUATED);
            assertThat(e.getAttemptedStatus()).isEqualTo(EnrollmentStatus.APPLIED);
        }

        @Test
        void systemError() {
            Exception cause = new RuntimeException("db down");
            EnrollmentException e = EnrollmentException.systemError("enroll", cause);

            assertThat(e).hasMessage("System error during enroll").hasCause(cause);
            assertThat(e.getAdditionalData()).containsEntry("operation", "enroll");
        }
    }

    @Nested
    class Classification {

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void recoverable(ErrorCode code) {
            assertThat(of(code).isRecoverable()).isEqualTo(RECOVERABLE.contains(code));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void manualIntervention(ErrorCode code) {
            assertThat(of(code).requiresManualIntervention()).isEqualTo(MANUAL.contains(code));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "COURSE_FULL | Join the waitlist",
                "PREREQUISITE_NOT_MET | Complete the required prerequisite",
                "SCHEDULE_CONFLICT | Choose a different section",
                "DUPLICATE_ENROLLMENT | Check your current enrollments",
                "ENROLLMENT_CLOSED | Contact the registrar or instructor",
                "CREDIT_LIMIT_EXCEEDED | Drop another course",
                "PAYMENT_REQUIRED | Make the required payment",
                "HOLD_ON_ACCOUNT | Contact the appropriate office",
                "INSTRUCTOR_APPROVAL_REQUIRED | Contact the course instructor",
                "DEPARTMENT_PERMISSION_REQUIRED | Contact the department office",
                "SYSTEM_ERROR | Contact the registrar's office"
        })
        void suggestedActions(ErrorCode code, String prefix) {
            assertThat(of(code).getSuggestedActions()).startsWith(prefix);
        }

        @Test
        void fixedUserFriendlyMessages() {
            assertThat(of(ErrorCode.COURSE_FULL).getUserFriendlyMessage()).startsWith("This course is currently full.");
            assertThat(of(ErrorCode.DUPLICATE_ENROLLMENT).getUserFriendlyMessage())
                    .isEqualTo("You are already enrolled in this course.");
            assertThat(of(ErrorCode.ENROLLMENT_CLOSED).getUserFriendlyMessage())
                    .isEqualTo("The enrollment period for this course has ended.");
            assertThat(of(ErrorCode.WAITLIST_FULL).getUserFriendlyMessage())
                    .isEqualTo(ErrorCode.WAITLIST_FULL.getDescription());
        }

        // Regression: without the factory-supplied data these messages used to read
        // "You need to complete null before ..." / "payment of $null"; they now fall back to the description.
        @ParameterizedTest
        @EnumSource(value = ErrorCode.class, names = {
                "PREREQUISITE_NOT_MET", "SCHEDULE_CONFLICT", "CREDIT_LIMIT_EXCEEDED", "PAYMENT_REQUIRED", "HOLD_ON_ACCOUNT"})
        void userFriendlyMessageNeverContainsNullWhenDataIsMissing(ErrorCode code) {
            String message = of(code).getUserFriendlyMessage();

            assertThat(message).doesNotContain("null").isEqualTo(code.getDescription());
        }
    }

    @Nested
    class ToString {

        @Test
        void includesPresentFields() {
            EnrollmentException e = EnrollmentException.builder(ErrorCode.SYSTEM_ERROR)
                    .studentId("S1").courseId("C1").semester("F")
                    .currentStatus(EnrollmentStatus.ENROLLED).attemptedStatus(EnrollmentStatus.WITHDRAWN)
                    .addData("k", "v").cause(new IllegalStateException("bad")).build();

            assertThat(e.toString()).startsWith("EnrollmentException{errorCode=SYSTEM_ERROR")
                    .contains("studentId='S1'", "courseId='C1'", "semester='F'", "currentStatus=",
                            "attemptedStatus=", "additionalData={k=v}", "cause=IllegalStateException: bad")
                    .endsWith("}");
        }

        @Test
        void omitsAbsentFields() {
            assertThat(of(ErrorCode.SYSTEM_ERROR).toString())
                    .doesNotContain("studentId=", "courseId=", "semester=", "currentStatus=", "additionalData=", "cause=");
        }
    }
}
