// File location: src/main/java/enums/UserRole.java

package enums;

import java.util.*;

/**
 * Enumeration defining user roles in the SmartCampus system
 * Includes role hierarchy, permissions, and access levels
 */
public enum UserRole {
    
    // Student roles
    STUDENT("Student", "Basic student access", 1, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.ENROLL_IN_COURSES,
        Permission.VIEW_OWN_GRADES,
        Permission.VIEW_SCHEDULE,
        Permission.SUBMIT_ASSIGNMENTS
    )),
    
    STUDENT_LEADER("Student Leader", "Student with leadership responsibilities", 2, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.ENROLL_IN_COURSES,
        Permission.VIEW_OWN_GRADES,
        Permission.VIEW_SCHEDULE,
        Permission.SUBMIT_ASSIGNMENTS,
        Permission.ORGANIZE_STUDENT_EVENTS,
        Permission.ACCESS_STUDENT_RESOURCES
    )),
    
    GRADUATE_STUDENT("Graduate Student", "Graduate level student access", 3, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.ENROLL_IN_COURSES,
        Permission.VIEW_OWN_GRADES,
        Permission.VIEW_SCHEDULE,
        Permission.SUBMIT_ASSIGNMENTS,
        Permission.ACCESS_RESEARCH_RESOURCES,
        Permission.MENTOR_UNDERGRADUATES
    )),
    
    // Faculty roles
    TEACHING_ASSISTANT("Teaching Assistant", "Graduate student teaching assistant", 4, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.VIEW_ASSIGNED_COURSES,
        Permission.GRADE_ASSIGNMENTS,
        Permission.VIEW_STUDENT_GRADES,
        Permission.ASSIST_IN_TEACHING,
        Permission.ACCESS_RESEARCH_RESOURCES
    )),
    
    ADJUNCT_PROFESSOR("Adjunct Professor", "Part-time faculty member", 5, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.MANAGE_ASSIGNED_COURSES,
        Permission.GRADE_ASSIGNMENTS,
        Permission.VIEW_STUDENT_GRADES,
        Permission.EDIT_COURSE_CONTENT,
        Permission.COMMUNICATE_WITH_STUDENTS
    )),
    
    ASSISTANT_PROFESSOR("Assistant Professor", "Entry-level tenure-track faculty", 6, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.MANAGE_ASSIGNED_COURSES,
        Permission.CREATE_COURSES,
        Permission.GRADE_ASSIGNMENTS,
        Permission.VIEW_STUDENT_GRADES,
        Permission.EDIT_COURSE_CONTENT,
        Permission.COMMUNICATE_WITH_STUDENTS,
        Permission.ACCESS_RESEARCH_RESOURCES,
        Permission.SUPERVISE_STUDENTS
    )),
    
    ASSOCIATE_PROFESSOR("Associate Professor", "Mid-level tenure-track faculty", 7, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.MANAGE_ASSIGNED_COURSES,
        Permission.CREATE_COURSES,
        Permission.GRADE_ASSIGNMENTS,
        Permission.VIEW_STUDENT_GRADES,
        Permission.EDIT_COURSE_CONTENT,
        Permission.COMMUNICATE_WITH_STUDENTS,
        Permission.ACCESS_RESEARCH_RESOURCES,
        Permission.SUPERVISE_STUDENTS,
        Permission.PARTICIPATE_IN_COMMITTEES,
        Permission.REVIEW_TENURE_CASES
    )),
    
    FULL_PROFESSOR("Full Professor", "Senior tenure-track faculty", 8, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_COURSES,
        Permission.MANAGE_ASSIGNED_COURSES,
        Permission.CREATE_COURSES,
        Permission.GRADE_ASSIGNMENTS,
        Permission.VIEW_STUDENT_GRADES,
        Permission.EDIT_COURSE_CONTENT,
        Permission.COMMUNICATE_WITH_STUDENTS,
        Permission.ACCESS_RESEARCH_RESOURCES,
        Permission.SUPERVISE_STUDENTS,
        Permission.PARTICIPATE_IN_COMMITTEES,
        Permission.REVIEW_TENURE_CASES,
        Permission.LEAD_RESEARCH_PROJECTS,
        Permission.MENTOR_JUNIOR_FACULTY
    )),
    
    // Administrative roles
    DEPARTMENT_CHAIR("Department Chair", "Head of academic department", 9, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_ALL_DEPARTMENT_COURSES,
        Permission.MANAGE_DEPARTMENT_COURSES,
        Permission.VIEW_DEPARTMENT_FACULTY,
        Permission.MANAGE_DEPARTMENT_FACULTY,
        Permission.VIEW_DEPARTMENT_STUDENTS,
        Permission.APPROVE_COURSE_CHANGES,
        Permission.MANAGE_DEPARTMENT_BUDGET,
        Permission.HIRE_FACULTY,
        Permission.EVALUATE_FACULTY
    )),
    
    DEAN("Dean", "Head of academic college", 10, Arrays.asList(
        Permission.VIEW_OWN_PROFILE,
        Permission.EDIT_OWN_PROFILE,
        Permission.VIEW_ALL_COLLEGE_DATA,
        Permission.MANAGE_COLLEGE_DEPARTMENTS,
        Permission.APPROVE_MAJOR_CHANGES,
        Permission.MANAGE_COLLEGE_BUDGET,
        Permission.HIRE_DEPARTMENT_CHAIRS,
        Permission.STRATEGIC_PLANNING,
        Permission.EXTERNAL_RELATIONS
    )),
    
    // Staff roles
    REGISTRAR("Registrar", "Student records and enrollment management", 6, Arrays.asList(
        Permission.VIEW_STUDENT_RECORDS,
        Permission.EDIT_STUDENT_RECORDS,
        Permission.MANAGE_ENROLLMENTS,
        Permission.GENERATE_TRANSCRIPTS,
        Permission.MANAGE_ACADEMIC_CALENDAR,
        Permission.VERIFY_DEGREES,
        Permission.TRANSFER_CREDITS
    )),
    
    ACADEMIC_ADVISOR("Academic Advisor", "Student academic guidance", 5, Arrays.asList(
        Permission.VIEW_STUDENT_RECORDS,
        Permission.VIEW_DEGREE_REQUIREMENTS,
        Permission.ADVISE_STUDENTS,
        Permission.APPROVE_COURSE_SELECTIONS,
        Permission.TRACK_STUDENT_PROGRESS,
        Permission.GENERATE_DEGREE_AUDITS
    )),
    
    FINANCIAL_AID_OFFICER("Financial Aid Officer", "Student financial assistance", 5, Arrays.asList(
        Permission.VIEW_STUDENT_FINANCIAL_DATA,
        Permission.PROCESS_FINANCIAL_AID,
        Permission.VERIFY_ELIGIBILITY,
        Permission.MANAGE_SCHOLARSHIPS,
        Permission.COMMUNICATE_WITH_STUDENTS
    )),
    
    IT_SUPPORT("IT Support", "Technical support and maintenance", 4, Arrays.asList(
        Permission.ACCESS_SYSTEM_LOGS,
        Permission.MANAGE_USER_ACCOUNTS,
        Permission.PERFORM_SYSTEM_MAINTENANCE,
        Permission.BACKUP_RESTORE_DATA,
        Permission.MONITOR_SYSTEM_PERFORMANCE
    )),
    
    // Super administrative roles
    SYSTEM_ADMINISTRATOR("System Administrator", "Full system access", 11, Arrays.asList(
        Permission.FULL_SYSTEM_ACCESS,
        Permission.MANAGE_ALL_USERS,
        Permission.SYSTEM_CONFIGURATION,
        Permission.SECURITY_MANAGEMENT,
        Permission.DATABASE_ADMINISTRATION,
        Permission.AUDIT_SYSTEM_ACTIVITY
    )),
    
    SUPER_ADMIN("Super Administrator", "Ultimate system control", 12, 
        Arrays.asList(Permission.values()));
    
    private final String displayName;
    private final String description;
    private final int hierarchyLevel;
    private final List<Permission> permissions;
    
    UserRole(String displayName, String description, int hierarchyLevel, List<Permission> permissions) {
        this.displayName = displayName;
        this.description = description;
        this.hierarchyLevel = hierarchyLevel;
        this.permissions = new ArrayList<>(permissions);
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getHierarchyLevel() {
        return hierarchyLevel;
    }
    
    public List<Permission> getPermissions() {
        return new ArrayList<>(permissions);
    }
    
    /**
     * Check if this role has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Check if this role has higher hierarchy than another role
     */
    public boolean isHigherThan(UserRole other) {
        return this.hierarchyLevel > other.hierarchyLevel;
    }
    
    /**
     * Check if this role has lower hierarchy than another role
     */
    public boolean isLowerThan(UserRole other) {
        return this.hierarchyLevel < other.hierarchyLevel;
    }
    
    /**
     * Check if this role is equal in hierarchy to another role
     */
    public boolean isEqualTo(UserRole other) {
        return this.hierarchyLevel == other.hierarchyLevel;
    }
    
    /**
     * Get all roles with hierarchy level equal or lower than this role
     */
    public List<UserRole> getSubordinateRoles() {
        List<UserRole> subordinates = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            if (role.hierarchyLevel <= this.hierarchyLevel && role != this) {
                subordinates.add(role);
            }
        }
        return subordinates;
    }
    
    /**
     * Get all roles with hierarchy level higher than this role
     */
    public List<UserRole> getSuperiorRoles() {
        List<UserRole> superiors = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            if (role.hierarchyLevel > this.hierarchyLevel) {
                superiors.add(role);
            }
        }
        return superiors;
    }
    
    /**
     * Get all student roles
     */
    public static List<UserRole> getStudentRoles() {
        return Arrays.asList(STUDENT, STUDENT_LEADER, GRADUATE_STUDENT);
    }
    
    /**
     * Get all faculty roles
     */
    public static List<UserRole> getFacultyRoles() {
        return Arrays.asList(TEACHING_ASSISTANT, ADJUNCT_PROFESSOR, ASSISTANT_PROFESSOR, 
                           ASSOCIATE_PROFESSOR, FULL_PROFESSOR);
    }
    
    /**
     * Get all administrative roles
     */
    public static List<UserRole> getAdministrativeRoles() {
        return Arrays.asList(DEPARTMENT_CHAIR, DEAN, REGISTRAR, ACADEMIC_ADVISOR, 
                           FINANCIAL_AID_OFFICER, IT_SUPPORT, SYSTEM_ADMINISTRATOR, SUPER_ADMIN);
    }
    
    /**
     * Get all staff roles
     */
    public static List<UserRole> getStaffRoles() {
        return Arrays.asList(REGISTRAR, ACADEMIC_ADVISOR, FINANCIAL_AID_OFFICER, IT_SUPPORT);
    }
    
    /**
     * Check if role is a student role
     */
    public boolean isStudentRole() {
        return getStudentRoles().contains(this);
    }
    
    /**
     * Check if role is a faculty role
     */
    public boolean isFacultyRole() {
        return getFacultyRoles().contains(this);
    }
    
    /**
     * Check if role is an administrative role
     */
    public boolean isAdministrativeRole() {
        return getAdministrativeRoles().contains(this);
    }
    
    /**
     * Check if role is a staff role
     */
    public boolean isStaffRole() {
        return getStaffRoles().contains(this);
    }
    
    /**
     * Get role category
     */
    public RoleCategory getCategory() {
        if (isStudentRole()) return RoleCategory.STUDENT;
        if (isFacultyRole()) return RoleCategory.FACULTY;
        if (isStaffRole()) return RoleCategory.STAFF;
        if (isAdministrativeRole()) return RoleCategory.ADMINISTRATIVE;
        return RoleCategory.OTHER;
    }
    
    /**
     * Find role by display name
     */
    public static Optional<UserRole> findByDisplayName(String displayName) {
        for (UserRole role : UserRole.values()) {
            if (role.displayName.equalsIgnoreCase(displayName)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get roles by hierarchy level
     */
    public static List<UserRole> getRolesByLevel(int level) {
        List<UserRole> roles = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            if (role.hierarchyLevel == level) {
                roles.add(role);
            }
        }
        return roles;
    }
    
    /**
     * Get maximum hierarchy level
     */
    public static int getMaxHierarchyLevel() {
        return Arrays.stream(UserRole.values())
                     .mapToInt(role -> role.hierarchyLevel)
                     .max()
                     .orElse(0);
    }
    
    /**
     * Get minimum hierarchy level
     */
    public static int getMinHierarchyLevel() {
        return Arrays.stream(UserRole.values())
                     .mapToInt(role -> role.hierarchyLevel)
                     .min()
                     .orElse(0);
    }
    
    /**
     * Check if user can perform action on target user based on role hierarchy
     */
    public boolean canManage(UserRole targetRole) {
        return this.hierarchyLevel > targetRole.hierarchyLevel;
    }
    
    /**
     * Get common permissions between two roles
     */
    public List<Permission> getCommonPermissions(UserRole otherRole) {
        List<Permission> common = new ArrayList<>(this.permissions);
        common.retainAll(otherRole.permissions);
        return common;
    }
    
    /**
     * Get unique permissions (permissions this role has but other doesn't)
     */
    public List<Permission> getUniquePermissions(UserRole otherRole) {
        List<Permission> unique = new ArrayList<>(this.permissions);
        unique.removeAll(otherRole.permissions);
        return unique;
    }
    
    @Override
    public String toString() {
        return String.format("%s (Level %d): %s", displayName, hierarchyLevel, description);
    }
    
    /**
     * Role category enumeration
     */
    public enum RoleCategory {
        STUDENT("Student", "Students enrolled in the university"),
        FACULTY("Faculty", "Teaching and research staff"),
        STAFF("Staff", "Administrative and support staff"),
        ADMINISTRATIVE("Administrative", "Administrative personnel"),
        OTHER("Other", "Other role types");
        
        private final String displayName;
        private final String description;
        
        RoleCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Permission enumeration
     */
    public enum Permission {
        // Student permissions
        VIEW_OWN_PROFILE("View own profile"),
        EDIT_OWN_PROFILE("Edit own profile"),
        VIEW_COURSES("View available courses"),
        ENROLL_IN_COURSES("Enroll in courses"),
        VIEW_OWN_GRADES("View own grades"),
        VIEW_SCHEDULE("View class schedule"),
        SUBMIT_ASSIGNMENTS("Submit assignments"),
        ORGANIZE_STUDENT_EVENTS("Organize student events"),
        ACCESS_STUDENT_RESOURCES("Access student resources"),
        ACCESS_RESEARCH_RESOURCES("Access research resources"),
        MENTOR_UNDERGRADUATES("Mentor undergraduate students"),
        
        // Faculty permissions
        VIEW_ASSIGNED_COURSES("View assigned courses"),
        MANAGE_ASSIGNED_COURSES("Manage assigned courses"),
        CREATE_COURSES("Create new courses"),
        GRADE_ASSIGNMENTS("Grade assignments"),
        VIEW_STUDENT_GRADES("View student grades"),
        EDIT_COURSE_CONTENT("Edit course content"),
        COMMUNICATE_WITH_STUDENTS("Communicate with students"),
        ASSIST_IN_TEACHING("Assist in teaching"),
        SUPERVISE_STUDENTS("Supervise students"),
        PARTICIPATE_IN_COMMITTEES("Participate in committees"),
        REVIEW_TENURE_CASES("Review tenure cases"),
        LEAD_RESEARCH_PROJECTS("Lead research projects"),
        MENTOR_JUNIOR_FACULTY("Mentor junior faculty"),
        
        // Administrative permissions
        VIEW_ALL_DEPARTMENT_COURSES("View all department courses"),
        MANAGE_DEPARTMENT_COURSES("Manage department courses"),
        VIEW_DEPARTMENT_FACULTY("View department faculty"),
        MANAGE_DEPARTMENT_FACULTY("Manage department faculty"),
        VIEW_DEPARTMENT_STUDENTS("View department students"),
        APPROVE_COURSE_CHANGES("Approve course changes"),
        MANAGE_DEPARTMENT_BUDGET("Manage department budget"),
        HIRE_FACULTY("Hire faculty"),
        EVALUATE_FACULTY("Evaluate faculty"),
        VIEW_ALL_COLLEGE_DATA("View all college data"),
        MANAGE_COLLEGE_DEPARTMENTS("Manage college departments"),
        APPROVE_MAJOR_CHANGES("Approve major changes"),
        MANAGE_COLLEGE_BUDGET("Manage college budget"),
        HIRE_DEPARTMENT_CHAIRS("Hire department chairs"),
        STRATEGIC_PLANNING("Strategic planning"),
        EXTERNAL_RELATIONS("External relations"),
        
        // Staff permissions
        VIEW_STUDENT_RECORDS("View student records"),
        EDIT_STUDENT_RECORDS("Edit student records"),
        MANAGE_ENROLLMENTS("Manage enrollments"),
        GENERATE_TRANSCRIPTS("Generate transcripts"),
        MANAGE_ACADEMIC_CALENDAR("Manage academic calendar"),
        VERIFY_DEGREES("Verify degrees"),
        TRANSFER_CREDITS("Transfer credits"),
        VIEW_DEGREE_REQUIREMENTS("View degree requirements"),
        ADVISE_STUDENTS("Advise students"),
        APPROVE_COURSE_SELECTIONS("Approve course selections"),
        TRACK_STUDENT_PROGRESS("Track student progress"),
        GENERATE_DEGREE_AUDITS("Generate degree audits"),
        VIEW_STUDENT_FINANCIAL_DATA("View student financial data"),
        PROCESS_FINANCIAL_AID("Process financial aid"),
        VERIFY_ELIGIBILITY("Verify eligibility"),
        MANAGE_SCHOLARSHIPS("Manage scholarships"),
        
        // System permissions
        ACCESS_SYSTEM_LOGS("Access system logs"),
        MANAGE_USER_ACCOUNTS("Manage user accounts"),
        PERFORM_SYSTEM_MAINTENANCE("Perform system maintenance"),
        BACKUP_RESTORE_DATA("Backup and restore data"),
        MONITOR_SYSTEM_PERFORMANCE("Monitor system performance"),
        FULL_SYSTEM_ACCESS("Full system access"),
        MANAGE_ALL_USERS("Manage all users"),
        SYSTEM_CONFIGURATION("System configuration"),
        SECURITY_MANAGEMENT("Security management"),
        DATABASE_ADMINISTRATION("Database administration"),
        AUDIT_SYSTEM_ACTIVITY("Audit system activity");
        
        private final String description;
        
        Permission(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
}