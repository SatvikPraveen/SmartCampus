// File location: src/main/java/events/StudentEnrolledEvent.java
package events;

import enums.Semester;
import enums.EnrollmentStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event fired when a student enrolls in a course
 * Contains all relevant enrollment information for event processing
 */
public class StudentEnrolledEvent extends Event {
    
    public static final String EVENT_TYPE = "StudentEnrolled";
    
    // Event payload
    private final String studentId;
    private final String studentName;
    private final String courseId;
    private final String courseName;
    private final String courseCode;
    private final Semester semester;
    private final int academicYear;
    private final EnrollmentStatus enrollmentStatus;
    private final LocalDateTime enrollmentDate;
    private final String enrolledBy;
    private final int courseCredits;
    private final String departmentCode;
    private final String instructorId;
    private final String instructorName;
    private final int currentEnrollmentCount;
    private final int maxEnrollmentCapacity;
    private final boolean isWaitlisted;
    private final String enrollmentMethod;
    private final String previousStatus;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new StudentEnrolledEvent with basic enrollment information
     */
    public StudentEnrolledEvent(String studentId, String studentName, String courseId, 
                               String courseName, String courseCode, Semester semester, 
                               int academicYear, EnrollmentStatus enrollmentStatus,
                               LocalDateTime enrollmentDate, String enrolledBy) {
        super(EVENT_TYPE, Priority.NORMAL, studentId, "Student", null);
        
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.academicYear = academicYear;
        this.enrollmentStatus = enrollmentStatus;
        this.enrollmentDate = enrollmentDate;
        this.enrolledBy = enrolledBy;
        this.courseCredits = 0;
        this.departmentCode = null;
        this.instructorId = null;
        this.instructorName = null;
        this.currentEnrollmentCount = 0;
        this.maxEnrollmentCapacity = 0;
        this.isWaitlisted = false;
        this.enrollmentMethod = "MANUAL";
        this.previousStatus = null;
        
        // Add basic metadata
        addEnrollmentMetadata();
    }
    
    /**
     * Creates a new StudentEnrolledEvent with comprehensive enrollment details
     */
    public StudentEnrolledEvent(String studentId, String studentName, String courseId, 
                               String courseName, String courseCode, Semester semester, 
                               int academicYear, EnrollmentStatus enrollmentStatus,
                               LocalDateTime enrollmentDate, String enrolledBy,
                               int courseCredits, String departmentCode, String instructorId,
                               String instructorName, int currentEnrollmentCount,
                               int maxEnrollmentCapacity, boolean isWaitlisted,
                               String enrollmentMethod, String previousStatus) {
        super(EVENT_TYPE, Priority.NORMAL, studentId, "Student", null);
        
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.academicYear = academicYear;
        this.enrollmentStatus = enrollmentStatus;
        this.enrollmentDate = enrollmentDate;
        this.enrolledBy = enrolledBy;
        this.courseCredits = courseCredits;
        this.departmentCode = departmentCode;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.currentEnrollmentCount = currentEnrollmentCount;
        this.maxEnrollmentCapacity = maxEnrollmentCapacity;
        this.isWaitlisted = isWaitlisted;
        this.enrollmentMethod = enrollmentMethod != null ? enrollmentMethod : "MANUAL";
        this.previousStatus = previousStatus;
        
        // Add comprehensive metadata
        addEnrollmentMetadata();
        addCourseMetadata();
        addCapacityMetadata();
    }
    
    /**
     * Copy constructor for event reconstruction
     */
    protected StudentEnrolledEvent(String eventId, LocalDateTime timestamp, String sourceSystem,
                                  String correlationId, int version, Priority priority,
                                  Long aggregateVersion, Map<String, Object> metadata,
                                  String studentId, String studentName, String courseId,
                                  String courseName, String courseCode, Semester semester,
                                  int academicYear, EnrollmentStatus enrollmentStatus,
                                  LocalDateTime enrollmentDate, String enrolledBy,
                                  int courseCredits, String departmentCode, String instructorId,
                                  String instructorName, int currentEnrollmentCount,
                                  int maxEnrollmentCapacity, boolean isWaitlisted,
                                  String enrollmentMethod, String previousStatus) {
        super(eventId, EVENT_TYPE, timestamp, sourceSystem, correlationId, version,
              priority, studentId, "Student", aggregateVersion, metadata);
        
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.semester = semester;
        this.academicYear = academicYear;
        this.enrollmentStatus = enrollmentStatus;
        this.enrollmentDate = enrollmentDate;
        this.enrolledBy = enrolledBy;
        this.courseCredits = courseCredits;
        this.departmentCode = departmentCode;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.currentEnrollmentCount = currentEnrollmentCount;
        this.maxEnrollmentCapacity = maxEnrollmentCapacity;
        this.isWaitlisted = isWaitlisted;
        this.enrollmentMethod = enrollmentMethod;
        this.previousStatus = previousStatus;
    }
    
