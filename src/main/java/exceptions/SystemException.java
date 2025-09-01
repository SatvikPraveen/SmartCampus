// File location: src/main/java/exceptions/SystemException.java
package exceptions;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Exception class for system-level errors in the campus management system
 * Provides detailed error information for infrastructure and system-wide issues
 */
public class SystemException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    // Error codes for different system issues
    public enum ErrorCode {
        CONFIGURATION_ERROR("SYS_001", "System configuration error"),
        SERVICE_UNAVAILABLE("SYS_002", "Service is currently unavailable"),
        RESOURCE_EXHAUSTED("SYS_003", "System resources exhausted"),
        MEMORY_ERROR("SYS_004", "Out of memory error"),
        DISK_SPACE_ERROR("SYS_005", "Insufficient disk space"),
        NETWORK_ERROR("SYS_006", "Network connectivity error"),
        TIMEOUT_ERROR("SYS_007", "Operation timeout"),
        THREAD_POOL_EXHAUSTED("SYS_008", "Thread pool exhausted"),
        CONNECTION_POOL_ERROR("SYS_009", "Connection pool error"),
        CACHE_ERROR("SYS_010", "Cache system error"),
        SCHEDULER_ERROR("SYS_011", "Task scheduler error"),
        MESSAGE_QUEUE_ERROR("SYS_012", "Message queue error"),
        FILE_SYSTEM_ERROR("SYS_013", "File system error"),
        PERMISSION_ERROR("SYS_014", "System permission error"),
        SECURITY_ERROR("SYS_015", "System security error"),
        ENCRYPTION_ERROR("SYS_016", "Encryption/decryption error"),
        SERIALIZATION_ERROR("SYS_017", "Object serialization error"),
        DESERIALIZATION_ERROR("SYS_018", "Object deserialization error"),
        CLASS_LOADING_ERROR("SYS_019", "Class loading error"),
        REFLECTION_ERROR("SYS_020", "Reflection operation error"),
        ANNOTATION_PROCESSING_ERROR("SYS_021", "Annotation processing error"),
        PROXY_ERROR("SYS_022", "Dynamic proxy error"),
        DEPENDENCY_INJECTION_ERROR("SYS_023", "Dependency injection error"),
        BEAN_CREATION_ERROR("SYS_024", "Bean creation error"),
        LIFECYCLE_ERROR("SYS_025", "Component lifecycle error"),
        EVENT_PROCESSING_ERROR("SYS_026", "Event processing error"),
        TRANSACTION_MANAGER_ERROR("SYS_027", "Transaction manager error"),
        SESSION_MANAGER_ERROR("SYS_028", "Session manager error"),
        AUDIT_SYSTEM_ERROR("SYS_029", "Audit system error"),
        LOGGING_ERROR("SYS_030", "Logging system error"),
        MONITORING_ERROR("SYS_031", "Monitoring system error"),
        HEALTH_CHECK_FAILED("SYS_032", "Health check failed"),
        BACKUP_ERROR("SYS_033", "Backup operation error"),
        RESTORE_ERROR("SYS_034", "Restore operation error"),
        MIGRATION_ERROR("SYS_035", "System migration error"),
        UPGRADE_ERROR("SYS_036", "System upgrade error"),
        DEPLOYMENT_ERROR("SYS_037", "Deployment error"),
        STARTUP_ERROR("SYS_038", "System startup error"),
        SHUTDOWN_ERROR("SYS_039", "System shutdown error"),
        MAINTENANCE_MODE_ERROR("SYS_040", "Maintenance mode error"),
        CLUSTER_ERROR("SYS_041", "Cluster operation error"),
        LOAD_BALANCER_ERROR("SYS_042", "Load balancer error"),
        SERVICE_DISCOVERY_ERROR("SYS_043", "Service discovery error"),
        API_GATEWAY_ERROR("SYS_044", "API gateway error"),
        CIRCUIT_BREAKER_OPEN("SYS_045", "Circuit breaker is open"),
        RATE_LIMIT_EXCEEDED("SYS_046", "Rate limit exceeded"),
        QUOTA_EXCEEDED("SYS_047", "Quota exceeded"),
        LICENSE_ERROR("SYS_048", "License validation error"),
        FEATURE_DISABLED("SYS_049", "Feature is disabled"),
        VERSION_MISMATCH("SYS_050", "Version compatibility error"),
        PLUGIN_ERROR("SYS_051", "Plugin system error"),
        EXTERNAL_API_ERROR("SYS_052", "External API error"),
        WEBHOOK_ERROR("SYS_053", "Webhook processing error"),
        INTEGRATION_ERROR("SYS_054", "Integration system error"),
        WORKFLOW_ENGINE_ERROR("SYS_055", "Workflow engine error"),
        RULE_ENGINE_ERROR("SYS_056", "Rule engine error"),
        TEMPLATE_ENGINE_ERROR("SYS_057", "Template engine error"),
        REPORT_GENERATION_ERROR("SYS_058", "Report generation error"),
        NOTIFICATION_ERROR("SYS_059", "Notification system error"),
        EMAIL_SERVICE_ERROR("SYS_060", "Email service error"),
        SMS_SERVICE_ERROR("SYS_061", "SMS service error"),
        PUSH_NOTIFICATION_ERROR("SYS_062", "Push notification error"),
        FILE_UPLOAD_ERROR("SYS_063", "File upload error"),
        FILE_DOWNLOAD_ERROR("SYS_064", "File download error"),
        IMAGE_PROCESSING_ERROR("SYS_065", "Image processing error"),
        PDF_GENERATION_ERROR("SYS_066", "PDF generation error"),
        SEARCH_INDEX_ERROR("SYS_067", "Search index error"),
        ANALYTICS_ERROR("SYS_068", "Analytics system error"),
        TELEMETRY_ERROR("SYS_069", "Telemetry system error"),
        UNKNOWN_SYSTEM_ERROR("SYS_999", "Unknown system error");
        
        private final String code;
        private final String description;
        
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // System component types
    public enum ComponentType {
        DATABASE("Database"),
        CACHE("Cache"),
        MESSAGE_QUEUE("Message Queue"),
        FILE_SYSTEM("File System"),
        NETWORK("Network"),
        SECURITY("Security"),
        AUTHENTICATION("Authentication"),
        AUTHORIZATION("Authorization"),
        SESSION_MANAGEMENT("Session Management"),
        TRANSACTION_MANAGEMENT("Transaction Management"),
        AUDIT("Audit"),
        LOGGING("Logging"),
        MONITORING("Monitoring"),
        BACKUP("Backup"),
        SCHEDULER("Scheduler"),
        WORKFLOW("Workflow"),
        NOTIFICATION("Notification"),
        EMAIL("Email"),
        SMS("SMS"),
        FILE_UPLOAD("File Upload"),
        SEARCH("Search"),
        ANALYTICS("Analytics"),
        REPORTING("Reporting"),
        INTEGRATION("Integration"),
        API_GATEWAY("API Gateway"),
        LOAD_BALANCER("Load Balancer"),
        SERVICE_DISCOVERY("Service Discovery"),
        CIRCUIT_BREAKER("Circuit Breaker"),
        RATE_LIMITER("Rate Limiter"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        ComponentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Exception properties
    private final ErrorCode errorCode;
    private final ComponentType componentType;
    private final String componentName;
    private final String operation;
    private final LocalDateTime timestamp;
    private final String correlationId;
    private final Map<String, Object> systemContext;
    private final String hostName;
    private final String applicationVersion;
    private final String environmentName;
    private final Severity severity;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new SystemException with error code and message
     */
    public SystemException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDescription());
        this.errorCode = errorCode;
        this.componentType = ComponentType.UNKNOWN;
        this.componentName = null;
        this.operation = null;
        this.timestamp = LocalDateTime.now();
        this.correlationId = UUID.randomUUID().toString();
        this.systemContext = new HashMap<>();
        this.hostName = getSystemHostName();
        this.applicationVersion = getSystemVersion();
        this.environmentName = getSystemEnvironment();
        this.severity = determineSeverity(errorCode);
    }
    
    /**
     * Creates a new SystemException with error code, message, and cause
     */
    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getDescription(), cause);
        this.errorCode = errorCode;
        this.componentType = ComponentType.UNKNOWN;
        this.componentName = null;
        this.operation = null;
        this.timestamp = LocalDateTime.now();
        this.correlationId = UUID.randomUUID().toString();
        this.systemContext = new HashMap<>();
        this.hostName = getSystemHostName();
        this.applicationVersion = getSystemVersion();
        this.environmentName = getSystemEnvironment();
        this.severity = determineSeverity(errorCode);
    }
    
    /**
     * Creates a new SystemException with detailed system context
     */
    public SystemException(ErrorCode errorCode, ComponentType componentType, String componentName,
                         String operation, String message, Throwable cause) {
        super(buildDetailedMessage(errorCode, componentType, componentName, operation, message), cause);
        this.errorCode = errorCode;
        this.componentType = componentType;
        this.componentName = componentName;
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
        this.correlationId = UUID.randomUUID().toString();
        this.systemContext = new HashMap<>();
        this.hostName = getSystemHostName();
        this.applicationVersion = getSystemVersion();
        this.environmentName = getSystemEnvironment();
        this.severity = determineSeverity(errorCode);
    }
    
    /**
     * Creates a new SystemException with full context
     */
    public SystemException(ErrorCode errorCode, ComponentType componentType, String componentName,
                         String operation, String correlationId, Severity severity,
                         String message, Throwable cause) {
        super(buildDetailedMessage(errorCode, componentType, componentName, operation, message), cause);
        this.errorCode = errorCode;
        this.componentType = componentType;
        this.componentName = componentName;
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.systemContext = new HashMap<>();
        this.hostName = getSystemHostName();
        this.applicationVersion = getSystemVersion();
        this.environmentName = getSystemEnvironment();
        this.severity = severity != null ? severity : determineSeverity(errorCode);
    }
    
    // ==================== BUILDER PATTERN ====================
    
    /**
     * Builder for creating SystemException with fluent interface
     */
    public static class Builder {
        private ErrorCode errorCode;
        private ComponentType componentType = ComponentType.UNKNOWN;
        private String componentName;
        private String operation;
        private String message;
        private Throwable cause;
        private String correlationId;
        private Severity severity;
        private final Map<String, Object> systemContext = new HashMap<>();
        
        public Builder(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
        
        public Builder componentType(ComponentType componentType) {
            this.componentType = componentType;
            return this;
        }
        
        public Builder componentName(String componentName) {
            this.componentName = componentName;
            return this;
        }
        
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder addContext(String key, Object value) {
            this.systemContext.put(key, value);
            return this;
        }
        
        public Builder addContext(Map<String, Object> context) {
            this.systemContext.putAll(context);
            return this;
        }
        
        public SystemException build() {
            SystemException exception = new SystemException(errorCode, componentType, componentName,
                operation, correlationId, severity, message, cause);
            exception.systemContext.putAll(this.systemContext);
            return exception;
        }
    }
    
    /**
     * Creates a new builder with the specified error code
     */
    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode);
    }
    
    // ==================== SEVERITY ENUM ====================
    
    public enum Severity {
        LOW("Low", 1),
        MEDIUM("Medium", 2),
        HIGH("High", 3),
        CRITICAL("Critical", 4);
        
        private final String displayName;
        private final int level;
        
        Severity(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Creates exception for configuration errors
     */
    public static SystemException configurationError(String configKey, String reason, Throwable cause) {
        return builder(ErrorCode.CONFIGURATION_ERROR)
            .componentType(ComponentType.UNKNOWN)
            .addContext("configKey", configKey)
            .addContext("reason", reason)
            .message("Configuration error for key '" + configKey + "': " + reason)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for service unavailable
     */
    public static SystemException serviceUnavailable(String serviceName, String reason) {
        return builder(ErrorCode.SERVICE_UNAVAILABLE)
            .componentName(serviceName)
            .addContext("reason", reason)
            .message("Service '" + serviceName + "' is unavailable: " + reason)
            .build();
    }
    
    /**
     * Creates exception for resource exhaustion
     */
    public static SystemException resourceExhausted(String resourceType, String resourceName, 
                                                  long currentUsage, long maxCapacity) {
        return builder(ErrorCode.RESOURCE_EXHAUSTED)
            .addContext("resourceType", resourceType)
            .addContext("resourceName", resourceName)
            .addContext("currentUsage", currentUsage)
            .addContext("maxCapacity", maxCapacity)
            .message(String.format("%s '%s' exhausted: %d/%d", resourceType, resourceName, currentUsage, maxCapacity))
            .build();
    }
    
    /**
     * Creates exception for network errors
     */
    public static SystemException networkError(String host, int port, String operation, Throwable cause) {
        return builder(ErrorCode.NETWORK_ERROR)
            .componentType(ComponentType.NETWORK)
            .operation(operation)
            .addContext("host", host)
            .addContext("port", port)
            .message("Network error during " + operation + " to " + host + ":" + port)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for timeout errors
     */
    public static SystemException timeoutError(String operation, long timeoutMs, Throwable cause) {
        return builder(ErrorCode.TIMEOUT_ERROR)
            .operation(operation)
            .addContext("timeoutMs", timeoutMs)
            .message("Operation '" + operation + "' timed out after " + timeoutMs + "ms")
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for database errors
     */
    public static SystemException databaseError(String operation, String tableName, Throwable cause) {
        return builder(ErrorCode.SERVICE_UNAVAILABLE)
            .componentType(ComponentType.DATABASE)
            .operation(operation)
            .addContext("tableName", tableName)
            .message("Database error during " + operation + (tableName != null ? " on table " + tableName : ""))
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for cache errors
     */
    public static SystemException cacheError(String cacheName, String operation, Throwable cause) {
        return builder(ErrorCode.CACHE_ERROR)
            .componentType(ComponentType.CACHE)
            .componentName(cacheName)
            .operation(operation)
            .message("Cache error in '" + cacheName + "' during " + operation)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for file system errors
     */
    public static SystemException fileSystemError(String path, String operation, Throwable cause) {
        return builder(ErrorCode.FILE_SYSTEM_ERROR)
            .componentType(ComponentType.FILE_SYSTEM)
            .operation(operation)
            .addContext("path", path)
            .message("File system error during " + operation + " at path: " + path)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for external API errors
     */
    public static SystemException externalApiError(String apiName, String endpoint, 
                                                  int statusCode, String response, Throwable cause) {
        return builder(ErrorCode.EXTERNAL_API_ERROR)
            .componentType(ComponentType.INTEGRATION)
            .componentName(apiName)
            .addContext("endpoint", endpoint)
            .addContext("statusCode", statusCode)
            .addContext("response", response)
            .message("External API error from " + apiName + " at " + endpoint + " (status: " + statusCode + ")")
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for rate limit exceeded
     */
    public static SystemException rateLimitExceeded(String rateLimiterName, long currentRate, long maxRate) {
        return builder(ErrorCode.RATE_LIMIT_EXCEEDED)
            .componentType(ComponentType.RATE_LIMITER)
            .componentName(rateLimiterName)
            .addContext("currentRate", currentRate)
            .addContext("maxRate", maxRate)
            .message("Rate limit exceeded for '" + rateLimiterName + "': " + currentRate + "/" + maxRate)
            .build();
    }
    
    // ==================== GETTERS ====================
    
    public ErrorCode getErrorCode() { return errorCode; }
    public ComponentType getComponentType() { return componentType; }
    public String getComponentName() { return componentName; }
    public String getOperation() { return operation; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public Map<String, Object> getSystemContext() { return new HashMap<>(systemContext); }
    public String getHostName() { return hostName; }
    public String getApplicationVersion() { return applicationVersion; }
    public String getEnvironmentName() { return environmentName; }
    public Severity getSeverity() { return severity; }
    
    /**
     * Gets specific system context value
     */
    public Object getSystemContext(String key) {
        return systemContext.get(key);
    }
    
    /**
     * Gets system context value as specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getSystemContext(String key, Class<T> type) {
        Object value = systemContext.get(key);
        return (value != null && type.isInstance(value)) ? (T) value : null;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if this exception is recoverable
     */
    public boolean isRecoverable() {
        switch (errorCode) {
            case TIMEOUT_ERROR:
            case NETWORK_ERROR:
            case SERVICE_UNAVAILABLE:
            case RESOURCE_EXHAUSTED:
            case THREAD_POOL_EXHAUSTED:
            case CONNECTION_POOL_ERROR:
            case RATE_LIMIT_EXCEEDED:
            case CIRCUIT_BREAKER_OPEN:
            case EXTERNAL_API_ERROR:
                return true;
            case CONFIGURATION_ERROR:
            case MEMORY_ERROR:
            case DISK_SPACE_ERROR:
            case PERMISSION_ERROR:
            case SECURITY_ERROR:
            case CLASS_LOADING_ERROR:
            case VERSION_MISMATCH:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this exception requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH;
    }
    
    /**
     * Checks if this exception affects system availability
     */
    public boolean affectsAvailability() {
        switch (errorCode) {
            case SERVICE_UNAVAILABLE:
            case STARTUP_ERROR:
            case SHUTDOWN_ERROR:
            case HEALTH_CHECK_FAILED:
            case CLUSTER_ERROR:
            case LOAD_BALANCER_ERROR:
            case API_GATEWAY_ERROR:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets recovery suggestions
     */
    public String getRecoverySuggestion() {
        switch (errorCode) {
            case CONFIGURATION_ERROR:
                return "Check system configuration and restart if necessary";
            case SERVICE_UNAVAILABLE:
                return "Check service health and restart if needed";
            case RESOURCE_EXHAUSTED:
                return "Scale up resources or optimize resource usage";
            case NETWORK_ERROR:
                return "Check network connectivity and retry operation";
            case TIMEOUT_ERROR:
                return "Increase timeout values or optimize operation performance";
            case CACHE_ERROR:
                return "Clear cache and restart cache service";
            case FILE_SYSTEM_ERROR:
                return "Check file system permissions and disk space";
            case RATE_LIMIT_EXCEEDED:
                return "Reduce request rate or increase rate limits";
            case EXTERNAL_API_ERROR:
                return "Check external API status and retry with backoff";
            default:
                return "Contact system administrator for assistance";
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Builds a detailed error message with context
     */
    private static String buildDetailedMessage(ErrorCode errorCode, ComponentType componentType,
                                             String componentName, String operation, String customMessage) {
        StringBuilder message = new StringBuilder();
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append(customMessage);
        } else {
            message.append(errorCode.getDescription());
        }
        
        // Add context information
        StringBuilder context = new StringBuilder();
        if (componentType != ComponentType.UNKNOWN) {
            context.append("Component: ").append(componentType.getDisplayName());
        }
        if (componentName != null) {
            if (context.length() > 0) context.append(", ");
            context.append("Name: ").append(componentName);
        }
        if (operation != null) {
            if (context.length() > 0) context.append(", ");
            context.append("Operation: ").append(operation);
        }
        
        if (context.length() > 0) {
            message.append(" [").append(context).append("]");
        }
        
        return message.toString();
    }
    
    /**
     * Determines severity based on error code
     */
    private static Severity determineSeverity(ErrorCode errorCode) {
        switch (errorCode) {
            case MEMORY_ERROR:
            case DISK_SPACE_ERROR:
            case STARTUP_ERROR:
            case SHUTDOWN_ERROR:
            case SECURITY_ERROR:
            case HEALTH_CHECK_FAILED:
                return Severity.CRITICAL;
            case SERVICE_UNAVAILABLE:
            case RESOURCE_EXHAUSTED:
            case NETWORK_ERROR:
            case CONFIGURATION_ERROR:
            case BACKUP_ERROR:
            case RESTORE_ERROR:
                return Severity.HIGH;
            case TIMEOUT_ERROR:
            case CACHE_ERROR:
            case FILE_SYSTEM_ERROR:
            case THREAD_POOL_EXHAUSTED:
            case CONNECTION_POOL_ERROR:
            case EXTERNAL_API_ERROR:
            case RATE_LIMIT_EXCEEDED:
                return Severity.MEDIUM;
            case SCHEDULER_ERROR:
            case MESSAGE_QUEUE_ERROR:
            case LOGGING_ERROR:
            case MONITORING_ERROR:
            case NOTIFICATION_ERROR:
                return Severity.LOW;
            default:
                return Severity.MEDIUM;
        }
    }
    
    /**
     * Gets system host name
     */
    private static String getSystemHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Gets system version (simplified implementation)
     */
    private static String getSystemVersion() {
        return System.getProperty("application.version", "unknown");
    }
    
    /**
     * Gets system environment
     */
    private static String getSystemEnvironment() {
        return System.getProperty("application.environment", 
                  System.getProperty("ENVIRONMENT", "unknown"));
    }
    
    // ==================== MONITORING AND ALERTING ====================
    
    /**
     * Creates a monitoring alert entry
     */
    public Map<String, Object> toMonitoringAlert() {
        Map<String, Object> alert = new HashMap<>();
        
        alert.put("alertType", "SYSTEM_EXCEPTION");
        alert.put("severity", severity.toString());
        alert.put("errorCode", errorCode.getCode());
        alert.put("component", componentType.getDisplayName());
        alert.put("componentName", componentName);
        alert.put("operation", operation);
        alert.put("message", getMessage());
        alert.put("timestamp", timestamp.toString());
        alert.put("correlationId", correlationId);
        alert.put("hostName", hostName);
        alert.put("environment", environmentName);
        alert.put("version", applicationVersion);
        alert.put("recoverable", isRecoverable());
        alert.put("affectsAvailability", affectsAvailability());
        alert.put("requiresImmediateAttention", requiresImmediateAttention());
        
        // Add system context
        if (!systemContext.isEmpty()) {
            alert.put("context", systemContext);
        }
        
        // Add cause information
        if (getCause() != null) {
            Map<String, Object> causeInfo = new HashMap<>();
            causeInfo.put("type", getCause().getClass().getSimpleName());
            causeInfo.put("message", getCause().getMessage());
            
            // Add stack trace summary
            StackTraceElement[] stackTrace = getCause().getStackTrace();
            if (stackTrace.length > 0) {
                causeInfo.put("location", stackTrace[0].toString());
            }
            
            alert.put("cause", causeInfo);
        }
        
        return alert;
    }
    
    /**
     * Gets recommended alert channels based on severity
     */
    public java.util.List<String> getRecommendedAlertChannels() {
        java.util.List<String> channels = new java.util.ArrayList<>();
        
        switch (severity) {
            case CRITICAL:
                channels.add("PAGER");
                channels.add("SMS");
                channels.add("EMAIL");
                channels.add("SLACK");
                break;
            case HIGH:
                channels.add("EMAIL");
                channels.add("SLACK");
                break;
            case MEDIUM:
                channels.add("SLACK");
                break;
            case LOW:
                channels.add("LOG");
                break;
        }
        
        return channels;
    }
    
    /**
     * Gets alert priority for external systems
     */
    public int getAlertPriority() {
        switch (severity) {
            case CRITICAL: return 1;
            case HIGH: return 2;
            case MEDIUM: return 3;
            case LOW: return 4;
            default: return 3;
        }
    }
    
    /**
     * Checks if this exception should trigger auto-recovery
     */
    public boolean shouldTriggerAutoRecovery() {
        return isRecoverable() && (severity == Severity.HIGH || severity == Severity.CRITICAL);
    }
    
    /**
     * Gets auto-recovery actions
     */
    public java.util.List<String> getAutoRecoveryActions() {
        java.util.List<String> actions = new java.util.ArrayList<>();
        
        switch (errorCode) {
            case SERVICE_UNAVAILABLE:
                actions.add("RESTART_SERVICE");
                actions.add("HEALTH_CHECK");
                break;
            case RESOURCE_EXHAUSTED:
                actions.add("SCALE_UP");
                actions.add("CLEAR_CACHE");
                break;
            case CACHE_ERROR:
                actions.add("CLEAR_CACHE");
                actions.add("RESTART_CACHE_SERVICE");
                break;
            case NETWORK_ERROR:
                actions.add("RETRY_WITH_BACKOFF");
                actions.add("FAILOVER");
                break;
            case TIMEOUT_ERROR:
                actions.add("RETRY_WITH_BACKOFF");
                break;
            case THREAD_POOL_EXHAUSTED:
                actions.add("SCALE_THREAD_POOL");
                actions.add("REJECT_NEW_REQUESTS");
                break;
            case CONNECTION_POOL_ERROR:
                actions.add("RECREATE_CONNECTIONS");
                actions.add("SCALE_CONNECTION_POOL");
                break;
            case CIRCUIT_BREAKER_OPEN:
                actions.add("WAIT_FOR_RECOVERY");
                actions.add("HEALTH_CHECK");
                break;
        }
        
        return actions;
    }
    
    // ==================== DIAGNOSTIC INFORMATION ====================
    
    /**
     * Collects diagnostic information for troubleshooting
     */
    public Map<String, Object> getDiagnosticInfo() {
        Map<String, Object> diagnostics = new HashMap<>();
        
        // Basic exception info
        diagnostics.put("errorCode", errorCode.getCode());
        diagnostics.put("errorDescription", errorCode.getDescription());
        diagnostics.put("severity", severity.toString());
        diagnostics.put("timestamp", timestamp.toString());
        diagnostics.put("correlationId", correlationId);
        
        // System info
        diagnostics.put("hostName", hostName);
        diagnostics.put("applicationVersion", applicationVersion);
        diagnostics.put("environment", environmentName);
        diagnostics.put("javaVersion", System.getProperty("java.version"));
        diagnostics.put("osName", System.getProperty("os.name"));
        diagnostics.put("osVersion", System.getProperty("os.version"));
        
        // Component info
        diagnostics.put("componentType", componentType.getDisplayName());
        diagnostics.put("componentName", componentName);
        diagnostics.put("operation", operation);
        
        // Runtime info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> runtimeInfo = new HashMap<>();
        runtimeInfo.put("maxMemory", runtime.maxMemory());
        runtimeInfo.put("totalMemory", runtime.totalMemory());
        runtimeInfo.put("freeMemory", runtime.freeMemory());
        runtimeInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        runtimeInfo.put("availableProcessors", runtime.availableProcessors());
        diagnostics.put("runtime", runtimeInfo);
        
        // Thread info
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("activeThreadCount", Thread.activeCount());
        threadInfo.put("currentThreadName", Thread.currentThread().getName());
        threadInfo.put("currentThreadId", Thread.currentThread().getId());
        diagnostics.put("thread", threadInfo);
        
        // System context
        if (!systemContext.isEmpty()) {
            diagnostics.put("systemContext", systemContext);
        }
        
        // Stack trace
        if (getCause() != null) {
            StackTraceElement[] stackTrace = getCause().getStackTrace();
            if (stackTrace.length > 0) {
                java.util.List<String> relevantTrace = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    relevantTrace.add(stackTrace[i].toString());
                }
                diagnostics.put("stackTrace", relevantTrace);
            }
        }
        
        return diagnostics;
    }
    
    /**
     * Gets performance impact assessment
     */
    public String getPerformanceImpact() {
        switch (severity) {
            case CRITICAL:
                return "Service disruption likely, immediate action required";
            case HIGH:
                return "Significant performance degradation possible";
            case MEDIUM:
                return "Minor performance impact, monitor closely";
            case LOW:
                return "Minimal performance impact";
            default:
                return "Unknown performance impact";
        }
    }
    
    /**
     * Gets business impact description
     */
    public String getBusinessImpact() {
        if (affectsAvailability()) {
            return "High - Service availability affected";
        }
        
        switch (componentType) {
            case DATABASE:
                return "High - Data operations affected";
            case AUTHENTICATION:
            case AUTHORIZATION:
                return "High - User access affected";
            case NOTIFICATION:
            case EMAIL:
            case SMS:
                return "Medium - Communication services affected";
            case CACHE:
            case SEARCH:
                return "Medium - Performance may be degraded";
            case ANALYTICS:
            case REPORTING:
                return "Low - Non-critical features affected";
            default:
                return "Medium - System functionality may be affected";
        }
    }
    
    // ==================== SERIALIZATION SUPPORT ====================
    
    /**
     * Creates a structured log entry for centralized logging
     */
    public Map<String, Object> toStructuredLogEntry() {
        Map<String, Object> logEntry = new HashMap<>();
        
        logEntry.put("@timestamp", timestamp.toString());
        logEntry.put("level", severity.toString());
        logEntry.put("logger", "SystemException");
        logEntry.put("message", getMessage());
        logEntry.put("correlationId", correlationId);
        
        // Exception details
        Map<String, Object> exception = new HashMap<>();
        exception.put("code", errorCode.getCode());
        exception.put("description", errorCode.getDescription());
        exception.put("type", "SystemException");
        logEntry.put("exception", exception);
        
        // System details
        Map<String, Object> system = new HashMap<>();
        system.put("host", hostName);
        system.put("environment", environmentName);
        system.put("version", applicationVersion);
        logEntry.put("system", system);
        
        // Component details
        Map<String, Object> component = new HashMap<>();
        component.put("type", componentType.getDisplayName());
        component.put("name", componentName);
        component.put("operation", operation);
        logEntry.put("component", component);
        
        // Additional context
        if (!systemContext.isEmpty()) {
            logEntry.put("context", systemContext);
        }
        
        // Cause information
        if (getCause() != null) {
            Map<String, Object> cause = new HashMap<>();
            cause.put("type", getCause().getClass().getName());
            cause.put("message", getCause().getMessage());
            logEntry.put("cause", cause);
        }
        
        // Add tags for easier searching
        java.util.List<String> tags = new java.util.ArrayList<>();
        tags.add("system-exception");
        tags.add(severity.toString().toLowerCase());
        tags.add(componentType.toString().toLowerCase());
        if (isRecoverable()) tags.add("recoverable");
        if (affectsAvailability()) tags.add("availability-impact");
        logEntry.put("tags", tags);
        
        return logEntry;
    }
    
    /**
     * Returns a detailed string representation for logging
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SystemException{");
        sb.append("errorCode=").append(errorCode.getCode());
        sb.append(", severity=").append(severity);
        sb.append(", message='").append(getMessage()).append("'");
        sb.append(", component=").append(componentType.getDisplayName());
        
        if (componentName != null) sb.append(", componentName='").append(componentName).append("'");
        if (operation != null) sb.append(", operation='").append(operation).append("'");
        
        sb.append(", timestamp=").append(timestamp);
        sb.append(", correlationId='").append(correlationId).append("'");
        sb.append(", host='").append(hostName).append("'");
        sb.append(", environment='").append(environmentName).append("'");
        sb.append(", recoverable=").append(isRecoverable());
        sb.append(", affectsAvailability=").append(affectsAvailability());
        
        if (!systemContext.isEmpty()) {
            sb.append(", context=").append(systemContext);
        }
        
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append(": ").append(getCause().getMessage());
        }
        
        sb.append("}");
        return sb.toString();
    }
}