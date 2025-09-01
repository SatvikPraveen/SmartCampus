// File location: src/main/java/repositories/EnrollmentRepository.java

package repositories;

import models.Enrollment;
import models.Student;
import models.Course;
import models.Department;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository for Enrollment entity operations
 * Provides specialized queries for enrollment data access
 */
public class EnrollmentRepository extends BaseRepository<Enrollment, Long> {
    
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    protected Long extractId(Enrollment enrollment) {
        return enrollment.getEnrollmentId();
    }
    
    @Override
    protected void setId(Enrollment enrollment, Long id) {
        // Enrollment ID would need a setter in the Enrollment model
        // This is for completeness
    }
    
    @Override
    protected Long generateId() {
        return idGenerator.getAndIncrement();
    }
    
    // Specialized query methods for enrollments
    
    /**
     * Find enrollments by student
     */
    public List<Enrollment> findByStudent(Student student) {
        return findByPredicate(enrollment -> 
            enrollment.getStudent() != null && 
            enrollment.getStudent().equals(student)
        );
    }
    
    /**
     * Find enrollments by course
     */
    public List<Enrollment> findByCourse(Course course) {
        return findByPredicate(enrollment -> 
            enrollment.getCourse() != null && 
            enrollment.getCourse().equals(course)
        );
    }
    
    /**
     * Find enrollment by student and course
     */
    public Optional<Enrollment> findByStudentAndCourse(Student student, Course course) {
        return findFirstByPredicate(enrollment -> 
            enrollment.getStudent() != null && 
            enrollment.getStudent().equals(student) &&
            enrollment.getCourse() != null && 
            enrollment.getCourse().equals(course)
        );
    }
    
    /**
     * Find enrollments by enrollment date range
     */
    public List<Enrollment> findByEnrollmentDateBetween(Date startDate, Date endDate) {
        return findByPredicate(enrollment -> {
            Date enrollmentDate = enrollment.getEnrollmentDate();
            return enrollmentDate.compareTo(startDate) >= 0 && 
                   enrollmentDate.compareTo(endDate) <= 0;
        });
    }
    
    /**
     * Find enrollments by semester
     */
    public List<Enrollment> findBySemester(String semester) {
        return findByPredicate(enrollment -> 
            enrollment.getSemester().equalsIgnoreCase(semester)
        );
    }
    
    /**
     * Find enrollments by academic year
     */
    public List<Enrollment> findByAcademicYear(String academicYear) {
        return findByPredicate(enrollment -> 
            enrollment.getAcademicYear().equals(academicYear)
        );
    }
    
    /**
     * Find enrollments by department
     */
    public List<Enrollment> findByDepartment(Department department) {
        return findByPredicate(enrollment -> 
            enrollment.getCourse() != null &&
            enrollment.getCourse().getDepartment() != null &&
            enrollment.getCourse().getDepartment().equals(department)
        );
    }
    
    /**
     * Find recent enrollments (last N days)
     */
    public List<Enrollment> findRecentEnrollments(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        Date cutoffDate = cal.getTime();
        
        return findByPredicate(enrollment -> 
            enrollment.getEnrollmentDate().after(cutoffDate)
        );
    }
    
