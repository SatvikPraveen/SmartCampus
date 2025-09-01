// File location: src/main/java/utils/SecurityUtil.java
package utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Utility class for security operations including password hashing,
 * input sanitization, and basic cryptographic functions
 */
public final class SecurityUtil {
    
    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==================== CONSTANTS ====================
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String SALT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_SALT_LENGTH = 16;
    private static final int DEFAULT_HASH_ITERATIONS = 10000;
    
    // Password validation patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    
    // Input sanitization patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT|SELECT|UNION|UPDATE)\\b)", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Failed attempt tracking
    private static final Map<String, FailedAttemptInfo> FAILED_ATTEMPTS = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 30;
    
    // ==================== PASSWORD HASHING ====================
    
    /**
     * Generates a random salt
     */
    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }
    
    /**
     * Generates a random salt of specified length
     */
    public static String generateSalt(int length) {
        StringBuilder salt = new StringBuilder();
        for (int i = 0; i < length; i++) {
            salt.append(SALT_CHARS.charAt(SECURE_RANDOM.nextInt(SALT_CHARS.length())));
        }
        return salt.toString();
    }
    
    /**
     * Hashes password with salt using SHA-256
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Hashes password with automatically generated salt
     */
    public static PasswordHash hashPassword(String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        return new PasswordHash(hash, salt);
    }
    
    /**
     * Verifies password against hash and salt
     */
    public static boolean verifyPassword(String password, String hash, String salt) {
        if (password == null || hash == null || salt == null) {
            return false;
        }
        
        String computedHash = hashPassword(password, salt);
        return constantTimeEquals(hash, computedHash);
    }
    
    /**
     * Verifies password against PasswordHash object
     */
    public static boolean verifyPassword(String password, PasswordHash passwordHash) {
        if (passwordHash == null) return false;
        return verifyPassword(password, passwordHash.getHash(), passwordHash.getSalt());
    }
    
    /**
     * Hashes password using PBKDF2 (more secure for production)
     */
    public static String hashPasswordPBKDF2(String password, String salt, int iterations) {
        try {
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), 
                salt.getBytes(StandardCharsets.UTF_8), 
                iterations, 
                256
            );
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password with PBKDF2", e);
        }
    }
    
    /**
     * Hashes password using PBKDF2 with default iterations
     */
    public static PasswordHash hashPasswordPBKDF2(String password) {
        String salt = generateSalt();
        String hash = hashPasswordPBKDF2(password, salt, DEFAULT_HASH_ITERATIONS);
        return new PasswordHash(hash, salt, DEFAULT_HASH_ITERATIONS);
    }
    
    // ==================== PASSWORD VALIDATION ====================
    
    /**
     * Password strength levels
     */
    public enum PasswordStrength {
        VERY_WEAK(0, "Very Weak"),
        WEAK(1, "Weak"),
        FAIR(2, "Fair"),
        GOOD(3, "Good"),
        STRONG(4, "Strong"),
        VERY_STRONG(5, "Very Strong");
        
        private final int score;
        private final String description;
        
        PasswordStrength(int score, String description) {
            this.score = score;
            this.description = description;
        }
        
        public int getScore() { return score; }
        public String getDescription() { return description; }
    }
    
    /**
     * Password validation result
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final PasswordStrength strength;
        private final List<String> violations;
        private final List<String> suggestions;
        
        public PasswordValidationResult(boolean valid, PasswordStrength strength, 
                                      List<String> violations, List<String> suggestions) {
            this.valid = valid;
            this.strength = strength;
            this.violations = new ArrayList<>(violations);
            this.suggestions = new ArrayList<>(suggestions);
        }
        
        public boolean isValid() { return valid; }
        public PasswordStrength getStrength() { return strength; }
        public List<String> getViolations() { return new ArrayList<>(violations); }
        public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
    }
    
    /**
     * Validates password strength and requirements
     */
    public static PasswordValidationResult validatePassword(String password) {
        List<String> violations = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        if (password == null) {
            violations.add("Password cannot be null");
            return new PasswordValidationResult(false, PasswordStrength.VERY_WEAK, violations, suggestions);
        }
        
        // Length check
        if (password.length() < 8) {
            violations.add("Password must be at least 8 characters long");
            suggestions.add("Use at least 8 characters");
        }
        
        if (password.length() > 128) {
            violations.add("Password must not exceed 128 characters");
        }
        
        // Character requirements
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one uppercase letter");
            suggestions.add("Add uppercase letters (A-Z)");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one lowercase letter");
            suggestions.add("Add lowercase letters (a-z)");
        }
        
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one digit");
            suggestions.add("Add numbers (0-9)");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one special character");
            suggestions.add("Add special characters (!@#$%^&*)");
        }
        
        // Common password checks
        if (isCommonPassword(password)) {
            violations.add("Password is too common");
            suggestions.add("Use a more unique password");
        }
        
        // Calculate strength
        PasswordStrength strength = calculatePasswordStrength(password);
        
        // Additional suggestions based on strength
        if (strength.getScore() < 4) {
            suggestions.add("Consider using a longer password");
            suggestions.add("Mix different types of characters");
            suggestions.add("Avoid dictionary words and common patterns");
        }
        
        boolean isValid = violations.isEmpty() && strength.getScore() >= 3;
        
        return new PasswordValidationResult(isValid, strength, violations, suggestions);
    }
    
    /**
     * Calculates password strength score
     */
    public static PasswordStrength calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.VERY_WEAK;
        }
        
        int score = 0;
        
        // Length score
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;
        
        // Character type score
        if (UPPERCASE_PATTERN.matcher(password).matches()) score++;
        if (LOWERCASE_PATTERN.matcher(password).matches()) score++;
        if (DIGIT_PATTERN.matcher(password).matches()) score++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score++;
        
        // Complexity bonus
        Set<Character> uniqueChars = password.chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.toSet());
        if (uniqueChars.size() >= password.length() * 0.7) score++;
        
        // Penalty for common patterns
        if (hasCommonPatterns(password)) score = Math.max(0, score - 2);
        if (isCommonPassword(password)) score = Math.max(0, score - 3);
        
        // Convert score to strength
        switch (Math.min(score, 5)) {
            case 0: case 1: return PasswordStrength.VERY_WEAK;
            case 2: return PasswordStrength.WEAK;
            case 3: return PasswordStrength.FAIR;
            case 4: return PasswordStrength.GOOD;
            case 5: return PasswordStrength.STRONG;
            default: return PasswordStrength.VERY_STRONG;
        }
    }
    
    /**
     * Checks if password contains common patterns
     */
    private static boolean hasCommonPatterns(String password) {
        String lower = password.toLowerCase();
        
        // Sequential characters
        if (lower.contains("123") || lower.contains("abc") || lower.contains("qwe")) {
            return true;
        }
        
        // Repeated characters
        for (int i = 0; i < lower.length() - 2; i++) {
            if (lower.charAt(i) == lower.charAt(i + 1) && 
                lower.charAt(i + 1) == lower.charAt(i + 2)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if password is in common passwords list (simplified)
     */
    private static boolean isCommonPassword(String password) {
        Set<String> commonPasswords = Set.of(
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "dragon", "master",
            "sunshine", "princess", "football", "baseball", "superman"
        );
        return commonPasswords.contains(password.toLowerCase());
    }
    
    // ==================== AUTHENTICATION SECURITY ====================
    
    /**
     * Failed attempt tracking
     */
    private static class FailedAttemptInfo {
        private int attempts;
        private LocalDateTime lastAttempt;
        private LocalDateTime lockoutUntil;
        
        public FailedAttemptInfo() {
            this.attempts = 0;
            this.lastAttempt = LocalDateTime.now();
        }
        
        public void addFailedAttempt() {
            this.attempts++;
            this.lastAttempt = LocalDateTime.now();
            
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                this.lockoutUntil = LocalDateTime.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);
            }
        }
        
        public boolean isLockedOut() {
            return lockoutUntil != null && LocalDateTime.now().isBefore(lockoutUntil);
        }
        
        public void reset() {
            this.attempts = 0;
            this.lockoutUntil = null;
        }
        
        public int getAttempts() { return attempts; }
        public LocalDateTime getLastAttempt() { return lastAttempt; }
        public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    }
    
    /**
     * Records a failed login attempt
     */
    public static void recordFailedAttempt(String identifier) {
        if (identifier == null) return;
        
        FAILED_ATTEMPTS.computeIfAbsent(identifier, k -> new FailedAttemptInfo())
                     .addFailedAttempt();
    }
    
    /**
     * Resets failed attempts for identifier
     */
    public static void resetFailedAttempts(String identifier) {
        if (identifier == null) return;
        
        FailedAttemptInfo info = FAILED_ATTEMPTS.get(identifier);
        if (info != null) {
            info.reset();
        }
    }
    
    /**
     * Checks if identifier is locked out
     */
    public static boolean isLockedOut(String identifier) {
        if (identifier == null) return false;
        
        FailedAttemptInfo info = FAILED_ATTEMPTS.get(identifier);
        return info != null && info.isLockedOut();
    }
    
    /**
     * Gets remaining lockout time in minutes
     */
    public static long getRemainingLockoutMinutes(String identifier) {
        if (identifier == null) return 0;
        
        FailedAttemptInfo info = FAILED_ATTEMPTS.get(identifier);
        if (info == null || info.getLockoutUntil() == null) return 0;
        
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), info.getLockoutUntil());
    }
    
    /**
     * Gets failed attempt count
     */
    public static int getFailedAttemptCount(String identifier) {
        if (identifier == null) return 0;
        
        FailedAttemptInfo info = FAILED_ATTEMPTS.get(identifier);
        return info != null ? info.getAttempts() : 0;
    }
    
    // ==================== INPUT SANITIZATION ====================
    
    /**
     * Sanitizes input to prevent XSS attacks
     */
    public static String sanitizeForXSS(String input) {
        if (input == null) return null;
        
        return input.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("&", "&amp;")
                   .replaceAll("/", "&#x2F;");
    }
    
    /**
     * Sanitizes input to prevent SQL injection
     */
    public static String sanitizeForSQL(String input) {
        if (input == null) return null;
        
        return input.replaceAll("'", "''")
                   .replaceAll("\"", "\"\"")
                   .replaceAll(";", "\\;")
                   .replaceAll("--", "\\-\\-")
                   .replaceAll("/\\*", "\\/\\*")
                   .replaceAll("\\*/", "\\*\\/");
    }
    
    /**
     * Validates input against SQL injection patterns
     */
    public static boolean containsSQLInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Validates input against XSS patterns
     */
    public static boolean containsXSS(String input) {
        if (input == null) return false;
        return XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * General input sanitization
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove potentially dangerous characters
        String sanitized = input.replaceAll("[<>\"'&]", "");
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        return sanitized;
    }
    
    /**
     * Validates email format securely
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.length() > 254) return false;
        
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }
    
    // ==================== CRYPTOGRAPHIC UTILITIES ====================
    
    /**
     * Generates a secure random token
     */
    public static String generateSecureToken() {
        return generateSecureToken(32);
    }
    
    /**
     * Generates a secure random token of specified length
     */
    public static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }
    
    /**
     * Generates UUID-based token
     */
    public static String generateUUIDToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Simple encryption using AES (for demonstration - use proper key management in production)
     */
    public static String encrypt(String plaintext, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Simple decryption using AES
     */
    public static String decrypt(String encryptedText, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Converts byte array to hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        if (a.length() != b.length()) return false;
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    /**
     * Generates secure random number within range
     */
    public static int generateSecureRandomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return SECURE_RANDOM.nextInt(max - min) + min;
    }
    
    /**
     * Generates secure random boolean
     */
    public static boolean generateSecureRandomBoolean() {
        return SECURE_RANDOM.nextBoolean();
    }
    
    /**
     * Creates a secure session ID
     */
    public static String generateSessionId() {
        return generateSecureToken(64);
    }
    
    /**
     * Validates session ID format
     */
    public static boolean isValidSessionId(String sessionId) {
        return sessionId != null && 
               sessionId.length() == 128 && 
               sessionId.matches("^[a-f0-9]+$");
    }
    
    // ==================== PASSWORD HASH CLASS ====================
    
    /**
     * Container for password hash and salt
     */
    public static class PasswordHash {
        private final String hash;
        private final String salt;
        private final int iterations;
        
        public PasswordHash(String hash, String salt) {
            this(hash, salt, 1);
        }
        
        public PasswordHash(String hash, String salt, int iterations) {
            this.hash = hash;
            this.salt = salt;
            this.iterations = iterations;
        }
        
        public String getHash() { return hash; }
        public String getSalt() { return salt; }
        public int getIterations() { return iterations; }
        
        @Override
        public String toString() {
            return String.format("PasswordHash{iterations=%d, salt='%s', hash='%s'}", 
                                iterations, salt, hash);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            PasswordHash that = (PasswordHash) obj;
            return iterations == that.iterations &&
                   Objects.equals(hash, that.hash) &&
                   Objects.equals(salt, that.salt);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(hash, salt, iterations);
        }
    }
    
    // ==================== SECURITY AUDIT ====================
    
    /**
     * Security audit information
     */
    public static class SecurityAudit {
        private final Map<String, Integer> failedAttemptsSummary;
        private final int totalLockedAccounts;
        private final LocalDateTime auditTime;
        
        public SecurityAudit() {
            this.auditTime = LocalDateTime.now();
            this.failedAttemptsSummary = new HashMap<>();
            
            for (Map.Entry<String, FailedAttemptInfo> entry : FAILED_ATTEMPTS.entrySet()) {
                int attempts = entry.getValue().getAttempts();
                failedAttemptsSummary.put(entry.getKey(), attempts);
            }
            
            this.totalLockedAccounts = (int) FAILED_ATTEMPTS.values().stream()
                .filter(FailedAttemptInfo::isLockedOut)
                .count();
        }
        
        public Map<String, Integer> getFailedAttemptsSummary() { 
            return new HashMap<>(failedAttemptsSummary); 
        }
        public int getTotalLockedAccounts() { return totalLockedAccounts; }
        public LocalDateTime getAuditTime() { return auditTime; }
    }
    
    /**
     * Performs security audit
     */
    public static SecurityAudit performSecurityAudit() {
        return new SecurityAudit();
    }
    
    /**
     * Cleans up old failed attempt records
     */
    public static void cleanupOldFailedAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        
        FAILED_ATTEMPTS.entrySet().removeIf(entry -> {
            FailedAttemptInfo info = entry.getValue();
            return info.getLastAttempt().isBefore(cutoff) && !info.isLockedOut();
        });
    }
}