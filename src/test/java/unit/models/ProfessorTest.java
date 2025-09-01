// File location: src/test/java/unit/models/ProfessorTest.java

package com.smartcampus.test.unit.models;

import com.smartcampus.models.Professor;
import com.smartcampus.models.User;
import com.smartcampus.models.Department;
import com.smartcampus.models.Course;
import com.smartcampus.models.enums.ProfessorTitle;
import com.smartcampus.models.enums.TenureStatus;
import com.smartcampus.models.enums.ProfessorStatus;
import com.smartcampus.models.enums.UserRole;

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
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for the Professor model class
 * Tests professor entity creation, validation, and business logic
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Professor Model Tests")
class ProfessorTest {

    private Professor professor;
    private User user;
    private Department department;
    private List<Course> courses;

    @BeforeEach
    void setUp() {
        // Set up test user
        user = new User();
        user.setId(1L);
        user.setUsername("dr.smith");
        user.setEmail("dr.smith@smartcampus.edu");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setPhone("555-1234");
        user.setRole(UserRole.PROFESSOR);
        user.setActive(true);

        // Set up test department
        department = new Department();
        department.setId(1L);
        department.setCode("CS");
        department.setName("Computer Science");
        department.setDescription("Department of Computer Science");

        // Set up test courses
        courses = new ArrayList<>();
        Course course1 = new Course();
        course1.setId(1L);
        course1.setCourseCode("CS101");
        course1.setCourseName("Introduction to Programming");
        course1.setCredits(3);
        courses.add(course1);

        Course course2 = new Course();
        course2.setId(2L);
        course2.setCourseCode("CS201");
        course2.setCourseName("Data Structures");
        course2.setCredits(4);
        courses.add(course2);

        // Set up test professor
        professor = new Professor();
        professor.setId(1L);
        professor.setEmployeeId("EMP2024001");
        professor.setUser(user);
        professor.setDepartment(department);
        professor.setTitle(ProfessorTitle.ASSOCIATE_PROFESSOR);
        professor.setSpecialization("Software Engineering, Machine Learning");
        professor.setHireDate(LocalDate.of(2020, 8, 15));
        professor.setTenureStatus(TenureStatus.TENURE_TRACK);
        professor.setOfficeBuilding("Science Building");
        professor.setOfficeRoom("302");
        professor.setOfficePhone("555-8765");
        professor.setOfficeHours("MWF 2:00-4:00 PM, TTh 1:00-3:00 PM");
        professor.setResearchInterests("Artificial Intelligence, Data Mining, Web Development");
        professor.setEducationBackground("PhD Computer Science - Stanford University, MS Computer Science - MIT");
        professor.setPublicationsCount(25);
        professor.setSalary(BigDecimal.valueOf(85000.00));
        professor.setDepartmentHead(false);
        professor.setMaxCourseLoad(4);
        professor.setStatus(ProfessorStatus.ACTIVE);
        professor.setCourses(courses);
    }

    @Nested
    @DisplayName("Professor Creation Tests")
    class ProfessorCreationTests {

