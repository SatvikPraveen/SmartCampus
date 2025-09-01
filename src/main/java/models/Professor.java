// File: src/main/java/models/Professor.java
package models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Professor class extending User - demonstrates Inheritance and Polymorphism
 * 
 * Key Java concepts covered:
 * - Inheritance (extends User)
 * - Method overriding (@Override)
 * - Collections (ArrayList)
 * - Enums for type safety
 * - BigDecimal for precise financial calculations
 * - Additional business logic
 */
public class Professor extends User {
    
    // Enum for academic ranks
    public enum AcademicRank {
        ADJUNCT("Adjunct Professor", 1),
        ASSISTANT("Assistant Professor", 2),
        ASSOCIATE("Associate Professor", 3),
        FULL("Full Professor", 4),
        EMERITUS("Professor Emeritus", 5);
        
        private final String title;
        private final int level;
        
        AcademicRank(String title, int level) {
            this.title = title;
            this.level = level;
        }
        
        public String getTitle() { return title; }
        public int getLevel() { return level; }
        
        @Override
        public String toString() { return title; }
    }
    
    // Enum for employment status
    public enum EmploymentStatus {
        FULL_TIME("Full-time"),
        PART_TIME("Part-time"),
        CONTRACT("Contract"),
        SABBATICAL("On Sabbatical"),
        RETIRED("Retired");
        
        private final String description;
        
        EmploymentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return description; }
    }
    
    // Professor-specific fields
    private String professorId;
    private String departmentId;
    private AcademicRank academicRank;
    private EmploymentStatus employmentStatus;
    private BigDecimal salary;
    private LocalDate hireDate;
    private String officeLocation;
    private String specialization;
    private List<String> teachingCourseIds; // Courses currently teaching
    private List<String> qualifications; // Degrees, certifications
    private int yearsOfExperience;
    private double teachingRating; // Average student rating
    private boolean isTenured;
    private String researchArea;
    
    // Default constructor
    public Professor() {
        super();
        this.teachingCourseIds = new ArrayList<>();
        this.qualifications = new ArrayList<>();
        this.salary = BigDecimal.ZERO;
        this.yearsOfExperience = 0;
        this.teachingRating = 0.0;
        this.isTenured = false;
        this.hireDate = LocalDate.now();
        this.employmentStatus = EmploymentStatus.FULL_TIME;
        this.academicRank = AcademicRank.ASSISTANT;
    }
    
    // Parameterized constructor
    public Professor(String userId, String firstName, String lastName, String email, 
                    String phoneNumber, String professorId, String departmentId, 
                    AcademicRank academicRank, String specialization) {
        super(userId, firstName, lastName, email, phoneNumber);
        this.professorId = professorId;
        this.departmentId = departmentId;
        this.academicRank = academicRank;
        this.specialization = specialization;
        this.teachingCourseIds = new ArrayList<>();
        this.qualifications = new ArrayList<>();
        this.salary = BigDecimal.ZERO;
        this.yearsOfExperience = 0;
        this.teachingRating = 0.0;
        this.isTenured = false;
        this.hireDate = LocalDate.now();
        this.employmentStatus = EmploymentStatus.FULL_TIME;
    }
    
    // Polymorphism: Override abstract method from User
    @Override
    public String getRole() {
        return "PROFESSOR";
    }
    
    // Polymorphism: Override abstract method from User
    @Override
    public void displayInfo() {
        System.out.println("=== Professor Information ===");
        System.out.println("Professor ID: " + professorId);
        System.out.println("Name: " + getFullName());
        System.out.println("Title: " + academicRank.getTitle());
        System.out.println("Email: " + getEmail());
        System.out.println("Department ID: " + departmentId);
        System.out.println("Specialization: " + specialization);
        System.out.println("Employment Status: " + employmentStatus);
        System.out.println("Office: " + officeLocation);
        System.out.println("Years of Experience: " + yearsOfExperience);
        System.out.println("Teaching Rating: " + String.format("%.1f", teachingRating) + "/5.0");
        System.out.println("Tenured: " + (isTenured ? "Yes" : "No"));
        System.out.println("Courses Teaching: " + teachingCourseIds.size());
        System.out.println("Research Area: " + researchArea);
        System.out.println("Hire Date: " + hireDate);
        System.out.println("Status: " + (isActive() ? "Active" : "Inactive"));
    }
    
    // Professor-specific business methods
    public void assignCourse(String courseId) {
        if (courseId != null && !courseId.trim().isEmpty() && 
            !teachingCourseIds.contains(courseId) && canTeachMoreCourses()) {
            teachingCourseIds.add(courseId);
        }
    }
    
    public void removeCourse(String courseId) {
        teachingCourseIds.remove(courseId);
    }
    
    public boolean canTeachMoreCourses() {
        int maxCourses = employmentStatus == EmploymentStatus.FULL_TIME ? 4 : 2;
        return teachingCourseIds.size() < maxCourses;
    }
    
    public void addQualification(String qualification) {
        if (qualification != null && !qualification.trim().isEmpty() && 
            !qualifications.contains(qualification)) {
            qualifications.add(qualification.trim());
        }
    }
    
    public void updateTeachingRating(double newRating) {
        if (newRating >= 0.0 && newRating <= 5.0) {
            this.teachingRating = newRating;
        }
    }
    
    public void promote(AcademicRank newRank) {
        if (newRank.getLevel() > this.academicRank.getLevel()) {
            this.academicRank = newRank;
            // Tenure is typically granted with promotion to Associate Professor
            if (newRank == AcademicRank.ASSOCIATE || newRank == AcademicRank.FULL) {
                this.isTenured = true;
            }
        }
    }
    
    public void grantTenure() {
        if (academicRank == AcademicRank.ASSOCIATE || academicRank == AcademicRank.FULL) {
            this.isTenured = true;
        }
    }
    
    public boolean isEligibleForPromotion() {
        return yearsOfExperience >= 6 && teachingRating >= 3.5 && isTenured;
    }
    
    public void goOnSabbatical() {
        if (isTenured) {
            this.employmentStatus = EmploymentStatus.SABBATICAL;
        }
    }
    
    public void returnFromSabbatical() {
        if (employmentStatus == EmploymentStatus.SABBATICAL) {
            this.employmentStatus = EmploymentStatus.FULL_TIME;
        }
    }
    
    // Calculate annual workload score
    public double calculateWorkloadScore() {
        double courseLoad = teachingCourseIds.size() * 1.0;
        double experienceBonus = Math.min(yearsOfExperience * 0.1, 2.0);
        double rankMultiplier = academicRank.getLevel() * 0.2;
        return courseLoad + experienceBonus + rankMultiplier;
    }
    
    // Getters and Setters
    public String getProfessorId() {
        return professorId;
    }
    
    public void setProfessorId(String professorId) {
        if (professorId == null || professorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Professor ID cannot be null or empty");
        }
        this.professorId = professorId.trim();
    }
    
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    public AcademicRank getAcademicRank() {
        return academicRank;
    }
    
    public void setAcademicRank(AcademicRank academicRank) {
        this.academicRank = academicRank;
    }
    
    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }
    
    public void setEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }
    
    public BigDecimal getSalary() {
        return salary;
    }
    
    public void setSalary(BigDecimal salary) {
        if (salary != null && salary.compareTo(BigDecimal.ZERO) >= 0) {
            this.salary = salary;
        }
    }
    
    public LocalDate getHireDate() {
        return hireDate;
    }
    
    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
    
    public String getOfficeLocation() {
        return officeLocation;
    }
    
    public void setOfficeLocation(String officeLocation) {
        this.officeLocation = officeLocation;
    }
    
    public String getSpecialization() {
        return specialization;
    }
    
    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }
    
    public List<String> getTeachingCourseIds() {
        return new ArrayList<>(teachingCourseIds); // Defensive copy
    }
    
    public List<String> getQualifications() {
        return new ArrayList<>(qualifications); // Defensive copy
    }
    
    public int getYearsOfExperience() {
        return yearsOfExperience;
    }
    
    public void setYearsOfExperience(int yearsOfExperience) {
        if (yearsOfExperience >= 0) {
            this.yearsOfExperience = yearsOfExperience;
        }
    }
    
    public double getTeachingRating() {
        return teachingRating;
    }
    
    public void setTeachingRating(double teachingRating) {
        if (teachingRating >= 0.0 && teachingRating <= 5.0) {
            this.teachingRating = teachingRating;
        }
    }
    
    public boolean isTenured() {
        return isTenured;
    }
    
    public void setTenured(boolean tenured) {
        isTenured = tenured;
    }
    
    public String getResearchArea() {
        return researchArea;
    }
    
    public void setResearchArea(String researchArea) {
        this.researchArea = researchArea;
    }
    
    // Override equals to include professor-specific comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Professor professor = (Professor) obj;
        return Objects.equals(professorId, professor.professorId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), professorId);
    }
    
    // Override toString for better representation
    @Override
    public String toString() {
        return String.format("Professor{professorId='%s', name='%s', rank='%s', dept='%s', tenured=%s, active=%s}", 
            professorId, getFullName(), academicRank, departmentId, isTenured, isActive());
    }
}