    // ==================== GETTERS ====================
    
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getCourseCode() { return courseCode; }
    public Semester getSemester() { return semester; }
    public int getAcademicYear() { return academicYear; }
    public EnrollmentStatus getEnrollmentStatus() { return enrollmentStatus; }
    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }
    public String getEnrolledBy() { return enrolledBy; }
    public int getCourseCredits() { return courseCredits; }
    public String getDepartmentCode() { return departmentCode; }
    public String getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public int getCurrentEnrollmentCount() { return currentEnrollmentCount; }
    public int getMaxEnrollmentCapacity() { return maxEnrollmentCapacity; }
    public boolean isWaitlisted() { return isWaitlisted; }
    public String getEnrollmentMethod() { return enrollmentMethod; }
    public String getPreviousStatus() { return previousStatus; }
    
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
        
        // Enrollment information
        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("status", enrollmentStatus.toString());
        enrollment.put("date", enrollmentDate.toString());
        enrollment.put("enrolledBy", enrolledBy);
        enrollment.put("method", enrollmentMethod);
        enrollment.put("isWaitlisted", isWaitlisted);
        if (previousStatus != null) {
            enrollment.put("previousStatus", previousStatus);
        }
        payload.put("enrollment", enrollment);
        
        // Academic context
        Map<String, Object> academic = new HashMap<>();
        academic.put("semester", semester.toString());
        academic.put("academicYear", academicYear);
        payload.put("academic", academic);
        
        // Capacity information
        Map<String, Object> capacity = new HashMap<>();
        capacity.put("current", currentEnrollmentCount);
        capacity.put("maximum", maxEnrollmentCapacity);
        capacity.put("available", Math.max(0, maxEnrollmentCapacity - currentEnrollmentCount));
        capacity.put("utilizationPercentage", maxEnrollmentCapacity > 0 ? 
            (double) currentEnrollmentCount / maxEnrollmentCapacity * 100 : 0);
        payload.put("capacity", capacity);
        
        return payload;
    }
    
    @Override
    public boolean isValid() {
        return studentId != null && !studentId.trim().isEmpty() &&
               courseId != null && !courseId.trim().isEmpty() &&
               semester != null &&
               academicYear > 0 &&
               enrollmentStatus != null &&
               enrollmentDate != null &&
               enrolledBy != null && !enrolledBy.trim().isEmpty();
    }
    
    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        
        if (isWaitlisted) {
            description.append("Student ").append(studentName).append(" (").append(studentId)
                      .append(") was added to waitlist for course ").append(courseCode)
                      .append(" - ").append(courseName);
        } else {
            description.append("Student ").append(studentName).append(" (").append(studentId)
                      .append(") enrolled in course ").append(courseCode)
                      .append(" - ").append(courseName);
        }
        
        description.append(" for ").append(semester.getFullName()).append(" ").append(academicYear);
        
        if (previousStatus != null) {
            description.append(" (previous status: ").append(previousStatus).append(")");
        }
        
        return description.toString();
    }
    
    @Override
    protected Event createCopy(String eventId, String eventType, LocalDateTime timestamp,
                              String sourceSystem, String correlationId, int version,
                              Priority priority, String aggregateId, String aggregateType,
                              Long aggregateVersion, Map<String, Object> metadata) {
        return new StudentEnrolledEvent(eventId, timestamp, sourceSystem, correlationId,
                                       version, priority, aggregateVersion, metadata,
                                       studentId, studentName, courseId, courseName, courseCode,
                                       semester, academicYear, enrollmentStatus, enrollmentDate,
                                       enrolledBy, courseCredits, departmentCode, instructorId,
                                       instructorName, currentEnrollmentCount, maxEnrollmentCapacity,
                                       isWaitlisted, enrollmentMethod, previousStatus);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if the course is at capacity
     */
    public boolean isCourseAtCapacity() {
        return currentEnrollmentCount >= maxEnrollmentCapacity;
    }
    
    /**
     * Gets the number of available spots
     */
    public int getAvailableSpots() {
        return Math.max(0, maxEnrollmentCapacity - currentEnrollmentCount);
    }
    
    /**
     * Gets the enrollment utilization percentage
     */
    public double getUtilizationPercentage() {
        return maxEnrollmentCapacity > 0 ? 
            (double) currentEnrollmentCount / maxEnrollmentCapacity * 100 : 0;
    }
    
    /**
     * Checks if this was a status change enrollment (re-enrollment)
     */
    public boolean isStatusChange() {
        return previousStatus != null;
    }
    
    /**
     * Checks if this was an automated enrollment
     */
    public boolean isAutomatedEnrollment() {
        return "AUTOMATED".equals(enrollmentMethod) || 
               "BATCH".equals(enrollmentMethod) ||
               "SYSTEM".equals(enrollmentMethod);
    }
    
    /**
     * Checks if this enrollment affects course capacity
     */
    public boolean affectsCapacity() {
        return !isWaitlisted && enrollmentStatus == EnrollmentStatus.ENROLLED;
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
    
    // ==================== METADATA HELPERS ====================
    
    /**
     * Adds basic enrollment metadata
     */
    private void addEnrollmentMetadata() {
        addMetadata("enrollment.studentId", studentId);
        addMetadata("enrollment.courseId", courseId);
        addMetadata("enrollment.semester", semester.toString());
        addMetadata("enrollment.academicYear", academicYear);
        addMetadata("enrollment.status", enrollmentStatus.toString());
        addMetadata("enrollment.method", enrollmentMethod);
        addMetadata("enrollment.isWaitlisted", isWaitlisted);
        addMetadata("enrollment.enrolledBy", enrolledBy);
        
        if (previousStatus != null) {
            addMetadata("enrollment.previousStatus", previousStatus);
            addMetadata("enrollment.isStatusChange", true);
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
     * Adds capacity-related metadata
     */
    private void addCapacityMetadata() {
        addMetadata("capacity.current", currentEnrollmentCount);
        addMetadata("capacity.maximum", maxEnrollmentCapacity);
        addMetadata("capacity.available", getAvailableSpots());
        addMetadata("capacity.utilizationPercentage", getUtilizationPercentage());
        addMetadata("capacity.atCapacity", isCourseAtCapacity());
    }
    
    // ==================== BUILDER ====================
    
    /**
     * Builder for creating StudentEnrolledEvent
     */
    public static class Builder extends Event.Builder<StudentEnrolledEvent, Builder> {
        private String studentId;
        private String studentName;
        private String courseId;
        private String courseName;
        private String courseCode;
        private Semester semester;
        private int academicYear;
        private EnrollmentStatus enrollmentStatus;
        private LocalDateTime enrollmentDate;
        private String enrolledBy;
        private int courseCredits = 0;
        private String departmentCode;
        private String instructorId;
        private String instructorName;
        private int currentEnrollmentCount = 0;
        private int maxEnrollmentCapacity = 0;
        private boolean isWaitlisted = false;
        private String enrollmentMethod = "MANUAL";
        private String previousStatus;
        
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
        
        public Builder enrollment(EnrollmentStatus status, LocalDateTime date, String enrolledBy) {
            this.enrollmentStatus = status;
            this.enrollmentDate = date;
            this.enrolledBy = enrolledBy;
            return this;
        }
        
        public Builder courseDetails(int credits, String departmentCode) {
            this.courseCredits = credits;
            this.departmentCode = departmentCode;
            return this;
        }
        
        public Builder instructor(String instructorId, String instructorName) {
            this.instructorId = instructorId;
            this.instructorName = instructorName;
            return this;
        }
        
        public Builder capacity(int current, int maximum) {
            this.currentEnrollmentCount = current;
            this.maxEnrollmentCapacity = maximum;
            return this;
        }
        
        public Builder waitlisted(boolean isWaitlisted) {
            this.isWaitlisted = isWaitlisted;
            return this;
        }
        
        public Builder method(String enrollmentMethod) {
            this.enrollmentMethod = enrollmentMethod;
            return this;
        }
        
        public Builder previousStatus(String previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }
        
        @Override
        public StudentEnrolledEvent build() {
            StudentEnrolledEvent event = new StudentEnrolledEvent(
                studentId, studentName, courseId, courseName, courseCode,
                semester, academicYear, enrollmentStatus, enrollmentDate, enrolledBy,
                courseCredits, departmentCode, instructorId, instructorName,
                currentEnrollmentCount, maxEnrollmentCapacity, isWaitlisted,
                enrollmentMethod, previousStatus
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
        
        StudentEnrolledEvent that = (StudentEnrolledEvent) obj;
        return Objects.equals(studentId, that.studentId) &&
               Objects.equals(courseId, that.courseId) &&
               Objects.equals(semester, that.semester) &&
               academicYear == that.academicYear &&
               Objects.equals(enrollmentStatus, that.enrollmentStatus) &&
               Objects.equals(enrollmentDate, that.enrollmentDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), studentId, courseId, semester, 
                           academicYear, enrollmentStatus, enrollmentDate);
    }
}