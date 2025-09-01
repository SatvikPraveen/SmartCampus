// File location: src/test/java/unit/models/DepartmentTest.java

package com.smartcampus.test.unit.models;

import com.smartcampus.models.Department;
import com.smartcampus.models.Professor;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for the Department model class
 * Tests department entity creation, validation, and business logic
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Department Model Tests")
class DepartmentTest {

    private Department department;
    private Professor headProfessor;
    private List<Professor> professors;
    private List<Student> students;
    private List<Course> courses;

    @BeforeEach
    void setUp() {
        // Set up test head professor
        headProfessor = new Professor();
        headProfessor.setId(1L);
        headProfessor.setEmployeeId("EMP2024001");

        // Set up test professors
        professors = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Professor professor = new Professor();
            professor.setId((long) i);
            professor.setEmployeeId("EMP202400" + i);
            professors.add(professor);
        }

        // Set up test students
        students = new ArrayList<>();
        for (int i = 1; i <= 150; i++) {
            Student student = new Student();
            student.setId((long) i);
            student.setStudentId("CS202400" + String.format("%03d", i));
            students.add(student);
        }

        // Set up test courses
        courses = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Course course = new Course();
            course.setId((long) i);
            course.setCourseCode("CS" + (100 + i));
            course.setCourseName("Course " + i);
            courses.add(course);
        }

        // Set up test department
        department = new Department();
        department.setId(1L);
        department.setCode("CS");
        department.setName("Computer Science");
        department.setDescription("Department of Computer Science and Engineering");
        department.setHeadProfessor(headProfessor);
        department.setBuilding("Computer Science Building");
        department.setFloor("3rd Floor");
        department.setPhone("555-1234");
        department.setEmail("cs@smartcampus.edu");
        department.setBudget(BigDecimal.valueOf(2500000.00));
        department.setEstablishedYear(1985);
        department.setActive(true);
        department.setProfessors(professors);
        department.setStudents(students);
        department.setCourses(courses);
    }

    @Nested
    @DisplayName("Department Creation Tests")
    class DepartmentCreationTests {

        @Test
        @DisplayName("Should create department with valid data")
        void shouldCreateDepartmentWithValidData() {
            assertThat(department).isNotNull();
            assertThat(department.getCode()).isEqualTo("CS");
            assertThat(department.getName()).isEqualTo("Computer Science");
            assertThat(department.getDescription()).isEqualTo("Department of Computer Science and Engineering");
            assertThat(department.getHeadProfessor()).isEqualTo(headProfessor);
            assertThat(department.getBudget()).isEqualTo(BigDecimal.valueOf(2500000.00));
            assertThat(department.getEstablishedYear()).isEqualTo(1985);
            assertTrue(department.isActive());
        }

        @Test
        @DisplayName("Should create department with minimal required fields")
        void shouldCreateDepartmentWithMinimalFields() {
            Department minimalDepartment = new Department();
            minimalDepartment.setCode("MIN");
            minimalDepartment.setName("Minimal Department");
            minimalDepartment.setActive(true);

            assertThat(minimalDepartment.getCode()).isEqualTo("MIN");
            assertThat(minimalDepartment.getName()).isEqualTo("Minimal Department");
            assertTrue(minimalDepartment.isActive());
        }

        @Test
        @DisplayName("Should handle null head professor gracefully")
        void shouldHandleNullHeadProfessor() {
            department.setHeadProfessor(null);
            assertThat(department.getHeadProfessor()).isNull();
        }

        @Test
        @DisplayName("Should initialize collections")
        void shouldInitializeCollections() {
            Department newDepartment = new Department();
            assertThat(newDepartment.getProfessors()).isNotNull();
            assertThat(newDepartment.getStudents()).isNotNull();
            assertThat(newDepartment.getCourses()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Department Code Validation Tests")
    class DepartmentCodeValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid department codes")
        @ValueSource(strings = {"CS", "MATH", "ENG", "PHYS", "BIO", "CHEM", "HIST"})
        void shouldAcceptValidDepartmentCodes(String code) {
            department.setCode(code);
            assertThat(department.getCode()).isEqualTo(code);
        }

        @Test
        @DisplayName("Should handle empty department code")
        void shouldHandleEmptyDepartmentCode() {
            department.setCode("");
            assertThat(department.getCode()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null department code")
        void shouldHandleNullDepartmentCode() {
            department.setCode(null);
            assertThat(department.getCode()).isNull();
        }

        @Test
        @DisplayName("Should normalize department code to uppercase")
        void shouldNormalizeDepartmentCodeToUppercase() {
            department.setCode("cs");
            assertThat(department.getNormalizedCode()).isEqualTo("CS");

            department.setCode("Math");
            assertThat(department.getNormalizedCode()).isEqualTo("MATH");
        }
    }

    @Nested
    @DisplayName("Contact Information Tests")
    class ContactInformationTests {

        @Test
        @DisplayName("Should store complete contact information")
        void shouldStoreCompleteContactInformation() {
            department.setBuilding("Engineering Hall");
            department.setFloor("2nd Floor");
            department.setPhone("555-9876");
            department.setEmail("eng@smartcampus.edu");

            assertThat(department.getBuilding()).isEqualTo("Engineering Hall");
            assertThat(department.getFloor()).isEqualTo("2nd Floor");
            assertThat(department.getPhone()).isEqualTo("555-9876");
            assertThat(department.getEmail()).isEqualTo("eng@smartcampus.edu");
        }

        @Test
        @DisplayName("Should format full location")
        void shouldFormatFullLocation() {
            String expectedLocation = "Computer Science Building, 3rd Floor";
            assertThat(department.getFullLocation()).isEqualTo(expectedLocation);
        }

        @Test
        @DisplayName("Should handle missing location components")
        void shouldHandleMissingLocationComponents() {
            department.setBuilding("Science Building");
            department.setFloor(null);
            
            assertThat(department.getFullLocation()).isEqualTo("Science Building");

            department.setBuilding(null);
            department.setFloor("1st Floor");
            
            assertThat(department.getFullLocation()).isEqualTo("1st Floor");
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() {
            assertTrue(department.isValidEmail("valid@smartcampus.edu"));
            assertTrue(department.isValidEmail("dept.head@university.edu"));
            
            assertFalse(department.isValidEmail("invalid-email"));
            assertFalse(department.isValidEmail("@smartcampus.edu"));
            assertFalse(department.isValidEmail("user@"));
            assertFalse(department.isValidEmail(null));
            assertFalse(department.isValidEmail(""));
        }

        @Test
        @DisplayName("Should validate phone format")
        void shouldValidatePhoneFormat() {
            assertTrue(department.isValidPhone("555-1234"));
            assertTrue(department.isValidPhone("(555) 123-4567"));
            assertTrue(department.isValidPhone("555.123.4567"));
            assertTrue(department.isValidPhone("5551234567"));
            
            assertFalse(department.isValidPhone("123"));
            assertFalse(department.isValidPhone("abc-defg"));
            assertFalse(department.isValidPhone(null));
            assertFalse(department.isValidPhone(""));
        }
    }

    @Nested
    @DisplayName("Budget Management Tests")
    class BudgetManagementTests {

        @ParameterizedTest
        @DisplayName("Should handle various budget amounts")
        @CsvSource({
            "500000.00", "1000000.00", "2500000.00", "5000000.00"
        })
        void shouldHandleVariousBudgetAmounts(String budgetValue) {
            BigDecimal budget = new BigDecimal(budgetValue);
            department.setBudget(budget);
            assertThat(department.getBudget()).isEqualTo(budget);
        }

        @Test
        @DisplayName("Should determine budget category")
        void shouldDetermineBudgetCategory() {
            department.setBudget(BigDecimal.valueOf(500000.00));
            assertThat(department.getBudgetCategory()).isEqualTo("Small");

            department.setBudget(BigDecimal.valueOf(1500000.00));
            assertThat(department.getBudgetCategory()).isEqualTo("Medium");

            department.setBudget(BigDecimal.valueOf(3500000.00));
            assertThat(department.getBudgetCategory()).isEqualTo("Large");

            department.setBudget(BigDecimal.valueOf(7500000.00));
            assertThat(department.getBudgetCategory()).isEqualTo("Very Large");
        }

        @Test
        @DisplayName("Should handle null budget")
        void shouldHandleNullBudget() {
            department.setBudget(null);
            assertThat(department.getBudget()).isNull();
            assertThat(department.getBudgetCategory()).isEqualTo("Not Specified");
        }

        @Test
        @DisplayName("Should calculate budget per student")
        void shouldCalculateBudgetPerStudent() {
            department.setBudget(BigDecimal.valueOf(3000000.00));
            // 150 students from setup
            BigDecimal expectedBudgetPerStudent = BigDecimal.valueOf(20000.00);
            assertThat(department.getBudgetPerStudent()).isEqualTo(expectedBudgetPerStudent);
        }

        @Test
        @DisplayName("Should handle zero students for budget calculation")
        void shouldHandleZeroStudentsForBudgetCalculation() {
            department.setStudents(new ArrayList<>());
            assertThat(department.getBudgetPerStudent()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Staff Management Tests")
    class StaffManagementTests {

        @Test
        @DisplayName("Should count professors")
        void shouldCountProfessors() {
            assertThat(department.getProfessorCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should check if department has head professor")
        void shouldCheckIfDepartmentHasHeadProfessor() {
            assertTrue(department.hasHeadProfessor());

            department.setHeadProfessor(null);
            assertFalse(department.hasHeadProfessor());
        }

        @Test
        @DisplayName("Should add professor to department")
        void shouldAddProfessorToDepartment() {
            Professor newProfessor = new Professor();
            newProfessor.setId(6L);
            newProfessor.setEmployeeId("EMP2024006");
            
            department.addProfessor(newProfessor);
            assertThat(department.getProfessorCount()).isEqualTo(6);
            assertThat(department.getProfessors()).contains(newProfessor);
        }

        @Test
        @DisplayName("Should remove professor from department")
        void shouldRemoveProfessorFromDepartment() {
            Professor professorToRemove = professors.get(0);
            department.removeProfessor(professorToRemove);
            
            assertThat(department.getProfessorCount()).isEqualTo(4);
            assertThat(department.getProfessors()).doesNotContain(professorToRemove);
        }

        @Test
        @DisplayName("Should calculate student to faculty ratio")
        void shouldCalculateStudentToFacultyRatio() {
            // 150 students, 5 professors
            double expectedRatio = 150.0 / 5.0;
            assertThat(department.getStudentToFacultyRatio()).isEqualTo(expectedRatio, within(0.01));
        }

        @Test
        @DisplayName("Should handle zero professors for ratio calculation")
        void shouldHandleZeroProfessorsForRatioCalculation() {
            department.setProfessors(new ArrayList<>());
            assertThat(department.getStudentToFacultyRatio()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Student Management Tests")
    class StudentManagementTests {

        @Test
        @DisplayName("Should count students")
        void shouldCountStudents() {
            assertThat(department.getStudentCount()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should add student to department")
        void shouldAddStudentToDepartment() {
            Student newStudent = new Student();
            newStudent.setId(151L);
            newStudent.setStudentId("CS2024151");
            
            department.addStudent(newStudent);
            assertThat(department.getStudentCount()).isEqualTo(151);
            assertThat(department.getStudents()).contains(newStudent);
        }

        @Test
        @DisplayName("Should remove student from department")
        void shouldRemoveStudentFromDepartment() {
            Student studentToRemove = students.get(0);
            department.removeStudent(studentToRemove);
            
            assertThat(department.getStudentCount()).isEqualTo(149);
            assertThat(department.getStudents()).doesNotContain(studentToRemove);
        }

        @Test
        @DisplayName("Should determine department size category")
        void shouldDetermineDepartmentSizeCategory() {
            department.setStudents(createStudents(50));
            assertThat(department.getSizeCategory()).isEqualTo("Small");

            department.setStudents(createStudents(150));
            assertThat(department.getSizeCategory()).isEqualTo("Medium");

            department.setStudents(createStudents(350));
            assertThat(department.getSizeCategory()).isEqualTo("Large");

            department.setStudents(createStudents(750));
            assertThat(department.getSizeCategory()).isEqualTo("Very Large");
        }
    }

    @Nested
    @DisplayName("Course Management Tests")
    class CourseManagementTests {

        @Test
        @DisplayName("Should count courses")
        void shouldCountCourses() {
            assertThat(department.getCourseCount()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should add course to department")
        void shouldAddCourseToDepartment() {
            Course newCourse = new Course();
            newCourse.setId(21L);
            newCourse.setCourseCode("CS121");
            newCourse.setCourseName("New Course");
            
            department.addCourse(newCourse);
            assertThat(department.getCourseCount()).isEqualTo(21);
            assertThat(department.getCourses()).contains(newCourse);
        }

        @Test
        @DisplayName("Should remove course from department")
        void shouldRemoveCourseFromDepartment() {
            Course courseToRemove = courses.get(0);
            department.removeCourse(courseToRemove);
            
            assertThat(department.getCourseCount()).isEqualTo(19);
            assertThat(department.getCourses()).doesNotContain(courseToRemove);
        }

        @Test
        @DisplayName("Should find courses by level")
        void shouldFindCoursesByLevel() {
            // Add courses with different levels for testing
            List<Course> undergraduateCourses = department.getCoursesByLevel("UNDERGRADUATE");
            assertThat(undergraduateCourses).isNotNull();
            
            List<Course> graduateCourses = department.getCoursesByLevel("GRADUATE");
            assertThat(graduateCourses).isNotNull();
        }
    }

    @Nested
    @DisplayName("Status and Activity Tests")
    class StatusAndActivityTests {

        @Test
        @DisplayName("Should manage active status")
        void shouldManageActiveStatus() {
            department.setActive(true);
            assertTrue(department.isActive());

            department.setActive(false);
            assertFalse(department.isActive());
        }

        @Test
        @DisplayName("Should check if department can offer courses")
        void shouldCheckIfDepartmentCanOfferCourses() {
            department.setActive(true);
            assertTrue(department.canOfferCourses());

            department.setActive(false);
            assertFalse(department.canOfferCourses());

            department.setActive(true);
            department.setProfessors(new ArrayList<>());
            assertFalse(department.canOfferCourses());
        }

        @Test
        @DisplayName("Should determine operational status")
        void shouldDetermineOperationalStatus() {
            department.setActive(true);
            assertThat(department.getOperationalStatus()).isEqualTo("Active");

            department.setActive(false);
            assertThat(department.getOperationalStatus()).isEqualTo("Inactive");

            department.setActive(true);
            department.setProfessors(new ArrayList<>());
            assertThat(department.getOperationalStatus()).isEqualTo("Limited");
        }
    }

    @Nested
    @DisplayName("Historical Information Tests")
    class HistoricalInformationTests {

        @Test
        @DisplayName("Should handle established year")
        void shouldHandleEstablishedYear() {
            department.setEstablishedYear(1975);
            assertThat(department.getEstablishedYear()).isEqualTo(1975);
        }

        @Test
        @DisplayName("Should calculate years in operation")
        void shouldCalculateYearsInOperation() {
            int currentYear = java.time.Year.now().getValue();
            department.setEstablishedYear(1985);
            
            int expectedYears = currentYear - 1985;
            assertThat(department.getYearsInOperation()).isEqualTo(expectedYears);
        }

        @Test
        @DisplayName("Should handle future established year")
        void shouldHandleFutureEstablishedYear() {
            int futureYear = java.time.Year.now().getValue() + 10;
            department.setEstablishedYear(futureYear);
            
            assertThat(department.getYearsInOperation()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should determine department age category")
        void shouldDetermineDepartmentAgeCategory() {
            int currentYear = java.time.Year.now().getValue();
            
            department.setEstablishedYear(currentYear - 5);
            assertThat(department.getAgeCategory()).isEqualTo("New");

            department.setEstablishedYear(currentYear - 15);
            assertThat(department.getAgeCategory()).isEqualTo("Established");

            department.setEstablishedYear(currentYear - 35);
            assertThat(department.getAgeCategory()).isEqualTo("Mature");

            department.setEstablishedYear(currentYear - 75);
            assertThat(department.getAgeCategory()).isEqualTo("Historic");
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when codes match")
        void shouldBeEqualWhenCodesMatch() {
            Department department1 = new Department();
            department1.setCode("CS");

            Department department2 = new Department();
            department2.setCode("CS");

            assertThat(department1).isEqualTo(department2);
            assertThat(department1.hashCode()).isEqualTo(department2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when codes differ")
        void shouldNotBeEqualWhenCodesDiffer() {
            Department department1 = new Department();
            department1.setCode("CS");

            Department department2 = new Department();
            department2.setCode("MATH");

            assertThat(department1).isNotEqualTo(department2);
        }

        @Test
        @DisplayName("Should handle null comparisons")
        void shouldHandleNullComparisons() {
            assertThat(department).isNotEqualTo(null);
            assertThat(department).isNotEqualTo("not a department");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            String departmentString = department.toString();
            
            assertThat(departmentString).contains("CS");
            assertThat(departmentString).contains("Computer Science");
        }

        @Test
        @DisplayName("Should provide display name")
        void shouldProvideDisplayName() {
            String displayName = department.getDisplayName();
            assertThat(displayName).isEqualTo("CS - Computer Science");
        }

        @Test
        @DisplayName("Should provide department summary")
        void shouldProvideDepartmentSummary() {
            String summary = department.getSummary();
            assertThat(summary).contains("Computer Science");
            assertThat(summary).contains("150 students");
            assertThat(summary).contains("5 professors");
            assertThat(summary).contains("20 courses");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            assertTrue(department.isValid());

            department.setCode(null);
            assertFalse(department.isValid());

            department.setCode("CS");
            department.setName(null);
            assertFalse(department.isValid());

            department.setName("Computer Science");
            department.setActive(false);
            assertTrue(department.isValid()); // inactive departments can still be valid
        }

        @Test
        @DisplayName("Should validate department code format")
        void shouldValidateDepartmentCodeFormat() {
            assertTrue(department.isValidCodeFormat("CS"));
            assertTrue(department.isValidCodeFormat("MATH"));
            assertTrue(department.isValidCodeFormat("ENG"));
            
            assertFalse(department.isValidCodeFormat("cs")); // lowercase
            assertFalse(department.isValidCodeFormat("C"));  // too short
            assertFalse(department.isValidCodeFormat("COMPUTER")); // too long
            assertFalse(department.isValidCodeFormat("CS1")); // contains numbers
            assertFalse(department.isValidCodeFormat(null));
            assertFalse(department.isValidCodeFormat(""));
        }

        @Test
        @DisplayName("Should validate budget amount")
        void shouldValidateBudgetAmount() {
            assertTrue(department.isValidBudget(BigDecimal.valueOf(1000000.00)));
            assertTrue(department.isValidBudget(BigDecimal.valueOf(0.00)));
            
            assertFalse(department.isValidBudget(BigDecimal.valueOf(-1000.00)));
            assertFalse(department.isValidBudget(null));
        }

        @Test
        @DisplayName("Should validate established year")
        void shouldValidateEstablishedYear() {
            int currentYear = java.time.Year.now().getValue();
            
            assertTrue(department.isValidEstablishedYear(1900));
            assertTrue(department.isValidEstablishedYear(currentYear));
            
            assertFalse(department.isValidEstablishedYear(1850)); // too early
            assertFalse(department.isValidEstablishedYear(currentYear + 10)); // future
            assertFalse(department.isValidEstablishedYear(0));
        }
    }

    // Helper method to create a list of students
    private List<Student> createStudents(int count) {
        List<Student> studentList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Student student = new Student();
            student.setId((long) i);
            student.setStudentId("TEST" + String.format("%03d", i));
            studentList.add(student);
        }
        return studentList;
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}