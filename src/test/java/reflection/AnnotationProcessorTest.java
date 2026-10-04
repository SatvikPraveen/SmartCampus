package reflection;

import annotations.Async;
import annotations.Audited;
import annotations.Cacheable;
import annotations.Entity;
import annotations.Validator;
import annotations.Validator.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reflection.AnnotationProcessor.AnnotationMetadata;
import reflection.AnnotationProcessor.ProcessingResult;
import reflection.AnnotationProcessor.ValidationResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationProcessorTest {

    // ==================== FIXTURES ====================

    @Entity(table = "people", cacheTTL = 120)
    static class TableEntity { }

    @Entity(cacheTTL = 0)
    static class UncachedEntity { }

    @Cacheable(cacheName = "things")
    @Audited
    @Async
    static class FullyAnnotated { }

    static class Plain { }

    static class Service {
        @Cacheable
        public String lookup(String id, Object extra) { return id; }

        @Cacheable(key = "#id", cacheName = "custom")
        public String keyed(String id) { return id; }

        @Audited(value = Audited.AuditType.UPDATE, includeParameters = true, tags = {"t1"})
        public void update(String id) { }

        @Audited(includeParameters = true)
        public void auditedNoArgs() { }

        @Async(strategy = Async.Strategy.FUTURE)
        public void background() { }

        public void ping() { }

        public void guarded(@Validator(type = Type.NOT_NULL, message = "id required") String id,
                            @Validator(type = Type.POSITIVE, message = "amount must be > 0") int amount) { }
    }

    static class Form {
        @Validator(type = Type.NOT_NULL)
        Object required;

        @Validator(type = Type.NOT_EMPTY)
        String name;

        @Validator(type = Type.NOT_EMPTY)
        List<String> tags;

        @Validator(type = Type.MIN_LENGTH, min = 3)
        @Validator(type = Type.MAX_LENGTH, max = 5)
        String code;

        @Validator(type = Type.EMAIL, message = "bad email")
        String email;

        @Validator(type = Type.REGEX, pattern = "[A-Z]{2}\\d{3}")
        String course;

        @Validator(type = Type.POSITIVE)
        Number credits;

        @Validator(type = Type.CUSTOM)
        String special;

        @Validator(type = Type.MIN_LENGTH, min = 3)
        Integer notAString;

        String unvalidated;

        static Form valid() {
            Form f = new Form();
            f.required = new Object();
            f.name = "Alice";
            f.tags = List.of("a");
            f.code = "ABCD";
            f.email = "a@b.edu";
            f.course = "CS101";
            f.credits = 3;
            f.special = "x";
            f.notAString = 1;
            return f;
        }
    }

    private AnnotationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new AnnotationProcessor();
    }

    private static Method method(String name, Class<?>... params) throws NoSuchMethodException {
        return Service.class.getMethod(name, params);
    }

    // ==================== TESTS ====================

    @Nested
    class ProcessClass {

        @Test
        void entityAnnotationProducesMetadataAndMessage() {
            ProcessingResult result = processor.processClass(TableEntity.class);

            assertThat(result.getMetadata()).containsKey("entity");
            assertThat(result.getMetadata().get("entity")).isNotNull();
            assertThat(result.getMessages()).containsExactly("Entity annotation processed for: TableEntity");
        }

        @Test
        void entityWithoutCacheTtlIsStillProcessed() {
            ProcessingResult result = processor.processClass(UncachedEntity.class);

            assertThat(result.getMetadata()).containsKey("entity");
            assertThat(result.getMessages()).hasSize(1);
        }

        @Test
        void cacheableAuditedAndAsyncClassAnnotationsAreAllReported() {
            ProcessingResult result = processor.processClass(FullyAnnotated.class);

            assertThat(result.getMessages()).containsExactly(
                    "Cacheable annotation processed for class: FullyAnnotated",
                    "Audited annotation processed for class: FullyAnnotated",
                    "Async annotation processed for class: FullyAnnotated");
            assertThat(result.getMetadata()).isEmpty();
        }

        @Test
        void unannotatedClassYieldsEmptyResult() {
            ProcessingResult result = processor.processClass(Plain.class);

            assertThat(result.getMessages()).isEmpty();
            assertThat(result.getMetadata()).isEmpty();
            assertThat(result.hasCachedResult()).isFalse();
            assertThat(result.isAsyncExecution()).isFalse();
        }

        @Test
        void processingSameClassTwiceIsStable() {
            ProcessingResult first = processor.processClass(TableEntity.class);
            ProcessingResult second = new AnnotationProcessor().processClass(TableEntity.class);

            assertThat(second.getMessages()).isEqualTo(first.getMessages());
        }
    }

    @Nested
    class ProcessMethod {

        @Test
        void cacheableMethodReportsMissWhenNothingCached() throws Exception {
            ProcessingResult result = processor.processMethod(
                    method("lookup", String.class, Object.class), new Service(), new Object[]{"S1", null});

            assertThat(result.getMessages()).containsExactly("Cache miss for method: lookup");
            assertThat(result.hasCachedResult()).isFalse();
            assertThat(result.getCachedResult()).isNull();
        }

        @Test
        void cacheableMethodWithExplicitKeyAndCacheName() throws Exception {
            ProcessingResult result = processor.processMethod(
                    method("keyed", String.class), new Service(), new Object[]{"S1"});

            assertThat(result.getMessages()).containsExactly("Cache miss for method: keyed");
        }

        @Test
        void auditedMethodRecordsEvent() throws Exception {
            ProcessingResult result = processor.processMethod(
                    method("update", String.class), new Service(), new Object[]{"S1"});

            assertThat(result.getMessages()).containsExactly("Audit event recorded for method: update");
        }

        @Test
        void asyncMethodIsScheduled() throws Exception {
            ProcessingResult result = processor.processMethod(method("background"), new Service(), new Object[0]);

            assertThat(result.isAsyncExecution()).isTrue();
            assertThat(result.getMessages()).containsExactly("Async execution scheduled for method: background");
        }

        @Test
        void unannotatedMethodProducesNoMessages() throws Exception {
            ProcessingResult result = processor.processMethod(method("ping"), new Service(), new Object[0]);

            assertThat(result.getMessages()).isEmpty();
            assertThat(result.getValidationResult().isValid()).isTrue();
        }

        @Test
        void parameterValidatorsReportViolationsWithCustomMessages() throws Exception {
            ProcessingResult result = processor.processMethod(
                    method("guarded", String.class, int.class), new Service(), new Object[]{null, -5});

            ValidationResult validation = result.getValidationResult();
            assertThat(validation.isValid()).isFalse();
            assertThat(validation.getErrors()).containsExactly("id required", "amount must be > 0");
        }

        @Test
        void parameterValidatorsPassForValidArguments() throws Exception {
            ProcessingResult result = processor.processMethod(
                    method("guarded", String.class, int.class), new Service(), new Object[]{"S1", 3});

            assertThat(result.getValidationResult().isValid()).isTrue();
        }

        @Test
        void fewerArgumentsThanParametersOnlyValidatesSuppliedOnes() throws Exception {
            ProcessingResult result = processor.processMethod(
                    method("guarded", String.class, int.class), new Service(), new Object[]{null});

            assertThat(result.getValidationResult().getErrors()).containsExactly("id required");
        }

        // A no-arg invocation passes args == null (Method.invoke / InvocationHandler convention).
        @Test
        void noArgMethodWithNullArgsDoesNotThrow() throws Exception {
            ProcessingResult result = processor.processMethod(method("ping"), new Service(), null);

            assertThat(result.getMessages()).isEmpty();
            assertThat(result.getValidationResult().isValid()).isTrue();
        }

        // Regression: @Audited(includeParameters = true) on a no-arg method called with args == null
        // threw NullPointerException from Arrays.asList(null).
        @Test
        void auditedNoArgMethodWithNullArgsRecordsEvent() throws Exception {
            ProcessingResult result = processor.processMethod(method("auditedNoArgs"), new Service(), null);

            assertThat(result.getMessages()).containsExactly("Audit event recorded for method: auditedNoArgs");
        }
    }

    @Nested
    class FieldValidation {

        @Test
        void fullyValidObjectHasNoErrors() {
            ValidationResult result = processor.processValidation(Form.valid());

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        void customValidatorIsReportedAsUnimplementedWarning() {
            ValidationResult result = processor.processValidation(Form.valid());

            assertThat(result.getWarnings()).containsExactly("Custom validation not implemented for field: special");
        }

        @Test
        void everyViolationIsReportedWithDefaultOrCustomMessage() {
            Form f = Form.valid();
            f.required = null;
            f.name = "";
            f.tags = new ArrayList<>();
            f.code = "AB";
            f.email = "not-an-email";
            f.course = "cs-101";
            f.credits = 0;

            ValidationResult result = processor.processValidation(f);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactlyInAnyOrder(
                    "required cannot be null",
                    "name cannot be empty",
                    "tags cannot be empty",
                    "code must be at least 3 characters",
                    "bad email",
                    "course does not match required pattern",
                    "credits must be positive");
        }

        @Test
        void repeatedValidatorsOnOneFieldAreAllApplied() {
            Form f = Form.valid();
            f.code = "TOOLONG";

            assertThat(processor.processValidation(f).getErrors())
                    .containsExactly("code must be at most 5 characters");
        }

        @Test
        void nullValueFailsNotEmpty() {
            Form f = Form.valid();
            f.name = null;

            assertThat(processor.processValidation(f).getErrors()).containsExactly("name cannot be empty");
        }

        @Test
        void typeSpecificValidatorsIgnoreValuesOfOtherTypes() {
            Form f = Form.valid();
            f.code = null;    // MIN/MAX_LENGTH only apply to strings
            f.email = null;   // EMAIL only applies to strings
            f.course = null;  // REGEX only applies to strings
            f.credits = null; // POSITIVE only applies to numbers

            assertThat(processor.processValidation(f).isValid()).isTrue();
        }
    }

    @Nested
    class ResultContainers {

        @Test
        void processingResultReturnsDefensiveCopies() {
            ProcessingResult result = new ProcessingResult();
            result.addMessage("m");
            result.addMetadata("k", "v");
            result.setCachedResult("cached");
            result.setAsyncExecution(true);

            result.getMessages().clear();
            result.getMetadata().clear();

            assertThat(result.getMessages()).containsExactly("m");
            assertThat(result.getMetadata()).containsEntry("k", "v");
            assertThat(result.hasCachedResult()).isTrue();
            assertThat(result.getCachedResult()).isEqualTo("cached");
            assertThat(result.isAsyncExecution()).isTrue();
        }

        @Test
        void validationResultTracksErrorsAndWarningsSeparately() {
            ValidationResult result = new ValidationResult();
            result.addWarning("w");
            assertThat(result.isValid()).isTrue();

            result.addError("e");
            result.getErrors().clear();

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactly("e");
            assertThat(result.getWarnings()).containsExactly("w");
        }

        @Test
        void annotationMetadataBuilderIndexesClassMethodAndFieldAnnotations() throws Exception {
            Entity entity = TableEntity.class.getAnnotation(Entity.class);
            Method update = method("update", String.class);
            Annotation audited = update.getAnnotation(Audited.class);
            java.lang.reflect.Field code = Form.class.getDeclaredField("email");
            Annotation validator = code.getAnnotation(Validator.class);

            AnnotationMetadata metadata = new AnnotationMetadata.Builder(TableEntity.class)
                    .addAnnotation(Entity.class, entity)
                    .addMethodAnnotation(update, Audited.class, audited)
                    .addFieldAnnotation(code, Validator.class, validator)
                    .build();

            assertThat(metadata.getClazz()).isEqualTo(TableEntity.class);
            assertThat(metadata.hasClassAnnotation(Entity.class)).isTrue();
            assertThat(metadata.hasClassAnnotation(Cacheable.class)).isFalse();
            assertThat(metadata.getClassAnnotation(Entity.class).table()).isEqualTo("people");
            assertThat(metadata.getClassAnnotations()).containsOnlyKeys(Entity.class);
            assertThat(metadata.getMethodAnnotations().get(update)).containsEntry(Audited.class, audited);
            assertThat(metadata.getFieldAnnotations().get(code)).containsEntry(Validator.class, validator);
        }
    }
}
