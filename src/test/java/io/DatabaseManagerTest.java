package io;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseManagerTest {

    @TempDir
    static Path configDir;

    private static Path originalConfigDir;
    private static DatabaseManager db;

    /** Points ConfigManager at a private database.properties for an isolated in-memory H2 database. */
    static void configureIsolatedDatabase(Path directory, int maxConnections) throws IOException {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:h2:mem:io_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1");
        p.setProperty("db.username", "sa");
        p.setProperty("db.password", "");
        p.setProperty("db.driver", "org.h2.Driver");
        p.setProperty("db.max.connections", String.valueOf(maxConnections));
        p.setProperty("db.connection.timeout", "1000");
        try (var out = Files.newOutputStream(directory.resolve("database.properties"))) {
            p.store(out, null);
        }
        ConfigManager.setConfigDirectory(directory);
    }

    static void resetSingleton() throws Exception {
        Field f = DatabaseManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @BeforeAll
    static void startDatabase() throws Exception {
        originalConfigDir = ConfigManager.getConfigDirectory();
        configureIsolatedDatabase(configDir, 2);
        resetSingleton();
        db = DatabaseManager.getInstance();
    }

    @AfterAll
    static void stopDatabase() throws Exception {
        db.close();
        resetSingleton();
        ConfigManager.setConfigDirectory(originalConfigDir);
    }

    @BeforeEach
    void freshTable() throws SQLException {
        db.dropTableIfExists("items");
        db.executeUpdate("CREATE TABLE items (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50))");
    }

    private static final DatabaseManager.RowMapper<String> NAME = rs -> rs.getString("name");

    private List<String> names() throws SQLException {
        return db.queryForList("SELECT name FROM items ORDER BY id", NAME);
    }

    @Test
    void getInstanceIsASingleton() {
        assertThat(DatabaseManager.getInstance()).isSameAs(db);
    }

    // Regression: double-checked locking on a non-volatile field may publish a partially
    // constructed instance to other threads.
    @Test
    void singletonFieldIsVolatileForSafeDoubleCheckedLocking() throws Exception {
        assertThat(Modifier.isVolatile(DatabaseManager.class.getDeclaredField("instance").getModifiers())).isTrue();
    }

    @Nested
    class Queries {

        @Test
        void insertReturnsGeneratedKeysAndQueriesMapRows() throws Exception {
            long first = db.executeInsert("INSERT INTO items(name) VALUES (?)", "a");
            long second = db.executeInsert("INSERT INTO items(name) VALUES (?)", "b");

            assertThat(second).isEqualTo(first + 1);
            assertThat(names()).containsExactly("a", "b");
            assertThat(db.queryForObject("SELECT name FROM items WHERE id = ?", NAME, second)).contains("b");
            assertThat(db.queryForObject("SELECT name FROM items WHERE id = ?", NAME, -1L)).isEqualTo(Optional.empty());
            assertThat(db.queryForCount("SELECT COUNT(*) FROM items WHERE name LIKE ?", "%")).isEqualTo(2);
            assertThat(db.queryForCount("SELECT 1 FROM items WHERE 1 = 0")).isZero();
            assertThat(db.getTableRowCount("items")).isEqualTo(2);
        }

        @Test
        void insertAffectingNoRowsFails() {
            assertThatThrownBy(() -> db.executeInsert("INSERT INTO items(name) SELECT name FROM items WHERE 1 = 0"))
                    .isInstanceOf(SQLException.class).hasMessageContaining("no rows affected");
        }

        @Test
        void updateReturnsAffectedRowsAndBatchAppliesAll() throws Exception {
            int[] counts = db.executeBatch("INSERT INTO items(name) VALUES (?)",
                    List.of(new Object[]{"x"}, new Object[]{"y"}, new Object[]{"z"}));

            assertThat(counts).containsExactly(1, 1, 1);
            assertThat(db.executeUpdate("UPDATE items SET name = ? WHERE name <> ?", "q", "x")).isEqualTo(2);
            assertThat(names()).containsExactly("x", "q", "q");
        }

        @Test
        void executeQueryReturnsLiveResultSet() throws Exception {
            db.executeUpdate("INSERT INTO items(name) VALUES ('a'), ('b')");

            try (ResultSet rs = db.executeQuery("SELECT name FROM items WHERE name = ?", "b")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("b");
                assertThat(rs.next()).isFalse();
            }
        }

        // Regression: executeQuery never released its statement or pooled connection, so the pool
        // (2 connections here) was exhausted after two calls even when callers closed the ResultSet.
        @Test
        void closingQueryResultSetReleasesPooledConnection() throws Exception {
            for (int i = 0; i < 5; i++) {
                try (ResultSet rs = db.executeQuery("SELECT COUNT(*) FROM items")) {
                    assertThat(rs.next()).isTrue();
                }
            }
            assertThat(db.getConnectionPoolStats().getActiveConnections()).isZero();
        }

        @Test
        void failingQueryReleasesConnection() {
            assertThatThrownBy(() -> db.executeQuery("SELECT * FROM no_such_table")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> db.executeQuery("SELECT * FROM no_such_table")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> db.executeQuery("SELECT * FROM no_such_table")).isInstanceOf(SQLException.class);
            assertThat(db.getConnectionPoolStats().getActiveConnections()).isZero();
        }

        @Test
        void truncateEmptiesTable() throws Exception {
            db.executeUpdate("INSERT INTO items(name) VALUES ('a')");
            db.truncateTable("items");
            assertThat(db.getTableRowCount("items")).isZero();
        }
    }

    @Nested
    class Transactions {

        // Regression: statements run inside a transaction closed (and thereby rolled back and
        // released) the transaction's connection, so commit failed and nothing was atomic.
        @Test
        void committedTransactionPersistsAllStatements() throws Exception {
            Long id = db.executeInTransaction(() -> {
                long generated = db.executeInsert("INSERT INTO items(name) VALUES (?)", "a");
                db.executeUpdate("INSERT INTO items(name) VALUES (?)", "b");
                db.executeBatch("INSERT INTO items(name) VALUES (?)", List.<Object[]>of(new Object[]{"c"}));
                assertThat(db.queryForCount("SELECT COUNT(*) FROM items")).isEqualTo(3);
                return generated;
            });

            assertThat(id).isPositive();
            assertThat(names()).containsExactly("a", "b", "c");
            assertThat(db.getConnectionPoolStats().getActiveConnections()).isZero();
        }

        @Test
        void failingCallbackRollsBackEverything() throws Exception {
            assertThatThrownBy(() -> db.executeInTransaction(() -> {
                db.executeUpdate("INSERT INTO items(name) VALUES (?)", "a");
                db.executeUpdate("INSERT INTO items(name) VALUES (?)", "b");
                throw new IllegalStateException("boom");
            })).isInstanceOf(SQLException.class).hasMessage("Transaction failed")
                    .hasCauseInstanceOf(IllegalStateException.class);

            assertThat(names()).isEmpty();
            assertThat(db.getConnectionPoolStats().getActiveConnections()).isZero();
        }

        @Test
        void sqlExceptionFromCallbackIsRethrownAsIs() throws Exception {
            SQLException original = new SQLException("bad");
            assertThatThrownBy(() -> db.executeInTransaction(() -> {
                db.executeUpdate("INSERT INTO items(name) VALUES ('a')");
                throw original;
            })).isSameAs(original);
            assertThat(names()).isEmpty();
        }

        @Test
        void manualTransactionSharesConnectionAndQueriesSeeUncommittedRows() throws Exception {
            db.beginTransaction();
            try {
                db.executeUpdate("INSERT INTO items(name) VALUES ('t')");
                try (ResultSet rs = db.executeQuery("SELECT COUNT(*) FROM items")) {
                    rs.next();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
                Connection c1 = db.getConnection();
                c1.close(); // must not end the transaction
                assertThat(c1.isClosed()).isFalse();
            } finally {
                db.rollbackTransaction();
            }
            assertThat(names()).isEmpty();

            // no transaction active: both are no-ops
            db.commitTransaction();
            db.rollbackTransaction();
        }
    }

    @Nested
    class SchemaAndMaintenance {

        @Test
        void initializeSchemaIsIdempotentAndVisibleInMetadata() throws Exception {
            db.initializeSchema();
            db.initializeSchema();

            assertThat(db.tableExists("students")).isTrue();
            assertThat(db.tableExists("grades")).isTrue();
            assertThat(db.tableExists("nothing_here")).isFalse();

            DatabaseManager.DatabaseMetadata meta = db.getDatabaseMetadata();
            assertThat(meta.getProductName()).isEqualTo("H2");
            assertThat(meta.getProductVersion()).isNotBlank();
            assertThat(meta.getDriverName()).isNotBlank();
            assertThat(meta.getDriverVersion()).isNotBlank();
            assertThat(meta.getTables()).contains("STUDENTS", "COURSES", "ENROLLMENTS", "ITEMS");
            assertThat(meta.getTableColumns().get("ITEMS")).containsExactly("ID", "NAME");
            assertThat(meta.toString()).startsWith("DatabaseMetadata{product='H2");
        }

        // Regression: SCRIPT TO was run through executeUpdate, which H2 rejects for statements that
        // produce a result set, so every database backup failed.
        @Test
        void backupAndRestoreRoundTrip() throws Exception {
            db.executeUpdate("INSERT INTO items(name) VALUES ('keep')");
            Path script = configDir.resolve("backup-" + UUID.randomUUID() + ".sql");

            db.backupDatabase(script.toString());
            db.executeUpdate("DROP ALL OBJECTS");
            assertThat(db.tableExists("items")).isFalse();
            db.restoreDatabase(script.toString());

            assertThat(script).isNotEmptyFile();
            assertThat(names()).containsExactly("keep");
        }

        @Test
        void connectionChecksAndPoolStats() {
            assertThat(db.testConnection()).isTrue();
            DatabaseManager.ConnectionPoolStats stats = db.getConnectionPoolStats();
            assertThat(stats.getTotalConnections()).isBetween(0, 2);
            assertThat(stats.getIdleConnections()).isBetween(0, 2);
            assertThat(stats.getThreadsAwaitingConnection()).isZero();
            assertThat(stats.toString()).startsWith("ConnectionPoolStats{total=");
        }

        @Test
        void closedManagerReportsFailedConnection() throws Exception {
            Constructor<DatabaseManager> ctor = DatabaseManager.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            DatabaseManager other = ctor.newInstance();
            assertThat(other.testConnection()).isTrue();

            other.close();

            assertThat(other.testConnection()).isFalse();
            assertThatThrownBy(other::getConnection).isInstanceOf(SQLException.class);
        }
    }
}
