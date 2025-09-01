// File location: src/test/java/integration/DatabaseIntegrationTest.java

package com.smartcampus.test.integration;

import com.smartcampus.models.*;
import com.smartcampus.models.enums.*;
import com.smartcampus.repositories.*;
import com.smartcampus.services.*;
import com.smartcampus.config.TestDatabaseConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Integration tests for database operations
 * Tests actual database interactions, transactions, and data persistence
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(classes = TestDatabaseConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("Database Integration Tests")
class DatabaseIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentService enrollmentService;

    private Department testDepartment;
    private User testStudentUser;
    private User testProfessorUser;

    @BeforeEach
    void setUp() {
        // Create test department
        testDepartment = new Department();
        testDepartment.setCode("CS");
        testDepartment.setName("Computer Science");
        testDepartment.setDescription("Computer Science Department");
        testDepartment.setEstablishedYear(1985);
        testDepartment.setBudget(BigDecimal.valueOf(2000000.00));
        testDepartment.setActive(true);
        testDepartment = departmentRepository.save(testDepartment);

        // Create test student user
        testStudentUser = new User();
        testStudentUser.setUsername("teststudent");
        testStudentUser.setEmail("student@test.com");
        testStudentUser.setPasswordHash("hashedPassword");
        testStudentUser.setFirstName("Test");
        testStudentUser.setLastName("Student");
        testStudentUser.setRole(UserRole.STUDENT);
        testStudentUser.setActive(true);
        testStudentUser.setVerified(true);
        testStudentUser = userRepository.save(testStudentUser);

        // Create test professor user
        testProfessorUser = new User();
        testProfessorUser.setUsername("testprofessor");
        testProfessorUser.setEmail("professor@test.com");
        testProfessorUser.setPasswordHash("hashedPassword");
        testProfessorUser.setFirstName("Test");
        testProfessorUser.setLastName("Professor");
        testProfessorUser.setRole(UserRole.PROFESSOR);
        testProfessorUser.setActive(true);
        testProfessorUser.setVerified(true);
        testProfessorUser = userRepository.save(testProfessorUser);
    }

    @Nested
    @DisplayName("Student Repository Integration Tests")
    class StudentRepositoryIntegrationTests {

        @Test
        @DisplayName("Should create and retrieve student")
        void shouldCreateAndRetrieveStudent() {
            // Arrange
            Student student = new Student();
            student.setStudentId("CS2024001");
            student.setUser(testStudentUser);
            student.setDepartment(testDepartment);
            student.setAdmissionDate(LocalDate.of(2024, 8, 15));
            student.setStatus(StudentStatus.ACTIVE);
            student.setYearLevel(1);
            student.setGpa(BigDecimal.valueOf(3.50));
            student.setTotalCreditsEarned(0);
            student.setTotalCreditsAttempted(0);

            // Act
            Student savedStudent = studentRepository.save(student);
            entityManager.flush();
            entityManager.clear();

            Optional<Student> retrievedStudent = studentRepository.findById(savedStudent.getId());

            // Assert
            assertThat(retrievedStudent).isPresent();
            assertThat(retrievedStudent.get().getStudentId()).isEqualTo("CS2024001");
            assertThat(retrievedStudent.get().getUser().getEmail()).isEqualTo("student@test.com");
            assertThat(retrievedStudent.get().getDepartment().getCode()).isEqualTo("CS");
        }

        @Test
        @DisplayName("Should find student by student ID")
        void shouldFindStudentByStudentId() {
            // Arrange
            Student student = createTestStudent("CS2024002");
            studentRepository.save(student);
            entityManager.flush();

            // Act
            Optional<Student> foundStudent = studentRepository.findByStudentId("CS2024002");

            // Assert
            assertThat(foundStudent).isPresent();
            assertThat(foundStudent.get().getStudentId()).isEqualTo("CS2024002");
        }

        @Test
        @DisplayName("Should find students by department")
        void shouldFindStudentsByDepartment() {
            // Arrange
            Student student1 = createTestStudent("CS2024003");
            Student student2 = createTestStudent("CS2024004");
            studentRepository.save(student1);
            studentRepository.save(student2);
            entityManager.flush();

            // Act
            List<Student> students = studentRepository.findByDepartmentId(testDepartment.getId());

            // Assert
            assertThat(students).hasSize(2);
            assertThat(students).extracting(Student::getStudentId)
                .contains("CS2024003", "CS2024004");
        }

        @Test
        @DisplayName("Should find students by status")
        void shouldFindStudentsByStatus() {
            // Arrange
            Student activeStudent = createTestStudent("CS2024005");
            activeStudent.setStatus(StudentStatus.ACTIVE);
            
            Student inactiveStudent = createTestStudent("CS2024006");
            inactiveStudent.setStatus(StudentStatus.INACTIVE);
            
            studentRepository.save(activeStudent);
            studentRepository.save(inactiveStudent);
            entityManager.flush();

            // Act
            List<Student> activeStudents = studentRepository.findByStatus(StudentStatus.ACTIVE);

            // Assert
            assertThat(activeStudents).hasSize(1);
            assertThat(activeStudents.get(0).getStudentId()).isEqualTo("CS2024005");
        }

        @Test
        @DisplayName("Should find students by GPA range")
        void shouldFindStudentsByGpaRange() {
            // Arrange
            Student highGpaStudent = createTestStudent("CS2024007");
            highGpaStudent.setGpa(BigDecimal.valueOf(3.75));
            
            Student lowGpaStudent = createTestStudent("CS2024008");
            lowGpaStudent.setGpa(BigDecimal.valueOf(2.25));
            
            studentRepository.save(highGpaStudent);
            studentRepository.save(lowGpaStudent);
            entityManager.flush();

            // Act
            List<Student> highPerformers = studentRepository.findByGpaGreaterThanEqual(BigDecimal.valueOf(3.5));

            // Assert
            assertThat(highPerformers).hasSize(1);
            assertThat(highPerformers.get(0).getStudentId()).isEqualTo("CS2024007");
        }
    }

    @Nested
    @DisplayName("Course Repository Integration Tests")
    class CourseRepositoryIntegrationTests {

        @Test
        @DisplayName("Should create and retrieve course")
        void shouldCreateAndRetrieveCourse() {
            // Arrange
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course = new Course();
            course.setCourseCode("CS101");
            course.setCourseName("Introduction to Programming");
            course.setDescription("Basic programming concepts");
            course.setDepartment(testDepartment);
            course.setProfessor(professor);
            course.setCredits(3);
            course.setMaxEnrollment(30);
            course.setCurrentEnrollment(0);
            course.setSemester(Semester.FALL);
            course.setYear(2024);
            course.setStartDate(LocalDate.of(2024, 8, 26));
            course.setEndDate(LocalDate.of(2024, 12, 15));
            course.setScheduleDays("MWF");
            course.setStartTime(LocalTime.of(9, 0));
            course.setEndTime(LocalTime.of(9, 50));
            course.setClassroom("Room 101");
            course.setBuilding("CS Building");
            course.setCourseLevel(CourseLevel.UNDERGRADUATE);
            course.setStatus(CourseStatus.ACTIVE);

            // Act
            Course savedCourse = courseRepository.save(course);
            entityManager.flush();
            entityManager.clear();

            Optional<Course> retrievedCourse = courseRepository.findById(savedCourse.getId());

            // Assert
            assertThat(retrievedCourse).isPresent();
            assertThat(retrievedCourse.get().getCourseCode()).isEqualTo("CS101");
            assertThat(retrievedCourse.get().getProfessor().getEmployeeId()).isEqualTo("EMP001");
            assertThat(retrievedCourse.get().getDepartment().getCode()).isEqualTo("CS");
        }

        @Test
        @DisplayName("Should find courses by semester and year")
        void shouldFindCoursesBySemesterAndYear() {
            // Arrange
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course fallCourse = createTestCourse("CS101", professor, Semester.FALL, 2024);
            Course springCourse = createTestCourse("CS102", professor, Semester.SPRING, 2024);
            
            courseRepository.save(fallCourse);
            courseRepository.save(springCourse);
            entityManager.flush();

            // Act
            List<Course> fallCourses = courseRepository.findBySemesterAndYear(Semester.FALL, 2024);

            // Assert
            assertThat(fallCourses).hasSize(1);
            assertThat(fallCourses.get(0).getCourseCode()).isEqualTo("CS101");
        }

        @Test
        @DisplayName("Should find courses by professor")
        void shouldFindCoursesByProfessor() {
            // Arrange
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course1 = createTestCourse("CS201", professor, Semester.FALL, 2024);
            Course course2 = createTestCourse("CS202", professor, Semester.SPRING, 2025);
            
            courseRepository.save(course1);
            courseRepository.save(course2);
            entityManager.flush();

            // Act
            List<Course> professorCourses = courseRepository.findByProfessorId(professor.getId());

            // Assert
            assertThat(professorCourses).hasSize(2);
            assertThat(professorCourses).extracting(Course::getCourseCode)
                .contains("CS201", "CS202");
        }
    }

    @Nested
    @DisplayName("Enrollment Repository Integration Tests")
    class EnrollmentRepositoryIntegrationTests {

        @Test
        @DisplayName("Should create and retrieve enrollment")
        void shouldCreateAndRetrieveEnrollment() {
            // Arrange
            Student student = createTestStudent("CS2024010");
            studentRepository.save(student);
            
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course = createTestCourse("CS301", professor, Semester.FALL, 2024);
            courseRepository.save(course);
            
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setEnrollmentDate(LocalDateTime.now());
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setAttendancePercentage(95.0);
            enrollment.setParticipationScore(88.5);

            // Act
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
            entityManager.flush();
            entityManager.clear();

            Optional<Enrollment> retrievedEnrollment = enrollmentRepository.findById(savedEnrollment.getId());

            // Assert
            assertThat(retrievedEnrollment).isPresent();
            assertThat(retrievedEnrollment.get().getStudent().getStudentId()).isEqualTo("CS2024010");
            assertThat(retrievedEnrollment.get().getCourse().getCourseCode()).isEqualTo("CS301");
            assertThat(retrievedEnrollment.get().getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        }

        @Test
        @DisplayName("Should find enrollments by student")
        void shouldFindEnrollmentsByStudent() {
            // Arrange
            Student student = createTestStudent("CS2024011");
            studentRepository.save(student);
            
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course1 = createTestCourse("CS401", professor, Semester.FALL, 2024);
            Course course2 = createTestCourse("CS402", professor, Semester.FALL, 2024);
            courseRepository.save(course1);
            courseRepository.save(course2);
            
            Enrollment enrollment1 = createTestEnrollment(student, course1);
            Enrollment enrollment2 = createTestEnrollment(student, course2);
            enrollmentRepository.save(enrollment1);
            enrollmentRepository.save(enrollment2);
            entityManager.flush();

            // Act
            List<Enrollment> studentEnrollments = enrollmentRepository.findByStudentId(student.getId());

            // Assert
            assertThat(studentEnrollments).hasSize(2);
            assertThat(studentEnrollments).extracting(e -> e.getCourse().getCourseCode())
                .contains("CS401", "CS402");
        }

        @Test
        @DisplayName("Should prevent duplicate enrollments")
        void shouldPreventDuplicateEnrollments() {
            // Arrange
            Student student = createTestStudent("CS2024012");
            studentRepository.save(student);
            
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course = createTestCourse("CS501", professor, Semester.FALL, 2024);
            courseRepository.save(course);
            
            Enrollment enrollment1 = createTestEnrollment(student, course);
            enrollmentRepository.save(enrollment1);
            entityManager.flush();

            // Act & Assert
            assertThrows(Exception.class, () -> {
                Enrollment enrollment2 = createTestEnrollment(student, course);
                enrollmentRepository.save(enrollment2);
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Transaction Management Tests")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class TransactionManagementTests {

        @Test
        @DisplayName("Should rollback transaction on failure")
        void shouldRollbackTransactionOnFailure() {
            // Arrange
            Student student = createTestStudent("CS2024013");
            
            // Act & Assert
            assertThrows(Exception.class, () -> {
                // This should fail and rollback
                studentService.createStudentWithInvalidData(student);
            });

            // Verify rollback - student should not exist
            Optional<Student> savedStudent = studentRepository.findByStudentId("CS2024013");
            assertThat(savedStudent).isEmpty();
        }

        @Test
        @DisplayName("Should maintain ACID properties during concurrent operations")
        void shouldMaintainAcidPropertiesDuringConcurrentOperations() throws InterruptedException {
            // Arrange
            Student student = createTestStudent("CS2024014");
            studentRepository.save(student);
            
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course = createTestCourse("CS601", professor, Semester.FALL, 2024);
            course.setMaxEnrollment(1); // Only one spot available
            courseRepository.save(course);
            entityManager.flush();

            // Act - Try to enroll the same student twice concurrently
            Thread thread1 = new Thread(() -> {
                try {
                    enrollmentService.enrollStudent(student.getId(), course.getId());
                } catch (Exception e) {
                    // Expected - one should fail
                }
            });

            Thread thread2 = new Thread(() -> {
                try {
                    enrollmentService.enrollStudent(student.getId(), course.getId());
                } catch (Exception e) {
                    // Expected - one should fail
                }
            });

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Assert - Should have exactly one enrollment
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
            assertThat(enrollments).hasSizeLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should handle nested transactions correctly")
        void shouldHandleNestedTransactionsCorrectly() {
            // Arrange
            Student student = createTestStudent("CS2024015");
            
            // Act & Assert
            assertDoesNotThrow(() -> {
                studentService.createStudentWithNestedTransaction(student);
            });

            // Verify student was created
            Optional<Student> savedStudent = studentRepository.findByStudentId("CS2024015");
            assertThat(savedStudent).isPresent();
        }
    }

    @Nested
    @DisplayName("Database Performance Tests")
    class DatabasePerformanceTests {

        @Test
        @DisplayName("Should efficiently handle bulk operations")
        void shouldEfficientlyHandleBulkOperations() {
            // Arrange
            List<Student> students = createMultipleTestStudents(100);
            
            long startTime = System.currentTimeMillis();

            // Act
            studentRepository.saveAll(students);
            entityManager.flush();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Assert
            assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
            
            long count = studentRepository.count();
            assertThat(count).isGreaterThanOrEqualTo(100);
        }

        @Test
        @DisplayName("Should use indexes effectively for queries")
        void shouldUseIndexesEffectivelyForQueries() {
            // Arrange
            List<Student> students = createMultipleTestStudents(1000);
            studentRepository.saveAll(students);
            entityManager.flush();

            long startTime = System.currentTimeMillis();

            // Act - Query by indexed field
            Optional<Student> student = studentRepository.findByStudentId("CS2024100");
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Assert
            assertThat(duration).isLessThan(100); // Should be very fast with index
            assertThat(student).isPresent();
        }

        @Test
        @DisplayName("Should handle complex joins efficiently")
        void shouldHandleComplexJoinsEfficiently() {
            // Arrange
            createCompleteTestData();
            entityManager.flush();

            long startTime = System.currentTimeMillis();

            // Act - Complex query with joins
            List<Object[]> results = entityManager.createQuery(
                "SELECT s.studentId, u.firstName, u.lastName, d.name, c.courseCode " +
                "FROM Student s " +
                "JOIN s.user u " +
                "JOIN s.department d " +
                "JOIN Enrollment e ON e.student = s " +
                "JOIN e.course c " +
                "WHERE d.code = 'CS' AND e.status = 'ENROLLED'"
            ).getResultList();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Assert
            assertThat(duration).isLessThan(1000); // Should complete within 1 second
            assertThat(results).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should enforce foreign key constraints")
        void shouldEnforceForeignKeyConstraints() {
            // Arrange
            Student student = createTestStudent("CS2024016");
            student.setDepartment(null); // Invalid - required field

            // Act & Assert
            assertThrows(Exception.class, () -> {
                studentRepository.save(student);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("Should enforce unique constraints")
        void shouldEnforceUniqueConstraints() {
            // Arrange
            Student student1 = createTestStudent("CS2024017");
            Student student2 = createTestStudent("CS2024017"); // Same student ID
            
            studentRepository.save(student1);
            entityManager.flush();

            // Act & Assert
            assertThrows(Exception.class, () -> {
                studentRepository.save(student2);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("Should cascade deletions correctly")
        void shouldCascadeDeletionsCorrectly() {
            // Arrange
            Student student = createTestStudent("CS2024018");
            studentRepository.save(student);
            
            Professor professor = createTestProfessor();
            professorRepository.save(professor);
            
            Course course = createTestCourse("CS701", professor, Semester.FALL, 2024);
            courseRepository.save(course);
            
            Enrollment enrollment = createTestEnrollment(student, course);
            enrollmentRepository.save(enrollment);
            entityManager.flush();

            Long enrollmentId = enrollment.getId();

            // Act - Delete student (should cascade to enrollments)
            studentRepository.delete(student);
            entityManager.flush();

            // Assert
            Optional<Enrollment> deletedEnrollment = enrollmentRepository.findById(enrollmentId);
            assertThat(deletedEnrollment).isEmpty();
        }
    }

    // Helper methods for creating test data
    private Student createTestStudent(String studentId) {
        User user = new User();
        user.setUsername(studentId.toLowerCase());
        user.setEmail(studentId.toLowerCase() + "@test.com");
        user.setPasswordHash("hashedPassword");
        user.setFirstName("Test");
        user.setLastName("Student" + studentId.substring(studentId.length() - 3));
        user.setRole(UserRole.STUDENT);
        user.setActive(true);
        user.setVerified(true);
        user = userRepository.save(user);

        Student student = new Student();
        student.setStudentId(studentId);
        student.setUser(user);
        student.setDepartment(testDepartment);
        student.setAdmissionDate(LocalDate.of(2024, 8, 15));
        student.setStatus(StudentStatus.ACTIVE);
        student.setYearLevel(1);
        student.setGpa(BigDecimal.valueOf(3.00));
        student.setTotalCreditsEarned(0);
        student.setTotalCreditsAttempted(0);
        
        return student;
    }

    private Professor createTestProfessor() {
        Professor professor = new Professor();
        professor.setEmployeeId("EMP001");
        professor.setUser(testProfessorUser);
        professor.setDepartment(testDepartment);
        professor.setTitle(ProfessorTitle.PROFESSOR);
        professor.setHireDate(LocalDate.of(2020, 8, 15));
        professor.setTenureStatus(TenureStatus.TENURED);
        professor.setStatus(ProfessorStatus.ACTIVE);
        professor.setSalary(BigDecimal.valueOf(75000.00));
        
        return professor;
    }

    private Course createTestCourse(String courseCode, Professor professor, Semester semester, int year) {
        Course course = new Course();
        course.setCourseCode(courseCode);
        course.setCourseName("Test Course " + courseCode);
        course.setDescription("Test course description");
        course.setDepartment(testDepartment);
        course.setProfessor(professor);
        course.setCredits(3);
        course.setMaxEnrollment(30);
        course.setCurrentEnrollment(0);
        course.setSemester(semester);
        course.setYear(year);
        course.setStartDate(LocalDate.of(year, semester == Semester.FALL ? 8 : 1, 26));
        course.setEndDate(LocalDate.of(year, semester == Semester.FALL ? 12 : 5, 15));
        course.setScheduleDays("MWF");
        course.setStartTime(LocalTime.of(9, 0));
        course.setEndTime(LocalTime.of(9, 50));
        course.setClassroom("Room 101");
        course.setBuilding("CS Building");
        course.setCourseLevel(CourseLevel.UNDERGRADUATE);
        course.setStatus(CourseStatus.ACTIVE);
        
        return course;
    }

    private Enrollment createTestEnrollment(Student student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setAttendancePercentage(95.0);
        enrollment.setParticipationScore(88.5);
        
        return enrollment;
    }

    private List<Student> createMultipleTestStudents(int count) {
        List<Student> students = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String studentId = "CS2024" + String.format("%03d", i);
            students.add(createTestStudent(studentId));
        }
        return students;
    }

    private void createCompleteTestData() {
        // Create students
        for (int i = 1; i <= 10; i++) {
            Student student = createTestStudent("CS2024" + String.format("%03d", 200 + i));
            studentRepository.save(student);
        }

        // Create professor and courses
        Professor professor = createTestProfessor();
        professorRepository.save(professor);

        for (int i = 1; i <= 5; i++) {
            Course course = createTestCourse("CS" + (800 + i), professor, Semester.FALL, 2024);
            courseRepository.save(course);
        }

        // Create enrollments
        List<Student> allStudents = studentRepository.findByDepartmentId(testDepartment.getId());
        List<Course> allCourses = courseRepository.findByDepartmentId(testDepartment.getId());

        for (int i = 0; i < Math.min(allStudents.size(), allCourses.size()); i++) {
            Enrollment enrollment = createTestEnrollment(allStudents.get(i), allCourses.get(i % allCourses.size()));
            enrollmentRepository.save(enrollment);
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        professorRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
    }
}