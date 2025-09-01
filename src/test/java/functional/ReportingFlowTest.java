// File location: src/test/java/functional/ReportingFlowTest.java

package com.smartcampus.test.functional;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.models.Student;
import com.smartcampus.models.Course;
import com.smartcampus.models.Department;
import com.smartcampus.models.Professor;
import com.smartcampus.models.Enrollment;
import com.smartcampus.models.Grade;
import com.smartcampus.models.Assignment;
import com.smartcampus.models.User;
import com.smartcampus.models.enums.UserRole;
import com.smartcampus.models.enums.StudentStatus;
import com.smartcampus.models.enums.EnrollmentStatus;
import com.smartcampus.models.enums.AssignmentType;
import com.smartcampus.models.enums.ReportFormat;
import com.smartcampus.models.enums.ReportType;
import com.smartcampus.repositories.StudentRepository;
import com.smartcampus.repositories.CourseRepository;
import com.smartcampus.repositories.DepartmentRepository;
import com.smartcampus.repositories.ProfessorRepository;
import com.smartcampus.repositories.EnrollmentRepository;
import com.smartcampus.repositories.GradeRepository;
import com.smartcampus.repositories.AssignmentRepository;
import com.smartcampus.repositories.UserRepository;
import com.smartcampus.services.ReportGenerationService;
import com.smartcampus.services.AnalyticsService;
import com.smartcampus.services.NotificationService;
import com.smartcampus.dto.ReportRequest;
import com.smartcampus.dto.ReportResponse;
import com.smartcampus.exceptions.ReportGenerationException;

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
import java.util.Arrays;
import java.io.ByteArrayOutputStream;

