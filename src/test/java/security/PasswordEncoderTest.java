package security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordEncoderTest {

    /** Low iteration count keeps PBKDF2 fast; behaviour is otherwise identical to the default. */
    private final PasswordEncoder encoder = new PasswordEncoder("PBKDF2WithHmacSHA256", 1_000, 256, 16);

    @Nested
    class EncodeAndMatch {

        @ParameterizedTest
        @ValueSource(strings = {"Secr3t!Pass", "a", "", "  spaces  ", "ünïcødé-密码-🔑", "colon:in:password"})
        void encodedPasswordMatchesOriginal(String raw) {
            String encoded = encoder.encode(raw);

            assertThat(encoder.matches(raw, encoded)).isTrue();
        }

        @Test
        void encodedFormatIsAlgorithmIterationsSaltHash() {
            String encoded = encoder.encode("Secr3t!Pass");
            String[] parts = encoded.split(":");

            assertThat(parts).hasSize(4);
            assertThat(parts[0]).isEqualTo("PBKDF2WithHmacSHA256");
            assertThat(parts[1]).isEqualTo("1000");
            assertThat(Base64.getDecoder().decode(parts[2])).hasSize(16);
            assertThat(Base64.getDecoder().decode(parts[3])).hasSize(256 / 8);
            assertThat(encoded).doesNotContain("Secr3t!Pass");
        }

        @Test
        void samePasswordProducesDifferentEncodingsBecauseSaltIsRandom() {
            Set<String> encodings = new HashSet<>();
            Set<String> salts = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                String encoded = encoder.encode("SamePassword1!");
                encodings.add(encoded);
                salts.add(encoded.split(":")[2]);
                assertThat(encoder.matches("SamePassword1!", encoded)).isTrue();
            }

            assertThat(encodings).hasSize(20);
            assertThat(salts).hasSize(20);
        }

        @Test
        void generatedSaltsAreUniqueAndOfConfiguredLength() {
            Set<String> salts = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                byte[] salt = encoder.generateSalt();
                assertThat(salt).hasSize(16);
                salts.add(Base64.getEncoder().encodeToString(salt));
            }
            assertThat(salts).hasSize(50);
        }

        @Test
        void explicitSaltIsDeterministic() {
            byte[] salt = new byte[16];
            for (int i = 0; i < salt.length; i++) salt[i] = (byte) i;

            String first = encoder.encode("Secr3t!Pass", salt);
            String second = encoder.encode("Secr3t!Pass", salt);

            assertThat(first).isEqualTo(second);
            assertThat(encoder.matches("Secr3t!Pass", first)).isTrue();
            assertThat(encoder.encode("Other!Pass1", salt)).isNotEqualTo(first);
        }

        @Test
        void defaultEncoderRoundTrips() {
            PasswordEncoder defaults = new PasswordEncoder();
            String encoded = defaults.encode("Default#Pass9");

            assertThat(encoded).startsWith("PBKDF2WithHmacSHA256:100000:");
            assertThat(defaults.matches("Default#Pass9", encoded)).isTrue();
            assertThat(defaults.matches("default#Pass9", encoded)).isFalse();
        }

        @Test
        void nullPasswordOrSaltIsRejectedWhenEncoding() {
            assertThatThrownBy(() -> encoder.encode(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> encoder.encode(null, new byte[16])).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> encoder.encode("x", null)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Rejection {

        private final String encoded = encoder.encode("Correct#Horse9");

        @ParameterizedTest
        @ValueSource(strings = {"correct#Horse9", "Correct#Horse", "Correct#Horse99", " Correct#Horse9", "", "Correct#Horse9 "})
        void wrongPasswordIsRejected(String wrong) {
            assertThat(encoder.matches(wrong, encoded)).isFalse();
        }

        @Test
        void nullInputsAreRejected() {
            assertThat(encoder.matches(null, encoded)).isFalse();
            assertThat(encoder.matches("Correct#Horse9", null)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"garbage", "a:b:c", "PBKDF2WithHmacSHA256:notanumber:AAAA:AAAA",
                "PBKDF2WithHmacSHA256:1000:!!notbase64!!:AAAA", "NoSuchAlgorithm:1000:AAAAAAAA:AAAAAAAA",
                "PBKDF2WithHmacSHA256:0:AAAAAAAA:AAAAAAAA", "PBKDF2WithHmacSHA256:1000:AAAAAAAA:"})
        void malformedEncodedValuesAreRejectedWithoutThrowing(String malformed) {
            assertThat(encoder.matches("anything", malformed)).isFalse();
        }

        @Test
        void tamperedHashIsRejected() {
            String[] parts = encoded.split(":");
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            hash[0] ^= 0x01;
            String tampered = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + Base64.getEncoder().encodeToString(hash);

            assertThat(encoder.matches("Correct#Horse9", tampered)).isFalse();
        }

        @Test
        void tamperedSaltIsRejected() {
            String[] parts = encoded.split(":");
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            salt[salt.length - 1] ^= 0x01;
            String tampered = parts[0] + ":" + parts[1] + ":" + Base64.getEncoder().encodeToString(salt) + ":" + parts[3];

            assertThat(encoder.matches("Correct#Horse9", tampered)).isFalse();
        }

        @Test
        void tamperedIterationCountIsRejected() {
            String[] parts = encoded.split(":");
            String tampered = parts[0] + ":1001:" + parts[2] + ":" + parts[3];

            assertThat(encoder.matches("Correct#Horse9", tampered)).isFalse();
        }
    }

    @Nested
    class CrossConfiguration {

        @Test
        void verifiesHashesProducedWithOtherIterationCountsUsingStoredParameters() {
            PasswordEncoder legacy = new PasswordEncoder("PBKDF2WithHmacSHA256", 500, 256, 16);
            String legacyHash = legacy.encode("Legacy#Pass1");

            assertThat(encoder.matches("Legacy#Pass1", legacyHash)).isTrue();
            assertThat(encoder.matches("Wrong#Pass1", legacyHash)).isFalse();
        }

        @Test
        void verifiesHashesProducedWithOtherAlgorithmUsingStoredAlgorithm() {
            PasswordEncoder sha1 = new PasswordEncoder("PBKDF2WithHmacSHA1", 1_000, 256, 16);
            String sha1Hash = sha1.encode("Legacy#Pass1");

            assertThat(encoder.matches("Legacy#Pass1", sha1Hash)).isTrue();
        }

        @Test
        void verifiesHashesProducedWithDifferentKeyLength() {
            // Regression: matches() used this encoder's key length instead of the stored hash length,
            // so hashes written by an encoder with another key length could never be verified.
            PasswordEncoder wide = new PasswordEncoder("PBKDF2WithHmacSHA256", 1_000, 512, 16);
            String wideHash = wide.encode("Wide#Pass12");

            assertThat(encoder.matches("Wide#Pass12", wideHash)).isTrue();
            assertThat(encoder.matches("Wide#Pass13", wideHash)).isFalse();
            assertThat(wide.matches("Wide#Pass12", encoder.encode("Wide#Pass12"))).isTrue();
        }

        @Test
        void needsUpgradeDetectsWeakerParametersOnly() {
            assertThat(encoder.needsUpgrade(encoder.encode("x"))).isFalse();
            assertThat(encoder.needsUpgrade(new PasswordEncoder("PBKDF2WithHmacSHA256", 999, 256, 16).encode("x"))).isTrue();
            assertThat(encoder.needsUpgrade(new PasswordEncoder("PBKDF2WithHmacSHA256", 2_000, 256, 16).encode("x"))).isFalse();
            assertThat(encoder.needsUpgrade(new PasswordEncoder("PBKDF2WithHmacSHA1", 1_000, 256, 16).encode("x"))).isTrue();
            assertThat(encoder.needsUpgrade(null)).isTrue();
            assertThat(encoder.needsUpgrade("not-an-encoded-password")).isTrue();
            assertThat(encoder.needsUpgrade("PBKDF2WithHmacSHA256:abc:AAAA:AAAA")).isTrue();
        }

        @Test
        void verifyAndCheckUpgradeReturnsFreshHashForOutdatedEncoding() {
            String legacyHash = new PasswordEncoder("PBKDF2WithHmacSHA256", 500, 256, 16).encode("Legacy#Pass1");

            PasswordEncoder.VerificationResult result = encoder.verifyAndCheckUpgrade("Legacy#Pass1", legacyHash);

            assertThat(result.isMatches()).isTrue();
            assertThat(result.needsUpgrade()).isTrue();
            assertThat(result.getUpgradedHash()).isNotEqualTo(legacyHash).startsWith("PBKDF2WithHmacSHA256:1000:");
            assertThat(encoder.matches("Legacy#Pass1", result.getUpgradedHash())).isTrue();
        }

        @Test
        void verifyAndCheckUpgradeKeepsCurrentHash() {
            String current = encoder.encode("Current#Pass1");

            PasswordEncoder.VerificationResult result = encoder.verifyAndCheckUpgrade("Current#Pass1", current);

            assertThat(result.isMatches()).isTrue();
            assertThat(result.needsUpgrade()).isFalse();
            assertThat(result.getUpgradedHash()).isEqualTo(current);
        }

        @Test
        void verifyAndCheckUpgradeReportsMismatch() {
            String current = encoder.encode("Current#Pass1");

            assertThat(encoder.verifyAndCheckUpgrade("wrong", current).isMatches()).isFalse();
        }
    }

    @Nested
    class Strength {

        @ParameterizedTest
        @ValueSource(strings = {"Tr0ub4dor&Zx", "Kq7!mWp2#Lz9", "Zebra!Moon42"})
        void strongPasswordsAreAccepted(String password) {
            PasswordEncoder.PasswordStrengthResult result = encoder.validateStrength(password);

            assertThat(result.isStrong()).as(result.toString()).isTrue();
            assertThat(result.getScore()).isGreaterThanOrEqualTo(4);
            assertThat(result.getStrengthLevel()).isIn("Good", "Strong");
        }

        @Test
        void allCriteriaYieldMaximumScore() {
            PasswordEncoder.PasswordStrengthResult result = encoder.validateStrength("Kq7!mWp2#Lz9");

            assertThat(result.getScore()).isEqualTo(5);
            assertThat(result.getStrengthLevel()).isEqualTo("Strong");
            assertThat(result.getFeedback()).isEqualTo("Strong password");
        }

        @ParameterizedTest
        @ValueSource(strings = {"short", "Ab1!", "alllowercase", "ALLUPPERCASE", "12345678", "Password123!", "Abc!defgh9", "zzzzmmmmqq"})
        void weakPasswordsAreRejected(String password) {
            PasswordEncoder.PasswordStrengthResult result = encoder.validateStrength(password);

            assertThat(result.isStrong()).as(result.toString()).isFalse();
            assertThat(result.getFeedback()).isNotEqualTo("Strong password");
        }

        @Test
        void feedbackNamesEachMissingCategory() {
            String feedback = encoder.validateStrength("zzzz").getFeedback();

            assertThat(feedback)
                    .contains("at least 8 characters")
                    .contains("uppercase")
                    .contains("number")
                    .contains("special character")
                    .doesNotContain("lowercase");
        }

        @Test
        void commonPatternsReduceScore() {
            int withSequence = encoder.validateStrength("Xq!9Lm#abcW").getScore();
            int withoutSequence = encoder.validateStrength("Xq!9Lm#azcW").getScore();

            assertThat(withSequence).isLessThan(withoutSequence);
        }

        @Test
        void scoreIsClampedToZeroForTerriblePasswords() {
            PasswordEncoder.PasswordStrengthResult result = encoder.validateStrength("aaa123");

            assertThat(result.getScore()).isZero();
            assertThat(result.getStrengthLevel()).isEqualTo("Very Weak");
        }

        @Test
        void nullPasswordIsNotStrong() {
            PasswordEncoder.PasswordStrengthResult result = encoder.validateStrength(null);

            assertThat(result.isStrong()).isFalse();
            assertThat(result.getScore()).isZero();
        }
    }

    @Nested
    class Generation {

        @ParameterizedTest
        @ValueSource(ints = {8, 12, 32, 64})
        void generatedPasswordHasRequestedLengthAndAllCategories(int length) {
            for (int i = 0; i < 20; i++) {
                String password = encoder.generateSecurePassword(length);

                assertThat(password).hasSize(length);
                assertThat(password).containsPattern("[A-Z]").containsPattern("[a-z]").containsPattern("\\d")
                        .containsPattern("[^A-Za-z0-9]");
            }
        }

        @Test
        void generatedPasswordsDiffer() {
            Set<String> passwords = new HashSet<>();
            for (int i = 0; i < 20; i++) passwords.add(encoder.generateSecurePassword(16));
            assertThat(passwords).hasSize(20);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 0, 7})
        void tooShortLengthIsRejected(int length) {
            assertThatThrownBy(() -> encoder.generateSecurePassword(length))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Hmac {

        @Test
        void hmacIsDeterministicAndKeyDependent() {
            String a = encoder.generateHMAC("password", "key-1");

            assertThat(encoder.generateHMAC("password", "key-1")).isEqualTo(a);
            assertThat(encoder.generateHMAC("password", "key-2")).isNotEqualTo(a);
            assertThat(encoder.generateHMAC("passwore", "key-1")).isNotEqualTo(a);
            assertThat(Base64.getDecoder().decode(a)).hasSize(32);
        }

        @Test
        void hmacMatchesKnownVector() {
            // RFC 4231 test case 2
            String expected = Base64.getEncoder().encodeToString(hex(
                    "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843"));

            assertThat(encoder.generateHMAC("what do ya want for nothing?", "Jefe")).isEqualTo(expected);
        }

        private byte[] hex(String s) {
            byte[] out = new byte[s.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
            }
            return out;
        }
    }

    @Test
    void exposesConfiguration() {
        assertThat(encoder.getAlgorithm()).isEqualTo("PBKDF2WithHmacSHA256");
        assertThat(encoder.getIterations()).isEqualTo(1_000);
        assertThat(encoder.getKeyLength()).isEqualTo(256);
        assertThat(encoder.getSaltLength()).isEqualTo(16);
        PasswordEncoder defaults = new PasswordEncoder();
        assertThat(defaults.getIterations()).isEqualTo(100_000);
        assertThat(defaults.getSaltLength()).isEqualTo(32);
    }
}
