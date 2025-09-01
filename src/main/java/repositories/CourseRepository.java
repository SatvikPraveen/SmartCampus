// File location: src/main/java/repositories/CourseRepository.java

package repositories;

import models.Course;
import models.Department;
import models.Professor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository for Course entity operations
 * Provides specialized queries for course data access
 */
public class CourseRepository extends BaseRepository<Course, String> {
    
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    protected String extractId(Course course) {
        return course.getCourseCode();
    }
    
    @Override
    protected void setId(Course course, String id) {
        // Course code is set during construction, this is for completeness
    }
    
    @Override
    protected String generateId() {
        return "COURSE" + String.format("%04d", idGenerator.getAndIncrement());
    }
    
    // Specialized query methods for courses
    
    /**
     * Find courses by department
     */
    public List<Course> findByDepartment(Department department) {
        return findByPredicate(course -> 
            course.getDepartment() != null && 
            course.getDepartment().equals(department)
        );
    }
    
    /**
     * Find course by course code
     */
    public Optional<Course> findByCourseCode(String courseCode) {
        return findById(courseCode);
    }
    
    /**
     * Find courses by professor
     */
    public List<Course> findByProfessor(Professor professor) {
        return findByPredicate(course -> 
            course.getProfessor() != null && 
            course.getProfessor().equals(professor)
        );
    }
    
    /**
     * Find courses by credits
     */
    public List<Course> findByCredits(int credits) {
        return findByPredicate(course -> 
            course.getCredits() == credits
        );
    }
    
    /**
     * Find courses by credit range
     */
    public List<Course> findByCreditRange(int minCredits, int maxCredits) {
        return findByPredicate(course -> {
            int credits = course.getCredits();
            return credits >= minCredits && credits <= maxCredits;
        });
    }
    
    /**
     * Find courses by name pattern (case-insensitive)
     */
    public List<Course> findByNameContaining(String namePattern) {
        String pattern = namePattern.toLowerCase();
        return findByPredicate(course -> 
            course.getName().toLowerCase().contains(pattern)
        );
    }
    
    /**
     * Find courses by description keywords
     */
    public List<Course> findByDescriptionContaining(String keyword) {
        String searchTerm = keyword.toLowerCase();
        return findByPredicate(course -> 
            course.getDescription().toLowerCase().contains(searchTerm)
        );
    }
    
    /**
     * Find courses by semester
     */
    public List<Course> findBySemester(String semester) {
        return findByPredicate(course -> 
            course.getSemester().equalsIgnoreCase(semester)
        );
    }
    
    /**
     * Find courses by academic year
     */
    public List<Course> findByAcademicYear(String academicYear) {
        return findByPredicate(course -> 
            course.getAcademicYear().equals(academicYear)
        );
    }
    
    /**
     * Find courses by capacity range
     */
    public List<Course> findByCapacityRange(int minCapacity, int maxCapacity) {
        return findByPredicate(course -> {
            int capacity = course.getCapacity();
            return capacity >= minCapacity && capacity <= maxCapacity;
        });
    }
    
    /**
     * Find courses with available spots
     */
    public List<Course> findCoursesWithAvailableSpots() {
        return findByPredicate(course -> 
            course.getEnrolledStudents() < course.getCapacity()
        );
    }
    
    /**
     * Find full courses
     */
    public List<Course> findFullCourses() {
        return findByPredicate(course -> 
            course.getEnrolledStudents() >= course.getCapacity()
        );
    }
    
    /**
     * Find courses by enrollment threshold
     */
    public List<Course> findByMinEnrollment(int minEnrollment) {
        return findByPredicate(course -> 
            course.getEnrolledStudents() >= minEnrollment
        );
    }
    
