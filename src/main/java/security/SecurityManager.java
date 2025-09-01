// File location: src/main/java/security/SecurityManager.java
package security;

import models.User;
import enums.UserRole;
import exceptions.AuthenticationException;
import cache.CacheManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.security.SecureRandom;

/**
 * Central security management system for the campus management application
 * Handles authentication, authorization, session management, and security policies
 */
public class SecurityManager {
    
    private static final SecurityManager INSTANCE = new SecurityManager();
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final RoleBasedAccess roleBasedAccess;
    private final CacheManager cacheManager;
    private final ReentrantReadWriteLock lock;
    
    // Security configurations
    private final SecurityConfig config;
    
    // Active sessions tracking
    private final Map<String, SessionInfo> activeSessions;
    private final Map<String, List<SessionInfo>> userSessions;
    
    // Failed login attempts tracking
    private final Map<String, AttemptInfo> loginAttempts;
    private final Set<String> blockedIPs;
    
    // Password history for reuse prevention
    private final Map<String, List<String>> passwordHistory;
    
    private SecurityManager() {
        this.passwordEncoder = new PasswordEncoder();
        this.tokenManager = new TokenManager();
        this.roleBasedAccess = new RoleBasedAccess();
        this.cacheManager = CacheManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.config = new SecurityConfig();
        
        this.activeSessions = new ConcurrentHashMap<>();
        this.userSessions = new ConcurrentHashMap<>();
        this.loginAttempts = new ConcurrentHashMap<>();
        this.blockedIPs = ConcurrentHashMap.newKeySet();
        this.passwordHistory = new ConcurrentHashMap<>();
        
        initializeCaches();
    }
    
    public static SecurityManager getInstance() {
        return INSTANCE;
    }
    
    private void initializeCaches() {
        // Initialize security-related caches
        cacheManager.createCache("sessions");
        cacheManager.createCache("tokens");
        cacheManager.createCache("permissions");
        cacheManager.createCache("blocked_ips");
    }
    
    // ==================== AUTHENTICATION ====================
    
    /**
     * Authenticates user with username and password
     */
    public AuthenticationResult authenticate(String username, String password, String ipAddress, String userAgent) {
        if (username == null || password == null) {
            throw AuthenticationException.invalidCredentials(username, ipAddress);
        }
        
        // Check if IP is blocked
        if (isIPBlocked(ipAddress)) {
            throw AuthenticationException.ipAddressBlocked(ipAddress, "Too many failed attempts");
        }
        
        // Check login attempts
        if (isAccountTemporarilyLocked(username)) {
            AttemptInfo attempts = loginAttempts.get(username);
            throw AuthenticationException.accountLocked(username, attempts.lockoutUntil);
        }
        
        try {
            // Validate credentials (this would typically query a database)
            User user = validateCredentials(username, password);
            
            if (user == null) {
                recordFailedAttempt(username, ipAddress);
                throw AuthenticationException.invalidCredentials(username, ipAddress);
            }
            
            // Check account status
            validateAccountStatus(user);
            
            // Check password expiration
            checkPasswordExpiration(user);
            
            // Check concurrent sessions
            checkConcurrentSessions(username);
            
            // Clear failed attempts on successful login
            clearFailedAttempts(username);
            
            // Create session
            SessionInfo session = createSession(user, ipAddress, userAgent);
            
            // Generate token
            String token = tokenManager.generateToken(user, session.getSessionId());
            
            return new AuthenticationResult(user, session, token, true);
            
        } catch (AuthenticationException e) {
            recordFailedAttempt(username, ipAddress);
            throw e;
        }
    }
    
