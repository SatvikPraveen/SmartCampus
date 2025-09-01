// File: src/main/java/services/GradeService.java
package services;

import models.Grade;
import models.Grade.GradeComponent;
import models.Grade.GradeStatus;
import models.Enrollment;
import interfaces.Searchable;
import interfaces.Reportable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GradeService class providing grade management operations.
 * This service manages grade entries, calculations, and grade-related analytics.
 * 
 * Key Java concepts demonstrated:
 * - Advanced Stream API operations
 * - Functional programming with method references
 * - Statistical calculations and aggregations
 * - Complex business logic for grade calculations
 * - Interface implementations
 * - Thread-safe operations
 * - Performance optimization with caching
 */
public class GradeService implements Searchable<Grade>, Reportable {
    
    // Instance fields
    private final Map<String, Grade> grades;
    private final Map<String, List<String>> studentGrades; // studentId -> gradeIds
    private final Map<String, List<String>> courseGrades; // courseId -> gradeIds
    private final Map<String, List<String>> enrollmentGrades; // enrollmentId -> gradeIds
    private final Map<String, Double> calculatedGPAs; // studentId -> GPA
    private final Map<String, Map<GradeComponent, Double>> gradeWeights; // courseId -> component weights
    
    // Grade scale configuration
    private final Map<String, Double> gradeScale;
    private final double passingGrade = 60.0;
    
    // Statistics cache
    private volatile Map<String, Object> cachedStatistics;
    private volatile LocalDateTime lastStatisticsUpdate;
    
    /**
     * Constructor initializing the service.
     */
    public GradeService() {
        this.grades = new ConcurrentHashMap<>();
        this.studentGrades = new ConcurrentHashMap<>();
        this.courseGrades = new ConcurrentHashMap<>();
        this.enrollmentGrades = new ConcurrentHashMap<>();
        this.calculatedGPAs = new ConcurrentHashMap<>();
        this.gradeWeights = new ConcurrentHashMap<>();
        this.gradeScale = initializeGradeScale();
        this.cachedStatistics = new HashMap<>();
        this.lastStatisticsUpdate = LocalDateTime.now();
    }
    
    // Core grade management operations
    
    /**
     * Add a new grade.
     * 
     * @param grade The grade to add
     * @return true if grade was added successfully, false otherwise
     */
    public boolean addGrade(Grade grade) {
        if (grade == null || !ValidationUtil.isValidString(grade.getGradeId())) {
            return false;
        }
        
        if (grades.containsKey(grade.getGradeId())) {
            return false; // Grade already exists
        }
        
        grades.put(grade.getGradeId(), grade);
        
        // Update indices
        studentGrades.computeIfAbsent(grade.getStudentId(), k -> new ArrayList<>()).add(grade.getGradeId());
        courseGrades.computeIfAbsent(grade.getCourseId(), k -> new ArrayList<>()).add(grade.getGradeId());
        if (grade.getEnrollmentId() != null) {
            enrollmentGrades.computeIfAbsent(grade.getEnrollmentId(), k -> new ArrayList<>()).add(grade.getGradeId());
        }
        
        // Invalidate cached GPA
        calculatedGPAs.remove(grade.getStudentId());
        invalidateStatisticsCache();
        
        return true;
    }
    
    /**
     * Update an existing grade.
     * 
     * @param grade The updated grade data
     * @return true if grade was updated successfully, false otherwise
     */
    public boolean updateGrade(Grade grade) {
        if (grade == null || !grades.containsKey(grade.getGradeId())) {
            return false;
        }
        
        Grade existingGrade = grades.get(grade.getGradeId());
        grades.put(grade.getGradeId(), grade);
        
        // Invalidate cached GPA if student changed
        calculatedGPAs.remove(existingGrade.getStudentId());
        if (!existingGrade.getStudentId().equals(grade.getStudentId())) {
            calculatedGPAs.remove(grade.getStudentId());
        }
        
        invalidateStatisticsCache();
        return true;
    }
    
