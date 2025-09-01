// File location: src/main/java/security/RoleBasedAccess.java
package security;

import enums.UserRole;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Role-Based Access Control (RBAC) system for the campus management application
 * Manages permissions and role hierarchies for secure resource access
 */
public class RoleBasedAccess {
    
    // Permission definitions
    private final Map<String, Set<String>> rolePermissions;
    private final Map<String, ResourcePermission> resourcePermissions;
    private final Map<UserRole, Set<UserRole>> roleHierarchy;
    
    // Resource categories
    public static final String STUDENT_MANAGEMENT = "student_management";
    public static final String PROFESSOR_MANAGEMENT = "professor_management";
    public static final String COURSE_MANAGEMENT = "course_management";
    public static final String ENROLLMENT_MANAGEMENT = "enrollment_management";
    public static final String GRADE_MANAGEMENT = "grade_management";
    public static final String DEPARTMENT_MANAGEMENT = "department_management";
    public static final String USER_MANAGEMENT = "user_management";
    public static final String SYSTEM_ADMINISTRATION = "system_administration";
    public static final String REPORTS = "reports";
    public static final String NOTIFICATIONS = "notifications";
    
    // Common actions
    public static final String CREATE = "create";
    public static final String READ = "read";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String EXECUTE = "execute";
    public static final String APPROVE = "approve";
    public static final String REJECT = "reject";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";
    
    public RoleBasedAccess() {
        this.rolePermissions = new ConcurrentHashMap<>();
        this.resourcePermissions = new ConcurrentHashMap<>();
        this.roleHierarchy = new ConcurrentHashMap<>();
        
        initializeRoleHierarchy();
        initializePermissions();
    }
    
    // ==================== ROLE HIERARCHY SETUP ====================
    
    private void initializeRoleHierarchy() {
        // Define role hierarchy (higher roles inherit lower role permissions)
        roleHierarchy.put(UserRole.SUPER_ADMIN, Set.of(UserRole.ADMIN, UserRole.PROFESSOR, UserRole.STUDENT));
        roleHierarchy.put(UserRole.ADMIN, Set.of(UserRole.PROFESSOR, UserRole.STUDENT));
        roleHierarchy.put(UserRole.PROFESSOR, Set.of(UserRole.STUDENT));
        roleHierarchy.put(UserRole.STUDENT, Set.of()); // No inherited roles
    }
    
    // ==================== PERMISSION INITIALIZATION ====================
    
    private void initializePermissions() {
        // Initialize permissions for each role
        initializeStudentPermissions();
        initializeProfessorPermissions();
        initializeAdminPermissions();
        initializeSuperAdminPermissions();
        
        // Initialize resource-specific permissions
        initializeResourcePermissions();
    }
    
