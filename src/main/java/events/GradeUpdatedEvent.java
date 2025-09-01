// File location: src/main/java/events/GradeUpdatedEvent.java
package events;

import enums.GradeLevel;
import enums.Semester;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event fired when a student's grade is updated in a course
 * Contains all relevant grade information for event processing and audit trail
 */
public class GradeUpdatedEvent extends Event {
    
    public static final String EVENT_TYPE = "GradeUpdated";
    
    // Event payload
    private final String studentId;
    private final String studentName;
    private final String courseId;
    private final String courseName;
    private final String courseCode;
    private final Semester semester;
    private final int academicYear;
    private final GradeLevel newGrade;
    private final GradeLevel previousGrade;
    private final double newPercentage;
    private final double previousPercentage;
    private final String gradedBy;
    private final LocalDateTime gradedDate;
    private final String gradeComments;
    private final String updateReason;
    private final boolean isFinalGrade;
    private final String assignmentName;
    private final String departmentCode;
    private final String instructorId;
    private final String instructorName;
    private final int courseCredits;
    private final Double newGpaPoints;
    private final Double previousGpaPoints;
    private final boolean affectsGpa;
    private final String gradeType;
    private final String approvedBy;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new GradeUpdatedEvent with basic grade information
     */
    public GradeUpdatedEvent(String studentId, String studentName, String courseId,
                            String courseName, String courseCode, Semester semester,
                            int academicYear, GradeLevel newGrade, GradeLevel previousGrade,
                            String gradedBy, LocalDateTime gradedDate, boolean isFinalGrade) {
        super(EVENT_TYPE, Priority.NORMAL, studentId, "Student", null);
        
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.academicYear = academicYear;
        this.newGrade = newGrade;
        this.previousGrade = previousGrade;
        this.newPercentage = 0.0;
        this.previousPercentage = 0.0;
        this.gradedBy = gradedBy;
        this.gradedDate = gradedDate;
        this.gradeComments = null;
        this.updateReason = null;
        this.isFinalGrade = isFinalGrade;
        this.assignmentName = null;
        this.departmentCode = null;
        this.instructorId = null;
        this.instructorName = null;
        this.courseCredits = 0;
        this.newGpaPoints = newGrade != null ? newGrade.getGpaPoints() : null;
        this.previousGpaPoints = previousGrade != null ? previousGrade.getGpaPoints() : null;
        this.affectsGpa = newGrade != null && newGrade.affectsGpa();
        this.gradeType = isFinalGrade ? "FINAL" : "ASSIGNMENT";
        this.approvedBy = null;
        
        // Add basic metadata
        addGradeMetadata();
    }
    
    /**
     * Creates a new GradeUpdatedEvent with comprehensive grade details
     */
    public GradeUpdatedEvent(String studentId, String studentName, String courseId,
                            String courseName, String courseCode, Semester semester,
                            int academicYear, GradeLevel newGrade, GradeLevel previousGrade,
                            double newPercentage, double previousPercentage, String gradedBy,
                            LocalDateTime gradedDate, String gradeComments, String updateReason,
                            boolean isFinalGrade, String assignmentName, String departmentCode,
                            String instructorId, String instructorName, int courseCredits,
                            String gradeType, String approvedBy) {
        super(EVENT_TYPE, determineGradePriority(newGrade, previousGrade, isFinalGrade), 
              studentId, "Student", null);
        
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.academicYear = academicYear;
        this.newGrade = newGrade;
        this.previousGrade = previousGrade;
        this.newPercentage = newPercentage;
        this.previousPercentage = previousPercentage;
        this.gradedBy = gradedBy;
        this.gradedDate = gradedDate;
        this.gradeComments = gradeComments;
        this.updateReason = updateReason;
        this.isFinalGrade = isFinalGrade;
        this.assignmentName = assignmentName;
        this.departmentCode = departmentCode;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.courseCredits = courseCredits;
        this.newGpaPoints = newGrade != null ? newGrade.getGpaPoints() : null;
        this.previousGpaPoints = previousGrade != null ? previousGrade.getGpaPoints() : null;
        this.affectsGpa = newGrade != null && newGrade.affectsGpa();
        this.gradeType = gradeType != null ? gradeType : (isFinalGrade ? "FINAL" : "ASSIGNMENT");
        this.approvedBy = approvedBy;
        
        // Add comprehensive metadata
        addGradeMetadata();
        addCourseMetadata();
        addGpaImpactMetadata();
    }
    
