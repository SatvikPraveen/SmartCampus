// File location: src/main/java/security/TokenManager.java
package security;

import models.User;
import enums.UserRole;
import cache.CacheManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT-like token management system for authentication and authorization
 * Handles token generation, validation, refresh, and lifecycle management
 */
public class TokenManager {
    
    private final String secretKey;
    private final CacheManager cacheManager;
    private final Map<String, TokenInfo> activeTokens;
    private final Map<String, Set<String>> sessionTokens;
    private final SecureRandom secureRandom;
    
    // Token configuration
    private final long accessTokenExpiryMinutes = 60;      // 1 hour
    private final long refreshTokenExpiryDays = 7;         // 7 days
    private final long rememberMeTokenExpiryDays = 30;     // 30 days
    private final String issuer = "SmartCampus";
    private final String algorithm = "HS256";
    
    public TokenManager() {
        this.secretKey = generateSecretKey();
        this.cacheManager = CacheManager.getInstance();
        this.activeTokens = new ConcurrentHashMap<>();
        this.sessionTokens = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        
        // Initialize token cache
        if (!cacheManager.cacheExists("tokens")) {
            cacheManager.createCache("tokens");
        }
    }
    
    // ==================== TOKEN GENERATION ====================
    
    /**
     * Generates access token for authenticated user
     */
    public String generateToken(User user, String sessionId) {
        return generateToken(user, sessionId, TokenType.ACCESS);
    }
    
    /**
     * Generates token with specific type
     */
    public String generateToken(User user, String sessionId, TokenType tokenType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = calculateExpiryTime(now, tokenType);
        
        String tokenId = generateTokenId();
        
        // Create token payload
        TokenPayload payload = new TokenPayload(
            tokenId,
            user.getUsername(),
            user.getRole(),
            sessionId,
            issuer,
            now,
            expiresAt,
            tokenType
        );
        
        // Generate token string
        String token = createTokenString(payload);
        
        // Store token info
        TokenInfo tokenInfo = new TokenInfo(tokenId, user.getUsername(), sessionId, 
                                          tokenType, now, expiresAt, token);
        activeTokens.put(tokenId, tokenInfo);
        
        // Track session tokens
        sessionTokens.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(tokenId);
        
        // Cache token
        cacheManager.put("tokens", tokenId, tokenInfo);
        
        return token;
    }
    
    /**
     * Generates refresh token
     */
    public String generateRefreshToken(User user, String sessionId) {
        return generateToken(user, sessionId, TokenType.REFRESH);
    }
    
    /**
     * Generates remember-me token for extended sessions
     */
    public String generateRememberMeToken(User user, String sessionId) {
        return generateToken(user, sessionId, TokenType.REMEMBER_ME);
    }
    
    // ==================== TOKEN VALIDATION ====================
    
    /**
     * Validates token and returns token info
     */
    public TokenInfo validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Parse token
            TokenPayload payload = parseTokenString(token);
            if (payload == null) {
                return null;
            }
            
            // Check if token exists and is not revoked
            TokenInfo tokenInfo = getTokenInfo(payload.getTokenId());
            if (tokenInfo == null || tokenInfo.isRevoked()) {
                return null;
            }
            
            // Check expiration
            if (payload.isExpired()) {
                // Mark as expired and remove
                revokeToken(payload.getTokenId());
                return null;
            }
            
            // Verify signature
            if (!verifyTokenSignature(token, payload)) {
                return null;
            }
            
            // Update last used timestamp
            tokenInfo.updateLastUsed();
            
