package exceptions;

import exceptions.ValidationException.ErrorCode;
import exceptions.ValidationException.ValidationError;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationExceptionTest {

    private static ValidationError error(String field, ErrorCode code, String message) {
        return new ValidationError(field, "bad", code, message);
    }

    private static ValidationException twoFieldException() {
        return ValidationException.builder("Student")
                .addError("email", "x", ErrorCode.INVALID_EMAIL, "email invalid")
                .addError("email", "x", ErrorCode.INVALID_LENGTH, "email too short")
                .addError("gpa", 5.0, ErrorCode.INVALID_RANGE, "gpa out of range")
                .addError(new ValidationError(null, null, ErrorCode.BUSINESS_RULE_VIOLATION, "global rule"))
                .addGlobalContext("source", "import")
                .build();
    }

    @Nested
    class ValidationErrors {

        @Test
        void exposesFieldsAndDefensiveContext() {
            Map<String, Object> ctx = new HashMap<>(Map.of("min", 1));
            ValidationError e = new ValidationError("age", -1, ErrorCode.INVALID_RANGE, "too small", ctx);
            ctx.put("min", 99);

            assertThat(e.getFieldName()).isEqualTo("age");
            assertThat(e.getRejectedValue()).isEqualTo(-1);
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_RANGE);
            assertThat(e.getMessage()).isEqualTo("too small");
            assertThat(e.getContext()).containsOnly(Map.entry("min", 1));
            e.getContext().clear();
            assertThat(e.getContext("min")).isEqualTo(1);
            assertThat(e.getContext("min", Integer.class)).isEqualTo(1);
            assertThat(e.getContext("min", String.class)).isNull();
            assertThat(e.getContext("missing", Integer.class)).isNull();
        }

        @Test
        void nullContextBecomesEmpty() {
            assertThat(new ValidationError("f", null, ErrorCode.INVALID_FORMAT, "m", null).getContext()).isEmpty();
            assertThat(error("f", ErrorCode.INVALID_FORMAT, "m").getContext()).isEmpty();
        }

        @Test
        void equalityIgnoresMessage() {
            ValidationError a = error("f", ErrorCode.INVALID_FORMAT, "one");
            ValidationError b = error("f", ErrorCode.INVALID_FORMAT, "two");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isEqualTo(a);
            assertThat(a).isNotEqualTo(error("g", ErrorCode.INVALID_FORMAT, "one"))
                    .isNotEqualTo(error("f", ErrorCode.INVALID_EMAIL, "one"))
                    .isNotEqualTo(new ValidationError("f", "other", ErrorCode.INVALID_FORMAT, "one"))
                    .isNotEqualTo(null)
                    .isNotEqualTo("f");
        }

        @Test
        void toStringShowsAllParts() {
            assertThat(error("f", ErrorCode.INVALID_FORMAT, "m").toString())
                    .isEqualTo("ValidationError{field='f', value=bad, code=INVALID_FORMAT, message='m'}");
        }

        @Test
        void codesAreUnique() {
            assertThat(Stream.of(ErrorCode.values()).map(ErrorCode::getCode)).doesNotHaveDuplicates()
                    .allMatch(c -> c.startsWith("VAL_"));
            assertThat(ErrorCode.INVALID_EMAIL.getDescription()).isEqualTo("Email format is invalid");
        }
    }

    @Nested
    class Messages {

        @Test
        void singleErrorMessage() {
            ValidationException e = new ValidationException("name", null, ErrorCode.REQUIRED_FIELD_MISSING, "name required");

            assertThat(e.getMessage()).isEqualTo("Validation failed: name required");
            assertThat(e.getObjectName()).isNull();
            assertThat(e.getErrorCount()).isEqualTo(1);
        }

        @Test
        void multipleErrorMessage() {
            ValidationException e = new ValidationException(List.of(
                    error("a", ErrorCode.INVALID_FORMAT, "a bad"), error("b", ErrorCode.INVALID_FORMAT, "b bad")));

            assertThat(e.getMessage()).isEqualTo("Validation failed with 2 errors: a bad; b bad");
        }

        @Test
        void emptyOrNullErrorListMessage() {
            assertThat(new ValidationException(List.of()).getMessage()).isEqualTo("Validation failed");
            ValidationException nullList = new ValidationException((List<ValidationError>) null);
            assertThat(nullList.getMessage()).isEqualTo("Validation failed");
            assertThat(nullList.hasErrors()).isFalse();
        }

        // Regression: the object name used to produce "Validation failed for User: : msg"
        // (double colon), "... User:  with 2 errors" and a dangling "User: " for no errors.
        @Test
        void objectNameIsSplicedInWithoutDuplicatePunctuation() {
            assertThat(new ValidationException("User", List.of(error("a", ErrorCode.INVALID_FORMAT, "a bad")))
                    .getMessage()).isEqualTo("Validation failed for User: a bad");
            assertThat(new ValidationException("User", List.of(
                    error("a", ErrorCode.INVALID_FORMAT, "a bad"), error("b", ErrorCode.INVALID_FORMAT, "b bad")))
                    .getMessage()).isEqualTo("Validation failed for User with 2 errors: a bad; b bad");
            assertThat(new ValidationException("User", List.of()).getMessage())
                    .isEqualTo("Validation failed for User");
        }

        @Test
        void blankObjectNameIsIgnored() {
            assertThat(new ValidationException("  ", List.of(error("a", ErrorCode.INVALID_FORMAT, "x"))).getMessage())
                    .isEqualTo("Validation failed: x");
        }
    }

    @Nested
    class Construction {

        @Test
        void listConstructorCopiesInput() {
            List<ValidationError> errors = new ArrayList<>(List.of(error("a", ErrorCode.INVALID_FORMAT, "m")));
            ValidationException e = new ValidationException("Obj", errors);
            errors.clear();

            assertThat(e.getErrorCount()).isEqualTo(1);
            assertThat(e.getObjectName()).isEqualTo("Obj");
            e.getValidationErrors().clear();
            assertThat(e.getValidationErrors()).hasSize(1);
        }

        @Test
        void fullConstructorCopiesContextAndChainsCause() {
            Map<String, Object> ctx = new HashMap<>(Map.of("k", "v"));
            RuntimeException cause = new RuntimeException("parse");
            ValidationException e = new ValidationException("Obj", null, ctx, cause);
            ctx.put("k", "changed");

            assertThat(e).hasCause(cause);
            assertThat(e.getGlobalContext()).containsOnly(Map.entry("k", "v"));
            assertThat(e.getValidationErrors()).isEmpty();
            assertThat(new ValidationException("Obj", null, (Map<String, Object>) null, null).getGlobalContext()).isEmpty();
            assertThat(e.getTimestamp()).isNotNull();
        }

        @Test
        void builderVariants() {
            ValidationException e = ValidationException.builder()
                    .objectName("Course")
                    .addErrors(List.of(error("a", ErrorCode.INVALID_FORMAT, "m")))
                    .addGlobalContext(Map.of("x", 1))
                    .cause(new IllegalStateException())
                    .build();

            assertThat(e.getObjectName()).isEqualTo("Course");
            assertThat(e.getGlobalContext()).containsEntry("x", 1);
            assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
            assertThat(new ValidationException.Builder("X").build().getObjectName()).isEqualTo("X");
        }
    }

    @Nested
    class Queries {

        @Test
        void perFieldQueries() {
            ValidationException e = twoFieldException();

            assertThat(e.hasErrors()).isTrue();
            assertThat(e.getErrorCount()).isEqualTo(4);
            assertThat(e.getErrorsForField("email")).hasSize(2);
            assertThat(e.getErrorCountForField("email")).isEqualTo(2);
            assertThat(e.getErrorCountForField("nope")).isZero();
            assertThat(e.hasErrorsForField("gpa")).isTrue();
            assertThat(e.hasErrorsForField("nope")).isFalse();
            assertThat(e.getErrorsForField(null)).extracting(ValidationError::getMessage).containsExactly("global rule");
            assertThat(e.getFieldsWithErrors()).containsExactlyInAnyOrder("email", "gpa");
        }

        @Test
        void perCodeQueries() {
            ValidationException e = twoFieldException();

            assertThat(e.getErrorsByCode(ErrorCode.INVALID_RANGE)).extracting(ValidationError::getFieldName)
                    .containsExactly("gpa");
            assertThat(e.hasErrorsOfType(ErrorCode.INVALID_EMAIL)).isTrue();
            assertThat(e.hasErrorsOfType(ErrorCode.INVALID_SSN)).isFalse();
            assertThat(e.getErrorCodes()).containsExactlyInAnyOrder(ErrorCode.INVALID_EMAIL, ErrorCode.INVALID_LENGTH,
                    ErrorCode.INVALID_RANGE, ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        @Test
        void messages() {
            ValidationException e = twoFieldException();

            assertThat(e.getUserFriendlyMessages()).containsExactly("email invalid", "email too short",
                    "gpa out of range", "global rule");
            assertThat(e.getUserFriendlyMessages("email")).containsExactly("email invalid", "email too short");
            assertThat(e.getFieldErrorMessages()).containsOnly(
                    Map.entry("email", List.of("email invalid", "email too short")),
                    Map.entry("gpa", List.of("gpa out of range")));
        }

        @Test
        void globalContextLookups() {
            ValidationException e = twoFieldException();

            assertThat(e.getGlobalContext("source")).isEqualTo("import");
            assertThat(e.getGlobalContext("source", String.class)).isEqualTo("import");
            assertThat(e.getGlobalContext("source", Integer.class)).isNull();
            assertThat(e.getGlobalContext("absent", String.class)).isNull();
            e.getGlobalContext().clear();
            assertThat(e.getGlobalContext()).isNotEmpty();
        }

        @Test
        void mergeCombinesErrorsAndContext() {
            ValidationException first = ValidationException.builder("A")
                    .addError("a", 1, ErrorCode.INVALID_FORMAT, "a bad").addGlobalContext("k1", 1).build();
            ValidationException second = ValidationException.builder("B")
                    .addError("b", 2, ErrorCode.INVALID_FORMAT, "b bad").addGlobalContext("k2", 2).build();

            ValidationException merged = first.merge(second);

            assertThat(merged.getObjectName()).isEqualTo("A");
            assertThat(merged.getUserFriendlyMessages()).containsExactly("a bad", "b bad");
            assertThat(merged.getGlobalContext()).containsOnly(Map.entry("k1", 1), Map.entry("k2", 2));
            assertThat(first.merge(null)).isSameAs(first);
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void requiredField() {
            ValidationException e = ValidationException.requiredField("name");

            assertThat(e.getMessage()).isEqualTo("Validation failed: Field 'name' is required");
            assertThat(e.getValidationErrors().get(0).getErrorCode()).isEqualTo(ErrorCode.REQUIRED_FIELD_MISSING);
        }

        @Test
        void invalidFormat() {
            ValidationError err = ValidationException.invalidFormat("date", "2024/13", "yyyy-MM-dd")
                    .getValidationErrors().get(0);

            assertThat(err.getMessage()).isEqualTo("Field 'date' has invalid format. Expected: yyyy-MM-dd");
            assertThat(err.getContext("expectedFormat")).isEqualTo("yyyy-MM-dd");
        }

        @Test
        void invalidLengthRecordsActualLength() {
            ValidationError err = ValidationException.invalidLength("code", "ab", 3, 5).getValidationErrors().get(0);
            ValidationError nullValue = ValidationException.invalidLength("code", null, 3, 5).getValidationErrors().get(0);

            assertThat(err.getMessage()).isEqualTo("Field 'code' length must be between 3 and 5 characters");
            assertThat(err.getContext()).containsEntry("actualLength", 2).containsEntry("minLength", 3)
                    .containsEntry("maxLength", 5);
            assertThat(nullValue.getContext("actualLength")).isEqualTo(0);
        }

        @Test
        void invalidRange() {
            ValidationError err = ValidationException.invalidRange("gpa", 4.5, 0.0, 4.0).getValidationErrors().get(0);

            assertThat(err.getMessage()).isEqualTo("Field 'gpa' must be between 0.0 and 4.0");
            assertThat(err.getContext()).containsEntry("minValue", 0.0).containsEntry("maxValue", 4.0);
        }

        @Test
        void emailAndPhone() {
            assertThat(ValidationException.invalidEmail("email", "x").getValidationErrors().get(0).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_EMAIL);
            assertThat(ValidationException.invalidPhone("phone", "1").getMessage())
                    .isEqualTo("Validation failed: Field 'phone' must be a valid phone number");
        }

        @Test
        void duplicateAndReference() {
            ValidationError dup = ValidationException.duplicateValue("email", "a@b.c", "Student")
                    .getValidationErrors().get(0);
            assertThat(dup.getMessage()).isEqualTo("Value 'a@b.c' already exists for field 'email' in Student");
            assertThat(dup.getContext("entity")).isEqualTo("Student");

            ValidationError ref = ValidationException.invalidReference("deptId", "D9", "Department")
                    .getValidationErrors().get(0);
            assertThat(ref.getMessage()).isEqualTo("Referenced Department with ID 'D9' does not exist");
            assertThat(ref.getRejectedValue()).isEqualTo("D9");
        }

        @Test
        void businessRuleViolationUsesGlobalContext() {
            ValidationException e = ValidationException.businessRuleViolation("maxCredits", "too many credits");

            assertThat(e.getValidationErrors().get(0).getFieldName()).isNull();
            assertThat(e.getMessage()).isEqualTo("Validation failed: Business rule violation: maxCredits - too many credits");
            assertThat(e.getGlobalContext()).containsEntry("ruleName", "maxCredits")
                    .containsEntry("ruleDescription", "too many credits");
            assertThat(e.getFieldsWithErrors()).isEmpty();
        }

        @Test
        void crossFieldValidation() {
            ValidationError err = ValidationException.crossFieldValidation(new String[]{"start", "end"}, "end before start")
                    .getValidationErrors().get(0);

            assertThat(err.getFieldName()).isEqualTo("start,end");
            assertThat(err.getContext("relatedFields", List.class)).isEqualTo(List.of("start", "end"));
        }
    }

    @Nested
    class Serialization {

        @Test
        void structuredMapGroupsErrorsByFieldWithGlobalBucket() {
            Map<String, Object> map = twoFieldException().toStructuredMap();

            assertThat(map).containsEntry("objectName", "Student").containsEntry("errorCount", 4)
                    .containsEntry("globalContext", Map.of("source", "import"))
                    .containsKeys("timestamp", "message");
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> fieldErrors =
                    (Map<String, List<Map<String, Object>>>) map.get("fieldErrors");
            assertThat(fieldErrors).containsOnlyKeys("email", "gpa", "global");
            assertThat(fieldErrors.get("email")).hasSize(2);
            assertThat(fieldErrors.get("gpa").get(0)).containsEntry("code", "VAL_004")
                    .containsEntry("rejectedValue", 5.0).containsEntry("message", "gpa out of range");
        }

        @Test
        void structuredMapOmitsEmptyGlobalContext() {
            assertThat(ValidationException.requiredField("x").toStructuredMap()).doesNotContainKey("globalContext");
        }

        @Test
        void toStringIncludesErrorsContextAndCause() {
            ValidationException e = ValidationException.builder("Obj")
                    .addError("f", 1, ErrorCode.INVALID_FORMAT, "m")
                    .addGlobalContext("k", "v")
                    .cause(new IllegalArgumentException("why"))
                    .build();

            assertThat(e.toString()).startsWith("ValidationException{objectName='Obj', errorCount=1")
                    .contains("errors=[ValidationError{field='f'", "globalContext={k=v}",
                            "cause=IllegalArgumentException: why")
                    .endsWith("}");
            assertThat(new ValidationException(List.of()).toString())
                    .doesNotContain("errors=[", "globalContext=", "cause=");
        }
    }
}
