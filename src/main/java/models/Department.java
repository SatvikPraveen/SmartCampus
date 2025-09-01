// File: src/main/java/models/Department.java
package models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Department class representing academic departments in the Smart Campus system.
 * 
 * Key Java concepts covered:
 * - Encapsulation with validation
 * - Collections (List, Map)
 * - Stream API usage
 * - Method overloading
 * - Static methods
 * - Aggregation relationships
 * - Business logic implementation
 */
public class Department {
    
    // Department information
    private String departmentId;
    private String departmentCode; // e.g., "CS", "MATH", "ENG"
    private String departmentName;
    private String description;
    private String location; // Building and floor
    private String phoneNumber;
    private String email;
    private String website;
    
    // Leadership and staff
    private String headOfDepartmentId; // Professor ID
    private String administratorId; // Admin staff ID
    private List<String> professorIds;
    private List<String> staffIds;
    
    // Academic programs
    private List<String> courseIds;
    private List<String> majorPrograms; // e.g., "Computer Science", "Software Engineering"
    private List<String> minorPrograms;
    private List<String> graduatePrograms;
    
    // Students
    private List<String> studentIds; // Students majoring in this department
    private Map<String, Integer> studentsByYear; // Academic year -> count
    
    // Budget and resources
    private BigDecimal annualBudget;
    private BigDecimal allocatedBudget;
    private BigDecimal spentBudget;
    private List<String> facilities; // Labs, classrooms, etc.
    private Map<String, String> equipment; // Equipment name -> location
    
    // Department statistics
    private int totalEnrollment;
    private double averageClassSize;
    private double studentToFacultyRatio;
    private int numberOfPrograms;
    
    // Metadata
    private boolean isActive;
    private String establishedYear;
    private String accreditation;
    private String mission;
    private String vision;
    
    // Default constructor
    public Department() {
        this.professorIds = new ArrayList<>();
        this.staffIds = new ArrayList<>();
        this.courseIds = new ArrayList<>();
        this.majorPrograms = new ArrayList<>();
        this.minorPrograms = new ArrayList<>();
        this.graduatePrograms = new ArrayList<>();
        this.studentIds = new ArrayList<>();
        this.studentsByYear = new HashMap<>();
        this.facilities = new ArrayList<>();
        this.equipment = new HashMap<>();
        this.annualBudget = BigDecimal.ZERO;
        this.allocatedBudget = BigDecimal.ZERO;
        this.spentBudget = BigDecimal.ZERO;
        this.isActive = true;
        this.totalEnrollment = 0;
        this.averageClassSize = 0.0;
        this.studentToFacultyRatio = 0.0;
        this.numberOfPrograms = 0;
        initializeStudentsByYear();
    }
    
    // Parameterized constructor
    public Department(String departmentId, String departmentCode, String departmentName, 
                     String description, String location) {
        this();
        setDepartmentId(departmentId);
        setDepartmentCode(departmentCode);
        setDepartmentName(departmentName);
        setDescription(description);
        setLocation(location);
    }
    
    // Full constructor
    public Department(String departmentId, String departmentCode, String departmentName, 
                     String description, String location, String headOfDepartmentId, 
                     BigDecimal annualBudget) {
        this(departmentId, departmentCode, departmentName, description, location);
        this.headOfDepartmentId = headOfDepartmentId;
        this.annualBudget = annualBudget;
        this.allocatedBudget = annualBudget;
    }
    
    // Initialize student counts by academic year
    private void initializeStudentsByYear() {
        studentsByYear.put("Freshman", 0);
        studentsByYear.put("Sophomore", 0);
        studentsByYear.put("Junior", 0);
        studentsByYear.put("Senior", 0);
        studentsByYear.put("Graduate", 0);
    }
    
    // Professor management methods
    public void addProfessor(String professorId) {
        if (professorId != null && !professorId.trim().isEmpty() && 
            !professorIds.contains(professorId)) {
            professorIds.add(professorId);
            updateStatistics();
        }
    }
    
    public void removeProfessor(String professorId) {
        if (professorIds.remove(professorId)) {
            // If removing head of department, clear the position
            if (professorId.equals(headOfDepartmentId)) {
                headOfDepartmentId = null;
            }
            updateStatistics();
        }
    }
    
    public boolean hasProfessor(String professorId) {
        return professorIds.contains(professorId);
    }
    
    public int getProfessorCount() {
        return professorIds.size();
    }
    
    // Course management methods
    public void addCourse(String courseId) {
        if (courseId != null && !courseId.trim().isEmpty() && 
            !courseIds.contains(courseId)) {
            courseIds.add(courseId);
            updateStatistics();
        }
    }
    
    public void removeCourse(String courseId) {
        if (courseIds.remove(courseId)) {
            updateStatistics();
        }
    }
    