            return tokenInfo;
            
        } catch (Exception e) {
            // Token parsing or validation failed
            return null;
        }
    }
    
    /**
     * Validates token for specific purpose
     */
    public boolean validateTokenForPurpose(String token, TokenType expectedType) {
        TokenInfo tokenInfo = validateToken(token);
        return tokenInfo != null && tokenInfo.getTokenType() == expectedType;
    }
    
    /**
     * Validates access token specifically
     */
    public TokenInfo validateAccessToken(String token) {
        TokenInfo tokenInfo = validateToken(token);
        return (tokenInfo != null && tokenInfo.getTokenType() == TokenType.ACCESS) ? tokenInfo : null;
    }
    
    /**
     * Validates refresh token
     */
    public TokenInfo validateRefreshToken(String token) {
        TokenInfo tokenInfo = validateToken(token);
        return (tokenInfo != null && tokenInfo.getTokenType() == TokenType.REFRESH) ? tokenInfo : null;
    }
    
    // ==================== TOKEN REFRESH ====================
    
    /**
     * Refreshes access token using refresh token
     */
    public RefreshResult refreshAccessToken(String refreshToken) {
        TokenInfo refreshTokenInfo = validateRefreshToken(refreshToken);
        if (refreshTokenInfo == null) {
            return new RefreshResult(false, null, null, "Invalid refresh token");
        }
        
        try {
            // Get user info (would typically come from database)
            User user = getUserByUsername(refreshTokenInfo.getUsername());
            if (user == null) {
                return new RefreshResult(false, null, null, "User not found");
            }
            
            // Generate new access token
            String newAccessToken = generateToken(user, refreshTokenInfo.getSessionId(), TokenType.ACCESS);
            
            // Optionally generate new refresh token (refresh token rotation)
            String newRefreshToken = null;
            if (shouldRotateRefreshToken(refreshTokenInfo)) {
                newRefreshToken = generateRefreshToken(user, refreshTokenInfo.getSessionId());
                // Revoke old refresh token
                revokeToken(refreshTokenInfo.getTokenId());
            }
            
            return new RefreshResult(true, newAccessToken, newRefreshToken, "Token refreshed successfully");
            
        } catch (Exception e) {
            return new RefreshResult(false, null, null, "Token refresh failed: " + e.getMessage());
        }
    }
    
    // ==================== TOKEN REVOCATION ====================
    
    /**
     * Revokes single token
     */
    public boolean revokeToken(String tokenId) {
        if (tokenId == null) return false;
        
        TokenInfo tokenInfo = activeTokens.get(tokenId);
        if (tokenInfo != null) {
            tokenInfo.revoke();
            cacheManager.put("tokens", tokenId, tokenInfo);
            return true;
        }
        return false;
    }
    
    /**
     * Revokes all tokens for a session
     */
    public void invalidateTokensBySession(String sessionId) {
        if (sessionId == null) return;
        
        Set<String> tokenIds = sessionTokens.remove(sessionId);
        if (tokenIds != null) {
            for (String tokenId : tokenIds) {
                revokeToken(tokenId);
                activeTokens.remove(tokenId);
                cacheManager.remove("tokens", tokenId);
            }
        }
    }
    
    /**
     * Revokes all tokens for a user
     */
    public void invalidateTokensByUser(String username) {
        if (username == null) return;
        
        List<String> tokensToRevoke = new ArrayList<>();
        
        for (TokenInfo tokenInfo : activeTokens.values()) {
            if (username.equals(tokenInfo.getUsername())) {
                tokensToRevoke.add(tokenInfo.getTokenId());
            }
        }
        
        for (String tokenId : tokensToRevoke) {
            revokeToken(tokenId);
        }
    }
    
    /**
     * Revokes expired tokens (cleanup)
     */
    public int revokeExpiredTokens() {
        int revokedCount = 0;
        List<String> expiredTokens = new ArrayList<>();
        
        for (Map.Entry<String, TokenInfo> entry : activeTokens.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredTokens.add(entry.getKey());
            }
        }
        
        for (String tokenId : expiredTokens) {
            if (revokeToken(tokenId)) {
                activeTokens.remove(tokenId);
                cacheManager.remove("tokens", tokenId);
                revokedCount++;
            }
        }
        
        return revokedCount;
    }
    
    // ==================== TOKEN INFORMATION ====================
    
    /**
     * Gets token information
     */
    public TokenInfo getTokenInfo(String tokenId) {
        if (tokenId == null) return null;
        
        // Try cache first
        TokenInfo tokenInfo = cacheManager.get("tokens", tokenId);
        if (tokenInfo != null) {
            return tokenInfo;
        }
        
        // Fallback to in-memory store
        return activeTokens.get(tokenId);
    }
    
    /**
     * Gets all active tokens for user
     */
    public List<TokenInfo> getUserTokens(String username) {
        return activeTokens.values().stream()
                          .filter(token -> username.equals(token.getUsername()))
                          .filter(token -> !token.isExpired() && !token.isRevoked())
                          .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets token statistics
     */
    public TokenStats getTokenStats() {
        int totalTokens = activeTokens.size();
        int activeCount = 0;
        int expiredCount = 0;
        int revokedCount = 0;
        
        Map<TokenType, Integer> typeCount = new HashMap<>();
        Map<String, Integer> userCount = new HashMap<>();
        
        for (TokenInfo token : activeTokens.values()) {
            if (token.isRevoked()) {
                revokedCount++;
            } else if (token.isExpired()) {
                expiredCount++;
            } else {
                activeCount++;
            }
            
            typeCount.merge(token.getTokenType(), 1, Integer::sum);
            userCount.merge(token.getUsername(), 1, Integer::sum);
        }
        
        return new TokenStats(totalTokens, activeCount, expiredCount, revokedCount, 
                            typeCount, userCount);
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private String generateSecretKey() {
        byte[] key = new byte[64]; // 512 bits
        secureRandom.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    private String generateTokenId() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private LocalDateTime calculateExpiryTime(LocalDateTime now, TokenType tokenType) {
        return switch (tokenType) {
            case ACCESS -> now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES);
            case REFRESH -> now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS);
            case REMEMBER_ME -> now.plus(rememberMeTokenExpiryDays, ChronoUnit.DAYS);
        };
    }
    
    private String createTokenString(TokenPayload payload) {
        // Create JWT-like structure: header.payload.signature
        String header = createHeader();
        String payloadJson = createPayloadJson(payload);
        
        String headerEncoded = base64UrlEncode(header);
        String payloadEncoded = base64UrlEncode(payloadJson);
        
        String signature = createSignature(headerEncoded + "." + payloadEncoded);
        
        return headerEncoded + "." + payloadEncoded + "." + signature;
    }
    
    private TokenPayload parseTokenString(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String payloadJson = base64UrlDecode(parts[1]);
            return parsePayloadJson(payloadJson);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean verifyTokenSignature(String token, TokenPayload payload) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String expectedSignature = createSignature(parts[0] + "." + parts[1]);
            return constantTimeEquals(expectedSignature, parts[2]);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private String createHeader() {
        return String.format("{\"alg\":\"%s\",\"typ\":\"JWT\"}", algorithm);
    }
    
    private String createPayloadJson(TokenPayload payload) {
        return String.format(
            "{\"jti\":\"%s\",\"sub\":\"%s\",\"role\":\"%s\",\"sid\":\"%s\"," +
            "\"iss\":\"%s\",\"iat\":%d,\"exp\":%d,\"type\":\"%s\"}",
            payload.getTokenId(),
            payload.getUsername(),
            payload.getUserRole(),
            payload.getSessionId(),
            payload.getIssuer(),
            payload.getIssuedAt().toEpochSecond(ZoneOffset.UTC),
            payload.getExpiresAt().toEpochSecond(ZoneOffset.UTC),
            payload.getTokenType()
        );
    }
    
    private TokenPayload parsePayloadJson(String json) {
        // Simple JSON parsing (in production, use a proper JSON library)
        try {
            String jti = extractJsonValue(json, "jti");
            String sub = extractJsonValue(json, "sub");
            String role = extractJsonValue(json, "role");
            String sid = extractJsonValue(json, "sid");
            String iss = extractJsonValue(json, "iss");
            long iat = Long.parseLong(extractJsonValue(json, "iat"));
            long exp = Long.parseLong(extractJsonValue(json, "exp"));
            String type = extractJsonValue(json, "type");
            
            return new TokenPayload(
                jti, sub, UserRole.valueOf(role), sid, iss,
                LocalDateTime.ofEpochSecond(iat, 0, ZoneOffset.UTC),
                LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC),
                TokenType.valueOf(type)
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^,}\"]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Error creating token signature", e);
        }
    }
    
    private String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
    
    private String base64UrlDecode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
    
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    private boolean shouldRotateRefreshToken(TokenInfo tokenInfo) {
        // Rotate refresh token if it's been used or if it's more than halfway to expiration
        LocalDateTime halfLife = tokenInfo.getIssuedAt().plus(
            ChronoUnit.MILLIS.between(tokenInfo.getIssuedAt(), tokenInfo.getExpiresAt()) / 2,
            ChronoUnit.MILLIS
        );
        return LocalDateTime.now().isAfter(halfLife);
    }
    
    private User getUserByUsername(String username) {
        // Placeholder - would typically query database
        return null;
    }
    
    // ==================== ENUMS AND INNER CLASSES ====================
    
    public enum TokenType {
        ACCESS,
        REFRESH,
        REMEMBER_ME
    }
    
    /**
     * Token payload information
     */
    public static class TokenPayload {
        private final String tokenId;
        private final String username;
        private final UserRole userRole;
        private final String sessionId;
        private final String issuer;
        private final LocalDateTime issuedAt;
        private final LocalDateTime expiresAt;
        private final TokenType tokenType;
        
        public TokenPayload(String tokenId, String username, UserRole userRole, String sessionId,
                          String issuer, LocalDateTime issuedAt, LocalDateTime expiresAt, TokenType tokenType) {
            this.tokenId = tokenId;
            this.username = username;
            this.userRole = userRole;
            this.sessionId = sessionId;
            this.issuer = issuer;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.tokenType = tokenType;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        // Getters
        public String getTokenId() { return tokenId; }
        public String getUsername() { return username; }
        public UserRole getUserRole() { return userRole; }
        public String getSessionId() { return sessionId; }
        public String getIssuer() { return issuer; }
        public LocalDateTime getIssuedAt() { return issuedAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public TokenType getTokenType() { return tokenType; }
    }
    
    /**
     * Token information and metadata
     */
    public static class TokenInfo {
        private final String tokenId;
        private final String username;
        private final String sessionId;
        private final TokenType tokenType;
        private final LocalDateTime issuedAt;
        private final LocalDateTime expiresAt;
        private final String tokenString;
        private LocalDateTime lastUsed;
        private boolean revoked;
        
        public TokenInfo(String tokenId, String username, String sessionId, TokenType tokenType,
                        LocalDateTime issuedAt, LocalDateTime expiresAt, String tokenString) {
            this.tokenId = tokenId;
            this.username = username;
            this.sessionId = sessionId;
            this.tokenType = tokenType;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.tokenString = tokenString;
            this.lastUsed = issuedAt;
            this.revoked = false;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        public void updateLastUsed() {
            this.lastUsed = LocalDateTime.now();
        }
        
        public void revoke() {
            this.revoked = true;
        }
        
        // Getters
        public String getTokenId() { return tokenId; }
        public String getUsername() { return username; }
        public String getSessionId() { return sessionId; }
        public TokenType getTokenType() { return tokenType; }
        public LocalDateTime getIssuedAt() { return issuedAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getTokenString() { return tokenString; }
        public LocalDateTime getLastUsed() { return lastUsed; }
        public boolean isRevoked() { return revoked; }
    }
    
    /**
     * Token refresh result
     */
    public static class RefreshResult {
        private final boolean success;
        private final String newAccessToken;
        private final String newRefreshToken;
        private final String message;
        
        public RefreshResult(boolean success, String newAccessToken, String newRefreshToken, String message) {
            this.success = success;
            this.newAccessToken = newAccessToken;
            this.newRefreshToken = newRefreshToken;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getNewAccessToken() { return newAccessToken; }
        public String getNewRefreshToken() { return newRefreshToken; }
        public String getMessage() { return message; }
    }
    
    /**
     * Token statistics
     */
    public static class TokenStats {
        private final int totalTokens;
        private final int activeTokens;
        private final int expiredTokens;
        private final int revokedTokens;
        private final Map<TokenType, Integer> tokensByType;
        private final Map<String, Integer> tokensByUser;
        
        public TokenStats(int totalTokens, int activeTokens, int expiredTokens, int revokedTokens,
                         Map<TokenType, Integer> tokensByType, Map<String, Integer> tokensByUser) {
            this.totalTokens = totalTokens;
            this.activeTokens = activeTokens;
            this.expiredTokens = expiredTokens;
            this.revokedTokens = revokedTokens;
            this.tokensByType = new HashMap<>(tokensByType);
            this.tokensByUser = new HashMap<>(tokensByUser);
        }
        
        // Getters
        public int getTotalTokens() { return totalTokens; }
        public int getActiveTokens() { return activeTokens; }
        public int getExpiredTokens() { return expiredTokens; }
        public int getRevokedTokens() { return revokedTokens; }
        public Map<TokenType, Integer> getTokensByType() { return new HashMap<>(tokensByType); }
        public Map<String, Integer> getTokensByUser() { return new HashMap<>(tokensByUser); }
        
        @Override
        public String toString() {
            return String.format("TokenStats{total=%d, active=%d, expired=%d, revoked=%d}", 
                               totalTokens, activeTokens, expiredTokens, revokedTokens);
        }
    }
}