/**
 * Functional tests for the complete reporting workflow
 * Tests end-to-end report generation processes including analytics, formatting, and distribution
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@SpringBootTest(classes = SmartCampusApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Reporting Flow Functional Tests")
class ReportingFlowTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportGenerationService reportGenerationService;

    @Autowired
    private AnalyticsService analyticsService;

    @MockBean
    private NotificationService notificationService;

    private Department testDepartment;
    private Professor testProfessor;
    private Course testCourse1;
    private Course testCourse2;
    private Student testStudent1;
    private Student testStudent2;
    private Student testStudent3;
    private List<Enrollment> testEnrollments;
    private List<Assignment> testAssignments;
    private List<Grade> testGrades;

    @BeforeEach
    void setUp() {
        // Mock notification service
        doNothing().when(notificationService).sendReportGenerationNotification(any(), any());

        // Create comprehensive test data
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
    @DisplayName("Academic Reports Generation")
    @TestMethodOrder(OrderAnnotation.class)
    class AcademicReportsGeneration {

        @Test
        @Order(1)
        @DisplayName("Should generate comprehensive student transcript")
        void shouldGenerateComprehensiveStudentTranscript() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.STUDENT_TRANSCRIPT);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of("studentId", testStudent1.getId()));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReportData()).isNotEmpty();
            assertThat(response.getFileName()).endsWith(".pdf");
            assertThat(response.getMimeType()).isEqualTo("application/pdf");

            // Verify content structure
            assertThat(response.getReportData().length).isGreaterThan(1000); // PDF should have substantial content
            
            // Verify metadata
            assertThat(response.getMetadata()).containsKey("studentName");
            assertThat(response.getMetadata()).containsKey("studentId");
            assertThat(response.getMetadata()).containsKey("gpa");
            assertThat(response.getMetadata()).containsKey("totalCredits");
            assertThat(response.getMetadata()).containsKey("coursesCompleted");
        }

        @Test
        @Order(2)
        @DisplayName("Should generate course enrollment report")
        void shouldGenerateCourseEnrollmentReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.COURSE_ENROLLMENT);
            request.setFormat(ReportFormat.EXCEL);
            request.setParameters(Map.of(
                "courseId", testCourse1.getId(),
                "includeGrades", true,
                "includeAttendance", false
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getFileName()).endsWith(".xlsx");
            assertThat(response.getMimeType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            // Verify Excel content would contain enrollment data
            assertThat(response.getReportData().length).isGreaterThan(5000); // Excel file should have content
            
            // Verify metadata contains summary statistics
            assertThat(response.getMetadata()).containsKey("totalEnrolled");
            assertThat(response.getMetadata()).containsKey("courseName");
            assertThat(response.getMetadata()).containsKey("averageGrade");
        }

        @Test
        @Order(3)
        @DisplayName("Should generate departmental performance report")
        void shouldGenerateDepartmentalPerformanceReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.DEPARTMENT_PERFORMANCE);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of(
                "departmentId", testDepartment.getId(),
                "startDate", LocalDate.now().minusMonths(6),
                "endDate", LocalDate.now(),
                "includeCharts", true
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReportData().length).isGreaterThan(10000); // Report with charts should be larger
            
            // Verify comprehensive metadata
            assertThat(response.getMetadata()).containsKey("departmentName");
            assertThat(response.getMetadata()).containsKey("totalStudents");
            assertThat(response.getMetadata()).containsKey("totalCourses");
            assertThat(response.getMetadata()).containsKey("averageDepartmentGPA");
            assertThat(response.getMetadata()).containsKey("enrollmentTrends");
        }

        @Test
        @Order(4)
        @DisplayName("Should generate grade distribution analysis")
        void shouldGenerateGradeDistributionAnalysis() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.GRADE_DISTRIBUTION);
            request.setFormat(ReportFormat.HTML);
            request.setParameters(Map.of(
                "courseId", testCourse1.getId(),
                "semester", "Fall 2024",
                "includeHistograms", true,
                "compareWithPrevious", false
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getFileName()).endsWith(".html");
            assertThat(response.getMimeType()).isEqualTo("text/html");

            // Verify HTML content
            String htmlContent = new String(response.getReportData());
            assertThat(htmlContent).contains("<html>");
            assertThat(htmlContent).contains("Grade Distribution");
            assertThat(htmlContent).contains(testCourse1.getTitle());
            assertThat(htmlContent).contains("chart"); // Should contain chart elements
        }
    }

    @Nested
    @DisplayName("Administrative Reports")
    class AdministrativeReports {

        @Test
        @DisplayName("Should generate enrollment statistics report")
        void shouldGenerateEnrollmentStatisticsReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.ENROLLMENT_STATISTICS);
            request.setFormat(ReportFormat.CSV);
            request.setParameters(Map.of(
                "academicYear", "2024-2025",
                "includeTrends", true,
                "groupByDepartment", true
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getFileName()).endsWith(".csv");
            assertThat(response.getMimeType()).isEqualTo("text/csv");

            // Verify CSV content
            String csvContent = new String(response.getReportData());
            assertThat(csvContent).contains("Department,Course,Enrolled,Capacity,Utilization");
            assertThat(csvContent).contains(testDepartment.getCode());
            assertThat(csvContent).contains(testCourse1.getCode());
        }

        @Test
        @DisplayName("Should generate financial aid report")
        void shouldGenerateFinancialAidReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.FINANCIAL_AID);
            request.setFormat(ReportFormat.EXCEL);
            request.setParameters(Map.of(
                "semester", "Fall 2024",
                "aidTypes", Arrays.asList("SCHOLARSHIP", "LOAN", "GRANT"),
                "includeDemographics", true
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReportData().length).isGreaterThan(3000);
            
            // Verify metadata contains financial summaries
            assertThat(response.getMetadata()).containsKey("totalAidDistributed");
            assertThat(response.getMetadata()).containsKey("studentsAided");
            assertThat(response.getMetadata()).containsKey("aidBreakdown");
        }

        @Test
        @DisplayName("Should generate compliance audit report")
        void shouldGenerateComplianceAuditReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.COMPLIANCE_AUDIT);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of(
                "auditDate", LocalDate.now(),
                "includeRecommendations", true,
                "complianceStandards", Arrays.asList("FERPA", "ADA", "TITLE_IX")
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReportData().length).isGreaterThan(5000);
            
            // Verify audit-specific metadata
            assertThat(response.getMetadata()).containsKey("complianceScore");
            assertThat(response.getMetadata()).containsKey("violationsFound");
            assertThat(response.getMetadata()).containsKey("recommendationsCount");
        }
    }

    @Nested
    @DisplayName("Analytics and Business Intelligence")
    class AnalyticsAndBusinessIntelligence {

        @Test
        @DisplayName("Should generate predictive analytics report")
        void shouldGeneratePredictiveAnalyticsReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.PREDICTIVE_ANALYTICS);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of(
                "predictionType", "GRADUATION_RISK",
                "studentsScope", "ALL",
                "includeMachineLearningModels", true,
                "forecastPeriod", "2_YEARS"
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReportData().length).isGreaterThan(8000); // ML reports are typically larger
            
            // Verify predictive metadata
            assertThat(response.getMetadata()).containsKey("studentsAtRisk");
            assertThat(response.getMetadata()).containsKey("modelAccuracy");
            assertThat(response.getMetadata()).containsKey("riskFactors");
            assertThat(response.getMetadata()).containsKey("interventionRecommendations");
        }

        @Test
        @DisplayName("Should generate performance dashboard data")
        void shouldGeneratePerformanceDashboardData() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.DASHBOARD_DATA);
            request.setFormat(ReportFormat.JSON);
            request.setParameters(Map.of(
                "dashboardType", "EXECUTIVE_SUMMARY",
                "refreshInterval", "REAL_TIME",
                "includeKPIs", true
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMimeType()).isEqualTo("application/json");

            // Verify JSON structure
            String jsonContent = new String(response.getReportData());
            assertThat(jsonContent).contains("\"totalStudents\":");
            assertThat(jsonContent).contains("\"averageGPA\":");
            assertThat(jsonContent).contains("\"enrollmentTrends\":");
            assertThat(jsonContent).contains("\"departmentPerformance\":");
        }

        @Test
        @DisplayName("Should generate trend analysis report")
        void shouldGenerateTrendAnalysisReport() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.TREND_ANALYSIS);
            request.setFormat(ReportFormat.HTML);
            request.setParameters(Map.of(
                "analysisType", "MULTI_YEAR",
                "startYear", 2020,
                "endYear", 2024,
                "includeProjections", true,
                "trendMetrics", Arrays.asList("ENROLLMENT", "GRADUATION_RATE", "GPA", "RETENTION")
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            
            String htmlContent = new String(response.getReportData());
            assertThat(htmlContent).contains("Trend Analysis");
            assertThat(htmlContent).contains("2020");
            assertThat(htmlContent).contains("2024");
            assertThat(htmlContent).contains("chart"); // Should contain trend charts
        }
    }

    @Nested
    @DisplayName("Custom and Scheduled Reports")
    class CustomAndScheduledReports {

        @Test
        @DisplayName("Should create and execute custom report template")
        void shouldCreateAndExecuteCustomReportTemplate() {
            // Arrange - Create custom report template
            Map<String, Object> templateConfig = new HashMap<>();
            templateConfig.put("name", "Custom Academic Standing Report");
            templateConfig.put("description", "Custom report for academic standing by GPA ranges");
            templateConfig.put("query", "SELECT s.student_id, s.gpa, s.status FROM students s WHERE s.gpa BETWEEN ? AND ?");
            templateConfig.put("parameters", Arrays.asList("minGpa", "maxGpa"));
            templateConfig.put("charts", Arrays.asList(
                Map.of("type", "pie", "field", "status", "title", "Students by Academic Status"),
                Map.of("type", "histogram", "field", "gpa", "title", "GPA Distribution")
            ));

            Long templateId = reportGenerationService.createCustomReportTemplate(templateConfig);

            // Execute custom report
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.CUSTOM);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of(
                "templateId", templateId,
                "minGpa", 2.0,
                "maxGpa", 4.0
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getReportData().length).isGreaterThan(3000);
            assertThat(response.getMetadata()).containsKey("customTemplateName");
        }

        @Test
        @DisplayName("Should schedule recurring report generation")
        void shouldScheduleRecurringReportGeneration() {
            // Arrange
            Map<String, Object> scheduleConfig = Map.of(
                "reportType", ReportType.ENROLLMENT_STATISTICS,
                "format", ReportFormat.EXCEL,
                "frequency", "WEEKLY",
                "dayOfWeek", "MONDAY",
                "time", "08:00",
                "recipients", Arrays.asList("admin@smartcampus.edu", "registrar@smartcampus.edu"),
                "parameters", Map.of("includeWaitlists", true, "groupByDepartment", true)
            );

            // Act
            String scheduleId = reportGenerationService.scheduleRecurringReport(scheduleConfig);

            // Assert
            assertThat(scheduleId).isNotNull();
            assertThat(scheduleId).isNotEmpty();

            // Verify schedule was created
            Map<String, Object> scheduleDetails = reportGenerationService.getScheduleDetails(scheduleId);
            assertThat(scheduleDetails).containsKey("frequency");
            assertThat(scheduleDetails).containsKey("nextRunDate");
            assertThat(scheduleDetails).containsKey("isActive");
            assertThat((Boolean) scheduleDetails.get("isActive")).isTrue();
        }

        @Test
        @DisplayName("Should handle report generation with filters and parameters")
        void shouldHandleReportGenerationWithFiltersAndParameters() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.STUDENT_LIST);
            request.setFormat(ReportFormat.CSV);
            request.setFilters(Map.of(
                "department", testDepartment.getId(),
                "status", Arrays.asList("ACTIVE", "PROBATION"),
                "minGpa", 2.0,
                "maxGpa", 4.0,
                "enrollmentYear", 2024
            ));
            request.setParameters(Map.of(
                "includeContactInfo", true,
                "includeEmergencyContacts", false,
                "sortBy", "lastName",
                "sortOrder", "ASC"
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            
            String csvContent = new String(response.getReportData());
            assertThat(csvContent).contains("Student ID");
            assertThat(csvContent).contains("Last Name");
            assertThat(csvContent).contains("GPA");
            assertThat(csvContent).contains("Status");
        }
    }

    @Nested
    @DisplayName("Report Export and Distribution")
    class ReportExportAndDistribution {

        @Test
        @DisplayName("Should export report to multiple formats")
        void shouldExportReportToMultipleFormats() {
            // Arrange
            Long reportId = reportGenerationService.generateAndSaveReport(ReportType.COURSE_ROSTER, 
                Map.of("courseId", testCourse1.getId()));

            // Act - Export to different formats
            byte[] pdfData = reportGenerationService.exportReport(reportId, ReportFormat.PDF);
            byte[] excelData = reportGenerationService.exportReport(reportId, ReportFormat.EXCEL);
            byte[] csvData = reportGenerationService.exportReport(reportId, ReportFormat.CSV);

            // Assert
            assertThat(pdfData).isNotEmpty();
            assertThat(excelData).isNotEmpty();
            assertThat(csvData).isNotEmpty();
            
            // Verify different formats have different content structures
            assertThat(pdfData.length).isNotEqualTo(excelData.length);
            assertThat(new String(csvData)).contains(","); // CSV should contain commas
        }

        @Test
        @DisplayName("Should distribute report via email")
        void shouldDistributeReportViaEmail() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.GRADE_REPORT);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of("courseId", testCourse1.getId()));
            request.setDistributionList(Arrays.asList(
                "professor@smartcampus.edu",
                "dean@smartcampus.edu",
                "registrar@smartcampus.edu"
            ));

            // Act
            ReportResponse response = reportGenerationService.generateAndDistributeReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getDistributionStatus()).isEqualTo("SENT");
            
            // Verify notifications were sent
            verify(notificationService, times(3)).sendReportDistributionNotification(any(), any());
        }

        @Test
        @DisplayName("Should handle secure report sharing")
        void shouldHandleSecureReportSharing() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.STUDENT_TRANSCRIPT);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of(
                "studentId", testStudent1.getId(),
                "includeSSN", false,
                "watermark", "CONFIDENTIAL"
            ));
            request.setSecurityLevel("RESTRICTED");
            request.setAccessPermissions(Arrays.asList("VIEW_ONLY", "PRINT_DISABLED"));

            // Act
            ReportResponse response = reportGenerationService.generateSecureReport(request);

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getSecurityLevel()).isEqualTo("RESTRICTED");
            assertThat(response.getAccessToken()).isNotEmpty();
            assertThat(response.getExpirationDate()).isAfter(LocalDateTime.now());
            
            // Verify watermark and security features
            assertThat(response.getMetadata()).containsKey("watermarked");
            assertThat(response.getMetadata()).containsKey("printDisabled");
        }
    }

    @Nested
    @DisplayName("Report Performance and Optimization")
    class ReportPerformanceAndOptimization {

        @Test
        @DisplayName("Should handle large dataset reports efficiently")
        void shouldHandleLargeDatasetReportsEfficiently() {
            // Arrange - Create large dataset (simulate with parameters)
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.COMPREHENSIVE_ANALYTICS);
            request.setFormat(ReportFormat.EXCEL);
            request.setParameters(Map.of(
                "includeAllYears", true,
                "includeAllDepartments", true,
                "includeDetailedBreakdowns", true,
                "estimatedRecords", 50000
            ));

            long startTime = System.currentTimeMillis();

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            long duration = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(response.isSuccess()).isTrue();
            assertThat(duration).isLessThan(30000); // Should complete within 30 seconds
            assertThat(response.getProcessingTime()).isLessThan(30000);
            
            // Verify optimization techniques were used
            assertThat(response.getMetadata()).containsKey("optimizationUsed");
            assertThat(response.getMetadata()).containsKey("dataStreaming");
        }

        @Test
        @DisplayName("Should implement report caching for frequently requested reports")
        void shouldImplementReportCachingForFrequentlyRequestedReports() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.ENROLLMENT_SUMMARY);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of("semester", "Fall 2024"));

            // Act - Generate report first time
            long startTime1 = System.currentTimeMillis();
            ReportResponse response1 = reportGenerationService.generateReport(request);
            long duration1 = System.currentTimeMillis() - startTime1;

            // Act - Generate same report second time (should be cached)
            long startTime2 = System.currentTimeMillis();
            ReportResponse response2 = reportGenerationService.generateReport(request);
            long duration2 = System.currentTimeMillis() - startTime2;

            // Assert
            assertThat(response1.isSuccess()).isTrue();
            assertThat(response2.isSuccess()).isTrue();
            assertThat(duration2).isLessThan(duration1 / 2); // Cached version should be much faster
            
            // Verify cache was used
            assertThat(response2.getMetadata()).containsKey("fromCache");
            assertThat((Boolean) response2.getMetadata().get("fromCache")).isTrue();
        }

        @Test
        @DisplayName("Should handle concurrent report generation requests")
        void shouldHandleConcurrentReportGenerationRequests() {
            // This would typically involve threading, simplified for this test
            
            // Arrange
            ReportRequest request1 = new ReportRequest();
            request1.setReportType(ReportType.STUDENT_LIST);
            request1.setFormat(ReportFormat.CSV);
            request1.setParameters(Map.of("department", testDepartment.getId()));

            ReportRequest request2 = new ReportRequest();
            request2.setReportType(ReportType.COURSE_ENROLLMENT);
            request2.setFormat(ReportFormat.EXCEL);
            request2.setParameters(Map.of("semester", "Fall 2024"));

            // Act - Submit concurrent requests
            long startTime = System.currentTimeMillis();
            ReportResponse response1 = reportGenerationService.generateReport(request1);
            ReportResponse response2 = reportGenerationService.generateReport(request2);
            long totalDuration = System.currentTimeMillis() - startTime;

            // Assert
            assertThat(response1.isSuccess()).isTrue();
            assertThat(response2.isSuccess()).isTrue();
            assertThat(totalDuration).isLessThan(15000); // Both should complete reasonably quickly
        }
    }

    @Nested
    @DisplayName("Report Error Handling and Validation")
    class ReportErrorHandlingAndValidation {

        @Test
        @DisplayName("Should handle invalid report parameters gracefully")
        void shouldHandleInvalidReportParametersGracefully() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.STUDENT_TRANSCRIPT);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of("studentId", -1L)); // Invalid student ID

            // Act & Assert
            ReportResponse response = reportGenerationService.generateReport(request);
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorMessage()).contains("Invalid student ID");
            assertThat(response.getErrorCode()).isEqualTo("INVALID_PARAMETER");
        }

        @Test
        @DisplayName("Should validate report permissions and access control")
        void shouldValidateReportPermissionsAndAccessControl() {
            // Arrange
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.FINANCIAL_AID);
            request.setFormat(ReportFormat.EXCEL);
            request.setRequestedBy("student@smartcampus.edu"); // Student shouldn't access financial reports
            request.setParameters(Map.of("semester", "Fall 2024"));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorMessage()).contains("Insufficient permissions");
            assertThat(response.getErrorCode()).isEqualTo("ACCESS_DENIED");
        }

        @Test
        @DisplayName("Should handle system errors during report generation")
        void shouldHandleSystemErrorsDuringReportGeneration() {
            // Arrange - Create request that might fail due to system issues
            ReportRequest request = new ReportRequest();
            request.setReportType(ReportType.CUSTOM);
            request.setFormat(ReportFormat.PDF);
            request.setParameters(Map.of(
                "templateId", -999L, // Non-existent template
                "timeout", 1 // Very short timeout to simulate failure
            ));

            // Act
            ReportResponse response = reportGenerationService.generateReport(request);

            // Assert
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isIn("TEMPLATE_NOT_FOUND", "GENERATION_TIMEOUT", "SYSTEM_ERROR");
            assertThat(response.getErrorMessage()).isNotEmpty();
            assertThat(response.getReportData()).isNull();
        }
    }

    // Helper methods
    private void setupTestData() {
        // Create department
        testDepartment = createTestDepartment();

        // Create professor
        testProfessor = createTestProfessor();

        // Create courses
        testCourse1 = createTestCourse("CS101", "Introduction to Programming");
        testCourse2 = createTestCourse("CS201", "Data Structures");

        // Create students
        testStudent1 = createTestStudent("RPT001");
        testStudent2 = createTestStudent("RPT002");
        testStudent3 = createTestStudent("RPT003");

        // Create enrollments
        testEnrollments = Arrays.asList(
            createTestEnrollment(testStudent1, testCourse1),
            createTestEnrollment(testStudent1, testCourse2),
            createTestEnrollment(testStudent2, testCourse1),
            createTestEnrollment(testStudent3, testCourse2)
        );

        // Create assignments
        testAssignments = Arrays.asList(
            createTestAssignment(testCourse1, "Midterm Exam", AssignmentType.EXAM),
            createTestAssignment(testCourse1, "Final Project", AssignmentType.PROJECT),
            createTestAssignment(testCourse2, "Algorithm Quiz", AssignmentType.QUIZ),
            createTestAssignment(testCourse2, "Data Structure Assignment", AssignmentType.HOMEWORK)
        );

        // Create grades
        testGrades = Arrays.asList(
            createTestGrade(testStudent1, testAssignments.get(0), 85.0, "B+"),
            createTestGrade(testStudent1, testAssignments.get(1), 92.0, "A-"),
            createTestGrade(testStudent2, testAssignments.get(0), 78.0, "C+"),
            createTestGrade(testStudent3, testAssignments.get(2), 95.0, "A")
        );
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
        User professorUser = new User();
        professorUser.setUsername("prof.reports");
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
        professor.setTitle("Associate Professor");
        professor.setCreatedAt(LocalDateTime.now());
        return professorRepository.save(professor);
    }

    private Course createTestCourse(String courseCode, String title) {
        Course course = new Course();
        course.setCode(courseCode);
        course.setTitle(title);
        course.setDescription("Test course for reporting");
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
        User studentUser = new User();
        studentUser.setUsername(studentId.toLowerCase());
        studentUser.setEmail(studentId.toLowerCase() + "@test.com");
        studentUser.setPasswordHash("$2a$10$test");
        studentUser.setFirstName("Test");
        studentUser.setLastName("Student" + studentId.substring(3));
        studentUser.setRole(UserRole.STUDENT);
        studentUser.setActive(true);
        studentUser.setVerified(true);
        studentUser.setCreatedAt(LocalDateTime.now());
        studentUser = userRepository.save(studentUser);

        Student student = new Student();
        student.setUser(studentUser);
        student.setStudentId(studentId);
        student.setDepartment(testDepartment);
        student.setStatus(StudentStatus.ACTIVE);
        student.setEnrollmentDate(LocalDate.now().minusMonths(6));
        student.setExpectedGraduation(LocalDate.now().plusYears(4));
        student.setGpa(3.0 + (Math.random() * 1.0)); // Random GPA between 3.0 and 4.0
        student.setTotalCredits(30 + (int)(Math.random() * 90)); // Random credits 30-120
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

    private Assignment createTestAssignment(Course course, String title, AssignmentType type) {
        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription("Test assignment: " + title);
        assignment.setType(type);
        assignment.setCourse(course);
        assignment.setCreatedBy(testProfessor);
        assignment.setMaxPoints(100.0);
        assignment.setDueDate(LocalDateTime.now().plusDays(7));
        assignment.setWeight(0.25);
        assignment.setCreatedAt(LocalDateTime.now());
        return assignmentRepository.save(assignment);
    }

    private Grade createTestGrade(Student student, Assignment assignment, Double points, String letterGrade) {
        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setAssignment(assignment);
        grade.setPointsEarned(points);
        grade.setPercentage(points); // Assuming max points is 100
        grade.setLetterGrade(letterGrade);
        grade.setComments("Test grade for reporting");
        grade.setGradedBy(testProfessor);
        grade.setGradedAt(LocalDateTime.now());
        grade.setCreatedAt(LocalDateTime.now());
        return gradeRepository.save(grade);
    }
}