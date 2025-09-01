// File: src/main/java/services/StudentService.java
package services;

import models.Student;
import models.Student.AcademicYear;
import models.Enrollment;
import models.Grade;
import interfaces.Searchable;
import interfaces.Reportable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StudentService class providing student-related business operations.
 * This service manages student data and operations using Stream API and functional programming.
 * 
 * Key Java concepts demonstrated:
 * - Stream API for data processing
 * - Lambda expressions and method references
 * - Functional interfaces (Predicate, Function, Collector)
 * - Optional for null-safe operations
 * - Interface implementation (Searchable, Reportable)
 * - Concurrent collections for thread safety
 * - Statistical operations with streams
 * - Custom collectors and grouping operations
 */
public class StudentService implements Searchable<Student>, Reportable {
    
    // Instance fields
    private final Map<String, Student> students;
    private final Map<String, List<Enrollment>> studentEnrollments;
    private final Map<String, List<Grade>> studentGrades;
    private final Map<String, Double> calculatedGPAs;
    
    // Statistics cache
    private volatile Map<String, Object> cachedStatistics;
    private volatile LocalDateTime lastStatisticsUpdate;
    
    /**
     * Constructor initializing the service.
     */
    public StudentService() {
        this.students = new ConcurrentHashMap<>();
        this.studentEnrollments = new ConcurrentHashMap<>();
        this.studentGrades = new ConcurrentHashMap<>();
        this.calculatedGPAs = new ConcurrentHashMap<>();
        this.cachedStatistics = new HashMap<>();
        this.lastStatisticsUpdate = LocalDateTime.now();
    }
    
    // Core CRUD operations
    
    /**
     * Add a new student.
     * 
     * @param student The student to add
     * @return true if student was added successfully, false otherwise
     */
    public boolean addStudent(Student student) {
        if (student == null || !ValidationUtil.isValidString(student.getStudentId())) {
            return false;
        }
        
        if (students.containsKey(student.getStudentId())) {
            return false; // Student already exists
        }
        
        students.put(student.getStudentId(), student);
        studentEnrollments.put(student.getStudentId(), new ArrayList<>());
        studentGrades.put(student.getStudentId(), new ArrayList<>());
        invalidateStatisticsCache();
        
        return true;
    }
    
    /**
     * Update an existing student.
     * 
     * @param student The updated student data
     * @return true if student was updated successfully, false otherwise
     */
    public boolean updateStudent(Student student) {
        if (student == null || !students.containsKey(student.getStudentId())) {
            return false;
        }
        
        students.put(student.getStudentId(), student);
        invalidateStatisticsCache();
        return true;
    }
    
