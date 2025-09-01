// File location: src/test/java/unit/services/StudentServiceTest.java

package com.smartcampus.test.unit.services;

import com.smartcampus.services.StudentService;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.models.Student;
import com.smartcampus.models.User;
import com.smartcampus.models.Department;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.exceptions.StudentNotFoundException;
import com.smartcampus.exceptions.InvalidStudentDataException;
import com.smartcampus.exceptions.DuplicateStudentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for the StudentService class
 * Tests service layer functionality with mocked dependencies
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Student Service Tests")
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student testStudent;
    private User testUser;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Set up test department
        testDepartment = new Department();
        testDepartment.setId(1L);
        testDepartment.setCode("CS");
        testDepartment.setName("Computer Science");

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john.doe");
        testUser.setEmail("john.doe@smartcampus.edu");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.STUDENT);
        testUser.setActive(true);

        // Set up test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("CS2024001");
        testStudent.setUser(testUser);
        testStudent.setDepartment(testDepartment);
        testStudent.setAdmissionDate(LocalDate.of(2024, 8, 15));
        testStudent.setStatus(StudentStatus.ACTIVE);
        testStudent.setYearLevel(1);
        testStudent.setGpa(BigDecimal.valueOf(3.50));
        testStudent.setTotalCreditsEarned(15);
        testStudent.setTotalCreditsAttempted(18);
    }

    @Nested
    @DisplayName("Create Student Tests")
    class CreateStudentTests {

        @Test
        @DisplayName("Should create student successfully")
        void shouldCreateStudentSuccessfully() {
            // Arrange
            when(studentRepository.existsByStudentId(testStudent.getStudentId())).thenReturn(false);
            when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(false);
            when(departmentRepository.findById(testDepartment.getId())).thenReturn(Optional.of(testDepartment));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

            // Act
            Student result = studentService.createStudent(testStudent);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStudentId()).isEqualTo("CS2024001");
            assertThat(result.getStatus()).isEqualTo(StudentStatus.ACTIVE);

            verify(studentRepository).existsByStudentId(testStudent.getStudentId());
            verify(userRepository).existsByEmail(testUser.getEmail());
            verify(userRepository).save(any(User.class));
            verify(studentRepository).save(any(Student.class));
        }

        @Test
        @DisplayName("Should throw exception when student ID already exists")
        void shouldThrowExceptionWhenStudentIdAlreadyExists() {
            // Arrange
            when(studentRepository.existsByStudentId(testStudent.getStudentId())).thenReturn(true);

            // Act & Assert
            assertThrows(DuplicateStudentException.class, () -> {
                studentService.createStudent(testStudent);
            });

            verify(studentRepository).existsByStudentId(testStudent.getStudentId());
            verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Arrange
            when(studentRepository.existsByStudentId(testStudent.getStudentId())).thenReturn(false);
            when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(true);

            // Act & Assert
            assertThrows(DuplicateStudentException.class, () -> {
                studentService.createStudent(testStudent);
            });

            verify(userRepository).existsByEmail(testUser.getEmail());
            verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void shouldThrowExceptionWhenDepartmentNotFound() {
            // Arrange
            when(studentRepository.existsByStudentId(testStudent.getStudentId())).thenReturn(false);
            when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(false);
            when(departmentRepository.findById(testDepartment.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidStudentDataException.class, () -> {
                studentService.createStudent(testStudent);
            });

            verify(departmentRepository).findById(testDepartment.getId());
            verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        @DisplayName("Should throw exception when student data is invalid")
        void shouldThrowExceptionWhenStudentDataIsInvalid() {
            // Arrange
            testStudent.setStudentId(null); // Invalid data

            // Act & Assert
            assertThrows(InvalidStudentDataException.class, () -> {
                studentService.createStudent(testStudent);
            });

            verify(studentRepository, never()).save(any(Student.class));
        }
    }

    @Nested
    @DisplayName("Find Student Tests")
    class FindStudentTests {

        @Test
        @DisplayName("Should find student by ID successfully")
        void shouldFindStudentByIdSuccessfully() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act
            Student result = studentService.findStudentById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStudentId()).isEqualTo("CS2024001");

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when student not found by ID")
        void shouldThrowExceptionWhenStudentNotFoundById() {
            // Arrange
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(StudentNotFoundException.class, () -> {
                studentService.findStudentById(999L);
            });

            verify(studentRepository).findById(999L);
        }

        @Test
        @DisplayName("Should find student by student ID successfully")
        void shouldFindStudentByStudentIdSuccessfully() {
            // Arrange
            when(studentRepository.findByStudentId("CS2024001")).thenReturn(Optional.of(testStudent));

            // Act
            Student result = studentService.findStudentByStudentId("CS2024001");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStudentId()).isEqualTo("CS2024001");

            verify(studentRepository).findByStudentId("CS2024001");
        }

        @Test
        @DisplayName("Should throw exception when student not found by student ID")
        void shouldThrowExceptionWhenStudentNotFoundByStudentId() {
            // Arrange
            when(studentRepository.findByStudentId("NONEXISTENT")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(StudentNotFoundException.class, () -> {
                studentService.findStudentByStudentId("NONEXISTENT");
            });

            verify(studentRepository).findByStudentId("NONEXISTENT");
        }

        @Test
        @DisplayName("Should find student by email successfully")
        void shouldFindStudentByEmailSuccessfully() {
            // Arrange
            when(studentRepository.findByUserEmail("john.doe@smartcampus.edu")).thenReturn(Optional.of(testStudent));

            // Act
            Student result = studentService.findStudentByEmail("john.doe@smartcampus.edu");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUser().getEmail()).isEqualTo("john.doe@smartcampus.edu");

            verify(studentRepository).findByUserEmail("john.doe@smartcampus.edu");
        }

        @Test
        @DisplayName("Should return null when student not found by email")
        void shouldReturnNullWhenStudentNotFoundByEmail() {
            // Arrange
            when(studentRepository.findByUserEmail("nonexistent@smartcampus.edu")).thenReturn(Optional.empty());

            // Act
            Student result = studentService.findStudentByEmail("nonexistent@smartcampus.edu");

            // Assert
            assertThat(result).isNull();

            verify(studentRepository).findByUserEmail("nonexistent@smartcampus.edu");
        }
    }

    @Nested
    @DisplayName("Update Student Tests")
    class UpdateStudentTests {

        @Test
        @DisplayName("Should update student successfully")
        void shouldUpdateStudentSuccessfully() {
            // Arrange
            Student updatedStudent = new Student();
            updatedStudent.setId(1L);
            updatedStudent.setStudentId("CS2024001");
            updatedStudent.setYearLevel(2);
            updatedStudent.setGpa(BigDecimal.valueOf(3.75));
            updatedStudent.setUser(testUser);
            updatedStudent.setDepartment(testDepartment);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(updatedStudent);

            // Act
            Student result = studentService.updateStudent(1L, updatedStudent);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getYearLevel()).isEqualTo(2);
            assertThat(result.getGpa()).isEqualTo(BigDecimal.valueOf(3.75));

            verify(studentRepository).findById(1L);
            verify(studentRepository).save(any(Student.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent student")
        void shouldThrowExceptionWhenUpdatingNonExistentStudent() {
            // Arrange
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(StudentNotFoundException.class, () -> {
                studentService.updateStudent(999L, testStudent);
            });

            verify(studentRepository).findById(999L);
            verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        @DisplayName("Should update student status")
        void shouldUpdateStudentStatus() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

            // Act
            Student result = studentService.updateStudentStatus(1L, StudentStatus.GRADUATED);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StudentStatus.GRADUATED);

            ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
            verify(studentRepository).save(studentCaptor.capture());
            assertThat(studentCaptor.getValue().getStatus()).isEqualTo(StudentStatus.GRADUATED);
        }

        @Test
        @DisplayName("Should update student GPA")
        void shouldUpdateStudentGpa() {
            // Arrange
            BigDecimal newGpa = BigDecimal.valueOf(3.85);
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

            // Act
            Student result = studentService.updateStudentGpa(1L, newGpa);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
            verify(studentRepository).save(studentCaptor.capture());
            assertThat(studentCaptor.getValue().getGpa()).isEqualTo(newGpa);
        }
    }

    @Nested
    @DisplayName("Delete Student Tests")
    class DeleteStudentTests {

        @Test
        @DisplayName("Should delete student successfully")
        void shouldDeleteStudentSuccessfully() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            doNothing().when(studentRepository).delete(testStudent);

            // Act
            studentService.deleteStudent(1L);

            // Assert
            verify(studentRepository).findById(1L);
            verify(studentRepository).delete(testStudent);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent student")
        void shouldThrowExceptionWhenDeletingNonExistentStudent() {
            // Arrange
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(StudentNotFoundException.class, () -> {
                studentService.deleteStudent(999L);
            });

            verify(studentRepository).findById(999L);
            verify(studentRepository, never()).delete(any(Student.class));
        }

        @Test
        @DisplayName("Should soft delete student by changing status")
        void shouldSoftDeleteStudentByChangingStatus() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

            // Act
            Student result = studentService.softDeleteStudent(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
            verify(studentRepository).save(studentCaptor.capture());
            assertThat(studentCaptor.getValue().getStatus()).isEqualTo(StudentStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("List Students Tests")
    class ListStudentsTests {

        @Test
        @DisplayName("Should get all students")
        void shouldGetAllStudents() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            when(studentRepository.findAll()).thenReturn(students);

            // Act
            List<Student> result = studentService.getAllStudents();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudentId()).isEqualTo("CS2024001");

            verify(studentRepository).findAll();
        }

        @Test
        @DisplayName("Should get students by department")
        void shouldGetStudentsByDepartment() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            when(studentRepository.findByDepartmentId(1L)).thenReturn(students);

            // Act
            List<Student> result = studentService.getStudentsByDepartment(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment().getId()).isEqualTo(1L);

            verify(studentRepository).findByDepartmentId(1L);
        }

        @Test
        @DisplayName("Should get students by status")
        void shouldGetStudentsByStatus() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            when(studentRepository.findByStatus(StudentStatus.ACTIVE)).thenReturn(students);

            // Act
            List<Student> result = studentService.getStudentsByStatus(StudentStatus.ACTIVE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(StudentStatus.ACTIVE);

            verify(studentRepository).findByStatus(StudentStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should get students by year level")
        void shouldGetStudentsByYearLevel() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            when(studentRepository.findByYearLevel(1)).thenReturn(students);

            // Act
            List<Student> result = studentService.getStudentsByYearLevel(1);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getYearLevel()).isEqualTo(1);

            verify(studentRepository).findByYearLevel(1);
        }

        @Test
        @DisplayName("Should return empty list when no students found")
        void shouldReturnEmptyListWhenNoStudentsFound() {
            // Arrange
            when(studentRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<Student> result = studentService.getAllStudents();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(studentRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Search Students Tests")
    class SearchStudentsTests {

        @Test
        @DisplayName("Should search students by name")
        void shouldSearchStudentsByName() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            when(studentRepository.findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCase("John", "John"))
                .thenReturn(students);

            // Act
            List<Student> result = studentService.searchStudentsByName("John");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUser().getFirstName()).isEqualTo("John");

            verify(studentRepository).findByUserFirstNameContainingIgnoreCaseOrUserLastNameContainingIgnoreCase("John", "John");
        }

        @Test
        @DisplayName("Should search students by GPA range")
        void shouldSearchStudentsByGpaRange() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            BigDecimal minGpa = BigDecimal.valueOf(3.0);
            BigDecimal maxGpa = BigDecimal.valueOf(4.0);
            when(studentRepository.findByGpaBetween(minGpa, maxGpa)).thenReturn(students);

            // Act
            List<Student> result = studentService.searchStudentsByGpaRange(minGpa, maxGpa);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGpa()).isBetween(minGpa, maxGpa);

            verify(studentRepository).findByGpaBetween(minGpa, maxGpa);
        }

        @Test
        @DisplayName("Should search students with advanced filters")
        void shouldSearchStudentsWithAdvancedFilters() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            when(studentRepository.findByDepartmentIdAndStatusAndYearLevel(1L, StudentStatus.ACTIVE, 1))
                .thenReturn(students);

            // Act
            List<Student> result = studentService.searchStudentsAdvanced(1L, StudentStatus.ACTIVE, 1);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment().getId()).isEqualTo(1L);
            assertThat(result.get(0).getStatus()).isEqualTo(StudentStatus.ACTIVE);
            assertThat(result.get(0).getYearLevel()).isEqualTo(1);

            verify(studentRepository).findByDepartmentIdAndStatusAndYearLevel(1L, StudentStatus.ACTIVE, 1);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get total student count")
        void shouldGetTotalStudentCount() {
            // Arrange
            when(studentRepository.count()).thenReturn(150L);

            // Act
            long result = studentService.getTotalStudentCount();

            // Assert
            assertThat(result).isEqualTo(150L);

            verify(studentRepository).count();
        }

        @Test
        @DisplayName("Should get student count by department")
        void shouldGetStudentCountByDepartment() {
            // Arrange
            when(studentRepository.countByDepartmentId(1L)).thenReturn(50L);

            // Act
            long result = studentService.getStudentCountByDepartment(1L);

            // Assert
            assertThat(result).isEqualTo(50L);

            verify(studentRepository).countByDepartmentId(1L);
        }

        @Test
        @DisplayName("Should get student count by status")
        void shouldGetStudentCountByStatus() {
            // Arrange
            when(studentRepository.countByStatus(StudentStatus.ACTIVE)).thenReturn(120L);

            // Act
            long result = studentService.getStudentCountByStatus(StudentStatus.ACTIVE);

            // Assert
            assertThat(result).isEqualTo(120L);

            verify(studentRepository).countByStatus(StudentStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should get average GPA by department")
        void shouldGetAverageGpaByDepartment() {
            // Arrange
            when(studentRepository.getAverageGpaByDepartment(1L)).thenReturn(3.45);

            // Act
            double result = studentService.getAverageGpaByDepartment(1L);

            // Assert
            assertThat(result).isEqualTo(3.45, within(0.01));

            verify(studentRepository).getAverageGpaByDepartment(1L);
        }

        @Test
        @DisplayName("Should get students on dean's list")
        void shouldGetStudentsOnDeansList() {
            // Arrange
            List<Student> students = Arrays.asList(testStudent);
            BigDecimal minGpa = BigDecimal.valueOf(3.5);
            when(studentRepository.findByGpaGreaterThanEqualAndStatus(minGpa, StudentStatus.ACTIVE))
                .thenReturn(students);

            // Act
            List<Student> result = studentService.getStudentsOnDeansList(minGpa);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGpa()).isGreaterThanOrEqualTo(minGpa);

            verify(studentRepository).findByGpaGreaterThanEqualAndStatus(minGpa, StudentStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate student data before creation")
        void shouldValidateStudentDataBeforeCreation() {
            // Arrange
            Student invalidStudent = new Student();
            invalidStudent.setStudentId(""); // Invalid empty ID
            invalidStudent.setUser(testUser);
            invalidStudent.setDepartment(testDepartment);

            // Act & Assert
            assertThrows(InvalidStudentDataException.class, () -> {
                studentService.createStudent(invalidStudent);
            });

            verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        @DisplayName("Should validate GPA range")
        void shouldValidateGpaRange() {
            // Arrange
            BigDecimal invalidGpa = BigDecimal.valueOf(5.0); // Invalid GPA > 4.0
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act & Assert
            assertThrows(InvalidStudentDataException.class, () -> {
                studentService.updateStudentGpa(1L, invalidGpa);
            });

            verify(studentRepository, never()).save(any(Student.class));
        }

        @Test
        @DisplayName("Should validate year level range")
        void shouldValidateYearLevelRange() {
            // Arrange
            testStudent.setYearLevel(0); // Invalid year level
            when(studentRepository.existsByStudentId(testStudent.getStudentId())).thenReturn(false);
            when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(false);

            // Act & Assert
            assertThrows(InvalidStudentDataException.class, () -> {
                studentService.createStudent(testStudent);
            });

            verify(studentRepository, never()).save(any(Student.class));
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should determine if student can graduate")
        void shouldDetermineIfStudentCanGraduate() {
            // Arrange
            testStudent.setTotalCreditsEarned(120);
            testStudent.setGpa(BigDecimal.valueOf(2.5));
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act
            boolean result = studentService.canStudentGraduate(1L);

            // Assert
            assertTrue(result);

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should determine student cannot graduate with insufficient credits")
        void shouldDetermineStudentCannotGraduateWithInsufficientCredits() {
            // Arrange
            testStudent.setTotalCreditsEarned(100); // Insufficient credits
            testStudent.setGpa(BigDecimal.valueOf(3.0));
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act
            boolean result = studentService.canStudentGraduate(1L);

            // Assert
            assertFalse(result);

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should determine student cannot graduate with low GPA")
        void shouldDetermineStudentCannotGraduateWithLowGpa() {
            // Arrange
            testStudent.setTotalCreditsEarned(120);
            testStudent.setGpa(BigDecimal.valueOf(1.8)); // Low GPA
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act
            boolean result = studentService.canStudentGraduate(1L);

            // Assert
            assertFalse(result);

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should calculate completion rate")
        void shouldCalculateCompletionRate() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act
            double result = studentService.calculateCompletionRate(1L);

            // Assert
            double expected = (15.0 / 18.0) * 100; // 83.33%
            assertThat(result).isEqualTo(expected, within(0.01));

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should promote student to next year level")
        void shouldPromoteStudentToNextYearLevel() {
            // Arrange
            testStudent.setYearLevel(1);
            testStudent.setTotalCreditsEarned(30);
            testStudent.setGpa(BigDecimal.valueOf(3.0));
            
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

            // Act
            Student result = studentService.promoteStudent(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
            verify(studentRepository).save(studentCaptor.capture());
            assertThat(studentCaptor.getValue().getYearLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not promote student with insufficient credits")
        void shouldNotPromoteStudentWithInsufficientCredits() {
            // Arrange
            testStudent.setYearLevel(1);
            testStudent.setTotalCreditsEarned(15); // Insufficient credits
            testStudent.setGpa(BigDecimal.valueOf(3.0));
            
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act & Assert
            assertThrows(InvalidStudentDataException.class, () -> {
                studentService.promoteStudent(1L);
            });

            verify(studentRepository, never()).save(any(Student.class));
        }
    }

    @Nested
    @DisplayName("Academic Standing Tests")
    class AcademicStandingTests {

        @Test
        @DisplayName("Should get students on academic probation")
        void shouldGetStudentsOnAcademicProbation() {
            // Arrange
            testStudent.setGpa(BigDecimal.valueOf(1.5));
            List<Student> students = Arrays.asList(testStudent);
            BigDecimal maxGpa = BigDecimal.valueOf(2.0);
            
            when(studentRepository.findByGpaLessThanAndStatus(maxGpa, StudentStatus.ACTIVE))
                .thenReturn(students);

            // Act
            List<Student> result = studentService.getStudentsOnAcademicProbation();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGpa()).isLessThan(maxGpa);

            verify(studentRepository).findByGpaLessThanAndStatus(maxGpa, StudentStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should update academic standing")
        void shouldUpdateAcademicStanding() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);

            // Act
            String result = studentService.updateAcademicStanding(1L);

            // Assert
            assertThat(result).isEqualTo("Honor Roll");

            verify(studentRepository).findById(1L);
            verify(studentRepository).save(any(Student.class));
        }
    }

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {

        @Test
        @DisplayName("Should create multiple students")
        void shouldCreateMultipleStudents() {
            // Arrange
            Student student2 = new Student();
            student2.setStudentId("CS2024002");
            student2.setUser(testUser);
            student2.setDepartment(testDepartment);

            List<Student> studentsToCreate = Arrays.asList(testStudent, student2);
            
            when(studentRepository.existsByStudentId(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(testDepartment));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(studentRepository.saveAll(anyList())).thenReturn(studentsToCreate);

            // Act
            List<Student> result = studentService.createStudentsBatch(studentsToCreate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            verify(studentRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should update student statuses in batch")
        void shouldUpdateStudentStatusesInBatch() {
            // Arrange
            List<Long> studentIds = Arrays.asList(1L, 2L, 3L);
            when(studentRepository.updateStatusByIds(studentIds, StudentStatus.GRADUATED))
                .thenReturn(3);

            // Act
            int result = studentService.updateStudentStatusBatch(studentIds, StudentStatus.GRADUATED);

            // Assert
            assertThat(result).isEqualTo(3);

            verify(studentRepository).updateStatusByIds(studentIds, StudentStatus.GRADUATED);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Arrange
            when(studentRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                studentService.findStudentById(1L);
            });

            verify(studentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should handle null input parameters")
        void shouldHandleNullInputParameters() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                studentService.createStudent(null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                studentService.findStudentById(null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                studentService.updateStudent(null, testStudent);
            });
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete student lifecycle")
        void shouldHandleCompleteStudentLifecycle() {
            // Arrange - Create
            when(studentRepository.existsByStudentId(testStudent.getStudentId())).thenReturn(false);
            when(userRepository.existsByEmail(testUser.getEmail())).thenReturn(false);
            when(departmentRepository.findById(testDepartment.getId())).thenReturn(Optional.of(testDepartment));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
            
            // Arrange - Find
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            
            // Arrange - Update
            testStudent.setYearLevel(4);
            testStudent.setStatus(StudentStatus.GRADUATED);

            // Act - Create
            Student created = studentService.createStudent(testStudent);

            // Act - Find
            Student found = studentService.findStudentById(1L);

            // Act - Update
            Student updated = studentService.updateStudentStatus(1L, StudentStatus.GRADUATED);

            // Assert
            assertThat(created).isNotNull();
            assertThat(found).isNotNull();
            assertThat(updated).isNotNull();
            assertThat(found.getId()).isEqualTo(created.getId());

            verify(studentRepository, times(2)).save(any(Student.class));
            verify(studentRepository, times(2)).findById(1L);
        }
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}