    /**
     * Remove a grade.
     * 
     * @param gradeId The ID of the grade to remove
     * @return true if grade was removed successfully, false otherwise
     */
    public boolean removeGrade(String gradeId) {
        if (!ValidationUtil.isValidString(gradeId)) {
            return false;
        }
        
        Grade removed = grades.remove(gradeId);
        if (removed != null) {
            // Update indices
            List<String> studentGradeIds = studentGrades.get(removed.getStudentId());
            if (studentGradeIds != null) {
                studentGradeIds.remove(gradeId);
            }
            
            List<String> courseGradeIds = courseGrades.get(removed.getCourseId());
            if (courseGradeIds != null) {
                courseGradeIds.remove(gradeId);
            }
            
            if (removed.getEnrollmentId() != null) {
                List<String> enrollmentGradeIds = enrollmentGrades.get(removed.getEnrollmentId());
                if (enrollmentGradeIds != null) {
                    enrollmentGradeIds.remove(gradeId);
                }
            }
            
            // Invalidate cached GPA
            calculatedGPAs.remove(removed.getStudentId());
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Get grade by ID.
     * 
     * @param gradeId The grade ID
     * @return Optional containing the grade if found
     */
    public Optional<Grade> getGradeById(String gradeId) {
        return Optional.ofNullable(grades.get(gradeId));
    }
    
    /**
     * Get all grades.
     * 
     * @return List of all grades
     */
    public List<Grade> getAllGrades() {
        return new ArrayList<>(grades.values());
    }
    
    // Grade assignment and modification operations
    
    /**
     * Assign grade to student for assignment.
     * 
     * @param studentId The student ID
     * @param courseId The course ID
     * @param assignmentName The assignment name
     * @param component The grade component type
     * @param pointsEarned Points earned by student
     * @param pointsPossible Total points possible
     * @param gradedBy The grader's ID
     * @param feedback Optional feedback
     * @return true if grade was assigned successfully
     */
    public boolean assignGrade(String studentId, String courseId, String assignmentName, 
                              GradeComponent component, double pointsEarned, double pointsPossible,
                              String gradedBy, String feedback) {
        
        if (!ValidationUtil.isValidString(studentId) || !ValidationUtil.isValidString(courseId) ||
            pointsEarned < 0 || pointsPossible <= 0 || pointsEarned > pointsPossible) {
            return false;
        }
        
        Grade grade = Grade.createGrade(null, studentId, courseId, assignmentName, component, pointsPossible);
        grade.gradeAssignment(pointsEarned, gradedBy, feedback);
        
        return addGrade(grade);
    }
    
    /**
     * Update grade points.
     * 
     * @param gradeId The grade ID
     * @param pointsEarned New points earned
     * @param gradedBy The grader's ID
     * @param feedback Updated feedback
     * @return true if grade was updated successfully
     */
    public boolean updateGradePoints(String gradeId, double pointsEarned, String gradedBy, String feedback) {
        return getGradeById(gradeId)
                .map(grade -> {
                    grade.gradeAssignment(pointsEarned, gradedBy, feedback);
                    calculatedGPAs.remove(grade.getStudentId());
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Submit assignment for grading.
     * 
     * @param gradeId The grade ID
     * @return true if submission was successful
     */
    public boolean submitAssignment(String gradeId) {
        return getGradeById(gradeId)
                .map(grade -> grade.submitAssignment())
                .orElse(false);
    }
    
    /**
     * Return graded assignment to student.
     * 
     * @param gradeId The grade ID
     * @return true if return was successful
     */
    public boolean returnGradedAssignment(String gradeId) {
        return getGradeById(gradeId)
                .map(grade -> grade.returnToStudent())
                .orElse(false);
    }
    
    /**
     * Excuse assignment for student.
     * 
     * @param gradeId The grade ID
     * @param reason The excuse reason
     * @return true if excuse was successful
     */
    public boolean excuseAssignment(String gradeId, String reason) {
        return getGradeById(gradeId)
                .map(grade -> grade.excuseAssignment(reason))
                .orElse(false);
    }
    
    // Advanced query operations using Stream API
    
    /**
     * Get grades for student using Stream API.
     * 
     * @param studentId The student ID
     * @return List of grades for the student
     */
    public List<Grade> getStudentGrades(String studentId) {
        return studentGrades.getOrDefault(studentId, new ArrayList<>()).stream()
                .map(grades::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Grade::getDateAssigned))
                .collect(Collectors.toList());
    }
    
    /**
     * Get grades for course using Stream API.
     * 
     * @param courseId The course ID
     * @return List of grades for the course
     */
    public List<Grade> getCourseGrades(String courseId) {
        return courseGrades.getOrDefault(courseId, new ArrayList<>()).stream()
                .map(grades::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Grade::getStudentId)
                       .thenComparing(Grade::getAssignmentName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get grades by status using Stream API.
     * 
     * @param status The grade status
     * @return List of grades with the specified status
     */
    public List<Grade> getGradesByStatus(GradeStatus status) {
        return grades.values().stream()
                .filter(grade -> grade.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * Get grades by component type using Stream API.
     * 
     * @param component The grade component type
     * @return List of grades of the specified component type
     */
    public List<Grade> getGradesByComponent(GradeComponent component) {
        return grades.values().stream()
                .filter(grade -> grade.getComponent() == component)
                .collect(Collectors.toList());
    }
    
    /**
     * Get failing grades using Stream API.
     * 
     * @return List of grades below passing threshold
     */
    public List<Grade> getFailingGrades() {
        return grades.values().stream()
                .filter(grade -> grade.getPercentage() >= 0 && grade.getPercentage() < passingGrade)
                .collect(Collectors.toList());
    }
    
    /**
     * Get high-performing grades using Stream API.
     * 
     * @param threshold The minimum percentage threshold
     * @return List of grades above the threshold
     */
    public List<Grade> getHighPerformingGrades(double threshold) {
        return grades.values().stream()
                .filter(grade -> grade.getPercentage() >= threshold)
                .sorted(Comparator.comparingDouble(Grade::getPercentage).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get overdue assignments using Stream API.
     * 
     * @return List of grades for overdue assignments
     */
    public List<Grade> getOverdueAssignments() {
        return grades.values().stream()
                .filter(Grade::isOverdue)
                .collect(Collectors.toList());
    }
    
    /**
     * Get grades pending review using Stream API.
     * 
     * @return List of grades that need to be graded
     */
    public List<Grade> getGradesPendingReview() {
        return grades.values().stream()
                .filter(grade -> grade.getStatus() == GradeStatus.SUBMITTED || 
                               grade.getStatus() == GradeStatus.LATE)
                .collect(Collectors.toList());
    }
    
    // GPA and statistical calculations using Stream API
    
    /**
     * Calculate GPA for student using Stream API.
     * 
     * @param studentId The student ID
     * @return The calculated GPA
     */
    public double calculateStudentGPA(String studentId) {
        return calculatedGPAs.computeIfAbsent(studentId, this::computeGPA);
    }
    
    /**
     * Calculate course average using Stream API.
     * 
     * @param courseId The course ID
     * @return The average grade percentage for the course
     */
    public OptionalDouble calculateCourseAverage(String courseId) {
        return getCourseGrades(courseId).stream()
                .filter(grade -> grade.getPercentage() >= 0)
                .filter(Grade::countsTowardFinalGrade)
                .mapToDouble(Grade::getPercentage)
                .average();
    }
    
    /**
     * Calculate weighted course grade for student using Stream API.
     * 
     * @param studentId The student ID
     * @param courseId The course ID
     * @return The weighted course grade
     */
    public OptionalDouble calculateWeightedCourseGrade(String studentId, String courseId) {
        List<Grade> studentCourseGrades = getStudentGrades(studentId).stream()
                .filter(grade -> courseId.equals(grade.getCourseId()))
                .filter(Grade::countsTowardFinalGrade)
                .collect(Collectors.toList());
        
        if (studentCourseGrades.isEmpty()) {
            return OptionalDouble.empty();
        }
        
        Map<GradeComponent, Double> weights = gradeWeights.getOrDefault(courseId, getDefaultWeights());
        
        // Group grades by component and calculate weighted average
        Map<GradeComponent, Double> componentAverages = studentCourseGrades.stream()
                .collect(Collectors.groupingBy(
                    Grade::getComponent,
                    Collectors.averagingDouble(Grade::getPercentage)
                ));
        
        double weightedSum = componentAverages.entrySet().stream()
                .mapToDouble(entry -> entry.getValue() * weights.getOrDefault(entry.getKey(), 0.0))
                .sum();
        
        double totalWeight = componentAverages.keySet().stream()
                .mapToDouble(component -> weights.getOrDefault(component, 0.0))
                .sum();
        
        return totalWeight > 0 ? OptionalDouble.of(weightedSum / totalWeight) : OptionalDouble.empty();
    }
    
    /**
     * Set grade weights for course.
     * 
     * @param courseId The course ID
     * @param weights Map of component to weight
     * @return true if weights were set successfully
     */
    public boolean setGradeWeights(String courseId, Map<GradeComponent, Double> weights) {
        if (ValidationUtil.isValidString(courseId) && weights != null && !weights.isEmpty()) {
            // Validate weights sum to 1.0 (100%)
            double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(totalWeight - 1.0) < 0.01) { // Allow small floating point errors
                gradeWeights.put(courseId, new HashMap<>(weights));
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get grade distribution for course using Stream API.
     * 
     * @param courseId The course ID
     * @return Map of letter grades to count
     */
    public Map<String, Long> getGradeDistribution(String courseId) {
        return getCourseGrades(courseId).stream()
                .filter(grade -> grade.getLetterGrade() != null)
                .filter(Grade::countsTowardFinalGrade)
                .collect(Collectors.groupingBy(
                    Grade::getLetterGrade,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get grade statistics for course using Stream API.
     * 
     * @param courseId The course ID
     * @return Map containing grade statistics
     */
    public Map<String, Object> getCourseGradeStatistics(String courseId) {
        List<Grade> courseGrades = getCourseGrades(courseId).stream()
                .filter(grade -> grade.getPercentage() >= 0)
                .filter(Grade::countsTowardFinalGrade)
                .collect(Collectors.toList());
        
        if (courseGrades.isEmpty()) {
            return Map.of();
        }
        
        DoubleSummaryStatistics stats = courseGrades.stream()
                .mapToDouble(Grade::getPercentage)
                .summaryStatistics();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("count", courseGrades.size());
        statistics.put("average", stats.getAverage());
        statistics.put("min", stats.getMin());
        statistics.put("max", stats.getMax());
        statistics.put("median", calculateMedian(courseGrades.stream()
                .mapToDouble(Grade::getPercentage)
                .boxed()
                .collect(Collectors.toList())));
        statistics.put("standardDeviation", calculateStandardDeviation(courseGrades.stream()
                .mapToDouble(Grade::getPercentage)
                .boxed()
                .collect(Collectors.toList())));
        statistics.put("gradeDistribution", getGradeDistribution(courseId));
        
        return statistics;
    }
    
    /**
     * Calculate overall grade statistics using Stream API.
     * 
     * @return Map containing various statistical measures
     */
    public Map<String, Object> calculateOverallStatistics() {
        if (isStatisticsCacheValid()) {
            return new HashMap<>(cachedStatistics);
        }
        
        List<Grade> validGrades = grades.values().stream()
                .filter(grade -> grade.getPercentage() >= 0)
                .filter(Grade::countsTowardFinalGrade)
                .collect(Collectors.toList());
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (!validGrades.isEmpty()) {
            DoubleSummaryStatistics gradeStats = validGrades.stream()
                    .mapToDouble(Grade::getPercentage)
                    .summaryStatistics();
            
            statistics.put("totalGrades", grades.size());
            statistics.put("validGrades", validGrades.size());
            statistics.put("averageGrade", gradeStats.getAverage());
            statistics.put("minGrade", gradeStats.getMin());
            statistics.put("maxGrade", gradeStats.getMax());
            statistics.put("gradesByStatus", getGradeStatisticsByStatus());
            statistics.put("gradesByComponent", getGradeStatisticsByComponent());
            statistics.put("overallGradeDistribution", getOverallGradeDistribution());
            statistics.put("failingGradeCount", getFailingGrades().size());
            statistics.put("overdueAssignmentCount", getOverdueAssignments().size());
            statistics.put("pendingReviewCount", getGradesPendingReview().size());
            statistics.put("completionRate", calculateCompletionRate());
        }
        
        // Cache the results
        cachedStatistics = new HashMap<>(statistics);
        lastStatisticsUpdate = LocalDateTime.now();
        
        return statistics;
    }
    
    // Statistical operations using Stream API
    
    /**
     * Get grade statistics by status using Stream API.
     * 
     * @return Map of grade status to count
     */
    public Map<GradeStatus, Long> getGradeStatisticsByStatus() {
        return grades.values().stream()
                .collect(Collectors.groupingBy(
                    Grade::getStatus,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get grade statistics by component using Stream API.
     * 
     * @return Map of grade component to count
     */
    public Map<GradeComponent, Long> getGradeStatisticsByComponent() {
        return grades.values().stream()
                .collect(Collectors.groupingBy(
                    Grade::getComponent,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get overall grade distribution using Stream API.
     * 
     * @return Map of letter grades to count
     */
    public Map<String, Long> getOverallGradeDistribution() {
        return grades.values().stream()
                .filter(grade -> grade.getLetterGrade() != null)
                .filter(Grade::countsTowardFinalGrade)
                .collect(Collectors.groupingBy(
                    Grade::getLetterGrade,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get students at risk (low grades) using Stream API.
     * 
     * @param threshold The grade threshold below which students are considered at risk
     * @return List of student IDs at risk
     */
    public List<String> getStudentsAtRisk(double threshold) {
        return studentGrades.entrySet().stream()
                .filter(entry -> {
                    double averageGrade = entry.getValue().stream()
                            .map(grades::get)
                            .filter(Objects::nonNull)
                            .filter(grade -> grade.getPercentage() >= 0)
                            .filter(Grade::countsTowardFinalGrade)
                            .mapToDouble(Grade::getPercentage)
                            .average()
                            .orElse(100.0);
                    return averageGrade < threshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Get top performers using Stream API.
     * 
     * @param n Number of top students to return
     * @return List of top N student IDs by GPA
     */
    public List<String> getTopPerformers(int n) {
        return studentGrades.keySet().stream()
                .sorted(Comparator.comparingDouble(this::calculateStudentGPA).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }
    
    // Searchable interface implementation
    
    @Override
    public List<Grade> search(String keyword) {
        return grades.values().stream()
                .filter(grade -> matchesKeyword(grade, keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Grade> search(Map<String, SearchCriterion> searchCriteria) {
        return grades.values().stream()
                .filter(grade -> matchesSearchCriteria(grade, searchCriteria))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Grade> search(Predicate<Grade> predicate) {
        return grades.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Grade> searchAndSort(String keyword, String sortBy, SortOrder sortOrder) {
        Comparator<Grade> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return search(keyword).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    @Override
    public SearchResult<Grade> searchWithPagination(String keyword, int page, int pageSize) {
        List<Grade> allResults = search(keyword);
        int totalElements = allResults.size();
        
        List<Grade> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements);
    }
    
    @Override
    public SearchResult<Grade> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria,
                                                           String sortBy, SortOrder sortOrder,
                                                           int page, int pageSize) {
        Comparator<Grade> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        List<Grade> allResults = search(searchCriteria).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        int totalElements = allResults.size();
        
        List<Grade> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements, sortBy, sortOrder);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialInput, int maxSuggestions) {
        return grades.values().stream()
                .flatMap(grade -> Arrays.stream(new String[]{
                    grade.getStudentId(),
                    grade.getCourseId(),
                    grade.getAssignmentName(),
                    grade.getComponent().toString(),
                    grade.getStatus().toString(),
                    grade.getLetterGrade()
                }))
                .filter(Objects::nonNull)
                .filter(field -> field.toLowerCase().contains(partialInput.toLowerCase()))
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Grade> filter(Predicate<Grade> predicate) {
        return search(predicate);
    }
    
    @Override
    public List<String> getSearchableFields() {
        return Arrays.asList("studentId", "courseId", "assignmentName", "component", 
                           "status", "letterGrade", "percentage", "pointsEarned");
    }
    
    @Override
    public List<String> getSortableFields() {
        return Arrays.asList("dateAssigned", "dateDue", "dateGraded", "studentId", "courseId", 
                           "assignmentName", "component", "status", "percentage", "pointsEarned");
    }
    
    @Override
    public long countSearchResults(String keyword) {
        return search(keyword).size();
    }
    
    @Override
    public long countSearchResults(Map<String, SearchCriterion> searchCriteria) {
        return search(searchCriteria).size();
    }
    
    // Reportable interface implementation
    
    @Override
    public ReportData generateReport(ReportType reportType) {
        return generateReport(reportType, Map.of());
    }
    
    @Override
    public ReportData generateReport(ReportType reportType, Map<String, Object> parameters) {
        String reportId = "RPT_GRADE_" + System.currentTimeMillis();
        
        switch (reportType) {
            case GRADE_REPORT:
                return generateGradeReport(reportId, parameters);
            case PERFORMANCE_REPORT:
                return generatePerformanceReport(reportId, parameters);
            case STATISTICAL_SUMMARY:
                return generateStatisticalReport(reportId, parameters);
            default:
                return new ReportData(reportId, reportType, "Unsupported Report Type", "Report type not supported");
        }
    }
    
    @Override
    public ReportData generateReportForDateRange(ReportType reportType, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> parameters = Map.of(
            "startDate", startDate,
            "endDate", endDate
        );
        return generateReport(reportType, parameters);
    }
    
    @Override
    public boolean exportReport(ReportData reportData, ReportFormat format, String filePath) {
        return true; // Simplified for demo
    }
    
    @Override
    public List<ReportType> getAvailableReportTypes() {
        return Arrays.asList(
            ReportType.GRADE_REPORT,
            ReportType.PERFORMANCE_REPORT,
            ReportType.STATISTICAL_SUMMARY
        );
    }
    
    @Override
    public List<ReportFormat> getSupportedFormats() {
        return Arrays.asList(ReportFormat.PDF, ReportFormat.CSV, ReportFormat.EXCEL, ReportFormat.JSON);
    }
    
    @Override
    public String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients) {
        return "SCHED_GRADE_" + System.currentTimeMillis(); // Simplified for demo
    }
    
    @Override
    public boolean cancelScheduledReport(String scheduledReportId) {
        return true; // Simplified for demo
    }
    
    @Override
    public List<ReportMetadata> getReportHistory(ReportType reportType, int limit) {
        return new ArrayList<>(); // Simplified for demo
    }
    
    @Override
    public Map<String, Object> getSummaryStatistics() {
        return calculateOverallStatistics();
    }
    
    // Helper methods
    
    /**
     * Compute GPA for student using functional approach.
     */
    private double computeGPA(String studentId) {
        List<Grade> studentCourseGrades = getStudentGrades(studentId);
        
        if (studentCourseGrades.isEmpty()) {
            return 0.0;
        }
        
        // Group by course and calculate course averages
        Map<String, OptionalDouble> courseAverages = studentCourseGrades.stream()
                .filter(Grade::countsTowardFinalGrade)
                .collect(Collectors.groupingBy(
                    Grade::getCourseId,
                    Collectors.averagingDouble(Grade::getPercentage)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> OptionalDouble.of(entry.getValue())
                ));
        
        // Convert course averages to GPA points and calculate overall GPA
        double totalGpaPoints = courseAverages.values().stream()
                .filter(OptionalDouble::isPresent)
                .mapToDouble(avg -> convertPercentageToGpaPoints(avg.getAsDouble()))
                .sum();
        
        int courseCount = (int) courseAverages.values().stream()
                .filter(OptionalDouble::isPresent)
                .count();
        
        return courseCount > 0 ? totalGpaPoints / courseCount : 0.0;
    }
    
    /**
     * Convert percentage to GPA points.
     */
    private double convertPercentageToGpaPoints(double percentage) {
        if (percentage >= 97) return 4.0;
        else if (percentage >= 93) return 4.0;
        else if (percentage >= 90) return 3.7;
        else if (percentage >= 87) return 3.3;
        else if (percentage >= 83) return 3.0;
        else if (percentage >= 80) return 2.7;
        else if (percentage >= 77) return 2.3;
        else if (percentage >= 73) return 2.0;
        else if (percentage >= 70) return 1.7;
        else if (percentage >= 67) return 1.3;
        else if (percentage >= 63) return 1.0;
        else if (percentage >= 60) return 0.7;
        else return 0.0;
    }
    
    /**
     * Initialize grade scale.
     */
    private Map<String, Double> initializeGradeScale() {
        Map<String, Double> scale = new HashMap<>();
        scale.put("A+", 4.0);
        scale.put("A", 4.0);
        scale.put("A-", 3.7);
        scale.put("B+", 3.3);
        scale.put("B", 3.0);
        scale.put("B-", 2.7);
        scale.put("C+", 2.3);
        scale.put("C", 2.0);
        scale.put("C-", 1.7);
        scale.put("D+", 1.3);
        scale.put("D", 1.0);
        scale.put("D-", 0.7);
        scale.put("F", 0.0);
        return scale;
    }
    
    /**
     * Get default grade weights.
     */
    private Map<GradeComponent, Double> getDefaultWeights() {
        Map<GradeComponent, Double> weights = new HashMap<>();
        weights.put(GradeComponent.EXAM, 0.4);
        weights.put(GradeComponent.HOMEWORK, 0.3);
        weights.put(GradeComponent.PROJECT, 0.2);
        weights.put(GradeComponent.PARTICIPATION, 0.1);
        return weights;
    }
    
    /**
     * Calculate median from list of doubles.
     */
    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        
        List<Double> sorted = values.stream()
                .sorted()
                .collect(Collectors.toList());
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    /**
     * Calculate standard deviation.
     */
    private double calculateStandardDeviation(List<Double> values) {
        if (values.size() <= 1) return 0.0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate completion rate.
     */
    private double calculateCompletionRate() {
        long totalGrades = grades.size();
        long completedGrades = grades.values().stream()
                .filter(grade -> grade.getStatus() == GradeStatus.GRADED || 
                               grade.getStatus() == GradeStatus.RETURNED)
                .count();
        
        return totalGrades > 0 ? (completedGrades * 100.0) / totalGrades : 0.0;
    }
    
    /**
     * Check if grade matches keyword.
     */
    private boolean matchesKeyword(Grade grade, String keyword) {
        return grade.getStudentId().toLowerCase().contains(keyword) ||
               grade.getCourseId().toLowerCase().contains(keyword) ||
               grade.getAssignmentName().toLowerCase().contains(keyword) ||
               grade.getComponent().toString().toLowerCase().contains(keyword) ||
               grade.getStatus().toString().toLowerCase().contains(keyword) ||
               (grade.getLetterGrade() != null && grade.getLetterGrade().toLowerCase().contains(keyword));
    }
    
    /**
     * Check if grade matches search criteria.
     */
    private boolean matchesSearchCriteria(Grade grade, Map<String, SearchCriterion> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> matchesCriterion(grade, entry.getKey(), entry.getValue()));
    }
    
    /**
     * Check if grade matches a specific criterion.
     */
    private boolean matchesCriterion(Grade grade, String field, SearchCriterion criterion) {
        Object fieldValue = getFieldValue(grade, field);
        if (fieldValue == null) return false;
        
        String fieldStr = fieldValue.toString().toLowerCase();
        String searchValue = criterion.getValue().toString().toLowerCase();
        
        switch (criterion.getCriteria()) {
            case EXACT_MATCH:
                return fieldStr.equals(searchValue);
            case CONTAINS:
                return fieldStr.contains(searchValue);
            case STARTS_WITH:
                return fieldStr.startsWith(searchValue);
            case ENDS_WITH:
                return fieldStr.endsWith(searchValue);
            default:
                return false;
        }
    }
    
    /**
     * Get field value from grade.
     */
    private Object getFieldValue(Grade grade, String field) {
        switch (field.toLowerCase()) {
            case "studentid": return grade.getStudentId();
            case "courseid": return grade.getCourseId();
            case "assignmentname": return grade.getAssignmentName();
            case "component": return grade.getComponent();
            case "status": return grade.getStatus();
            case "lettergrade": return grade.getLetterGrade();
            case "percentage": return grade.getPercentage();
            case "pointsearned": return grade.getPointsEarned();
            case "pointspossible": return grade.getPointsPossible();
            case "dateassigned": return grade.getDateAssigned();
            case "datedue": return grade.getDateDue();
            case "dategraded": return grade.getDateGraded();
            default: return null;
        }
    }
    
    /**
     * Get comparator for sorting.
     */
    private Comparator<Grade> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "dateassigned":
                return Comparator.comparing(Grade::getDateAssigned, Comparator.nullsLast(Comparator.naturalOrder()));
            case "datedue":
                return Comparator.comparing(Grade::getDateDue, Comparator.nullsLast(Comparator.naturalOrder()));
            case "dategraded":
                return Comparator.comparing(Grade::getDateGraded, Comparator.nullsLast(Comparator.naturalOrder()));
            case "studentid":
                return Comparator.comparing(Grade::getStudentId);
            case "courseid":
                return Comparator.comparing(Grade::getCourseId);
            case "assignmentname":
                return Comparator.comparing(Grade::getAssignmentName);
            case "component":
                return Comparator.comparing(Grade::getComponent);
            case "status":
                return Comparator.comparing(Grade::getStatus);
            case "percentage":
                return Comparator.comparingDouble(Grade::getPercentage);
            case "pointsearned":
                return Comparator.comparingDouble(Grade::getPointsEarned);
            default:
                return Comparator.comparing(Grade::getDateAssigned, Comparator.nullsLast(Comparator.naturalOrder()));
        }
    }
    
    /**
     * Generate grade report.
     */
    private ReportData generateGradeReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Grade ID", "Student ID", "Course ID", "Assignment", "Component", 
                                           "Points", "Percentage", "Letter Grade", "Status", "Date");
        
        List<Map<String, Object>> rows = grades.values().stream()
                .map(grade -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Grade ID", grade.getGradeId());
                    row.put("Student ID", grade.getStudentId());
                    row.put("Course ID", grade.getCourseId());
                    row.put("Assignment", grade.getAssignmentName());
                    row.put("Component", grade.getComponent().toString());
                    row.put("Points", String.format("%.1f/%.1f", grade.getPointsEarned(), grade.getPointsPossible()));
                    row.put("Percentage", String.format("%.1f%%", grade.getPercentage()));
                    row.put("Letter Grade", grade.getLetterGrade() != null ? grade.getLetterGrade() : "N/A");
                    row.put("Status", grade.getStatus().toString());
                    row.put("Date", grade.getDateGraded() != null ? grade.getDateGraded() : grade.getDateAssigned());
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.GRADE_REPORT, "Grade Report", 
                            columns, rows, Map.of("totalGrades", grades.size()));
    }
    
    /**
     * Generate performance report.
     */
    private ReportData generatePerformanceReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Student ID", "Course Count", "Average Grade", "GPA", "Status");
        
        List<Map<String, Object>> rows = studentGrades.entrySet().stream()
                .map(entry -> {
                    String studentId = entry.getKey();
                    List<Grade> studentGradeList = entry.getValue().stream()
                            .map(grades::get)
                            .filter(Objects::nonNull)
                            .filter(Grade::countsTowardFinalGrade)
                            .collect(Collectors.toList());
                    
                    Map<String, Object> row = new HashMap<>();
                    row.put("Student ID", studentId);
                    row.put("Course Count", studentGradeList.stream()
                            .map(Grade::getCourseId)
                            .distinct()
                            .count());
                    
                    double avgGrade = studentGradeList.stream()
                            .filter(grade -> grade.getPercentage() >= 0)
                            .mapToDouble(Grade::getPercentage)
                            .average()
                            .orElse(0.0);
                    
                    row.put("Average Grade", String.format("%.1f%%", avgGrade));
                    row.put("GPA", String.format("%.2f", calculateStudentGPA(studentId)));
                    row.put("Status", avgGrade >= passingGrade ? "Passing" : "At Risk");
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.PERFORMANCE_REPORT, "Student Performance Report", 
                            columns, rows, calculateOverallStatistics());
    }
    
    /**
     * Generate statistical report.
     */
    private ReportData generateStatisticalReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        String content = statistics.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, "Grade Statistical Summary", content);
    }
    
    /**
     * Check if statistics cache is valid.
     */
    private boolean isStatisticsCacheValid() {
        return lastStatisticsUpdate != null && 
               LocalDateTime.now().minusMinutes(5).isBefore(lastStatisticsUpdate);
    }
    
    /**
     * Invalidate statistics cache.
     */
    private void invalidateStatisticsCache() {
        lastStatisticsUpdate = null;
    }
}