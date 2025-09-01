// File: src/main/java/utils/ValidationUtil.java
package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * ValidationUtil class providing static utility methods for data validation.
 * 
 * Key Java concepts covered:
 * - Static methods and utility classes
 * - Regular expressions (Pattern class)
 * - Method overloading
 * - Exception handling
 * - String manipulation
 * - Date/time validation
 * - Final class design (cannot be extended)
 */
public final class ValidationUtil {
    
    // Regular expression patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+1-?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})$"
    );
    
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile(
        "^STU\\d{6}$" // STU followed by 6 digits
    );
    
    private static final Pattern PROFESSOR_ID_PATTERN = Pattern.compile(
        "^PROF\\d{5}$" // PROF followed by 5 digits
    );
    
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile(
        "^[A-Z]{2,4}\\d{3}[A-Z]?$" // 2-4 letters, 3 digits, optional letter
    );
    
    private static final Pattern DEPARTMENT_CODE_PATTERN = Pattern.compile(
        "^[A-Z]{2,6}$" // 2-6 uppercase letters
    );
    
    // Grade validation constants
    private static final double MIN_GPA = 0.0;
    private static final double MAX_GPA = 4.0;
    private static final double MIN_GRADE = 0.0;
    private static final double MAX_GRADE = 100.0;
    
    // Private constructor to prevent instantiation
    private ValidationUtil() {
        throw new UnsupportedOperationException("ValidationUtil is a utility class and cannot be instantiated");
    }
    
    // Email validation methods
    /**
     * Validates email address format using regex pattern.
     * @param email The email address to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validates and normalizes email address.
     * @param email The email address to validate and normalize
     * @return normalized email address
     * @throws IllegalArgumentException if email is invalid
     */
    public static String validateAndNormalizeEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return email.trim().toLowerCase();
    }
    
    // Phone number validation methods
    /**
     * Validates US phone number format.
     * @param phoneNumber The phone number to validate
     * @return true if phone number is valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }
    
    /**
     * Normalizes phone number to standard format.
     * @param phoneNumber The phone number to normalize
     * @return normalized phone number in format (XXX) XXX-XXXX
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }
        
        // Extract digits only
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        
        // Handle country code
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s", 
                digits.substring(0, 3),
                digits.substring(3, 6),
                digits.substring(6));
        }
        
        throw new IllegalArgumentException("Invalid phone number length: " + phoneNumber);
    }
    
    // ID validation methods
    /**
     * Validates student ID format (STU followed by 6 digits).
     * @param studentId The student ID to validate
     * @return true if student ID is valid, false otherwise
     */
    public static boolean isValidStudentId(String studentId) {
        return studentId != null && STUDENT_ID_PATTERN.matcher(studentId.trim()).matches();
    }
    
    /**
     * Validates professor ID format (PROF followed by 5 digits).
     * @param professorId The professor ID to validate
     * @return true if professor ID is valid, false otherwise
     */
    public static boolean isValidProfessorId(String professorId) {
        return professorId != null && PROFESSOR_ID_PATTERN.matcher(professorId.trim()).matches();
    }
    
    /**
     * Validates course code format (2-4 letters, 3 digits, optional letter).
     * @param courseCode The course code to validate
     * @return true if course code is valid, false otherwise
     */
    public static boolean isValidCourseCode(String courseCode) {
        return courseCode != null && COURSE_CODE_PATTERN.matcher(courseCode.trim().toUpperCase()).matches();
    }
    
    /**
     * Validates department code format (2-6 uppercase letters).
     * @param departmentCode The department code to validate
     * @return true if department code is valid, false otherwise
     */
    public static boolean isValidDepartmentCode(String departmentCode) {
        return departmentCode != null && DEPARTMENT_CODE_PATTERN.matcher(departmentCode.trim().toUpperCase()).matches();
    }
    
    // String validation methods
    /**
     * Checks if a string is null, empty, or contains only whitespace.
     * @param str The string to check
     * @return true if string is null or blank, false otherwise
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Validates that a string is not null or blank.
     * @param str The string to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if string is null or blank
     */
    public static void requireNonBlank(String str, String fieldName) {
        if (isNullOrBlank(str)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
    
    /**
     * Validates string length constraints.
     * @param str The string to validate
     * @param minLength Minimum allowed length
     * @param maxLength Maximum allowed length
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if length constraints are violated
     */
    public static void validateStringLength(String str, int minLength, int maxLength, String fieldName) {
        requireNonBlank(str, fieldName);
        
        int length = str.trim().length();
        if (length < minLength || length > maxLength) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d characters long", fieldName, minLength, maxLength)
            );
        }
    }
    
    // Numeric validation methods
    /**
     * Validates GPA value (0.0 to 4.0).
     * @param gpa The GPA to validate
     * @return true if GPA is valid, false otherwise
     */
    public static boolean isValidGPA(double gpa) {
        return gpa >= MIN_GPA && gpa <= MAX_GPA;
    }
    
    /**
     * Validates grade value (0.0 to 100.0).
     * @param grade The grade to validate
     * @return true if grade is valid, false otherwise
     */
    public static boolean isValidGrade(double grade) {
        return grade >= MIN_GRADE && grade <= MAX_GRADE;
    }
    
    /**
     * Validates that a number is within a specified range.
     * @param value The value to validate
     * @param min Minimum allowed value (inclusive)
     * @param max Maximum allowed value (inclusive)
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if value is outside the range
     */
    public static void validateRange(double value, double min, double max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %.2f and %.2f", fieldName, min, max)
            );
        }
    }
    
    /**
     * Validates that an integer is positive.
     * @param value The value to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if value is not positive
     */
    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
    
    /**
     * Validates that an integer is non-negative.
     * @param value The value to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if value is negative
     */
    public static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
    
    // Date validation methods
    /**
     * Validates that a date is not in the future.
     * @param date The date to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if date is in the future
     */
    public static void requireNotFuture(LocalDate date, String fieldName) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " cannot be in the future");
        }
    }
    
    /**
     * Validates that a date is not in the past.
     * @param date The date to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if date is in the past
     */
    public static void requireNotPast(LocalDate date, String fieldName) {
        if (date != null && date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " cannot be in the past");
        }
    }
    
    /**
     * Validates that a datetime is recent (within last 24 hours).
     * @param dateTime The datetime to validate
     * @return true if datetime is recent, false otherwise
     */
    public static boolean isRecentDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.isAfter(LocalDateTime.now().minusHours(24));
    }
    
    /**
     * Parses and validates a date string.
     * @param dateStr The date string to parse
     * @param fieldName The name of the field for error messages
     * @return parsed LocalDate
     * @throws IllegalArgumentException if date string is invalid
     */
    public static LocalDate parseDate(String dateStr, String fieldName) {
        if (isNullOrBlank(dateStr)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for " + fieldName + ": " + dateStr);
        }
    }
    
    // Academic validation methods
    /**
     * Validates academic year format (e.g., "2023-2024").
     * @param academicYear The academic year to validate
     * @return true if format is valid, false otherwise
     */
    public static boolean isValidAcademicYear(String academicYear) {
        if (isNullOrBlank(academicYear)) {
            return false;
        }
        
        Pattern academicYearPattern = Pattern.compile("^\\d{4}-\\d{4}$");
        if (!academicYearPattern.matcher(academicYear.trim()).matches()) {
            return false;
        }
        
        String[] years = academicYear.split("-");
        try {
            int startYear = Integer.parseInt(years[0]);
            int endYear = Integer.parseInt(years[1]);
            return endYear == startYear + 1 && startYear >= 2020 && startYear <= 2030;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validates semester format.
     * @param semester The semester to validate
     * @return true if semester is valid, false otherwise
     */
    public static boolean isValidSemester(String semester) {
        if (isNullOrBlank(semester)) {
            return false;
        }
        
        String normalizedSemester = semester.trim().toLowerCase();
        return normalizedSemester.equals("fall") || 
               normalizedSemester.equals("spring") || 
               normalizedSemester.equals("summer") ||
               normalizedSemester.equals("winter");
    }
    
    /**
     * Validates credit hours for a course.
     * @param credits The credit hours to validate
     * @return true if credits are valid, false otherwise
     */
    public static boolean isValidCredits(int credits) {
        return credits >= 1 && credits <= 6;
    }
    
    // Password validation methods
    /**
     * Validates password strength.
     * @param password The password to validate
     * @return true if password meets strength requirements, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        if (isNullOrBlank(password) || password.length() < 8) {
            return false;
        }
        
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
    
    /**
     * Gets password strength requirements as a descriptive string.
     * @return string describing password requirements
     */
    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters long and contain: " +
               "uppercase letter, lowercase letter, digit, and special character (!@#$%^&*()_+-=[]{}|;:,.<>?)";
    }
    
    // Collection validation methods
    /**
     * Validates that a collection is not null or empty.
     * @param collection The collection to validate
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if collection is null or empty
     */
    public static void requireNonEmpty(java.util.Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
    
    /**
     * Validates collection size constraints.
     * @param collection The collection to validate
     * @param minSize Minimum allowed size
     * @param maxSize Maximum allowed size
     * @param fieldName The name of the field for error messages
     * @throws IllegalArgumentException if size constraints are violated
     */
    public static void validateCollectionSize(java.util.Collection<?> collection, int minSize, int maxSize, String fieldName) {
        if (collection == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        
        int size = collection.size();
        if (size < minSize || size > maxSize) {
            throw new IllegalArgumentException(
                String.format("%s must contain between %d and %d elements", fieldName, minSize, maxSize)
            );
        }
    }
    
    // Method to demonstrate method overloading
    public static String sanitizeInput(String input) {
        return sanitizeInput(input, 255);
    }
    
    public static String sanitizeInput(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input.trim();
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength);
        }
        
        // Remove potentially dangerous characters
        return sanitized.replaceAll("[<>\"'&]", "");
    }
    
    // Comprehensive validation method
    /**
     * Performs comprehensive validation for user registration data.
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param phoneNumber Phone number (optional)
     * @throws IllegalArgumentException if any validation fails
     */
    public static void validateUserRegistration(String firstName, String lastName, String email, String phoneNumber) {
        validateStringLength(firstName, 1, 50, "First name");
        validateStringLength(lastName, 1, 50, "Last name");
        
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (phoneNumber != null && !phoneNumber.trim().isEmpty() && !isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
}