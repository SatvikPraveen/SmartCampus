package patterns;

import models.Course;
import models.Department;
import models.Professor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseBuilderTest {

    /** A builder with every required field populated. */
    private static CourseBuilder valid() {
        return new CourseBuilder()
            .courseCode("CS101")
            .name("Intro to Programming")
            .description("Basics")
            .departmentId("DEPT_CS")
            .professorId("PROF00001")
            .fall()
            .year(2025);
    }

    private static Professor professor() {
        return new Professor("PROF00001", "Alan", "Turing", "alan@university.edu", null, "PROF00001",
                             "DEPT_CS", Professor.AcademicRank.ASSISTANT, "Computation");
    }

    @Nested
    class Build {

        @Test
        void buildsCourseWithDefaults() {
            Course c = valid().build();

            assertThat(c.getCourseId()).isEqualTo("CS101");
            assertThat(c.getCourseCode()).isEqualTo("CS101");
            assertThat(c.getCourseName()).isEqualTo("Intro to Programming");
            assertThat(c.getDescription()).isEqualTo("Basics");
            assertThat(c.getCredits()).isEqualTo(3);
            assertThat(c.getMaxEnrollment()).isEqualTo(30);
            assertThat(c.getDepartmentId()).isEqualTo("DEPT_CS");
            assertThat(c.getProfessorId()).isEqualTo("PROF00001");
            assertThat(c.getSemester()).isEqualTo("Fall");
            assertThat(c.getYear()).isEqualTo(2025);
            assertThat(c.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.BEGINNER);
        }

        @Test
        void appliesCreditsCapacityAndDifficulty() {
            Course c = valid().credits(4).capacity(55).advanced().build();

            assertThat(c.getCredits()).isEqualTo(4);
            assertThat(c.getMaxEnrollment()).isEqualTo(55);
            assertThat(c.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.ADVANCED);
            assertThat(valid().intermediate().build().getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.INTERMEDIATE);
            assertThat(valid().beginner().build().getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.BEGINNER);
            assertThat(valid().difficulty(CourseBuilder.DifficultyLevel.EXPERT).build().getDifficultyLevel())
                .isEqualTo(Course.DifficultyLevel.EXPERT);
        }

        @Test
        void semesterShortcutsSetSemester() {
            assertThat(valid().spring().build().getSemester()).isEqualTo("Spring");
            assertThat(valid().summer().build().getSemester()).isEqualTo("Summer");
            assertThat(valid().winter().build().getSemester()).isEqualTo("Winter");
            assertThat(valid().semester("Intersession").build().getSemester()).isEqualTo("Intersession");
        }

        @Test
        void defaultsToCurrentSemesterAndYear() {
            int year = Calendar.getInstance().get(Calendar.YEAR);
            Course c = new CourseBuilder().courseCode("CS1").name("N").departmentId("D").professorId("P").build();

            assertThat(c.getYear()).isEqualTo(year);
            assertThat(c.getSemester()).isIn("Fall", "Spring", "Summer");
            assertThat(valid().year(1999).currentYear().build().getYear()).isEqualTo(year);
        }

        @Test
        void classSizePresetsStayInDocumentedRanges() {
            for (int i = 0; i < 20; i++) {
                assertThat(valid().smallClass().build().getMaxEnrollment()).isBetween(10, 15);
                assertThat(valid().mediumClass().build().getMaxEnrollment()).isBetween(20, 35);
                assertThat(valid().largeClass().build().getMaxEnrollment()).isBetween(50, 100);
            }
        }

        @Test
        void descriptiveAttributesAreAcceptedWithValidValues() {
            Course c = valid()
                .undergraduate().graduate().doctoral().level(CourseBuilder.CourseLevel.UNDERGRADUATE)
                .lecture().laboratory().seminar().workshop().online().hybrid()
                .type(CourseBuilder.CourseType.INDEPENDENT_STUDY)
                .schedule("MWF 10:00-11:00").room("B-101")
                .prerequisites(List.of("CS100")).prerequisite("MATH100")
                .objectives(List.of("Learn")).objective("Practice")
                .textbook("SICP", "Abelson", "0262510871")
                .standardGrading()
                .tags("intro", "programming")
                .active().inactive().cancelled()
                .properties(Map.of("lab", true))
                .build();

            assertThat(c.getCourseCode()).isEqualTo("CS101");
        }

        @Test
        void professorAndDepartmentObjectsPopulateIdsAndNames() {
            Department cs = new Department("DEPT_CS", "CS", "Computer Science", "desc", "Bldg");
            CourseBuilder b = valid().department(cs).professor(professor());

            assertThat(b.getSummary())
                .contains("Department: Computer Science")
                .contains("Professor: Alan Turing");
            Course c = b.build();
            assertThat(c.getDepartmentId()).isEqualTo("DEPT_CS");
            assertThat(c.getProfessorId()).isEqualTo("PROF00001");
        }

        @Test
        void idSettersIgnoreBlankAndReplaceDisplayName() {
            CourseBuilder b = valid().professor(professor()).professorId(" ").departmentId(null);
            assertThat(b.getSummary()).contains("Professor: Alan Turing").contains("Department: DEPT_CS");

            b.professorId("PROF00002");
            assertThat(b.getSummary()).contains("Professor: PROF00002");

            b.professor(null).department(null);
            assertThat(b.getSummary()).contains("Professor: Not set").contains("Department: Not set");
        }
    }

    @Nested
    class Validation {

        @Test
        void reportsAllMissingRequiredFields() {
            assertThatThrownBy(() -> new CourseBuilder().semester("").year(0).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Course code is required")
                .hasMessageContaining("Course name is required")
                .hasMessageContaining("Department is required")
                .hasMessageContaining("Professor is required")
                .hasMessageContaining("Semester is required")
                .hasMessageContaining("Year is required");
        }

        @Test
        void gradingSchemeMustTotalOneHundred() {
            assertThatThrownBy(() -> valid().gradingScheme(Map.of("Exams", 50.0, "Labs", 40.0)).build())
                .hasMessageContaining("Grading scheme must total 100%");
            assertThat(valid().gradingScheme(Map.of("Exams", 50.0, "Labs", 50.0)).build()).isNotNull();
        }

        @Test
        void scheduleMustMatchDayTimeFormat() {
            assertThatThrownBy(() -> valid().schedule("Mondays at ten").build())
                .hasMessageContaining("Schedule format");
            assertThat(valid().schedule("TR 9:30-10:45").build()).isNotNull();
        }

        @Test
        void settersRejectOutOfRangeValues() {
            assertThatThrownBy(() -> new CourseBuilder().credits(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CourseBuilder().credits(7)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CourseBuilder().capacity(0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void validationCanBeDisabledForPartialCourses() {
            Course c = new CourseBuilder().courseCode("X1").name("Draft").validateOnBuild(false).build();

            assertThat(c.getCourseCode()).isEqualTo("X1");
            assertThat(c.getDepartmentId()).isNull();
            assertThat(c.getProfessorId()).isNull();
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void isCompleteTracksRequiredFields() {
            assertThat(new CourseBuilder().isComplete()).isFalse();
            assertThat(valid().isComplete()).isTrue();
            assertThat(valid().year(0).isComplete()).isFalse();
        }

        @Test
        void summaryDescribesState() {
            assertThat(valid().credits(4).capacity(25).tags("a").getSummary())
                .contains("Course Code: CS101")
                .contains("Name: Intro to Programming")
                .contains("Credits: 4")
                .contains("Semester: Fall 2025")
                .contains("Capacity: 25")
                .contains("Additional Properties: 1 items")
                .contains("Complete: true");
            assertThat(new CourseBuilder().getSummary()).contains("Course Code: Not set").contains("Name: Not set");
        }

        @Test
        void resetRestoresDefaults() {
            CourseBuilder b = valid().credits(5).capacity(99).tags("x").validateOnBuild(false).reset();

            assertThat(b.isComplete()).isFalse();
            assertThat(b.getSummary())
                .contains("Credits: 3").contains("Capacity: 30").contains("Additional Properties: 0 items");
            assertThatThrownBy(b::build).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void fromCopiesExistingCourse() {
            Course original = valid().credits(4).capacity(40).advanced().build();

            Course copy = CourseBuilder.from(original).build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getCourseCode()).isEqualTo(original.getCourseCode());
            assertThat(copy.getCourseName()).isEqualTo(original.getCourseName());
            assertThat(copy.getDescription()).isEqualTo(original.getDescription());
            assertThat(copy.getCredits()).isEqualTo(4);
            assertThat(copy.getMaxEnrollment()).isEqualTo(40);
            assertThat(copy.getDepartmentId()).isEqualTo(original.getDepartmentId());
            assertThat(copy.getProfessorId()).isEqualTo(original.getProfessorId());
            assertThat(copy.getSemester()).isEqualTo(original.getSemester());
            assertThat(copy.getYear()).isEqualTo(original.getYear());
            assertThat(copy.getDifficultyLevel()).isEqualTo(Course.DifficultyLevel.ADVANCED);
        }

        @Test
        void buildMultipleCreatesNumberedSections() {
            List<Course> sections = valid().capacity(20).buildMultiple(3);

            assertThat(sections).extracting(Course::getCourseCode).containsExactly("CS101-01", "CS101-02", "CS101-03");
            assertThat(sections).extracting(Course::getCourseName).containsExactly(
                "Intro to Programming (Section 1)", "Intro to Programming (Section 2)", "Intro to Programming (Section 3)");
            assertThat(sections).allSatisfy(c -> {
                assertThat(c.getMaxEnrollment()).isEqualTo(20);
                assertThat(c.getProfessorId()).isEqualTo("PROF00001");
            });
        }
    }

    @Nested
    class TestData {

        @RepeatedTest(10)
        void testDataBuildsOnceDepartmentAndProfessorAreSupplied() {
            Course c = CourseBuilder.withTestData().departmentId("DEPT_CS").professorId("PROF00001").build();

            assertThat(c.getCourseCode()).matches("CS[1-4]\\d\\d");
            assertThat(c.getCredits()).isBetween(3, 4);
            assertThat(c.getMaxEnrollment()).isBetween(20, 50);
            assertThat(c.getDescription()).startsWith("A comprehensive course covering ");
        }
    }
}
