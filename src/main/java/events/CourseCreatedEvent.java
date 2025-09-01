// File location: src/main/java/events/CourseCreatedEvent.java
package events;

import enums.Semester;
import enums.CourseStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Event fired when a new course is created in the system
 * Contains all relevant course information for event processing and notifications
 */
public class CourseCreatedEvent extends Event {
    
    public static final String EVENT_TYPE = "CourseCreated";
    
    // Event payload
    private final String courseId;
    private final String courseName;
    private final String courseCode;
    private final String courseNumber;
    private final String departmentCode;
    private final String departmentName;
    private final String description;
    private final int credits;
    private final Semester semester;
    private final int academicYear;
    private final CourseStatus status;
    private final String instructorId;
    private final String instructorName;
    private final int maxEnrollment;
    private final String location;
    private final String schedule;
    private final List<String> prerequisites;
    private final String createdBy;
    private final LocalDateTime createdDate;
    private final boolean isOnline;
    private final boolean isHybrid;
    private final String courseLevel;
    private final String syllabus;
    private final String gradeScale;
    private final boolean hasWaitlist;
    private final String approvedBy;
    private final LocalDateTime approvedDate;
    private final String catalogDescription;
    private final List<String> learningObjectives;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new CourseCreatedEvent with basic course information
     */
    public CourseCreatedEvent(String courseId, String courseName, String courseCode,
                             String courseNumber, String departmentCode, String departmentName,
                             String description, int credits, Semester semester, int academicYear,
                             CourseStatus status, String instructorId, String instructorName,
                             int maxEnrollment, String createdBy, LocalDateTime createdDate) {
        super(EVENT_TYPE, Priority.NORMAL, courseId, "Course", null);
        
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseNumber = courseNumber;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.description = description;
        this.credits = credits;
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.maxEnrollment = maxEnrollment;
        this.location = null;
        this.schedule = null;
        this.prerequisites = null;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.isOnline = false;
        this.isHybrid = false;
        this.courseLevel = determineCourseLevel(courseNumber);
        this.syllabus = null;
        this.gradeScale = null;
        this.hasWaitlist = false;
        this.approvedBy = null;
        this.approvedDate = null;
        this.catalogDescription = null;
        this.learningObjectives = null;
        
        // Add basic metadata
        addCourseMetadata();
    }
    
    /**
     * Creates a new CourseCreatedEvent with comprehensive course details
     */
    public CourseCreatedEvent(String courseId, String courseName, String courseCode,
                             String courseNumber, String departmentCode, String departmentName,
                             String description, int credits, Semester semester, int academicYear,
                             CourseStatus status, String instructorId, String instructorName,
                             int maxEnrollment, String location, String schedule,
                             List<String> prerequisites, String createdBy, LocalDateTime createdDate,
                             boolean isOnline, boolean isHybrid, String syllabus,
                             String gradeScale, boolean hasWaitlist, String approvedBy,
                             LocalDateTime approvedDate, String catalogDescription,
                             List<String> learningObjectives) {
        super(EVENT_TYPE, Priority.NORMAL, courseId, "Course", null);
        
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseNumber = courseNumber;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.description = description;
        this.credits = credits;
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.maxEnrollment = maxEnrollment;
        this.location = location;
        this.schedule = schedule;
        this.prerequisites = prerequisites;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.isOnline = isOnline;
        this.isHybrid = isHybrid;
        this.courseLevel = determineCourseLevel(courseNumber);
        this.syllabus = syllabus;
        this.gradeScale = gradeScale;
        this.hasWaitlist = hasWaitlist;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
        this.catalogDescription = catalogDescription;
        this.learningObjectives = learningObjectives;
        
        // Add comprehensive metadata
        addCourseMetadata();
        addInstructorMetadata();
        addScheduleMetadata();
        addAcademicMetadata();
    }
    