    /**
     * Copy constructor for event reconstruction
     */
    protected GradeUpdatedEvent(String eventId, LocalDateTime timestamp, String sourceSystem,
                               String correlationId, int version, Priority priority,
                               Long aggregateVersion, Map<String, Object> metadata,
                               String studentId, String studentName, String courseId,
                               String courseName, String courseCode, Semester semester,
                               int academicYear, GradeLevel newGrade, GradeLevel previousGrade,
                               double newPercentage, double previousPercentage, String gradedBy,
                               LocalDateTime gradedDate, String gradeComments, String updateReason,
                               boolean isFinalGrade, String assignmentName, String departmentCode,
                               String instructorId, String instructorName, int courseCredits,
                               String gradeType, String approvedBy) {
        super(eventId, EVENT_TYPE, timestamp, sourceSystem, correlationId, version,
              priority, studentId, "Student", aggregateVersion, metadata);
        
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.academicYear = academicYear;
        this.newGrade = newGrade;
        this.previousGrade = previousGrade;
        this.newPercentage = newPercentage;
        this.previousPercentage = previousPercentage;
        this.gradedBy = gradedBy;
        this.gradedDate = gradedDate;
        this.gradeComments = gradeComments;
        this.updateReason = updateReason;
        this.isFinalGrade = isFinalGrade;
        this.assignmentName = assignmentName;
        this.departmentCode = departmentCode;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.courseCredits = courseCredits;
        this.newGpaPoints = newGrade != null ? newGrade.getGpaPoints() : null;
        this.previousGpaPoints = previousGrade != null ? previousGrade.getGpaPoints() : null;
        this.affectsGpa = newGrade != null && newGrade.affectsGpa();
        this.gradeType = gradeType;
        this.approvedBy = approvedBy;
    }
    
    // ==================== GETTERS ====================
    
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getCourseCode() { return courseCode; }
    public Semester getSemester() { return semester; }
    public int getAcademicYear() { return academicYear; }
    public GradeLevel getNewGrade() { return newGrade; }
    public GradeLevel getPreviousGrade() { return previousGrade; }
    public double getNewPercentage() { return newPercentage; }
    public double getPreviousPercentage() { return previousPercentage; }
    public String getGradedBy() { return gradedBy; }
    public LocalDateTime getGradedDate() { return gradedDate; }
    public String getGradeComments() { return gradeComments; }
    public String getUpdateReason() { return updateReason; }
    public boolean isFinalGrade() { return isFinalGrade; }
    public String getAssignmentName() { return assignmentName; }
    public String getDepartmentCode() { return departmentCode; }
    public String getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public int getCourseCredits() { return courseCredits; }
    public Double getNewGpaPoints() { return newGpaPoints; }
    public Double getPreviousGpaPoints() { return previousGpaPoints; }
    public boolean affectsGpa() { return affectsGpa; }
    public String getGradeType() { return gradeType; }
    public String getApprovedBy() { return approvedBy; }
    
    // ==================== EVENT IMPLEMENTATION ====================
    
    @Override
    public Category getCategory() {
        return Category.DOMAIN;
    }
    
    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        
        // Student information
        Map<String, Object> student = new HashMap<>();
        student.put("id", studentId);
        student.put("name", studentName);
        payload.put("student", student);
        
