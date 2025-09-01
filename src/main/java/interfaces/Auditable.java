// File: src/main/java/interfaces/Auditable.java
package interfaces;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Auditable interface defining audit trail and logging operations.
 * This interface provides a contract for entities that need to track changes and maintain audit logs.
 * 
 * Key Java concepts demonstrated:
 * - Interface definition for audit functionality
 * - Enums for audit actions and levels
 * - Nested classes for audit records
 * - Default methods with implementations
 * - Generic types for flexible audit data
 */
public interface Auditable {
    
    /**
     * Audit action types enumeration.
     */
    enum AuditAction {
        CREATE("Create"),
        READ("Read"),
        UPDATE("Update"),
        DELETE("Delete"),
        LOGIN("Login"),
        LOGOUT("Logout"),
        ACCESS("Access"),
        MODIFY("Modify"),
        EXPORT("Export"),
        IMPORT("Import"),
        BACKUP("Backup"),
        RESTORE("Restore"),
        ARCHIVE("Archive"),
        ACTIVATE("Activate"),
        DEACTIVATE("Deactivate"),
        APPROVE("Approve"),
        REJECT("Reject"),
        SUBMIT("Submit"),
        CANCEL("Cancel"),
        TRANSFER("Transfer"),
        ENROLL("Enroll"),
        DROP("Drop"),
        GRADE("Grade"),
        OTHER("Other");
        
        private final String displayName;
        
