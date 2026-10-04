package io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class LogManagerTest {

    @TempDir
    Path dir;

    private Path originalConfigDir;
    private Path logDir;
    private LogManager log;

    @BeforeEach
    void setUp() throws Exception {
        originalConfigDir = ConfigManager.getConfigDirectory();
        logDir = dir.resolve("logs");
        log = newLogManager("INFO", "3");
    }

    @AfterEach
    void tearDown() throws Exception {
        log.shutdown();
        Logger root = Logger.getLogger("SmartCampus");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
            h.close();
        }
        resetSingleton();
        ConfigManager.setConfigDirectory(originalConfigDir);
    }

    /** Fresh, non-singleton LogManager configured to log into the temp directory. */
    private LogManager newLogManager(String level, String backupIndex) throws Exception {
        Path configDir = Files.createDirectories(dir.resolve("config-" + level + "-" + backupIndex));
        Properties p = new Properties();
        p.setProperty("log.level", level);
        p.setProperty("log.directory", logDir.toString());
        p.setProperty("log.filename", "app.log");
        p.setProperty("log.max.file.size", "1048576");
        p.setProperty("log.max.backup.index", backupIndex);
        p.setProperty("log.console.enabled", "false");
        try (var out = Files.newOutputStream(configDir.resolve("logging.properties"))) {
            p.store(out, null);
        }
        ConfigManager.setConfigDirectory(configDir);
        Constructor<LogManager> ctor = LogManager.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void resetSingleton() throws Exception {
        Field f = LogManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    private List<String> logLines() throws Exception {
        return Files.readAllLines(logDir.resolve("app.log.0"));
    }

    @Test
    void getInstanceReturnsSingleton() throws Exception {
        resetSingleton();
        LogManager first = LogManager.getInstance();
        try {
            assertThat(LogManager.getInstance()).isSameAs(first);
        } finally {
            first.shutdown();
        }
        assertThat(Modifier.isVolatile(LogManager.class.getDeclaredField("instance").getModifiers())).isTrue();
    }

    @Nested
    class Writing {

        // Regression: messages use "{}" placeholders, which java.util.logging's formatter ignores,
        // so every logged event read e.g. "User [{}] performed action [{}] - {}".
        @Test
        void placeholdersAreSubstitutedInOrder() throws Exception {
            log.logUserAction("u1", "login", "ok");
            log.info("too few {} {}", "one");
            log.info("too many {}", "a", "b");
            log.info("no params {}");

            assertThat(logLines())
                    .anySatisfy(l -> assertThat(l).endsWith("USER_ACTION: User [u1] performed action [login] - ok"))
                    .anySatisfy(l -> assertThat(l).endsWith("too few one {}"))
                    .anySatisfy(l -> assertThat(l).endsWith("too many a"))
                    .anySatisfy(l -> assertThat(l).endsWith("no params {}"));
        }

        @Test
        void lineFormatHasTimestampLevelAndCallerClass() throws Exception {
            log.warn("careful");

            assertThat(logLines()).singleElement().asString()
                    .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\[WARNING] io\\.LogManagerTest\\$Writing - careful");
        }

        @Test
        void messagesBelowLevelAreDroppedUntilLevelIsLowered() throws Exception {
            log.debug("hidden {}", 1);
            log.logDatabaseOperation("INSERT", "items", 3, 12);
            assertThat(log.getLogStatistics().getTotalEntries()).isZero();

            log.setLogLevel(Level.FINE);
            log.debug("shown {}", 2);
            log.logDatabaseOperation("INSERT", "items", 3, 12);

            assertThat(log.getLogStatistics().getCurrentLogLevel()).isEqualTo(Level.FINE);
            assertThat(log.searchLogs("[FINE]", 10)).hasSize(2)
                    .anySatisfy(l -> assertThat(l).endsWith("shown 2"))
                    .anySatisfy(l -> assertThat(l).endsWith("DATABASE: INSERT on table [items] affected 3 records in 12ms"));
            assertThat(log.searchLogs("hidden", 10)).isEmpty();
        }

        @Test
        void domainEventHelpers() throws Exception {
            log.logEnrollmentEvent("s1", "CS101", "enrolled in", true);
            log.logEnrollmentEvent("s1", "CS102", "enroll in", false);
            log.logGradeEvent("s1", "CS101", "A", "p9");
            log.logSystemEvent("BOOT", "started");
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("rows", 5);
            log.logPerformance("load", 30, metrics);
            log.logPerformance("noop", 1, null);
            log.logPerformance("empty", 2, Map.of());
            log.logSecurityEvent("LOGIN", "u1", "10.0.0.1", true);
            log.logSecurityEvent("LOGIN", "u2", "10.0.0.2", false);
            log.error("failed {}", new IllegalStateException("x"), "job");

            assertThat(logLines()).hasSize(10)
                    .anySatisfy(l -> assertThat(l).contains("[INFO]").endsWith("ENROLLMENT: Student [s1] enrolled in course [CS101] successfully"))
                    .anySatisfy(l -> assertThat(l).contains("[WARNING]").endsWith("ENROLLMENT: Student [s1] failed to enroll in course [CS102]"))
                    .anySatisfy(l -> assertThat(l).endsWith("GRADE: Professor [p9] assigned grade [A] to student [s1] for course [CS101]"))
                    .anySatisfy(l -> assertThat(l).endsWith("SYSTEM: BOOT - started"))
                    .anySatisfy(l -> assertThat(l).endsWith("PERFORMANCE: Operation [load] took 30ms - Metrics: rows=5 "))
                    .anySatisfy(l -> assertThat(l).endsWith("PERFORMANCE: Operation [noop] took 1ms"))
                    .anySatisfy(l -> assertThat(l).contains("[INFO]").endsWith("SECURITY: LOGIN - User [u1] from IP [10.0.0.1] - SUCCESS"))
                    .anySatisfy(l -> assertThat(l).contains("[WARNING]").endsWith("SECURITY: LOGIN - User [u2] from IP [10.0.0.2] - FAILURE"))
                    .anySatisfy(l -> assertThat(l).contains("[SEVERE]").endsWith("failed job"));
        }
    }

    @Nested
    class Formatters {

        // Regression: the JSON formatter only escaped double quotes, so backslashes and line breaks
        // in messages produced invalid JSON.
        @Test
        void jsonFormatterProducesValidJsonForSpecialCharacters() throws Exception {
            log.setLogFormatter("json");
            log.info("path C:\\temp \"quoted\"\nsecond line\ttab");

            List<String> lines = logLines();
            JsonNode node = new ObjectMapper().readTree(lines.get(lines.size() - 1));
            assertThat(node.get("message").asText()).isEqualTo("path C:\\temp \"quoted\"\nsecond line\ttab");
            assertThat(node.get("level").asText()).isEqualTo("INFO");
            assertThat(node.get("logger").asText()).isEqualTo("io.LogManagerTest$Formatters");
            assertThat(node.get("timestamp").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        void patternFormatterSubstitutesTokens() throws Exception {
            log.setLogFormatter("pattern");
            log.warn("hello {}", "world");

            assertThat(logLines()).last().asString()
                    .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} \\[WARNING] io\\.LogManagerTest\\$Formatters - hello world");
        }

        @Test
        void unknownFormatterIsReportedAndIgnored() throws Exception {
            log.setLogFormatter("nope");
            log.info("still simple");

            assertThat(logLines()).anySatisfy(l -> assertThat(l).contains("[WARNING]").endsWith("Unknown log formatter: nope"))
                    .last().asString().contains("[INFO]");
        }
    }

    @Nested
    class Management {

        // Regression: with more than one generation FileHandler writes "<name>.0", but statistics,
        // search, export and reports read "<name>", so they always saw an empty log.
        @Test
        void statisticsAndSearchReadTheActiveLogFile() throws Exception {
            log.info("needle one");
            log.info("NEEDLE two");
            log.info("needle three");

            assertThat(log.searchLogs("needle", 2)).hasSize(2);
            assertThat(log.searchLogs("needle", 10)).hasSize(3);

            LogManager.LogStatistics stats = log.getLogStatistics();
            assertThat(stats.getCurrentLogFileSize()).isEqualTo(Files.size(logDir.resolve("app.log.0"))).isPositive();
            assertThat(stats.getTotalEntries()).isEqualTo(3);
            assertThat(stats.getQueueSize()).isZero();
            assertThat(stats.isFileLoggingEnabled()).isTrue();
            assertThat(stats.isAsyncLoggingEnabled()).isFalse();
            assertThat(stats.isConsoleLoggingEnabled()).isFalse();
            assertThat(stats.toString()).startsWith("LogStatistics{entries=3, queueSize=0, fileSize=");
        }

        @Test
        void singleGenerationLogsToThePlainFileName() throws Exception {
            log.shutdown();
            log = newLogManager("INFO", "1");

            log.info("single");

            assertThat(Files.readAllLines(logDir.resolve("app.log"))).singleElement().asString().endsWith("single");
            assertThat(log.searchLogs("single", 5)).hasSize(1);
        }

        // Regression: "DEBUG"/"WARN"/"ERROR" are accepted by ConfigManager's validation but
        // Level.parse rejected them, so LogManager failed to start.
        @Test
        void commonLevelNamesAreAccepted() throws Exception {
            log.shutdown();
            log = newLogManager("DEBUG", "3");
            assertThat(log.getLogStatistics().getCurrentLogLevel()).isEqualTo(Level.FINE);
            log.shutdown();
            log = newLogManager("WARN", "3");
            assertThat(log.getLogStatistics().getCurrentLogLevel()).isEqualTo(Level.WARNING);
            log.shutdown();
            log = newLogManager("ERROR", "3");
            assertThat(log.getLogStatistics().getCurrentLogLevel()).isEqualTo(Level.SEVERE);
        }

        @Test
        void reportCountsLevels() throws Exception {
            log.info("a");
            log.info("b");
            log.warn("c");
            log.error("d");
            LocalDateTime now = LocalDateTime.now();

            LogManager.LogReport report = log.generateLogReport(now.minusDays(1), now);

            assertThat(report.getTotalLogEntries()).isEqualTo(4);
            assertThat(report.getErrorCount()).isEqualTo(1);
            assertThat(report.getWarningCount()).isEqualTo(1);
            assertThat(report.getLevelCounts()).containsEntry(Level.INFO, 2);
            assertThat(report.getErrorMessages()).singleElement().asString().endsWith(" - d");
            assertThat(report.getLoggerCounts()).isEmpty();
            assertThat(report.getStartDate()).isEqualTo(now.minusDays(1));
            assertThat(report.getEndDate()).isEqualTo(now);
        }

        @Test
        void exportCopiesEntriesMatchingTheDates() throws Exception {
            log.info("exported entry");
            Path export = dir.resolve("export.log");
            LocalDateTime today = LocalDate.now().atStartOfDay();

            log.exportLogs(today, today, export);
            log.exportLogs(today.minusYears(5), today.minusYears(5), dir.resolve("none.log"));

            assertThat(Files.readAllLines(export)).anySatisfy(l -> assertThat(l).endsWith("exported entry"));
            assertThat(dir.resolve("none.log")).isEmptyFile();
        }

        @Test
        void rotationKeepsLogging() throws Exception {
            log.info("before");
            log.rotateLogFile();
            log.info("after");

            assertThat(log.searchLogs("Log file rotated successfully", 5)).hasSize(1);
            assertThat(log.searchLogs("after", 5)).hasSize(1);
        }

        @Test
        void consoleLoggingCanBeToggled() {
            log.setConsoleLogging(true);
            assertThat(log.getLogStatistics().isConsoleLoggingEnabled()).isTrue();
            log.setConsoleLogging(true);
            log.setConsoleLogging(false);
            assertThat(log.getLogStatistics().isConsoleLoggingEnabled()).isFalse();
        }

        @Test
        void archiveMovesOnlyOldLogFiles() throws Exception {
            Path old = Files.writeString(logDir.resolve("old.log"), "x");
            Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(30, ChronoUnit.DAYS)));
            Path recent = Files.writeString(logDir.resolve("recent.log"), "y");

            log.archiveOldLogs(7);

            assertThat(old).doesNotExist();
            assertThat(logDir.resolve("archive/old.log")).hasContent("x");
            assertThat(recent).exists();
            assertThat(log.searchLogs("Archived old log file: old.log", 5)).hasSize(1);
        }

        @Test
        void cleanupDeletesOnlyOldLogFiles() throws Exception {
            Path old = Files.writeString(logDir.resolve("old.log.1"), "x");
            Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(30, ChronoUnit.DAYS)));
            Path oldOther = Files.writeString(logDir.resolve("notes.txt"), "z");
            Files.setLastModifiedTime(oldOther, FileTime.from(Instant.now().minus(30, ChronoUnit.DAYS)));

            log.cleanupOldLogs(7);

            assertThat(old).doesNotExist();
            assertThat(oldOther).exists();
            assertThat(logDir.resolve("app.log.0")).exists();
            assertThat(log.searchLogs("Deleted old log file: old.log.1", 5)).hasSize(1);
        }

        @Test
        void cleanupOfMissingDirectoryIsReportedNotThrown() throws Exception {
            log.shutdown();
            FileUtil.deleteDirectoryRecursively(logDir);

            log.cleanupOldLogs(1); // directory gone: error is logged, nothing thrown
            assertThat(logDir).doesNotExist();
        }

        @Test
        void shutdownLogsAndClosesFileHandler() throws Exception {
            log.shutdown();
            long size = Files.size(logDir.resolve("app.log.0"));
            log.info("after shutdown");

            assertThat(Files.size(logDir.resolve("app.log.0"))).isEqualTo(size);
            assertThat(logLines()).last().asString().endsWith("Shutting down LogManager");
        }
    }

    @Nested
    class ValueObjects {

        @Test
        void statisticsFormatSize() {
            assertThat(stats(10).getFormattedFileSize()).isEqualTo("10 B");
            assertThat(stats(2048).getFormattedFileSize()).isEqualTo(String.format("%.1f KB", 2.0));
            assertThat(stats(3L * 1024 * 1024).getFormattedFileSize()).isEqualTo(String.format("%.1f MB", 3.0));
        }

        private LogManager.LogStatistics stats(long size) {
            return new LogManager.LogStatistics(0, 0, size, Level.INFO, false, true, false);
        }
    }
}
