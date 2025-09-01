// File location: src/main/java/annotations/Validator.java
package annotations;

import java.lang.annotation.*;

/**
 * Custom validation annotation for field and method validation
 * Provides declarative validation rules for data integrity
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Validator.List.class)
public @interface Validator {
    
    /**
     * Validation rule type
     */
    enum Type {
        NOT_NULL,
        NOT_EMPTY,
        NOT_BLANK,
        MIN_LENGTH,
        MAX_LENGTH,
        RANGE,
        EMAIL,
        PHONE,
        REGEX,
        CUSTOM,
        POSITIVE,
        NEGATIVE,
        FUTURE,
        PAST,
        UNIQUE,
        EXISTS,
        ALPHANUMERIC,
        NUMERIC,
        ALPHA,
        PASSWORD_STRENGTH
    }
    
    /**
     * The validation type to apply
     * @return validation type
     */
    Type type();
    
    /**
     * Error message when validation fails
     * @return error message
     */
    String message() default "";
    
    /**
     * Minimum value for numeric validations or minimum length for strings
     * @return minimum value
     */
    long min() default Long.MIN_VALUE;
    
    /**
     * Maximum value for numeric validations or maximum length for strings
     * @return maximum value
     */
    long max() default Long.MAX_VALUE;
    
    /**
     * Regular expression pattern for REGEX validation
     * @return regex pattern
     */
    String pattern() default "";
    
    /**
     * Custom validator class for CUSTOM validation type
     * @return validator class
     */
    Class<?> validator() default Void.class;
    
    /**
     * Groups for conditional validation
     * @return validation groups
     */
    String[] groups() default {};
    
    /**
     * Whether validation is required or optional
     * @return true if validation is required
     */
    boolean required() default true;
    
    /**
     * Priority order for multiple validations
     * @return priority (lower number = higher priority)
     */
    int priority() default 100;
    
    /**
     * Custom parameters for validation
     * @return parameter array
     */
    String[] params() default {};
    
    /**
     * Field name to reference for cross-field validation
     * @return referenced field name
     */
    String reference() default "";
    
    /**
     * Whether to stop validation on first failure
     * @return true to stop on first failure
     */
    boolean stopOnFailure() default false;
    
    /**
     * Container annotation for multiple validators on same element
     */
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        Validator[] value();
    }
}