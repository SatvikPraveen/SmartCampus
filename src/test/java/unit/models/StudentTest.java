// File location: src/test/java/unit/models/StudentTest.java

package com.smartcampus.test.unit.models;

import com.smartcampus.models.Student;
import com.smartcampus.models.User;
import com.smartcampus.models.Department;
import com.smartcampus.models.enums.StudentStatus;
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

/**
 * Unit tests for the Student model class
 * Tests student entity creation, validation, and business logic
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Student Model Tests")
class StudentTest {

    private Student student;
    private User user;
    private Department department;

    @BeforeEach
    void setUp() {
        // Set up test user
        user = new User();
        user.setId(1L);
        user.setUsername("john.doe");
        user.setEmail("john.doe@smartcampus.edu");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("555-1234");
        user.setRole(UserRole.STUDENT);
        user.setActive(true);

        // Set up test department
        department = new Department();
        department.setId(1L);
        department.setCode("CS");
        department.setName("Computer Science");
        department.setDescription("Department of Computer Science");

        // Set up test student
        student = new Student();
        student.setId(1L);
        student.setStudentId("CS2024001");
        student.setUser(user);
        student.setDepartment(department);
        student.setAdmissionDate(LocalDate.of(2024, 8, 15));
        student.setStatus(StudentStatus.ACTIVE);
        student.setYearLevel(1);
        student.setGpa(BigDecimal.valueOf(3.50));
        student.setTotalCreditsEarned(15);
        student.setTotalCreditsAttempted(18);
        student.setAddress("123 Campus Drive");
        student.setCity("University City");
        student.setState("CA");
        student.setPostalCode("90210");
        student.setCountry("USA");
        student.setEmergencyContactName("Jane Doe");
        student.setEmergencyContactPhone("555-5678");
        student.setEmergencyContactRelationship("Mother");
        student.setScholarshipAmount(BigDecimal.valueOf(5000.00));
        student.setInternational(false);
    }

    @Nested
    @DisplayName("Student Creation Tests")
    class StudentCreationTests {

        @Test
        @DisplayName("Should create student with valid data")
        void shouldCreateStudentWithValidData() {
            assertThat(student).isNotNull();
            assertThat(student.getStudentId()).isEqualTo("CS2024001");
            assertThat(student.getUser()).isEqualTo(user);
            assertThat(student.getDepartment()).isEqualTo(department);
            assertThat(student.getStatus()).isEqualTo(StudentStatus.ACTIVE);
            assertThat(student.getYearLevel()).isEqualTo(1);
            assertThat(student.getGpa()).isEqualTo(BigDecimal.valueOf(3.50));
        }

        @Test
        @DisplayName("Should create student with minimal required fields")
        void shouldCreateStudentWithMinimalFields() {
            Student minimalStudent = new Student();
            minimalStudent.setStudentId("MIN2024001");
            minimalStudent.setUser(user);
            minimalStudent.setDepartment(department);
            minimalStudent.setAdmissionDate(LocalDate.now());
            minimalStudent.setStatus(StudentStatus.ACTIVE);
            minimalStudent.setYearLevel(1);

            assertThat(minimalStudent.getStudentId()).isEqualTo("MIN2024001");
            assertThat(minimalStudent.getUser()).isEqualTo(user);
            assertThat(minimalStudent.getDepartment()).isEqualTo(department);
            assertThat(minimalStudent.getStatus()).isEqualTo(StudentStatus.ACTIVE);
            assertThat(minimalStudent.getYearLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null user gracefully")
        void shouldHandleNullUser() {
            student.setUser(null);
            assertThat(student.getUser()).isNull();
        }

        @Test
        @DisplayName("Should handle null department gracefully")
        void shouldHandleNullDepartment() {
            student.setDepartment(null);
            assertThat(student.getDepartment()).isNull();
        }
    }

    @Nested
    @DisplayName("Student ID Validation Tests")
    class StudentIdValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid student IDs")
        @ValueSource(strings = {"CS2024001", "ENG2023456", "MATH2025999", "BIO2024123"})
        void shouldAcceptValidStudentIds(String studentId) {
            student.setStudentId(studentId);
            assertThat(student.getStudentId()).isEqualTo(studentId);
        }

        @Test
        @DisplayName("Should handle empty student ID")
        void shouldHandleEmptyStudentId() {
            student.setStudentId("");
            assertThat(student.getStudentId()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null student ID")
        void shouldHandleNullStudentId() {
            student.setStudentId(null);
            assertThat(student.getStudentId()).isNull();
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusManagementTests {

        @ParameterizedTest
        @DisplayName("Should accept all valid student statuses")
        @EnumSource(StudentStatus.class)
        void shouldAcceptAllValidStatuses(StudentStatus status) {
            student.setStatus(status);
            assertThat(student.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("Should check if student is active")
        void shouldCheckIfStudentIsActive() {
            student.setStatus(StudentStatus.ACTIVE);
            assertTrue(student.isActive());

            student.setStatus(StudentStatus.INACTIVE);
            assertFalse(student.isActive());

            student.setStatus(StudentStatus.GRADUATED);
            assertFalse(student.isActive());

            student.setStatus(StudentStatus.SUSPENDED);
            assertFalse(student.isActive());
        }

        @Test
        @DisplayName("Should check if student is enrolled")
        void shouldCheckIfStudentIsEnrolled() {
            student.setStatus(StudentStatus.ACTIVE);
            assertTrue(student.canEnroll());

            student.setStatus(StudentStatus.INACTIVE);
            assertFalse(student.canEnroll());

            student.setStatus(StudentStatus.SUSPENDED);
            assertFalse(student.canEnroll());

            student.setStatus(StudentStatus.GRADUATED);
            assertFalse(student.canEnroll());
        }
    }

    @Nested
    @DisplayName("Year Level Tests")
    class YearLevelTests {

        @ParameterizedTest
        @DisplayName("Should accept valid year levels")
        @ValueSource(ints = {1, 2, 3, 4, 5})
        void shouldAcceptValidYearLevels(int yearLevel) {
            student.setYearLevel(yearLevel);
            assertThat(student.getYearLevel()).isEqualTo(yearLevel);
        }

        @Test
        @DisplayName("Should get year level description")
        void shouldGetYearLevelDescription() {
            student.setYearLevel(1);
            assertThat(student.getYearLevelDescription()).isEqualTo("Freshman");

            student.setYearLevel(2);
            assertThat(student.getYearLevelDescription()).isEqualTo("Sophomore");

            student.setYearLevel(3);
            assertThat(student.getYearLevelDescription()).isEqualTo("Junior");

            student.setYearLevel(4);
            assertThat(student.getYearLevelDescription()).isEqualTo("Senior");

            student.setYearLevel(5);
            assertThat(student.getYearLevelDescription()).isEqualTo("Graduate");
        }

        @Test
        @DisplayName("Should handle invalid year level")
        void shouldHandleInvalidYearLevel() {
            student.setYearLevel(0);
            assertThat(student.getYearLevelDescription()).isEqualTo("Unknown");

            student.setYearLevel(6);
            assertThat(student.getYearLevelDescription()).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("GPA and Credits Tests")
    class GpaAndCreditsTests {

        @ParameterizedTest
        @DisplayName("Should accept valid GPA values")
        @CsvSource({
            "0.00", "1.50", "2.00", "2.75", "3.00", "3.50", "3.75", "4.00"
        })
        void shouldAcceptValidGpaValues(String gpaValue) {
            BigDecimal gpa = new BigDecimal(gpaValue);
            student.setGpa(gpa);
            assertThat(student.getGpa()).isEqualTo(gpa);
        }

        @Test
        @DisplayName("Should calculate completion rate")
        void shouldCalculateCompletionRate() {
            student.setTotalCreditsEarned(15);
            student.setTotalCreditsAttempted(18);
            
            double expectedRate = (15.0 / 18.0) * 100;
            assertThat(student.getCompletionRate()).isEqualTo(expectedRate, within(0.01));
        }

        @Test
        @DisplayName("Should handle zero credits attempted")
        void shouldHandleZeroCreditsAttempted() {
            student.setTotalCreditsEarned(0);
            student.setTotalCreditsAttempted(0);
            
            assertThat(student.getCompletionRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should determine academic standing")
        void shouldDetermineAcademicStanding() {
            student.setGpa(BigDecimal.valueOf(3.75));
            assertThat(student.getAcademicStanding()).isEqualTo("Dean's List");

            student.setGpa(BigDecimal.valueOf(3.50));
            assertThat(student.getAcademicStanding()).isEqualTo("Honor Roll");

            student.setGpa(BigDecimal.valueOf(3.00));
            assertThat(student.getAcademicStanding()).isEqualTo("Good Standing");

            student.setGpa(BigDecimal.valueOf(2.50));
            assertThat(student.getAcademicStanding()).isEqualTo("Good Standing");

            student.setGpa(BigDecimal.valueOf(1.80));
            assertThat(student.getAcademicStanding()).isEqualTo("Academic Warning");

            student.setGpa(BigDecimal.valueOf(1.50));
            assertThat(student.getAcademicStanding()).isEqualTo("Academic Probation");

            student.setGpa(BigDecimal.valueOf(1.00));
            assertThat(student.getAcademicStanding()).isEqualTo("Academic Suspension");
        }
    }

    @Nested
    @DisplayName("Contact Information Tests")
    class ContactInformationTests {

        @Test
        @DisplayName("Should store complete address information")
        void shouldStoreCompleteAddressInformation() {
            student.setAddress("456 University Ave");
            student.setCity("College Town");
            student.setState("NY");
            student.setPostalCode("12345");
            student.setCountry("USA");

            assertThat(student.getAddress()).isEqualTo("456 University Ave");
            assertThat(student.getCity()).isEqualTo("College Town");
            assertThat(student.getState()).isEqualTo("NY");
            assertThat(student.getPostalCode()).isEqualTo("12345");
            assertThat(student.getCountry()).isEqualTo("USA");
        }

        @Test
        @DisplayName("Should format full address")
        void shouldFormatFullAddress() {
            String expectedAddress = "123 Campus Drive, University City, CA 90210, USA";
            assertThat(student.getFullAddress()).isEqualTo(expectedAddress);
        }

        @Test
        @DisplayName("Should store emergency contact information")
        void shouldStoreEmergencyContactInformation() {
            student.setEmergencyContactName("John Smith");
            student.setEmergencyContactPhone("555-9999");
            student.setEmergencyContactRelationship("Father");

            assertThat(student.getEmergencyContactName()).isEqualTo("John Smith");
            assertThat(student.getEmergencyContactPhone()).isEqualTo("555-9999");
            assertThat(student.getEmergencyContactRelationship()).isEqualTo("Father");
        }
    }

    @Nested
    @DisplayName("Date Management Tests")
    class DateManagementTests {

        @Test
        @DisplayName("Should calculate years enrolled")
        void shouldCalculateYearsEnrolled() {
            LocalDate admissionDate = LocalDate.of(2022, 8, 15);
            student.setAdmissionDate(admissionDate);

            long yearsEnrolled = student.getYearsEnrolled();
            assertThat(yearsEnrolled).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should handle future admission date")
        void shouldHandleFutureAdmissionDate() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            student.setAdmissionDate(futureDate);

            long yearsEnrolled = student.getYearsEnrolled();
            assertThat(yearsEnrolled).isEqualTo(0);
        }

        @Test
        @DisplayName("Should set graduation date")
        void shouldSetGraduationDate() {
            LocalDate graduationDate = LocalDate.of(2026, 5, 15);
            student.setGraduationDate(graduationDate);
            student.setStatus(StudentStatus.GRADUATED);

            assertThat(student.getGraduationDate()).isEqualTo(graduationDate);
            assertThat(student.getStatus()).isEqualTo(StudentStatus.GRADUATED);
            assertFalse(student.isActive());
        }
    }

    @Nested
    @DisplayName("Scholarship and Financial Tests")
    class ScholarshipAndFinancialTests {

        @Test
        @DisplayName("Should handle scholarship amount")
        void shouldHandleScholarshipAmount() {
            BigDecimal scholarshipAmount = BigDecimal.valueOf(7500.00);
            student.setScholarshipAmount(scholarshipAmount);

            assertThat(student.getScholarshipAmount()).isEqualTo(scholarshipAmount);
            assertTrue(student.hasScholarship());
        }

        @Test
        @DisplayName("Should determine if student has scholarship")
        void shouldDetermineIfStudentHasScholarship() {
            student.setScholarshipAmount(BigDecimal.ZERO);
            assertFalse(student.hasScholarship());

            student.setScholarshipAmount(BigDecimal.valueOf(1000.00));
            assertTrue(student.hasScholarship());

            student.setScholarshipAmount(null);
            assertFalse(student.hasScholarship());
        }
    }

    @Nested
    @DisplayName("International Status Tests")
    class InternationalStatusTests {

        @Test
        @DisplayName("Should handle international student status")
        void shouldHandleInternationalStudentStatus() {
            student.setInternational(true);
            assertTrue(student.isInternational());

            student.setInternational(false);
            assertFalse(student.isInternational());
        }

        @Test
        @DisplayName("Should determine if student needs visa documentation")
        void shouldDetermineIfStudentNeedsVisaDocumentation() {
            student.setInternational(true);
            student.setCountry("Canada");
            assertTrue(student.needsVisaDocumentation());

            student.setInternational(false);
            student.setCountry("USA");
            assertFalse(student.needsVisaDocumentation());
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when student IDs match")
        void shouldBeEqualWhenStudentIdsMatch() {
            Student student1 = new Student();
            student1.setStudentId("CS2024001");

            Student student2 = new Student();
            student2.setStudentId("CS2024001");

            assertThat(student1).isEqualTo(student2);
            assertThat(student1.hashCode()).isEqualTo(student2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when student IDs differ")
        void shouldNotBeEqualWhenStudentIdsDiffer() {
            Student student1 = new Student();
            student1.setStudentId("CS2024001");

            Student student2 = new Student();
            student2.setStudentId("CS2024002");

            assertThat(student1).isNotEqualTo(student2);
        }

        @Test
        @DisplayName("Should handle null comparisons")
        void shouldHandleNullComparisons() {
            assertThat(student).isNotEqualTo(null);
            assertThat(student).isNotEqualTo("not a student");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            String studentString = student.toString();
            
            assertThat(studentString).contains("CS2024001");
            assertThat(studentString).contains("John");
            assertThat(studentString).contains("Doe");
            assertThat(studentString).contains("Computer Science");
        }

        @Test
        @DisplayName("Should provide display name")
        void shouldProvideDisplayName() {
            String displayName = student.getDisplayName();
            assertThat(displayName).isEqualTo("John Doe (CS2024001)");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() {
            assertTrue(student.isValidForEnrollment());

            student.setUser(null);
            assertFalse(student.isValidForEnrollment());

            student.setUser(user);
            student.setDepartment(null);
            assertFalse(student.isValidForEnrollment());

            student.setDepartment(department);
            student.setStatus(StudentStatus.SUSPENDED);
            assertFalse(student.isValidForEnrollment());
        }

        @Test
        @DisplayName("Should validate GPA range")
        void shouldValidateGpaRange() {
            assertTrue(student.isValidGpa(BigDecimal.valueOf(3.50)));
            assertTrue(student.isValidGpa(BigDecimal.valueOf(0.00)));
            assertTrue(student.isValidGpa(BigDecimal.valueOf(4.00)));
            
            assertFalse(student.isValidGpa(BigDecimal.valueOf(-1.00)));
            assertFalse(student.isValidGpa(BigDecimal.valueOf(4.01)));
            assertFalse(student.isValidGpa(null));
        }

        @Test
        @DisplayName("Should validate year level range")
        void shouldValidateYearLevelRange() {
            assertTrue(student.isValidYearLevel(1));
            assertTrue(student.isValidYearLevel(4));
            assertTrue(student.isValidYearLevel(5));
            
            assertFalse(student.isValidYearLevel(0));
            assertFalse(student.isValidYearLevel(6));
            assertFalse(student.isValidYearLevel(-1));
        }
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}