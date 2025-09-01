// File location: src/main/java/patterns/StudentBuilder.java

package patterns;

import models.*;
import utils.ValidationUtil;
import java.util.*;

/**
 * Builder pattern implementation for Student objects
 * Provides fluent interface for constructing Student instances with validation
 */
public class StudentBuilder {
    
    private String id;
    private String name;
    private String email;
    private Department department;
    private Date enrollmentDate;
    private Map<String, Object> additionalProperties;
    private boolean validateOnBuild = true;
    
    public StudentBuilder() {
        this.additionalProperties = new HashMap<>();
        this.enrollmentDate = new Date(); // Default to current date
    }
    
    /**
     * Set student ID
     */
    public StudentBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * Set student name
     */
    public StudentBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Set student email
     */
    public StudentBuilder email(String email) {
        this.email = email;
        return this;
    }
    
    /**
     * Set student department
     */
    public StudentBuilder department(Department department) {
        this.department = department;
        return this;
    }
    
    /**
     * Set student department by code
     */
    public StudentBuilder departmentCode(String departmentCode) {
        // In a real implementation, you'd look up the department
        // For now, create a minimal department object
        if (departmentCode != null && !departmentCode.trim().isEmpty()) {
            this.department = new Department(departmentCode, "", "", "", 0, 0);
        }
        return this;
    }
    
    /**
     * Set enrollment date
     */
    public StudentBuilder enrollmentDate(Date enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
        return this;
    }
    
