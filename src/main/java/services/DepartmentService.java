// File: src/main/java/services/DepartmentService.java
package services;

import models.Department;
import models.Student;
import models.Professor;
import models.Course;
import interfaces.Searchable;
import interfaces.Reportable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DepartmentService class providing department-related business operations.
 * This service manages departments and their relationships with students, professors, and courses.
 * 
 * Key Java concepts demonstrated:
 * - Service layer architecture
 * - Stream API for complex data operations
 * - Functional programming with lambdas
 * - Interface implementations
 * - Concurrent collections for thread safety
 * - Statistical analysis with streams
 * - Business logic encapsulation
 */
public class DepartmentService implements Searchable<Department>, Reportable {
    
    // Instance fields
    private final Map<String, Department> departments;
    private final Map<String, List<String>> departmentStudents; // departmentId -> studentIds
    private final Map<String, List<String>> departmentProfessors; // departmentId -> professorIds
    private final Map<String, List<String>> departmentCourses; // departmentId -> courseIds
    private final Map<String, Map<String, Object>> departmentBudgets; // departmentId -> budget info
    
    // Statistics cache
    private volatile Map<String, Object> cachedStatistics;
    private volatile LocalDateTime lastStatisticsUpdate;
    
    /**
     * Constructor initializing the service.
     */
    public DepartmentService() {
        this.departments = new ConcurrentHashMap<>();
        this.departmentStudents = new ConcurrentHashMap<>();
        this.departmentProfessors = new ConcurrentHashMap<>();
        this.departmentCourses = new ConcurrentHashMap<>();
        this.departmentBudgets = new ConcurrentHashMap<>();
        this.cachedStatistics = new HashMap<>();
        this.lastStatisticsUpdate = LocalDateTime.now();
    }
    
    // Core CRUD operations
    
    /**
     * Add a new department.
     * 
     * @param department The department to add
     * @return true if department was added successfully, false otherwise
     */
    public boolean addDepartment(Department department) {
        if (department == null || !ValidationUtil.isValidString(department.getDepartmentId())) {
            return false;
        }
        
        if (departments.containsKey(department.getDepartmentId())) {
            return false; // Department already exists
        }
        
        departments.put(department.getDepartmentId(), department);
        departmentStudents.put(department.getDepartmentId(), new ArrayList<>());
        departmentProfessors.put(department.getDepartmentId(), new ArrayList<>());
        departmentCourses.put(department.getDepartmentId(), new ArrayList<>());
        departmentBudgets.put(department.getDepartmentId(), createDefaultBudget());
        invalidateStatisticsCache();
        
        return true;
    }
    
    /**
     * Update an existing department.
     * 
     * @param department The updated department data
     * @return true if department was updated successfully, false otherwise
     */
    public boolean updateDepartment(Department department) {
        if (department == null || !departments.containsKey(department.getDepartmentId())) {
            return false;
        }
        
        departments.put(department.getDepartmentId(), department);
        invalidateStatisticsCache();
        return true;
    }
    
    /**
     * Remove a department.
     * 
     * @param departmentId The ID of the department to remove
     * @return true if department was removed successfully, false otherwise
     */
    public boolean removeDepartment(String departmentId) {
        if (!ValidationUtil.isValidString(departmentId)) {
            return false;
        }
        
        Department removed = departments.remove(departmentId);
        if (removed != null) {
            departmentStudents.remove(departmentId);
            departmentProfessors.remove(departmentId);
            departmentCourses.remove(departmentId);
            departmentBudgets.remove(departmentId);
            invalidateStatisticsCache();
            return true;
        }
        return false;
    }
    
    /**
     * Get department by ID.
     * 
     * @param departmentId The department ID
     * @return Optional containing the department if found
     */
    public Optional<Department> getDepartmentById(String departmentId) {
        return Optional.ofNullable(departments.get(departmentId));
    }
    
    /**
     * Get all departments.
     * 
     * @return List of all departments
     */
    public List<Department> getAllDepartments() {
        return new ArrayList<>(departments.values());
    }
    
