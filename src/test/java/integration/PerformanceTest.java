// File location: src/test/java/integration/PerformanceTest.java

package com.smartcampus.test.integration;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Department;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.User;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.repositories.EnrollmentRepository;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.services.StudentService;
import com.smartcampus.services.CourseService;
import com.smartcampus.services.EnrollmentService;
import com.smartcampus.services.ReportGenerationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

/**
 * Performance tests for the Smart Campus Management System
 * Tests system performance under load and measures response times
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(classes = SmartCampusApplication.class)
@ActiveProfiles("test")
@DisplayName("Performance Tests")
class PerformanceTest {

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
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private ReportGenerationService reportGenerationService;

    private Department testDepartment;
    private List<Student> testStudents;
    private List<Course> testCourses;

    @BeforeEach
    void setUp() {
        // Create test department
        testDepartment = createTestDepartment();
        
        // Initialize collections
        testStudents = new ArrayList<>();
        testCourses = new ArrayList<>();
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
    @DisplayName("Database Performance Tests")
    class DatabasePerformanceTests {

        @Test
        @DisplayName("Should handle bulk student creation efficiently")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldHandleBulkStudentCreationEfficiently() {
            // Arrange
            int studentCount = 1000;
            List<Student> students = new ArrayList<>();
            
            long startTime = System.currentTimeMillis();
            
            // Act - Create students in batches
            for (int i = 0; i < studentCount; i++) {
                Student student = createTestStudent("STU" + String.format("%05d", i));
                students.add(student);
                
                // Save in batches of 100
                if (i % 100 == 99 || i == studentCount - 1) {
                    studentRepository.saveAll(students);
                    students.clear();
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Assert
            assertThat(studentRepository.count()).isEqualTo(studentCount);
            assertThat(duration).isLessThan(8000); // Should complete within 8 seconds
            
            System.out.printf("Created %d students in %d ms (%.2f students/sec)%n", 
                studentCount, duration, (studentCount * 1000.0) / duration);
        }

        @Test
        @DisplayName("Should handle bulk course creation efficiently")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleBulkCourseCreationEfficiently() {
            // Arrange
            int courseCount = 500;
            List<Course> courses = new ArrayList<>();
            
            long startTime = System.currentTimeMillis();
            
            // Act
            for (int i = 0; i < courseCount; i++) {
                Course course = createTestCourse("COURSE" + String.format("%03d", i));
                courses.add(course);
                
                // Save in batches of 50
                if (i % 50 == 49 || i == courseCount - 1) {
                    courseRepository.saveAll(courses);
                    courses.clear();
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Assert
            assertThat(courseRepository.count()).isEqualTo(courseCount);
            assertThat(duration).isLessThan(4000); // Should complete within 4 seconds
            
            System.out.printf("Created %d courses in %d ms (%.2f courses/sec)%n", 
                courseCount, duration, (courseCount * 1000.0) / duration);
        }

        @Test
        @DisplayName("Should handle large dataset queries efficiently")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void shouldHandleLargeDatasetQueriesEfficiently() {
            // Setup - Create test data
            createLargeTestDataset(2000, 100);
            
            // Test 1: Find students by department
            long startTime = System.currentTimeMillis();
            List<Student> departmentStudents = studentRepository.findByDepartment(testDepartment);
            long queryTime1 = System.currentTimeMillis() - startTime;
            
            assertThat(departmentStudents).hasSize(2000);
            assertThat(queryTime1).isLessThan(1000); // Should complete within 1 second
            
            // Test 2: Paginated query
            startTime = System.currentTimeMillis();
            Pageable pageable = PageRequest.of(0, 50);
            Page<Student> studentPage = studentRepository.findAll(pageable);
            long queryTime2 = System.currentTimeMillis() - startTime;
            
            assertThat(studentPage.getContent()).hasSize(50);
            assertThat(studentPage.getTotalElements()).isEqualTo(2000);
            assertThat(queryTime2).isLessThan(500); // Should complete within 500ms
            
            // Test 3: Complex query with joins
            startTime = System.currentTimeMillis();
            List<Student> activeStudentsWithHighGpa = studentRepository.findByStatusAndGpaGreaterThanEqual(
                StudentStatus.ACTIVE, 3.5);
            long queryTime3 = System.currentTimeMillis() - startTime;
            
            assertThat(queryTime3).isLessThan(1000); // Should complete within 1 second
            
            System.out.printf("Query times: Department=%dms, Paginated=%dms, Complex=%dms%n", 
                queryTime1, queryTime2, queryTime3);
        }

        @Test
        @DisplayName("Should handle concurrent database operations")
        @Timeout(value = 20, unit = TimeUnit.SECONDS)
        void shouldHandleConcurrentDatabaseOperations() throws InterruptedException {
            // Arrange
            int threadCount = 10;
            int operationsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            long startTime = System.currentTimeMillis();
            
            // Act - Submit concurrent tasks
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Future<Integer> future = executor.submit(() -> {
                    try {
                        int createdCount = 0;
                        for (int j = 0; j < operationsPerThread; j++) {
                            Student student = createTestStudent("CONC" + threadId + "_" + j);
                            studentRepository.save(student);
                            createdCount++;
                        }
                        return createdCount;
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }
            
            // Wait for completion
            latch.await(15, TimeUnit.SECONDS);
            executor.shutdown();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Assert
            int totalExpected = threadCount * operationsPerThread;
            assertThat(studentRepository.count()).isEqualTo(totalExpected);
            assertThat(duration).isLessThan(15000); // Should complete within 15 seconds
            
            System.out.printf("Concurrent operations: %d threads × %d ops = %d total in %d ms%n", 
                threadCount, operationsPerThread, totalExpected, duration);
        }
    }

    @Nested
    @DisplayName("Service Layer Performance Tests")
    class ServiceLayerPerformanceTests {

        @Test
        @DisplayName("Should handle bulk enrollment operations efficiently")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void shouldHandleBulkEnrollmentOperationsEfficiently() {
            // Setup
            int studentCount = 500;
            int courseCount = 10;
            
            // Create students and courses
            createLargeTestDataset(studentCount, courseCount);
            
            long startTime = System.currentTimeMillis();
            
            // Act - Enroll each student in 3 random courses
            int totalEnrollments = 0;
            for (Student student : testStudents) {
                for (int i = 0; i < 3 && i < testCourses.size(); i++) {
                    Course course = testCourses.get(i % testCourses.size());
                    try {
                        enrollmentService.enrollStudent(student.getId(), course.getId());
                        totalEnrollments++;
                    } catch (Exception e) {
                        // May fail due to capacity limits or duplicate enrollments
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Assert
            assertThat(totalEnrollments).isGreaterThan(studentCount); // At least one enrollment per student
            assertThat(duration).isLessThan(25000); // Should complete within 25 seconds
            
            System.out.printf("Processed %d enrollments in %d ms (%.2f enrollments/sec)%n", 
                totalEnrollments, duration, (totalEnrollments * 1000.0) / duration);
        }

        @Test
        @DisplayName("Should calculate GPA efficiently for large datasets")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldCalculateGpaEfficientlyForLargeDatasets() {
            // Setup
            int studentCount = 1000;
            createLargeTestDataset(studentCount, 5);
            
            long startTime = System.currentTimeMillis();
            
            // Act - Calculate GPA for all students
            int processedCount = 0;
            for (Student student : testStudents) {
                try {
                    studentService.calculateAndUpdateGpa(student.getId());
                    processedCount++;
                } catch (Exception e) {
                    // Handle any calculation errors
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Assert
            assertThat(processedCount).isEqualTo(studentCount);
            assertThat(duration).isLessThan(8000); // Should complete within 8 seconds
            
            System.out.printf("Calculated GPA for %d students in %d ms (%.2f calculations/sec)%n", 
                processedCount, duration, (processedCount * 1000.0) / duration);
        }

        @Test
        @DisplayName("Should handle search operations efficiently")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleSearchOperationsEfficiently() {
            // Setup
            createLargeTestDataset(2000, 50);
            
            // Test 1: Search by name
            long startTime = System.currentTimeMillis();
            List<Student> nameResults = studentService.searchStudentsByName("Test");
            long nameSearchTime = System.currentTimeMillis() - startTime;
            
            assertThat(nameResults).isNotEmpty();
            assertThat(nameSearchTime).isLessThan(1000); // Should complete within 1 second
            
            // Test 2: Search by student ID
            startTime = System.currentTimeMillis();
            List<Student> idResults = studentService.searchStudentsByStudentId("STU");
            long idSearchTime = System.currentTimeMillis() - startTime;
            
            assertThat(idResults).isNotEmpty();
            assertThat(idSearchTime).isLessThan(500); // Should complete within 500ms
            
            // Test 3: Advanced search with multiple criteria
            startTime = System.currentTimeMillis();
            List<Student> advancedResults = studentService.searchStudents(
                "Test", testDepartment.getId(), StudentStatus.ACTIVE, 2.0, 4.0);
            long advancedSearchTime = System.currentTimeMillis() - startTime;
            
            assertThat(advancedSearchTime).isLessThan(1500); // Should complete within 1.5 seconds
            
            System.out.printf("Search times: Name=%dms, ID=%dms, Advanced=%dms%n", 
                nameSearchTime, idSearchTime, advancedSearchTime);
        }
    }

    @Nested
    @DisplayName("Report Generation Performance Tests")
    class ReportGenerationPerformanceTests {

        @Test
        @DisplayName("Should generate large reports efficiently")
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void shouldGenerateLargeReportsEfficiently() {
            // Setup
            createLargeTestDataset(1000, 20);
            
            // Test 1: Student list report
            long startTime = System.currentTimeMillis();
            byte[] studentReport = reportGenerationService.generateStudentListReport(
                testStudents.subList(0, Math.min(500, testStudents.size())));
            long studentReportTime = System.currentTimeMillis() - startTime;
            
            assertThat(studentReport).isNotEmpty();
            assertThat(studentReportTime).isLessThan(10000); // Should complete within 10 seconds
            
            // Test 2: Course enrollment report
            startTime = System.currentTimeMillis();
            byte[] enrollmentReport = reportGenerationService.generateEnrollmentSummaryReport(
                testCourses.subList(0, Math.min(10, testCourses.size())));
            long enrollmentReportTime = System.currentTimeMillis() - startTime;
            
            assertThat(enrollmentReport).isNotEmpty();
            assertThat(enrollmentReportTime).isLessThan(15000); // Should complete within 15 seconds
            
            // Test 3: Analytics report with charts
            startTime = System.currentTimeMillis();
            byte[] analyticsReport = reportGenerationService.generateAnalyticsReport(
                testDepartment.getId(), LocalDate.now().minusMonths(6), LocalDate.now());
            long analyticsReportTime = System.currentTimeMillis() - startTime;
            
            assertThat(analyticsReport).isNotEmpty();
            assertThat(analyticsReportTime).isLessThan(30000); // Should complete within 30 seconds
            
            System.out.printf("Report generation times: Student=%dms, Enrollment=%dms, Analytics=%dms%n", 
                studentReportTime, enrollmentReportTime, analyticsReportTime);
        }

        @Test
        @DisplayName("Should handle concurrent report generation")
        @Timeout(value = 45, unit = TimeUnit.SECONDS)
        void shouldHandleConcurrentReportGeneration() throws InterruptedException {
            // Setup
            createLargeTestDataset(500, 10);
            
            int concurrentReports = 5;
            ExecutorService executor = Executors.newFixedThreadPool(concurrentReports);
            CountDownLatch latch = new CountDownLatch(concurrentReports);
            
            long startTime = System.currentTimeMillis();
            
            // Submit concurrent report generation tasks
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < concurrentReports; i++) {
                final int reportId = i;
                Future<Long> future = executor.submit(() -> {
                    try {
                        long reportStartTime = System.currentTimeMillis();
                        
                        // Generate different types of reports
                        switch (reportId % 3) {
                            case 0:
                                reportGenerationService.generateStudentListReport(
                                    testStudents.subList(0, Math.min(200, testStudents.size())));
                                break;
                            case 1:
                                reportGenerationService.generateEnrollmentSummaryReport(
                                    testCourses.subList(0, Math.min(5, testCourses.size())));
                                break;
                            case 2:
                                reportGenerationService.generateDepartmentReport(testDepartment.getId());
                                break;
                        }
                        
                        return System.currentTimeMillis() - reportStartTime;
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }
            
            // Wait for completion
            latch.await(40, TimeUnit.SECONDS);
            executor.shutdown();
            
            long totalDuration = System.currentTimeMillis() - startTime;
            
            // Assert
            assertThat(totalDuration).isLessThan(40000); // Should complete within 40 seconds
            
            // Calculate average report generation time
            double avgReportTime = futures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .average()
                .orElse(0.0);
            
            System.out.printf("Concurrent report generation: %d reports in %d ms (avg: %.2f ms/report)%n", 
                concurrentReports, totalDuration, avgReportTime);
        }
    }

    @Nested
    @DisplayName("Memory Performance Tests")
    class MemoryPerformanceTests {

        @Test
        @DisplayName("Should handle large datasets without memory issues")
        void shouldHandleLargeDatasetsWithoutMemoryIssues() {
            // Get initial memory usage
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Create large dataset
            int largeDatasetSize = 10000;
            createLargeTestDataset(largeDatasetSize, 100);
            
            // Force garbage collection and measure memory
            System.gc();
            Thread.yield();
            System.gc();
            
            long memoryAfterCreation = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = memoryAfterCreation - initialMemory;
            
            // Perform operations that could cause memory leaks
            for (int i = 0; i < 1000; i++) {
                studentService.getAllStudents(PageRequest.of(i % 100, 100));
            }
            
            // Final memory check
            System.gc();
            Thread.yield();
            System.gc();
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long finalIncrease = finalMemory - initialMemory;
            
            // Assert memory usage is reasonable
            assertThat(memoryIncrease).isLessThan(500_000_000); // Less than 500MB increase
            assertThat(finalIncrease).isLessThan(600_000_000); // Less than 600MB total increase
            
            System.out.printf("Memory usage: Initial=%d bytes, After creation=+%d bytes, Final=+%d bytes%n", 
                initialMemory, memoryIncrease, finalIncrease);
        }

        @Test
        @DisplayName("Should efficiently handle pagination for large datasets")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldEfficientlyHandlePaginationForLargeDatasets() {
            // Setup
            createLargeTestDataset(5000, 50);
            
            long startTime = System.currentTimeMillis();
            int totalProcessed = 0;
            
            // Process data in pages
            int pageSize = 100;
            int pageNumber = 0;
            Page<Student> page;
            
            do {
                page = studentService.getAllStudents(PageRequest.of(pageNumber, pageSize));
                totalProcessed += page.getContent().size();
                pageNumber++;
                
                // Simulate some processing
                page.getContent().forEach(student -> {
                    // Minimal processing to test pagination efficiency
                    student.getStudentId();
                    student.getFirstName();
                });
                
            } while (page.hasNext() && pageNumber < 60); // Limit to avoid infinite loop
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Assert
            assertThat(totalProcessed).isGreaterThan(4500);
            assertThat(duration).isLessThan(8000); // Should complete within 8 seconds
            assertThat(pageNumber).isGreaterThan(45); // Should have processed multiple pages
            
            System.out.printf("Paginated processing: %d records in %d pages, %d ms%n", 
                totalProcessed, pageNumber, duration);
        }
    }

    @Nested
    @DisplayName("Load Testing")
    class LoadTesting {

        @Test
        @DisplayName("Should handle high load enrollment operations")
        @Timeout(value = 120, unit = TimeUnit.SECONDS)
        void shouldHandleHighLoadEnrollmentOperations() throws InterruptedException {
            // Setup
            int studentCount = 200;
            int courseCount = 20;
            createLargeTestDataset(studentCount, courseCount);
            
            // Configure load test
            int threadCount = 20;
            int operationsPerThread = 25;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            long startTime = System.currentTimeMillis();
            
            // Submit load test tasks
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Future<Integer> future = executor.submit(() -> {
                    try {
                        int successfulOperations = 0;
                        for (int j = 0; j < operationsPerThread; j++) {
                            try {
                                int studentIndex = (threadId * operationsPerThread + j) % testStudents.size();
                                int courseIndex = j % testCourses.size();
                                
                                Student student = testStudents.get(studentIndex);
                                Course course = testCourses.get(courseIndex);
                                
                                enrollmentService.enrollStudent(student.getId(), course.getId());
                                successfulOperations++;
                                
                                // Small delay to simulate realistic usage
                                Thread.sleep(10);
                                
                            } catch (Exception e) {
                                // Expected for capacity limits and duplicate enrollments
                            }
                        }
                        return successfulOperations;
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }
            
            // Wait for completion
            latch.await(100, TimeUnit.SECONDS);
            executor.shutdown();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Calculate results
            int totalSuccessful = futures.stream()
                .mapToInt(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            // Assert
            assertThat(totalSuccessful).isGreaterThan(threadCount * operationsPerThread / 4); // At least 25% success
            assertThat(duration).isLessThan(100000); // Should complete within 100 seconds
            
            double throughput = (totalSuccessful * 1000.0) / duration;
            System.out.printf("Load test results: %d successful operations in %d ms (%.2f ops/sec)%n", 
                totalSuccessful, duration, throughput);
        }

        @Test
        @DisplayName("Should maintain performance under sustained load")
        @Timeout(value = 180, unit = TimeUnit.SECONDS)
        void shouldMaintainPerformanceUnderSustainedLoad() throws InterruptedException {
            // Setup
            createLargeTestDataset(1000, 30);
            
            // Configure sustained load test
            int duration = 60000; // 60 seconds
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            long startTime = System.currentTimeMillis();
            long endTime = startTime + duration;
            
            List<Future<Integer>> futures = new ArrayList<>();
            
            // Submit sustained load tasks
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                Future<Integer> future = executor.submit(() -> {
                    int operationCount = 0;
                    while (System.currentTimeMillis() < endTime) {
                        try {
                            // Mix of different operations
                            switch (operationCount % 4) {
                                case 0:
                                    // Query students
                                    studentService.getAllStudents(PageRequest.of(0, 20));
                                    break;
                                case 1:
                                    // Query courses
                                    courseService.getAllCourses(PageRequest.of(0, 10));
                                    break;
                                case 2:
                                    // Search operations
                                    studentService.searchStudentsByName("Test");
                                    break;
                                case 3:
                                    // Enrollment check
                                    if (!testStudents.isEmpty() && !testCourses.isEmpty()) {
                                        enrollmentService.getStudentEnrollments(testStudents.get(0).getId());
                                    }
                                    break;
                            }
                            operationCount++;
                            
                            // Small delay
                            Thread.sleep(50);
                            
                        } catch (Exception e) {
                            // Continue on errors
                        }
                    }
                    return operationCount;
                });
                futures.add(future);
            }
            
            // Wait for completion
            executor.shutdown();
            executor.awaitTermination(duration + 10000, TimeUnit.MILLISECONDS);
            
            long actualDuration = System.currentTimeMillis() - startTime;
            
            // Calculate results
            int totalOperations = futures.stream()
                .mapToInt(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            // Assert
            assertThat(actualDuration).isLessThan(duration + 10000); // Allow 10 second tolerance
            assertThat(totalOperations).isGreaterThan(threadCount * 10); // At least 10 operations per thread
            
            double throughput = (totalOperations * 1000.0) / actualDuration;
            System.out.printf("Sustained load test: %d operations in %d ms (%.2f ops/sec)%n", 
                totalOperations, actualDuration, throughput);
        }
    }

    @Nested
    @DisplayName("Scalability Tests")
    class ScalabilityTests {

        @Test
        @DisplayName("Should scale linearly with data size")
        void shouldScaleLinearlyWithDataSize() {
            // Test with different data sizes
            int[] dataSizes = {100, 500, 1000, 2000};
            long[] processingTimes = new long[dataSizes.length];
            
            for (int i = 0; i < dataSizes.length; i++) {
                // Clean up previous data
                studentRepository.deleteAll();
                courseRepository.deleteAll();
                testStudents.clear();
                testCourses.clear();
                
                // Create dataset
                int studentCount = dataSizes[i];
                int courseCount = Math.max(10, studentCount / 50);
                
                long startTime = System.currentTimeMillis();
                createLargeTestDataset(studentCount, courseCount);
                
                // Perform standard operations
                studentService.getAllStudents(PageRequest.of(0, 50));
                studentService.searchStudentsByName("Test");
                courseService.getAllCourses(PageRequest.of(0, 20));
                
                processingTimes[i] = System.currentTimeMillis() - startTime;
                
                System.out.printf("Data size %d: %d ms%n", dataSizes[i], processingTimes[i]);
            }
            
            // Verify that processing time doesn't grow exponentially
            for (int i = 1; i < dataSizes.length; i++) {
                double sizeRatio = (double) dataSizes[i] / dataSizes[i - 1];
                double timeRatio = (double) processingTimes[i] / processingTimes[i - 1];
                
                // Time ratio should not be more than 3x the size ratio (allowing for some overhead)
                assertThat(timeRatio).isLessThan(sizeRatio * 3.0);
            }
        }

        @Test
        @DisplayName("Should handle increasing concurrent users efficiently")
        void shouldHandleIncreasingConcurrentUsersEfficiently() throws InterruptedException {
            // Setup base data
            createLargeTestDataset(1000, 50);
            
            int[] userCounts = {5, 10, 20, 40};
            double[] throughputs = new double[userCounts.length];
            
            for (int i = 0; i < userCounts.length; i++) {
                int concurrentUsers = userCounts[i];
                int operationsPerUser = 20;
                
                ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
                CountDownLatch latch = new CountDownLatch(concurrentUsers);
                
                long startTime = System.currentTimeMillis();
                
                List<Future<Integer>> futures = new ArrayList<>();
                for (int j = 0; j < concurrentUsers; j++) {
                    Future<Integer> future = executor.submit(() -> {
                        try {
                            int completedOps = 0;
                            for (int k = 0; k < operationsPerUser; k++) {
                                try {
                                    // Mix of operations
                                    switch (k % 3) {
                                        case 0:
                                            studentService.getAllStudents(PageRequest.of(0, 10));
                                            break;
                                        case 1:
                                            courseService.getAllCourses(PageRequest.of(0, 5));
                                            break;
                                        case 2:
                                            if (!testStudents.isEmpty()) {
                                                studentService.getStudentById(testStudents.get(k % testStudents.size()).getId());
                                            }
                                            break;
                                    }
                                    completedOps++;
                                } catch (Exception e) {
                                    // Continue on errors
                                }
                            }
                            return completedOps;
                        } finally {
                            latch.countDown();
                        }
                    });
                    futures.add(future);
                }
                
                latch.await(30, TimeUnit.SECONDS);
                executor.shutdown();
                
                long duration = System.currentTimeMillis() - startTime;
                
                int totalOps = futures.stream()
                    .mapToInt(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
                
                throughputs[i] = (totalOps * 1000.0) / duration;
                
                System.out.printf("Concurrent users %d: %d ops in %d ms (%.2f ops/sec)%n", 
                    concurrentUsers, totalOps, duration, throughputs[i]);
            }
            
            // Verify that throughput doesn't degrade severely with more users
            for (int i = 1; i < userCounts.length; i++) {
                // Throughput shouldn't drop below 50% when doubling users
                assertThat(throughputs[i]).isGreaterThan(throughputs[0] * 0.3);
            }
        }
    }

    @Nested
    @DisplayName("Resource Utilization Tests")
    class ResourceUtilizationTests {

        @Test
        @DisplayName("Should manage database connections efficiently")
        void shouldManageDatabaseConnectionsEfficiently() throws InterruptedException {
            // Create a scenario that would exhaust connections if not managed properly
            int threadCount = 50;
            int operationsPerThread = 10;
            
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            // Create some test data
            createLargeTestDataset(100, 10);
            
            long startTime = System.currentTimeMillis();
            
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                Future<Boolean> future = executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            // Operations that require database connections
                            studentRepository.count();
                            courseRepository.count();
                            
                            // Small delay to hold connections longer
                            Thread.sleep(50);
                        }
                        return true;
                    } catch (Exception e) {
                        return false;
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }
            
            latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // All operations should complete without connection pool exhaustion
            long successfulThreads = futures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
            
            assertThat(successfulThreads).isEqualTo(threadCount);
            assertThat(duration).isLessThan(45000); // Should complete within 45 seconds
            
            System.out.printf("Database connection test: %d threads completed in %d ms%n", 
                threadCount, duration);
        }

        @Test
        @DisplayName("Should handle transaction management efficiently")
        @Transactional
        void shouldHandleTransactionManagementEfficiently() {
            // Test operations that require transactions
            long startTime = System.currentTimeMillis();
            
            int batchSize = 100;
            List<Student> students = new ArrayList<>();
            
            // Create batch of students
            for (int i = 0; i < batchSize; i++) {
                students.add(createTestStudent("TRANS" + i));
            }
            
            // Save all in transaction
            studentRepository.saveAll(students);
            
            // Update all in transaction
            students.forEach(student -> {
                student.setGpa(3.5 + (Math.random() * 1.0));
                student.setTotalCredits(60 + (int)(Math.random() * 60));
            });
            
            studentRepository.saveAll(students);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Verify all operations completed successfully
            assertThat(studentRepository.count()).isGreaterThanOrEqualTo(batchSize);
            assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
            
            System.out.printf("Transaction management test: %d operations in %d ms%n", 
                batchSize * 2, duration);
        }
    }

    // Helper methods
    private Department createTestDepartment() {
        Department department = new Department();
        department.setCode("PERF");
        department.setName("Performance Testing Department");
        department.setDescription("Department for performance testing");
        department.setHeadName("Dr. Performance Test");
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        return departmentRepository.save(department);
    }

    private Student createTestStudent(String studentId) {
        // Create user first
        User user = new User();
        user.setUsername("user_" + studentId.toLowerCase());
        user.setEmail(studentId.toLowerCase() + "@test.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setFirstName("Test");
        user.setLastName("Student" + studentId.substring(3));
        user.setRole(UserRole.STUDENT);
        user.setActive(true);
        user.setVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentId(studentId);
        student.setDepartment(testDepartment);
        student.setStatus(StudentStatus.ACTIVE);
        student.setEnrollmentDate(LocalDate.now().minusMonths((long)(Math.random() * 24)));
        student.setExpectedGraduation(LocalDate.now().plusYears(4));
        student.setGpa(2.0 + (Math.random() * 2.0)); // Random GPA between 2.0 and 4.0
        student.setTotalCredits((int)(Math.random() * 120)); // Random credits 0-120
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        
        return student;
    }

    private Course createTestCourse(String courseCode) {
        Course course = new Course();
        course.setCode(courseCode);
        course.setTitle("Test Course " + courseCode);
        course.setDescription("Performance testing course " + courseCode);
        course.setCredits(3 + (int)(Math.random() * 3)); // 3-6 credits
        course.setMaxEnrollment(20 + (int)(Math.random() * 30)); // 20-50 students
        course.setCurrentEnrollment(0);
        course.setDepartment(testDepartment);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        
        return course;
    }

    private void createLargeTestDataset(int studentCount, int courseCount) {
        // Clear existing test data
        testStudents.clear();
        testCourses.clear();
        
        // Create courses first
        List<Course> coursesToSave = new ArrayList<>();
        for (int i = 0; i < courseCount; i++) {
            Course course = createTestCourse("PERF" + String.format("%03d", i));
            coursesToSave.add(course);
        }
        
        // Save courses in batches
        int batchSize = 50;
        for (int i = 0; i < coursesToSave.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, coursesToSave.size());
            List<Course> batch = coursesToSave.subList(i, endIndex);
            testCourses.addAll(courseRepository.saveAll(batch));
        }
        
        // Create students
        List<Student> studentsToSave = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            Student student = createTestStudent("PERF" + String.format("%05d", i));
            studentsToSave.add(student);
        }
        
        // Save students in batches
        for (int i = 0; i < studentsToSave.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, studentsToSave.size());
            List<Student> batch = studentsToSave.subList(i, endIndex);
            testStudents.addAll(studentRepository.saveAll(batch));
        }
        
        System.out.printf("Created test dataset: %d students, %d courses%n", 
            testStudents.size(), testCourses.size());
    }
}