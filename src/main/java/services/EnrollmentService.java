// File: src/main/java/services/EnrollmentService.java
package services;

import models.Enrollment;
import models.Enrollment.EnrollmentStatus;
import models.Enrollment.EnrollmentType;
import models.Student;
import models.Course;
import interfaces.Enrollable;
import interfaces.Searchable;
import interfaces.Reportable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EnrollmentService class providing enrollment management operations.
 * This service manages student course enrollments, waitlists, and enrollment policies.
 * 
 * Key Java concepts demonstrated:
 * - Complex business logic implementation
 * - Stream API for enrollment processing
 * - Functional programming with predicates
 * - Interface implementations
 * - Thread-safe operations
 * - Enrollment workflow management
 * - Statistical analysis of enrollment data
 */
public class EnrollmentService implements Enrollable, Searchable<Enrollment>, Reportable {
    
    // Instance fields
    private final Map<String, Enrollment> enrollments;
    private final Map<String, List<String>> studentEnrollments; // studentId -> enrollmentIds
    private final Map<String, List<String>> courseEnrollments; // courseId -> enrollmentIds
    private final Map<String, List<String>> waitlists; // courseId -> enrollmentIds
    private final Map<String, Integer> courseLimits; // courseId -> enrollment limit
    private final Map<String, Set<String>> prerequisites; // courseId -> prerequisite courseIds
    
    // Enrollment policies
    private final int maxEnrollmentsPerStudent = 6;
    private final int maxWaitlistSize = 20;
    private final boolean allowPrerequisiteOverride = false;
    
    // Statistics cache
    private volatile Map<String, Object> cachedStatistics;
    private volatile LocalDateTime lastStatisticsUpdate;
    
    /**
     * Constructor initializing the service.
     */
    public EnrollmentService() {
        this.enrollments = new ConcurrentHashMap<>();
        this.studentEnrollments = new ConcurrentHashMap<>();
        this.courseEnrollments = new ConcurrentHashMap<>();
        this.waitlists = new ConcurrentHashMap<>();
        this.courseLimits = new ConcurrentHashMap<>();
        this.prerequisites = new ConcurrentHashMap<>();
        this.cachedStatistics = new HashMap<>();
        this.lastStatisticsUpdate = LocalDateTime.now();
    }
    
    // Core enrollment operations
    
    @Override
    public boolean enrollStudent(String studentId, String courseId, String semester, int year) {
        // Validation checks
        if (!ValidationUtil.isValidString(studentId) || !ValidationUtil.isValidString(courseId)) {
            return false;
        }
        
        // Check enrollment eligibility
        if (!canEnrollStudent(studentId, courseId)) {
            return false;
        }
        
        // Create enrollment
        Enrollment enrollment = Enrollment.createEnrollment(studentId, courseId, semester, year);
        enrollment.setEnrolledBy("SYSTEM");
        
        // Store enrollment
        enrollments.put(enrollment.getEnrollmentId(), enrollment);
        
        // Update indices
        studentEnrollments.computeIfAbsent(studentId, k -> new ArrayList<>()).add(enrollment.getEnrollmentId());
        courseEnrollments.computeIfAbsent(courseId, k -> new ArrayList<>()).add(enrollment.getEnrollmentId());
        
        invalidateStatisticsCache();
        return true;
    }
    
