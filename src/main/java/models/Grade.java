// File: src/main/java/models/Grade.java
package models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Grade class representing individual grade entries for students in courses.
 * This class manages detailed grading information beyond the basic grade in Enrollment.
 * 
 * Key Java concepts demonstrated:
 * - Enums for type safety
 * - Composition relationship with Enrollment
 * - Date/time handling
 * - Business logic validation
 * - Method overloading
 * - Static factory methods
 * - Encapsulation and data validation
 */
public class Grade {
    
    // Grade component types
    public enum GradeComponent {
        EXAM("Exam"),
        QUIZ("Quiz"),
        HOMEWORK("Homework"),
        PROJECT("Project"),
        LAB("Lab"),
        PARTICIPATION("Participation"),
        PRESENTATION("Presentation"),
        MIDTERM("Midterm"),
        FINAL("Final"),
        ATTENDANCE("Attendance"),
        DISCUSSION("Discussion"),
        RESEARCH("Research"),
        FIELD_WORK("Field Work"),
        INTERNSHIP("Internship"),
        THESIS("Thesis"),
        OTHER("Other");
        
        private final String displayName;
        
        GradeComponent(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Grade status enumeration
    public enum GradeStatus {
        DRAFT("Draft"),
        SUBMITTED("Submitted"),
        GRADED("Graded"),
        RETURNED("Returned"),
        LATE("Late"),
        EXCUSED("Excused"),
        MISSING("Missing"),
        INVALID("Invalid");
        
        private final String displayName;
        
        GradeStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Fields
    private String gradeId;
    private String enrollmentId;
    private String studentId;
    private String courseId;
    private String assignmentId;
    private String assignmentName;
    private GradeComponent component;
    private double pointsEarned;
    private double pointsPossible;
    private double percentage;
    private String letterGrade;
    private GradeStatus status;
    private LocalDateTime dateAssigned;
    private LocalDateTime dateDue;
    private LocalDateTime dateSubmitted;
    private LocalDateTime dateGraded;
    private LocalDateTime lastModified;
    private String gradedBy; // Professor or TA ID
    private String feedback;
    private String rubric;
    private boolean isExtraCredit;
    private boolean isDropped; // For dropping lowest grade scenarios
    private double weight; // Weight in final grade calculation
    private int attemptNumber; // For multiple attempts
    private String semester;
    private int year;
    private String notes;
    
    // Constructors
    
    /**
     * Default constructor.
     */
    public Grade() {
        this.dateAssigned = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.status = GradeStatus.DRAFT;
        this.attemptNumber = 1;
        this.weight = 1.0;
        this.pointsEarned = -1; // -1 indicates not graded yet
    }
    
    /**
     * Constructor with basic grade information.
     */
    public Grade(String gradeId, String enrollmentId, String studentId, String courseId, 
                String assignmentName, GradeComponent component) {
        this();
        this.gradeId = gradeId;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.assignmentName = assignmentName;
        this.component = component;
    }
    
    /**
     * Constructor with assignment details.
     */
    public Grade(String gradeId, String enrollmentId, String studentId, String courseId,
                String assignmentId, String assignmentName, GradeComponent component,
                double pointsPossible, LocalDateTime dateDue) {
        this(gradeId, enrollmentId, studentId, courseId, assignmentName, component);
        this.assignmentId = assignmentId;
        this.pointsPossible = pointsPossible;
        this.dateDue = dateDue;
    }
    
    /**
     * Full constructor with all details.
     */
    public Grade(String gradeId, String enrollmentId, String studentId, String courseId,
                String assignmentId, String assignmentName, GradeComponent component,
                double pointsEarned, double pointsPossible, LocalDateTime dateDue,
                String gradedBy, String semester, int year) {
        this(gradeId, enrollmentId, studentId, courseId, assignmentId, assignmentName, component, pointsPossible, dateDue);
        this.pointsEarned = pointsEarned;
        this.gradedBy = gradedBy;
        this.semester = semester;
        this.year = year;
        calculatePercentageAndLetter();
    }
    
    // Static factory methods
    
    /**
     * Create a new grade entry with generated ID.
     */
    public static Grade createGrade(String enrollmentId, String studentId, String courseId,
                                   String assignmentName, GradeComponent component, double pointsPossible) {
        String gradeId = generateGradeId(studentId, courseId, assignmentName);
        Grade grade = new Grade(gradeId, enrollmentId, studentId, courseId, assignmentName, component);
        grade.setPointsPossible(pointsPossible);
        return grade;
    }
    
    /**
     * Create an extra credit grade.
     */
    public static Grade createExtraCreditGrade(String enrollmentId, String studentId, String courseId,
                                              String assignmentName, double pointsPossible) {
        Grade grade = createGrade(enrollmentId, studentId, courseId, assignmentName, GradeComponent.OTHER, pointsPossible);
        grade.setExtraCredit(true);
        return grade;
    }
    
    // Business logic methods
    
    /**
     * Submit the assignment.
     */
    public boolean submitAssignment() {
        if (status == GradeStatus.DRAFT || status == GradeStatus.MISSING) {
            this.status = GradeStatus.SUBMITTED;
            this.dateSubmitted = LocalDateTime.now();
            this.lastModified = LocalDateTime.now();
            
            // Check if late
            if (dateDue != null && dateSubmitted.isAfter(dateDue)) {
                this.status = GradeStatus.LATE;
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Grade the assignment.
     */
    public boolean gradeAssignment(double pointsEarned, String gradedBy, String feedback) {
        if (canBeGraded() && pointsEarned >= 0 && pointsEarned <= pointsPossible) {
            this.pointsEarned = pointsEarned;
            this.gradedBy = gradedBy;
            this.feedback = feedback;
            this.status = GradeStatus.GRADED;
            this.dateGraded = LocalDateTime.now();
            this.lastModified = LocalDateTime.now();
            
            calculatePercentageAndLetter();
            return true;
        }
        return false;
    }
    
    /**
     * Return graded assignment to student.
     */
    public boolean returnToStudent() {
        if (status == GradeStatus.GRADED) {
            this.status = GradeStatus.RETURNED;
            this.lastModified = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * Excuse the assignment.
     */
    public boolean excuseAssignment(String reason) {
        this.status = GradeStatus.EXCUSED;
        this.lastModified = LocalDateTime.now();
        if (reason != null && !reason.trim().isEmpty()) {
            this.notes = (this.notes != null ? this.notes + "; " : "") + "Excused: " + reason;
        }
        return true;
    }
    
    /**
     * Mark assignment as missing.
     */
    public boolean markAsMissing() {
        if (status == GradeStatus.DRAFT && dateDue != null && LocalDateTime.now().isAfter(dateDue)) {
            this.status = GradeStatus.MISSING;
            this.pointsEarned = 0.0;
            this.lastModified = LocalDateTime.now();
            calculatePercentageAndLetter();
            return true;
        }
        return false;
    }
    
    /**
     * Drop this grade from calculations.
     */
    public void dropGrade() {
        this.isDropped = true;
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Include this grade in calculations.
     */
    public void includeGrade() {
        this.isDropped = false;
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Check if grade can be graded.
     */
    public boolean canBeGraded() {
        return status == GradeStatus.SUBMITTED || status == GradeStatus.LATE || status == GradeStatus.GRADED;
    }
    
    /**
     * Check if grade is complete and counts toward final grade.
     */
    public boolean countsTowardFinalGrade() {
        return !isDropped && 
               (status == GradeStatus.GRADED || status == GradeStatus.RETURNED) &&
               status != GradeStatus.EXCUSED &&
               pointsEarned >= 0;
    }
    
    /**
     * Check if assignment is overdue.
     */
    public boolean isOverdue() {
        return dateDue != null && 
               LocalDateTime.now().isAfter(dateDue) && 
               (status == GradeStatus.DRAFT || status == GradeStatus.MISSING);
    }
    
    /**
     * Get weighted points for final grade calculation.
     */
    public double getWeightedPoints() {
        if (countsTowardFinalGrade()) {
            return pointsEarned * weight;
        }
        return 0.0;
    }
    
    /**
     * Get weighted possible points.
     */
    public double getWeightedPossiblePoints() {
        if (!isDropped && status != GradeStatus.EXCUSED) {
            return pointsPossible * weight;
        }
        return 0.0;
    }
    
    // Helper methods
    
    /**
     * Calculate percentage and letter grade based on points.
     */
    private void calculatePercentageAndLetter() {
        if (pointsEarned >= 0 && pointsPossible > 0) {
            this.percentage = (pointsEarned / pointsPossible) * 100.0;
            this.letterGrade = convertPercentageToLetter(percentage);
        }
    }
    
    /**
     * Convert percentage to letter grade.
     */
    private String convertPercentageToLetter(double percentage) {
        if (percentage >= 97) return "A+";
        else if (percentage >= 93) return "A";
        else if (percentage >= 90) return "A-";
        else if (percentage >= 87) return "B+";
        else if (percentage >= 83) return "B";
        else if (percentage >= 80) return "B-";
        else if (percentage >= 77) return "C+";
        else if (percentage >= 73) return "C";
        else if (percentage >= 70) return "C-";
        else if (percentage >= 67) return "D+";
        else if (percentage >= 63) return "D";
        else if (percentage >= 60) return "D-";
        else return "F";
    }
    
    /**
     * Generate grade ID.
     */
    private static String generateGradeId(String studentId, String courseId, String assignmentName) {
        return String.format("GRD_%s_%s_%s_%d", 
            studentId, courseId, assignmentName.replaceAll("\\s+", "_"), System.currentTimeMillis() % 10000);
    }
    
    // Getters and Setters
    
    public String getGradeId() {
        return gradeId;
    }
    
    public void setGradeId(String gradeId) {
        this.gradeId = gradeId;
    }
    
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
    
    public String getAssignmentId() {
        return assignmentId;
    }
    
    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }
    
    public String getAssignmentName() {
        return assignmentName;
    }
    
    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }
    
    public GradeComponent getComponent() {
        return component;
    }
    
    public void setComponent(GradeComponent component) {
        this.component = component;
    }
    
    public double getPointsEarned() {
        return pointsEarned;
    }
    
    public void setPointsEarned(double pointsEarned) {
        if (pointsEarned >= 0 && pointsEarned <= pointsPossible) {
            this.pointsEarned = pointsEarned;
            this.lastModified = LocalDateTime.now();
            calculatePercentageAndLetter();
        }
    }
    
    public double getPointsPossible() {
        return pointsPossible;
    }
    
    public void setPointsPossible(double pointsPossible) {
        if (pointsPossible > 0) {
            this.pointsPossible = pointsPossible;
            if (this.pointsEarned >= 0) {
                calculatePercentageAndLetter();
            }
        }
    }
    
    public double getPercentage() {
        return percentage;
    }
    
    public String getLetterGrade() {
        return letterGrade;
    }
    
    public GradeStatus getStatus() {
        return status;
    }
    
    public void setStatus(GradeStatus status) {
        this.status = status;
        this.lastModified = LocalDateTime.now();
    }
    
    public LocalDateTime getDateAssigned() {
        return dateAssigned;
    }
    
    public void setDateAssigned(LocalDateTime dateAssigned) {
        this.dateAssigned = dateAssigned;
    }
    
    public LocalDateTime getDateDue() {
        return dateDue;
    }
    
    public void setDateDue(LocalDateTime dateDue) {
        this.dateDue = dateDue;
    }
    
    public LocalDateTime getDateSubmitted() {
        return dateSubmitted;
    }
    
    public void setDateSubmitted(LocalDateTime dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }
    
    public LocalDateTime getDateGraded() {
        return dateGraded;
    }
    
    public void setDateGraded(LocalDateTime dateGraded) {
        this.dateGraded = dateGraded;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public String getGradedBy() {
        return gradedBy;
    }
    
    public void setGradedBy(String gradedBy) {
        this.gradedBy = gradedBy;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getRubric() {
        return rubric;
    }
    
    public void setRubric(String rubric) {
        this.rubric = rubric;
    }
    
    public boolean isExtraCredit() {
        return isExtraCredit;
    }
    
    public void setExtraCredit(boolean extraCredit) {
        isExtraCredit = extraCredit;
    }
    
    public boolean isDropped() {
        return isDropped;
    }
    
    public void setDropped(boolean dropped) {
        isDropped = dropped;
        this.lastModified = LocalDateTime.now();
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        if (weight >= 0) {
            this.weight = weight;
        }
    }
    
    public int getAttemptNumber() {
        return attemptNumber;
    }
    
    public void setAttemptNumber(int attemptNumber) {
        if (attemptNumber > 0) {
            this.attemptNumber = attemptNumber;
        }
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
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        this.lastModified = LocalDateTime.now();
    }
    
    // Object overrides
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Grade grade = (Grade) obj;
        return Objects.equals(gradeId, grade.gradeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(gradeId);
    }
    
    @Override
    public String toString() {
        return String.format("Grade{id='%s', assignment='%s', student='%s', points=%.1f/%.1f (%.1f%%), status='%s'}", 
            gradeId, assignmentName, studentId, pointsEarned, pointsPossible, percentage, status.getDisplayName());
    }
}