// File location: src/test/java/functional/GradingFlowTest.java

package com.smartcampus.test.functional;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Department;
import com.smartcampus.models.Professor;
import com.smartcampus.models.Grade;
import com.smartcampus.models.Assignment;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.User;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.models.enums.EnrollmentStatus;
import com.smartcampus.models.enums.AssignmentType;
import com.smartcampus.models.enums.GradeStatus;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.repositories.ProfessorRepository;
import com.smartcampus.repositories.GradeRepository;
import com.smartcampus.repositories.AssignmentRepository;
import com.smartcampus.repositories.EnrollmentRepository;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.services.GradingService;
import com.smartcampus.services.NotificationService;
import com.smartcampus.services.EmailService;
import com.smartcampus.exceptions.GradingException;

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
import java.util.Map;
import java.util.HashMap;

/**
 * Functional tests for the complete grading workflow
 * Tests end-to-end grading processes including assignments, grade calculations, and notifications
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(classes = SmartCampusApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Grading Flow Functional Tests")
class GradingFlowTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradingService gradingService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private EmailService emailService;

    private Department testDepartment;
    private Professor testProfessor;
    private Course testCourse;
    private Student testStudent1;
    private Student testStudent2;
    private Assignment midtermExam;
    private Assignment finalExam;
    private Assignment homework1;
    private Assignment homework2;
    private Enrollment enrollment1;
    private Enrollment enrollment2;

    @BeforeEach
    void setUp() {
        // Mock notification services
        doNothing().when(notificationService).sendGradeNotification(any(), any(), any());
        doNothing().when(emailService).sendGradeNotificationEmail(any(), any(), any());

        // Create test data
        setupTestData();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        gradeRepository.deleteAll();
        assignmentRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        professorRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Assignment Creation and Management")
    @TestMethodOrder(OrderAnnotation.class)
    class AssignmentCreationAndManagement {

        @Test
        @Order(1)
        @DisplayName("Should create assignment with grading rubric")
        void shouldCreateAssignmentWithGradingRubric() {
            // Arrange
            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("title", "Project Assignment");
            assignmentData.put("description", "Comprehensive programming project");
            assignmentData.put("type", AssignmentType.PROJECT);
            assignmentData.put("maxPoints", 100.0);
            assignmentData.put("weight", 0.25);
            assignmentData.put("dueDate", LocalDateTime.now().plusDays(14));
            assignmentData.put("rubric", createTestRubric());

            // Act
            Assignment assignment = gradingService.createAssignment(testCourse.getId(), testProfessor.getId(), assignmentData);

            // Assert
            assertThat(assignment).isNotNull();
            assertThat(assignment.getTitle()).isEqualTo("Project Assignment");
            assertThat(assignment.getType()).isEqualTo(AssignmentType.PROJECT);
            assertThat(assignment.getMaxPoints()).isEqualTo(100.0);
            assertThat(assignment.getWeight()).isEqualTo(0.25);
            assertThat(assignment.getRubric()).isNotEmpty();
            assertThat(assignment.getCourse()).isEqualTo(testCourse);
            assertThat(assignment.getCreatedBy()).isEqualTo(testProfessor);
        }

        @Test
        @Order(2)
        @DisplayName("Should validate assignment weight distribution")
        void shouldValidateAssignmentWeightDistribution() {
            // Arrange - Create assignments that exceed 100% weight
            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("title", "Extra Assignment");
            assignmentData.put("type", AssignmentType.HOMEWORK);
            assignmentData.put("maxPoints", 50.0);
            assignmentData.put("weight", 0.6); // This would make total > 1.0
            assignmentData.put("dueDate", LocalDateTime.now().plusDays(7));

            // Act & Assert
            assertThrows(GradingException.class, () -> {
                gradingService.createAssignment(testCourse.getId(), testProfessor.getId(), assignmentData);
            });

            // Verify total weight remains valid
            List<Assignment> assignments = assignmentRepository.findByCourse(testCourse);
            double totalWeight = assignments.stream().mapToDouble(Assignment::getWeight).sum();
            assertThat(totalWeight).isLessThanOrEqualTo(1.0);
        }

        @Test
        @Order(3)
        @DisplayName("Should update assignment and redistribute weights")
        void shouldUpdateAssignmentAndRedistributeWeights() {
            // Act - Update assignment weight
            Map<String, Object> updates = Map.of("weight", 0.35);
            Assignment updatedAssignment = gradingService.updateAssignment(midtermExam.getId(), updates);

            // Assert
            assertThat(updatedAssignment.getWeight()).isEqualTo(0.35);

            // Verify other assignments adjusted proportionally
            List<Assignment> assignments = assignmentRepository.findByCourse(testCourse);
            double totalWeight = assignments.stream().mapToDouble(Assignment::getWeight).sum();
            assertThat(totalWeight).isCloseTo(1.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Grade Entry and Validation")
    class GradeEntryAndValidation {

        @Test
        @DisplayName("Should enter individual grade for assignment")
        void shouldEnterIndividualGradeForAssignment() {
            // Act
            Grade grade = gradingService.enterGrade(
                testStudent1.getId(),
                midtermExam.getId(),
                85.5,
                "Good understanding of concepts, minor calculation errors"
            );

            // Assert
            assertThat(grade).isNotNull();
            assertThat(grade.getStudent()).isEqualTo(testStudent1);
            assertThat(grade.getAssignment()).isEqualTo(midtermExam);
            assertThat(grade.getPointsEarned()).isEqualTo(85.5);
            assertThat(grade.getPercentage()).isEqualTo(85.5); // Assuming maxPoints is 100
            assertThat(grade.getComments()).contains("Good understanding");
            assertThat(grade.getGradedBy()).isEqualTo(testProfessor);
            assertThat(grade.getGradedAt()).isNotNull();
            assertThat(grade.getStatus()).isEqualTo(GradeStatus.FINAL);

            // Verify notification sent
            verify(notificationService).sendGradeNotification(testStudent1, testCourse, grade);
        }

        @Test
        @DisplayName("Should validate grade within point range")
        void shouldValidateGradeWithinPointRange() {
            // Test grade exceeding maximum points
            assertThrows(GradingException.class, () -> {
                gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 110.0, "Exceeds maximum");
            });

            // Test negative grade
            assertThrows(GradingException.class, () -> {
                gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), -5.0, "Negative points");
            });

            // Test valid grade at boundary
            assertDoesNotThrow(() -> {
                gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 100.0, "Perfect score");
            });
        }

        @Test
        @DisplayName("Should handle bulk grade entry")
        void shouldHandleBulkGradeEntry() {
            // Arrange
            Map<Long, Double> bulkGrades = new HashMap<>();
            bulkGrades.put(testStudent1.getId(), 88.0);
            bulkGrades.put(testStudent2.getId(), 92.5);

            Map<Long, String> comments = new HashMap<>();
            comments.put(testStudent1.getId(), "Well done");
            comments.put(testStudent2.getId(), "Excellent work");

            // Act
            List<Grade> grades = gradingService.enterBulkGrades(homework1.getId(), bulkGrades, comments);

            // Assert
            assertThat(grades).hasSize(2);
            assertThat(grades).extracting(Grade::getPointsEarned).containsExactlyInAnyOrder(88.0, 92.5);

            // Verify all students received notifications
            verify(notificationService, times(2)).sendGradeNotification(any(Student.class), any(Course.class), any(Grade.class));
        }

        @Test
        @DisplayName("Should update existing grade")
        void shouldUpdateExistingGrade() {
            // Arrange - Enter initial grade
            Grade initialGrade = gradingService.enterGrade(testStudent1.getId(), homework1.getId(), 75.0, "Initial grade");

            // Act - Update the grade
            Grade updatedGrade = gradingService.updateGrade(
                initialGrade.getId(),
                82.0,
                "Updated after review - added partial credit"
            );

            // Assert
            assertThat(updatedGrade.getId()).isEqualTo(initialGrade.getId());
            assertThat(updatedGrade.getPointsEarned()).isEqualTo(82.0);
            assertThat(updatedGrade.getComments()).contains("Updated after review");
            assertThat(updatedGrade.getUpdatedAt()).isAfter(updatedGrade.getGradedAt());

            // Verify grade history is maintained
            assertThat(updatedGrade.getPreviousGrade()).isEqualTo(75.0);
            assertThat(updatedGrade.getUpdateReason()).contains("Updated after review");
        }
    }

    @Nested
    @DisplayName("Grade Calculation and Final Grades")
    class GradeCalculationAndFinalGrades {

        @Test
        @DisplayName("Should calculate weighted course grade")
        void shouldCalculateWeightedCourseGrade() {
            // Arrange - Enter grades for all assignments
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 85.0, "Midterm grade");
            gradingService.enterGrade(testStudent1.getId(), finalExam.getId(), 90.0, "Final grade");
            gradingService.enterGrade(testStudent1.getId(), homework1.getId(), 95.0, "Homework 1");
            gradingService.enterGrade(testStudent1.getId(), homework2.getId(), 88.0, "Homework 2");

            // Act
            double finalGrade = gradingService.calculateFinalGrade(testStudent1.getId(), testCourse.getId());

            // Assert
            // Expected: 85*0.3 + 90*0.4 + (95+88)/2*0.3 = 25.5 + 36 + 27.45 = 88.95
            assertThat(finalGrade).isCloseTo(88.95, within(0.01));

            // Verify final grade is stored
            Enrollment updatedEnrollment = enrollmentRepository.findByStudentAndCourse(testStudent1, testCourse);
            assertThat(updatedEnrollment.getFinalGradePercentage()).isCloseTo(88.95, within(0.01));
        }

        @Test
        @DisplayName("Should convert percentage to letter grade")
        void shouldConvertPercentageToLetterGrade() {
            // Test different grade ranges
            assertThat(gradingService.convertToLetterGrade(97.0)).isEqualTo("A+");
            assertThat(gradingService.convertToLetterGrade(92.0)).isEqualTo("A");
            assertThat(gradingService.convertToLetterGrade(88.0)).isEqualTo("A-");
            assertThat(gradingService.convertToLetterGrade(85.0)).isEqualTo("B+");
            assertThat(gradingService.convertToLetterGrade(82.0)).isEqualTo("B");
            assertThat(gradingService.convertToLetterGrade(78.0)).isEqualTo("B-");
            assertThat(gradingService.convertToLetterGrade(75.0)).isEqualTo("C+");
            assertThat(gradingService.convertToLetterGrade(72.0)).isEqualTo("C");
            assertThat(gradingService.convertToLetterGrade(68.0)).isEqualTo("C-");
            assertThat(gradingService.convertToLetterGrade(65.0)).isEqualTo("D+");
            assertThat(gradingService.convertToLetterGrade(62.0)).isEqualTo("D");
            assertThat(gradingService.convertToLetterGrade(58.0)).isEqualTo("D-");
            assertThat(gradingService.convertToLetterGrade(55.0)).isEqualTo("F");
        }

        @Test
        @DisplayName("Should calculate grade point average")
        void shouldCalculateGradePointAverage() {
            // Arrange - Complete multiple courses
            Course course2 = createTestCourse("MATH101", "Mathematics");
            Enrollment enrollment3 = createTestEnrollment(testStudent1, course2);
            
            // Set final grades
            enrollment1.setFinalGrade("A");
            enrollment1.setGradePoints(4.0);
            enrollmentRepository.save(enrollment1);
            
            enrollment3.setFinalGrade("B+");
            enrollment3.setGradePoints(3.3);
            enrollmentRepository.save(enrollment3);

            // Act
            double gpa = gradingService.calculateStudentGPA(testStudent1.getId());

            // Assert
            // Expected: (4.0 * 3 + 3.3 * 3) / (3 + 3) = 21.9 / 6 = 3.65
            assertThat(gpa).isCloseTo(3.65, within(0.01));

            // Verify student record updated
            Student updatedStudent = studentRepository.findById(testStudent1.getId()).orElse(null);
            assertThat(updatedStudent.getGpa()).isCloseTo(3.65, within(0.01));
        }

        @Test
        @DisplayName("Should handle incomplete assignments in grade calculation")
        void shouldHandleIncompleteAssignmentsInGradeCalculation() {
            // Arrange - Enter grades for only some assignments
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 85.0, "Midterm grade");
            gradingService.enterGrade(testStudent1.getId(), homework1.getId(), 95.0, "Homework 1");
            // Missing: finalExam and homework2

            // Act
            double partialGrade = gradingService.calculatePartialGrade(testStudent1.getId(), testCourse.getId());

            // Assert - Should calculate based on completed assignments only
            // Expected: (85*0.3 + 95*0.15) / (0.3 + 0.15) = (25.5 + 14.25) / 0.45 = 88.33
            assertThat(partialGrade).isCloseTo(88.33, within(0.01));
        }

        @Test
        @DisplayName("Should apply grade curves when specified")
        void shouldApplyGradeCurvesWhenSpecified() {
            // Arrange - Enter grades below average
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 70.0, "Below average");
            gradingService.enterGrade(testStudent2.getId(), midtermExam.getId(), 65.0, "Below average");

            // Act - Apply a 10-point curve
            gradingService.applyCurveToAssignment(midtermExam.getId(), 10.0, "Curve applied due to low class average");

            // Assert
            List<Grade> curvedGrades = gradeRepository.findByAssignment(midtermExam);
            assertThat(curvedGrades).hasSize(2);
            
            Grade student1Grade = curvedGrades.stream()
                .filter(g -> g.getStudent().equals(testStudent1))
                .findFirst().orElse(null);
            assertThat(student1Grade.getPointsEarned()).isEqualTo(80.0);
            assertThat(student1Grade.getComments()).contains("Curve applied");

            Grade student2Grade = curvedGrades.stream()
                .filter(g -> g.getStudent().equals(testStudent2))
                .findFirst().orElse(null);
            assertThat(student2Grade.getPointsEarned()).isEqualTo(75.0);
        }
    }

    @Nested
    @DisplayName("Grade Analytics and Statistics")
    class GradeAnalyticsAndStatistics {

        @Test
        @DisplayName("Should calculate assignment statistics")
        void shouldCalculateAssignmentStatistics() {
            // Arrange - Enter grades for multiple students
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 85.0, "Student 1");
            gradingService.enterGrade(testStudent2.getId(), midtermExam.getId(), 92.0, "Student 2");
            
            // Create additional students and grades
            Student student3 = createTestStudent("STATS003");
            Student student4 = createTestStudent("STATS004");
            createTestEnrollment(student3, testCourse);
            createTestEnrollment(student4, testCourse);
            
            gradingService.enterGrade(student3.getId(), midtermExam.getId(), 78.0, "Student 3");
            gradingService.enterGrade(student4.getId(), midtermExam.getId(), 95.0, "Student 4");

            // Act
            Map<String, Double> stats = gradingService.calculateAssignmentStatistics(midtermExam.getId());

            // Assert
            assertThat(stats.get("average")).isCloseTo(87.5, within(0.1)); // (85+92+78+95)/4
            assertThat(stats.get("median")).isCloseTo(88.5, within(0.1)); // (85+92)/2
            assertThat(stats.get("minimum")).isEqualTo(78.0);
            assertThat(stats.get("maximum")).isEqualTo(95.0);
            assertThat(stats.get("standardDeviation")).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should generate grade distribution report")
        void shouldGenerateGradeDistributionReport() {
            // Arrange - Create grades with various letter grades
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 95.0, "A grade");
            gradingService.enterGrade(testStudent2.getId(), midtermExam.getId(), 87.0, "B grade");

            Student student3 = createTestStudent("DIST003");
            Student student4 = createTestStudent("DIST004");
            Student student5 = createTestStudent("DIST005");
            createTestEnrollment(student3, testCourse);
            createTestEnrollment(student4, testCourse);
            createTestEnrollment(student5, testCourse);

            gradingService.enterGrade(student3.getId(), midtermExam.getId(), 82.0, "B grade");
            gradingService.enterGrade(student4.getId(), midtermExam.getId(), 75.0, "C grade");
            gradingService.enterGrade(student5.getId(), midtermExam.getId(), 68.0, "D grade");

            // Act
            Map<String, Integer> distribution = gradingService.getGradeDistribution(midtermExam.getId());

            // Assert
            assertThat(distribution.get("A")).isEqualTo(1);
            assertThat(distribution.get("B")).isEqualTo(2);
            assertThat(distribution.get("C")).isEqualTo(1);
            assertThat(distribution.get("D")).isEqualTo(1);
            assertThat(distribution.get("F")).isEqualTo(0);
        }

        @Test
        @DisplayName("Should identify students at risk")
        void shouldIdentifyStudentsAtRisk() {
            // Arrange - Create grades indicating at-risk students
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 55.0, "Failing grade");
            gradingService.enterGrade(testStudent1.getId(), homework1.getId(), 45.0, "Poor homework");
            
            gradingService.enterGrade(testStudent2.getId(), midtermExam.getId(), 88.0, "Good grade");
            gradingService.enterGrade(testStudent2.getId(), homework1.getId(), 92.0, "Good homework");

            // Act
            List<Student> atRiskStudents = gradingService.identifyAtRiskStudents(testCourse.getId());

            // Assert
            assertThat(atRiskStudents).hasSize(1);
            assertThat(atRiskStudents.get(0)).isEqualTo(testStudent1);

            // Verify notification sent to advisor
            verify(notificationService).sendAtRiskNotification(testStudent1, testCourse);
        }
    }

    @Nested
    @DisplayName("Grade Dispute and Review Process")
    class GradeDisputeAndReviewProcess {

        @Test
        @DisplayName("Should initiate grade dispute process")
        void shouldInitiateGradeDisputeProcess() {
            // Arrange
            Grade originalGrade = gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 75.0, "Original grade");

            // Act
            gradingService.initiateGradeDispute(
                originalGrade.getId(),
                testStudent1.getId(),
                "I believe my answer to question 3 was correct and should receive full credit"
            );

            // Assert
            Grade disputedGrade = gradeRepository.findById(originalGrade.getId()).orElse(null);
            assertThat(disputedGrade.getStatus()).isEqualTo(GradeStatus.DISPUTED);
            assertThat(disputedGrade.getDisputeReason()).contains("question 3 was correct");
            assertThat(disputedGrade.getDisputeDate()).isEqualTo(LocalDate.now());

            // Verify notification sent to professor
            verify(notificationService).sendGradeDisputeNotification(testProfessor, originalGrade);
        }

        @Test
        @DisplayName("Should resolve grade dispute with review")
        void shouldResolveGradeDisputeWithReview() {
            // Arrange
            Grade originalGrade = gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 75.0, "Original grade");
            gradingService.initiateGradeDispute(originalGrade.getId(), testStudent1.getId(), "Dispute reason");

            // Act - Professor reviews and adjusts grade
            Grade resolvedGrade = gradingService.resolveGradeDispute(
                originalGrade.getId(),
                82.0,
                "Upon review, partial credit awarded for alternative solution approach"
            );

            // Assert
            assertThat(resolvedGrade.getStatus()).isEqualTo(GradeStatus.FINAL);
            assertThat(resolvedGrade.getPointsEarned()).isEqualTo(82.0);
            assertThat(resolvedGrade.getComments()).contains("Upon review");
            assertThat(resolvedGrade.getReviewedBy()).isEqualTo(testProfessor);
            assertThat(resolvedGrade.getReviewDate()).isEqualTo(LocalDate.now());

            // Verify resolution notification sent
            verify(notificationService).sendGradeDisputeResolutionNotification(testStudent1, resolvedGrade);
        }

        @Test
        @DisplayName("Should handle grade resubmission requests")
        void shouldHandleGradeResubmissionRequests() {
            // Arrange
            Assignment assignment = createTestAssignment("RESUBMIT", AssignmentType.HOMEWORK, true); // Allow resubmission
            Grade originalGrade = gradingService.enterGrade(testStudent1.getId(), assignment.getId(), 65.0, "Original attempt");

            // Act
            Grade resubmittedGrade = gradingService.handleResubmission(
                originalGrade.getId(),
                testStudent1.getId(),
                "Revised submission addressing feedback"
            );

            // Assert
            assertThat(resubmittedGrade.getStatus()).isEqualTo(GradeStatus.RESUBMITTED);
            assertThat(resubmittedGrade.getResubmissionDate()).isEqualTo(LocalDate.now());
            assertThat(resubmittedGrade.getResubmissionComments()).contains("Revised submission");

            // Original grade should be marked as superseded
            Grade originalUpdated = gradeRepository.findById(originalGrade.getId()).orElse(null);
            assertThat(originalUpdated.getStatus()).isEqualTo(GradeStatus.SUPERSEDED);
        }
    }

    @Nested
    @DisplayName("Grade Export and Reporting")
    class GradeExportAndReporting {

        @Test
        @DisplayName("Should export gradebook to CSV")
        void shouldExportGradebookToCsv() {
            // Arrange - Enter grades for multiple assignments
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 85.0, "Midterm");
            gradingService.enterGrade(testStudent1.getId(), homework1.getId(), 95.0, "HW1");
            gradingService.enterGrade(testStudent2.getId(), midtermExam.getId(), 90.0, "Midterm");
            gradingService.enterGrade(testStudent2.getId(), homework1.getId(), 88.0, "HW1");

            // Act
            String csvContent = gradingService.exportGradebookToCsv(testCourse.getId());

            // Assert
            assertThat(csvContent).isNotEmpty();
            assertThat(csvContent).contains("Student ID,Student Name,Midterm Exam,Homework 1,Final Grade");
            assertThat(csvContent).contains(testStudent1.getStudentId());
            assertThat(csvContent).contains(testStudent2.getStudentId());
            assertThat(csvContent).contains("85.0");
            assertThat(csvContent).contains("95.0");
            assertThat(csvContent).contains("90.0");
            assertThat(csvContent).contains("88.0");
        }

        @Test
        @DisplayName("Should generate individual student grade report")
        void shouldGenerateIndividualStudentGradeReport() {
            // Arrange
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 85.0, "Strong performance");
            gradingService.enterGrade(testStudent1.getId(), homework1.getId(), 95.0, "Excellent work");
            gradingService.enterGrade(testStudent1.getId(), homework2.getId(), 88.0, "Good effort");

            // Act
            Map<String, Object> report = gradingService.generateStudentGradeReport(testStudent1.getId(), testCourse.getId());

            // Assert
            assertThat(report).containsKey("student");
            assertThat(report).containsKey("course");
            assertThat(report).containsKey("assignments");
            assertThat(report).containsKey("currentGrade");
            assertThat(report).containsKey("letterGrade");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) report.get("assignments");
            assertThat(assignments).hasSize(3);
            
            Double currentGrade = (Double) report.get("currentGrade");
            assertThat(currentGrade).isGreaterThan(80.0);
        }

        @Test
        @DisplayName("Should generate class performance summary")
        void shouldGenerateClassPerformanceSummary() {
            // Arrange - Enter grades for multiple students and assignments
            gradingService.enterGrade(testStudent1.getId(), midtermExam.getId(), 85.0, "Grade 1");
            gradingService.enterGrade(testStudent2.getId(), midtermExam.getId(), 92.0, "Grade 2");
            
            Student student3 = createTestStudent("SUMMARY003");
            createTestEnrollment(student3, testCourse);
            gradingService.enterGrade(student3.getId(), midtermExam.getId(), 78.0, "Grade 3");

            // Act
            Map<String, Object> summary = gradingService.generateClassPerformanceSummary(testCourse.getId());

            // Assert
            assertThat(summary).containsKey("course");
            assertThat(summary).containsKey("totalStudents");
            assertThat(summary).containsKey("assignmentStatistics");
            assertThat(summary).containsKey("gradeDistribution");
            assertThat(summary).containsKey("classAverage");
            
            Integer totalStudents = (Integer) summary.get("totalStudents");
            assertThat(totalStudents).isEqualTo(3);
            
            Double classAverage = (Double) summary.get("classAverage");
            assertThat(classAverage).isCloseTo(85.0, within(2.0));
        }
    }

    @Nested
    @DisplayName("Automated Grading Features")
    class AutomatedGradingFeatures {

        @Test
        @DisplayName("Should auto-grade multiple choice assignments")
        void shouldAutoGradeMultipleChoiceAssignments() {
            // Arrange
            Assignment mcqAssignment = createTestAssignment("MCQ_QUIZ", AssignmentType.QUIZ, false);
            
            // Set up answer key
            Map<String, String> answerKey = Map.of(
                "question1", "A",
                "question2", "C",
                "question3", "B",
                "question4", "D"
            );
            mcqAssignment.setAnswerKey(answerKey);
            assignmentRepository.save(mcqAssignment);

            // Student answers
            Map<String, String> studentAnswers = Map.of(
                "question1", "A", // Correct
                "question2", "B", // Incorrect
                "question3", "B", // Correct
                "question4", "D"  // Correct
            );

            // Act
            Grade autoGrade = gradingService.autoGradeMultipleChoice(
                testStudent1.getId(),
                mcqAssignment.getId(),
                studentAnswers
            );

            // Assert
            assertThat(autoGrade.getPointsEarned()).isEqualTo(75.0); // 3/4 correct = 75%
            assertThat(autoGrade.getStatus()).isEqualTo(GradeStatus.FINAL);
            assertThat(autoGrade.isAutoGraded()).isTrue();
            assertThat(autoGrade.getComments()).contains("Auto-graded");
        }

        @Test
        @DisplayName("Should detect and handle potential academic dishonesty")
        void shouldDetectAndHandlePotentialAcademicDishonesty() {
            // Arrange - Create similar submissions
            Assignment assignment = createTestAssignment("PLAGIARISM_CHECK", AssignmentType.PROJECT, false);
            
            // Act - Submit very similar answers
            Grade grade1 = gradingService.enterGrade(testStudent1.getId(), assignment.getId(), 90.0, "First submission");
            Grade grade2 = gradingService.enterGrade(testStudent2.getId(), assignment.getId(), 89.0, "Very similar submission");

            // Simulate plagiarism detection
            gradingService.checkForPlagiarism(assignment.getId());

            // Assert
            Grade flaggedGrade1 = gradeRepository.findById(grade1.getId()).orElse(null);
            Grade flaggedGrade2 = gradeRepository.findById(grade2.getId()).orElse(null);

            assertThat(flaggedGrade1.isPlagiarismFlagged()).isTrue();
            assertThat(flaggedGrade2.isPlagiarismFlagged()).isTrue();

            // Verify notifications sent to instructor and academic office
            verify(notificationService).sendPlagiarismAlert(testProfessor, List.of(flaggedGrade1, flaggedGrade2));
        }
    }

    // Helper methods
    private void setupTestData() {
        // Create department
        testDepartment = createTestDepartment();

        // Create professor
        testProfessor = createTestProfessor();

        // Create course
        testCourse = createTestCourse("CS101", "Introduction to Programming");

        // Create students
        testStudent1 = createTestStudent("GRADE001");
        testStudent2 = createTestStudent("GRADE002");

        // Create enrollments
        enrollment1 = createTestEnrollment(testStudent1, testCourse);
        enrollment2 = createTestEnrollment(testStudent2, testCourse);

        // Create assignments
        midtermExam = createTestAssignment("MIDTERM", AssignmentType.EXAM, false);
        finalExam = createTestAssignment("FINAL", AssignmentType.EXAM, false);
        homework1 = createTestAssignment("HW1", AssignmentType.HOMEWORK, true);
        homework2 = createTestAssignment("HW2", AssignmentType.HOMEWORK, true);
    }

    private Department createTestDepartment() {
        Department department = new Department();
        department.setCode("CS");
        department.setName("Computer Science");
        department.setDescription("Computer Science Department");
        department.setCreatedAt(LocalDateTime.now());
        return departmentRepository.save(department);
    }

    private Professor createTestProfessor() {
        // Create user for professor
        User professorUser = new User();
        professorUser.setUsername("prof.grading");
        professorUser.setEmail("professor@test.com");
        professorUser.setPasswordHash("$2a$10$test");
        professorUser.setFirstName("Test");
        professorUser.setLastName("Professor");
        professorUser.setRole(UserRole.PROFESSOR);
        professorUser.setActive(true);
        professorUser.setVerified(true);
        professorUser.setCreatedAt(LocalDateTime.now());
        professorUser = userRepository.save(professorUser);

        Professor professor = new Professor();
        professor.setUser(professorUser);
        professor.setEmployeeId("PROF001");
        professor.setDepartment(testDepartment);
        professor.setTitle("Assistant Professor");
        professor.setCreatedAt(LocalDateTime.now());
        return professorRepository.save(professor);
    }

    private Course createTestCourse(String courseCode, String title) {
        Course course = new Course();
        course.setCode(courseCode);
        course.setTitle(title);
        course.setDescription("Test course for grading");
        course.setCredits(3);
        course.setMaxEnrollment(30);
        course.setCurrentEnrollment(2);
        course.setDepartment(testDepartment);
        course.setProfessor(testProfessor);
        course.setActive(true);
        course.setCreatedAt(LocalDateTime.now());
        return courseRepository.save(course);
    }

    private Student createTestStudent(String studentId) {
        // Create user
        User studentUser = new User();
        studentUser.setUsername(studentId.toLowerCase());
        studentUser.setEmail(studentId.toLowerCase() + "@test.com");
        studentUser.setPasswordHash("$2a$10$test");
        studentUser.setFirstName("Test");
        studentUser.setLastName("Student" + studentId.substring(5));
        studentUser.setRole(UserRole.STUDENT);
        studentUser.setActive(true);
        studentUser.setVerified(true);
        studentUser.setCreatedAt(LocalDateTime.now());
        studentUser = userRepository.save(studentUser);

        // Create student
        Student student = new Student();
        student.setUser(studentUser);
        student.setStudentId(studentId);
        student.setDepartment(testDepartment);
        student.setStatus(StudentStatus.ACTIVE);
        student.setEnrollmentDate(LocalDate.now().minusMonths(6));
        student.setExpectedGraduation(LocalDate.now().plusYears(4));
        student.setGpa(3.0);
        student.setTotalCredits(30);
        student.setCreatedAt(LocalDateTime.now());
        return studentRepository.save(student);
    }

    private Enrollment createTestEnrollment(Student student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDate.now().minusDays(30));
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setCreatedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    private Assignment createTestAssignment(String title, AssignmentType type, boolean allowResubmission) {
        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription("Test assignment: " + title);
        assignment.setType(type);
        assignment.setCourse(testCourse);
        assignment.setCreatedBy(testProfessor);
        assignment.setMaxPoints(100.0);
        assignment.setDueDate(LocalDateTime.now().plusDays(7));
        assignment.setAllowResubmission(allowResubmission);
        assignment.setCreatedAt(LocalDateTime.now());
        
        // Set weights based on type
        switch (type) {
            case EXAM:
                assignment.setWeight(title.contains("MIDTERM") ? 0.3 : 0.4);
                break;
            case HOMEWORK:
                assignment.setWeight(0.15);
                break;
            case QUIZ:
                assignment.setWeight(0.1);
                break;
            case PROJECT:
                assignment.setWeight(0.25);
                break;
            default:
                assignment.setWeight(0.1);
        }
        
        return assignmentRepository.save(assignment);
    }

    private Map<String, Object> createTestRubric() {
        Map<String, Object> rubric = new HashMap<>();
        rubric.put("criteria", List.of(
            Map.of("name", "Code Quality", "points", 30, "description", "Clean, readable code"),
            Map.of("name", "Functionality", "points", 40, "description", "Correct implementation"),
            Map.of("name", "Documentation", "points", 20, "description", "Adequate comments and docs"),
            Map.of("name", "Testing", "points", 10, "description", "Comprehensive test cases")
        ));
        return rubric;
    }
}