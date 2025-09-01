// File: src/main/java/models/Enrollment.java
package models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Enrollment class representing the relationship between students and courses.
 * This class manages student course registrations and related information.
 * 
 * Key Java concepts demonstrated:
 * - Enums for type safety and constants
 * - Association between entities (Student and Course)
 * - Encapsulation with private fields
 * - Constructor overloading
 * - Method overriding (equals, hashCode, toString)
 * - Date/time handling with LocalDateTime
 * - Business logic validation
 * - Static factory methods
 */
public class Enrollment {
    
    // Enrollment status enumeration
    public enum EnrollmentStatus {
        ENROLLED("Enrolled"),
        WAITLISTED("Waitlisted"),
        DROPPED("Dropped"),
        COMPLETED("Completed"),
        FAILED("Failed"),
        WITHDRAWN("Withdrawn"),
        AUDIT("Audit");
        
        private final String displayName;
        
        EnrollmentStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Enrollment type enumeration
    public enum EnrollmentType {
        REGULAR("Regular"),
        AUDIT("Audit"),
        CREDIT_NO_CREDIT("Credit/No Credit"),
        INDEPENDENT_STUDY("Independent Study"),
        INTERNSHIP("Internship"),
        RESEARCH("Research");
        
        private final String displayName;
        
        EnrollmentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Grade scale enumeration
    public enum Grade {
        A_PLUS("A+", 4.0),
        A("A", 4.0),
        A_MINUS("A-", 3.7),
        B_PLUS("B+", 3.3),
        B("B", 3.0),
        B_MINUS("B-", 2.7),
        C_PLUS("C+", 2.3),
        C("C", 2.0),
        C_MINUS("C-", 1.7),
        D_PLUS("D+", 1.3),
        D("D", 1.0),
        D_MINUS("D-", 0.7),
        F("F", 0.0),
        INCOMPLETE("I", 0.0),
        PASS("P", 0.0),
        NO_PASS("NP", 0.0),
        WITHDRAW("W", 0.0),
        NOT_GRADED("NG", 0.0);
        
        private final String letter;
        private final double points;
        
        Grade(String letter, double points) {
            this.letter = letter;
            this.points = points;
        }
        
        public String getLetter() { return letter; }
        public double getPoints() { return points; }
    }
    
    // Fields
    private String enrollmentId;
    private String studentId;
    private String courseId;
    private String semester;
    private int year;
    private EnrollmentStatus status;
    private EnrollmentType enrollmentType;
    private Grade grade;
    private double numericGrade; // 0-100 scale
    private LocalDateTime enrollmentDate;
    private LocalDateTime lastModified;
    private String enrolledBy; // Admin or system user who processed enrollment
    private String section;
    private int creditHours;
    private boolean isActive;
    private String notes;
    private int attendancePercentage;
    private boolean isRetake;
    private String prerequisiteWaiver;
    
    // Constructors
    
    /**
     * Default constructor.
     */
    public Enrollment() {
        this.enrollmentDate = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.status = EnrollmentStatus.ENROLLED;
        this.enrollmentType = EnrollmentType.REGULAR;
        this.isActive = true;
        this.attendancePercentage = 0;
        this.numericGrade = -1; // -1 indicates no grade assigned
    }
    
    /**
     * Constructor with basic enrollment information.
     */
    public Enrollment(String enrollmentId, String studentId, String courseId, String semester, int year) {
        this();
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.year = year;
    }
    
    /**
     * Constructor with enrollment details.
     */
    public Enrollment(String enrollmentId, String studentId, String courseId, String semester, int year,
                     EnrollmentStatus status, EnrollmentType enrollmentType, String section, int creditHours) {
        this(enrollmentId, studentId, courseId, semester, year);
        this.status = status;
        this.enrollmentType = enrollmentType;
        this.section = section;
        this.creditHours = creditHours;
    }
    
    /**
     * Full constructor with all details.
     */
    public Enrollment(String enrollmentId, String studentId, String courseId, String semester, int year,
                     EnrollmentStatus status, EnrollmentType enrollmentType, String section, int creditHours,
                     String enrolledBy, boolean isRetake) {
        this(enrollmentId, studentId, courseId, semester, year, status, enrollmentType, section, creditHours);
        this.enrolledBy = enrolledBy;
        this.isRetake = isRetake;
    }
    
    // Static factory methods
    