    /**
     * Copy constructor for event reconstruction
     */
    protected CourseCreatedEvent(String eventId, LocalDateTime timestamp, String sourceSystem,
                                String correlationId, int version, Priority priority,
                                Long aggregateVersion, Map<String, Object> metadata,
                                String courseId, String courseName, String courseCode,
                                String courseNumber, String departmentCode, String departmentName,
                                String description, int credits, Semester semester, int academicYear,
                                CourseStatus status, String instructorId, String instructorName,
                                int maxEnrollment, String location, String schedule,
                                List<String> prerequisites, String createdBy, LocalDateTime createdDate,
                                boolean isOnline, boolean isHybrid, String courseLevel,
                                String syllabus, String gradeScale, boolean hasWaitlist,
                                String approvedBy, LocalDateTime approvedDate,
                                String catalogDescription, List<String> learningObjectives) {
        super(eventId, EVENT_TYPE, timestamp, sourceSystem, correlationId, version,
              priority, courseId, "Course", aggregateVersion, metadata);
        
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.courseNumber = courseNumber;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.description = description;
        this.credits = credits;
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.maxEnrollment = maxEnrollment;
        this.location = location;
        this.schedule = schedule;
        this.prerequisites = prerequisites;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.isOnline = isOnline;
        this.isHybrid = isHybrid;
        this.courseLevel = courseLevel;
        this.syllabus = syllabus;
        this.gradeScale = gradeScale;
        this.hasWaitlist = hasWaitlist;
        this.approvedBy = approvedBy;
        this.approvedDate = approvedDate;
        this.catalogDescription = catalogDescription;
        this.learningObjectives = learningObjectives;
    }
    
