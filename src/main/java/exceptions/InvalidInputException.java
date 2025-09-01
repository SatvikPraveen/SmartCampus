// File: src/main/java/exceptions/InvalidInputException.java
package exceptions;

/**
 * Custom exception class for invalid input scenarios.
 * 
 * Key Java concepts covered:
 * - Custom exception creation
 * - Exception inheritance (extends RuntimeException)
 * - Constructor overloading
 * - Exception chaining (cause parameter)
 * - Serialization support
 */
public class InvalidInputException extends RuntimeException {
    
    // Serial version UID for serialization compatibility
    private static final long serialVersionUID = 1L;
    
    // Additional fields for more detailed error information
    private final String fieldName;
    private final Object invalidValue;
    private final String errorCode;
    
    /**
     * Default constructor.
     */
    public InvalidInputException() {
        super("Invalid input provided");
        this.fieldName = null;
        this.invalidValue = null;
        this.errorCode = "INVALID_INPUT";
    }
    
    /**
     * Constructor with custom message.
     * @param message The detail message
     */
    public InvalidInputException(String message) {
        super(message);
        this.fieldName = null;
        this.invalidValue = null;
        this.errorCode = "INVALID_INPUT";
    }
    
    /**
     * Constructor with message and cause.
     * @param message The detail message
     * @param cause The cause of this exception
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.invalidValue = null;
        this.errorCode = "INVALID_INPUT";
    }
    
    /**
     * Constructor with just the cause.
     * @param cause The cause of this exception
     */
    public InvalidInputException(Throwable cause) {
        super(cause);
        this.fieldName = null;
        this.invalidValue = null;
        this.errorCode = "INVALID_INPUT";
    }
    
    /**
     * Constructor with field name and invalid value.
     * @param fieldName The name of the invalid field
     * @param invalidValue The invalid value that was provided
     */
    public InvalidInputException(String fieldName, Object invalidValue) {
        super(String.format("Invalid value for field '%s': %s", fieldName, invalidValue));
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.errorCode = "INVALID_FIELD_VALUE";
    }
    
    /**
     * Constructor with field name, invalid value, and custom message.
     * @param fieldName The name of the invalid field
     * @param invalidValue The invalid value that was provided
     * @param message Additional detail message
     */
    public InvalidInputException(String fieldName, Object invalidValue, String message) {
        super(String.format("Invalid value for field '%s': %s. %s", fieldName, invalidValue, message));
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.errorCode = "INVALID_FIELD_VALUE";
    }
    
    /**
     * Constructor with error code, field name, and invalid value.
     * @param errorCode Specific error code for categorization
     * @param fieldName The name of the invalid field
     * @param invalidValue The invalid value that was provided
     * @param message Additional detail message
     */
    public InvalidInputException(String errorCode, String fieldName, Object invalidValue, String message) {
        super(String.format("[%s] Invalid value for field '%s': %s. %s", errorCode, fieldName, invalidValue, message));
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.errorCode = errorCode;
    }
    
    /**
     * Full constructor with all parameters.
     * @param errorCode Specific error code for categorization
     * @param fieldName The name of the invalid field
     * @param invalidValue The invalid value that was provided
     * @param message Additional detail message
     * @param cause The cause of this exception
     */
    public InvalidInputException(String errorCode, String fieldName, Object invalidValue, String message, Throwable cause) {
        super(String.format("[%s] Invalid value for field '%s': %s. %s", errorCode, fieldName, invalidValue, message), cause);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.errorCode = errorCode;
    }
    
    // Static factory methods for common scenarios
    
    /**
     * Creates an exception for null or empty field.
     * @param fieldName The name of the field that cannot be null/empty
     * @return InvalidInputException instance
     */
    public static InvalidInputException nullOrEmpty(String fieldName) {
        return new InvalidInputException("NULL_OR_EMPTY", fieldName, null, 
            "Field cannot be null or empty");
    }
    
    /**
     * Creates an exception for invalid format.
     * @param fieldName The name of the field with invalid format
     * @param value The invalid value
     * @param expectedFormat Description of expected format
     * @return InvalidInputException instance
     */
    public static InvalidInputException invalidFormat(String fieldName, Object value, String expectedFormat) {
        return new InvalidInputException("INVALID_FORMAT", fieldName, value, 
            "Expected format: " + expectedFormat);
    }
    
    /**
     * Creates an exception for value out of range.
     * @param fieldName The name of the field with out-of-range value
     * @param value The out-of-range value
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return InvalidInputException instance
     */
    public static InvalidInputException outOfRange(String fieldName, Object value, Object min, Object max) {
        return new InvalidInputException("OUT_OF_RANGE", fieldName, value, 
            String.format("Value must be between %s and %s", min, max));
    }
    
