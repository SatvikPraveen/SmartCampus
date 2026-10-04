package patterns;

import models.Department;
import models.Student;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentBuilderTest {

    private static Date date(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date yearsFromNow(int years) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, years);
        return cal.getTime();
    }

    /** A builder with every required field populated. */
    private static StudentBuilder valid() {
        return new StudentBuilder()
            .id("STU000001")
            .name("Ada Lovelace")
            .email("ada@university.edu")
            .departmentId("DEPT_CS")
            .enrollmentDate("2023-08-20");
    }

    @Nested
    class Build {

        @Test
        void buildsStudentFromRequiredFields() {
            Student s = valid().build();

            assertThat(s.getStudentId()).isEqualTo("STU000001");
            assertThat(s.getUserId()).isEqualTo("STU000001");
            assertThat(s.getFirstName()).isEqualTo("Ada");
            assertThat(s.getLastName()).isEqualTo("Lovelace");
            assertThat(s.getEmail()).isEqualTo("ada@university.edu");
            assertThat(s.getDepartmentId()).isEqualTo("DEPT_CS");
            assertThat(s.getEnrollmentDate()).isEqualTo(LocalDate.of(2023, 8, 20));
            assertThat(s.getAcademicYear()).isEqualTo(Student.AcademicYear.FRESHMAN);
            assertThat(s.getGpa()).isZero();
        }

        @Test
        void multiWordFirstNameKeepsLastWordAsSurname() {
            Student s = valid().name("Mary Ann Smith").build();

            assertThat(s.getFirstName()).isEqualTo("Mary Ann");
            assertThat(s.getLastName()).isEqualTo("Smith");
        }

        @Test
        void appliesOptionalPropertiesToStudent() {
            Department cs = new Department("DEPT_CS", "CS", "Computer Science", "desc", "Bldg");
            Student s = valid()
                .department(cs)
                .junior()
                .gpa(3.5)
                .phoneNumber("555-123-4567")
                .build();

            assertThat(s.getDepartmentId()).isEqualTo("DEPT_CS");
            assertThat(s.getMajor()).isEqualTo("Computer Science");
            assertThat(s.getAcademicYear()).isEqualTo(Student.AcademicYear.JUNIOR);
            assertThat(s.getGpa()).isEqualTo(3.5);
            assertThat(s.getPhoneNumber()).isEqualTo("555-123-4567");
        }

        @Test
        void explicitMajorOverridesDepartmentName() {
            Department cs = new Department("DEPT_CS", "CS", "Computer Science", "desc", "Bldg");

            assertThat(valid().department(cs).major("Data Science").build().getMajor()).isEqualTo("Data Science");
        }

        @Test
        void yearLevelShortcutsMapToAcademicYear() {
            assertThat(valid().freshman().build().getAcademicYear()).isEqualTo(Student.AcademicYear.FRESHMAN);
            assertThat(valid().sophomore().build().getAcademicYear()).isEqualTo(Student.AcademicYear.SOPHOMORE);
            assertThat(valid().junior().build().getAcademicYear()).isEqualTo(Student.AcademicYear.JUNIOR);
            assertThat(valid().senior().build().getAcademicYear()).isEqualTo(Student.AcademicYear.SENIOR);
            assertThat(valid().graduate().build().getAcademicYear()).isEqualTo(Student.AcademicYear.GRADUATE);
        }

        @Test
        void yearLevelWithoutAcademicYearFallsBackToFreshman() {
            Student s = valid().property("yearLevel", 6).build();

            assertThat(s.getAcademicYear()).isEqualTo(Student.AcademicYear.FRESHMAN);
        }

        @Test
        void informationalPropertiesDoNotBreakBuild() {
            Student s = valid()
                .minor("Math").address("1 Main St").emergencyContact("Bob", "555-000-1111")
                .nationality("UK").international().domestic()
                .scholarship("Merit", 5000).financialAid(true).partTime().fullTime()
                .active().inactive().graduated().suspended()
                .status(StudentBuilder.StudentStatus.TRANSFERRED)
                .properties(Map.of("club", "Chess"))
                .dateOfBirth("2000-01-01")
                .build();

            assertThat(s.getStudentId()).isEqualTo("STU000001");
        }
    }

    @Nested
    class Validation {

        @Test
        void reportsAllMissingRequiredFields() {
            assertThatThrownBy(() -> new StudentBuilder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Student ID is required")
                .hasMessageContaining("Student name is required")
                .hasMessageContaining("Student email is required")
                .hasMessageContaining("Student department is required");
        }

        @Test
        void rejectsMalformedEmailAndSingleWordName() {
            assertThatThrownBy(() -> valid().email("not-an-email").name("Madonna").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email format is invalid")
                .hasMessageContaining("first and last name");
        }

        @Test
        void rejectsFutureOrMissingEnrollmentDate() {
            assertThatThrownBy(() -> valid().enrollmentDate(yearsFromNow(1)).build())
                .hasMessageContaining("Enrollment date cannot be in the future");
            assertThatThrownBy(() -> valid().enrollmentDate((Date) null).build())
                .hasMessageContaining("Enrollment date is required");
        }

        @Test
        void rejectsInvalidAdditionalProperties() {
            assertThatThrownBy(() -> valid().property("gpa", 4.5).property("yearLevel", 7)
                    .phoneNumber("12").dateOfBirth(yearsFromNow(1)).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GPA must be between 0.0 and 4.0")
                .hasMessageContaining("Year level must be between 1 and 6")
                .hasMessageContaining("Phone number format is invalid")
                .hasMessageContaining("Date of birth cannot be in the future")
                .hasMessageContaining("at least 16 years old");
        }

        @Test
        void rejectsStudentYoungerThanSixteen() {
            assertThatThrownBy(() -> valid().dateOfBirth(yearsFromNow(-10)).build())
                .hasMessageContaining("at least 16 years old")
                .hasMessageNotContaining("cannot be in the future");
        }

        @Test
        void gpaSetterRejectsOutOfRangeImmediately() {
            assertThatThrownBy(() -> new StudentBuilder().gpa(-0.1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new StudentBuilder().gpa(4.01)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void dateParsersRejectBadFormat() {
            assertThatThrownBy(() -> new StudentBuilder().enrollmentDate("20/08/2023"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("yyyy-MM-dd");
            assertThatThrownBy(() -> new StudentBuilder().dateOfBirth("yesterday"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("yyyy-MM-dd");
        }

        @Test
        void validationCanBeDisabled() {
            Student s = new StudentBuilder().id("X").name("Solo").email("solo@u.edu").validateOnBuild(false).build();

            assertThat(s.getFirstName()).isEqualTo("Solo");
            assertThat(s.getLastName()).isEqualTo("Solo");
            assertThat(s.getDepartmentId()).isNull();
        }
    }

    @Nested
    class DepartmentSelection {

        @Test
        void departmentIdIgnoresBlankAndClearsName() {
            Department cs = new Department("DEPT_CS", "CS", "Computer Science", "desc", "Bldg");
            StudentBuilder b = valid().department(cs).departmentId("  ");

            assertThat(b.getSummary()).contains("Department: Computer Science");

            b.departmentId("DEPT_MATH");
            assertThat(b.getSummary()).contains("Department: DEPT_MATH");
        }

        @Test
        void nullDepartmentClearsDepartment() {
            StudentBuilder b = valid().department(null);

            assertThat(b.isComplete()).isFalse();
            assertThat(b.getSummary()).contains("Department: Not set");
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void isCompleteTracksRequiredFields() {
            assertThat(new StudentBuilder().isComplete()).isFalse();
            assertThat(valid().isComplete()).isTrue();
            assertThat(valid().enrollmentDate((Date) null).isComplete()).isFalse();
        }

        @Test
        void summaryDescribesState() {
            String summary = valid().gpa(3.0).getSummary();

            assertThat(summary)
                .contains("ID: STU000001")
                .contains("Name: Ada Lovelace")
                .contains("Email: ada@university.edu")
                .contains("Additional Properties: 1 items")
                .contains("Complete: true");
            assertThat(new StudentBuilder().getSummary())
                .contains("ID: Not set").contains("Name: Not set").contains("Email: Not set")
                .contains("Complete: false");
        }

        @Test
        void resetClearsEverythingAndReenablesValidation() {
            StudentBuilder b = valid().gpa(3.0).validateOnBuild(false).reset();

            assertThat(b.isComplete()).isFalse();
            assertThat(b.getSummary()).contains("Additional Properties: 0 items").contains("ID: Not set");
            assertThatThrownBy(b::build).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void fromCopiesCoreFieldsOfExistingStudent() {
            Student original = valid().build();

            Student copy = StudentBuilder.from(original).build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getStudentId()).isEqualTo(original.getStudentId());
            assertThat(copy.getFullName()).isEqualTo(original.getFullName());
            assertThat(copy.getEmail()).isEqualTo(original.getEmail());
            assertThat(copy.getDepartmentId()).isEqualTo(original.getDepartmentId());
            assertThat(copy.getEnrollmentDate()).isEqualTo(original.getEnrollmentDate());
        }

        @Test
        void buildMultipleCreatesDistinctVariations() {
            List<Student> students = valid().sophomore().buildMultiple(3);

            assertThat(students).extracting(Student::getStudentId)
                .containsExactly("STU000001_1", "STU000001_2", "STU000001_3");
            assertThat(students).extracting(Student::getEmail)
                .containsExactly("ada+1@university.edu", "ada+2@university.edu", "ada+3@university.edu");
            assertThat(students).allSatisfy(s -> {
                assertThat(s.getAcademicYear()).isEqualTo(Student.AcademicYear.SOPHOMORE);
                assertThat(s.getDepartmentId()).isEqualTo("DEPT_CS");
                assertThat(s.getEnrollmentDate()).isEqualTo(LocalDate.of(2023, 8, 20));
            });
            assertThat(valid().buildMultiple(0)).isEmpty();
        }

        @Test
        void enrollmentDateAcceptsDateObject() {
            assertThat(valid().enrollmentDate(date(2022, 1, 15)).build().getEnrollmentDate())
                .isEqualTo(LocalDate.of(2022, 1, 15));
        }
    }

    @Nested
    class TestData {

        // Regression: withTestData() generated a 7-digit phone ("555-1234") that build()'s own phone
        // validation rejects, so test-data students could never be built.
        @RepeatedTest(20)
        void testDataBuildsOnceDepartmentIsSupplied() {
            Student s = StudentBuilder.withTestData().departmentId("DEPT_CS").build();

            assertThat(s.getStudentId()).matches("STU\\d{6}");
            assertThat(s.getEmail()).endsWith("@university.edu");
            assertThat(s.getGpa()).isBetween(2.0, 4.0);
            assertThat(s.getPhoneNumber()).matches("555-555-\\d{4}");
        }
    }
}
