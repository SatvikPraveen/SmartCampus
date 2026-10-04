// File location: src/main/java/repositories/EnrollmentRepository.java

package repositories;

import models.Enrollment;
import models.Student;
import models.Course;
import models.Department;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository for Enrollment entity operations
 * Provides specialized queries for enrollment data access
 */
public class EnrollmentRepository extends BaseRepository<Enrollment, String> {
    
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    protected String extractId(Enrollment enrollment) {
        return enrollment.getEnrollmentId();
    }
    
    @Override
    protected void setId(Enrollment enrollment, String id) {
        enrollment.setEnrollmentId(id);
    }
    
    @Override
    protected String generateId() {
        return "ENR" + String.format("%06d", idGenerator.getAndIncrement());
    }
    
    // Specialized query methods for enrollments
    
    /**
     * Find enrollments by student
     */
    public List<Enrollment> findByStudent(Student student) {
        return findByPredicate(enrollment -> 
            enrollment.getStudentId() != null && 
            enrollment.getStudentId().equals(student.getStudentId())
        );
    }
    
    /**
     * Find enrollments by course
     */
    public List<Enrollment> findByCourse(Course course) {
        return findByPredicate(enrollment -> 
            enrollment.getCourseId() != null && 
            enrollment.getCourseId().equals(course.getCourseId())
        );
    }
    
    /**
     * Find enrollment by student and course
     */
    public Optional<Enrollment> findByStudentAndCourse(Student student, Course course) {
        return findFirstByPredicate(enrollment -> 
            enrollment.getStudentId() != null && 
            enrollment.getStudentId().equals(student.getStudentId()) &&
            enrollment.getCourseId() != null && 
            enrollment.getCourseId().equals(course.getCourseId())
        );
    }
    
