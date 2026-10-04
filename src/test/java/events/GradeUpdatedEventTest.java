package events;

import enums.GradeLevel;
import enums.Semester;
import events.Event.Category;
import events.Event.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

class GradeUpdatedEventTest {

    private static final LocalDateTime GRADED = LocalDateTime.of(2025, 12, 15, 17, 0);

    /** Valid, non-final assignment grade change B -> A with every optional field set. */
    private static GradeUpdatedEvent.Builder full() {
        return GradeUpdatedEvent.builder()
                .student("S1", "Bob")
                .course("C1", "Intro", "CS101")
                .semester(Semester.FALL, 2025)
                .grades(GradeLevel.A, GradeLevel.B)
                .percentages(94.5, 85.0)
                .gradingInfo("P1", GRADED)
                .comments("Great work")
                .updateReason("regrade")
                .finalGrade(false)
                .assignment("HW1")
                .courseDetails("CS", 3)
                .instructor("P1", "Dr. Ada")
                .gradeType("HOMEWORK")
                .approvedBy("chair");
    }

    private static GradeUpdatedEvent change(GradeLevel from, GradeLevel to) {
        return full().grades(to, from).build();
    }

    private static GradeUpdatedEvent basic(GradeLevel newGrade, GradeLevel previousGrade, boolean isFinal) {
        return new GradeUpdatedEvent("S1", "Bob", "C1", "Intro", "CS101", Semester.FALL, 2025,
                newGrade, previousGrade, "P1", GRADED, isFinal);
    }

    @Nested
    class Construction {

        @Test
        void builderPopulatesEveryField() {
            GradeUpdatedEvent e = full().build();

            assertThat(e.getEventType()).isEqualTo(GradeUpdatedEvent.EVENT_TYPE);
            assertThat(e.getCategory()).isEqualTo(Category.DOMAIN);
            assertThat(e.getAggregateType()).isEqualTo("Student");
            assertThat(e.getAggregateId()).isEqualTo("S1");
            assertThat(e.getStudentId()).isEqualTo("S1");
            assertThat(e.getStudentName()).isEqualTo("Bob");
            assertThat(e.getCourseId()).isEqualTo("C1");
            assertThat(e.getCourseName()).isEqualTo("Intro");
            assertThat(e.getCourseCode()).isEqualTo("CS101");
            assertThat(e.getSemester()).isEqualTo(Semester.FALL);
            assertThat(e.getAcademicYear()).isEqualTo(2025);
            assertThat(e.getNewGrade()).isEqualTo(GradeLevel.A);
            assertThat(e.getPreviousGrade()).isEqualTo(GradeLevel.B);
            assertThat(e.getNewPercentage()).isEqualTo(94.5);
            assertThat(e.getPreviousPercentage()).isEqualTo(85.0);
            assertThat(e.getGradedBy()).isEqualTo("P1");
            assertThat(e.getGradedDate()).isEqualTo(GRADED);
            assertThat(e.getGradeComments()).isEqualTo("Great work");
            assertThat(e.getUpdateReason()).isEqualTo("regrade");
            assertThat(e.isFinalGrade()).isFalse();
            assertThat(e.getAssignmentName()).isEqualTo("HW1");
            assertThat(e.getDepartmentCode()).isEqualTo("CS");
            assertThat(e.getInstructorId()).isEqualTo("P1");
            assertThat(e.getInstructorName()).isEqualTo("Dr. Ada");
            assertThat(e.getCourseCredits()).isEqualTo(3);
            assertThat(e.getNewGpaPoints()).isEqualTo(4.0);
            assertThat(e.getPreviousGpaPoints()).isEqualTo(3.0);
            assertThat(e.affectsGpa()).isTrue();
            assertThat(e.getGradeType()).isEqualTo("HOMEWORK");
            assertThat(e.getApprovedBy()).isEqualTo("chair");
            assertThat(e.isValid()).isTrue();
        }

        @Test
        void gradeTypeDefaultsFromFinalFlag() {
            assertThat(full().gradeType(null).finalGrade(true).build().getGradeType()).isEqualTo("FINAL");
            assertThat(full().gradeType(null).finalGrade(false).build().getGradeType()).isEqualTo("ASSIGNMENT");
            assertThat(basic(GradeLevel.A, null, true).getGradeType()).isEqualTo("FINAL");
            assertThat(basic(GradeLevel.A, null, false).getGradeType()).isEqualTo("ASSIGNMENT");
        }

        @Test
        void basicConstructorLeavesDetailsEmpty() {
            GradeUpdatedEvent e = basic(GradeLevel.B, GradeLevel.C, false);

            assertThat(e.getNewPercentage()).isZero();
            assertThat(e.getCourseCredits()).isZero();
            assertThat(e.getAssignmentName()).isNull();
            assertThat(e.getInstructorId()).isNull();
            assertThat(e.getApprovedBy()).isNull();
            assertThat(e.getMetadata()).containsEntry("grade.studentId", "S1")
                    .doesNotContainKeys("course.code", "gpaImpact.pointsChange");
        }

