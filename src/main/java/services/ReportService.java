// File: src/main/java/services/ReportService.java
package services;

import models.*;
import interfaces.Reportable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * ReportService class providing comprehensive reporting functionality.
 * This service demonstrates extensive use of method references and functional programming.
 * 
 * Key Java concepts demonstrated:
 * - Method references (static, instance, constructor, arbitrary object)
 * - Functional interfaces and composition
 * - Stream API with method reference chains
 * - CompletableFuture for async report generation
 * - Function composition and chaining
 * - Custom collectors with method references
 * - Advanced functional programming patterns
 */
public class ReportService implements Reportable {
    
    // Service dependencies
    private final StudentService studentService;
    private final ProfessorService professorService;
    private final CourseService courseService;
    private final DepartmentService departmentService;
    private final EnrollmentService enrollmentService;
    private final GradeService gradeService;
    
    // Report storage and caching
    private final Map<String, ReportData> reportCache;
    private final Map<String, ReportMetadata> reportHistory;
    private final Map<String, ScheduledReport> scheduledReports;
    
    // Method reference examples for different types
    
    // Static method references
    private final Function<String, String> upperCaseConverter = String::toUpperCase;
    private final Function<String, String> trimConverter = String::trim;
    private final Function<Double, String> percentageFormatter = ReportService::formatPercentage;
    private final Function<LocalDateTime, String> dateFormatter = ReportService::formatDateTime;
    
    // Instance method references
    private final Predicate<String> validationCheck = ValidationUtil::isValidString;
    private final Function<Object, String> stringConverter = Object::toString;
    private final Supplier<LocalDateTime> currentTimeSupplier = LocalDateTime::now;
    private final Supplier<String> reportIdGenerator = this::generateReportId;
    
    // Constructor method references
    private final Supplier<List<String>> listSupplier = ArrayList::new;
    private final Supplier<Map<String, Object>> mapSupplier = HashMap::new;
    private final Function<String, Optional<String>> optionalWrapper = Optional::ofNullable;
    
    // Arbitrary object method references (will be demonstrated in collections)
    private final Comparator<String> stringComparator = String::compareTo;
    private final Comparator<ReportMetadata> reportDateComparator = 
        Comparator.comparing(ReportMetadata::getGeneratedAt);
    
    // Functional composition examples
    private final Function<String, String> cleanAndUppercase = 
        trimConverter.andThen(upperCaseConverter);
    
    private final Function<Double, String> formatAndValidate = 
        percentageFormatter.andThen(this::validateFormattedString);
    
    /**
     * Constructor with service dependencies.
     */
    public ReportService(StudentService studentService, ProfessorService professorService,
                        CourseService courseService, DepartmentService departmentService,
                        EnrollmentService enrollmentService, GradeService gradeService) {
        this.studentService = studentService;
        this.professorService = professorService;
        this.courseService = courseService;
        this.departmentService = departmentService;
        this.enrollmentService = enrollmentService;
        this.gradeService = gradeService;
        
        this.reportCache = new ConcurrentHashMap<>();
        this.reportHistory = new ConcurrentHashMap<>();
        this.scheduledReports = new ConcurrentHashMap<>();
    }
    
    // Core reporting methods using method references
    
    @Override
    public ReportData generateReport(ReportType reportType) {
        return generateReport(reportType, createDefaultParameters.get());
    }
    
    @Override
    public ReportData generateReport(ReportType reportType, Map<String, Object> parameters) {
        String reportId = reportIdGenerator.get();
        String title = formatReportTitle(reportType, currentTimeSupplier.get());
        
        // Using method references to select appropriate generator
        Function<Map<String, Object>, ReportData> generator = getReportGenerator(reportType, reportId, title);
        
        ReportData report = generator.apply(parameters);
        
        // Cache report using method reference
        Optional.ofNullable(report)
               .ifPresent(this::cacheReport);
        
        // Record in history using method reference
        Optional.ofNullable(report)
               .map(this::createReportMetadata)
               .ifPresent(this::recordReportHistory);
        
        return report;
    }
    
    @Override
    public ReportData generateReportForDateRange(ReportType reportType, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> parameters = Map.of(
            "startDate", startDate,
            "endDate", endDate,
            "dateRange", true
        );
        return generateReport(reportType, parameters);
    }
    
    // Async report generation using method references and CompletableFuture
    