    private void initializeStudentPermissions() {
        Set<String> studentPermissions = new HashSet<>();
        
        // Profile management
        studentPermissions.add(permissionKey("profile", READ));
        studentPermissions.add(permissionKey("profile", UPDATE));
        
        // Course browsing and enrollment
        studentPermissions.add(permissionKey(COURSE_MANAGEMENT, READ));
        studentPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, CREATE));
        studentPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, READ));
        studentPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, DELETE)); // Drop course
        
        // Grade viewing
        studentPermissions.add(permissionKey(GRADE_MANAGEMENT, READ));
        
        // Basic reports
        studentPermissions.add(permissionKey(REPORTS, READ));
        
        rolePermissions.put(UserRole.STUDENT.name(), studentPermissions);
    }
    
    private void initializeProfessorPermissions() {
        Set<String> professorPermissions = new HashSet<>();
        
        // Professor profile management
        professorPermissions.add(permissionKey("professor_profile", READ));
        professorPermissions.add(permissionKey("professor_profile", UPDATE));
        
        // Course management
        professorPermissions.add(permissionKey(COURSE_MANAGEMENT, CREATE));
        professorPermissions.add(permissionKey(COURSE_MANAGEMENT, READ));
        professorPermissions.add(permissionKey(COURSE_MANAGEMENT, UPDATE));
        
        // Student management (within their courses)
        professorPermissions.add(permissionKey(STUDENT_MANAGEMENT, READ));
        
        // Enrollment management (for their courses)
        professorPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, READ));
        professorPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, APPROVE));
        professorPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, REJECT));
        
        // Grade management
        professorPermissions.add(permissionKey(GRADE_MANAGEMENT, CREATE));
        professorPermissions.add(permissionKey(GRADE_MANAGEMENT, READ));
        professorPermissions.add(permissionKey(GRADE_MANAGEMENT, UPDATE));
        
        // Reports
        professorPermissions.add(permissionKey(REPORTS, READ));
        professorPermissions.add(permissionKey(REPORTS, CREATE));
        professorPermissions.add(permissionKey(REPORTS, EXPORT));
        
        // Notifications
        professorPermissions.add(permissionKey(NOTIFICATIONS, CREATE));
        professorPermissions.add(permissionKey(NOTIFICATIONS, READ));
        
        rolePermissions.put(UserRole.PROFESSOR.name(), professorPermissions);
    }
    
    private void initializeAdminPermissions() {
        Set<String> adminPermissions = new HashSet<>();
        
        // Full student management
        adminPermissions.add(permissionKey(STUDENT_MANAGEMENT, CREATE));
        adminPermissions.add(permissionKey(STUDENT_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(STUDENT_MANAGEMENT, UPDATE));
        adminPermissions.add(permissionKey(STUDENT_MANAGEMENT, DELETE));
        
        // Full professor management
        adminPermissions.add(permissionKey(PROFESSOR_MANAGEMENT, CREATE));
        adminPermissions.add(permissionKey(PROFESSOR_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(PROFESSOR_MANAGEMENT, UPDATE));
        adminPermissions.add(permissionKey(PROFESSOR_MANAGEMENT, DELETE));
        
        // Full course management
        adminPermissions.add(permissionKey(COURSE_MANAGEMENT, CREATE));
        adminPermissions.add(permissionKey(COURSE_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(COURSE_MANAGEMENT, UPDATE));
        adminPermissions.add(permissionKey(COURSE_MANAGEMENT, DELETE));
        
        // Department management
        adminPermissions.add(permissionKey(DEPARTMENT_MANAGEMENT, CREATE));
        adminPermissions.add(permissionKey(DEPARTMENT_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(DEPARTMENT_MANAGEMENT, UPDATE));
        adminPermissions.add(permissionKey(DEPARTMENT_MANAGEMENT, DELETE));
        
        // Full enrollment management
        adminPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, CREATE));
        adminPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, UPDATE));
        adminPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, DELETE));
        adminPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, APPROVE));
        adminPermissions.add(permissionKey(ENROLLMENT_MANAGEMENT, REJECT));
        
        // Grade management oversight
        adminPermissions.add(permissionKey(GRADE_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(GRADE_MANAGEMENT, UPDATE));
        adminPermissions.add(permissionKey(GRADE_MANAGEMENT, APPROVE));
        
        // Full reporting
        adminPermissions.add(permissionKey(REPORTS, CREATE));
        adminPermissions.add(permissionKey(REPORTS, READ));
        adminPermissions.add(permissionKey(REPORTS, UPDATE));
        adminPermissions.add(permissionKey(REPORTS, DELETE));
        adminPermissions.add(permissionKey(REPORTS, EXPORT));
        adminPermissions.add(permissionKey(REPORTS, IMPORT));
        
        // User management (limited)
        adminPermissions.add(permissionKey(USER_MANAGEMENT, READ));
        adminPermissions.add(permissionKey(USER_MANAGEMENT, UPDATE));
        
        // Notification management
        adminPermissions.add(permissionKey(NOTIFICATIONS, CREATE));
        adminPermissions.add(permissionKey(NOTIFICATIONS, READ));
        adminPermissions.add(permissionKey(NOTIFICATIONS, UPDATE));
        adminPermissions.add(permissionKey(NOTIFICATIONS, DELETE));
        
        rolePermissions.put(UserRole.ADMIN.name(), adminPermissions);
    }
    
    private void initializeSuperAdminPermissions() {
        Set<String> superAdminPermissions = new HashSet<>();
        
        // Full system administration
        superAdminPermissions.add(permissionKey(SYSTEM_ADMINISTRATION, CREATE));
        superAdminPermissions.add(permissionKey(SYSTEM_ADMINISTRATION, READ));
        superAdminPermissions.add(permissionKey(SYSTEM_ADMINISTRATION, UPDATE));
        superAdminPermissions.add(permissionKey(SYSTEM_ADMINISTRATION, DELETE));
        superAdminPermissions.add(permissionKey(SYSTEM_ADMINISTRATION, EXECUTE));
        
        // Full user management
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, CREATE));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, READ));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, UPDATE));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, DELETE));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, "reset_password"));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, "disable_account"));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, "enable_account"));
        superAdminPermissions.add(permissionKey(USER_MANAGEMENT, "change_role"));
        
        rolePermissions.put(UserRole.SUPER_ADMIN.name(), superAdminPermissions);
    }
    
    private void initializeResourcePermissions() {
        // Student Management
        resourcePermissions.put(STUDENT_MANAGEMENT, new ResourcePermission(
            STUDENT_MANAGEMENT,
            "Student management operations",
            Set.of(CREATE, READ, UPDATE, DELETE),
            UserRole.ADMIN
        ));
        
        // Professor Management
        resourcePermissions.put(PROFESSOR_MANAGEMENT, new ResourcePermission(
            PROFESSOR_MANAGEMENT,
            "Professor management operations",
            Set.of(CREATE, READ, UPDATE, DELETE),
            UserRole.ADMIN
        ));
        
        // Course Management
        resourcePermissions.put(COURSE_MANAGEMENT, new ResourcePermission(
            COURSE_MANAGEMENT,
            "Course management operations",
            Set.of(CREATE, READ, UPDATE, DELETE),
            UserRole.PROFESSOR
        ));
        
        // Grade Management
        resourcePermissions.put(GRADE_MANAGEMENT, new ResourcePermission(
            GRADE_MANAGEMENT,
            "Grade management operations",
            Set.of(CREATE, READ, UPDATE, APPROVE),
            UserRole.PROFESSOR
        ));
        
        // System Administration
        resourcePermissions.put(SYSTEM_ADMINISTRATION, new ResourcePermission(
            SYSTEM_ADMINISTRATION,
            "System administration operations",
            Set.of(CREATE, READ, UPDATE, DELETE, EXECUTE),
            UserRole.SUPER_ADMIN
        ));
    }
    
    // ==================== PERMISSION CHECKING ====================
    
    /**
     * Checks if a role has permission for a resource and action
     */
    public boolean hasPermission(UserRole role, String resource, String action) {
        if (role == null || resource == null || action == null) {
            return false;
        }
        
        String permission = permissionKey(resource, action);
        
        // Check direct permissions
        if (hasDirectPermission(role, permission)) {
            return true;
        }
        
        // Check inherited permissions from role hierarchy
        return hasInheritedPermission(role, permission);
    }
    
    /**
     * Checks if a role has any permission for a resource
     */
    public boolean hasAnyPermission(UserRole role, String resource) {
        if (role == null || resource == null) {
            return false;
        }
        
        Set<String> permissions = getAllPermissions(role);
        return permissions.stream().anyMatch(perm -> perm.startsWith(resource + ":"));
    }
    
    /**
     * Checks if one role can perform action on behalf of another role
     */
    public boolean canActOnBehalf(UserRole actorRole, UserRole targetRole, String resource, String action) {
        // Check if actor has the required permission
        if (!hasPermission(actorRole, resource, action)) {
            return false;
        }
        
        // Check role hierarchy - can only act on behalf of lower roles
        Set<UserRole> subordinateRoles = roleHierarchy.getOrDefault(actorRole, new HashSet<>());
        return subordinateRoles.contains(targetRole);
    }
    
    /**
     * Checks role hierarchy relationship
     */
    public boolean hasRole(UserRole userRole, UserRole requiredRole) {
        if (userRole == null || requiredRole == null) {
            return false;
        }
        
        // Exact match
        if (userRole == requiredRole) {
            return true;
        }
        
        // Check if user role is higher in hierarchy
        Set<UserRole> subordinateRoles = roleHierarchy.getOrDefault(userRole, new HashSet<>());
        return subordinateRoles.contains(requiredRole);
    }
    
    // ==================== PERMISSION QUERIES ====================
    
    /**
     * Gets all permissions for a role (including inherited)
     */
    public Set<String> getAllPermissions(UserRole role) {
        if (role == null) {
            return new HashSet<>();
        }
        
        Set<String> allPermissions = new HashSet<>();
        
        // Add direct permissions
        Set<String> directPermissions = rolePermissions.getOrDefault(role.name(), new HashSet<>());
        allPermissions.addAll(directPermissions);
        
        // Add inherited permissions
        Set<UserRole> inheritedRoles = roleHierarchy.getOrDefault(role, new HashSet<>());
        for (UserRole inheritedRole : inheritedRoles) {
            Set<String> inheritedPermissions = rolePermissions.getOrDefault(inheritedRole.name(), new HashSet<>());
            allPermissions.addAll(inheritedPermissions);
        }
        
        return allPermissions;
    }
    
    /**
     * Gets permissions for a specific resource
     */
    public Set<String> getResourcePermissions(UserRole role, String resource) {
        Set<String> allPermissions = getAllPermissions(role);
        return allPermissions.stream()
                           .filter(perm -> perm.startsWith(resource + ":"))
                           .map(perm -> perm.substring(resource.length() + 1))
                           .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Gets required role for resource and action
     */
    public UserRole getRequiredRole(String resource, String action) {
        ResourcePermission resourcePerm = resourcePermissions.get(resource);
        if (resourcePerm != null && resourcePerm.getActions().contains(action)) {
            return resourcePerm.getMinimumRole();
        }
        return null;
    }
    
    /**
     * Gets required permissions for resource and action
     */
    public Set<String> getRequiredPermissions(String resource, String action) {
        return Set.of(permissionKey(resource, action));
    }
    
    // ==================== ROLE MANAGEMENT ====================
    
    /**
     * Adds permission to role
     */
    public void addPermission(UserRole role, String resource, String action) {
        String permission = permissionKey(resource, action);
        rolePermissions.computeIfAbsent(role.name(), k -> ConcurrentHashMap.newKeySet()).add(permission);
    }
    
    /**
     * Removes permission from role
     */
    public void removePermission(UserRole role, String resource, String action) {
        String permission = permissionKey(resource, action);
        Set<String> permissions = rolePermissions.get(role.name());
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Adds role to hierarchy
     */
    public void addRoleHierarchy(UserRole parentRole, UserRole childRole) {
        roleHierarchy.computeIfAbsent(parentRole, k -> ConcurrentHashMap.newKeySet()).add(childRole);
    }
    
    /**
     * Removes role from hierarchy
     */
    public void removeRoleHierarchy(UserRole parentRole, UserRole childRole) {
        Set<UserRole> children = roleHierarchy.get(parentRole);
        if (children != null) {
            children.remove(childRole);
        }
    }
    
    // ==================== CONTEXT-AWARE PERMISSIONS ====================
    
    /**
     * Checks permission with context (e.g., professor can only grade their own courses)
     */
    public boolean hasPermissionWithContext(UserRole role, String resource, String action, 
                                          Map<String, Object> context) {
        // First check if role has basic permission
        if (!hasPermission(role, resource, action)) {
            return false;
        }
        
        // Apply context-specific rules
        return applyContextualRules(role, resource, action, context);
    }
    
    /**
     * Checks if user can access specific entity
     */
    public boolean canAccessEntity(UserRole role, String entityType, String entityId, 
                                 String userId, String action) {
        Map<String, Object> context = new HashMap<>();
        context.put("entityType", entityType);
        context.put("entityId", entityId);
        context.put("userId", userId);
        
        return hasPermissionWithContext(role, entityType, action, context);
    }
    
    // ==================== PERMISSION VALIDATION ====================
    
    /**
     * Validates if a permission request is valid
     */
    public PermissionValidationResult validatePermission(UserRole role, String resource, 
                                                       String action, Map<String, Object> context) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check if role exists
        if (role == null) {
            violations.add("Role cannot be null");
            return new PermissionValidationResult(false, violations, warnings);
        }
        
        // Check if resource exists
        if (resource == null || resource.trim().isEmpty()) {
            violations.add("Resource cannot be null or empty");
        }
        
        // Check if action exists
        if (action == null || action.trim().isEmpty()) {
            violations.add("Action cannot be null or empty");
        }
        
        if (!violations.isEmpty()) {
            return new PermissionValidationResult(false, violations, warnings);
        }
        
        // Check if resource is defined
        ResourcePermission resourcePerm = resourcePermissions.get(resource);
        if (resourcePerm == null) {
            warnings.add("Resource '" + resource + "' is not defined in the system");
        } else if (!resourcePerm.getActions().contains(action)) {
            warnings.add("Action '" + action + "' is not defined for resource '" + resource + "'");
        }
        
        // Check permission
        boolean hasPermission = hasPermissionWithContext(role, resource, action, context);
        
        if (!hasPermission) {
            violations.add("Role '" + role + "' does not have permission for '" + resource + ":" + action + "'");
        }
        
        return new PermissionValidationResult(hasPermission && violations.isEmpty(), violations, warnings);
    }
    
    // ==================== REPORTING AND ANALYSIS ====================
    
    /**
     * Gets security report for role
     */
    public SecurityReport getRoleSecurityReport(UserRole role) {
        Set<String> permissions = getAllPermissions(role);
        Set<UserRole> inheritedRoles = roleHierarchy.getOrDefault(role, new HashSet<>());
        
        Map<String, Set<String>> permissionsByResource = new HashMap<>();
        for (String permission : permissions) {
            String[] parts = permission.split(":", 2);
            if (parts.length == 2) {
                permissionsByResource.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
            }
        }
        
        return new SecurityReport(role, permissions, inheritedRoles, permissionsByResource);
    }
    
    /**
     * Gets all roles that can perform action on resource
     */
    public Set<UserRole> getRolesWithPermission(String resource, String action) {
        Set<UserRole> rolesWithPermission = new HashSet<>();
        
        for (UserRole role : UserRole.values()) {
            if (hasPermission(role, resource, action)) {
                rolesWithPermission.add(role);
            }
        }
        
        return rolesWithPermission;
    }
    
    /**
     * Gets permission matrix for all roles and resources
     */
    public Map<UserRole, Map<String, Set<String>>> getPermissionMatrix() {
        Map<UserRole, Map<String, Set<String>>> matrix = new HashMap<>();
        
        for (UserRole role : UserRole.values()) {
            matrix.put(role, getResourcePermissionsMap(role));
        }
        
        return matrix;
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private boolean hasDirectPermission(UserRole role, String permission) {
        Set<String> permissions = rolePermissions.getOrDefault(role.name(), new HashSet<>());
        return permissions.contains(permission);
    }
    
    private boolean hasInheritedPermission(UserRole role, String permission) {
        Set<UserRole> inheritedRoles = roleHierarchy.getOrDefault(role, new HashSet<>());
        
        for (UserRole inheritedRole : inheritedRoles) {
            if (hasDirectPermission(inheritedRole, permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String permissionKey(String resource, String action) {
        return resource + ":" + action;
    }
    
    private boolean applyContextualRules(UserRole role, String resource, String action, 
                                       Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return true; // No contextual restrictions
        }
        
        // Apply role-specific contextual rules
        return switch (role) {
            case PROFESSOR -> applyProfessorContextualRules(resource, action, context);
            case STUDENT -> applyStudentContextualRules(resource, action, context);
            case ADMIN -> applyAdminContextualRules(resource, action, context);
            case SUPER_ADMIN -> true; // Super admin bypasses contextual restrictions
        };
    }
    
    private boolean applyProfessorContextualRules(String resource, String action, 
                                                Map<String, Object> context) {
        // Professors can only manage courses they teach
        if (COURSE_MANAGEMENT.equals(resource) || GRADE_MANAGEMENT.equals(resource)) {
            String courseId = (String) context.get("courseId");
            String professorId = (String) context.get("userId");
            
            if (courseId != null && professorId != null) {
                // In a real system, this would check if professor teaches the course
                return isCourseTaughtByProfessor(courseId, professorId);
            }
        }
        
        return true;
    }
    
    private boolean applyStudentContextualRules(String resource, String action, 
                                              Map<String, Object> context) {
        // Students can only access their own data
        if ("profile".equals(resource) || GRADE_MANAGEMENT.equals(resource)) {
            String requestingUserId = (String) context.get("requestingUserId");
            String targetUserId = (String) context.get("targetUserId");
            
            if (requestingUserId != null && targetUserId != null) {
                return requestingUserId.equals(targetUserId);
            }
        }
        
        return true;
    }
    
    private boolean applyAdminContextualRules(String resource, String action, 
                                            Map<String, Object> context) {
        // Admins have broad access but may have some restrictions
        if (USER_MANAGEMENT.equals(resource) && "change_role".equals(action)) {
            UserRole targetRole = (UserRole) context.get("targetRole");
            // Admins cannot promote users to SUPER_ADMIN
            return targetRole != UserRole.SUPER_ADMIN;
        }
        
        return true;
    }
    
    private Map<String, Set<String>> getResourcePermissionsMap(UserRole role) {
        Set<String> allPermissions = getAllPermissions(role);
        Map<String, Set<String>> resourceMap = new HashMap<>();
        
        for (String permission : allPermissions) {
            String[] parts = permission.split(":", 2);
            if (parts.length == 2) {
                resourceMap.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
            }
        }
        
        return resourceMap;
    }
    
    // Placeholder method - would integrate with course service
    private boolean isCourseTaughtByProfessor(String courseId, String professorId) {
        // In real implementation, would check database
        return true;
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Resource permission definition
     */
    public static class ResourcePermission {
        private final String resource;
        private final String description;
        private final Set<String> actions;
        private final UserRole minimumRole;
        
        public ResourcePermission(String resource, String description, 
                                Set<String> actions, UserRole minimumRole) {
            this.resource = resource;
            this.description = description;
            this.actions = new HashSet<>(actions);
            this.minimumRole = minimumRole;
        }
        
        public String getResource() { return resource; }
        public String getDescription() { return description; }
        public Set<String> getActions() { return new HashSet<>(actions); }
        public UserRole getMinimumRole() { return minimumRole; }
        
        @Override
        public String toString() {
            return String.format("ResourcePermission{resource='%s', minimumRole=%s, actions=%s}", 
                               resource, minimumRole, actions);
        }
    }
    
    /**
     * Permission validation result
     */
    public static class PermissionValidationResult {
        private final boolean valid;
        private final List<String> violations;
        private final List<String> warnings;
        
        public PermissionValidationResult(boolean valid, List<String> violations, List<String> warnings) {
            this.valid = valid;
            this.violations = new ArrayList<>(violations);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isValid() { return valid; }
        public List<String> getViolations() { return new ArrayList<>(violations); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public boolean hasViolations() { return !violations.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("PermissionValidation{valid=%s, violations=%d, warnings=%d}", 
                               valid, violations.size(), warnings.size());
        }
    }
    
    /**
     * Security report for role analysis
     */
    public static class SecurityReport {
        private final UserRole role;
        private final Set<String> permissions;
        private final Set<UserRole> inheritedRoles;
        private final Map<String, Set<String>> permissionsByResource;
        
        public SecurityReport(UserRole role, Set<String> permissions, 
                            Set<UserRole> inheritedRoles, Map<String, Set<String>> permissionsByResource) {
            this.role = role;
            this.permissions = new HashSet<>(permissions);
            this.inheritedRoles = new HashSet<>(inheritedRoles);
            this.permissionsByResource = new HashMap<>(permissionsByResource);
        }
        
        public UserRole getRole() { return role; }
        public Set<String> getPermissions() { return new HashSet<>(permissions); }
        public Set<UserRole> getInheritedRoles() { return new HashSet<>(inheritedRoles); }
        public Map<String, Set<String>> getPermissionsByResource() { 
            return new HashMap<>(permissionsByResource); 
        }
        
        public int getTotalPermissions() { return permissions.size(); }
        public int getResourceCount() { return permissionsByResource.size(); }
        
        @Override
        public String toString() {
            return String.format("SecurityReport{role=%s, permissions=%d, resources=%d, inherited=%s}", 
                               role, permissions.size(), permissionsByResource.size(), inheritedRoles);
        }
    }
}