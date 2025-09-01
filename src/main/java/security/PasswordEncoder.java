// File location: src/main/java/security/PasswordEncoder.java
package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

/**
 * Password encoding and validation utility for secure password management
 * Supports multiple hashing algorithms including PBKDF2, bcrypt-style, and HMAC
 */
public class PasswordEncoder {
    
    // Default configuration
    private static final String DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 100000;
    private static final int DEFAULT_KEY_LENGTH = 256;
    private static final int DEFAULT_SALT_LENGTH = 32;
    
    // Password strength patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+={}\\[\\]:;\"'<>,.?/|\\\\-].*");
    
    // Configuration
    private final String algorithm;
    private final int iterations;
    private final int keyLength;
    private final int saltLength;
    private final SecureRandom secureRandom;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Creates password encoder with default configuration
     */
    public PasswordEncoder() {
        this(DEFAULT_ALGORITHM, DEFAULT_ITERATIONS, DEFAULT_KEY_LENGTH, DEFAULT_SALT_LENGTH);
    }
    
    /**
     * Creates password encoder with custom configuration
     */
    public PasswordEncoder(String algorithm, int iterations, int keyLength, int saltLength) {
        this.algorithm = algorithm;
        this.iterations = iterations;
        this.keyLength = keyLength;
        this.saltLength = saltLength;
        this.secureRandom = new SecureRandom();
    }
    
    // ==================== PASSWORD ENCODING ====================
    
    /**
     * Encodes a password using PBKDF2 with random salt
     */
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(rawPassword, salt);
        
