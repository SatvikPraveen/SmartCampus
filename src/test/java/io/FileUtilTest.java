package io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilTest {

    @TempDir
    Path dir;

    private Path originalBase;

    @BeforeEach
    void redirectBaseDirectory() {
        originalBase = FileUtil.getBaseDirectory();
        FileUtil.setBaseDirectory(dir);
    }

    @AfterEach
    void restoreBaseDirectory() {
        FileUtil.setBaseDirectory(originalBase);
    }

    private Path file(String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        return p;
    }

    private static List<String> names(List<Path> paths) {
        return paths.stream().map(p -> p.getFileName().toString()).toList();
    }

    // Regression: loading FileUtil used to create data/, backup/ and temp/ in the working directory
    // as a static-initializer side effect (and failed class loading where that was not writable).
    @Test
    void defaultBaseDirectoryIsWorkingDirectoryAndNothingIsCreatedEagerly() {
        assertThat(originalBase).isEqualTo(Paths.get(""));
        assertThat(Paths.get("data")).doesNotExist();
        assertThat(Paths.get("temp")).doesNotExist();
        assertThatThrownBy(() -> FileUtil.setBaseDirectory(null)).isInstanceOf(NullPointerException.class);
    }

    @Nested
    class ReadWrite {

        @Test
        void writeAndReadLinesCreatesParents() throws Exception {
            Path p = dir.resolve("a/b/c.txt");
            FileUtil.writeLines(p, List.of("one", "two"));
            FileUtil.appendLines(p, List.of("three"));

            assertThat(FileUtil.readAllLines(p)).containsExactly("one", "two", "three");
            assertThat(FileUtil.readAllLines(p, "UTF-8")).containsExactly("one", "two", "three");

            FileUtil.writeLines(p, List.of("only"));
            assertThat(FileUtil.readAllLines(p)).containsExactly("only");
        }

        @Test
        void appendCreatesMissingFile() throws Exception {
            Path p = dir.resolve("new/log.txt");
            FileUtil.appendLines(p, List.of("x"));
            assertThat(FileUtil.readAllLines(p)).containsExactly("x");
        }

        @Test
        void stringRoundTripWithCustomCharset() throws Exception {
            Path p = dir.resolve("s/u.txt");
            FileUtil.writeStringToFile(p, "héllo\nwörld");
            assertThat(FileUtil.readFileAsString(p)).isEqualTo("héllo\nwörld");

            Files.write(p, List.of("é"), StandardCharsets.ISO_8859_1);
            assertThat(FileUtil.readAllLines(p, "ISO-8859-1")).containsExactly("é");
        }

        @Test
        void readingMissingFilesThrowsFileNotFound() {
            Path missing = dir.resolve("missing.txt");
            assertThatThrownBy(() -> FileUtil.readAllLines(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.readAllLines(missing, "UTF-8")).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.readFileAsString(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.getFileSize(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.getFileCreationTime(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.getFileModifiedTime(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.getFileInfo(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.createBackup(missing)).isInstanceOf(FileNotFoundException.class);
            assertThatThrownBy(() -> FileUtil.readLargeFile(missing, 16, l -> { }))
                    .isInstanceOf(FileNotFoundException.class);
        }

        @Test
        void largeFileStreamingRoundTrip() throws Exception {
            Path p = dir.resolve("big/large.txt");
            FileUtil.writeLargeFile(p, Stream.of("a", "b", "c"));

            List<String> seen = new ArrayList<>();
            FileUtil.readLargeFile(p, 8192, seen::add);
            assertThat(seen).containsExactly("a", "b", "c");
        }
    }

    @Nested
    class CopyMoveDelete {

        @Test
        void copyReplacesExistingAndCreatesParents() throws Exception {
            Path src = file("src.txt", "new");
            Path dst = file("x/dst.txt", "old");

            FileUtil.copyFile(src, dst);
            FileUtil.copyFile(src, dir.resolve("y/z/copy.txt"));

            assertThat(dst).hasContent("new");
            assertThat(dir.resolve("y/z/copy.txt")).hasContent("new");
            assertThat(src).exists();
        }

        @Test
        void moveRemovesSource() throws Exception {
            Path src = file("m.txt", "data");
            Path dst = dir.resolve("moved/m.txt");

            FileUtil.moveFile(src, dst);

            assertThat(src).doesNotExist();
            assertThat(dst).hasContent("data");
        }

        @Test
        void deleteHandlesFilesDirectoriesAndMissingPaths() throws Exception {
            Path f = file("del/f.txt", "x");
            file("del/sub/g.txt", "y");

            assertThat(FileUtil.deleteFile(f)).isTrue();
            assertThat(FileUtil.deleteFile(dir.resolve("del"))).isTrue();
            assertThat(dir.resolve("del")).doesNotExist();
            assertThat(FileUtil.deleteFile(dir.resolve("del"))).isFalse();
            FileUtil.deleteDirectoryRecursively(dir.resolve("never"));
        }
    }

    @Nested
    class Listing {

        @BeforeEach
        void tree() throws Exception {
            file("t/a.csv", "1");
            file("t/B.JSON", "22");
            file("t/c.txt", "333");
            file("t/sub/d.csv", "4444");
        }

        @Test
        void listFilesIsShallowAndFiltersCaseInsensitively() throws Exception {
            assertThat(names(FileUtil.listFiles(dir.resolve("t"))))
                    .containsExactlyInAnyOrder("a.csv", "B.JSON", "c.txt");
            assertThat(names(FileUtil.listFiles(dir.resolve("t"), ".CSV", ".json")))
                    .containsExactlyInAnyOrder("a.csv", "B.JSON");
            assertThat(FileUtil.listFiles(dir.resolve("nope"))).isEmpty();
            assertThat(FileUtil.listFiles(dir.resolve("t/a.csv"))).isEmpty();
        }

        @Test
        void listFilesRecursivelyDescends() throws Exception {
            assertThat(names(FileUtil.listFilesRecursively(dir.resolve("t"), ".csv")))
                    .containsExactlyInAnyOrder("a.csv", "d.csv");
            assertThat(FileUtil.listFilesRecursively(dir.resolve("t"))).hasSize(4);
            assertThat(FileUtil.listFilesRecursively(dir.resolve("nope"))).isEmpty();
        }

        @Test
        void findByGlobMatchesFileNames() throws Exception {
            assertThat(names(FileUtil.findFilesByPattern(dir.resolve("t"), "*.csv")))
                    .containsExactlyInAnyOrder("a.csv", "d.csv");
            assertThat(FileUtil.findFilesByPattern(dir.resolve("nope"), "*")).isEmpty();
        }

        @Test
        void sizes() throws Exception {
            assertThat(FileUtil.getFileSize(dir.resolve("t/c.txt"))).isEqualTo(3);
            assertThat(FileUtil.getDirectorySize(dir.resolve("t"))).isEqualTo(10);
            assertThat(FileUtil.getDirectorySize(dir.resolve("nope"))).isZero();
        }

        @Test
        void existenceChecksDistinguishFilesAndDirectories() {
            assertThat(FileUtil.fileExists(dir.resolve("t/a.csv"))).isTrue();
            assertThat(FileUtil.fileExists(dir.resolve("t"))).isFalse();
            assertThat(FileUtil.directoryExists(dir.resolve("t"))).isTrue();
            assertThat(FileUtil.directoryExists(dir.resolve("t/a.csv"))).isFalse();
            assertThat(FileUtil.directoryExists(dir.resolve("nope"))).isFalse();
        }

        @Test
        void createDirectoriesIsIdempotentAndNullSafe() throws Exception {
            FileUtil.createDirectoriesIfNotExists(null);
            FileUtil.createDirectoriesIfNotExists(dir.resolve("n/e/w"));
            FileUtil.createDirectoriesIfNotExists(dir.resolve("n/e/w"));
            assertThat(dir.resolve("n/e/w")).isDirectory();
        }
    }

    @Nested
    class Metadata {

        @Test
        void timesAndFileInfo() throws Exception {
            Path p = file("info.txt", "hello");
            Instant modified = Instant.parse("2020-05-01T10:15:30Z");
            Files.setLastModifiedTime(p, FileTime.from(modified));

            LocalDateTime expected = LocalDateTime.ofInstant(modified, java.time.ZoneId.systemDefault());
            assertThat(FileUtil.getFileModifiedTime(p)).isEqualTo(expected);
            assertThat(FileUtil.getFileCreationTime(p)).isNotNull();

            FileUtil.FileInfo info = FileUtil.getFileInfo(p);
            assertThat(info.getPath()).isEqualTo(p.toString());
            assertThat(info.getSize()).isEqualTo(5);
            assertThat(info.isFile()).isTrue();
            assertThat(info.isDirectory()).isFalse();
            assertThat(info.getModifiedTime()).isEqualTo(expected);
            assertThat(info.getCreationTime()).isNotNull();
            assertThat(info.toString()).contains("size=5 B", "isFile=true");

            assertThat(FileUtil.getFileInfo(dir).isDirectory()).isTrue();
        }

        @Test
        void formattedSizeUsesBinaryUnits() {
            assertThat(info(1023).getFormattedSize()).isEqualTo("1023 B");
            assertThat(info(1536).getFormattedSize()).isEqualTo(String.format("%.1f KB", 1.5));
            assertThat(info(5L * 1024 * 1024).getFormattedSize()).isEqualTo(String.format("%.1f MB", 5.0));
            assertThat(info(3L * 1024 * 1024 * 1024).getFormattedSize()).isEqualTo(String.format("%.1f GB", 3.0));
        }

        private FileUtil.FileInfo info(long size) {
            return new FileUtil.FileInfo("p", size, true, false, null, null);
        }
    }

    @Nested
    class Archives {

        @Test
        void zipRoundTripSkipsMissingAndDirectoryEntries() throws Exception {
            Path a = file("in/a.txt", "alpha");
            Path b = file("in/b.txt", "beta");
            Path zip = dir.resolve("out/archive.zip");

            FileUtil.createZipArchive(List.of(a, dir.resolve("in/missing"), dir.resolve("in"), b), zip);
            FileUtil.extractZipArchive(zip, dir.resolve("extracted"));

            assertThat(dir.resolve("extracted/a.txt")).hasContent("alpha");
            assertThat(dir.resolve("extracted/b.txt")).hasContent("beta");
            try (var files = Files.list(dir.resolve("extracted"))) {
                assertThat(files).hasSize(2);
            }
        }

        @Test
        void extractCreatesDirectoryEntriesAndNestedFiles() throws Exception {
            Path zip = dir.resolve("nested.zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
                zos.putNextEntry(new ZipEntry("folder/"));
                zos.closeEntry();
                zos.putNextEntry(new ZipEntry("deep/er/file.txt"));
                zos.write("x".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            FileUtil.extractZipArchive(zip, dir.resolve("dest"));

            assertThat(dir.resolve("dest/folder")).isDirectory();
            assertThat(dir.resolve("dest/deep/er/file.txt")).hasContent("x");
        }

        @Test
        void extractMissingArchiveThrows() {
            assertThatThrownBy(() -> FileUtil.extractZipArchive(dir.resolve("none.zip"), dir.resolve("d")))
                    .isInstanceOf(FileNotFoundException.class);
        }

        // Regression: entry names were resolved unchecked, so "../x" entries escaped the
        // destination directory ("zip slip").
        @Test
        void extractRejectsEntriesEscapingDestination() throws Exception {
            Path zip = dir.resolve("evil.zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
                zos.putNextEntry(new ZipEntry("../escaped.txt"));
                zos.write("pwned".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            assertThatThrownBy(() -> FileUtil.extractZipArchive(zip, dir.resolve("dest")))
                    .isInstanceOf(IOException.class).hasMessageContaining("outside");
            assertThat(dir.resolve("escaped.txt")).doesNotExist();
        }
    }

    @Nested
    class BaseDirectoryResources {

        @Test
        void backupIsCopiedIntoBackupDirectoryUnderBase() throws Exception {
            Path original = file("doc.txt", "content");

            Path backup = FileUtil.createBackup(original);

            assertThat(backup.getParent()).isEqualTo(dir.resolve("backup"));
            assertThat(backup.getFileName().toString()).matches("doc\\.txt_backup_\\d{8}_\\d{6}");
            assertThat(backup).hasContent("content");
        }

        @Test
        void tempFilesAreCreatedUnderBaseAndOldOnesCleanedUp() throws Exception {
            Path fresh = FileUtil.createTempFile("pre", ".tmp");
            Path old = FileUtil.createTempFile("old", ".tmp");
            Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));

            assertThat(fresh.getParent()).isEqualTo(dir.resolve("temp"));
            assertThat(fresh.getFileName().toString()).startsWith("pre").endsWith(".tmp");

            FileUtil.cleanupTempFiles(5);

            assertThat(fresh).exists();
            assertThat(old).doesNotExist();
        }

        @Test
        void cleanupWithoutTempDirectoryIsNoOp() throws Exception {
            FileUtil.cleanupTempFiles(1);
            assertThat(dir.resolve("temp")).doesNotExist();
        }
    }
}
