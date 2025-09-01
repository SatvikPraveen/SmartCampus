// File location: src/test/java/functional/EnrollmentFlowTest.java

package com.smartcampus.test.functional;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Department;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.User;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.models.enums.EnrollmentStatus;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.repositories.EnrollmentRepository;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.services.EnrollmentService;
import com.smartcampus.services.NotificationService;
import com.smartcampus.services.EmailService;
import com.smartcampus.exceptions.EnrollmentException;
import com.smartcampus.exceptions.CourseFullException;
import com.smartcampus.exceptions.PrerequisiteNotMetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Functional tests for the complete enrollment workflow
 * Tests end-to-end enrollment processes including prerequisites, capacity limits, and notifications
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(classes = SmartCampusApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Enrollment Flow Functional Tests")
class EnrollmentFlowTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private EmailService emailService;

    private Department testDepartment;
    private Student testStudent;
    private Course basicCourse;
    private Course advancedCourse;
    private Course prerequisiteCourse;
    private Course limitedCapacityCourse;

    @BeforeEach
    void setUp() {
        // Mock notification services
        doNothing().when(notificationService).sendEnrollmentConfirmation(any(), any());
        doNothing().when(notificationService).sendEnrollmentWaitlistNotification(any(), any());
        doNothing().when(emailService).sendEnrollmentConfirmationEmail(any(), any());

        // Create test data
        setupTestData();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Basic Enrollment Flow")
    @TestMethodOrder(OrderAnnotation.class)
    class BasicEnrollmentFlow {

        @Test
        @Order(1)
        @DisplayName("Should successfully enroll student in available course")
        void shouldSuccessfullyEnrollStudentInAvailableCourse() {
            // Act
            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());

            // Assert
            assertThat(enrollment).isNotNull();
            assertThat(enrollment.getStudent()).isEqualTo(testStudent);
            assertThat(enrollment.getCourse()).isEqualTo(basicCourse);
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
            assertThat(enrollment.getEnrollmentDate()).isEqualTo(LocalDate.now());

            // Verify enrollment is persisted
            List<Enrollment> studentEnrollments = enrollmentRepository.findByStudent(testStudent);
            assertThat(studentEnrollments).hasSize(1);
            assertThat(studentEnrollments.get(0).getCourse()).isEqualTo(basicCourse);

            // Verify course enrollment count updated
            Course updatedCourse = courseRepository.findById(basicCourse.getId()).orElse(null);
            assertThat(updatedCourse).isNotNull();
            assertThat(updatedCourse.getCurrentEnrollment()).isEqualTo(1);

            // Verify notifications sent
            verify(notificationService).sendEnrollmentConfirmation(testStudent, basicCourse);
            verify(emailService).sendEnrollmentConfirmationEmail(testStudent, basicCourse);
        }

        @Test
        @Order(2)
        @DisplayName("Should prevent duplicate enrollment")
        void shouldPreventDuplicateEnrollment() {
            // Arrange - Enroll student first
            enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());

            // Act & Assert
            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            });

            // Verify only one enrollment exists
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(testStudent);
            assertThat(enrollments).hasSize(1);
        }

        @Test
        @Order(3)
        @DisplayName("Should handle course capacity limits")
        void shouldHandleCourseCapacityLimits() {
            // Arrange - Fill the course to capacity
            for (int i = 0; i < limitedCapacityCourse.getMaxEnrollment(); i++) {
                Student student = createTestStudent("LIMIT" + i);
                enrollmentService.enrollStudent(student.getId(), limitedCapacityCourse.getId());
            }

            // Act & Assert - Try to enroll one more student
            assertThrows(CourseFullException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), limitedCapacityCourse.getId());
            });

            // Verify course is at capacity
            Course updatedCourse = courseRepository.findById(limitedCapacityCourse.getId()).orElse(null);
            assertThat(updatedCourse.getCurrentEnrollment()).isEqualTo(limitedCapacityCourse.getMaxEnrollment());
        }

        @Test
        @Order(4)
        @DisplayName("Should handle prerequisite requirements")
        void shouldHandlePrerequisiteRequirements() {
            // Act & Assert - Try to enroll in advanced course without prerequisite
            assertThrows(PrerequisiteNotMetException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), advancedCourse.getId());
            });

            // Complete prerequisite course
            enrollmentService.enrollStudent(testStudent.getId(), prerequisiteCourse.getId());
            enrollmentService.completeEnrollment(testStudent.getId(), prerequisiteCourse.getId(), "A");

            // Now enrollment should succeed
            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), advancedCourse.getId());
            assertThat(enrollment).isNotNull();
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        }
    }

    @Nested
    @DisplayName("Waitlist Management Flow")
    class WaitlistManagementFlow {

        @Test
        @DisplayName("Should add student to waitlist when course is full")
        void shouldAddStudentToWaitlistWhenCourseIsFull() {
            // Arrange - Fill course to capacity
            fillCourseToCapacity(limitedCapacityCourse);

            // Act - Try to enroll another student
            Enrollment waitlistEnrollment = enrollmentService.addToWaitlist(testStudent.getId(), limitedCapacityCourse.getId());

            // Assert
            assertThat(waitlistEnrollment).isNotNull();
            assertThat(waitlistEnrollment.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
            assertThat(waitlistEnrollment.getWaitlistPosition()).isEqualTo(1);

            // Verify waitlist notification sent
            verify(notificationService).sendEnrollmentWaitlistNotification(testStudent, limitedCapacityCourse);
        }

        @Test
        @DisplayName("Should process waitlist when spot becomes available")
        void shouldProcessWaitlistWhenSpotBecomesAvailable() {
            // Arrange - Fill course and add students to waitlist
            List<Student> enrolledStudents = fillCourseToCapacity(limitedCapacityCourse);
            
            // Add students to waitlist
            Student waitlistedStudent1 = createTestStudent("WAIT001");
            Student waitlistedStudent2 = createTestStudent("WAIT002");
            
            enrollmentService.addToWaitlist(waitlistedStudent1.getId(), limitedCapacityCourse.getId());
            enrollmentService.addToWaitlist(waitlistedStudent2.getId(), limitedCapacityCourse.getId());

            // Act - One student drops the course
            Student droppedStudent = enrolledStudents.get(0);
            enrollmentService.dropCourse(droppedStudent.getId(), limitedCapacityCourse.getId());

            // Process waitlist
            enrollmentService.processWaitlist(limitedCapacityCourse.getId());

            // Assert - First waitlisted student should be enrolled
            Enrollment firstWaitlistedEnrollment = enrollmentRepository.findByStudentAndCourse(waitlistedStudent1, limitedCapacityCourse);
            assertThat(firstWaitlistedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            // Second student should still be waitlisted
            Enrollment secondWaitlistedEnrollment = enrollmentRepository.findByStudentAndCourse(waitlistedStudent2, limitedCapacityCourse);
            assertThat(secondWaitlistedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
            assertThat(secondWaitlistedEnrollment.getWaitlistPosition()).isEqualTo(1);

            // Verify enrollment confirmation sent to promoted student
            verify(notificationService).sendEnrollmentConfirmation(waitlistedStudent1, limitedCapacityCourse);
        }

        @Test
        @DisplayName("Should maintain correct waitlist positions")
        void shouldMaintainCorrectWaitlistPositions() {
            // Arrange
            fillCourseToCapacity(limitedCapacityCourse);
            
            Student student1 = createTestStudent("POS001");
            Student student2 = createTestStudent("POS002");
            Student student3 = createTestStudent("POS003");

            // Act - Add students to waitlist
            enrollmentService.addToWaitlist(student1.getId(), limitedCapacityCourse.getId());
            enrollmentService.addToWaitlist(student2.getId(), limitedCapacityCourse.getId());
            enrollmentService.addToWaitlist(student3.getId(), limitedCapacityCourse.getId());

            // Assert positions
            Enrollment enrollment1 = enrollmentRepository.findByStudentAndCourse(student1, limitedCapacityCourse);
            Enrollment enrollment2 = enrollmentRepository.findByStudentAndCourse(student2, limitedCapacityCourse);
            Enrollment enrollment3 = enrollmentRepository.findByStudentAndCourse(student3, limitedCapacityCourse);

            assertThat(enrollment1.getWaitlistPosition()).isEqualTo(1);
            assertThat(enrollment2.getWaitlistPosition()).isEqualTo(2);
            assertThat(enrollment3.getWaitlistPosition()).isEqualTo(3);

            // Act - Remove middle student from waitlist
            enrollmentService.removeFromWaitlist(student2.getId(), limitedCapacityCourse.getId());

            // Assert positions updated
            enrollment1 = enrollmentRepository.findByStudentAndCourse(student1, limitedCapacityCourse);
            enrollment3 = enrollmentRepository.findByStudentAndCourse(student3, limitedCapacityCourse);

            assertThat(enrollment1.getWaitlistPosition()).isEqualTo(1);
            assertThat(enrollment3.getWaitlistPosition()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Course Withdrawal Flow")
    class CourseWithdrawalFlow {

        @Test
        @DisplayName("Should successfully withdraw student from course")
        void shouldSuccessfullyWithdrawStudentFromCourse() {
            // Arrange
            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

            // Act
            enrollmentService.dropCourse(testStudent.getId(), basicCourse.getId());

            // Assert
            Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElse(null);
            assertThat(updatedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
            assertThat(updatedEnrollment.getDropDate()).isEqualTo(LocalDate.now());

            // Verify course enrollment count decreased
            Course updatedCourse = courseRepository.findById(basicCourse.getId()).orElse(null);
            assertThat(updatedCourse.getCurrentEnrollment()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle withdrawal deadline restrictions")
        void shouldHandleWithdrawalDeadlineRestrictions() {
            // Arrange - Create course with past withdrawal deadline
            Course restrictedCourse = createTestCourse("RESTRICTED101", "Course with Past Deadline");
            restrictedCourse.setWithdrawalDeadline(LocalDate.now().minusDays(7));
            restrictedCourse = courseRepository.save(restrictedCourse);

            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), restrictedCourse.getId());

            // Act & Assert
            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.dropCourse(testStudent.getId(), restrictedCourse.getId());
            });

            // Verify enrollment status unchanged
            Enrollment unchangedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElse(null);
            assertThat(unchangedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        }

        @Test
        @DisplayName("Should process refund based on withdrawal timing")
        void shouldProcessRefundBasedOnWithdrawalTiming() {
            // Arrange
            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            
            // Set course fee
            basicCourse.setCourseFee(500.00);
            courseRepository.save(basicCourse);

            // Act - Withdraw within full refund period (assuming within 7 days)
            enrollmentService.dropCourse(testStudent.getId(), basicCourse.getId());

            // Assert
            Enrollment droppedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElse(null);
            assertThat(droppedEnrollment.getRefundAmount()).isEqualTo(500.00); // Full refund

            // Test partial refund scenario
            Student student2 = createTestStudent("REFUND002");
            Enrollment enrollment2 = enrollmentService.enrollStudent(student2.getId(), basicCourse.getId());
            
            // Simulate enrollment from 10 days ago
            enrollment2.setEnrollmentDate(LocalDate.now().minusDays(10));
            enrollmentRepository.save(enrollment2);
            
            enrollmentService.dropCourse(student2.getId(), basicCourse.getId());
            
            Enrollment droppedEnrollment2 = enrollmentRepository.findById(enrollment2.getId()).orElse(null);
            assertThat(droppedEnrollment2.getRefundAmount()).isLessThan(500.00); // Partial refund
        }
    }

    @Nested
    @DisplayName("Grade Submission and Completion Flow")
    class GradeSubmissionFlow {

        @Test
        @DisplayName("Should complete enrollment with final grade")
        void shouldCompleteEnrollmentWithFinalGrade() {
            // Arrange
            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());

            // Act
            enrollmentService.completeEnrollment(testStudent.getId(), basicCourse.getId(), "A");

            // Assert
            Enrollment completedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElse(null);
            assertThat(completedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
            assertThat(completedEnrollment.getFinalGrade()).isEqualTo("A");
            assertThat(completedEnrollment.getCompletionDate()).isEqualTo(LocalDate.now());
            assertThat(completedEnrollment.getGradePoints()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Should calculate credit hours earned")
        void shouldCalculateCreditHoursEarned() {
            // Arrange
            Enrollment enrollment = enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());

            // Act - Complete with passing grade
            enrollmentService.completeEnrollment(testStudent.getId(), basicCourse.getId(), "B+");

            // Assert
            Enrollment completedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElse(null);
            assertThat(completedEnrollment.getCreditHoursEarned()).isEqualTo(basicCourse.getCredits());

            // Test failing grade
            Student student2 = createTestStudent("FAIL001");
            Enrollment enrollment2 = enrollmentService.enrollStudent(student2.getId(), advancedCourse.getId());
            
            enrollmentService.completeEnrollment(student2.getId(), advancedCourse.getId(), "F");
            
            Enrollment failedEnrollment = enrollmentRepository.findById(enrollment2.getId()).orElse(null);
            assertThat(failedEnrollment.getCreditHoursEarned()).isEqualTo(0);
            assertThat(failedEnrollment.getGradePoints()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should update student transcript")
        void shouldUpdateStudentTranscript() {
            // Arrange
            enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            enrollmentService.enrollStudent(testStudent.getId(), prerequisiteCourse.getId());

            // Act
            enrollmentService.completeEnrollment(testStudent.getId(), basicCourse.getId(), "A");
            enrollmentService.completeEnrollment(testStudent.getId(), prerequisiteCourse.getId(), "B");

            // Assert - Check student's completed courses
            List<Enrollment> completedEnrollments = enrollmentRepository.findByStudentAndStatus(testStudent, EnrollmentStatus.COMPLETED);
            assertThat(completedEnrollments).hasSize(2);

            // Verify GPA calculation
            double expectedGpa = (4.0 * basicCourse.getCredits() + 3.0 * prerequisiteCourse.getCredits()) / 
                                (basicCourse.getCredits() + prerequisiteCourse.getCredits());
            
            // The service should update student GPA
            Student updatedStudent = studentRepository.findById(testStudent.getId()).orElse(null);
            assertThat(updatedStudent.getGpa()).isEqualTo(expectedGpa, within(0.01));
            assertThat(updatedStudent.getTotalCredits()).isEqualTo(basicCourse.getCredits() + prerequisiteCourse.getCredits());
        }
    }

    @Nested
    @DisplayName("Enrollment Validation Flow")
    class EnrollmentValidationFlow {

        @Test
        @DisplayName("Should validate student eligibility")
        void shouldValidateStudentEligibility() {
            // Test suspended student
            testStudent.setStatus(StudentStatus.SUSPENDED);
            studentRepository.save(testStudent);

            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            });

            // Test graduated student
            testStudent.setStatus(StudentStatus.GRADUATED);
            studentRepository.save(testStudent);

            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            });

            // Reset to active for other tests
            testStudent.setStatus(StudentStatus.ACTIVE);
            studentRepository.save(testStudent);
        }

        @Test
        @DisplayName("Should validate course availability")
        void shouldValidateCourseAvailability() {
            // Test inactive course
            basicCourse.setActive(false);
            courseRepository.save(basicCourse);

            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            });

            // Test course outside enrollment period
            basicCourse.setActive(true);
            basicCourse.setEnrollmentStartDate(LocalDate.now().plusDays(7));
            basicCourse.setEnrollmentEndDate(LocalDate.now().plusDays(14));
            courseRepository.save(basicCourse);

            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), basicCourse.getId());
            });
        }

        @Test
        @DisplayName("Should validate credit hour limits")
        void shouldValidateCreditHourLimits() {
            // Arrange - Set student to maximum credit hours for semester
            testStudent.setCurrentSemesterCredits(18); // Assume 18 is the limit
            studentRepository.save(testStudent);

            // Create a course that would exceed the limit
            Course highCreditCourse = createTestCourse("HIGH101", "High Credit Course");
            highCreditCourse.setCredits(6);
            highCreditCourse = courseRepository.save(highCreditCourse);

            // Act & Assert
            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), highCreditCourse.getId());
            });
        }

        @Test
        @DisplayName("Should validate time conflicts")
        void shouldValidateTimeConflicts() {
            // Arrange - Create courses with conflicting schedules
            Course course1 = createTestCourse("CONFLICT1", "Conflicting Course 1");
            course1.setScheduleDays("MWF");
            course1.setStartTime("10:00");
            course1.setEndTime("11:00");
            course1 = courseRepository.save(course1);

            Course course2 = createTestCourse("CONFLICT2", "Conflicting Course 2");
            course2.setScheduleDays("MWF");
            course2.setStartTime("10:30");
            course2.setEndTime("11:30");
            course2 = courseRepository.save(course2);

            // Enroll in first course
            enrollmentService.enrollStudent(testStudent.getId(), course1.getId());

            // Act & Assert - Try to enroll in conflicting course
            assertThrows(EnrollmentException.class, () -> {
                enrollmentService.enrollStudent(testStudent.getId(), course2.getId());
            });
        }
    }

    @Nested
    @DisplayName("Bulk Enrollment Operations")
    class BulkEnrollmentOperations {

        @Test
        @DisplayName("Should handle bulk student enrollment")
        void shouldHandleBulkStudentEnrollment() {
            // Arrange
            List<Student> students = List.of(
                createTestStudent("BULK001"),
                createTestStudent("BULK002"),
                createTestStudent("BULK003")
            );

            // Act
            List<Enrollment> enrollments = enrollmentService.bulkEnrollStudents(
                students.stream().map(Student::getId).toList(),
                basicCourse.getId()
            );

            // Assert
            assertThat(enrollments).hasSize(3);
            assertThat(enrollments).allMatch(e -> e.getStatus() == EnrollmentStatus.ENROLLED);
            assertThat(enrollments).allMatch(e -> e.getCourse().equals(basicCourse));

            // Verify course enrollment count
            Course updatedCourse = courseRepository.findById(basicCourse.getId()).orElse(null);
            assertThat(updatedCourse.getCurrentEnrollment()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle partial bulk enrollment failures")
        void shouldHandlePartialBulkEnrollmentFailures() {
            // Arrange - Fill course almost to capacity
            int slotsLeft = 2;
            fillCourseToCapacity(limitedCapacityCourse, limitedCapacityCourse.getMaxEnrollment() - slotsLeft);

            List<Student> students = List.of(
                createTestStudent("PARTIAL001"),
                createTestStudent("PARTIAL002"),
                createTestStudent("PARTIAL003")
            );

            // Act
            List<Enrollment> enrollments = enrollmentService.bulkEnrollStudents(
                students.stream().map(Student::getId).toList(),
                limitedCapacityCourse.getId()
            );

            // Assert - Only 2 should be enrolled, 1 should be waitlisted or failed
            long enrolledCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
            
            long waitlistedCount = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.WAITLISTED)
                .count();

            assertThat(enrolledCount).isEqualTo(2);
            assertThat(waitlistedCount).isEqualTo(1);
        }
    }

    // Helper methods
    private void setupTestData() {
        // Create department
        testDepartment = createTestDepartment();

        // Create student
        testStudent = createTestStudent("TEST001");

        // Create courses
        basicCourse = createTestCourse("BASIC101", "Basic Course");
        
        prerequisiteCourse = createTestCourse("PREREQ101", "Prerequisite Course");
        
        advancedCourse = createTestCourse("ADVANCED201", "Advanced Course");
        advancedCourse.setPrerequisites(Set.of(prerequisiteCourse));
        advancedCourse = courseRepository.save(advancedCourse);
        
        limitedCapacityCourse = createTestCourse("LIMITED101", "Limited Capacity Course");
        limitedCapacityCourse.setMaxEnrollment(3);
        limitedCapacityCourse = courseRepository.save(limitedCapacityCourse);
    }

    private Department createTestDepartment() {
        Department department = new Department();
        department.setCode("TEST");
        department.setName("Test Department");
        department.setDescription("Department for functional testing");
        department.setCreatedAt(LocalDateTime.now());
        return departmentRepository.save(department);
    }

    private Student createTestStudent(String studentId) {
        // Create user
        User user = new User();
        user.setUsername(studentId.toLowerCase());
        user.setEmail(studentId.toLowerCase() + "@test.com");
        user.setPasswordHash("$2a$10$test");
        user.setFirstName("Test");
        user.setLastName("Student" + studentId.substring(4));
        user.setRole(UserRole.STUDENT);
        user.setActive(true);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Create student
        Student student = new Student();
        student.setUser(user);
        student.setStudentId(studentId);
        student.setDepartment(testDepartment);
        student.setStatus(StudentStatus.ACTIVE);
        student.setEnrollmentDate(LocalDate.now().minusMonths(6));
        student.setExpectedGraduation(LocalDate.now().plusYears(4));
        student.setGpa(3.0);
        student.setTotalCredits(30);
        student.setCurrentSemesterCredits(12);
        student.setCreatedAt(LocalDateTime.now());
        return studentRepository.save(student);
    }

    private Course createTestCourse(String courseCode, String title) {
        Course course = new Course();
        course.setCode(courseCode);
        course.setTitle(title);
        course.setDescription("Functional test course: " + title);
        course.setCredits(3);
        course.setMaxEnrollment(30);
        course.setCurrentEnrollment(0);
        course.setDepartment(testDepartment);
        course.setActive(true);
        course.setEnrollmentStartDate(LocalDate.now().minusDays(30));
        course.setEnrollmentEndDate(LocalDate.now().plusDays(30));
        course.setWithdrawalDeadline(LocalDate.now().plusDays(60));
        course.setCourseFee(0.0);
        course.setCreatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    private List<Student> fillCourseToCapacity(Course course) {
        return fillCourseToCapacity(course, course.getMaxEnrollment());
    }

    private List<Student> fillCourseToCapacity(Course course, int numberOfStudents) {
        List<Student> enrolledStudents = new ArrayList<>();
        for (int i = 0; i < numberOfStudents; i++) {
            Student student = createTestStudent("FILL" + String.format("%03d", i));
            enrollmentService.enrollStudent(student.getId(), course.getId());
            enrolledStudents.add(student);
        }
        return enrolledStudents;
    }
}