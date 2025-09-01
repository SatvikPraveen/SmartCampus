// File location: src/main/java/repositories/DepartmentRepository.java

package repositories;

import models.Department;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository for Department entity operations
 * Provides specialized queries for department data access
 */
public class DepartmentRepository extends BaseRepository<Department, String> {
    
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    protected String extractId(Department department) {
        return department.getDepartmentCode();
    }
    
    @Override
    protected void setId(Department department, String id) {
        // Department code is set during construction, this is for completeness
    }
    
    @Override
    protected String generateId() {
        return "DEPT" + String.format("%03d", idGenerator.getAndIncrement());
    }
    
    // Specialized query methods for departments
    
    /**
     * Find department by department code
     */
    public Optional<Department> findByDepartmentCode(String departmentCode) {
        return findById(departmentCode);
    }
    
    /**
     * Find departments by name pattern (case-insensitive)
     */
    public List<Department> findByNameContaining(String namePattern) {
        String pattern = namePattern.toLowerCase();
        return findByPredicate(department -> 
            department.getName().toLowerCase().contains(pattern)
        );
    }
    
    /**
     * Find department by exact name
     */
    public Optional<Department> findByName(String name) {
        return findFirstByPredicate(department -> 
            department.getName().equalsIgnoreCase(name)
        );
    }
    
    /**
     * Find departments by head of department
     */
    public List<Department> findByHeadOfDepartment(String headName) {
        return findByPredicate(department -> 
            department.getHeadOfDepartment().toLowerCase()
                    .contains(headName.toLowerCase())
        );
    }
    
    /**
     * Find departments by building location
     */
    public List<Department> findByBuilding(String building) {
        return findByPredicate(department -> 
            department.getLocation().toLowerCase()
                    .contains(building.toLowerCase())
        );
    }
    
    /**
     * Find departments by establishment year
     */
    public List<Department> findByEstablishmentYear(int year) {
        return findByPredicate(department -> 
            department.getEstablishedYear() == year
        );
    }
    
    /**
     * Find departments established after a certain year
     */
    public List<Department> findEstablishedAfter(int year) {
        return findByPredicate(department -> 
            department.getEstablishedYear() > year
        );
    }
    
    /**
     * Find departments established before a certain year
     */
    public List<Department> findEstablishedBefore(int year) {
        return findByPredicate(department -> 
            department.getEstablishedYear() < year
        );
    }
    
    /**
     * Find departments within establishment year range
     */
    public List<Department> findByEstablishmentYearRange(int startYear, int endYear) {
        return findByPredicate(department -> {
            int year = department.getEstablishedYear();
            return year >= startYear && year <= endYear;
        });
    }
    
    /**
     * Find departments by student count range
     */
    public List<Department> findByStudentCountRange(int minStudents, int maxStudents) {
        return findByPredicate(department -> {
            int studentCount = department.getStudentCount();
            return studentCount >= minStudents && studentCount <= maxStudents;
        });
    }
    
    /**
     * Find departments with minimum student count
     */
    public List<Department> findByMinStudentCount(int minStudents) {
        return findByPredicate(department -> 
            department.getStudentCount() >= minStudents
        );
    }
    
    /**
     * Find large departments (above threshold)
     */
    public List<Department> findLargeDepartments(int studentThreshold) {
        return findByPredicate(department -> 
            department.getStudentCount() > studentThreshold
        );
    }
    
    /**
     * Find small departments (below threshold)
     */
    public List<Department> findSmallDepartments(int studentThreshold) {
        return findByPredicate(department -> 
            department.getStudentCount() < studentThreshold
        );
    }
    
    /**
     * Group departments by establishment decade
     */
    public Map<Integer, List<Department>> groupByEstablishmentDecade() {
        return findAll().stream()
                .collect(Collectors.groupingBy(department -> 
                    (department.getEstablishedYear() / 10) * 10
                ));
    }
    
    /**
     * Group departments by building/location
     */
    public Map<String, List<Department>> groupByBuilding() {
        return findAll().stream()
                .collect(Collectors.groupingBy(department -> {
                    String location = department.getLocation();
                    // Extract building name (assuming format like "Building A, Floor 2")
                    return location.split(",")[0].trim();
                }));
    }
    
    /**
     * Get departments sorted by student count (descending)
     */
    public List<Department> findAllSortedByStudentCount() {
        return findAllSorted(Comparator.comparingInt(Department::getStudentCount)
                                     .reversed());
    }
    
