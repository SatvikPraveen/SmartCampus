// File location: src/main/java/repositories/StudentRepository.java

package repositories;

import models.Student;
import models.Department;
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
        return student.getId();
    }
    
    @Override
    protected void setId(Student student, String id) {
        // Student ID is set during construction, this is for completeness
        // In a real scenario, you might use reflection or modify the Student class
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
            student.getDepartment() != null && 
            student.getDepartment().equals(department)
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
            student.getName().toLowerCase().contains(pattern)
        );
    }
    
    /**
     * Find students by enrollment year
     */
    public List<Student> findByEnrollmentYear(int year) {
        return findByPredicate(student -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(student.getEnrollmentDate());
            return cal.get(Calendar.YEAR) == year;
        });
    }
    
    /**
     * Find students enrolled between dates
     */
    public List<Student> findByEnrollmentDateBetween(Date startDate, Date endDate) {
        return findByPredicate(student -> {
            Date enrollmentDate = student.getEnrollmentDate();
            return enrollmentDate.compareTo(startDate) >= 0 && 
                   enrollmentDate.compareTo(endDate) <= 0;
        });
    }
    
    /**
     * Find students by multiple departments
     */
    public List<Student> findByDepartments(List<Department> departments) {
        Set<Department> deptSet = new HashSet<>(departments);
        return findByPredicate(student -> 
            student.getDepartment() != null && 
            deptSet.contains(student.getDepartment())
        );
    }
    
    /**
     * Find active students (those with recent activity)
     */
    public List<Student> findActiveStudents() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -6); // Active within last 6 months
        Date sixMonthsAgo = cal.getTime();
        
        return findByPredicate(student -> 
            student.getEnrollmentDate().after(sixMonthsAgo)
        );
    }
    
    /**
     * Get students grouped by department
     */
    public Map<Department, List<Student>> groupByDepartment() {
        return findAll().stream()
                .filter(student -> student.getDepartment() != null)
                .collect(Collectors.groupingBy(Student::getDepartment));
    }
    
    /**
     * Get students statistics by department
     */
    public Map<Department, Long> getStudentCountByDepartment() {
        return findAll().stream()
                .filter(student -> student.getDepartment() != null)
                .collect(Collectors.groupingBy(
                    Student::getDepartment,
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
                .collect(Collectors.groupingBy(
                    student -> {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(student.getEnrollmentDate());
                        return cal.get(Calendar.YEAR);
                    },
                    Collectors.counting()
                ));
    }
    
    /**
     * Find students enrolled in current academic year
     */
    public List<Student> findCurrentYearStudents() {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        
        // Academic year typically starts in August/September
        cal.set(currentYear, Calendar.AUGUST, 1);
        Date academicYearStart = cal.getTime();
        
        return findByPredicate(student -> 
            student.getEnrollmentDate().after(academicYearStart)
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
                matches &= student.getName().toLowerCase()
                          .contains(name.toLowerCase());
            }
            
            if (department != null) {
                matches &= student.getDepartment() != null && 
                          student.getDepartment().equals(department);
            }
            
            if (enrollmentDateFrom != null) {
                matches &= student.getEnrollmentDate()
                          .compareTo(enrollmentDateFrom) >= 0;
            }
            
            if (enrollmentDateTo != null) {
                matches &= student.getEnrollmentDate()
                          .compareTo(enrollmentDateTo) <= 0;
            }
            
            return matches;
        });
    }
    
    /**
     * Get recent enrollments (last N days)
     */
    public List<Student> getRecentEnrollments(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        Date cutoffDate = cal.getTime();
        
        return findByPredicate(student -> 
            student.getEnrollmentDate().after(cutoffDate)
        );
    }
}