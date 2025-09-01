// File: src/main/java/services/ProfessorService.java
package services;

import models.Professor;
import models.Professor.AcademicRank;
import models.Professor.EmploymentStatus;
import models.Course;
import models.Student;
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
 * ProfessorService class providing professor-related business operations.
 * This service manages professor data, course assignments, and academic operations.
 * 
 * Key Java concepts demonstrated:
 * - Advanced Stream API operations
 * - Lambda expressions and method references
 * - Functional interface composition
 * - Optional chaining for null safety
 * - Statistical calculations with collectors
 * - Complex data transformations
 * - Thread-safe operations with concurrent collections
 */
public class ProfessorService implements Searchable<Professor>, Reportable {
    
    // Instance fields
    private final Map<String, Professor> professors;
    private final Map<String, List<String>> professorCourses; // professorId -> courseIds
    private final Map<String, List<String>> professorStudents; // professorId -> studentIds
    private final Map<String, Map<String, Grade>> professorGrades; // professorId -> courseId -> grades
    private final Map<String, Double> teachingRatings;
    private final Map<String, Integer> researchPublications;
    
    // Statistics cache
    private volatile Map<String, Object> cachedStatistics;
    private volatile LocalDateTime lastStatisticsUpdate;
    
    /**
     * Constructor initializing the service.
     */
    public ProfessorService() {
        this.professors = new ConcurrentHashMap<>();
        this.professorCourses = new ConcurrentHashMap<>();
        this.professorStudents = new ConcurrentHashMap<>();
        this.professorGrades = new ConcurrentHashMap<>();
        this.teachingRatings = new ConcurrentHashMap<>();
        this.researchPublications = new ConcurrentHashMap<>();
        this.cachedStatistics = new HashMap<>();
        this.lastStatisticsUpdate = LocalDateTime.now();
    }
    
    // Core CRUD operations
    
    /**
     * Add a new professor.
     * 
     * @param professor The professor to add
     * @return true if professor was added successfully, false otherwise
     */
    public boolean addProfessor(Professor professor) {
        if (professor == null || !ValidationUtil.isValidString(professor.getProfessorId())) {
            return false;
        }
        
        if (professors.containsKey(professor.getProfessorId())) {
            return false; // Professor already exists
        }
        
        professors.put(professor.getProfessorId(), professor);
        professorCourses.put(professor.getProfessorId(), new ArrayList<>());
        professorStudents.put(professor.getProfessorId(), new ArrayList<>());
        professorGrades.put(professor.getProfessorId(), new HashMap<>());
        teachingRatings.put(professor.getProfessorId(), professor.getTeachingRating());
        researchPublications.put(professor.getProfessorId(), 0);
        invalidateStatisticsCache();
        
        return true;
    }
    
    /**
     * Update an existing professor.
     * 
     * @param professor The updated professor data
     * @return true if professor was updated successfully, false otherwise
     */
    public boolean updateProfessor(Professor professor) {
        if (professor == null || !professors.containsKey(professor.getProfessorId())) {
            return false;
        }
        
        professors.put(professor.getProfessorId(), professor);
        teachingRatings.put(professor.getProfessorId(), professor.getTeachingRating());
        invalidateStatisticsCache();
        return true;
    }
    
