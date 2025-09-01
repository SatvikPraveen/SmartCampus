// File location: src/main/java/io/LogManager.java

package io;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

/**
 * Comprehensive logging framework for the SmartCampus system
 * Provides structured logging with file rotation, async logging, and configurable levels
 */
public class LogManager {
    
    private static LogManager instance;
    private static final Object instanceLock = new Object();
    
    // Logging configuration
    private String logDirectory;
    private String logFileName;
    private Level logLevel;
    private int maxFileSize;
    private int maxBackupIndex;
    private boolean consoleLoggingEnabled;
    private boolean fileLoggingEnabled;
    private boolean asyncLoggingEnabled;
    private String logPattern;
    
    // Internal components
    private Logger rootLogger;
    private FileHandler fileHandler;
    private ConsoleHandler consoleHandler;
    private ExecutorService asyncExecutor;
    private BlockingQueue<LogEntry> logQueue;
    private final AtomicLong logEntryCount = new AtomicLong(0);
    private volatile boolean isShutdown = false;
    
    // Log formatters
    private final Map<String, Formatter> formatters = new HashMap<>();
    
    private LogManager() {
        loadConfiguration();
        initializeLogging();
        startAsyncProcessor();
    }
    
    /**
     * Get singleton instance
     */
    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load logging configuration
     */
    private void loadConfiguration() {
        this.logDirectory = ConfigManager.LoggingConfig.getLogDirectory();
        this.logFileName = ConfigManager.LoggingConfig.getLogFileName();
        this.logLevel = Level.parse(ConfigManager.LoggingConfig.getLogLevel());
        this.maxFileSize = ConfigManager.LoggingConfig.getMaxFileSize();
        this.maxBackupIndex = ConfigManager.LoggingConfig.getMaxBackupIndex();
        this.consoleLoggingEnabled = ConfigManager.LoggingConfig.isConsoleLoggingEnabled();
        this.fileLoggingEnabled = true; // Always enable file logging
        this.asyncLoggingEnabled = false; // Default to synchronous
        this.logPattern = ConfigManager.LoggingConfig.getLogPattern();
    }
    
