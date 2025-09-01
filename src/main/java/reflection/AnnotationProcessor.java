// File location: src/main/java/reflection/AnnotationProcessor.java
package reflection;

import annotations.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processor for handling custom annotations using reflection
 * Provides runtime processing of annotations for validation, caching, auditing, and async operations
 */
public class AnnotationProcessor {
    
    private static final Map<Class<?>, AnnotationMetadata> metadataCache = new ConcurrentHashMap<>();
    private final ValidationEngine validationEngine;
    private final CacheManager cacheManager;
    private final AuditManager auditManager;
    private final AsyncManager asyncManager;
    
    public AnnotationProcessor() {
        this.validationEngine = new ValidationEngine();
        this.cacheManager = new CacheManager();
        this.auditManager = new AuditManager();
        this.asyncManager = new AsyncManager();
    }
    
    // ==================== MAIN PROCESSING METHODS ====================
    
    /**
     * Processes all annotations on a class
     */
    public ProcessingResult processClass(Class<?> clazz) {
        AnnotationMetadata metadata = getOrCreateMetadata(clazz);
        ProcessingResult result = new ProcessingResult();
        
        // Process class-level annotations
        processEntityAnnotation(clazz, result);
        processCacheableAnnotation(clazz, result);
        processAuditedAnnotation(clazz, result);
        processAsyncAnnotation(clazz, result);
        
        return result;
    }
    
    /**
     * Processes annotations on a specific method
     */
    public ProcessingResult processMethod(Method method, Object instance, Object[] args) {
        ProcessingResult result = new ProcessingResult();
        
        // Process method annotations
        processCacheableMethod(method, instance, args, result);
        processAuditedMethod(method, instance, args, result);
        processAsyncMethod(method, instance, args, result);
        processValidatorAnnotations(method, args, result);
        
        return result;
    }
    
    /**
     * Processes annotations on object fields for validation
     */
    public ValidationResult processValidation(Object instance) {
        ValidationResult result = new ValidationResult();
        Class<?> clazz = instance.getClass();
        
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            processFieldValidation(field, instance, result);
        }
        
