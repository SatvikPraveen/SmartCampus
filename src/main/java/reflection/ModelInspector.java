// File location: src/main/java/reflection/ModelInspector.java
package reflection;

import annotations.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for inspecting model classes using Java Reflection API
 * Provides comprehensive analysis of classes, fields, methods, and annotations
 */
public class ModelInspector {
    
    private static final Map<Class<?>, ClassInfo> classInfoCache = new HashMap<>();
    
    // ==================== CLASS INSPECTION ====================
    
    /**
     * Inspects a class and returns comprehensive information
     */
    public static ClassInfo inspectClass(Class<?> clazz) {
        return classInfoCache.computeIfAbsent(clazz, ModelInspector::analyzeClass);
    }
    
    /**
     * Analyzes class structure and metadata
     */
    private static ClassInfo analyzeClass(Class<?> clazz) {
        ClassInfo.Builder builder = new ClassInfo.Builder(clazz);
        
        // Basic class information
        builder.packageName(clazz.getPackage() != null ? clazz.getPackage().getName() : "")
               .simpleName(clazz.getSimpleName())
               .canonicalName(clazz.getCanonicalName())
               .modifiers(clazz.getModifiers())
               .isInterface(clazz.isInterface())
               .isAbstract(Modifier.isAbstract(clazz.getModifiers()))
               .isEnum(clazz.isEnum())
               .isAnnotation(clazz.isAnnotation());
        
        // Inheritance information
        if (clazz.getSuperclass() != null) {
            builder.superClass(clazz.getSuperclass());
        }
        builder.interfaces(Arrays.asList(clazz.getInterfaces()));
        
        // Annotations
        builder.annotations(getAnnotationInfo(clazz.getAnnotations()));
        
        // Fields
        builder.fields(analyzeFields(clazz));
        
        // Methods
        builder.methods(analyzeMethods(clazz));
        
        // Constructors
        builder.constructors(analyzeConstructors(clazz));
        
        // Inner classes
        builder.innerClasses(Arrays.asList(clazz.getDeclaredClasses()));
        
        return builder.build();
    }
    
    /**
     * Analyzes all fields in a class
     */
    private static List<FieldInfo> analyzeFields(Class<?> clazz) {
        List<FieldInfo> fields = new ArrayList<>();
        
        // Get all fields including inherited ones
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                fields.add(analyzeField(field));
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * Analyzes a single field
     */
    private static FieldInfo analyzeField(Field field) {
        return new FieldInfo.Builder(field)
            .name(field.getName())
            .type(field.getType())
            .genericType(field.getGenericType())
            .modifiers(field.getModifiers())
            .isStatic(Modifier.isStatic(field.getModifiers()))
            .isFinal(Modifier.isFinal(field.getModifiers()))
            .isTransient(Modifier.isTransient(field.getModifiers()))
            .isVolatile(Modifier.isVolatile(field.getModifiers()))
            .annotations(getAnnotationInfo(field.getAnnotations()))
            .build();
    }
    
    /**
     * Analyzes all methods in a class
     */
    private static List<MethodInfo> analyzeMethods(Class<?> clazz) {
        List<MethodInfo> methods = new ArrayList<>();
        
        for (Method method : clazz.getDeclaredMethods()) {
            methods.add(analyzeMethod(method));
        }
        
        return methods;
    }
    
    /**
     * Analyzes a single method
     */
    private static MethodInfo analyzeMethod(Method method) {
        return new MethodInfo.Builder(method)
            .name(method.getName())
            .returnType(method.getReturnType())
            .genericReturnType(method.getGenericReturnType())
            .parameterTypes(Arrays.asList(method.getParameterTypes()))
            .genericParameterTypes(Arrays.asList(method.getGenericParameterTypes()))
            .exceptionTypes(Arrays.asList(method.getExceptionTypes()))
            .modifiers(method.getModifiers())
            .isStatic(Modifier.isStatic(method.getModifiers()))
            .isFinal(Modifier.isFinal(method.getModifiers()))
            .isAbstract(Modifier.isAbstract(method.getModifiers()))
            .isSynchronized(Modifier.isSynchronized(method.getModifiers()))
            .isNative(Modifier.isNative(method.getModifiers()))
            .annotations(getAnnotationInfo(method.getAnnotations()))
            .parameterAnnotations(getParameterAnnotations(method))
            .build();
    }
    
    /**
     * Analyzes all constructors in a class
     */
    private static List<ConstructorInfo> analyzeConstructors(Class<?> clazz) {
        List<ConstructorInfo> constructors = new ArrayList<>();
        
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            constructors.add(analyzeConstructor(constructor));
        }
        
        return constructors;
    }
    