    /**
     * Authenticates using token
     */
    public AuthenticationResult authenticateWithToken(String token, String ipAddress) {
        if (token == null) {
            throw AuthenticationException.invalidToken("Bearer", "Token is null");
        }
        
        try {
            // Validate token
            TokenManager.TokenInfo tokenInfo = tokenManager.validateToken(token);
            
            if (tokenInfo == null) {
                throw AuthenticationException.invalidToken("Bearer", "Invalid or expired token");
            }
            
            // Get session
            SessionInfo session = getSession(tokenInfo.getSessionId());
            if (session == null || session.isExpired()) {
                throw AuthenticationException.sessionExpired(tokenInfo.getSessionId(), tokenInfo.getUsername());
            }
            
            // Validate IP consistency (optional based on configuration)
            if (config.isStrictIPValidation() && !session.getIpAddress().equals(ipAddress)) {
                throw AuthenticationException.ipAddressBlocked(ipAddress, "IP address mismatch");
            }
            
            // Update session activity
            session.updateActivity();
            
            // Get user (typically from cache or database)
            User user = getUserByUsername(tokenInfo.getUsername());
            if (user == null) {
                throw AuthenticationException.accountNotFound(tokenInfo.getUsername());
            }
            
            return new AuthenticationResult(user, session, token, false);
            
        } catch (Exception e) {
            if (e instanceof AuthenticationException) {
                throw e;
            }
            throw AuthenticationException.builder(AuthenticationException.ErrorCode.TOKEN_INVALID)
                .message("Token validation failed: " + e.getMessage())
                .cause(e)
                .build();
        }
    }
    
    /**
     * Multi-factor authentication
     */
    public boolean verifyMFA(String username, String mfaCode, String method) {
        // Implementation would integrate with MFA providers (TOTP, SMS, etc.)
        // For now, this is a placeholder
        
        if (mfaCode == null || mfaCode.length() != 6) {
            return false;
        }
        
        // Simulate MFA verification
        return switch (method.toLowerCase()) {
            case "totp" -> verifyTOTP(username, mfaCode);
            case "sms" -> verifySMS(username, mfaCode);
            case "email" -> verifyEmail(username, mfaCode);
            default -> false;
        };
    }
    
    // ==================== SESSION MANAGEMENT ====================
    
    /**
     * Creates new session
     */
    private SessionInfo createSession(User user, String ipAddress, String userAgent) {
        String sessionId = generateSessionId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(config.getSessionTimeoutMinutes(), ChronoUnit.MINUTES);
        
        SessionInfo session = new SessionInfo(sessionId, user.getUsername(), user.getRole(),
                                            ipAddress, userAgent, now, expiresAt);
        
        lock.writeLock().lock();
        try {
            activeSessions.put(sessionId, session);
            userSessions.computeIfAbsent(user.getUsername(), k -> new ArrayList<>()).add(session);
        } finally {
            lock.writeLock().unlock();
        }
        
        // Cache session
        cacheManager.put("sessions", sessionId, session);
        
        return session;
    }
    