        return result;
    }
    
    // ==================== ENTITY ANNOTATION PROCESSING ====================
    
    private void processEntityAnnotation(Class<?> clazz, ProcessingResult result) {
        Entity entityAnnotation = clazz.getAnnotation(Entity.class);
        if (entityAnnotation != null) {
            EntityMetadata entityMetadata = createEntityMetadata(clazz, entityAnnotation);
            result.addMetadata("entity", entityMetadata);
            
            // Setup caching if specified
            if (entityAnnotation.cacheTTL() > 0) {
                setupEntityCache(clazz, entityAnnotation);
            }
            
            result.addMessage("Entity annotation processed for: " + clazz.getSimpleName());
        }
    }
    
    private EntityMetadata createEntityMetadata(Class<?> clazz, Entity annotation) {
        return new EntityMetadata.Builder()
            .className(clazz.getSimpleName())
            .tableName(annotation.table().isEmpty() ? clazz.getSimpleName().toLowerCase() : annotation.table())
            .schema(annotation.schema())
            .primaryKey(annotation.primaryKey())
            .auditable(annotation.auditable())
            .softDelete(annotation.softDelete())
            .cacheStrategy(annotation.cacheStrategy())
            .cacheTTL(annotation.cacheTTL())
            .description(annotation.description())
            .version(annotation.version())
            .readOnly(annotation.readOnly())
            .build();
    }
    
    private void setupEntityCache(Class<?> clazz, Entity annotation) {
        String cacheName = "entity_" + clazz.getSimpleName().toLowerCase();
        cacheManager.createCache(cacheName, annotation.cacheStrategy(), annotation.cacheTTL());
    }
    
    // ==================== CACHEABLE ANNOTATION PROCESSING ====================
    
    private void processCacheableAnnotation(Class<?> clazz, ProcessingResult result) {
        Cacheable cacheableAnnotation = clazz.getAnnotation(Cacheable.class);
        if (cacheableAnnotation != null) {
            setupClassCache(clazz, cacheableAnnotation);
            result.addMessage("Cacheable annotation processed for class: " + clazz.getSimpleName());
        }
    }
    
    private void processCacheableMethod(Method method, Object instance, Object[] args, ProcessingResult result) {
        Cacheable cacheableAnnotation = method.getAnnotation(Cacheable.class);
        if (cacheableAnnotation != null) {
            String cacheKey = generateCacheKey(method, args, cacheableAnnotation);
            
            // Check condition
            if (evaluateCondition(cacheableAnnotation.condition(), instance, args)) {
                Object cachedValue = cacheManager.get(getCacheName(method, cacheableAnnotation), cacheKey);
                if (cachedValue != null) {
                    result.setCachedResult(cachedValue);
                    result.addMessage("Cache hit for method: " + method.getName());
                } else {
                    result.addMessage("Cache miss for method: " + method.getName());
                }
            }
        }
    }
    
    private void setupClassCache(Class<?> clazz, Cacheable annotation) {
        String cacheName = getCacheName(clazz, annotation);
        cacheManager.createCache(cacheName, annotation.strategy().name(), annotation.ttl());
    }
    
    private String generateCacheKey(Method method, Object[] args, Cacheable annotation) {
        if (!annotation.key().isEmpty()) {
            return evaluateSpEL(annotation.key(), method, args);
        }
        
        StringBuilder keyBuilder = new StringBuilder(method.getName());
        if (args != null) {
            for (Object arg : args) {
                keyBuilder.append("_").append(arg != null ? arg.hashCode() : "null");
            }
        }
        return keyBuilder.toString();
    }
    
    private String getCacheName(Method method, Cacheable annotation) {
        return annotation.cacheName().isEmpty() ? 
               method.getDeclaringClass().getSimpleName() + "_" + method.getName() : 
               annotation.cacheName();
    }
    
    private String getCacheName(Class<?> clazz, Cacheable annotation) {
        return annotation.cacheName().isEmpty() ? 
               clazz.getSimpleName() : 
               annotation.cacheName();
    }
    
    // ==================== AUDITED ANNOTATION PROCESSING ====================
    
    private void processAuditedAnnotation(Class<?> clazz, ProcessingResult result) {
        Audited auditedAnnotation = clazz.getAnnotation(Audited.class);
        if (auditedAnnotation != null) {
            setupClassAuditing(clazz, auditedAnnotation);
            result.addMessage("Audited annotation processed for class: " + clazz.getSimpleName());
        }
    }
    
    private void processAuditedMethod(Method method, Object instance, Object[] args, ProcessingResult result) {
        Audited auditedAnnotation = method.getAnnotation(Audited.class);
        if (auditedAnnotation != null) {
            if (evaluateCondition(auditedAnnotation.condition(), instance, args)) {
                AuditEvent auditEvent = createAuditEvent(method, instance, args, auditedAnnotation);
                auditManager.recordEvent(auditEvent);
                result.addMessage("Audit event recorded for method: " + method.getName());
            }
        }
    }
    
    private void setupClassAuditing(Class<?> clazz, Audited annotation) {
        auditManager.registerClass(clazz, annotation);
    }
    
    private AuditEvent createAuditEvent(Method method, Object instance, Object[] args, Audited annotation) {
        return new AuditEvent.Builder()
            .eventType(annotation.value()[0])  // Use first event type
            .className(method.getDeclaringClass().getSimpleName())
            .methodName(method.getName())
            .level(annotation.level())
            .includeParameters(annotation.includeParameters())
            .includeReturnValue(annotation.includeReturnValue())
            .parameters(annotation.includeParameters() ? Arrays.asList(args) : null)
            .category(annotation.category())
            .tags(Arrays.asList(annotation.tags()))
            .async(annotation.async())
            .build();
    }
    
    // ==================== ASYNC ANNOTATION PROCESSING ====================
    
    private void processAsyncAnnotation(Class<?> clazz, ProcessingResult result) {
        Async asyncAnnotation = clazz.getAnnotation(Async.class);
        if (asyncAnnotation != null) {
            setupClassAsync(clazz, asyncAnnotation);
            result.addMessage("Async annotation processed for class: " + clazz.getSimpleName());
        }
    }
    
    private void processAsyncMethod(Method method, Object instance, Object[] args, ProcessingResult result) {
        Async asyncAnnotation = method.getAnnotation(Async.class);
        if (asyncAnnotation != null) {
            if (evaluateCondition(asyncAnnotation.condition(), instance, args)) {
                AsyncTask task = createAsyncTask(method, instance, args, asyncAnnotation);
                asyncManager.executeAsync(task);
                result.addMessage("Async execution scheduled for method: " + method.getName());
                result.setAsyncExecution(true);
            }
        }
    }
    
    private void setupClassAsync(Class<?> clazz, Async annotation) {
        asyncManager.registerClass(clazz, annotation);
    }
    
    private AsyncTask createAsyncTask(Method method, Object instance, Object[] args, Async annotation) {
        return new AsyncTask.Builder()
            .method(method)
            .instance(instance)
            .arguments(args)
            .strategy(annotation.strategy())
            .priority(annotation.priority())
            .timeout(annotation.timeout())
            .maxRetries(annotation.maxRetries())
            .retryDelay(annotation.retryDelay())
            .callback(annotation.callback())
            .errorHandler(annotation.errorHandler())
            .build();
    }
    
    // ==================== VALIDATION PROCESSING ====================
    
    private void processValidatorAnnotations(Method method, Object[] args, ProcessingResult result) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            processParameterValidation(parameters[i], args[i], result.getValidationResult());
        }
    }
    
    private void processFieldValidation(Field field, Object instance, ValidationResult result) {
        Validator[] validators = field.getAnnotationsByType(Validator.class);
        if (validators.length > 0) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(instance);
                
                for (Validator validator : validators) {
                    validateField(field.getName(), fieldValue, validator, result);
                }
            } catch (IllegalAccessException e) {
                result.addError("Cannot access field: " + field.getName());
            }
        }
    }
    
    private void processParameterValidation(Parameter parameter, Object value, ValidationResult result) {
        Validator[] validators = parameter.getAnnotationsByType(Validator.class);
        for (Validator validator : validators) {
            validateField(parameter.getName(), value, validator, result);
        }
    }
    
    private void validateField(String fieldName, Object value, Validator validator, ValidationResult result) {
        switch (validator.type()) {
            case NOT_NULL:
                if (value == null) {
                    result.addError(getValidationMessage(fieldName, "cannot be null", validator.message()));
                }
                break;
            case NOT_EMPTY:
                if (value == null || (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof Collection && ((Collection<?>) value).isEmpty())) {
                    result.addError(getValidationMessage(fieldName, "cannot be empty", validator.message()));
                }
                break;
            case MIN_LENGTH:
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.length() < validator.min()) {
                        result.addError(getValidationMessage(fieldName, 
                            "must be at least " + validator.min() + " characters", validator.message()));
                    }
                }
                break;
            case MAX_LENGTH:
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.length() > validator.max()) {
                        result.addError(getValidationMessage(fieldName, 
                            "must be at most " + validator.max() + " characters", validator.message()));
                    }
                }
                break;
            case EMAIL:
                if (value instanceof String) {
                    if (!isValidEmail((String) value)) {
                        result.addError(getValidationMessage(fieldName, "must be a valid email", validator.message()));
                    }
                }
                break;
            case REGEX:
                if (value instanceof String) {
                    if (!((String) value).matches(validator.pattern())) {
                        result.addError(getValidationMessage(fieldName, 
                            "does not match required pattern", validator.message()));
                    }
                }
                break;
            case POSITIVE:
                if (value instanceof Number) {
                    if (((Number) value).doubleValue() <= 0) {
                        result.addError(getValidationMessage(fieldName, "must be positive", validator.message()));
                    }
                }
                break;
            case CUSTOM:
                // Handle custom validation
                processCustomValidation(fieldName, value, validator, result);
                break;
        }
    }
    
    private String getValidationMessage(String fieldName, String defaultMessage, String customMessage) {
        return customMessage.isEmpty() ? fieldName + " " + defaultMessage : customMessage;
    }
    
    private void processCustomValidation(String fieldName, Object value, Validator validator, ValidationResult result) {
        // Implementation would invoke custom validator class
        // This is a placeholder for custom validation logic
        result.addWarning("Custom validation not implemented for field: " + fieldName);
    }
    
    // ==================== UTILITY METHODS ====================
    
    private AnnotationMetadata getOrCreateMetadata(Class<?> clazz) {
        return metadataCache.computeIfAbsent(clazz, this::createAnnotationMetadata);
    }
    
    private AnnotationMetadata createAnnotationMetadata(Class<?> clazz) {
        AnnotationMetadata.Builder builder = new AnnotationMetadata.Builder(clazz);
        
        // Scan for annotations
        for (Annotation annotation : clazz.getAnnotations()) {
            builder.addAnnotation(annotation.annotationType(), annotation);
        }
        
        // Scan methods
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                builder.addMethodAnnotation(method, annotation.annotationType(), annotation);
            }
        }
        
        // Scan fields
        for (Field field : clazz.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                builder.addFieldAnnotation(field, annotation.annotationType(), annotation);
            }
        }
        
        return builder.build();
    }
    
    private boolean evaluateCondition(String condition, Object instance, Object[] args) {
        if (condition.isEmpty()) {
            return true;
        }
        // Simplified condition evaluation - in real implementation would use SpEL
        return true;
    }
    
    private String evaluateSpEL(String expression, Method method, Object[] args) {
        // Simplified SpEL evaluation - in real implementation would use Spring's SpEL
        return expression;
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Processing result container
     */
    public static class ProcessingResult {
        private final List<String> messages = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private final ValidationResult validationResult = new ValidationResult();
        private Object cachedResult;
        private boolean asyncExecution = false;
        
        public void addMessage(String message) {
            messages.add(message);
        }
        
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        public void setCachedResult(Object result) {
            this.cachedResult = result;
        }
        
        public void setAsyncExecution(boolean async) {
            this.asyncExecution = async;
        }
        
        // Getters
        public List<String> getMessages() { return new ArrayList<>(messages); }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        public ValidationResult getValidationResult() { return validationResult; }
        public Object getCachedResult() { return cachedResult; }
        public boolean isAsyncExecution() { return asyncExecution; }
        public boolean hasCachedResult() { return cachedResult != null; }
    }
    
    /**
     * Validation result container
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
    
    /**
     * Annotation metadata container
     */
    public static class AnnotationMetadata {
        private final Class<?> clazz;
        private final Map<Class<? extends Annotation>, Annotation> classAnnotations;
        private final Map<Method, Map<Class<? extends Annotation>, Annotation>> methodAnnotations;
        private final Map<Field, Map<Class<? extends Annotation>, Annotation>> fieldAnnotations;
        
        private AnnotationMetadata(Builder builder) {
            this.clazz = builder.clazz;
            this.classAnnotations = new HashMap<>(builder.classAnnotations);
            this.methodAnnotations = new HashMap<>(builder.methodAnnotations);
            this.fieldAnnotations = new HashMap<>(builder.fieldAnnotations);
        }
        
        public Class<?> getClazz() { return clazz; }
        public Map<Class<? extends Annotation>, Annotation> getClassAnnotations() { return new HashMap<>(classAnnotations); }
        public Map<Method, Map<Class<? extends Annotation>, Annotation>> getMethodAnnotations() { return new HashMap<>(methodAnnotations); }
        public Map<Field, Map<Class<? extends Annotation>, Annotation>> getFieldAnnotations() { return new HashMap<>(fieldAnnotations); }
        
        public boolean hasClassAnnotation(Class<? extends Annotation> annotationType) {
            return classAnnotations.containsKey(annotationType);
        }
        
        public <T extends Annotation> T getClassAnnotation(Class<T> annotationType) {
            return annotationType.cast(classAnnotations.get(annotationType));
        }
        
        public static class Builder {
            private final Class<?> clazz;
            private final Map<Class<? extends Annotation>, Annotation> classAnnotations = new HashMap<>();
            private final Map<Method, Map<Class<? extends Annotation>, Annotation>> methodAnnotations = new HashMap<>();
            private final Map<Field, Map<Class<? extends Annotation>, Annotation>> fieldAnnotations = new HashMap<>();
            
            public Builder(Class<?> clazz) {
                this.clazz = clazz;
            }
            
            public Builder addAnnotation(Class<? extends Annotation> type, Annotation annotation) {
                classAnnotations.put(type, annotation);
                return this;
            }
            
            public Builder addMethodAnnotation(Method method, Class<? extends Annotation> type, Annotation annotation) {
                methodAnnotations.computeIfAbsent(method, k -> new HashMap<>()).put(type, annotation);
                return this;
            }
            
            public Builder addFieldAnnotation(Field field, Class<? extends Annotation> type, Annotation annotation) {
                fieldAnnotations.computeIfAbsent(field, k -> new HashMap<>()).put(type, annotation);
                return this;
            }
            
            public AnnotationMetadata build() {
                return new AnnotationMetadata(this);
            }
        }
    }
    
    // ==================== MANAGER PLACEHOLDER CLASSES ====================
    
    private static class CacheManager {
        public void createCache(String name, String strategy, int ttl) {
            // Placeholder implementation
        }
        
        public Object get(String cacheName, String key) {
            // Placeholder implementation
            return null;
        }
    }
    
    private static class AuditManager {
        public void registerClass(Class<?> clazz, Audited annotation) {
            // Placeholder implementation
        }
        
        public void recordEvent(AuditEvent event) {
            // Placeholder implementation
        }
    }
    
    private static class AsyncManager {
        public void registerClass(Class<?> clazz, Async annotation) {
            // Placeholder implementation
        }
        
        public void executeAsync(AsyncTask task) {
            // Placeholder implementation
        }
    }
    
    private static class ValidationEngine {
        // Placeholder implementation
    }
    
    // Placeholder classes for supporting types
    private static class EntityMetadata {
        public static class Builder {
            public Builder className(String name) { return this; }
            public Builder tableName(String name) { return this; }
            public Builder schema(String schema) { return this; }
            public Builder primaryKey(String key) { return this; }
            public Builder auditable(boolean auditable) { return this; }
            public Builder softDelete(boolean softDelete) { return this; }
            public Builder cacheStrategy(String strategy) { return this; }
            public Builder cacheTTL(int ttl) { return this; }
            public Builder description(String desc) { return this; }
            public Builder version(String version) { return this; }
            public Builder readOnly(boolean readOnly) { return this; }
            public EntityMetadata build() { return new EntityMetadata(); }
        }
    }
    
    private static class AuditEvent {
        public static class Builder {
            public Builder eventType(Audited.AuditType type) { return this; }
            public Builder className(String name) { return this; }
            public Builder methodName(String name) { return this; }
            public Builder level(Audited.Level level) { return this; }
            public Builder includeParameters(boolean include) { return this; }
            public Builder includeReturnValue(boolean include) { return this; }
            public Builder parameters(List<Object> params) { return this; }
            public Builder category(String category) { return this; }
            public Builder tags(List<String> tags) { return this; }
            public Builder async(boolean async) { return this; }
            public AuditEvent build() { return new AuditEvent(); }
        }
    }
    
    private static class AsyncTask {
        public static class Builder {
            public Builder method(Method method) { return this; }
            public Builder instance(Object instance) { return this; }
            public Builder arguments(Object[] args) { return this; }
            public Builder strategy(Async.Strategy strategy) { return this; }
            public Builder priority(Async.Priority priority) { return this; }
            public Builder timeout(long timeout) { return this; }
            public Builder maxRetries(int retries) { return this; }
            public Builder retryDelay(long delay) { return this; }
            public Builder callback(String callback) { return this; }
            public Builder errorHandler(String handler) { return this; }
            public AsyncTask build() { return new AsyncTask(); }
        }
    }
}