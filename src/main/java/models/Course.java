// File: src/main/java/models/Course.java
package models;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Course class representing academic courses in the Smart Campus system.
 * 
 * Key Java concepts covered:
 * - Encapsulation with validation
 * - Collections (List, Map)
 * - Enums for type safety
 * - Method overloading
 * - Static final constants
 * - Defensive copying
 * - Business logic implementation
 */
public class Course {
    
    // Constants
    public static final int DEFAULT_CREDITS = 3;
    public static final int MAX_STUDENTS = 50;
    public static final int MIN_STUDENTS = 5;
    
    // Enum for course status
    public enum CourseStatus {
        DRAFT("Draft"),
        OPEN("Open for Enrollment"),
        CLOSED("Enrollment Closed"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String description;
        
        CourseStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return description; }
    }
    
    // Enum for course difficulty level
    public enum DifficultyLevel {
        BEGINNER(1, "Beginner"),
        INTERMEDIATE(2, "Intermediate"),
        ADVANCED(3, "Advanced"),
        EXPERT(4, "Expert");
        
        private final int level;
        private final String name;
        
        DifficultyLevel(int level, String name) {
            this.level = level;
            this.name = name;
        }
        
        public int getLevel() { return level; }
        public String getName() { return name; }
        
        @Override
        public String toString() { return name; }
    }
    
    // Core course information
    private String courseId;
    private String courseCode; // e.g., "CS101"
    private String courseName;
    private String description;
    private int credits;
    private String departmentId;
    private String professorId;
    private CourseStatus status;
    private DifficultyLevel difficultyLevel;
    
    // Enrollment information
    private List<String> enrolledStudentIds;
    private List<String> waitlistedStudentIds;
    private int maxEnrollment;
    
    // Course prerequisites and requirements
    private List<String> prerequisiteCourseIds;
    private List<String> corequisiteCourseIds; // Must take simultaneously
    
    // Schedule information
    private List<String> meetingDays; // e.g., ["Monday", "Wednesday", "Friday"]
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroom;
    private String building;
    
    // Course materials and resources
    private List<String> textbooks;
    private List<String> onlineResources;
    private String syllabus;
    
    // Grading and assessment
    private Map<String, Double> gradingComponents; // e.g., "Midterm" -> 30.0
    private double passingGrade;
    
    // Course metadata
    private String semester; // e.g., "Fall 2024"
    private int year;
    private boolean isActive;
    private String courseNotes;
    
    // Default constructor
    public Course() {
        this.enrolledStudentIds = new ArrayList<>();
        this.waitlistedStudentIds = new ArrayList<>();
        this.prerequisiteCourseIds = new ArrayList<>();
        this.corequisiteCourseIds = new ArrayList<>();
        this.meetingDays = new ArrayList<>();
        this.textbooks = new ArrayList<>();
        this.onlineResources = new ArrayList<>();
        this.gradingComponents = new HashMap<>();
        this.credits = DEFAULT_CREDITS;
        this.maxEnrollment = MAX_STUDENTS;
        this.status = CourseStatus.DRAFT;
        this.difficultyLevel = DifficultyLevel.BEGINNER;
        this.passingGrade = 60.0;
        this.isActive = true;
        this.year = 2024;
    }
    
    // Parameterized constructor
    public Course(String courseId, String courseCode, String courseName, 
                  String description, int credits, String departmentId) {
        this();
        setCourseId(courseId);
        setCourseCode(courseCode);
        setCourseName(courseName);
        setDescription(description);
        setCredits(credits);
        setDepartmentId(departmentId);
    }
    
    // Full constructor
    public Course(String courseId, String courseCode, String courseName, 
                  String description, int credits, String departmentId, String professorId, 
                  DifficultyLevel difficultyLevel, String semester, int year) {
        this(courseId, courseCode, courseName, description, credits, departmentId);
        this.professorId = professorId;
        this.difficultyLevel = difficultyLevel;
        this.semester = semester;
        this.year = year;
    }
    
