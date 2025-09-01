// File location: src/main/java/exceptions/ValidationException.java
package exceptions;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exception class for data validation errors in the campus management system
 * Provides detailed validation error information with field-level details
 */
public class ValidationException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    // Error codes for different validation issues
    public enum ErrorCode {
        REQUIRED_FIELD_MISSING("VAL_001", "Required field is missing"),
        INVALID_FORMAT("VAL_002", "Field value has invalid format"),
        INVALID_LENGTH("VAL_003", "Field value length is invalid"),
        INVALID_RANGE("VAL_004", "Field value is outside valid range"),
        INVALID_EMAIL("VAL_005", "Email format is invalid"),
        INVALID_PHONE("VAL_006", "Phone number format is invalid"),
        INVALID_DATE("VAL_007", "Date format or value is invalid"),
        INVALID_URL("VAL_008", "URL format is invalid"),
        INVALID_ENUM_VALUE("VAL_009", "Value is not a valid enum option"),
        DUPLICATE_VALUE("VAL_010", "Duplicate value found"),
        INVALID_REFERENCE("VAL_011", "Referenced entity does not exist"),
        CIRCULAR_REFERENCE("VAL_012", "Circular reference detected"),
        INVALID_JSON("VAL_013", "JSON format is invalid"),
        INVALID_XML("VAL_014", "XML format is invalid"),
        INVALID_REGEX("VAL_015", "Value does not match required pattern"),
        INVALID_CREDIT_CARD("VAL_016", "Credit card number is invalid"),
        INVALID_SSN("VAL_017", "Social Security Number is invalid"),
        INVALID_PASSWORD("VAL_018", "Password does not meet requirements"),
        INVALID_USERNAME("VAL_019", "Username format is invalid"),
        INVALID_FILE_TYPE("VAL_020", "File type is not allowed"),
        FILE_TOO_LARGE("VAL_021", "File size exceeds maximum limit"),
        INVALID_IMAGE("VAL_022", "Image file is invalid or corrupted"),
        INVALID_CURRENCY("VAL_023", "Currency format is invalid"),
        INVALID_PERCENTAGE("VAL_024", "Percentage value is invalid"),
        INVALID_COORDINATE("VAL_025", "Geographic coordinate is invalid"),
        INVALID_TIME_ZONE("VAL_026", "Time zone is invalid"),
        INVALID_LOCALE("VAL_027", "Locale is invalid"),
        BUSINESS_RULE_VIOLATION("VAL_028", "Business rule validation failed"),
        CROSS_FIELD_VALIDATION("VAL_029", "Cross-field validation failed"),
        CONDITIONAL_VALIDATION("VAL_030", "Conditional validation failed"),
        CUSTOM_VALIDATION("VAL_031", "Custom validation rule failed"),
        AGGREGATION_VALIDATION("VAL_032", "Aggregation validation failed"),
        TEMPORAL_VALIDATION("VAL_033", "Temporal validation failed"),
        SECURITY_VALIDATION("VAL_034", "Security validation failed"),
        PERFORMANCE_VALIDATION("VAL_035", "Performance validation failed"),
        UNKNOWN_VALIDATION_ERROR("VAL_999", "Unknown validation error");
        
        private final String code;
        private final String description;
        
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    // Validation error detail class
    public static class ValidationError {
        private final String fieldName;
        private final Object rejectedValue;
        private final ErrorCode errorCode;
        private final String message;
        private final Map<String, Object> context;
        
        public ValidationError(String fieldName, Object rejectedValue, ErrorCode errorCode, String message) {
            this.fieldName = fieldName;
            this.rejectedValue = rejectedValue;
            this.errorCode = errorCode;
            this.message = message;
            this.context = new HashMap<>();
        }
        
        public ValidationError(String fieldName, Object rejectedValue, ErrorCode errorCode, 
                             String message, Map<String, Object> context) {
            this.fieldName = fieldName;
            this.rejectedValue = rejectedValue;
            this.errorCode = errorCode;
            this.message = message;
            this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        }
        
        // Getters
        public String getFieldName() { return fieldName; }
        public Object getRejectedValue() { return rejectedValue; }
        public ErrorCode getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public Map<String, Object> getContext() { return new HashMap<>(context); }
        
        public Object getContext(String key) { return context.get(key); }
        
        @SuppressWarnings("unchecked")
        public <T> T getContext(String key, Class<T> type) {
            Object value = context.get(key);
            return (value != null && type.isInstance(value)) ? (T) value : null;
        }
        
        @Override
        public String toString() {
            return String.format("ValidationError{field='%s', value=%s, code=%s, message='%s'}", 
                               fieldName, rejectedValue, errorCode, message);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            ValidationError that = (ValidationError) obj;
            return Objects.equals(fieldName, that.fieldName) &&
                   Objects.equals(errorCode, that.errorCode) &&
                   Objects.equals(rejectedValue, that.rejectedValue);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(fieldName, errorCode, rejectedValue);
        }
    }
    
    // Exception properties
    private final List<ValidationError> validationErrors;
    private final String objectName;
    private final LocalDateTime timestamp;
    private final Map<String, Object> globalContext;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new ValidationException with a single validation error
     */
    public ValidationException(String fieldName, Object rejectedValue, ErrorCode errorCode, String message) {
        super(buildMessage(Collections.singletonList(
            new ValidationError(fieldName, rejectedValue, errorCode, message))));
        this.validationErrors = Collections.singletonList(
            new ValidationError(fieldName, rejectedValue, errorCode, message));
        this.objectName = null;
        this.timestamp = LocalDateTime.now();
        this.globalContext = new HashMap<>();
    }
    
    /**
     * Creates a new ValidationException with multiple validation errors
     */
    public ValidationException(List<ValidationError> validationErrors) {
        super(buildMessage(validationErrors));
        this.validationErrors = validationErrors != null ? 
            new ArrayList<>(validationErrors) : new ArrayList<>();
        this.objectName = null;
        this.timestamp = LocalDateTime.now();
        this.globalContext = new HashMap<>();
    }
    
    /**
     * Creates a new ValidationException with object context
     */
    public ValidationException(String objectName, List<ValidationError> validationErrors) {
        super(buildMessage(objectName, validationErrors));
        this.validationErrors = validationErrors != null ? 
            new ArrayList<>(validationErrors) : new ArrayList<>();
        this.objectName = objectName;
        this.timestamp = LocalDateTime.now();
        this.globalContext = new HashMap<>();
    }
    
    /**
     * Creates a new ValidationException with full context
     */
    public ValidationException(String objectName, List<ValidationError> validationErrors, 
                             Map<String, Object> globalContext, Throwable cause) {
        super(buildMessage(objectName, validationErrors), cause);
        this.validationErrors = validationErrors != null ? 
            new ArrayList<>(validationErrors) : new ArrayList<>();
        this.objectName = objectName;
        this.timestamp = LocalDateTime.now();
        this.globalContext = globalContext != null ? 
            new HashMap<>(globalContext) : new HashMap<>();
    }
    
    // ==================== BUILDER PATTERN ====================
    
    /**
     * Builder for creating ValidationException with fluent interface
     */
    public static class Builder {
        private String objectName;
        private final List<ValidationError> validationErrors = new ArrayList<>();
        private final Map<String, Object> globalContext = new HashMap<>();
        private Throwable cause;
        
        public Builder() {}
        
        public Builder(String objectName) {
            this.objectName = objectName;
        }
        
        public Builder objectName(String objectName) {
            this.objectName = objectName;
            return this;
        }
        
        public Builder addError(String fieldName, Object rejectedValue, ErrorCode errorCode, String message) {
            this.validationErrors.add(new ValidationError(fieldName, rejectedValue, errorCode, message));
            return this;
        }
        
        public Builder addError(ValidationError error) {
            this.validationErrors.add(error);
            return this;
        }
        
        public Builder addErrors(List<ValidationError> errors) {
            this.validationErrors.addAll(errors);
            return this;
        }
        
        public Builder addGlobalContext(String key, Object value) {
            this.globalContext.put(key, value);
            return this;
        }
        
        public Builder addGlobalContext(Map<String, Object> context) {
            this.globalContext.putAll(context);
            return this;
        }
        
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }
        
        public ValidationException build() {
            return new ValidationException(objectName, validationErrors, globalContext, cause);
        }
    }
    
    /**
     * Creates a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a new builder with object name
     */
    public static Builder builder(String objectName) {
        return new Builder(objectName);
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Creates exception for required field missing
     */
    public static ValidationException requiredField(String fieldName) {
        return new ValidationException(fieldName, null, ErrorCode.REQUIRED_FIELD_MISSING, 
                                     "Field '" + fieldName + "' is required");
    }
    
    /**
     * Creates exception for invalid format
     */
    public static ValidationException invalidFormat(String fieldName, Object value, String expectedFormat) {
        Map<String, Object> context = new HashMap<>();
        context.put("expectedFormat", expectedFormat);
        
        ValidationError error = new ValidationError(fieldName, value, ErrorCode.INVALID_FORMAT,
            "Field '" + fieldName + "' has invalid format. Expected: " + expectedFormat, context);
        
        return new ValidationException(Collections.singletonList(error));
    }
    
    /**
     * Creates exception for invalid length
     */
    public static ValidationException invalidLength(String fieldName, Object value, 
                                                  int minLength, int maxLength) {
        Map<String, Object> context = new HashMap<>();
        context.put("minLength", minLength);
        context.put("maxLength", maxLength);
        context.put("actualLength", value != null ? value.toString().length() : 0);
        
        String message = String.format("Field '%s' length must be between %d and %d characters", 
                                     fieldName, minLength, maxLength);
        
        ValidationError error = new ValidationError(fieldName, value, ErrorCode.INVALID_LENGTH, message, context);
        return new ValidationException(Collections.singletonList(error));
    }
    
    /**
     * Creates exception for invalid range
     */
    public static ValidationException invalidRange(String fieldName, Object value, 
                                                 Object minValue, Object maxValue) {
        Map<String, Object> context = new HashMap<>();
        context.put("minValue", minValue);
        context.put("maxValue", maxValue);
        
        String message = String.format("Field '%s' must be between %s and %s", 
                                     fieldName, minValue, maxValue);
        
        ValidationError error = new ValidationError(fieldName, value, ErrorCode.INVALID_RANGE, message, context);
        return new ValidationException(Collections.singletonList(error));
    }
    
    /**
     * Creates exception for invalid email
     */
    public static ValidationException invalidEmail(String fieldName, String email) {
        return new ValidationException(fieldName, email, ErrorCode.INVALID_EMAIL,
                                     "Field '" + fieldName + "' must be a valid email address");
    }
    
    /**
     * Creates exception for invalid phone number
     */
    public static ValidationException invalidPhone(String fieldName, String phone) {
        return new ValidationException(fieldName, phone, ErrorCode.INVALID_PHONE,
                                     "Field '" + fieldName + "' must be a valid phone number");
    }
    
    /**
     * Creates exception for duplicate value
     */
    public static ValidationException duplicateValue(String fieldName, Object value, String entity) {
        Map<String, Object> context = new HashMap<>();
        context.put("entity", entity);
        
        String message = String.format("Value '%s' already exists for field '%s' in %s", 
                                     value, fieldName, entity);
        
        ValidationError error = new ValidationError(fieldName, value, ErrorCode.DUPLICATE_VALUE, message, context);
        return new ValidationException(Collections.singletonList(error));
    }
    
    /**
     * Creates exception for invalid reference
     */
    public static ValidationException invalidReference(String fieldName, Object referenceId, 
                                                     String referencedEntity) {
        Map<String, Object> context = new HashMap<>();
        context.put("referencedEntity", referencedEntity);
        
        String message = String.format("Referenced %s with ID '%s' does not exist", 
                                     referencedEntity, referenceId);
        
        ValidationError error = new ValidationError(fieldName, referenceId, ErrorCode.INVALID_REFERENCE, 
                                                   message, context);
        return new ValidationException(Collections.singletonList(error));
    }
    
    /**
     * Creates exception for business rule violation
     */
    public static ValidationException businessRuleViolation(String ruleName, String description) {
        ValidationError error = new ValidationError(null, null, ErrorCode.BUSINESS_RULE_VIOLATION,
            "Business rule violation: " + ruleName + " - " + description);
        
        Builder builder = builder().addError(error);
        builder.addGlobalContext("ruleName", ruleName);
        builder.addGlobalContext("ruleDescription", description);
        
        return builder.build();
    }
    
    /**
     * Creates exception for cross-field validation
     */
    public static ValidationException crossFieldValidation(String[] fieldNames, String description) {
        Map<String, Object> context = new HashMap<>();
        context.put("relatedFields", Arrays.asList(fieldNames));
        
        ValidationError error = new ValidationError(String.join(",", fieldNames), null, 
            ErrorCode.CROSS_FIELD_VALIDATION, "Cross-field validation failed: " + description, context);
        
        return new ValidationException(Collections.singletonList(error));
    }
    
    // ==================== GETTERS ====================
    
    public List<ValidationError> getValidationErrors() { 
        return new ArrayList<>(validationErrors); 
    }
    
    public String getObjectName() { return objectName; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getGlobalContext() { return new HashMap<>(globalContext); }
    
    /**
     * Gets errors for a specific field
     */
    public List<ValidationError> getErrorsForField(String fieldName) {
        return validationErrors.stream()
            .filter(error -> Objects.equals(error.getFieldName(), fieldName))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets errors by error code
     */
    public List<ValidationError> getErrorsByCode(ErrorCode errorCode) {
        return validationErrors.stream()
            .filter(error -> error.getErrorCode() == errorCode)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all field names with errors
     */
    public Set<String> getFieldsWithErrors() {
        return validationErrors.stream()
            .map(ValidationError::getFieldName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    /**
     * Gets all error codes present
     */
    public Set<ErrorCode> getErrorCodes() {
        return validationErrors.stream()
            .map(ValidationError::getErrorCode)
            .collect(Collectors.toSet());
    }
    
    /**
     * Gets specific global context value
     */
    public Object getGlobalContext(String key) {
        return globalContext.get(key);
    }
    
    /**
     * Gets global context value as specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getGlobalContext(String key, Class<T> type) {
        Object value = globalContext.get(key);
        return (value != null && type.isInstance(value)) ? (T) value : null;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if there are any validation errors
     */
    public boolean hasErrors() {
        return !validationErrors.isEmpty();
    }
    
    /**
     * Checks if there are errors for a specific field
     */
    public boolean hasErrorsForField(String fieldName) {
        return validationErrors.stream()
            .anyMatch(error -> Objects.equals(error.getFieldName(), fieldName));
    }
    
    /**
     * Checks if there are errors of a specific type
     */
    public boolean hasErrorsOfType(ErrorCode errorCode) {
        return validationErrors.stream()
            .anyMatch(error -> error.getErrorCode() == errorCode);
    }
    
    /**
     * Gets count of validation errors
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
    
    /**
     * Gets count of errors for a specific field
     */
    public int getErrorCountForField(String fieldName) {
        return (int) validationErrors.stream()
            .filter(error -> Objects.equals(error.getFieldName(), fieldName))
            .count();
    }
    
    /**
     * Gets user-friendly error messages
     */
    public List<String> getUserFriendlyMessages() {
        return validationErrors.stream()
            .map(ValidationError::getMessage)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets user-friendly error messages for a specific field
     */
    public List<String> getUserFriendlyMessages(String fieldName) {
        return getErrorsForField(fieldName).stream()
            .map(ValidationError::getMessage)
            .collect(Collectors.toList());
    }
    
    /**
     * Creates a map of field names to error messages
     */
    public Map<String, List<String>> getFieldErrorMessages() {
        return validationErrors.stream()
            .filter(error -> error.getFieldName() != null)
            .collect(Collectors.groupingBy(
                ValidationError::getFieldName,
                Collectors.mapping(ValidationError::getMessage, Collectors.toList())
            ));
    }
    
    /**
     * Merges this exception with another ValidationException
     */
    public ValidationException merge(ValidationException other) {
        if (other == null) return this;
        
        Builder builder = builder(this.objectName);
        builder.addErrors(this.validationErrors);
        builder.addErrors(other.validationErrors);
        builder.addGlobalContext(this.globalContext);
        builder.addGlobalContext(other.globalContext);
        
        return builder.build();
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Builds the exception message from validation errors
     */
    private static String buildMessage(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }
        
        if (errors.size() == 1) {
            return "Validation failed: " + errors.get(0).getMessage();
        }
        
        return String.format("Validation failed with %d errors: %s", 
                           errors.size(),
                           errors.stream()
                               .map(ValidationError::getMessage)
                               .collect(Collectors.joining("; ")));
    }
    
    /**
     * Builds the exception message with object context
     */
    private static String buildMessage(String objectName, List<ValidationError> errors) {
        String baseMessage = buildMessage(errors);
        
        if (objectName != null && !objectName.trim().isEmpty()) {
            return "Validation failed for " + objectName + ": " + 
                   baseMessage.substring("Validation failed".length());
        }
        
        return baseMessage;
    }
    
    // ==================== SERIALIZATION SUPPORT ====================
    
    /**
     * Creates a structured representation for JSON serialization
     */
    public Map<String, Object> toStructuredMap() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("objectName", objectName);
        result.put("timestamp", timestamp.toString());
        result.put("errorCount", validationErrors.size());
        result.put("message", getMessage());
        
        // Add field errors
        Map<String, List<Map<String, Object>>> fieldErrors = new HashMap<>();
        for (ValidationError error : validationErrors) {
            String fieldName = error.getFieldName() != null ? error.getFieldName() : "global";
            
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("code", error.getErrorCode().getCode());
            errorMap.put("message", error.getMessage());
            errorMap.put("rejectedValue", error.getRejectedValue());
            errorMap.put("context", error.getContext());
            
            fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMap);
        }
        result.put("fieldErrors", fieldErrors);
        
        // Add global context
        if (!globalContext.isEmpty()) {
            result.put("globalContext", globalContext);
        }
        
        return result;
    }
    
    /**
     * Returns a detailed string representation for logging
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationException{");
        sb.append("objectName='").append(objectName).append("'");
        sb.append(", errorCount=").append(validationErrors.size());
        sb.append(", timestamp=").append(timestamp);
        
        if (!validationErrors.isEmpty()) {
            sb.append(", errors=[");
            sb.append(validationErrors.stream()
                .map(ValidationError::toString)
                .collect(Collectors.joining(", ")));
            sb.append("]");
        }
        
        if (!globalContext.isEmpty()) {
            sb.append(", globalContext=").append(globalContext);
        }
        
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append(": ").append(getCause().getMessage());
        }
        
        sb.append("}");
        return sb.toString();
    }
}