    // Advanced query operations using Stream API
    
    /**
     * Get active departments using Stream API.
     * 
     * @return List of active departments
     */
    public List<Department> getActiveDepartments() {
        return departments.values().stream()
                .filter(Department::isActive)
                .sorted(Comparator.comparing(Department::getDepartmentName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get departments by type using Stream API.
     * 
     * @param type The department type to filter by
     * @return List of departments of the specified type
     */
    public List<Department> getDepartmentsByType(Department.DepartmentType type) {
        return departments.values().stream()
                .filter(department -> type.equals(department.getType()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get departments with enrollment above threshold using Stream API.
     * 
     * @param threshold The minimum enrollment threshold
     * @return List of departments with enrollment above the threshold
     */
    public List<Department> getDepartmentsWithEnrollmentAbove(int threshold) {
        return departments.values().stream()
                .filter(department -> department.getTotalEnrollment() >= threshold)
                .sorted(Comparator.comparingInt(Department::getTotalEnrollment).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get largest departments by enrollment using Stream API.
     * 
     * @param n Number of largest departments to return
     * @return List of top N departments by enrollment
     */
    public List<Department> getLargestDepartments(int n) {
        return departments.values().stream()
                .sorted(Comparator.comparingInt(Department::getTotalEnrollment).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }
    
    /**
     * Get departments offering graduate programs using Stream API.
     * 
     * @return List of departments with graduate programs
     */
    public List<Department> getDepartmentsWithGraduatePrograms() {
        return departments.values().stream()
                .filter(department -> !department.getGraduatePrograms().isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Get departments by college using Stream API.
     * 
     * @param college The college name to filter by
     * @return List of departments in the specified college
     */
    public List<Department> getDepartmentsByCollege(String college) {
        return departments.values().stream()
                .filter(department -> college.equalsIgnoreCase(department.getCollege()))
                .sorted(Comparator.comparing(Department::getDepartmentName))
                .collect(Collectors.toList());
    }
    
    // Department management operations
    
    /**
     * Add student to department.
     * 
     * @param departmentId The department ID
     * @param studentId The student ID to add
     * @return true if student was added successfully
     */
    public boolean addStudent(String departmentId, String studentId) {
        List<String> students = departmentStudents.get(departmentId);
        if (students != null && ValidationUtil.isValidString(studentId) && !students.contains(studentId)) {
            students.add(studentId);
            updateDepartmentEnrollment(departmentId);
            return true;
        }
        return false;
    }
    
    /**
     * Remove student from department.
     * 
     * @param departmentId The department ID
     * @param studentId The student ID to remove
     * @return true if student was removed successfully
     */
    public boolean removeStudent(String departmentId, String studentId) {
        List<String> students = departmentStudents.get(departmentId);
        if (students != null && students.remove(studentId)) {
            updateDepartmentEnrollment(departmentId);
            return true;
        }
        return false;
    }
    
    /**
     * Get students in department.
     * 
     * @param departmentId The department ID
     * @return List of student IDs in the department
     */
    public List<String> getDepartmentStudents(String departmentId) {
        return departmentStudents.getOrDefault(departmentId, new ArrayList<>());
    }
    
    /**
     * Add professor to department.
     * 
     * @param departmentId The department ID
     * @param professorId The professor ID to add
     * @return true if professor was added successfully
     */
    public boolean addProfessor(String departmentId, String professorId) {
        List<String> professors = departmentProfessors.get(departmentId);
        if (professors != null && ValidationUtil.isValidString(professorId) && !professors.contains(professorId)) {
            professors.add(professorId);
            updateDepartmentProfessorCount(departmentId);
            return true;
        }
        return false;
    }
    
    /**
     * Remove professor from department.
     * 
     * @param departmentId The department ID
     * @param professorId The professor ID to remove
     * @return true if professor was removed successfully
     */
    public boolean removeProfessor(String departmentId, String professorId) {
        List<String> professors = departmentProfessors.get(departmentId);
        if (professors != null && professors.remove(professorId)) {
            updateDepartmentProfessorCount(departmentId);
            return true;
        }
        return false;
    }
    
    /**
     * Get professors in department.
     * 
     * @param departmentId The department ID
     * @return List of professor IDs in the department
     */
    public List<String> getDepartmentProfessors(String departmentId) {
        return departmentProfessors.getOrDefault(departmentId, new ArrayList<>());
    }
    
    /**
     * Add course to department.
     * 
     * @param departmentId The department ID
     * @param courseId The course ID to add
     * @return true if course was added successfully
     */
    public boolean addCourse(String departmentId, String courseId) {
        List<String> courses = departmentCourses.get(departmentId);
        if (courses != null && ValidationUtil.isValidString(courseId) && !courses.contains(courseId)) {
            courses.add(courseId);
            updateDepartmentCourseCount(departmentId);
            return true;
        }
        return false;
    }
    
    /**
     * Remove course from department.
     * 
     * @param departmentId The department ID
     * @param courseId The course ID to remove
     * @return true if course was removed successfully
     */
    public boolean removeCourse(String departmentId, String courseId) {
        List<String> courses = departmentCourses.get(departmentId);
        if (courses != null && courses.remove(courseId)) {
            updateDepartmentCourseCount(departmentId);
            return true;
        }
        return false;
    }
    
    /**
     * Get courses in department.
     * 
     * @param departmentId The department ID
     * @return List of course IDs in the department
     */
    public List<String> getDepartmentCourses(String departmentId) {
        return departmentCourses.getOrDefault(departmentId, new ArrayList<>());
    }
    
    // Budget management
    
    /**
     * Set department budget.
     * 
     * @param departmentId The department ID
     * @param budget The budget amount
     * @param fiscalYear The fiscal year
     * @return true if budget was set successfully
     */
    public boolean setDepartmentBudget(String departmentId, double budget, int fiscalYear) {
        if (departments.containsKey(departmentId) && budget >= 0) {
            Map<String, Object> budgetInfo = departmentBudgets.get(departmentId);
            budgetInfo.put("totalBudget", budget);
            budgetInfo.put("fiscalYear", fiscalYear);
            budgetInfo.put("lastUpdated", LocalDateTime.now());
            return true;
        }
        return false;
    }
    
    /**
     * Get department budget.
     * 
     * @param departmentId The department ID
     * @return Optional containing budget information
     */
    public Optional<Map<String, Object>> getDepartmentBudget(String departmentId) {
        return Optional.ofNullable(departmentBudgets.get(departmentId));
    }
    
    /**
     * Allocate budget for category.
     * 
     * @param departmentId The department ID
     * @param category The budget category
     * @param amount The amount to allocate
     * @return true if allocation was successful
     */
    public boolean allocateBudget(String departmentId, String category, double amount) {
        Map<String, Object> budgetInfo = departmentBudgets.get(departmentId);
        if (budgetInfo != null && amount >= 0) {
            @SuppressWarnings("unchecked")
            Map<String, Double> allocations = (Map<String, Double>) budgetInfo.computeIfAbsent("allocations", k -> new HashMap<String, Double>());
            allocations.put(category, amount);
            return true;
        }
        return false;
    }
    
    // Statistical operations using Stream API
    
    /**
     * Get enrollment statistics by department type using Stream API.
     * 
     * @return Map of department type to total enrollment
     */
    public Map<Department.DepartmentType, Integer> getEnrollmentStatisticsByType() {
        return departments.values().stream()
                .collect(Collectors.groupingBy(
                    Department::getType,
                    Collectors.summingInt(Department::getTotalEnrollment)
                ));
    }
    
    /**
     * Get department distribution by college using Stream API.
     * 
     * @return Map of college to department count
     */
    public Map<String, Long> getDepartmentDistributionByCollege() {
        return departments.values().stream()
                .collect(Collectors.groupingBy(
                    Department::getCollege,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get average enrollment by department type using Stream API.
     * 
     * @return Map of department type to average enrollment
     */
    public Map<Department.DepartmentType, Double> getAverageEnrollmentByType() {
        return departments.values().stream()
                .collect(Collectors.groupingBy(
                    Department::getType,
                    Collectors.averagingDouble(Department::getTotalEnrollment)
                ));
    }
    
    /**
     * Calculate faculty-to-student ratios using Stream API.
     * 
     * @return Map of department ID to faculty-to-student ratio
     */
    public Map<String, Double> getFacultyToStudentRatios() {
        return departments.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> calculateFacultyToStudentRatio(entry.getKey())
                ));
    }
    
    /**
     * Get program offering statistics using Stream API.
     * 
     * @return Map containing program statistics
     */
    public Map<String, Object> getProgramOfferingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUndergraduatePrograms = departments.values().stream()
                .mapToLong(dept -> dept.getUndergraduatePrograms().size())
                .sum();
        
        long totalGraduatePrograms = departments.values().stream()
                .mapToLong(dept -> dept.getGraduatePrograms().size())
                .sum();
        
        Map<String, Long> programsByType = departments.values().stream()
                .collect(Collectors.groupingBy(
                    dept -> dept.getType().toString(),
                    Collectors.summingLong(dept -> dept.getUndergraduatePrograms().size() + dept.getGraduatePrograms().size())
                ));
        
        stats.put("totalUndergraduatePrograms", totalUndergraduatePrograms);
        stats.put("totalGraduatePrograms", totalGraduatePrograms);
        stats.put("programsByDepartmentType", programsByType);
        
        return stats;
    }
    
    /**
     * Calculate overall department statistics using Stream API.
     * 
     * @return Map containing various statistical measures
     */
    public Map<String, Object> calculateOverallStatistics() {
        if (isStatisticsCacheValid()) {
            return new HashMap<>(cachedStatistics);
        }
        
        List<Department> activeDepartments = getActiveDepartments();
        
        IntSummaryStatistics enrollmentStats = activeDepartments.stream()
                .mapToInt(Department::getTotalEnrollment)
                .summaryStatistics();
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalDepartments", departments.size());
        statistics.put("activeDepartments", activeDepartments.size());
        statistics.put("totalEnrollment", enrollmentStats.getSum());
        statistics.put("averageEnrollment", enrollmentStats.getAverage());
        statistics.put("maxEnrollment", enrollmentStats.getMax());
        statistics.put("minEnrollment", enrollmentStats.getMin());
        statistics.put("departmentsByType", getDepartmentCountByType());
        statistics.put("departmentsByCollege", getDepartmentDistributionByCollege());
        statistics.put("enrollmentByType", getEnrollmentStatisticsByType());
        statistics.put("averageEnrollmentByType", getAverageEnrollmentByType());
        statistics.put("facultyToStudentRatios", getFacultyToStudentRatios());
        statistics.put("programStatistics", getProgramOfferingStatistics());
        statistics.put("totalBudget", getTotalBudget());
        statistics.put("averageBudget", getAverageBudget());
        
        // Cache the results
        cachedStatistics = new HashMap<>(statistics);
        lastStatisticsUpdate = LocalDateTime.now();
        
        return statistics;
    }
    
    // Advanced filtering using functional programming
    
    /**
     * Filter departments using custom predicate.
     * 
     * @param predicate The predicate to filter by
     * @return List of departments matching the predicate
     */
    public List<Department> filterDepartments(Predicate<Department> predicate) {
        return departments.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Find departments matching multiple criteria using Stream API.
     * 
     * @param criteria Map of field names to expected values
     * @return List of departments matching all criteria
     */
    public List<Department> findDepartmentsByCriteria(Map<String, Object> criteria) {
        return departments.values().stream()
                .filter(department -> matchesAllCriteria(department, criteria))
                .collect(Collectors.toList());
    }
    
    /**
     * Get departments needing attention (low enrollment, budget issues, etc.).
     * 
     * @return List of departments that may need administrative attention
     */
    public List<Department> getDepartmentsNeedingAttention() {
        return departments.values().stream()
                .filter(department -> department.getTotalEnrollment() < 50 || // Low enrollment
                                    getDepartmentProfessors(department.getDepartmentId()).size() < 3 || // Few professors
                                    !department.isActive()) // Inactive
                .collect(Collectors.toList());
    }
    
    // Searchable interface implementation
    
    @Override
    public List<Department> search(String keyword) {
        return departments.values().stream()
                .filter(department -> matchesKeyword(department, keyword.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Department> search(Map<String, SearchCriterion> searchCriteria) {
        return departments.values().stream()
                .filter(department -> matchesSearchCriteria(department, searchCriteria))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Department> search(Predicate<Department> predicate) {
        return departments.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Department> searchAndSort(String keyword, String sortBy, SortOrder sortOrder) {
        Comparator<Department> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return search(keyword).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    @Override
    public SearchResult<Department> searchWithPagination(String keyword, int page, int pageSize) {
        List<Department> allResults = search(keyword);
        int totalElements = allResults.size();
        
        List<Department> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements);
    }
    
    @Override
    public SearchResult<Department> advancedSearchWithPagination(Map<String, SearchCriterion> searchCriteria,
                                                                String sortBy, SortOrder sortOrder,
                                                                int page, int pageSize) {
        Comparator<Department> comparator = getComparator(sortBy);
        if (sortOrder == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        List<Department> allResults = search(searchCriteria).stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        int totalElements = allResults.size();
        
        List<Department> pageResults = allResults.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return new SearchResult<>(pageResults, page, pageSize, totalElements, sortBy, sortOrder);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialInput, int maxSuggestions) {
        return departments.values().stream()
                .flatMap(department -> Arrays.stream(new String[]{
                    department.getDepartmentName(),
                    department.getDepartmentCode(),
                    department.getDepartmentId(),
                    department.getCollege(),
                    department.getType().toString()
                }))
                .filter(Objects::nonNull)
                .filter(field -> field.toLowerCase().contains(partialInput.toLowerCase()))
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Department> filter(Predicate<Department> predicate) {
        return filterDepartments(predicate);
    }
    
    @Override
    public List<String> getSearchableFields() {
        return Arrays.asList("departmentName", "departmentCode", "departmentId", "college", 
                           "type", "totalEnrollment", "active");
    }
    
    @Override
    public List<String> getSortableFields() {
        return Arrays.asList("departmentName", "departmentCode", "college", "type", 
                           "totalEnrollment", "active");
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
        String reportId = "RPT_DEPT_" + System.currentTimeMillis();
        
        switch (reportType) {
            case ENROLLMENT_REPORT:
                return generateEnrollmentReport(reportId, parameters);
            case DEMOGRAPHIC_REPORT:
                return generateDemographicReport(reportId, parameters);
            case FINANCIAL_REPORT:
                return generateFinancialReport(reportId, parameters);
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
            ReportType.DEMOGRAPHIC_REPORT,
            ReportType.FINANCIAL_REPORT,
            ReportType.STATISTICAL_SUMMARY
        );
    }
    
    @Override
    public List<ReportFormat> getSupportedFormats() {
        return Arrays.asList(ReportFormat.PDF, ReportFormat.CSV, ReportFormat.EXCEL, ReportFormat.JSON);
    }
    
    @Override
    public String scheduleRecurringReport(ReportType reportType, String frequency, List<String> recipients) {
        return "SCHED_DEPT_" + System.currentTimeMillis(); // Simplified for demo
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
     * Update department enrollment count.
     */
    private void updateDepartmentEnrollment(String departmentId) {
        Department department = departments.get(departmentId);
        if (department != null) {
            List<String> students = departmentStudents.get(departmentId);
            if (students != null) {
                department.setTotalEnrollment(students.size());
                invalidateStatisticsCache();
            }
        }
    }
    
    /**
     * Update department professor count.
     */
    private void updateDepartmentProfessorCount(String departmentId) {
        Department department = departments.get(departmentId);
        if (department != null) {
            List<String> professors = departmentProfessors.get(departmentId);
            if (professors != null) {
                // Update professor IDs in department
                department.getProfessorIds().clear();
                department.getProfessorIds().addAll(professors);
                invalidateStatisticsCache();
            }
        }
    }
    
    /**
     * Update department course count.
     */
    private void updateDepartmentCourseCount(String departmentId) {
        Department department = departments.get(departmentId);
        if (department != null) {
            List<String> courses = departmentCourses.get(departmentId);
            if (courses != null) {
                // Update course IDs in department
                department.getCourseIds().clear();
                department.getCourseIds().addAll(courses);
                invalidateStatisticsCache();
            }
        }
    }
    
    /**
     * Calculate faculty-to-student ratio for a department.
     */
    private double calculateFacultyToStudentRatio(String departmentId) {
        List<String> professors = departmentProfessors.get(departmentId);
        List<String> students = departmentStudents.get(departmentId);
        
        if (professors != null && students != null && !professors.isEmpty()) {
            return (double) students.size() / professors.size();
        }
        return 0.0;
    }
    
    /**
     * Create default budget structure.
     */
    private Map<String, Object> createDefaultBudget() {
        Map<String, Object> budget = new HashMap<>();
        budget.put("totalBudget", 0.0);
        budget.put("fiscalYear", LocalDateTime.now().getYear());
        budget.put("allocations", new HashMap<String, Double>());
        budget.put("lastUpdated", LocalDateTime.now());
        return budget;
    }
    
    /**
     * Get department count by type.
     */
    private Map<Department.DepartmentType, Long> getDepartmentCountByType() {
        return departments.values().stream()
                .collect(Collectors.groupingBy(
                    Department::getType,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get total budget across all departments.
     */
    private double getTotalBudget() {
        return departmentBudgets.values().stream()
                .mapToDouble(budget -> (Double) budget.getOrDefault("totalBudget", 0.0))
                .sum();
    }
    
    /**
     * Get average budget across all departments.
     */
    private double getAverageBudget() {
        if (departmentBudgets.isEmpty()) return 0.0;
        return getTotalBudget() / departmentBudgets.size();
    }
    
    /**
     * Check if department matches keyword.
     */
    private boolean matchesKeyword(Department department, String keyword) {
        return department.getDepartmentName().toLowerCase().contains(keyword) ||
               department.getDepartmentCode().toLowerCase().contains(keyword) ||
               department.getDepartmentId().toLowerCase().contains(keyword) ||
               department.getCollege().toLowerCase().contains(keyword) ||
               department.getType().toString().toLowerCase().contains(keyword);
    }
    
    /**
     * Check if department matches search criteria.
     */
    private boolean matchesSearchCriteria(Department department, Map<String, SearchCriterion> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> matchesCriterion(department, entry.getKey(), entry.getValue()));
    }
    
    /**
     * Check if department matches a specific criterion.
     */
    private boolean matchesCriterion(Department department, String field, SearchCriterion criterion) {
        Object fieldValue = getFieldValue(department, field);
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
     * Get field value from department.
     */
    private Object getFieldValue(Department department, String field) {
        switch (field.toLowerCase()) {
            case "departmentname": return department.getDepartmentName();
            case "departmentcode": return department.getDepartmentCode();
            case "departmentid": return department.getDepartmentId();
            case "college": return department.getCollege();
            case "type": return department.getType();
            case "totalenrollment": return department.getTotalEnrollment();
            case "active": return department.isActive();
            default: return null;
        }
    }
    
    /**
     * Check if department matches all criteria.
     */
    private boolean matchesAllCriteria(Department department, Map<String, Object> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> Objects.equals(getFieldValue(department, entry.getKey()), entry.getValue()));
    }
    
    /**
     * Get comparator for sorting.
     */
    private Comparator<Department> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "departmentname":
                return Comparator.comparing(Department::getDepartmentName);
            case "departmentcode":
                return Comparator.comparing(Department::getDepartmentCode);
            case "college":
                return Comparator.comparing(Department::getCollege);
            case "type":
                return Comparator.comparing(Department::getType);
            case "totalenrollment":
                return Comparator.comparingInt(Department::getTotalEnrollment);
            case "active":
                return Comparator.comparing(Department::isActive);
            default:
                return Comparator.comparing(Department::getDepartmentName);
        }
    }
    
    /**
     * Generate enrollment report.
     */
    private ReportData generateEnrollmentReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Department", "Code", "College", "Type", "Enrollment", "Professors", "Courses", "Ratio");
        
        List<Map<String, Object>> rows = departments.values().stream()
                .map(department -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Department", department.getDepartmentName());
                    row.put("Code", department.getDepartmentCode());
                    row.put("College", department.getCollege());
                    row.put("Type", department.getType().toString());
                    row.put("Enrollment", department.getTotalEnrollment());
                    row.put("Professors", getDepartmentProfessors(department.getDepartmentId()).size());
                    row.put("Courses", getDepartmentCourses(department.getDepartmentId()).size());
                    row.put("Ratio", String.format("%.1f:1", calculateFacultyToStudentRatio(department.getDepartmentId())));
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.ENROLLMENT_REPORT, "Department Enrollment Report", 
                            columns, rows, Map.of("totalDepartments", departments.size()));
    }
    
    /**
     * Generate demographic report.
     */
    private ReportData generateDemographicReport(String reportId, Map<String, Object> parameters) {
        Map<String, Long> collegeStats = getDepartmentDistributionByCollege();
        Map<Department.DepartmentType, Long> typeStats = getDepartmentCountByType();
        
        List<String> columns = Arrays.asList("Category", "Value", "Count", "Percentage");
        List<Map<String, Object>> rows = new ArrayList<>();
        
        // Add college statistics
        long totalDepartments = departments.size();
        collegeStats.forEach((college, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("Category", "College");
            row.put("Value", college);
            row.put("Count", count);
            row.put("Percentage", String.format("%.1f%%", (count * 100.0) / totalDepartments));
            rows.add(row);
        });
        
        // Add type statistics
        typeStats.forEach((type, count) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("Category", "Department Type");
            row.put("Value", type.toString());
            row.put("Count", count);
            row.put("Percentage", String.format("%.1f%%", (count * 100.0) / totalDepartments));
            rows.add(row);
        });
        
        return new ReportData(reportId, ReportType.DEMOGRAPHIC_REPORT, "Department Demographic Report", 
                            columns, rows, Map.of("totalDepartments", totalDepartments));
    }
    
    /**
     * Generate financial report.
     */
    private ReportData generateFinancialReport(String reportId, Map<String, Object> parameters) {
        List<String> columns = Arrays.asList("Department", "Total Budget", "Per Student", "Fiscal Year", "Last Updated");
        
        List<Map<String, Object>> rows = departments.values().stream()
                .map(department -> {
                    Map<String, Object> row = new HashMap<>();
                    Map<String, Object> budgetInfo = departmentBudgets.get(department.getDepartmentId());
                    
                    double totalBudget = (Double) budgetInfo.getOrDefault("totalBudget", 0.0);
                    int enrollment = department.getTotalEnrollment();
                    double perStudent = enrollment > 0 ? totalBudget / enrollment : 0.0;
                    
                    row.put("Department", department.getDepartmentName());
                    row.put("Total Budget", String.format("$%.2f", totalBudget));
                    row.put("Per Student", String.format("$%.2f", perStudent));
                    row.put("Fiscal Year", budgetInfo.getOrDefault("fiscalYear", "N/A"));
                    row.put("Last Updated", budgetInfo.getOrDefault("lastUpdated", "N/A"));
                    return row;
                })
                .collect(Collectors.toList());
        
        return new ReportData(reportId, ReportType.FINANCIAL_REPORT, "Department Financial Report", 
                            columns, rows, Map.of("totalBudget", getTotalBudget()));
    }
    
    /**
     * Generate statistical report.
     */
    private ReportData generateStatisticalReport(String reportId, Map<String, Object> parameters) {
        Map<String, Object> statistics = calculateOverallStatistics();
        String content = statistics.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        
        return new ReportData(reportId, ReportType.STATISTICAL_SUMMARY, "Department Statistical Summary", content);
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