    /**
     * Group courses by department
     */
    public Map<Department, List<Course>> groupByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartment() != null)
                .collect(Collectors.groupingBy(Course::getDepartment));
    }
    
    /**
     * Group courses by professor
     */
    public Map<Professor, List<Course>> groupByProfessor() {
        return findAll().stream()
                .filter(course -> course.getProfessor() != null)
                .collect(Collectors.groupingBy(Course::getProfessor));
    }
    
    /**
     * Group courses by semester
     */
    public Map<String, List<Course>> groupBySemester() {
        return findAll().stream()
                .collect(Collectors.groupingBy(Course::getSemester));
    }
    
    /**
     * Get course count by department
     */
    public Map<Department, Long> getCourseCountByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartment() != null)
                .collect(Collectors.groupingBy(
                    Course::getDepartment,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get total credits by department
     */
    public Map<Department, Integer> getTotalCreditsByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartment() != null)
                .collect(Collectors.groupingBy(
                    Course::getDepartment,
                    Collectors.summingInt(Course::getCredits)
                ));
    }
    
    /**
     * Get average capacity by department
     */
    public Map<Department, Double> getAverageCapacityByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartment() != null)
                .collect(Collectors.groupingBy(
                    Course::getDepartment,
                    Collectors.averagingInt(Course::getCapacity)
                ));
    }
    
    /**
     * Find courses by multiple criteria
     */
    public List<Course> searchCourses(String name, Department department, 
                                    Professor professor, String semester,
                                    String academicYear, Integer minCredits, 
                                    Integer maxCredits) {
        return findByPredicate(course -> {
            boolean matches = true;
            
            if (name != null && !name.trim().isEmpty()) {
                matches &= course.getName().toLowerCase()
                          .contains(name.toLowerCase());
            }
            
            if (department != null) {
                matches &= course.getDepartment() != null && 
                          course.getDepartment().equals(department);
            }
            
            if (professor != null) {
                matches &= course.getProfessor() != null && 
                          course.getProfessor().equals(professor);
            }
            
            if (semester != null && !semester.trim().isEmpty()) {
                matches &= course.getSemester().equalsIgnoreCase(semester);
            }
            
            if (academicYear != null && !academicYear.trim().isEmpty()) {
                matches &= course.getAcademicYear().equals(academicYear);
            }
            
            if (minCredits != null) {
                matches &= course.getCredits() >= minCredits;
            }
            
            if (maxCredits != null) {
                matches &= course.getCredits() <= maxCredits;
            }
            
            return matches;
        });
    }
    
    /**
     * Find prerequisite courses
     */
    public List<Course> findPrerequisites(Course course) {
        // This would require a prerequisites field in Course model
        // For now, return empty list as placeholder
        return new ArrayList<>();
    }
    
    /**
     * Find courses that have the given course as prerequisite
     */
    public List<Course> findCoursesWithPrerequisite(Course prerequisite) {
        // This would require a prerequisites field in Course model
        // For now, return empty list as placeholder
        return new ArrayList<>();
    }
    
    /**
     * Get enrollment statistics
     */
    public Map<String, Object> getEnrollmentStatistics() {
        List<Course> courses = findAll();
        
        int totalCapacity = courses.stream()
                .mapToInt(Course::getCapacity)
                .sum();
        
        int totalEnrolled = courses.stream()
                .mapToInt(Course::getEnrolledStudents)
                .sum();
        
        double utilizationRate = totalCapacity > 0 ? 
                (double) totalEnrolled / totalCapacity * 100 : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCourses", courses.size());
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalEnrolled", totalEnrolled);
        stats.put("utilizationRate", utilizationRate);
        stats.put("availableSpots", totalCapacity - totalEnrolled);
        
        return stats;
    }
    
    /**
     * Find popular courses (high enrollment ratio)
     */
    public List<Course> findPopularCourses(double minUtilizationRate) {
        return findByPredicate(course -> {
            if (course.getCapacity() == 0) return false;
            double utilizationRate = (double) course.getEnrolledStudents() / 
                                   course.getCapacity();
            return utilizationRate >= (minUtilizationRate / 100.0);
        });
    }
    
    /**
     * Find underutilized courses (low enrollment ratio)
     */
    public List<Course> findUnderutilizedCourses(double maxUtilizationRate) {
        return findByPredicate(course -> {
            if (course.getCapacity() == 0) return true;
            double utilizationRate = (double) course.getEnrolledStudents() / 
                                   course.getCapacity();
            return utilizationRate <= (maxUtilizationRate / 100.0);
        });
    }
}