    // ==================== GETTERS ====================
    
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getCourseCode() { return courseCode; }
    public String getCourseNumber() { return courseNumber; }
    public String getDepartmentCode() { return departmentCode; }
    public String getDepartmentName() { return departmentName; }
    public String getDescription() { return description; }
    public int getCredits() { return credits; }
    public Semester getSemester() { return semester; }
    public int getAcademicYear() { return academicYear; }
    public CourseStatus getStatus() { return status; }
    public String getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public int getMaxEnrollment() { return maxEnrollment; }
    public String getLocation() { return location; }
    public String getSchedule() { return schedule; }
    public List<String> getPrerequisites() { return prerequisites; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isOnline() { return isOnline; }
    public boolean isHybrid() { return isHybrid; }
    public String getCourseLevel() { return courseLevel; }
    public String getSyllabus() { return syllabus; }
    public String getGradeScale() { return gradeScale; }
    public boolean hasWaitlist() { return hasWaitlist; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedDate() { return approvedDate; }
    public String getCatalogDescription() { return catalogDescription; }
    public List<String> getLearningObjectives() { return learningObjectives; }
    
    // ==================== EVENT IMPLEMENTATION ====================
    
    @Override
    public Category getCategory() {
        return Category.DOMAIN;
    }
    
    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        
        // Basic course information
        Map<String, Object> course = new HashMap<>();
        course.put("id", courseId);
        course.put("name", courseName);
        course.put("code", courseCode);
        course.put("number", courseNumber);
        course.put("description", description);
        course.put("credits", credits);
        course.put("status", status.toString());
        course.put("maxEnrollment", maxEnrollment);
        course.put("courseLevel", courseLevel);
        course.put("isOnline", isOnline);
        course.put("isHybrid", isHybrid);
        course.put("hasWaitlist", hasWaitlist);
        
        if (location != null) course.put("location", location);
        if (schedule != null) course.put("schedule", schedule);
        if (syllabus != null) course.put("syllabus", syllabus);
        if (gradeScale != null) course.put("gradeScale", gradeScale);
        if (catalogDescription != null) course.put("catalogDescription", catalogDescription);
        if (prerequisites != null && !prerequisites.isEmpty()) {
            course.put("prerequisites", prerequisites);
        }
        if (learningObjectives != null && !learningObjectives.isEmpty()) {
            course.put("learningObjectives", learningObjectives);
        }
        
        payload.put("course", course);
        
        // Department information
        Map<String, Object> department = new HashMap<>();
        department.put("code", departmentCode);
        department.put("name", departmentName);
        payload.put("department", department);
        
        // Instructor information
        if (instructorId != null) {
            Map<String, Object> instructor = new HashMap<>();
            instructor.put("id", instructorId);
            instructor.put("name", instructorName);
            payload.put("instructor", instructor);
        }
        
        // Academic context
        Map<String, Object> academic = new HashMap<>();
        academic.put("semester", semester.toString());
        academic.put("academicYear", academicYear);
        payload.put("academic", academic);
        
        // Creation information
        Map<String, Object> creation = new HashMap<>();
        creation.put("createdBy", createdBy);
        creation.put("createdDate", createdDate.toString());
        if (approvedBy != null) {
            creation.put("approvedBy", approvedBy);
            creation.put("approvedDate", approvedDate.toString());
        }
        payload.put("creation", creation);
        
        // Delivery mode
        String deliveryMode;
        if (isOnline) {
            deliveryMode = "ONLINE";
        } else if (isHybrid) {
            deliveryMode = "HYBRID";
        } else {
            deliveryMode = "IN_PERSON";
        }
        course.put("deliveryMode", deliveryMode);
        
        return payload;
    }
    
    @Override
    public boolean isValid() {
        return courseId != null && !courseId.trim().isEmpty() &&
               courseName != null && !courseName.trim().isEmpty() &&
               courseCode != null && !courseCode.trim().isEmpty() &&
               courseNumber != null && !courseNumber.trim().isEmpty() &&
               departmentCode != null && !departmentCode.trim().isEmpty() &&
               credits > 0 &&
               semester != null &&
               academicYear > 0 &&
               status != null &&
               maxEnrollment > 0 &&
               createdBy != null && !createdBy.trim().isEmpty() &&
               createdDate != null;
    }
    
    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        
        description.append("New course created: ").append(courseCode)
                   .append(" - ").append(courseName)
                   .append(" (").append(credits).append(" credits)");
        
        if (instructorName != null) {
            description.append(" taught by ").append(instructorName);
        }
        
        description.append(" in ").append(departmentName)
                   .append(" for ").append(semester.getFullName())
                   .append(" ").append(academicYear);
        
        String deliveryMode = getDeliveryModeDisplay();
        if (!deliveryMode.equals("In-Person")) {
            description.append(" (").append(deliveryMode).append(")");
        }
        
        if (status != CourseStatus.ACTIVE) {
            description.append(" - Status: ").append(status.getDisplayName());
        }
        
        return description.toString();
    }
    