    @Override
    public boolean dropStudent(String studentId, String courseId, String reason) {
        return findActiveEnrollment(studentId, courseId)
                .map(enrollment -> {
                    enrollment.dropEnrollment(reason);
                    
                    // Remove from course enrollments
                    List<String> courseEnrollmentIds = courseEnrollments.get(courseId);
                    if (courseEnrollmentIds != null) {
                        courseEnrollmentIds.remove(enrollment.getEnrollmentId());
                    }
                    
                    // Process waitlist
                    processWaitlist(courseId, 1);
                    
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    @Override
    public boolean addToWaitlist(String studentId, String courseId, String semester, int year) {
        // Validation checks
        if (!ValidationUtil.isValidString(studentId) || !ValidationUtil.isValidString(courseId)) {
            return false;
        }
        
        // Check if already enrolled or waitlisted
        if (isStudentEnrolled(studentId, courseId) || isStudentWaitlisted(studentId, courseId)) {
            return false;
        }
        
        // Check waitlist capacity
        List<String> waitlistIds = waitlists.getOrDefault(courseId, new ArrayList<>());
        if (waitlistIds.size() >= maxWaitlistSize) {
            return false;
        }
        
        // Create waitlist enrollment
        Enrollment enrollment = Enrollment.createWaitlistedEnrollment(studentId, courseId, semester, year);
        enrollment.setEnrolledBy("SYSTEM");
        
        // Store enrollment
        enrollments.put(enrollment.getEnrollmentId(), enrollment);
        
        // Update indices
        studentEnrollments.computeIfAbsent(studentId, k -> new ArrayList<>()).add(enrollment.getEnrollmentId());
        waitlists.computeIfAbsent(courseId, k -> new ArrayList<>()).add(enrollment.getEnrollmentId());
        
        invalidateStatisticsCache();
        return true;
    }
    
    @Override
    public boolean removeFromWaitlist(String studentId, String courseId) {
        return findWaitlistEnrollment(studentId, courseId)
                .map(enrollment -> {
                    enrollment.dropEnrollment("Removed from waitlist");
                    
                    // Remove from waitlist
                    List<String> waitlistIds = waitlists.get(courseId);
                    if (waitlistIds != null) {
                        waitlistIds.remove(enrollment.getEnrollmentId());
                    }
                    
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    @Override
    public int processWaitlist(String courseId, int numberOfStudents) {
        List<String> waitlistIds = waitlists.getOrDefault(courseId, new ArrayList<>());
        if (waitlistIds.isEmpty()) {
            return 0;
        }
        
        int enrolled = 0;
        Iterator<String> iterator = waitlistIds.iterator();
        
        while (iterator.hasNext() && enrolled < numberOfStudents) {
            String enrollmentId = iterator.next();
            Enrollment enrollment = enrollments.get(enrollmentId);
            
            if (enrollment != null && hasAvailableSpots(courseId)) {
                // Move from waitlist to enrolled
                enrollment.enrollFromWaitlist();
                
                // Update indices
                iterator.remove(); // Remove from waitlist
                courseEnrollments.computeIfAbsent(courseId, k -> new ArrayList<>()).add(enrollmentId);
                
                enrolled++;
            }
        }
        
        if (enrolled > 0) {
            invalidateStatisticsCache();
        }
        
        return enrolled;
    }
    
    @Override
    public boolean isStudentEnrolled(String studentId, String courseId) {
        return findActiveEnrollment(studentId, courseId).isPresent();
    }
    
    @Override
    public boolean isStudentWaitlisted(String studentId, String courseId) {
        return findWaitlistEnrollment(studentId, courseId).isPresent();
    }
    
    @Override
    public List<Enrollment> getStudentEnrollments(String studentId) {
        return studentEnrollments.getOrDefault(studentId, new ArrayList<>()).stream()
                .map(enrollments::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Enrollment> getCourseEnrollments(String courseId) {
        return courseEnrollments.getOrDefault(courseId, new ArrayList<>()).stream()
                .map(enrollments::get)
                .filter(Objects::nonNull)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .collect(Collectors.toList());
    }
    
    @Override
    public int getCurrentEnrollmentCount(String courseId) {
        return getCourseEnrollments(courseId).size();
    }
    
    @Override
    public int getCurrentWaitlistCount(String courseId) {
        return waitlists.getOrDefault(courseId, new ArrayList<>()).size();
    }
    
    @Override
    public boolean hasAvailableSpots(String courseId) {
        int limit = getMaxEnrollmentCapacity(courseId);
        int current = getCurrentEnrollmentCount(courseId);
        return current < limit;
    }
    
    @Override
    public int getMaxEnrollmentCapacity(String courseId) {
        return courseLimits.getOrDefault(courseId, 30); // Default capacity
    }
    
    @Override
    public boolean transferStudent(String studentId, String fromCourseId, String toCourseId, String semester, int year) {
        // Check if student is enrolled in source course
        if (!isStudentEnrolled(studentId, fromCourseId)) {
            return false;
        }
        
        // Check if target course has availability
        if (!hasAvailableSpots(toCourseId)) {
            return false;
        }
        
        // Perform transfer
        boolean dropped = dropStudent(studentId, fromCourseId, "Transfer to " + toCourseId);
        if (dropped) {
            boolean enrolled = enrollStudent(studentId, toCourseId, semester, year);
            if (!enrolled) {
                // Rollback: re-enroll in original course
                enrollStudent(studentId, fromCourseId, semester, year);
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public int bulkEnrollStudents(List<String> studentIds, String courseId, String semester, int year) {
        return studentIds.stream()
                .mapToInt(studentId -> enrollStudent(studentId, courseId, semester, year) ? 1 : 0)
                .sum();
    }
    
    @Override
    public EnrollmentStatistics getEnrollmentStatistics(String courseId) {
        List<Enrollment> allEnrollments = getAllCourseEnrollments(courseId);
        
        int enrolled = (int) allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        
        int waitlisted = getCurrentWaitlistCount(courseId);
        
        int dropped = (int) allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.DROPPED)
                .count();
        
        int completed = (int) allEnrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        
        int capacity = getMaxEnrollmentCapacity(courseId);
        
        return new EnrollmentStatistics(enrolled, waitlisted, dropped, completed, capacity);
    }
    
    // Additional enrollment management methods
    
    /**
     * Set course enrollment limit.
     * 
     * @param courseId The course ID
     * @param limit The enrollment limit
     * @return true if limit was set successfully
     */
    public boolean setCourseLimit(String courseId, int limit) {
        if (ValidationUtil.isValidString(courseId) && limit > 0) {
            courseLimits.put(courseId, limit);
            return true;
        }
        return false;
    }
    
    /**
     * Add course prerequisite.
     * 
     * @param courseId The course ID
     * @param prerequisiteCourseId The prerequisite course ID
     * @return true if prerequisite was added successfully
     */
    public boolean addPrerequisite(String courseId, String prerequisiteCourseId) {
        if (ValidationUtil.isValidString(courseId) && ValidationUtil.isValidString(prerequisiteCourseId)) {
            prerequisites.computeIfAbsent(courseId, k -> new HashSet<>()).add(prerequisiteCourseId);
            return true;
        }
        return false;
    }
    
    /**
     * Remove course prerequisite.
     * 
     * @param courseId The course ID
     * @param prerequisiteCourseId The prerequisite course ID to remove
     * @return true if prerequisite was removed successfully
     */
    public boolean removePrerequisite(String courseId, String prerequisiteCourseId) {
        Set<String> coursePrereqs = prerequisites.get(courseId);
        return coursePrereqs != null && coursePrereqs.remove(prerequisiteCourseId);
    }
    
    /**
     * Get course prerequisites.
     * 
     * @param courseId The course ID
     * @return Set of prerequisite course IDs
     */
    public Set<String> getCoursePrerequisites(String courseId) {
        return new HashSet<>(prerequisites.getOrDefault(courseId, new HashSet<>()));
    }
    
    /**
     * Check if student meets prerequisites for a course.
     * 
     * @param studentId The student ID
     * @param courseId The course ID
     * @return true if prerequisites are met
     */
    public boolean checkPrerequisites(String studentId, String courseId) {
        if (allowPrerequisiteOverride) {
            return true;
        }
        
        Set<String> requiredCourses = getCoursePrerequisites(courseId);
        if (requiredCourses.isEmpty()) {
            return true;
        }
        
        Set<String> completedCourses = getCompletedCourses(studentId);
        return completedCourses.containsAll(requiredCourses);
    }
    
    /**
     * Get courses completed by student.
     * 
     * @param studentId The student ID
     * @return Set of completed course IDs
     */
    public Set<String> getCompletedCourses(String studentId) {
        return getStudentEnrollments(studentId).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .map(Enrollment::getCourseId)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get enrollment by ID.
     * 
     * @param enrollmentId The enrollment ID
     * @return Optional containing the enrollment if found
     */
    public Optional<Enrollment> getEnrollmentById(String enrollmentId) {
        return Optional.ofNullable(enrollments.get(enrollmentId));
    }
    
    /**
     * Update enrollment status.
     * 
     * @param enrollmentId The enrollment ID
     * @param status The new status
     * @return true if status was updated successfully
     */
    public boolean updateEnrollmentStatus(String enrollmentId, EnrollmentStatus status) {
        return getEnrollmentById(enrollmentId)
                .map(enrollment -> {
                    enrollment.setStatus(status);
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Withdraw student from course.
     * 
     * @param studentId The student ID
     * @param courseId The course ID
     * @param reason The withdrawal reason
     * @return true if withdrawal was successful
     */
    public boolean withdrawStudent(String studentId, String courseId, String reason) {
        return findActiveEnrollment(studentId, courseId)
                .map(enrollment -> {
                    enrollment.withdrawFromCourse(reason);
                    invalidateStatisticsCache();
                    return true;
                })
                .orElse(false);
    }
    
    // Advanced enrollment queries using Stream API
    
    /**
     * Get enrollments by status using Stream API.
     * 
     * @param status The enrollment status
     * @return List of enrollments with the specified status
     */
    public List<Enrollment> getEnrollmentsByStatus(EnrollmentStatus status) {
        return enrollments.values().stream()
                .filter(enrollment -> enrollment.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * Get enrollments by semester using Stream API.
     * 
     * @param semester The semester
     * @param year The year
     * @return List of enrollments for the specified semester
     */
    public List<Enrollment> getEnrollmentsBySemester(String semester, int year) {
        return enrollments.values().stream()
                .filter(enrollment -> semester.equals(enrollment.getSemester()) && year == enrollment.getYear())
                .collect(Collectors.toList());
    }
    
    /**
     * Get enrollments by type using Stream API.
     * 
     * @param enrollmentType The enrollment type
     * @return List of enrollments of the specified type
     */
    public List<Enrollment> getEnrollmentsByType(EnrollmentType enrollmentType) {
        return enrollments.values().stream()
                .filter(enrollment -> enrollment.getEnrollmentType() == enrollmentType)
                .collect(Collectors.toList());
    }
    
    /**
     * Get overenrolled courses using Stream API.
     * 
     * @return List of course IDs that are over their enrollment limit
     */
    public List<String> getOverenrolledCourses() {
        return courseLimits.entrySet().stream()
                .filter(entry -> getCurrentEnrollmentCount(entry.getKey()) > entry.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Get students with maximum enrollments using Stream API.
     * 
     * @return List of student IDs who have reached maximum enrollment limit
     */
    public List<String> getStudentsWithMaxEnrollments() {
        return studentEnrollments.entrySet().stream()
                .filter(entry -> getActiveEnrollmentCount(entry.getKey()) >= maxEnrollmentsPerStudent)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    // Statistical operations using Stream API
    
    /**
     * Get enrollment statistics by status using Stream API.
     * 
     * @return Map of enrollment status to count
     */
    public Map<EnrollmentStatus, Long> getEnrollmentStatisticsByStatus() {
        return enrollments.values().stream()
                .collect(Collectors.groupingBy(
                    Enrollment::getStatus,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get enrollment statistics by semester using Stream API.
     * 
     * @return Map of semester to enrollment count
     */
    public Map<String, Long> getEnrollmentStatisticsBySemester() {
        return enrollments.values().stream()
                .collect(Collectors.groupingBy(
                    enrollment -> enrollment.getSemester() + " " + enrollment.getYear(),
                    Collectors.counting()
                ));
    }
    
    /**
     * Get enrollment rate by course using Stream API.
     * 
     * @return Map of course ID to enrollment rate percentage
     */
    public Map<String, Double> getEnrollmentRateByCourse() {
        return courseLimits.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        int enrolled = getCurrentEnrollmentCount(entry.getKey());
                        int capacity = entry.getValue();
                        return capacity > 0 ? (enrolled * 100.0) / capacity : 0.0;
                    }
                ));
    }
    
    /**
     * Calculate overall enrollment statistics using Stream API.
     * 
     * @return Map containing various statistical measures
     */
    public Map<String, Object> calculateOverallStatistics() {
        if (isStatisticsCacheValid()) {
            return new HashMap<>(cachedStatistics);
        }
        
        Map<String, Object> statistics = new HashMap<>();
        
        statistics.put("totalEnrollments", enrollments.size());
        statistics.put("activeEnrollments", getActiveEnrollmentCount());
        statistics.put("enrollmentsByStatus", getEnrollmentStatisticsByStatus());
        statistics.put("enrollmentsBySemester", getEnrollmentStatisticsBySemester());
        statistics.put("enrollmentRateByCourse", getEnrollmentRateByCourse());
        statistics.put("averageEnrollmentRate", calculateAverageEnrollmentRate());
        statistics.put("totalWaitlistCount", getTotalWaitlistCount());
        statistics.put("overenrolledCourses", getOverenrolledCourses().size());
        statistics.put("studentsWithMaxEnrollments", getStudentsWithMaxEnrollments().size());
        statistics.put("completionRate", calculateCompletionRate());
        statistics.put("dropRate", calculateDropRate());
        
        // Cache the results
        cachedStatistics = new HashMap<>(statistics);
        lastStatisticsUpdate = LocalDateTime.now();
        
        return statistics;
    }
    
    // Searchable interface implementation
    
    @Override
    public List<Enrollment> search(String keyword) {
        return enrollments.values().stream()
                .filter(enrollment -> matchesKeyword(enrollment, keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Enrollment> search(Map<String, SearchCriterion> searchCriteria) {
        return enrollments.values().stream()
                .filter(enrollment -> matchesSearchCriteria(enrollment, searchCriteria))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Enrollment> search(Predicate<Enrollment> predicate) {
        return enrollments.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Enrollment> searchAndSort(String keyword, String sortBy, SortOrder sortOrder) {
        Comparator<Enrollment> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return search(keyword).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    @Override
    public SearchResult<Enrollment> searchWithPagination(String keyword, int page, int pageSize) {
        List<Enrollment> allResults = search(keyword);
        int totalElements = allResults.size();
        
        List<Enrollment> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements);
    }
    
    @Override
    public SearchResult<Enrollment> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria,
                                                                String sortBy, SortOrder sortOrder,
                                                                int page, int pageSize) {
        Comparator<Enrollment> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        List<Enrollment> allResults = search(searchCriteria).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        int totalElements = allResults.size();
        
        List<Enrollment> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements, sortBy, sortOrder);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialInput, int maxSuggestions) {
        return enrollments.values().stream()
                .flatMap(enrollment -> Arrays.stream(new String[]{
                    enrollment.getStudentId(),
                    enrollment.getCourseId(),
                    enrollment.getSemester(),
                    enrollment.getStatus().toString(),
                    enrollment.getEnrollmentType().toString()
                }))
                .filter(Objects::nonNull)
                .filter(field -> field.toLowerCase().contains(partialInput.toLowerCase()))
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Enrollment> filter(Predicate<Enrollment> predicate) {
        return search(predicate);
    }
    
    @Override
    public List<String> getSearchableFields() {
        return Arrays.asList("studentId", "courseId", "semester", "year", "status", "enrollmentType", "grade");
    }
    
    @Override
    public List<String> getSortableFields() {
        return Arrays.asList("enrollmentDate", "studentId", "courseId", "semester", "year", "status", "grade");
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
        String reportId = "RPT_ENR_" + System.currentTimeMillis();
        
        switch (reportType) {
            case ENROLLMENT_REPORT:
                return generateEnrollmentReport(reportId, parameters);
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
            ReportType.ENROLLMENT_REPORT,
            ReportType.STATISTICAL_SUMMARY
        );
    }
    
    @Override
    public List<ReportFormat> getSupportedFormats() {
        return Arrays.asList(ReportFormat.PDF, ReportFormat.CSV, ReportFormat.EXCEL, ReportFormat.JSON);
    }
    
    @Override
    public String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients) {
        return "SCHED_ENR_" + System.currentTimeMillis(); // Simplified for demo
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
     * Check if student can enroll in course.
     */
    private boolean canEnrollStudent(String studentId, String courseId) {
        // Check if already enrolled
        if (isStudentEnrolled(studentId, courseId)) {
            return false;
        }
        
        // Check enrollment limit
        if (getActiveEnrollmentCount(studentId) >= maxEnrollmentsPerStudent) {
            return false;
        }
        
        // Check course capacity
        if (!hasAvailableSpots(courseId)) {
            return false;
        }
        
        // Check prerequisites
        if (!checkPrerequisites(studentId, courseId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Find active enrollment for student in course.
     */
    private Optional<Enrollment> findActiveEnrollment(String studentId, String courseId) {
        return getStudentEnrollments(studentId).stream()
                .filter(enrollment -> courseId.equals(enrollment.getCourseId()))
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .findFirst();
    }
    
    /**
     * Find waitlist enrollment for student in course.
     */
    private Optional<Enrollment> findWaitlistEnrollment(String studentId, String courseId) {
        return getStudentEnrollments(studentId).stream()
                .filter(enrollment -> courseId.equals(enrollment.getCourseId()))
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.WAITLISTED)
                .findFirst();
    }
    
    /**
     * Get all course enrollments (including dropped, completed, etc.).
     */
    private List<Enrollment> getAllCourseEnrollments(String courseId) {
        List<String> allEnrollmentIds = new ArrayList<>();
        allEnrollmentIds.addAll(courseEnrollments.getOrDefault(courseId, new ArrayList<>()));
        allEnrollmentIds.addAll(waitlists.getOrDefault(courseId, new ArrayList<>()));
        
        return allEnrollmentIds.stream()
                .map(enrollments::get)
                .filter(Objects::nonNull)
                .filter(enrollment -> courseId.equals(enrollment.getCourseId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get active enrollment count for student.
     */
    private int getActiveEnrollmentCount(String studentId) {
        return (int) getStudentEnrollments(studentId).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
    }
    
    /**
     * Get total active enrollment count.
     */
    private long getActiveEnrollmentCount() {
        return enrollments.values().stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
    }
    
    /**
     * Calculate average enrollment rate across all courses.
     */
    private double calculateAverageEnrollmentRate() {
        return getEnrollmentRateByCourse().values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Get total waitlist count across all courses.
     */
    private int getTotalWaitlistCount() {
        return waitlists.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Calculate completion rate.
     */
    private double calculateCompletionRate() {
        long completed = enrollments.values().stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        
        long total = enrollments.size();
        return total > 0 ? (completed * 100.0) / total : 0.0;
    }
    
    /**
     * Calculate drop rate.
     */
    private double calculateDropRate() {
        long dropped = enrollments.values().stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.DROPPED)
                .count();
        
        long total = enrollments.size();
        return total > 0 ? (dropped * 100.0) / total : 0.0;
    }
    
    /**
     * Check if enrollment matches keyword.
     */
    private boolean matchesKeyword(Enrollment enrollment, String keyword) {
        return enrollment.getStudentId().toLowerCase().contains(keyword) ||
               enrollment.getCourseId().toLowerCase().contains(keyword) ||
               enrollment.getSemester().toLowerCase().contains(keyword) ||
               enrollment.getStatus().toString().toLowerCase().contains(keyword) ||
               enrollment.getEnrollmentType().toString().toLowerCase().contains(keyword);
    }
    
    /**
     * Check if enrollment matches search criteria.
     */
    private boolean matchesSearchCriteria(Enrollment enrollment, Map<String, SearchCriterion> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> matchesCriterion(enrollment, entry.getKey(), entry.getValue()));
    }
    
    /**
     * Check if enrollment matches a specific criterion.
     */
    private boolean matchesCriterion(Enrollment enrollment, String field, SearchCriterion criterion) {
        Object fieldValue = getFieldValue(enrollment, field);
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
     * Get field value from enrollment.
     */
    private Object getFieldValue(Enrollment enrollment, String field) {
        switch (field.toLowerCase()) {
            case "studentid": return enrollment.getStudentId();
            case "courseid": return enrollment.getCourseId();
            case "semester": return enrollment.getSemester();
            case "year": return enrollment.getYear();
            case "status": return enrollment.getStatus();
            case "enrollmenttype": return enrollment.getEnrollmentType();
            case "grade": return enrollment.getGrade();
            case "enrollmentdate": return enrollment.getEnrollmentDate();
            default: return null;
        }
    }
    
    /**
     * Get comparator for sorting.
     */
    private Comparator<Enrollment> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "enrollmentdate":
                return Comparator.comparing(Enrollment::getEnrollmentDate);
            case "studentid":
                return Comparator.comparing(Enrollment::getStudentId);
            case "courseid":
                return Comparator.comparing(Enrollment::getCourseId);
            case "semester":
                return Comparator.comparing(Enrollment::getSemester);
            case "year":
                return Comparator.comparing(Enrollment::getYear);
            case "status":
                return Comparator.comparing(Enrollment::getStatus);
            default:
                return Comparator.comparing(Enrollment::getEnrollmentDate);
        }
    }
    
    /**
     * Generate enrollment report.
     */
    private ReportData generateEnrollmentReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Enrollment ID", "Student ID", "Course ID", "Semester", "Status", "Type", "Grade", "Date");
        
        List<Map<String, Object>> rows = enrollments.values().stream()
                .map(enrollment -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Enrollment ID", enrollment.getEnrollmentId());
                    row.put("Student ID", enrollment.getStudentId());
                    row.put("Course ID", enrollment.getCourseId());
                    row.put("Semester", enrollment.getSemester() + " " + enrollment.getYear());
                    row.put("Status", enrollment.getStatus().toString());
                    row.put("Type", enrollment.getEnrollmentType().toString());
                    row.put("Grade", enrollment.getGrade() != null ? enrollment.getGrade().getLetter() : "N/A");
                    row.put("Date", enrollment.getEnrollmentDate());
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.ENROLLMENT_REPORT, "Enrollment Report", 
                            columns, rows, Map.of("totalEnrollments", enrollments.size()));
    }
    
    /**
     * Generate statistical report.
     */
    private ReportData generateStatisticalReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        String content = statistics.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, "Enrollment Statistical Summary", content);
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