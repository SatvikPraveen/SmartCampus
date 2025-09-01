// File: src/main/java/services/AuthService.java
package services;

import models.User;
import models.Student;
import models.Professor;
import models.Admin;
import interfaces.Auditable;
import utils.ValidationUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuthService class providing authentication and authorization services.
 * This service manages user login, session management, and access control.
 * 
 * Key Java concepts demonstrated:
 * - Service layer pattern
 * - Interface implementation (Auditable)
 * - Concurrent collections for thread safety
 * - Optional for null-safe operations
 * - Enums for type safety
 * - Method overloading
 * - Static factory methods
 * - Security best practices
 */
public class AuthService implements Auditable {
    
    // Authentication result enumeration
    public enum AuthResult {
        SUCCESS("Authentication successful"),
        INVALID_CREDENTIALS("Invalid username or password"),
        ACCOUNT_LOCKED("Account is locked"),
        ACCOUNT_DISABLED("Account is disabled"),
        PASSWORD_EXPIRED("Password has expired"),
        TOO_MANY_ATTEMPTS("Too many failed login attempts"),
        SESSION_EXPIRED("Session has expired"),
        INSUFFICIENT_PRIVILEGES("Insufficient privileges"),
        USER_NOT_FOUND("User not found"),
        INVALID_TOKEN("Invalid authentication token");
        
        private final String message;
        
        AuthResult(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
    }
    
    // Session status enumeration
    public enum SessionStatus {
        ACTIVE("Active"),
        EXPIRED("Expired"),
        TERMINATED("Terminated"),
        INVALID("Invalid");
        
        private final String displayName;
        
        SessionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Permission levels enumeration
    public enum PermissionLevel {
        READ("Read"),
        WRITE("Write"),
        ADMIN("Admin"),
        SUPER_ADMIN("Super Admin");
        
        private final String displayName;
        
        PermissionLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Instance fields
    private final Map<String, User> users;
    private final Map<String, String> userCredentials; // userId -> hashedPassword
    private final Map<String, Session> activeSessions;
    private final Map<String, Integer> loginAttempts;
    private final Map<String, LocalDateTime> accountLockouts;
    private final List<AuditRecord> auditRecords;
    
    // Configuration
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MINUTES = 30;
    private static final long SESSION_TIMEOUT_MINUTES = 60;
    private static final long PASSWORD_EXPIRY_DAYS = 90;
    
    // Singleton instance
    private static volatile AuthService instance;
    
    /**
     * Private constructor for singleton pattern.
     */
    private AuthService() {
        this.users = new ConcurrentHashMap<>();
        this.userCredentials = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.loginAttempts = new ConcurrentHashMap<>();
        this.accountLockouts = new ConcurrentHashMap<>();
        this.auditRecords = new ArrayList<>();
        initializeDefaultAdmin();
    }
    
    /**
     * Get singleton instance of AuthService.
     * 
     * @return AuthService instance
     */
    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }
    
    // Authentication methods
    