        @Test
        void nonGpaGradesHaveNoGpaPoints() {
            GradeUpdatedEvent e = change(GradeLevel.INCOMPLETE, GradeLevel.PASS);

            assertThat(e.getNewGpaPoints()).isNull();
            assertThat(e.getPreviousGpaPoints()).isNull();
            assertThat(e.affectsGpa()).isFalse();
            assertThat(e.getGpaPointsChange()).isNull();
            assertThat(e.getWeightedGpaImpact()).isNull();
        }

        @Test
        void missingGradesAreHandled() {
            GradeUpdatedEvent e = full().grades(null, null).build();

            assertThat(e.getNewGpaPoints()).isNull();
            assertThat(e.affectsGpa()).isFalse();
            assertThat(e.isValid()).isFalse();
            assertThat(e.getGradeChangeDisplay()).isEqualTo("Grade Updated");
        }

        // Regression: Event.Builder#priority/#correlationId/#aggregate were silently ignored by build().
        @Test
        void builderAppliesCommonEventProperties() {
            GradeUpdatedEvent e = full().priority(Priority.LOW).correlationId("corr-1")
                    .aggregate("Student", "S1", 3L).metadata("k", "v").build();

            assertThat(e.getPriority()).isEqualTo(Priority.LOW);
            assertThat(e.getCorrelationId()).isEqualTo("corr-1");
            assertThat(e.getAggregateVersion()).isEqualTo(3L);
            assertThat(e.getMetadata()).containsEntry("k", "v");
        }

        @Test
        void builderWithoutExplicitPriorityKeepsDerivedPriority() {
            assertThat(full().finalGrade(true).build().getPriority()).isEqualTo(Priority.HIGH);
        }

        // Regression: a builder without a semester threw NullPointerException instead of producing an
        // event that isValid() reports as invalid.
        @Test
        void missingSemesterYieldsInvalidEventInsteadOfException() {
            assertThatCode(() -> full().semester(null, 2025).build()).doesNotThrowAnyException();
            GradeUpdatedEvent e = full().semester(null, 2025).build();
            assertThat(e.isValid()).isFalse();
            assertThat(e.hasMetadata("grade.semester")).isFalse();
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
            assertThat(full().grades(null, GradeLevel.B).build().isValid()).isFalse();
            assertThat(full().gradingInfo(null, GRADED).build().isValid()).isFalse();
            assertThat(full().gradingInfo(" ", GRADED).build().isValid()).isFalse();
            assertThat(full().gradingInfo("P1", null).build().isValid()).isFalse();
            assertThat(full().semester(Semester.FALL, 0).build().isValid()).isFalse();
            assertThat(full().grades(GradeLevel.A, null).build().isValid()).isTrue();
        }
    }

    @Nested
    class Priorities {

        @ParameterizedTest
        @CsvSource({
                "B, A, false, NORMAL",   // improvement within passing
                "C, A, false, NORMAL",   // significant but still passing
                "F, C, false, HIGH",     // failing -> passing
                "B, F, false, HIGH",     // passing -> failing
                "B, A, true,  HIGH",     // final grades are always high
                "PASS, INCOMPLETE, false, NORMAL"
        })
        void priorityReflectsImportanceOfChange(GradeLevel from, GradeLevel to, boolean isFinal, Priority expected) {
            assertThat(full().grades(to, from).finalGrade(isFinal).build().getPriority()).isEqualTo(expected);
        }

        @Test
        void firstGradeHasNormalPriority() {
            assertThat(full().grades(GradeLevel.F, null).build().getPriority()).isEqualTo(Priority.NORMAL);
        }

        // Regression: the basic constructor hard-coded NORMAL priority, so the same final grade or
        // pass/fail change was HIGH via the builder but NORMAL via this constructor.
        @Test
        void basicConstructorDerivesPriorityLikeTheFullConstructor() {
            assertThat(basic(GradeLevel.A, GradeLevel.B, true).getPriority()).isEqualTo(Priority.HIGH);
            assertThat(basic(GradeLevel.C, GradeLevel.F, false).getPriority()).isEqualTo(Priority.HIGH);
            assertThat(basic(GradeLevel.A, GradeLevel.B, false).getPriority()).isEqualTo(Priority.NORMAL);
        }
    }

    @Nested
    class DerivedValues {