    /**
     * Analyzes a single constructor
     */
    private static ConstructorInfo analyzeConstructor(Constructor<?> constructor) {
        return new ConstructorInfo.Builder(constructor)
            .parameterTypes(Arrays.asList(constructor.getParameterTypes()))
            .genericParameterTypes(Arrays.asList(constructor.getGenericParameterTypes()))
            .exceptionTypes(Arrays.asList(constructor.getExceptionTypes()))
            .modifiers(constructor.getModifiers())
            .annotations(getAnnotationInfo(constructor.getAnnotations()))
            .parameterAnnotations(getParameterAnnotations(constructor))
            .build();
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Extracts annotation information
     */
    private static List<AnnotationInfo> getAnnotationInfo(Annotation[] annotations) {
        return Arrays.stream(annotations)
                    .map(ModelInspector::createAnnotationInfo)
                    .collect(Collectors.toList());
    }
    
    /**
     * Creates annotation information
     */
    private static AnnotationInfo createAnnotationInfo(Annotation annotation) {
        Map<String, Object> attributes = new HashMap<>();
        
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getDeclaringClass() == annotation.annotationType()) {
                try {
                    Object value = method.invoke(annotation);
                    attributes.put(method.getName(), value);
                } catch (Exception e) {
                    // Ignore reflection errors
                }
            }
        }
        
