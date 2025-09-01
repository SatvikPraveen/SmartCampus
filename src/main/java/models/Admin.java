// File: src/main/java/models/Admin.java
package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Admin class representing administrative users in the Smart Campus system.
 * Inherits from User and adds admin-specific functionality.
 * 
 * Key Java concepts demonstrated:
 * - Inheritance (extends User)
 * - Enums for type safety
 * - Constructor overloading
 * - Method overriding
 * - Encapsulation with private fields
 * - Collections (List)
 * - Object equality and hashing
 * - String formatting and toString
 */
public class Admin extends User {
    
    // Admin-specific enums
    public enum AdminLevel {
        JUNIOR_ADMIN("Junior Admin", 1),
        SENIOR_ADMIN("Senior Admin", 2),
        SYSTEM_ADMIN("System Administrator", 3),
        SUPER_ADMIN("Super Administrator", 4);
        
        private final String displayName;
        private final int level;
        
        AdminLevel(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }
    }
    
    public enum Department {
        ACADEMIC_AFFAIRS("Academic Affairs"),
        STUDENT_SERVICES("Student Services"),
        IT_SERVICES("IT Services"),
        HUMAN_RESOURCES("Human Resources"),
        FINANCE("Finance"),
        REGISTRAR("Registrar"),
        ADMISSIONS("Admissions"),
        FACILITIES("Facilities Management");
        
        private final String displayName;
        