    /**
     * Authenticate user with username and password.
     * 
     * @param username The username
     * @param password The password
     * @return AuthenticationResult containing result and session info
     */
    public AuthenticationResult authenticate(String username, String password) {
        // Validate input
        if (!ValidationUtil.isValidString(username) || !ValidationUtil.isValidString(password)) {
            logAuditEvent(AuditAction.LOGIN, AuditLevel.WARNING, username, null, 
                         "Authentication failed: Invalid input");
            return new AuthenticationResult(AuthResult.INVALID_CREDENTIALS, null, null);
        }
        
        // Check if account is locked
        if (isAccountLocked(username)) {
            logAuditEvent(AuditAction.LOGIN, AuditLevel.SECURITY, username, null, 
                         "Authentication failed: Account locked");
            return new AuthenticationResult(AuthResult.ACCOUNT_LOCKED, null, null);
        }
        
        // Find user
        Optional<User> userOpt = findUserByUsername(username);
        if (userOpt.isEmpty()) {
            incrementLoginAttempts(username);
            logAuditEvent(AuditAction.LOGIN, AuditLevel.WARNING, username, null, 
                         "Authentication failed: User not found");
            return new AuthenticationResult(AuthResult.USER_NOT_FOUND, null, null);
        }
        
        User user = userOpt.get();
        
        // Check if account is active
        if (!user.isActive()) {
            logAuditEvent(AuditAction.LOGIN, AuditLevel.WARNING, user.getUserId(), user.getUserId(), 
                         "Authentication failed: Account disabled");
            return new AuthenticationResult(AuthResult.ACCOUNT_DISABLED, null, user);
        }
        
        // Verify password
        if (!verifyPassword(user.getUserId(), password)) {
            incrementLoginAttempts(username);
            checkAndLockAccount(username);
            logAuditEvent(AuditAction.LOGIN, AuditLevel.SECURITY, user.getUserId(), user.getUserId(), 
                         "Authentication failed: Invalid password");
            return new AuthenticationResult(AuthResult.INVALID_CREDENTIALS, null, user);
        }
        
        // Check password expiry
        if (isPasswordExpired(user.getUserId())) {
            logAuditEvent(AuditAction.LOGIN, AuditLevel.WARNING, user.getUserId(), user.getUserId(), 
                         "Authentication failed: Password expired");
            return new AuthenticationResult(AuthResult.PASSWORD_EXPIRED, null, user);
        }
        
        // Create session
        Session session = createSession(user);
        
        // Reset login attempts
        resetLoginAttempts(username);
        
        // Log successful authentication
        logAuditEvent(AuditAction.LOGIN, AuditLevel.INFO, user.getUserId(), user.getUserId(), 
                     "Authentication successful");
        
        return new AuthenticationResult(AuthResult.SUCCESS, session, user);
    }
    
    /**
     * Authenticate user with session token.
     * 
     * @param sessionToken The session token
     * @return AuthenticationResult containing result and session info
     */
    public AuthenticationResult authenticateWithToken(String sessionToken) {
        if (!ValidationUtil.isValidString(sessionToken)) {
            logAuditEvent(AuditAction.ACCESS, AuditLevel.WARNING, null, null, 
                         "Token authentication failed: Invalid token format");
            return new AuthenticationResult(AuthResult.INVALID_TOKEN, null, null);
        }
        
        Session session = activeSessions.get(sessionToken);
        if (session == null) {
            logAuditEvent(AuditAction.ACCESS, AuditLevel.WARNING, null, null, 
                         "Token authentication failed: Session not found");
            return new AuthenticationResult(AuthResult.INVALID_TOKEN, null, null);
        }
        
        if (session.isExpired()) {
            activeSessions.remove(sessionToken);
            logAuditEvent(AuditAction.ACCESS, AuditLevel.INFO, session.getUserId(), session.getUserId(), 
                         "Token authentication failed: Session expired");
            return new AuthenticationResult(AuthResult.SESSION_EXPIRED, null, null);
        }
        
        // Update last access time
        session.updateLastAccess();
        
        Optional<User> userOpt = findUserById(session.getUserId());
        if (userOpt.isEmpty()) {
            activeSessions.remove(sessionToken);
            logAuditEvent(AuditAction.ACCESS, AuditLevel.ERROR, session.getUserId(), session.getUserId(), 
                         "Token authentication failed: User not found");
            return new AuthenticationResult(AuthResult.USER_NOT_FOUND, null, null);
        }
        
        return new AuthenticationResult(AuthResult.SUCCESS, session, userOpt.get());
    }
    
    /**
     * Logout user and invalidate session.
     * 
     * @param sessionToken The session token to invalidate
     * @return true if logout was successful, false otherwise
     */
    public boolean logout(String sessionToken) {
        Session session = activeSessions.remove(sessionToken);
        if (session != null) {
            session.invalidate();
            logAuditEvent(AuditAction.LOGOUT, AuditLevel.INFO, session.getUserId(), session.getUserId(), 
                         "User logged out");
            return true;
        }
        return false;
    }
    