    public boolean hasCourse(String courseId) {
        return courseIds.contains(courseId);
    }
    
    public int getCourseCount() {
        return courseIds.size();
    }
    
    // Student management methods
    public void addStudent(String studentId, String academicYear) {
        if (studentId != null && !studentId.trim().isEmpty() && 
            !studentIds.contains(studentId)) {
            studentIds.add(studentId);
            
            if (academicYear != null && studentsByYear.containsKey(academicYear)) {
                studentsByYear.put(academicYear, studentsByYear.get(academicYear) + 1);
            }
            
            updateStatistics();
        }
    }
    
    public void removeStudent(String studentId, String academicYear) {
        if (studentIds.remove(studentId)) {
            if (academicYear != null && studentsByYear.containsKey(academicYear)) {
                int currentCount = studentsByYear.get(academicYear);
                if (currentCount > 0) {
                    studentsByYear.put(academicYear, currentCount - 1);
                }
            }
            updateStatistics();
        }
    }
    
    public boolean hasStudent(String studentId) {
        return studentIds.contains(studentId);
    }
    
    public int getStudentCount() {
        return studentIds.size();
    }
    
    public int getStudentCountByYear(String academicYear) {
        return studentsByYear.getOrDefault(academicYear, 0);
    }
    
    // Program management methods
    public void addMajorProgram(String program) {
        if (program != null && !program.trim().isEmpty() && 
            !majorPrograms.contains(program)) {
            majorPrograms.add(program);
            numberOfPrograms = majorPrograms.size() + minorPrograms.size() + graduatePrograms.size();
        }
    }
    
    public void addMinorProgram(String program) {
        if (program != null && !program.trim().isEmpty() && 
            !minorPrograms.contains(program)) {
            minorPrograms.add(program);
            numberOfPrograms = majorPrograms.size() + minorPrograms.size() + graduatePrograms.size();
        }
    }
    
    public void addGraduateProgram(String program) {
        if (program != null && !program.trim().isEmpty() && 
            !graduatePrograms.contains(program)) {
            graduatePrograms.add(program);
            numberOfPrograms = majorPrograms.size() + minorPrograms.size() + graduatePrograms.size();
        }
    }
    
    // Facility and equipment management
    public void addFacility(String facility) {
        if (facility != null && !facility.trim().isEmpty() && 
            !facilities.contains(facility)) {
            facilities.add(facility);
        }
    }
    
    public void addEquipment(String equipmentName, String location) {
        if (equipmentName != null && !equipmentName.trim().isEmpty()) {
            equipment.put(equipmentName, location);
        }
    }
    
    public void removeEquipment(String equipmentName) {
        equipment.remove(equipmentName);
    }
    
    public List<String> getEquipmentAtLocation(String location) {
        return equipment.entrySet().stream()
            .filter(entry -> location.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    // Budget management methods
    public boolean allocateBudget(BigDecimal amount, String purpose) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newSpent = spentBudget.add(amount);
            if (newSpent.compareTo(allocatedBudget) <= 0) {
                spentBudget = newSpent;
                return true;
            }
        }
        return false;
    }
    
    public BigDecimal getRemainingBudget() {
        return allocatedBudget.subtract(spentBudget);
    }
    
