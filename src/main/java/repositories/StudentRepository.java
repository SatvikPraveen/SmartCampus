// File location: src/main/java/repositories/StudentRepository.java

package repositories;

import models.Student;
import models.Department;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository for Student entity operations
 * Provides specialized queries for student data access
 */
public class StudentRepository extends BaseRepository<Student, String> {
    
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    protected String extractId(Student student) {
        return student.getStudentId();
    }
    
    @Override
    protected void setId(Student student, String id) {
        student.setStudentId(id);
    }
    
    @Override
    protected String generateId() {
        return "STU" + String.format("%06d", idGenerator.getAndIncrement());
    }
    
    // Specialized query methods for students
    
    /**
     * Find students by department
     */
    public List<Student> findByDepartment(Department department) {
        return findByPredicate(student -> 
            student.getDepartmentId() != null && 
            student.getDepartmentId().equals(department.getDepartmentId())
        );
    }
    
    /**
     * Find students by email
     */
    public Optional<Student> findByEmail(String email) {
        return findFirstByPredicate(student -> 
            email.equals(student.getEmail())
        );
    }
    
    /**
     * Find students by name pattern (case-insensitive)
     */
    public List<Student> findByNameContaining(String namePattern) {
        String pattern = namePattern.toLowerCase();
        return findByPredicate(student -> 
            student.getFullName().toLowerCase().contains(pattern)
        );
    }
    
    /**
     * Find students by enrollment year
     */
    public List<Student> findByEnrollmentYear(int year) {
        return findByPredicate(student -> 
            student.getEnrollmentDate() != null &&
            student.getEnrollmentDate().getYear() == year
        );
    }
    
    /**
     * Find students enrolled between dates
     */
    public List<Student> findByEnrollmentDateBetween(Date startDate, Date endDate) {
        LocalDate start = toLocalDate(startDate);
        LocalDate end = toLocalDate(endDate);
        return findByPredicate(student -> {
            LocalDate enrollmentDate = student.getEnrollmentDate();
            return enrollmentDate != null &&
                   !enrollmentDate.isBefore(start) && 
                   !enrollmentDate.isAfter(end);
        });
    }
    
    /**
     * Find students by multiple departments
     */
    public List<Student> findByDepartments(List<Department> departments) {
        Set<String> deptIds = departments.stream()
                .map(Department::getDepartmentId)
                .collect(Collectors.toSet());
        return findByPredicate(student -> 
            student.getDepartmentId() != null && 
            deptIds.contains(student.getDepartmentId())
        );
    }
    
    /**
     * Find active students (those with recent activity)
     */
    public List<Student> findActiveStudents() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6); // Active within last 6 months
        
        return findByPredicate(student -> 
            student.getEnrollmentDate() != null &&
            student.getEnrollmentDate().isAfter(sixMonthsAgo)
        );
    }
    
    /**
     * Get students grouped by department ID
     */
    public Map<String, List<Student>> groupByDepartment() {
        return findAll().stream()
                .filter(student -> student.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Student::getDepartmentId));
    }
    
    /**
     * Get students statistics by department ID
     */
    public Map<String, Long> getStudentCountByDepartment() {
        return findAll().stream()
                .filter(student -> student.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                    Student::getDepartmentId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Find students with similar emails (same domain)
     */
    public List<Student> findByEmailDomain(String domain) {
        return findByPredicate(student -> 
            student.getEmail().toLowerCase().endsWith("@" + domain.toLowerCase())
        );
    }
    
    /**
     * Get enrollment statistics by year
     */
    public Map<Integer, Long> getEnrollmentStatsByYear() {
        return findAll().stream()
                .filter(student -> student.getEnrollmentDate() != null)
                .collect(Collectors.groupingBy(
                    student -> student.getEnrollmentDate().getYear(),
                    Collectors.counting()
                ));
    }
    
    /**
     * Find students enrolled in current academic year
     */
    public List<Student> findCurrentYearStudents() {
        int currentYear = LocalDate.now().getYear();
        
        // Academic year typically starts in August/September
        LocalDate academicYearStart = LocalDate.of(currentYear, 8, 1);
        
        return findByPredicate(student -> 
            student.getEnrollmentDate() != null &&
            student.getEnrollmentDate().isAfter(academicYearStart)
        );
    }
    
    /**
     * Search students by multiple criteria
     */
    public List<Student> searchStudents(String name, Department department, 
                                      Date enrollmentDateFrom, Date enrollmentDateTo) {
        return findByPredicate(student -> {
            boolean matches = true;
            
            if (name != null && !name.trim().isEmpty()) {
                matches &= student.getFullName().toLowerCase()
                          .contains(name.toLowerCase());
            }
            
            if (department != null) {
                matches &= student.getDepartmentId() != null && 
                          student.getDepartmentId().equals(department.getDepartmentId());
            }
            
            if (enrollmentDateFrom != null) {
                matches &= student.getEnrollmentDate() != null &&
                          !student.getEnrollmentDate().isBefore(toLocalDate(enrollmentDateFrom));
            }
            
            if (enrollmentDateTo != null) {
                matches &= student.getEnrollmentDate() != null &&
                          !student.getEnrollmentDate().isAfter(toLocalDate(enrollmentDateTo));
            }
            
            return matches;
        });
    }
    
    /**
     * Get recent enrollments (last N days)
     */
    public List<Student> getRecentEnrollments(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        
        return findByPredicate(student -> 
            student.getEnrollmentDate() != null &&
            student.getEnrollmentDate().isAfter(cutoffDate)
        );
    }
    
    private static LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}