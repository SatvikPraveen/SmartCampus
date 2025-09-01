// File location: src/main/java/patterns/CourseBuilder.java

package patterns;

import models.*;
import utils.ValidationUtil;
import java.util.*;

/**
 * Builder pattern implementation for Course objects
 * Provides fluent interface for constructing Course instances with validation
 */
public class CourseBuilder {
    
    private String courseCode;
    private String name;
    private String description;
    private int credits;
    private Department department;
    private Professor professor;
    private String semester;
    private String academicYear;
    private int capacity;
    private int enrolledStudents;
    private Map<String, Object> additionalProperties;
    private boolean validateOnBuild = true;
    
    public CourseBuilder() {
        this.additionalProperties = new HashMap<>();
        this.credits = 3; // Default credit value
        this.capacity = 30; // Default capacity
        this.enrolledStudents = 0;
        this.semester = getCurrentSemester();
        this.academicYear = getCurrentAcademicYear();
    }
    
    /**
     * Set course code
     */
    public CourseBuilder courseCode(String courseCode) {
        this.courseCode = courseCode;
        return this;
    }
    
    /**
     * Set course name
     */
    public CourseBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Set course description
     */
    public CourseBuilder description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Set course credits
     */
    public CourseBuilder credits(int credits) {
        if (credits < 1 || credits > 6) {
            throw new IllegalArgumentException("Credits must be between 1 and 6");
        }
        this.credits = credits;
        return this;
    }
    
    /**
     * Set department
     */
    public CourseBuilder department(Department department) {
        this.department = department;
        return this;
    }
    
    /**
     * Set department by code
     */
    public CourseBuilder departmentCode(String departmentCode) {
        if (departmentCode != null && !departmentCode.trim().isEmpty()) {
            this.department = new Department(departmentCode, "", "", "", 0, 0);
        }
        return this;
    }
    
    /**
     * Set professor
     */
    public CourseBuilder professor(Professor professor) {
        this.professor = professor;
        return this;
    }
    
    /**
     * Set professor by ID
     */
    public CourseBuilder professorId(String professorId) {
        if (professorId != null && !professorId.trim().isEmpty()) {
            // In a real implementation, you'd look up the professor
            this.professor = new Professor(professorId, "", "", null, "", "", 0);
        }
        return this;
    }
    
    /**
     * Set semester
     */
    public CourseBuilder semester(String semester) {
        this.semester = semester;
        return this;
    }
    
    /**
     * Set as fall semester
     */
    public CourseBuilder fall() {
        this.semester = "Fall";
        return this;
    }
    
    /**
     * Set as spring semester
     */
    public CourseBuilder spring() {
        this.semester = "Spring";
        return this;
    }
    
    /**
     * Set as summer semester
     */
    public CourseBuilder summer() {
        this.semester = "Summer";
        return this;
    }
    
    /**
     * Set as winter semester
     */
    public CourseBuilder winter() {
        this.semester = "Winter";
        return this;
    }
    
    /**
     * Set academic year
     */
    public CourseBuilder academicYear(String academicYear) {
        this.academicYear = academicYear;
        return this;
    }
    
    /**
     * Set academic year as current year
     */
    public CourseBuilder currentAcademicYear() {
        this.academicYear = getCurrentAcademicYear();
        return this;
    }
    
