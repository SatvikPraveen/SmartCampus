package reflection;

import annotations.Validator;
import utils.ValidationUtil;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Evaluates {@link Validator} rules against a value. Shared by {@link AnnotationProcessor} and
 * {@link ModelInspector} so that both apply the same rules with the same messages.
 *
 * <p>A {@code null} value fails only {@code NOT_NULL}, {@code NOT_EMPTY} and {@code NOT_BLANK}, and
 * only when the rule is {@code required}; every other rule treats {@code null} as "nothing to check".
 * Rules run in ascending {@link Validator#priority()} order, and a failing rule with
 * {@link Validator#stopOnFailure()} skips the remaining rules for that element.</p>
 */
final class ValidationRules {

    private ValidationRules() {
    }

    /**
     * Applies all {@code validators} to {@code value}, reporting failures to {@code errors} and rules
     * that cannot be evaluated here to {@code warnings}.
     */
    static void apply(String name, Object value, Validator[] validators,
                      Consumer<String> errors, Consumer<String> warnings) {
        Validator[] ordered = validators.clone();
        Arrays.sort(ordered, Comparator.comparingInt(Validator::priority));
        for (Validator validator : ordered) {
            boolean failed = !check(name, value, validator, errors, warnings);
            if (failed && validator.stopOnFailure()) {
                return;
            }
        }
    }

    /** Applies one rule; returns {@code false} if it reported an error. */
    static boolean check(String name, Object value, Validator validator,
                         Consumer<String> errors, Consumer<String> warnings) {
        String problem = evaluate(name, value, validator, warnings);
        if (problem == null) {
            return true;
        }
        errors.accept(validator.message().isEmpty() ? name + " " + problem : validator.message());
        return false;
    }

    /** Returns the default failure text, or {@code null} if the value passes. */
    private static String evaluate(String name, Object value, Validator validator, Consumer<String> warnings) {
        Validator.Type type = validator.type();
        if (value == null) {
            boolean nullFails = type == Validator.Type.NOT_NULL || type == Validator.Type.NOT_EMPTY
                    || type == Validator.Type.NOT_BLANK;
            if (!nullFails || !validator.required()) {
                return null;
            }
        }
        switch (type) {
            case NOT_NULL:
                return value == null ? "cannot be null" : null;
            case NOT_EMPTY:
                return value == null || sizeOf(value) == 0 ? "cannot be empty" : null;
            case NOT_BLANK:
                return value == null || value.toString().isBlank() ? "cannot be blank" : null;
            case MIN_LENGTH: {
                long size = sizeOf(value);
                return size >= 0 && size < validator.min()
                        ? "must be at least " + validator.min() + " characters" : null;
            }
            case MAX_LENGTH: {
                long size = sizeOf(value);
                return size > validator.max() ? "must be at most " + validator.max() + " characters" : null;
            }
            case RANGE:
                if (value instanceof Number number) {
                    double d = number.doubleValue();
                    return d < validator.min() || d > validator.max()
                            ? "must be between " + validator.min() + " and " + validator.max() : null;
                }
                return null;
            case POSITIVE:
                return value instanceof Number number && number.doubleValue() <= 0 ? "must be positive" : null;
            case NEGATIVE:
                return value instanceof Number number && number.doubleValue() >= 0 ? "must be negative" : null;
            case EMAIL:
                return value instanceof String s && !ValidationUtil.isValidEmail(s) ? "must be a valid email" : null;
            case PHONE:
                return value instanceof String s && !ValidationUtil.isValidPhoneNumber(s)
                        ? "must be a valid phone number" : null;
            case PASSWORD_STRENGTH:
                return value instanceof String s && !ValidationUtil.isStrongPassword(s)
                        ? "must be a strong password" : null;
            case REGEX:
                if (validator.pattern().isEmpty()) {
                    warnings.accept("No pattern given for REGEX validation of field: " + name);
                    return null;
                }
                return value instanceof String s && !s.matches(validator.pattern())
                        ? "does not match required pattern" : null;
            case ALPHANUMERIC:
                return value instanceof String s && !s.matches("[A-Za-z0-9]*") ? "must be alphanumeric" : null;
            case ALPHA:
                return value instanceof String s && !s.matches("[A-Za-z]*") ? "must contain only letters" : null;
            case NUMERIC:
                return value instanceof String s && !s.matches("[0-9]+") ? "must be numeric" : null;
            case FUTURE: {
                Integer cmp = compareToNow(value);
                return cmp != null && cmp <= 0 ? "must be in the future" : null;
            }
            case PAST: {
                Integer cmp = compareToNow(value);
                return cmp != null && cmp >= 0 ? "must be in the past" : null;
            }
            case CUSTOM:
                return custom(name, value, validator, warnings);
            case UNIQUE:
            case EXISTS:
                warnings.accept(type + " validation needs a data source and was not checked for field: " + name);
                return null;
            default:
                return null;
        }
    }

    /** Length of a string, collection, map or array; {@code -1} for anything else. */
    private static long sizeOf(Object value) {
        if (value instanceof CharSequence cs) {
            return cs.length();
        }
        if (value instanceof Collection<?> c) {
            return c.size();
        }
        if (value instanceof Map<?, ?> m) {
            return m.size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return -1;
    }

    /** Sign of {@code value - now} for supported date/time types, or {@code null} if unsupported. */
    private static Integer compareToNow(Object value) {
        if (value instanceof LocalDate d) {
            return d.compareTo(LocalDate.now());
        }
        if (value instanceof LocalDateTime d) {
            return d.compareTo(LocalDateTime.now());
        }
        if (value instanceof Instant d) {
            return d.compareTo(Instant.now());
        }
        if (value instanceof ZonedDateTime d) {
            return d.toInstant().compareTo(Instant.now());
        }
        if (value instanceof OffsetDateTime d) {
            return d.toInstant().compareTo(Instant.now());
        }
        if (value instanceof Date d) {
            return d.compareTo(new Date());
        }
        return null;
    }

    /**
     * Runs a {@code CUSTOM} rule. The {@link Validator#validator()} class must implement
     * {@code Predicate<Object>} and have a no-argument constructor.
     */
    @SuppressWarnings("unchecked")
    private static String custom(String name, Object value, Validator validator, Consumer<String> warnings) {
        Class<?> validatorClass = validator.validator();
        if (validatorClass == Void.class || !Predicate.class.isAssignableFrom(validatorClass)) {
            warnings.accept("Custom validation not implemented for field: " + name);
            return null;
        }
        try {
            Predicate<Object> predicate =
                    (Predicate<Object>) validatorClass.getDeclaredConstructor().newInstance();
            return predicate.test(value) ? null : "failed custom validation";
        } catch (ReflectiveOperationException e) {
            warnings.accept("Cannot instantiate custom validator " + validatorClass.getName()
                    + " for field: " + name);
            return null;
        }
    }
}