    /**
     * Logout all sessions for a user.
     * 
     * @param userId The user ID
     * @return Number of sessions invalidated
     */
    public int logoutAllSessions(String userId) {
        int count = 0;
        List<String> tokensToRemove = new ArrayList<>();
        
        for (Map.Entry<String, Session> entry : activeSessions.entrySet()) {
            if (entry.getValue().getUserId().equals(userId)) {
                tokensToRemove.add(entry.getKey());
                entry.getValue().invalidate();
                count++;
            }
        }
        
        tokensToRemove.forEach(activeSessions::remove);
        
        if (count > 0) {
            logAuditEvent(AuditAction.LOGOUT, AuditLevel.INFO, userId, userId, 
                         "All sessions logged out (" + count + " sessions)");
        }
        
        return count;
    }
    
    // Authorization methods
    
    /**
     * Check if user has permission for an operation.
     * 
     * @param userId The user ID
     * @param resource The resource being accessed
     * @param permission The required permission level
     * @return true if user has permission, false otherwise
     */
    public boolean hasPermission(String userId, String resource, PermissionLevel permission) {
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // Super admin has all permissions
        if (user instanceof Admin) {
            Admin admin = (Admin) user;
            if (admin.getAdminLevel() == Admin.AdminLevel.SUPER_ADMIN) {
                return true;
            }
            
            // Check admin permissions
            return checkAdminPermissions(admin, resource, permission);
        }
        
        // Check role-based permissions
        return checkRolePermissions(user, resource, permission);
    }
    
    /**
     * Check if user can access a specific entity.
     * 
     * @param userId The user ID
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @param action The action being performed
     * @return true if access is allowed, false otherwise
     */
    public boolean canAccess(String userId, String entityType, String entityId, String action) {
        Optional<User> userOpt = findUserById(userId);
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            logAuditEvent(AuditAction.ACCESS, AuditLevel.SECURITY, entityId, userId, 
                         "Access denied: User not found or inactive");
            return false;
        }
        
        User user = userOpt.get();
        boolean hasAccess = false;
        
        // Check access based on user type and entity
        switch (entityType.toLowerCase()) {
            case "student":
                hasAccess = canAccessStudent(user, entityId, action);
                break;
            case "professor":
                hasAccess = canAccessProfessor(user, entityId, action);
                break;
            case "course":
                hasAccess = canAccessCourse(user, entityId, action);
                break;
            case "grade":
                hasAccess = canAccessGrade(user, entityId, action);
                break;
            default:
                hasAccess = user instanceof Admin; // Only admins can access other entities
        }
        
        // Log access attempt
        AuditLevel level = hasAccess ? AuditLevel.INFO : AuditLevel.SECURITY;
        String details = String.format("Access %s for %s %s action %s", 
                                     hasAccess ? "granted" : "denied", entityType, entityId, action);
        logAuditEvent(AuditAction.ACCESS, level, entityId, userId, details);
        
        return hasAccess;
    }
    
    // User management methods
    
    /**
     * Register a new user.
     * 
     * @param user The user to register
     * @param password The initial password
     * @return true if registration was successful, false otherwise
     */
    public boolean registerUser(User user, String password) {
        if (user == null || !ValidationUtil.isValidString(password)) {
            return false;
        }
        
        // Check if user already exists
        if (users.containsKey(user.getUserId()) || findUserByEmail(user.getEmail()).isPresent()) {
            logAuditEvent(AuditAction.CREATE, AuditLevel.WARNING, user.getUserId(), null, 
                         "User registration failed: User already exists");
            return false;
        }
        
        // Store user and credentials
        users.put(user.getUserId(), user);
        userCredentials.put(user.getUserId(), hashPassword(password));
        
        logAuditEvent(AuditAction.CREATE, AuditLevel.INFO, user.getUserId(), null, 
                     "User registered: " + user.getRole());
        
        return true;
    }
    
    /**
     * Change user password.
     * 
     * @param userId The user ID
     * @param oldPassword The current password
     * @param newPassword The new password
     * @return true if password change was successful, false otherwise
     */
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        if (!ValidationUtil.isValidString(oldPassword) || !ValidationUtil.isValidString(newPassword)) {
            return false;
        }
        
