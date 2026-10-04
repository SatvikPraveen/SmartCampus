package security;

import models.Professor;
import models.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static security.TokenManager.TokenType.ACCESS;
import static security.TokenManager.TokenType.REFRESH;
import static security.TokenManager.TokenType.REMEMBER_ME;

class TokenManagerTest {

    private static final Instant START = Instant.parse("2026-03-01T10:00:00Z");

    private MutableClock clock;
    private TokenManager tokens;
    private Student alice;
    private Student bob;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        tokens = new TokenManager(clock);
        alice = new Student("U-ALICE", "Alice", "Lee", "alice@campus.edu", null, "S1", "CS",
                Student.AcademicYear.FRESHMAN);
        bob = new Student("U-BOB", "Bob", "Ray", "bob@campus.edu", null, "S2", "EE",
                Student.AcademicYear.JUNIOR);
    }

    @Test
    void defaultConstructorWorks() {
        // Regression: the constructor used secureRandom before it was initialised (NPE).
        TokenManager manager = new TokenManager();
        String token = manager.generateToken(alice, "sess");

        assertThat(manager.validateToken(token)).isNotNull();
    }

    @Nested
    class GenerationAndValidation {

        @Test
        void accessTokenRoundTrips() {
            String token = tokens.generateToken(alice, "sess-1");

            TokenManager.TokenInfo info = tokens.validateToken(token);

            assertThat(info).isNotNull();
            assertThat(info.getUsername()).isEqualTo("U-ALICE");
            assertThat(info.getSessionId()).isEqualTo("sess-1");
            assertThat(info.getTokenType()).isEqualTo(ACCESS);
            assertThat(info.getTokenString()).isEqualTo(token);
            assertThat(info.isRevoked()).isFalse();
            assertThat(Duration.between(info.getIssuedAt(), info.getExpiresAt())).isEqualTo(Duration.ofMinutes(60));
        }

        @Test
        void tokenHasJwtShapeWithSignedClaims() {
            String token = tokens.generateToken(alice, "sess-1");
            String[] parts = token.split("\\.");

            assertThat(parts).hasSize(3);
            assertThat(decode(parts[0])).isEqualTo("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String payload = decode(parts[1]);
            assertThat(payload)
                    .contains("\"sub\":\"U-ALICE\"")
                    .contains("\"role\":\"STUDENT\"")
                    .contains("\"sid\":\"sess-1\"")
                    .contains("\"iss\":\"SmartCampus\"")
                    .contains("\"type\":\"ACCESS\"")
                    .contains("\"iat\":" + START.getEpochSecond())
                    .contains("\"exp\":" + START.plus(60, ChronoUnit.MINUTES).getEpochSecond());
            assertThat(Base64.getUrlDecoder().decode(parts[2])).hasSize(32);
        }

        @Test
        void roleClaimReflectsUserRole() {
            Professor prof = new Professor("U-P", "Pat", "Kim", "pat@campus.edu", null, "P1", "D1",
                    Professor.AcademicRank.FULL, "AI");

            String payload = decode(tokens.generateToken(prof, "s").split("\\.")[1]);

            assertThat(payload).contains("\"role\":\"FULL_PROFESSOR\"");
        }

        @Test
        void everyTokenIsUnique() {
            Set<String> issued = new HashSet<>();
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                String token = tokens.generateToken(alice, "sess-1");
                issued.add(token);
                ids.add(tokens.validateToken(token).getTokenId());
            }
            assertThat(issued).hasSize(50);
            assertThat(ids).hasSize(50);
        }

        @Test
        void typeSpecificValidators() {
            String access = tokens.generateToken(alice, "s");
            String refresh = tokens.generateRefreshToken(alice, "s");
            String remember = tokens.generateRememberMeToken(alice, "s");

            assertThat(tokens.validateAccessToken(access)).isNotNull();
            assertThat(tokens.validateAccessToken(refresh)).isNull();
            assertThat(tokens.validateRefreshToken(refresh)).isNotNull();
            assertThat(tokens.validateRefreshToken(access)).isNull();
            assertThat(tokens.validateTokenForPurpose(remember, REMEMBER_ME)).isTrue();
            assertThat(tokens.validateTokenForPurpose(remember, ACCESS)).isFalse();
            assertThat(tokens.validateTokenForPurpose("junk", ACCESS)).isFalse();
        }

        @Test
        void validationUpdatesLastUsed() {
            String token = tokens.generateToken(alice, "s");
            clock.advance(Duration.ofMinutes(5));

            TokenManager.TokenInfo info = tokens.validateToken(token);

            assertThat(info.getLastUsed()).isEqualTo(info.getIssuedAt().plusMinutes(5));
        }

        @Test
        void refreshWithAccessTokenIsRejected() {
            String access = tokens.generateToken(alice, "s");

            TokenManager.RefreshResult result = tokens.refreshAccessToken(access);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getNewAccessToken()).isNull();
            assertThat(result.getMessage()).isEqualTo("Invalid refresh token");
        }
    }

    @Nested
    class Expiry {

        @Test
        void accessTokenIsValidUntilItsExpiryInstant() {
            String token = tokens.generateToken(alice, "s");

            clock.advance(Duration.ofMinutes(60));
            assertThat(tokens.validateToken(token)).as("valid exactly at expiry").isNotNull();

            clock.advance(Duration.ofSeconds(1));
            assertThat(tokens.validateToken(token)).as("expired one second later").isNull();
        }

        @Test
        void expiredTokenStaysInvalidEvenIfClockGoesBack() {
            String token = tokens.generateToken(alice, "s");
            clock.advance(Duration.ofMinutes(61));
            assertThat(tokens.validateToken(token)).isNull();

            clock.advance(Duration.ofMinutes(-30));

            assertThat(tokens.validateToken(token)).as("expired tokens are revoked on detection").isNull();
        }

        @Test
        void refreshTokenLastsSevenDays() {
            String token = tokens.generateRefreshToken(alice, "s");

            clock.advance(Duration.ofDays(7));
            assertThat(tokens.validateRefreshToken(token)).isNotNull();
            clock.advance(Duration.ofSeconds(1));
            assertThat(tokens.validateRefreshToken(token)).isNull();
        }

        @Test
        void rememberMeTokenLastsThirtyDays() {
            String token = tokens.generateRememberMeToken(alice, "s");

            clock.advance(Duration.ofDays(30));
            assertThat(tokens.validateToken(token)).isNotNull();
            clock.advance(Duration.ofSeconds(1));
            assertThat(tokens.validateToken(token)).isNull();
        }

        @Test
        void revokeExpiredTokensRemovesOnlyExpiredOnes() {
            tokens.generateToken(alice, "s1");
            tokens.generateToken(bob, "s2");
            String refresh = tokens.generateRefreshToken(alice, "s1");
            clock.advance(Duration.ofHours(2));

            assertThat(tokens.revokeExpiredTokens()).isEqualTo(2);
            assertThat(tokens.revokeExpiredTokens()).isZero();
            assertThat(tokens.getTokenStats().getTotalTokens()).isEqualTo(1);
            assertThat(tokens.validateRefreshToken(refresh)).isNotNull();
        }

        @Test
        void statsAndUserTokensDistinguishActiveExpiredAndRevoked() {
            String a1 = tokens.generateToken(alice, "s1");
            tokens.generateRefreshToken(alice, "s1");
            tokens.generateToken(bob, "s2");
            tokens.revokeToken(tokens.validateToken(a1).getTokenId());
            clock.advance(Duration.ofHours(2)); // bob's access token expires

            TokenManager.TokenStats stats = tokens.getTokenStats();

            assertThat(stats.getTotalTokens()).isEqualTo(3);
            assertThat(stats.getRevokedTokens()).isEqualTo(1);
            assertThat(stats.getExpiredTokens()).isEqualTo(1);
            assertThat(stats.getActiveTokens()).isEqualTo(1);
            assertThat(stats.getTokensByType()).containsEntry(ACCESS, 2).containsEntry(REFRESH, 1);
            assertThat(stats.getTokensByUser()).containsEntry("U-ALICE", 2).containsEntry("U-BOB", 1);
            assertThat(tokens.getUserTokens("U-ALICE")).extracting(TokenManager.TokenInfo::getTokenType)
                    .containsExactly(REFRESH);
            assertThat(tokens.getUserTokens("U-BOB")).isEmpty();
        }
    }

    @Nested
    class Tampering {

        @Test
        void modifiedClaimsWithOriginalSignatureAreRejected() {
            String token = tokens.generateToken(alice, "s");

            String escalated = withPayload(token, p -> p.replace("\"role\":\"STUDENT\"", "\"role\":\"SUPER_ADMIN\""));
            String impersonated = withPayload(token, p -> p.replace("U-ALICE", "U-BOB"));
            String otherType = withPayload(token, p -> p.replace("\"type\":\"ACCESS\"", "\"type\":\"REFRESH\""));

            assertThat(tokens.validateToken(escalated)).isNull();
            assertThat(tokens.validateToken(impersonated)).isNull();
            assertThat(tokens.validateRefreshToken(otherType)).isNull();
            assertThat(tokens.validateToken(token)).isNotNull();
        }

        @Test
        void extendedExpiryWithOriginalSignatureIsRejected() {
            String token = tokens.generateToken(alice, "s");
            long exp = START.plus(60, ChronoUnit.MINUTES).getEpochSecond();
            String extended = withPayload(token, p -> p.replace("\"exp\":" + exp, "\"exp\":" + (exp + 86_400)));
            clock.advance(Duration.ofHours(2));

            assertThat(tokens.validateToken(extended)).isNull();
        }

        @Test
        void forgedTokenCannotRevokeTheGenuineOne() {
            // Regression: validateToken acted on the unverified payload (revoking the token id it named
            // when its exp was in the past) before checking the signature.
            String token = tokens.generateToken(alice, "s");
            long exp = START.plus(60, ChronoUnit.MINUTES).getEpochSecond();
            String forged = withPayload(token, p -> p.replace("\"exp\":" + exp, "\"exp\":1"));

            assertThat(tokens.validateToken(forged)).isNull();
            assertThat(tokens.validateToken(token)).isNotNull();
        }

        @Test
        void alteredSignatureIsRejected() {
            String token = tokens.generateToken(alice, "s");
            char last = token.charAt(token.length() - 2);
            String altered = token.substring(0, token.length() - 2) + (last == 'A' ? 'B' : 'A') + token.charAt(token.length() - 1);

            assertThat(tokens.validateToken(altered)).isNull();
            assertThat(tokens.validateToken(token.substring(0, token.length() - 1))).isNull();
        }

        @Test
        void signatureFromAnotherTokenIsRejected() {
            String first = tokens.generateToken(alice, "s");
            String second = tokens.generateToken(bob, "s");
            String[] a = first.split("\\.");
            String[] b = second.split("\\.");

            assertThat(tokens.validateToken(a[0] + "." + a[1] + "." + b[2])).isNull();
        }

        @Test
        void tokenFromAnotherManagerIsRejected() {
            TokenManager other = new TokenManager(clock);
            String foreign = other.generateToken(alice, "s");

            assertThat(tokens.validateToken(foreign)).isNull();
            assertThat(other.validateToken(foreign)).isNotNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "abc", "a.b", "a.b.c", "a.b.c.d", "....", "eyJ.eyJ.sig", "!!!.###.$$$"})
        void malformedTokensAreRejectedWithoutThrowing(String token) {
            assertThat(tokens.validateToken(token)).isNull();
        }

        @Test
        void headerSwapIsRejected() {
            String token = tokens.generateToken(alice, "s");
            String[] parts = token.split("\\.");
            String noneHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

            assertThat(tokens.validateToken(noneHeader + "." + parts[1] + "." + parts[2])).isNull();
            assertThat(tokens.validateToken(noneHeader + "." + parts[1] + ".")).isNull();
        }
    }

    @Nested
    class Revocation {

        @Test
        void revokedTokenIsRejected() {
            String token = tokens.generateToken(alice, "s");
            String id = tokens.validateToken(token).getTokenId();

            assertThat(tokens.revokeToken(id)).isTrue();
            assertThat(tokens.validateToken(token)).isNull();
            assertThat(tokens.getTokenInfo(id).isRevoked()).isTrue();
        }

        @Test
        void revokingUnknownOrNullIdReturnsFalse() {
            assertThat(tokens.revokeToken(null)).isFalse();
            assertThat(tokens.revokeToken("does-not-exist")).isFalse();
            assertThat(tokens.getTokenInfo(null)).isNull();
        }

        @Test
        void invalidatingSessionOnlyAffectsThatSession() {
            String s1a = tokens.generateToken(alice, "s1");
            String s1b = tokens.generateRefreshToken(alice, "s1");
            String s2 = tokens.generateToken(alice, "s2");

            tokens.invalidateTokensBySession("s1");
            tokens.invalidateTokensBySession(null);
            tokens.invalidateTokensBySession("unknown");

            assertThat(tokens.validateToken(s1a)).isNull();
            assertThat(tokens.validateToken(s1b)).isNull();
            assertThat(tokens.validateToken(s2)).isNotNull();
            assertThat(tokens.getTokenStats().getTotalTokens()).isEqualTo(1);
        }

        @Test
        void invalidatingUserOnlyAffectsThatUser() {
            String a1 = tokens.generateToken(alice, "s1");
            String a2 = tokens.generateToken(alice, "s2");
            String b1 = tokens.generateToken(bob, "s3");

            tokens.invalidateTokensByUser("U-ALICE");
            tokens.invalidateTokensByUser(null);

            assertThat(tokens.validateToken(a1)).isNull();
            assertThat(tokens.validateToken(a2)).isNull();
            assertThat(tokens.validateToken(b1)).isNotNull();
            assertThat(tokens.getUserTokens("U-ALICE")).isEmpty();
            assertThat(tokens.getUserTokens("U-BOB")).hasSize(1);
        }
    }

    @Test
    void tokenInfoExpiryIsInclusiveOfExpiryInstant() {
        var issued = java.time.LocalDateTime.of(2026, 1, 1, 0, 0);
        var info = new TokenManager.TokenInfo("id", "u", "s", ACCESS, issued, issued.plusMinutes(1), "t");

        assertThat(info.isExpired(issued.plusMinutes(1))).isFalse();
        assertThat(info.isExpired(issued.plusMinutes(1).plusNanos(1))).isTrue();
        assertThat(info.getLastUsed()).isEqualTo(issued);
        info.revoke();
        assertThat(info.isRevoked()).isTrue();
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static String withPayload(String token, java.util.function.UnaryOperator<String> edit) {
        String[] parts = token.split("\\.");
        String original = decode(parts[1]);
        String edited = edit.apply(original);
        assertThat(edited).as("test edit must change the payload").isNotEqualTo(original);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(edited.getBytes(StandardCharsets.UTF_8));
        return parts[0] + "." + encoded + "." + parts[2];
    }
}
