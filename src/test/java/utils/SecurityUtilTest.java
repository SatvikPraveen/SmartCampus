package utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import utils.SecurityUtil.PasswordHash;
import utils.SecurityUtil.PasswordStrength;
import utils.SecurityUtil.PasswordValidationResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilTest {

    @Nested
    class Hashing {
        @Test
        void saltHasRequestedLengthAndAlphabet() {
            assertThat(SecurityUtil.generateSalt()).hasSize(16).matches("[A-Za-z0-9]+");
            assertThat(SecurityUtil.generateSalt(40)).hasSize(40);
            assertThat(SecurityUtil.generateSalt(0)).isEmpty();
        }

        @Test
        void sha256HashIsSaltFollowedByPassword() {
            // SHA-256("abc") test vector from FIPS 180-2
            assertThat(SecurityUtil.hashPassword("bc", "a"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        }

        @Test
        void hashRejectsNulls() {
            assertThatThrownBy(() -> SecurityUtil.hashPassword(null, "s")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> SecurityUtil.hashPassword("p", null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void hashAndVerifyRoundTrip() {
            PasswordHash ph = SecurityUtil.hashPassword("S3cret!pass");
            assertThat(ph.getHash()).hasSize(64).matches("[0-9a-f]+");
            assertThat(ph.getIterations()).isEqualTo(1);
            assertThat(SecurityUtil.verifyPassword("S3cret!pass", ph)).isTrue();
            assertThat(SecurityUtil.verifyPassword("S3cret!pasS", ph)).isFalse();
            assertThat(SecurityUtil.verifyPassword("x", (PasswordHash) null)).isFalse();
            assertThat(SecurityUtil.verifyPassword(null, ph.getHash(), ph.getSalt())).isFalse();
            assertThat(SecurityUtil.verifyPassword("S3cret!pass", "short", ph.getSalt())).isFalse();
        }

        @Test
        void sameSecretWithDifferentSaltsHashesDifferently() {
            PasswordHash a = SecurityUtil.hashPassword("same");
            PasswordHash b = SecurityUtil.hashPassword("same");
            assertThat(a.getSalt()).isNotEqualTo(b.getSalt());
            assertThat(a.getHash()).isNotEqualTo(b.getHash());
        }

        @Test
        void pbkdf2MatchesKnownVector() {
            // PBKDF2-HMAC-SHA256, P="password", S="salt", c=1, dkLen=32
            assertThat(SecurityUtil.hashPasswordPBKDF2("password", "salt", 1))
                .isEqualTo("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b");
        }

        @Test
        void pbkdf2WithGeneratedSaltIsReproducible() {
            PasswordHash ph = SecurityUtil.hashPasswordPBKDF2("pw");
            assertThat(ph.getIterations()).isEqualTo(10000);
            assertThat(SecurityUtil.hashPasswordPBKDF2("pw", ph.getSalt(), ph.getIterations())).isEqualTo(ph.getHash());
        }

        @Test
        void passwordHashValueSemantics() {
            PasswordHash a = new PasswordHash("h", "s", 3);
            assertThat(a).isEqualTo(new PasswordHash("h", "s", 3)).hasSameHashCodeAs(new PasswordHash("h", "s", 3));
            assertThat(a).isNotEqualTo(new PasswordHash("h", "s", 4)).isNotEqualTo(new PasswordHash("h", "t", 3));
            assertThat(a.toString()).contains("iterations=3", "salt='s'", "hash='h'");
        }
    }

    @Nested
    class PasswordPolicy {
        @ParameterizedTest
        @CsvSource({
            "Kx9#mQ2$vL7!pZ4@, VERY_STRONG",
            "Tr0ub4dor&3xyz!, VERY_STRONG",
            "Summer24, STRONG",
            "Abcdef1!, GOOD",
            "password, VERY_WEAK",
            "aaaaaaaa, VERY_WEAK"
        })
        void strength(String password, PasswordStrength expected) {
            assertThat(SecurityUtil.calculatePasswordStrength(password)).isEqualTo(expected);
        }

        @Test
        void everyStrengthLevelIsReachable() {
            Set<PasswordStrength> seen = new HashSet<>();
            for (String pw : new String[]{"", "ab", "abcdefgh", "Abcdefgh", "Abcdefg1", "Abcdef1!", "Summer24",
                                          "Tr0ub4dor&3xyz!", "Kx9#mQ2$vL7!pZ4@"}) {
                seen.add(SecurityUtil.calculatePasswordStrength(pw));
            }
            assertThat(seen).containsExactlyInAnyOrder(PasswordStrength.values());
        }

        @Test
        void nullOrEmptyIsVeryWeak() {
            assertThat(SecurityUtil.calculatePasswordStrength(null)).isEqualTo(PasswordStrength.VERY_WEAK);
            assertThat(SecurityUtil.calculatePasswordStrength("")).isEqualTo(PasswordStrength.VERY_WEAK);
        }

        @Test
        void validPasswordHasNoViolations() {
            PasswordValidationResult r = SecurityUtil.validatePassword("Kx9#mQ2$vL7!pZ4@");
            assertThat(r.isValid()).isTrue();
            assertThat(r.getViolations()).isEmpty();
            assertThat(r.getStrength().getScore()).isGreaterThanOrEqualTo(3);
        }

        @Test
        void violationsAreReportedIndividually() {
            PasswordValidationResult r = SecurityUtil.validatePassword("short");
            assertThat(r.isValid()).isFalse();
            assertThat(r.getViolations()).anyMatch(v -> v.contains("at least 8"))
                .anyMatch(v -> v.contains("uppercase"))
                .anyMatch(v -> v.contains("digit"))
                .anyMatch(v -> v.contains("special"))
                .noneMatch(v -> v.contains("lowercase"));
            assertThat(r.getSuggestions()).isNotEmpty();
        }

        @Test
        void commonAndOverlongPasswordsAreRejected() {
            assertThat(SecurityUtil.validatePassword("Password").getViolations()).contains("Password is too common");
            String longPw = "Aa1!" + "x".repeat(130);
            assertThat(SecurityUtil.validatePassword(longPw).getViolations())
                .contains("Password must not exceed 128 characters");
            PasswordValidationResult nul = SecurityUtil.validatePassword(null);
            assertThat(nul.isValid()).isFalse();
            assertThat(nul.getStrength()).isEqualTo(PasswordStrength.VERY_WEAK);
        }

        @Test
        void resultListsAreDefensiveCopies() {
            PasswordValidationResult r = SecurityUtil.validatePassword("short");
            r.getViolations().clear();
            assertThat(r.getViolations()).isNotEmpty();
        }

        @Test
        void strengthEnumMetadata() {
            assertThat(PasswordStrength.VERY_STRONG.getScore()).isEqualTo(5);
            assertThat(PasswordStrength.FAIR.getDescription()).isEqualTo("Fair");
        }
    }

    @Nested
    class Lockout {
        private final String user = "user-" + UUID.randomUUID();

        @Test
        void locksAfterFiveFailuresAndResets() {
            for (int i = 0; i < 4; i++) {
                SecurityUtil.recordFailedAttempt(user);
            }
            assertThat(SecurityUtil.getFailedAttemptCount(user)).isEqualTo(4);
            assertThat(SecurityUtil.isLockedOut(user)).isFalse();
            assertThat(SecurityUtil.getRemainingLockoutMinutes(user)).isZero();

            SecurityUtil.recordFailedAttempt(user);
            assertThat(SecurityUtil.isLockedOut(user)).isTrue();
            assertThat(SecurityUtil.getRemainingLockoutMinutes(user)).isBetween(29L, 30L);

            SecurityUtil.resetFailedAttempts(user);
            assertThat(SecurityUtil.isLockedOut(user)).isFalse();
            assertThat(SecurityUtil.getFailedAttemptCount(user)).isZero();
        }

        @Test
        void unknownAndNullIdentifiersAreHarmless() {
            SecurityUtil.recordFailedAttempt(null);
            SecurityUtil.resetFailedAttempts(null);
            SecurityUtil.resetFailedAttempts(user);
            assertThat(SecurityUtil.isLockedOut(null)).isFalse();
            assertThat(SecurityUtil.getFailedAttemptCount(user)).isZero();
            assertThat(SecurityUtil.getRemainingLockoutMinutes(null)).isZero();
        }

        @Test
        void auditReflectsTrackedAttemptsAndCleanupKeepsRecentOnes() {
            SecurityUtil.recordFailedAttempt(user);
            SecurityUtil.recordFailedAttempt(user);
            SecurityUtil.cleanupOldFailedAttempts();
            SecurityUtil.SecurityAudit audit = SecurityUtil.performSecurityAudit();
            assertThat(audit.getFailedAttemptsSummary()).containsEntry(user, 2);
            assertThat(audit.getAuditTime()).isNotNull();
            assertThat(audit.getTotalLockedAccounts()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    class Sanitization {
        @Test
        void xssEscapingEncodesEachCharacterExactlyOnce() {
            assertThat(SecurityUtil.sanitizeForXSS("<a href='x'>Tom & \"Jerry\"</a>"))
                .isEqualTo("&lt;a href=&#x27;x&#x27;&gt;Tom &amp; &quot;Jerry&quot;&lt;&#x2F;a&gt;");
            assertThat(SecurityUtil.sanitizeForXSS("plain")).isEqualTo("plain");
            assertThat(SecurityUtil.sanitizeForXSS(null)).isNull();
        }

        @Test
        void sqlEscapingQuotesAndNeutralisesCommentAndTerminatorTokens() {
            assertThat(SecurityUtil.sanitizeForSQL("O'Brien")).isEqualTo("O''Brien");
            assertThat(SecurityUtil.sanitizeForSQL("say \"hi\"")).isEqualTo("say \"\"hi\"\"");
            assertThat(SecurityUtil.sanitizeForSQL("x; DROP TABLE t--")).isEqualTo("x\\; DROP TABLE t\\-\\-");
            assertThat(SecurityUtil.sanitizeForSQL("a /* c */ b")).isEqualTo("a \\/\\* c \\*\\/ b");
            assertThat(SecurityUtil.sanitizeForSQL(null)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"1; DROP TABLE users", "x' UNION SELECT *", "delete from t", "EXEC sp"})
        void detectsSqlKeywords(String input) {
            assertThat(SecurityUtil.containsSQLInjection(input)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Selection of courses", "updated grades", "John O'Neil"})
        void sqlDetectionUsesWordBoundaries(String input) {
            assertThat(SecurityUtil.containsSQLInjection(input)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"<script>alert(1)</script>", "<SCRIPT src=x>evil</SCRIPT>", "javascript:void(0)",
                                "<img onerror = 'x'>"})
        void detectsXss(String input) {
            assertThat(SecurityUtil.containsXSS(input)).isTrue();
        }

        @Test
        void benignInputIsNotFlagged() {
            assertThat(SecurityUtil.containsXSS("I like Java and scripts")).isFalse();
            assertThat(SecurityUtil.containsXSS(null)).isFalse();
            assertThat(SecurityUtil.containsSQLInjection(null)).isFalse();
        }

        @Test
        void generalSanitizer() {
            assertThat(SecurityUtil.sanitizeInput("  <b>\"x\"</b>  &  y ")).isEqualTo("bx/b y");
            assertThat(SecurityUtil.sanitizeInput(null)).isNull();
        }

        @Test
        void emailValidation() {
            assertThat(SecurityUtil.isValidEmail("a@b.co")).isTrue();
            assertThat(SecurityUtil.isValidEmail("a@b")).isFalse();
            assertThat(SecurityUtil.isValidEmail("a".repeat(250) + "@b.co")).isFalse();
            assertThat(SecurityUtil.isValidEmail(null)).isFalse();
        }
    }

    @Nested
    class Crypto {
        private static final String KEY = "0123456789abcdef"; // 128-bit

        @Test
        void encryptDecryptRoundTrip() {
            String secret = "Grades: A, B+, Ä";
            String cipher = SecurityUtil.encrypt(secret, KEY);
            assertThat(cipher).isNotEqualTo(secret).matches("[A-Za-z0-9+/=]+");
            assertThat(SecurityUtil.decrypt(cipher, KEY)).isEqualTo(secret);
        }

        @Test
        void invalidKeysAndCiphertextFail() {
            assertThatThrownBy(() -> SecurityUtil.encrypt("x", "short")).isInstanceOf(RuntimeException.class)
                .hasMessage("Encryption failed");
            assertThatThrownBy(() -> SecurityUtil.decrypt("not base64!!", KEY)).isInstanceOf(RuntimeException.class)
                .hasMessage("Decryption failed");
        }

        @Test
        void tokens() {
            assertThat(SecurityUtil.generateSecureToken()).hasSize(64).matches("[0-9a-f]+");
            assertThat(SecurityUtil.generateSecureToken(4)).hasSize(8);
            assertThat(SecurityUtil.generateUUIDToken()).hasSize(32).doesNotContain("-");
            assertThat(SecurityUtil.generateSecureToken()).isNotEqualTo(SecurityUtil.generateSecureToken());
        }

        @Test
        void sessionIdsAreValidatedByFormat() {
            String id = SecurityUtil.generateSessionId();
            assertThat(SecurityUtil.isValidSessionId(id)).isTrue();
            assertThat(SecurityUtil.isValidSessionId(id.substring(1))).isFalse();
            assertThat(SecurityUtil.isValidSessionId(id.toUpperCase().replaceAll("[0-9]", "A"))).isFalse();
            assertThat(SecurityUtil.isValidSessionId(null)).isFalse();
        }

        @Test
        void secureRandomIntIsHalfOpen() {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                seen.add(SecurityUtil.generateSecureRandomInt(1, 4));
            }
            assertThat(seen).isSubsetOf(1, 2, 3);
            assertThatThrownBy(() -> SecurityUtil.generateSecureRandomInt(3, 3)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
