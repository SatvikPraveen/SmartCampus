package exceptions;

import enums.UserRole;
import exceptions.AuthenticationException.ErrorCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationExceptionTest {

    private static final Set<ErrorCode> RETRYABLE = EnumSet.of(
            ErrorCode.SESSION_EXPIRED, ErrorCode.TOKEN_EXPIRED, ErrorCode.CAPTCHA_REQUIRED,
            ErrorCode.MULTI_FACTOR_REQUIRED, ErrorCode.FORCE_PASSWORD_CHANGE, ErrorCode.TERMS_NOT_ACCEPTED,
            ErrorCode.PRIVACY_POLICY_NOT_ACCEPTED);
    private static final Set<ErrorCode> SECURITY_CRITICAL = EnumSet.of(
            ErrorCode.TOO_MANY_ATTEMPTS, ErrorCode.IP_ADDRESS_BLOCKED, ErrorCode.ACCOUNT_LOCKED,
            ErrorCode.CONCURRENT_SESSION_LIMIT, ErrorCode.BIOMETRIC_FAILED, ErrorCode.SIGNATURE_VERIFICATION_FAILED,
            ErrorCode.CERTIFICATE_EXPIRED, ErrorCode.CERTIFICATE_INVALID);
    private static final Set<ErrorCode> USER_ACTION = EnumSet.of(
            ErrorCode.PASSWORD_EXPIRED, ErrorCode.WEAK_PASSWORD, ErrorCode.EMAIL_NOT_VERIFIED,
            ErrorCode.PHONE_NOT_VERIFIED, ErrorCode.FORCE_PASSWORD_CHANGE, ErrorCode.ACCOUNT_SETUP_INCOMPLETE,
            ErrorCode.TERMS_NOT_ACCEPTED, ErrorCode.PRIVACY_POLICY_NOT_ACCEPTED);

    private static AuthenticationException of(ErrorCode code) {
        return new AuthenticationException(code, null);
    }

    @Nested
    class Constructors {

        @Test
        void codesAreUnique() {
            assertThat(Stream.of(ErrorCode.values()).map(ErrorCode::getCode)).doesNotHaveDuplicates()
                    .allMatch(c -> c.startsWith("AUTH_"));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void nullMessageFallsBackToDescription(ErrorCode code) {
            assertThat(of(code).getMessage()).isEqualTo(code.getDescription());
        }

        @Test
        void simpleConstructorsHaveEmptyContext() {
            RuntimeException cause = new RuntimeException();
            AuthenticationException e = new AuthenticationException(ErrorCode.LDAP_ERROR, "ldap", cause);

            assertThat(e).hasMessage("ldap").hasCause(cause);
            assertThat(e.getUsername()).isNull();
            assertThat(e.getIpAddress()).isNull();
            assertThat(e.getUserAgent()).isNull();
            assertThat(e.getSessionId()).isNull();
            assertThat(e.getResource()).isNull();
            assertThat(e.getAction()).isNull();
            assertThat(e.getRequiredRole()).isNull();
            assertThat(e.getRequiredPermissions()).isEmpty();
            assertThat(e.getContext()).isEmpty();
            assertThat(e.getTimestamp()).isNotNull();
            assertThat(new AuthenticationException(ErrorCode.LDAP_ERROR, null, cause).getMessage())
                    .isEqualTo("LDAP authentication error");
        }

        @Test
        void detailedConstructorAppendsUserIpAndCode() {
            AuthenticationException e = new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "bob",
                    "10.0.0.1", "curl", "nope");

            assertThat(e.getMessage()).isEqualTo("nope [User: bob] [IP: 10.0.0.1] [Code: AUTH_001]");
            assertThat(e.getUserAgent()).isEqualTo("curl");
        }

        @Test
        void detailedConstructorWithoutUserOrIpStillAppendsCode() {
            AuthenticationException e = new AuthenticationException(ErrorCode.SSO_ERROR, null, null, null, null);

            assertThat(e.getMessage()).isEqualTo("Single Sign-On authentication error [Code: AUTH_039]");
        }

        @Test
        void fullConstructorCopiesPermissions() {
            Set<String> perms = new HashSet<>(Set.of("READ"));
            AuthenticationException e = new AuthenticationException(ErrorCode.ACCESS_DENIED, "u", null, null,
                    "sess", "/x", "GET", UserRole.STUDENT, perms, "m", null);
            perms.add("WRITE");

            assertThat(e.getRequiredPermissions()).containsExactly("READ");
            e.getRequiredPermissions().add("HACK");
            assertThat(e.getRequiredPermissions()).containsExactly("READ");
            assertThat(new AuthenticationException(ErrorCode.ACCESS_DENIED, null, null, null, null, null, null,
                    null, null, null, null).getRequiredPermissions()).isEmpty();
        }
    }

    @Nested
    class BuilderAndContext {

        @Test
        void builderProducesConsistentState() {
            AuthenticationException e = AuthenticationException.builder(ErrorCode.ROLE_NOT_AUTHORIZED)
                    .username("alice").ipAddress("1.2.3.4").userAgent("ua").sessionId("s1")
                    .resource("grades").action("write").requiredRole(UserRole.STUDENT)
                    .requiredPermission("GRADE_WRITE").requiredPermissions(Set.of("GRADE_READ"))
                    .addContext("k", "v").addContext(Map.of("n", 1))
                    .message("denied").cause(new IllegalStateException())
                    .build();

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ROLE_NOT_AUTHORIZED);
            assertThat(e.getMessage()).isEqualTo("denied [User: alice] [IP: 1.2.3.4] [Code: AUTH_013]");
            assertThat(e.getUserAgent()).isEqualTo("ua");
            assertThat(e.getSessionId()).isEqualTo("s1");
            assertThat(e.getResource()).isEqualTo("grades");
            assertThat(e.getAction()).isEqualTo("write");
            assertThat(e.getRequiredRole()).isEqualTo(UserRole.STUDENT);
            assertThat(e.getRequiredPermissions()).containsExactlyInAnyOrder("GRADE_WRITE", "GRADE_READ");
            assertThat(e.getContext()).containsOnly(Map.entry("k", "v"), Map.entry("n", 1));
            assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void contextIsDefensivelyCopiedAndTyped() {
            AuthenticationException e = AuthenticationException.builder(ErrorCode.OAUTH_ERROR).addContext("n", 1).build();

            e.getContext().clear();

            assertThat(e.getContext("n")).isEqualTo(1);
            assertThat(e.getContext("n", Integer.class)).isEqualTo(1);
            assertThat(e.getContext("n", String.class)).isNull();
            assertThat(e.getContext("absent", String.class)).isNull();
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void credentialAndAccountFactories() {
            AuthenticationException invalid = AuthenticationException.invalidCredentials("bob", "ip");
            assertThat(invalid.getMessage()).isEqualTo("Invalid username or password for user: bob [User: bob] [IP: ip] [Code: AUTH_001]");

            LocalDateTime until = LocalDateTime.of(2030, 1, 1, 0, 0);
            AuthenticationException locked = AuthenticationException.accountLocked("bob", until);
            assertThat(locked.getMessage()).startsWith("Account bob is locked until 2030-01-01T00:00");
            assertThat(locked.getContext("lockoutUntil", LocalDateTime.class)).isEqualTo(until);

            AuthenticationException disabled = AuthenticationException.accountDisabled("bob", "fraud");
            assertThat(disabled.getContext()).containsEntry("reason", "fraud");
            assertThat(disabled.getSeverity()).isEqualTo("CRITICAL");

            AuthenticationException pwd = AuthenticationException.passwordExpired("bob", until);
            assertThat(pwd.getMessage()).startsWith("Password for bob expired on 2030-01-01T00:00");
            assertThat(pwd.requiresUserAction()).isTrue();
        }

        @Test
        void sessionAndTokenFactories() {
            AuthenticationException session = AuthenticationException.sessionExpired("s1", "bob");
            assertThat(session.getSessionId()).isEqualTo("s1");
            assertThat(session.isRetryable()).isTrue();

            AuthenticationException token = AuthenticationException.invalidToken("JWT", "bad signature");
            assertThat(token.getMessage()).startsWith("Invalid JWT token: bad signature [Code: AUTH_009]");
            assertThat(token.getHttpStatusCode()).isEqualTo(401);

            AuthenticationException limit = AuthenticationException.concurrentSessionLimit("bob", 3);
            assertThat(limit.getMessage()).startsWith("Maximum concurrent sessions (3) exceeded for bob");
            assertThat(limit.getContext("maxSessions")).isEqualTo(3);
        }

        @Test
        void authorizationFactories() {
            AuthenticationException priv = AuthenticationException.insufficientPrivileges("bob", "grades", "delete",
                    UserRole.STUDENT);
            assertThat(priv.getRequiredRole()).isEqualTo(UserRole.STUDENT);
            assertThat(priv.getResource()).isEqualTo("grades");
            assertThat(priv.getAction()).isEqualTo("delete");
            assertThat(priv.getMessage()).contains("to delete grades");

            AuthenticationException denied = AuthenticationException.accessDenied("bob", "/admin", Set.of("ADMIN"));
            assertThat(denied.getRequiredPermissions()).containsExactly("ADMIN");
            assertThat(denied.getHttpStatusCode()).isEqualTo(403);
        }

        @Test
        void throttlingAndExternalFactories() {
            AuthenticationException many = AuthenticationException.tooManyAttempts("bob", "ip", 5, 5);
            assertThat(many.getMessage()).startsWith("Too many failed attempts (5/5) for bob");
            assertThat(many.getContext()).containsEntry("attemptCount", 5).containsEntry("maxAttempts", 5);
            assertThat(many.isSecurityCritical()).isTrue();

            AuthenticationException mfa = AuthenticationException.multiFactorRequired("bob", "TOTP");
            assertThat(mfa.getContext()).containsEntry("method", "TOTP");

            AuthenticationException blocked = AuthenticationException.ipAddressBlocked("6.6.6.6", "abuse");
            assertThat(blocked.getIpAddress()).isEqualTo("6.6.6.6");
            assertThat(blocked.getUsername()).isNull();
            assertThat(blocked.getMessage()).isEqualTo("IP address 6.6.6.6 is blocked: abuse [IP: 6.6.6.6] [Code: AUTH_016]");

            AuthenticationException oauth = AuthenticationException.oauthError("google", "invalid_grant", "expired");
            assertThat(oauth.getContext()).containsEntry("provider", "google").containsEntry("error", "invalid_grant")
                    .containsEntry("errorDescription", "expired");
        }
    }

    @Nested
    class Classification {

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void retryable(ErrorCode code) {
            assertThat(of(code).isRetryable()).isEqualTo(RETRYABLE.contains(code));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void securityCritical(ErrorCode code) {
            assertThat(of(code).isSecurityCritical()).isEqualTo(SECURITY_CRITICAL.contains(code));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void userAction(ErrorCode code) {
            assertThat(of(code).requiresUserAction()).isEqualTo(USER_ACTION.contains(code));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ACCOUNT_DISABLED, CRITICAL", "ACCOUNT_EXPIRED, CRITICAL", "IP_ADDRESS_BLOCKED, CRITICAL",
                "CERTIFICATE_EXPIRED, CRITICAL", "CERTIFICATE_INVALID, CRITICAL", "TOO_MANY_ATTEMPTS, HIGH",
                "ACCOUNT_LOCKED, HIGH", "CONCURRENT_SESSION_LIMIT, HIGH", "BIOMETRIC_FAILED, HIGH",
                "SIGNATURE_VERIFICATION_FAILED, HIGH", "SESSION_EXPIRED, MEDIUM", "TOKEN_EXPIRED, MEDIUM",
                "PASSWORD_EXPIRED, MEDIUM", "ACCESS_DENIED, MEDIUM", "INSUFFICIENT_PRIVILEGES, MEDIUM",
                "MULTI_FACTOR_REQUIRED, LOW", "CAPTCHA_REQUIRED, LOW", "WEAK_PASSWORD, LOW",
                "FORCE_PASSWORD_CHANGE, LOW", "LDAP_ERROR, MEDIUM"
        })
        void severity(ErrorCode code, String expected) {
            assertThat(of(code).getSeverity()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "INVALID_CREDENTIALS, 401", "PASSWORD_EXPIRED, 401", "TOKEN_EXPIRED, 401", "TOKEN_INVALID, 401",
                "TOKEN_MALFORMED, 401", "INSUFFICIENT_PRIVILEGES, 403", "ACCESS_DENIED, 403",
                "ROLE_NOT_AUTHORIZED, 403", "ACCOUNT_NOT_FOUND, 404", "TOO_MANY_ATTEMPTS, 429",
                "ACCOUNT_LOCKED, 429", "IP_ADDRESS_BLOCKED, 429", "CONCURRENT_SESSION_LIMIT, 429",
                "MAINTENANCE_MODE, 503", "WEAK_PASSWORD, 400"
        })
        void httpStatus(ErrorCode code, int status) {
            assertThat(of(code).getHttpStatusCode()).isEqualTo(status);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "INVALID_CREDENTIALS | Invalid username or password. Please try again.",
                "ACCOUNT_LOCKED | Your account is temporarily locked.",
                "ACCOUNT_DISABLED | Your account has been disabled.",
                "PASSWORD_EXPIRED | Your password has expired.",
                "SESSION_EXPIRED | Your session has expired.",
                "TOO_MANY_ATTEMPTS | Too many failed login attempts.",
                "MULTI_FACTOR_REQUIRED | Multi-factor authentication is required.",
                "ACCESS_DENIED | You do not have permission",
                "INSUFFICIENT_PRIVILEGES | You do not have sufficient privileges",
                "MAINTENANCE_MODE | The system is currently under maintenance.",
                "SAML_ERROR | Authentication failed. Please try again"
        })
        void userFriendlyMessages(ErrorCode code, String prefix) {
            assertThat(of(code).getUserFriendlyMessage()).startsWith(prefix);
        }

        @Test
        void userFriendlyMessageDoesNotLeakUsernameOrIp() {
            AuthenticationException e = AuthenticationException.invalidCredentials("secret-user", "9.9.9.9");

            assertThat(e.getUserFriendlyMessage()).doesNotContain("secret-user", "9.9.9.9");
        }
    }

    @Nested
    class Serialization {

        @Test
        void logMapContainsPopulatedFieldsOnly() {
            AuthenticationException full = AuthenticationException.builder(ErrorCode.ACCESS_DENIED)
                    .username("u").ipAddress("ip").userAgent("ua").sessionId("s").resource("r").action("a")
                    .requiredRole(UserRole.STUDENT).requiredPermission("P").addContext("k", "v").build();

            assertThat(full.toLogMap())
                    .containsEntry("errorCode", "AUTH_012")
                    .containsEntry("errorDescription", "Access denied to requested resource")
                    .containsEntry("severity", "MEDIUM")
                    .containsEntry("httpStatusCode", 403)
                    .containsEntry("username", "u").containsEntry("ipAddress", "ip").containsEntry("userAgent", "ua")
                    .containsEntry("sessionId", "s").containsEntry("resource", "r").containsEntry("action", "a")
                    .containsEntry("requiredRole", UserRole.STUDENT)
                    .containsEntry("requiredPermissions", Set.of("P"))
                    .containsEntry("context", Map.of("k", "v"))
                    .containsKeys("message", "timestamp");

            assertThat(of(ErrorCode.ACCESS_DENIED).toLogMap()).doesNotContainKeys("username", "ipAddress",
                    "userAgent", "sessionId", "resource", "action", "requiredRole", "requiredPermissions", "context");
        }

        @Test
        void toStringShowsCoreFields() {
            AuthenticationException e = AuthenticationException.invalidCredentials("bob", "ip");

            assertThat(e.toString()).startsWith("AuthenticationException{errorCode=INVALID_CREDENTIALS")
                    .contains("username='bob'", "ipAddress='ip'");
        }
    }
}