        @Test
        void improvementAndDecline() {
            assertThat(change(GradeLevel.B, GradeLevel.A).isImprovement()).isTrue();
            assertThat(change(GradeLevel.B, GradeLevel.A).isDecline()).isFalse();
            assertThat(change(GradeLevel.A, GradeLevel.B).isDecline()).isTrue();
            assertThat(change(GradeLevel.A, GradeLevel.B).isImprovement()).isFalse();
            assertThat(change(GradeLevel.A, GradeLevel.A_PLUS).isImprovement()).isFalse(); // both 4.0
            assertThat(change(GradeLevel.A, GradeLevel.A_PLUS).isDecline()).isFalse();
        }

        @Test
        void improvementNeedsBothGradesWithGpaPoints() {
            assertThat(full().grades(GradeLevel.A, null).build().isImprovement()).isFalse();
            assertThat(full().grades(GradeLevel.A, null).build().isDecline()).isFalse();
            assertThat(change(GradeLevel.PASS, GradeLevel.A).isImprovement()).isFalse();
            assertThat(change(GradeLevel.A, GradeLevel.PASS).isDecline()).isFalse();
        }

        @Test
        void gpaAndPercentageChanges() {
            GradeUpdatedEvent e = full().build(); // B (3.0, 85%) -> A (4.0, 94.5%), 3 credits

            assertThat(e.getGpaPointsChange()).isCloseTo(1.0, within(1e-9));
            assertThat(e.getWeightedGpaImpact()).isCloseTo(3.0, within(1e-9));
            assertThat(e.getPercentageChange()).isCloseTo(9.5, within(1e-9));
            assertThat(e.isSignificantChange()).isTrue();
            assertThat(change(GradeLevel.B, GradeLevel.B_PLUS).isSignificantChange()).isFalse();
            assertThat(change(GradeLevel.A, GradeLevel.C).isSignificantChange()).isTrue();
        }

        @Test
        void passFailTransitions() {
            assertThat(change(GradeLevel.F, GradeLevel.C).isFailingToPassingChange()).isTrue();
            assertThat(change(GradeLevel.F, GradeLevel.C).isPassingToFailingChange()).isFalse();
            assertThat(change(GradeLevel.C, GradeLevel.F).isPassingToFailingChange()).isTrue();
            assertThat(change(GradeLevel.B, GradeLevel.A).isFailingToPassingChange()).isFalse();
            assertThat(full().grades(GradeLevel.A, null).build().isFailingToPassingChange()).isFalse();
            assertThat(full().grades(GradeLevel.F, null).build().isPassingToFailingChange()).isFalse();
        }

        @Test
        void displayHelpers() {
            GradeUpdatedEvent e = full().build();

            assertThat(e.getSemesterDisplay()).isEqualTo("Fall 2025");
            assertThat(e.getCourseDisplay()).isEqualTo("CS101 - Intro");
            assertThat(e.getGradeChangeDisplay()).isEqualTo("B → A");
            assertThat(full().grades(GradeLevel.A, null).build().getGradeChangeDisplay()).isEqualTo("New: A");
        }
    }

    @Nested
    class Description {

        @Test
        void assignmentGradeChange() {
            assertThat(full().build().getDescription()).isEqualTo(
                    "Grade updated for student Bob (S1) on assignment 'HW1' in course CS101 - Intro: B → A"
                            + " for Fall 2025 (Reason: regrade)");
        }

        @Test
        void finalGradeWithoutPreviousGradeOrReason() {
            assertThat(basic(GradeLevel.A, null, true).getDescription()).isEqualTo(
                    "Final grade updated for student Bob (S1) in course CS101 - Intro: A for Fall 2025");
        }

        @Test
        void nonFinalWithoutAssignmentOrGrade() {
            assertThat(basic(null, null, false).getDescription()).isEqualTo(
                    "Grade updated for student Bob (S1) in course CS101 - Intro for Fall 2025");
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
            assertThat(payload.get("academic"))
                    .isEqualTo(Map.of("semester", Semester.FALL.toString(), "academicYear", 2025));

            Map<String, Object> grade = (Map<String, Object>) payload.get("grade");
            assertThat(grade)
                    .containsEntry("gradedBy", "P1")
                    .containsEntry("gradedDate", GRADED.toString())
                    .containsEntry("isFinalGrade", false)
                    .containsEntry("gradeType", "HOMEWORK")
                    .containsEntry("affectsGpa", true)
                    .containsEntry("comments", "Great work")
                    .containsEntry("updateReason", "regrade")
                    .containsEntry("assignmentName", "HW1")
                    .containsEntry("approvedBy", "chair");
            assertThat(grade.get("new")).isEqualTo(Map.of("level", GradeLevel.A.toString(), "letterGrade", "A",
                    "gpaPoints", 4.0, "percentage", 94.5, "passing", true));
            assertThat(grade.get("previous")).isEqualTo(Map.of("level", GradeLevel.B.toString(),
                    "letterGrade", "B", "gpaPoints", 3.0, "percentage", 85.0, "passing", true));

            Map<String, Object> impact = (Map<String, Object>) payload.get("gpaImpact");
            assertThat((Double) impact.get("pointsChange")).isCloseTo(1.0, within(1e-9));
            assertThat(impact).containsEntry("creditHours", 3);
            assertThat((Double) impact.get("weightedChange")).isCloseTo(3.0, within(1e-9));
        }

