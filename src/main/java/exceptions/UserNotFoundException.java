// File: src/main/java/exceptions/UserNotFoundException.java
package exceptions;

/**
 * Custom exception class for user not found scenarios.
 * 
 * Key Java concepts covered:
 * - Custom exception creation
 * - Exception inheritance (extends RuntimeException)
 * - Constructor overloading
 * - Static factory methods
 * - Exception chaining
 * - Business-specific exception handling
 */
public class UserNotFoundException extends RuntimeException {
    
    // Serial version UID for serialization compatibility
    private static final long serialVersionUID = 1L;
    
    // Additional fields for context
    private final String userId;
    private final String userType;
    private final String searchCriteria;
    
    /**
     * Default constructor.
     */
    public UserNotFoundException() {
        super("User not found");
        this.userId = null;
        this.userType = null;
        this.searchCriteria = null;
    }
    
    /**
     * Constructor with custom message.
     * @param message The detail message
     */
    public UserNotFoundException(String message) {
        super(message);
        this.userId = null;
        this.userType = null;
        this.searchCriteria = null;
    }
    
    /**
     * Constructor with message and cause.
     * @param message The detail message
     * @param cause The cause of this exception
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.userId = null;
        this.userType = null;
        this.searchCriteria = null;
    }
    
    /**
     * Constructor with user ID.
     * @param userId The ID of the user that was not found
     */
    public UserNotFoundException(String userId) {
        super("User not found with ID: " + userId);
        this.userId = userId;
        this.userType = null;
        this.searchCriteria = userId;
    }
    
    /**
     * Constructor with user ID and user type.
     * @param userId The ID of the user that was not found
     * @param userType The type of user (Student, Professor, etc.)
     */
    public UserNotFoundException(String userId, String userType) {
        super(String.format("%s not found with ID: %s", userType, userId));
        this.userId = userId;
        this.userType = userType;
        this.searchCriteria = userId;
    }
    
    /**
     * Constructor with user ID, user type, and custom message.
     * @param userId The ID of the user that was not found
     * @param userType The type of user
     * @param message Additional detail message
     */
    public UserNotFoundException(String userId, String userType, String message) {
        super(String.format("%s not found with ID: %s. %s", userType, userId, message));
        this.userId = userId;
        this.userType = userType;
        this.searchCriteria = userId;
    }
    
    /**
     * Full constructor with all context information.
     * @param userId The ID of the user that was not found
     * @param userType The type of user
     * @param searchCriteria The criteria used to search for the user
     * @param message Additional detail message
     */
    public UserNotFoundException(String userId, String userType, String searchCriteria, String message) {
        super(String.format("%s not found. Search criteria: %s. %s", userType, searchCriteria, message));
        this.userId = userId;
        this.userType = userType;
        this.searchCriteria = searchCriteria;
    }
    
    /**
     * Constructor with cause.
     * @param userId The ID of the user that was not found
     * @param userType The type of user
     * @param cause The cause of this exception
     */
    public UserNotFoundException(String userId, String userType, Throwable cause) {
        super(String.format("%s not found with ID: %s", userType, userId), cause);
        this.userId = userId;
        this.userType = userType;
        this.searchCriteria = userId;
    }
    
    // Static factory methods for common scenarios
    
    /**
     * Creates exception for student not found.
     * @param studentId The student ID that was not found
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException student(String studentId) {
        return new UserNotFoundException(studentId, "Student");
    }
    
    /**
     * Creates exception for professor not found.
     * @param professorId The professor ID that was not found
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException professor(String professorId) {
        return new UserNotFoundException(professorId, "Professor");
    }
    
    /**
     * Creates exception for admin not found.
     * @param adminId The admin ID that was not found
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException admin(String adminId) {
        return new UserNotFoundException(adminId, "Administrator");
    }
    
    /**
     * Creates exception for user not found by email.
     * @param email The email that was searched
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException(null, "User", "email: " + email, 
            "No user found with the provided email address.");
    }
    
    /**
     * Creates exception for user not found by username.
     * @param username The username that was searched
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException(null, "User", "username: " + username, 
            "No user found with the provided username.");
    }
    
    /**
     * Creates exception for inactive user.
     * @param userId The user ID
     * @param userType The type of user
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException inactive(String userId, String userType) {
        return new UserNotFoundException(userId, userType, 
            "User account is inactive or has been deactivated.");
    }
    
    /**
     * Creates exception for user not found during authentication.
     * @param credentials The credentials used (without sensitive info)
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException duringAuthentication(String credentials) {
        return new UserNotFoundException(null, "User", credentials, 
            "Authentication failed - user not found or credentials invalid.");
    }
    
    /**
     * Creates exception for user not found during enrollment.
     * @param studentId The student ID
     * @param courseId The course ID for context
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException duringEnrollment(String studentId, String courseId) {
        return new UserNotFoundException(studentId, "Student", 
            String.format("student ID: %s for course: %s", studentId, courseId),
            "Cannot enroll - student not found in the system.");
    }
    
    /**
     * Creates exception for multiple users search returning no results.
     * @param searchQuery The search query used
     * @param userType The type of users searched for
     * @return UserNotFoundException instance
     */
    public static UserNotFoundException noSearchResults(String searchQuery, String userType) {
        return new UserNotFoundException(null, userType, 
            "search query: " + searchQuery, 
            "No users found matching the search criteria.");
    }
    