    @Override
    protected Event createCopy(String eventId, String eventType, LocalDateTime timestamp,
                              String sourceSystem, String correlationId, int version,
                              Priority priority, String aggregateId, String aggregateType,
                              Long aggregateVersion, Map<String, Object> metadata) {
        return new CourseCreatedEvent(eventId, timestamp, sourceSystem, correlationId,
                                     version, priority, aggregateVersion, metadata,
                                     courseId, courseName, courseCode, courseNumber,
                                     departmentCode, departmentName, description, credits,
                                     semester, academicYear, status, instructorId, instructorName,
                                     maxEnrollment, location, schedule, prerequisites,
                                     createdBy, createdDate, isOnline, isHybrid, courseLevel,
                                     syllabus, gradeScale, hasWaitlist, approvedBy, approvedDate,
                                     catalogDescription, learningObjectives);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Gets the delivery mode display string
     */
    public String getDeliveryModeDisplay() {
        if (isOnline) return "Online";
        if (isHybrid) return "Hybrid";
        return "In-Person";
    }
    
    /**
     * Checks if the course is undergraduate level
     */
    public boolean isUndergraduateLevel() {
        return "UNDERGRADUATE".equals(courseLevel);
    }
    
    /**
     * Checks if the course is graduate level
     */
    public boolean isGraduateLevel() {
        return "GRADUATE".equals(courseLevel);
    }
    
    /**
     * Checks if the course has prerequisites
     */
    public boolean hasPrerequisites() {
        return prerequisites != null && !prerequisites.isEmpty();
    }
    
    /**
     * Checks if the course has learning objectives
     */
    public boolean hasLearningObjectives() {
        return learningObjectives != null && !learningObjectives.isEmpty();
    }
    
    /**
     * Checks if the course was approved
     */
    public boolean isApproved() {
        return approvedBy != null && approvedDate != null;
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
     * Gets the full course identifier
     */
    public String getFullCourseIdentifier() {
        return departmentCode + " " + courseNumber + " - " + courseName;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Determines course level from course number
     */
    private static String determineCourseLevel(String courseNumber) {
        if (courseNumber != null && !courseNumber.isEmpty()) {
            char firstDigit = courseNumber.charAt(0);
            if (firstDigit >= '1' && firstDigit <= '4') {
                return "UNDERGRADUATE";
            } else if (firstDigit >= '5' && firstDigit <= '9') {
                return "GRADUATE";
            }
        }
        return "UNKNOWN";
    }
    
    /**
     * Adds basic course metadata
     */
    private void addCourseMetadata() {
        addMetadata("course.id", courseId);
        addMetadata("course.code", courseCode);
        addMetadata("course.number", courseNumber);
        addMetadata("course.name", courseName);
        addMetadata("course.credits", credits);
        addMetadata("course.status", status.toString());
        addMetadata("course.maxEnrollment", maxEnrollment);
        addMetadata("course.level", courseLevel);
        addMetadata("course.isOnline", isOnline);
        addMetadata("course.isHybrid", isHybrid);
        addMetadata("course.hasWaitlist", hasWaitlist);
        addMetadata("course.deliveryMode", getDeliveryModeDisplay());
        addMetadata("course.createdBy", createdBy);
        
        if (location != null) {
            addMetadata("course.location", location);
        }
        
        if (schedule != null) {
            addMetadata("course.schedule", schedule);
        }
        
        if (hasPrerequisites()) {
            addMetadata("course.hasPrerequisites", true);
            addMetadata("course.prerequisiteCount", prerequisites.size());
        }
    }
    
    /**
     * Adds instructor metadata
     */
    private void addInstructorMetadata() {
        if (instructorId != null) {
            addMetadata("instructor.id", instructorId);
            addMetadata("instructor.name", instructorName);
        }
    }
    
    /**
     * Adds schedule metadata
     */
    private void addScheduleMetadata() {
        addMetadata("academic.semester", semester.toString());
        addMetadata("academic.academicYear", academicYear);
        addMetadata("academic.semesterDisplay", getSemesterDisplay());
        
        if (schedule != null) {
            // Parse schedule for more detailed metadata
            addMetadata("schedule.raw", schedule);
        }
    }
    
    /**
     * Adds academic metadata
     */
    private void addAcademicMetadata() {
        addMetadata("department.code", departmentCode);
        addMetadata("department.name", departmentName);
        
        if (syllabus != null) {
            addMetadata("course.hasSyllabus", true);
        }
        
        if (gradeScale != null) {
            addMetadata("course.gradeScale", gradeScale);
        }
        
        if (catalogDescription != null) {
            addMetadata("course.hasCatalogDescription", true);
        }
        
        if (hasLearningObjectives()) {
            addMetadata("course.hasLearningObjectives", true);
            addMetadata("course.learningObjectiveCount", learningObjectives.size());
        }
        
        if (isApproved()) {
            addMetadata("course.isApproved", true);
            addMetadata("course.approvedBy", approvedBy);
            addMetadata("course.approvedDate", approvedDate.toString());
        }
    }
    
    // ==================== BUILDER ====================
    
    /**
     * Builder for creating CourseCreatedEvent
     */
    public static class Builder extends Event.Builder<CourseCreatedEvent, Builder> {
        private String courseId;
        private String courseName;
        private String courseCode;
        private String courseNumber;
        private String departmentCode;
        private String departmentName;
        private String description;
        private int credits = 0;
        private Semester semester;
        private int academicYear;
        private CourseStatus status = CourseStatus.ACTIVE;
        private String instructorId;
        private String instructorName;
        private int maxEnrollment = 0;
        private String location;
        private String schedule;
        private List<String> prerequisites;
        private String createdBy;
        private LocalDateTime createdDate = LocalDateTime.now();
        private boolean isOnline = false;
        private boolean isHybrid = false;
        private String syllabus;
        private String gradeScale;
        private boolean hasWaitlist = false;
        private String approvedBy;
        private LocalDateTime approvedDate;
        private String catalogDescription;
        private List<String> learningObjectives;
        
        public Builder course(String courseId, String courseName, String courseCode, String courseNumber) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.courseCode = courseCode;
            this.courseNumber = courseNumber;
            return this;
        }
        
        public Builder department(String departmentCode, String departmentName) {
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder credits(int credits) {
            this.credits = credits;
            return this;
        }
        
        public Builder semester(Semester semester, int academicYear) {
            this.semester = semester;
            this.academicYear = academicYear;
            return this;
        }
        
        public Builder status(CourseStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder instructor(String instructorId, String instructorName) {
            this.instructorId = instructorId;
            this.instructorName = instructorName;
            return this;
        }
        
        public Builder enrollment(int maxEnrollment, boolean hasWaitlist) {
            this.maxEnrollment = maxEnrollment;
            this.hasWaitlist = hasWaitlist;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder schedule(String schedule) {
            this.schedule = schedule;
            return this;
        }
        
        public Builder prerequisites(List<String> prerequisites) {
            this.prerequisites = prerequisites;
            return this;
        }
        
        public Builder creation(String createdBy, LocalDateTime createdDate) {
            this.createdBy = createdBy;
            this.createdDate = createdDate;
            return this;
        }
        
        public Builder deliveryMode(boolean isOnline, boolean isHybrid) {
            this.isOnline = isOnline;
            this.isHybrid = isHybrid;
            return this;
        }
        
        public Builder syllabus(String syllabus) {
            this.syllabus = syllabus;
            return this;
        }
        
        public Builder gradeScale(String gradeScale) {
            this.gradeScale = gradeScale;
            return this;
        }
        
        public Builder approval(String approvedBy, LocalDateTime approvedDate) {
            this.approvedBy = approvedBy;
            this.approvedDate = approvedDate;
            return this;
        }
        
        public Builder catalogDescription(String catalogDescription) {
            this.catalogDescription = catalogDescription;
            return this;
        }
        
        public Builder learningObjectives(List<String> learningObjectives) {
            this.learningObjectives = learningObjectives;
            return this;
        }
        
        @Override
        public CourseCreatedEvent build() {
            CourseCreatedEvent event = new CourseCreatedEvent(
                courseId, courseName, courseCode, courseNumber, departmentCode, departmentName,
                description, credits, semester, academicYear, status, instructorId, instructorName,
                maxEnrollment, location, schedule, prerequisites, createdBy, createdDate,
                isOnline, isHybrid, syllabus, gradeScale, hasWaitlist, approvedBy, approvedDate,
                catalogDescription, learningObjectives
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
        
        CourseCreatedEvent that = (CourseCreatedEvent) obj;
        return Objects.equals(courseId, that.courseId) &&
               Objects.equals(courseCode, that.courseCode) &&
               Objects.equals(semester, that.semester) &&
               academicYear == that.academicYear &&
               Objects.equals(createdDate, that.createdDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), courseId, courseCode, semester, 
                           academicYear, createdDate);
    }
}