        // Verify old password
        if (!verifyPassword(userId, oldPassword)) {
            logAuditEvent(AuditAction.MODIFY, AuditLevel.SECURITY, userId, userId, 
                         "Password change failed: Invalid old password");
            return false;
        }
        
        // Update password
        userCredentials.put(userId, hashPassword(newPassword));
        
        logAuditEvent(AuditAction.MODIFY, AuditLevel.INFO, userId, userId, 
                     "Password changed successfully");
        
        return true;
    }
    
    /**
     * Reset user password (admin function).
     * 
     * @param adminUserId The admin user ID performing the reset
     * @param targetUserId The user whose password is being reset
     * @param newPassword The new password
     * @return true if reset was successful, false otherwise
     */
    public boolean resetPassword(String adminUserId, String targetUserId, String newPassword) {
        // Check admin permissions
        if (!hasPermission(adminUserId, "user_management", PermissionLevel.ADMIN)) {
            logAuditEvent(AuditAction.MODIFY, AuditLevel.SECURITY, targetUserId, adminUserId, 
                         "Password reset denied: Insufficient permissions");
            return false;
        }
        
        if (!ValidationUtil.isValidString(newPassword)) {
            return false;
        }
        
        // Update password
        userCredentials.put(targetUserId, hashPassword(newPassword));
        
        logAuditEvent(AuditAction.MODIFY, AuditLevel.ADMIN, targetUserId, adminUserId, 
                     "Password reset by admin");
        
        return true;
    }
    
    // Session management methods
    
    /**
     * Get active session for user.
     * 
     * @param sessionToken The session token
     * @return Optional containing session if found and valid
     */
    public Optional<Session> getSession(String sessionToken) {
        Session session = activeSessions.get(sessionToken);
        if (session != null && !session.isExpired()) {
            return Optional.of(session);
        }
        return Optional.empty();
    }
    
    /**
     * Get all active sessions for a user.
     * 
     * @param userId The user ID
     * @return List of active sessions
     */
    public List<Session> getActiveSessions(String userId) {
        return activeSessions.values().stream()
                .filter(session -> session.getUserId().equals(userId) && !session.isExpired())
                .toList();
    }
    
    /**
     * Clean up expired sessions.
     * 
     * @return Number of sessions cleaned up
     */
    public int cleanupExpiredSessions() {
        List<String> expiredTokens = activeSessions.entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .map(Map.Entry::getKey)
                .toList();
        
        expiredTokens.forEach(activeSessions::remove);
        
        if (!expiredTokens.isEmpty()) {
            logAuditEvent(AuditAction.SYSTEM, AuditLevel.INFO, null, "SYSTEM", 
                         "Cleaned up " + expiredTokens.size() + " expired sessions");
        }
        
        return expiredTokens.size();
    }
    
    // Helper methods
    
    /**
     * Initialize default admin user.
     */
    private void initializeDefaultAdmin() {
        Admin defaultAdmin = new Admin("ADMIN001", "System", "Administrator", 
                                     "admin@smartcampus.edu", "(555) 000-0000",
                                     "ADM001", Admin.AdminLevel.SUPER_ADMIN, 
                                     Admin.Department.IT_SERVICES, "System Administrator");
        
        registerUser(defaultAdmin, "admin123"); // In production, use secure password
        
        logAuditEvent(AuditAction.CREATE, AuditLevel.SYSTEM, defaultAdmin.getUserId(), "SYSTEM", 
                     "Default admin user created");
    }
    
