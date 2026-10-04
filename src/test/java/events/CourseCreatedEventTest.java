package events;

import enums.CourseStatus;
import enums.Semester;
import events.Event.Category;
import events.Event.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CourseCreatedEventTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2025, 3, 1, 9, 0);
    private static final LocalDateTime APPROVED = LocalDateTime.of(2025, 3, 2, 10, 30);

    /** Fully valid builder with every optional field filled in. */
    private static CourseCreatedEvent.Builder full() {
        return CourseCreatedEvent.builder()
                .course("C1", "Data Structures", "CS201", "201")
                .department("CS", "Computer Science")
                .description("Lists, trees and graphs")
                .credits(4)
                .semester(Semester.FALL, 2025)
                .status(CourseStatus.ENROLLMENT_OPEN)
                .instructor("P1", "Dr. Ada")
                .enrollment(30, true)
                .location("Room 101")
                .schedule("MWF 10:00")
                .prerequisites(List.of("CS101"))
                .creation("admin", CREATED)
                .deliveryMode(false, true)
                .syllabus("syllabus.pdf")
                .gradeScale("A-F")
                .approval("dean", APPROVED)
                .catalogDescription("Core CS course")
                .learningObjectives(List.of("Trees", "Graphs"));
    }

    /** Valid event via the basic constructor. */
    private static CourseCreatedEvent basic(String courseNumber, CourseStatus status) {
        return new CourseCreatedEvent("C2", "Intro", "CS101", courseNumber, "CS", "Computer Science",
                "Basics", 3, Semester.SPRING, 2026, status, null, null, 25, "admin", CREATED);
    }

    @Nested
    class Construction {

        @Test
        void builderPopulatesEveryField() {
            CourseCreatedEvent e = full().build();

            assertThat(e.getEventType()).isEqualTo(CourseCreatedEvent.EVENT_TYPE);
            assertThat(e.getCategory()).isEqualTo(Category.DOMAIN);
            assertThat(e.getAggregateType()).isEqualTo("Course");
            assertThat(e.getAggregateId()).isEqualTo("C1");
            assertThat(e.getCourseId()).isEqualTo("C1");
            assertThat(e.getCourseName()).isEqualTo("Data Structures");
            assertThat(e.getCourseCode()).isEqualTo("CS201");
            assertThat(e.getCourseNumber()).isEqualTo("201");
            assertThat(e.getDepartmentCode()).isEqualTo("CS");
            assertThat(e.getDepartmentName()).isEqualTo("Computer Science");
            assertThat(e.getCourseDescription()).isEqualTo("Lists, trees and graphs");
            assertThat(e.getCredits()).isEqualTo(4);
            assertThat(e.getSemester()).isEqualTo(Semester.FALL);
            assertThat(e.getAcademicYear()).isEqualTo(2025);
            assertThat(e.getStatus()).isEqualTo(CourseStatus.ENROLLMENT_OPEN);
            assertThat(e.getInstructorId()).isEqualTo("P1");
            assertThat(e.getInstructorName()).isEqualTo("Dr. Ada");
            assertThat(e.getMaxEnrollment()).isEqualTo(30);
            assertThat(e.hasWaitlist()).isTrue();
            assertThat(e.getLocation()).isEqualTo("Room 101");
            assertThat(e.getSchedule()).isEqualTo("MWF 10:00");
            assertThat(e.getPrerequisites()).containsExactly("CS101");
            assertThat(e.getCreatedBy()).isEqualTo("admin");
            assertThat(e.getCreatedDate()).isEqualTo(CREATED);
            assertThat(e.isOnline()).isFalse();
            assertThat(e.isHybrid()).isTrue();
            assertThat(e.getSyllabus()).isEqualTo("syllabus.pdf");
            assertThat(e.getGradeScale()).isEqualTo("A-F");
            assertThat(e.getApprovedBy()).isEqualTo("dean");
            assertThat(e.getApprovedDate()).isEqualTo(APPROVED);
            assertThat(e.getCatalogDescription()).isEqualTo("Core CS course");
            assertThat(e.getLearningObjectives()).containsExactly("Trees", "Graphs");
            assertThat(e.isValid()).isTrue();
        }

        @Test
        void basicConstructorLeavesOptionalFieldsEmpty() {
            CourseCreatedEvent e = basic("101", CourseStatus.DRAFT);

            assertThat(e.getLocation()).isNull();
            assertThat(e.getSchedule()).isNull();
            assertThat(e.getPrerequisites()).isNull();
            assertThat(e.isOnline()).isFalse();
            assertThat(e.isHybrid()).isFalse();
            assertThat(e.hasWaitlist()).isFalse();
            assertThat(e.isApproved()).isFalse();
            assertThat(e.hasPrerequisites()).isFalse();
            assertThat(e.hasLearningObjectives()).isFalse();
            assertThat(e.getPriority()).isEqualTo(Priority.NORMAL);
            assertThat(e.isValid()).isTrue();
        }

        // Regression: Event.Builder#priority/#correlationId/#aggregate were silently ignored by build().
        @Test
        void builderAppliesCommonEventProperties() {
            CourseCreatedEvent e = full()
                    .priority(Priority.CRITICAL)
                    .correlationId("corr-42")
                    .aggregate("Course", "C1", 5L)
                    .metadata("source", "import")
                    .metadata(Map.of("batch", 7))
                    .build();

            assertThat(e.getPriority()).isEqualTo(Priority.CRITICAL);
            assertThat(e.getCorrelationId()).isEqualTo("corr-42");
            assertThat(e.getAggregateVersion()).isEqualTo(5L);
            assertThat(e.getMetadata()).containsEntry("source", "import").containsEntry("batch", 7);
            assertThat(e.getCourseCode()).isEqualTo("CS201");
            assertThat(e.getCourseLevel()).isEqualTo("UNDERGRADUATE");
        }

        @Test
        void builderWithoutCommonPropertiesKeepsEventDefaults() {
            CourseCreatedEvent e = full().aggregate("Course", "C1").build();

            assertThat(e.getPriority()).isEqualTo(Priority.NORMAL);
            assertThat(e.getCorrelationId()).isNotBlank();
            assertThat(e.getAggregateVersion()).isNull();
        }

        // Regression: a builder without semester/status threw NullPointerException while building
        // metadata, although isValid() exists precisely to report such incomplete events.
        @Test
        void incompleteEventCanBeBuiltAndIsReportedInvalid() {
            CourseCreatedEvent.Builder builder = CourseCreatedEvent.builder()
                    .course("C1", "Name", "CODE", "101")
                    .status(null);

            assertThatCode(builder::build).doesNotThrowAnyException();
            CourseCreatedEvent e = builder.build();
            assertThat(e.isValid()).isFalse();
            assertThat(e.hasMetadata("course.status")).isFalse();
            assertThat(e.hasMetadata("academic.semester")).isFalse();
            assertThat(e.hasMetadata("academic.semesterDisplay")).isFalse();
        }
    }

    @Nested
    class Validation {

        @Test
        void requiresAllMandatoryFields() {
            assertThat(full().course(null, "n", "c", "1").build().isValid()).isFalse();
            assertThat(full().course(" ", "n", "c", "1").build().isValid()).isFalse();
            assertThat(full().course("id", "", "c", "1").build().isValid()).isFalse();
            assertThat(full().course("id", "n", null, "1").build().isValid()).isFalse();
            assertThat(full().course("id", "n", "c", " ").build().isValid()).isFalse();
            assertThat(full().department(null, "x").build().isValid()).isFalse();
            assertThat(full().credits(0).build().isValid()).isFalse();
            assertThat(full().semester(Semester.FALL, 0).build().isValid()).isFalse();
            assertThat(full().enrollment(0, false).build().isValid()).isFalse();
            assertThat(full().creation(" ", CREATED).build().isValid()).isFalse();
            assertThat(full().creation("admin", null).build().isValid()).isFalse();
            assertThat(full().build().isValid()).isTrue();
        }
    }

    @Nested
    class DerivedValues {

        @ParameterizedTest
        @CsvSource({
                "101, UNDERGRADUATE",
                "499, UNDERGRADUATE",
                "500, GRADUATE",
                "901, GRADUATE",
                "099, UNKNOWN",
                "X10, UNKNOWN"
        })
        void courseLevelIsDerivedFromFirstDigit(String number, String level) {
            CourseCreatedEvent e = basic(number, CourseStatus.ENROLLMENT_OPEN);

            assertThat(e.getCourseLevel()).isEqualTo(level);
            assertThat(e.isUndergraduateLevel()).isEqualTo(level.equals("UNDERGRADUATE"));
            assertThat(e.isGraduateLevel()).isEqualTo(level.equals("GRADUATE"));
        }

        @Test
        void emptyOrMissingCourseNumberHasUnknownLevel() {
            assertThat(basic("", CourseStatus.DRAFT).getCourseLevel()).isEqualTo("UNKNOWN");
            assertThat(basic(null, CourseStatus.DRAFT).getCourseLevel()).isEqualTo("UNKNOWN");
        }

        @Test
        void deliveryModeDisplayPrefersOnlineOverHybrid() {
            assertThat(full().deliveryMode(true, true).build().getDeliveryModeDisplay()).isEqualTo("Online");
            assertThat(full().deliveryMode(false, true).build().getDeliveryModeDisplay()).isEqualTo("Hybrid");
            assertThat(full().deliveryMode(false, false).build().getDeliveryModeDisplay()).isEqualTo("In-Person");
        }

        @Test
        void displayHelpers() {
            CourseCreatedEvent e = full().build();

            assertThat(e.getSemesterDisplay()).isEqualTo("Fall 2025");
            assertThat(e.getCourseDisplay()).isEqualTo("CS201 - Data Structures");
            assertThat(e.getFullCourseIdentifier()).isEqualTo("CS 201 - Data Structures");
        }

        @Test
        void optionalCollectionsAndApproval() {
            assertThat(full().build().hasPrerequisites()).isTrue();
            assertThat(full().prerequisites(List.of()).build().hasPrerequisites()).isFalse();
            assertThat(full().build().hasLearningObjectives()).isTrue();
            assertThat(full().learningObjectives(List.of()).build().hasLearningObjectives()).isFalse();
            assertThat(full().build().isApproved()).isTrue();
            assertThat(full().approval("dean", null).build().isApproved()).isFalse();
            assertThat(full().approval(null, APPROVED).build().isApproved()).isFalse();
        }
    }

    @Nested
    class Description {

        @Test
        void fullDescriptionMentionsInstructorAndDeliveryMode() {
            assertThat(full().build().getDescription()).isEqualTo(
                    "New course created: CS201 - Data Structures (4 credits) taught by Dr. Ada"
                            + " in Computer Science for Fall 2025 (Hybrid)");
        }

        @Test
        void inPersonCourseWithoutInstructorShowsNonOpenStatus() {
            assertThat(basic("101", CourseStatus.DRAFT).getDescription()).isEqualTo(
                    "New course created: CS101 - Intro (3 credits) in Computer Science for Spring 2026"
                            + " - Status: Draft");
        }
    }

    @Nested
    class PayloadAndMetadata {

        @Test
        @SuppressWarnings("unchecked")
        void payloadForFullCourse() {
            Map<String, Object> payload = (Map<String, Object>) full().build().getPayload();

            Map<String, Object> course = (Map<String, Object>) payload.get("course");
            assertThat(course)
                    .containsEntry("id", "C1")
                    .containsEntry("code", "CS201")
                    .containsEntry("credits", 4)
                    .containsEntry("status", CourseStatus.ENROLLMENT_OPEN.toString())
                    .containsEntry("courseLevel", "UNDERGRADUATE")
                    .containsEntry("location", "Room 101")
                    .containsEntry("schedule", "MWF 10:00")
                    .containsEntry("syllabus", "syllabus.pdf")
                    .containsEntry("gradeScale", "A-F")
                    .containsEntry("catalogDescription", "Core CS course")
                    .containsEntry("prerequisites", List.of("CS101"))
                    .containsEntry("learningObjectives", List.of("Trees", "Graphs"))
                    .containsEntry("deliveryMode", "HYBRID");
            assertThat(payload.get("department")).isEqualTo(Map.of("code", "CS", "name", "Computer Science"));
            assertThat(payload.get("instructor")).isEqualTo(Map.of("id", "P1", "name", "Dr. Ada"));
            assertThat(payload.get("academic"))
                    .isEqualTo(Map.of("semester", Semester.FALL.toString(), "academicYear", 2025));
            assertThat(payload.get("creation")).isEqualTo(Map.of("createdBy", "admin",
                    "createdDate", CREATED.toString(), "approvedBy", "dean", "approvedDate", APPROVED.toString()));
        }

        @Test
        @SuppressWarnings("unchecked")
        void payloadForMinimalCourseOmitsOptionalSections() {
            Map<String, Object> payload = (Map<String, Object>) basic("101", CourseStatus.DRAFT).getPayload();

            Map<String, Object> course = (Map<String, Object>) payload.get("course");
            assertThat(course).doesNotContainKeys("location", "schedule", "syllabus", "gradeScale",
                    "catalogDescription", "prerequisites", "learningObjectives");
            assertThat(course).containsEntry("deliveryMode", "IN_PERSON");
            assertThat(payload).doesNotContainKey("instructor");
            assertThat((Map<String, Object>) payload.get("creation")).doesNotContainKey("approvedBy");
        }

        @Test
        @SuppressWarnings("unchecked")
        void onlineCourseHasOnlineDeliveryMode() {
            Map<String, Object> payload = (Map<String, Object>) full().deliveryMode(true, false).build().getPayload();
            assertThat((Map<String, Object>) payload.get("course")).containsEntry("deliveryMode", "ONLINE");
        }

        // Regression: an approver without an approval date made getPayload() throw NullPointerException.
        @Test
        @SuppressWarnings("unchecked")
        void approverWithoutDateDoesNotBreakPayload() {
            CourseCreatedEvent e = full().approval("dean", null).build();

            Map<String, Object> creation = (Map<String, Object>) ((Map<String, Object>) e.getPayload()).get("creation");

            assertThat(creation).containsEntry("approvedBy", "dean").doesNotContainKey("approvedDate");
        }

        @Test
        void fullConstructorRecordsDetailedMetadata() {
            CourseCreatedEvent e = full().build();

            assertThat(e.getMetadata())
                    .containsEntry("course.id", "C1")
                    .containsEntry("course.level", "UNDERGRADUATE")
                    .containsEntry("course.deliveryMode", "Hybrid")
                    .containsEntry("course.location", "Room 101")
                    .containsEntry("course.schedule", "MWF 10:00")
                    .containsEntry("course.hasPrerequisites", true)
                    .containsEntry("course.prerequisiteCount", 1)
                    .containsEntry("instructor.id", "P1")
                    .containsEntry("academic.semesterDisplay", "Fall 2025")
                    .containsEntry("schedule.raw", "MWF 10:00")
                    .containsEntry("department.name", "Computer Science")
                    .containsEntry("course.hasSyllabus", true)
                    .containsEntry("course.gradeScale", "A-F")
                    .containsEntry("course.hasCatalogDescription", true)
                    .containsEntry("course.learningObjectiveCount", 2)
                    .containsEntry("course.isApproved", true)
                    .containsEntry("course.approvedDate", APPROVED.toString());
        }

        @Test
        void sparseFullConstructorSkipsOptionalMetadata() {
            CourseCreatedEvent e = full().instructor(null, null).location(null).schedule(null)
                    .prerequisites(null).syllabus(null).gradeScale(null).catalogDescription(null)
                    .learningObjectives(null).approval(null, null).build();

            assertThat(e.getMetadata()).doesNotContainKeys("instructor.id", "course.location", "course.schedule",
                    "schedule.raw", "course.hasPrerequisites", "course.hasSyllabus", "course.gradeScale",
                    "course.hasCatalogDescription", "course.hasLearningObjectives", "course.isApproved");
        }

        @Test
        void basicConstructorRecordsOnlyCourseMetadata() {
            CourseCreatedEvent e = basic("101", CourseStatus.DRAFT);

            assertThat(e.getMetadata()).containsEntry("course.code", "CS101")
                    .doesNotContainKeys("instructor.id", "academic.semester", "department.code");
        }
    }

    @Nested
    class CopiesAndEquality {

        @Test
        void copiesKeepCoursePayload() {
            CourseCreatedEvent original = full().build();

            Event copy = original.withPriority(Priority.HIGH);

            assertThat(copy).isInstanceOf(CourseCreatedEvent.class);
            CourseCreatedEvent c = (CourseCreatedEvent) copy;
            assertThat(c.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(c.getEventId()).isEqualTo(original.getEventId());
            assertThat(c.getCourseLevel()).isEqualTo(original.getCourseLevel());
            assertThat(c.getLearningObjectives()).isEqualTo(original.getLearningObjectives());
            assertThat(c.getApprovedDate()).isEqualTo(APPROVED);
            assertThat(c.getMetadata()).isEqualTo(original.getMetadata());
            assertThat(c).isEqualTo(original).hasSameHashCodeAs(original);
        }

        @Test
        void distinctEventsAreNotEqual() {
            CourseCreatedEvent e = full().build();
            assertThat(e).isEqualTo(e).isNotEqualTo(full().build()).isNotEqualTo(null);
            assertThat(e.equals(new TestEvent("CourseCreated", "x"))).isFalse();
        }
    }
}
