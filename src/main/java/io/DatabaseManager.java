// File location: src/main/java/io/DatabaseManager.java

package io;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Manages database connections and operations using JDBC
 * Provides connection pooling, transaction management, and query execution
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private static final ReentrantLock instanceLock = new ReentrantLock();
    
    private DataSource dataSource;
    private final Map<String, PreparedStatement> statementCache = new ConcurrentHashMap<>();
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    
    // Database configuration
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;
    private int maxPoolSize;
    private int connectionTimeout;
    
    private DatabaseManager() {
        loadConfiguration();
        initializeDataSource();
    }
    
    /**
     * Get singleton instance
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instanceLock.lock();
            try {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            } finally {
                instanceLock.unlock();
            }
        }
        return instance;
    }
    
    /**
     * Load database configuration
     */
    private void loadConfiguration() {
        this.jdbcUrl = ConfigManager.DatabaseConfig.getConnectionUrl();
        this.username = ConfigManager.DatabaseConfig.getUsername();
        this.password = ConfigManager.DatabaseConfig.getPassword();
        this.driverClassName = ConfigManager.DatabaseConfig.getDriverClass();
        this.maxPoolSize = ConfigManager.DatabaseConfig.getMaxConnections();
        this.connectionTimeout = ConfigManager.DatabaseConfig.getConnectionTimeout();
    }
    
    /**
     * Initialize connection pool
     */
    private void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setAutoCommit(ConfigManager.DatabaseConfig.isAutoCommit());
        
        // Connection pool settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        this.dataSource = new HikariDataSource(config);
    }
    
    /**
     * Get database connection
     */
    public Connection getConnection() throws SQLException {
        Connection transConn = transactionConnection.get();
        if (transConn != null && !transConn.isClosed()) {
            return transConn;
        }
        return dataSource.getConnection();
    }
    
    /**
     * Execute query and return ResultSet
     */
    public ResultSet executeQuery(String sql, Object... parameters) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        
        // Set parameters
        for (int i = 0; i < parameters.length; i++) {
            stmt.setObject(i + 1, parameters[i]);
        }
        
        return stmt.executeQuery();
    }
    
    /**
     * Execute update and return affected rows
     */
    public int executeUpdate(String sql, Object... parameters) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rowMapper.mapRow(rs));
                }
            }
        }
        
        return results;
    }
    
    /**
     * Execute query and return count
     */
    public long queryForCount(String sql, Object... parameters) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }
    
    /**
     * Begin transaction
     */
    public void beginTransaction() throws SQLException {
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        transactionConnection.set(conn);
    }
    
    /**
     * Commit transaction
     */
    public void commitTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try {
                conn.commit();
            } finally {
                conn.setAutoCommit(true);
                conn.close();
                transactionConnection.remove();
            }
        }
    }
    
    /**
     * Rollback transaction
     */
    public void rollbackTransaction() throws SQLException {
        Connection conn = transactionConnection.get();
        if (conn != null) {
            try {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
                conn.close();
                transactionConnection.remove();
            }
        }
    }
    
    /**
     * Execute within transaction
     */
    public <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException {
        beginTransaction();
        try {
            T result = callback.execute();
            commitTransaction();
            return result;
        } catch (Exception e) {
            rollbackTransaction();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Transaction failed", e);
            }
        }
    }
    
    /**
     * Initialize database schema
     */
    public void initializeSchema() throws SQLException {
        String[] schemaSql = {
            // Departments table
            """
            CREATE TABLE IF NOT EXISTS departments (
                department_code VARCHAR(10) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                head_of_department VARCHAR(100),
                location VARCHAR(200),
                established_year INT,
                student_count INT DEFAULT 0
            )
            """,
            
            // Students table
            """
            CREATE TABLE IF NOT EXISTS students (
                id VARCHAR(20) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                department_code VARCHAR(10),
                enrollment_date DATE,
                FOREIGN KEY (department_code) REFERENCES departments(department_code)
            )
            """,
            
            // Professors table
            """
            CREATE TABLE IF NOT EXISTS professors (
                id VARCHAR(20) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                department_code VARCHAR(10),
                specialization VARCHAR(200),
                office_location VARCHAR(100),
                years_of_experience INT,
                FOREIGN KEY (department_code) REFERENCES departments(department_code)
            )
            """,
            
            // Courses table
            """
            CREATE TABLE IF NOT EXISTS courses (
                course_code VARCHAR(10) PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description TEXT,
                credits INT NOT NULL,
                department_code VARCHAR(10),
                professor_id VARCHAR(20),
                semester VARCHAR(20),
                academic_year VARCHAR(10),
                capacity INT DEFAULT 0,
                enrolled_students INT DEFAULT 0,
                FOREIGN KEY (department_code) REFERENCES departments(department_code),
                FOREIGN KEY (professor_id) REFERENCES professors(id)
            )
            """,
            
            // Enrollments table
            """
            CREATE TABLE IF NOT EXISTS enrollments (
                enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                student_id VARCHAR(20),
                course_code VARCHAR(10),
                enrollment_date DATE,
                semester VARCHAR(20),
                academic_year VARCHAR(10),
                FOREIGN KEY (student_id) REFERENCES students(id),
                FOREIGN KEY (course_code) REFERENCES courses(course_code)
            )
            """,
            
            // Grades table
            """
            CREATE TABLE IF NOT EXISTS grades (
                grade_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                student_id VARCHAR(20),
                course_code VARCHAR(10),
                numeric_grade DECIMAL(5,2),
                letter_grade VARCHAR(2),
                semester VARCHAR(20),
                academic_year VARCHAR(10),
                graded_date DATE,
                comments TEXT,
                FOREIGN KEY (student_id) REFERENCES students(id),
                FOREIGN KEY (course_code) REFERENCES courses(course_code)
            )
            """
        };
        
        for (String sql : schemaSql) {
            executeUpdate(sql);
        }
        
        // Create indexes for better performance
        createIndexes();
    }
    
    /**
     * Create database indexes
     */
    private void createIndexes() throws SQLException {
        String[] indexSql = {
            "CREATE INDEX IF NOT EXISTS idx_students_department ON students(department_code)",
            "CREATE INDEX IF NOT EXISTS idx_students_email ON students(email)",
            "CREATE INDEX IF NOT EXISTS idx_professors_department ON professors(department_code)",
            "CREATE INDEX IF NOT EXISTS idx_professors_email ON professors(email)",
            "CREATE INDEX IF NOT EXISTS idx_courses_department ON courses(department_code)",
            "CREATE INDEX IF NOT EXISTS idx_courses_professor ON courses(professor_id)",
            "CREATE INDEX IF NOT EXISTS idx_courses_semester ON courses(semester, academic_year)",
            "CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id)",
            "CREATE INDEX IF NOT EXISTS idx_enrollments_course ON enrollments(course_code)",
            "CREATE INDEX IF NOT EXISTS idx_enrollments_semester ON enrollments(semester, academic_year)",
            "CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id)",
            "CREATE INDEX IF NOT EXISTS idx_grades_course ON grades(course_code)",
            "CREATE INDEX IF NOT EXISTS idx_grades_semester ON grades(semester, academic_year)"
        };
        
        for (String sql : indexSql) {
            try {
                executeUpdate(sql);
            } catch (SQLException e) {
                // Index might already exist, continue
                System.out.println("Index creation warning: " + e.getMessage());
            }
        }
    }
    
    /**
     * Backup database to file
     */
    public void backupDatabase(String backupFilePath) throws SQLException {
        String backupSql = "SCRIPT TO '" + backupFilePath + "'";
        executeUpdate(backupSql);
    }
    
    /**
     * Restore database from file
     */
    public void restoreDatabase(String backupFilePath) throws SQLException {
        String restoreSql = "RUNSCRIPT FROM '" + backupFilePath + "'";
        executeUpdate(restoreSql);
    }
    
    /**
     * Get database metadata
     */
    public DatabaseMetadata getDatabaseMetadata() throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            
            Map<String, List<String>> tableColumns = new HashMap<>();
            for (String table : tables) {
                List<String> columns = new ArrayList<>();
                try (ResultSet rs = metaData.getColumns(null, null, table, "%")) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME"));
                    }
                }
                tableColumns.put(table, columns);
            }
            
            return new DatabaseMetadata(
                metaData.getDatabaseProductName(),
                metaData.getDatabaseProductVersion(),
                metaData.getDriverName(),
                metaData.getDriverVersion(),
                tables,
                tableColumns
            );
        }
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get connection pool statistics
     */
    public ConnectionPoolStats getConnectionPoolStats() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDS = (HikariDataSource) dataSource;
            return new ConnectionPoolStats(
                hikariDS.getHikariPoolMXBean().getTotalConnections(),
                hikariDS.getHikariPoolMXBean().getActiveConnections(),
                hikariDS.getHikariPoolMXBean().getIdleConnections(),
                hikariDS.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return new ConnectionPoolStats(0, 0, 0, 0);
    }
    
    /**
     * Close database manager and cleanup resources
     */
    public void close() {
        try {
            // Clear statement cache
            for (PreparedStatement stmt : statementCache.values()) {
                stmt.close();
            }
            statementCache.clear();
            
            // Close data source
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database manager: " + e.getMessage());
        }
    }
    
    // Utility methods for common operations
    
    /**
     * Check if table exists
     */
    public boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        }
    }
    
    /**
     * Get table row count
     */
    public long getTableRowCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        return queryForCount(sql);
    }
    
    /**
     * Truncate table
     */
    public void truncateTable(String tableName) throws SQLException {
        String sql = "TRUNCATE TABLE " + tableName;
        executeUpdate(sql);
    }
    
    /**
     * Drop table if exists
     */
    public void dropTableIfExists(String tableName) throws SQLException {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        executeUpdate(sql);
    }
    
    // Inner classes and interfaces
    
    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(ResultSet rs) throws SQLException;
    }
    
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute() throws SQLException;
    }
    
    public static class DatabaseMetadata {
        private final String productName;
        private final String productVersion;
        private final String driverName;
        private final String driverVersion;
        private final List<String> tables;
        private final Map<String, List<String>> tableColumns;
        
        public DatabaseMetadata(String productName, String productVersion,
                              String driverName, String driverVersion,
                              List<String> tables, Map<String, List<String>> tableColumns) {
            this.productName = productName;
            this.productVersion = productVersion;
            this.driverName = driverName;
            this.driverVersion = driverVersion;
            this.tables = tables;
            this.tableColumns = tableColumns;
        }
        
        // Getters
        public String getProductName() { return productName; }
        public String getProductVersion() { return productVersion; }
        public String getDriverName() { return driverName; }
        public String getDriverVersion() { return driverVersion; }
        public List<String> getTables() { return tables; }
        public Map<String, List<String>> getTableColumns() { return tableColumns; }
        
        @Override
        public String toString() {
            return String.format("DatabaseMetadata{product='%s %s', driver='%s %s', tables=%d}",
                               productName, productVersion, driverName, driverVersion, tables.size());
        }
    }
    
    public static class ConnectionPoolStats {
        private final int totalConnections;
        private final int activeConnections;
        private final int idleConnections;
        private final int threadsAwaitingConnection;
        
        public ConnectionPoolStats(int totalConnections, int activeConnections,
                                 int idleConnections, int threadsAwaitingConnection) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
        }
        
        // Getters
        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        
        @Override
        public String toString() {
            return String.format("ConnectionPoolStats{total=%d, active=%d, idle=%d, waiting=%d}",
                               totalConnections, activeConnections, idleConnections, threadsAwaitingConnection);
        }
    }
}i + 1, parameters[i]);
            }
            
            return stmt.executeUpdate();
        }
    }
    
    /**
     * Execute insert and return generated key
     */
    public long executeInsert(String sql, Object... parameters) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Insert failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Execute batch operations
     */
    public int[] executeBatch(String sql, List<Object[]> parametersList) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (Object[] parameters : parametersList) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setObject(i + 1, parameters[i]);
                }
                stmt.addBatch();
            }
            
            return stmt.executeBatch();
        }
    }
    
    /**
     * Execute query and return single result
     */
    public <T> Optional<T> queryForObject(String sql, RowMapper<T> rowMapper, Object... parameters) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowMapper.mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }
    
    /**
     * Execute query and return list of results
     */
    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... parameters) throws SQLException {
        List<T> results = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(