package security;

import enums.UserRole;
import exceptions.AuthenticationException;
import exceptions.AuthenticationException.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * SecurityManager is a process-wide singleton, so every test uses its own username and IP address.
 * The credential store is still a placeholder (no user ever authenticates), so these tests focus on
 * the rejection paths: input validation, lockout and IP blocking.
 */
class SecurityManagerTest {

    private final SecurityManager security = SecurityManager.getInstance();

    private static String uniqueUser() {
        return "user-" + UUID.randomUUID();
    }

    private static String uniqueIp() {
        UUID id = UUID.randomUUID();
        return "10." + (id.hashCode() & 0xff) + "." + ((id.hashCode() >> 8) & 0xff) + "." + id.toString().substring(0, 8);
    }

    private static ErrorCode failureCode(ThrowingCall call) {
        AuthenticationException e = catchThrowableOfType(call::run, AuthenticationException.class);
        assertThat(e).as("expected an AuthenticationException").isNotNull();
        return e.getErrorCode();
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }

    @Test
    void singletonInitialises() {
        // Regression: the constructor re-created the "tokens" cache already created by TokenManager,
        // so class initialisation failed with ExceptionInInitializerError.
        assertThat(security).isNotNull().isSameAs(SecurityManager.getInstance());
    }

    @Test
    void nullCredentialsAreRejected() {
        assertThat(failureCode(() -> security.authenticate(null, "pw", uniqueIp(), "ua")))
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(failureCode(() -> security.authenticate(uniqueUser(), null, uniqueIp(), "ua")))
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void unknownUserIsRejected() {
        assertThat(failureCode(() -> security.authenticate(uniqueUser(), "Secr3t!pw", uniqueIp(), "ua")))
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void accountLocksAfterExactlyFiveFailedAttempts() {
        // Regression: each failure was recorded twice, locking the account after the third attempt.
        String user = uniqueUser();
        String ip = uniqueIp();

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(failureCode(() -> security.authenticate(user, "wrong", ip, "ua")))
                    .as("attempt %d", attempt).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        AuthenticationException locked = catchThrowableOfType(
                () -> security.authenticate(user, "wrong", ip, "ua"), AuthenticationException.class);
        assertThat(locked.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
        assertThat(locked.getMessage()).contains(user);
    }

    @Test
    void lockoutIsPerAccount() {
        String locked = uniqueUser();
        String ip = uniqueIp();
        for (int i = 0; i < 5; i++) {
            failureCode(() -> security.authenticate(locked, "wrong", ip, "ua"));
        }

        assertThat(failureCode(() -> security.authenticate(locked, "wrong", ip, "ua")))
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
        assertThat(failureCode(() -> security.authenticate(uniqueUser(), "wrong", uniqueIp(), "ua")))
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void ipIsBlockedAfterTwentyFailuresAcrossAccounts() {
        String ip = uniqueIp();
        for (int user = 0; user < 5; user++) {
            String name = uniqueUser();
            for (int i = 0; i < 4; i++) {
                assertThat(failureCode(() -> security.authenticate(name, "wrong", ip, "ua")))
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            }
        }

        assertThat(failureCode(() -> security.authenticate(uniqueUser(), "any", ip, "ua")))
                .isEqualTo(ErrorCode.IP_ADDRESS_BLOCKED);
        assertThat(failureCode(() -> security.authenticate(uniqueUser(), "any", uniqueIp(), "ua")))
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void tokenAuthenticationRejectsMissingOrInvalidTokens() {
        assertThat(failureCode(() -> security.authenticateWithToken(null, uniqueIp())))
                .isEqualTo(ErrorCode.TOKEN_INVALID);
        assertThat(failureCode(() -> security.authenticateWithToken("not.a.token", uniqueIp())))
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL, totp", "12345, totp", "1234567, totp", "123456, carrier-pigeon"}, nullValues = "NULL")
    void malformedMfaIsRejected(String code, String method) {
        assertThat(security.verifyMFA("u", code, method)).isFalse();
    }

    @Test
    void sessionLookupsForUnknownIdsAreEmpty() {
        assertThat(security.getSession(null)).isNull();
        assertThat(security.getSession("no-such-session")).isNull();
        assertThat(security.getUserSessions(uniqueUser())).isEmpty();
        security.removeSession(null);
        security.removeSession("no-such-session");
        security.removeAllUserSessions(uniqueUser());
    }

    @Test
    void unknownUsersHaveNoPermissions() {
        String user = uniqueUser();

        assertThat(security.hasPermission(user, RoleBasedAccess.COURSE_MANAGEMENT, RoleBasedAccess.READ)).isFalse();
        assertThat(security.hasRole(user, UserRole.STUDENT)).isFalse();
        assertThatThrownBy(() -> security.validateAccess(user, RoleBasedAccess.SYSTEM_ADMINISTRATION, RoleBasedAccess.EXECUTE))
                .isInstanceOf(AuthenticationException.class)
                .extracting(e -> ((AuthenticationException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_PRIVILEGES);
        assertThatThrownBy(() -> security.validateAccess(user, "profile", RoleBasedAccess.READ))
                .isInstanceOf(AuthenticationException.class)
                .extracting(e -> ((AuthenticationException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void sessionInfoExpiry() {
        LocalDateTime now = LocalDateTime.now();
        SecurityManager.SessionInfo live = new SecurityManager.SessionInfo("s", "u", UserRole.STUDENT, "ip", "ua",
                now, now.plusHours(1));
        SecurityManager.SessionInfo dead = new SecurityManager.SessionInfo("s", "u", UserRole.STUDENT, "ip", "ua",
                now.minusHours(2), now.minusHours(1));

        assertThat(live.isExpired()).isFalse();
        assertThat(dead.isExpired()).isTrue();
        assertThat(live.getLastActivity()).isEqualTo(now);
    }
}