        AuditAction(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Audit level enumeration for categorizing audit events.
     */
    enum AuditLevel {
        INFO("Information"),
        WARNING("Warning"),
        ERROR("Error"),
        CRITICAL("Critical"),
        SECURITY("Security"),
        ADMIN("Administrative"),
        SYSTEM("System");
        
        private final String displayName;
        
        AuditLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Record an audit event.
     * 
     * @param action The action being performed
     * @param entityId The ID of the entity being acted upon
     * @param userId The ID of the user performing the action
     * @param details Additional details about the action
     * @return AuditRecord representing the logged event
     */
    AuditRecord logAuditEvent(AuditAction action, String entityId, String userId, String details);
    
    /**
     * Record an audit event with specific level.
     * 
     * @param action The action being performed
     * @param level The audit level
     * @param entityId The ID of the entity being acted upon
     * @param userId The ID of the user performing the action
     * @param details Additional details about the action
     * @return AuditRecord representing the logged event
     */
    AuditRecord logAuditEvent(AuditAction action, AuditLevel level, String entityId, String userId, String details);
    
    /**
     * Record an audit event with metadata.
     * 
     * @param action The action being performed
     * @param level The audit level
     * @param entityId The ID of the entity being acted upon
     * @param userId The ID of the user performing the action
     * @param details Additional details about the action
     * @param metadata Additional metadata as key-value pairs
     * @return AuditRecord representing the logged event
     */
    AuditRecord logAuditEvent(AuditAction action, AuditLevel level, String entityId, String userId, 
                             String details, Map<String, Object> metadata);
    
    /**
     * Get audit history for a specific entity.
     * 
     * @param entityId The ID of the entity
     * @return List of audit records for the entity
     */
    List<AuditRecord> getAuditHistory(String entityId);
    
    /**
     * Get audit history for a specific entity within a date range.
     * 
     * @param entityId The ID of the entity
     * @param startDate Start date for the audit search
     * @param endDate End date for the audit search
     * @return List of audit records for the entity within the date range
     */
    List<AuditRecord> getAuditHistory(String entityId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get audit history for a specific user.
     * 
     * @param userId The ID of the user
     * @return List of audit records for actions performed by the user
     */
    List<AuditRecord> getUserAuditHistory(String userId);
    
    /**
     * Get audit history filtered by action type.
     * 
     * @param action The action type to filter by
     * @return List of audit records for the specified action
     */
    List<AuditRecord> getAuditHistoryByAction(AuditAction action);
    
    /**
     * Get audit history filtered by level.
     * 
     * @param level The audit level to filter by
     * @return List of audit records for the specified level
     */
    List<AuditRecord> getAuditHistoryByLevel(AuditLevel level);
    
    /**
     * Search audit logs based on criteria.
     * 
     * @param criteria Map of search criteria
     * @return List of matching audit records
     */
    List<AuditRecord> searchAuditLogs(Map<String, Object> criteria);
    
    /**
     * Get recent audit events.
     * 
     * @param limit Maximum number of recent events to return
     * @return List of recent audit records
     */
    List<AuditRecord> getRecentAuditEvents(int limit);
    
    /**
     * Get audit statistics.
     * 
     * @return Map containing audit statistics
     */
    Map<String, Object> getAuditStatistics();
    
    /**
     * Archive old audit records.
     * 
     * @param beforeDate Archive records older than this date
     * @return Number of records archived
     */
    int archiveAuditRecords(LocalDateTime beforeDate);
    
    /**
     * Purge old audit records.
     * 
     * @param beforeDate Purge records older than this date
     * @return Number of records purged
     */
    int purgeAuditRecords(LocalDateTime beforeDate);
    
    /**
     * Export audit logs.
     * 
     * @param startDate Start date for export
     * @param endDate End date for export
     * @param format Export format (CSV, JSON, XML)
     * @param filePath Path to save the export file
     * @return true if export was successful, false otherwise
     */
    boolean exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String format, String filePath);
    
    /**
     * Default method to log a simple create action.
     * 
     * @param entityId The ID of the created entity
     * @param userId The ID of the user who created the entity
     * @return AuditRecord representing the logged event
     */
    default AuditRecord logCreate(String entityId, String userId) {
        return logAuditEvent(AuditAction.CREATE, entityId, userId, "Entity created");
    }
    
    /**
     * Default method to log a simple update action.
     * 
     * @param entityId The ID of the updated entity
     * @param userId The ID of the user who updated the entity
     * @param changes Description of changes made
     * @return AuditRecord representing the logged event
     */
    default AuditRecord logUpdate(String entityId, String userId, String changes) {
        return logAuditEvent(AuditAction.UPDATE, entityId, userId, "Entity updated: " + changes);
    }
    
    /**
     * Default method to log a simple delete action.
     * 
     * @param entityId The ID of the deleted entity
     * @param userId The ID of the user who deleted the entity
     * @return AuditRecord representing the logged event
     */
    default AuditRecord logDelete(String entityId, String userId) {
        return logAuditEvent(AuditAction.DELETE, entityId, userId, "Entity deleted");
    }
    
    /**
     * Default method to log access to sensitive data.
     * 
     * @param entityId The ID of the accessed entity
     * @param userId The ID of the user who accessed the data
     * @param accessType Type of access (view, export, etc.)
     * @return AuditRecord representing the logged event
     */
    default AuditRecord logDataAccess(String entityId, String userId, String accessType) {
        return logAuditEvent(AuditAction.ACCESS, AuditLevel.SECURITY, entityId, userId, 
                           "Data accessed: " + accessType);
    }
    
    /**
     * Inner class representing an audit record.
     */
    class AuditRecord {
        private final String auditId;
        private final AuditAction action;
        private final AuditLevel level;
        private final String entityId;
        private final String entityType;
        private final String userId;
        private final String userName;
        private final LocalDateTime timestamp;
        private final String details;
        private final Map<String, Object> metadata;
        private final String ipAddress;
        private final String userAgent;
        private final String sessionId;
        
        public AuditRecord(String auditId, AuditAction action, AuditLevel level, String entityId, 
                          String entityType, String userId, String userName, String details) {
            this.auditId = auditId;
            this.action = action;
            this.level = level;
            this.entityId = entityId;
            this.entityType = entityType;
            this.userId = userId;
            this.userName = userName;
            this.details = details;
            this.timestamp = LocalDateTime.now();
            this.metadata = Map.of();
            this.ipAddress = null;
            this.userAgent = null;
            this.sessionId = null;
        }
        
        public AuditRecord(String auditId, AuditAction action, AuditLevel level, String entityId, 
                          String entityType, String userId, String userName, String details,
                          Map<String, Object> metadata, String ipAddress, String userAgent, String sessionId) {
            this.auditId = auditId;
            this.action = action;
            this.level = level;
            this.entityId = entityId;
            this.entityType = entityType;
            this.userId = userId;
            this.userName = userName;
            this.details = details;
            this.metadata = metadata != null ? metadata : Map.of();
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.sessionId = sessionId;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getAuditId() { return auditId; }
        public AuditAction getAction() { return action; }
        public AuditLevel getLevel() { return level; }
        public String getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getDetails() { return details; }
        public Map<String, Object> getMetadata() { return metadata; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public String getSessionId() { return sessionId; }
        
        /**
         * Check if this audit record is security-related.
         * 
         * @return true if this is a security-related audit record
         */
        public boolean isSecurityRelated() {
            return level == AuditLevel.SECURITY || level == AuditLevel.CRITICAL ||
                   action == AuditAction.LOGIN || action == AuditAction.LOGOUT ||
                   action == AuditAction.ACCESS;
        }
        
        /**
         * Check if this audit record indicates an error.
         * 
         * @return true if this audit record indicates an error
         */
        public boolean isError() {
            return level == AuditLevel.ERROR || level == AuditLevel.CRITICAL;
        }
        
        /**
         * Get a formatted summary of the audit record.
         * 
         * @return Formatted summary string
         */
        public String getSummary() {
            return String.format("[%s] %s by %s on %s: %s", 
                level.getDisplayName(), 
                action.getDisplayName(), 
                userName != null ? userName : userId, 
                entityId, 
                details);
        }
        
        @Override
        public String toString() {
            return String.format("AuditRecord{id='%s', action=%s, level=%s, entity='%s', user='%s', timestamp=%s}",
                auditId, action, level, entityId, userId, timestamp);
        }
    }
    
    /**
     * Inner class for audit search criteria.
     */
    class AuditSearchCriteria {
        private String entityId;
        private String userId;
        private AuditAction action;
        private AuditLevel level;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String details;
        private String ipAddress;
        private String sessionId;
        
        // Constructors
        public AuditSearchCriteria() {}
        
        public AuditSearchCriteria(String entityId, String userId) {
            this.entityId = entityId;
            this.userId = userId;
        }
        
        // Builder pattern methods
        public AuditSearchCriteria withEntityId(String entityId) {
            this.entityId = entityId;
            return this;
        }
        
        public AuditSearchCriteria withUserId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public AuditSearchCriteria withAction(AuditAction action) {
            this.action = action;
            return this;
        }
        
        public AuditSearchCriteria withLevel(AuditLevel level) {
            this.level = level;
            return this;
        }
        
        public AuditSearchCriteria withDateRange(LocalDateTime startDate, LocalDateTime endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }
        
        public AuditSearchCriteria withDetails(String details) {
            this.details = details;
            return this;
        }
        
        public AuditSearchCriteria withIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public AuditSearchCriteria withSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        // Getters
        public String getEntityId() { return entityId; }
        public String getUserId() { return userId; }
        public AuditAction getAction() { return action; }
        public AuditLevel getLevel() { return level; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public String getDetails() { return details; }
        public String getIpAddress() { return ipAddress; }
        public String getSessionId() { return sessionId; }
        
        @Override
        public String toString() {
            return String.format("AuditSearchCriteria{entity='%s', user='%s', action=%s, level=%s, dateRange=%s to %s}",
                entityId, userId, action, level, startDate, endDate);
        }
    }
}