    /**
     * Find enrollments by enrollment date range
     */
    public List<Enrollment> findByEnrollmentDateBetween(Date startDate, Date endDate) {
        LocalDateTime start = toLocalDateTime(startDate);
        LocalDateTime end = toLocalDateTime(endDate);
        return findByPredicate(enrollment -> {
            LocalDateTime enrollmentDate = enrollment.getEnrollmentDate();
            return enrollmentDate != null &&
                   !enrollmentDate.isBefore(start) && 
                   !enrollmentDate.isAfter(end);
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
            academicYearOf(enrollment).equals(academicYear)
        );
    }
    
    /**
     * Find enrollments in courses offered by the department
     */
    public List<Enrollment> findByDepartment(Department department) {
        Set<String> departmentCourseIds = new HashSet<>(department.getCourseIds());
        return findByPredicate(enrollment -> 
            enrollment.getCourseId() != null &&
            departmentCourseIds.contains(enrollment.getCourseId())
        );
    }
    
    /**
     * Find recent enrollments (last N days)
     */
    public List<Enrollment> findRecentEnrollments(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        return findByPredicate(enrollment -> 
            enrollment.getEnrollmentDate() != null &&
            enrollment.getEnrollmentDate().isAfter(cutoffDate)
        );
    }
    
    /**
     * Find enrollments by enrollment year
     */
    public List<Enrollment> findByEnrollmentYear(int year) {
        return findByPredicate(enrollment -> 
            enrollment.getEnrollmentDate() != null &&
            enrollment.getEnrollmentDate().getYear() == year
        );
    }
    
    /**
     * Find active enrollments (current semester/year)
     */
    public List<Enrollment> findActiveEnrollments() {
        int currentYear = LocalDate.now().getYear();
        String currentSemester = getCurrentSemester();
        
        return findByPredicate(enrollment -> {
            LocalDateTime enrollmentDate = enrollment.getEnrollmentDate();
            return (enrollmentDate != null && enrollmentDate.getYear() == currentYear) || 
                   currentSemester.equals(enrollment.getSemester());
        });
    }
    
    /**
     * Group enrollments by student ID
     */
    public Map<String, List<Enrollment>> groupByStudent() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getStudentId() != null)
                .collect(Collectors.groupingBy(Enrollment::getStudentId));
    }
    
    /**
     * Group enrollments by course ID
     */
    public Map<String, List<Enrollment>> groupByCourse() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getCourseId() != null)
                .collect(Collectors.groupingBy(Enrollment::getCourseId));
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
                .collect(Collectors.groupingBy(EnrollmentRepository::academicYearOf));
    }
    
    /**
     * Get enrollment count by course ID
     */
    public Map<String, Long> getEnrollmentCountByCourse() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getCourseId() != null)
                .collect(Collectors.groupingBy(
                    Enrollment::getCourseId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get student course count (number of courses per student ID)
     */
    public Map<String, Long> getStudentCourseCount() {
        return findAll().stream()
                .filter(enrollment -> enrollment.getStudentId() != null)
                .collect(Collectors.groupingBy(
                    Enrollment::getStudentId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Find IDs of students enrolled in multiple courses
     */
    public List<String> findStudentsWithMultipleCourses() {
        Map<String, Long> courseCounts = getStudentCourseCount();
        return courseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Find IDs of courses with low enrollment
     */
    public List<String> findCoursesWithLowEnrollment(int threshold) {
        Map<String, Long> enrollmentCounts = getEnrollmentCountByCourse();
        return enrollmentCounts.entrySet().stream()
                .filter(entry -> entry.getValue() < threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Find IDs of courses with high enrollment
     */
    public List<String> findCoursesWithHighEnrollment(int threshold) {
        Map<String, Long> enrollmentCounts = getEnrollmentCountByCourse();
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
                .map(Enrollment::getStudentId)
                .distinct()
                .count();
        long uniqueCourses = enrollments.stream()
                .map(Enrollment::getCourseId)
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
                matches &= enrollment.getStudentId() != null && 
                          enrollment.getStudentId().equals(student.getStudentId());
            }
            
            if (course != null) {
                matches &= enrollment.getCourseId() != null && 
                          enrollment.getCourseId().equals(course.getCourseId());
            }
            
            if (semester != null && !semester.trim().isEmpty()) {
                matches &= enrollment.getSemester().equalsIgnoreCase(semester);
            }
            
            if (academicYear != null && !academicYear.trim().isEmpty()) {
                matches &= academicYearOf(enrollment).equals(academicYear);
            }
            
            if (enrollmentDateFrom != null) {
                matches &= enrollment.getEnrollmentDate() != null &&
                          !enrollment.getEnrollmentDate().isBefore(toLocalDateTime(enrollmentDateFrom));
            }
            
            if (enrollmentDateTo != null) {
                matches &= enrollment.getEnrollmentDate() != null &&
                          !enrollment.getEnrollmentDate().isAfter(toLocalDateTime(enrollmentDateTo));
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
                .filter(enrollment -> enrollment.getEnrollmentDate() != null)
                .collect(Collectors.groupingBy(
                    enrollment -> String.format("%d-%02d", 
                                                enrollment.getEnrollmentDate().getYear(),
                                                enrollment.getEnrollmentDate().getMonthValue()),
                    Collectors.counting()
                ));
    }
    
    /**
     * Helper method to get current semester
     */
    private String getCurrentSemester() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        
        if (month >= Calendar.AUGUST && month <= Calendar.DECEMBER) {
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
    
    /**
     * Derive the academic year (e.g. "2024-2025") from the enrollment's semester and year.
     * Fall terms open an academic year; Spring and Summer terms close it.
     */
    private static String academicYearOf(Enrollment enrollment) {
        int year = enrollment.getYear();
        String semester = enrollment.getSemester();
        if (semester != null && semester.toLowerCase().contains("fall")) {
            return year + "-" + (year + 1);
        }
        return (year - 1) + "-" + year;
    }
    
    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }
}