        // Format: algorithm:iterations:salt:hash (all base64 encoded)
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);
        
        return String.format("%s:%d:%s:%s", algorithm, iterations, saltBase64, hashBase64);
    }
    
    /**
     * Encodes password with custom salt (for testing or migration)
     */
    public String encode(String rawPassword, byte[] salt) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (salt == null) {
            throw new IllegalArgumentException("Salt cannot be null");
        }
        
        byte[] hash = hashPassword(rawPassword, salt);
        
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);
        
        return String.format("%s:%d:%s:%s", algorithm, iterations, saltBase64, hashBase64);
    }
    
    // ==================== PASSWORD VERIFICATION ====================
    
    /**
     * Verifies a password against an encoded hash
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        try {
            // Parse encoded password
            String[] parts = encodedPassword.split(":", 4);
            if (parts.length != 4) {
                return false;
            }
            
            String storedAlgorithm = parts[0];
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] storedHash = Base64.getDecoder().decode(parts[3]);
            
            // Hash the provided password with the stored salt and parameters
            byte[] computedHash = hashPassword(rawPassword, salt, storedAlgorithm, storedIterations);
            
            // Compare hashes using constant-time comparison
            return constantTimeEquals(storedHash, computedHash);
            
        } catch (Exception e) {
            // If parsing or hashing fails, return false
            return false;
        }
    }
    
    /**
     * Verifies password and returns upgrade recommendation
     */
    public VerificationResult verifyAndCheckUpgrade(String rawPassword, String encodedPassword) {
        boolean matches = matches(rawPassword, encodedPassword);
        boolean needsUpgrade = needsUpgrade(encodedPassword);
        
        return new VerificationResult(matches, needsUpgrade, 
                                    needsUpgrade ? encode(rawPassword) : encodedPassword);
    }
    
    // ==================== PASSWORD STRENGTH VALIDATION ====================
    
    /**
     * Validates password strength
     */
    public PasswordStrengthResult validateStrength(String password) {
        if (password == null) {
            return new PasswordStrengthResult(false, 0, "Password cannot be null");
        }
        
        int score = 0;
        StringBuilder feedback = new StringBuilder();
        
        // Length check
        if (password.length() < 8) {
            feedback.append("Password must be at least 8 characters long. ");
        } else if (password.length() >= 8) {
            score += 1;
        }
        
        if (password.length() >= 12) {
            score += 1;
        }
        
        // Character variety checks
        if (UPPERCASE_PATTERN.matcher(password).matches()) {
            score += 1;
        } else {
            feedback.append("Include at least one uppercase letter. ");
        }
        
        if (LOWERCASE_PATTERN.matcher(password).matches()) {
            score += 1;
        } else {
            feedback.append("Include at least one lowercase letter. ");
        }
        
        if (DIGIT_PATTERN.matcher(password).matches()) {
            score += 1;
        } else {
            feedback.append("Include at least one number. ");
        }
        
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            score += 1;
        } else {
            feedback.append("Include at least one special character. ");
        }
        
        // Common patterns check
        if (containsCommonPatterns(password)) {
            score -= 2;
            feedback.append("Avoid common patterns like '123' or 'abc'. ");
        }
        
        // Repetition check
        if (hasExcessiveRepetition(password)) {
            score -= 1;
            feedback.append("Avoid excessive character repetition. ");
        }
        
        // Dictionary word check (simplified)
        if (containsCommonWords(password)) {
            score -= 1;
            feedback.append("Avoid common dictionary words. ");
        }
        
        score = Math.max(0, Math.min(5, score)); // Clamp between 0 and 5
        
        boolean isStrong = score >= 4 && password.length() >= 8;
        String strength = getStrengthLevel(score);
        
        return new PasswordStrengthResult(isStrong, score, 
                                        feedback.length() > 0 ? feedback.toString().trim() : "Strong password",
                                        strength);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Generates cryptographically secure random salt
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[saltLength];
        secureRandom.nextBytes(salt);
        return salt;
    }
    
    /**
     * Generates a secure random password
     */
    public String generateSecurePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8");
        }
        
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String allChars = uppercase + lowercase + digits + special;
        
        StringBuilder password = new StringBuilder(length);
        
        // Ensure at least one character from each category
        password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
        password.append(digits.charAt(secureRandom.nextInt(digits.length())));
        password.append(special.charAt(secureRandom.nextInt(special.length())));
        
        // Fill remaining length with random characters
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }
        
        // Shuffle the password
        return shuffleString(password.toString());
    }
    
    /**
     * Checks if encoded password needs upgrade
     */
    public boolean needsUpgrade(String encodedPassword) {
        if (encodedPassword == null) return true;
        
        try {
            String[] parts = encodedPassword.split(":", 4);
            if (parts.length != 4) return true;
            
            String storedAlgorithm = parts[0];
            int storedIterations = Integer.parseInt(parts[1]);
            
            // Check if algorithm is outdated
            if (!algorithm.equals(storedAlgorithm)) {
                return true;
            }
            
            // Check if iteration count is too low
            return storedIterations < iterations;
            
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Generates HMAC for password verification
     */
    public String generateHMAC(String password, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC", e);
        }
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private byte[] hashPassword(String password, byte[] salt) {
        return hashPassword(password, salt, algorithm, iterations);
    }
    
    private byte[] hashPassword(String password, byte[] salt, String algorithm, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
    
    private boolean containsCommonPatterns(String password) {
        String lower = password.toLowerCase();
        
        // Check for sequential characters
        String[] sequences = {"123", "234", "345", "456", "567", "678", "789",
                             "abc", "bcd", "cde", "def", "efg", "fgh", "ghi", "hij"};
        
        for (String seq : sequences) {
            if (lower.contains(seq) || lower.contains(new StringBuilder(seq).reverse().toString())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasExcessiveRepetition(String password) {
        if (password.length() < 3) return false;
        
        int maxRepeats = 0;
        int currentRepeats = 1;
        
        for (int i = 1; i < password.length(); i++) {
            if (password.charAt(i) == password.charAt(i - 1)) {
                currentRepeats++;
                maxRepeats = Math.max(maxRepeats, currentRepeats);
            } else {
                currentRepeats = 1;
            }
        }
        
        return maxRepeats > 2;
    }
    
    private boolean containsCommonWords(String password) {
        String lower = password.toLowerCase();
        
        // Simple list of common passwords/words
        String[] commonWords = {"password", "admin", "user", "login", "welcome",
                               "qwerty", "letmein", "monkey", "dragon", "master"};
        
        for (String word : commonWords) {
            if (lower.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    private String getStrengthLevel(int score) {
        return switch (score) {
            case 0, 1 -> "Very Weak";
            case 2 -> "Weak";
            case 3 -> "Fair";
            case 4 -> "Good";
            case 5 -> "Strong";
            default -> "Unknown";
        };
    }
    
    private String shuffleString(String str) {
        char[] chars = str.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
    
    // ==================== RESULT CLASSES ====================
    
    /**
     * Result of password verification with upgrade recommendation
     */
    public static class VerificationResult {
        private final boolean matches;
        private final boolean needsUpgrade;
        private final String upgradedHash;
        
        public VerificationResult(boolean matches, boolean needsUpgrade, String upgradedHash) {
            this.matches = matches;
            this.needsUpgrade = needsUpgrade;
            this.upgradedHash = upgradedHash;
        }
        
        public boolean isMatches() { return matches; }
        public boolean needsUpgrade() { return needsUpgrade; }
        public String getUpgradedHash() { return upgradedHash; }
    }
    
    /**
     * Result of password strength validation
     */
    public static class PasswordStrengthResult {
        private final boolean isStrong;
        private final int score;
        private final String feedback;
        private final String strengthLevel;
        
        public PasswordStrengthResult(boolean isStrong, int score, String feedback) {
            this(isStrong, score, feedback, null);
        }
        
        public PasswordStrengthResult(boolean isStrong, int score, String feedback, String strengthLevel) {
            this.isStrong = isStrong;
            this.score = score;
            this.feedback = feedback;
            this.strengthLevel = strengthLevel;
        }
        
        public boolean isStrong() { return isStrong; }
        public int getScore() { return score; }
        public String getFeedback() { return feedback; }
        public String getStrengthLevel() { return strengthLevel; }
        
        @Override
        public String toString() {
            return String.format("PasswordStrength{strong=%s, score=%d, level=%s, feedback='%s'}", 
                               isStrong, score, strengthLevel, feedback);
        }
    }
    
    // ==================== CONFIGURATION GETTERS ====================
    
    public String getAlgorithm() { return algorithm; }
    public int getIterations() { return iterations; }
    public int getKeyLength() { return keyLength; }
    public int getSaltLength() { return saltLength; }
    
    @Override
    public String toString() {
        return String.format("PasswordEncoder{algorithm='%s', iterations=%d, keyLength=%d, saltLength=%d}", 
                           algorithm, iterations, keyLength, saltLength);
    }
}