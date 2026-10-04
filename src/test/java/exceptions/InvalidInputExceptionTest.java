package exceptions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidInputExceptionTest {

    @Nested
    class Constructors {

        @Test
        void defaultConstructor() {
            InvalidInputException e = new InvalidInputException();

            assertThat(e).isInstanceOf(RuntimeException.class).hasMessage("Invalid input provided");
            assertThat(e.getErrorCode()).isEqualTo("INVALID_INPUT");
            assertThat(e.getFieldName()).isNull();
            assertThat(e.getInvalidValue()).isNull();
            assertThat(e.hasFieldInfo()).isFalse();
        }

        @Test
        void messageAndCauseConstructors() {
            Throwable cause = new NumberFormatException("NaN");

            assertThat(new InvalidInputException("bad")).hasMessage("bad").hasNoCause();
            assertThat(new InvalidInputException("bad", cause)).hasMessage("bad").hasCause(cause);
            InvalidInputException causeOnly = new InvalidInputException(cause);
            assertThat(causeOnly).hasCause(cause).hasMessage(cause.toString());
            assertThat(causeOnly.getErrorCode()).isEqualTo("INVALID_INPUT");
        }

        @Test
        void fieldConstructors() {
            InvalidInputException basic = new InvalidInputException("age", -1);
            assertThat(basic).hasMessage("Invalid value for field 'age': -1");
            assertThat(basic.getErrorCode()).isEqualTo("INVALID_FIELD_VALUE");
            assertThat(basic.getFieldName()).isEqualTo("age");
            assertThat(basic.getInvalidValue()).isEqualTo(-1);

            assertThat(new InvalidInputException("age", -1, "must be positive"))
                    .hasMessage("Invalid value for field 'age': -1. must be positive");

            InvalidInputException coded = new InvalidInputException("NEG", "age", -1, "must be positive");
            assertThat(coded).hasMessage("[NEG] Invalid value for field 'age': -1. must be positive");
            assertThat(coded.getErrorCode()).isEqualTo("NEG");

            Throwable cause = new IllegalStateException();
            assertThat(new InvalidInputException("NEG", "age", -1, "m", cause)).hasCause(cause);
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void nullOrEmpty() {
            InvalidInputException e = InvalidInputException.nullOrEmpty("name");

            assertThat(e.getErrorCode()).isEqualTo("NULL_OR_EMPTY");
            assertThat(e).hasMessage("[NULL_OR_EMPTY] Invalid value for field 'name': null. Field cannot be null or empty");
            assertThat(e.getInvalidValue()).isNull();
        }

        @Test
        void formatRangeAndLength() {
            assertThat(InvalidInputException.invalidFormat("date", "x", "yyyy-MM-dd").getMessage())
                    .endsWith("Expected format: yyyy-MM-dd");
            InvalidInputException range = InvalidInputException.outOfRange("credits", 30, 1, 21);
            assertThat(range.getErrorCode()).isEqualTo("OUT_OF_RANGE");
            assertThat(range.getMessage()).endsWith("Value must be between 1 and 21");
            InvalidInputException length = InvalidInputException.invalidLength("code", 2, 3, 8);
            assertThat(length.getInvalidValue()).isEqualTo(2);
            assertThat(length.getMessage()).endsWith("Length must be between 3 and 8 characters");
        }

        @Test
        void domainSpecificFactories() {
            assertThat(InvalidInputException.duplicateValue("email", "a@b").getErrorCode()).isEqualTo("DUPLICATE_VALUE");
            InvalidInputException email = InvalidInputException.invalidEmail("nope");
            assertThat(email.getFieldName()).isEqualTo("email");
            assertThat(email.getErrorCode()).isEqualTo("INVALID_EMAIL");
            InvalidInputException phone = InvalidInputException.invalidPhoneNumber("123");
            assertThat(phone.getFieldName()).isEqualTo("phoneNumber");
            InvalidInputException gpa = InvalidInputException.invalidGPA(4.5);
            assertThat(gpa.getInvalidValue()).isEqualTo(4.5);
            assertThat(gpa.getMessage()).isEqualTo("[INVALID_GPA] Invalid value for field 'gpa': 4.5. GPA must be between 0.0 and 4.0");
            InvalidInputException enrollment = InvalidInputException.invalidEnrollment("CS101", "closed");
            assertThat(enrollment.getFieldName()).isEqualTo("courseId");
            assertThat(enrollment.getMessage()).endsWith("'courseId': CS101. closed");
        }
    }

    @Nested
    class Reporting {

        @Test
        void userFriendlyMessageDependsOnFieldInfo() {
            assertThat(InvalidInputException.invalidEmail("x").getUserFriendlyMessage())
                    .isEqualTo("Invalid input for email. Please check your entry and try again.");
            assertThat(new InvalidInputException().getUserFriendlyMessage())
                    .isEqualTo("Invalid input provided. Please check your entries and try again.");
            assertThat(new InvalidInputException("  ", 1).hasFieldInfo()).isFalse();
        }

        @Test
        void detailedReportIncludesPresentDetails() {
            InvalidInputException e = new InvalidInputException("NEG", "age", -1, "m", new IllegalStateException("root"));

            assertThat(e.getDetailedReport())
                    .startsWith("InvalidInputException Details:\n")
                    .contains("Error Code: NEG\n", "Field Name: age\n", "Invalid Value: -1\n",
                            "Value Type: Integer\n", "Cause: root\n");
            assertThat(new InvalidInputException().getDetailedReport())
                    .doesNotContain("Field Name:", "Invalid Value:", "Value Type:", "Cause:");
        }

        @Test
        void toStringPrefixesNonDefaultCode() {
            assertThat(new InvalidInputException("bad")).hasToString("InvalidInputException: bad");
            assertThat(new InvalidInputException("age", 1).toString())
                    .isEqualTo("InvalidInputException: [INVALID_FIELD_VALUE] Invalid value for field 'age': 1");
        }

        // Regression: withContext used to push the already-formatted message through the
        // "[code] Invalid value for field ..." template a second time, duplicating it.
        @Test
        void withContextAppendsContextWithoutReformattingTheMessage() {
            Throwable cause = new IllegalStateException();
            InvalidInputException original = new InvalidInputException("NEG", "age", -1, "must be positive", cause);

            InvalidInputException copy = original.withContext("row 7");

            assertThat(copy).isNotSameAs(original)
                    .hasMessage("[NEG] Invalid value for field 'age': -1. must be positive Context: row 7")
                    .hasCause(cause);
            assertThat(copy.getErrorCode()).isEqualTo("NEG");
            assertThat(copy.getFieldName()).isEqualTo("age");
            assertThat(copy.getInvalidValue()).isEqualTo(-1);
        }

        // Regression: a plain-message exception gained a bogus "Invalid value for field 'null'" prefix.
        @Test
        void withContextOnPlainMessageKeepsItPlain() {
            InvalidInputException copy = new InvalidInputException("bad input").withContext("import");

            assertThat(copy).hasMessage("bad input Context: import");
            assertThat(copy.getErrorCode()).isEqualTo("INVALID_INPUT");
            assertThat(copy.toString()).isEqualTo("InvalidInputException: bad input Context: import");
        }
    }
}
