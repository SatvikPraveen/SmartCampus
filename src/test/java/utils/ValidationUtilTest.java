package utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilTest {

    @Nested
    class Email {
        @ParameterizedTest
        @ValueSource(strings = {"jane@campus.edu", "  jane.doe+cs@mail.campus.edu  ", "a_b%c@x-y.io"})
        void valid(String email) {
            assertThat(ValidationUtil.isValidEmail(email)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"jane", "jane@", "jane@campus", "@campus.edu", "ja ne@campus.edu", "jane@campus.e"})
        void invalid(String email) {
            assertThat(ValidationUtil.isValidEmail(email)).isFalse();
        }

        @Test
        void normalizeTrimsAndLowercases() {
            assertThat(ValidationUtil.validateAndNormalizeEmail("  Jane.Doe@Campus.EDU ")).isEqualTo("jane.doe@campus.edu");
            assertThatThrownBy(() -> ValidationUtil.validateAndNormalizeEmail("bad"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bad");
        }
    }

    @Nested
    class Phone {
        @ParameterizedTest
        @CsvSource({
            "'(555) 123-4567', '(555) 123-4567'",
            "555-123-4567, '(555) 123-4567'",
            "555.123.4567, '(555) 123-4567'",
            "5551234567, '(555) 123-4567'",
            "+1-555-123-4567, '(555) 123-4567'",
            "+15551234567, '(555) 123-4567'"
        })
        void validNumbersNormalizeToCanonicalForm(String input, String expected) {
            assertThat(ValidationUtil.isValidPhoneNumber(input)).isTrue();
            assertThat(ValidationUtil.normalizePhoneNumber(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"555-1234", "55512345678", "abc-def-ghij", "+44 20 7946 0958"})
        void invalidNumbers(String input) {
            assertThat(ValidationUtil.isValidPhoneNumber(input)).isFalse();
            assertThatThrownBy(() -> ValidationUtil.normalizePhoneNumber(input))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Identifiers {
        @ParameterizedTest
        @CsvSource({"STU123456, true", " STU000001 , true", "STU12345, false", "stu123456, false", "STU1234567, false"})
        void studentId(String id, boolean expected) {
            assertThat(ValidationUtil.isValidStudentId(id)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"PROF12345, true", "PROF1234, false", "PRF12345, false"})
        void professorId(String id, boolean expected) {
            assertThat(ValidationUtil.isValidProfessorId(id)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"CS101, true", "cs101, true", "MATH201A, true", "C101, false", "COMPS101, false", "CS10, false", "CS101AB, false"})
        void courseCodeIsCaseInsensitive(String code, boolean expected) {
            assertThat(ValidationUtil.isValidCourseCode(code)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"CS, true", "math, true", "ENGRNG, true", "C, false", "ENGINEER, false", "CS1, false"})
        void departmentCode(String code, boolean expected) {
            assertThat(ValidationUtil.isValidDepartmentCode(code)).isEqualTo(expected);
        }

        @Test
        void nullIdsAreInvalid() {
            assertThat(ValidationUtil.isValidStudentId(null)).isFalse();
            assertThat(ValidationUtil.isValidProfessorId(null)).isFalse();
            assertThat(ValidationUtil.isValidCourseCode(null)).isFalse();
            assertThat(ValidationUtil.isValidDepartmentCode(null)).isFalse();
        }
    }

    @Nested
    class Strings {
        @Test
        void emptyVsBlank() {
            assertThat(ValidationUtil.isEmpty(null)).isTrue();
            assertThat(ValidationUtil.isEmpty("")).isTrue();
            assertThat(ValidationUtil.isEmpty("  ")).isFalse();
            assertThat(ValidationUtil.isNullOrBlank("  ")).isTrue();
            assertThat(ValidationUtil.isValidString("  ")).isFalse();
            assertThat(ValidationUtil.isValidString(" x ")).isTrue();
        }

        @Test
        void requireNonBlank() {
            assertThatCode(() -> ValidationUtil.requireNonBlank("x", "Name")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.requireNonBlank(" ", "Name"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Name cannot be null or blank");
        }

        @Test
        void lengthIsMeasuredOnTrimmedValueAndBoundsAreInclusive() {
            assertThatCode(() -> ValidationUtil.validateStringLength("  abc  ", 3, 3, "F")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.validateStringLength("ab", 3, 5, "F"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 3 and 5");
            assertThatThrownBy(() -> ValidationUtil.validateStringLength("abcdef", 3, 5, "F"))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ValidationUtil.validateStringLength(null, 0, 5, "F"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void sanitizeInput() {
            assertThat(ValidationUtil.sanitizeInput("  <b>\"Tom\" & 'Jerry'</b> ")).isEqualTo("bTom  Jerry/b");
            assertThat(ValidationUtil.sanitizeInput("abcdef", 3)).isEqualTo("abc");
            assertThat(ValidationUtil.sanitizeInput("x".repeat(300))).hasSize(255);
            assertThat(ValidationUtil.sanitizeInput(null)).isNull();
        }
    }

    @Nested
    class Numbers {
        @ParameterizedTest
        @CsvSource({"0.0, true", "4.0, true", "2.5, true", "-0.01, false", "4.01, false"})
        void gpa(double gpa, boolean expected) {
            assertThat(ValidationUtil.isValidGPA(gpa)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"0, true", "100, true", "100.5, false", "-1, false"})
        void grade(double grade, boolean expected) {
            assertThat(ValidationUtil.isValidGrade(grade)).isEqualTo(expected);
        }

        @Test
        void rangeAndSign() {
            assertThatCode(() -> ValidationUtil.validateRange(5, 5, 10, "X")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.validateRange(10.5, 5, 10, "X"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("X must be between");
            assertThatCode(() -> ValidationUtil.requirePositive(1, "n")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.requirePositive(0, "n")).isInstanceOf(IllegalArgumentException.class);
            assertThatCode(() -> ValidationUtil.requireNonNegative(0, "n")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.requireNonNegative(-1, "n")).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"0, false", "1, true", "6, true", "7, false"})
        void credits(int credits, boolean expected) {
            assertThat(ValidationUtil.isValidCredits(credits)).isEqualTo(expected);
        }
    }

    @Nested
    class Dates {
        // Dates far from "today" keep these checks independent of the actual run date.
        private final LocalDate farPast = LocalDate.of(1990, 1, 1);
        private final LocalDate farFuture = LocalDate.of(2999, 1, 1);

        @Test
        void notFutureAndNotPast() {
            assertThatCode(() -> ValidationUtil.requireNotFuture(farPast, "d")).doesNotThrowAnyException();
            assertThatCode(() -> ValidationUtil.requireNotFuture(null, "d")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.requireNotFuture(farFuture, "Birth date"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Birth date cannot be in the future");
            assertThatCode(() -> ValidationUtil.requireNotPast(farFuture, "d")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.requireNotPast(farPast, "d"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void recentDateTime() {
            LocalDateTime now = LocalDateTime.now();
            assertThat(ValidationUtil.isRecentDateTime(now.minusHours(1))).isTrue();
            assertThat(ValidationUtil.isRecentDateTime(now.minusDays(2))).isFalse();
            assertThat(ValidationUtil.isRecentDateTime(null)).isFalse();
        }

        @Test
        void parseDate() {
            assertThat(ValidationUtil.parseDate(" 2024-02-29 ", "d")).isEqualTo(LocalDate.of(2024, 2, 29));
            assertThatThrownBy(() -> ValidationUtil.parseDate("2023-02-29", "Start"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Start");
            assertThatThrownBy(() -> ValidationUtil.parseDate("02/29/2024", "d"))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ValidationUtil.parseDate(" ", "d"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Academic {
        @ParameterizedTest
        @CsvSource({
            "2023-2024, true", "2020-2021, true", "2030-2031, true", "' 2024-2025 ', true",
            "2023-2025, false", "2019-2020, false", "2031-2032, false", "2023/2024, false", "23-24, false"
        })
        void academicYear(String value, boolean expected) {
            assertThat(ValidationUtil.isValidAcademicYear(value)).isEqualTo(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"Fall", "SPRING", " summer ", "winter"})
        void validSemesters(String s) {
            assertThat(ValidationUtil.isValidSemester(s)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"Autumn", "fall2024"})
        void invalidSemesters(String s) {
            assertThat(ValidationUtil.isValidSemester(s)).isFalse();
        }
    }

    @Nested
    class Passwords {
        @ParameterizedTest
        @ValueSource(strings = {"Passw0rd!", "Abcdef1#", "Z9y8x7w6?q"})
        void strong(String pw) {
            assertThat(ValidationUtil.isStrongPassword(pw)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"Pa0!", "password1!", "PASSWORD1!", "Password!!", "Password12", "        "})
        void weak(String pw) {
            assertThat(ValidationUtil.isStrongPassword(pw)).isFalse();
        }

        @Test
        void requirementsMentionEveryRule() {
            assertThat(ValidationUtil.getPasswordRequirements())
                .contains("8 characters", "uppercase", "lowercase", "digit", "special");
        }
    }

    @Nested
    class Collections {
        @Test
        void requireNonEmpty() {
            assertThatCode(() -> ValidationUtil.requireNonEmpty(List.of(1), "c")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.requireNonEmpty(List.of(), "Courses"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Courses cannot be null or empty");
            assertThatThrownBy(() -> ValidationUtil.requireNonEmpty(null, "c"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void collectionSizeBoundsAreInclusive() {
            assertThatCode(() -> ValidationUtil.validateCollectionSize(List.of(1, 2), 2, 2, "c")).doesNotThrowAnyException();
            assertThatThrownBy(() -> ValidationUtil.validateCollectionSize(List.of(1), 2, 3, "c"))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ValidationUtil.validateCollectionSize(List.of(1, 2, 3, 4), 2, 3, "c"))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ValidationUtil.validateCollectionSize(null, 0, 3, "c"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("c cannot be null");
        }
    }

    @Nested
    class Registration {
        @Test
        void acceptsValidDataWithOptionalPhone() {
            assertThatCode(() -> ValidationUtil.validateUserRegistration("Ada", "Lovelace", "ada@campus.edu", null))
                .doesNotThrowAnyException();
            assertThatCode(() -> ValidationUtil.validateUserRegistration("Ada", "Lovelace", "ada@campus.edu", "  "))
                .doesNotThrowAnyException();
            assertThatCode(() -> ValidationUtil.validateUserRegistration("Ada", "Lovelace", "ada@campus.edu", "555-123-4567"))
                .doesNotThrowAnyException();
        }

        @Test
        void rejectsEachInvalidField() {
            assertThatThrownBy(() -> ValidationUtil.validateUserRegistration(" ", "L", "a@b.co", null))
                .hasMessageContaining("First name");
            assertThatThrownBy(() -> ValidationUtil.validateUserRegistration("F", "x".repeat(51), "a@b.co", null))
                .hasMessageContaining("Last name");
            assertThatThrownBy(() -> ValidationUtil.validateUserRegistration("F", "L", "nope", null))
                .hasMessage("Invalid email format");
            assertThatThrownBy(() -> ValidationUtil.validateUserRegistration("F", "L", "a@b.co", "123"))
                .hasMessage("Invalid phone number format");
        }
    }
}