    /**
     * Initialize logging components
     */
    private void initializeLogging() {
        try {
            // Create log directory if it doesn't exist
            Path logDir = Paths.get(logDirectory);
            Files.createDirectories(logDir);
            
            // Initialize root logger
            rootLogger = Logger.getLogger("SmartCampus");
            rootLogger.setLevel(logLevel);
            rootLogger.setUseParentHandlers(false);
            
            // Initialize formatters
            initializeFormatters();
            
            // Setup file handler
            if (fileLoggingEnabled) {
                setupFileHandler();
            }
            
            // Setup console handler
            if (consoleLoggingEnabled) {
                setupConsoleHandler();
            }
            
            // Setup async logging if enabled
            if (asyncLoggingEnabled) {
                setupAsyncLogging();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize log formatters
     */
    private void initializeFormatters() {
        // Simple formatter
        formatters.put("simple", new SimpleFormatter() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            @Override
            public String format(LogRecord record) {
                return String.format("%s [%s] %s - %s%n",
                    LocalDateTime.now().format(formatter),
                    record.getLevel(),
                    record.getLoggerName(),
                    formatMessage(record)
                );
            }
        });
        
        // JSON formatter
        formatters.put("json", new Formatter() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            
            @Override
            public String format(LogRecord record) {
                return String.format(
                    "{\"timestamp\":\"%s\",\"level\":\"%s\",\"logger\":\"%s\",\"message\":\"%s\",\"thread\":\"%s\"}%n",
                    LocalDateTime.now().format(formatter),
                    record.getLevel(),
                    record.getLoggerName(),
                    formatMessage(record).replace("\"", "\\\""),
                    Thread.currentThread().getName()
                );
            }
        });
        
        // Custom pattern formatter
        formatters.put("pattern", new PatternFormatter(logPattern));
    }
    
    /**
     * Setup file handler with rotation
     */
    private void setupFileHandler() throws IOException {
        Path logFile = Paths.get(logDirectory, logFileName);
        
        fileHandler = new FileHandler(
            logFile.toString(),
            maxFileSize,
            maxBackupIndex,
            true // append
        );
        
        fileHandler.setLevel(logLevel);
        fileHandler.setFormatter(formatters.get("simple"));
        rootLogger.addHandler(fileHandler);
    }
    
    /**
     * Setup console handler
     */
    private void setupConsoleHandler() {
        consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(logLevel);
        consoleHandler.setFormatter(formatters.get("simple"));
        rootLogger.addHandler(consoleHandler);
    }
    
    /**
     * Setup async logging
     */
    private void setupAsyncLogging() {
        logQueue = new LinkedBlockingQueue<>();
        asyncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LogManager-Async");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Start async log processor
     */
    private void startAsyncProcessor() {
        if (asyncLoggingEnabled && asyncExecutor != null) {
            asyncExecutor.submit(() -> {
                while (!isShutdown || !logQueue.isEmpty()) {
                    try {
                        LogEntry entry = logQueue.poll(1, TimeUnit.SECONDS);
                        if (entry != null) {
                            processLogEntry(entry);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error processing log entry: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    /**
     * Process log entry
     */
    private void processLogEntry(LogEntry entry) {
        LogRecord record = new LogRecord(entry.getLevel(), entry.getMessage());
        record.setLoggerName(entry.getLoggerName());
        record.setMillis(entry.getTimestamp());
        record.setParameters(entry.getParameters());
        
        if (entry.getThrowable() != null) {
            record.setThrown(entry.getThrowable());
        }
        
        rootLogger.log(record);
    }
    
    // Public logging methods
    
    /**
     * Log debug message
     */
    public void debug(String message, Object... params) {
        log(Level.FINE, message, params);
    }
    
    /**
     * Log info message
     */
    public void info(String message, Object... params) {
        log(Level.INFO, message, params);
    }
    
    /**
     * Log warning message
     */
    public void warn(String message, Object... params) {
        log(Level.WARNING, message, params);
    }
    
    /**
     * Log error message
     */
    public void error(String message, Object... params) {
        log(Level.SEVERE, message, params);
    }
    
    /**
     * Log error message with exception
     */
    public void error(String message, Throwable throwable, Object... params) {
        log(Level.SEVERE, message, throwable, params);
    }
    
    /**
     * Log message with custom level
     */
    public void log(Level level, String message, Object... params) {
        log(level, message, null, params);
    }
    
    /**
     * Log message with custom level and exception
     */
    public void log(Level level, String message, Throwable throwable, Object... params) {
        if (!rootLogger.isLoggable(level)) {
            return;
        }
        
        String loggerName = getCallerClassName();
        long timestamp = System.currentTimeMillis();
        
        LogEntry entry = new LogEntry(level, message, loggerName, timestamp, throwable, params);
        logEntryCount.incrementAndGet();
        
        if (asyncLoggingEnabled) {
            try {
                logQueue.offer(entry, 1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Fall back to synchronous logging
                processLogEntry(entry);
            }
        } else {
            processLogEntry(entry);
        }
    }
    
    /**
     * Get caller class name for logging context
     */
    private String getCallerClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Skip LogManager methods and find the actual caller
        for (int i = 3; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!className.startsWith("io.LogManager")) {
                return className;
            }
        }
        return "Unknown";
    }
    
    // Specialized logging methods for SmartCampus events
    
    /**
     * Log user action
     */
    public void logUserAction(String userId, String action, String details) {
        info("USER_ACTION: User [{}] performed action [{}] - {}", userId, action, details);
    }
    
    /**
     * Log enrollment event
     */
    public void logEnrollmentEvent(String studentId, String courseCode, String action, boolean success) {
        if (success) {
            info("ENROLLMENT: Student [{}] {} course [{}] successfully", studentId, action, courseCode);
        } else {
            warn("ENROLLMENT: Student [{}] failed to {} course [{}]", studentId, action, courseCode);
        }
    }
    
    /**
     * Log grade event
     */
    public void logGradeEvent(String studentId, String courseCode, String grade, String professorId) {
        info("GRADE: Professor [{}] assigned grade [{}] to student [{}] for course [{}]", 
             professorId, grade, studentId, courseCode);
    }
    
    /**
     * Log system event
     */
    public void logSystemEvent(String event, String details) {
        info("SYSTEM: {} - {}", event, details);
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformance(String operation, long duration, Map<String, Object> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("PERFORMANCE: Operation [").append(operation).append("] took ").append(duration).append("ms");
        
        if (metrics != null && !metrics.isEmpty()) {
            sb.append(" - Metrics: ");
            metrics.forEach((key, value) -> sb.append(key).append("=").append(value).append(" "));
        }
        
        info(sb.toString());
    }
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String userId, String ipAddress, boolean success) {
        Level level = success ? Level.INFO : Level.WARNING;
        String status = success ? "SUCCESS" : "FAILURE";
        log(level, "SECURITY: {} - User [{}] from IP [{}] - {}", event, userId, ipAddress, status);
    }
    
    /**
     * Log database operation
     */
    public void logDatabaseOperation(String operation, String table, int recordsAffected, long duration) {
        debug("DATABASE: {} on table [{}] affected {} records in {}ms", 
              operation, table, recordsAffected, duration);
    }
    
    // Log management operations
    
    /**
     * Set log level dynamically
     */
    public void setLogLevel(Level level) {
        this.logLevel = level;
        rootLogger.setLevel(level);
        
        if (fileHandler != null) {
            fileHandler.setLevel(level);
        }
        if (consoleHandler != null) {
            consoleHandler.setLevel(level);
        }
        
        info("Log level changed to: {}", level);
    }
    
    /**
     * Enable/disable console logging
     */
    public void setConsoleLogging(boolean enabled) {
        this.consoleLoggingEnabled = enabled;
        
        if (enabled && consoleHandler == null) {
            setupConsoleHandler();
        } else if (!enabled && consoleHandler != null) {
            rootLogger.removeHandler(consoleHandler);
            consoleHandler.close();
            consoleHandler = null;
        }
        
        info("Console logging {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Switch log formatter
     */
    public void setLogFormatter(String formatterName) {
        Formatter formatter = formatters.get(formatterName);
        if (formatter != null) {
            if (fileHandler != null) {
                fileHandler.setFormatter(formatter);
            }
            if (consoleHandler != null) {
                consoleHandler.setFormatter(formatter);
            }
            info("Log formatter changed to: {}", formatterName);
        } else {
            warn("Unknown log formatter: {}", formatterName);
        }
    }
    
    /**
     * Force log file rotation
     */
    public void rotateLogFile() {
        if (fileHandler != null) {
            try {
                // Close current handler
                rootLogger.removeHandler(fileHandler);
                fileHandler.close();
                
                // Create new handler
                setupFileHandler();
                
                info("Log file rotated successfully");
            } catch (IOException e) {
                error("Failed to rotate log file", e);
            }
        }
    }
    
    /**
     * Get log statistics
     */
    public LogStatistics getLogStatistics() {
        long totalEntries = logEntryCount.get();
        int queueSize = logQueue != null ? logQueue.size() : 0;
        long currentLogFileSize = getCurrentLogFileSize();
        
        return new LogStatistics(
            totalEntries,
            queueSize,
            currentLogFileSize,
            logLevel,
            consoleLoggingEnabled,
            fileLoggingEnabled,
            asyncLoggingEnabled
        );
    }
    
    /**
     * Get current log file size
     */
    private long getCurrentLogFileSize() {
        try {
            Path logFile = Paths.get(logDirectory, logFileName);
            if (Files.exists(logFile)) {
                return Files.size(logFile);
            }
        } catch (IOException e) {
            // Ignore
        }
        return 0;
    }
    
    /**
     * Archive old log files
     */
    public void archiveOldLogs(int daysOld) {
        try {
            Path logDir = Paths.get(logDirectory);
            Path archiveDir = logDir.resolve("archive");
            Files.createDirectories(archiveDir);
            
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
            
            Files.list(logDir)
                 .filter(Files::isRegularFile)
                 .filter(path -> path.getFileName().toString().endsWith(".log"))
                 .filter(path -> {
                     try {
                         return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                     } catch (IOException e) {
                         return false;
                     }
                 })
                 .forEach(path -> {
                     try {
                         Path archivePath = archiveDir.resolve(path.getFileName());
                         Files.move(path, archivePath);
                         info("Archived old log file: {}", path.getFileName());
                     } catch (IOException e) {
                         error("Failed to archive log file: {}", e, path.getFileName());
                     }
                 });
                 
        } catch (IOException e) {
            error("Failed to archive old logs", e);
        }
    }
    
    /**
     * Clean up log files older than specified days
     */
    public void cleanupOldLogs(int daysOld) {
        try {
            Path logDir = Paths.get(logDirectory);
            long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
            
            Files.list(logDir)
                 .filter(Files::isRegularFile)
                 .filter(path -> path.getFileName().toString().contains("log"))
                 .filter(path -> {
                     try {
                         return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                     } catch (IOException e) {
                         return false;
                     }
                 })
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                         info("Deleted old log file: {}", path.getFileName());
                     } catch (IOException e) {
                         error("Failed to delete log file: {}", e, path.getFileName());
                     }
                 });
                 
        } catch (IOException e) {
            error("Failed to cleanup old logs", e);
        }
    }
    
    /**
     * Export logs for a specific date range
     */
    public void exportLogs(LocalDateTime startDate, LocalDateTime endDate, Path exportFile) {
        try {
            List<String> exportedLogs = new ArrayList<>();
            Path logFile = Paths.get(logDirectory, logFileName);
            
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                
                for (String line : lines) {
                    // Simple date parsing - in production, you'd want more robust parsing
                    if (line.contains(startDate.toLocalDate().toString()) || 
                        line.contains(endDate.toLocalDate().toString())) {
                        exportedLogs.add(line);
                    }
                }
            }
            
            Files.write(exportFile, exportedLogs);
            info("Exported {} log entries to {}", exportedLogs.size(), exportFile);
            
        } catch (IOException e) {
            error("Failed to export logs", e);
        }
    }
    
    /**
     * Search logs for specific patterns
     */
    public List<String> searchLogs(String searchPattern, int maxResults) {
        List<String> results = new ArrayList<>();
        
        try {
            Path logFile = Paths.get(logDirectory, logFileName);
            
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                
                for (String line : lines) {
                    if (line.toLowerCase().contains(searchPattern.toLowerCase())) {
                        results.add(line);
                        if (results.size() >= maxResults) {
                            break;
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            error("Failed to search logs", e);
        }
        
        return results;
    }
    
    /**
     * Create log report
     */
    public LogReport generateLogReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<Level, Integer> levelCounts = new EnumMap<>(Level.class);
        Map<String, Integer> loggerCounts = new HashMap<>();
        List<String> errorMessages = new ArrayList<>();
        
        try {
            Path logFile = Paths.get(logDirectory, logFileName);
            
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                
                for (String line : lines) {
                    // Parse log entry - simplified parsing
                    if (line.contains("[INFO]")) {
                        levelCounts.merge(Level.INFO, 1, Integer::sum);
                    } else if (line.contains("[WARNING]")) {
                        levelCounts.merge(Level.WARNING, 1, Integer::sum);
                    } else if (line.contains("[SEVERE]")) {
                        levelCounts.merge(Level.SEVERE, 1, Integer::sum);
                        errorMessages.add(line);
                    } else if (line.contains("[FINE]")) {
                        levelCounts.merge(Level.FINE, 1, Integer::sum);
                    }
                }
            }
            
        } catch (IOException e) {
            error("Failed to generate log report", e);
        }
        
        return new LogReport(startDate, endDate, levelCounts, loggerCounts, errorMessages);
    }
    
    /**
     * Shutdown log manager
     */
    public void shutdown() {
        info("Shutting down LogManager");
        isShutdown = true;
        
        // Shutdown async executor
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Close handlers
        if (fileHandler != null) {
            fileHandler.close();
        }
        if (consoleHandler != null) {
            consoleHandler.close();
        }
    }
    
    // Inner classes
    
    private static class LogEntry {
        private final Level level;
        private final String message;
        private final String loggerName;
        private final long timestamp;
        private final Throwable throwable;
        private final Object[] parameters;
        
        public LogEntry(Level level, String message, String loggerName, 
                       long timestamp, Throwable throwable, Object[] parameters) {
            this.level = level;
            this.message = message;
            this.loggerName = loggerName;
            this.timestamp = timestamp;
            this.throwable = throwable;
            this.parameters = parameters;
        }
        
        // Getters
        public Level getLevel() { return level; }
        public String getMessage() { return message; }
        public String getLoggerName() { return loggerName; }
        public long getTimestamp() { return timestamp; }
        public Throwable getThrowable() { return throwable; }
        public Object[] getParameters() { return parameters; }
    }
    
    public static class LogStatistics {
        private final long totalEntries;
        private final int queueSize;
        private final long currentLogFileSize;
        private final Level currentLogLevel;
        private final boolean consoleLoggingEnabled;
        private final boolean fileLoggingEnabled;
        private final boolean asyncLoggingEnabled;
        
        public LogStatistics(long totalEntries, int queueSize, long currentLogFileSize,
                           Level currentLogLevel, boolean consoleLoggingEnabled,
                           boolean fileLoggingEnabled, boolean asyncLoggingEnabled) {
            this.totalEntries = totalEntries;
            this.queueSize = queueSize;
            this.currentLogFileSize = currentLogFileSize;
            this.currentLogLevel = currentLogLevel;
            this.consoleLoggingEnabled = consoleLoggingEnabled;
            this.fileLoggingEnabled = fileLoggingEnabled;
            this.asyncLoggingEnabled = asyncLoggingEnabled;
        }
        
        // Getters
        public long getTotalEntries() { return totalEntries; }
        public int getQueueSize() { return queueSize; }
        public long getCurrentLogFileSize() { return currentLogFileSize; }
        public Level getCurrentLogLevel() { return currentLogLevel; }
        public boolean isConsoleLoggingEnabled() { return consoleLoggingEnabled; }
        public boolean isFileLoggingEnabled() { return fileLoggingEnabled; }
        public boolean isAsyncLoggingEnabled() { return asyncLoggingEnabled; }
        
        public String getFormattedFileSize() {
            if (currentLogFileSize < 1024) return currentLogFileSize + " B";
            if (currentLogFileSize < 1024 * 1024) return String.format("%.1f KB", currentLogFileSize / 1024.0);
            return String.format("%.1f MB", currentLogFileSize / (1024.0 * 1024));
        }
        
        @Override
        public String toString() {
            return String.format("LogStatistics{entries=%d, queueSize=%d, fileSize=%s, level=%s}",
                               totalEntries, queueSize, getFormattedFileSize(), currentLogLevel);
        }
    }
    
    public static class LogReport {
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;
        private final Map<Level, Integer> levelCounts;
        private final Map<String, Integer> loggerCounts;
        private final List<String> errorMessages;
        
        public LogReport(LocalDateTime startDate, LocalDateTime endDate,
                        Map<Level, Integer> levelCounts, Map<String, Integer> loggerCounts,
                        List<String> errorMessages) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.levelCounts = levelCounts;
            this.loggerCounts = loggerCounts;
            this.errorMessages = errorMessages;
        }
        
        // Getters
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public Map<Level, Integer> getLevelCounts() { return levelCounts; }
        public Map<String, Integer> getLoggerCounts() { return loggerCounts; }
        public List<String> getErrorMessages() { return errorMessages; }
        
        public int getTotalLogEntries() {
            return levelCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public int getErrorCount() {
            return levelCounts.getOrDefault(Level.SEVERE, 0);
        }
        
        public int getWarningCount() {
            return levelCounts.getOrDefault(Level.WARNING, 0);
        }
    }
    
    private static class PatternFormatter extends Formatter {
        private final String pattern;
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        public PatternFormatter(String pattern) {
            this.pattern = pattern != null ? pattern : "%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n";
        }
        
        @Override
        public String format(LogRecord record) {
            String formatted = pattern;
            
            // Replace placeholders
            formatted = formatted.replace("%d{yyyy-MM-dd HH:mm:ss}", 
                LocalDateTime.now().format(dateFormatter));
            formatted = formatted.replace("%level", record.getLevel().toString());
            formatted = formatted.replace("%logger{36}", 
                truncateLogger(record.getLoggerName(), 36));
            formatted = formatted.replace("%msg", formatMessage(record));
            formatted = formatted.replace("%n", System.lineSeparator());
            
            return formatted;
        }
        
        private String truncateLogger(String loggerName, int maxLength) {
            if (loggerName.length() <= maxLength) {
                return loggerName;
            }
            
            String[] parts = loggerName.split("\\.");
            if (parts.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    sb.append(parts[i].charAt(0)).append(".");
                }
                sb.append(parts[parts.length - 1]);
                return sb.toString();
            }
            
            return loggerName.substring(0, maxLength - 3) + "...";
        }
    }
}