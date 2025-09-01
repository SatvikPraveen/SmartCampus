// File: src/main/java/models/University.java
package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * University class representing the main university entity in the Smart Campus system.
 * This is the top-level container that manages all other entities.
 * 
 * Key Java concepts demonstrated:
 * - Composition and aggregation relationships
 * - Collections management (Lists, Maps)
 * - Enums for type safety
 * - Business logic and validation
 * - Static factory methods
 * - Builder pattern implementation
 * - Statistical calculations
 * - Data aggregation methods
 */
public class University {
    
    // University type enumeration
    public enum UniversityType {
        PUBLIC("Public University"),
        PRIVATE("Private University"),
        COMMUNITY_COLLEGE("Community College"),
        TECHNICAL_INSTITUTE("Technical Institute"),
        LIBERAL_ARTS("Liberal Arts College"),
        RESEARCH_UNIVERSITY("Research University"),
        ONLINE("Online University");
        
        private final String displayName;
        
        UniversityType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Accreditation status
    public enum AccreditationStatus {
        FULLY_ACCREDITED("Fully Accredited"),
        CONDITIONALLY_ACCREDITED("Conditionally Accredited"),
        CANDIDATE_STATUS("Candidate Status"),
        NOT_ACCREDITED("Not Accredited"),
        UNDER_REVIEW("Under Review");
        
        private final String displayName;
        
        AccreditationStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Fields
    private String universityId;
    private String name;
    private String shortName;
    private String motto;
    private UniversityType type;
    private AccreditationStatus accreditationStatus;
    private LocalDate foundedDate;
    private LocalDateTime lastModified;
    
    // Contact and location information
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phoneNumber;
    private String emailDomain;
    private String website;
    
    // Administrative information
    private String presidentName;
    private String registrarName;
    private String adminContactEmail;
    private String emergencyContact;
    private String mailingAddress;
    
    // Academic information
    private List<String> departmentIds;
    private List<String> studentIds;
    private List<String> professorIds;
    private List<String> adminIds;
    private List<String> courseIds;
    private Map<String, Integer> enrollmentByDepartment;
    private Map<String, Integer> enrollmentBySemester;
    
    // Statistics and metrics
    private int totalStudents;
    private int totalProfessors;
    private int totalAdmins;
    private int totalDepartments;
    private int totalCourses;
    private double averageGPA;
    private double graduationRate;
    private double retentionRate;
    private double facultyToStudentRatio;
    
    // Financial information
    private double tuitionInState;
    private double tuitionOutOfState;
    private double tuitionInternational;
    private double endowmentAmount;
    private double annualBudget;
    
    // Academic calendar
    private LocalDate currentSemesterStart;
    private LocalDate currentSemesterEnd;
    private LocalDate nextSemesterStart;
    private LocalDate nextSemesterEnd;
    private List<String> semesterSchedule;
    
    // Settings and configuration
    private boolean isActive;
    private boolean acceptingApplications;
    private int maxStudentsPerCourse;
    private int maxCoursesPerStudent;
    private double minimumGpaRequirement;
    private String currentSemester;
    private int currentYear;
    
    // Constructors
    
    /**
     * Default constructor.
     */
    public University() {
        this.lastModified = LocalDateTime.now();
        this.departmentIds = new ArrayList<>();
        this.studentIds = new ArrayList<>();
        this.professorIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
        this.courseIds = new ArrayList<>();
        this.enrollmentByDepartment = new HashMap<>();
        this.enrollmentBySemester = new HashMap<>();
        this.semesterSchedule = new ArrayList<>();
        this.isActive = true;
        this.acceptingApplications = true;
        this.maxStudentsPerCourse = 30;
        this.maxCoursesPerStudent = 6;
        this.minimumGpaRequirement = 2.0;
        initializeDefaultSemesters();
    }
    
    /**
     * Constructor with basic university information.
     */
    public University(String universityId, String name, String shortName, UniversityType type) {
        this();
        this.universityId = universityId;
        this.name = name;
        this.shortName = shortName;
        this.type = type;
        this.foundedDate = LocalDate.now();
    }
    
    /**
     * Constructor with detailed university information.
     */
    public University(String universityId, String name, String shortName, String motto,
                     UniversityType type, AccreditationStatus accreditationStatus,
                     LocalDate foundedDate, String city, String state, String country) {
        this(universityId, name, shortName, type);
        this.motto = motto;
        this.accreditationStatus = accreditationStatus;
        this.foundedDate = foundedDate;
        this.city = city;
        this.state = state;
        this.country = country;
    }
    
