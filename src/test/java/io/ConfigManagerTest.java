package io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigManagerTest {

    @TempDir
    Path dir;

    private Path originalConfigDir;
    private final List<Runnable> cleanups = new ArrayList<>();

    @BeforeEach
    void redirectConfigDirectory() {
        originalConfigDir = ConfigManager.getConfigDirectory();
        ConfigManager.setConfigDirectory(dir);
    }

    @AfterEach
    void restore() {
        cleanups.forEach(Runnable::run);
        ConfigManager.setConfigDirectory(originalConfigDir);
    }

    private Path writeProps(String name, String... keyValues) throws IOException {
        Properties p = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            p.setProperty(keyValues[i], keyValues[i + 1]);
        }
        Path file = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, null);
        }
        return file;
    }

    private static Properties readRaw(Path file) throws IOException {
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) {
            p.load(in);
        }
        return p;
    }

    /** Make an external edit visible to the modification-time based cache. */
    private static void touchLater(Path file) throws IOException {
        FileTime later = FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 10_000);
        Files.setLastModifiedTime(file, later);
    }

    private void listen(Path file, ConfigManager.ConfigChangeListener listener) {
        ConfigManager.addConfigChangeListener(file.toString(), listener);
        cleanups.add(() -> ConfigManager.removeConfigChangeListener(file.toString(), listener));
    }

    @Test
    void defaultDirectoryIsConfigUnderWorkingDirectory() {
        assertThat(originalConfigDir).isEqualTo(Paths.get("config"));
        assertThatThrownBy(() -> ConfigManager.setConfigDirectory(null)).isInstanceOf(NullPointerException.class);
    }

    @Nested
    class Loading {

        @Test
        void missingStandardConfigsAreCreatedWithDefaults() throws Exception {
            Properties app = ConfigManager.loadApplicationConfig();
            Properties db = ConfigManager.loadDatabaseConfig();
            Properties log = ConfigManager.loadLoggingConfig();

            assertThat(dir.resolve("application.properties")).exists();
            assertThat(app.getProperty("app.name")).isEqualTo("SmartCampus");
            assertThat(app.getProperty("app.timezone")).isEqualTo("UTC");
            assertThat(db.getProperty("db.url")).isEqualTo("jdbc:h2:mem:smartcampus");
            assertThat(db.getProperty("db.pool.enabled")).isEqualTo("true");
            assertThat(log.getProperty("log.level")).isEqualTo("INFO");
            assertThat(log.getProperty("log.async.enabled")).isEqualTo("false");
            assertThat(readRaw(dir.resolve("logging.properties")).getProperty("log.file.enabled")).isEqualTo("true");
        }

        @Test
        void missingCustomConfigIsCreatedEmpty() throws Exception {
            Path custom = dir.resolve("sub/custom.properties");
            Properties p = ConfigManager.loadConfig(custom);
            assertThat(p.stringPropertyNames()).isEmpty();
            assertThat(custom).exists();
        }

        @Test
        void returnedPropertiesAreIndependentCopies() throws Exception {
            writeProps("x.properties", "k", "v");
            Properties first = ConfigManager.loadConfig("x.properties");
            first.setProperty("k", "changed");
            first.setProperty("extra", "1");

            Properties second = ConfigManager.loadConfig("x.properties");
            assertThat(second.getProperty("k")).isEqualTo("v");
            assertThat(second.getProperty("extra")).isNull();
        }

        // Regression: loadConfig returned an empty Properties whose *defaults* were the cached values,
        // so keySet()/entrySet()/putAll() saw nothing (e.g. getAllConfigurations() was always empty).
        @Test
        void returnedPropertiesExposeEntriesDirectly() throws Exception {
            writeProps("x.properties", "a", "1", "b", "2");

            Properties p = ConfigManager.loadConfig("x.properties");

            assertThat(p).containsOnlyKeys("a", "b");
            Properties copy = new Properties();
            copy.putAll(p);
            assertThat(copy.getProperty("a")).isEqualTo("1");
        }

        @Test
        void externalEditsAreReloadedWhenFileIsNewer() throws Exception {
            Path file = writeProps("x.properties", "k", "v1");
            assertThat(ConfigManager.loadConfig(file).getProperty("k")).isEqualTo("v1");

            writeProps("x.properties", "k", "v2");
            touchLater(file);

            assertThat(ConfigManager.loadConfig(file).getProperty("k")).isEqualTo("v2");
        }

        @Test
        void reloadAllConfigsReReadsCachedFiles() throws Exception {
            Path file = writeProps("x.properties", "k", "v1");
            ConfigManager.loadConfig(file);
            List<String> seen = new ArrayList<>();
            listen(file, (path, props) -> seen.add(props.getProperty("k")));

            writeProps("x.properties", "k", "v2"); // possibly same mtime: only a forced reload sees it
            ConfigManager.reloadAllConfigs();

            assertThat(seen).containsExactly("v2");
            assertThat(ConfigManager.loadConfig(file).getProperty("k")).isEqualTo("v2");
        }

        @Test
        void unreadablePathPropagatesIOException() throws Exception {
            Path asDirectory = Files.createDirectory(dir.resolve("dir.properties"));
            assertThatThrownBy(() -> ConfigManager.loadConfig(asDirectory)).isInstanceOf(IOException.class);
        }
    }

    @Nested
    class Saving {

        @Test
        void saveWritesFileAndUpdatesCache() throws Exception {
            Path file = dir.resolve("nested/s.properties");
            Properties p = new Properties();
            p.setProperty("a", "1");

            ConfigManager.saveConfig(p, file);

            assertThat(readRaw(file).getProperty("a")).isEqualTo("1");
            assertThat(ConfigManager.loadConfig(file).getProperty("a")).isEqualTo("1");
        }

        // Regression: the cache held a Properties *backed by* the caller's object (as defaults), so
        // mutating it after saving silently changed what loadConfig returned.
        @Test
        void mutatingSavedPropertiesDoesNotLeakIntoCache() throws Exception {
            Path file = dir.resolve("s.properties");
            Properties p = new Properties();
            p.setProperty("a", "1");
            ConfigManager.saveConfig(p, file);

            p.setProperty("a", "mutated");
            p.setProperty("b", "new");

            Properties loaded = ConfigManager.loadConfig(file);
            assertThat(loaded.getProperty("a")).isEqualTo("1");
            assertThat(loaded.getProperty("b")).isNull();
        }

        // Regression: setConfigValue stored only the new key, because the loaded Properties held all
        // other values as non-persisted defaults, wiping the rest of the file.
        @Test
        void setConfigValueKeepsOtherKeys() throws Exception {
            writeProps("x.properties", "keep", "yes", "k", "old");

            ConfigManager.setConfigValue("x.properties", "k", "new");

            Properties onDisk = readRaw(dir.resolve("x.properties"));
            assertThat(onDisk.getProperty("keep")).isEqualTo("yes");
            assertThat(onDisk.getProperty("k")).isEqualTo("new");
            assertThat(ConfigManager.getConfigValue("x.properties", "keep", "default")).isEqualTo("yes");
        }
    }

    @Nested
    class TypedAccessors {

        @BeforeEach
        void props() throws IOException {
            writeProps("t.properties", "int", "42", "badInt", "4x", "bool", "TRUE", "dbl", "2.5", "badDbl", "x");
        }

        @Test
        void stringValuesFallBackToDefault() {
            assertThat(ConfigManager.getConfigValue("t.properties", "int", "d")).isEqualTo("42");
            assertThat(ConfigManager.getConfigValue("t.properties", "missing", "d")).isEqualTo("d");
        }

        @Test
        void numericAndBooleanParsingWithFallback() {
            assertThat(ConfigManager.getConfigValueAsInt("t.properties", "int", 0)).isEqualTo(42);
            assertThat(ConfigManager.getConfigValueAsInt("t.properties", "badInt", 7)).isEqualTo(7);
            assertThat(ConfigManager.getConfigValueAsInt("t.properties", "missing", 9)).isEqualTo(9);
            assertThat(ConfigManager.getConfigValueAsBoolean("t.properties", "bool", false)).isTrue();
            assertThat(ConfigManager.getConfigValueAsBoolean("t.properties", "missing", true)).isTrue();
            assertThat(ConfigManager.getConfigValueAsDouble("t.properties", "dbl", 0)).isEqualTo(2.5);
            assertThat(ConfigManager.getConfigValueAsDouble("t.properties", "badDbl", 1.5)).isEqualTo(1.5);
        }

        @Test
        void ioErrorsYieldDefault() throws Exception {
            Files.createDirectory(dir.resolve("broken.properties"));
            assertThat(ConfigManager.getConfigValue("broken.properties", "k", "fallback")).isEqualTo("fallback");
        }
    }

    @Nested
    class SectionAccessors {

        @Test
        void applicationDefaults() {
            assertThat(ConfigManager.ApplicationConfig.getApplicationName()).isEqualTo("SmartCampus");
            assertThat(ConfigManager.ApplicationConfig.getApplicationVersion()).isEqualTo("1.0.0");
            assertThat(ConfigManager.ApplicationConfig.getMaxThreads()).isEqualTo(10);
            assertThat(ConfigManager.ApplicationConfig.getSessionTimeout()).isEqualTo(3600);
            assertThat(ConfigManager.ApplicationConfig.isDebugEnabled()).isFalse();
            assertThat(ConfigManager.ApplicationConfig.getDataDirectory()).isEqualTo("data");
            assertThat(ConfigManager.ApplicationConfig.getBackupDirectory()).isEqualTo("backup");
            assertThat(ConfigManager.ApplicationConfig.getBackupRetentionDays()).isEqualTo(30);
        }

        @Test
        void databaseValuesComeFromFile() throws Exception {
            writeProps("database.properties", "db.url", "jdbc:h2:mem:x", "db.max.connections", "3",
                    "db.auto.commit", "false");
            assertThat(ConfigManager.DatabaseConfig.getConnectionUrl()).isEqualTo("jdbc:h2:mem:x");
            assertThat(ConfigManager.DatabaseConfig.getUsername()).isEqualTo("sa");
            assertThat(ConfigManager.DatabaseConfig.getPassword()).isEmpty();
            assertThat(ConfigManager.DatabaseConfig.getDriverClass()).isEqualTo("org.h2.Driver");
            assertThat(ConfigManager.DatabaseConfig.getMaxConnections()).isEqualTo(3);
            assertThat(ConfigManager.DatabaseConfig.getConnectionTimeout()).isEqualTo(30000);
            assertThat(ConfigManager.DatabaseConfig.isAutoCommit()).isFalse();
            assertThat(ConfigManager.DatabaseConfig.getBackupPath()).isEqualTo("backup/db");
        }

        @Test
        void loggingDefaults() {
            assertThat(ConfigManager.LoggingConfig.getLogLevel()).isEqualTo("INFO");
            assertThat(ConfigManager.LoggingConfig.getLogDirectory()).isEqualTo("logs");
            assertThat(ConfigManager.LoggingConfig.getLogFileName()).isEqualTo("smartcampus.log");
            assertThat(ConfigManager.LoggingConfig.getMaxFileSize()).isEqualTo(10485760);
            assertThat(ConfigManager.LoggingConfig.getMaxBackupIndex()).isEqualTo(5);
            assertThat(ConfigManager.LoggingConfig.isConsoleLoggingEnabled()).isTrue();
            assertThat(ConfigManager.LoggingConfig.getLogPattern()).contains("%msg");
        }

        // Regression: was always empty because putAll() ignores the defaults chain of the copies.
        @Test
        void allConfigurationsMergesTheThreeStandardFiles() throws Exception {
            Properties all = ConfigManager.getAllConfigurations();

            assertThat(all.getProperty("app.name")).isEqualTo("SmartCampus");
            assertThat(all.getProperty("db.driver")).isEqualTo("org.h2.Driver");
            assertThat(all.getProperty("log.level")).isEqualTo("INFO");
            assertThat(all).hasSize(29);
        }

        @Test
        void allConfigurationsToleratesUnreadableFiles() throws Exception {
            Files.createDirectory(dir.resolve("application.properties"));
            Properties all = ConfigManager.getAllConfigurations();
            assertThat(all.getProperty("app.name")).isNull();
            assertThat(all.getProperty("db.driver")).isEqualTo("org.h2.Driver");
        }
    }

    @Nested
    class Validation {

        @Test
        void defaultFilesAreValidButWarnAboutMissingDirectories() throws Exception {
            ConfigManager.loadApplicationConfig();
            ConfigManager.loadDatabaseConfig();
            ConfigManager.loadLoggingConfig();

            ConfigManager.ConfigValidationResult db = ConfigManager.validateConfig(dir.resolve("database.properties"));
            assertThat(db.isValid()).isTrue();
            assertThat(db.hasWarnings()).isFalse();
            assertThat(db).hasToString("ConfigValidationResult{valid=true, errors=0, warnings=0}");

            ConfigManager.ConfigValidationResult log = ConfigManager.validateConfig(dir.resolve("logging.properties"));
            assertThat(log.isValid()).isTrue();
            assertThat(log.getErrors()).isEmpty();

            ConfigManager.ConfigValidationResult app = ConfigManager.validateConfig(dir.resolve("application.properties"));
            assertThat(app.isValid()).isTrue();
        }

        @Test
        void applicationProblemsAreReported() throws Exception {
            Path file = writeProps("application.properties", "app.name", " ", "app.max.threads", "500",
                    "app.session.timeout", "soon", "app.data.directory", dir.resolve("missing").toString(),
                    "app.backup.directory", writeProps("plain.txt").toString());

            ConfigManager.ConfigValidationResult r = ConfigManager.validateConfig(file);

            assertThat(r.isValid()).isFalse();
            assertThat(r.hasErrors()).isTrue();
            assertThat(r.getErrors()).containsExactlyInAnyOrder(
                    "Required property missing: app.name",
                    "Required property missing: app.version",
                    "Property app.session.timeout must be a valid integer: soon");
            assertThat(r.getWarnings()).hasSize(3)
                    .anySatisfy(w -> assertThat(w).startsWith("Property app.max.threads value 500 is outside"))
                    .anySatisfy(w -> assertThat(w).startsWith("Directory does not exist"))
                    .anySatisfy(w -> assertThat(w).startsWith("Path is not a directory"));
        }

        @Test
        void loggingAndDatabaseProblemsAreReported() throws Exception {
            Path log = writeProps("logging.properties", "log.level", "verbose", "log.directory", "logs",
                    "log.max.backup.index", "0");
            ConfigManager.ConfigValidationResult lr = ConfigManager.validateConfig(log);
            assertThat(lr.isValid()).isTrue();
            assertThat(lr.getWarnings()).hasSize(2).contains("Invalid log level: verbose");

            Path db = writeProps("database.properties", "db.url", "u");
            ConfigManager.ConfigValidationResult dr = ConfigManager.validateConfig(db);
            assertThat(dr.getErrors()).containsExactly("Required property missing: db.driver");
        }

        @Test
        void missingAndUnreadableFiles() throws Exception {
            ConfigManager.ConfigValidationResult missing = ConfigManager.validateConfig(dir.resolve("none.properties"));
            assertThat(missing.isValid()).isFalse();
            assertThat(missing.getErrors()).singleElement().asString().startsWith("Configuration file does not exist");

            Path broken = Files.createDirectory(dir.resolve("b.properties"));
            assertThat(ConfigManager.validateConfig(broken).getErrors())
                    .singleElement().asString().startsWith("Configuration validation error");
        }
    }

    @Nested
    class Listeners {

        @Test
        void listenersAreNotifiedOnLoadAndSaveUntilRemoved() throws Exception {
            Path file = dir.resolve("l.properties");
            List<String> events = new ArrayList<>();
            ConfigManager.ConfigChangeListener listener = (path, props) -> events.add(path + "=" + props.getProperty("k"));
            listen(file, listener);

            Properties p = new Properties();
            p.setProperty("k", "1");
            ConfigManager.saveConfig(p, file);
            ConfigManager.loadConfig(file); // cached, unchanged: no event

            ConfigManager.removeConfigChangeListener(file.toString(), listener);
            p.setProperty("k", "2");
            ConfigManager.saveConfig(p, file);
            ConfigManager.removeConfigChangeListener("never-registered", listener);

            assertThat(events).containsExactly(file + "=1");
        }

        @Test
        void throwingListenerDoesNotBreakOthers() throws Exception {
            Path file = dir.resolve("l.properties");
            List<String> events = new ArrayList<>();
            listen(file, (path, props) -> { throw new IllegalStateException("boom"); });
            listen(file, (path, props) -> events.add("second"));

            ConfigManager.saveConfig(new Properties(), file);

            assertThat(events).containsExactly("second");
        }

        // Regression: listeners were kept in a plain ArrayList that was iterated while notifying, so a
        // listener unregistering itself (one-shot listener) made the iteration skip the next listener
        // (or throw ConcurrentModificationException out of saveConfig/loadConfig).
        @Test
        void listenerMayUnregisterItselfDuringNotification() throws Exception {
            Path file = dir.resolve("l.properties");
            List<String> events = new ArrayList<>();
            ConfigManager.ConfigChangeListener oneShot = new ConfigManager.ConfigChangeListener() {
                @Override
                public void onConfigChange(String path, Properties props) {
                    events.add("once");
                    ConfigManager.removeConfigChangeListener(path, this);
                }
            };
            listen(file, oneShot);
            listen(file, (path, props) -> events.add("other"));

            ConfigManager.saveConfig(new Properties(), file);
            ConfigManager.saveConfig(new Properties(), file);

            assertThat(events).containsExactly("once", "other", "other");
        }
    }
}