    /**
     * Remove a professor.
     * 
     * @param professorId The ID of the professor to remove
     * @return true if professor was removed successfully, false otherwise
     */
    public boolean removeProfessor(String professorId) {
        if (!ValidationUtil.isValidString(professorId)) {
            return false;
        }
        
        Professor removed = professors.remove(professorId);
        if (removed != null) {
            professorCourses.remove(professorId);
            professorStudents.remove(professorId);
            professorGrades.remove(professorId);
            teachingRatings.remove(professorId);
            researchPublications.remove(professorId);
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Get professor by ID.
     * 
     * @param professorId The professor ID
     * @return Optional containing the professor if found
     */
    public Optional<Professor> getProfessorById(String professorId) {
        return Optional.ofNullable(professors.get(professorId));
    }
    
    /**
     * Get all professors.
     * 
     * @return List of all professors
     */
    public List<Professor> getAllProfessors() {
        return new ArrayList<>(professors.values());
    }
    
    // Advanced query operations using Stream API
    
    /**
     * Get professors by department using Stream API.
     * 
     * @param departmentId The department ID to filter by
     * @return List of professors in the specified department
     */
    public List<Professor> getProfessorsByDepartment(String departmentId) {
        return professors.values().stream()
                .filter(professor -> departmentId.equals(professor.getDepartmentId()))
                .sorted(Comparator.comparing(Professor::getLastName)
                       .thenComparing(Professor::getFirstName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get professors by academic rank using Stream API.
     * 
     * @param academicRank The academic rank to filter by
     * @return List of professors with the specified rank
     */
    public List<Professor> getProfessorsByRank(AcademicRank academicRank) {
        return professors.values().stream()
                .filter(professor -> academicRank.equals(professor.getAcademicRank()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get tenured professors using Stream API.
     * 
     * @return List of tenured professors
     */
    public List<Professor> getTenuredProfessors() {
        return professors.values().stream()
                .filter(Professor::isTenured)
                .collect(Collectors.toList());
    }
    
    /**
     * Get professors with teaching rating above threshold using Stream API.
     * 
     * @param ratingThreshold The minimum rating threshold
     * @return List of professors with rating above the threshold
     */
    public List<Professor> getProfessorsWithRatingAbove(double ratingThreshold) {
        return professors.values().stream()
                .filter(professor -> professor.getTeachingRating() >= ratingThreshold)
                .sorted(Comparator.comparingDouble(Professor::getTeachingRating).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get top N professors by teaching rating using Stream API.
     * 
     * @param n Number of top professors to return
     * @return List of top N professors by teaching rating
     */
    public List<Professor> getTopProfessorsByRating(int n) {
        return professors.values().stream()
                .sorted(Comparator.comparingDouble(Professor::getTeachingRating).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * Get professors by years of experience range using Stream API.
     * 
     * @param minYears Minimum years of experience
     * @param maxYears Maximum years of experience
     * @return List of professors within the experience range
     */
    public List<Professor> getProfessorsByExperienceRange(int minYears, int maxYears) {
        return professors.values().stream()
                .filter(professor -> professor.getYearsOfExperience() >= minYears &&
                                   professor.getYearsOfExperience() <= maxYears)
                .collect(Collectors.toList());
    }
    
    /**
     * Get professors by research area using Stream API.
     * 
     * @param researchArea The research area to filter by
     * @return List of professors in the specified research area
     */
    public List<Professor> getProfessorsByResearchArea(String researchArea) {
        return professors.values().stream()
                .filter(professor -> researchArea.equalsIgnoreCase(professor.getResearchArea()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get professors teaching specific course using Stream API.
     * 
     * @param courseId The course ID
     * @return List of professors teaching the course
     */
    public List<Professor> getProfessorsTeachingCourse(String courseId) {
        return professors.values().stream()
                .filter(professor -> isTeachingCourse(professor.getProfessorId(), courseId))
                .collect(Collectors.toList());
    }
    
    // Course and student management
    
    /**
     * Assign course to professor.
     * 
     * @param professorId The professor ID
     * @param courseId The course ID to assign
     * @return true if course was assigned successfully
     */
    public boolean assignCourse(String professorId, String courseId) {
        List<String> courses = professorCourses.get(professorId);
        if (courses != null && ValidationUtil.isValidString(courseId) && !courses.contains(courseId)) {
            courses.add(courseId);
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Remove course assignment from professor.
     * 
     * @param professorId The professor ID
     * @param courseId The course ID to remove
     * @return true if course was removed successfully
     */
    public boolean removeCourseAssignment(String professorId, String courseId) {
        List<String> courses = professorCourses.get(professorId);
        if (courses != null && courses.remove(courseId)) {
            // Remove associated grades
            Map<String, Grade> professorGradeMap = professorGrades.get(professorId);
            if (professorGradeMap != null) {
                professorGradeMap.remove(courseId);
            }
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Get courses assigned to professor.
     * 
     * @param professorId The professor ID
     * @return List of course IDs assigned to the professor
     */
    public List<String> getProfessorCourses(String professorId) {
        return professorCourses.getOrDefault(professorId, new ArrayList<>());
    }
    
    /**
     * Add student to professor's class.
     * 
     * @param professorId The professor ID
     * @param studentId The student ID to add
     * @return true if student was added successfully
     */
    public boolean addStudent(String professorId, String studentId) {
        List<String> students = professorStudents.get(professorId);
        if (students != null && ValidationUtil.isValidString(studentId) && !students.contains(studentId)) {
            students.add(studentId);
            return true;
        }
        return false;
    }
    
    /**
     * Remove student from professor's class.
     * 
     * @param professorId The professor ID
     * @param studentId The student ID to remove
     * @return true if student was removed successfully
     */
    public boolean removeStudent(String professorId, String studentId) {
        List<String> students = professorStudents.get(professorId);
        return students != null && students.remove(studentId);
    }
    
    /**
     * Get students taught by professor.
     * 
     * @param professorId The professor ID
     * @return List of student IDs taught by the professor
     */
    public List<String> getProfessorStudents(String professorId) {
        return professorStudents.getOrDefault(professorId, new ArrayList<>());
    }
    
    // Statistical operations using Stream API
    
    /**
     * Get teaching statistics by department using Stream API.
     * 
     * @return Map of department to professor count
     */
    public Map<String, Long> getTeachingStatisticsByDepartment() {
        return professors.values().stream()
                .collect(Collectors.groupingBy(
                    Professor::getDepartmentId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get professor distribution by rank using Stream API.
     * 
     * @return Map of academic rank to professor count
     */
    public Map<AcademicRank, Long> getProfessorDistributionByRank() {
        return professors.values().stream()
                .collect(Collectors.groupingBy(
                    Professor::getAcademicRank,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get average teaching rating by department using Stream API.
     * 
     * @return Map of department to average teaching rating
     */
    public Map<String, Double> getAverageRatingByDepartment() {
        return professors.values().stream()
                .collect(Collectors.groupingBy(
                    Professor::getDepartmentId,
                    Collectors.averagingDouble(Professor::getTeachingRating)
                ));
    }
    
    /**
     * Get experience distribution using Stream API.
     * 
     * @return Map of experience ranges to professor counts
     */
    public Map<String, Long> getExperienceDistribution() {
        return professors.values().stream()
                .collect(Collectors.groupingBy(
                    professor -> getExperienceRange(professor.getYearsOfExperience()),
                    Collectors.counting()
                ));
    }
    
    /**
     * Get workload statistics using Stream API.
     * 
     * @return Map of professor ID to course count
     */
    public Map<String, Integer> getWorkloadStatistics() {
        return professorCourses.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ));
    }
    
    /**
     * Calculate overall professor statistics using Stream API.
     * 
     * @return Map containing various statistical measures
     */
    public Map<String, Object> calculateOverallStatistics() {
        if (isStatisticsCacheValid()) {
            return new HashMap<>(cachedStatistics);
        }
        
        List<Double> ratings = professors.values().stream()
                .mapToDouble(Professor::getTeachingRating)
                .boxed()
                .collect(Collectors.toList());
        
        DoubleSummaryStatistics ratingStats = ratings.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalProfessors", professors.size());
        statistics.put("averageRating", ratingStats.getAverage());
        statistics.put("minRating", ratingStats.getMin());
        statistics.put("maxRating", ratingStats.getMax());
        statistics.put("medianRating", calculateMedian(ratings));
        statistics.put("tenuredCount", getTenuredCount());
        statistics.put("professorsByDepartment", getTeachingStatisticsByDepartment());
        statistics.put("professorsByRank", getProfessorDistributionByRank());
        statistics.put("averageRatingByDepartment", getAverageRatingByDepartment());
        statistics.put("experienceDistribution", getExperienceDistribution());
        statistics.put("workloadStatistics", getWorkloadStatistics());
        statistics.put("averageExperience", calculateAverageExperience());
        statistics.put("totalCourseAssignments", getTotalCourseAssignments());
        
        // Cache the results
        cachedStatistics = new HashMap<>(statistics);
        lastStatisticsUpdate = LocalDateTime.now();
        
        return statistics;
    }
    
    // Advanced filtering using functional programming
    
    /**
     * Filter professors using custom predicate.
     * 
     * @param predicate The predicate to filter by
     * @return List of professors matching the predicate
     */
    public List<Professor> filterProfessors(Predicate<Professor> predicate) {
        return professors.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Find professors matching multiple criteria using Stream API.
     * 
     * @param criteria Map of field names to expected values
     * @return List of professors matching all criteria
     */
    public List<Professor> findProfessorsByCriteria(Map<String, Object> criteria) {
        return professors.values().stream()
                .filter(professor -> matchesAllCriteria(professor, criteria))
                .collect(Collectors.toList());
    }
    
    /**
     * Get senior professors (Associate Professor and above) using method reference.
     * 
     * @return List of senior professors
     */
    public List<Professor> getSeniorProfessors() {
        return professors.values().stream()
                .filter(professor -> professor.getAcademicRank() == AcademicRank.ASSOCIATE ||
                                   professor.getAcademicRank() == AcademicRank.FULL ||
                                   professor.getAcademicRank() == AcademicRank.DISTINGUISHED)
                .collect(Collectors.toList());
    }
    
    /**
     * Get professors eligible for tenure using Stream API.
     * 
     * @return List of professors eligible for tenure (Assistant+ with 6+ years experience)
     */
    public List<Professor> getTenureEligibleProfessors() {
        return professors.values().stream()
                .filter(professor -> !professor.isTenured() &&
                                   professor.getYearsOfExperience() >= 6 &&
                                   professor.getAcademicRank() != AcademicRank.INSTRUCTOR &&
                                   professor.getAcademicRank() != AcademicRank.LECTURER)
                .collect(Collectors.toList());
    }
    
    // Grading operations
    
    /**
     * Add grade for a professor's course.
     * 
     * @param professorId The professor ID
     * @param courseId The course ID
     * @param grade The grade to add
     * @return true if grade was added successfully
     */
    public boolean addGrade(String professorId, String courseId, Grade grade) {
        Map<String, Grade> courseGrades = professorGrades.get(professorId);
        if (courseGrades != null && ValidationUtil.isValidString(courseId) && grade != null) {
            courseGrades.put(courseId + "_" + grade.getGradeId(), grade);
            return true;
        }
        return false;
    }
    
    /**
     * Get grades for professor's course.
     * 
     * @param professorId The professor ID
     * @param courseId The course ID
     * @return List of grades for the course
     */
    public List<Grade> getCourseGrades(String professorId, String courseId) {
        Map<String, Grade> courseGrades = professorGrades.get(professorId);
        if (courseGrades != null) {
            return courseGrades.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(courseId + "_"))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    /**
     * Calculate average grade for professor's course.
     * 
     * @param professorId The professor ID
     * @param courseId The course ID
     * @return Average grade percentage
     */
    public OptionalDouble calculateCourseAverageGrade(String professorId, String courseId) {
        return getCourseGrades(professorId, courseId).stream()
                .filter(grade -> grade.getPercentage() >= 0)
                .mapToDouble(Grade::getPercentage)
                .average();
    }
    
    // Research and publication management
    
    /**
     * Add research publication for professor.
     * 
     * @param professorId The professor ID
     * @param publicationCount Number of publications to add
     * @return true if publications were added successfully
     */
    public boolean addResearchPublications(String professorId, int publicationCount) {
        if (professors.containsKey(professorId) && publicationCount > 0) {
            researchPublications.merge(professorId, publicationCount, Integer::sum);
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Get research publication count for professor.
     * 
     * @param professorId The professor ID
     * @return Number of publications
     */
    public int getResearchPublicationCount(String professorId) {
        return researchPublications.getOrDefault(professorId, 0);
    }
    
    /**
     * Update teaching rating for professor.
     * 
     * @param professorId The professor ID
     * @param newRating The new teaching rating
     * @return true if rating was updated successfully
     */
    public boolean updateTeachingRating(String professorId, double newRating) {
        if (professors.containsKey(professorId) && newRating >= 0.0 && newRating <= 5.0) {
            teachingRatings.put(professorId, newRating);
            
            // Update professor object as well
            Professor professor = professors.get(professorId);
            professor.setTeachingRating(newRating);
            
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    // Searchable interface implementation
    
    @Override
    public List<Professor> search(String keyword) {
        return professors.values().stream()
                .filter(professor -> matchesKeyword(professor, keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Professor> search(Map<String, SearchCriterion> searchCriteria) {
        return professors.values().stream()
                .filter(professor -> matchesSearchCriteria(professor, searchCriteria))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Professor> search(Predicate<Professor> predicate) {
        return professors.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Professor> searchAndSort(String keyword, String sortBy, SortOrder sortOrder) {
        Comparator<Professor> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return search(keyword).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    @Override
    public SearchResult<Professor> searchWithPagination(String keyword, int page, int pageSize) {
        List<Professor> allResults = search(keyword);
        int totalElements = allResults.size();
        
        List<Professor> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements);
    }
    
    @Override
    public SearchResult<Professor> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria,
                                                               String sortBy, SortOrder sortOrder,
                                                               int page, int pageSize) {
        Comparator<Professor> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        List<Professor> allResults = search(searchCriteria).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        int totalElements = allResults.size();
        
        List<Professor> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements, sortBy, sortOrder);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialInput, int maxSuggestions) {
        return professors.values().stream()
                .flatMap(professor -> Arrays.stream(new String[]{
                    professor.getFirstName(),
                    professor.getLastName(),
                    professor.getEmail(),
                    professor.getProfessorId(),
                    professor.getDepartmentId(),
                    professor.getResearchArea(),
                    professor.getAcademicRank().toString()
                }))
                .filter(Objects::nonNull)
                .filter(field -> field.toLowerCase().contains(partialInput.toLowerCase()))
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Professor> filter(Predicate<Professor> predicate) {
        return filterProfessors(predicate);
    }
    
    @Override
    public List<String> getSearchableFields() {
        return Arrays.asList("firstName", "lastName", "email", "professorId", "departmentId", 
                           "academicRank", "researchArea", "teachingRating", "yearsOfExperience");
    }
    
    @Override
    public List<String> getSortableFields() {
        return Arrays.asList("firstName", "lastName", "email", "professorId", "departmentId", 
                           "academicRank", "teachingRating", "yearsOfExperience", "tenured");
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
        String reportId = "RPT_PROF_" + System.currentTimeMillis();
        
        switch (reportType) {
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
        return "SCHED_PROF_" + System.currentTimeMillis(); // Simplified for demo
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
     * Check if professor is teaching a specific course.
     */
    private boolean isTeachingCourse(String professorId, String courseId) {
        List<String> courses = professorCourses.get(professorId);
        return courses != null && courses.contains(courseId);
    }
    
    /**
     * Get experience range for years of experience.
     */
    private String getExperienceRange(int years) {
        if (years < 5) return "0-4 years";
        else if (years < 10) return "5-9 years";
        else if (years < 15) return "10-14 years";
        else if (years < 20) return "15-19 years";
        else return "20+ years";
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
     * Get tenured professor count.
     */
    private long getTenuredCount() {
        return professors.values().stream()
                .filter(Professor::isTenured)
                .count();
    }
    
    /**
     * Calculate average experience.
     */
    private double calculateAverageExperience() {
        return professors.values().stream()
                .mapToInt(Professor::getYearsOfExperience)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Get total course assignments.
     */
    private int getTotalCourseAssignments() {
        return professorCourses.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Check if professor matches keyword.
     */
    private boolean matchesKeyword(Professor professor, String keyword) {
        return professor.getFirstName().toLowerCase().contains(keyword) ||
               professor.getLastName().toLowerCase().contains(keyword) ||
               professor.getEmail().toLowerCase().contains(keyword) ||
               professor.getProfessorId().toLowerCase().contains(keyword) ||
               professor.getDepartmentId().toLowerCase().contains(keyword) ||
               (professor.getResearchArea() != null && professor.getResearchArea().toLowerCase().contains(keyword)) ||
               professor.getAcademicRank().toString().toLowerCase().contains(keyword);
    }
    
    /**
     * Check if professor matches search criteria.
     */
    private boolean matchesSearchCriteria(Professor professor, Map<String, SearchCriterion> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> matchesCriterion(professor, entry.getKey(), entry.getValue()));
    }
    
    /**
     * Check if professor matches a specific criterion.
     */
    private boolean matchesCriterion(Professor professor, String field, SearchCriterion criterion) {
        Object fieldValue = getFieldValue(professor, field);
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
     * Get field value from professor.
     */
    private Object getFieldValue(Professor professor, String field) {
        switch (field.toLowerCase()) {
            case "firstname": return professor.getFirstName();
            case "lastname": return professor.getLastName();
            case "email": return professor.getEmail();
            case "professorid": return professor.getProfessorId();
            case "departmentid": return professor.getDepartmentId();
            case "academicrank": return professor.getAcademicRank();
            case "researcharea": return professor.getResearchArea();
            case "teachingrating": return professor.getTeachingRating();
            case "yearsofexperience": return professor.getYearsOfExperience();
            case "tenured": return professor.isTenured();
            default: return null;
        }
    }
    
    /**
     * Check if professor matches all criteria.
     */
    private boolean matchesAllCriteria(Professor professor, Map<String, Object> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> Objects.equals(getFieldValue(professor, entry.getKey()), entry.getValue()));
    }
    
    /**
     * Get comparator for sorting.
     */
    private Comparator<Professor> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "firstname":
                return Comparator.comparing(Professor::getFirstName);
            case "lastname":
                return Comparator.comparing(Professor::getLastName);
            case "email":
                return Comparator.comparing(Professor::getEmail);
            case "professorid":
                return Comparator.comparing(Professor::getProfessorId);
            case "departmentid":
                return Comparator.comparing(Professor::getDepartmentId);
            case "academicrank":
                return Comparator.comparing(Professor::getAcademicRank);
            case "teachingrating":
                return Comparator.comparingDouble(Professor::getTeachingRating);
            case "yearsofexperience":
                return Comparator.comparingInt(Professor::getYearsOfExperience);
            case "tenured":
                return Comparator.comparing(Professor::isTenured);
            default:
                return Comparator.comparing(Professor::getLastName);
        }
    }
    
    /**
     * Generate performance report.
     */
    private ReportData generatePerformanceReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Professor ID", "Name", "Department", "Rank", "Rating", "Experience", "Courses", "Publications");
        
        List<Map<String, Object>> rows = professors.values().stream()
                .map(professor -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Professor ID", professor.getProfessorId());
                    row.put("Name", professor.getFullName());
                    row.put("Department", professor.getDepartmentId());
                    row.put("Rank", professor.getAcademicRank().toString());
                    row.put("Rating", String.format("%.2f", professor.getTeachingRating()));
                    row.put("Experience", professor.getYearsOfExperience() + " years");
                    row.put("Courses", getProfessorCourses(professor.getProfessorId()).size());
                    row.put("Publications", getResearchPublicationCount(professor.getProfessorId()));
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.PERFORMANCE_REPORT, "Professor Performance Report", 
                            columns, rows, calculateOverallStatistics());
    }
    
    /**
     * Generate demographic report.
     */
    private ReportData generateDemographicReport(String reportId, Map<String, Object> parameters) {
        Map<String, Long> departmentStats = getTeachingStatisticsByDepartment();
        Map<AcademicRank, Long> rankStats = getProfessorDistributionByRank();
        
        List<String> columns = Arrays.asList("Category", "Value", "Count", "Percentage");
        List<Map<String, Object>> rows = new ArrayList<>();
        
        // Add department statistics
        long totalProfessors = professors.size();
        departmentStats.forEach((department, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("Category", "Department");
            row.put("Value", department);
            row.put("Count", count);
            row.put("Percentage", String.format("%.1f%%", (count * 100.0) / totalProfessors));
            rows.add(row);
        });
        
        // Add rank statistics
        rankStats.forEach((rank, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("Category", "Academic Rank");
            row.put("Value", rank.toString());
            row.put("Count", count);
            row.put("Percentage", String.format("%.1f%%", (count * 100.0) / totalProfessors));
            rows.add(row);
        });
        
        return new ReportData(reportId, ReportType.DEMOGRAPHIC_REPORT, "Professor Demographic Report", 
                            columns, rows, Map.of("totalProfessors", totalProfessors));
    }
    
    /**
     * Generate statistical report.
     */
    private ReportData generateStatisticalReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        String content = statistics.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, "Professor Statistical Summary", content);
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