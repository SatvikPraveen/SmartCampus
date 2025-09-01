// File location: src/main/java/exceptions/AuthenticationException.java
package exceptions;

import enums.UserRole;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Exception class for authentication and authorization errors in the campus management system
 * Provides detailed error information for security-related operations
 */
public class AuthenticationException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    // Error codes for different authentication/authorization issues
    public enum ErrorCode {
        INVALID_CREDENTIALS("AUTH_001", "Invalid username or password"),
        ACCOUNT_LOCKED("AUTH_002", "Account is temporarily locked"),
        ACCOUNT_DISABLED("AUTH_003", "Account has been disabled"),
        ACCOUNT_EXPIRED("AUTH_004", "Account has expired"),
        PASSWORD_EXPIRED("AUTH_005", "Password has expired"),
        SESSION_EXPIRED("AUTH_006", "Session has expired"),
        SESSION_INVALID("AUTH_007", "Invalid or corrupted session"),
        TOKEN_EXPIRED("AUTH_008", "Authentication token has expired"),
        TOKEN_INVALID("AUTH_009", "Invalid authentication token"),
        TOKEN_MALFORMED("AUTH_010", "Malformed authentication token"),
        INSUFFICIENT_PRIVILEGES("AUTH_011", "Insufficient privileges for this operation"),
        ACCESS_DENIED("AUTH_012", "Access denied to requested resource"),
        ROLE_NOT_AUTHORIZED("AUTH_013", "User role not authorized for this action"),
        MULTI_FACTOR_REQUIRED("AUTH_014", "Multi-factor authentication required"),
        CAPTCHA_REQUIRED("AUTH_015", "CAPTCHA verification required"),
        IP_ADDRESS_BLOCKED("AUTH_016", "IP address is blocked"),
        TOO_MANY_ATTEMPTS("AUTH_017", "Too many failed authentication attempts"),
        CONCURRENT_SESSION_LIMIT("AUTH_018", "Maximum concurrent sessions exceeded"),
        WEAK_PASSWORD("AUTH_019", "Password does not meet security requirements"),
        PASSWORD_REUSE("AUTH_020", "Password has been used recently"),
        ACCOUNT_NOT_FOUND("AUTH_021", "Account not found"),
        EMAIL_NOT_VERIFIED("AUTH_022", "Email address not verified"),
        PHONE_NOT_VERIFIED("AUTH_023", "Phone number not verified"),
        SECURITY_QUESTION_FAILED("AUTH_024", "Security question answer incorrect"),
        BIOMETRIC_FAILED("AUTH_025", "Biometric authentication failed"),
        DEVICE_NOT_TRUSTED("AUTH_026", "Device is not trusted"),
        LOCATION_NOT_ALLOWED("AUTH_027", "Login from this location is not allowed"),
        TIME_RESTRICTION("AUTH_028", "Access outside allowed time window"),
        MAINTENANCE_MODE("AUTH_029", "System is in maintenance mode"),
        FORCE_PASSWORD_CHANGE("AUTH_030", "Password change required"),
        ACCOUNT_SETUP_INCOMPLETE("AUTH_031", "Account setup is incomplete"),
        TERMS_NOT_ACCEPTED("AUTH_032", "Terms and conditions not accepted"),
        PRIVACY_POLICY_NOT_ACCEPTED("AUTH_033", "Privacy policy not accepted"),
        ENCRYPTION_ERROR("AUTH_034", "Authentication data encryption error"),
        DECRYPTION_ERROR("AUTH_035", "Authentication data decryption error"),
        SIGNATURE_VERIFICATION_FAILED("AUTH_036", "Digital signature verification failed"),
        CERTIFICATE_EXPIRED("AUTH_037", "Security certificate expired"),
        CERTIFICATE_INVALID("AUTH_038", "Invalid security certificate"),
        SSO_ERROR("AUTH_039", "Single Sign-On authentication error"),
        LDAP_ERROR("AUTH_040", "LDAP authentication error"),
        OAUTH_ERROR("AUTH_041", "OAuth authentication error"),
        SAML_ERROR("AUTH_042", "SAML authentication error"),
        FEDERATION_ERROR("AUTH_043", "Identity federation error"),
        UNKNOWN_ERROR("AUTH_999", "Unknown authentication error");
        
        private final String code;
        private final String description;
        
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // Exception properties
    private final ErrorCode errorCode;
    private final String username;
    private final String ipAddress;
    private final String userAgent;
    private final LocalDateTime timestamp;
    private final Map<String, Object> context;
    private final UserRole requiredRole;
    private final Set<String> requiredPermissions;
    private final String sessionId;
    private final String resource;
    private final String action;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new AuthenticationException with error code and message
     */
    public AuthenticationException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDescription());
        this.errorCode = errorCode;
        this.username = null;
        this.ipAddress = null;
        this.userAgent = null;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.requiredRole = null;
        this.requiredPermissions = new HashSet<>();
        this.sessionId = null;
        this.resource = null;
        this.action = null;
    }
    
    /**
     * Creates a new AuthenticationException with error code, message, and cause
     */
    public AuthenticationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getDescription(), cause);
        this.errorCode = errorCode;
        this.username = null;
        this.ipAddress = null;
        this.userAgent = null;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.requiredRole = null;
        this.requiredPermissions = new HashSet<>();
        this.sessionId = null;
        this.resource = null;
        this.action = null;
    }
    
    /**
     * Creates a new AuthenticationException with detailed authentication context
     */
    public AuthenticationException(ErrorCode errorCode, String username, String ipAddress, 
                                 String userAgent, String message) {
        super(buildDetailedMessage(errorCode, username, ipAddress, message));
        this.errorCode = errorCode;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.requiredRole = null;
        this.requiredPermissions = new HashSet<>();
        this.sessionId = null;
        this.resource = null;
        this.action = null;
    }
    
    /**
     * Creates a new AuthenticationException with full context
     */
    public AuthenticationException(ErrorCode errorCode, String username, String ipAddress, 
                                 String userAgent, String sessionId, String resource, 
                                 String action, UserRole requiredRole, 
                                 Set<String> requiredPermissions, String message, Throwable cause) {
        super(buildDetailedMessage(errorCode, username, ipAddress, message), cause);
        this.errorCode = errorCode;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.requiredRole = requiredRole;
        this.requiredPermissions = requiredPermissions != null ? new HashSet<>(requiredPermissions) : new HashSet<>();
        this.sessionId = sessionId;
        this.resource = resource;
        this.action = action;
    }
    
    // ==================== BUILDER PATTERN ====================
    
    /**
     * Builder for creating AuthenticationException with fluent interface
     */
    public static class Builder {
        private ErrorCode errorCode;
        private String message;
        private Throwable cause;
        private String username;
        private String ipAddress;
        private String userAgent;
        private String sessionId;
        private String resource;
        private String action;
        private UserRole requiredRole;
        private Set<String> requiredPermissions = new HashSet<>();
        private final Map<String, Object> context = new HashMap<>();
        
        public Builder(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public Builder requiredRole(UserRole requiredRole) {
            this.requiredRole = requiredRole;
            return this;
        }
        
        public Builder requiredPermission(String permission) {
            this.requiredPermissions.add(permission);
            return this;
        }
        
        public Builder requiredPermissions(Set<String> permissions) {
            this.requiredPermissions.addAll(permissions);
            return this;
        }
        
        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
        
        public Builder addContext(Map<String, Object> context) {
            this.context.putAll(context);
            return this;
        }
        
        public AuthenticationException build() {
            AuthenticationException exception = new AuthenticationException(
                errorCode, username, ipAddress, userAgent, sessionId, resource, 
                action, requiredRole, requiredPermissions, message, cause
            );
            exception.context.putAll(this.context);
            return exception;
        }
    }
    
    /**
     * Creates a new builder with the specified error code
     */
    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode);
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Creates exception for invalid credentials
     */
    public static AuthenticationException invalidCredentials(String username, String ipAddress) {
        return builder(ErrorCode.INVALID_CREDENTIALS)
            .username(username)
            .ipAddress(ipAddress)
            .message("Invalid username or password for user: " + username)
            .build();
    }
    
    /**
     * Creates exception for locked account
     */
    public static AuthenticationException accountLocked(String username, LocalDateTime lockoutUntil) {
        return builder(ErrorCode.ACCOUNT_LOCKED)
            .username(username)
            .addContext("lockoutUntil", lockoutUntil)
            .message("Account " + username + " is locked until " + lockoutUntil)
            .build();
    }
    
    /**
     * Creates exception for disabled account
     */
    public static AuthenticationException accountDisabled(String username, String reason) {
        return builder(ErrorCode.ACCOUNT_DISABLED)
            .username(username)
            .addContext("reason", reason)
            .message("Account " + username + " has been disabled: " + reason)
            .build();
    }
    
    /**
     * Creates exception for expired session
     */
    public static AuthenticationException sessionExpired(String sessionId, String username) {
        return builder(ErrorCode.SESSION_EXPIRED)
            .username(username)
            .sessionId(sessionId)
            .message("Session expired for user: " + username)
            .build();
    }
    
    /**
     * Creates exception for invalid token
     */
    public static AuthenticationException invalidToken(String tokenType, String reason) {
        return builder(ErrorCode.TOKEN_INVALID)
            .addContext("tokenType", tokenType)
            .addContext("reason", reason)
            .message("Invalid " + tokenType + " token: " + reason)
            .build();
    }
    
    /**
     * Creates exception for insufficient privileges
     */
    public static AuthenticationException insufficientPrivileges(String username, String resource, 
                                                               String action, UserRole requiredRole) {
        return builder(ErrorCode.INSUFFICIENT_PRIVILEGES)
            .username(username)
            .resource(resource)
            .action(action)
            .requiredRole(requiredRole)
            .message("User " + username + " lacks " + requiredRole + " role to " + action + " " + resource)
            .build();
    }
    
    /**
     * Creates exception for access denied
     */
    public static AuthenticationException accessDenied(String username, String resource, 
                                                     Set<String> requiredPermissions) {
        return builder(ErrorCode.ACCESS_DENIED)
            .username(username)
            .resource(resource)
            .requiredPermissions(requiredPermissions)
            .message("Access denied to " + resource + " for user " + username)
            .build();
    }
    
    /**
     * Creates exception for too many attempts
     */
    public static AuthenticationException tooManyAttempts(String username, String ipAddress, 
                                                        int attemptCount, int maxAttempts) {
        return builder(ErrorCode.TOO_MANY_ATTEMPTS)
            .username(username)
            .ipAddress(ipAddress)
            .addContext("attemptCount", attemptCount)
            .addContext("maxAttempts", maxAttempts)
            .message("Too many failed attempts (" + attemptCount + "/" + maxAttempts + ") for " + username)
            .build();
    }
    
    /**
     * Creates exception for multi-factor authentication required
     */
    public static AuthenticationException multiFactorRequired(String username, String method) {
        return builder(ErrorCode.MULTI_FACTOR_REQUIRED)
            .username(username)
            .addContext("method", method)
            .message("Multi-factor authentication required for " + username + " using " + method)
            .build();
    }
    
    /**
     * Creates exception for blocked IP address
     */
    public static AuthenticationException ipAddressBlocked(String ipAddress, String reason) {
        return builder(ErrorCode.IP_ADDRESS_BLOCKED)
            .ipAddress(ipAddress)
            .addContext("reason", reason)
            .message("IP address " + ipAddress + " is blocked: " + reason)
            .build();
    }
    
    /**
     * Creates exception for password expired
     */
    public static AuthenticationException passwordExpired(String username, LocalDateTime expiredDate) {
        return builder(ErrorCode.PASSWORD_EXPIRED)
            .username(username)
            .addContext("expiredDate", expiredDate)
            .message("Password for " + username + " expired on " + expiredDate)
            .build();
    }
    
    /**
     * Creates exception for concurrent session limit
     */
    public static AuthenticationException concurrentSessionLimit(String username, int maxSessions) {
        return builder(ErrorCode.CONCURRENT_SESSION_LIMIT)
            .username(username)
            .addContext("maxSessions", maxSessions)
            .message("Maximum concurrent sessions (" + maxSessions + ") exceeded for " + username)
            .build();
    }
    
    /**
     * Creates exception for OAuth errors
     */
    public static AuthenticationException oauthError(String provider, String error, String errorDescription) {
        return builder(ErrorCode.OAUTH_ERROR)
            .addContext("provider", provider)
            .addContext("error", error)
            .addContext("errorDescription", errorDescription)
            .message("OAuth error from " + provider + ": " + error)
            .build();
    }
    
    // ==================== GETTERS ====================
    
    public ErrorCode getErrorCode() { return errorCode; }
    public String getUsername() { return username; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getContext() { return new HashMap<>(context); }
    public UserRole getRequiredRole() { return requiredRole; }
    public Set<String> getRequiredPermissions() { return new HashSet<>(requiredPermissions); }
    public String getSessionId() { return sessionId; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    
    /**
     * Gets specific context data by key
     */
    public Object getContext(String key) {
        return context.get(key);
    }
    
    /**
     * Gets context data as specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key, Class<T> type) {
        Object value = context.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if this exception is retryable based on error code
     */
    public boolean isRetryable() {
        return switch (errorCode) {
            case SESSION_EXPIRED, TOKEN_EXPIRED, CAPTCHA_REQUIRED, 
                 MULTI_FACTOR_REQUIRED, FORCE_PASSWORD_CHANGE,
                 TERMS_NOT_ACCEPTED, PRIVACY_POLICY_NOT_ACCEPTED -> true;
            default -> false;
        };
    }
    
    /**
     * Checks if this is a security-critical error
     */
    public boolean isSecurityCritical() {
        return switch (errorCode) {
            case TOO_MANY_ATTEMPTS, IP_ADDRESS_BLOCKED, ACCOUNT_LOCKED,
                 CONCURRENT_SESSION_LIMIT, BIOMETRIC_FAILED,
                 SIGNATURE_VERIFICATION_FAILED, CERTIFICATE_EXPIRED,
                 CERTIFICATE_INVALID -> true;
            default -> false;
        };
    }
    
    /**
     * Checks if this error requires user action
     */
    public boolean requiresUserAction() {
        return switch (errorCode) {
            case PASSWORD_EXPIRED, WEAK_PASSWORD, EMAIL_NOT_VERIFIED,
                 PHONE_NOT_VERIFIED, FORCE_PASSWORD_CHANGE,
                 ACCOUNT_SETUP_INCOMPLETE, TERMS_NOT_ACCEPTED,
                 PRIVACY_POLICY_NOT_ACCEPTED -> true;
            default -> false;
        };
    }
    
    /**
     * Gets severity level of the error
     */
    public String getSeverity() {
        return switch (errorCode) {
            case ACCOUNT_DISABLED, ACCOUNT_EXPIRED, IP_ADDRESS_BLOCKED,
                 CERTIFICATE_EXPIRED, CERTIFICATE_INVALID -> "CRITICAL";
            case TOO_MANY_ATTEMPTS, ACCOUNT_LOCKED, CONCURRENT_SESSION_LIMIT,
                 BIOMETRIC_FAILED, SIGNATURE_VERIFICATION_FAILED -> "HIGH";
            case SESSION_EXPIRED, TOKEN_EXPIRED, PASSWORD_EXPIRED,
                 ACCESS_DENIED, INSUFFICIENT_PRIVILEGES -> "MEDIUM";
            case MULTI_FACTOR_REQUIRED, CAPTCHA_REQUIRED, WEAK_PASSWORD,
                 FORCE_PASSWORD_CHANGE -> "LOW";
            default -> "MEDIUM";
        };
    }
    
    /**
     * Gets HTTP status code associated with this error
     */
    public int getHttpStatusCode() {
        return switch (errorCode) {
            case INVALID_CREDENTIALS, PASSWORD_EXPIRED, TOKEN_EXPIRED,
                 TOKEN_INVALID, TOKEN_MALFORMED -> 401;
            case INSUFFICIENT_PRIVILEGES, ACCESS_DENIED, ROLE_NOT_AUTHORIZED -> 403;
            case ACCOUNT_NOT_FOUND -> 404;
            case TOO_MANY_ATTEMPTS, ACCOUNT_LOCKED, IP_ADDRESS_BLOCKED,
                 CONCURRENT_SESSION_LIMIT -> 429;
            case MAINTENANCE_MODE -> 503;
            default -> 400;
        };
    }
    
    /**
     * Creates a map representation for logging
     */
    public Map<String, Object> toLogMap() {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("errorCode", errorCode.getCode());
        logMap.put("errorDescription", errorCode.getDescription());
        logMap.put("message", getMessage());
        logMap.put("timestamp", timestamp);
        logMap.put("severity", getSeverity());
        logMap.put("httpStatusCode", getHttpStatusCode());
        
        if (username != null) logMap.put("username", username);
        if (ipAddress != null) logMap.put("ipAddress", ipAddress);
        if (userAgent != null) logMap.put("userAgent", userAgent);
        if (sessionId != null) logMap.put("sessionId", sessionId);
        if (resource != null) logMap.put("resource", resource);
        if (action != null) logMap.put("action", action);
        if (requiredRole != null) logMap.put("requiredRole", requiredRole);
        if (!requiredPermissions.isEmpty()) logMap.put("requiredPermissions", requiredPermissions);
        if (!context.isEmpty()) logMap.put("context", context);
        
        return logMap;
    }
    
    /**
     * Builds detailed error message
     */
    private static String buildDetailedMessage(ErrorCode errorCode, String username, String ipAddress, String customMessage) {
        StringBuilder message = new StringBuilder();
        
        if (customMessage != null) {
            message.append(customMessage);
        } else {
            message.append(errorCode.getDescription());
        }
        
        if (username != null) {
            message.append(" [User: ").append(username).append("]");
        }
        
        if (ipAddress != null) {
            message.append(" [IP: ").append(ipAddress).append("]");
        }
        
        message.append(" [Code: ").append(errorCode.getCode()).append("]");
        
        return message.toString();
    }
    
    /**
     * Creates a user-friendly message (without sensitive information)
     */
    public String getUserFriendlyMessage() {
        return switch (errorCode) {
            case INVALID_CREDENTIALS -> "Invalid username or password. Please try again.";
            case ACCOUNT_LOCKED -> "Your account is temporarily locked. Please try again later or contact support.";
            case ACCOUNT_DISABLED -> "Your account has been disabled. Please contact support for assistance.";
            case PASSWORD_EXPIRED -> "Your password has expired. Please reset your password to continue.";
            case SESSION_EXPIRED -> "Your session has expired. Please log in again.";
            case TOO_MANY_ATTEMPTS -> "Too many failed login attempts. Please try again later.";
            case MULTI_FACTOR_REQUIRED -> "Multi-factor authentication is required. Please complete the additional verification step.";
            case ACCESS_DENIED -> "You do not have permission to access this resource.";
            case INSUFFICIENT_PRIVILEGES -> "You do not have sufficient privileges to perform this action.";
            case MAINTENANCE_MODE -> "The system is currently under maintenance. Please try again later.";
            default -> "Authentication failed. Please try again or contact support if the problem persists.";
        };
    }
    
    @Override
    public String toString() {
        return String.format("AuthenticationException{errorCode=%s, message='%s', timestamp=%s, username='%s', ipAddress='%s'}", 
                           errorCode, getMessage(), timestamp, username, ipAddress);
    }
}