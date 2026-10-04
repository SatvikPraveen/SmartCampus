package events;

import enums.EnrollmentStatus;
import enums.Semester;
import events.Event.Category;
import events.Event.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

class StudentEnrolledEventTest {

    private static final LocalDateTime ENROLLED = LocalDateTime.of(2025, 8, 20, 8, 30);

    /** Valid enrollment with all optional details: 24 of 30 seats taken. */
    private static StudentEnrolledEvent.Builder full() {
        return StudentEnrolledEvent.builder()
                .student("S1", "Bob")
                .course("C1", "Intro", "CS101")
                .semester(Semester.FALL, 2025)
                .enrollment(EnrollmentStatus.ENROLLED, ENROLLED, "registrar")
                .courseDetails(3, "CS")
                .instructor("P1", "Dr. Ada")
                .capacity(24, 30)
                .waitlisted(false)
                .method("ONLINE_PORTAL")
                .previousStatus("DROPPED");
    }

    private static StudentEnrolledEvent basic() {
        return new StudentEnrolledEvent("S1", "Bob", "C1", "Intro", "CS101", Semester.SPRING, 2026,
                EnrollmentStatus.ENROLLED, ENROLLED, "registrar");
    }

    @Nested
    class Construction {

        @Test
        void builderPopulatesEveryField() {
            StudentEnrolledEvent e = full().build();

            assertThat(e.getEventType()).isEqualTo(StudentEnrolledEvent.EVENT_TYPE);
            assertThat(e.getCategory()).isEqualTo(Category.DOMAIN);
            assertThat(e.isForAggregate("Student", "S1")).isTrue();
            assertThat(e.getStudentId()).isEqualTo("S1");
            assertThat(e.getStudentName()).isEqualTo("Bob");
            assertThat(e.getCourseId()).isEqualTo("C1");
            assertThat(e.getCourseName()).isEqualTo("Intro");
            assertThat(e.getCourseCode()).isEqualTo("CS101");
            assertThat(e.getSemester()).isEqualTo(Semester.FALL);
            assertThat(e.getAcademicYear()).isEqualTo(2025);
            assertThat(e.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(e.getEnrollmentDate()).isEqualTo(ENROLLED);
            assertThat(e.getEnrolledBy()).isEqualTo("registrar");
            assertThat(e.getCourseCredits()).isEqualTo(3);
            assertThat(e.getDepartmentCode()).isEqualTo("CS");
            assertThat(e.getInstructorId()).isEqualTo("P1");
            assertThat(e.getInstructorName()).isEqualTo("Dr. Ada");
            assertThat(e.getCurrentEnrollmentCount()).isEqualTo(24);
            assertThat(e.getMaxEnrollmentCapacity()).isEqualTo(30);
            assertThat(e.isWaitlisted()).isFalse();
            assertThat(e.getEnrollmentMethod()).isEqualTo("ONLINE_PORTAL");
            assertThat(e.getPreviousStatus()).isEqualTo("DROPPED");
            assertThat(e.getPriority()).isEqualTo(Priority.NORMAL);
            assertThat(e.isValid()).isTrue();
        }

        @Test
        void basicConstructorUsesManualMethodAndNoCapacity() {
            StudentEnrolledEvent e = basic();

            assertThat(e.getEnrollmentMethod()).isEqualTo("MANUAL");
            assertThat(e.getCourseCredits()).isZero();
            assertThat(e.getDepartmentCode()).isNull();
            assertThat(e.getMaxEnrollmentCapacity()).isZero();
            assertThat(e.isWaitlisted()).isFalse();
            assertThat(e.getPreviousStatus()).isNull();
            assertThat(e.isValid()).isTrue();
        }

        @Test
        void nullMethodDefaultsToManual() {
            assertThat(full().method(null).build().getEnrollmentMethod()).isEqualTo("MANUAL");
        }

        // Regression: Event.Builder#priority/#correlationId/#aggregate were silently ignored by build().
        @Test
        void builderAppliesCommonEventProperties() {
            StudentEnrolledEvent e = full().priority(Priority.HIGH).correlationId("c-7")
                    .aggregate("Student", "S1", 9L).metadata("channel", "web").build();

            assertThat(e.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(e.getCorrelationId()).isEqualTo("c-7");
            assertThat(e.getAggregateVersion()).isEqualTo(9L);
            assertThat(e.getMetadata()).containsEntry("channel", "web");
            assertThat(e.getEnrollmentMethod()).isEqualTo("ONLINE_PORTAL");
        }

        // Regression: missing semester or status threw NullPointerException at construction instead of
        // yielding an event that isValid() rejects.
        @Test
        void incompleteEventCanBeBuiltAndIsInvalid() {
            StudentEnrolledEvent.Builder builder = StudentEnrolledEvent.builder().student("S1", "Bob");

            assertThatCode(builder::build).doesNotThrowAnyException();
            StudentEnrolledEvent e = builder.build();
            assertThat(e.isValid()).isFalse();
            assertThat(e.getMetadata()).doesNotContainKeys("enrollment.semester", "enrollment.status");
        }
    }

    @Nested
    class Validation {

        @Test
        void requiresMandatoryFields() {
            assertThat(full().student(null, "x").build().isValid()).isFalse();
            assertThat(full().student(" ", "x").build().isValid()).isFalse();
            assertThat(full().course(null, "n", "c").build().isValid()).isFalse();
            assertThat(full().course("", "n", "c").build().isValid()).isFalse();
            assertThat(full().semester(null, 2025).build().isValid()).isFalse();
            assertThat(full().semester(Semester.FALL, 0).build().isValid()).isFalse();
            assertThat(full().enrollment(null, ENROLLED, "r").build().isValid()).isFalse();
            assertThat(full().enrollment(EnrollmentStatus.ENROLLED, null, "r").build().isValid()).isFalse();
            assertThat(full().enrollment(EnrollmentStatus.ENROLLED, ENROLLED, " ").build().isValid()).isFalse();
            assertThat(full().enrollment(EnrollmentStatus.ENROLLED, ENROLLED, null).build().isValid()).isFalse();
        }
    }

    @Nested
    class Capacity {

        @ParameterizedTest
        @CsvSource({
                "24, 30, 6, 80.0, false",
                "30, 30, 0, 100.0, true",
                "35, 30, 0, 116.66666666666667, true",
                "0, 0, 0, 0.0, true"
        })
        void capacityFigures(int current, int max, int available, double utilization, boolean atCapacity) {
            StudentEnrolledEvent e = full().capacity(current, max).build();

            assertThat(e.getAvailableSpots()).isEqualTo(available);
            assertThat(e.getUtilizationPercentage()).isCloseTo(utilization, within(1e-9));
            assertThat(e.isCourseAtCapacity()).isEqualTo(atCapacity);
        }

        @Test
        void onlyNonWaitlistedEnrolledStudentsAffectCapacity() {
            assertThat(full().build().affectsCapacity()).isTrue();
            assertThat(full().waitlisted(true).build().affectsCapacity()).isFalse();
            assertThat(full().enrollment(EnrollmentStatus.AUDIT, ENROLLED, "r").build().affectsCapacity()).isFalse();
        }
    }

    @Nested
    class DerivedValues {

        @ParameterizedTest
        @ValueSource(strings = {"AUTOMATED", "BATCH", "SYSTEM"})
        void automatedMethods(String method) {
            assertThat(full().method(method).build().isAutomatedEnrollment()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"MANUAL", "ONLINE_PORTAL", "automated"})
        void nonAutomatedMethods(String method) {
            assertThat(full().method(method).build().isAutomatedEnrollment()).isFalse();
        }

        @Test
        void statusChangeDependsOnPreviousStatus() {
            assertThat(full().build().isStatusChange()).isTrue();
            assertThat(basic().isStatusChange()).isFalse();
        }

        @Test
        void displayHelpers() {
            StudentEnrolledEvent e = full().build();
            assertThat(e.getSemesterDisplay()).isEqualTo("Fall 2025");
            assertThat(e.getCourseDisplay()).isEqualTo("CS101 - Intro");
        }
    }

    @Nested
    class Description {

        @Test
        void enrolledWithPreviousStatus() {
            assertThat(full().build().getDescription()).isEqualTo(
                    "Student Bob (S1) enrolled in course CS101 - Intro for Fall 2025 (previous status: DROPPED)");
        }

        @Test
        void waitlistedWithoutPreviousStatus() {
            assertThat(full().waitlisted(true).previousStatus(null).build().getDescription()).isEqualTo(
                    "Student Bob (S1) was added to waitlist for course CS101 - Intro for Fall 2025");
        }
    }

    @Nested
    class PayloadAndMetadata {

        @Test
        @SuppressWarnings("unchecked")
        void payloadForFullEvent() {
            Map<String, Object> payload = (Map<String, Object>) full().build().getPayload();

            assertThat(payload.get("student")).isEqualTo(Map.of("id", "S1", "name", "Bob"));
            assertThat(payload.get("course")).isEqualTo(Map.of("id", "C1", "name", "Intro", "code", "CS101",
                    "credits", 3, "departmentCode", "CS"));
            assertThat(payload.get("instructor")).isEqualTo(Map.of("id", "P1", "name", "Dr. Ada"));
            assertThat(payload.get("enrollment")).isEqualTo(Map.of(
                    "status", EnrollmentStatus.ENROLLED.toString(), "date", ENROLLED.toString(),
                    "enrolledBy", "registrar", "method", "ONLINE_PORTAL", "isWaitlisted", false,
                    "previousStatus", "DROPPED"));
            assertThat(payload.get("academic"))
                    .isEqualTo(Map.of("semester", Semester.FALL.toString(), "academicYear", 2025));
            Map<String, Object> capacity = (Map<String, Object>) payload.get("capacity");
            assertThat(capacity).containsEntry("current", 24).containsEntry("maximum", 30)
                    .containsEntry("available", 6);
            assertThat((Double) capacity.get("utilizationPercentage")).isCloseTo(80.0, within(1e-9));
        }

        @Test
        @SuppressWarnings("unchecked")
        void payloadForBasicEventOmitsOptionalSections() {
            Map<String, Object> payload = (Map<String, Object>) basic().getPayload();

            assertThat(payload).doesNotContainKey("instructor");
            assertThat((Map<String, Object>) payload.get("enrollment")).doesNotContainKey("previousStatus");
            assertThat((Map<String, Object>) payload.get("capacity"))
                    .containsEntry("available", 0).containsEntry("utilizationPercentage", 0.0);
        }

        @Test
        void fullConstructorRecordsDetailedMetadata() {
            assertThat(full().build().getMetadata())
                    .containsEntry("enrollment.studentId", "S1")
                    .containsEntry("enrollment.semester", Semester.FALL.toString())
                    .containsEntry("enrollment.status", EnrollmentStatus.ENROLLED.toString())
                    .containsEntry("enrollment.method", "ONLINE_PORTAL")
                    .containsEntry("enrollment.previousStatus", "DROPPED")
                    .containsEntry("enrollment.isStatusChange", true)
                    .containsEntry("course.department", "CS")
                    .containsEntry("course.instructorName", "Dr. Ada")
                    .containsEntry("capacity.available", 6)
                    .containsEntry("capacity.utilizationPercentage", 80.0)
                    .containsEntry("capacity.atCapacity", false);
        }

        @Test
        void sparseMetadataOmitsOptionalEntries() {
            assertThat(full().previousStatus(null).courseDetails(0, null).instructor(null, null).build()
                    .getMetadata())
                    .doesNotContainKeys("enrollment.previousStatus", "enrollment.isStatusChange",
                            "course.department", "course.instructorId");
            assertThat(basic().getMetadata()).doesNotContainKeys("course.code", "capacity.current");
        }
    }

    @Nested
    class CopiesAndEquality {

        @Test
        void copyKeepsEnrollmentData() {
            StudentEnrolledEvent original = full().build();

            StudentEnrolledEvent copy = (StudentEnrolledEvent) original.withVersion(2);

            assertThat(copy.getVersion()).isEqualTo(2);
            assertThat(copy.getEnrollmentMethod()).isEqualTo("ONLINE_PORTAL");
            assertThat(copy.getCurrentEnrollmentCount()).isEqualTo(24);
            assertThat(copy.getPreviousStatus()).isEqualTo("DROPPED");
            assertThat(copy.getMetadata()).isEqualTo(original.getMetadata());
            assertThat(copy).isEqualTo(original).hasSameHashCodeAs(original);
        }

        @Test
        void distinctEventsAreNotEqual() {
            StudentEnrolledEvent e = full().build();
            assertThat(e).isEqualTo(e).isNotEqualTo(full().build()).isNotEqualTo(null);
            assertThat(e.equals(new TestEvent(StudentEnrolledEvent.EVENT_TYPE, "x"))).isFalse();
        }
    }
}