        return new AnnotationInfo(annotation.annotationType(), attributes);
    }
    
    /**
     * Gets parameter annotations for method or constructor
     */
    private static List<List<AnnotationInfo>> getParameterAnnotations(Executable executable) {
        List<List<AnnotationInfo>> paramAnnotations = new ArrayList<>();
        
        for (Annotation[] paramAnns : executable.getParameterAnnotations()) {
            paramAnnotations.add(getAnnotationInfo(paramAnns));
        }
        
        return paramAnnotations;
    }
    
    // ==================== QUERY METHODS ====================
    
    /**
     * Finds fields with specific annotation
     */
    public static List<FieldInfo> getFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return inspectClass(clazz).getFields().stream()
                .filter(field -> field.hasAnnotation(annotationType))
                .collect(Collectors.toList());
    }
    
    /**
     * Finds methods with specific annotation
     */
    public static List<MethodInfo> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return inspectClass(clazz).getMethods().stream()
                .filter(method -> method.hasAnnotation(annotationType))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all entity classes (marked with @Entity annotation)
     */
    public static List<Class<?>> getEntityClasses(String packageName) {
        // In a real implementation, this would scan the classpath
        // For now, return empty list as placeholder
        return new ArrayList<>();
    }
    
    /**
     * Gets field value using reflection
     */
    public static Object getFieldValue(Object instance, String fieldName) {
        try {
            Field field = findField(instance.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(instance);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing field: " + fieldName, e);
        }
        return null;
    }
    
    /**
     * Sets field value using reflection
     */
    public static void setFieldValue(Object instance, String fieldName, Object value) {
        try {
            Field field = findField(instance.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(instance, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error setting field: " + fieldName, e);
        }
    }
    
    /**
     * Invokes method using reflection
     */
    public static Object invokeMethod(Object instance, String methodName, Object... args) {
        try {
            Class<?>[] argTypes = Arrays.stream(args)
                    .map(Object::getClass)
                    .toArray(Class<?>[]::new);
            
            Method method = instance.getClass().getDeclaredMethod(methodName, argTypes);
            method.setAccessible(true);
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Error invoking method: " + methodName, e);
        }
    }
    
    /**
     * Creates instance using default constructor
     */
    public static <T> T createInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating instance of: " + clazz.getName(), e);
        }
    }
    
    /**
     * Finds field in class hierarchy
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Validates object using reflection and annotations
     */
    public static ValidationResult validateObject(Object instance) {
        ValidationResult result = new ValidationResult();
        ClassInfo classInfo = inspectClass(instance.getClass());
        
        for (FieldInfo fieldInfo : classInfo.getFields()) {
            validateField(instance, fieldInfo, result);
        }
        
        return result;
    }
    
    /**
     * Validates a single field
     */
    private static void validateField(Object instance, FieldInfo fieldInfo, ValidationResult result) {
        Object fieldValue = getFieldValue(instance, fieldInfo.getName());
        
        for (AnnotationInfo annotation : fieldInfo.getAnnotations()) {
            if (annotation.getType() == Validator.class) {
                // Perform validation based on annotation attributes
                // This is a simplified version
                validateWithAnnotation(fieldInfo.getName(), fieldValue, annotation, result);
            }
        }
    }
    
    /**
     * Validates field value with annotation
     */
    private static void validateWithAnnotation(String fieldName, Object value, AnnotationInfo annotation, ValidationResult result) {
        // Implementation would check annotation attributes and validate accordingly
        // This is a placeholder for actual validation logic
    }
    
    // ==================== DATA CLASSES ====================
    
    /**
     * Comprehensive class information
     */
    public static class ClassInfo {
        private final Class<?> clazz;
        private final String packageName;
        private final String simpleName;
        private final String canonicalName;
        private final int modifiers;
        private final boolean isInterface;
        private final boolean isAbstract;
        private final boolean isEnum;
        private final boolean isAnnotation;
        private final Class<?> superClass;
        private final List<Class<?>> interfaces;
        private final List<AnnotationInfo> annotations;
        private final List<FieldInfo> fields;
        private final List<MethodInfo> methods;
        private final List<ConstructorInfo> constructors;
        private final List<Class<?>> innerClasses;
        
        private ClassInfo(Builder builder) {
            this.clazz = builder.clazz;
            this.packageName = builder.packageName;
            this.simpleName = builder.simpleName;
            this.canonicalName = builder.canonicalName;
            this.modifiers = builder.modifiers;
            this.isInterface = builder.isInterface;
            this.isAbstract = builder.isAbstract;
            this.isEnum = builder.isEnum;
            this.isAnnotation = builder.isAnnotation;
            this.superClass = builder.superClass;
            this.interfaces = new ArrayList<>(builder.interfaces);
            this.annotations = new ArrayList<>(builder.annotations);
            this.fields = new ArrayList<>(builder.fields);
            this.methods = new ArrayList<>(builder.methods);
            this.constructors = new ArrayList<>(builder.constructors);
            this.innerClasses = new ArrayList<>(builder.innerClasses);
        }
        
        // Getters
        public Class<?> getClazz() { return clazz; }
        public String getPackageName() { return packageName; }
        public String getSimpleName() { return simpleName; }
        public String getCanonicalName() { return canonicalName; }
        public int getModifiers() { return modifiers; }
        public boolean isInterface() { return isInterface; }
        public boolean isAbstract() { return isAbstract; }
        public boolean isEnum() { return isEnum; }
        public boolean isAnnotation() { return isAnnotation; }
        public Class<?> getSuperClass() { return superClass; }
        public List<Class<?>> getInterfaces() { return new ArrayList<>(interfaces); }
        public List<AnnotationInfo> getAnnotations() { return new ArrayList<>(annotations); }
        public List<FieldInfo> getFields() { return new ArrayList<>(fields); }
        public List<MethodInfo> getMethods() { return new ArrayList<>(methods); }
        public List<ConstructorInfo> getConstructors() { return new ArrayList<>(constructors); }
        public List<Class<?>> getInnerClasses() { return new ArrayList<>(innerClasses); }
        
        public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return annotations.stream().anyMatch(ann -> ann.getType() == annotationType);
        }
        
        public static class Builder {
            private final Class<?> clazz;
            private String packageName;
            private String simpleName;
            private String canonicalName;
            private int modifiers;
            private boolean isInterface;
            private boolean isAbstract;
            private boolean isEnum;
            private boolean isAnnotation;
            private Class<?> superClass;
            private List<Class<?>> interfaces = new ArrayList<>();
            private List<AnnotationInfo> annotations = new ArrayList<>();
            private List<FieldInfo> fields = new ArrayList<>();
            private List<MethodInfo> methods = new ArrayList<>();
            private List<ConstructorInfo> constructors = new ArrayList<>();
            private List<Class<?>> innerClasses = new ArrayList<>();
            
            public Builder(Class<?> clazz) {
                this.clazz = clazz;
            }
            
            public Builder packageName(String packageName) {
                this.packageName = packageName;
                return this;
            }
            
            public Builder simpleName(String simpleName) {
                this.simpleName = simpleName;
                return this;
            }
            
            public Builder canonicalName(String canonicalName) {
                this.canonicalName = canonicalName;
                return this;
            }
            
            public Builder modifiers(int modifiers) {
                this.modifiers = modifiers;
                return this;
            }
            
            public Builder isInterface(boolean isInterface) {
                this.isInterface = isInterface;
                return this;
            }
            
            public Builder isAbstract(boolean isAbstract) {
                this.isAbstract = isAbstract;
                return this;
            }
            
            public Builder isEnum(boolean isEnum) {
                this.isEnum = isEnum;
                return this;
            }
            
            public Builder isAnnotation(boolean isAnnotation) {
                this.isAnnotation = isAnnotation;
                return this;
            }
            
            public Builder superClass(Class<?> superClass) {
                this.superClass = superClass;
                return this;
            }
            
            public Builder interfaces(List<Class<?>> interfaces) {
                this.interfaces = interfaces;
                return this;
            }
            
            public Builder annotations(List<AnnotationInfo> annotations) {
                this.annotations = annotations;
                return this;
            }
            
            public Builder fields(List<FieldInfo> fields) {
                this.fields = fields;
                return this;
            }
            
            public Builder methods(List<MethodInfo> methods) {
                this.methods = methods;
                return this;
            }
            
            public Builder constructors(List<ConstructorInfo> constructors) {
                this.constructors = constructors;
                return this;
            }
            
            public Builder innerClasses(List<Class<?>> innerClasses) {
                this.innerClasses = innerClasses;
                return this;
            }
            
            public ClassInfo build() {
                return new ClassInfo(this);
            }
        }
    }
    
    /**
     * Field information
     */
    public static class FieldInfo {
        private final Field field;
        private final String name;
        private final Class<?> type;
        private final Type genericType;
        private final int modifiers;
        private final boolean isStatic;
        private final boolean isFinal;
        private final boolean isTransient;
        private final boolean isVolatile;
        private final List<AnnotationInfo> annotations;
        
        private FieldInfo(Builder builder) {
            this.field = builder.field;
            this.name = builder.name;
            this.type = builder.type;
            this.genericType = builder.genericType;
            this.modifiers = builder.modifiers;
            this.isStatic = builder.isStatic;
            this.isFinal = builder.isFinal;
            this.isTransient = builder.isTransient;
            this.isVolatile = builder.isVolatile;
            this.annotations = new ArrayList<>(builder.annotations);
        }
        
        // Getters
        public Field getField() { return field; }
        public String getName() { return name; }
        public Class<?> getType() { return type; }
        public Type getGenericType() { return genericType; }
        public int getModifiers() { return modifiers; }
        public boolean isStatic() { return isStatic; }
        public boolean isFinal() { return isFinal; }
        public boolean isTransient() { return isTransient; }
        public boolean isVolatile() { return isVolatile; }
        public List<AnnotationInfo> getAnnotations() { return new ArrayList<>(annotations); }
        
        public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return annotations.stream().anyMatch(ann -> ann.getType() == annotationType);
        }
        
        public static class Builder {
            private final Field field;
            private String name;
            private Class<?> type;
            private Type genericType;
            private int modifiers;
            private boolean isStatic;
            private boolean isFinal;
            private boolean isTransient;
            private boolean isVolatile;
            private List<AnnotationInfo> annotations = new ArrayList<>();
            
            public Builder(Field field) {
                this.field = field;
            }
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder type(Class<?> type) {
                this.type = type;
                return this;
            }
            
            public Builder genericType(Type genericType) {
                this.genericType = genericType;
                return this;
            }
            
            public Builder modifiers(int modifiers) {
                this.modifiers = modifiers;
                return this;
            }
            
            public Builder isStatic(boolean isStatic) {
                this.isStatic = isStatic;
                return this;
            }
            
            public Builder isFinal(boolean isFinal) {
                this.isFinal = isFinal;
                return this;
            }
            
            public Builder isTransient(boolean isTransient) {
                this.isTransient = isTransient;
                return this;
            }
            
            public Builder isVolatile(boolean isVolatile) {
                this.isVolatile = isVolatile;
                return this;
            }
            
            public Builder annotations(List<AnnotationInfo> annotations) {
                this.annotations = annotations;
                return this;
            }
            
            public FieldInfo build() {
                return new FieldInfo(this);
            }
        }
    }
    
    /**
     * Method information
     */
    public static class MethodInfo {
        private final Method method;
        private final String name;
        private final Class<?> returnType;
        private final Type genericReturnType;
        private final List<Class<?>> parameterTypes;
        private final List<Type> genericParameterTypes;
        private final List<Class<?>> exceptionTypes;
        private final int modifiers;
        private final boolean isStatic;
        private final boolean isFinal;
        private final boolean isAbstract;
        private final boolean isSynchronized;
        private final boolean isNative;
        private final List<AnnotationInfo> annotations;
        private final List<List<AnnotationInfo>> parameterAnnotations;
        
        private MethodInfo(Builder builder) {
            this.method = builder.method;
            this.name = builder.name;
            this.returnType = builder.returnType;
            this.genericReturnType = builder.genericReturnType;
            this.parameterTypes = new ArrayList<>(builder.parameterTypes);
            this.genericParameterTypes = new ArrayList<>(builder.genericParameterTypes);
            this.exceptionTypes = new ArrayList<>(builder.exceptionTypes);
            this.modifiers = builder.modifiers;
            this.isStatic = builder.isStatic;
            this.isFinal = builder.isFinal;
            this.isAbstract = builder.isAbstract;
            this.isSynchronized = builder.isSynchronized;
            this.isNative = builder.isNative;
            this.annotations = new ArrayList<>(builder.annotations);
            this.parameterAnnotations = new ArrayList<>(builder.parameterAnnotations);
        }
        
        // Getters
        public Method getMethod() { return method; }
        public String getName() { return name; }
        public Class<?> getReturnType() { return returnType; }
        public Type getGenericReturnType() { return genericReturnType; }
        public List<Class<?>> getParameterTypes() { return new ArrayList<>(parameterTypes); }
        public List<Type> getGenericParameterTypes() { return new ArrayList<>(genericParameterTypes); }
        public List<Class<?>> getExceptionTypes() { return new ArrayList<>(exceptionTypes); }
        public int getModifiers() { return modifiers; }
        public boolean isStatic() { return isStatic; }
        public boolean isFinal() { return isFinal; }
        public boolean isAbstract() { return isAbstract; }
        public boolean isSynchronized() { return isSynchronized; }
        public boolean isNative() { return isNative; }
        public List<AnnotationInfo> getAnnotations() { return new ArrayList<>(annotations); }
        public List<List<AnnotationInfo>> getParameterAnnotations() { return new ArrayList<>(parameterAnnotations); }
        
        public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return annotations.stream().anyMatch(ann -> ann.getType() == annotationType);
        }
        
        public static class Builder {
            private final Method method;
            private String name;
            private Class<?> returnType;
            private Type genericReturnType;
            private List<Class<?>> parameterTypes = new ArrayList<>();
            private List<Type> genericParameterTypes = new ArrayList<>();
            private List<Class<?>> exceptionTypes = new ArrayList<>();
            private int modifiers;
            private boolean isStatic;
            private boolean isFinal;
            private boolean isAbstract;
            private boolean isSynchronized;
            private boolean isNative;
            private List<AnnotationInfo> annotations = new ArrayList<>();
            private List<List<AnnotationInfo>> parameterAnnotations = new ArrayList<>();
            
            public Builder(Method method) {
                this.method = method;
            }
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder returnType(Class<?> returnType) {
                this.returnType = returnType;
                return this;
            }
            
            public Builder genericReturnType(Type genericReturnType) {
                this.genericReturnType = genericReturnType;
                return this;
            }
            
            public Builder parameterTypes(List<Class<?>> parameterTypes) {
                this.parameterTypes = parameterTypes;
                return this;
            }
            
            public Builder genericParameterTypes(List<Type> genericParameterTypes) {
                this.genericParameterTypes = genericParameterTypes;
                return this;
            }
            
            public Builder exceptionTypes(List<Class<?>> exceptionTypes) {
                this.exceptionTypes = exceptionTypes;
                return this;
            }
            
            public Builder modifiers(int modifiers) {
                this.modifiers = modifiers;
                return this;
            }
            
            public Builder isStatic(boolean isStatic) {
                this.isStatic = isStatic;
                return this;
            }
            
            public Builder isFinal(boolean isFinal) {
                this.isFinal = isFinal;
                return this;
            }
            
            public Builder isAbstract(boolean isAbstract) {
                this.isAbstract = isAbstract;
                return this;
            }
            
            public Builder isSynchronized(boolean isSynchronized) {
                this.isSynchronized = isSynchronized;
                return this;
            }
            
            public Builder isNative(boolean isNative) {
                this.isNative = isNative;
                return this;
            }
            
            public Builder annotations(List<AnnotationInfo> annotations) {
                this.annotations = annotations;
                return this;
            }
            
            public Builder parameterAnnotations(List<List<AnnotationInfo>> parameterAnnotations) {
                this.parameterAnnotations = parameterAnnotations;
                return this;
            }
            
            public MethodInfo build() {
                return new MethodInfo(this);
            }
        }
    }
    
    /**
     * Constructor information
     */
    public static class ConstructorInfo {
        private final Constructor<?> constructor;
        private final List<Class<?>> parameterTypes;
        private final List<Type> genericParameterTypes;
        private final List<Class<?>> exceptionTypes;
        private final int modifiers;
        private final List<AnnotationInfo> annotations;
        private final List<List<AnnotationInfo>> parameterAnnotations;
        
        private ConstructorInfo(Builder builder) {
            this.constructor = builder.constructor;
            this.parameterTypes = new ArrayList<>(builder.parameterTypes);
            this.genericParameterTypes = new ArrayList<>(builder.genericParameterTypes);
            this.exceptionTypes = new ArrayList<>(builder.exceptionTypes);
            this.modifiers = builder.modifiers;
            this.annotations = new ArrayList<>(builder.annotations);
            this.parameterAnnotations = new ArrayList<>(builder.parameterAnnotations);
        }
        
        // Getters
        public Constructor<?> getConstructor() { return constructor; }
        public List<Class<?>> getParameterTypes() { return new ArrayList<>(parameterTypes); }
        public List<Type> getGenericParameterTypes() { return new ArrayList<>(genericParameterTypes); }
        public List<Class<?>> getExceptionTypes() { return new ArrayList<>(exceptionTypes); }
        public int getModifiers() { return modifiers; }
        public List<AnnotationInfo> getAnnotations() { return new ArrayList<>(annotations); }
        public List<List<AnnotationInfo>> getParameterAnnotations() { return new ArrayList<>(parameterAnnotations); }
        
        public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return annotations.stream().anyMatch(ann -> ann.getType() == annotationType);
        }
        
        public static class Builder {
            private final Constructor<?> constructor;
            private List<Class<?>> parameterTypes = new ArrayList<>();
            private List<Type> genericParameterTypes = new ArrayList<>();
            private List<Class<?>> exceptionTypes = new ArrayList<>();
            private int modifiers;
            private List<AnnotationInfo> annotations = new ArrayList<>();
            private List<List<AnnotationInfo>> parameterAnnotations = new ArrayList<>();
            
            public Builder(Constructor<?> constructor) {
                this.constructor = constructor;
            }
            
            public Builder parameterTypes(List<Class<?>> parameterTypes) {
                this.parameterTypes = parameterTypes;
                return this;
            }
            
            public Builder genericParameterTypes(List<Type> genericParameterTypes) {
                this.genericParameterTypes = genericParameterTypes;
                return this;
            }
            
            public Builder exceptionTypes(List<Class<?>> exceptionTypes) {
                this.exceptionTypes = exceptionTypes;
                return this;
            }
            
            public Builder modifiers(int modifiers) {
                this.modifiers = modifiers;
                return this;
            }
            
            public Builder annotations(List<AnnotationInfo> annotations) {
                this.annotations = annotations;
                return this;
            }
            
            public Builder parameterAnnotations(List<List<AnnotationInfo>> parameterAnnotations) {
                this.parameterAnnotations = parameterAnnotations;
                return this;
            }
            
            public ConstructorInfo build() {
                return new ConstructorInfo(this);
            }
        }
    }
    
    /**
     * Annotation information
     */
    public static class AnnotationInfo {
        private final Class<? extends Annotation> type;
        private final Map<String, Object> attributes;
        
        public AnnotationInfo(Class<? extends Annotation> type, Map<String, Object> attributes) {
            this.type = type;
            this.attributes = new HashMap<>(attributes);
        }
        
        public Class<? extends Annotation> getType() { return type; }
        public Map<String, Object> getAttributes() { return new HashMap<>(attributes); }
        public Object getAttribute(String name) { return attributes.get(name); }
        public boolean hasAttribute(String name) { return attributes.containsKey(name); }
    }
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
    }
}