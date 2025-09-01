// File location: src/test/java/unit/utils/ValidationUtilTest.java

package com.smartcampus.test.unit.utils;

import com.smartcampus.utils.ValidationUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for the ValidationUtil class
 * Tests various validation utility methods
 * 
 * @author Smart Campus Development Team
 * @version 1.0
 */
@DisplayName("Validation Util Tests")
class ValidationUtilTest {

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid email addresses")
        @ValueSource(strings = {
            "test@smartcampus.edu",
            "user.name@university.edu",
            "student123@school.org",
            "admin@college.edu",
            "faculty.member@institution.edu",
            "john.doe+test@smartcampus.edu"
        })
        void shouldAcceptValidEmailAddresses(String email) {
            assertTrue(ValidationUtil.isValidEmail(email));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid email addresses")
        @ValueSource(strings = {
            "invalid-email",
            "@smartcampus.edu",
            "user@",
            "user@@smartcampus.edu",
            "user@smartcampus",
            "user name@smartcampus.edu",
            "user@smart campus.edu",
            ".user@smartcampus.edu",
            "user.@smartcampus.edu"
        })
        void shouldRejectInvalidEmailAddresses(String email) {
            assertFalse(ValidationUtil.isValidEmail(email));
        }

        @ParameterizedTest
        @DisplayName("Should reject null and empty email addresses")
        @NullAndEmptySource
        void shouldRejectNullAndEmptyEmailAddresses(String email) {
            assertFalse(ValidationUtil.isValidEmail(email));
        }

        @Test
        @DisplayName("Should handle whitespace in email addresses")
        void shouldHandleWhitespaceInEmailAddresses() {
            assertFalse(ValidationUtil.isValidEmail(" test@smartcampus.edu "));
            assertFalse(ValidationUtil.isValidEmail("test @smartcampus.edu"));
            assertFalse(ValidationUtil.isValidEmail("test@ smartcampus.edu"));
        }
    }

    @Nested
    @DisplayName("Phone Number Validation Tests")
    class PhoneNumberValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid phone numbers")
        @ValueSource(strings = {
            "555-1234",
            "(555) 123-4567",
            "555.123.4567",
            "5551234567",
            "+1-555-123-4567",
            "1-800-555-1234",
            "(800) 555-1234"
        })
        void shouldAcceptValidPhoneNumbers(String phone) {
            assertTrue(ValidationUtil.isValidPhone(phone));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid phone numbers")
        @ValueSource(strings = {
            "123",
            "abc-defg",
            "555-abc-1234",
            "555-123-456",
            "555-123-45678",
            "phone number",
            "123-456-789a"
        })
        void shouldRejectInvalidPhoneNumbers(String phone) {
            assertFalse(ValidationUtil.isValidPhone(phone));
        }

        @ParameterizedTest
        @DisplayName("Should reject null and empty phone numbers")
        @NullAndEmptySource
        void shouldRejectNullAndEmptyPhoneNumbers(String phone) {
            assertFalse(ValidationUtil.isValidPhone(phone));
        }

        @Test
        @DisplayName("Should normalize phone numbers")
        void shouldNormalizePhoneNumbers() {
            assertThat(ValidationUtil.normalizePhone("(555) 123-4567")).isEqualTo("5551234567");
            assertThat(ValidationUtil.normalizePhone("555.123.4567")).isEqualTo("5551234567");
            assertThat(ValidationUtil.normalizePhone("555-123-4567")).isEqualTo("5551234567");
            assertThat(ValidationUtil.normalizePhone("+1-555-123-4567")).isEqualTo("15551234567");
        }
    }

    @Nested
    @DisplayName("String Validation Tests")
    class StringValidationTests {

        @Test
        @DisplayName("Should check if string is null or empty")
        void shouldCheckIfStringIsNullOrEmpty() {
            assertTrue(ValidationUtil.isNullOrEmpty(null));
            assertTrue(ValidationUtil.isNullOrEmpty(""));
            assertFalse(ValidationUtil.isNullOrEmpty(" "));
            assertFalse(ValidationUtil.isNullOrEmpty("text"));
        }

