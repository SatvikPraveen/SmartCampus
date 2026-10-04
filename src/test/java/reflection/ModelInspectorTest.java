package reflection;

import annotations.Audited;
import annotations.Entity;
import annotations.Validator;
import annotations.Validator.Type;
import models.Student;
import models.Student.AcademicYear;
import models.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reflection.ModelInspector.AnnotationInfo;
import reflection.ModelInspector.ClassInfo;
import reflection.ModelInspector.ConstructorInfo;
import reflection.ModelInspector.FieldInfo;
import reflection.ModelInspector.MethodInfo;
import reflection.ModelInspector.ValidationResult;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelInspectorTest {

    // ==================== FIXTURES ====================

    @Entity(table = "widgets", version = "2.0")
    static class Widget implements Comparable<Widget> {
        static final String KIND = "widget";

        @Validator(type = Type.NOT_EMPTY, message = "name required")
        private String name;

        private transient int cachedHash;
        private volatile boolean dirty;
        private final List<String> tags = new ArrayList<>();

        Widget() { }

        @Deprecated
        Widget(@Validator(type = Type.NOT_NULL) String name) throws IOException {
            this.name = name;
        }

        @Audited(category = "rename")
        public synchronized void rename(@Validator(type = Type.NOT_EMPTY) String newName) { this.name = newName; }

        public static Widget of(String name) throws IOException { return new Widget(name); }

        public final List<String> getTags() { return tags; }

        String describe(Object o) { return "object"; }

        String describe(String s) { return "string:" + s; }

        int add(int a, long b) { return (int) (a + b); }

        @Override
        public int compareTo(Widget o) { return 0; }
    }

    abstract static class Shape {
        abstract double area();
    }

    static class Square extends Shape {
        double side = 2;
        @Override double area() { return side * side; }
    }

    /** Used only by the concurrency test so the cache is cold for it. */
    static class ColdCacheFixture {
        int a;
        String b;
    }

    static class NoDefaultConstructor {
        NoDefaultConstructor(int ignored) { }
    }

    private static Student sampleStudent() {
        return new Student("USR1", "Alice", "Johnson", "alice@university.edu", "(555) 123-4567",
                "STU123456", "Computer Science", AcademicYear.SOPHOMORE);
    }

    private static FieldInfo field(ClassInfo info, String name) {
        return info.getFields().stream().filter(f -> f.getName().equals(name)).findFirst().orElseThrow();
    }

    private static MethodInfo method(ClassInfo info, String name) {
        return info.getMethods().stream().filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
    }

    // ==================== TESTS ====================

    @Nested
    class InspectRealModel {

        @Test
        void describesStudentClassAndHierarchy() {
            ClassInfo info = ModelInspector.inspectClass(Student.class);

            assertThat(info.getClazz()).isEqualTo(Student.class);
            assertThat(info.getSimpleName()).isEqualTo("Student");
            assertThat(info.getPackageName()).isEqualTo("models");
            assertThat(info.getCanonicalName()).isEqualTo("models.Student");
            assertThat(info.getSuperClass()).isEqualTo(User.class);
            assertThat(Modifier.isPublic(info.getModifiers())).isTrue();
            assertThat(info.isInterface()).isFalse();
            assertThat(info.isAbstract()).isFalse();
            assertThat(info.isEnum()).isFalse();
            assertThat(info.isAnnotation()).isFalse();
            assertThat(info.getInnerClasses()).contains(AcademicYear.class);
            assertThat(info.getConstructors()).hasSize(2);
        }

        @Test
        void fieldsIncludeInheritedOnes() {
            ClassInfo info = ModelInspector.inspectClass(Student.class);

            assertThat(info.getFields()).extracting(FieldInfo::getName)
                    .contains("studentId", "gpa", "enrolledCourseIds", "email", "firstName", "userId");

            FieldInfo gpa = field(info, "gpa");
            assertThat(gpa.getType()).isEqualTo(double.class);
            assertThat(gpa.isStatic()).isFalse();
            assertThat(gpa.getField().getDeclaringClass()).isEqualTo(Student.class);
            assertThat(field(info, "email").getField().getDeclaringClass()).isEqualTo(User.class);
        }

        @Test
        void methodsAreThoseDeclaredOnTheClass() {
            ClassInfo info = ModelInspector.inspectClass(Student.class);

            MethodInfo setGpa = method(info, "setGpa");
            assertThat(setGpa.getParameterTypes()).containsExactly(double.class);
            assertThat(setGpa.getReturnType()).isEqualTo(void.class);
            assertThat(info.getMethods()).extracting(MethodInfo::getName)
                    .contains("getRole", "displayInfo")
                    .doesNotContain("getFullName"); // declared on User, not Student
        }

        @Test
        void inspectionIsCachedPerClass() {
            assertThat(ModelInspector.inspectClass(Student.class)).isSameAs(ModelInspector.inspectClass(Student.class));
        }

        @Test
        void classFlagsForAbstractEnumInterfaceAndAnnotationTypes() {
            assertThat(ModelInspector.inspectClass(User.class).isAbstract()).isTrue();
            assertThat(ModelInspector.inspectClass(AcademicYear.class).isEnum()).isTrue();
            assertThat(ModelInspector.inspectClass(Comparable.class).isInterface()).isTrue();
            assertThat(ModelInspector.inspectClass(Comparable.class).getSuperClass()).isNull();
            assertThat(ModelInspector.inspectClass(Validator.class).isAnnotation()).isTrue();
        }

        @Test
        void primitiveTypeHasNoPackageOrSuperclass() {
            ClassInfo info = ModelInspector.inspectClass(int.class);

            assertThat(info.getPackageName()).isEmpty();
            assertThat(info.getSuperClass()).isNull();
            assertThat(info.getFields()).isEmpty();
        }
    }

    @Nested
    class InspectAnnotatedFixture {

        @Test
        void classAnnotationsAndAttributesAreCaptured() {
            ClassInfo info = ModelInspector.inspectClass(Widget.class);

            assertThat(info.hasAnnotation(Entity.class)).isTrue();
            assertThat(info.hasAnnotation(Audited.class)).isFalse();
            AnnotationInfo entity = info.getAnnotations().get(0);
            assertThat(entity.getType()).isEqualTo(Entity.class);
            assertThat(entity.getAttribute("table")).isEqualTo("widgets");
            assertThat(entity.getAttribute("version")).isEqualTo("2.0");
            assertThat(entity.getAttribute("primaryKey")).isEqualTo("id");
            assertThat(entity.hasAttribute("cacheTTL")).isTrue();
            assertThat(entity.hasAttribute("nope")).isFalse();
            assertThat(info.getInterfaces()).containsExactly(Comparable.class);
        }

        @Test
        void fieldModifiersAndGenericsAreReported() {
            ClassInfo info = ModelInspector.inspectClass(Widget.class);

            FieldInfo kind = field(info, "KIND");
            assertThat(kind.isStatic()).isTrue();
            assertThat(kind.isFinal()).isTrue();
            assertThat(Modifier.isStatic(kind.getModifiers())).isTrue();
            assertThat(field(info, "cachedHash").isTransient()).isTrue();
            assertThat(field(info, "dirty").isVolatile()).isTrue();

            FieldInfo tags = field(info, "tags");
            assertThat(tags.getType()).isEqualTo(List.class);
            assertThat(tags.getGenericType()).isInstanceOf(ParameterizedType.class);

            FieldInfo name = field(info, "name");
            assertThat(name.hasAnnotation(Validator.class)).isTrue();
            assertThat(name.getAnnotations()).singleElement()
                    .satisfies(a -> assertThat(a.getAttribute("message")).isEqualTo("name required"));
        }

        @Test
        void methodDetailsIncludeModifiersExceptionsAndParameterAnnotations() {
            ClassInfo info = ModelInspector.inspectClass(Widget.class);

            MethodInfo rename = method(info, "rename");
            assertThat(rename.isSynchronized()).isTrue();
            assertThat(rename.isStatic()).isFalse();
            assertThat(rename.isNative()).isFalse();
            assertThat(rename.hasAnnotation(Audited.class)).isTrue();
            assertThat(rename.getAnnotations()).singleElement()
                    .satisfies(a -> assertThat(a.getAttribute("category")).isEqualTo("rename"));
            assertThat(rename.getParameterAnnotations()).hasSize(1);
            assertThat(rename.getParameterAnnotations().get(0)).extracting(AnnotationInfo::getType)
                    .containsExactly(Validator.class);
            assertThat(Modifier.isPublic(rename.getModifiers())).isTrue();
            assertThat(rename.getMethod().getDeclaringClass()).isEqualTo(Widget.class);

            MethodInfo of = method(info, "of");
            assertThat(of.isStatic()).isTrue();
            assertThat(of.getExceptionTypes()).containsExactly(IOException.class);
            assertThat(of.getGenericParameterTypes()).containsExactly(String.class);

            MethodInfo getTags = method(info, "getTags");
            assertThat(getTags.isFinal()).isTrue();
            assertThat(getTags.getGenericReturnType()).isInstanceOf(ParameterizedType.class);
        }

        @Test
        void abstractMethodIsFlagged() {
            assertThat(method(ModelInspector.inspectClass(Shape.class), "area").isAbstract()).isTrue();
            assertThat(method(ModelInspector.inspectClass(Square.class), "area").isAbstract()).isFalse();
        }

        @Test
        void constructorDetailsAreReported() {
            ClassInfo info = ModelInspector.inspectClass(Widget.class);

            ConstructorInfo withName = info.getConstructors().stream()
                    .filter(c -> c.getParameterTypes().size() == 1).findFirst().orElseThrow();
            assertThat(withName.getParameterTypes()).containsExactly(String.class);
            assertThat(withName.getGenericParameterTypes()).containsExactly(String.class);
            assertThat(withName.getExceptionTypes()).containsExactly(IOException.class);
            assertThat(withName.hasAnnotation(Deprecated.class)).isTrue();
            assertThat(withName.hasAnnotation(Audited.class)).isFalse();
            assertThat(withName.getAnnotations()).hasSize(1);
            assertThat(withName.getParameterAnnotations().get(0)).extracting(AnnotationInfo::getType)
                    .containsExactly(Validator.class);
            assertThat(withName.getConstructor().getDeclaringClass()).isEqualTo(Widget.class);
            assertThat(Modifier.isPrivate(withName.getModifiers())).isFalse();
        }

        @Test
        void queriesFilterFieldsAndMethodsByAnnotation() {
            assertThat(ModelInspector.getFieldsWithAnnotation(Widget.class, Validator.class))
                    .extracting(FieldInfo::getName).containsExactly("name");
            assertThat(ModelInspector.getMethodsWithAnnotation(Widget.class, Audited.class))
                    .extracting(MethodInfo::getName).containsExactly("rename");
            assertThat(ModelInspector.getMethodsWithAnnotation(Widget.class, Entity.class)).isEmpty();
        }

        @Test
        void entityScanningIsNotImplemented() {
            assertThat(ModelInspector.getEntityClasses("models")).isEmpty();
        }

        @Test
        void returnedCollectionsAreDefensiveCopies() {
            ClassInfo info = ModelInspector.inspectClass(Widget.class);
            int fieldCount = info.getFields().size();

            info.getFields().clear();
            info.getMethods().clear();
            method(info, "rename").getParameterAnnotations().clear();
            info.getAnnotations().get(0).getAttributes().clear();

            assertThat(info.getFields()).hasSize(fieldCount);
            assertThat(method(info, "rename").getParameterAnnotations()).hasSize(1);
            assertThat(info.getAnnotations().get(0).getAttribute("table")).isEqualTo("widgets");
        }

        @Test
        void annotationInfoCopiesAttributesOnConstruction() {
            Map<String, Object> attrs = new java.util.HashMap<>(Map.of("k", "v"));
            AnnotationInfo info = new AnnotationInfo(Entity.class, attrs);
            attrs.clear();

            assertThat(info.getAttributes()).containsEntry("k", "v");
        }
    }

    @Nested
    class ReflectiveAccess {

        @Test
        void getFieldValueReadsPrivateAndInheritedFields() {
            Student student = sampleStudent();

            assertThat(ModelInspector.getFieldValue(student, "studentId")).isEqualTo("STU123456");
            assertThat(ModelInspector.getFieldValue(student, "email")).isEqualTo("alice@university.edu");
            assertThat(ModelInspector.getFieldValue(student, "noSuchField")).isNull();
        }

        @Test
        void setFieldValueWritesPrivateAndInheritedFieldsBypassingSetters() {
            Student student = sampleStudent();

            ModelInspector.setFieldValue(student, "major", "Physics");
            ModelInspector.setFieldValue(student, "firstName", "Alicia");
            ModelInspector.setFieldValue(student, "noSuchField", "ignored");

            assertThat(student.getMajor()).isEqualTo("Physics");
            assertThat(student.getFirstName()).isEqualTo("Alicia");
        }

        @Test
        void setFieldValueWithIncompatibleTypeFails() {
            assertThatThrownBy(() -> ModelInspector.setFieldValue(sampleStudent(), "gpa", "high"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invokeMethodCallsMethodWithMatchingArgumentTypes() {
            Student student = sampleStudent();

            ModelInspector.invokeMethod(student, "setMajor", "Mathematics");

            assertThat(student.getMajor()).isEqualTo("Mathematics");
            assertThat(ModelInspector.invokeMethod(student, "getRole")).isEqualTo(student.getRole());
        }

        // Regression: argument types were taken from the boxed runtime classes, so methods with
        // primitive parameters (e.g. setGpa(double)) could never be found.
        @Test
        void invokeMethodSupportsPrimitiveParameters() {
            Student student = sampleStudent();

            ModelInspector.invokeMethod(student, "setGpa", 3.5);
            Object sum = ModelInspector.invokeMethod(new Widget(), "add", 2, 40L);

            assertThat(student.getGpa()).isEqualTo(3.5);
            assertThat(sum).isEqualTo(42);
        }

        // Regression: only the instance's own class was searched, so inherited methods were not found.
        @Test
        void invokeMethodFindsInheritedMethods() {
            assertThat(ModelInspector.invokeMethod(sampleStudent(), "getFullName")).isEqualTo("Alice Johnson");
        }

        // Regression: a null argument caused a NullPointerException while computing argument types.
        @Test
        void invokeMethodAcceptsNullForReferenceParameters() {
            Student student = sampleStudent();

            ModelInspector.invokeMethod(student, "setDepartmentId", (Object) null);

            assertThat(student.getDepartmentId()).isNull();
        }

        @Test
        void invokeMethodPrefersExactOverload() {
            Widget widget = new Widget();

            assertThat(ModelInspector.invokeMethod(widget, "describe", "x")).isEqualTo("string:x");
            assertThat(ModelInspector.invokeMethod(widget, "describe", 5)).isEqualTo("object");
        }

        @Test
        void invokeMethodWrapsFailures() {
            Student student = sampleStudent();

            assertThatThrownBy(() -> ModelInspector.invokeMethod(student, "noSuchMethod"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Error invoking method: noSuchMethod")
                    .hasCauseInstanceOf(NoSuchMethodException.class);
            assertThatThrownBy(() -> ModelInspector.invokeMethod(student, "setGpa", (Object) null))
                    .hasCauseInstanceOf(NoSuchMethodException.class);
            assertThatThrownBy(() -> ModelInspector.invokeMethod(student, "setGpa", 9.0))
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("GPA must be between 0.0 and 4.0");
        }

        @Test
        void createInstanceUsesNoArgConstructorEvenWhenNotPublic() {
            Widget widget = ModelInspector.createInstance(Widget.class);
            Student student = ModelInspector.createInstance(Student.class);

            assertThat(widget).isNotNull();
            assertThat(student.getEnrolledCourseIds()).isEmpty();
        }

        @Test
        void createInstanceWithoutNoArgConstructorFails() {
            assertThatThrownBy(() -> ModelInspector.createInstance(NoDefaultConstructor.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error creating instance of: ")
                    .hasCauseInstanceOf(NoSuchMethodException.class);
        }
    }

    @Nested
    class Validation {

        @Test
        void validateObjectVisitsAllFieldsWithoutErrorForValidObject() throws IOException {
            ValidationResult result = ModelInspector.validateObject(Widget.of("gizmo"));

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
            assertThat(result.getWarnings()).isEmpty();
        }

        @Test
        void validationResultCollectsErrorsAndWarnings() {
            ValidationResult result = new ValidationResult();
            result.addWarning("careful");
            assertThat(result.isValid()).isTrue();

            result.addError("broken");
            result.getErrors().clear();

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactly("broken");
            assertThat(result.getWarnings()).containsExactly("careful");
        }
    }

    // Regression: the class-info cache was a plain HashMap mutated from a public static API;
    // concurrent first inspections could corrupt it or build duplicate ClassInfo instances.
    // (A data race cannot be forced deterministically; this guards the ConcurrentHashMap fix.)
    @Test
    @Timeout(10)
    void concurrentFirstInspectionYieldsSingleCachedInstance() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<ClassInfo>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return ModelInspector.inspectClass(ColdCacheFixture.class);
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            ClassInfo first = results.get(0).get(5, TimeUnit.SECONDS);
            for (Future<ClassInfo> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).isSameAs(first);
            }
            assertThat(first.getFields()).extracting(FieldInfo::getName).contains("a", "b");
        } finally {
            pool.shutdownNow();
        }
    }
}