    /**
     * Remove a student.
     * 
     * @param studentId The ID of the student to remove
     * @return true if student was removed successfully, false otherwise
     */
    public boolean removeStudent(String studentId) {
        if (!ValidationUtil.isValidString(studentId)) {
            return false;
        }
        
        Student removed = students.remove(studentId);
        if (removed != null) {
            studentEnrollments.remove(studentId);
            studentGrades.remove(studentId);
            calculatedGPAs.remove(studentId);
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Get student by ID.
     * 
     * @param studentId The student ID
     * @return Optional containing the student if found
     */
    public Optional<Student> getStudentById(String studentId) {
        return Optional.ofNullable(students.get(studentId));
    }
    
    /**
     * Get all students.
     * 
     * @return List of all students
     */
    public List<Student> getAllStudents() {
        return new ArrayList<>(students.values());
    }
    
    // Advanced query operations using Stream API
    
    /**
     * Get students by major using Stream API.
     * 
     * @param major The major to filter by
     * @return List of students with the specified major
     */
    public List<Student> getStudentsByMajor(String major) {
        return students.values().stream()
                .filter(student -> major.equalsIgnoreCase(student.getMajor()))
                .sorted(Comparator.comparing(Student::getLastName)
                       .thenComparing(Student::getFirstName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get students by academic year using Stream API.
     * 
     * @param academicYear The academic year to filter by
     * @return List of students in the specified academic year
     */
    public List<Student> getStudentsByAcademicYear(AcademicYear academicYear) {
        return students.values().stream()
                .filter(student -> academicYear.equals(student.getAcademicYear()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get students with GPA above threshold using Stream API.
     * 
     * @param gpaThreshold The minimum GPA threshold
     * @return List of students with GPA above the threshold
     */
    public List<Student> getStudentsWithGPAAbove(double gpaThreshold) {
        return students.values().stream()
                .filter(student -> calculateGPA(student.getStudentId()) >= gpaThreshold)
                .sorted(Comparator.comparingDouble((Student s) -> calculateGPA(s.getStudentId())).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get top N students by GPA using Stream API.
     * 
     * @param n Number of top students to return
     * @return List of top N students by GPA
     */
    public List<Student> getTopStudentsByGPA(int n) {
        return students.values().stream()
                .sorted(Comparator.comparingDouble((Student s) -> calculateGPA(s.getStudentId())).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * Get students by enrollment status using Stream API.
     * 
     * @param status The enrollment status to filter by
     * @return List of students with the specified enrollment status
     */
    public List<Student> getStudentsByEnrollmentStatus(Enrollment.EnrollmentStatus status) {
        return students.values().stream()
                .filter(student -> hasEnrollmentWithStatus(student.getStudentId(), status))
                .collect(Collectors.toList());
    }
    
    /**
     * Get students enrolled in a specific course using Stream API.
     * 
     * @param courseId The course ID
     * @return List of students enrolled in the course
     */
    public List<Student> getStudentsInCourse(String courseId) {
        return students.values().stream()
                .filter(student -> isEnrolledInCourse(student.getStudentId(), courseId))
                .collect(Collectors.toList());
    }
    
    // Statistical operations using Stream API
    
    /**
     * Get enrollment statistics by major using Stream API.
     * 
     * @return Map of major to student count
     */
    public Map<String, Long> getEnrollmentStatisticsByMajor() {
        return students.values().stream()
                .collect(Collectors.groupingBy(
                    Student::getMajor,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get enrollment statistics by academic year using Stream API.
     * 
     * @return Map of academic year to student count
     */
    public Map<AcademicYear, Long> getEnrollmentStatisticsByAcademicYear() {
        return students.values().stream()
                .collect(Collectors.groupingBy(
                    Student::getAcademicYear,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get average GPA by major using Stream API.
     * 
     * @return Map of major to average GPA
     */
    public Map<String, Double> getAverageGPAByMajor() {
        return students.values().stream()
                .collect(Collectors.groupingBy(
                    Student::getMajor,
                    Collectors.averagingDouble(student -> calculateGPA(student.getStudentId()))
                ));
    }
    
    /**
     * Get grade distribution statistics using Stream API.
     * 
     * @return Map of grade ranges to student counts
     */
    public Map<String, Long> getGradeDistribution() {
        return students.values().stream()
                .collect(Collectors.groupingBy(
                    student -> getGradeRange(calculateGPA(student.getStudentId())),
                    Collectors.counting()
                ));
    }
    
    /**
     * Calculate overall statistics using Stream API.
     * 
     * @return Map containing various statistical measures
     */
    public Map<String, Object> calculateOverallStatistics() {
        if (isStatisticsCacheValid()) {
            return new HashMap<>(cachedStatistics);
        }
        
        List<Double> gpas = students.values().stream()
                .mapToDouble(student -> calculateGPA(student.getStudentId()))
                .boxed()
                .collect(Collectors.toList());
        
        DoubleSummaryStatistics gpaStats = gpas.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalStudents", students.size());
        statistics.put("averageGPA", gpaStats.getAverage());
        statistics.put("minGPA", gpaStats.getMin());
        statistics.put("maxGPA", gpaStats.getMax());
        statistics.put("medianGPA", calculateMedian(gpas));
        statistics.put("standardDeviationGPA", calculateStandardDeviation(gpas));
        statistics.put("enrollmentByMajor", getEnrollmentStatisticsByMajor());
        statistics.put("enrollmentByYear", getEnrollmentStatisticsByAcademicYear());
        statistics.put("gradeDistribution", getGradeDistribution());
        statistics.put("activeStudents", getActiveStudentCount());
        statistics.put("totalEnrollments", getTotalEnrollmentCount());
        
        // Cache the results
        cachedStatistics = new HashMap<>(statistics);
        lastStatisticsUpdate = LocalDateTime.now();
        
        return statistics;
    }
    
    // GPA and grade management
    
    /**
     * Calculate GPA for a student.
     * 
     * @param studentId The student ID
     * @return The calculated GPA
     */
    public double calculateGPA(String studentId) {
        return calculatedGPAs.computeIfAbsent(studentId, this::computeGPA);
    }
    
    /**
     * Recalculate GPA for a student (force refresh).
     * 
     * @param studentId The student ID
     * @return The recalculated GPA
     */
    public double recalculateGPA(String studentId) {
        double gpa = computeGPA(studentId);
        calculatedGPAs.put(studentId, gpa);
        invalidateStatisticsCache();
        return gpa;
    }
    
    /**
     * Add enrollment for student.
     * 
     * @param studentId The student ID
     * @param enrollment The enrollment to add
     * @return true if enrollment was added successfully
     */
    public boolean addEnrollment(String studentId, Enrollment enrollment) {
        List<Enrollment> enrollments = studentEnrollments.get(studentId);
        if (enrollments != null && enrollment != null) {
            enrollments.add(enrollment);
            invalidateGPACache(studentId);
            return true;
        }
        return false;
    }
    
    /**
     * Add grade for student.
     * 
     * @param studentId The student ID
     * @param grade The grade to add
     * @return true if grade was added successfully
     */
    public boolean addGrade(String studentId, Grade grade) {
        List<Grade> grades = studentGrades.get(studentId);
        if (grades != null && grade != null) {
            grades.add(grade);
            invalidateGPACache(studentId);
            return true;
        }
        return false;
    }
    
    /**
     * Get enrollments for student.
     * 
     * @param studentId The student ID
     * @return List of enrollments for the student
     */
    public List<Enrollment> getStudentEnrollments(String studentId) {
        return studentEnrollments.getOrDefault(studentId, new ArrayList<>());
    }
    
    /**
     * Get grades for student.
     * 
     * @param studentId The student ID
     * @return List of grades for the student
     */
    public List<Grade> getStudentGrades(String studentId) {
        return studentGrades.getOrDefault(studentId, new ArrayList<>());
    }
    
    // Advanced filtering using functional programming
    
    /**
     * Filter students using custom predicate.
     * 
     * @param predicate The predicate to filter by
     * @return List of students matching the predicate
     */
    public List<Student> filterStudents(Predicate<Student> predicate) {
        return students.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Find students matching multiple criteria using Stream API.
     * 
     * @param criteria Map of field names to expected values
     * @return List of students matching all criteria
     */
    public List<Student> findStudentsByCriteria(Map<String, Object> criteria) {
        return students.values().stream()
                .filter(student -> matchesAllCriteria(student, criteria))
                .collect(Collectors.toList());
    }
    
    /**
     * Get students with honors (GPA >= 3.5) using method reference.
     * 
     * @return List of students with honors
     */
    public List<Student> getHonorsStudents() {
        return getStudentsWithGPAAbove(3.5);
    }
    
    /**
     * Get students on academic probation (GPA < 2.0) using Stream API.
     * 
     * @return List of students on academic probation
     */
    public List<Student> getStudentsOnProbation() {
        return students.values().stream()
                .filter(student -> calculateGPA(student.getStudentId()) < 2.0)
                .collect(Collectors.toList());
    }
    
    // Searchable interface implementation
    
    @Override
    public List<Student> search(String keyword) {
        return students.values().stream()
                .filter(student -> matchesKeyword(student, keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Student> search(Map<String, SearchCriterion> searchCriteria) {
        return students.values().stream()
                .filter(student -> matchesSearchCriteria(student, searchCriteria))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Student> search(Predicate<Student> predicate) {
        return students.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Student> searchAndSort(String keyword, String sortBy, SortOrder sortOrder) {
        Comparator<Student> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return search(keyword).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    @Override
    public SearchResult<Student> searchWithPagination(String keyword, int page, int pageSize) {
        List<Student> allResults = search(keyword);
        int totalElements = allResults.size();
        
        List<Student> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements);
    }
    
    @Override
    public SearchResult<Student> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria,
                                                             String sortBy, SortOrder sortOrder,
                                                             int page, int pageSize) {
        Comparator<Student> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        List<Student> allResults = search(searchCriteria).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        int totalElements = allResults.size();
        
        List<Student> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements, sortBy, sortOrder);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialInput, int maxSuggestions) {
        return students.values().stream()
                .flatMap(student -> Arrays.stream(new String[]{
                    student.getFirstName(),
                    student.getLastName(),
                    student.getEmail(),
                    student.getStudentId(),
                    student.getMajor()
                }))
                .filter(Objects::nonNull)
                .filter(field -> field.toLowerCase().contains(partialInput.toLowerCase()))
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Student> filter(Predicate<Student> predicate) {
        return filterStudents(predicate);
    }
    
    @Override
    public List<String> getSearchableFields() {
        return Arrays.asList("firstName", "lastName", "email", "studentId", "major", "academicYear");
    }
    
    @Override
    public List<String> getSortableFields() {
        return Arrays.asList("firstName", "lastName", "email", "studentId", "major", "academicYear", "gpa");
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
        String reportId = "RPT_" + System.currentTimeMillis();
        
        switch (reportType) {
            case ENROLLMENT_REPORT:
                return generateEnrollmentReport(reportId, parameters);
            case GRADE_REPORT:
                return generateGradeReport(reportId, parameters);
            case PERFORMANCE_REPORT:
                return generatePerformanceReport(reportId, parameters);
            case DEMOGRAPHIC_REPORT:
                return generateDemographicReport(reportId, parameters);
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
        // In a real implementation, write to file system
        return true; // Simplified for demo
    }
    
    @Override
    public List<ReportType> getAvailableReportTypes() {
        return Arrays.asList(
            ReportType.ENROLLMENT_REPORT,
            ReportType.GRADE_REPORT,
            ReportType.PERFORMANCE_REPORT,
            ReportType.DEMOGRAPHIC_REPORT,
            ReportType.STATISTICAL_SUMMARY
        );
    }
    
    @Override
    public List<ReportFormat> getSupportedFormats() {
        return Arrays.asList(ReportFormat.PDF, ReportFormat.CSV, ReportFormat.EXCEL, ReportFormat.JSON);
    }
    
    @Override
    public String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients) {
        // In a real implementation, schedule with a job scheduler
        return "SCHED_" + System.currentTimeMillis(); // Simplified for demo
    }
    
    @Override
    public boolean cancelScheduledReport(String scheduledReportId) {
        // In a real implementation, cancel scheduled job
        return true; // Simplified for demo
    }
    
    @Override
    public List<ReportMetadata> getReportHistory(ReportType reportType, int limit) {
        // In a real implementation, retrieve from report storage
        return new ArrayList<>(); // Simplified for demo
    }
    
    @Override
    public Map<String, Object> getSummaryStatistics() {
        return calculateOverallStatistics();
    }
    
    // Helper methods
    
    /**
     * Compute GPA for a student using Stream API.
     */
    private double computeGPA(String studentId) {
        List<Grade> grades = studentGrades.getOrDefault(studentId, new ArrayList<>());
        
        OptionalDouble averageGPA = grades.stream()
                .filter(grade -> grade.countsTowardFinalGrade())
                .filter(grade -> grade.getLetterGrade() != null)
                .mapToDouble(this::convertLetterGradeToPoints)
                .average();
        
        return averageGPA.orElse(0.0);
    }
    
    /**
     * Convert letter grade to GPA points.
     */
    private double convertLetterGradeToPoints(Grade grade) {
        String letter = grade.getLetterGrade();
        switch (letter) {
            case "A+": case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.3;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.3;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.3;
            case "D": return 1.0;
            case "D-": return 0.7;
            case "F": return 0.0;
            default: return 0.0;
        }
    }
    
    /**
     * Check if student has enrollment with specific status.
     */
    private boolean hasEnrollmentWithStatus(String studentId, Enrollment.EnrollmentStatus status) {
        return studentEnrollments.getOrDefault(studentId, new ArrayList<>()).stream()
                .anyMatch(enrollment -> enrollment.getStatus() == status);
    }
    
    /**
     * Check if student is enrolled in a specific course.
     */
    private boolean isEnrolledInCourse(String studentId, String courseId) {
        return studentEnrollments.getOrDefault(studentId, new ArrayList<>()).stream()
                .anyMatch(enrollment -> courseId.equals(enrollment.getCourseId()) &&
                         enrollment.getStatus() == Enrollment.EnrollmentStatus.ENROLLED);
    }
    
    /**
     * Get grade range for GPA.
     */
    private String getGradeRange(double gpa) {
        if (gpa >= 3.8) return "A (3.8-4.0)";
        else if (gpa >= 3.3) return "B+ (3.3-3.7)";
        else if (gpa >= 2.7) return "B (2.7-3.2)";
        else if (gpa >= 2.0) return "C (2.0-2.6)";
        else if (gpa >= 1.0) return "D (1.0-1.9)";
        else return "F (0.0-0.9)";
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
     * Get active student count.
     */
    private long getActiveStudentCount() {
        return students.values().stream()
                .filter(Student::isActive)
                .count();
    }
    
    /**
     * Get total enrollment count.
     */
    private long getTotalEnrollmentCount() {
        return studentEnrollments.values().stream()
                .mapToLong(List::size)
                .sum();
    }
    
    /**
     * Check if student matches keyword.
     */
    private boolean matchesKeyword(Student student, String keyword) {
        return student.getFirstName().toLowerCase().contains(keyword) ||
               student.getLastName().toLowerCase().contains(keyword) ||
               student.getEmail().toLowerCase().contains(keyword) ||
               student.getStudentId().toLowerCase().contains(keyword) ||
               student.getMajor().toLowerCase().contains(keyword) ||
               student.getAcademicYear().toString().toLowerCase().contains(keyword);
    }
    
    /**
     * Check if student matches search criteria.
     */
    private boolean matchesSearchCriteria(Student student, Map<String, SearchCriterion> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> matchesCriterion(student, entry.getKey(), entry.getValue()));
    }
    
    /**
     * Check if student matches a specific criterion.
     */
    private boolean matchesCriterion(Student student, String field, SearchCriterion criterion) {
        Object fieldValue = getFieldValue(student, field);
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
     * Get field value from student using reflection-like approach.
     */
    private Object getFieldValue(Student student, String field) {
        switch (field.toLowerCase()) {
            case "firstname": return student.getFirstName();
            case "lastname": return student.getLastName();
            case "email": return student.getEmail();
            case "studentid": return student.getStudentId();
            case "major": return student.getMajor();
            case "academicyear": return student.getAcademicYear();
            case "gpa": return calculateGPA(student.getStudentId());
            default: return null;
        }
    }
    
    /**
     * Check if student matches all criteria.
     */
    private boolean matchesAllCriteria(Student student, Map<String, Object> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> Objects.equals(getFieldValue(student, entry.getKey()), entry.getValue()));
    }
    
    /**
     * Get comparator for sorting.
     */
    private Comparator<Student> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "firstname":
                return Comparator.comparing(Student::getFirstName);
            case "lastname":
                return Comparator.comparing(Student::getLastName);
            case "email":
                return Comparator.comparing(Student::getEmail);
            case "studentid":
                return Comparator.comparing(Student::getStudentId);
            case "major":
                return Comparator.comparing(Student::getMajor);
            case "academicyear":
                return Comparator.comparing(Student::getAcademicYear);
            case "gpa":
                return Comparator.comparingDouble(student -> calculateGPA(student.getStudentId()));
            default:
                return Comparator.comparing(Student::getLastName);
        }
    }
    
    /**
     * Generate enrollment report.
     */
    private ReportData generateEnrollmentReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Student ID", "Name", "Major", "Academic Year", "Enrollments", "Status");
        
        List<Map<String, Object>> rows = students.values().stream()
                .map(student -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Student ID", student.getStudentId());
                    row.put("Name", student.getFullName());
                    row.put("Major", student.getMajor());
                    row.put("Academic Year", student.getAcademicYear().toString());
                    row.put("Enrollments", getStudentEnrollments(student.getStudentId()).size());
                    row.put("Status", student.isActive() ? "Active" : "Inactive");
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.ENROLLMENT_REPORT, "Student Enrollment Report", 
                            columns, rows, Map.of("totalStudents", students.size()));
    }
    
    /**
     * Generate grade report.
     */
    private ReportData generateGradeReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Student ID", "Name", "Major", "GPA", "Total Credits", "Grade Level");
        
        List<Map<String, Object>> rows = students.values().stream()
                .map(student -> {
                    Map<String, Object> row = new HashMap<>();
                    double gpa = calculateGPA(student.getStudentId());
                    row.put("Student ID", student.getStudentId());
                    row.put("Name", student.getFullName());
                    row.put("Major", student.getMajor());
                    row.put("GPA", String.format("%.2f", gpa));
                    row.put("Total Credits", getStudentGrades(student.getStudentId()).size());
                    row.put("Grade Level", getGradeRange(gpa));
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.GRADE_REPORT, "Student Grade Report", 
                            columns, rows, calculateOverallStatistics());
    }
    
    /**
     * Generate performance report.
     */
    private ReportData generatePerformanceReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        
        List<String> columns = Arrays.asList("Metric", "Value");
        List<Map<String, Object>> rows = statistics.entrySet().stream()
                .map(entry -> Map.of("Metric", entry.getKey(), "Value", entry.getValue().toString()))
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.PERFORMANCE_REPORT, "Student Performance Report", 
                            columns, rows, statistics);
    }
    
    /**
     * Generate demographic report.
     */
    private ReportData generateDemographicReport(String reportId, Map<String, Object> parameters) {
        Map<String, Long> majorStats = getEnrollmentStatisticsByMajor();
        Map<AcademicYear, Long> yearStats = getEnrollmentStatisticsByAcademicYear();
        
        List<String> columns = Arrays.asList("Category", "Value", "Count", "Percentage");
        List<Map<String, Object>> rows = new ArrayList<>();
        
        // Add major statistics
        long totalStudents = students.size();
        majorStats.forEach((major, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("Category", "Major");
            row.put("Value", major);
            row.put("Count", count);
            row.put("Percentage", String.format("%.1f%%", (count * 100.0) / totalStudents));
            rows.add(row);
        });
        
        // Add year statistics
        yearStats.forEach((year, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("Category", "Academic Year");
            row.put("Value", year.toString());
            row.put("Count", count);
            row.put("Percentage", String.format("%.1f%%", (count * 100.0) / totalStudents));
            rows.add(row);
        });
        
        return new ReportData(reportId, ReportType.DEMOGRAPHIC_REPORT, "Student Demographic Report", 
                            columns, rows, Map.of("totalStudents", totalStudents));
    }
    
    /**
     * Generate statistical report.
     */
    private ReportData generateStatisticalReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        String content = statistics.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, "Student Statistical Summary", content);
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
    
    /**
     * Invalidate GPA cache for a student.
     */
    private void invalidateGPACache(String studentId) {
        calculatedGPAs.remove(studentId);
        invalidateStatisticsCache();
    }
}