        @Test
        @DisplayName("Should check if string is null, empty, or blank")
        void shouldCheckIfStringIsNullEmptyOrBlank() {
            assertTrue(ValidationUtil.isBlank(null));
            assertTrue(ValidationUtil.isBlank(""));
            assertTrue(ValidationUtil.isBlank(" "));
            assertTrue(ValidationUtil.isBlank("\t\n\r"));
            assertFalse(ValidationUtil.isBlank("text"));
            assertFalse(ValidationUtil.isBlank(" text "));
        }

        @Test
        @DisplayName("Should check if string is not blank")
        void shouldCheckIfStringIsNotBlank() {
            assertFalse(ValidationUtil.isNotBlank(null));
            assertFalse(ValidationUtil.isNotBlank(""));
            assertFalse(ValidationUtil.isNotBlank(" "));
            assertTrue(ValidationUtil.isNotBlank("text"));
            assertTrue(ValidationUtil.isNotBlank(" text "));
        }

        @ParameterizedTest
        @DisplayName("Should validate string length")
        @CsvSource({
            "hello, 3, 10, true",
            "hi, 3, 10, false",
            "verylongstring, 3, 10, false",
            "test, 4, 4, true",
            "'', 0, 5, true"
        })
        void shouldValidateStringLength(String input, int minLength, int maxLength, boolean expected) {
            assertThat(ValidationUtil.isValidLength(input, minLength, maxLength)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should validate string contains only letters")
        void shouldValidateStringContainsOnlyLetters() {
            assertTrue(ValidationUtil.isAlphabetic("HelloWorld"));
            assertTrue(ValidationUtil.isAlphabetic("ABC"));
            assertFalse(ValidationUtil.isAlphabetic("Hello123"));
            assertFalse(ValidationUtil.isAlphabetic("Hello World"));
            assertFalse(ValidationUtil.isAlphabetic("Hello!"));
        }

        @Test
        @DisplayName("Should validate string contains only alphanumeric characters")
        void shouldValidateStringContainsOnlyAlphanumeric() {
            assertTrue(ValidationUtil.isAlphanumeric("Hello123"));
            assertTrue(ValidationUtil.isAlphanumeric("ABC123"));
            assertTrue(ValidationUtil.isAlphanumeric("test"));
            assertFalse(ValidationUtil.isAlphanumeric("Hello World"));
            assertFalse(ValidationUtil.isAlphanumeric("Hello!"));
            assertFalse(ValidationUtil.isAlphanumeric("test@example"));
        }

        @Test
        @DisplayName("Should validate string matches pattern")
        void shouldValidateStringMatchesPattern() {
            String pattern = "^[A-Z]{2}\\d{4}\\d{3}$"; // Student ID pattern
            assertTrue(ValidationUtil.matchesPattern("CS2024001", pattern));
            assertTrue(ValidationUtil.matchesPattern("EN2023456", pattern));
            assertFalse(ValidationUtil.matchesPattern("cs2024001", pattern)); // lowercase
            assertFalse(ValidationUtil.matchesPattern("CS24001", pattern)); // too short
        }
    }

    @Nested
    @DisplayName("Numeric Validation Tests")
    class NumericValidationTests {

        @Test
        @DisplayName("Should validate integer range")
        void shouldValidateIntegerRange() {
            assertTrue(ValidationUtil.isInRange(5, 1, 10));
            assertTrue(ValidationUtil.isInRange(1, 1, 10));
            assertTrue(ValidationUtil.isInRange(10, 1, 10));
            assertFalse(ValidationUtil.isInRange(0, 1, 10));
            assertFalse(ValidationUtil.isInRange(11, 1, 10));
        }

        @Test
        @DisplayName("Should validate double range")
        void shouldValidateDoubleRange() {
            assertTrue(ValidationUtil.isInRange(5.5, 1.0, 10.0));
            assertTrue(ValidationUtil.isInRange(1.0, 1.0, 10.0));
            assertTrue(ValidationUtil.isInRange(10.0, 1.0, 10.0));
            assertFalse(ValidationUtil.isInRange(0.9, 1.0, 10.0));
            assertFalse(ValidationUtil.isInRange(10.1, 1.0, 10.0));
        }

        @Test
        @DisplayName("Should validate BigDecimal range")
        void shouldValidateBigDecimalRange() {
            assertTrue(ValidationUtil.isInRange(
                BigDecimal.valueOf(2.5), BigDecimal.valueOf(0.0), BigDecimal.valueOf(4.0)
            ));
            assertFalse(ValidationUtil.isInRange(
                BigDecimal.valueOf(5.0), BigDecimal.valueOf(0.0), BigDecimal.valueOf(4.0)
            ));
        }

        @Test
        @DisplayName("Should validate positive numbers")
        void shouldValidatePositiveNumbers() {
            assertTrue(ValidationUtil.isPositive(5));
            assertTrue(ValidationUtil.isPositive(0.1));
            assertTrue(ValidationUtil.isPositive(BigDecimal.valueOf(10.5)));
            assertFalse(ValidationUtil.isPositive(-5));
            assertFalse(ValidationUtil.isPositive(-0.1));
            assertFalse(ValidationUtil.isPositive(BigDecimal.valueOf(-10.5)));
        }

        @Test
        @DisplayName("Should validate non-negative numbers")
        void shouldValidateNonNegativeNumbers() {
            assertTrue(ValidationUtil.isNonNegative(5));
            assertTrue(ValidationUtil.isNonNegative(0));
            assertTrue(ValidationUtil.isNonNegative(0.0));
            assertTrue(ValidationUtil.isNonNegative(BigDecimal.ZERO));
            assertFalse(ValidationUtil.isNonNegative(-5));
            assertFalse(ValidationUtil.isNonNegative(-0.1));
        }

        @ParameterizedTest
        @DisplayName("Should validate numeric strings")
        @ValueSource(strings = {"123", "456.78", "0", "999.99"})
        void shouldValidateNumericStrings(String input) {
            assertTrue(ValidationUtil.isNumeric(input));
        }

        @ParameterizedTest
        @DisplayName("Should reject non-numeric strings")
        @ValueSource(strings = {"abc", "12.34.56", "12a", "12.34f", "12,34"})
        void shouldRejectNonNumericStrings(String input) {
            assertFalse(ValidationUtil.isNumeric(input));
        }
    }

    @Nested
    @DisplayName("GPA Validation Tests")
    class GpaValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid GPA values")
        @ValueSource(strings = {"0.00", "2.00", "3.50", "4.00"})
        void shouldAcceptValidGpaValues(String gpaString) {
            BigDecimal gpa = new BigDecimal(gpaString);
            assertTrue(ValidationUtil.isValidGpa(gpa));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid GPA values")
        @ValueSource(strings = {"-0.01", "4.01", "5.00"})
        void shouldRejectInvalidGpaValues(String gpaString) {
            BigDecimal gpa = new BigDecimal(gpaString);
            assertFalse(ValidationUtil.isValidGpa(gpa));
        }

        @Test
        @DisplayName("Should reject null GPA")
        void shouldRejectNullGpa() {
            assertFalse(ValidationUtil.isValidGpa(null));
        }

        @Test
        @DisplayName("Should validate GPA precision")
        void shouldValidateGpaPrecision() {
            assertTrue(ValidationUtil.isValidGpaPrecision(BigDecimal.valueOf(3.50)));
            assertTrue(ValidationUtil.isValidGpaPrecision(BigDecimal.valueOf(3.5)));
            assertFalse(ValidationUtil.isValidGpaPrecision(BigDecimal.valueOf(3.555)));
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should validate date range")
        void shouldValidateDateRange() {
            assertTrue(ValidationUtil.isValidDateRange(
                java.time.LocalDate.of(2024, 1, 1),
                java.time.LocalDate.of(2024, 12, 31)
            ));
            assertFalse(ValidationUtil.isValidDateRange(
                java.time.LocalDate.of(2024, 12, 31),
                java.time.LocalDate.of(2024, 1, 1)
            ));
        }

        @Test
        @DisplayName("Should validate academic year")
        void shouldValidateAcademicYear() {
            int currentYear = java.time.Year.now().getValue();
            assertTrue(ValidationUtil.isValidAcademicYear(currentYear));
            assertTrue(ValidationUtil.isValidAcademicYear(currentYear + 1));
            assertFalse(ValidationUtil.isValidAcademicYear(1800));
            assertFalse(ValidationUtil.isValidAcademicYear(currentYear + 10));
        }

        @Test
        @DisplayName("Should validate age range")
        void shouldValidateAgeRange() {
            assertTrue(ValidationUtil.isValidAge(20));
            assertTrue(ValidationUtil.isValidAge(65));
            assertFalse(ValidationUtil.isValidAge(10));
            assertFalse(ValidationUtil.isValidAge(120));
        }
    }

    @Nested
    @DisplayName("Collection Validation Tests")
    class CollectionValidationTests {

        @Test
        @DisplayName("Should check if collection is null or empty")
        void shouldCheckIfCollectionIsNullOrEmpty() {
            assertTrue(ValidationUtil.isNullOrEmpty((List<String>) null));
            assertTrue(ValidationUtil.isNullOrEmpty(Collections.emptyList()));
            assertFalse(ValidationUtil.isNullOrEmpty(Arrays.asList("item")));
        }

        @Test
        @DisplayName("Should validate collection size")
        void shouldValidateCollectionSize() {
            List<String> list = Arrays.asList("a", "b", "c");
            assertTrue(ValidationUtil.hasValidSize(list, 1, 5));
            assertFalse(ValidationUtil.hasValidSize(list, 5, 10));
            assertFalse(ValidationUtil.hasValidSize(list, 1, 2));
        }

        @Test
        @DisplayName("Should check if collection contains only valid items")
        void shouldCheckIfCollectionContainsOnlyValidItems() {
            List<String> validEmails = Arrays.asList(
                "test@smartcampus.edu", 
                "user@university.edu"
            );
            assertTrue(ValidationUtil.allItemsValid(validEmails, ValidationUtil::isValidEmail));

            List<String> invalidEmails = Arrays.asList(
                "test@smartcampus.edu", 
                "invalid-email"
            );
            assertFalse(ValidationUtil.allItemsValid(invalidEmails, ValidationUtil::isValidEmail));
        }

        @Test
        @DisplayName("Should check if any collection item is valid")
        void shouldCheckIfAnyCollectionItemIsValid() {
            List<String> mixedEmails = Arrays.asList(
                "invalid-email", 
                "test@smartcampus.edu"
            );
            assertTrue(ValidationUtil.anyItemValid(mixedEmails, ValidationUtil::isValidEmail));

            List<String> allInvalidEmails = Arrays.asList(
                "invalid-email1", 
                "invalid-email2"
            );
            assertFalse(ValidationUtil.anyItemValid(allInvalidEmails, ValidationUtil::isValidEmail));
        }
    }

    @Nested
    @DisplayName("Academic Validation Tests")
    class AcademicValidationTests {

        @ParameterizedTest
        @DisplayName("Should validate student ID format")
        @ValueSource(strings = {"CS2024001", "ENG2023456", "MATH2025999"})
        void shouldValidateStudentIdFormat(String studentId) {
            assertTrue(ValidationUtil.isValidStudentId(studentId));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid student ID format")
        @ValueSource(strings = {"cs2024001", "CS24001", "123456789", "TOOLONGPREFIX2024001"})
        void shouldRejectInvalidStudentIdFormat(String studentId) {
            assertFalse(ValidationUtil.isValidStudentId(studentId));
        }

        @ParameterizedTest
        @DisplayName("Should validate course code format")
        @ValueSource(strings = {"CS101", "MATH201", "ENG301", "PHYS101L"})
        void shouldValidateCourseCodeFormat(String courseCode) {
            assertTrue(ValidationUtil.isValidCourseCode(courseCode));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid course code format")
        @ValueSource(strings = {"cs101", "CS", "101", "VERYLONGCOURSE101"})
        void shouldRejectInvalidCourseCodeFormat(String courseCode) {
            assertFalse(ValidationUtil.isValidCourseCode(courseCode));
        }

        @ParameterizedTest
        @DisplayName("Should validate year level")
        @ValueSource(ints = {1, 2, 3, 4, 5})
        void shouldValidateYearLevel(int yearLevel) {
            assertTrue(ValidationUtil.isValidYearLevel(yearLevel));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid year level")
        @ValueSource(ints = {0, 6, -1, 10})
        void shouldRejectInvalidYearLevel(int yearLevel) {
            assertFalse(ValidationUtil.isValidYearLevel(yearLevel));
        }

        @ParameterizedTest
        @DisplayName("Should validate credit hours")
        @ValueSource(ints = {1, 2, 3, 4, 5, 6})
        void shouldValidateCreditHours(int credits) {
            assertTrue(ValidationUtil.isValidCredits(credits));
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid credit hours")
        @ValueSource(ints = {0, 7, -1, 20})
        void shouldRejectInvalidCreditHours(int credits) {
            assertFalse(ValidationUtil.isValidCredits(credits));
        }

        @Test
        @DisplayName("Should validate grade letter")
        void shouldValidateGradeLetter() {
            assertTrue(ValidationUtil.isValidGradeLetter("A"));
            assertTrue(ValidationUtil.isValidGradeLetter("A+"));
            assertTrue(ValidationUtil.isValidGradeLetter("B-"));
            assertTrue(ValidationUtil.isValidGradeLetter("F"));
            assertFalse(ValidationUtil.isValidGradeLetter("Z"));
            assertFalse(ValidationUtil.isValidGradeLetter("A++"));
            assertFalse(ValidationUtil.isValidGradeLetter(""));
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept strong passwords")
        @ValueSource(strings = {
            "StrongP@ssw0rd",
            "Complex!Pass123",
            "MySecur3P@ss",
            "Univ3rsity!2024"
        })
        void shouldAcceptStrongPasswords(String password) {
            assertTrue(ValidationUtil.isStrongPassword(password));
        }

        @ParameterizedTest
        @DisplayName("Should reject weak passwords")
        @ValueSource(strings = {
            "weak",
            "password",
            "12345678",
            "ALLUPPERCASE",
            "alllowercase",
            "NoNumbers!",
            "NoSpecialChar123",
            "nouppercas3!"
        })
        void shouldRejectWeakPasswords(String password) {
            assertFalse(ValidationUtil.isStrongPassword(password));
        }

        @Test
        @DisplayName("Should check password complexity requirements")
        void shouldCheckPasswordComplexityRequirements() {
            String complexPassword = "Complex!Pass123";
            assertTrue(ValidationUtil.hasUpperCase(complexPassword));
            assertTrue(ValidationUtil.hasLowerCase(complexPassword));
            assertTrue(ValidationUtil.hasDigit(complexPassword));
            assertTrue(ValidationUtil.hasSpecialCharacter(complexPassword));
            assertTrue(ValidationUtil.hasMinLength(complexPassword, 8));
        }

        @Test
        @DisplayName("Should validate password length")
        void shouldValidatePasswordLength() {
            assertTrue(ValidationUtil.hasMinLength("password123", 8));
            assertFalse(ValidationUtil.hasMinLength("pass", 8));
            assertTrue(ValidationUtil.hasMaxLength("password123", 20));
            assertFalse(ValidationUtil.hasMaxLength("verylongpasswordthatexceedslimit", 20));
        }
    }

    @Nested
    @DisplayName("Sanitization Tests")
    class SanitizationTests {

        @Test
        @DisplayName("Should sanitize input strings")
        void shouldSanitizeInputStrings() {
            assertThat(ValidationUtil.sanitizeInput("  Hello World  ")).isEqualTo("Hello World");
            assertThat(ValidationUtil.sanitizeInput("Hello\tWorld\n")).isEqualTo("Hello World");
            assertThat(ValidationUtil.sanitizeInput("")).isEqualTo("");
            assertThat(ValidationUtil.sanitizeInput(null)).isNull();
        }

        @Test
        @DisplayName("Should remove special characters")
        void shouldRemoveSpecialCharacters() {
            assertThat(ValidationUtil.removeSpecialChars("Hello@World!")).isEqualTo("HelloWorld");
            assertThat(ValidationUtil.removeSpecialChars("Test123")).isEqualTo("Test123");
            assertThat(ValidationUtil.removeSpecialChars("@#$%")).isEqualTo("");
        }

        @Test
        @DisplayName("Should escape HTML characters")
        void shouldEscapeHtmlCharacters() {
            assertThat(ValidationUtil.escapeHtml("<script>alert('test')</script>"))
                .isEqualTo("&lt;script&gt;alert('test')&lt;/script&gt;");
            assertThat(ValidationUtil.escapeHtml("Hello & goodbye")).isEqualTo("Hello &amp; goodbye");
        }

        @Test
        @DisplayName("Should normalize strings")
        void shouldNormalizeStrings() {
            assertThat(ValidationUtil.normalize("  HELLO world  ")).isEqualTo("hello world");
            assertThat(ValidationUtil.normalize("Test\t\nString")).isEqualTo("test string");
        }
    }

    @Nested
    @DisplayName("Compound Validation Tests")
    class CompoundValidationTests {

        @Test
        @DisplayName("Should validate multiple conditions with AND logic")
        void shouldValidateMultipleConditionsWithAndLogic() {
            assertTrue(ValidationUtil.validateAll(
                () -> ValidationUtil.isValidEmail("test@smartcampus.edu"),
                () -> ValidationUtil.isNotBlank("test@smartcampus.edu"),
                () -> ValidationUtil.isValidLength("test@smartcampus.edu", 5, 50)
            ));

            assertFalse(ValidationUtil.validateAll(
                () -> ValidationUtil.isValidEmail("test@smartcampus.edu"),
                () -> ValidationUtil.isValidEmail("invalid-email"),
                () -> ValidationUtil.isValidLength("test@smartcampus.edu", 5, 50)
            ));
        }

        @Test
        @DisplayName("Should validate multiple conditions with OR logic")
        void shouldValidateMultipleConditionsWithOrLogic() {
            assertTrue(ValidationUtil.validateAny(
                () -> ValidationUtil.isValidEmail("invalid-email"),
                () -> ValidationUtil.isValidEmail("test@smartcampus.edu"),
                () -> ValidationUtil.isValidEmail("another-invalid")
            ));

            assertFalse(ValidationUtil.validateAny(
                () -> ValidationUtil.isValidEmail("invalid1"),
                () -> ValidationUtil.isValidEmail("invalid2"),
                () -> ValidationUtil.isValidEmail("invalid3")
            ));
        }

        @Test
        @DisplayName("Should validate complex business rules")
        void shouldValidateComplexBusinessRules() {
            // Example: Student enrollment validation
            String studentId = "CS2024001";
            String email = "student@smartcampus.edu";
            int yearLevel = 2;
            BigDecimal gpa = BigDecimal.valueOf(3.25);

            boolean isValidForEnrollment = ValidationUtil.validateAll(
                () -> ValidationUtil.isValidStudentId(studentId),
                () -> ValidationUtil.isValidEmail(email),
                () -> ValidationUtil.isValidYearLevel(yearLevel),
                () -> ValidationUtil.isValidGpa(gpa)
            );

            assertTrue(isValidForEnrollment);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null inputs gracefully")
        void shouldHandleNullInputsGracefully() {
            assertFalse(ValidationUtil.isValidEmail(null));
            assertFalse(ValidationUtil.isValidPhone(null));
            assertFalse(ValidationUtil.isValidStudentId(null));
            assertFalse(ValidationUtil.isValidCourseCode(null));
        }

        @Test
        @DisplayName("Should handle empty inputs gracefully")
        void shouldHandleEmptyInputsGracefully() {
            assertFalse(ValidationUtil.isValidEmail(""));
            assertFalse(ValidationUtil.isValidPhone(""));
            assertFalse(ValidationUtil.isValidStudentId(""));
            assertFalse(ValidationUtil.isValidCourseCode(""));
        }

        @Test
        @DisplayName("Should handle malformed inputs gracefully")
        void shouldHandleMalformedInputsGracefully() {
            assertDoesNotThrow(() -> ValidationUtil.isValidEmail("malformed@@email"));
            assertDoesNotThrow(() -> ValidationUtil.isNumeric("not-a-number"));
            assertDoesNotThrow(() -> ValidationUtil.isValidGpa(BigDecimal.valueOf(-999)));
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should efficiently validate large collections")
        void shouldEfficientlyValidateLargeCollections() {
            // Create a large list of valid emails
            List<String> emails = java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> "user" + i + "@smartcampus.edu")
                .collect(java.util.stream.Collectors.toList());

            // This should complete quickly without performance issues
            long startTime = System.currentTimeMillis();
            boolean allValid = ValidationUtil.allItemsValid(emails, ValidationUtil::isValidEmail);
            long endTime = System.currentTimeMillis();

            assertTrue(allValid);
            assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
        }

        @Test
        @DisplayName("Should cache regex patterns for efficiency")
        void shouldCacheRegexPatternsForEfficiency() {
            // Multiple validations should reuse compiled patterns
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                ValidationUtil.isValidEmail("test" + i + "@smartcampus.edu");
                ValidationUtil.isValidPhone("555-123-" + String.format("%04d", i));
                ValidationUtil.isValidStudentId("CS2024" + String.format("%03d", i));
            }
            long endTime = System.currentTimeMillis();

            // Should be reasonably fast due to pattern caching
            assertThat(endTime - startTime).isLessThan(500);
        }
    }
}