    // Enrollment methods
    public boolean enrollStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return false;
        }
        
        if (status != CourseStatus.OPEN) {
            return false;
        }
        
        if (enrolledStudentIds.contains(studentId)) {
            return false; // Already enrolled
        }
        
        if (enrolledStudentIds.size() >= maxEnrollment) {
            // Add to waitlist if not already there
            if (!waitlistedStudentIds.contains(studentId)) {
                waitlistedStudentIds.add(studentId);
            }
            return false;
        }
        
        enrolledStudentIds.add(studentId);
        waitlistedStudentIds.remove(studentId); // Remove from waitlist if they were there
        
        // Close enrollment if at capacity
        if (enrolledStudentIds.size() >= maxEnrollment) {
            status = CourseStatus.CLOSED;
        }
        
        return true;
    }
    
    public boolean dropStudent(String studentId) {
        boolean removed = enrolledStudentIds.remove(studentId);
        
        if (removed) {
            // If there's a waitlist, automatically enroll the first student
            if (!waitlistedStudentIds.isEmpty()) {
                String nextStudent = waitlistedStudentIds.remove(0);
                enrolledStudentIds.add(nextStudent);
            } else if (status == CourseStatus.CLOSED && enrolledStudentIds.size() < maxEnrollment) {
                status = CourseStatus.OPEN; // Reopen enrollment
            }
        }
        
        return removed;
    }
    
    public boolean addToWaitlist(String studentId) {
        if (studentId != null && !studentId.trim().isEmpty() && 
            !enrolledStudentIds.contains(studentId) && 
            !waitlistedStudentIds.contains(studentId)) {
            waitlistedStudentIds.add(studentId);
            return true;
        }
        return false;
    }
    
    // Prerequisite methods
    public void addPrerequisite(String courseId) {
        if (courseId != null && !courseId.trim().isEmpty() && 
            !prerequisiteCourseIds.contains(courseId)) {
            prerequisiteCourseIds.add(courseId);
        }
    }
    
    public void removePrerequisite(String courseId) {
        prerequisiteCourseIds.remove(courseId);
    }
    
    public boolean hasPrerequisites() {
        return !prerequisiteCourseIds.isEmpty();
    }
    
    // Schedule methods
    public void setSchedule(List<String> days, LocalTime startTime, LocalTime endTime) {
        this.meetingDays = new ArrayList<>(days);
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public void addMeetingDay(String day) {
        if (day != null && !day.trim().isEmpty() && !meetingDays.contains(day)) {
            meetingDays.add(day);
        }
    }
    
    // Grading methods
    public void addGradingComponent(String component, double weight) {
        if (component != null && !component.trim().isEmpty() && weight > 0) {
            gradingComponents.put(component, weight);
        }
    }
    
    public void removeGradingComponent(String component) {
        gradingComponents.remove(component);
    }
    
    public double getTotalGradingWeight() {
        return gradingComponents.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    // Course resource methods
    public void addTextbook(String textbook) {
        if (textbook != null && !textbook.trim().isEmpty() && !textbooks.contains(textbook)) {
            textbooks.add(textbook);
        }
    }
    
    public void addOnlineResource(String resource) {
        if (resource != null && !resource.trim().isEmpty() && !onlineResources.contains(resource)) {
            onlineResources.add(resource);
        }
    }
    
    // Status management
    public void openEnrollment() {
        if (status == CourseStatus.DRAFT && isValidForEnrollment()) {
            status = CourseStatus.OPEN;
        }
    }
    
    public void closeEnrollment() {
        if (status == CourseStatus.OPEN) {
            status = CourseStatus.CLOSED;
        }
    }
    
    public void startCourse() {
        if ((status == CourseStatus.CLOSED || status == CourseStatus.OPEN) && 
            enrolledStudentIds.size() >= MIN_STUDENTS) {
            status = CourseStatus.IN_PROGRESS;
        }
    }
    
    public void completeCourse() {
        if (status == CourseStatus.IN_PROGRESS) {
            status = CourseStatus.COMPLETED;
        }
    }
    
    public void cancelCourse() {
        if (status != CourseStatus.COMPLETED) {
            status = CourseStatus.CANCELLED;
        }
    }
    
    // Validation methods
    public boolean isValidForEnrollment() {
        return professorId != null && !professorId.trim().isEmpty() &&
               courseName != null && !courseName.trim().isEmpty() &&
               courseCode != null && !courseCode.trim().isEmpty() &&
               departmentId != null && !departmentId.trim().isEmpty() &&
               credits > 0 && maxEnrollment > 0;
    }
    
    public boolean isEnrollmentOpen() {
        return status == CourseStatus.OPEN && enrolledStudentIds.size() < maxEnrollment;
    }
    
    public boolean hasWaitlist() {
        return !waitlistedStudentIds.isEmpty();
    }
    
    public int getAvailableSeats() {
        return Math.max(0, maxEnrollment - enrolledStudentIds.size());
    }
    
    public double getEnrollmentPercentage() {
        return maxEnrollment > 0 ? (double) enrolledStudentIds.size() / maxEnrollment * 100 : 0;
    }
    
    // Display method
    public void displayCourseInfo() {
        System.out.println("=== Course Information ===");
        System.out.println("Course ID: " + courseId);
        System.out.println("Course Code: " + courseCode);
        System.out.println("Course Name: " + courseName);
        System.out.println("Credits: " + credits);
        System.out.println("Department: " + departmentId);
        System.out.println("Professor: " + professorId);
        System.out.println("Status: " + status);
        System.out.println("Difficulty: " + difficultyLevel);
        System.out.println("Semester: " + semester + " " + year);
        System.out.println("Enrolled: " + enrolledStudentIds.size() + "/" + maxEnrollment);
        System.out.println("Waitlist: " + waitlistedStudentIds.size());
        System.out.println("Meeting Days: " + String.join(", ", meetingDays));
        if (startTime != null && endTime != null) {
            System.out.println("Time: " + startTime + " - " + endTime);
        }
        System.out.println("Classroom: " + classroom + " (" + building + ")");
        System.out.println("Prerequisites: " + prerequisiteCourseIds.size());
        System.out.println("Active: " + (isActive ? "Yes" : "No"));
    }
    
    // Getters and Setters with validation
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        if (courseId == null || courseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Course ID cannot be null or empty");
        }
        this.courseId = courseId.trim();
    }
    
    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be null or empty");
        }
        this.courseCode = courseCode.trim().toUpperCase();
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Course name cannot be null or empty");
        }
        this.courseName = courseName.trim();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getCredits() {
        return credits;
    }
    
    public void setCredits(int credits) {
        if (credits <= 0 || credits > 6) {
            throw new IllegalArgumentException("Credits must be between 1 and 6");
        }
        this.credits = credits;
    }
    
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    public String getProfessorId() {
        return professorId;
    }
    
    public void setProfessorId(String professorId) {
        this.professorId = professorId;
    }
    
    public CourseStatus getStatus() {
        return status;
    }
    
    public void setStatus(CourseStatus status) {
        this.status = status;
    }
    
    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }
    
    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
    
    public List<String> getEnrolledStudentIds() {
        return new ArrayList<>(enrolledStudentIds); // Defensive copy
    }
    
    public List<String> getWaitlistedStudentIds() {
        return new ArrayList<>(waitlistedStudentIds); // Defensive copy
    }
    
    public int getMaxEnrollment() {
        return maxEnrollment;
    }
    
    public void setMaxEnrollment(int maxEnrollment) {
        if (maxEnrollment <= 0) {
            throw new IllegalArgumentException("Max enrollment must be positive");
        }
        this.maxEnrollment = maxEnrollment;
    }
    
    public List<String> getPrerequisiteCourseIds() {
        return new ArrayList<>(prerequisiteCourseIds); // Defensive copy
    }
    
    public List<String> getCorequisiteCourseIds() {
        return new ArrayList<>(corequisiteCourseIds); // Defensive copy
    }
    
    public List<String> getMeetingDays() {
        return new ArrayList<>(meetingDays); // Defensive copy
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
    
    public String getClassroom() {
        return classroom;
    }
    
    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }
    
    public String getBuilding() {
        return building;
    }
    
    public void setBuilding(String building) {
        this.building = building;
    }
    
    public List<String> getTextbooks() {
        return new ArrayList<>(textbooks); // Defensive copy
    }
    
    public List<String> getOnlineResources() {
        return new ArrayList<>(onlineResources); // Defensive copy
    }
    
    public String getSyllabus() {
        return syllabus;
    }
    
    public void setSyllabus(String syllabus) {
        this.syllabus = syllabus;
    }
    
    public Map<String, Double> getGradingComponents() {
        return new HashMap<>(gradingComponents); // Defensive copy
    }
    
    public double getPassingGrade() {
        return passingGrade;
    }
    
    public void setPassingGrade(double passingGrade) {
        if (passingGrade < 0 || passingGrade > 100) {
            throw new IllegalArgumentException("Passing grade must be between 0 and 100");
        }
        this.passingGrade = passingGrade;
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
        if (year < 2020 || year > 2030) {
            throw new IllegalArgumentException("Year must be between 2020 and 2030");
        }
        this.year = year;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getCourseNotes() {
        return courseNotes;
    }
    
    public void setCourseNotes(String courseNotes) {
        this.courseNotes = courseNotes;
    }
    
    // Object overrides
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Course course = (Course) obj;
        return Objects.equals(courseId, course.courseId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }
    
    @Override
    public String toString() {
        return String.format("Course{id='%s', code='%s', name='%s', credits=%d, enrolled=%d/%d, status='%s'}", 
            courseId, courseCode, courseName, credits, enrolledStudentIds.size(), maxEnrollment, status);
    }
}