    /**
     * Creates an exception for invalid length.
     * @param fieldName The name of the field with invalid length
     * @param actualLength The actual length
     * @param minLength Minimum allowed length
     * @param maxLength Maximum allowed length
     * @return InvalidInputException instance
     */
    public static InvalidInputException invalidLength(String fieldName, int actualLength, int minLength, int maxLength) {
        return new InvalidInputException("INVALID_LENGTH", fieldName, actualLength, 
            String.format("Length must be between %d and %d characters", minLength, maxLength));
    }
    
    /**
     * Creates an exception for duplicate value.
     * @param fieldName The name of the field with duplicate value
     * @param value The duplicate value
     * @return InvalidInputException instance
     */
    public static InvalidInputException duplicateValue(String fieldName, Object value) {
        return new InvalidInputException("DUPLICATE_VALUE", fieldName, value, 
            "Value already exists");
    }
    
    /**
     * Creates an exception for invalid email format.
     * @param email The invalid email
     * @return InvalidInputException instance
     */
    public static InvalidInputException invalidEmail(String email) {
        return new InvalidInputException("INVALID_EMAIL", "email", email, 
            "Email must be in format: user@domain.com");
    }
    
    /**
     * Creates an exception for invalid phone number.
     * @param phoneNumber The invalid phone number
     * @return InvalidInputException instance
     */
    public static InvalidInputException invalidPhoneNumber(String phoneNumber) {
        return new InvalidInputException("INVALID_PHONE", "phoneNumber", phoneNumber, 
            "Phone number must be in format: (XXX) XXX-XXXX");
    }
    
    /**
     * Creates an exception for invalid GPA.
     * @param gpa The invalid GPA value
     * @return InvalidInputException instance
     */
    public static InvalidInputException invalidGPA(double gpa) {
        return new InvalidInputException("INVALID_GPA", "gpa", gpa, 
            "GPA must be between 0.0 and 4.0");
    }
    
    /**
     * Creates an exception for invalid course enrollment.
     * @param courseId The course ID
     * @param reason The reason for invalid enrollment
     * @return InvalidInputException instance
     */
    public static InvalidInputException invalidEnrollment(String courseId, String reason) {
        return new InvalidInputException("INVALID_ENROLLMENT", "courseId", courseId, reason);
    }
    
    // Getter methods for additional fields
    
    /**
     * Gets the name of the field that caused the validation error.
     * @return field name, or null if not specified
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Gets the invalid value that caused the error.
     * @return invalid value, or null if not specified
     */
    public Object getInvalidValue() {
        return invalidValue;
    }
    
    /**
     * Gets the error code for categorization.
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Checks if this exception has field-specific information.
     * @return true if field name is specified, false otherwise
     */
    public boolean hasFieldInfo() {
        return fieldName != null && !fieldName.trim().isEmpty();
    }
    
    /**
     * Gets a user-friendly error message.
     * @return formatted error message for end users
     */
    public String getUserFriendlyMessage() {
        if (hasFieldInfo()) {
            return String.format("Invalid input for %s. Please check your entry and try again.", fieldName);
        }
        return "Invalid input provided. Please check your entries and try again.";
    }
    
    /**
     * Gets a detailed error report.
     * @return detailed error information for debugging
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("InvalidInputException Details:\n");
        report.append("Error Code: ").append(errorCode).append("\n");
        report.append("Message: ").append(getMessage()).append("\n");
        
        if (fieldName != null) {
            report.append("Field Name: ").append(fieldName).append("\n");
        }
        
        if (invalidValue != null) {
            report.append("Invalid Value: ").append(invalidValue).append("\n");
            report.append("Value Type: ").append(invalidValue.getClass().getSimpleName()).append("\n");
        }
        
        if (getCause() != null) {
            report.append("Cause: ").append(getCause().getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Creates a copy of this exception with additional context.
     * @param additionalContext Additional context information
     * @return new InvalidInputException with added context
     */
    public InvalidInputException withContext(String additionalContext) {
        String newMessage = getMessage() + " Context: " + additionalContext;
        return new InvalidInputException(errorCode, fieldName, invalidValue, newMessage, getCause());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(": ");
        
        if (errorCode != null && !errorCode.equals("INVALID_INPUT")) {
            sb.append("[").append(errorCode).append("] ");
        }
        
        sb.append(getMessage());
        
        return sb.toString();
    }
}