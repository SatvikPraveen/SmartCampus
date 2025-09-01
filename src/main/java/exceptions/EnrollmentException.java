// File location: src/main/java/exceptions/EnrollmentException.java
package exceptions;

import enums.EnrollmentStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Exception class for enrollment-related errors in the campus management system
 * Provides detailed error information for enrollment operations
 */
public class EnrollmentException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    // Error codes for different enrollment issues
    public enum ErrorCode {
        COURSE_FULL("ENROLL_001", "Course has reached maximum enrollment capacity"),
        PREREQUISITE_NOT_MET("ENROLL_002", "Student has not met course prerequisites"),
        SCHEDULE_CONFLICT("ENROLL_003", "Course schedule conflicts with existing enrollment"),
        DUPLICATE_ENROLLMENT("ENROLL_004", "Student is already enrolled in this course"),
        ENROLLMENT_CLOSED("ENROLL_005", "Enrollment period has closed"),
        INVALID_SEMESTER("ENROLL_006", "Invalid semester for enrollment"),
        CREDIT_LIMIT_EXCEEDED("ENROLL_007", "Enrollment would exceed maximum credit hours"),
        INVALID_STUDENT_STATUS("ENROLL_008", "Student status does not allow enrollment"),
        COURSE_NOT_AVAILABLE("ENROLL_009", "Course is not available for enrollment"),
        WAITLIST_FULL("ENROLL_010", "Course waitlist is full"),
        PAYMENT_REQUIRED("ENROLL_011", "Payment required before enrollment"),
        HOLD_ON_ACCOUNT("ENROLL_012", "Academic or financial hold prevents enrollment"),
        INVALID_ENROLLMENT_STATUS("ENROLL_013", "Invalid enrollment status transition"),
        DEADLINE_PASSED("ENROLL_014", "Enrollment deadline has passed"),
        INSTRUCTOR_APPROVAL_REQUIRED("ENROLL_015", "Instructor approval required for enrollment"),
        GRADE_LEVEL_RESTRICTION("ENROLL_016", "Course restricted to specific grade levels"),
        DEPARTMENT_PERMISSION_REQUIRED("ENROLL_017", "Department permission required"),
        CONCURRENT_ENROLLMENT_LIMIT("ENROLL_018", "Concurrent enrollment limit exceeded"),
        ACADEMIC_PROBATION_RESTRICTION("ENROLL_019", "Academic probation prevents enrollment"),
        SYSTEM_ERROR("ENROLL_999", "System error during enrollment process");
        
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
    private final String studentId;
    private final String courseId;
    private final String semester;
    private final LocalDateTime timestamp;
    private final Map<String, Object> additionalData;
    private final EnrollmentStatus currentStatus;
    private final EnrollmentStatus attemptedStatus;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates a new EnrollmentException with error code and message
     */
    public EnrollmentException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.getDescription());
        this.errorCode = errorCode;
        this.studentId = null;
        this.courseId = null;
        this.semester = null;
        this.timestamp = LocalDateTime.now();
        this.additionalData = new HashMap<>();
        this.currentStatus = null;
        this.attemptedStatus = null;
    }
    
    /**
     * Creates a new EnrollmentException with error code, message, and cause
     */
    public EnrollmentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.getDescription(), cause);
        this.errorCode = errorCode;
        this.studentId = null;
        this.courseId = null;
        this.semester = null;
        this.timestamp = LocalDateTime.now();
        this.additionalData = new HashMap<>();
        this.currentStatus = null;
        this.attemptedStatus = null;
    }
    
    /**
     * Creates a new EnrollmentException with detailed enrollment context
     */
    public EnrollmentException(ErrorCode errorCode, String studentId, String courseId, 
                             String semester, String message) {
        super(buildDetailedMessage(errorCode, studentId, courseId, semester, message));
        this.errorCode = errorCode;
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.timestamp = LocalDateTime.now();
        this.additionalData = new HashMap<>();
        this.currentStatus = null;
        this.attemptedStatus = null;
    }
    
    /**
     * Creates a new EnrollmentException with full context including status information
     */
    public EnrollmentException(ErrorCode errorCode, String studentId, String courseId, 
                             String semester, EnrollmentStatus currentStatus, 
                             EnrollmentStatus attemptedStatus, String message, Throwable cause) {
        super(buildDetailedMessage(errorCode, studentId, courseId, semester, message), cause);
        this.errorCode = errorCode;
        this.studentId = studentId;
        this.courseId = courseId;
        this.semester = semester;
        this.timestamp = LocalDateTime.now();
        this.additionalData = new HashMap<>();
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
    
    // ==================== BUILDER PATTERN ====================
    
    /**
     * Builder for creating EnrollmentException with fluent interface
     */
    public static class Builder {
        private ErrorCode errorCode;
        private String message;
        private Throwable cause;
        private String studentId;
        private String courseId;
        private String semester;
        private EnrollmentStatus currentStatus;
        private EnrollmentStatus attemptedStatus;
        private final Map<String, Object> additionalData = new HashMap<>();
        
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
        
        public Builder studentId(String studentId) {
            this.studentId = studentId;
            return this;
        }
        
        public Builder courseId(String courseId) {
            this.courseId = courseId;
            return this;
        }
        
        public Builder semester(String semester) {
            this.semester = semester;
            return this;
        }
        
        public Builder currentStatus(EnrollmentStatus currentStatus) {
            this.currentStatus = currentStatus;
            return this;
        }
        
        public Builder attemptedStatus(EnrollmentStatus attemptedStatus) {
            this.attemptedStatus = attemptedStatus;
            return this;
        }
        
        public Builder addData(String key, Object value) {
            this.additionalData.put(key, value);
            return this;
        }
        
        public Builder addData(Map<String, Object> data) {
            this.additionalData.putAll(data);
            return this;
        }
        
        public EnrollmentException build() {
            EnrollmentException exception = new EnrollmentException(
                errorCode, studentId, courseId, semester, currentStatus, 
                attemptedStatus, message, cause
            );
            exception.additionalData.putAll(this.additionalData);
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
     * Creates exception for course at capacity
     */
    public static EnrollmentException courseFull(String studentId, String courseId, int maxEnrollment) {
        return builder(ErrorCode.COURSE_FULL)
            .studentId(studentId)
            .courseId(courseId)
            .addData("maxEnrollment", maxEnrollment)
            .message("Course " + courseId + " is at maximum capacity (" + maxEnrollment + " students)")
            .build();
    }
    
    /**
     * Creates exception for unmet prerequisites
     */
    public static EnrollmentException prerequisiteNotMet(String studentId, String courseId, 
                                                       String missingPrerequisite) {
        return builder(ErrorCode.PREREQUISITE_NOT_MET)
            .studentId(studentId)
            .courseId(courseId)
            .addData("missingPrerequisite", missingPrerequisite)
            .message("Student " + studentId + " has not completed prerequisite: " + missingPrerequisite)
            .build();
    }
    
    /**
     * Creates exception for schedule conflicts
     */
    public static EnrollmentException scheduleConflict(String studentId, String courseId, 
                                                     String conflictingCourseId, String timeSlot) {
        return builder(ErrorCode.SCHEDULE_CONFLICT)
            .studentId(studentId)
            .courseId(courseId)
            .addData("conflictingCourse", conflictingCourseId)
            .addData("timeSlot", timeSlot)
            .message("Course " + courseId + " conflicts with " + conflictingCourseId + " at " + timeSlot)
            .build();
    }
    
    /**
     * Creates exception for duplicate enrollment
     */
    public static EnrollmentException duplicateEnrollment(String studentId, String courseId, 
                                                        EnrollmentStatus existingStatus) {
        return builder(ErrorCode.DUPLICATE_ENROLLMENT)
            .studentId(studentId)
            .courseId(courseId)
            .currentStatus(existingStatus)
            .message("Student " + studentId + " is already enrolled in course " + courseId + 
                    " with status: " + existingStatus)
            .build();
    }
    
    /**
     * Creates exception for closed enrollment period
     */
    public static EnrollmentException enrollmentClosed(String courseId, LocalDateTime closedDate) {
        return builder(ErrorCode.ENROLLMENT_CLOSED)
            .courseId(courseId)
            .addData("closedDate", closedDate)
            .message("Enrollment for course " + courseId + " closed on " + closedDate)
            .build();
    }
    
    /**
     * Creates exception for credit limit exceeded
     */
    public static EnrollmentException creditLimitExceeded(String studentId, int currentCredits, 
                                                        int courseCredits, int maxCredits) {
        return builder(ErrorCode.CREDIT_LIMIT_EXCEEDED)
            .studentId(studentId)
            .addData("currentCredits", currentCredits)
            .addData("courseCredits", courseCredits)
            .addData("maxCredits", maxCredits)
            .message("Enrollment would exceed credit limit. Current: " + currentCredits + 
                    ", Course: " + courseCredits + ", Max: " + maxCredits)
            .build();
    }
    
    /**
     * Creates exception for payment required
     */
    public static EnrollmentException paymentRequired(String studentId, double amountDue) {
        return builder(ErrorCode.PAYMENT_REQUIRED)
            .studentId(studentId)
            .addData("amountDue", amountDue)
            .message("Payment of $" + String.format("%.2f", amountDue) + " required for enrollment")
            .build();
    }
    
    /**
     * Creates exception for hold on account
     */
    public static EnrollmentException holdOnAccount(String studentId, String holdType, String holdDescription) {
        return builder(ErrorCode.HOLD_ON_ACCOUNT)
            .studentId(studentId)
            .addData("holdType", holdType)
            .addData("holdDescription", holdDescription)
            .message("Student " + studentId + " has a " + holdType + " hold: " + holdDescription)
            .build();
    }
    
    /**
     * Creates exception for invalid status transition
     */
    public static EnrollmentException invalidStatusTransition(String studentId, String courseId,
                                                            EnrollmentStatus currentStatus, 
                                                            EnrollmentStatus attemptedStatus) {
        return builder(ErrorCode.INVALID_ENROLLMENT_STATUS)
            .studentId(studentId)
            .courseId(courseId)
            .currentStatus(currentStatus)
            .attemptedStatus(attemptedStatus)
            .message("Invalid status transition from " + currentStatus + " to " + attemptedStatus)
            .build();
    }
    
    /**
     * Creates exception for system errors
     */
    public static EnrollmentException systemError(String operation, Throwable cause) {
        return builder(ErrorCode.SYSTEM_ERROR)
            .addData("operation", operation)
            .message("System error during " + operation)
            .cause(cause)
            .build();
    }
    
    // ==================== GETTERS ====================
    
    public ErrorCode getErrorCode() { return errorCode; }
    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public String getSemester() { return semester; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getAdditionalData() { return new HashMap<>(additionalData); }
    public EnrollmentStatus getCurrentStatus() { return currentStatus; }
    public EnrollmentStatus getAttemptedStatus() { return attemptedStatus; }
    
    /**
     * Gets specific additional data by key
     */
    public Object getAdditionalData(String key) {
        return additionalData.get(key);
    }
    
    /**
     * Gets additional data as specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalData(String key, Class<T> type) {
        Object value = additionalData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if this exception is recoverable (can be retried)
     */
    public boolean isRecoverable() {
        switch (errorCode) {
            case SYSTEM_ERROR:
            case COURSE_FULL: // Might open up
            case WAITLIST_FULL: // Might open up
                return true;
            case PREREQUISITE_NOT_MET:
            case SCHEDULE_CONFLICT:
            case DUPLICATE_ENROLLMENT:
            case ENROLLMENT_CLOSED:
            case DEADLINE_PASSED:
            case INVALID_SEMESTER:
            case INVALID_STUDENT_STATUS:
            case GRADE_LEVEL_RESTRICTION:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this exception requires manual intervention
     */
    public boolean requiresManualIntervention() {
        switch (errorCode) {
            case INSTRUCTOR_APPROVAL_REQUIRED:
            case DEPARTMENT_PERMISSION_REQUIRED:
            case HOLD_ON_ACCOUNT:
            case PAYMENT_REQUIRED:
            case ACADEMIC_PROBATION_RESTRICTION:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets user-friendly error message
     */
    public String getUserFriendlyMessage() {
        StringBuilder message = new StringBuilder();
        
        switch (errorCode) {
            case COURSE_FULL:
                message.append("This course is currently full. You may want to join the waitlist or choose an alternative course.");
                break;
            case PREREQUISITE_NOT_MET:
                String prerequisite = getAdditionalData("missingPrerequisite", String.class);
                message.append("You need to complete ").append(prerequisite).append(" before enrolling in this course.");
                break;
            case SCHEDULE_CONFLICT:
                String conflictingCourse = getAdditionalData("conflictingCourse", String.class);
                message.append("This course conflicts with your enrollment in ").append(conflictingCourse).append(".");
                break;
            case DUPLICATE_ENROLLMENT:
                message.append("You are already enrolled in this course.");
                break;
            case ENROLLMENT_CLOSED:
                message.append("The enrollment period for this course has ended.");
                break;
            case CREDIT_LIMIT_EXCEEDED:
                Integer maxCredits = getAdditionalData("maxCredits", Integer.class);
                message.append("Enrolling in this course would exceed your maximum credit limit of ").append(maxCredits).append(" hours.");
                break;
            case PAYMENT_REQUIRED:
                Double amountDue = getAdditionalData("amountDue", Double.class);
                message.append("Please make a payment of $").append(String.format("%.2f", amountDue)).append(" to complete enrollment.");
                break;
            case HOLD_ON_ACCOUNT:
                String holdType = getAdditionalData("holdType", String.class);
                message.append("You have a ").append(holdType).append(" hold on your account that prevents enrollment.");
                break;
            default:
                message.append(errorCode.getDescription());
        }
        
        return message.toString();
    }
    
    /**
     * Gets suggested actions for resolving the error
     */
    public String getSuggestedActions() {
        switch (errorCode) {
            case COURSE_FULL:
                return "Join the waitlist, check for additional sections, or choose an alternative course.";
            case PREREQUISITE_NOT_MET:
                return "Complete the required prerequisite course(s) before attempting to enroll.";
            case SCHEDULE_CONFLICT:
                return "Choose a different section or drop the conflicting course.";
            case DUPLICATE_ENROLLMENT:
                return "Check your current enrollments or contact the registrar if this appears to be an error.";
            case ENROLLMENT_CLOSED:
                return "Contact the registrar or instructor for late enrollment permission.";
            case CREDIT_LIMIT_EXCEEDED:
                return "Drop another course or request a credit overload approval from your advisor.";
            case PAYMENT_REQUIRED:
                return "Make the required payment through the student portal or bursar's office.";
            case HOLD_ON_ACCOUNT:
                return "Contact the appropriate office to resolve the hold on your account.";
            case INSTRUCTOR_APPROVAL_REQUIRED:
                return "Contact the course instructor for enrollment permission.";
            case DEPARTMENT_PERMISSION_REQUIRED:
                return "Contact the department office for enrollment permission.";
            default:
                return "Contact the registrar's office for assistance.";
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Builds a detailed error message with context
     */
    private static String buildDetailedMessage(ErrorCode errorCode, String studentId, 
                                             String courseId, String semester, String customMessage) {
        StringBuilder message = new StringBuilder();
        
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            message.append(customMessage);
        } else {
            message.append(errorCode.getDescription());
        }
        
        if (studentId != null || courseId != null || semester != null) {
            message.append(" [");
            boolean needsComma = false;
            
            if (studentId != null) {
                message.append("Student: ").append(studentId);
                needsComma = true;
            }
            
            if (courseId != null) {
                if (needsComma) message.append(", ");
                message.append("Course: ").append(courseId);
                needsComma = true;
            }
            
            if (semester != null) {
                if (needsComma) message.append(", ");
                message.append("Semester: ").append(semester);
            }
            
            message.append("]");
        }
        
        return message.toString();
    }
    
    // ==================== SERIALIZATION SUPPORT ====================
    
    /**
     * Returns a detailed string representation for logging
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EnrollmentException{");
        sb.append("errorCode=").append(errorCode);
        sb.append(", message='").append(getMessage()).append("'");
        
        if (studentId != null) sb.append(", studentId='").append(studentId).append("'");
        if (courseId != null) sb.append(", courseId='").append(courseId).append("'");
        if (semester != null) sb.append(", semester='").append(semester).append("'");
        if (currentStatus != null) sb.append(", currentStatus=").append(currentStatus);
        if (attemptedStatus != null) sb.append(", attemptedStatus=").append(attemptedStatus);
        
        sb.append(", timestamp=").append(timestamp);
        
        if (!additionalData.isEmpty()) {
            sb.append(", additionalData=").append(additionalData);
        }
        
        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName())
              .append(": ").append(getCause().getMessage());
        }
        
        sb.append("}");
        return sb.toString();
    }
}