// File location: src/main/java/annotations/Audited.java
package annotations;

import java.lang.annotation.*;

/**
 * Annotation to enable audit logging for classes, methods, or fields
 * Tracks changes, access patterns, and maintains audit trail
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    
    /**
     * Audit event types
     */
    enum AuditType {
        CREATE,     // Entity creation
        UPDATE,     // Entity modification
        DELETE,     // Entity deletion
        READ,       // Entity access/read
        LOGIN,      // User login
        LOGOUT,     // User logout
        ACCESS,     // Resource access
        OPERATION,  // Method execution
        SECURITY,   // Security-related events
        CUSTOM      // Custom audit events
    }
    
    /**
     * Audit level enumeration
     */
    enum Level {
        NONE,       // No auditing
        BASIC,      // Basic audit info (who, when, what)
        DETAILED,   // Detailed audit with before/after values
        FULL        // Full audit including context and metadata
    }
    
    /**
     * Storage strategy for audit logs
     */
    enum Storage {
        DATABASE,   // Store in database
        FILE,       // Store in log files
        MEMORY,     // Store in memory (temporary)
        EXTERNAL,   // Send to external system
        CUSTOM      // Custom storage implementation
    }
    
    /**
     * Types of audit events to track
     * @return array of audit types
     */
    AuditType[] value() default {AuditType.CREATE, AuditType.UPDATE, AuditType.DELETE};
    
    /**
     * Audit level detail
     * @return audit level
     */
    Level level() default Level.BASIC;
    
    /**
     * Storage strategy for audit logs
     * @return storage strategy
     */
    Storage storage() default Storage.DATABASE;
    
    /**
     * Whether to audit successful operations
     * @return true to audit successful operations
     */
    boolean auditSuccess() default true;
    
    /**
     * Whether to audit failed operations
     * @return true to audit failed operations
     */
    boolean auditFailure() default true;
    
    /**
     * Whether to include method parameters in audit
     * @return true to include parameters
     */
    boolean includeParameters() default false;
    
    /**
     * Whether to include return values in audit
     * @return true to include return values
     */
    boolean includeReturnValue() default false;
    
    /**
     * Whether to include stack trace for exceptions
     * @return true to include stack trace
     */
    boolean includeStackTrace() default false;
    
    /**
     * Fields to exclude from auditing (for sensitive data)
     * @return array of field names to exclude
     */
    String[] excludeFields() default {};
    
    /**
     * Fields to mask in audit logs (show partially)
     * @return array of field names to mask
     */
    String[] maskFields() default {};
    
    /**
     * Custom audit message template
     * @return message template
     */
    String messageTemplate() default "";
    
    /**
     * Audit category for grouping related events
     * @return category name
     */
    String category() default "";
    
    /**
     * Tags for audit event classification
     * @return array of tags
     */
    String[] tags() default {};
    
    /**
     * Condition for conditional auditing
     * @return SpEL expression that must be true for auditing
     */
    String condition() default "";
    
    /**
     * Whether to audit asynchronously
     * @return true for async auditing
     */
    boolean async() default true;
    
    /**
     * Retention period for audit logs in days
     * @return retention period, 0 means indefinite
     */
    int retentionDays() default 365;
    
    /**
     * Priority for audit processing
     * @return priority level (higher number = higher priority)
     */
    int priority() default 0;
    
    /**
     * Whether to compress audit data
     * @return true to enable compression
     */
    boolean compress() default false;
    
    /**
     * Whether to encrypt audit data
     * @return true to enable encryption
     */
    boolean encrypt() default false;
    
    /**
     * Custom audit processor class
     * @return processor class
     */
    Class<?> processor() default Void.class;
    
    /**
     * Whether to include user context in audit
     * @return true to include user context
     */
    boolean includeUserContext() default true;
    
    /**
     * Whether to include system context in audit
     * @return true to include system context
     */
    boolean includeSystemContext() default false;
    
    /**
     * Whether to include request context in audit (for web applications)
     * @return true to include request context
     */
    boolean includeRequestContext() default false;
    
    /**
     * Custom correlation ID for tracking related operations
     * @return correlation ID expression
     */
    String correlationId() default "";
    
    /**
     * Whether to generate alerts for specific audit events
     * @return true to enable alerting
     */
    boolean enableAlerts() default false;
    
    /**
     * Alert threshold conditions
     * @return array of alert conditions
     */
    String[] alertConditions() default {};
}