    /**
     * Find enrollments by enrollment year
     */
    public List<Enrollment> findByEnrollmentYear(int year) {
        return findByPredicate(enrollment -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(enrollment.getEnrollmentDate());
            return cal.get(Calendar.YEAR) == year;
        });
    }
    
    /**
     * Find active enrollments (current semester/year)
     */
    public List<Enrollment> findActiveEnrollments() {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        String currentSemester = getCurrentSemester();
        
        return findByPredicate(enrollment -> {
            cal.setTime(enrollment.getEnrollmentDate());
            int enrollmentYear = cal.get(Calendar.YEAR);
            return enrollmentYear == currentYear || 
                   enrollment.getSemester().equals(currentSemester);
        });
    }
    
    /**
     * Group enrollments by student
     */
    public Map<Student, List<Enrollment>> groupByStudent() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getStudent() != null)
                .collect(Collectors.groupingBy(Enrollment::getStudent));
    }
    
    /**
     * Group enrollments by course
     */
    public Map<Course, List<Enrollment>> groupByCourse() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .collect(Collectors.groupingBy(Enrollment::getCourse));
    }
    
    /**
     * Group enrollments by semester
     */
    public Map<String, List<Enrollment>> groupBySemester() {
        return findAll().stream()
                .collect(Collectors.groupingBy(Enrollment::getSemester));
    }
    
    /**
     * Group enrollments by academic year
     */
    public Map<String, List<Enrollment>> groupByAcademicYear() {
        return findAll().stream()
                .collect(Collectors.groupingBy(Enrollment::getAcademicYear));
    }
    
    /**
     * Group enrollments by department
     */
    public Map<Department, List<Enrollment>> groupByDepartment() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getCourse() != null &&
                                    enrollment.getCourse().getDepartment() != null)
                .collect(Collectors.groupingBy(enrollment -> 
                    enrollment.getCourse().getDepartment()));
    }
    
    /**
     * Get enrollment count by course
     */
    public Map<Course, Long> getEnrollmentCountByCourse() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .collect(Collectors.groupingBy(
                    Enrollment::getCourse,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get enrollment count by department
     */
    public Map<Department, Long> getEnrollmentCountByDepartment() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getCourse() != null &&
                                    enrollment.getCourse().getDepartment() != null)
                .collect(Collectors.groupingBy(
                    enrollment -> enrollment.getCourse().getDepartment(),
                    Collectors.counting()
                ));
    }
    
    /**
     * Get student course count (number of courses per student)
     */
    public Map<Student, Long> getStudentCourseCount() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getStudent() != null)
                .collect(Collectors.groupingBy(
                    Enrollment::getStudent,
                    Collectors.counting()
                ));
    }
    
    /**
     * Find students enrolled in multiple courses
     */
    public List<Student> findStudentsWithMultipleCourses() {
        Map<Student, Long> courseCounts = getStudentCourseCount();
        return courseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Find courses with low enrollment
     */
    public List<Course> findCoursesWithLowEnrollment(int threshold) {
        Map<Course, Long> enrollmentCounts = getEnrollmentCountByCourse();
        return enrollmentCounts.entrySet().stream()
                .filter(entry -> entry.getValue() < threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Find courses with high enrollment
     */
    public List<Course> findCoursesWithHighEnrollment(int threshold) {
        Map<Course, Long> enrollmentCounts = getEnrollmentCountByCourse();
        return enrollmentCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Get enrollment statistics
     */
    public Map<String, Object> getEnrollmentStatistics() {
        List<Enrollment> enrollments = findAll();
        
        long totalEnrollments = enrollments.size();
        long uniqueStudents = enrollments.stream()
                .map(Enrollment::getStudent)
                .distinct()
                .count();
        long uniqueCourses = enrollments.stream()
                .map(Enrollment::getCourse)
                .distinct()
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEnrollments", totalEnrollments);
        stats.put("uniqueStudents", uniqueStudents);
        stats.put("uniqueCourses", uniqueCourses);
        stats.put("averageCoursesPerStudent", 
                  uniqueStudents > 0 ? (double) totalEnrollments / uniqueStudents : 0);
        stats.put("averageStudentsPerCourse", 
                  uniqueCourses > 0 ? (double) totalEnrollments / uniqueCourses : 0);
        
        return stats;
    }
    
    /**
     * Search enrollments by multiple criteria
     */
    public List<Enrollment> searchEnrollments(Student student, Course course,
                                            String semester, String academicYear,
                                            Date enrollmentDateFrom, 
                                            Date enrollmentDateTo) {
        return findByPredicate(enrollment -> {
            boolean matches = true;
            
            if (student != null) {
                matches &= enrollment.getStudent() != null && 
                          enrollment.getStudent().equals(student);
            }
            
            if (course != null) {
                matches &= enrollment.getCourse() != null && 
                          enrollment.getCourse().equals(course);
            }
            
            if (semester != null && !semester.trim().isEmpty()) {
                matches &= enrollment.getSemester().equalsIgnoreCase(semester);
            }
            
            if (academicYear != null && !academicYear.trim().isEmpty()) {
                matches &= enrollment.getAcademicYear().equals(academicYear);
            }
            
            if (enrollmentDateFrom != null) {
                matches &= enrollment.getEnrollmentDate()
                          .compareTo(enrollmentDateFrom) >= 0;
            }
            
            if (enrollmentDateTo != null) {
                matches &= enrollment.getEnrollmentDate()
                          .compareTo(enrollmentDateTo) <= 0;
            }
            
            return matches;
        });
    }
    
    /**
     * Find enrollments for current academic year
     */
    public List<Enrollment> findCurrentAcademicYearEnrollments() {
        String currentAcademicYear = getCurrentAcademicYear();
        return findByAcademicYear(currentAcademicYear);
    }
    
    /**
     * Get enrollment trends by month
     */
    public Map<String, Long> getEnrollmentTrendsByMonth() {
        return findAll().stream()
                .collect(Collectors.groupingBy(
                    enrollment -> {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(enrollment.getEnrollmentDate());
                        return String.format("%d-%02d", 
                                           cal.get(Calendar.YEAR),
                                           cal.get(Calendar.MONTH) + 1);
                    },
                    Collectors.counting()
                ));
    }
    
    /**
     * Helper method to get current semester
     */
    private String getCurrentSemester() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        
        if (month >= Calendar.AUGUST || month <= Calendar.DECEMBER) {
            return "Fall";
        } else if (month >= Calendar.JANUARY && month <= Calendar.MAY) {
            return "Spring";
        } else {
            return "Summer";
        }
    }
    
    /**
     * Helper method to get current academic year
     */
    private String getCurrentAcademicYear() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        
        // Academic year typically starts in August
        if (month >= Calendar.AUGUST) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }
}