        // Course information
        Map<String, Object> course = new HashMap<>();
        course.put("id", courseId);
        course.put("name", courseName);
        course.put("code", courseCode);
        course.put("credits", courseCredits);
        course.put("departmentCode", departmentCode);
        payload.put("course", course);
        
        // Instructor information
        if (instructorId != null) {
            Map<String, Object> instructor = new HashMap<>();
            instructor.put("id", instructorId);
            instructor.put("name", instructorName);
            payload.put("instructor", instructor);
        }
        
        // Grade information
        Map<String, Object> grade = new HashMap<>();
        if (newGrade != null) {
            Map<String, Object> newGradeInfo = new HashMap<>();
            newGradeInfo.put("level", newGrade.toString());
            newGradeInfo.put("letterGrade", newGrade.getLetterGrade());
            newGradeInfo.put("gpaPoints", newGrade.getGpaPoints());
            newGradeInfo.put("percentage", newPercentage);
            newGradeInfo.put("passing", newGrade.isPassingGrade());
            grade.put("new", newGradeInfo);
        }
        
        if (previousGrade != null) {
            Map<String, Object> previousGradeInfo = new HashMap<>();
            previousGradeInfo.put("level", previousGrade.toString());
            previousGradeInfo.put("letterGrade", previousGrade.getLetterGrade());
            previousGradeInfo.put("gpaPoints", previousGrade.getGpaPoints());
            previousGradeInfo.put("percentage", previousPercentage);
            previousGradeInfo.put("passing", previousGrade.isPassingGrade());
            grade.put("previous", previousGradeInfo);
        }
        
        grade.put("gradedBy", gradedBy);
        grade.put("gradedDate", gradedDate.toString());
        grade.put("isFinalGrade", isFinalGrade);
        grade.put("gradeType", gradeType);
        grade.put("affectsGpa", affectsGpa);
        
        if (gradeComments != null) {
            grade.put("comments", gradeComments);
        }
        if (updateReason != null) {
            grade.put("updateReason", updateReason);
        }
        if (assignmentName != null) {
            grade.put("assignmentName", assignmentName);
        }
        if (approvedBy != null) {
            grade.put("approvedBy", approvedBy);
        }
        
        payload.put("grade", grade);
        
        // Academic context
        Map<String, Object> academic = new HashMap<>();
        academic.put("semester", semester.toString());
        academic.put("academicYear", academicYear);
        payload.put("academic", academic);
        
        // GPA impact
        if (affectsGpa && newGpaPoints != null && previousGpaPoints != null) {
            Map<String, Object> gpaImpact = new HashMap<>();
            gpaImpact.put("pointsChange", newGpaPoints - previousGpaPoints);
            gpaImpact.put("creditHours", courseCredits);
            gpaImpact.put("weightedChange", (newGpaPoints - previousGpaPoints) * courseCredits);
            payload.put("gpaImpact", gpaImpact);
        }
        
