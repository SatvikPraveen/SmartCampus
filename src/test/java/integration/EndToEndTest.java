// File location: src/test/java/integration/EndToEndTest.java

package com.smartcampus.test.integration;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.controllers.StudentController;
import com.smartcampus.controllers.CourseController;
import com.smartcampus.controllers.EnrollmentController;
import com.smartcampus.controllers.AuthController;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Department;
import com.smartcampus.models.User;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.dto.LoginRequest;
import com.smartcampus.dto.RegisterRequest;
import com.smartcampus.dto.LoginResponse;
import com.smartcampus.dto.EnrollmentRequest;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.services.EmailService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * End-to-end integration tests for the Smart Campus Management System
 * Tests complete user workflows from registration to course management
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(
    classes = SmartCampusApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("End-to-End Integration Tests")
class EndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private EmailService emailService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    private String baseUrl;
    private String authToken;
    private Department testDepartment;
    private Course testCourse1;
    private Course testCourse2;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Mock email service
        doNothing().when(emailService).sendVerificationEmail(any(), any());
        doNothing().when(emailService).sendPasswordResetEmail(any(), any());
        
        // Create test department
        createTestDepartment();
        
        // Create test courses
        createTestCourses();
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        studentRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Complete User Registration and Authentication Flow")
    @TestMethodOrder(OrderAnnotation.class)
    class UserRegistrationAndAuthenticationFlow {

        @Test
        @Order(1)
        @DisplayName("Should complete full user registration flow")
        void shouldCompleteFullUserRegistrationFlow() {
            // Step 1: Register new user
            RegisterRequest registerRequest = createRegisterRequest();
            
            ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                baseUrl + "/auth/register", 
                registerRequest, 
                Map.class
            );
            
            assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(registerResponse.getBody()).containsKey("message");
            assertThat(registerResponse.getBody().get("message")).isEqualTo("User registered successfully");

            // Step 2: Verify email (simulate verification)
            User createdUser = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.isVerified()).isFalse();
            
            // Simulate email verification
            String verificationToken = createdUser.getVerificationToken();
            ResponseEntity<Map> verifyResponse = restTemplate.postForEntity(
                baseUrl + "/auth/verify-email?token=" + verificationToken,
                null,
                Map.class
            );
            
            assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            // Step 3: Login with verified credentials
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(registerRequest.getUsername());
            loginRequest.setPassword(registerRequest.getPassword());
            
            ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginRequest,
                LoginResponse.class
            );
            
            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(loginResponse.getBody()).isNotNull();
            assertThat(loginResponse.getBody().getToken()).isNotEmpty();
            
            // Store auth token for subsequent tests
            authToken = loginResponse.getBody().getToken();
        }

        @Test
        @Order(2)
        @DisplayName("Should handle authentication errors correctly")
        void shouldHandleAuthenticationErrorsCorrectly() {
            // Test invalid credentials
            LoginRequest invalidLogin = new LoginRequest();
            invalidLogin.setUsername("nonexistent");
            invalidLogin.setPassword("wrongpassword");
            
            ResponseEntity<Map> errorResponse = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                invalidLogin,
                Map.class
            );
            
            assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorResponse.getBody()).containsKey("error");
        }

        @Test
        @Order(3)
        @DisplayName("Should handle password reset flow")
        void shouldHandlePasswordResetFlow() {
            // Step 1: Register user first
            RegisterRequest registerRequest = createRegisterRequest();
            restTemplate.postForEntity(baseUrl + "/auth/register", registerRequest, Map.class);
            
            // Step 2: Request password reset
            Map<String, String> resetRequest = Map.of("email", registerRequest.getEmail());
            ResponseEntity<Map> resetResponse = restTemplate.postForEntity(
                baseUrl + "/auth/forgot-password",
                resetRequest,
                Map.class
            );
            
            assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            // Step 3: Verify reset token was created
            User user = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);
            assertThat(user).isNotNull();
            assertThat(user.getPasswordResetToken()).isNotNull();
            assertThat(user.getPasswordResetExpires()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Student Management Workflow")
    @TestMethodOrder(OrderAnnotation.class)
    class StudentManagementWorkflow {

        @Test
        @Order(1)
        @DisplayName("Should create and manage student profile")
        void shouldCreateAndManageStudentProfile() {
            // Step 1: Authenticate as admin
            authenticateAsAdmin();
            
            // Step 2: Create student profile
            Student studentRequest = createStudentRequest();
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Student> entity = new HttpEntity<>(studentRequest, headers);
            
            ResponseEntity<Student> createResponse = restTemplate.postForEntity(
                baseUrl + "/students",
                entity,
                Student.class
            );
            
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(createResponse.getBody()).isNotNull();
            assertThat(createResponse.getBody().getStudentId()).isNotEmpty();
            
            Long studentId = createResponse.getBody().getId();
            
            // Step 3: Retrieve student profile
            ResponseEntity<Student> getResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Student.class
            );
            
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(getResponse.getBody()).isNotNull();
            
            // Step 4: Update student profile
            Student updateRequest = getResponse.getBody();
            updateRequest.setGpa(3.8);
            updateRequest.setTotalCredits(60);
            
            ResponseEntity<Student> updateResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId,
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                Student.class
            );
            
            assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(updateResponse.getBody().getGpa()).isEqualTo(3.8);
            assertThat(updateResponse.getBody().getTotalCredits()).isEqualTo(60);
        }

        @Test
        @Order(2)
        @DisplayName("Should list and filter students")
        void shouldListAndFilterStudents() {
            // Authenticate as admin
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            // Create multiple students
            for (int i = 1; i <= 3; i++) {
                Student student = createStudentRequest();
                student.setStudentId("STU00" + i);
                student.setGpa(2.0 + i * 0.5);
                
                restTemplate.postForEntity(
                    baseUrl + "/students",
                    new HttpEntity<>(student, headers),
                    Student.class
                );
            }
            
            // Test listing all students
            ResponseEntity<List> listResponse = restTemplate.exchange(
                baseUrl + "/students",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
            );
            
            assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(listResponse.getBody()).hasSizeGreaterThanOrEqualTo(3);
            
            // Test filtering by GPA
            ResponseEntity<List> filteredResponse = restTemplate.exchange(
                baseUrl + "/students?minGpa=3.0",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
            );
            
            assertThat(filteredResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(filteredResponse.getBody()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @Order(3)
        @DisplayName("Should handle student status changes")
        void shouldHandleStudentStatusChanges() {
            // Authenticate and create student
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            Student student = createStudentRequest();
            ResponseEntity<Student> createResponse = restTemplate.postForEntity(
                baseUrl + "/students",
                new HttpEntity<>(student, headers),
                Student.class
            );
            
            Long studentId = createResponse.getBody().getId();
            
            // Test status change to SUSPENDED
            Map<String, String> statusRequest = Map.of("status", "SUSPENDED", "reason", "Academic probation");
            ResponseEntity<Student> suspendResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId + "/status",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(statusRequest, headers),
                Student.class
            );
            
            assertThat(suspendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(suspendResponse.getBody().getStatus()).isEqualTo(StudentStatus.SUSPENDED);
            
            // Test reactivation
            Map<String, String> reactivateRequest = Map.of("status", "ACTIVE");
            ResponseEntity<Student> reactivateResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId + "/status",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(reactivateRequest, headers),
                Student.class
            );
            
            assertThat(reactivateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reactivateResponse.getBody().getStatus()).isEqualTo(StudentStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Course Enrollment Workflow")
    @TestMethodOrder(OrderAnnotation.class)
    class CourseEnrollmentWorkflow {

        @Test
        @Order(1)
        @DisplayName("Should complete full enrollment process")
        void shouldCompleteFullEnrollmentProcess() {
            // Step 1: Create student and authenticate
            authenticateAsAdmin();
            HttpHeaders adminHeaders = createAuthHeaders();
            
            Student student = createStudentRequest();
            ResponseEntity<Student> studentResponse = restTemplate.postForEntity(
                baseUrl + "/students",
                new HttpEntity<>(student, adminHeaders),
                Student.class
            );
            Long studentId = studentResponse.getBody().getId();
            
            // Step 2: Student enrolls in course
            authenticateAsStudent();
            HttpHeaders studentHeaders = createAuthHeaders();
            
            EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
            enrollmentRequest.setStudentId(studentId);
            enrollmentRequest.setCourseId(testCourse1.getId());
            
            ResponseEntity<Map> enrollResponse = restTemplate.postForEntity(
                baseUrl + "/enrollments",
                new HttpEntity<>(enrollmentRequest, studentHeaders),
                Map.class
            );
            
            assertThat(enrollResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            
            // Step 3: Verify enrollment
            ResponseEntity<List> enrollmentsResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId + "/enrollments",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(studentHeaders),
                List.class
            );
            
            assertThat(enrollmentsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(enrollmentsResponse.getBody()).hasSize(1);
            
            // Step 4: Check course capacity
            ResponseEntity<Course> courseResponse = restTemplate.exchange(
                baseUrl + "/courses/" + testCourse1.getId(),
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(studentHeaders),
                Course.class
            );
            
            assertThat(courseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(courseResponse.getBody().getCurrentEnrollment()).isEqualTo(1);
        }

        @Test
        @Order(2)
        @DisplayName("Should handle enrollment capacity limits")
        void shouldHandleEnrollmentCapacityLimits() {
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            // Set course capacity to 1
            testCourse2.setMaxEnrollment(1);
            courseRepository.save(testCourse2);
            
            // Create two students
            Student student1 = createStudentRequest();
            student1.setStudentId("STU001");
            Student student2 = createStudentRequest();
            student2.setStudentId("STU002");
            
            ResponseEntity<Student> s1Response = restTemplate.postForEntity(
                baseUrl + "/students", new HttpEntity<>(student1, headers), Student.class);
            ResponseEntity<Student> s2Response = restTemplate.postForEntity(
                baseUrl + "/students", new HttpEntity<>(student2, headers), Student.class);
            
            // First enrollment should succeed
            EnrollmentRequest enrollment1 = new EnrollmentRequest();
            enrollment1.setStudentId(s1Response.getBody().getId());
            enrollment1.setCourseId(testCourse2.getId());
            
            ResponseEntity<Map> enrollResponse1 = restTemplate.postForEntity(
                baseUrl + "/enrollments",
                new HttpEntity<>(enrollment1, headers),
                Map.class
            );
            assertThat(enrollResponse1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            
            // Second enrollment should fail (capacity exceeded)
            EnrollmentRequest enrollment2 = new EnrollmentRequest();
            enrollment2.setStudentId(s2Response.getBody().getId());
            enrollment2.setCourseId(testCourse2.getId());
            
            ResponseEntity<Map> enrollResponse2 = restTemplate.postForEntity(
                baseUrl + "/enrollments",
                new HttpEntity<>(enrollment2, headers),
                Map.class
            );
            assertThat(enrollResponse2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(enrollResponse2.getBody()).containsKey("error");
        }

        @Test
        @Order(3)
        @DisplayName("Should handle course withdrawal")
        void shouldHandleCourseWithdrawal() {
            // Setup: Create student and enroll in course
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            Student student = createStudentRequest();
            ResponseEntity<Student> studentResponse = restTemplate.postForEntity(
                baseUrl + "/students", new HttpEntity<>(student, headers), Student.class);
            Long studentId = studentResponse.getBody().getId();
            
            EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
            enrollmentRequest.setStudentId(studentId);
            enrollmentRequest.setCourseId(testCourse1.getId());
            
            restTemplate.postForEntity(
                baseUrl + "/enrollments",
                new HttpEntity<>(enrollmentRequest, headers),
                Map.class
            );
            
            // Test withdrawal
            ResponseEntity<Map> withdrawResponse = restTemplate.exchange(
                baseUrl + "/enrollments/student/" + studentId + "/course/" + testCourse1.getId(),
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class
            );
            
            assertThat(withdrawResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            // Verify withdrawal
            ResponseEntity<List> enrollmentsResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId + "/enrollments",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
            );
            
            assertThat(enrollmentsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(enrollmentsResponse.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Grade Management Workflow")
    class GradeManagementWorkflow {

        @Test
        @DisplayName("Should handle complete grading workflow")
        void shouldHandleCompleteGradingWorkflow() {
            // Setup: Create student, course, and enrollment
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            Student student = createStudentRequest();
            ResponseEntity<Student> studentResponse = restTemplate.postForEntity(
                baseUrl + "/students", new HttpEntity<>(student, headers), Student.class);
            Long studentId = studentResponse.getBody().getId();
            
            EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
            enrollmentRequest.setStudentId(studentId);
            enrollmentRequest.setCourseId(testCourse1.getId());
            
            restTemplate.postForEntity(
                baseUrl + "/enrollments",
                new HttpEntity<>(enrollmentRequest, headers),
                Map.class
            );
            
            // Step 1: Add assignment grades
            Map<String, Object> gradeRequest = Map.of(
                "studentId", studentId,
                "courseId", testCourse1.getId(),
                "assignmentName", "Midterm Exam",
                "grade", 85.5,
                "maxPoints", 100.0,
                "weight", 0.3
            );
            
            ResponseEntity<Map> gradeResponse = restTemplate.postForEntity(
                baseUrl + "/grades",
                new HttpEntity<>(gradeRequest, headers),
                Map.class
            );
            
            assertThat(gradeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            
            // Step 2: Add final exam grade
            Map<String, Object> finalGradeRequest = Map.of(
                "studentId", studentId,
                "courseId", testCourse1.getId(),
                "assignmentName", "Final Exam",
                "grade", 92.0,
                "maxPoints", 100.0,
                "weight", 0.4
            );
            
            restTemplate.postForEntity(
                baseUrl + "/grades",
                new HttpEntity<>(finalGradeRequest, headers),
                Map.class
            );
            
            // Step 3: Calculate final course grade
            ResponseEntity<Map> finalGradeResponse = restTemplate.exchange(
                baseUrl + "/grades/student/" + studentId + "/course/" + testCourse1.getId() + "/final",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            assertThat(finalGradeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(finalGradeResponse.getBody()).containsKey("finalGrade");
            assertThat(finalGradeResponse.getBody()).containsKey("letterGrade");
            
            // Step 4: Update student GPA
            ResponseEntity<Student> updatedStudentResponse = restTemplate.exchange(
                baseUrl + "/students/" + studentId + "/calculate-gpa",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(headers),
                Student.class
            );
            
            assertThat(updatedStudentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(updatedStudentResponse.getBody().getGpa()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should generate grade reports")
        void shouldGenerateGradeReports() {
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            // Create student with grades (reuse setup from previous test)
            Student student = createStudentRequest();
            ResponseEntity<Student> studentResponse = restTemplate.postForEntity(
                baseUrl + "/students", new HttpEntity<>(student, headers), Student.class);
            Long studentId = studentResponse.getBody().getId();
            
            // Generate individual student report
            ResponseEntity<byte[]> reportResponse = restTemplate.exchange(
                baseUrl + "/reports/student/" + studentId + "/grades",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
            );
            
            assertThat(reportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reportResponse.getBody()).isNotEmpty();
            assertThat(reportResponse.getHeaders().get("Content-Type")).contains("application/pdf");
            
            // Generate course grade report
            ResponseEntity<byte[]> courseReportResponse = restTemplate.exchange(
                baseUrl + "/reports/course/" + testCourse1.getId() + "/grades",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
            );
            
            assertThat(courseReportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(courseReportResponse.getBody()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Data Import/Export Workflow")
    class DataImportExportWorkflow {

        @Test
        @DisplayName("Should import student data from CSV")
        void shouldImportStudentDataFromCsv() throws Exception {
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            headers.set("Content-Type", "multipart/form-data");
            
            // Create CSV content
            String csvContent = """
                student_id,first_name,last_name,email,department_id,admission_date,status
                CS2024001,John,Doe,john.doe@test.com,1,2024-08-15,ACTIVE
                CS2024002,Jane,Smith,jane.smith@test.com,1,2024-08-15,ACTIVE
                CS2024003,Bob,Johnson,bob.johnson@test.com,1,2024-08-15,ACTIVE
                """;
            
            // Create multipart request (simplified for testing)
            Map<String, Object> importRequest = Map.of(
                "csvData", csvContent,
                "validateData", true
            );
            
            ResponseEntity<Map> importResponse = restTemplate.postForEntity(
                baseUrl + "/import/students",
                new HttpEntity<>(importRequest, headers),
                Map.class
            );
            
            assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(importResponse.getBody()).containsKey("totalRecords");
            assertThat(importResponse.getBody()).containsKey("successfulImports");
            assertThat((Integer) importResponse.getBody().get("totalRecords")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should export student data to CSV")
        void shouldExportStudentDataToCsv() {
            // Setup: Create test students
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            for (int i = 1; i <= 3; i++) {
                Student student = createStudentRequest();
                student.setStudentId("EXP00" + i);
                restTemplate.postForEntity(
                    baseUrl + "/students",
                    new HttpEntity<>(student, headers),
                    Student.class
                );
            }
            
            // Export students
            ResponseEntity<byte[]> exportResponse = restTemplate.exchange(
                baseUrl + "/export/students?format=csv",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
            );
            
            assertThat(exportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(exportResponse.getBody()).isNotEmpty();
            assertThat(exportResponse.getHeaders().get("Content-Type")).contains("text/csv");
            assertThat(exportResponse.getHeaders().get("Content-Disposition"))
                .contains("attachment; filename=students.csv");
            
            // Verify CSV content
            String csvContent = new String(exportResponse.getBody());
            assertThat(csvContent).contains("student_id,first_name,last_name");
            assertThat(csvContent).contains("EXP001");
            assertThat(csvContent).contains("EXP002");
            assertThat(csvContent).contains("EXP003");
        }
    }

    @Nested
    @DisplayName("Concurrent Operations Tests")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle concurrent enrollment requests")
        void shouldHandleConcurrentEnrollmentRequests() throws InterruptedException, ExecutionException {
            // Setup
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            // Create course with limited capacity
            Course limitedCourse = new Course();
            limitedCourse.setCode("LIMITED101");
            limitedCourse.setTitle("Limited Capacity Course");
            limitedCourse.setCredits(3);
            limitedCourse.setMaxEnrollment(2);
            limitedCourse.setCurrentEnrollment(0);
            limitedCourse.setDepartment(testDepartment);
            
            ResponseEntity<Course> courseResponse = restTemplate.postForEntity(
                baseUrl + "/courses",
                new HttpEntity<>(limitedCourse, headers),
                Course.class
            );
            Long courseId = courseResponse.getBody().getId();
            
            // Create multiple students
            Student[] students = new Student[5];
            for (int i = 0; i < 5; i++) {
                Student student = createStudentRequest();
                student.setStudentId("CONC00" + (i + 1));
                ResponseEntity<Student> studentResponse = restTemplate.postForEntity(
                    baseUrl + "/students",
                    new HttpEntity<>(student, headers),
                    Student.class
                );
                students[i] = studentResponse.getBody();
            }
            
            // Create concurrent enrollment tasks
            CompletableFuture<ResponseEntity<Map>>[] futures = new CompletableFuture[5];
            for (int i = 0; i < 5; i++) {
                final int index = i;
                futures[i] = CompletableFuture.supplyAsync(() -> {
                    EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
                    enrollmentRequest.setStudentId(students[index].getId());
                    enrollmentRequest.setCourseId(courseId);
                    
                    return restTemplate.postForEntity(
                        baseUrl + "/enrollments",
                        new HttpEntity<>(enrollmentRequest, headers),
                        Map.class
                    );
                });
            }
            
            // Wait for all requests to complete
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
            
            // Count successful enrollments
            long successfulEnrollments = 0;
            long failedEnrollments = 0;
            
            for (CompletableFuture<ResponseEntity<Map>> future : futures) {
                ResponseEntity<Map> response = future.get();
                if (response.getStatusCode().is2xxSuccessful()) {
                    successfulEnrollments++;
                } else {
                    failedEnrollments++;
                }
            }
            
            // Verify only 2 students were enrolled (capacity limit)
            assertThat(successfulEnrollments).isEqualTo(2);
            assertThat(failedEnrollments).isEqualTo(3);
            
            // Verify course current enrollment
            ResponseEntity<Course> finalCourseState = restTemplate.exchange(
                baseUrl + "/courses/" + courseId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Course.class
            );
            
            assertThat(finalCourseState.getBody().getCurrentEnrollment()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Error Handling and Recovery")
    class ErrorHandlingAndRecovery {

        @Test
        @DisplayName("Should handle database connection failures gracefully")
        void shouldHandleDatabaseConnectionFailuresGracefully() {
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            // This test would typically require a way to simulate database failures
            // For now, we'll test general error responses
            
            // Try to access non-existent resource
            ResponseEntity<Map> errorResponse = restTemplate.exchange(
                baseUrl + "/students/99999",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(errorResponse.getBody()).containsKey("error");
            assertThat(errorResponse.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("Should validate request data and return appropriate errors")
        void shouldValidateRequestDataAndReturnAppropriateErrors() {
            authenticateAsAdmin();
            HttpHeaders headers = createAuthHeaders();
            
            // Test invalid student data
            Student invalidStudent = new Student();
            // Missing required fields
            
            ResponseEntity<Map> validationResponse = restTemplate.postForEntity(
                baseUrl + "/students",
                new HttpEntity<>(invalidStudent, headers),
                Map.class
            );
            
            assertThat(validationResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(validationResponse.getBody()).containsKey("errors");
        }

        @Test
        @DisplayName("Should handle authorization failures")
        void shouldHandleAuthorizationFailures() {
            // Try to access admin endpoint without authentication
            ResponseEntity<Map> unauthorizedResponse = restTemplate.getForEntity(
                baseUrl + "/admin/system-stats",
                Map.class
            );
            
            assertThat(unauthorizedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            
            // Try to access admin endpoint with student role
            authenticateAsStudent();
            HttpHeaders studentHeaders = createAuthHeaders();
            
            ResponseEntity<Map> forbiddenResponse = restTemplate.exchange(
                baseUrl + "/admin/system-stats",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(studentHeaders),
                Map.class
            );
            
            assertThat(forbiddenResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // Helper methods
    private void createTestDepartment() {
        testDepartment = new Department();
        testDepartment.setCode("CS");
        testDepartment.setName("Computer Science");
        testDepartment.setDescription("Department of Computer Science and Engineering");
        testDepartment.setHeadName("Dr. John Smith");
        testDepartment.setCreatedAt(LocalDateTime.now());
        testDepartment.setUpdatedAt(LocalDateTime.now());
        testDepartment = departmentRepository.save(testDepartment);
    }
    
    private void createTestCourses() {
        testCourse1 = new Course();
        testCourse1.setCode("CS101");
        testCourse1.setTitle("Introduction to Programming");
        testCourse1.setDescription("Basic programming concepts and techniques");
        testCourse1.setCredits(3);
        testCourse1.setMaxEnrollment(30);
        testCourse1.setCurrentEnrollment(0);
        testCourse1.setDepartment(testDepartment);
        testCourse1.setCreatedAt(LocalDateTime.now());
        testCourse1.setUpdatedAt(LocalDateTime.now());
        testCourse1 = courseRepository.save(testCourse1);
        
        testCourse2 = new Course();
        testCourse2.setCode("CS201");
        testCourse2.setTitle("Data Structures");
        testCourse2.setDescription("Advanced data structures and algorithms");
        testCourse2.setCredits(4);
        testCourse2.setMaxEnrollment(25);
        testCourse2.setCurrentEnrollment(0);
        testCourse2.setDepartment(testDepartment);
        testCourse2.setCreatedAt(LocalDateTime.now());
        testCourse2.setUpdatedAt(LocalDateTime.now());
        testCourse2 = courseRepository.save(testCourse2);
    }

    private RegisterRequest createRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser" + System.currentTimeMillis());
        request.setEmail("test" + System.currentTimeMillis() + "@smartcampus.edu");
        request.setPassword("TestPass123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setPhone("555-1234");
        request.setRole(UserRole.STUDENT);
        return request;
    }

    private Student createStudentRequest() {
        Student student = new Student();
        student.setStudentId("STU" + System.currentTimeMillis());
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setEmail("student" + System.currentTimeMillis() + "@test.com");
        student.setDepartment(testDepartment);
        student.setStatus(StudentStatus.ACTIVE);
        student.setEnrollmentDate(LocalDate.now());
        student.setExpectedGraduation(LocalDate.now().plusYears(4));
        student.setGpa(3.0);
        student.setTotalCredits(0);
        return student;
    }

    private void authenticateAsAdmin() {
        // Create admin user and get token
        RegisterRequest adminRequest = createRegisterRequest();
        adminRequest.setRole(UserRole.ADMIN);
        
        restTemplate.postForEntity(baseUrl + "/auth/register", adminRequest, Map.class);
        
        // Verify admin user
        User adminUser = userRepository.findByEmail(adminRequest.getEmail()).orElse(null);
        adminUser.setVerified(true);
        userRepository.save(adminUser);
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(adminRequest.getUsername());
        loginRequest.setPassword(adminRequest.getPassword());
        
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            baseUrl + "/auth/login", loginRequest, LoginResponse.class);
        
        authToken = response.getBody().getToken();
    }

    private void authenticateAsStudent() {
        RegisterRequest studentRequest = createRegisterRequest();
        studentRequest.setRole(UserRole.STUDENT);
        
        restTemplate.postForEntity(baseUrl + "/auth/register", studentRequest, Map.class);
        
        User studentUser = userRepository.findByEmail(studentRequest.getEmail()).orElse(null);
        studentUser.setVerified(true);
        userRepository.save(studentUser);
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(studentRequest.getUsername());
        loginRequest.setPassword(studentRequest.getPassword());
        
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            baseUrl + "/auth/login", loginRequest, LoginResponse.class);
        
        authToken = response.getBody().getToken();
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}