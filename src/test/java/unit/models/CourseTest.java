// File location: src/test/java/unit/models/CourseTest.java

package com.smartcampus.test.unit.models;

import com.smartcampus.models.Course;
import com.smartcampus.models.Professor;
import com.smartcampus.models.Department;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.enums.Semester;
import com.smartcampus.models.enums.CourseStatus;
import com.smartcampus.models.enums.CourseLevel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for the Course model class
 * Tests course entity creation, validation, and business logic
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Course Model Tests")
class CourseTest {

    private Course course;
    private Professor professor;
    private Department department;
    private List<Enrollment> enrollments;

    @BeforeEach
    void setUp() {
        // Set up test department
        department = new Department();
        department.setId(1L);
        department.setCode("CS");
        department.setName("Computer Science");
        department.setDescription("Department of Computer Science");

        // Set up test professor
        professor = new Professor();
        professor.setId(1L);
        professor.setEmployeeId("EMP2024001");

        // Set up test enrollments
        enrollments = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Enrollment enrollment = new Enrollment();
            enrollment.setId((long) i);
            enrollments.add(enrollment);
        }

        // Set up test course
        course = new Course();
        course.setId(1L);
        course.setCourseCode("CS101");
        course.setCourseName("Introduction to Programming");
        course.setDescription("An introductory course covering fundamental programming concepts");
        course.setDepartment(department);
        course.setProfessor(professor);
        course.setCredits(3);
        course.setMaxEnrollment(30);
        course.setCurrentEnrollment(15);
        course.setSemester(Semester.FALL);
        course.setYear(2024);
        course.setStartDate(LocalDate.of(2024, 8, 26));
        course.setEndDate(LocalDate.of(2024, 12, 15));
        course.setScheduleDays("MWF");
        course.setStartTime(LocalTime.of(9, 0));
        course.setEndTime(LocalTime.of(9, 50));
        course.setClassroom("Room 101");
        course.setBuilding("Computer Science Building");
        course.setPrerequisites("None");
        course.setCourseLevel(CourseLevel.UNDERGRADUATE);
        course.setStatus(CourseStatus.ACTIVE);
        course.setSyllabusUrl("https://smartcampus.edu/syllabus/cs101-fall2024");
        course.setMaterialsRequired("Textbook: Introduction to Programming (5th Ed), Laptop");
        course.setGradingPolicy("Homework: 30%, Midterm: 30%, Final: 40%");
        course.setAttendancePolicy("Attendance is required for all classes");
        course.setEnrollments(enrollments);
    }

    @Nested
    @DisplayName("Course Creation Tests")
    class CourseCreationTests {

        @Test
        @DisplayName("Should create course with valid data")
        void shouldCreateCourseWithValidData() {
            assertThat(course).isNotNull();
            assertThat(course.getCourseCode()).isEqualTo("CS101");
            assertThat(course.getCourseName()).isEqualTo("Introduction to Programming");
            assertThat(course.getDepartment()).isEqualTo(department);
            assertThat(course.getProfessor()).isEqualTo(professor);
            assertThat(course.getCredits()).isEqualTo(3);
            assertThat(course.getSemester()).isEqualTo(Semester.FALL);
            assertThat(course.getYear()).isEqualTo(2024);
            assertThat(course.getStatus()).isEqualTo(CourseStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should create course with minimal required fields")
        void shouldCreateCourseWithMinimalFields() {
            Course minimalCourse = new Course();
            minimalCourse.setCourseCode("MIN101");
            minimalCourse.setCourseName("Minimal Course");
            minimalCourse.setDepartment(department);
            minimalCourse.setProfessor(professor);
            minimalCourse.setCredits(1);
            minimalCourse.setSemester(Semester.SPRING);
            minimalCourse.setYear(2024);
            minimalCourse.setStatus(CourseStatus.ACTIVE);

            assertThat(minimalCourse.getCourseCode()).isEqualTo("MIN101");
            assertThat(minimalCourse.getCourseName()).isEqualTo("Minimal Course");
            assertThat(minimalCourse.getDepartment()).isEqualTo(department);
            assertThat(minimalCourse.getProfessor()).isEqualTo(professor);
            assertThat(minimalCourse.getCredits()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null professor gracefully")
        void shouldHandleNullProfessor() {
            course.setProfessor(null);
            assertThat(course.getProfessor()).isNull();
        }

        @Test
        @DisplayName("Should handle null department gracefully")
        void shouldHandleNullDepartment() {
            course.setDepartment(null);
            assertThat(course.getDepartment()).isNull();
        }
    }

    @Nested
    @DisplayName("Course Code Validation Tests")
    class CourseCodeValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid course codes")
        @ValueSource(strings = {"CS101", "MATH201", "ENG301", "PHY101L", "BIO499"})
        void shouldAcceptValidCourseCodes(String courseCode) {
            course.setCourseCode(courseCode);
            assertThat(course.getCourseCode()).isEqualTo(courseCode);
        }

        @Test
        @DisplayName("Should handle empty course code")
        void shouldHandleEmptyCourseCode() {
            course.setCourseCode("");
            assertThat(course.getCourseCode()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null course code")
        void shouldHandleNullCourseCode() {
            course.setCourseCode(null);
            assertThat(course.getCourseCode()).isNull();
        }

        @Test
        @DisplayName("Should extract department prefix from course code")
        void shouldExtractDepartmentPrefixFromCourseCode() {
            course.setCourseCode("CS101");
            assertThat(course.getDepartmentPrefix()).isEqualTo("CS");

            course.setCourseCode("MATH201");
            assertThat(course.getDepartmentPrefix()).isEqualTo("MATH");

            course.setCourseCode("101");
            assertThat(course.getDepartmentPrefix()).isEqualTo("");
        }

        @Test
        @DisplayName("Should extract course number from course code")
        void shouldExtractCourseNumberFromCourseCode() {
            course.setCourseCode("CS101");
            assertThat(course.getCourseNumber()).isEqualTo("101");

            course.setCourseCode("MATH201L");
            assertThat(course.getCourseNumber()).isEqualTo("201L");

            course.setCourseCode("CS");
            assertThat(course.getCourseNumber()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Enrollment Management Tests")
    class EnrollmentManagementTests {

        @Test
        @DisplayName("Should manage enrollment capacity")
        void shouldManageEnrollmentCapacity() {
            assertThat(course.getCurrentEnrollment()).isEqualTo(15);
            assertThat(course.getMaxEnrollment()).isEqualTo(30);
            assertThat(course.getAvailableSpots()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should check if course is full")
        void shouldCheckIfCourseIsFull() {
            assertFalse(course.isFull());

            course.setCurrentEnrollment(30);
            assertTrue(course.isFull());

            course.setCurrentEnrollment(35);
            assertTrue(course.isFull());
        }

        @Test
        @DisplayName("Should check if course has available spots")
        void shouldCheckIfCourseHasAvailableSpots() {
            assertTrue(course.hasAvailableSpots());

            course.setCurrentEnrollment(30);
            assertFalse(course.hasAvailableSpots());
        }

        @Test
        @DisplayName("Should calculate enrollment percentage")
        void shouldCalculateEnrollmentPercentage() {
            double expected = (15.0 / 30.0) * 100;
            assertThat(course.getEnrollmentPercentage()).isEqualTo(expected, within(0.01));
        }

        @Test
        @DisplayName("Should handle zero max enrollment")
        void shouldHandleZeroMaxEnrollment() {
            course.setMaxEnrollment(0);
            assertThat(course.getEnrollmentPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should increment enrollment count")
        void shouldIncrementEnrollmentCount() {
            int initialCount = course.getCurrentEnrollment();
            course.incrementEnrollment();
            assertThat(course.getCurrentEnrollment()).isEqualTo(initialCount + 1);
        }

        @Test
        @DisplayName("Should decrement enrollment count")
        void shouldDecrementEnrollmentCount() {
            int initialCount = course.getCurrentEnrollment();
            course.decrementEnrollment();
            assertThat(course.getCurrentEnrollment()).isEqualTo(initialCount - 1);
        }

        @Test
        @DisplayName("Should not decrement below zero")
        void shouldNotDecrementBelowZero() {
            course.setCurrentEnrollment(0);
            course.decrementEnrollment();
            assertThat(course.getCurrentEnrollment()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Credits and Level Tests")
    class CreditsAndLevelTests {

        @ParameterizedTest
        @DisplayName("Should accept valid credit amounts")
        @ValueSource(ints = {1, 2, 3, 4, 5, 6})
        void shouldAcceptValidCreditAmounts(int credits) {
            course.setCredits(credits);
            assertThat(course.getCredits()).isEqualTo(credits);
        }

        @ParameterizedTest
        @DisplayName("Should accept all valid course levels")
        @EnumSource(CourseLevel.class)
        void shouldAcceptAllValidCourseLevels(CourseLevel level) {
            course.setCourseLevel(level);
            assertThat(course.getCourseLevel()).isEqualTo(level);
        }

        @Test
        @DisplayName("Should determine if course is graduate level")
        void shouldDetermineIfCourseIsGraduateLevel() {
            course.setCourseLevel(CourseLevel.UNDERGRADUATE);
            assertFalse(course.isGraduateLevel());

            course.setCourseLevel(CourseLevel.GRADUATE);
            assertTrue(course.isGraduateLevel());
        }

        @Test
        @DisplayName("Should determine course difficulty level")
        void shouldDetermineCourseDifficultyLevel() {
            course.setCourseCode("CS101");
            assertThat(course.getDifficultyLevel()).isEqualTo("Introductory");

            course.setCourseCode("CS201");
            assertThat(course.getDifficultyLevel()).isEqualTo("Intermediate");

            course.setCourseCode("CS301");
            assertThat(course.getDifficultyLevel()).isEqualTo("Advanced");

            course.setCourseCode("CS499");
            assertThat(course.getDifficultyLevel()).isEqualTo("Advanced");

            course.setCourseCode("CS");
            assertThat(course.getDifficultyLevel()).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("Semester and Status Tests")
    class SemesterAndStatusTests {

        @ParameterizedTest
        @DisplayName("Should accept all valid semesters")
        @EnumSource(Semester.class)
        void shouldAcceptAllValidSemesters(Semester semester) {
            course.setSemester(semester);
            assertThat(course.getSemester()).isEqualTo(semester);
        }

        @ParameterizedTest
        @DisplayName("Should accept all valid course statuses")
        @EnumSource(CourseStatus.class)
        void shouldAcceptAllValidCourseStatuses(CourseStatus status) {
            course.setStatus(status);
            assertThat(course.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("Should check if course is active")
        void shouldCheckIfCourseIsActive() {
            course.setStatus(CourseStatus.ACTIVE);
            assertTrue(course.isActive());

            course.setStatus(CourseStatus.INACTIVE);
            assertFalse(course.isActive());

            course.setStatus(CourseStatus.CANCELLED);
            assertFalse(course.isActive());

            course.setStatus(CourseStatus.COMPLETED);
            assertFalse(course.isActive());
        }

        @Test
        @DisplayName("Should check if course accepts enrollments")
        void shouldCheckIfCourseAcceptsEnrollments() {
            course.setStatus(CourseStatus.ACTIVE);
            assertTrue(course.acceptsEnrollments());

            course.setStatus(CourseStatus.CANCELLED);
            assertFalse(course.acceptsEnrollments());

            course.setStatus(CourseStatus.COMPLETED);
            assertFalse(course.acceptsEnrollments());
        }

        @Test
        @DisplayName("Should generate semester display string")
        void shouldGenerateSemesterDisplayString() {
            course.setSemester(Semester.FALL);
            course.setYear(2024);
            assertThat(course.getSemesterDisplay()).isEqualTo("Fall 2024");

            course.setSemester(Semester.SPRING);
            course.setYear(2025);
            assertThat(course.getSemesterDisplay()).isEqualTo("Spring 2025");

            course.setSemester(Semester.SUMMER);
            course.setYear(2024);
            assertThat(course.getSemesterDisplay()).isEqualTo("Summer 2024");
        }
    }

    @Nested
    @DisplayName("Schedule and Location Tests")
    class ScheduleAndLocationTests {

        @Test
        @DisplayName("Should handle schedule days")
        void shouldHandleScheduleDays() {
            course.setScheduleDays("TTh");
            assertThat(course.getScheduleDays()).isEqualTo("TTh");
        }

        @Test
        @DisplayName("Should format schedule display")
        void shouldFormatScheduleDisplay() {
            course.setScheduleDays("MWF");
            course.setStartTime(LocalTime.of(10, 30));
            course.setEndTime(LocalTime.of(11, 45));
            
            String expectedSchedule = "MWF 10:30-11:45";
            assertThat(course.getScheduleDisplay()).isEqualTo(expectedSchedule);
        }

        @Test
        @DisplayName("Should handle location information")
        void shouldHandleLocationInformation() {
            course.setBuilding("Science Hall");
            course.setClassroom("Room 205");
            
            assertThat(course.getBuilding()).isEqualTo("Science Hall");
            assertThat(course.getClassroom()).isEqualTo("Room 205");
        }

        @Test
        @DisplayName("Should format full location")
        void shouldFormatFullLocation() {
            String expectedLocation = "Computer Science Building, Room 101";
            assertThat(course.getFullLocation()).isEqualTo(expectedLocation);
        }

        @Test
        @DisplayName("Should calculate course duration in minutes")
        void shouldCalculateCourseDurationInMinutes() {
            course.setStartTime(LocalTime.of(9, 0));
            course.setEndTime(LocalTime.of(10, 15));
            
            assertThat(course.getDurationMinutes()).isEqualTo(75);
        }

        @Test
        @DisplayName("Should handle same start and end time")
        void shouldHandleSameStartAndEndTime() {
            LocalTime time = LocalTime.of(9, 0);
            course.setStartTime(time);
            course.setEndTime(time);
            
            assertThat(course.getDurationMinutes()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Date Management Tests")
    class DateManagementTests {

        @Test
        @DisplayName("Should validate date range")
        void shouldValidateDateRange() {
            LocalDate startDate = LocalDate.of(2024, 8, 26);
            LocalDate endDate = LocalDate.of(2024, 12, 15);
            
            course.setStartDate(startDate);
            course.setEndDate(endDate);
            
            assertTrue(course.isValidDateRange());
        }

        @Test
        @DisplayName("Should detect invalid date range")
        void shouldDetectInvalidDateRange() {
            LocalDate startDate = LocalDate.of(2024, 12, 15);
            LocalDate endDate = LocalDate.of(2024, 8, 26);
            
            course.setStartDate(startDate);
            course.setEndDate(endDate);
            
            assertFalse(course.isValidDateRange());
        }

        @Test
        @DisplayName("Should check if course is currently running")
        void shouldCheckIfCourseIsCurrentlyRunning() {
            LocalDate today = LocalDate.now();
            course.setStartDate(today.minusDays(10));
            course.setEndDate(today.plusDays(10));
            course.setStatus(CourseStatus.ACTIVE);
            
            assertTrue(course.isCurrentlyRunning());
        }

        @Test
        @DisplayName("Should check if course has ended")
        void shouldCheckIfCourseHasEnded() {
            LocalDate today = LocalDate.now();
            course.setEndDate(today.minusDays(1));
            
            assertTrue(course.hasEnded());
            
            course.setEndDate(today.plusDays(1));
            assertFalse(course.hasEnded());
        }

        @Test
        @DisplayName("Should calculate course duration in weeks")
        void shouldCalculateCourseDurationInWeeks() {
            course.setStartDate(LocalDate.of(2024, 8, 26));
            course.setEndDate(LocalDate.of(2024, 12, 15));
            
            long weeks = course.getDurationWeeks();
            assertThat(weeks).isEqualTo(16);
        }
    }

    @Nested
    @DisplayName("Prerequisites and Requirements Tests")
    class PrerequisitesAndRequirementsTests {

        @Test
        @DisplayName("Should handle prerequisites")
        void shouldHandlePrerequisites() {
            course.setPrerequisites("CS100 or equivalent programming experience");
            assertThat(course.getPrerequisites()).isEqualTo("CS100 or equivalent programming experience");
        }

        @Test
        @DisplayName("Should check if course has prerequisites")
        void shouldCheckIfCourseHasPrerequisites() {
            course.setPrerequisites("CS100, MATH101");
            assertTrue(course.hasPrerequisites());

            course.setPrerequisites("None");
            assertFalse(course.hasPrerequisites());

            course.setPrerequisites("");
            assertFalse(course.hasPrerequisites());

            course.setPrerequisites(null);
            assertFalse(course.hasPrerequisites());
        }

        @Test
        @DisplayName("Should handle materials required")
        void shouldHandleMaterialsRequired() {
            String materials = "Textbook, Calculator, Lab Kit";
            course.setMaterialsRequired(materials);
            assertThat(course.getMaterialsRequired()).isEqualTo(materials);
        }

        @Test
        @DisplayName("Should check if materials are required")
        void shouldCheckIfMaterialsAreRequired() {
            course.setMaterialsRequired("Textbook required");
            assertTrue(course.requiresMaterials());

            course.setMaterialsRequired("");
            assertFalse(course.requiresMaterials());

            course.setMaterialsRequired(null);
            assertFalse(course.requiresMaterials());
        }
    }

    @Nested
    @DisplayName("Policy Information Tests")
    class PolicyInformationTests {

        @Test
        @DisplayName("Should handle grading policy")
        void shouldHandleGradingPolicy() {
            String gradingPolicy = "Midterm: 40%, Final: 40%, Homework: 20%";
            course.setGradingPolicy(gradingPolicy);
            assertThat(course.getGradingPolicy()).isEqualTo(gradingPolicy);
        }

        @Test
        @DisplayName("Should handle attendance policy")
        void shouldHandleAttendancePolicy() {
            String attendancePolicy = "Attendance mandatory, 3 absences = automatic fail";
            course.setAttendancePolicy(attendancePolicy);
            assertThat(course.getAttendancePolicy()).isEqualTo(attendancePolicy);
        }

        @Test
        @DisplayName("Should handle syllabus URL")
        void shouldHandleSyllabusUrl() {
            String syllabusUrl = "https://smartcampus.edu/syllabus/cs101-spring2024";
            course.setSyllabusUrl(syllabusUrl);
            assertThat(course.getSyllabusUrl()).isEqualTo(syllabusUrl);
        }

        @Test
        @DisplayName("Should check if syllabus is available")
        void shouldCheckIfSyllabusIsAvailable() {
            assertTrue(course.hasSyllabus());

            course.setSyllabusUrl("");
            assertFalse(course.hasSyllabus());

            course.setSyllabusUrl(null);
            assertFalse(course.hasSyllabus());
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when course codes, semester, and year match")
        void shouldBeEqualWhenIdentifiersMatch() {
            Course course1 = new Course();
            course1.setCourseCode("CS101");
            course1.setSemester(Semester.FALL);
            course1.setYear(2024);

            Course course2 = new Course();
            course2.setCourseCode("CS101");
            course2.setSemester(Semester.FALL);
            course2.setYear(2024);

            assertThat(course1).isEqualTo(course2);
            assertThat(course1.hashCode()).isEqualTo(course2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when identifiers differ")
        void shouldNotBeEqualWhenIdentifiersDiffer() {
            Course course1 = new Course();
            course1.setCourseCode("CS101");
            course1.setSemester(Semester.FALL);
            course1.setYear(2024);

            Course course2 = new Course();
            course2.setCourseCode("CS102");
            course2.setSemester(Semester.FALL);
            course2.setYear(2024);

            assertThat(course1).isNotEqualTo(course2);
        }

        @Test
        @DisplayName("Should handle null comparisons")
        void shouldHandleNullComparisons() {
            assertThat(course).isNotEqualTo(null);
            assertThat(course).isNotEqualTo("not a course");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            String courseString = course.toString();
            
            assertThat(courseString).contains("CS101");
            assertThat(courseString).contains("Introduction to Programming");
            assertThat(courseString).contains("Fall 2024");
        }

        @Test
        @DisplayName("Should provide display name")
        void shouldProvideDisplayName() {
            String displayName = course.getDisplayName();
            assertThat(displayName).isEqualTo("CS101 - Introduction to Programming");
        }

        @Test
        @DisplayName("Should provide full course description")
        void shouldProvideFullCourseDescription() {
            String fullDescription = course.getFullDescription();
            assertThat(fullDescription).contains("CS101");
            assertThat(fullDescription).contains("Introduction to Programming");
            assertThat(fullDescription).contains("3 credits");
            assertThat(fullDescription).contains("Fall 2024");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            assertTrue(course.isValidForOffering());

            course.setCourseCode(null);
            assertFalse(course.isValidForOffering());

            course.setCourseCode("CS101");
            course.setCourseName(null);
            assertFalse(course.isValidForOffering());

            course.setCourseName("Introduction to Programming");
            course.setProfessor(null);
            assertFalse(course.isValidForOffering());

            course.setProfessor(professor);
            course.setDepartment(null);
            assertFalse(course.isValidForOffering());
        }

        @Test
        @DisplayName("Should validate credits range")
        void shouldValidateCreditsRange() {
            assertTrue(course.isValidCredits(3));
            assertTrue(course.isValidCredits(1));
            assertTrue(course.isValidCredits(6));
            
            assertFalse(course.isValidCredits(0));
            assertFalse(course.isValidCredits(7));
            assertFalse(course.isValidCredits(-1));
        }

        @Test
        @DisplayName("Should validate enrollment capacity")
        void shouldValidateEnrollmentCapacity() {
            assertTrue(course.isValidEnrollmentCapacity());

            course.setCurrentEnrollment(-1);
            assertFalse(course.isValidEnrollmentCapacity());

            course.setCurrentEnrollment(35);
            course.setMaxEnrollment(30);
            assertFalse(course.isValidEnrollmentCapacity());

            course.setCurrentEnrollment(25);
            course.setMaxEnrollment(0);
            assertFalse(course.isValidEnrollmentCapacity());
        }

        @Test
        @DisplayName("Should validate schedule times")
        void shouldValidateScheduleTimes() {
            assertTrue(course.isValidSchedule());

            course.setStartTime(LocalTime.of(10, 0));
            course.setEndTime(LocalTime.of(9, 0));
            assertFalse(course.isValidSchedule());

            course.setStartTime(null);
            course.setEndTime(LocalTime.of(10, 0));
            assertFalse(course.isValidSchedule());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should determine if course can be cancelled")
        void shouldDetermineIfCourseCanBeCancelled() {
            course.setCurrentEnrollment(5);
            course.setStatus(CourseStatus.ACTIVE);
            assertTrue(course.canBeCancelled());

            course.setCurrentEnrollment(15);
            assertFalse(course.canBeCancelled());

            course.setCurrentEnrollment(5);
            course.setStatus(CourseStatus.COMPLETED);
            assertFalse(course.canBeCancelled());
        }

        @Test
        @DisplayName("Should check if course needs waitlist")
        void shouldCheckIfCourseNeedsWaitlist() {
            course.setCurrentEnrollment(25);
            course.setMaxEnrollment(30);
            assertFalse(course.needsWaitlist());

            course.setCurrentEnrollment(30);
            assertTrue(course.needsWaitlist());
        }

        @Test
        @DisplayName("Should determine enrollment priority")
        void shouldDetermineEnrollmentPriority() {
            course.setCurrentEnrollment(5);
            course.setMaxEnrollment(30);
            assertThat(course.getEnrollmentPriority()).isEqualTo("Low");

            course.setCurrentEnrollment(20);
            assertThat(course.getEnrollmentPriority()).isEqualTo("Medium");

            course.setCurrentEnrollment(28);
            assertThat(course.getEnrollmentPriority()).isEqualTo("High");

            course.setCurrentEnrollment(30);
            assertThat(course.getEnrollmentPriority()).isEqualTo("Full");
        }
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}