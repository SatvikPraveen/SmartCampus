package exceptions;

import exceptions.DatabaseException.ErrorCode;
import exceptions.DatabaseException.Severity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseExceptionTest {

    private static final Set<ErrorCode> TRANSIENT = EnumSet.of(
            ErrorCode.CONNECTION_TIMEOUT, ErrorCode.CONNECTION_POOL_EXHAUSTED, ErrorCode.DEADLOCK_DETECTED,
            ErrorCode.LOCK_TIMEOUT, ErrorCode.DISK_FULL, ErrorCode.UNKNOWN_ERROR);
    private static final Set<ErrorCode> CONNECTION = EnumSet.of(
            ErrorCode.CONNECTION_FAILED, ErrorCode.CONNECTION_TIMEOUT, ErrorCode.CONNECTION_POOL_EXHAUSTED,
            ErrorCode.DRIVER_ERROR);
    private static final Set<ErrorCode> CONSTRAINT = EnumSet.of(
            ErrorCode.CONSTRAINT_VIOLATION, ErrorCode.FOREIGN_KEY_VIOLATION, ErrorCode.UNIQUE_CONSTRAINT_VIOLATION,
            ErrorCode.NOT_NULL_VIOLATION, ErrorCode.CHECK_CONSTRAINT_VIOLATION);

    private static DatabaseException of(ErrorCode code) {
        return new DatabaseException(code, null);
    }

    private static SQLException sql(String state, int vendorCode) {
        return new SQLException("driver says no", state, vendorCode);
    }

    @Nested
    class Constructors {

        @Test
        void codesAreUnique() {
            assertThat(Stream.of(ErrorCode.values()).map(ErrorCode::getCode)).doesNotHaveDuplicates()
                    .allMatch(c -> c.startsWith("DB_"));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void nullMessageFallsBackToDescription(ErrorCode code) {
            assertThat(of(code).getMessage()).isEqualTo(code.getDescription());
        }

        @Test
        void simpleConstructorHasNoSqlDetails() {
            DatabaseException e = new DatabaseException(ErrorCode.DRIVER_ERROR, "x");

            assertThat(e.getMessage()).isEqualTo("x");
            assertThat(e.getOperation()).isNull();
            assertThat(e.getTableName()).isNull();
            assertThat(e.getQuery()).isNull();
            assertThat(e.getSqlErrorCode()).isZero();
            assertThat(e.getSqlState()).isNull();
            assertThat(e.getExecutionTimeMs()).isZero();
            assertThat(e.getTimestamp()).isNotNull();
            assertThat(e.getContext()).isEmpty();
        }

        @Test
        void causeConstructorExtractsSqlDetails() {
            SQLException cause = sql("23505", 0);
            DatabaseException e = new DatabaseException(ErrorCode.CONSTRAINT_VIOLATION, null, cause);

            assertThat(e).hasCause(cause).hasMessage("Database constraint violation");
            assertThat(e.getSqlState()).isEqualTo("23505");
            assertThat(e.getSqlErrorCode()).isZero();
        }

        @Test
        void nonSqlCauseLeavesSqlDetailsEmpty() {
            DatabaseException e = new DatabaseException(ErrorCode.DRIVER_ERROR, "m", new IllegalStateException());

            assertThat(e.getSqlState()).isNull();
            assertThat(e.getSqlErrorCode()).isZero();
        }

        @Test
        void detailedConstructorFormatsOperationAndTable() {
            DatabaseException e = new DatabaseException(ErrorCode.QUERY_EXECUTION_FAILED, "select", "users",
                    "SELECT 1", "failed", sql("42000", 1064));

            assertThat(e.getMessage()).isEqualTo("failed [Operation: select, Table: users]");
            assertThat(e.getQuery()).isEqualTo("SELECT 1");
            assertThat(e.getSqlErrorCode()).isEqualTo(1064);
            assertThat(e.getSqlState()).isEqualTo("42000");
        }

        @Test
        void detailedConstructorWithoutSqlCause() {
            DatabaseException e = new DatabaseException(ErrorCode.QUERY_EXECUTION_FAILED, null, "users",
                    null, "  ", null);

            assertThat(e.getMessage()).isEqualTo("Failed to execute database query [Table: users]");
            assertThat(e.getSqlState()).isNull();
        }

        @Test
        void timedConstructorKeepsExecutionTime() {
            DatabaseException withSql = new DatabaseException(ErrorCode.LOCK_TIMEOUT, "update", null, "UPDATE t",
                    750, null, sql("40001", 1205));
            DatabaseException withoutSql = new DatabaseException(ErrorCode.LOCK_TIMEOUT, null, null, null,
                    10, "m", null);

            assertThat(withSql.getExecutionTimeMs()).isEqualTo(750);
            assertThat(withSql.getMessage()).isEqualTo("Database lock timeout [Operation: update]");
            assertThat(withSql.getSqlErrorCode()).isEqualTo(1205);
            assertThat(withoutSql.getMessage()).isEqualTo("m");
            assertThat(withoutSql.getSqlErrorCode()).isZero();
        }
    }

    @Nested
    class BuilderAndContext {

        @Test
        void builderProducesConsistentState() {
            DatabaseException e = DatabaseException.builder(ErrorCode.BATCH_OPERATION_FAILED)
                    .operation("batchInsert")
                    .tableName("grades")
                    .query("INSERT ...")
                    .executionTime(42)
                    .message("batch failed")
                    .addContext("rows", 10)
                    .addContext(Map.of("failedRow", 7))
                    .build();

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BATCH_OPERATION_FAILED);
            assertThat(e.getMessage()).isEqualTo("batch failed [Operation: batchInsert, Table: grades]");
            assertThat(e.getExecutionTimeMs()).isEqualTo(42);
            assertThat(e.getContext()).containsOnly(Map.entry("rows", 10), Map.entry("failedRow", 7));
        }

        @Test
        void contextIsDefensivelyCopied() {
            DatabaseException e = DatabaseException.builder(ErrorCode.DRIVER_ERROR).addContext("k", "v").build();

            e.getContext().put("k", "changed");

            assertThat(e.getContext("k")).isEqualTo("v");
        }

        @Test
        void typedContextLookup() {
            DatabaseException e = DatabaseException.builder(ErrorCode.DRIVER_ERROR).addContext("n", 3).build();

            assertThat(e.getContext("n", Integer.class)).isEqualTo(3);
            assertThat(e.getContext("n", String.class)).isNull();
            assertThat(e.getContext("absent", Integer.class)).isNull();
        }
    }

    @Nested
    class FactoryMethods {

        @Test
        void connectionFailed() {
            SQLException cause = sql("08001", 0);
            DatabaseException e = DatabaseException.connectionFailed("db.local", 5432, "campus", cause);

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONNECTION_FAILED);
            assertThat(e.getMessage()).isEqualTo("Failed to connect to database campus at db.local:5432");
            assertThat(e.getContext()).containsEntry("host", "db.local").containsEntry("port", 5432)
                    .containsEntry("database", "campus");
            assertThat(e.getSqlState()).isEqualTo("08001");
        }

        @Test
        void connectionTimeout() {
            DatabaseException e = DatabaseException.connectionTimeout(3000);

            assertThat(e.getMessage()).isEqualTo("Database connection timed out after 3000ms");
            assertThat(e.getContext("timeoutMs", Long.class)).isEqualTo(3000L);
        }

        @Test
        void queryExecutionFailedVariants() {
            DatabaseException plain = DatabaseException.queryExecutionFailed("SELECT x", null);
            DatabaseException timed = DatabaseException.queryExecutionFailed("SELECT x", 99, null);

            assertThat(plain.getQuery()).isEqualTo("SELECT x");
            assertThat(plain.getMessage()).isEqualTo("Failed to execute query");
            assertThat(timed.getExecutionTimeMs()).isEqualTo(99);
            assertThat(timed.getMessage()).isEqualTo("Failed to execute query after 99ms");
        }

        @Test
        void transactionAndConstraintFactories() {
            assertThat(DatabaseException.transactionFailed("commit", null).getMessage())
                    .isEqualTo("Transaction failed during commit [Operation: commit]");

            DatabaseException cv = DatabaseException.constraintViolation("users", "chk_age", "insert", null);
            assertThat(cv.getMessage()).isEqualTo("Constraint violation on table users: chk_age [Operation: insert, Table: users]");
            assertThat(cv.getContext()).containsEntry("constraintName", "chk_age");

            DatabaseException fk = DatabaseException.foreignKeyViolation("enroll", "fk_student", "students", null);
            assertThat(fk.getErrorCode()).isEqualTo(ErrorCode.FOREIGN_KEY_VIOLATION);
            assertThat(fk.getMessage()).startsWith("Foreign key constraint violation: fk_student references students");

            DatabaseException uq = DatabaseException.uniqueConstraintViolation("users", "email", "a@b.c", null);
            assertThat(uq.getMessage()).startsWith("Unique constraint violation on users.email: a@b.c");
            assertThat(uq.getContext()).containsEntry("duplicateValue", "a@b.c");
        }

        @Test
        void otherFactories() {
            assertThat(DatabaseException.deadlockDetected("update", "t", null).getMessage())
                    .isEqualTo("Deadlock detected during update on table t [Operation: update, Table: t]");
            assertThat(DatabaseException.tableNotFound("ghost", "select").getMessage())
                    .startsWith("Table 'ghost' not found during select");

            DatabaseException pd = DatabaseException.permissionDenied("drop", "users", "bob");
            assertThat(pd.getMessage()).startsWith("Permission denied for user 'bob' to drop on users");
            assertThat(pd.getContext()).containsEntry("resource", "users").containsEntry("user", "bob");

            DatabaseException mig = DatabaseException.migrationFailed("V2", "add column", null);
            assertThat(mig.getOperation()).isEqualTo("migration");
            assertThat(mig.getMessage()).startsWith("Migration failed: V2 - add column");
            assertThat(mig.getSeverity()).isEqualTo(Severity.CRITICAL);
        }
    }

    @Nested
    class FromSqlException {

        @ParameterizedTest(name = "SQLState {0} -> {1}")
        @CsvSource({
                "08006, CONNECTION_FAILED", "23505, CONSTRAINT_VIOLATION", "42601, INVALID_SQL_SYNTAX",
                "53100, DISK_FULL", "57014, PERMISSION_DENIED", "40P01, DEADLOCK_DETECTED", "XX000, UNKNOWN_ERROR"
        })
        void mapsStandardSqlStateClasses(String state, ErrorCode expected) {
            DatabaseException e = DatabaseException.fromSqlException(sql(state, 0), "op");

            assertThat(e.getErrorCode()).isEqualTo(expected);
            assertThat(e.getOperation()).isEqualTo("op");
            assertThat(e.getSqlState()).isEqualTo(state);
            assertThat(e.getMessage()).isEqualTo("driver says no [Operation: op]");
        }

        @ParameterizedTest(name = "vendor {0} -> {1}")
        @CsvSource({
                "1044, PERMISSION_DENIED", "1045, PERMISSION_DENIED", "1062, UNIQUE_CONSTRAINT_VIOLATION",
                "1146, TABLE_NOT_FOUND", "1054, COLUMN_NOT_FOUND", "1452, FOREIGN_KEY_VIOLATION",
                "1213, DEADLOCK_DETECTED", "1205, LOCK_TIMEOUT", "9999, UNKNOWN_ERROR"
        })
        void mapsMySqlVendorCodesWithoutSqlState(int vendorCode, ErrorCode expected) {
            assertThat(DatabaseException.fromSqlException(sql(null, vendorCode), "op").getErrorCode())
                    .isEqualTo(expected);
        }

        // Regression: MySQL reports SQLState 23000/42S02/42S22 alongside these vendor codes; the
        // SQLState class used to win, making the specific vendor mappings unreachable.
        @ParameterizedTest(name = "vendor {1} with SQLState {0} -> {2}")
        @CsvSource({
                "23000, 1062, UNIQUE_CONSTRAINT_VIOLATION", "23000, 1452, FOREIGN_KEY_VIOLATION",
                "42S02, 1146, TABLE_NOT_FOUND", "42S22, 1054, COLUMN_NOT_FOUND", "28000, 1045, PERMISSION_DENIED"
        })
        void realMySqlErrorsUseTheMoreSpecificVendorCode(String state, int vendorCode, ErrorCode expected) {
            assertThat(DatabaseException.fromSqlException(sql(state, vendorCode), "op").getErrorCode())
                    .isEqualTo(expected);
        }

        // Regression: SQLStates shorter than two characters threw StringIndexOutOfBoundsException.
        @ParameterizedTest
        @ValueSource(strings = {"", "0"})
        void shortSqlStateDoesNotThrow(String state) {
            DatabaseException e = DatabaseException.fromSqlException(sql(state, 0), "op");

            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNKNOWN_ERROR);
        }

        @Test
        void nullSqlMessageFallsBackToDescription() {
            DatabaseException e = DatabaseException.fromSqlException(new SQLException(null, "08001"), null);

            assertThat(e.getMessage()).isEqualTo("Failed to establish database connection");
        }
    }

    @Nested
    class Classification {

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void transientCodes(ErrorCode code) {
            assertThat(of(code).isTransient()).isEqualTo(TRANSIENT.contains(code));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void connectionCodes(ErrorCode code) {
            assertThat(of(code).isConnectionIssue()).isEqualTo(CONNECTION.contains(code));
        }

        @ParameterizedTest
        @EnumSource(ErrorCode.class)
        void constraintCodes(ErrorCode code) {
            assertThat(of(code).isConstraintViolation()).isEqualTo(CONSTRAINT.contains(code));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "CORRUPTION_DETECTED, CRITICAL", "BACKUP_FAILED, CRITICAL", "RESTORE_FAILED, CRITICAL",
                "MIGRATION_FAILED, CRITICAL", "CONNECTION_FAILED, HIGH", "TRANSACTION_FAILED, HIGH",
                "ROLLBACK_FAILED, HIGH", "COMMIT_FAILED, HIGH", "REPLICATION_ERROR, HIGH",
                "DEADLOCK_DETECTED, MEDIUM", "LOCK_TIMEOUT, MEDIUM", "PERMISSION_DENIED, MEDIUM",
                "CONSTRAINT_VIOLATION, MEDIUM", "QUERY_EXECUTION_FAILED, LOW", "INVALID_SQL_SYNTAX, LOW",
                "TABLE_NOT_FOUND, LOW", "DRIVER_ERROR, MEDIUM"
        })
        void severity(ErrorCode code, Severity expected) {
            assertThat(of(code).getSeverity()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "CONNECTION_FAILED | Check database connectivity",
                "CONNECTION_TIMEOUT | Check database connectivity",
                "CONNECTION_POOL_EXHAUSTED | Wait for connections",
                "DEADLOCK_DETECTED | Retry the transaction",
                "LOCK_TIMEOUT | Retry the operation",
                "CONSTRAINT_VIOLATION | Check data integrity",
                "FOREIGN_KEY_VIOLATION | Check data integrity",
                "UNIQUE_CONSTRAINT_VIOLATION | Check data integrity",
                "PERMISSION_DENIED | Contact database administrator",
                "TABLE_NOT_FOUND | Check database schema",
                "COLUMN_NOT_FOUND | Check database schema",
                "INVALID_SQL_SYNTAX | Review and correct",
                "DISK_FULL | Free up disk space",
                "DRIVER_ERROR | Contact technical support"
        })
        void recoverySuggestion(ErrorCode code, String prefix) {
            assertThat(of(code).getRecoverySuggestion()).startsWith(prefix);
        }
    }

    @Nested
    class ToString {

        @Test
        void includesAllPopulatedDetails() {
            DatabaseException e = DatabaseException.builder(ErrorCode.LOCK_TIMEOUT)
                    .operation("update").tableName("t").executionTime(12)
                    .addContext("k", "v").message("m").cause(sql("40001", 1205)).build();

            assertThat(e.toString()).startsWith("DatabaseException{errorCode=LOCK_TIMEOUT")
                    .contains("operation='update'", "tableName='t'", "executionTimeMs=12", "sqlErrorCode=1205",
                            "sqlState='40001'", "severity=MEDIUM", "context={k=v}",
                            "cause=SQLException: driver says no")
                    .endsWith("}");
        }

        @Test
        void omitsAbsentDetails() {
            assertThat(of(ErrorCode.DRIVER_ERROR).toString())
                    .doesNotContain("operation=", "tableName=", "executionTimeMs=", "sqlErrorCode=", "sqlState=",
                            "context=", "cause=");
        }
    }
}
