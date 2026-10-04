// File location: src/main/java/repositories/ProfessorRepository.java

package repositories;

import models.Professor;
import models.Department;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repository for Professor entity operations
 * Provides specialized queries for professor data access
 */
public class ProfessorRepository extends BaseRepository<Professor, String> {
    
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    protected String extractId(Professor professor) {
        return professor.getProfessorId();
    }
    
    @Override
    protected void setId(Professor professor, String id) {
        professor.setProfessorId(id);
    }
    
    @Override
    protected String generateId() {
        return "PROF" + String.format("%06d", idGenerator.getAndIncrement());
    }
    
    // Specialized query methods for professors
    
    /**
     * Find professors by department
     */
    public List<Professor> findByDepartment(Department department) {
        return findByPredicate(professor -> 
            professor.getDepartmentId() != null && 
            professor.getDepartmentId().equals(department.getDepartmentId())
        );
    }
    
    /**
     * Find professor by email
     */
    public Optional<Professor> findByEmail(String email) {
        return findFirstByPredicate(professor -> 
            email.equals(professor.getEmail())
        );
    }
    
    /**
     * Find professors by specialization
     */
    public List<Professor> findBySpecialization(String specialization) {
        return findByPredicate(professor -> 
            professor.getSpecialization().toLowerCase()
                    .contains(specialization.toLowerCase())
        );
    }
    
    /**
     * Find professors by office location
     */
    public List<Professor> findByOfficeLocation(String officeLocation) {
        return findByPredicate(professor -> 
            professor.getOfficeLocation().toLowerCase()
                    .contains(officeLocation.toLowerCase())
        );
    }
    
    /**
     * Find professors by minimum years of experience
     */
    public List<Professor> findByMinExperience(int minYears) {
        return findByPredicate(professor -> 
            professor.getYearsOfExperience() >= minYears
        );
    }
    
    /**
     * Find professors within experience range
     */
    public List<Professor> findByExperienceRange(int minYears, int maxYears) {
        return findByPredicate(professor -> {
            int experience = professor.getYearsOfExperience();
            return experience >= minYears && experience <= maxYears;
        });
    }
    
    /**
     * Find professors by name pattern (case-insensitive)
     */
    public List<Professor> findByNameContaining(String namePattern) {
        String pattern = namePattern.toLowerCase();
        return findByPredicate(professor -> 
            professor.getFullName().toLowerCase().contains(pattern)
        );
    }
    
    /**
     * Find senior professors (experience > threshold)
     */
    public List<Professor> findSeniorProfessors(int experienceThreshold) {
        return findByPredicate(professor -> 
            professor.getYearsOfExperience() > experienceThreshold
        );
    }
    
    /**
     * Find junior professors (experience <= threshold)
     */
    public List<Professor> findJuniorProfessors(int experienceThreshold) {
        return findByPredicate(professor -> 
            professor.getYearsOfExperience() <= experienceThreshold
        );
    }
    
    /**
     * Group professors by department ID
     */
    public Map<String, List<Professor>> groupByDepartment() {
        return findAll().stream()
                .filter(professor -> professor.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Professor::getDepartmentId));
    }
    
    /**
     * Group professors by specialization
     */
    public Map<String, List<Professor>> groupBySpecialization() {
        return findAll().stream()
                .collect(Collectors.groupingBy(Professor::getSpecialization));
    }
    
    /**
     * Get professor count by department ID
     */
    public Map<String, Long> getProfessorCountByDepartment() {
        return findAll().stream()
                .filter(professor -> professor.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                    Professor::getDepartmentId,
                    Collectors.counting()
                ));
    }
    
    /**
     * Get average experience by department ID
     */
    public Map<String, Double> getAverageExperienceByDepartment() {
        return findAll().stream()
                .filter(professor -> professor.getDepartmentId() != null)
                .collect(Collectors.groupingBy(
                    Professor::getDepartmentId,
                    Collectors.averagingInt(Professor::getYearsOfExperience)
                ));
    }
    
    /**
     * Find professors by office building
     */
    public List<Professor> findByOfficeBuilding(String building) {
        return findByPredicate(professor -> 
            professor.getOfficeLocation().toLowerCase()
                    .startsWith(building.toLowerCase())
        );
    }
    
    /**
     * Find professors with multiple specializations
     */
    public List<Professor> findProfessorsWithMultipleSpecializations() {
        return findByPredicate(professor -> 
            professor.getSpecialization().contains(",") || 
            professor.getSpecialization().contains(";")
        );
    }
    
    /**
     * Search professors by multiple criteria
     */
    public List<Professor> searchProfessors(String name, Department department, 
                                          String specialization, 
                                          Integer minExperience, 
                                          Integer maxExperience) {
        return findByPredicate(professor -> {
            boolean matches = true;
            
            if (name != null && !name.trim().isEmpty()) {
                matches &= professor.getFullName().toLowerCase()
                          .contains(name.toLowerCase());
            }
            
            if (department != null) {
                matches &= professor.getDepartmentId() != null && 
                          professor.getDepartmentId().equals(department.getDepartmentId());
            }
            
            if (specialization != null && !specialization.trim().isEmpty()) {
                matches &= professor.getSpecialization().toLowerCase()
                          .contains(specialization.toLowerCase());
            }
            
            if (minExperience != null) {
                matches &= professor.getYearsOfExperience() >= minExperience;
            }
            
            if (maxExperience != null) {
                matches &= professor.getYearsOfExperience() <= maxExperience;
            }
            
            return matches;
        });
    }
    
    /**
     * Find professors by email domain
     */
    public List<Professor> findByEmailDomain(String domain) {
        return findByPredicate(professor -> 
            professor.getEmail().toLowerCase().endsWith("@" + domain.toLowerCase())
        );
    }
    
    /**
     * Get experience statistics
     */
    public Map<String, Object> getExperienceStatistics() {
        List<Professor> professors = findAll();
        
        OptionalDouble avgExperience = professors.stream()
                .mapToInt(Professor::getYearsOfExperience)
                .average();
        
        OptionalInt maxExperience = professors.stream()
                .mapToInt(Professor::getYearsOfExperience)
                .max();
        
        OptionalInt minExperience = professors.stream()
                .mapToInt(Professor::getYearsOfExperience)
                .min();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProfessors", professors.size());
        stats.put("averageExperience", avgExperience.orElse(0.0));
        stats.put("maxExperience", maxExperience.orElse(0));
        stats.put("minExperience", minExperience.orElse(0));
        
        return stats;
    }
    
    /**
     * Find professors by office floor
     */
    public List<Professor> findByOfficeFloor(String floor) {
        return findByPredicate(professor -> 
            professor.getOfficeLocation().toLowerCase().contains(floor.toLowerCase())
        );
    }
}