    /**
     * Generate report asynchronously using method references.
     * 
     * @param reportType The report type
     * @param parameters Report parameters
     * @return CompletableFuture containing the report
     */
    public CompletableFuture<ReportData> generateReportAsync(ReportType reportType, Map<String, Object> parameters) {
        return CompletableFuture
                .supplyAsync(() -> generateReport(reportType, parameters))
                .thenApply(this::enhanceReport)
                .thenApply(this::validateReport)
                .exceptionally(this::handleReportError);
    }
    
    /**
     * Generate multiple reports in parallel using method references.
     * 
     * @param reportTypes List of report types to generate
     * @return CompletableFuture containing all reports
     */
    public CompletableFuture<List<ReportData>> generateMultipleReportsAsync(List<ReportType> reportTypes) {
        List<CompletableFuture<ReportData>> reportFutures = reportTypes.stream()
                .map(type -> generateReportAsync(type, createDefaultParameters.get()))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(reportFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> reportFutures.stream()
                           .map(CompletableFuture::join)
                           .collect(Collectors.toList()))
                .thenApply(this::sortReportsByType);
    }
    
    // Report generation methods using extensive method references
    
    /**
     * Generate enrollment report using method references extensively.
     */
    private ReportData generateEnrollmentReport(String reportId, String title, Map<String, Object> parameters) {
        List<String> columns = List.of("Student ID", "Student Name", "Course", "Status", "Date", "Type");
        
        List<Map<String, Object>> rows = enrollmentService.getAllStudentEnrollments()
                .stream()
                .filter(Objects::nonNull)
                .filter(this::isEnrollmentInDateRange)
                .map(this::convertEnrollmentToRow)
                .sorted(this::compareEnrollmentRows)
                .collect(Collectors.toList());
        
        Map<String, Object> metadata = createEnrollmentMetadata();
        
        return new ReportData(reportId, ReportType.ENROLLMENT_REPORT, title, columns, rows, metadata);
    }
    