    /**
     * Get departments sorted by establishment year
     */
    public List<Department> findAllSortedByEstablishmentYear() {
        return findAllSorted(Comparator.comparingInt(Department::getEstablishedYear));
    }
    
    /**
     * Get departments sorted by name
     */
    public List<Department> findAllSortedByName() {
        return findAllSorted(Comparator.comparing(Department::getName));
    }
    
    /**
     * Find newest departments (last N years)
     */
    public List<Department> findNewestDepartments(int years) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int cutoffYear = currentYear - years;
        
        return findByPredicate(department -> 
            department.getEstablishedYear() > cutoffYear
        );
    }
    
    /**
     * Find oldest departments (first N departments by establishment year)
     */
    public List<Department> findOldestDepartments(int count) {
        return findAll().stream()
                .sorted(Comparator.comparingInt(Department::getEstablishedYear))
                .limit(count)
                .collect(Collectors.toList());
    }
    
    /**
     * Search departments by multiple criteria
     */
    public List<Department> searchDepartments(String name, String headOfDepartment,
                                            String location, Integer establishedYear,
                                            Integer minStudents, Integer maxStudents) {
        return findByPredicate(department -> {
            boolean matches = true;
            
            if (name != null && !name.trim().isEmpty()) {
                matches &= department.getName().toLowerCase()
                          .contains(name.toLowerCase());
            }
            
            if (headOfDepartment != null && !headOfDepartment.trim().isEmpty()) {
                matches &= department.getHeadOfDepartment().toLowerCase()
                          .contains(headOfDepartment.toLowerCase());
            }
            
            if (location != null && !location.trim().isEmpty()) {
                matches &= department.getLocation().toLowerCase()
                          .contains(location.toLowerCase());
            }
            
            if (establishedYear != null) {
                matches &= department.getEstablishedYear() == establishedYear;
            }
            
            if (minStudents != null) {
                matches &= department.getStudentCount() >= minStudents;
            }
            
            if (maxStudents != null) {
                matches &= department.getStudentCount() <= maxStudents;
            }
            
            return matches;
        });
    }
    
    /**
     * Get department statistics
     */
    public Map<String, Object> getDepartmentStatistics() {
        List<Department> departments = findAll();
        
        OptionalDouble avgStudentCount = departments.stream()
                .mapToInt(Department::getStudentCount)
                .average();
        
        OptionalInt maxStudentCount = departments.stream()
                .mapToInt(Department::getStudentCount)
                .max();
        
        OptionalInt minStudentCount = departments.stream()
                .mapToInt(Department::getStudentCount)
                .min();
        
        int totalStudents = departments.stream()
                .mapToInt(Department::getStudentCount)
                .sum();
        
        OptionalInt oldestYear = departments.stream()
                .mapToInt(Department::getEstablishedYear)
                .min();
        
        OptionalInt newestYear = departments.stream()
                .mapToInt(Department::getEstablishedYear)
                .max();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDepartments", departments.size());
        stats.put("totalStudentsAcrossAllDepartments", totalStudents);
        stats.put("averageStudentCount", avgStudentCount.orElse(0.0));
        stats.put("maxStudentCount", maxStudentCount.orElse(0));
        stats.put("minStudentCount", minStudentCount.orElse(0));
        stats.put("oldestDepartmentYear", oldestYear.orElse(0));
        stats.put("newestDepartmentYear", newestYear.orElse(0));
        
        return stats;
    }
    
    /**
     * Find departments by head name pattern
     */
    public List<Department> findByHeadNameContaining(String headNamePattern) {
        String pattern = headNamePattern.toLowerCase();
        return findByPredicate(department -> 
            department.getHeadOfDepartment().toLowerCase().contains(pattern)
        );
    }
    
    /**
     * Get average establishment year
     */
    public OptionalDouble getAverageEstablishmentYear() {
        return findAll().stream()
                .mapToInt(Department::getEstablishedYear)
                .average();
    }
    
    /**
     * Find departments with similar student counts (within tolerance)
     */
    public List<Department> findSimilarSizedDepartments(int targetStudentCount, 
                                                       int tolerance) {
        return findByPredicate(department -> {
            int studentCount = department.getStudentCount();
            return Math.abs(studentCount - targetStudentCount) <= tolerance;
        });
    }
}