        @Test
        @DisplayName("Should create professor with valid data")
        void shouldCreateProfessorWithValidData() {
            assertThat(professor).isNotNull();
            assertThat(professor.getEmployeeId()).isEqualTo("EMP2024001");
            assertThat(professor.getUser()).isEqualTo(user);
            assertThat(professor.getDepartment()).isEqualTo(department);
            assertThat(professor.getTitle()).isEqualTo(ProfessorTitle.ASSOCIATE_PROFESSOR);
            assertThat(professor.getTenureStatus()).isEqualTo(TenureStatus.TENURE_TRACK);
            assertThat(professor.getStatus()).isEqualTo(ProfessorStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should create professor with minimal required fields")
        void shouldCreateProfessorWithMinimalFields() {
            Professor minimalProfessor = new Professor();
            minimalProfessor.setEmployeeId("MIN2024001");
            minimalProfessor.setUser(user);
            minimalProfessor.setDepartment(department);
            minimalProfessor.setTitle(ProfessorTitle.INSTRUCTOR);
            minimalProfessor.setHireDate(LocalDate.now());
            minimalProfessor.setStatus(ProfessorStatus.ACTIVE);

            assertThat(minimalProfessor.getEmployeeId()).isEqualTo("MIN2024001");
            assertThat(minimalProfessor.getUser()).isEqualTo(user);
            assertThat(minimalProfessor.getDepartment()).isEqualTo(department);
            assertThat(minimalProfessor.getTitle()).isEqualTo(ProfessorTitle.INSTRUCTOR);
            assertThat(minimalProfessor.getStatus()).isEqualTo(ProfessorStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should handle null user gracefully")
        void shouldHandleNullUser() {
            professor.setUser(null);
            assertThat(professor.getUser()).isNull();
        }

        @Test
        @DisplayName("Should handle null department gracefully")
        void shouldHandleNullDepartment() {
            professor.setDepartment(null);
            assertThat(professor.getDepartment()).isNull();
        }
    }

    @Nested
    @DisplayName("Employee ID Validation Tests")
    class EmployeeIdValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid employee IDs")
        @ValueSource(strings = {"EMP2024001", "PROF2023456", "FAC2025999", "INST2024123"})
        void shouldAcceptValidEmployeeIds(String employeeId) {
            professor.setEmployeeId(employeeId);
            assertThat(professor.getEmployeeId()).isEqualTo(employeeId);
        }

        @Test
        @DisplayName("Should handle empty employee ID")
        void shouldHandleEmptyEmployeeId() {
            professor.setEmployeeId("");
            assertThat(professor.getEmployeeId()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null employee ID")
        void shouldHandleNullEmployeeId() {
            professor.setEmployeeId(null);
            assertThat(professor.getEmployeeId()).isNull();
        }
    }

    @Nested
    @DisplayName("Title and Status Management Tests")
    class TitleAndStatusManagementTests {

        @ParameterizedTest
        @DisplayName("Should accept all valid professor titles")
        @EnumSource(ProfessorTitle.class)
        void shouldAcceptAllValidTitles(ProfessorTitle title) {
            professor.setTitle(title);
            assertThat(professor.getTitle()).isEqualTo(title);
        }

        @ParameterizedTest
        @DisplayName("Should accept all valid tenure statuses")
        @EnumSource(TenureStatus.class)
        void shouldAcceptAllValidTenureStatuses(TenureStatus tenureStatus) {
            professor.setTenureStatus(tenureStatus);
            assertThat(professor.getTenureStatus()).isEqualTo(tenureStatus);
        }

        @ParameterizedTest
        @DisplayName("Should accept all valid professor statuses")
        @EnumSource(ProfessorStatus.class)
        void shouldAcceptAllValidStatuses(ProfessorStatus status) {
            professor.setStatus(status);
            assertThat(professor.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("Should check if professor is active")
        void shouldCheckIfProfessorIsActive() {
            professor.setStatus(ProfessorStatus.ACTIVE);
            assertTrue(professor.isActive());

            professor.setStatus(ProfessorStatus.INACTIVE);
            assertFalse(professor.isActive());

            professor.setStatus(ProfessorStatus.SABBATICAL);
            assertFalse(professor.isActive());

            professor.setStatus(ProfessorStatus.RETIRED);
            assertFalse(professor.isActive());
        }

        @Test
        @DisplayName("Should check if professor can teach")
        void shouldCheckIfProfessorCanTeach() {
            professor.setStatus(ProfessorStatus.ACTIVE);
            assertTrue(professor.canTeach());

            professor.setStatus(ProfessorStatus.SABBATICAL);
            assertFalse(professor.canTeach());

            professor.setStatus(ProfessorStatus.INACTIVE);
            assertFalse(professor.canTeach());

            professor.setStatus(ProfessorStatus.RETIRED);
            assertFalse(professor.canTeach());
        }

        @Test
        @DisplayName("Should determine if professor is tenured")
        void shouldDetermineIfProfessorIsTenured() {
            professor.setTenureStatus(TenureStatus.TENURED);
            assertTrue(professor.isTenured());

            professor.setTenureStatus(TenureStatus.TENURE_TRACK);
            assertFalse(professor.isTenured());

            professor.setTenureStatus(TenureStatus.NON_TENURE);
            assertFalse(professor.isTenured());
        }
    }

    @Nested
    @DisplayName("Office Information Tests")
    class OfficeInformationTests {

        @Test
        @DisplayName("Should store complete office information")
        void shouldStoreCompleteOfficeInformation() {
            professor.setOfficeBuilding("Engineering Building");
            professor.setOfficeRoom("A-205");
            professor.setOfficePhone("555-9876");

            assertThat(professor.getOfficeBuilding()).isEqualTo("Engineering Building");
            assertThat(professor.getOfficeRoom()).isEqualTo("A-205");
            assertThat(professor.getOfficePhone()).isEqualTo("555-9876");
        }

        @Test
        @DisplayName("Should format full office location")
        void shouldFormatFullOfficeLocation() {
            String expectedLocation = "Science Building, Room 302";
            assertThat(professor.getFullOfficeLocation()).isEqualTo(expectedLocation);
        }

        @Test
        @DisplayName("Should handle office hours")
        void shouldHandleOfficeHours() {
            String officeHours = "Monday 9:00-11:00 AM, Wednesday 2:00-4:00 PM";
            professor.setOfficeHours(officeHours);
            assertThat(professor.getOfficeHours()).isEqualTo(officeHours);
        }

        @Test
        @DisplayName("Should determine if office hours are set")
        void shouldDetermineIfOfficeHoursAreSet() {
            assertTrue(professor.hasOfficeHours());

            professor.setOfficeHours("");
            assertFalse(professor.hasOfficeHours());

            professor.setOfficeHours(null);
            assertFalse(professor.hasOfficeHours());
        }
    }

    @Nested
    @DisplayName("Academic Information Tests")
    class AcademicInformationTests {

        @Test
        @DisplayName("Should handle specialization")
        void shouldHandleSpecialization() {
            String specialization = "Database Systems, Cloud Computing";
            professor.setSpecialization(specialization);
            assertThat(professor.getSpecialization()).isEqualTo(specialization);
        }

        @Test
        @DisplayName("Should handle research interests")
        void shouldHandleResearchInterests() {
            String researchInterests = "Machine Learning, Natural Language Processing";
            professor.setResearchInterests(researchInterests);
            assertThat(professor.getResearchInterests()).isEqualTo(researchInterests);
        }

        @Test
        @DisplayName("Should handle education background")
        void shouldHandleEducationBackground() {
            String education = "PhD Computer Science - UC Berkeley, MS Mathematics - Caltech";
            professor.setEducationBackground(education);
            assertThat(professor.getEducationBackground()).isEqualTo(education);
        }

        @Test
        @DisplayName("Should track publications count")
        void shouldTrackPublicationsCount() {
            professor.setPublicationsCount(42);
            assertThat(professor.getPublicationsCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("Should determine research activity level")
        void shouldDetermineResearchActivityLevel() {
            professor.setPublicationsCount(50);
            assertThat(professor.getResearchActivityLevel()).isEqualTo("Highly Active");

            professor.setPublicationsCount(20);
            assertThat(professor.getResearchActivityLevel()).isEqualTo("Active");

            professor.setPublicationsCount(5);
            assertThat(professor.getResearchActivityLevel()).isEqualTo("Moderate");

            professor.setPublicationsCount(0);
            assertThat(professor.getResearchActivityLevel()).isEqualTo("Limited");
        }
    }

    @Nested
    @DisplayName("Course Load Management Tests")
    class CourseLoadManagementTests {

        @Test
        @DisplayName("Should manage course assignments")
        void shouldManageCourseAssignments() {
            assertThat(professor.getCourses()).hasSize(2);
            assertThat(professor.getCurrentCourseLoad()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should check if professor can take additional courses")
        void shouldCheckIfProfessorCanTakeAdditionalCourses() {
            professor.setMaxCourseLoad(4);
            assertTrue(professor.canTakeAdditionalCourse());

            professor.setMaxCourseLoad(2);
            assertFalse(professor.canTakeAdditionalCourse());

            professor.setMaxCourseLoad(1);
            assertFalse(professor.canTakeAdditionalCourse());
        }

        @Test
        @DisplayName("Should calculate course load percentage")
        void shouldCalculateCourseLoadPercentage() {
            professor.setMaxCourseLoad(4);
            double expected = (2.0 / 4.0) * 100;
            assertThat(professor.getCourseLoadPercentage()).isEqualTo(expected, within(0.01));
        }

        @Test
        @DisplayName("Should handle zero max course load")
        void shouldHandleZeroMaxCourseLoad() {
            professor.setMaxCourseLoad(0);
            assertThat(professor.getCourseLoadPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should add course to professor")
        void shouldAddCourseToProfessor() {
            Course newCourse = new Course();
            newCourse.setId(3L);
            newCourse.setCourseCode("CS301");
            newCourse.setCourseName("Advanced Algorithms");
            
            professor.addCourse(newCourse);
            assertThat(professor.getCourses()).hasSize(3);
            assertThat(professor.getCurrentCourseLoad()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should remove course from professor")
        void shouldRemoveCourseFromProfessor() {
            Course courseToRemove = courses.get(0);
            professor.removeCourse(courseToRemove);
            
            assertThat(professor.getCourses()).hasSize(1);
            assertThat(professor.getCurrentCourseLoad()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Salary and Compensation Tests")
    class SalaryAndCompensationTests {

        @ParameterizedTest
        @DisplayName("Should handle various salary amounts")
        @CsvSource({
            "50000.00", "75000.00", "100000.00", "125000.00", "150000.00"
        })
        void shouldHandleVariousSalaryAmounts(String salaryValue) {
            BigDecimal salary = new BigDecimal(salaryValue);
            professor.setSalary(salary);
            assertThat(professor.getSalary()).isEqualTo(salary);
        }

        @Test
        @DisplayName("Should determine salary band")
        void shouldDetermineSalaryBand() {
            professor.setSalary(BigDecimal.valueOf(45000.00));
            assertThat(professor.getSalaryBand()).isEqualTo("Entry Level");

            professor.setSalary(BigDecimal.valueOf(65000.00));
            assertThat(professor.getSalaryBand()).isEqualTo("Mid Level");

            professor.setSalary(BigDecimal.valueOf(95000.00));
            assertThat(professor.getSalaryBand()).isEqualTo("Senior Level");

            professor.setSalary(BigDecimal.valueOf(125000.00));
            assertThat(professor.getSalaryBand()).isEqualTo("Executive Level");
        }

        @Test
        @DisplayName("Should handle null salary")
        void shouldHandleNullSalary() {
            professor.setSalary(null);
            assertThat(professor.getSalary()).isNull();
            assertThat(professor.getSalaryBand()).isEqualTo("Not Specified");
        }
    }

    @Nested
    @DisplayName("Department Head Management Tests")
    class DepartmentHeadManagementTests {

        @Test
        @DisplayName("Should manage department head status")
        void shouldManageDepartmentHeadStatus() {
            professor.setDepartmentHead(true);
            assertTrue(professor.isDepartmentHead());

            professor.setDepartmentHead(false);
            assertFalse(professor.isDepartmentHead());
        }

        @Test
        @DisplayName("Should check department head eligibility")
        void shouldCheckDepartmentHeadEligibility() {
            professor.setTitle(ProfessorTitle.PROFESSOR);
            professor.setTenureStatus(TenureStatus.TENURED);
            professor.setStatus(ProfessorStatus.ACTIVE);
            assertTrue(professor.isEligibleForDepartmentHead());

            professor.setTitle(ProfessorTitle.INSTRUCTOR);
            assertFalse(professor.isEligibleForDepartmentHead());

            professor.setTitle(ProfessorTitle.PROFESSOR);
            professor.setTenureStatus(TenureStatus.NON_TENURE);
            assertFalse(professor.isEligibleForDepartmentHead());

            professor.setTenureStatus(TenureStatus.TENURED);
            professor.setStatus(ProfessorStatus.SABBATICAL);
            assertFalse(professor.isEligibleForDepartmentHead());
        }
    }

    @Nested
    @DisplayName("Date and Experience Tests")
    class DateAndExperienceTests {

        @Test
        @DisplayName("Should calculate years of service")
        void shouldCalculateYearsOfService() {
            LocalDate hireDate = LocalDate.of(2020, 8, 15);
            professor.setHireDate(hireDate);

            long yearsOfService = professor.getYearsOfService();
            assertThat(yearsOfService).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Should handle future hire date")
        void shouldHandleFutureHireDate() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            professor.setHireDate(futureDate);

            long yearsOfService = professor.getYearsOfService();
            assertThat(yearsOfService).isEqualTo(0);
        }

        @Test
        @DisplayName("Should determine experience level")
        void shouldDetermineExperienceLevel() {
            professor.setHireDate(LocalDate.now().minusYears(15));
            assertThat(professor.getExperienceLevel()).isEqualTo("Veteran");

            professor.setHireDate(LocalDate.now().minusYears(8));
            assertThat(professor.getExperienceLevel()).isEqualTo("Senior");

            professor.setHireDate(LocalDate.now().minusYears(3));
            assertThat(professor.getExperienceLevel()).isEqualTo("Experienced");

            professor.setHireDate(LocalDate.now().minusYears(1));
            assertThat(professor.getExperienceLevel()).isEqualTo("Junior");

            professor.setHireDate(LocalDate.now());
            assertThat(professor.getExperienceLevel()).isEqualTo("New");
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when employee IDs match")
        void shouldBeEqualWhenEmployeeIdsMatch() {
            Professor professor1 = new Professor();
            professor1.setEmployeeId("EMP2024001");

            Professor professor2 = new Professor();
            professor2.setEmployeeId("EMP2024001");

            assertThat(professor1).isEqualTo(professor2);
            assertThat(professor1.hashCode()).isEqualTo(professor2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when employee IDs differ")
        void shouldNotBeEqualWhenEmployeeIdsDiffer() {
            Professor professor1 = new Professor();
            professor1.setEmployeeId("EMP2024001");

            Professor professor2 = new Professor();
            professor2.setEmployeeId("EMP2024002");

            assertThat(professor1).isNotEqualTo(professor2);
        }

        @Test
        @DisplayName("Should handle null comparisons")
        void shouldHandleNullComparisons() {
            assertThat(professor).isNotEqualTo(null);
            assertThat(professor).isNotEqualTo("not a professor");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            String professorString = professor.toString();
            
            assertThat(professorString).contains("EMP2024001");
            assertThat(professorString).contains("Jane");
            assertThat(professorString).contains("Smith");
            assertThat(professorString).contains("Computer Science");
        }

        @Test
        @DisplayName("Should provide display name")
        void shouldProvideDisplayName() {
            String displayName = professor.getDisplayName();
            assertThat(displayName).isEqualTo("Dr. Jane Smith (ASSOCIATE_PROFESSOR)");
        }

        @Test
        @DisplayName("Should provide formal title")
        void shouldProvideFormalTitle() {
            String formalTitle = professor.getFormalTitle();
            assertThat(formalTitle).contains("Associate Professor");
            assertThat(formalTitle).contains("Jane Smith");
            assertThat(formalTitle).contains("Computer Science");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            assertTrue(professor.isValidForTeaching());

            professor.setUser(null);
            assertFalse(professor.isValidForTeaching());

            professor.setUser(user);
            professor.setDepartment(null);
            assertFalse(professor.isValidForTeaching());

            professor.setDepartment(department);
            professor.setStatus(ProfessorStatus.INACTIVE);
            assertFalse(professor.isValidForTeaching());
        }

        @Test
        @DisplayName("Should validate course load limits")
        void shouldValidateCourseLoadLimits() {
            professor.setMaxCourseLoad(4);
            assertTrue(professor.isValidCourseLoad());

            professor.setMaxCourseLoad(1);
            assertFalse(professor.isValidCourseLoad());

            professor.setMaxCourseLoad(0);
            assertFalse(professor.isValidCourseLoad());
        }

        @Test
        @DisplayName("Should validate salary range")
        void shouldValidateSalaryRange() {
            assertTrue(professor.isValidSalary(BigDecimal.valueOf(50000.00)));
            assertTrue(professor.isValidSalary(BigDecimal.valueOf(150000.00)));
            
            assertFalse(professor.isValidSalary(BigDecimal.valueOf(-1000.00)));
            assertFalse(professor.isValidSalary(BigDecimal.ZERO));
            assertFalse(professor.isValidSalary(null));
        }
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}