        return payload;
    }
    
    @Override
    public boolean isValid() {
        return studentId != null && !studentId.trim().isEmpty() &&
               courseId != null && !courseId.trim().isEmpty() &&
               newGrade != null &&
               gradedBy != null && !gradedBy.trim().isEmpty() &&
               gradedDate != null &&
               semester != null &&
               academicYear > 0;
    }
    
    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        
        if (isFinalGrade) {
            description.append("Final grade updated for student ").append(studentName)
                      .append(" (").append(studentId).append(") in course ")
                      .append(courseCode).append(" - ").append(courseName);
        } else {
            description.append("Grade updated for student ").append(studentName)
                      .append(" (").append(studentId).append(")");
            
            if (assignmentName != null) {
                description.append(" on assignment '").append(assignmentName).append("'");
            }
            
            description.append(" in course ").append(courseCode).append(" - ").append(courseName);
        }
        
        if (previousGrade != null && newGrade != null) {
            description.append(": ").append(previousGrade.getLetterGrade())
                      .append(" → ").append(newGrade.getLetterGrade());
        } else if (newGrade != null) {
            description.append(": ").append(newGrade.getLetterGrade());
        }
        
        description.append(" for ").append(semester.getFullName()).append(" ").append(academicYear);
        
        if (updateReason != null) {
            description.append(" (Reason: ").append(updateReason).append(")");
        }
        
        return description.toString();
    }
    
    @Override
    protected Event createCopy(String eventId, String eventType, LocalDateTime timestamp,
                              String sourceSystem, String correlationId, int version,
                              Priority priority, String aggregateId, String aggregateType,
                              Long aggregateVersion, Map<String, Object> metadata) {
        return new GradeUpdatedEvent(eventId, timestamp, sourceSystem, correlationId,
                                    version, priority, aggregateVersion, metadata,
                                    studentId, studentName, courseId, courseName, courseCode,
                                    semester, academicYear, newGrade, previousGrade,
                                    newPercentage, previousPercentage, gradedBy, gradedDate,
                                    gradeComments, updateReason, isFinalGrade, assignmentName,
                                    departmentCode, instructorId, instructorName, courseCredits,
                                    gradeType, approvedBy);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if this was a grade improvement
     */
    public boolean isImprovement() {
        if (previousGrade == null || newGrade == null) return false;
        if (previousGrade.getGpaPoints() == null || newGrade.getGpaPoints() == null) return false;
        return newGrade.getGpaPoints() > previousGrade.getGpaPoints();
    }
    
    /**
     * Checks if this was a grade decline
     */
    public boolean isDecline() {
        if (previousGrade == null || newGrade == null) return false;
        if (previousGrade.getGpaPoints() == null || newGrade.getGpaPoints() == null) return false;
        return newGrade.getGpaPoints() < previousGrade.getGpaPoints();
    }
    
    /**
     * Gets the GPA points change
     */
    public Double getGpaPointsChange() {
        if (newGpaPoints == null || previousGpaPoints == null) return null;
        return newGpaPoints - previousGpaPoints;
    }
    
    /**
     * Gets the percentage change
     */
    public double getPercentageChange() {
        return newPercentage - previousPercentage;
    }
    
    /**
     * Checks if the grade changed from failing to passing
     */
    public boolean isFailingToPassingChange() {
        return previousGrade != null && newGrade != null &&
               previousGrade.isFailingGrade() && newGrade.isPassingGrade();
    }
    
    /**
     * Checks if the grade changed from passing to failing
     */
    public boolean isPassingToFailingChange() {
        return previousGrade != null && newGrade != null &&
               previousGrade.isPassingGrade() && newGrade.isFailingGrade();
    }
    
    /**
     * Checks if this is a significant grade change (more than 1 letter grade)
     */
    public boolean isSignificantChange() {
        Double change = getGpaPointsChange();
        return change != null && Math.abs(change) >= 1.0;
    }
    
    /**
     * Gets the weighted GPA impact (points change × credit hours)
     */
    public Double getWeightedGpaImpact() {
        Double pointsChange = getGpaPointsChange();
        return pointsChange != null ? pointsChange * courseCredits : null;
    }
    
    /**
     * Gets the semester display string
     */
    public String getSemesterDisplay() {
        return semester.getFullName() + " " + academicYear;
    }
    
    /**
     * Gets the course display string
     */
    public String getCourseDisplay() {
        return courseCode + " - " + courseName;
    }
    
    /**
     * Gets the grade change display string
     */
    public String getGradeChangeDisplay() {
        if (previousGrade != null && newGrade != null) {
            return previousGrade.getLetterGrade() + " → " + newGrade.getLetterGrade();
        } else if (newGrade != null) {
            return "New: " + newGrade.getLetterGrade();
        }
        return "Grade Updated";
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Determines event priority based on grade change
     */
    private static Priority determineGradePriority(GradeLevel newGrade, GradeLevel previousGrade, 
                                                  boolean isFinalGrade) {
        if (isFinalGrade) {
            return Priority.HIGH;
        }
        
        if (previousGrade != null && newGrade != null) {
            // Failing to passing or vice versa is high priority
            if ((previousGrade.isFailingGrade() && newGrade.isPassingGrade()) ||
                (previousGrade.isPassingGrade() && newGrade.isFailingGrade())) {
                return Priority.HIGH;
            }
            
            // Significant grade changes are medium priority
            if (previousGrade.getGpaPoints() != null && newGrade.getGpaPoints() != null) {
                double change = Math.abs(newGrade.getGpaPoints() - previousGrade.getGpaPoints());
                if (change >= 1.0) {
                    return Priority.MEDIUM;
                }
            }
        }
        
        return Priority.NORMAL;
    }
    
    /**
     * Adds grade-related metadata
     */
    private void addGradeMetadata() {
        addMetadata("grade.studentId", studentId);
        addMetadata("grade.courseId", courseId);
        addMetadata("grade.semester", semester.toString());
        addMetadata("grade.academicYear", academicYear);
        addMetadata("grade.isFinalGrade", isFinalGrade);
        addMetadata("grade.gradeType", gradeType);
        addMetadata("grade.gradedBy", gradedBy);
        addMetadata("grade.affectsGpa", affectsGpa);
        
        if (newGrade != null) {
            addMetadata("grade.new.level", newGrade.toString());
            addMetadata("grade.new.letterGrade", newGrade.getLetterGrade());
            addMetadata("grade.new.gpaPoints", newGrade.getGpaPoints());
            addMetadata("grade.new.passing", newGrade.isPassingGrade());
        }
        
        if (previousGrade != null) {
            addMetadata("grade.previous.level", previousGrade.toString());
            addMetadata("grade.previous.letterGrade", previousGrade.getLetterGrade());
            addMetadata("grade.previous.gpaPoints", previousGrade.getGpaPoints());
            addMetadata("grade.previous.passing", previousGrade.isPassingGrade());
            
            addMetadata("grade.isImprovement", isImprovement());
            addMetadata("grade.isDecline", isDecline());
            addMetadata("grade.isSignificantChange", isSignificantChange());
            addMetadata("grade.isFailingToPassingChange", isFailingToPassingChange());
            addMetadata("grade.isPassingToFailingChange", isPassingToFailingChange());
        }
        
        if (assignmentName != null) {
            addMetadata("grade.assignmentName", assignmentName);
        }
        
        if (updateReason != null) {
            addMetadata("grade.updateReason", updateReason);
        }
        
        if (approvedBy != null) {
            addMetadata("grade.approvedBy", approvedBy);
        }
    }
    
    /**
     * Adds course-related metadata
     */
    private void addCourseMetadata() {
        addMetadata("course.code", courseCode);
        addMetadata("course.name", courseName);
        addMetadata("course.credits", courseCredits);
        
        if (departmentCode != null) {
            addMetadata("course.department", departmentCode);
        }
        
        if (instructorId != null) {
            addMetadata("course.instructorId", instructorId);
            addMetadata("course.instructorName", instructorName);
        }
    }
    
    /**
     * Adds GPA impact metadata
     */
    private void addGpaImpactMetadata() {
        if (affectsGpa) {
            Double pointsChange = getGpaPointsChange();
            if (pointsChange != null) {
                addMetadata("gpaImpact.pointsChange", pointsChange);
                addMetadata("gpaImpact.creditHours", courseCredits);
                addMetadata("gpaImpact.weightedChange", pointsChange * courseCredits);
            }
        }
    }
    
    // ==================== BUILDER ====================
    
    /**
     * Builder for creating GradeUpdatedEvent
     */
    public static class Builder extends Event.Builder<GradeUpdatedEvent, Builder> {
        private String studentId;
        private String studentName;
        private String courseId;
        private String courseName;
        private String courseCode;
        private Semester semester;
        private int academicYear;
        private GradeLevel newGrade;
        private GradeLevel previousGrade;
        private double newPercentage = 0.0;
        private double previousPercentage = 0.0;
        private String gradedBy;
        private LocalDateTime gradedDate;
        private String gradeComments;
        private String updateReason;
        private boolean isFinalGrade = false;
        private String assignmentName;
        private String departmentCode;
        private String instructorId;
        private String instructorName;
        private int courseCredits = 0;
        private String gradeType;
        private String approvedBy;
        
        public Builder student(String studentId, String studentName) {
            this.studentId = studentId;
            this.studentName = studentName;
            return this;
        }
        
        public Builder course(String courseId, String courseName, String courseCode) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.courseCode = courseCode;
            return this;
        }
        
        public Builder semester(Semester semester, int academicYear) {
            this.semester = semester;
            this.academicYear = academicYear;
            return this;
        }
        
        public Builder grades(GradeLevel newGrade, GradeLevel previousGrade) {
            this.newGrade = newGrade;
            this.previousGrade = previousGrade;
            return this;
        }
        
        public Builder percentages(double newPercentage, double previousPercentage) {
            this.newPercentage = newPercentage;
            this.previousPercentage = previousPercentage;
            return this;
        }
        
        public Builder gradingInfo(String gradedBy, LocalDateTime gradedDate) {
            this.gradedBy = gradedBy;
            this.gradedDate = gradedDate;
            return this;
        }
        
        public Builder comments(String gradeComments) {
            this.gradeComments = gradeComments;
            return this;
        }
        
        public Builder updateReason(String updateReason) {
            this.updateReason = updateReason;
            return this;
        }
        
        public Builder finalGrade(boolean isFinalGrade) {
            this.isFinalGrade = isFinalGrade;
            return this;
        }
        
        public Builder assignment(String assignmentName) {
            this.assignmentName = assignmentName;
            return this;
        }
        
        public Builder courseDetails(String departmentCode, int credits) {
            this.departmentCode = departmentCode;
            this.courseCredits = credits;
            return this;
        }
        
        public Builder instructor(String instructorId, String instructorName) {
            this.instructorId = instructorId;
            this.instructorName = instructorName;
            return this;
        }
        
        public Builder gradeType(String gradeType) {
            this.gradeType = gradeType;
            return this;
        }
        
        public Builder approvedBy(String approvedBy) {
            this.approvedBy = approvedBy;
            return this;
        }
        
        @Override
        public GradeUpdatedEvent build() {
            GradeUpdatedEvent event = new GradeUpdatedEvent(
                studentId, studentName, courseId, courseName, courseCode,
                semester, academicYear, newGrade, previousGrade,
                newPercentage, previousPercentage, gradedBy, gradedDate,
                gradeComments, updateReason, isFinalGrade, assignmentName,
                departmentCode, instructorId, instructorName, courseCredits,
                gradeType, approvedBy
            );
            
            // Apply builder properties
            event.addMetadata(metadata);
            
            return event;
        }
    }
    
    /**
     * Creates a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // ==================== EQUALITY AND HASH CODE ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        
        GradeUpdatedEvent that = (GradeUpdatedEvent) obj;
        return Objects.equals(studentId, that.studentId) &&
               Objects.equals(courseId, that.courseId) &&
               Objects.equals(semester, that.semester) &&
               academicYear == that.academicYear &&
               Objects.equals(newGrade, that.newGrade) &&
               Objects.equals(gradedDate, that.gradedDate) &&
               Objects.equals(assignmentName, that.assignmentName) &&
               isFinalGrade == that.isFinalGrade;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), studentId, courseId, semester, 
                           academicYear, newGrade, gradedDate, assignmentName, isFinalGrade);
    }
}