    // Getter methods
    
    /**
     * Gets the user ID that was not found.
     * @return user ID, or null if not specified
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Gets the type of user that was not found.
     * @return user type, or null if not specified
     */
    public String getUserType() {
        return userType;
    }
    
    /**
     * Gets the search criteria that was used.
     * @return search criteria, or null if not specified
     */
    public String getSearchCriteria() {
        return searchCriteria;
    }
    
    /**
     * Checks if this exception has user ID information.
     * @return true if user ID is specified, false otherwise
     */
    public boolean hasUserId() {
        return userId != null && !userId.trim().isEmpty();
    }
    
    /**
     * Checks if this exception has user type information.
     * @return true if user type is specified, false otherwise
     */
    public boolean hasUserType() {
        return userType != null && !userType.trim().isEmpty();
    }
    
    /**
     * Gets a user-friendly error message.
     * @return formatted error message for end users
     */
    public String getUserFriendlyMessage() {
        if (hasUserType() && hasUserId()) {
            return String.format("The %s with ID '%s' could not be found. Please verify the ID and try again.", 
                userType.toLowerCase(), userId);
        } else if (hasUserType()) {
            return String.format("The requested %s could not be found. Please check your search criteria.", 
                userType.toLowerCase());
        } else {
            return "The requested user could not be found. Please verify your information and try again.";
        }
    }
    
    /**
     * Gets suggestions for resolving the issue.
     * @return list of suggestions as a formatted string
     */
    public String getSuggestions() {
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("Suggestions to resolve this issue:\n");
        
        if (hasUserId()) {
            suggestions.append("• Verify that the ID '").append(userId).append("' is correct\n");
            suggestions.append("• Check if the user account is active\n");
        }
        
        if (hasUserType()) {
            suggestions.append("• Ensure you're searching in the correct user category (").append(userType).append(")\n");
        }
        
        suggestions.append("• Contact system administrator if the user should exist\n");
        suggestions.append("• Try searching with different criteria (email, name, etc.)\n");
        
        return suggestions.toString();
    }
    
    /**
     * Gets a detailed error report for debugging.
     * @return detailed error information
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("UserNotFoundException Details:\n");
        report.append("Message: ").append(getMessage()).append("\n");
        
        if (userId != null) {
            report.append("User ID: ").append(userId).append("\n");
        }
        
        if (userType != null) {
            report.append("User Type: ").append(userType).append("\n");
        }
        
        if (searchCriteria != null) {
            report.append("Search Criteria: ").append(searchCriteria).append("\n");
        }
        
        if (getCause() != null) {
            report.append("Cause: ").append(getCause().getMessage()).append("\n");
        }
        
        report.append("\nTimestamp: ").append(new java.util.Date()).append("\n");
        
        return report.toString();
    }
    
    /**
     * Creates a copy of this exception with additional context.
     * @param additionalContext Additional context information
     * @return new UserNotFoundException with added context
     */
    public UserNotFoundException withContext(String additionalContext) {
        String newMessage = getMessage() + " Additional context: " + additionalContext;
        UserNotFoundException newException = new UserNotFoundException(newMessage, getCause());
        return newException;
    }
    
    /**
     * Checks if this exception is related to authentication.
     * @return true if the message contains authentication-related keywords
     */
    public boolean isAuthenticationRelated() {
        String message = getMessage().toLowerCase();
        return message.contains("authentication") || 
               message.contains("login") || 
               message.contains("credentials");
    }
    
    /**
     * Checks if this exception is related to enrollment.
     * @return true if the message contains enrollment-related keywords
     */
    public boolean isEnrollmentRelated() {
        String message = getMessage().toLowerCase();
        return message.contains("enrollment") || 
               message.contains("enroll") || 
               message.contains("course");
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(": ");
        sb.append(getMessage());
        
        if (hasUserId() || hasUserType()) {
            sb.append(" [");
            if (hasUserType()) {
                sb.append("Type: ").append(userType);
            }
            if (hasUserId()) {
                if (hasUserType()) sb.append(", ");
                sb.append("ID: ").append(userId);
            }
            sb.append("]");
        }
        
        return sb.toString();
    }
}