    /**
     * Gets session by ID
     */
    public SessionInfo getSession(String sessionId) {
        if (sessionId == null) return null;
        
        // Try cache first
        SessionInfo session = cacheManager.get("sessions", sessionId);
        if (session != null && !session.isExpired()) {
            return session;
        }
        
        // Get from active sessions
        lock.readLock().lock();
        try {
            session = activeSessions.get(sessionId);
            if (session != null && session.isExpired()) {
                // Remove expired session
                removeSession(sessionId);
                return null;
            }
            return session;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Removes session (logout)
     */
    public void removeSession(String sessionId) {
        if (sessionId == null) return;
        
        lock.writeLock().lock();
        try {
            SessionInfo session = activeSessions.remove(sessionId);
            if (session != null) {
                List<SessionInfo> sessions = userSessions.get(session.getUsername());
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        userSessions.remove(session.getUsername());
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        
        // Remove from cache
        cacheManager.remove("sessions", sessionId);
        
        // Invalidate associated tokens
        tokenManager.invalidateTokensBySession(sessionId);
    }
    
    /**
     * Gets all sessions for user
     */
    public List<SessionInfo> getUserSessions(String username) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(userSessions.getOrDefault(username, new ArrayList<>()));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Removes all sessions for user
     */
    public void removeAllUserSessions(String username) {
        lock.writeLock().lock();
        try {
            List<SessionInfo> sessions = userSessions.remove(username);
            if (sessions != null) {
                for (SessionInfo session : sessions) {
                    activeSessions.remove(session.getSessionId());
                    cacheManager.remove("sessions", session.getSessionId());
                    tokenManager.invalidateTokensBySession(session.getSessionId());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ==================== AUTHORIZATION ====================
    
    /**
     * Checks if user has permission for resource and action
     */
    public boolean hasPermission(String username, String resource, String action) {
        User user = getUserByUsername(username);
        if (user == null) return false;
        
        return roleBasedAccess.hasPermission(user.getRole(), resource, action);
    }
    
    /**
     * Checks if user has required role
     */
    public boolean hasRole(String username, UserRole requiredRole) {
        User user = getUserByUsername(username);
        if (user == null) return false;
        
        return roleBasedAccess.hasRole(user.getRole(), requiredRole);
    }
    
    /**
     * Validates access to resource
     */
    public void validateAccess(String username, String resource, String action) {
        if (!hasPermission(username, resource, action)) {
            User user = getUserByUsername(username);
            UserRole requiredRole = roleBasedAccess.getRequiredRole(resource, action);
            
            if (requiredRole != null) {
                throw AuthenticationException.insufficientPrivileges(username, resource, action, requiredRole);
            } else {
                Set<String> requiredPermissions = roleBasedAccess.getRequiredPermissions(resource, action);
                throw AuthenticationException.accessDenied(username, resource, requiredPermissions);
            }
        }
    }
    
    // ==================== PASSWORD MANAGEMENT ====================
    
    /**
     * Changes user password
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        // Validate old password
        User user = validateCredentials(username, oldPassword);
        if (user == null) {
            throw AuthenticationException.invalidCredentials(username, null);
        }
        
        // Validate new password strength
        if (!isPasswordStrong(newPassword)) {
            throw AuthenticationException.builder(AuthenticationException.ErrorCode.WEAK_PASSWORD)
                .username(username)
                .message("Password does not meet security requirements")
                .build();
        }
        
        // Check password history
        if (isPasswordReused(username, newPassword)) {
            throw AuthenticationException.builder(AuthenticationException.ErrorCode.PASSWORD_REUSE)
                .username(username)
                .message("Password has been used recently")
                .build();
        }
        
        // Hash new password
        String hashedPassword = passwordEncoder.encode(newPassword);
        
        // Update password (this would update the database)
        updateUserPassword(username, hashedPassword);
        
        // Update password history
        updatePasswordHistory(username, hashedPassword);
        
        // Invalidate all user sessions to force re-login
        removeAllUserSessions(username);
    }
    
    /**
     * Resets password (admin function)
     */
    public String resetPassword(String username, String adminUsername) {
        // Validate admin permissions
        validateAccess(adminUsername, "user_management", "reset_password");
        
        // Generate temporary password
        String tempPassword = generateTemporaryPassword();
        String hashedPassword = passwordEncoder.encode(tempPassword);
        
        // Update password
        updateUserPassword(username, hashedPassword);
        
        // Mark password as expired to force change on next login
        markPasswordExpired(username);
        
        // Invalidate all user sessions
        removeAllUserSessions(username);
        
        return tempPassword;
    }
    
    // ==================== SECURITY VALIDATIONS ====================
    
    private User validateCredentials(String username, String password) {
        // This would typically query the database
        // For now, return null to simulate not found
        return null;
    }
    
    private void validateAccountStatus(User user) {
        if (user.isDisabled()) {
            throw AuthenticationException.accountDisabled(user.getUsername(), "Account disabled by administrator");
        }
        
        if (user.isExpired()) {
            throw AuthenticationException.builder(AuthenticationException.ErrorCode.ACCOUNT_EXPIRED)
                .username(user.getUsername())
                .message("Account has expired")
                .build();
        }
    }
    
    private void checkPasswordExpiration(User user) {
        if (isPasswordExpired(user.getUsername())) {
            throw AuthenticationException.passwordExpired(user.getUsername(), getPasswordExpirationDate(user.getUsername()));
        }
    }
    
    private void checkConcurrentSessions(String username) {
        List<SessionInfo> sessions = getUserSessions(username);
        if (sessions.size() >= config.getMaxConcurrentSessions()) {
            throw AuthenticationException.concurrentSessionLimit(username, config.getMaxConcurrentSessions());
        }
    }
    
    // ==================== ATTEMPT TRACKING ====================
    
    private void recordFailedAttempt(String username, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        
        // Track by username
        lock.writeLock().lock();
        try {
            AttemptInfo attempts = loginAttempts.computeIfAbsent(username, 
                k -> new AttemptInfo(username, ipAddress));
            attempts.recordFailedAttempt();
            
            if (attempts.getFailedCount() >= config.getMaxLoginAttempts()) {
                attempts.lockAccount(config.getLockoutDurationMinutes());
            }
        } finally {
            lock.writeLock().unlock();
        }
        
        // Track by IP
        if (ipAddress != null) {
            // Count failed attempts from this IP across all users
            long ipAttempts = loginAttempts.values().stream()
                .filter(attempt -> ipAddress.equals(attempt.getIpAddress()))
                .filter(attempt -> attempt.getLastAttempt().isAfter(now.minusMinutes(config.getIpBlockWindowMinutes())))
                .mapToLong(AttemptInfo::getFailedCount)
                .sum();
                
            if (ipAttempts >= config.getMaxIPAttempts()) {
                blockedIPs.add(ipAddress);
                cacheManager.put("blocked_ips", ipAddress, now.plus(config.getIpBlockDurationMinutes(), ChronoUnit.MINUTES));
            }
        }
    }
    
    private void clearFailedAttempts(String username) {
        lock.writeLock().lock();
        try {
            loginAttempts.remove(username);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private boolean isAccountTemporarilyLocked(String username) {
        AttemptInfo attempts = loginAttempts.get(username);
        return attempts != null && attempts.isLocked();
    }
    
    private boolean isIPBlocked(String ipAddress) {
        if (ipAddress == null) return false;
        
        if (blockedIPs.contains(ipAddress)) {
            // Check if block has expired
            LocalDateTime blockExpiry = cacheManager.get("blocked_ips", ipAddress);
            if (blockExpiry != null && LocalDateTime.now().isAfter(blockExpiry)) {
                blockedIPs.remove(ipAddress);
                cacheManager.remove("blocked_ips", ipAddress);
                return false;
            }
            return true;
        }
        return false;
    }
    
    // ==================== HELPER METHODS ====================
    
    private String generateSessionId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder(12);
        
        for (int i = 0; i < 12; i++) {
            password.append(charset.charAt(random.nextInt(charset.length())));
        }
        
        return password.toString();
    }
    
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < config.getMinPasswordLength()) {
            return false;
        }
        
        return password.matches(".*[A-Z].*") && // Upper case
               password.matches(".*[a-z].*") && // Lower case
               password.matches(".*\\d.*") && // Digit
               password.matches(".*[!@#$%^&*()_+={}\\[\\]:;\"'<>,.?/|\\\\].*"); // Special char
    }
    
    private boolean isPasswordReused(String username, String newPassword) {
        List<String> history = passwordHistory.get(username);
        if (history == null) return false;
        
        return history.stream()
                     .anyMatch(hashedPassword -> passwordEncoder.matches(newPassword, hashedPassword));
    }
    
    private void updatePasswordHistory(String username, String hashedPassword) {
        List<String> history = passwordHistory.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(0, hashedPassword);
        
        // Keep only the configured number of previous passwords
        while (history.size() > config.getPasswordHistorySize()) {
            history.remove(history.size() - 1);
        }
    }
    
    // Placeholder methods that would interact with database
    private User getUserByUsername(String username) { return null; }
    private void updateUserPassword(String username, String hashedPassword) { }
    private boolean isPasswordExpired(String username) { return false; }
    private LocalDateTime getPasswordExpirationDate(String username) { return LocalDateTime.now().plusDays(90); }
    private void markPasswordExpired(String username) { }
    private boolean verifyTOTP(String username, String code) { return true; }
    private boolean verifySMS(String username, String code) { return true; }
    private boolean verifyEmail(String username, String code) { return true; }
    
    // ==================== RESULT CLASSES ====================
    
    public static class AuthenticationResult {
        private final User user;
        private final SessionInfo session;
        private final String token;
        private final boolean isNewLogin;
        
        public AuthenticationResult(User user, SessionInfo session, String token, boolean isNewLogin) {
            this.user = user;
            this.session = session;
            this.token = token;
            this.isNewLogin = isNewLogin;
        }
        
        public User getUser() { return user; }
        public SessionInfo getSession() { return session; }
        public String getToken() { return token; }
        public boolean isNewLogin() { return isNewLogin; }
    }
    
    // ==================== CONFIGURATION ====================
    
    private static class SecurityConfig {
        private final int maxLoginAttempts = 5;
        private final int lockoutDurationMinutes = 30;
        private final int maxIPAttempts = 20;
        private final int ipBlockWindowMinutes = 15;
        private final int ipBlockDurationMinutes = 60;
        private final int sessionTimeoutMinutes = 60;
        private final int maxConcurrentSessions = 5;
        private final int minPasswordLength = 8;
        private final int passwordHistorySize = 5;
        private final boolean strictIPValidation = false;
        
        public int getMaxLoginAttempts() { return maxLoginAttempts; }
        public int getLockoutDurationMinutes() { return lockoutDurationMinutes; }
        public int getMaxIPAttempts() { return maxIPAttempts; }
        public int getIpBlockWindowMinutes() { return ipBlockWindowMinutes; }
        public int getIpBlockDurationMinutes() { return ipBlockDurationMinutes; }
        public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
        public int getMaxConcurrentSessions() { return maxConcurrentSessions; }
        public int getMinPasswordLength() { return minPasswordLength; }
        public int getPasswordHistorySize() { return passwordHistorySize; }
        public boolean isStrictIPValidation() { return strictIPValidation; }
    }
    
    // ==================== SESSION INFO CLASS ====================
    
    public static class SessionInfo {
        private final String sessionId;
        private final String username;
        private final UserRole userRole;
        private final String ipAddress;
        private final String userAgent;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private LocalDateTime lastActivity;
        
        public SessionInfo(String sessionId, String username, UserRole userRole, 
                          String ipAddress, String userAgent, LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.userRole = userRole;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.lastActivity = createdAt;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        public void updateActivity() {
            this.lastActivity = LocalDateTime.now();
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public UserRole getUserRole() { return userRole; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }
    
    // ==================== ATTEMPT INFO CLASS ====================
    
    private static class AttemptInfo {
        private final String username;
        private final String ipAddress;
        private int failedCount;
        private LocalDateTime lastAttempt;
        private LocalDateTime lockoutUntil;
        
        public AttemptInfo(String username, String ipAddress) {
            this.username = username;
            this.ipAddress = ipAddress;
            this.failedCount = 0;
            this.lastAttempt = LocalDateTime.now();
        }
        
        public void recordFailedAttempt() {
            this.failedCount++;
            this.lastAttempt = LocalDateTime.now();
        }
        
        public void lockAccount(int durationMinutes) {
            this.lockoutUntil = LocalDateTime.now().plus(durationMinutes, ChronoUnit.MINUTES);
        }
        
        public boolean isLocked() {
            return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
        }
        
        public void clearAttempts() {
            this.failedCount = 0;
            this.lockoutUntil = null;
        }
        
        // Getters
        public String getUsername() { return username; }
        public String getIpAddress() { return ipAddress; }
        public int getFailedCount() { return failedCount; }
        public LocalDateTime getLastAttempt() { return lastAttempt; }
        public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    }
}