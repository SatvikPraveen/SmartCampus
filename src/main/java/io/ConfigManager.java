// File location: src/main/java/io/ConfigManager.java

package io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages application configuration using Properties files
 * Provides centralized configuration management for the SmartCampus system
 */
public class ConfigManager {
    
    private static final String DEFAULT_CONFIG_DIR = "config";
    private static final String APPLICATION_CONFIG = "application.properties";
    private static final String DATABASE_CONFIG = "database.properties";
    private static final String LOGGING_CONFIG = "logging.properties";
    
    private static final Map<String, Properties> configCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastModified = new ConcurrentHashMap<>();
    
    // Configuration change listeners
    private static final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();
    
    /**
     * Load application configuration
     */
    public static Properties loadApplicationConfig() throws IOException {
        return loadConfig(APPLICATION_CONFIG);
    }
    
    /**
     * Load database configuration
     */
    public static Properties loadDatabaseConfig() throws IOException {
        return loadConfig(DATABASE_CONFIG);
    }
    
    /**
     * Load logging configuration
     */
    public static Properties loadLoggingConfig() throws IOException {
        return loadConfig(LOGGING_CONFIG);
    }
    
    /**
     * Load configuration from specified file
     */
    public static Properties loadConfig(String configFileName) throws IOException {
        Path configPath = Paths.get(DEFAULT_CONFIG_DIR, configFileName);
        return loadConfig(configPath);
    }
    
    /**
     * Load configuration from specified path
     */
    public static Properties loadConfig(Path configPath) throws IOException {
        String key = configPath.toString();
        
        // Check if file exists
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        
        // Check if we need to reload (file has been modified)
        long currentModified = Files.getLastModifiedTime(configPath).toMillis();
        Long cachedModified = lastModified.get(key);
        
        if (cachedModified == null || currentModified > cachedModified) {
            Properties properties = new Properties();
            
            try (InputStream input = Files.newInputStream(configPath)) {
                properties.load(input);
            }
            
            configCache.put(key, properties);
            lastModified.put(key, currentModified);
            
            // Notify listeners of config change
            notifyConfigChange(key, properties);
        }
        
        return new Properties(configCache.get(key)); // Return copy to prevent modification
    }
    
    /**
     * Save configuration to file
     */
    public static void saveConfig(Properties properties, Path configPath) throws IOException {
        FileUtil.createDirectoriesIfNotExists(configPath.getParent());
        
        try (OutputStream output = Files.newOutputStream(configPath)) {
            properties.store(output, "SmartCampus Configuration - Updated: " + new Date());
        }
        
        // Update cache
        String key = configPath.toString();
        configCache.put(key, new Properties(properties));
        lastModified.put(key, Files.getLastModifiedTime(configPath).toMillis());
        
        // Notify listeners
        notifyConfigChange(key, properties);
    }
    