    /**
     * Find user by username (email).
     */
    private Optional<User> findUserByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(username))
                .findFirst();
    }
    
    /**
     * Find user by ID.
     */
    private Optional<User> findUserById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }
    
    /**
     * Find user by email.
     */
    private Optional<User> findUserByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
    
    /**
     * Verify password for user.
     */
    private boolean verifyPassword(String userId, String password) {
        String storedHash = userCredentials.get(userId);
        return storedHash != null && storedHash.equals(hashPassword(password));
    }
    
    /**
     * Hash password (simplified - in production use proper hashing like BCrypt).
     */
    private String hashPassword(String password) {
        // Simplified hashing - in production use BCrypt or similar
        return "HASH_" + password.hashCode() + "_" + password.length();
    }
    
    /**
     * Check if password is expired.
     */
    private boolean isPasswordExpired(String userId) {
        // In a real implementation, store password creation dates
        return false; // Simplified for demo
    }
    
    /**
     * Check if account is locked.
     */
    private boolean isAccountLocked(String username) {
        LocalDateTime lockoutTime = accountLockouts.get(username);
        if (lockoutTime == null) {
            return false;
        }
        
        // Check if lockout has expired
        if (LocalDateTime.now().isAfter(lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES))) {
            accountLockouts.remove(username);
            loginAttempts.remove(username);
            return false;
        }
        
        return true;
    }
    
    /**
     * Increment login attempts for user.
     */
    private void incrementLoginAttempts(String username) {
        loginAttempts.merge(username, 1, Integer::sum);
    }
    
    /**
     * Reset login attempts for user.
     */
    private void resetLoginAttempts(String username) {
        loginAttempts.remove(username);
        accountLockouts.remove(username);
    }
    
    /**
     * Check and lock account if too many failed attempts.
     */
    private void checkAndLockAccount(String username) {
        int attempts = loginAttempts.getOrDefault(username, 0);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            accountLockouts.put(username, LocalDateTime.now());
            logAuditEvent(AuditAction.SYSTEM, AuditLevel.SECURITY, username, "SYSTEM", 
                         "Account locked due to too many failed login attempts");
        }
    }
    
    /**
     * Create new session for user.
     */
    private Session createSession(User user) {
        String sessionToken = generateSessionToken();
        Session session = new Session(sessionToken, user.getUserId(), user.getRole());
        activeSessions.put(sessionToken, session);
        return session;
    }
    
    /**
     * Generate unique session token.
     */
    private String generateSessionToken() {
        return "SES_" + System.currentTimeMillis() + "_" + System.nanoTime() % 100000;
    }
    
    /**
     * Check admin permissions.
     */
    private boolean checkAdminPermissions(Admin admin, String resource, PermissionLevel required) {
        // System admins have elevated permissions
        if (admin.getAdminLevel() == Admin.AdminLevel.SYSTEM_ADMIN || 
            admin.getAdminLevel() == Admin.AdminLevel.SUPER_ADMIN) {
            return true;
        }
        
        // Check specific permissions based on admin level and department
        switch (required) {
            case READ:
                return true; // All admins can read
            case WRITE:
                return admin.getAdminLevel() != Admin.AdminLevel.JUNIOR_ADMIN;
            case ADMIN:
                return admin.getAdminLevel() == Admin.AdminLevel.SENIOR_ADMIN ||
                       admin.getAdminLevel() == Admin.AdminLevel.SYSTEM_ADMIN ||
                       admin.getAdminLevel() == Admin.AdminLevel.SUPER_ADMIN;
            case SUPER_ADMIN:
                return admin.getAdminLevel() == Admin.AdminLevel.SUPER_ADMIN;
            default:
                return false;
        }
    }
    
    /**
     * Check role-based permissions.
     */
    private boolean checkRolePermissions(User user, String resource, PermissionLevel required) {
        // Students can only read their own data
        if (user instanceof Student) {
            return required == PermissionLevel.READ;
        }
        
        // Professors can read/write course-related data
        if (user instanceof Professor) {
            return required == PermissionLevel.READ || 
                   (required == PermissionLevel.WRITE && resource.contains("course"));
        }
        
        return false;
    }
    
    /**
     * Check student access permissions.
     */
    private boolean canAccessStudent(User user, String studentId, String action) {
        if (user instanceof Admin) {
            return true; // Admins can access all student data
        }
        
        if (user instanceof Professor) {
            // Professors can access students in their courses
            return true; // Simplified - would check course enrollments in real implementation
        }
        
        if (user instanceof Student) {
            // Students can only access their own data
            return user.getUserId().equals(studentId);
        }
        
        return false;
    }
    
    /**
     * Check professor access permissions.
     */
    private boolean canAccessProfessor(User user, String professorId, String action) {
        if (user instanceof Admin) {
            return true; // Admins can access all professor data
        }
        
        if (user instanceof Professor) {
            // Professors can access their own data and limited data of colleagues
            return user.getUserId().equals(professorId) || action.equals("read");
        }
        
        return false;
    }
    
    /**
     * Check course access permissions.
     */
    private boolean canAccessCourse(User user, String courseId, String action) {
        if (user instanceof Admin) {
            return true; // Admins can access all course data
        }
        
        if (user instanceof Professor) {
            // Professors can access courses they teach
            return true; // Simplified - would check course assignments in real implementation
        }
        
        if (user instanceof Student) {
            // Students can read courses they're enrolled in
            return action.equals("read"); // Simplified - would check enrollments in real implementation
        }
        
        return false;
    }
    
    /**
     * Check grade access permissions.
     */
    private boolean canAccessGrade(User user, String gradeId, String action) {
        if (user instanceof Admin) {
            return true; // Admins can access all grade data
        }
        
        if (user instanceof Professor) {
            // Professors can access grades for their courses
            return true; // Simplified - would check course assignments in real implementation
        }
        
        if (user instanceof Student) {
            // Students can only read their own grades
            return action.equals("read"); // Simplified - would check grade ownership in real implementation
        }
        
        return false;
    }
    
    // Auditable interface implementation
    
    @Override
    public AuditRecord logAuditEvent(AuditAction action, String entityId, String userId, String details) {
        return logAuditEvent(action, AuditLevel.INFO, entityId, userId, details);
    }
    
    @Override
    public AuditRecord logAuditEvent(AuditAction action, AuditLevel level, String entityId, String userId, String details) {
        return logAuditEvent(action, level, entityId, userId, details, Map.of());
    }
    
    @Override
    public AuditRecord logAuditEvent(AuditAction action, AuditLevel level, String entityId, String userId, 
                                   String details, Map<String, Object> metadata) {
        String auditId = "AUD_" + System.currentTimeMillis() + "_" + auditRecords.size();
        String userName = userId != null ? getUserName(userId) : "Unknown";
        
        AuditRecord record = new AuditRecord(auditId, action, level, entityId, "User", 
                                           userId, userName, details, metadata, null, null, null);
        
        auditRecords.add(record);
        return record;
    }
    
    @Override
    public List<AuditRecord> getAuditHistory(String entityId) {
        return auditRecords.stream()
                .filter(record -> entityId.equals(record.getEntityId()))
                .toList();
    }
    
    @Override
    public List<AuditRecord> getAuditHistory(String entityId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditRecords.stream()
                .filter(record -> entityId.equals(record.getEntityId()) &&
                               !record.getTimestamp().isBefore(startDate) &&
                               !record.getTimestamp().isAfter(endDate))
                .toList();
    }
    
    @Override
    public List<AuditRecord> getUserAuditHistory(String userId) {
        return auditRecords.stream()
                .filter(record -> userId.equals(record.getUserId()))
                .toList();
    }
    
    @Override
    public List<AuditRecord> getAuditHistoryByAction(AuditAction action) {
        return auditRecords.stream()
                .filter(record -> action.equals(record.getAction()))
                .toList();
    }
    
    @Override
    public List<AuditRecord> getAuditHistoryByLevel(AuditLevel level) {
        return auditRecords.stream()
                .filter(record -> level.equals(record.getLevel()))
                .toList();
    }
    
    @Override
    public List<AuditRecord> searchAuditLogs(Map<String, Object> criteria) {
        // Simplified search implementation
        return auditRecords.stream()
                .filter(record -> matchesCriteria(record, criteria))
                .toList();
    }
    
    @Override
    public List<AuditRecord> getRecentAuditEvents(int limit) {
        return auditRecords.stream()
                .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
                .limit(limit)
                .toList();
    }
    
    @Override
    public Map<String, Object> getAuditStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", auditRecords.size());
        stats.put("securityEvents", auditRecords.stream()
                .filter(AuditRecord::isSecurityRelated)
                .count());
        stats.put("errorEvents", auditRecords.stream()
                .filter(AuditRecord::isError)
                .count());
        stats.put("activeUsers", users.size());
        stats.put("activeSessions", activeSessions.size());
        stats.put("lockedAccounts", accountLockouts.size());
        return stats;
    }
    
    @Override
    public int archiveAuditRecords(LocalDateTime beforeDate) {
        // In a real implementation, move records to archive storage
        return 0; // Simplified for demo
    }
    
    @Override
    public int purgeAuditRecords(LocalDateTime beforeDate) {
        int initialSize = auditRecords.size();
        auditRecords.removeIf(record -> record.getTimestamp().isBefore(beforeDate));
        return initialSize - auditRecords.size();
    }
    
    @Override
    public boolean exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String format, String filePath) {
        // In a real implementation, export to file system
        return true; // Simplified for demo
    }
    
    /**
     * Get user name for audit logging.
     */
    private String getUserName(String userId) {
        return findUserById(userId)
                .map(User::getFullName)
                .orElse("Unknown User");
    }
    
    /**
     * Check if audit record matches search criteria.
     */
    private boolean matchesCriteria(AuditRecord record, Map<String, Object> criteria) {
        // Simplified criteria matching
        for (Map.Entry<String, Object> criterion : criteria.entrySet()) {
            String key = criterion.getKey();
            Object value = criterion.getValue();
            
            switch (key) {
                case "action":
                    if (!record.getAction().toString().equals(value.toString())) return false;
                    break;
                case "level":
                    if (!record.getLevel().toString().equals(value.toString())) return false;
                    break;
                case "userId":
                    if (!record.getUserId().equals(value.toString())) return false;
                    break;
                case "entityId":
                    if (!record.getEntityId().equals(value.toString())) return false;
                    break;
            }
        }
        return true;
    }
    
    // Inner classes
    
    /**
     * Authentication result class.
     */
    public static class AuthenticationResult {
        private final AuthResult result;
        private final Session session;
        private final User user;
        
        public AuthenticationResult(AuthResult result, Session session, User user) {
            this.result = result;
            this.session = session;
            this.user = user;
        }
        
        public AuthResult getResult() { return result; }
        public Session getSession() { return session; }
        public User getUser() { return user; }
        
        public boolean isSuccess() {
            return result == AuthResult.SUCCESS;
        }
        
        public String getMessage() {
            return result.getMessage();
        }
        
        @Override
        public String toString() {
            return String.format("AuthenticationResult{result=%s, hasSession=%s, user=%s}", 
                result, session != null, user != null ? user.getUserId() : "null");
        }
    }
    
    /**
     * Session class representing user sessions.
     */
    public static class Session {
        private final String sessionToken;
        private final String userId;
        private final String userRole;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccessAt;
        private boolean valid;
        
        public Session(String sessionToken, String userId, String userRole) {
            this.sessionToken = sessionToken;
            this.userId = userId;
            this.userRole = userRole;
            this.createdAt = LocalDateTime.now();
            this.lastAccessAt = LocalDateTime.now();
            this.valid = true;
        }
        
        public String getSessionToken() { return sessionToken; }
        public String getUserId() { return userId; }
        public String getUserRole() { return userRole; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessAt() { return lastAccessAt; }
        public boolean isValid() { return valid; }
        
        public void updateLastAccess() {
            this.lastAccessAt = LocalDateTime.now();
        }
        
        public void invalidate() {
            this.valid = false;
        }
        
        public boolean isExpired() {
            if (!valid) return true;
            
            LocalDateTime expiryTime = lastAccessAt.plusMinutes(SESSION_TIMEOUT_MINUTES);
            return LocalDateTime.now().isAfter(expiryTime);
        }
        
        public long getAgeMinutes() {
            return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        }
        
        public long getIdleMinutes() {
            return java.time.Duration.between(lastAccessAt, LocalDateTime.now()).toMinutes();
        }
        
        @Override
        public String toString() {
            return String.format("Session{token='%s', userId='%s', role='%s', created=%s, valid=%s}", 
                sessionToken, userId, userRole, createdAt, valid);
        }
    }
}