    /**
     * Generate grade report using method references.
     */
    private ReportData generateGradeReport(String reportId, String title, Map<String, Object> parameters) {
        List<String> columns = List.of("Student", "Course", "Assignment", "Grade", "Percentage", "Status");
        
        List<Map<String, Object>> rows = gradeService.getAllGrades()
                .stream()
                .filter(Objects::nonNull)
                .filter(Grade::countsTowardFinalGrade)
                .map(this::convertGradeToRow)
                .sorted(this::compareGradeRows)
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.GRADE_REPORT, title, columns, rows, 
                            gradeService.getSummaryStatistics());
    }
    
    /**
     * Generate performance report using method references and stream operations.
     */
    private ReportData generatePerformanceReport(String reportId, String title, Map<String, Object> parameters) {
        List<String> columns = List.of("Entity", "Metric", "Value", "Trend", "Benchmark");
        
        // Student performance using method references
        List<Map<String, Object>> studentRows = studentService.getAllStudents()
                .stream()
                .filter(Student::isActive)
                .map(this::calculateStudentPerformance)
                .map(this::convertPerformanceToRow)
                .collect(Collectors.toList());
        
        // Professor performance using method references
        List<Map<String, Object>> professorRows = professorService.getAllProfessors()
                .stream()
                .filter(Professor::isActive)
                .map(this::calculateProfessorPerformance)
                .map(this::convertPerformanceToRow)
                .collect(Collectors.toList());
        
        // Course performance using method references
        List<Map<String, Object>> courseRows = courseService.getAllCourses()
                .stream()
                .filter(course -> course.getStatus() == Course.CourseStatus.ACTIVE)
                .map(this::calculateCoursePerformance)
                .map(this::convertPerformanceToRow)
                .collect(Collectors.toList());
        
        // Combine all rows using method references
        List<Map<String, Object>> allRows = Stream.of(studentRows, professorRows, courseRows)
                .flatMap(List::stream)
                .sorted(this::comparePerformanceRows)
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.PERFORMANCE_REPORT, title, columns, allRows,
                            calculatePerformanceMetadata());
    }
    
    /**
     * Generate demographic report using method references.
     */
    private ReportData generateDemographicReport(String reportId, String title, Map<String, Object> parameters) {
        List<String> columns = List.of("Category", "Subcategory", "Count", "Percentage", "Trend");
        
        // Student demographics using method references
        Map<String, Long> studentByMajor = studentService.getAllStudents()
                .stream()
                .collect(Collectors.groupingBy(Student::getMajor, Collectors.counting()));
        
        Map<Student.AcademicYear, Long> studentByYear = studentService.getAllStudents()
                .stream()
                .collect(Collectors.groupingBy(Student::getAcademicYear, Collectors.counting()));
        
        // Professor demographics using method references
        Map<Professor.AcademicRank, Long> professorByRank = professorService.getAllProfessors()
                .stream()
                .collect(Collectors.groupingBy(Professor::getAcademicRank, Collectors.counting()));
        
        // Convert to rows using method references
        List<Map<String, Object>> rows = Stream.of(
                convertMapToRows("Student Major", studentByMajor, Object::toString),
                convertMapToRows("Student Year", studentByYear, Object::toString),
                convertMapToRows("Professor Rank", professorByRank, Object::toString)
        )
                .flatMap(List::stream)
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.DEMOGRAPHIC_REPORT, title, columns, rows,
                            calculateDemographicMetadata());
    }
    
    /**
     * Generate financial report using method references.
     */
    private ReportData generateFinancialReport(String reportId, String title, Map<String, Object> parameters) {
        List<String> columns = List.of("Department", "Budget", "Spent", "Remaining", "Utilization %");
        
        List<Map<String, Object>> rows = departmentService.getAllDepartments()
                .stream()
                .filter(Department::isActive)
                .map(this::calculateDepartmentFinancials)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(this::compareFinancialRows)
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.FINANCIAL_REPORT, title, columns, rows,
                            calculateFinancialMetadata());
    }
    
    // Advanced method reference usage patterns
    
    /**
     * Process report data using function composition and method references.
     */
    private ReportData processReportData(ReportData rawReport) {
        return Optional.ofNullable(rawReport)
                .map(this::validateReportStructure)
                .map(this::enhanceWithCalculations)
                .map(this::applyFormatting)
                .map(this::addSummaryStatistics)
                .orElseThrow(() -> new RuntimeException("Failed to process report data"));
    }
    
    /**
     * Apply transformations using method reference chains.
     */
    private List<Map<String, Object>> transformReportRows(List<Map<String, Object>> rows) {
        return rows.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeRowData)
                .map(this::applyRowFormatting)
                .map(this::addRowCalculations)
                .filter(this::isValidRow)
                .collect(Collectors.toList());
    }
    
    /**
     * Aggregate data using collectors and method references.
     */
    private Map<String, Object> aggregateReportData(List<Map<String, Object>> rows) {
        // Using method references with custom collectors
        Map<String, Object> aggregates = mapSupplier.get();
        
        // Count aggregation using method reference
        long totalRows = rows.stream()
                .filter(Objects::nonNull)
                .count();
        
        // Sum aggregation using method references
        double totalValues = rows.stream()
                .filter(Objects::nonNull)
                .map(row -> row.get("value"))
                .filter(Objects::nonNull)
                .mapToDouble(this::parseNumericValue)
                .sum();
        
        // Group and count using method references
        Map<String, Long> categoryCounts = rows.stream()
                .filter(Objects::nonNull)
                .map(row -> row.get("category"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        
        aggregates.put("totalRows", totalRows);
        aggregates.put("totalValues", totalValues);
        aggregates.put("categoryCounts", categoryCounts);
        aggregates.put("averageValue", totalRows > 0 ? totalValues / totalRows : 0);
        
        return aggregates;
    }
    
    // Method reference utility functions
    
    /**
     * Static method reference example for percentage formatting.
     */
    private static String formatPercentage(Double value) {
        return value != null ? String.format("%.1f%%", value) : "N/A";
    }
    
    /**
     * Static method reference example for date formatting.
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A";
    }
    
    /**
     * Method reference supplier for default parameters.
     */
    private final Supplier<Map<String, Object>> createDefaultParameters = () -> {
        Map<String, Object> params = mapSupplier.get();
        params.put("includeInactive", false);
        params.put("includeArchived", false);
        params.put("dateRange", false);
        return params;
    };
    
    // Helper methods for method reference examples
    
    private Function<Map<String, Object>, ReportData> getReportGenerator(ReportType reportType, String reportId, String title) {
        return switch (reportType) {
            case ENROLLMENT_REPORT -> params -> generateEnrollmentReport(reportId, title, params);
            case GRADE_REPORT -> params -> generateGradeReport(reportId, title, params);
            case PERFORMANCE_REPORT -> params -> generatePerformanceReport(reportId, title, params);
            case DEMOGRAPHIC_REPORT -> params -> generateDemographicReport(reportId, title, params);
            case FINANCIAL_REPORT -> params -> generateFinancialReport(reportId, title, params);
            default -> params -> generateStatisticalSummary(reportId, title, params);
        };
    }
    
    private ReportData generateStatisticalSummary(String reportId, String title, Map<String, Object> parameters) {
        Map<String, Object> allStats = mapSupplier.get();
        
        // Gather statistics using method references
        allStats.putAll(studentService.getSummaryStatistics());
        allStats.putAll(professorService.getSummaryStatistics());
        allStats.putAll(courseService.getSummaryStatistics());
        allStats.putAll(enrollmentService.getSummaryStatistics());
        allStats.putAll(gradeService.getSummaryStatistics());
        
        String content = allStats.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, title, content);
    }
    
    // Method reference examples for data conversion and processing
    
    private Map<String, Object> convertEnrollmentToRow(Enrollment enrollment) {
        Map<String, Object> row = mapSupplier.get();
        row.put("Student ID", enrollment.getStudentId());
        row.put("Student Name", getStudentName(enrollment.getStudentId()));
        row.put("Course", getCourseName(enrollment.getCourseId()));
        row.put("Status", enrollment.getStatus().getDisplayName());
        row.put("Date", dateFormatter.apply(enrollment.getEnrollmentDate()));
        row.put("Type", enrollment.getEnrollmentType().getDisplayName());
        return row;
    }
    
    private Map<String, Object> convertGradeToRow(Grade grade) {
        Map<String, Object> row = mapSupplier.get();
        row.put("Student", getStudentName(grade.getStudentId()));
        row.put("Course", getCourseName(grade.getCourseId()));
        row.put("Assignment", grade.getAssignmentName());
        row.put("Grade", grade.getLetterGrade());
        row.put("Percentage", percentageFormatter.apply(grade.getPercentage()));
        row.put("Status", grade.getStatus().getDisplayName());
        return row;
    }
    
    private PerformanceMetric calculateStudentPerformance(Student student) {
        double gpa = studentService.calculateGPA(student.getStudentId());
        return new PerformanceMetric("Student", student.getFullName(), "GPA", gpa, "4.0");
    }
    
    private PerformanceMetric calculateProfessorPerformance(Professor professor) {
        double rating = professor.getTeachingRating();
        return new PerformanceMetric("Professor", professor.getFullName(), "Teaching Rating", rating, "5.0");
    }
    
    private PerformanceMetric calculateCoursePerformance(Course course) {
        int enrolled = courseService.getCurrentEnrollmentCount(course.getCourseId());
        int capacity = course.getMaxEnrollment();
        double rate = capacity > 0 ? (enrolled * 100.0) / capacity : 0.0;
        return new PerformanceMetric("Course", course.getCourseName(), "Enrollment Rate", rate, "100%");
    }
    
    private Map<String, Object> convertPerformanceToRow(PerformanceMetric metric) {
        Map<String, Object> row = mapSupplier.get();
        row.put("Entity", metric.entityType() + ": " + metric.entityName());
        row.put("Metric", metric.metricName());
        row.put("Value", formatMetricValue(metric.value()));
        row.put("Trend", calculateTrend(metric.value()));
        row.put("Benchmark", metric.benchmark());
        return row;
    }
    
    private <K> List<Map<String, Object>> convertMapToRows(String category, Map<K, Long> data, Function<K, String> keyConverter) {
        long total = data.values().stream().mapToLong(Long::longValue).sum();
        
        return data.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = mapSupplier.get();
                    row.put("Category", category);
                    row.put("Subcategory", keyConverter.apply(entry.getKey()));
                    row.put("Count", entry.getValue());
                    row.put("Percentage", percentageFormatter.apply(total > 0 ? (entry.getValue() * 100.0) / total : 0.0));
                    row.put("Trend", "Stable"); // Simplified
                    return row;
                })
                .collect(Collectors.toList());
    }
    
    // Method reference examples for comparisons and sorting
    
    private int compareEnrollmentRows(Map<String, Object> row1, Map<String, Object> row2) {
        return stringComparator.compare(
            row1.get("Student ID").toString(),
            row2.get("Student ID").toString()
        );
    }
    
    private int compareGradeRows(Map<String, Object> row1, Map<String, Object> row2) {
        return stringComparator.compare(
            row1.get("Student").toString(),
            row2.get("Student").toString()
        );
    }
    
    private int comparePerformanceRows(Map<String, Object> row1, Map<String, Object> row2) {
        return stringComparator.compare(
            row1.get("Entity").toString(),
            row2.get("Entity").toString()
        );
    }
    
    private int compareFinancialRows(Map<String, Object> row1, Map<String, Object> row2) {
        return stringComparator.compare(
            row1.get("Department").toString(),
            row2.get("Department").toString()
        );
    }
    
    // Additional utility methods using method references
    
    private String generateReportId() {
        return "RPT_" + System.currentTimeMillis() + "_" + System.nanoTime() % 1000;
    }
    
    private void cacheReport(ReportData report) {
        reportCache.put(report.getReportId(), report);
    }
    
    private ReportMetadata createReportMetadata(ReportData report) {
        return new ReportMetadata(
            report.getReportId(),
            report.getReportType(),
            report.getTitle(),
            report.getGeneratedAt(),
            "SYSTEM",
            estimateReportSize(report),
            ReportFormat.JSON,
            null
        );
    }
    
    private void recordReportHistory(ReportMetadata metadata) {
        reportHistory.put(metadata.getReportId(), metadata);
    }
    
    private ReportData enhanceReport(ReportData report) {
        // Add enhancement logic using method references
        return report;
    }
    
    private ReportData validateReport(ReportData report) {
        return Optional.ofNullable(report)
                .filter(r -> validationCheck.test(r.getReportId()))
                .filter(r -> validationCheck.test(r.getTitle()))
                .orElseThrow(() -> new RuntimeException("Invalid report data"));
    }
    
    private ReportData handleReportError(Throwable throwable) {
        String errorReportId = reportIdGenerator.get();
        return new ReportData(errorReportId, ReportType.STATISTICAL_SUMMARY, 
                            "Error Report", "Error generating report: " + throwable.getMessage());
    }
    
    private List<ReportData> sortReportsByType(List<ReportData> reports) {
        return reports.stream()
                .sorted(Comparator.comparing(ReportData::getReportType))
                .collect(Collectors.toList());
    }
    
    // Implement remaining Reportable interface methods
    
    @Override
    public boolean exportReport(ReportData reportData, ReportFormat format, String filePath) {
        return Optional.ofNullable(reportData)
                .filter(report -> validationCheck.test(report.getReportId()))
                .map(report -> performExport(report, format, filePath))
                .orElse(false);
    }
    
    @Override
    public List<ReportType> getAvailableReportTypes() {
        return Arrays.asList(ReportType.values());
    }
    
    @Override
    public List<ReportFormat> getSupportedFormats() {
        return Arrays.asList(ReportFormat.values());
    }
    
    @Override
    public String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients) {
        String scheduleId = "SCHED_" + reportIdGenerator.get();
        ScheduledReport scheduledReport = new ScheduledReport(scheduleId, reportType, frequency, recipients);
        scheduledReports.put(scheduleId, scheduledReport);
        return scheduleId;
    }
    
    @Override
    public boolean cancelScheduledReport(String scheduledReportId) {
        return scheduledReports.remove(scheduledReportId) != null;
    }
    
    @Override
    public List<ReportMetadata> getReportHistory(ReportType reportType, int limit) {
        return reportHistory.values().stream()
                .filter(metadata -> reportType == null || reportType.equals(metadata.getReportType()))
                .sorted(reportDateComparator.reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getSummaryStatistics() {
        Map<String, Object> stats = mapSupplier.get();
        stats.put("totalReports", reportHistory.size());
        stats.put("cachedReports", reportCache.size());
        stats.put("scheduledReports", scheduledReports.size());
        stats.put("lastGenerated", reportHistory.values().stream()
                .map(ReportMetadata::getGeneratedAt)
                .max(LocalDateTime::compareTo)
                .map(dateFormatter)
                .orElse("N/A"));
        return stats;
    }
    
    // Helper methods and utility functions
    
    private String getStudentName(String studentId) {
        return studentService.getStudentById(studentId)
                .map(Student::getFullName)
                .orElse("Unknown Student");
    }
    
    private String getCourseName(String courseId) {
        return courseService.getCourseById(courseId)
                .map(Course::getCourseName)
                .orElse("Unknown Course");
    }
    
    private String validateFormattedString(String formatted) {
        return validationCheck.test(formatted) ? formatted : "Invalid";
    }
    
    private boolean isEnrollmentInDateRange(Enrollment enrollment) {
        // Simplified date range check
        return enrollment.getEnrollmentDate().isAfter(LocalDateTime.now().minusMonths(6));
    }
    
    private Map<String, Object> createEnrollmentMetadata() {
        return Map.of(
            "totalEnrollments", enrollmentService.getSummaryStatistics().get("totalEnrollments"),
            "generatedAt", currentTimeSupplier.get()
        );
    }
    
    private Map<String, Object> calculatePerformanceMetadata() {
        return Map.of(
            "studentsAnalyzed", studentService.getAllStudents().size(),
            "professorsAnalyzed", professorService.getAllProfessors().size(),
            "coursesAnalyzed", courseService.getAllCourses().size()
        );
    }
    
    private Map<String, Object> calculateDemographicMetadata() {
        return Map.of(
            "totalStudents", studentService.getAllStudents().size(),
            "totalProfessors", professorService.getAllProfessors().size(),
            "totalDepartments", departmentService.getAllDepartments().size()
        );
    }
    
    private Map<String, Object> calculateFinancialMetadata() {
        return Map.of(
            "departmentsAnalyzed", departmentService.getAllDepartments().size(),
            "totalBudget", 0.0 // Simplified
        );
    }
    
    private Optional<Map<String, Object>> calculateDepartmentFinancials(Department department) {
        // Simplified financial calculation
        Map<String, Object> financials = mapSupplier.get();
        financials.put("Department", department.getDepartmentName());
        financials.put("Budget", "$100,000"); // Simplified
        financials.put("Spent", "$75,000"); // Simplified
        financials.put("Remaining", "$25,000"); // Simplified
        financials.put("Utilization %", "75%"); // Simplified
        return Optional.of(financials);
    }
    
    private String formatMetricValue(double value) {
        return String.format("%.2f", value);
    }
    
    private String calculateTrend(double value) {
        return value > 50 ? "Positive" : value > 30 ? "Stable" : "Negative";
    }
    
    private long estimateReportSize(ReportData report) {
        return report.toString().length() * 2L; // Simplified estimation
    }
    
    private boolean performExport(ReportData report, ReportFormat format, String filePath) {
        // Simplified export logic
        return true;
    }
    
    // Data processing methods using method references
    
    private ReportData validateReportStructure(ReportData report) {
        return report; // Simplified validation
    }
    
    private ReportData enhanceWithCalculations(ReportData report) {
        return report; // Simplified enhancement
    }
    
    private ReportData applyFormatting(ReportData report) {
        return report; // Simplified formatting
    }
    
    private ReportData addSummaryStatistics(ReportData report) {
        return report; // Simplified statistics addition
    }
    
    private Map<String, Object> normalizeRowData(Map<String, Object> row) {
        return row; // Simplified normalization
    }
    
    private Map<String, Object> applyRowFormatting(Map<String, Object> row) {
        return row; // Simplified formatting
    }
    
    private Map<String, Object> addRowCalculations(Map<String, Object> row) {
        return row; // Simplified calculations
    }
    
    private boolean isValidRow(Map<String, Object> row) {
        return row != null && !row.isEmpty();
    }
    
    private double parseNumericValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    // Inner classes and records for method reference examples
    
    /**
     * Record for performance metrics - demonstrates method references with records.
     */
    private record PerformanceMetric(String entityType, String entityName, String metricName, 
                                   double value, String benchmark) {}
    
    /**
     * Scheduled report class for demonstration.
     */
    private static class ScheduledReport {
        private final String scheduleId;
        private final ReportType reportType;
        private final String frequency;
        private final List<String> recipients;
        
        public ScheduledReport(String scheduleId, ReportType reportType, String frequency, List<String> recipients) {
            this.scheduleId = scheduleId;
            this.reportType = reportType;
            this.frequency = frequency;
            this.recipients = new ArrayList<>(recipients);
        }
        
        // Getters for method reference usage
        public String getScheduleId() { return scheduleId; }
        public ReportType getReportType() { return reportType; }
        public String getFrequency() { return frequency; }
        public List<String> getRecipients() { return recipients; }
    }
}