        @Test
        @SuppressWarnings("unchecked")
        void payloadForSparseEventOmitsOptionalSections() {
            Map<String, Object> payload = (Map<String, Object>) basic(GradeLevel.PASS, null, false).getPayload();

            assertThat(payload).doesNotContainKeys("instructor", "gpaImpact");
            Map<String, Object> grade = (Map<String, Object>) payload.get("grade");
            assertThat(grade).containsKey("new").doesNotContainKeys("previous", "comments", "updateReason",
                    "assignmentName", "approvedBy");
        }

        @Test
        @SuppressWarnings("unchecked")
        void payloadWithoutNewGradeHasNoNewSection() {
            Map<String, Object> payload = (Map<String, Object>) basic(null, GradeLevel.B, false).getPayload();
            assertThat((Map<String, Object>) payload.get("grade")).containsKey("previous").doesNotContainKey("new");
        }

        @Test
        void metadataDescribesTheChange() {
            GradeUpdatedEvent e = full().build();

            assertThat(e.getMetadata())
                    .containsEntry("grade.semester", Semester.FALL.toString())
                    .containsEntry("grade.gradeType", "HOMEWORK")
                    .containsEntry("grade.new.letterGrade", "A")
                    .containsEntry("grade.previous.letterGrade", "B")
                    .containsEntry("grade.isImprovement", true)
                    .containsEntry("grade.isDecline", false)
                    .containsEntry("grade.isSignificantChange", true)
                    .containsEntry("grade.isFailingToPassingChange", false)
                    .containsEntry("grade.isPassingToFailingChange", false)
                    .containsEntry("grade.assignmentName", "HW1")
                    .containsEntry("grade.updateReason", "regrade")
                    .containsEntry("grade.approvedBy", "chair")
                    .containsEntry("course.department", "CS")
                    .containsEntry("course.instructorId", "P1")
                    .containsEntry("gpaImpact.creditHours", 3);
            assertThat(e.getMetadata(("gpaImpact.weightedChange"), Double.class)).isCloseTo(3.0, within(1e-9));
        }

        @Test
        void sparseMetadataOmitsOptionalEntries() {
            GradeUpdatedEvent e = full().grades(GradeLevel.PASS, null).assignment(null).updateReason(null)
                    .approvedBy(null).courseDetails(null, 3).instructor(null, null).build();

            assertThat(e.getMetadata()).doesNotContainKeys("grade.new.gpaPoints", "grade.previous.level",
                    "grade.isImprovement", "grade.assignmentName", "grade.updateReason", "grade.approvedBy",
                    "course.department", "course.instructorId", "gpaImpact.pointsChange");
        }

        @Test
        void gpaAffectingGradeWithoutPreviousHasNoImpactMetadata() {
            assertThat(full().grades(GradeLevel.A, null).build().getMetadata())
                    .doesNotContainKey("gpaImpact.pointsChange");
        }
    }

    @Nested
    class CopiesAndEquality {

        @Test
        void copyKeepsGradeDataAndIdentity() {
            GradeUpdatedEvent original = full().build();

            GradeUpdatedEvent copy = (GradeUpdatedEvent) original.withCorrelationId("corr-x");

            assertThat(copy.getCorrelationId()).isEqualTo("corr-x");
            assertThat(copy.getNewGpaPoints()).isEqualTo(4.0);
            assertThat(copy.getPreviousGpaPoints()).isEqualTo(3.0);
            assertThat(copy.affectsGpa()).isTrue();
            assertThat(copy.getGradeType()).isEqualTo("HOMEWORK");
            assertThat(copy.getApprovedBy()).isEqualTo("chair");
            assertThat(copy).isEqualTo(original).hasSameHashCodeAs(original);
        }

        @Test
        void copyOfEventWithoutGradesKeepsNullGpa() {
            GradeUpdatedEvent copy = (GradeUpdatedEvent) full().grades(null, null).build().withVersion(2);
            assertThat(copy.getNewGpaPoints()).isNull();
            assertThat(copy.getPreviousGpaPoints()).isNull();
            assertThat(copy.affectsGpa()).isFalse();
        }

        @Test
        void distinctEventsAreNotEqual() {
            GradeUpdatedEvent e = full().build();
            assertThat(e).isEqualTo(e).isNotEqualTo(full().build()).isNotEqualTo(null);
            assertThat(e.equals(new TestEvent(GradeUpdatedEvent.EVENT_TYPE, "x"))).isFalse();
        }
    }
}
