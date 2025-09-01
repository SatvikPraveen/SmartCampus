// File location: src/test/java/unit/services/CourseServiceTest.java

package com.smartcampus.test.unit.services;

import com.smartcampus.services.CourseService;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.repositories.ProfessorRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.models.Course;
import com.smartcampus.models.Professor;
import com.smartcampus.models.Department;
import com.smartcampus.models.enums.Semester;
import com.smartcampus.models.enums.CourseStatus;
import com.smartcampus.models.enums.CourseLevel;
import com.smartcampus.exceptions.CourseNotFoundException;
import com.smartcampus.exceptions.InvalidCourseDataException;
import com.smartcampus.exceptions.DuplicateCourseException;
import com.smartcampus.exceptions.CourseConflictException;

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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for the CourseService class
 * Tests service layer functionality with mocked dependencies
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Course Service Tests")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private Professor testProfessor;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Set up test department
        testDepartment = new Department();
        testDepartment.setId(1L);
        testDepartment.setCode("CS");
        testDepartment.setName("Computer Science");

        // Set up test professor
        testProfessor = new Professor();
        testProfessor.setId(1L);
        testProfessor.setEmployeeId("EMP2024001");

        // Set up test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("CS101");
        testCourse.setCourseName("Introduction to Programming");
        testCourse.setDescription("Basic programming concepts and techniques");
        testCourse.setDepartment(testDepartment);
        testCourse.setProfessor(testProfessor);
        testCourse.setCredits(3);
        testCourse.setMaxEnrollment(30);
        testCourse.setCurrentEnrollment(15);
        testCourse.setSemester(Semester.FALL);
        testCourse.setYear(2024);
        testCourse.setStartDate(LocalDate.of(2024, 8, 26));
        testCourse.setEndDate(LocalDate.of(2024, 12, 15));
        testCourse.setScheduleDays("MWF");
        testCourse.setStartTime(LocalTime.of(9, 0));
        testCourse.setEndTime(LocalTime.of(9, 50));
        testCourse.setClassroom("Room 101");
        testCourse.setBuilding("CS Building");
        testCourse.setCourseLevel(CourseLevel.UNDERGRADUATE);
        testCourse.setStatus(CourseStatus.ACTIVE);
    }

    @Nested
    @DisplayName("Create Course Tests")
    class CreateCourseTests {

        @Test
        @DisplayName("Should create course successfully")
        void shouldCreateCourseSuccessfully() {
            // Arrange
            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(false);
            when(professorRepository.findById(testProfessor.getId())).thenReturn(Optional.of(testProfessor));
            when(departmentRepository.findById(testDepartment.getId())).thenReturn(Optional.of(testDepartment));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.createCourse(testCourse);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCourseCode()).isEqualTo("CS101");
            assertThat(result.getStatus()).isEqualTo(CourseStatus.ACTIVE);

            verify(courseRepository).existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear());
            verify(professorRepository).findById(testProfessor.getId());
            verify(departmentRepository).findById(testDepartment.getId());
            verify(courseRepository).save(any(Course.class));
        }

        @Test
        @DisplayName("Should throw exception when course already exists")
        void shouldThrowExceptionWhenCourseAlreadyExists() {
            // Arrange
            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(true);

            // Act & Assert
            assertThrows(DuplicateCourseException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(courseRepository).existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear());
            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should throw exception when professor not found")
        void shouldThrowExceptionWhenProfessorNotFound() {
            // Arrange
            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(false);
            when(professorRepository.findById(testProfessor.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(professorRepository).findById(testProfessor.getId());
            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void shouldThrowExceptionWhenDepartmentNotFound() {
            // Arrange
            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(false);
            when(professorRepository.findById(testProfessor.getId())).thenReturn(Optional.of(testProfessor));
            when(departmentRepository.findById(testDepartment.getId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(departmentRepository).findById(testDepartment.getId());
            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should throw exception when course data is invalid")
        void shouldThrowExceptionWhenCourseDataIsInvalid() {
            // Arrange
            testCourse.setCourseCode(null); // Invalid data

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should check for professor schedule conflicts")
        void shouldCheckForProfessorScheduleConflicts() {
            // Arrange
            Course conflictingCourse = new Course();
            conflictingCourse.setProfessor(testProfessor);
            conflictingCourse.setScheduleDays("MWF");
            conflictingCourse.setStartTime(LocalTime.of(8, 30));
            conflictingCourse.setEndTime(LocalTime.of(9, 30));
            conflictingCourse.setSemester(Semester.FALL);
            conflictingCourse.setYear(2024);

            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(false);
            when(professorRepository.findById(testProfessor.getId())).thenReturn(Optional.of(testProfessor));
            when(departmentRepository.findById(testDepartment.getId())).thenReturn(Optional.of(testDepartment));
            when(courseRepository.findByProfessorIdAndSemesterAndYear(
                testProfessor.getId(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(Arrays.asList(conflictingCourse));

            // Act & Assert
            assertThrows(CourseConflictException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(courseRepository).findByProfessorIdAndSemesterAndYear(
                testProfessor.getId(), testCourse.getSemester(), testCourse.getYear());
            verify(courseRepository, never()).save(any(Course.class));
        }
    }

    @Nested
    @DisplayName("Find Course Tests")
    class FindCourseTests {

        @Test
        @DisplayName("Should find course by ID successfully")
        void shouldFindCourseByIdSuccessfully() {
            // Arrange
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            // Act
            Course result = courseService.findCourseById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCourseCode()).isEqualTo("CS101");

            verify(courseRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when course not found by ID")
        void shouldThrowExceptionWhenCourseNotFoundById() {
            // Arrange
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> {
                courseService.findCourseById(999L);
            });

            verify(courseRepository).findById(999L);
        }

        @Test
        @DisplayName("Should find course by code, semester, and year")
        void shouldFindCourseByCodeSemesterAndYear() {
            // Arrange
            when(courseRepository.findByCourseCodeAndSemesterAndYear("CS101", Semester.FALL, 2024))
                .thenReturn(Optional.of(testCourse));

            // Act
            Course result = courseService.findCourseByCodeSemesterYear("CS101", Semester.FALL, 2024);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCourseCode()).isEqualTo("CS101");
            assertThat(result.getSemester()).isEqualTo(Semester.FALL);
            assertThat(result.getYear()).isEqualTo(2024);

            verify(courseRepository).findByCourseCodeAndSemesterAndYear("CS101", Semester.FALL, 2024);
        }

        @Test
        @DisplayName("Should return null when course not found by code")
        void shouldReturnNullWhenCourseNotFoundByCode() {
            // Arrange
            when(courseRepository.findByCourseCodeAndSemesterAndYear("NONEXISTENT", Semester.FALL, 2024))
                .thenReturn(Optional.empty());

            // Act
            Course result = courseService.findCourseByCodeSemesterYear("NONEXISTENT", Semester.FALL, 2024);

            // Assert
            assertThat(result).isNull();

            verify(courseRepository).findByCourseCodeAndSemesterAndYear("NONEXISTENT", Semester.FALL, 2024);
        }
    }

    @Nested
    @DisplayName("Update Course Tests")
    class UpdateCourseTests {

        @Test
        @DisplayName("Should update course successfully")
        void shouldUpdateCourseSuccessfully() {
            // Arrange
            Course updatedCourse = new Course();
            updatedCourse.setId(1L);
            updatedCourse.setCourseCode("CS101");
            updatedCourse.setCourseName("Advanced Programming");
            updatedCourse.setMaxEnrollment(40);
            updatedCourse.setDepartment(testDepartment);
            updatedCourse.setProfessor(testProfessor);

            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);

            // Act
            Course result = courseService.updateCourse(1L, updatedCourse);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCourseName()).isEqualTo("Advanced Programming");
            assertThat(result.getMaxEnrollment()).isEqualTo(40);

            verify(courseRepository).findById(1L);
            verify(courseRepository).save(any(Course.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent course")
        void shouldThrowExceptionWhenUpdatingNonExistentCourse() {
            // Arrange
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> {
                courseService.updateCourse(999L, testCourse);
            });

            verify(courseRepository).findById(999L);
            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should update course status")
        void shouldUpdateCourseStatus() {
            // Arrange
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.updateCourseStatus(1L, CourseStatus.CANCELLED);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(courseCaptor.capture());
            assertThat(courseCaptor.getValue().getStatus()).isEqualTo(CourseStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should update course enrollment")
        void shouldUpdateCourseEnrollment() {
            // Arrange
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.updateCourseEnrollment(1L, 20);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(courseCaptor.capture());
            assertThat(courseCaptor.getValue().getCurrentEnrollment()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should not update enrollment beyond maximum")
        void shouldNotUpdateEnrollmentBeyondMaximum() {
            // Arrange
            testCourse.setMaxEnrollment(30);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.updateCourseEnrollment(1L, 35);
            });

            verify(courseRepository, never()).save(any(Course.class));
        }
    }

    @Nested
    @DisplayName("Delete Course Tests")
    class DeleteCourseTests {

        @Test
        @DisplayName("Should delete course successfully")
        void shouldDeleteCourseSuccessfully() {
            // Arrange
            testCourse.setCurrentEnrollment(0); // No students enrolled
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            doNothing().when(courseRepository).delete(testCourse);

            // Act
            courseService.deleteCourse(1L);

            // Assert
            verify(courseRepository).findById(1L);
            verify(courseRepository).delete(testCourse);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent course")
        void shouldThrowExceptionWhenDeletingNonExistentCourse() {
            // Arrange
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(CourseNotFoundException.class, () -> {
                courseService.deleteCourse(999L);
            });

            verify(courseRepository).findById(999L);
            verify(courseRepository, never()).delete(any(Course.class));
        }

        @Test
        @DisplayName("Should not delete course with enrolled students")
        void shouldNotDeleteCourseWithEnrolledStudents() {
            // Arrange
            testCourse.setCurrentEnrollment(15); // Students enrolled
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.deleteCourse(1L);
            });

            verify(courseRepository).findById(1L);
            verify(courseRepository, never()).delete(any(Course.class));
        }

        @Test
        @DisplayName("Should cancel course instead of deleting with enrollments")
        void shouldCancelCourseInsteadOfDeletingWithEnrollments() {
            // Arrange
            testCourse.setCurrentEnrollment(15);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.cancelCourse(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(courseCaptor.capture());
            assertThat(courseCaptor.getValue().getStatus()).isEqualTo(CourseStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("List Courses Tests")
    class ListCoursesTests {

        @Test
        @DisplayName("Should get all courses")
        void shouldGetAllCourses() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findAll()).thenReturn(courses);

            // Act
            List<Course> result = courseService.getAllCourses();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseCode()).isEqualTo("CS101");

            verify(courseRepository).findAll();
        }

        @Test
        @DisplayName("Should get courses by department")
        void shouldGetCoursesByDepartment() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findByDepartmentId(1L)).thenReturn(courses);

            // Act
            List<Course> result = courseService.getCoursesByDepartment(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment().getId()).isEqualTo(1L);

            verify(courseRepository).findByDepartmentId(1L);
        }

        @Test
        @DisplayName("Should get courses by professor")
        void shouldGetCoursesByProfessor() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findByProfessorId(1L)).thenReturn(courses);

            // Act
            List<Course> result = courseService.getCoursesByProfessor(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProfessor().getId()).isEqualTo(1L);

            verify(courseRepository).findByProfessorId(1L);
        }

        @Test
        @DisplayName("Should get courses by semester and year")
        void shouldGetCoursesBySemesterAndYear() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findBySemesterAndYear(Semester.FALL, 2024)).thenReturn(courses);

            // Act
            List<Course> result = courseService.getCoursesBySemesterAndYear(Semester.FALL, 2024);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSemester()).isEqualTo(Semester.FALL);
            assertThat(result.get(0).getYear()).isEqualTo(2024);

            verify(courseRepository).findBySemesterAndYear(Semester.FALL, 2024);
        }

        @Test
        @DisplayName("Should get courses by status")
        void shouldGetCoursesByStatus() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findByStatus(CourseStatus.ACTIVE)).thenReturn(courses);

            // Act
            List<Course> result = courseService.getCoursesByStatus(CourseStatus.ACTIVE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(CourseStatus.ACTIVE);

            verify(courseRepository).findByStatus(CourseStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return empty list when no courses found")
        void shouldReturnEmptyListWhenNoCoursesFound() {
            // Arrange
            when(courseRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<Course> result = courseService.getAllCourses();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(courseRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Search Courses Tests")
    class SearchCoursesTests {

        @Test
        @DisplayName("Should search courses by name")
        void shouldSearchCoursesByName() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findByCourseNameContainingIgnoreCase("Programming"))
                .thenReturn(courses);

            // Act
            List<Course> result = courseService.searchCoursesByName("Programming");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseName()).contains("Programming");

            verify(courseRepository).findByCourseNameContainingIgnoreCase("Programming");
        }

        @Test
        @DisplayName("Should search courses by code")
        void shouldSearchCoursesByCode() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findByCourseCodeContainingIgnoreCase("CS"))
                .thenReturn(courses);

            // Act
            List<Course> result = courseService.searchCoursesByCode("CS");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourseCode()).contains("CS");

            verify(courseRepository).findByCourseCodeContainingIgnoreCase("CS");
        }

        @Test
        @DisplayName("Should search courses with advanced filters")
        void shouldSearchCoursesWithAdvancedFilters() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findByDepartmentIdAndSemesterAndYearAndStatus(
                1L, Semester.FALL, 2024, CourseStatus.ACTIVE))
                .thenReturn(courses);

            // Act
            List<Course> result = courseService.searchCoursesAdvanced(1L, Semester.FALL, 2024, CourseStatus.ACTIVE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDepartment().getId()).isEqualTo(1L);
            assertThat(result.get(0).getSemester()).isEqualTo(Semester.FALL);
            assertThat(result.get(0).getYear()).isEqualTo(2024);
            assertThat(result.get(0).getStatus()).isEqualTo(CourseStatus.ACTIVE);

            verify(courseRepository).findByDepartmentIdAndSemesterAndYearAndStatus(
                1L, Semester.FALL, 2024, CourseStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should get available courses for enrollment")
        void shouldGetAvailableCoursesForEnrollment() {
            // Arrange
            List<Course> courses = Arrays.asList(testCourse);
            when(courseRepository.findAvailableCoursesForEnrollment()).thenReturn(courses);

            // Act
            List<Course> result = courseService.getAvailableCoursesForEnrollment();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertTrue(result.get(0).hasAvailableSpots());

            verify(courseRepository).findAvailableCoursesForEnrollment();
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get total course count")
        void shouldGetTotalCourseCount() {
            // Arrange
            when(courseRepository.count()).thenReturn(50L);

            // Act
            long result = courseService.getTotalCourseCount();

            // Assert
            assertThat(result).isEqualTo(50L);

            verify(courseRepository).count();
        }

        @Test
        @DisplayName("Should get course count by department")
        void shouldGetCourseCountByDepartment() {
            // Arrange
            when(courseRepository.countByDepartmentId(1L)).thenReturn(15L);

            // Act
            long result = courseService.getCourseCountByDepartment(1L);

            // Assert
            assertThat(result).isEqualTo(15L);

            verify(courseRepository).countByDepartmentId(1L);
        }

        @Test
        @DisplayName("Should get course count by professor")
        void shouldGetCourseCountByProfessor() {
            // Arrange
            when(courseRepository.countByProfessorId(1L)).thenReturn(4L);

            // Act
            long result = courseService.getCourseCountByProfessor(1L);

            // Assert
            assertThat(result).isEqualTo(4L);

            verify(courseRepository).countByProfessorId(1L);
        }

        @Test
        @DisplayName("Should get average enrollment by department")
        void shouldGetAverageEnrollmentByDepartment() {
            // Arrange
            when(courseRepository.getAverageEnrollmentByDepartment(1L)).thenReturn(22.5);

            // Act
            double result = courseService.getAverageEnrollmentByDepartment(1L);

            // Assert
            assertThat(result).isEqualTo(22.5, within(0.01));

            verify(courseRepository).getAverageEnrollmentByDepartment(1L);
        }
    }

    @Nested
    @DisplayName("Enrollment Management Tests")
    class EnrollmentManagementTests {

        @Test
        @DisplayName("Should increment course enrollment")
        void shouldIncrementCourseEnrollment() {
            // Arrange
            int initialEnrollment = testCourse.getCurrentEnrollment();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.incrementEnrollment(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(courseCaptor.capture());
            assertThat(courseCaptor.getValue().getCurrentEnrollment()).isEqualTo(initialEnrollment + 1);
        }

        @Test
        @DisplayName("Should not increment enrollment when course is full")
        void shouldNotIncrementEnrollmentWhenCourseIsFull() {
            // Arrange
            testCourse.setCurrentEnrollment(30);
            testCourse.setMaxEnrollment(30);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.incrementEnrollment(1L);
            });

            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should decrement course enrollment")
        void shouldDecrementCourseEnrollment() {
            // Arrange
            int initialEnrollment = testCourse.getCurrentEnrollment();
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.decrementEnrollment(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(courseCaptor.capture());
            assertThat(courseCaptor.getValue().getCurrentEnrollment()).isEqualTo(initialEnrollment - 1);
        }

        @Test
        @DisplayName("Should not decrement enrollment below zero")
        void shouldNotDecrementEnrollmentBelowZero() {
            // Arrange
            testCourse.setCurrentEnrollment(0);
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(courseRepository.save(any(Course.class))).thenReturn(testCourse);

            // Act
            Course result = courseService.decrementEnrollment(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
            verify(courseRepository).save(courseCaptor.capture());
            assertThat(courseCaptor.getValue().getCurrentEnrollment()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate course data before creation")
        void shouldValidateCourseDataBeforeCreation() {
            // Arrange
            Course invalidCourse = new Course();
            invalidCourse.setCourseCode(""); // Invalid empty code
            invalidCourse.setCourseName("Test Course");

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.createCourse(invalidCourse);
            });

            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should validate credits range")
        void shouldValidateCreditsRange() {
            // Arrange
            testCourse.setCredits(0); // Invalid credits
            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(false);

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(courseRepository, never()).save(any(Course.class));
        }

        @Test
        @DisplayName("Should validate enrollment capacity")
        void shouldValidateEnrollmentCapacity() {
            // Arrange
            testCourse.setMaxEnrollment(0); // Invalid max enrollment
            when(courseRepository.existsByCourseCodeAndSemesterAndYear(
                testCourse.getCourseCode(), testCourse.getSemester(), testCourse.getYear()))
                .thenReturn(false);

            // Act & Assert
            assertThrows(InvalidCourseDataException.class, () -> {
                courseService.createCourse(testCourse);
            });

            verify(courseRepository, never()).save(any(Course.class));
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Arrange
            when(courseRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                courseService.findCourseById(1L);
            });

            verify(courseRepository).findById(1L);
        }

        @Test
        @DisplayName("Should handle null input parameters")
        void shouldHandleNullInputParameters() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                courseService.createCourse(null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                courseService.findCourseById(null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                courseService.updateCourse(null, testCourse);
            });
        }
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}