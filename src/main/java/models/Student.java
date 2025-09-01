// File: src/main/java/models/Student.java
package models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Student class extending User - demonstrates Inheritance and Polymorphism
 * 
 * Key Java concepts covered:
 * - Inheritance (extends User)
 * - Method overriding (@Override)
 * - Collections (ArrayList)
 * - Enums for type safety
 * - Additional encapsulation
 * - Polymorphic behavior
 */
public class Student extends User {
    
    // Enum for academic year - demonstrates type safety
    public enum AcademicYear {
        FRESHMAN(1, "Freshman"),
        SOPHOMORE(2, "Sophomore"), 
        JUNIOR(3, "Junior"),
        SENIOR(4, "Senior"),
        GRADUATE(5, "Graduate");
        
        private final int year;
        private final String displayName;
        
        AcademicYear(int year, String displayName) {
            this.year = year;
            this.displayName = displayName;
        }
        
        public int getYear() { return year; }
        public String getDisplayName() { return displayName; }
        
        @Override
        public String toString() { return displayName; }
    }
    
    // Student-specific fields
    private String studentId;
    private String major;
    private AcademicYear academicYear;
    private double gpa;
    private LocalDate enrollmentDate;
    private String departmentId;
    private List<String> enrolledCourseIds; // Will hold course IDs
    private List<String> completedCourseIds;
    private int totalCredits;
    private boolean isOnProbation;
    
    // Default constructor
    public Student() {
        super();
        this.enrolledCourseIds = new ArrayList<>();
        this.completedCourseIds = new ArrayList<>();
        this.gpa = 0.0;
        this.totalCredits = 0;
        this.isOnProbation = false;
        this.enrollmentDate = LocalDate.now();
    }
    
    // Parameterized constructor
    public Student(String userId, String firstName, String lastName, String email, 
                   String phoneNumber, String studentId, String major, AcademicYear academicYear) {
        super(userId, firstName, lastName, email, phoneNumber);
        this.studentId = studentId;
        this.major = major;
        this.academicYear = academicYear;
        this.enrolledCourseIds = new ArrayList<>();
        this.completedCourseIds = new ArrayList<>();
        this.gpa = 0.0;
        this.totalCredits = 0;
        this.isOnProbation = false;
        this.enrollmentDate = LocalDate.now();
    }
    
    // Polymorphism: Override abstract method from User
    @Override
    public String getRole() {
        return "STUDENT";
    }
    
    // Polymorphism: Override abstract method from User
    @Override
    public void displayInfo() {
        System.out.println("=== Student Information ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Name: " + getFullName());
        System.out.println("Email: " + getEmail());
        System.out.println("Major: " + major);
        System.out.println("Academic Year: " + academicYear);
        System.out.println("GPA: " + String.format("%.2f", gpa));
        System.out.println("Total Credits: " + totalCredits);
        System.out.println("Enrolled Courses: " + enrolledCourseIds.size());
        System.out.println("Completed Courses: " + completedCourseIds.size());
        System.out.println("On Probation: " + (isOnProbation ? "Yes" : "No"));
        System.out.println("Enrollment Date: " + enrollmentDate);
        System.out.println("Status: " + (isActive() ? "Active" : "Inactive"));
    }
    
    // Student-specific business methods
    public void enrollInCourse(String courseId) {
        if (courseId != null && !courseId.trim().isEmpty() && 
            !enrolledCourseIds.contains(courseId)) {
            enrolledCourseIds.add(courseId);
        }
    }
    
    public void dropCourse(String courseId) {
        enrolledCourseIds.remove(courseId);
    }
    
    public void completeCourse(String courseId, double grade) {
        if (enrolledCourseIds.contains(courseId)) {
            enrolledCourseIds.remove(courseId);
            completedCourseIds.add(courseId);
            // Simple GPA calculation (can be enhanced)
            updateGPA(grade);
        }
    }
    
    private void updateGPA(double newGrade) {
        if (completedCourseIds.size() == 1) {
            this.gpa = newGrade;
        } else {
            // Simple average calculation (in real system, would weight by credits)
            this.gpa = ((gpa * (completedCourseIds.size() - 1)) + newGrade) / completedCourseIds.size();
        }
        
        // Check if student should be on probation
        this.isOnProbation = this.gpa < 2.0;
    }
    
    public boolean canEnrollInCourse(String courseId) {
        return isActive() && 
               !isOnProbation && 
               !enrolledCourseIds.contains(courseId) && 
               !completedCourseIds.contains(courseId) &&
               enrolledCourseIds.size() < 6; // Max 6 courses per semester
    }
    
    public void promoteAcademicYear() {
        if (totalCredits >= 30 && academicYear == AcademicYear.FRESHMAN) {
            academicYear = AcademicYear.SOPHOMORE;
        } else if (totalCredits >= 60 && academicYear == AcademicYear.SOPHOMORE) {
            academicYear = AcademicYear.JUNIOR;
        } else if (totalCredits >= 90 && academicYear == AcademicYear.JUNIOR) {
            academicYear = AcademicYear.SENIOR;
        }
    }
    
    // Getters and Setters
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        this.studentId = studentId.trim();
    }
    
    public String getMajor() {
        return major;
    }
    
    public void setMajor(String major) {
        this.major = major;
    }
    
    public AcademicYear getAcademicYear() {
        return academicYear;
    }
    
    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }
    
    public double getGpa() {
        return gpa;
    }
    
    public void setGpa(double gpa) {
        if (gpa < 0.0 || gpa > 4.0) {
            throw new IllegalArgumentException("GPA must be between 0.0 and 4.0");
        }
        this.gpa = gpa;
        this.isOnProbation = gpa < 2.0;
    }
    
    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    public List<String> getEnrolledCourseIds() {
        return new ArrayList<>(enrolledCourseIds); // Return defensive copy
    }
    
    public List<String> getCompletedCourseIds() {
        return new ArrayList<>(completedCourseIds); // Return defensive copy
    }
    
    public int getTotalCredits() {
        return totalCredits;
    }
    
    public void setTotalCredits(int totalCredits) {
        if (totalCredits < 0) {
            throw new IllegalArgumentException("Total credits cannot be negative");
        }
        this.totalCredits = totalCredits;
        promoteAcademicYear(); // Auto-promote if eligible
    }
    
    public boolean isOnProbation() {
        return isOnProbation;
    }
    
    public void setOnProbation(boolean onProbation) {
        isOnProbation = onProbation;
    }
    
    // Override equals to include student-specific comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Student student = (Student) obj;
        return Objects.equals(studentId, student.studentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), studentId);
    }
    
    // Override toString for better representation
    @Override
    public String toString() {
        return String.format("Student{studentId='%s', name='%s', major='%s', year='%s', gpa=%.2f, active=%s}", 
            studentId, getFullName(), major, academicYear, gpa, isActive());
    }
}