    /**
     * Create a new enrollment with generated ID.
     */
    public static Enrollment createEnrollment(String studentId, String courseId, String semester, int year) {
        String enrollmentId = generateEnrollmentId(studentId, courseId, semester, year);
        return new Enrollment(enrollmentId, studentId, courseId, semester, year);
    }
    
    /**
     * Create a waitlisted enrollment.
     */
    public static Enrollment createWaitlistedEnrollment(String studentId, String courseId, String semester, int year) {
        Enrollment enrollment = createEnrollment(studentId, courseId, semester, year);
        enrollment.setStatus(EnrollmentStatus.WAITLISTED);
        return enrollment;
    }
    
    /**
     * Create an audit enrollment.
     */
    public static Enrollment createAuditEnrollment(String studentId, String courseId, String semester, int year) {
        Enrollment enrollment = createEnrollment(studentId, courseId, semester, year);
        enrollment.setStatus(EnrollmentStatus.AUDIT);
        enrollment.setEnrollmentType(EnrollmentType.AUDIT);
        return enrollment;
    }
    
    // Business logic methods
    
    /**
     * Assign a letter grade to the enrollment.
     */
    public boolean assignGrade(Grade grade) {
        if (canBeGraded()) {
            this.grade = grade;
            this.lastModified = LocalDateTime.now();
            
            // Update status based on grade
            if (grade == Grade.INCOMPLETE) {
                // Status remains as enrolled for incomplete
            } else if (grade == Grade.F) {
                this.status = EnrollmentStatus.FAILED;
            } else if (grade == Grade.WITHDRAW) {
                this.status = EnrollmentStatus.WITHDRAWN;
            } else if (grade != Grade.NOT_GRADED) {
                this.status = EnrollmentStatus.COMPLETED;
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Assign a numeric grade (0-100 scale).
     */
    public boolean assignNumericGrade(double numericGrade) {
        if (canBeGraded() && numericGrade >= 0 && numericGrade <= 100) {
            this.numericGrade = numericGrade;
            this.lastModified = LocalDateTime.now();
            
            // Auto-assign letter grade based on numeric grade
            this.grade = convertNumericToLetterGrade(numericGrade);
            
            return true;
        }
        return false;
    }
    
    /**
     * Drop the enrollment.
     */
    public boolean dropEnrollment(String reason) {
        if (status == EnrollmentStatus.ENROLLED || status == EnrollmentStatus.WAITLISTED) {
            this.status = EnrollmentStatus.DROPPED;
            this.isActive = false;
            this.lastModified = LocalDateTime.now();
            if (reason != null && !reason.trim().isEmpty()) {
                this.notes = (this.notes != null ? this.notes + "; " : "") + "Dropped: " + reason;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Withdraw from the course.
     */
    public boolean withdrawFromCourse(String reason) {
        if (status == EnrollmentStatus.ENROLLED) {
            this.status = EnrollmentStatus.WITHDRAWN;
            this.grade = Grade.WITHDRAW;
            this.lastModified = LocalDateTime.now();
            if (reason != null && !reason.trim().isEmpty()) {
                this.notes = (this.notes != null ? this.notes + "; " : "") + "Withdrawn: " + reason;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Move from waitlist to enrolled.
     */
    public boolean enrollFromWaitlist() {
        if (status == EnrollmentStatus.WAITLISTED) {
            this.status = EnrollmentStatus.ENROLLED;
            this.lastModified = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * Update attendance percentage.
     */
    public boolean updateAttendance(int attendancePercentage) {
        if (attendancePercentage >= 0 && attendancePercentage <= 100) {
            this.attendancePercentage = attendancePercentage;
            this.lastModified = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * Check if enrollment can be graded.
     */
    public boolean canBeGraded() {
        return status == EnrollmentStatus.ENROLLED && enrollmentType != EnrollmentType.AUDIT;
    }
    
    /**
     * Check if enrollment is currently active.
     */
    public boolean isCurrentlyActive() {
        return isActive && (status == EnrollmentStatus.ENROLLED || status == EnrollmentStatus.WAITLISTED);
    }
    
    /**
     * Get GPA points for this enrollment.
     */
    public double getGpaPoints() {
        if (grade != null && enrollmentType == EnrollmentType.REGULAR) {
            return grade.getPoints() * creditHours;
        }
        return 0.0;
    }
    
    /**
     * Check if this enrollment counts toward GPA.
     */
    public boolean countsTowardGpa() {
        return enrollmentType == EnrollmentType.REGULAR && 
               grade != null && 
               grade != Grade.PASS && 
               grade != Grade.NO_PASS && 
               grade != Grade.WITHDRAW &&
               grade != Grade.INCOMPLETE &&
               grade != Grade.NOT_GRADED;
    }
    
    // Helper methods
    
    /**
     * Generate enrollment ID based on student, course, semester, and year.
     */
    private static String generateEnrollmentId(String studentId, String courseId, String semester, int year) {
        return String.format("ENR_%s_%s_%s_%d_%d", 
            studentId, courseId, semester, year, System.currentTimeMillis() % 10000);
    }
    
    /**
     * Convert numeric grade to letter grade.
     */
    private Grade convertNumericToLetterGrade(double numericGrade) {
        if (numericGrade >= 97) return Grade.A_PLUS;
        else if (numericGrade >= 93) return Grade.A;
        else if (numericGrade >= 90) return Grade.A_MINUS;
        else if (numericGrade >= 87) return Grade.B_PLUS;
        else if (numericGrade >= 83) return Grade.B;
        else if (numericGrade >= 80) return Grade.B_MINUS;
        else if (numericGrade >= 77) return Grade.C_PLUS;
        else if (numericGrade >= 73) return Grade.C;
        else if (numericGrade >= 70) return Grade.C_MINUS;
        else if (numericGrade >= 67) return Grade.D_PLUS;
        else if (numericGrade >= 63) return Grade.D;
        else if (numericGrade >= 60) return Grade.D_MINUS;
        else return Grade.F;
    }
    
    // Getters and Setters
    
    public String getEnrollmentId() {
        return enrollmentId;
    }
    
    public void setEnrollmentId(String enrollmentId) {
        this.enrollmentId = enrollmentId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getSemester() {
        return semester;
    }
    
    public void setSemester(String semester) {
        this.semester = semester;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public EnrollmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(EnrollmentStatus status) {
        this.status = status;
        this.lastModified = LocalDateTime.now();
    }
    
    public EnrollmentType getEnrollmentType() {
        return enrollmentType;
    }
    
    public void setEnrollmentType(EnrollmentType enrollmentType) {
        this.enrollmentType = enrollmentType;
    }
    
    public Grade getGrade() {
        return grade;
    }
    
    public void setGrade(Grade grade) {
        this.grade = grade;
        this.lastModified = LocalDateTime.now();
    }
    
    public double getNumericGrade() {
        return numericGrade;
    }
    
    public void setNumericGrade(double numericGrade) {
        if (numericGrade >= -1 && numericGrade <= 100) {
            this.numericGrade = numericGrade;
            this.lastModified = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public String getEnrolledBy() {
        return enrolledBy;
    }
    
    public void setEnrolledBy(String enrolledBy) {
        this.enrolledBy = enrolledBy;
    }
    
    public String getSection() {
        return section;
    }
    
    public void setSection(String section) {
        this.section = section;
    }
    
    public int getCreditHours() {
        return creditHours;
    }
    
    public void setCreditHours(int creditHours) {
        if (creditHours >= 0) {
            this.creditHours = creditHours;
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        this.lastModified = LocalDateTime.now();
    }
    
    public int getAttendancePercentage() {
        return attendancePercentage;
    }
    
    public void setAttendancePercentage(int attendancePercentage) {
        if (attendancePercentage >= 0 && attendancePercentage <= 100) {
            this.attendancePercentage = attendancePercentage;
            this.lastModified = LocalDateTime.now();
        }
    }
    
    public boolean isRetake() {
        return isRetake;
    }
    
    public void setRetake(boolean retake) {
        isRetake = retake;
    }
    
    public String getPrerequisiteWaiver() {
        return prerequisiteWaiver;
    }
    
    public void setPrerequisiteWaiver(String prerequisiteWaiver) {
        this.prerequisiteWaiver = prerequisiteWaiver;
    }
    
    // Object overrides
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Enrollment that = (Enrollment) obj;
        return Objects.equals(enrollmentId, that.enrollmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enrollmentId);
    }
    
    @Override
    public String toString() {
        return String.format("Enrollment{id='%s', student='%s', course='%s', semester='%s %d', status='%s', grade='%s'}", 
            enrollmentId, studentId, courseId, semester, year, status.getDisplayName(), 
            grade != null ? grade.getLetter() : "Not Graded");
    }
}