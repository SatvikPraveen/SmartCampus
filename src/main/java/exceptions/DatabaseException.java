// File location: src/main/java/exceptions/DatabaseException.java
package exceptions;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

/**
 * Exception class for database-related errors in the campus management system
 * Provides detailed error information for database operations and connection issues
 */
public class DatabaseException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    // Error codes for different database issues
    public enum ErrorCode {
        CONNECTION_FAILED("DB_001", "Failed to establish database connection"),
        CONNECTION_TIMEOUT("DB_002", "Database connection timeout"),
        CONNECTION_POOL_EXHAUSTED("DB_003", "Database connection pool exhausted"),
        QUERY_EXECUTION_FAILED("DB_004", "Failed to execute database query"),
        TRANSACTION_FAILED("DB_005", "Database transaction failed"),
        ROLLBACK_FAILED("DB_006", "Failed to rollback transaction"),
        COMMIT_FAILED("DB_007", "Failed to commit transaction"),
        CONSTRAINT_VIOLATION("DB_008", "Database constraint violation"),
        FOREIGN_KEY_VIOLATION("DB_009", "Foreign key constraint violation"),
        UNIQUE_CONSTRAINT_VIOLATION("DB_010", "Unique constraint violation"),
        NOT_NULL_VIOLATION("DB_011", "Not null constraint violation"),
        CHECK_CONSTRAINT_VIOLATION("DB_012", "Check constraint violation"),
        DEADLOCK_DETECTED("DB_013", "Database deadlock detected"),
        LOCK_TIMEOUT("DB_014", "Database lock timeout"),
        INVALID_SQL_SYNTAX("DB_015", "Invalid SQL syntax"),
        TABLE_NOT_FOUND("DB_016", "Table or view does not exist"),
        COLUMN_NOT_FOUND("DB_017", "Column does not exist"),
        INDEX_NOT_FOUND("DB_018", "Index does not exist"),
        PROCEDURE_NOT_FOUND("DB_019", "Stored procedure or function not found"),
        PERMISSION_DENIED("DB_020", "Insufficient database permissions"),
        DISK_FULL("DB_021", "Database disk space full"),
        DATA_TRUNCATION("DB_022", "Data truncation occurred"),
        INVALID_DATA_TYPE("DB_023", "Invalid data type conversion"),
        BACKUP_FAILED("DB_024", "Database backup operation failed"),
        RESTORE_FAILED("DB_025", "Database restore operation failed"),
        MIGRATION_FAILED("DB_026", "Database schema migration failed"),
        REPLICATION_ERROR("DB_027", "Database replication error"),
        CORRUPTION_DETECTED("DB_028", "Database corruption detected"),
        CONFIGURATION_ERROR("DB_029", "Database configuration error"),
        DRIVER_ERROR("DB_030", "Database driver error"),
        UNSUPPORTED_OPERATION("DB_031", "Unsupported database operation"),
        BATCH_OPERATION_FAILED("DB_032", "Batch operation failed"),
        RESULT_SET_CLOSED("DB_033", "Result set is closed"),
        STATEMENT_CLOSED("DB_034", "Statement is closed"),
        UNKNOWN_ERROR("DB_999", "Unknown database error");
        
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
    private final String operation;
    private final String tableName;
    private final String query;
    private final LocalDateTime timestamp;
    private final Map<String, Object> context;
    private final int sqlErrorCode;
    private final String sqlState;
    private final long executionTimeMs;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new DatabaseException with error code and message
     */
    public DatabaseException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDescription());
        this.errorCode = errorCode;
        this.operation = null;
        this.tableName = null;
        this.query = null;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.sqlErrorCode = 0;
        this.sqlState = null;
        this.executionTimeMs = 0;
    }
    
    /**
     * Creates a new DatabaseException with error code, message, and cause
     */
    public DatabaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getDescription(), cause);
        this.errorCode = errorCode;
        this.operation = null;
        this.tableName = null;
        this.query = null;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.executionTimeMs = 0;
        
        // Extract SQL error details if cause is SQLException
        if (cause instanceof SQLException) {
            SQLException sqlEx = (SQLException) cause;
            this.sqlErrorCode = sqlEx.getErrorCode();
            this.sqlState = sqlEx.getSQLState();
        } else {
            this.sqlErrorCode = 0;
            this.sqlState = null;
        }
    }
    
    /**
     * Creates a new DatabaseException with detailed database context
     */
    public DatabaseException(ErrorCode errorCode, String operation, String tableName, 
                           String query, String message, Throwable cause) {
        super(buildDetailedMessage(errorCode, operation, tableName, message), cause);
        this.errorCode = errorCode;
        this.operation = operation;
        this.tableName = tableName;
        this.query = query;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.executionTimeMs = 0;
        
        if (cause instanceof SQLException) {
            SQLException sqlEx = (SQLException) cause;
            this.sqlErrorCode = sqlEx.getErrorCode();
            this.sqlState = sqlEx.getSQLState();
        } else {
            this.sqlErrorCode = 0;
            this.sqlState = null;
        }
    }
    
    /**
     * Creates a new DatabaseException with full context including timing
     */
    public DatabaseException(ErrorCode errorCode, String operation, String tableName, 
                           String query, long executionTimeMs, String message, Throwable cause) {
        super(buildDetailedMessage(errorCode, operation, tableName, message), cause);
        this.errorCode = errorCode;
        this.operation = operation;
        this.tableName = tableName;
        this.query = query;
        this.timestamp = LocalDateTime.now();
        this.context = new HashMap<>();
        this.executionTimeMs = executionTimeMs;
        
        if (cause instanceof SQLException) {
            SQLException sqlEx = (SQLException) cause;
            this.sqlErrorCode = sqlEx.getErrorCode();
            this.sqlState = sqlEx.getSQLState();
        } else {
            this.sqlErrorCode = 0;
            this.sqlState = null;
        }
    }
    
    // ==================== BUILDER PATTERN ====================
    
    /**
     * Builder for creating DatabaseException with fluent interface
     */
    public static class Builder {
        private ErrorCode errorCode;
        private String message;
        private Throwable cause;
        private String operation;
        private String tableName;
        private String query;
        private long executionTimeMs = 0;
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
        
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }
        
        public Builder executionTime(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
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
        
        public DatabaseException build() {
            DatabaseException exception = new DatabaseException(
                errorCode, operation, tableName, query, executionTimeMs, message, cause
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
     * Creates exception for connection failures
     */
    public static DatabaseException connectionFailed(String host, int port, String database, Throwable cause) {
        return builder(ErrorCode.CONNECTION_FAILED)
            .addContext("host", host)
            .addContext("port", port)
            .addContext("database", database)
            .message("Failed to connect to database " + database + " at " + host + ":" + port)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for connection timeouts
     */
    public static DatabaseException connectionTimeout(long timeoutMs) {
        return builder(ErrorCode.CONNECTION_TIMEOUT)
            .addContext("timeoutMs", timeoutMs)
            .message("Database connection timed out after " + timeoutMs + "ms")
            .build();
    }
    
    /**
     * Creates exception for query execution failures
     */
    public static DatabaseException queryExecutionFailed(String query, Throwable cause) {
        return builder(ErrorCode.QUERY_EXECUTION_FAILED)
            .query(query)
            .message("Failed to execute query")
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for query execution failures with timing
     */
    public static DatabaseException queryExecutionFailed(String query, long executionTimeMs, Throwable cause) {
        return builder(ErrorCode.QUERY_EXECUTION_FAILED)
            .query(query)
            .executionTime(executionTimeMs)
            .message("Failed to execute query after " + executionTimeMs + "ms")
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for transaction failures
     */
    public static DatabaseException transactionFailed(String operation, Throwable cause) {
        return builder(ErrorCode.TRANSACTION_FAILED)
            .operation(operation)
            .message("Transaction failed during " + operation)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for constraint violations
     */
    public static DatabaseException constraintViolation(String tableName, String constraintName, 
                                                       String operation, Throwable cause) {
        return builder(ErrorCode.CONSTRAINT_VIOLATION)
            .tableName(tableName)
            .operation(operation)
            .addContext("constraintName", constraintName)
            .message("Constraint violation on table " + tableName + ": " + constraintName)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for foreign key violations
     */
    public static DatabaseException foreignKeyViolation(String tableName, String foreignKey, 
                                                       String referencedTable, Throwable cause) {
        return builder(ErrorCode.FOREIGN_KEY_VIOLATION)
            .tableName(tableName)
            .addContext("foreignKey", foreignKey)
            .addContext("referencedTable", referencedTable)
            .message("Foreign key constraint violation: " + foreignKey + " references " + referencedTable)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for unique constraint violations
     */
    public static DatabaseException uniqueConstraintViolation(String tableName, String columnName, 
                                                            Object duplicateValue, Throwable cause) {
        return builder(ErrorCode.UNIQUE_CONSTRAINT_VIOLATION)
            .tableName(tableName)
            .addContext("columnName", columnName)
            .addContext("duplicateValue", duplicateValue)
            .message("Unique constraint violation on " + tableName + "." + columnName + ": " + duplicateValue)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for deadlock detection
     */
    public static DatabaseException deadlockDetected(String operation, String tableName, Throwable cause) {
        return builder(ErrorCode.DEADLOCK_DETECTED)
            .operation(operation)
            .tableName(tableName)
            .message("Deadlock detected during " + operation + " on table " + tableName)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception for table not found
     */
    public static DatabaseException tableNotFound(String tableName, String operation) {
        return builder(ErrorCode.TABLE_NOT_FOUND)
            .tableName(tableName)
            .operation(operation)
            .message("Table '" + tableName + "' not found during " + operation)
            .build();
    }
    
    /**
     * Creates exception for permission denied
     */
    public static DatabaseException permissionDenied(String operation, String resource, String user) {
        return builder(ErrorCode.PERMISSION_DENIED)
            .operation(operation)
            .addContext("resource", resource)
            .addContext("user", user)
            .message("Permission denied for user '" + user + "' to " + operation + " on " + resource)
            .build();
    }
    
    /**
     * Creates exception for migration failures
     */
    public static DatabaseException migrationFailed(String migrationVersion, String description, Throwable cause) {
        return builder(ErrorCode.MIGRATION_FAILED)
            .operation("migration")
            .addContext("version", migrationVersion)
            .addContext("description", description)
            .message("Migration failed: " + migrationVersion + " - " + description)
            .cause(cause)
            .build();
    }
    
    /**
     * Creates exception from SQLException
     */
    public static DatabaseException fromSqlException(SQLException sqlEx, String operation) {
        ErrorCode errorCode = mapSqlErrorCode(sqlEx.getErrorCode(), sqlEx.getSQLState());
        
        return builder(errorCode)
            .operation(operation)
            .message(sqlEx.getMessage())
            .cause(sqlEx)
            .build();
    }
    
    // ==================== GETTERS ====================
    
    public ErrorCode getErrorCode() { return errorCode; }
    public String getOperation() { return operation; }
    public String getTableName() { return tableName; }
    public String getQuery() { return query; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getContext() { return new HashMap<>(context); }
    public int getSqlErrorCode() { return sqlErrorCode; }
    public String getSqlState() { return sqlState; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    
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
     * Checks if this exception is transient (can be retried)
     */
    public boolean isTransient() {
        switch (errorCode) {
            case CONNECTION_TIMEOUT:
            case CONNECTION_POOL_EXHAUSTED:
            case DEADLOCK_DETECTED:
            case LOCK_TIMEOUT:
            case DISK_FULL:
            case UNKNOWN_ERROR:
                return true;
            case CONSTRAINT_VIOLATION:
            case FOREIGN_KEY_VIOLATION:
            case UNIQUE_CONSTRAINT_VIOLATION:
            case NOT_NULL_VIOLATION:
            case CHECK_CONSTRAINT_VIOLATION:
            case INVALID_SQL_SYNTAX:
            case TABLE_NOT_FOUND:
            case COLUMN_NOT_FOUND:
            case PERMISSION_DENIED:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this exception indicates a connection issue
     */
    public boolean isConnectionIssue() {
        switch (errorCode) {
            case CONNECTION_FAILED:
            case CONNECTION_TIMEOUT:
            case CONNECTION_POOL_EXHAUSTED:
            case DRIVER_ERROR:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this exception indicates a constraint violation
     */
    public boolean isConstraintViolation() {
        switch (errorCode) {
            case CONSTRAINT_VIOLATION:
            case FOREIGN_KEY_VIOLATION:
            case UNIQUE_CONSTRAINT_VIOLATION:
            case NOT_NULL_VIOLATION:
            case CHECK_CONSTRAINT_VIOLATION:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets recovery suggestions based on error type
     */
    public String getRecoverySuggestion() {
        switch (errorCode) {
            case CONNECTION_FAILED:
            case CONNECTION_TIMEOUT:
                return "Check database connectivity and retry the operation";
            case CONNECTION_POOL_EXHAUSTED:
                return "Wait for connections to be released or increase pool size";
            case DEADLOCK_DETECTED:
                return "Retry the transaction with appropriate backoff";
            case LOCK_TIMEOUT:
                return "Retry the operation or check for long-running transactions";
            case CONSTRAINT_VIOLATION:
            case FOREIGN_KEY_VIOLATION:
            case UNIQUE_CONSTRAINT_VIOLATION:
                return "Check data integrity and fix constraint violations";
            case PERMISSION_DENIED:
                return "Contact database administrator to grant necessary permissions";
            case TABLE_NOT_FOUND:
            case COLUMN_NOT_FOUND:
                return "Check database schema and ensure all migrations are applied";
            case INVALID_SQL_SYNTAX:
                return "Review and correct the SQL statement syntax";
            case DISK_FULL:
                return "Free up disk space or contact system administrator";
            default:
                return "Contact technical support for assistance";
        }
    }
    
    /**
     * Gets severity level of the exception
     */
    public Severity getSeverity() {
        switch (errorCode) {
            case CORRUPTION_DETECTED:
            case BACKUP_FAILED:
            case RESTORE_FAILED:
            case MIGRATION_FAILED:
                return Severity.CRITICAL;
            case CONNECTION_FAILED:
            case TRANSACTION_FAILED:
            case ROLLBACK_FAILED:
            case COMMIT_FAILED:
            case REPLICATION_ERROR:
                return Severity.HIGH;
            case DEADLOCK_DETECTED:
            case LOCK_TIMEOUT:
            case PERMISSION_DENIED:
            case CONSTRAINT_VIOLATION:
                return Severity.MEDIUM;
            case QUERY_EXECUTION_FAILED:
            case INVALID_SQL_SYNTAX:
            case TABLE_NOT_FOUND:
                return Severity.LOW;
            default:
                return Severity.MEDIUM;
        }
    }
    
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Builds a detailed error message with context
     */
    private static String buildDetailedMessage(ErrorCode errorCode, String operation, 
                                             String tableName, String customMessage) {
        StringBuilder message = new StringBuilder();
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append(customMessage);
        } else {
            message.append(errorCode.getDescription());
        }
        
        if (operation != null || tableName != null) {
            message.append(" [");
            boolean needsComma = false;
            
            if (operation != null) {
                message.append("Operation: ").append(operation);
                needsComma = true;
            }
            
            if (tableName != null) {
                if (needsComma) message.append(", ");
                message.append("Table: ").append(tableName);
            }
            
            message.append("]");
        }
        
        return message.toString();
    }
    
    /**
     * Maps SQL error codes to DatabaseException error codes
     */
    private static ErrorCode mapSqlErrorCode(int sqlErrorCode, String sqlState) {
        // PostgreSQL error codes
        if (sqlState != null) {
            switch (sqlState.substring(0, 2)) {
                case "08": return ErrorCode.CONNECTION_FAILED;
                case "23": return ErrorCode.CONSTRAINT_VIOLATION;
                case "42": return ErrorCode.INVALID_SQL_SYNTAX;
                case "53": return ErrorCode.DISK_FULL;
                case "57": return ErrorCode.PERMISSION_DENIED;
                case "40": return ErrorCode.DEADLOCK_DETECTED;
            }
        }
        
        // MySQL error codes
        switch (sqlErrorCode) {
            case 1044: case 1045: return ErrorCode.PERMISSION_DENIED;
            case 1062: return ErrorCode.UNIQUE_CONSTRAINT_VIOLATION;
            case 1146: return ErrorCode.TABLE_NOT_FOUND;
            case 1054: return ErrorCode.COLUMN_NOT_FOUND;
            case 1452: return ErrorCode.FOREIGN_KEY_VIOLATION;
            case 1213: return ErrorCode.DEADLOCK_DETECTED;
            case 1205: return ErrorCode.LOCK_TIMEOUT;
            default: return ErrorCode.UNKNOWN_ERROR;
        }
    }
    
    // ==================== SERIALIZATION SUPPORT ====================
    
    /**
     * Returns a detailed string representation for logging
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DatabaseException{");
        sb.append("errorCode=").append(errorCode);
        sb.append(", message='").append(getMessage()).append("'");
        
        if (operation != null) sb.append(", operation='").append(operation).append("'");
        if (tableName != null) sb.append(", tableName='").append(tableName).append("'");
        if (executionTimeMs > 0) sb.append(", executionTimeMs=").append(executionTimeMs);
        if (sqlErrorCode != 0) sb.append(", sqlErrorCode=").append(sqlErrorCode);
        if (sqlState != null) sb.append(", sqlState='").append(sqlState).append("'");
        
        sb.append(", timestamp=").append(timestamp);
        sb.append(", severity=").append(getSeverity());
        
        if (!context.isEmpty()) {
            sb.append(", context=").append(context);
        }
        
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append(": ").append(getCause().getMessage());
        }
        
        sb.append("}");
        return sb.toString();
    }
}