    /**
     * Set enrollment date with string (yyyy-MM-dd format)
     */
    public StudentBuilder enrollmentDate(String enrollmentDate) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            this.enrollmentDate = sdf.parse(enrollmentDate);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd");
        }
        return this;
    }
    
    /**
     * Add additional property
     */
    public StudentBuilder property(String key, Object value) {
        this.additionalProperties.put(key, value);
        return this;
    }
    
    /**
     * Add multiple properties
     */
    public StudentBuilder properties(Map<String, Object> properties) {
        this.additionalProperties.putAll(properties);
        return this;
    }
    
    /**
     * Enable or disable validation on build
     */
    public StudentBuilder validateOnBuild(boolean validate) {
        this.validateOnBuild = validate;
        return this;
    }
    
    /**
     * Set student as freshman (first year)
     */
    public StudentBuilder freshman() {
        this.additionalProperties.put("academicYear", "Freshman");
        this.additionalProperties.put("yearLevel", 1);
        return this;
    }
    
    /**
     * Set student as sophomore (second year)
     */
    public StudentBuilder sophomore() {
        this.additionalProperties.put("academicYear", "Sophomore");
        this.additionalProperties.put("yearLevel", 2);
        return this;
    }
    
    /**
     * Set student as junior (third year)
     */
    public StudentBuilder junior() {
        this.additionalProperties.put("academicYear", "Junior");
        this.additionalProperties.put("yearLevel", 3);
        return this;
    }
    
    /**
     * Set student as senior (fourth year)
     */
    public StudentBuilder senior() {
        this.additionalProperties.put("academicYear", "Senior");
        this.additionalProperties.put("yearLevel", 4);
        return this;
    }
    
    /**
     * Set student as graduate student
     */
    public StudentBuilder graduate() {
        this.additionalProperties.put("academicYear", "Graduate");
        this.additionalProperties.put("yearLevel", 5);
        return this;
    }
    
    /**
     * Set student status
     */
    public StudentBuilder status(StudentStatus status) {
        this.additionalProperties.put("status", status);
        return this;
    }
    
    /**
     * Set student as active
     */
    public StudentBuilder active() {
        return status(StudentStatus.ACTIVE);
    }
    
    /**
     * Set student as inactive
     */
    public StudentBuilder inactive() {
        return status(StudentStatus.INACTIVE);
    }
    
    /**
     * Set student as graduated
     */
    public StudentBuilder graduated() {
        return status(StudentStatus.GRADUATED);
    }
    
    /**
     * Set student as suspended
     */
    public StudentBuilder suspended() {
        return status(StudentStatus.SUSPENDED);
    }
    
    /**
     * Set student GPA
     */
    public StudentBuilder gpa(double gpa) {
        if (gpa < 0.0 || gpa > 4.0) {
            throw new IllegalArgumentException("GPA must be between 0.0 and 4.0");
        }
        this.additionalProperties.put("gpa", gpa);
        return this;
    }
    
    /**
     * Set student major
     */
    public StudentBuilder major(String major) {
        this.additionalProperties.put("major", major);
        return this;
    }
    
    /**
     * Set student minor
     */
    public StudentBuilder minor(String minor) {
        this.additionalProperties.put("minor", minor);
        return this;
    }
    
    /**
     * Set student phone number
     */
    public StudentBuilder phoneNumber(String phoneNumber) {
        this.additionalProperties.put("phoneNumber", phoneNumber);
        return this;
    }
    
    /**
     * Set student address
     */
    public StudentBuilder address(String address) {
        this.additionalProperties.put("address", address);
        return this;
    }
    
    /**
     * Set student emergency contact
     */
    public StudentBuilder emergencyContact(String name, String phone) {
        Map<String, String> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("phone", phone);
        this.additionalProperties.put("emergencyContact", contact);
        return this;
    }
    
    /**
     * Set student date of birth
     */
    public StudentBuilder dateOfBirth(Date dateOfBirth) {
        this.additionalProperties.put("dateOfBirth", dateOfBirth);
        return this;
    }
    
    /**
     * Set student date of birth with string (yyyy-MM-dd format)
     */
    public StudentBuilder dateOfBirth(String dateOfBirth) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            Date dob = sdf.parse(dateOfBirth);
            this.additionalProperties.put("dateOfBirth", dob);
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd");
        }
        return this;
    }
    
    /**
     * Set student nationality
     */
    public StudentBuilder nationality(String nationality) {
        this.additionalProperties.put("nationality", nationality);
        return this;
    }
    
    /**
     * Set student as international
     */
    public StudentBuilder international() {
        this.additionalProperties.put("isInternational", true);
        return this;
    }
    
    /**
     * Set student as domestic
     */
    public StudentBuilder domestic() {
        this.additionalProperties.put("isInternational", false);
        return this;
    }
    
    /**
     * Set student scholarship information
     */
    public StudentBuilder scholarship(String scholarshipName, double amount) {
        Map<String, Object> scholarship = new HashMap<>();
        scholarship.put("name", scholarshipName);
        scholarship.put("amount", amount);
        this.additionalProperties.put("scholarship", scholarship);
        return this;
    }
    
    /**
     * Set student financial aid status
     */
    public StudentBuilder financialAid(boolean hasFinancialAid) {
        this.additionalProperties.put("hasFinancialAid", hasFinancialAid);
        return this;
    }
    
    /**
     * Set student part-time status
     */
    public StudentBuilder partTime() {
        this.additionalProperties.put("enrollmentType", "Part-time");
        return this;
    }
    
    /**
     * Set student full-time status
     */
    public StudentBuilder fullTime() {
        this.additionalProperties.put("enrollmentType", "Full-time");
        return this;
    }
    
    /**
     * Create student from existing student (copy constructor pattern)
     */
    public static StudentBuilder from(Student existingStudent) {
        return new StudentBuilder()
            .id(existingStudent.getId())
            .name(existingStudent.getName())
            .email(existingStudent.getEmail())
            .department(existingStudent.getDepartment())
            .enrollmentDate(existingStudent.getEnrollmentDate());
    }
    
    /**
     * Create student with random test data
     */
    public static StudentBuilder withTestData() {
        Random random = new Random();
        String[] firstNames = {"John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Emily"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"};
        
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@university.edu";
        
        return new StudentBuilder()
            .id("STU" + String.format("%06d", random.nextInt(999999)))
            .name(firstName + " " + lastName)
            .email(email)
            .gpa(2.0 + random.nextDouble() * 2.0) // GPA between 2.0 and 4.0
            .phoneNumber("555-" + String.format("%04d", random.nextInt(9999)));
    }
    
    /**
     * Validate the student data
     */
    private void validate() {
        List<String> errors = new ArrayList<>();
        
        if (ValidationUtil.isEmpty(id)) {
            errors.add("Student ID is required");
        }
        
        if (ValidationUtil.isEmpty(name)) {
            errors.add("Student name is required");
        }
        
        if (ValidationUtil.isEmpty(email)) {
            errors.add("Student email is required");
        } else if (!ValidationUtil.isValidEmail(email)) {
            errors.add("Student email format is invalid");
        }
        
        if (department == null) {
            errors.add("Student department is required");
        }
        
        if (enrollmentDate == null) {
            errors.add("Enrollment date is required");
        } else if (enrollmentDate.after(new Date())) {
            errors.add("Enrollment date cannot be in the future");
        }
        
        // Validate additional properties
        validateAdditionalProperties(errors);
        
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Student validation failed: " + String.join(", ", errors));
        }
    }
    
    /**
     * Validate additional properties
     */
    private void validateAdditionalProperties(List<String> errors) {
        // Validate GPA if present
        Object gpaObj = additionalProperties.get("gpa");
        if (gpaObj instanceof Double) {
            double gpa = (Double) gpaObj;
            if (gpa < 0.0 || gpa > 4.0) {
                errors.add("GPA must be between 0.0 and 4.0");
            }
        }
        
        // Validate year level if present
        Object yearLevelObj = additionalProperties.get("yearLevel");
        if (yearLevelObj instanceof Integer) {
            int yearLevel = (Integer) yearLevelObj;
            if (yearLevel < 1 || yearLevel > 6) {
                errors.add("Year level must be between 1 and 6");
            }
        }
        
        // Validate phone number if present
        Object phoneObj = additionalProperties.get("phoneNumber");
        if (phoneObj instanceof String) {
            String phone = (String) phoneObj;
            if (!ValidationUtil.isValidPhoneNumber(phone)) {
                errors.add("Phone number format is invalid");
            }
        }
        
        // Validate date of birth if present
        Object dobObj = additionalProperties.get("dateOfBirth");
        if (dobObj instanceof Date) {
            Date dob = (Date) dobObj;
            if (dob.after(new Date())) {
                errors.add("Date of birth cannot be in the future");
            }
            
            // Check minimum age (e.g., 16 years old)
            Calendar minAge = Calendar.getInstance();
            minAge.add(Calendar.YEAR, -16);
            if (dob.after(minAge.getTime())) {
                errors.add("Student must be at least 16 years old");
            }
        }
    }
    
    /**
     * Reset the builder to initial state
     */
    public StudentBuilder reset() {
        this.id = null;
        this.name = null;
        this.email = null;
        this.department = null;
        this.enrollmentDate = new Date();
        this.additionalProperties.clear();
        this.validateOnBuild = true;
        return this;
    }
    
    /**
     * Check if all required fields are set
     */
    public boolean isComplete() {
        return !ValidationUtil.isEmpty(id) &&
               !ValidationUtil.isEmpty(name) &&
               !ValidationUtil.isEmpty(email) &&
               department != null &&
               enrollmentDate != null;
    }
    
    /**
     * Get a summary of the student being built
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Student Builder Summary:\n");
        sb.append("ID: ").append(id != null ? id : "Not set").append("\n");
        sb.append("Name: ").append(name != null ? name : "Not set").append("\n");
        sb.append("Email: ").append(email != null ? email : "Not set").append("\n");
        sb.append("Department: ").append(department != null ? department.getName() : "Not set").append("\n");
        sb.append("Enrollment Date: ").append(enrollmentDate != null ? enrollmentDate : "Not set").append("\n");
        sb.append("Additional Properties: ").append(additionalProperties.size()).append(" items\n");
        sb.append("Complete: ").append(isComplete()).append("\n");
        return sb.toString();
    }
    
    /**
     * Build the Student object
     */
    public Student build() {
        if (validateOnBuild) {
            validate();
        }
        
        // Create the base Student object
        Student student = new Student(id, name, email, department, enrollmentDate);
        
        // Apply additional properties if the Student class supports them
        // In a real implementation, you might use reflection or extend the Student class
        // For now, we'll create the basic student object
        
        return student;
    }
    
    /**
     * Build multiple students with variations
     */
    public List<Student> buildMultiple(int count) {
        List<Student> students = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Create variation for each student
            String variationId = id + "_" + (i + 1);
            String variationEmail = email.replace("@", "+" + (i + 1) + "@");
            
            Student student = new StudentBuilder()
                .id(variationId)
                .name(name + " " + (i + 1))
                .email(variationEmail)
                .department(department)
                .enrollmentDate(enrollmentDate)
                .properties(additionalProperties)
                .validateOnBuild(validateOnBuild)
                .build();
                
            students.add(student);
        }
        
        return students;
    }
    
    // Nested enum for student status
    public enum StudentStatus {
        ACTIVE, INACTIVE, GRADUATED, SUSPENDED, TRANSFERRED, WITHDRAWN
    }
}