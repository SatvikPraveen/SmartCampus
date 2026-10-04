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
        return course.getCourseId();
    }
    
    @Override
    protected void setId(Course course, String id) {
        course.setCourseId(id);
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
            course.getDepartmentId() != null && 
            course.getDepartmentId().equals(department.getDepartmentId())
        );
    }
    
    /**
     * Find course by course code
     */
    public Optional<Course> findByCourseCode(String courseCode) {
        return findFirstByPredicate(course -> 
            courseCode.equals(course.getCourseCode())
        );
    }
    
    /**
     * Find courses by professor
     */
    public List<Course> findByProfessor(Professor professor) {
        return findByPredicate(course -> 
            course.getProfessorId() != null && 
            course.getProfessorId().equals(professor.getProfessorId())
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
            course.getCourseName().toLowerCase().contains(pattern)
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
            String.valueOf(course.getYear()).equals(academicYear)
        );
    }
    
    /**
     * Find courses by capacity range
     */
    public List<Course> findByCapacityRange(int minCapacity, int maxCapacity) {
        return findByPredicate(course -> {
            int capacity = course.getMaxEnrollment();
            return capacity >= minCapacity && capacity <= maxCapacity;
        });
    }
    
    /**
     * Find courses with available spots
     */
    public List<Course> findCoursesWithAvailableSpots() {
        return findByPredicate(course -> 
            course.getEnrolledStudentIds().size() < course.getMaxEnrollment()
        );
    }
    
    /**
     * Find full courses
     */
    public List<Course> findFullCourses() {
        return findByPredicate(course -> 
            course.getEnrolledStudentIds().size() >= course.getMaxEnrollment()
        );
    }
    
    /**
     * Find courses by enrollment threshold
     */
    public List<Course> findByMinEnrollment(int minEnrollment) {
        return findByPredicate(course -> 
            course.getEnrolledStudentIds().size() >= minEnrollment
        );
    }
    
    /**
     * Group courses by department ID
     */
    public Map<String, List<Course>> groupByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Course::getDepartmentId));
    }
    
    /**
     * Group courses by professor ID
     */
    public Map<String, List<Course>> groupByProfessor() {
        return findAll().stream()
                .filter(course -> course.getProfessorId() != null)
                .collect(Collectors.groupingBy(Course::getProfessorId));
    }
    
    /**
     * Group courses by semester
     */
    public Map<String, List<Course>> groupBySemester() {
        return findAll().stream()
                .collect(Collectors.groupingBy(Course::getSemester));
    }
    
    /**
     * Get course count by department ID
     */
    public Map<String, Long> getCourseCountByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                    Course::getDepartmentId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get total credits by department ID
     */
    public Map<String, Integer> getTotalCreditsByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                    Course::getDepartmentId,
                    Collectors.summingInt(Course::getCredits)
                ));
    }
    
    /**
     * Get average capacity by department ID
     */
    public Map<String, Double> getAverageCapacityByDepartment() {
        return findAll().stream()
                .filter(course -> course.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                    Course::getDepartmentId,
                    Collectors.averagingInt(Course::getMaxEnrollment)
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
                matches &= course.getCourseName().toLowerCase()
                          .contains(name.toLowerCase());
            }
            
            if (department != null) {
                matches &= course.getDepartmentId() != null && 
                          course.getDepartmentId().equals(department.getDepartmentId());
            }
            
            if (professor != null) {
                matches &= course.getProfessorId() != null && 
                          course.getProfessorId().equals(professor.getProfessorId());
            }
            
            if (semester != null && !semester.trim().isEmpty()) {
                matches &= course.getSemester().equalsIgnoreCase(semester);
            }
            
            if (academicYear != null && !academicYear.trim().isEmpty()) {
                matches &= String.valueOf(course.getYear()).equals(academicYear);
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
        return findAllById(course.getPrerequisiteCourseIds());
    }
    
    /**
     * Find courses that have the given course as prerequisite
     */
    public List<Course> findCoursesWithPrerequisite(Course prerequisite) {
        return findByPredicate(course -> 
            course.getPrerequisiteCourseIds().contains(prerequisite.getCourseId())
        );
    }
    
    /**
     * Get enrollment statistics
     */
    public Map<String, Object> getEnrollmentStatistics() {
        List<Course> courses = findAll();
        
        int totalCapacity = courses.stream()
                .mapToInt(Course::getMaxEnrollment)
                .sum();
        
        int totalEnrolled = courses.stream()
                .mapToInt(course -> course.getEnrolledStudentIds().size())
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
            if (course.getMaxEnrollment() == 0) return false;
            double utilizationRate = (double) course.getEnrolledStudentIds().size() / 
                                   course.getMaxEnrollment();
            return utilizationRate >= (minUtilizationRate / 100.0);
        });
    }
    
    /**
     * Find underutilized courses (low enrollment ratio)
     */
    public List<Course> findUnderutilizedCourses(double maxUtilizationRate) {
        return findByPredicate(course -> {
            if (course.getMaxEnrollment() == 0) return true;
            double utilizationRate = (double) course.getEnrolledStudentIds().size() / 
                                   course.getMaxEnrollment();
            return utilizationRate <= (maxUtilizationRate / 100.0);
        });
    }
}