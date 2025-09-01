// File location: src/test/java/unit/services/EnrollmentServiceTest.java

package com.smartcampus.test.unit.services;

import com.smartcampus.services.EnrollmentService;
import com.smartcampus.repositories.EnrollmentRepository;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.enums.EnrollmentStatus;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.models.enums.CourseStatus;
import com.smartcampus.exceptions.EnrollmentNotFoundException;
import com.smartcampus.exceptions.InvalidEnrollmentException;
import com.smartcampus.exceptions.EnrollmentConflictException;
import com.smartcampus.exceptions.CourseFullException;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for the EnrollmentService class
 * Tests service layer functionality with mocked dependencies
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Enrollment Service Tests")
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Enrollment testEnrollment;
    private Student testStudent;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        // Set up test student
        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setStudentId("CS2024001");
        testStudent.setStatus(StudentStatus.ACTIVE);
        testStudent.setYearLevel(2);
        testStudent.setTotalCreditsEarned(30);

        // Set up test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setCourseCode("CS201");
        testCourse.setCourseName("Data Structures");
        testCourse.setCredits(4);
        testCourse.setMaxEnrollment(30);
        testCourse.setCurrentEnrollment(15);
        testCourse.setStatus(CourseStatus.ACTIVE);

        // Set up test enrollment
        testEnrollment = new Enrollment();
        testEnrollment.setId(1L);
        testEnrollment.setStudent(testStudent);
        testEnrollment.setCourse(testCourse);
        testEnrollment.setEnrollmentDate(LocalDateTime.now());
        testEnrollment.setStatus(EnrollmentStatus.ENROLLED);
        testEnrollment.setMidtermGrade("B");
        testEnrollment.setAttendancePercentage(95.0);
        testEnrollment.setParticipationScore(88.0);
    }

    @Nested
    @DisplayName("Enroll Student Tests")
    class EnrollStudentTests {

        @Test
        @DisplayName("Should enroll student successfully")
        void shouldEnrollStudentSuccessfully() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 1L)).thenReturn(false);
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.enrollStudent(1L, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStudent()).isEqualTo(testStudent);
            assertThat(result.getCourse()).isEqualTo(testCourse);
            assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            verify(studentRepository).findById(1L);
            verify(courseRepository).findById(1L);
            verify(enrollmentRepository).existsByStudentIdAndCourseId(1L, 1L);
            verify(enrollmentRepository).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should throw exception when student not found")
        void shouldThrowExceptionWhenStudentNotFound() {
            // Arrange
            when(studentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.enrollStudent(999L, 1L);
            });

            verify(studentRepository).findById(999L);
            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should throw exception when course not found")
        void shouldThrowExceptionWhenCourseNotFound() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.enrollStudent(1L, 999L);
            });

            verify(courseRepository).findById(999L);
            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should throw exception when student is inactive")
        void shouldThrowExceptionWhenStudentIsInactive() {
            // Arrange
            testStudent.setStatus(StudentStatus.SUSPENDED);
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.enrollStudent(1L, 1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should throw exception when course is inactive")
        void shouldThrowExceptionWhenCourseIsInactive() {
            // Arrange
            testCourse.setStatus(CourseStatus.CANCELLED);
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.enrollStudent(1L, 1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should throw exception when course is full")
        void shouldThrowExceptionWhenCourseIsFull() {
            // Arrange
            testCourse.setCurrentEnrollment(30);
            testCourse.setMaxEnrollment(30);
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));

            // Act & Assert
            assertThrows(CourseFullException.class, () -> {
                enrollmentService.enrollStudent(1L, 1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should throw exception when student already enrolled")
        void shouldThrowExceptionWhenStudentAlreadyEnrolled() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 1L)).thenReturn(true);

            // Act & Assert
            assertThrows(EnrollmentConflictException.class, () -> {
                enrollmentService.enrollStudent(1L, 1L);
            });

            verify(enrollmentRepository).existsByStudentIdAndCourseId(1L, 1L);
            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should check prerequisites before enrollment")
        void shouldCheckPrerequisitesBeforeEnrollment() {
            // Arrange
            testCourse.setPrerequisites("CS101, MATH101");
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 1L)).thenReturn(false);
            when(enrollmentService.hasCompletedPrerequisites(1L, "CS101, MATH101")).thenReturn(false);

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.enrollStudent(1L, 1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should check credit load limits")
        void shouldCheckCreditLoadLimits() {
            // Arrange
            testStudent.setTotalCreditsEarned(120); // Near graduation
            testCourse.setCredits(8); // High credit course
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 1L)).thenReturn(false);
            when(enrollmentService.getCurrentSemesterCredits(1L)).thenReturn(16); // Already at limit

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.enrollStudent(1L, 1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }
    }

    @Nested
    @DisplayName("Drop Enrollment Tests")
    class DropEnrollmentTests {

        @Test
        @DisplayName("Should drop enrollment successfully")
        void shouldDropEnrollmentSuccessfully() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.dropEnrollment(1L);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(enrollmentCaptor.capture());
            assertThat(enrollmentCaptor.getValue().getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
            assertThat(enrollmentCaptor.getValue().getDropDate()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when enrollment not found")
        void shouldThrowExceptionWhenEnrollmentNotFound() {
            // Arrange
            when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EnrollmentNotFoundException.class, () -> {
                enrollmentService.dropEnrollment(999L);
            });

            verify(enrollmentRepository).findById(999L);
            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should not allow dropping after deadline")
        void shouldNotAllowDroppingAfterDeadline() {
            // Arrange
            testEnrollment.setEnrollmentDate(LocalDateTime.now().minusWeeks(10)); // Old enrollment
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.dropEnrollment(1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should not allow dropping already dropped enrollment")
        void shouldNotAllowDroppingAlreadyDroppedEnrollment() {
            // Arrange
            testEnrollment.setStatus(EnrollmentStatus.DROPPED);
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.dropEnrollment(1L);
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }
    }

    @Nested
    @DisplayName("Find Enrollment Tests")
    class FindEnrollmentTests {

        @Test
        @DisplayName("Should find enrollment by ID successfully")
        void shouldFindEnrollmentByIdSuccessfully() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

            // Act
            Enrollment result = enrollmentService.findEnrollmentById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStudent()).isEqualTo(testStudent);
            assertThat(result.getCourse()).isEqualTo(testCourse);

            verify(enrollmentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when enrollment not found by ID")
        void shouldThrowExceptionWhenEnrollmentNotFoundById() {
            // Arrange
            when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EnrollmentNotFoundException.class, () -> {
                enrollmentService.findEnrollmentById(999L);
            });

            verify(enrollmentRepository).findById(999L);
        }

        @Test
        @DisplayName("Should find enrollment by student and course")
        void shouldFindEnrollmentByStudentAndCourse() {
            // Arrange
            when(enrollmentRepository.findByStudentIdAndCourseId(1L, 1L))
                .thenReturn(Optional.of(testEnrollment));

            // Act
            Enrollment result = enrollmentService.findEnrollmentByStudentAndCourse(1L, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStudent().getId()).isEqualTo(1L);
            assertThat(result.getCourse().getId()).isEqualTo(1L);

            verify(enrollmentRepository).findByStudentIdAndCourseId(1L, 1L);
        }

        @Test
        @DisplayName("Should return null when enrollment not found by student and course")
        void shouldReturnNullWhenEnrollmentNotFoundByStudentAndCourse() {
            // Arrange
            when(enrollmentRepository.findByStudentIdAndCourseId(999L, 999L))
                .thenReturn(Optional.empty());

            // Act
            Enrollment result = enrollmentService.findEnrollmentByStudentAndCourse(999L, 999L);

            // Assert
            assertThat(result).isNull();

            verify(enrollmentRepository).findByStudentIdAndCourseId(999L, 999L);
        }
    }

    @Nested
    @DisplayName("List Enrollments Tests")
    class ListEnrollmentsTests {

        @Test
        @DisplayName("Should get all enrollments")
        void shouldGetAllEnrollments() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findAll()).thenReturn(enrollments);

            // Act
            List<Enrollment> result = enrollmentService.getAllEnrollments();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);

            verify(enrollmentRepository).findAll();
        }

        @Test
        @DisplayName("Should get enrollments by student")
        void shouldGetEnrollmentsByStudent() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findByStudentId(1L)).thenReturn(enrollments);

            // Act
            List<Enrollment> result = enrollmentService.getEnrollmentsByStudent(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudent().getId()).isEqualTo(1L);

            verify(enrollmentRepository).findByStudentId(1L);
        }

        @Test
        @DisplayName("Should get enrollments by course")
        void shouldGetEnrollmentsByCourse() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findByCourseId(1L)).thenReturn(enrollments);

            // Act
            List<Enrollment> result = enrollmentService.getEnrollmentsByCourse(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCourse().getId()).isEqualTo(1L);

            verify(enrollmentRepository).findByCourseId(1L);
        }

        @Test
        @DisplayName("Should get enrollments by status")
        void shouldGetEnrollmentsByStatus() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findByStatus(EnrollmentStatus.ENROLLED)).thenReturn(enrollments);

            // Act
            List<Enrollment> result = enrollmentService.getEnrollmentsByStatus(EnrollmentStatus.ENROLLED);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            verify(enrollmentRepository).findByStatus(EnrollmentStatus.ENROLLED);
        }

        @Test
        @DisplayName("Should get active enrollments for student")
        void shouldGetActiveEnrollmentsForStudent() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(enrollments);

            // Act
            List<Enrollment> result = enrollmentService.getActiveEnrollmentsForStudent(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudent().getId()).isEqualTo(1L);
            assertThat(result.get(0).getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            verify(enrollmentRepository).findByStudentIdAndStatus(1L, EnrollmentStatus.ENROLLED);
        }

        @Test
        @DisplayName("Should return empty list when no enrollments found")
        void shouldReturnEmptyListWhenNoEnrollmentsFound() {
            // Arrange
            when(enrollmentRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<Enrollment> result = enrollmentService.getAllEnrollments();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(enrollmentRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Update Enrollment Tests")
    class UpdateEnrollmentTests {

        @Test
        @DisplayName("Should update enrollment status")
        void shouldUpdateEnrollmentStatus() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.updateEnrollmentStatus(1L, EnrollmentStatus.COMPLETED);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(enrollmentCaptor.capture());
            assertThat(enrollmentCaptor.getValue().getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should update midterm grade")
        void shouldUpdateMidtermGrade() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.updateMidtermGrade(1L, "A-");

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(enrollmentCaptor.capture());
            assertThat(enrollmentCaptor.getValue().getMidtermGrade()).isEqualTo("A-");
        }

        @Test
        @DisplayName("Should update attendance percentage")
        void shouldUpdateAttendancePercentage() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.updateAttendancePercentage(1L, 92.5);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(enrollmentCaptor.capture());
            assertThat(enrollmentCaptor.getValue().getAttendancePercentage()).isEqualTo(92.5);
        }

        @Test
        @DisplayName("Should update participation score")
        void shouldUpdateParticipationScore() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.updateParticipationScore(1L, 90.0);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(enrollmentCaptor.capture());
            assertThat(enrollmentCaptor.getValue().getParticipationScore()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should add notes to enrollment")
        void shouldAddNotesToEnrollment() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));
            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(testEnrollment);

            // Act
            Enrollment result = enrollmentService.addNotes(1L, "Student shows excellent progress");

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(enrollmentCaptor.capture());
            assertThat(enrollmentCaptor.getValue().getNotes()).isEqualTo("Student shows excellent progress");
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get total enrollment count")
        void shouldGetTotalEnrollmentCount() {
            // Arrange
            when(enrollmentRepository.count()).thenReturn(150L);

            // Act
            long result = enrollmentService.getTotalEnrollmentCount();

            // Assert
            assertThat(result).isEqualTo(150L);

            verify(enrollmentRepository).count();
        }

        @Test
        @DisplayName("Should get enrollment count by status")
        void shouldGetEnrollmentCountByStatus() {
            // Arrange
            when(enrollmentRepository.countByStatus(EnrollmentStatus.ENROLLED)).thenReturn(120L);

            // Act
            long result = enrollmentService.getEnrollmentCountByStatus(EnrollmentStatus.ENROLLED);

            // Assert
            assertThat(result).isEqualTo(120L);

            verify(enrollmentRepository).countByStatus(EnrollmentStatus.ENROLLED);
        }

        @Test
        @DisplayName("Should get enrollment count by course")
        void shouldGetEnrollmentCountByCourse() {
            // Arrange
            when(enrollmentRepository.countByCourseId(1L)).thenReturn(25L);

            // Act
            long result = enrollmentService.getEnrollmentCountByCourse(1L);

            // Assert
            assertThat(result).isEqualTo(25L);

            verify(enrollmentRepository).countByCourseId(1L);
        }

        @Test
        @DisplayName("Should get enrollment count by student")
        void shouldGetEnrollmentCountByStudent() {
            // Arrange
            when(enrollmentRepository.countByStudentId(1L)).thenReturn(5L);

            // Act
            long result = enrollmentService.getEnrollmentCountByStudent(1L);

            // Assert
            assertThat(result).isEqualTo(5L);

            verify(enrollmentRepository).countByStudentId(1L);
        }

        @Test
        @DisplayName("Should get average attendance by course")
        void shouldGetAverageAttendanceByCourse() {
            // Arrange
            when(enrollmentRepository.getAverageAttendancePercentageByCourse(1L)).thenReturn(88.5);

            // Act
            double result = enrollmentService.getAverageAttendanceByCourse(1L);

            // Assert
            assertThat(result).isEqualTo(88.5, within(0.01));

            verify(enrollmentRepository).getAverageAttendancePercentageByCourse(1L);
        }

        @Test
        @DisplayName("Should get completion rate by course")
        void shouldGetCompletionRateByCourse() {
            // Arrange
            when(enrollmentRepository.countByCourseIdAndStatus(1L, EnrollmentStatus.COMPLETED)).thenReturn(20L);
            when(enrollmentRepository.countByCourseId(1L)).thenReturn(25L);

            // Act
            double result = enrollmentService.getCompletionRateByCourse(1L);

            // Assert
            double expected = (20.0 / 25.0) * 100; // 80%
            assertThat(result).isEqualTo(expected, within(0.01));

            verify(enrollmentRepository).countByCourseIdAndStatus(1L, EnrollmentStatus.COMPLETED);
            verify(enrollmentRepository).countByCourseId(1L);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should check if student can enroll in course")
        void shouldCheckIfStudentCanEnrollInCourse() {
            // Arrange
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 1L)).thenReturn(false);

            // Act
            boolean result = enrollmentService.canStudentEnroll(1L, 1L);

            // Assert
            assertTrue(result);

            verify(studentRepository).findById(1L);
            verify(courseRepository).findById(1L);
            verify(enrollmentRepository).existsByStudentIdAndCourseId(1L, 1L);
        }

        @Test
        @DisplayName("Should not allow enrollment when student is inactive")
        void shouldNotAllowEnrollmentWhenStudentIsInactive() {
            // Arrange
            testStudent.setStatus(StudentStatus.SUSPENDED);
            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

            // Act
            boolean result = enrollmentService.canStudentEnroll(1L, 1L);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should calculate current semester credit load")
        void shouldCalculateCurrentSemesterCreditLoad() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findActiveEnrollmentsForCurrentSemester(1L))
                .thenReturn(enrollments);

            // Act
            int result = enrollmentService.getCurrentSemesterCredits(1L);

            // Assert
            assertThat(result).isEqualTo(4); // From testCourse.credits

            verify(enrollmentRepository).findActiveEnrollmentsForCurrentSemester(1L);
        }

        @Test
        @DisplayName("Should determine if student has completed prerequisites")
        void shouldDetermineIfStudentHasCompletedPrerequisites() {
            // Arrange
            when(enrollmentRepository.findCompletedCoursesByStudent(1L))
                .thenReturn(Arrays.asList("CS101", "MATH101"));

            // Act
            boolean result = enrollmentService.hasCompletedPrerequisites(1L, "CS101, MATH101");

            // Assert
            assertTrue(result);

            verify(enrollmentRepository).findCompletedCoursesByStudent(1L);
        }

        @Test
        @DisplayName("Should detect schedule conflicts")
        void shouldDetectScheduleConflicts() {
            // Arrange
            Course conflictingCourse = new Course();
            conflictingCourse.setScheduleDays("MWF");
            conflictingCourse.setStartTime(testCourse.getStartTime());
            conflictingCourse.setEndTime(testCourse.getEndTime());

            Enrollment conflictingEnrollment = new Enrollment();
            conflictingEnrollment.setCourse(conflictingCourse);

            when(enrollmentRepository.findActiveEnrollmentsForCurrentSemester(1L))
                .thenReturn(Arrays.asList(conflictingEnrollment));

            // Act
            boolean result = enrollmentService.hasScheduleConflict(1L, testCourse);

            // Assert
            assertTrue(result);

            verify(enrollmentRepository).findActiveEnrollmentsForCurrentSemester(1L);
        }

        @Test
        @DisplayName("Should withdraw student from all courses")
        void shouldWithdrawStudentFromAllCourses() {
            // Arrange
            List<Enrollment> enrollments = Arrays.asList(testEnrollment);
            when(enrollmentRepository.findByStudentIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(enrollments);
            when(enrollmentRepository.saveAll(anyList())).thenReturn(enrollments);

            // Act
            List<Enrollment> result = enrollmentService.withdrawStudentFromAllCourses(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(enrollmentRepository).findByStudentIdAndStatus(1L, EnrollmentStatus.ENROLLED);
            verify(enrollmentRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {

        @Test
        @DisplayName("Should enroll multiple students in course")
        void shouldEnrollMultipleStudentsInCourse() {
            // Arrange
            List<Long> studentIds = Arrays.asList(1L, 2L, 3L);
            Student student2 = new Student();
            student2.setId(2L);
            student2.setStatus(StudentStatus.ACTIVE);
            
            Student student3 = new Student();
            student3.setId(3L);
            student3.setStatus(StudentStatus.ACTIVE);

            when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
            when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
            when(studentRepository.findById(3L)).thenReturn(Optional.of(student3));
            when(courseRepository.findById(1L)).thenReturn(Optional.of(testCourse));
            when(enrollmentRepository.existsByStudentIdAndCourseId(anyLong(), eq(1L))).thenReturn(false);
            when(enrollmentRepository.saveAll(anyList())).thenReturn(Arrays.asList(testEnrollment));

            // Act
            List<Enrollment> result = enrollmentService.enrollMultipleStudents(studentIds, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);

            verify(enrollmentRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should update enrollment statuses in batch")
        void shouldUpdateEnrollmentStatusesInBatch() {
            // Arrange
            List<Long> enrollmentIds = Arrays.asList(1L, 2L, 3L);
            when(enrollmentRepository.updateStatusByIds(enrollmentIds, EnrollmentStatus.COMPLETED))
                .thenReturn(3);

            // Act
            int result = enrollmentService.updateEnrollmentStatusBatch(enrollmentIds, EnrollmentStatus.COMPLETED);

            // Assert
            assertThat(result).isEqualTo(3);

            verify(enrollmentRepository).updateStatusByIds(enrollmentIds, EnrollmentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate attendance percentage range")
        void shouldValidateAttendancePercentageRange() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.updateAttendancePercentage(1L, 150.0); // Invalid > 100
            });

            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.updateAttendancePercentage(1L, -10.0); // Invalid < 0
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should validate participation score range")
        void shouldValidateParticipationScoreRange() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.updateParticipationScore(1L, 110.0); // Invalid > 100
            });

            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.updateParticipationScore(1L, -5.0); // Invalid < 0
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }

        @Test
        @DisplayName("Should validate midterm grade format")
        void shouldValidateMidtermGradeFormat() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(testEnrollment));

            // Act & Assert
            assertThrows(InvalidEnrollmentException.class, () -> {
                enrollmentService.updateMidtermGrade(1L, "Z"); // Invalid grade
            });

            verify(enrollmentRepository, never()).save(any(Enrollment.class));
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Arrange
            when(enrollmentRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                enrollmentService.findEnrollmentById(1L);
            });

            verify(enrollmentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should handle null input parameters")
        void shouldHandleNullInputParameters() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                enrollmentService.enrollStudent(null, 1L);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                enrollmentService.enrollStudent(1L, null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                enrollmentService.findEnrollmentById(null);
            });
        }
    }

    private static double within(double tolerance) {
        return tolerance;
    }
}