    /**
     * Set capacity
     */
    public CourseBuilder capacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        this.capacity = capacity;
        return this;
    }
    
    /**
     * Set small class size (10-15 students)
     */
    public CourseBuilder smallClass() {
        this.capacity = 10 + new Random().nextInt(6); // 10-15
        return this;
    }
    
    /**
     * Set medium class size (20-35 students)
     */
    public CourseBuilder mediumClass() {
        this.capacity = 20 + new Random().nextInt(16); // 20-35
        return this;
    }
    
    /**
     * Set large class size (50-100 students)
     */
    public CourseBuilder largeClass() {
        this.capacity = 50 + new Random().nextInt(51); // 50-100
        return this;
    }
    
    /**
     * Set enrolled students count
     */
    public CourseBuilder enrolledStudents(int enrolledStudents) {
        if (enrolledStudents < 0) {
            throw new IllegalArgumentException("Enrolled students cannot be negative");
        }
        this.enrolledStudents = enrolledStudents;
        return this;
    }
    
    /**
     * Set course as full capacity
     */
    public CourseBuilder fullCapacity() {
        this.enrolledStudents = this.capacity;
        return this;
    }
    
    /**
     * Set course with available spots
     */
    public CourseBuilder withAvailableSpots(int availableSpots) {
        if (availableSpots < 0 || availableSpots > this.capacity) {
            throw new IllegalArgumentException("Available spots must be between 0 and capacity");
        }
        this.enrolledStudents = this.capacity - availableSpots;
        return this;
    }
    
    /**
     * Add additional property
     */
    public CourseBuilder property(String key, Object value) {
        this.additionalProperties.put(key, value);
        return this;
    }
    
    /**
     * Add multiple properties
     */
    public CourseBuilder properties(Map<String, Object> properties) {
        this.additionalProperties.putAll(properties);
        return this;
    }
    
    /**
     * Enable or disable validation on build
     */
    public CourseBuilder validateOnBuild(boolean validate) {
        this.validateOnBuild = validate;
        return this;
    }
    
    /**
     * Set course level
     */
    public CourseBuilder level(CourseLevel level) {
        this.additionalProperties.put("level", level);
        return this;
    }
    
    /**
     * Set as undergraduate course
     */
    public CourseBuilder undergraduate() {
        return level(CourseLevel.UNDERGRADUATE);
    }
    
    /**
     * Set as graduate course
     */
    public CourseBuilder graduate() {
        return level(CourseLevel.GRADUATE);
    }
    
    /**
     * Set as doctoral course
     */
    public CourseBuilder doctoral() {
        return level(CourseLevel.DOCTORAL);
    }
    
    /**
     * Set course type
     */
    public CourseBuilder type(CourseType type) {
        this.additionalProperties.put("type", type);
        return this;
    }
    
    /**
     * Set as lecture course
     */
    public CourseBuilder lecture() {
        return type(CourseType.LECTURE);
    }
    
    /**
     * Set as laboratory course
     */
    public CourseBuilder laboratory() {
        return type(CourseType.LABORATORY);
    }
    
    /**
     * Set as seminar course
     */
    public CourseBuilder seminar() {
        return type(CourseType.SEMINAR);
    }
    
    /**
     * Set as workshop course
     */
    public CourseBuilder workshop() {
        return type(CourseType.WORKSHOP);
    }
    
    /**
     * Set as online course
     */
    public CourseBuilder online() {
        return type(CourseType.ONLINE);
    }
    
    /**
     * Set as hybrid course
     */
    public CourseBuilder hybrid() {
        return type(CourseType.HYBRID);
    }
    
    /**
     * Set course schedule
     */
    public CourseBuilder schedule(String schedule) {
        this.additionalProperties.put("schedule", schedule);
        return this;
    }
    
    /**
     * Set course location/room
     */
    public CourseBuilder room(String room) {
        this.additionalProperties.put("room", room);
        return this;
    }
    
    /**
     * Set course prerequisites
     */
    public CourseBuilder prerequisites(List<String> prerequisites) {
        this.additionalProperties.put("prerequisites", new ArrayList<>(prerequisites));
        return this;
    }
    
    /**
     * Add single prerequisite
     */
    public CourseBuilder prerequisite(String prerequisite) {
        @SuppressWarnings("unchecked")
        List<String> prereqs = (List<String>) this.additionalProperties.computeIfAbsent("prerequisites", k -> new ArrayList<String>());
        prereqs.add(prerequisite);
        return this;
    }
    
    /**
     * Set course objectives
     */
    public CourseBuilder objectives(List<String> objectives) {
        this.additionalProperties.put("objectives", new ArrayList<>(objectives));
        return this;
    }
    
    /**
     * Add single objective
     */
    public CourseBuilder objective(String objective) {
        @SuppressWarnings("unchecked")
        List<String> objs = (List<String>) this.additionalProperties.computeIfAbsent("objectives", k -> new ArrayList<String>());
        objs.add(objective);
        return this;
    }
    
    /**
     * Set course textbook
     */
    public CourseBuilder textbook(String title, String author, String isbn) {
        Map<String, String> textbook = new HashMap<>();
        textbook.put("title", title);
        textbook.put("author", author);
        textbook.put("isbn", isbn);
        this.additionalProperties.put("textbook", textbook);
        return this;
    }
    
    /**
     * Set grading scheme
     */
    public CourseBuilder gradingScheme(Map<String, Double> gradingScheme) {
        this.additionalProperties.put("gradingScheme", new HashMap<>(gradingScheme));
        return this;
    }
    
    /**
     * Set standard grading scheme
     */
    public CourseBuilder standardGrading() {
        Map<String, Double> scheme = new HashMap<>();
        scheme.put("Exams", 60.0);
        scheme.put("Assignments", 25.0);
        scheme.put("Participation", 15.0);
        return gradingScheme(scheme);
    }
    
    /**
     * Set course difficulty level
     */
    public CourseBuilder difficulty(DifficultyLevel difficulty) {
        this.additionalProperties.put("difficulty", difficulty);
        return this;
    }
    
    /**
     * Set as beginner level
     */
    public CourseBuilder beginner() {
        return difficulty(DifficultyLevel.BEGINNER);
    }
    
    /**
     * Set as intermediate level
     */
    public CourseBuilder intermediate() {
        return difficulty(DifficultyLevel.INTERMEDIATE);
    }
    
    /**
     * Set as advanced level
     */
    public CourseBuilder advanced() {
        return difficulty(DifficultyLevel.ADVANCED);
    }
    
    /**
     * Set course tags/keywords
     */
    public CourseBuilder tags(String... tags) {
        this.additionalProperties.put("tags", Arrays.asList(tags));
        return this;
    }
    
    /**
     * Set course as active
     */
    public CourseBuilder active() {
        this.additionalProperties.put("status", "ACTIVE");
        return this;
    }
    
    /**
     * Set course as inactive
     */
    public CourseBuilder inactive() {
        this.additionalProperties.put("status", "INACTIVE");
        return this;
    }
    
    /**
     * Set course as cancelled
     */
    public CourseBuilder cancelled() {
        this.additionalProperties.put("status", "CANCELLED");
        return this;
    }
    
    /**
     * Create course builder from existing course
     */
    public static CourseBuilder from(Course existingCourse) {
        return new CourseBuilder()
            .courseCode(existingCourse.getCourseCode())
            .name(existingCourse.getName())
            .description(existingCourse.getDescription())
            .credits(existingCourse.getCredits())
            .department(existingCourse.getDepartment())
            .professor(existingCourse.getProfessor())
            .semester(existingCourse.getSemester())
            .academicYear(existingCourse.getAcademicYear())
            .capacity(existingCourse.getCapacity())
            .enrolledStudents(existingCourse.getEnrolledStudents());
    }
    
    /**
     * Create course with test data
     */
    public static CourseBuilder withTestData() {
        Random random = new Random();
        String[] subjects = {"Computer Science", "Mathematics", "Physics", "Chemistry", "Biology", "English", "History", "Art"};
        String[] types = {"Introduction to", "Advanced", "Principles of", "Foundations of", "Applied"};
        
        String subject = subjects[random.nextInt(subjects.length)];
        String type = types[random.nextInt(types.length)];
        String courseName = type + " " + subject;
        
        return new CourseBuilder()
            .courseCode("CS" + (100 + random.nextInt(400)))
            .name(courseName)
            .description("A comprehensive course covering " + courseName.toLowerCase())
            .credits(3 + random.nextInt(2)) // 3-4 credits
            .capacity(20 + random.nextInt(31)) // 20-50 capacity
            .enrolledStudents(random.nextInt(25)); // 0-24 enrolled
    }
    
    /**
     * Validate the course data
     */
    private void validate() {
        List<String> errors = new ArrayList<>();
        
        if (ValidationUtil.isEmpty(courseCode)) {
            errors.add("Course code is required");
        }
        
        if (ValidationUtil.isEmpty(name)) {
            errors.add("Course name is required");
        }
        
        if (credits < 1 || credits > 6) {
            errors.add("Credits must be between 1 and 6");
        }
        
        if (department == null) {
            errors.add("Department is required");
        }
        
        if (professor == null) {
            errors.add("Professor is required");
        }
        
        if (ValidationUtil.isEmpty(semester)) {
            errors.add("Semester is required");
        }
        
        if (ValidationUtil.isEmpty(academicYear)) {
            errors.add("Academic year is required");
        }
        
        if (capacity < 1) {
            errors.add("Capacity must be greater than 0");
        }
        
        if (enrolledStudents < 0) {
            errors.add("Enrolled students cannot be negative");
        }
        
        if (enrolledStudents > capacity) {
            errors.add("Enrolled students cannot exceed capacity");
        }
        
        // Validate additional properties
        validateAdditionalProperties(errors);
        
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Course validation failed: " + String.join(", ", errors));
        }
    }
    
    /**
     * Validate additional properties
     */
    private void validateAdditionalProperties(List<String> errors) {
        // Validate grading scheme totals to 100%
        @SuppressWarnings("unchecked")
        Map<String, Double> scheme = (Map<String, Double>) additionalProperties.get("gradingScheme");
        if (scheme != null) {
            double total = scheme.values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(total - 100.0) > 0.01) {
                errors.add("Grading scheme must total 100%");
            }
        }
        
        // Validate schedule format if present
        String schedule = (String) additionalProperties.get("schedule");
        if (schedule != null && !schedule.matches("^[MTWRFSU]+\\s+\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}$")) {
            errors.add("Schedule format should be like 'MWF 10:00-11:00'");
        }
    }
    
    /**
     * Get current semester based on date
     */
    private String getCurrentSemester() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        
        if (month >= Calendar.AUGUST || month <= Calendar.DECEMBER) {
            return "Fall";
        } else if (month >= Calendar.JANUARY && month <= Calendar.MAY) {
            return "Spring";
        } else {
            return "Summer";
        }
    }
    
    /**
     * Get current academic year
     */
    private String getCurrentAcademicYear() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        
        if (month >= Calendar.AUGUST) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }
    
    /**
     * Reset the builder to initial state
     */
    public CourseBuilder reset() {
        this.courseCode = null;
        this.name = null;
        this.description = null;
        this.credits = 3;
        this.department = null;
        this.professor = null;
        this.semester = getCurrentSemester();
        this.academicYear = getCurrentAcademicYear();
        this.capacity = 30;
        this.enrolledStudents = 0;
        this.additionalProperties.clear();
        this.validateOnBuild = true;
        return this;
    }
    
    /**
     * Check if all required fields are set
     */
    public boolean isComplete() {
        return !ValidationUtil.isEmpty(courseCode) &&
               !ValidationUtil.isEmpty(name) &&
               department != null &&
               professor != null &&
               !ValidationUtil.isEmpty(semester) &&
               !ValidationUtil.isEmpty(academicYear);
    }
    
    /**
     * Get a summary of the course being built
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Course Builder Summary:\n");
        sb.append("Course Code: ").append(courseCode != null ? courseCode : "Not set").append("\n");
        sb.append("Name: ").append(name != null ? name : "Not set").append("\n");
        sb.append("Credits: ").append(credits).append("\n");
        sb.append("Department: ").append(department != null ? department.getName() : "Not set").append("\n");
        sb.append("Professor: ").append(professor != null ? professor.getName() : "Not set").append("\n");
        sb.append("Semester: ").append(semester).append(" ").append(academicYear).append("\n");
        sb.append("Capacity: ").append(capacity).append(" (").append(enrolledStudents).append(" enrolled)\n");
        sb.append("Additional Properties: ").append(additionalProperties.size()).append(" items\n");
        sb.append("Complete: ").append(isComplete()).append("\n");
        return sb.toString();
    }
    
    /**
     * Build the Course object
     */
    public Course build() {
        if (validateOnBuild) {
            validate();
        }
        
        return new Course(courseCode, name, description, credits, department, professor,
                         semester, academicYear, capacity, enrolledStudents);
    }
    
    /**
     * Build multiple courses with variations
     */
    public List<Course> buildMultiple(int count) {
        List<Course> courses = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String variationCode = courseCode + "-" + String.format("%02d", i + 1);
            String variationName = name + " (Section " + (i + 1) + ")";
            
            Course course = new CourseBuilder()
                .courseCode(variationCode)
                .name(variationName)
                .description(description)
                .credits(credits)
                .department(department)
                .professor(professor)
                .semester(semester)
                .academicYear(academicYear)
                .capacity(capacity)
                .enrolledStudents(0) // Start with no enrolled students for variations
                .properties(additionalProperties)
                .validateOnBuild(validateOnBuild)
                .build();
                
            courses.add(course);
        }
        
        return courses;
    }
    
    // Nested enums for course properties
    public enum CourseLevel {
        UNDERGRADUATE, GRADUATE, DOCTORAL
    }
    
    public enum CourseType {
        LECTURE, LABORATORY, SEMINAR, WORKSHOP, ONLINE, HYBRID, INDEPENDENT_STUDY
    }
    
    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
}