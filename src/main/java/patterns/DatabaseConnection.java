// File location: src/main/java/patterns/DatabaseConnection.java

package patterns;

import io.ConfigManager;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton pattern implementation for database connection management
 * Provides thread-safe database connection handling with connection pooling
 */
public class DatabaseConnection {
    
    // Singleton instance with thread-safe lazy initialization
    private static volatile DatabaseConnection instance;
    private static final Object lock = new Object();
    
    // Connection management
    private Connection connection;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong lastConnectionTime = new AtomicLong(0);
    
    // Configuration properties
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;
    private int connectionTimeout;
    private boolean autoCommit;
    
    // Connection health monitoring
    private final AtomicLong lastHealthCheck = new AtomicLong(0);
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
    
    // Connection history for debugging
    private final Queue<ConnectionAttempt> connectionHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private DatabaseConnection() {
        loadConfiguration();
        initializeDriver();
    }
    
    /**
     * Get singleton instance using double-checked locking pattern
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
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
        this.connectionTimeout = ConfigManager.DatabaseConfig.getConnectionTimeout();
        this.autoCommit = ConfigManager.DatabaseConfig.isAutoCommit();
    }
    
    /**
     * Initialize database driver
     */
    private void initializeDriver() {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not found: " + driverClassName, e);
        }
    }
    
    /**
     * Get database connection with automatic health checking
     */
    public synchronized Connection getConnection() throws SQLException {
        // Check if we need to perform health check
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheck.get() > HEALTH_CHECK_INTERVAL) {
            performHealthCheck();
            lastHealthCheck.set(currentTime);
        }
        
        // Create new connection if needed
        if (!isConnected.get() || connection == null || connection.isClosed()) {
            createConnection();
        }
        
        return connection;
    }
    
    /**
     * Create new database connection
     */
    private void createConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        Exception connectionError = null;
        
        try {
            // Close existing connection if any
            closeConnection();
            
            // Create new connection with timeout
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("connectTimeout", String.valueOf(connectionTimeout));
            props.setProperty("socketTimeout", String.valueOf(connectionTimeout));
            
            connection = DriverManager.getConnection(jdbcUrl, props);
            connection.setAutoCommit(autoCommit);
            
            // Verify connection
            if (connection.isValid(5)) {
                isConnected.set(true);
                connectionCount.incrementAndGet();
                lastConnectionTime.set(System.currentTimeMillis());
                
                // Record successful connection
                recordConnectionAttempt(true, System.currentTimeMillis() - startTime, null);
                
            } else {
                throw new SQLException("Connection validation failed");
            }
            
        } catch (SQLException e) {
            connectionError = e;
            isConnected.set(false);
            recordConnectionAttempt(false, System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }
    
    /**
     * Perform connection health check
     */
    private void performHealthCheck() {
        try {
            if (connection != null && !connection.isClosed()) {
                if (!connection.isValid(5)) {
                    isConnected.set(false);
                    closeConnection();
                }
            } else {
                isConnected.set(false);
            }
        } catch (SQLException e) {
            isConnected.set(false);
            try {
                closeConnection();
            } catch (SQLException ignored) {
                // Ignore errors during cleanup
            }
        }
    }
    
    /**
     * Close the database connection
     */
    public synchronized void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        isConnected.set(false);
        connection = null;
    }
    
    /**
     * Test database connectivity
     */
    public boolean testConnection() {
        try (Connection testConn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return testConn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Execute a simple query to test connection
     */
    public boolean testConnectionWithQuery() {
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get connection status information
     */
    public ConnectionStatus getConnectionStatus() {
        try {
            boolean connected = isConnected.get() && connection != null && !connection.isClosed();
            return new ConnectionStatus(
                connected,
                connectionCount.get(),
                lastConnectionTime.get(),
                lastHealthCheck.get(),
                connection != null ? connection.getMetaData().getURL() : jdbcUrl
            );
        } catch (SQLException e) {
            return new ConnectionStatus(false, connectionCount.get(), 
                                      lastConnectionTime.get(), lastHealthCheck.get(), jdbcUrl);
        }
    }
    
    /**
     * Get database metadata
     */
    public DatabaseMetadata getDatabaseMetadata() throws SQLException {
        Connection conn = getConnection();
        DatabaseMetaData metaData = conn.getMetaData();
        
        return new DatabaseMetadata(
            metaData.getDatabaseProductName(),
            metaData.getDatabaseProductVersion(),
            metaData.getDatabaseMajorVersion(),
            metaData.getDatabaseMinorVersion(),
            metaData.getDriverName(),
            metaData.getDriverVersion(),
            metaData.getJDBCMajorVersion(),
            metaData.getJDBCMinorVersion(),
            metaData.getURL(),
            metaData.getUserName()
        );
    }
    
    /**
     * Reset connection (force reconnection)
     */
    public synchronized void resetConnection() throws SQLException {
        closeConnection();
        createConnection();
    }
    
    /**
     * Update configuration and reconnect
     */
    public synchronized void updateConfiguration() throws SQLException {
        closeConnection();
        loadConfiguration();
        initializeDriver();
        createConnection();
    }
    
    /**
     * Record connection attempt for debugging
     */
    private void recordConnectionAttempt(boolean successful, long duration, Exception error) {
        ConnectionAttempt attempt = new ConnectionAttempt(
            new Date(), successful, duration, error);
        
        synchronized (connectionHistory) {
            connectionHistory.offer(attempt);
            while (connectionHistory.size() > MAX_HISTORY_SIZE) {
                connectionHistory.poll();
            }
        }
    }
    
    /**
     * Get connection attempt history
     */
    public List<ConnectionAttempt> getConnectionHistory() {
        synchronized (connectionHistory) {
            return new ArrayList<>(connectionHistory);
        }
    }
    
    /**
     * Get connection statistics
     */
    public ConnectionStatistics getConnectionStatistics() {
        List<ConnectionAttempt> history = getConnectionHistory();
        
        long totalAttempts = history.size();
        long successfulAttempts = history.stream()
            .mapToLong(attempt -> attempt.isSuccessful() ? 1 : 0)
            .sum();
        
        double averageDuration = history.stream()
            .mapToLong(ConnectionAttempt::getDuration)
            .average()
            .orElse(0.0);
        
        long maxDuration = history.stream()
            .mapToLong(ConnectionAttempt::getDuration)
            .max()
            .orElse(0);
        
        return new ConnectionStatistics(
            totalAttempts,
            successfulAttempts,
            totalAttempts > 0 ? (double) successfulAttempts / totalAttempts * 100 : 0,
            averageDuration,
            maxDuration,
            connectionCount.get()
        );
    }
    
    /**
     * Check if connection is currently active
     */
    public boolean isConnected() {
        try {
            return isConnected.get() && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get time since last successful connection
     */
    public long getTimeSinceLastConnection() {
        long lastConn = lastConnectionTime.get();
        return lastConn > 0 ? System.currentTimeMillis() - lastConn : -1;
    }
    
    /**
     * Cleanup resources (for application shutdown)
     */
    public void cleanup() {
        try {
            closeConnection();
        } catch (SQLException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    // Inner classes for connection information
    
    public static class ConnectionStatus {
        private final boolean connected;
        private final long totalConnections;
        private final long lastConnectionTime;
        private final long lastHealthCheckTime;
        private final String jdbcUrl;
        
        public ConnectionStatus(boolean connected, long totalConnections, 
                              long lastConnectionTime, long lastHealthCheckTime, 
                              String jdbcUrl) {
            this.connected = connected;
            this.totalConnections = totalConnections;
            this.lastConnectionTime = lastConnectionTime;
            this.lastHealthCheckTime = lastHealthCheckTime;
            this.jdbcUrl = jdbcUrl;
        }
        
        // Getters
        public boolean isConnected() { return connected; }
        public long getTotalConnections() { return totalConnections; }
        public long getLastConnectionTime() { return lastConnectionTime; }
        public long getLastHealthCheckTime() { return lastHealthCheckTime; }
        public String getJdbcUrl() { return jdbcUrl; }
        
        @Override
        public String toString() {
            return String.format("ConnectionStatus{connected=%s, totalConnections=%d, url='%s'}",
                               connected, totalConnections, jdbcUrl);
        }
    }
    
    public static class DatabaseMetadata {
        private final String productName;
        private final String productVersion;
        private final int majorVersion;
        private final int minorVersion;
        private final String driverName;
        private final String driverVersion;
        private final int jdbcMajorVersion;
        private final int jdbcMinorVersion;
        private final String url;
        private final String userName;
        
        public DatabaseMetadata(String productName, String productVersion, 
                              int majorVersion, int minorVersion, String driverName, 
                              String driverVersion, int jdbcMajorVersion, 
                              int jdbcMinorVersion, String url, String userName) {
            this.productName = productName;
            this.productVersion = productVersion;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.driverName = driverName;
            this.driverVersion = driverVersion;
            this.jdbcMajorVersion = jdbcMajorVersion;
            this.jdbcMinorVersion = jdbcMinorVersion;
            this.url = url;
            this.userName = userName;
        }
        
        // Getters
        public String getProductName() { return productName; }
        public String getProductVersion() { return productVersion; }
        public int getMajorVersion() { return majorVersion; }
        public int getMinorVersion() { return minorVersion; }
        public String getDriverName() { return driverName; }
        public String getDriverVersion() { return driverVersion; }
        public int getJdbcMajorVersion() { return jdbcMajorVersion; }
        public int getJdbcMinorVersion() { return jdbcMinorVersion; }
        public String getUrl() { return url; }
        public String getUserName() { return userName; }
        
        @Override
        public String toString() {
            return String.format("DatabaseMetadata{product='%s %s', driver='%s %s', jdbc=%d.%d}",
                               productName, productVersion, driverName, driverVersion, 
                               jdbcMajorVersion, jdbcMinorVersion);
        }
    }
    
    public static class ConnectionAttempt {
        private final Date timestamp;
        private final boolean successful;
        private final long duration;
        private final Exception error;
        
        public ConnectionAttempt(Date timestamp, boolean successful, long duration, Exception error) {
            this.timestamp = timestamp;
            this.successful = successful;
            this.duration = duration;
            this.error = error;
        }
        
        // Getters
        public Date getTimestamp() { return timestamp; }
        public boolean isSuccessful() { return successful; }
        public long getDuration() { return duration; }
        public Exception getError() { return error; }
        
        @Override
        public String toString() {
            return String.format("ConnectionAttempt{time=%s, success=%s, duration=%dms, error=%s}",
                               timestamp, successful, duration, 
                               error != null ? error.getMessage() : "none");
        }
    }
    
    public static class ConnectionStatistics {
        private final long totalAttempts;
        private final long successfulAttempts;
        private final double successRate;
        private final double averageDuration;
        private final long maxDuration;
        private final long totalConnections;
        
        public ConnectionStatistics(long totalAttempts, long successfulAttempts, 
                                  double successRate, double averageDuration, 
                                  long maxDuration, long totalConnections) {
            this.totalAttempts = totalAttempts;
            this.successfulAttempts = successfulAttempts;
            this.successRate = successRate;
            this.averageDuration = averageDuration;
            this.maxDuration = maxDuration;
            this.totalConnections = totalConnections;
        }
        
        // Getters
        public long getTotalAttempts() { return totalAttempts; }
        public long getSuccessfulAttempts() { return successfulAttempts; }
        public double getSuccessRate() { return successRate; }
        public double getAverageDuration() { return averageDuration; }
        public long getMaxDuration() { return maxDuration; }
        public long getTotalConnections() { return totalConnections; }
        
        @Override
        public String toString() {
            return String.format("ConnectionStats{attempts=%d, success=%.1f%%, avgDuration=%.1fms, total=%d}",
                               totalAttempts, successRate, averageDuration, totalConnections);
        }
    }
    
    /**
     * Factory for creating different types of database connections
     */
    public static class DatabaseConnectionFactory {
        
        /**
         * Create connection for testing with in-memory database
         */
        public static DatabaseConnection createTestConnection() {
            DatabaseConnection conn = new DatabaseConnection();
            conn.jdbcUrl = "jdbc:h2:mem:testdb";
            conn.username = "sa";
            conn.password = "";
            conn.driverClassName = "org.h2.Driver";
            return conn;
        }
        
        /**
         * Create connection with custom configuration
         */
        public static DatabaseConnection createConnection(String url, String user, 
                                                        String pass, String driver) {
            DatabaseConnection conn = new DatabaseConnection();
            conn.jdbcUrl = url;
            conn.username = user;
            conn.password = pass;
            conn.driverClassName = driver;
            return conn;
        }
    }
    
    /**
     * Utility class for connection validation
     */
    public static class ConnectionValidator {
        
        /**
         * Validate connection configuration
         */
        public static ValidationResult validateConfiguration(String url, String username, 
                                                           String password, String driverClass) {
            List<String> errors = new ArrayList<>();
            
            if (url == null || url.trim().isEmpty()) {
                errors.add("JDBC URL is required");
            }
            
            if (username == null || username.trim().isEmpty()) {
                errors.add("Username is required");
            }
            
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                errors.add("Driver class not found: " + driverClass);
            }
            
            // Test connection if configuration is valid
            if (errors.isEmpty()) {
                try (Connection testConn = DriverManager.getConnection(url, username, password)) {
                    if (!testConn.isValid(5)) {
                        errors.add("Connection validation failed");
                    }
                } catch (SQLException e) {
                    errors.add("Connection test failed: " + e.getMessage());
                }
            }
            
            return new ValidationResult(errors.isEmpty(), errors);
        }
        
        public static class ValidationResult {
            private final boolean valid;
            private final List<String> errors;
            
            public ValidationResult(boolean valid, List<String> errors) {
                this.valid = valid;
                this.errors = errors;
            }
            
            public boolean isValid() { return valid; }
            public List<String> getErrors() { return errors; }
            
            @Override
            public String toString() {
                return valid ? "Valid" : "Invalid: " + String.join(", ", errors);
            }
        }
    }
}