        Department(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Admin-specific fields
    private String adminId;
    private AdminLevel adminLevel;
    private Department department;
    private String jobTitle;
    private LocalDateTime hireDate;
    private boolean hasSystemAccess;
    private boolean canManageUsers;
    private boolean canViewReports;
    private boolean canModifyData;
    private List<String> permissions;
    private List<String> managedDepartments;
    private String supervisorId;
    private double salary;
    private int vacationDays;
    private String emergencyContact;
    
    // Constructors
    
    /**
     * Default constructor for Admin.
     */
    public Admin() {
        super();
        this.permissions = new ArrayList<>();
        this.managedDepartments = new ArrayList<>();
        this.hasSystemAccess = false;
        this.canManageUsers = false;
        this.canViewReports = false;
        this.canModifyData = false;
        this.vacationDays = 20; // Default vacation days
    }
    
    /**
     * Constructor with basic admin information.
     */
    public Admin(String userId, String firstName, String lastName, String email, String phoneNumber,
                 String adminId, AdminLevel adminLevel, Department department, String jobTitle) {
        super(userId, firstName, lastName, email, phoneNumber);
        this.adminId = adminId;
        this.adminLevel = adminLevel;
        this.department = department;
        this.jobTitle = jobTitle;
        this.permissions = new ArrayList<>();
        this.managedDepartments = new ArrayList<>();
        this.hireDate = LocalDateTime.now();
        setDefaultPermissions();
    }
    
    /**
     * Full constructor with all admin details.
     */
    public Admin(String userId, String firstName, String lastName, String email, String phoneNumber,
                 String adminId, AdminLevel adminLevel, Department department, String jobTitle,
                 LocalDateTime hireDate, String supervisorId, double salary, String emergencyContact) {
        super(userId, firstName, lastName, email, phoneNumber);
        this.adminId = adminId;
        this.adminLevel = adminLevel;
        this.department = department;
        this.jobTitle = jobTitle;
        this.hireDate = hireDate;
        this.supervisorId = supervisorId;
        this.salary = salary;
        this.emergencyContact = emergencyContact;
        this.permissions = new ArrayList<>();
        this.managedDepartments = new ArrayList<>();
        this.vacationDays = 20;
        setDefaultPermissions();
    }
    
    // Override methods from User
    
    @Override
    public String getRole() {
        return "Admin";
    }
    
    @Override
    public void displayInfo() {
        System.out.println("=== ADMIN INFORMATION ===");
        super.displayInfo(); // Call parent's displayInfo
        System.out.println("Admin ID: " + adminId);
        System.out.println("Admin Level: " + adminLevel.getDisplayName());
        System.out.println("Department: " + department.getDisplayName());
        System.out.println("Job Title: " + jobTitle);
        System.out.println("Hire Date: " + (hireDate != null ? hireDate.toLocalDate() : "Not set"));
        System.out.println("Supervisor ID: " + (supervisorId != null ? supervisorId : "None"));
        System.out.println("System Access: " + (hasSystemAccess ? "Yes" : "No"));
        System.out.println("Can Manage Users: " + (canManageUsers ? "Yes" : "No"));
        System.out.println("Can View Reports: " + (canViewReports ? "Yes" : "No"));
        System.out.println("Can Modify Data: " + (canModifyData ? "Yes" : "No"));
        System.out.println("Permissions (" + permissions.size() + "): " + String.join(", ", permissions));
        System.out.println("Managed Departments (" + managedDepartments.size() + "): " + 
                          String.join(", ", managedDepartments));
        System.out.println("Vacation Days: " + vacationDays);
        System.out.println("Emergency Contact: " + (emergencyContact != null ? emergencyContact : "Not provided"));
    }
    
    // Admin-specific business methods
    
    /**
     * Set default permissions based on admin level.
     */
    private void setDefaultPermissions() {
        permissions.clear();
        
        switch (adminLevel) {
            case JUNIOR_ADMIN:
                hasSystemAccess = true;
                canViewReports = true;
                permissions.add("READ_STUDENT_DATA");
                permissions.add("READ_COURSE_DATA");
                break;
                
            case SENIOR_ADMIN:
                hasSystemAccess = true;
                canViewReports = true;
                canModifyData = true;
                permissions.add("READ_STUDENT_DATA");
                permissions.add("READ_COURSE_DATA");
                permissions.add("MODIFY_STUDENT_DATA");
                permissions.add("MODIFY_COURSE_DATA");
                break;
                
            case SYSTEM_ADMIN:
                hasSystemAccess = true;
                canViewReports = true;
                canModifyData = true;
                canManageUsers = true;
                permissions.add("FULL_SYSTEM_ACCESS");
                permissions.add("MANAGE_USERS");
                permissions.add("SYSTEM_CONFIGURATION");
                break;
                
            case SUPER_ADMIN:
                hasSystemAccess = true;
                canViewReports = true;
                canModifyData = true;
                canManageUsers = true;
                permissions.add("FULL_SYSTEM_ACCESS");
                permissions.add("MANAGE_USERS");
                permissions.add("SYSTEM_CONFIGURATION");
                permissions.add("MANAGE_ADMINS");
                permissions.add("DATABASE_ACCESS");
                break;
        }
    }
    
    /**
     * Add a permission to the admin.
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.trim().isEmpty() && !permissions.contains(permission)) {
            permissions.add(permission);
        }
    }
    
    /**
     * Remove a permission from the admin.
     */
    public void removePermission(String permission) {
        permissions.remove(permission);
    }
    
    /**
     * Check if admin has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Add a department to manage.
     */
    public void addManagedDepartment(String departmentId) {
        if (departmentId != null && !departmentId.trim().isEmpty() && !managedDepartments.contains(departmentId)) {
            managedDepartments.add(departmentId);
        }
    }
    
    /**
     * Remove a managed department.
     */
    public void removeManagedDepartment(String departmentId) {
        managedDepartments.remove(departmentId);
    }
    
    /**
     * Check if admin manages a specific department.
     */
    public boolean managesDepartment(String departmentId) {
        return managedDepartments.contains(departmentId);
    }
    
    /**
     * Promote admin to next level (if possible).
     */
    public boolean promoteAdmin() {
        AdminLevel[] levels = AdminLevel.values();
        int currentIndex = -1;
        
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] == adminLevel) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex >= 0 && currentIndex < levels.length - 1) {
            adminLevel = levels[currentIndex + 1];
            setDefaultPermissions(); // Update permissions for new level
            return true;
        }
        return false;
    }
    
    /**
     * Use vacation days.
     */
    public boolean useVacationDays(int days) {
        if (days > 0 && days <= vacationDays) {
            vacationDays -= days;
            return true;
        }
        return false;
    }
    
    /**
     * Add vacation days (annual refresh or bonus).
     */
    public void addVacationDays(int days) {
        if (days > 0) {
            vacationDays += days;
        }
    }
    
    // Getters and Setters
    
    public String getAdminId() {
        return adminId;
    }
    
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
    
    public AdminLevel getAdminLevel() {
        return adminLevel;
    }
    
    public void setAdminLevel(AdminLevel adminLevel) {
        this.adminLevel = adminLevel;
        setDefaultPermissions(); // Update permissions when level changes
    }
    
    public Department getDepartment() {
        return department;
    }
    
    public void setDepartment(Department department) {
        this.department = department;
    }
    
    public String getJobTitle() {
        return jobTitle;
    }
    
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
    
    public LocalDateTime getHireDate() {
        return hireDate;
    }
    
    public void setHireDate(LocalDateTime hireDate) {
        this.hireDate = hireDate;
    }
    
    public boolean hasSystemAccess() {
        return hasSystemAccess;
    }
    
    public void setSystemAccess(boolean hasSystemAccess) {
        this.hasSystemAccess = hasSystemAccess;
    }
    
    public boolean canManageUsers() {
        return canManageUsers;
    }
    
    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }
    
    public boolean canViewReports() {
        return canViewReports;
    }
    
    public void setCanViewReports(boolean canViewReports) {
        this.canViewReports = canViewReports;
    }
    
    public boolean canModifyData() {
        return canModifyData;
    }
    
    public void setCanModifyData(boolean canModifyData) {
        this.canModifyData = canModifyData;
    }
    
    public List<String> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }
    
    public List<String> getManagedDepartments() {
        return Collections.unmodifiableList(managedDepartments);
    }
    
    public String getSupervisorId() {
        return supervisorId;
    }
    
    public void setSupervisorId(String supervisorId) {
        this.supervisorId = supervisorId;
    }
    
    public double getSalary() {
        return salary;
    }
    
    public void setSalary(double salary) {
        if (salary >= 0) {
            this.salary = salary;
        }
    }
    
    public int getVacationDays() {
        return vacationDays;
    }
    
    public void setVacationDays(int vacationDays) {
        if (vacationDays >= 0) {
            this.vacationDays = vacationDays;
        }
    }
    
    public String getEmergencyContact() {
        return emergencyContact;
    }
    
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
    
    // Object overrides
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        Admin admin = (Admin) obj;
        return Objects.equals(adminId, admin.adminId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), adminId);
    }
    
    @Override
    public String toString() {
        return String.format("Admin{adminId='%s', name='%s', level='%s', dept='%s', title='%s', active=%s}", 
            adminId, getFullName(), adminLevel.getDisplayName(), department.getDisplayName(), 
            jobTitle, isActive());
    }
}