    public double getBudgetUtilizationPercentage() {
        if (allocatedBudget.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spentBudget.divide(allocatedBudget, 4, BigDecimal.ROUND_HALF_UP)
                         .multiply(BigDecimal.valueOf(100))
                         .doubleValue();
    }
    
    public boolean isOverBudget() {
        return spentBudget.compareTo(allocatedBudget) > 0;
    }
    
    // Statistics calculation methods
    private void updateStatistics() {
        totalEnrollment = studentIds.size();
        
        if (courseIds.size() > 0) {
            averageClassSize = (double) totalEnrollment / courseIds.size();
        }
        
        if (professorIds.size() > 0) {
            studentToFacultyRatio = (double) totalEnrollment / professorIds.size();
        }
    }
    
    public void calculateDetailedStatistics() {
        updateStatistics();
        
        // Additional complex statistics can be calculated here
        // This method demonstrates where more complex business logic would go
    }
    
    // Department performance methods
    public boolean isWellStaffed() {
        return studentToFacultyRatio <= 20.0 && professorIds.size() >= 3;
    }
    
    public boolean hasAdequateResources() {
        return facilities.size() >= 2 && 
               equipment.size() >= 5 && 
               !isOverBudget();
    }
    
    public String getDepartmentHealthStatus() {
        if (isWellStaffed() && hasAdequateResources() && totalEnrollment > 50) {
            return "Excellent";
        } else if (isWellStaffed() || hasAdequateResources()) {
            return "Good";
        } else if (totalEnrollment > 20) {
            return "Fair";
        } else {
            return "Needs Improvement";
        }
    }
    
    // Display methods
    public void displayDepartmentInfo() {
        System.out.println("=== Department Information ===");
        System.out.println("Department ID: " + departmentId);
        System.out.println("Department Code: " + departmentCode);
        System.out.println("Department Name: " + departmentName);
        System.out.println("Location: " + location);
        System.out.println("Head of Department: " + headOfDepartmentId);
        System.out.println("Contact: " + email + " | " + phoneNumber);
        System.out.println("Website: " + website);
        System.out.println();
        
        System.out.println("=== Statistics ===");
        System.out.println("Total Students: " + totalEnrollment);
        System.out.println("Total Professors: " + professorIds.size());
        System.out.println("Total Courses: " + courseIds.size());
        System.out.println("Student-to-Faculty Ratio: " + String.format("%.1f:1", studentToFacultyRatio));
        System.out.println("Average Class Size: " + String.format("%.1f", averageClassSize));
        System.out.println("Health Status: " + getDepartmentHealthStatus());
        System.out.println();
        
        System.out.println("=== Academic Programs ===");
        System.out.println("Major Programs (" + majorPrograms.size() + "): " + 
                          String.join(", ", majorPrograms));
        System.out.println("Minor Programs (" + minorPrograms.size() + "): " + 
                          String.join(", ", minorPrograms));
        System.out.println("Graduate Programs (" + graduatePrograms.size() + "): " + 
                          String.join(", ", graduatePrograms));
        System.out.println();
        
        System.out.println("=== Students by Academic Year ===");
        studentsByYear.forEach((year, count) -> 
            System.out.println(year + ": " + count));
        System.out.println();
        
        System.out.println("=== Budget Information ===");
        System.out.println("Annual Budget: $" + annualBudget);
        System.out.println("Allocated Budget: $" + allocatedBudget);
        System.out.println("Spent Budget: $" + spentBudget);
        System.out.println("Remaining Budget: $" + getRemainingBudget());
        System.out.println("Budget Utilization: " + String.format("%.1f%%", getBudgetUtilizationPercentage()));
        System.out.println();
        
        System.out.println("=== Facilities & Resources ===");
        System.out.println("Facilities (" + facilities.size() + "): " + String.join(", ", facilities));
        System.out.println("Equipment Items: " + equipment.size());
        System.out.println("Established: " + establishedYear);
        System.out.println("Accreditation: " + accreditation);
        System.out.println("Status: " + (isActive ? "Active" : "Inactive"));
    }
    
    public void displayBudgetSummary() {
        System.out.println("=== Budget Summary for " + departmentName + " ===");
        System.out.println("Annual Budget: $" + annualBudget);
        System.out.println("Allocated: $" + allocatedBudget);
        System.out.println("Spent: $" + spentBudget);
        System.out.println("Remaining: $" + getRemainingBudget());
        System.out.println("Utilization: " + String.format("%.1f%%", getBudgetUtilizationPercentage()));
        if (isOverBudget()) {
            System.out.println("⚠️  WARNING: Department is over budget!");
        }
    }
    
    // Static utility methods
    public static Department createBasicDepartment(String code, String name) {
        String id = "DEPT_" + code + "_" + System.currentTimeMillis();
        return new Department(id, code, name, "", "");
    }
    
    public static String generateDepartmentId(String departmentCode) {
        return "DEPT_" + departmentCode.toUpperCase() + "_" + System.currentTimeMillis();
    }
    
    // Search and filter methods using Stream API
    public List<String> searchEquipment(String keyword) {
        return equipment.keySet().stream()
            .filter(equipmentName -> equipmentName.toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    public List<String> getProgramsByType(String type) {
        switch (type.toLowerCase()) {
            case "major":
                return new ArrayList<>(majorPrograms);
            case "minor":
                return new ArrayList<>(minorPrograms);
            case "graduate":
                return new ArrayList<>(graduatePrograms);
            default:
                List<String> allPrograms = new ArrayList<>();
                allPrograms.addAll(majorPrograms);
                allPrograms.addAll(minorPrograms);
                allPrograms.addAll(graduatePrograms);
                return allPrograms;
        }
    }
    
    // Validation methods
    public boolean isValidDepartment() {
        return departmentId != null && !departmentId.trim().isEmpty() &&
               departmentCode != null && !departmentCode.trim().isEmpty() &&
               departmentName != null && !departmentName.trim().isEmpty() &&
               isActive;
    }
    
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        
        if (departmentId == null || departmentId.trim().isEmpty()) {
            errors.add("Department ID is required");
        }
        if (departmentCode == null || departmentCode.trim().isEmpty()) {
            errors.add("Department code is required");
        }
        if (departmentName == null || departmentName.trim().isEmpty()) {
            errors.add("Department name is required");
        }
        if (headOfDepartmentId == null || headOfDepartmentId.trim().isEmpty()) {
            errors.add("Head of department must be assigned");
        }
        if (professorIds.isEmpty()) {
            errors.add("Department must have at least one professor");
        }
        if (majorPrograms.isEmpty()) {
            errors.add("Department must offer at least one major program");
        }
        
        return errors;
    }
    
    // Getters and Setters with validation
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        if (departmentId == null || departmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Department ID cannot be null or empty");
        }
        this.departmentId = departmentId.trim();
    }
    
    public String getDepartmentCode() {
        return departmentCode;
    }
    
    public void setDepartmentCode(String departmentCode) {
        if (departmentCode == null || departmentCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Department code cannot be null or empty");
        }
        this.departmentCode = departmentCode.trim().toUpperCase();
    }
    
    public String getDepartmentName() {
        return departmentName;
    }
    
    public void setDepartmentName(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name cannot be null or empty");
        }
        this.departmentName = departmentName.trim();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        if (email != null && !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public String getHeadOfDepartmentId() {
        return headOfDepartmentId;
    }
    
    public void setHeadOfDepartmentId(String headOfDepartmentId) {
        this.headOfDepartmentId = headOfDepartmentId;
    }
    
    public String getAdministratorId() {
        return administratorId;
    }
    
    public void setAdministratorId(String administratorId) {
        this.administratorId = administratorId;
    }
    
    public List<String> getProfessorIds() {
        return new ArrayList<>(professorIds); // Defensive copy
    }
    
    public List<String> getStaffIds() {
        return new ArrayList<>(staffIds); // Defensive copy
    }
    
    public List<String> getCourseIds() {
        return new ArrayList<>(courseIds); // Defensive copy
    }
    
    public List<String> getMajorPrograms() {
        return new ArrayList<>(majorPrograms); // Defensive copy
    }
    
    public List<String> getMinorPrograms() {
        return new ArrayList<>(minorPrograms); // Defensive copy
    }
    
    public List<String> getGraduatePrograms() {
        return new ArrayList<>(graduatePrograms); // Defensive copy
    }
    
    public List<String> getStudentIds() {
        return new ArrayList<>(studentIds); // Defensive copy
    }
    
    public Map<String, Integer> getStudentsByYear() {
        return new HashMap<>(studentsByYear); // Defensive copy
    }
    
    public BigDecimal getAnnualBudget() {
        return annualBudget;
    }
    
    public void setAnnualBudget(BigDecimal annualBudget) {
        if (annualBudget != null && annualBudget.compareTo(BigDecimal.ZERO) >= 0) {
            this.annualBudget = annualBudget;
            this.allocatedBudget = annualBudget; // Reset allocated budget
        }
    }
    
    public BigDecimal getAllocatedBudget() {
        return allocatedBudget;
    }
    
    public void setAllocatedBudget(BigDecimal allocatedBudget) {
        if (allocatedBudget != null && allocatedBudget.compareTo(BigDecimal.ZERO) >= 0 &&
            allocatedBudget.compareTo(annualBudget) <= 0) {
            this.allocatedBudget = allocatedBudget;
        }
    }
    
    public BigDecimal getSpentBudget() {
        return spentBudget;
    }
    
    public List<String> getFacilities() {
        return new ArrayList<>(facilities); // Defensive copy
    }
    
    public Map<String, String> getEquipment() {
        return new HashMap<>(equipment); // Defensive copy
    }
    
    public int getTotalEnrollment() {
        return totalEnrollment;
    }
    
    public double getAverageClassSize() {
        return averageClassSize;
    }
    
    public double getStudentToFacultyRatio() {
        return studentToFacultyRatio;
    }
    
    public int getNumberOfPrograms() {
        return numberOfPrograms;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getEstablishedYear() {
        return establishedYear;
    }
    
    public void setEstablishedYear(String establishedYear) {
        this.establishedYear = establishedYear;
    }
    
    public String getAccreditation() {
        return accreditation;
    }
    
    public void setAccreditation(String accreditation) {
        this.accreditation = accreditation;
    }
    
    public String getMission() {
        return mission;
    }
    
    public void setMission(String mission) {
        this.mission = mission;
    }
    
    public String getVision() {
        return vision;
    }
    
    public void setVision(String vision) {
        this.vision = vision;
    }
    
    // Object overrides
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Department that = (Department) obj;
        return Objects.equals(departmentId, that.departmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(departmentId);
    }
    
    @Override
    public String toString() {
        return String.format("Department{id='%s', code='%s', name='%s', students=%d, professors=%d, courses=%d, active=%s}", 
            departmentId, departmentCode, departmentName, totalEnrollment, professorIds.size(), courseIds.size(), isActive);
    }
}