package patterns;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import patterns.DatabaseConnection.ConnectionAttempt;
import patterns.DatabaseConnection.ConnectionStatistics;
import patterns.DatabaseConnection.ConnectionStatus;
import patterns.DatabaseConnection.ConnectionValidator;
import patterns.DatabaseConnection.ConnectionValidator.ValidationResult;
import patterns.DatabaseConnection.DatabaseConnectionFactory;
import patterns.DatabaseConnection.DatabaseMetadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Uses factory-built connections against private in-memory H2 databases. {@link DatabaseConnection#getInstance()}
 * is deliberately not exercised: it reads (and on first use creates) {@code config/database.properties} in the
 * working directory, and the singleton offers no reset hook.
 */
class DatabaseConnectionTest {

    private static final String H2 = "org.h2.Driver";

    private final List<DatabaseConnection> opened = new ArrayList<>();

    @AfterEach
    void closeAll() {
        opened.forEach(DatabaseConnection::cleanup);
    }

    private DatabaseConnection h2() {
        String url = "jdbc:h2:mem:dbconn_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        DatabaseConnection conn = DatabaseConnectionFactory.createConnection(url, "sa", "", H2);
        opened.add(conn);
        return conn;
    }

    @Nested
    class Connecting {

        @Test
        void getConnectionOpensAndReusesConnection() throws SQLException {
            DatabaseConnection db = h2();
            assertThat(db.isConnected()).isFalse();
            assertThat(db.getTimeSinceLastConnection()).isEqualTo(-1);

            Connection first = db.getConnection();
            Connection second = db.getConnection();

            assertThat(first).isSameAs(second);
            assertThat(first.getAutoCommit()).isTrue();
            assertThat(db.isConnected()).isTrue();
            assertThat(db.getTimeSinceLastConnection()).isGreaterThanOrEqualTo(0);
            assertThat(db.getConnectionStatistics().getTotalConnections()).isEqualTo(1);
        }

        @Test
        void closedConnectionIsReopenedOnDemand() throws SQLException {
            DatabaseConnection db = h2();
            Connection first = db.getConnection();

            db.closeConnection();
            assertThat(first.isClosed()).isTrue();
            assertThat(db.isConnected()).isFalse();

            Connection second = db.getConnection();
            assertThat(second).isNotSameAs(first);
            assertThat(second.isClosed()).isFalse();
            assertThat(db.getConnectionStatistics().getTotalConnections()).isEqualTo(2);
        }

        @Test
        void externallyClosedConnectionIsReplaced() throws SQLException {
            DatabaseConnection db = h2();
            Connection first = db.getConnection();
            first.close();

            assertThat(db.isConnected()).isFalse();
            assertThat(db.getConnection()).isNotSameAs(first);
        }

        @Test
        void resetConnectionReplacesConnection() throws SQLException {
            DatabaseConnection db = h2();
            Connection first = db.getConnection();

            db.resetConnection();

            assertThat(first.isClosed()).isTrue();
            assertThat(db.isConnected()).isTrue();
            assertThat(db.getConnection()).isNotSameAs(first);
        }

        @Test
        void queryAndValidityChecksSucceedAgainstH2() {
            DatabaseConnection db = h2();

            assertThat(db.testConnection()).isTrue();
            assertThat(db.testConnectionWithQuery()).isTrue();
        }

        @Test
        void testConnectionFactoryUsesInMemoryH2() throws SQLException {
            DatabaseConnection db = DatabaseConnectionFactory.createTestConnection();
            opened.add(db);

            assertThat(db.getConnectionStatus().getJdbcUrl()).isEqualTo("jdbc:h2:mem:testdb");
            assertThat(db.getDatabaseMetadata().getProductName()).isEqualTo("H2");
        }

        @Test
        void unreachableDatabaseFailsAndIsRecorded() {
            DatabaseConnection db = DatabaseConnectionFactory.createConnection(
                "jdbc:nosuchdb://localhost/x", "sa", "", H2);
            opened.add(db);

            assertThat(db.testConnection()).isFalse();
            assertThat(db.testConnectionWithQuery()).isFalse();
            assertThatThrownBy(db::getConnection).isInstanceOf(SQLException.class);

            List<ConnectionAttempt> history = db.getConnectionHistory();
            assertThat(history).isNotEmpty().allSatisfy(a -> {
                assertThat(a.isSuccessful()).isFalse();
                assertThat(a.getError()).isInstanceOf(SQLException.class);
                assertThat(a.toString()).contains("success=false");
            });
            assertThat(db.getConnectionStatistics().getSuccessRate()).isZero();
            assertThat(db.isConnected()).isFalse();
        }

        // Regression: the factory went through the config-loading constructor, which initialized the
        // *configured* driver and ignored the supplied one (and read/created config/database.properties).
        @Test
        void factoryInitializesTheSuppliedDriver() {
            assertThatThrownBy(() -> DatabaseConnectionFactory.createConnection(
                    "jdbc:h2:mem:x", "sa", "", "com.example.NoSuchDriver"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database driver not found: com.example.NoSuchDriver");
        }

        // Regression: see above - creating a factory connection must not touch the global config file.
        @Test
        void factoryDoesNotCreateConfigFileInWorkingDirectory() {
            Path config = Paths.get("config", "database.properties");
            boolean existedBefore = Files.exists(config);

            h2();
            DatabaseConnectionFactory.createTestConnection().cleanup();

            assertThat(Files.exists(config)).isEqualTo(existedBefore);
        }

        // Regression: a null password made createConnection fail with a NullPointerException from
        // Properties.setProperty instead of attempting the connection.
        @Test
        void nullPasswordIsTreatedAsEmpty() throws SQLException {
            String url = "jdbc:h2:mem:nullpw_" + UUID.randomUUID().toString().replace("-", "");
            DatabaseConnection db = DatabaseConnectionFactory.createConnection(url, "sa", null, H2);
            opened.add(db);

            assertThat(db.getConnection().isValid(1)).isTrue();
        }
    }

    @Nested
    class Reporting {

        @Test
        void statusReflectsConnectionState() throws SQLException {
            DatabaseConnection db = h2();
            ConnectionStatus before = db.getConnectionStatus();
            assertThat(before.isConnected()).isFalse();
            assertThat(before.getTotalConnections()).isZero();
            assertThat(before.getJdbcUrl()).startsWith("jdbc:h2:mem:dbconn_");

            db.getConnection();
            ConnectionStatus after = db.getConnectionStatus();

            assertThat(after.isConnected()).isTrue();
            assertThat(after.getTotalConnections()).isEqualTo(1);
            assertThat(after.getLastConnectionTime()).isPositive();
            assertThat(after.getLastHealthCheckTime()).isPositive();
            assertThat(after.getJdbcUrl()).startsWith("jdbc:h2:mem:dbconn_");
            assertThat(after.toString()).contains("connected=true").contains("totalConnections=1");
        }

        @Test
        void statusFallsBackWhenConnectionIsClosedUnderneath() throws SQLException {
            DatabaseConnection db = h2();
            db.getConnection().close();

            ConnectionStatus status = db.getConnectionStatus();

            assertThat(status.isConnected()).isFalse();
            assertThat(status.getJdbcUrl()).startsWith("jdbc:h2:mem:dbconn_");
        }

        @Test
        void metadataDescribesDatabase() throws SQLException {
            DatabaseMetadata meta = h2().getDatabaseMetadata();

            assertThat(meta.getProductName()).isEqualTo("H2");
            assertThat(meta.getProductVersion()).isNotBlank();
            assertThat(meta.getMajorVersion()).isPositive();
            assertThat(meta.getMinorVersion()).isNotNegative();
            assertThat(meta.getDriverName()).containsIgnoringCase("H2");
            assertThat(meta.getDriverVersion()).isNotBlank();
            assertThat(meta.getJdbcMajorVersion()).isPositive();
            assertThat(meta.getJdbcMinorVersion()).isNotNegative();
            assertThat(meta.getUrl()).startsWith("jdbc:h2:mem:dbconn_");
            assertThat(meta.getUserName()).isEqualToIgnoringCase("sa");
            assertThat(meta.toString()).contains("product='H2");
        }

        @Test
        void statisticsSummarizeAttempts() throws SQLException {
            DatabaseConnection db = h2();
            db.getConnection();
            db.resetConnection();

            ConnectionStatistics stats = db.getConnectionStatistics();

            assertThat(stats.getTotalAttempts()).isEqualTo(2);
            assertThat(stats.getSuccessfulAttempts()).isEqualTo(2);
            assertThat(stats.getSuccessRate()).isEqualTo(100.0);
            assertThat(stats.getAverageDuration()).isNotNegative();
            assertThat(stats.getMaxDuration()).isGreaterThanOrEqualTo((long) stats.getAverageDuration());
            assertThat(stats.getTotalConnections()).isEqualTo(2);
            assertThat(stats.toString()).contains("attempts=2").contains("success=100.0%");
        }

        @Test
        void emptyStatistics() {
            ConnectionStatistics stats = h2().getConnectionStatistics();

            assertThat(stats.getTotalAttempts()).isZero();
            assertThat(stats.getSuccessRate()).isZero();
            assertThat(stats.getAverageDuration()).isZero();
            assertThat(stats.getMaxDuration()).isZero();
        }

        @Test
        void connectionAttemptDescribesItself() {
            Date now = new Date();
            ConnectionAttempt ok = new ConnectionAttempt(now, true, 12, null);
            ConnectionAttempt failed = new ConnectionAttempt(now, false, 3, new SQLException("boom"));

            assertThat(ok.getTimestamp()).isSameAs(now);
            assertThat(ok.getDuration()).isEqualTo(12);
            assertThat(ok.toString()).contains("duration=12ms").contains("error=none");
            assertThat(failed.toString()).contains("error=boom");
        }

        @Test
        void historyIsCappedAtOneHundredAttempts() throws SQLException {
            DatabaseConnection db = h2();
            for (int i = 0; i < 105; i++) {
                db.resetConnection();
            }

            assertThat(db.getConnectionHistory()).hasSize(100);
            assertThat(db.getConnectionStatistics().getTotalConnections()).isEqualTo(105);
            assertThat(db.getConnectionStatistics().getSuccessRate()).isCloseTo(100.0, within(1e-9));
        }
    }

    @Nested
    class Validation {

        @Test
        void validConfigurationPasses() {
            ValidationResult result = ConnectionValidator.validateConfiguration(
                "jdbc:h2:mem:validate_" + UUID.randomUUID().toString().replace("-", ""), "sa", "", H2);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
            assertThat(result.toString()).isEqualTo("Valid");
        }

        @Test
        void missingFieldsAndUnknownDriverAreReported() {
            ValidationResult result = ConnectionValidator.validateConfiguration(" ", null, "", "com.example.Nope");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactly(
                "JDBC URL is required", "Username is required", "Driver class not found: com.example.Nope");
            assertThat(result.toString()).startsWith("Invalid: JDBC URL is required");
        }

        @Test
        void unreachableDatabaseIsReported() {
            ValidationResult result = ConnectionValidator.validateConfiguration("jdbc:nosuchdb://x", "sa", "", H2);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).singleElement().asString().startsWith("Connection test failed: ");
        }

        // Regression: a null driver class made the validator throw NullPointerException instead of reporting it.
        @Test
        void missingDriverClassIsReportedNotThrown() {
            ValidationResult result = ConnectionValidator.validateConfiguration("jdbc:h2:mem:x", "sa", "", null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).containsExactly("Driver class is required");
        }
    }
}