    // Static factory methods
    
    /**
     * Create a new university with generated ID.
     */
    public static University createUniversity(String name, String shortName, UniversityType type) {
        String universityId = generateUniversityId(shortName);
        return new University(universityId, name, shortName, type);
    }
    
    /**
     * Create a research university.
     */
    public static University createResearchUniversity(String name, String shortName, String state) {
        University university = createUniversity(name, shortName, UniversityType.RESEARCH_UNIVERSITY);
        university.setState(state);
        university.setAccreditationStatus(AccreditationStatus.FULLY_ACCREDITED);
        return university;
    }
    
    // Business logic methods
    
    /**
     * Add a department to the university.
     */
    public boolean addDepartment(String departmentId) {
        if (departmentId != null && !departmentId.trim().isEmpty() && !departmentIds.contains(departmentId)) {
            departmentIds.add(departmentId);
            enrollmentByDepartment.put(departmentId, 0);
            totalDepartments = departmentIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Remove a department from the university.
     */
    public boolean removeDepartment(String departmentId) {
        if (departmentIds.remove(departmentId)) {
            enrollmentByDepartment.remove(departmentId);
            totalDepartments = departmentIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Add a student to the university.
     */
    public boolean addStudent(String studentId) {
        if (studentId != null && !studentId.trim().isEmpty() && !studentIds.contains(studentId)) {
            studentIds.add(studentId);
            totalStudents = studentIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Remove a student from the university.
     */
    public boolean removeStudent(String studentId) {
        if (studentIds.remove(studentId)) {
            totalStudents = studentIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Add a professor to the university.
     */
    public boolean addProfessor(String professorId) {
        if (professorId != null && !professorId.trim().isEmpty() && !professorIds.contains(professorId)) {
            professorIds.add(professorId);
            totalProfessors = professorIds.size();
            calculateFacultyToStudentRatio();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Remove a professor from the university.
     */
    public boolean removeProfessor(String professorId) {
        if (professorIds.remove(professorId)) {
            totalProfessors = professorIds.size();
            calculateFacultyToStudentRatio();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Add an admin to the university.
     */
    public boolean addAdmin(String adminId) {
        if (adminId != null && !adminId.trim().isEmpty() && !adminIds.contains(adminId)) {
            adminIds.add(adminId);
            totalAdmins = adminIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Remove an admin from the university.
     */
    public boolean removeAdmin(String adminId) {
        if (adminIds.remove(adminId)) {
            totalAdmins = adminIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Add a course to the university.
     */
    public boolean addCourse(String courseId) {
        if (courseId != null && !courseId.trim().isEmpty() && !courseIds.contains(courseId)) {
            courseIds.add(courseId);
            totalCourses = courseIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Remove a course from the university.
     */
    public boolean removeCourse(String courseId) {
        if (courseIds.remove(courseId)) {
            totalCourses = courseIds.size();
            updateLastModified();
            return true;
        }
        return false;
    }
    
    /**
     * Update enrollment statistics for a department.
     */
    public void updateDepartmentEnrollment(String departmentId, int enrollmentCount) {
        if (departmentIds.contains(departmentId)) {
            enrollmentByDepartment.put(departmentId, enrollmentCount);
            updateLastModified();
        }
    }
    
    /**
     * Update enrollment statistics for a semester.
     */
    public void updateSemesterEnrollment(String semester, int enrollmentCount) {
        enrollmentBySemester.put(semester, enrollmentCount);
        updateLastModified();
    }
    
    /**
     * Calculate faculty to student ratio.
     */
    private void calculateFacultyToStudentRatio() {
        if (totalProfessors > 0) {
            this.facultyToStudentRatio = (double) totalStudents / totalProfessors;
        } else {
            this.facultyToStudentRatio = 0.0;
        }
    }
    
    /**
     * Update statistics and metrics.
     */
    public void updateStatistics(double averageGPA, double graduationRate, double retentionRate) {
        this.averageGPA = averageGPA;
        this.graduationRate = graduationRate;
        this.retentionRate = retentionRate;
        updateLastModified();
    }
    
    /**
     * Check if university is currently accepting applications.
     */
    public boolean isAcceptingApplications() {
        return acceptingApplications && isActive;
    }
    
    /**
     * Start a new semester.
     */
    public boolean startNewSemester(String semester, int year, LocalDate startDate, LocalDate endDate) {
        this.currentSemester = semester;
        this.currentYear = year;
        this.currentSemesterStart = startDate;
        this.currentSemesterEnd = endDate;
        updateLastModified();
        return true;
    }
    
    /**
     * Get total enrollment across all departments.
     */
    public int getTotalEnrollment() {
        return enrollmentByDepartment.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get department with highest enrollment.
     */
    public String getMostPopularDepartment() {
        return enrollmentByDepartment.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Get enrollment statistics summary.
     */
    public Map<String, Object> getEnrollmentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalProfessors", totalProfessors);
        stats.put("totalDepartments", totalDepartments);
        stats.put("totalCourses", totalCourses);
        stats.put("facultyToStudentRatio", facultyToStudentRatio);
        stats.put("averageGPA", averageGPA);
        stats.put("graduationRate", graduationRate);
        stats.put("retentionRate", retentionRate);
        stats.put("totalEnrollment", getTotalEnrollment());
        stats.put("mostPopularDepartment", getMostPopularDepartment());
        return stats;
    }
    
    /**
     * Display university information.
     */
    public void displayInfo() {
        System.out.println("=== UNIVERSITY INFORMATION ===");
        System.out.println("University ID: " + universityId);
        System.out.println("Name: " + name);
        System.out.println("Short Name: " + shortName);
        System.out.println("Motto: " + (motto != null ? motto : "Not set"));
        System.out.println("Type: " + type.getDisplayName());
        System.out.println("Accreditation: " + (accreditationStatus != null ? accreditationStatus.getDisplayName() : "Not set"));
        System.out.println("Founded: " + (foundedDate != null ? foundedDate : "Not set"));
        System.out.println("Location: " + getFullAddress());
        System.out.println("Website: " + (website != null ? website : "Not set"));
        System.out.println("Phone: " + (phoneNumber != null ? phoneNumber : "Not set"));
        System.out.println("Email Domain: " + (emailDomain != null ? emailDomain : "Not set"));
        System.out.println();
        
        System.out.println("=== ACADEMIC STATISTICS ===");
        System.out.println("Total Students: " + totalStudents);
        System.out.println("Total Professors: " + totalProfessors);
        System.out.println("Total Administrators: " + totalAdmins);
        System.out.println("Total Departments: " + totalDepartments);
        System.out.println("Total Courses: " + totalCourses);
        System.out.println("Faculty-to-Student Ratio: 1:" + String.format("%.1f", facultyToStudentRatio));
        System.out.println("Average GPA: " + String.format("%.2f", averageGPA));
        System.out.println("Graduation Rate: " + String.format("%.1f%%", graduationRate));
        System.out.println("Retention Rate: " + String.format("%.1f%%", retentionRate));
        System.out.println();
        
        System.out.println("=== CURRENT SEMESTER ===");
        System.out.println("Semester: " + (currentSemester != null ? currentSemester + " " + currentYear : "Not set"));
        System.out.println("Start Date: " + (currentSemesterStart != null ? currentSemesterStart : "Not set"));
        System.out.println("End Date: " + (currentSemesterEnd != null ? currentSemesterEnd : "Not set"));
        System.out.println("Accepting Applications: " + (isAcceptingApplications() ? "Yes" : "No"));
        System.out.println("Status: " + (isActive ? "Active" : "Inactive"));
    }
    
    // Helper methods
    
    /**
     * Initialize default semester schedule.
     */
    private void initializeDefaultSemesters() {
        semesterSchedule.add("Fall");
        semesterSchedule.add("Spring");
        semesterSchedule.add("Summer");
    }
    
    /**
     * Generate university ID.
     */
    private static String generateUniversityId(String shortName) {
        return "UNI_" + shortName.toUpperCase().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis() % 10000;
    }
    
    /**
     * Update last modified timestamp.
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Get full address string.
     */
    public String getFullAddress() {
        StringBuilder addressBuilder = new StringBuilder();
        if (address != null) addressBuilder.append(address);
        if (city != null) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(city);
        }
        if (state != null) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(state);
        }
        if (zipCode != null) {
            if (addressBuilder.length() > 0) addressBuilder.append(" ");
            addressBuilder.append(zipCode);
        }
        if (country != null) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(country);
        }
        return addressBuilder.toString();
    }
    
    // Getters and Setters
    
    public String getUniversityId() {
        return universityId;
    }
    
    public void setUniversityId(String universityId) {
        this.universityId = universityId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        updateLastModified();
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public void setShortName(String shortName) {
        this.shortName = shortName;
        updateLastModified();
    }
    
    public String getMotto() {
        return motto;
    }
    
    public void setMotto(String motto) {
        this.motto = motto;
        updateLastModified();
    }
    
    public UniversityType getType() {
        return type;
    }
    
    public void setType(UniversityType type) {
        this.type = type;
        updateLastModified();
    }
    
    public AccreditationStatus getAccreditationStatus() {
        return accreditationStatus;
    }
    
    public void setAccreditationStatus(AccreditationStatus accreditationStatus) {
        this.accreditationStatus = accreditationStatus;
        updateLastModified();
    }
    
    public LocalDate getFoundedDate() {
        return foundedDate;
    }
    
    public void setFoundedDate(LocalDate foundedDate) {
        this.foundedDate = foundedDate;
        updateLastModified();
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
        updateLastModified();
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
        updateLastModified();
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
        updateLastModified();
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
        updateLastModified();
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
        updateLastModified();
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        updateLastModified();
    }
    
    public String getEmailDomain() {
        return emailDomain;
    }
    
    public void setEmailDomain(String emailDomain) {
        this.emailDomain = emailDomain;
        updateLastModified();
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
        updateLastModified();
    }
    
    public String getPresidentName() {
        return presidentName;
    }
    
    public void setPresidentName(String presidentName) {
        this.presidentName = presidentName;
        updateLastModified();
    }
    
    public String getRegistrarName() {
        return registrarName;
    }
    
    public void setRegistrarName(String registrarName) {
        this.registrarName = registrarName;
        updateLastModified();
    }
    
    public String getAdminContactEmail() {
        return adminContactEmail;
    }
    
    public void setAdminContactEmail(String adminContactEmail) {
        this.adminContactEmail = adminContactEmail;
        updateLastModified();
    }
    
    public String getEmergencyContact() {
        return emergencyContact;
    }
    
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
        updateLastModified();
    }
    
    public String getMailingAddress() {
        return mailingAddress;
    }
    
    public void setMailingAddress(String mailingAddress) {
        this.mailingAddress = mailingAddress;
        updateLastModified();
    }
    
    public List<String> getDepartmentIds() {
        return Collections.unmodifiableList(departmentIds);
    }
    
    public List<String> getStudentIds() {
        return Collections.unmodifiableList(studentIds);
    }
    
    public List<String> getProfessorIds() {
        return Collections.unmodifiableList(professorIds);
    }
    
    public List<String> getAdminIds() {
        return Collections.unmodifiableList(adminIds);
    }
    
    public List<String> getCourseIds() {
        return Collections.unmodifiableList(courseIds);
    }
    
    public Map<String, Integer> getEnrollmentByDepartment() {
        return Collections.unmodifiableMap(enrollmentByDepartment);
    }
    
    public Map<String, Integer> getEnrollmentBySemester() {
        return Collections.unmodifiableMap(enrollmentBySemester);
    }
    
    public int getTotalStudents() {
        return totalStudents;
    }
    
    public int getTotalProfessors() {
        return totalProfessors;
    }
    
    public int getTotalAdmins() {
        return totalAdmins;
    }
    
    public int getTotalDepartments() {
        return totalDepartments;
    }
    
    public int getTotalCourses() {
        return totalCourses;
    }
    
    public double getAverageGPA() {
        return averageGPA;
    }
    
    public void setAverageGPA(double averageGPA) {
        if (averageGPA >= 0.0 && averageGPA <= 4.0) {
            this.averageGPA = averageGPA;
            updateLastModified();
        }
    }
    
    public double getGraduationRate() {
        return graduationRate;
    }
    
    public void setGraduationRate(double graduationRate) {
        if (graduationRate >= 0.0 && graduationRate <= 100.0) {
            this.graduationRate = graduationRate;
            updateLastModified();
        }
    }
    
    public double getRetentionRate() {
        return retentionRate;
    }
    
    public void setRetentionRate(double retentionRate) {
        if (retentionRate >= 0.0 && retentionRate <= 100.0) {
            this.retentionRate = retentionRate;
            updateLastModified();
        }
    }
    
    public double getFacultyToStudentRatio() {
        return facultyToStudentRatio;
    }
    
    public double getTuitionInState() {
        return tuitionInState;
    }
    
    public void setTuitionInState(double tuitionInState) {
        if (tuitionInState >= 0) {
            this.tuitionInState = tuitionInState;
            updateLastModified();
        }
    }
    
    public double getTuitionOutOfState() {
        return tuitionOutOfState;
    }
    
    public void setTuitionOutOfState(double tuitionOutOfState) {
        if (tuitionOutOfState >= 0) {
            this.tuitionOutOfState = tuitionOutOfState;
            updateLastModified();
        }
    }
    
    public double getTuitionInternational() {
        return tuitionInternational;
    }
    
    public void setTuitionInternational(double tuitionInternational) {
        if (tuitionInternational >= 0) {
            this.tuitionInternational = tuitionInternational;
            updateLastModified();
        }
    }
    
    public double getEndowmentAmount() {
        return endowmentAmount;
    }
    
    public void setEndowmentAmount(double endowmentAmount) {
        if (endowmentAmount >= 0) {
            this.endowmentAmount = endowmentAmount;
            updateLastModified();
        }
    }
    
    public double getAnnualBudget() {
        return annualBudget;
    }
    
    public void setAnnualBudget(double annualBudget) {
        if (annualBudget >= 0) {
            this.annualBudget = annualBudget;
            updateLastModified();
        }
    }
    
    public LocalDate getCurrentSemesterStart() {
        return currentSemesterStart;
    }
    
    public void setCurrentSemesterStart(LocalDate currentSemesterStart) {
        this.currentSemesterStart = currentSemesterStart;
        updateLastModified();
    }
    
    public LocalDate getCurrentSemesterEnd() {
        return currentSemesterEnd;
    }
    
    public void setCurrentSemesterEnd(LocalDate currentSemesterEnd) {
        this.currentSemesterEnd = currentSemesterEnd;
        updateLastModified();
    }
    
    public LocalDate getNextSemesterStart() {
        return nextSemesterStart;
    }
    
    public void setNextSemesterStart(LocalDate nextSemesterStart) {
        this.nextSemesterStart = nextSemesterStart;
        updateLastModified();
    }
    
    public LocalDate getNextSemesterEnd() {
        return nextSemesterEnd;
    }
    
    public void setNextSemesterEnd(LocalDate nextSemesterEnd) {
        this.nextSemesterEnd = nextSemesterEnd;
        updateLastModified();
    }
    
    public List<String> getSemesterSchedule() {
        return Collections.unmodifiableList(semesterSchedule);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
        updateLastModified();
    }
    
    public void setAcceptingApplications(boolean acceptingApplications) {
        this.acceptingApplications = acceptingApplications;
        updateLastModified();
    }
    
    public int getMaxStudentsPerCourse() {
        return maxStudentsPerCourse;
    }
    
    public void setMaxStudentsPerCourse(int maxStudentsPerCourse) {
        if (maxStudentsPerCourse > 0) {
            this.maxStudentsPerCourse = maxStudentsPerCourse;
            updateLastModified();
        }
    }
    
    public int getMaxCoursesPerStudent() {
        return maxCoursesPerStudent;
    }
    
    public void setMaxCoursesPerStudent(int maxCoursesPerStudent) {
        if (maxCoursesPerStudent > 0) {
            this.maxCoursesPerStudent = maxCoursesPerStudent;
            updateLastModified();
        }
    }
    
    public double getMinimumGpaRequirement() {
        return minimumGpaRequirement;
    }
    
    public void setMinimumGpaRequirement(double minimumGpaRequirement) {
        if (minimumGpaRequirement >= 0.0 && minimumGpaRequirement <= 4.0) {
            this.minimumGpaRequirement = minimumGpaRequirement;
            updateLastModified();
        }
    }
    
    public String getCurrentSemester() {
        return currentSemester;
    }
    
    public void setCurrentSemester(String currentSemester) {
        this.currentSemester = currentSemester;
        updateLastModified();
    }
    
    public int getCurrentYear() {
        return currentYear;
    }
    
    public void setCurrentYear(int currentYear) {
        this.currentYear = currentYear;
        updateLastModified();
    }
    
    // Object overrides
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        University that = (University) obj;
        return Objects.equals(universityId, that.universityId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(universityId);
    }
    
    @Override
    public String toString() {
        return String.format("University{id='%s', name='%s', type='%s', students=%d, professors=%d, departments=%d, active=%s}", 
            universityId, name, type.getDisplayName(), totalStudents, totalProfessors, totalDepartments, isActive);
    }
}