    /**
     * Get configuration value with default
     */
    public static String getConfigValue(String configFileName, String key, String defaultValue) {
        try {
            Properties properties = loadConfig(configFileName);
            return properties.getProperty(key, defaultValue);
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Get configuration value as integer
     */
    public static int getConfigValueAsInt(String configFileName, String key, int defaultValue) {
        String value = getConfigValue(configFileName, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Get configuration value as boolean
     */
    public static boolean getConfigValueAsBoolean(String configFileName, String key, boolean defaultValue) {
        String value = getConfigValue(configFileName, key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Get configuration value as double
     */
    public static double getConfigValueAsDouble(String configFileName, String key, double defaultValue) {
        String value = getConfigValue(configFileName, key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Set configuration value
     */
    public static void setConfigValue(String configFileName, String key, String value) throws IOException {
        Path configPath = Paths.get(DEFAULT_CONFIG_DIR, configFileName);
        Properties properties = loadConfig(configPath);
        properties.setProperty(key, value);
        saveConfig(properties, configPath);
    }
    
    /**
     * Get application-specific configurations
     */
    public static class ApplicationConfig {
        private static final String CONFIG_FILE = APPLICATION_CONFIG;
        
        public static String getApplicationName() {
            return getConfigValue(CONFIG_FILE, "app.name", "SmartCampus");
        }
        
        public static String getApplicationVersion() {
            return getConfigValue(CONFIG_FILE, "app.version", "1.0.0");
        }
        
        public static int getMaxThreads() {
            return getConfigValueAsInt(CONFIG_FILE, "app.max.threads", 10);
        }
        
        public static int getSessionTimeout() {
            return getConfigValueAsInt(CONFIG_FILE, "app.session.timeout", 3600);
        }
        
        public static boolean isDebugEnabled() {
            return getConfigValueAsBoolean(CONFIG_FILE, "app.debug.enabled", false);
        }
        
        public static String getDataDirectory() {
            return getConfigValue(CONFIG_FILE, "app.data.directory", "data");
        }
        
        public static String getBackupDirectory() {
            return getConfigValue(CONFIG_FILE, "app.backup.directory", "backup");
        }
        
        public static int getBackupRetentionDays() {
            return getConfigValueAsInt(CONFIG_FILE, "app.backup.retention.days", 30);
        }
    }
    
    /**
     * Get database-specific configurations
     */
    public static class DatabaseConfig {
        private static final String CONFIG_FILE = DATABASE_CONFIG;
        
        public static String getConnectionUrl() {
            return getConfigValue(CONFIG_FILE, "db.url", "jdbc:h2:mem:smartcampus");
        }
        
        public static String getUsername() {
            return getConfigValue(CONFIG_FILE, "db.username", "sa");
        }
        
        public static String getPassword() {
            return getConfigValue(CONFIG_FILE, "db.password", "");
        }
        
        public static String getDriverClass() {
            return getConfigValue(CONFIG_FILE, "db.driver", "org.h2.Driver");
        }
        
        public static int getMaxConnections() {
            return getConfigValueAsInt(CONFIG_FILE, "db.max.connections", 20);
        }
        
        public static int getConnectionTimeout() {
            return getConfigValueAsInt(CONFIG_FILE, "db.connection.timeout", 30000);
        }
        
        public static boolean isAutoCommit() {
            return getConfigValueAsBoolean(CONFIG_FILE, "db.auto.commit", true);
        }
        
        public static String getBackupPath() {
            return getConfigValue(CONFIG_FILE, "db.backup.path", "backup/db");
        }
    }
    
    /**
     * Get logging-specific configurations
     */
    public static class LoggingConfig {
        private static final String CONFIG_FILE = LOGGING_CONFIG;
        
        public static String getLogLevel() {
            return getConfigValue(CONFIG_FILE, "log.level", "INFO");
        }
        
        public static String getLogDirectory() {
            return getConfigValue(CONFIG_FILE, "log.directory", "logs");
        }
        
        public static String getLogFileName() {
            return getConfigValue(CONFIG_FILE, "log.filename", "smartcampus.log");
        }
        
        public static int getMaxFileSize() {
            return getConfigValueAsInt(CONFIG_FILE, "log.max.file.size", 10485760); // 10MB
        }
        
        public static int getMaxBackupIndex() {
            return getConfigValueAsInt(CONFIG_FILE, "log.max.backup.index", 5);
        }
        
        public static boolean isConsoleLoggingEnabled() {
            return getConfigValueAsBoolean(CONFIG_FILE, "log.console.enabled", true);
        }
        
        public static String getLogPattern() {
            return getConfigValue(CONFIG_FILE, "log.pattern", "%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n");
        }
    }
    
    /**
     * Create default configuration if not exists
     */
    private static void createDefaultConfig(Path configPath) throws IOException {
        String fileName = configPath.getFileName().toString();
        Properties defaultProps = new Properties();
        
        switch (fileName) {
            case APPLICATION_CONFIG:
                createDefaultApplicationConfig(defaultProps);
                break;
            case DATABASE_CONFIG:
                createDefaultDatabaseConfig(defaultProps);
                break;
            case LOGGING_CONFIG:
                createDefaultLoggingConfig(defaultProps);
                break;
            default:
                // Create empty properties file
                break;
        }
        
        saveConfig(defaultProps, configPath);
    }
    
    /**
     * Create default application configuration
     */
    private static void createDefaultApplicationConfig(Properties props) {
        props.setProperty("app.name", "SmartCampus");
        props.setProperty("app.version", "1.0.0");
        props.setProperty("app.max.threads", "10");
        props.setProperty("app.session.timeout", "3600");
        props.setProperty("app.debug.enabled", "false");
        props.setProperty("app.data.directory", "data");
        props.setProperty("app.backup.directory", "backup");
        props.setProperty("app.backup.retention.days", "30");
        props.setProperty("app.locale", "en_US");
        props.setProperty("app.timezone", "UTC");
    }
    
    /**
     * Create default database configuration
     */
    private static void createDefaultDatabaseConfig(Properties props) {
        props.setProperty("db.url", "jdbc:h2:mem:smartcampus");
        props.setProperty("db.username", "sa");
        props.setProperty("db.password", "");
        props.setProperty("db.driver", "org.h2.Driver");
        props.setProperty("db.max.connections", "20");
        props.setProperty("db.connection.timeout", "30000");
        props.setProperty("db.auto.commit", "true");
        props.setProperty("db.backup.path", "backup/db");
        props.setProperty("db.init.schema", "true");
        props.setProperty("db.pool.enabled", "true");
    }
    
    /**
     * Create default logging configuration
     */
    private static void createDefaultLoggingConfig(Properties props) {
        props.setProperty("log.level", "INFO");
        props.setProperty("log.directory", "logs");
        props.setProperty("log.filename", "smartcampus.log");
        props.setProperty("log.max.file.size", "10485760");
        props.setProperty("log.max.backup.index", "5");
        props.setProperty("log.console.enabled", "true");
        props.setProperty("log.pattern", "%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n");
        props.setProperty("log.file.enabled", "true");
        props.setProperty("log.async.enabled", "false");
    }
    
    /**
     * Validate configuration
     */
    public static ConfigValidationResult validateConfig(Path configPath) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            if (!Files.exists(configPath)) {
                errors.add("Configuration file does not exist: " + configPath);
                return new ConfigValidationResult(false, errors, warnings);
            }
            
            Properties properties = loadConfig(configPath);
            
            // Validate based on config type
            String fileName = configPath.getFileName().toString();
            switch (fileName) {
                case APPLICATION_CONFIG:
                    validateApplicationConfig(properties, errors, warnings);
                    break;
                case DATABASE_CONFIG:
                    validateDatabaseConfig(properties, errors, warnings);
                    break;
                case LOGGING_CONFIG:
                    validateLoggingConfig(properties, errors, warnings);
                    break;
            }
            
        } catch (Exception e) {
            errors.add("Configuration validation error: " + e.getMessage());
        }
        
        return new ConfigValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Validate application configuration
     */
    private static void validateApplicationConfig(Properties props, List<String> errors, List<String> warnings) {
        validateRequiredProperty(props, "app.name", errors);
        validateRequiredProperty(props, "app.version", errors);
        
        validateIntegerProperty(props, "app.max.threads", 1, 100, errors, warnings);
        validateIntegerProperty(props, "app.session.timeout", 60, 86400, errors, warnings);
        
        validateDirectoryProperty(props, "app.data.directory", warnings);
        validateDirectoryProperty(props, "app.backup.directory", warnings);
    }
    
    /**
     * Validate database configuration
     */
    private static void validateDatabaseConfig(Properties props, List<String> errors, List<String> warnings) {
        validateRequiredProperty(props, "db.url", errors);
        validateRequiredProperty(props, "db.driver", errors);
        
        validateIntegerProperty(props, "db.max.connections", 1, 1000, errors, warnings);
        validateIntegerProperty(props, "db.connection.timeout", 1000, 300000, errors, warnings);
    }
    
    /**
     * Validate logging configuration
     */
    private static void validateLoggingConfig(Properties props, List<String> errors, List<String> warnings) {
        validateRequiredProperty(props, "log.level", errors);
        validateRequiredProperty(props, "log.directory", errors);
        
        String logLevel = props.getProperty("log.level");
        if (logLevel != null && !Arrays.asList("DEBUG", "INFO", "WARN", "ERROR", "FATAL").contains(logLevel.toUpperCase())) {
            warnings.add("Invalid log level: " + logLevel);
        }
        
        validateIntegerProperty(props, "log.max.file.size", 1024, 1073741824, errors, warnings); // 1KB to 1GB
        validateIntegerProperty(props, "log.max.backup.index", 1, 100, errors, warnings);
    }
    
    // Helper validation methods
    private static void validateRequiredProperty(Properties props, String key, List<String> errors) {
        if (props.getProperty(key) == null || props.getProperty(key).trim().isEmpty()) {
            errors.add("Required property missing: " + key);
        }
    }
    
    private static void validateIntegerProperty(Properties props, String key, int min, int max, 
                                              List<String> errors, List<String> warnings) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                int intValue = Integer.parseInt(value);
                if (intValue < min || intValue > max) {
                    warnings.add(String.format("Property %s value %d is outside recommended range [%d, %d]", 
                                              key, intValue, min, max));
                }
            } catch (NumberFormatException e) {
                errors.add("Property " + key + " must be a valid integer: " + value);
            }
        }
    }
    
    private static void validateDirectoryProperty(Properties props, String key, List<String> warnings) {
        String value = props.getProperty(key);
        if (value != null) {
            Path dir = Paths.get(value);
            if (!Files.exists(dir)) {
                warnings.add("Directory does not exist: " + value + " (will be created if needed)");
            } else if (!Files.isDirectory(dir)) {
                warnings.add("Path is not a directory: " + value);
            }
        }
    }
    
    /**
     * Configuration change listener interface
     */
    public interface ConfigChangeListener {
        void onConfigChange(String configPath, Properties newProperties);
    }
    
    /**
     * Add configuration change listener
     */
    public static void addConfigChangeListener(String configPath, ConfigChangeListener listener) {
        listeners.computeIfAbsent(configPath, k -> new ArrayList<>()).add(listener);
    }
    
    /**
     * Remove configuration change listener
     */
    public static void removeConfigChangeListener(String configPath, ConfigChangeListener listener) {
        List<ConfigChangeListener> configListeners = listeners.get(configPath);
        if (configListeners != null) {
            configListeners.remove(listener);
        }
    }
    
    /**
     * Notify configuration change listeners
     */
    private static void notifyConfigChange(String configPath, Properties properties) {
        List<ConfigChangeListener> configListeners = listeners.get(configPath);
        if (configListeners != null) {
            for (ConfigChangeListener listener : configListeners) {
                try {
                    listener.onConfigChange(configPath, properties);
                } catch (Exception e) {
                    System.err.println("Error notifying config change listener: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Reload all cached configurations
     */
    public static void reloadAllConfigs() {
        Set<String> configPaths = new HashSet<>(configCache.keySet());
        configCache.clear();
        lastModified.clear();
        
        for (String configPath : configPaths) {
            try {
                loadConfig(Paths.get(configPath));
            } catch (IOException e) {
                System.err.println("Error reloading config: " + configPath + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Get all configuration properties as a merged view
     */
    public static Properties getAllConfigurations() throws IOException {
        Properties merged = new Properties();
        
        // Load all standard configs
        try {
            merged.putAll(loadApplicationConfig());
        } catch (IOException e) {
            System.err.println("Warning: Could not load application config");
        }
        
        try {
            merged.putAll(loadDatabaseConfig());
        } catch (IOException e) {
            System.err.println("Warning: Could not load database config");
        }
        
        try {
            merged.putAll(loadLoggingConfig());
        } catch (IOException e) {
            System.err.println("Warning: Could not load logging config");
        }
        
        return merged;
    }
    
    /**
     * Configuration validation result
     */
    public static class ConfigValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ConfigValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("ConfigValidationResult{valid=%s, errors=%d, warnings=%d}",
                               valid, errors.size(), warnings.size());
        }
    }
}