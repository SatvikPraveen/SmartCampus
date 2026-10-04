package services;

import interfaces.Auditable.AuditAction;
import interfaces.Auditable.AuditLevel;
import models.Admin;
import models.Professor;
import models.Student;
import models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import services.AuthService.AuthResult;
import services.AuthService.AuthenticationResult;
import services.AuthService.PermissionLevel;
import services.AuthService.Session;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest {

    private static final String ADMIN_ID = "ADMIN001";
    private static final String ADMIN_EMAIL = "admin@smartcampus.edu";
    private static final String ADMIN_PASSWORD = "admin123";

    private AuthService auth;

    /** AuthService is a process-wide singleton; discard it so every test starts clean. */
    private static void resetSingleton() throws ReflectiveOperationException {
        Field instance = AuthService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        resetSingleton();
        auth = AuthService.getInstance();
    }

    @AfterEach
    void tearDown() throws ReflectiveOperationException {
        resetSingleton();
    }

    private static Student student(String id) {
        return new Student(id, "Stu", "Dent", id.toLowerCase() + "@campus.edu", null, "SID_" + id, "CS",
                Student.AcademicYear.JUNIOR);
    }

    private static Professor professor(String id) {
        return new Professor(id, "Pro", "Fessor", id.toLowerCase() + "@campus.edu", null, "PID_" + id, "CS",
                Professor.AcademicRank.ASSOCIATE, "Systems");
    }

    private static Admin admin(String id, Admin.AdminLevel level) {
        return new Admin(id, "Ad", "Min", id.toLowerCase() + "@campus.edu", null, "AID_" + id, level,
                Admin.Department.IT_SERVICES, "Admin");
    }

    private <T extends User> T register(T user) {
        assertThat(auth.registerUser(user, "pw-" + user.getUserId())).isTrue();
        return user;
    }

    private AuthenticationResult login(User user) {
        return auth.authenticate(user.getEmail(), "pw-" + user.getUserId());
    }

    @Test
    void singletonReturnsSameInstance() {
        assertThat(AuthService.getInstance()).isSameAs(auth);
    }

    @Nested
    class Authentication {

        @Test
        void defaultAdminCanLogIn() {
            AuthenticationResult result = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getUser().getUserId()).isEqualTo(ADMIN_ID);
            assertThat(result.getSession().getUserId()).isEqualTo(ADMIN_ID);
            assertThat(result.getSession().getUserRole()).isEqualTo(result.getUser().getRole());
            assertThat(result.getSession().isValid()).isTrue();
        }

        @Test
        void usernameIsCaseInsensitive() {
            assertThat(auth.authenticate("ADMIN@SmartCampus.edu", ADMIN_PASSWORD).isSuccess()).isTrue();
        }

        @ParameterizedTest
        @CsvSource(value = {"NULL,pw", "'',pw", "'   ',pw", "admin@smartcampus.edu,NULL", "admin@smartcampus.edu,''"},
                nullValues = "NULL")
        void blankInputIsInvalidCredentials(String username, String password) {
            AuthenticationResult result = auth.authenticate(username, password);
            assertThat(result.getResult()).isEqualTo(AuthResult.INVALID_CREDENTIALS);
            assertThat(result.getSession()).isNull();
        }

        @Test
        void unknownUserAndWrongPassword() {
            assertThat(auth.authenticate("nobody@campus.edu", "x").getResult()).isEqualTo(AuthResult.USER_NOT_FOUND);

            AuthenticationResult wrong = auth.authenticate(ADMIN_EMAIL, "nope");
            assertThat(wrong.getResult()).isEqualTo(AuthResult.INVALID_CREDENTIALS);
            assertThat(wrong.getSession()).isNull();
        }

        @Test
        void disabledAccountIsRejected() {
            Student s = register(student("S1"));
            s.setActive(false);

            AuthenticationResult result = login(s);
            assertThat(result.getResult()).isEqualTo(AuthResult.ACCOUNT_DISABLED);
            assertThat(result.getSession()).isNull();
        }

        @Test
        void locksAccountAfterThreeFailedAttempts() {
            auth.authenticate(ADMIN_EMAIL, "bad1");
            auth.authenticate(ADMIN_EMAIL, "bad2");
            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).isSuccess())
                    .as("two failures are tolerated").isTrue();

            for (int i = 0; i < 3; i++) {
                auth.authenticate(ADMIN_EMAIL, "bad");
            }
            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getResult()).isEqualTo(AuthResult.ACCOUNT_LOCKED);
            assertThat(auth.getAuditStatistics()).containsEntry("lockedAccounts", 1);
        }

        @Test
        void successfulLoginResetsFailureCounter() {
            auth.authenticate(ADMIN_EMAIL, "bad");
            auth.authenticate(ADMIN_EMAIL, "bad");
            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).isSuccess()).isTrue();
            auth.authenticate(ADMIN_EMAIL, "bad");
            auth.authenticate(ADMIN_EMAIL, "bad");

            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).isSuccess()).isTrue();
        }

        @Test
        void lockoutCannotBeBypassedByChangingUsernameCase() {
            // Regression: attempts and lockouts were keyed by the raw username while the
            // user lookup is case-insensitive, so each casing had its own counter.
            auth.authenticate("admin@smartcampus.edu", "bad");
            auth.authenticate("Admin@smartcampus.edu", "bad");
            auth.authenticate("ADMIN@smartcampus.edu", "bad");

            assertThat(auth.authenticate("admin@SMARTCAMPUS.edu", ADMIN_PASSWORD).getResult())
                    .isEqualTo(AuthResult.ACCOUNT_LOCKED);
            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getResult())
                    .isEqualTo(AuthResult.ACCOUNT_LOCKED);
        }

        @Test
        void lockingOneAccountDoesNotAffectOthers() {
            Student s = register(student("S1"));
            for (int i = 0; i < 3; i++) {
                auth.authenticate(ADMIN_EMAIL, "bad");
            }
            assertThat(login(s).isSuccess()).isTrue();
        }
    }

    @Nested
    class Sessions {

        @Test
        void tokenAuthenticationAndLogout() {
            String token = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getSession().getSessionToken();

            AuthenticationResult viaToken = auth.authenticateWithToken(token);
            assertThat(viaToken.isSuccess()).isTrue();
            assertThat(viaToken.getUser().getUserId()).isEqualTo(ADMIN_ID);
            assertThat(auth.getSession(token)).isPresent();

            assertThat(auth.logout(token)).isTrue();
            assertThat(auth.logout(token)).isFalse();
            assertThat(auth.getSession(token)).isEmpty();
            assertThat(auth.authenticateWithToken(token).getResult()).isEqualTo(AuthResult.INVALID_TOKEN);
        }

        @Test
        void blankOrUnknownTokenIsInvalid() {
            assertThat(auth.authenticateWithToken("").getResult()).isEqualTo(AuthResult.INVALID_TOKEN);
            assertThat(auth.authenticateWithToken(null).getResult()).isEqualTo(AuthResult.INVALID_TOKEN);
            assertThat(auth.authenticateWithToken("SES_bogus").getResult()).isEqualTo(AuthResult.INVALID_TOKEN);
        }

        @Test
        void invalidatedSessionIsReportedExpiredAndRemoved() {
            Session session = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getSession();
            session.invalidate();

            assertThat(session.isExpired()).isTrue();
            assertThat(auth.getSession(session.getSessionToken())).isEmpty();
            assertThat(auth.authenticateWithToken(session.getSessionToken()).getResult())
                    .isEqualTo(AuthResult.SESSION_EXPIRED);
            assertThat(auth.authenticateWithToken(session.getSessionToken()).getResult())
                    .as("expired session is removed on first use").isEqualTo(AuthResult.INVALID_TOKEN);
        }

        @Test
        void cleanupRemovesOnlyExpiredSessions() {
            Session expired = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getSession();
            Student s = register(student("S1"));
            Session live = login(s).getSession();
            expired.invalidate();

            assertThat(auth.cleanupExpiredSessions()).isEqualTo(1);
            assertThat(auth.cleanupExpiredSessions()).isZero();
            assertThat(auth.getSession(live.getSessionToken())).isPresent();
        }

        @Test
        void logoutAllSessionsForUser() {
            Student s = register(student("S1"));
            Session a = login(s).getSession();
            Session b = login(s).getSession();
            Session adminSession = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getSession();
            assertThat(a.getSessionToken()).isNotEqualTo(b.getSessionToken());
            assertThat(auth.getActiveSessions("S1")).hasSize(2);

            assertThat(auth.logoutAllSessions("S1")).isEqualTo(2);

            assertThat(a.isValid()).isFalse();
            assertThat(b.isValid()).isFalse();
            assertThat(auth.getActiveSessions("S1")).isEmpty();
            assertThat(auth.getSession(adminSession.getSessionToken())).isPresent();
            assertThat(auth.logoutAllSessions("S1")).isZero();
        }
    }

    @Nested
    class UserManagement {

        @Test
        void rejectsDuplicateIdOrEmail() {
            register(student("S1"));

            assertThat(auth.registerUser(student("S1"), "pw")).isFalse();
            Student sameEmail = new Student("S2", "A", "B", "S1@CAMPUS.EDU", null, "SID2", "CS",
                    Student.AcademicYear.JUNIOR);
            assertThat(auth.registerUser(sameEmail, "pw")).isFalse();
            assertThat(auth.registerUser(null, "pw")).isFalse();
            assertThat(auth.registerUser(student("S3"), " ")).isFalse();
        }

        @Test
        void changePasswordRequiresOldPassword() {
            Student s = register(student("S1"));

            assertThat(auth.changePassword("S1", "wrong", "new-pw")).isFalse();
            assertThat(auth.changePassword("S1", "pw-S1", "")).isFalse();
            assertThat(auth.changePassword("S1", "pw-S1", "new-pw")).isTrue();

            assertThat(login(s).getResult()).isEqualTo(AuthResult.INVALID_CREDENTIALS);
            assertThat(auth.authenticate(s.getEmail(), "new-pw").isSuccess()).isTrue();
        }

        @Test
        void adminCanResetPasswordButStudentCannot() {
            Student s = register(student("S1"));
            Student other = register(student("S2"));

            assertThat(auth.resetPassword("S2", "S1", "hijack")).isFalse();
            assertThat(auth.resetPassword(ADMIN_ID, "S1", "")).isFalse();
            assertThat(auth.resetPassword(ADMIN_ID, "S1", "fresh")).isTrue();

            assertThat(auth.authenticate(s.getEmail(), "fresh").isSuccess()).isTrue();
            assertThat(login(other).isSuccess()).isTrue();
        }

        @Test
        void resetPasswordForUnknownUserFails() {
            // Regression: resetting an unknown user's password silently created credentials.
            assertThat(auth.resetPassword(ADMIN_ID, "GHOST", "pw")).isFalse();
            assertThat(auth.resetPassword(ADMIN_ID, null, "pw")).isFalse();
        }
    }

    @Nested
    class Authorization {

        @ParameterizedTest
        @EnumSource(PermissionLevel.class)
        void superAdminHasEveryPermission(PermissionLevel level) {
            assertThat(auth.hasPermission(ADMIN_ID, "anything", level)).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                "READ,course_data,true", "WRITE,course_data,false", "ADMIN,course_data,false"})
        void studentsCanOnlyRead(PermissionLevel level, String resource, boolean expected) {
            register(student("S1"));
            assertThat(auth.hasPermission("S1", resource, level)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "READ,grades,true", "WRITE,course_material,true", "WRITE,grades,false", "ADMIN,course_material,false"})
        void professorsWriteOnlyCourseResources(PermissionLevel level, String resource, boolean expected) {
            register(professor("P1"));
            assertThat(auth.hasPermission("P1", resource, level)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "JUNIOR_ADMIN,READ,true", "JUNIOR_ADMIN,WRITE,false", "JUNIOR_ADMIN,ADMIN,false",
                "SENIOR_ADMIN,WRITE,true", "SENIOR_ADMIN,ADMIN,true", "SENIOR_ADMIN,SUPER_ADMIN,false"})
        void adminLevels(Admin.AdminLevel adminLevel, PermissionLevel level, boolean expected) {
            register(admin("A1", adminLevel));
            assertThat(auth.hasPermission("A1", "user_management", level)).isEqualTo(expected);
        }

        @Test
        void inactiveOrUnknownUsersHaveNoPermissions() {
            Student s = register(student("S1"));
            s.setActive(false);

            assertThat(auth.hasPermission("S1", "x", PermissionLevel.READ)).isFalse();
            assertThat(auth.hasPermission("GHOST", "x", PermissionLevel.READ)).isFalse();
            assertThat(auth.canAccess("S1", "student", "S1", "read")).isFalse();
        }

        @Test
        void seniorAdminCanResetPasswords() {
            register(admin("A1", Admin.AdminLevel.SENIOR_ADMIN));
            register(student("S1"));
            assertThat(auth.resetPassword("A1", "S1", "fresh")).isTrue();

            register(admin("A2", Admin.AdminLevel.JUNIOR_ADMIN));
            assertThat(auth.resetPassword("A2", "S1", "other")).isFalse();
        }

        @Test
        void entityAccessRules() {
            register(student("S1"));
            register(professor("P1"));

            assertThat(auth.canAccess("S1", "student", "S1", "read")).isTrue();
            assertThat(auth.canAccess("S1", "student", "S2", "read")).isFalse();
            assertThat(auth.canAccess("S1", "course", "C1", "read")).isTrue();
            assertThat(auth.canAccess("S1", "course", "C1", "write")).isFalse();
            assertThat(auth.canAccess("S1", "grade", "G1", "write")).isFalse();
            assertThat(auth.canAccess("S1", "professor", "P1", "read")).isFalse();

            assertThat(auth.canAccess("P1", "professor", "P1", "write")).isTrue();
            assertThat(auth.canAccess("P1", "professor", "P2", "read")).isTrue();
            assertThat(auth.canAccess("P1", "professor", "P2", "write")).isFalse();
            assertThat(auth.canAccess("P1", "department", "D1", "read")).isFalse();

            assertThat(auth.canAccess(ADMIN_ID, "department", "D1", "write")).isTrue();
            assertThat(auth.canAccess(ADMIN_ID, "STUDENT", "S1", "write")).isTrue();
        }
    }

    @Nested
    class Auditing {

        @Test
        void loginAttemptsAreAudited() {
            auth.authenticate(ADMIN_EMAIL, "bad");
            auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);

            assertThat(auth.getAuditHistoryByAction(AuditAction.LOGIN)).hasSize(2);
            assertThat(auth.getUserAuditHistory(ADMIN_ID))
                    .extracting(r -> r.getDetails())
                    .contains("Authentication failed: Invalid password", "Authentication successful");
            assertThat(auth.searchAuditLogs(Map.of("action", "LOGIN", "level", "SECURITY"))).hasSize(1);
        }

        @Test
        void freshInstanceHasOnlyBootstrapRecords() {
            assertThat(auth.getAuditHistory(ADMIN_ID))
                    .extracting(r -> r.getAction())
                    .containsExactly(AuditAction.CREATE, AuditAction.CREATE);
            assertThat(auth.getAuditHistoryByLevel(AuditLevel.SYSTEM)).hasSize(1);
            assertThat(auth.getAuditStatistics())
                    .containsEntry("activeUsers", 1)
                    .containsEntry("activeSessions", 0)
                    .containsEntry("lockedAccounts", 0);
        }

        @Test
        void recentEventsAreLimited() {
            for (int i = 0; i < 5; i++) {
                auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
            }
            assertThat(auth.getRecentAuditEvents(3)).hasSize(3);
        }

        @Test
        void searchByUserOrEntityToleratesSystemRecords() {
            // Regression: bootstrap and failed-login records carry a null userId/entityId, and
            // searchAuditLogs dereferenced them, throwing a NullPointerException.
            auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
            auth.authenticateWithToken("SES_bogus");

            assertThat(auth.searchAuditLogs(Map.of("userId", ADMIN_ID)))
                    .isNotEmpty()
                    .allSatisfy(r -> assertThat(r.getUserId()).isEqualTo(ADMIN_ID));
            assertThat(auth.searchAuditLogs(Map.of("entityId", ADMIN_ID, "action", "LOGIN")))
                    .singleElement()
                    .satisfies(r -> assertThat(r.getDetails()).isEqualTo("Authentication successful"));
            assertThat(auth.searchAuditLogs(Map.of("ignoredKey", "x"))).hasSameSizeAs(auth.getRecentAuditEvents(1000));
        }

        @Test
        void auditRecordsCarryUserNames() {
            auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
            auth.logAuditEvent(AuditAction.READ, "E1", "GHOST", "lookup");
            auth.logAuditEvent(AuditAction.READ, "E2", null, "anonymous");

            assertThat(auth.getUserAuditHistory(ADMIN_ID)).last()
                    .satisfies(r -> assertThat(r.getUserName()).isEqualTo("System Administrator"));
            assertThat(auth.getAuditHistory("E1")).singleElement().satisfies(r -> {
                assertThat(r.getUserName()).isEqualTo("Unknown User");
                assertThat(r.getLevel()).isEqualTo(AuditLevel.INFO);
            });
            assertThat(auth.getAuditHistory("E2")).singleElement()
                    .satisfies(r -> assertThat(r.getUserName()).isEqualTo("Unknown"));
        }

        @Test
        void historyByDateRangeAndPurge() {
            LocalDateTime before = LocalDateTime.now().minusMinutes(1);
            auth.logAuditEvent(AuditAction.READ, "E1", ADMIN_ID, "first");
            LocalDateTime after = LocalDateTime.now().plusMinutes(1);

            assertThat(auth.getAuditHistory("E1", before, after)).hasSize(1);
            assertThat(auth.getAuditHistory("E1", after, after.plusMinutes(1))).isEmpty();

            int total = auth.getRecentAuditEvents(1000).size();
            assertThat(auth.archiveAuditRecords(after)).isZero();
            assertThat(auth.purgeAuditRecords(before)).isZero();
            assertThat(auth.purgeAuditRecords(after)).isEqualTo(total);
            assertThat(auth.getAuditStatistics()).containsEntry("totalRecords", 0);
            assertThat(auth.exportAuditLogs(before, after, "csv", "unused.csv")).isTrue();
        }

        @Test
        void statisticsCountSecurityAndErrorEvents() {
            auth.authenticate(ADMIN_EMAIL, "bad");
            auth.logAuditEvent(AuditAction.OTHER, AuditLevel.ERROR, "E1", null, "boom");

            Map<String, Object> stats = auth.getAuditStatistics();
            assertThat((long) stats.get("securityEvents")).isGreaterThanOrEqualTo(1L);
            assertThat(stats).containsEntry("errorEvents", 1L);
        }

        @Test
        void concurrentAuditingLosesNoRecords() throws Exception {
            // Regression: audit records were kept in a plain ArrayList, so concurrent logins
            // (or any concurrent audit writes) could lose records or corrupt the list, and
            // readers streaming the list could fail with ConcurrentModificationException.
            int writers = 8;
            int eventsPerWriter = 500;
            int initial = auth.getRecentAuditEvents(Integer.MAX_VALUE).size();
            ExecutorService pool = Executors.newFixedThreadPool(writers + 1);
            CountDownLatch start = new CountDownLatch(1);
            AtomicBoolean writing = new AtomicBoolean(true);
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (int w = 0; w < writers; w++) {
                    String entity = "E" + w;
                    futures.add(pool.submit(() -> {
                        start.await();
                        for (int i = 0; i < eventsPerWriter; i++) {
                            auth.logAuditEvent(AuditAction.READ, entity, null, "event " + i);
                        }
                        return null;
                    }));
                }
                Future<?> reader = pool.submit(() -> {
                    start.await();
                    while (writing.get()) {
                        auth.getAuditHistoryByAction(AuditAction.READ);
                        auth.getRecentAuditEvents(5);
                    }
                    return null;
                });

                start.countDown();
                for (Future<?> f : futures) {
                    f.get(30, TimeUnit.SECONDS);
                }
                writing.set(false);
                reader.get(30, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
            }

            assertThat(auth.getAuditStatistics()).containsEntry("totalRecords", initial + writers * eventsPerWriter);
            for (int w = 0; w < writers; w++) {
                assertThat(auth.getAuditHistory("E" + w)).hasSize(eventsPerWriter);
            }
        }
    }

    @Nested
    class LockoutAndTokens {

        @SuppressWarnings("unchecked")
        private Map<String, LocalDateTime> lockouts() throws ReflectiveOperationException {
            Field field = AuthService.class.getDeclaredField("accountLockouts");
            field.setAccessible(true);
            return (Map<String, LocalDateTime>) field.get(auth);
        }

        @Test
        void lockoutExpiresAfterThirtyMinutes() throws ReflectiveOperationException {
            for (int i = 0; i < 3; i++) {
                auth.authenticate(ADMIN_EMAIL, "bad");
            }
            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getResult()).isEqualTo(AuthResult.ACCOUNT_LOCKED);

            // Simulate the lockout having started 31 minutes ago.
            lockouts().put(ADMIN_EMAIL, LocalDateTime.now().minusMinutes(31));

            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).isSuccess()).isTrue();
            assertThat(auth.getAuditStatistics()).containsEntry("lockedAccounts", 0);
        }

        @Test
        void lockoutStillActiveJustBeforeExpiry() throws ReflectiveOperationException {
            for (int i = 0; i < 3; i++) {
                auth.authenticate(ADMIN_EMAIL, "bad");
            }
            lockouts().put(ADMIN_EMAIL, LocalDateTime.now().minusMinutes(29));

            assertThat(auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getResult()).isEqualTo(AuthResult.ACCOUNT_LOCKED);
        }

        @Test
        void tokenOfDisabledAccountIsRejected() {
            // Regression: authenticateWithToken never re-checked the account, so a session
            // created before the account was disabled kept authenticating successfully.
            Student s = register(student("S1"));
            String token = login(s).getSession().getSessionToken();
            s.setActive(false);

            AuthenticationResult result = auth.authenticateWithToken(token);
            assertThat(result.getResult()).isEqualTo(AuthResult.ACCOUNT_DISABLED);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getSession()).isNull();
            assertThat(auth.getSession(token)).as("session is terminated").isEmpty();
        }

        @Test
        void sessionMetadata() {
            Session session = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD).getSession();

            assertThat(session.getSessionToken()).startsWith("SES_");
            assertThat(session.getCreatedAt()).isBeforeOrEqualTo(session.getLastAccessAt());
            assertThat(session.getAgeMinutes()).isZero();
            assertThat(session.getIdleMinutes()).isZero();
            assertThat(session.toString()).contains(session.getSessionToken(), ADMIN_ID, "valid=true");

            LocalDateTime lastAccess = session.getLastAccessAt();
            auth.authenticateWithToken(session.getSessionToken());
            assertThat(session.getLastAccessAt()).isAfterOrEqualTo(lastAccess);
        }

        @Test
        void authenticationResultDescribesItself() {
            AuthenticationResult ok = auth.authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
            AuthenticationResult bad = auth.authenticate("ghost@campus.edu", "pw");

            assertThat(ok.getMessage()).isEqualTo("Authentication successful");
            assertThat(ok.toString()).contains("SUCCESS", "hasSession=true", ADMIN_ID);
            assertThat(bad.getMessage()).isEqualTo("User not found");
            assertThat(bad.toString()).contains("USER_NOT_FOUND", "hasSession=false", "user=null");
        }

        @Test
        void enumsExposeDisplayNames() {
            assertThat(AuthResult.TOO_MANY_ATTEMPTS.getMessage()).isEqualTo("Too many failed login attempts");
            assertThat(AuthService.SessionStatus.EXPIRED.getDisplayName()).isEqualTo("Expired");
            assertThat(PermissionLevel.SUPER_ADMIN.getDisplayName()).isEqualTo("Super Admin");
        }
    }

    @Nested
    class MoreAuthorization {

        @Test
        void systemAdminHasEveryPermission() {
            register(admin("A1", Admin.AdminLevel.SYSTEM_ADMIN));
            for (PermissionLevel level : PermissionLevel.values()) {
                assertThat(auth.hasPermission("A1", "anything", level)).as(level.name()).isTrue();
            }
        }

        @Test
        void juniorAdminCannotActAsSuperAdmin() {
            register(admin("A1", Admin.AdminLevel.JUNIOR_ADMIN));
            assertThat(auth.hasPermission("A1", "anything", PermissionLevel.SUPER_ADMIN)).isFalse();
        }

        @Test
        void professorsAndAdminsCanAccessStudentsCoursesAndGrades() {
            register(professor("P1"));
            register(admin("A1", Admin.AdminLevel.JUNIOR_ADMIN));

            for (String type : List.of("student", "course", "grade")) {
                assertThat(auth.canAccess("P1", type, "X1", "write")).as("professor " + type).isTrue();
                assertThat(auth.canAccess("A1", type, "X1", "write")).as("admin " + type).isTrue();
            }
            assertThat(auth.canAccess("A1", "professor", "P1", "write")).isTrue();
            assertThat(auth.canAccess("S404", "student", "S404", "read")).isFalse();
        }

        @Test
        void studentsMayReadButNotWriteGrades() {
            register(student("S1"));
            assertThat(auth.canAccess("S1", "grade", "G1", "read")).isTrue();
        }

        @Test
        void accessDecisionsAreAudited() {
            register(student("S1"));
            auth.canAccess("S1", "student", "S2", "read");

            assertThat(auth.getAuditHistory("S2")).singleElement().satisfies(r -> {
                assertThat(r.getLevel()).isEqualTo(AuditLevel.SECURITY);
                assertThat(r.getDetails()).isEqualTo("Access denied for student S2 action read");
            });
        }
    }
}
