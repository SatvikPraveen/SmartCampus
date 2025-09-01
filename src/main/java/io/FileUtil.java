// File location: src/main/java/io/FileUtil.java

package io;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for file operations using NIO.2, Path, and Files
 * Provides comprehensive file handling capabilities for the SmartCampus system
 */
public class FileUtil {
    
    private static final String DATA_DIRECTORY = "data";
    private static final String BACKUP_DIRECTORY = "backup";
    private static final String TEMP_DIRECTORY = "temp";
    
    // Initialize directories on class load
    static {
        initializeDirectories();
    }
    
    /**
     * Initialize required directories
     */
    private static void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(DATA_DIRECTORY));
            Files.createDirectories(Paths.get(BACKUP_DIRECTORY));
            Files.createDirectories(Paths.get(TEMP_DIRECTORY));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize directories", e);
        }
    }
    
    /**
     * Read all lines from a file
     */
    public static List<String> readAllLines(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    }
    
    /**
     * Read all lines from a file with custom charset
     */
    public static List<String> readAllLines(Path filePath, String charset) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        return Files.readAllLines(filePath, java.nio.charset.Charset.forName(charset));
    }
    
    /**
     * Write lines to a file
     */
    public static void writeLines(Path filePath, List<String> lines) throws IOException {
        createDirectoriesIfNotExists(filePath.getParent());
        Files.write(filePath, lines, StandardCharsets.UTF_8, 
                   StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Append lines to a file
     */
    public static void appendLines(Path filePath, List<String> lines) throws IOException {
        createDirectoriesIfNotExists(filePath.getParent());
        Files.write(filePath, lines, StandardCharsets.UTF_8, 
                   StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    
    /**
     * Read file content as string
     */
    public static String readFileAsString(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
    
    /**
     * Write string content to file
     */
    public static void writeStringToFile(Path filePath, String content) throws IOException {
        createDirectoriesIfNotExists(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8,
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Copy file from source to destination
     */
    public static void copyFile(Path source, Path destination) throws IOException {
        createDirectoriesIfNotExists(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Move file from source to destination
     */
    public static void moveFile(Path source, Path destination) throws IOException {
        createDirectoriesIfNotExists(destination.getParent());
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Delete file or directory
     */
    public static boolean deleteFile(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            if (Files.isDirectory(filePath)) {
                deleteDirectoryRecursively(filePath);
            } else {
                Files.delete(filePath);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Delete directory recursively
     */
    public static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    /**
     * List all files in directory with optional file extension filter
     */
    public static List<Path> listFiles(Path directory, String... extensions) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Collections.emptyList();
        }
        
        Set<String> extensionSet = Arrays.stream(extensions)
                .map(ext -> ext.toLowerCase())
                .collect(Collectors.toSet());
        
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                       .filter(path -> {
                           if (extensionSet.isEmpty()) return true;
                           String fileName = path.getFileName().toString().toLowerCase();
                           return extensionSet.stream().anyMatch(fileName::endsWith);
                       })
                       .collect(Collectors.toList());
        }
    }
    
    /**
     * List all files in directory tree recursively
     */
    public static List<Path> listFilesRecursively(Path directory, String... extensions) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Collections.emptyList();
        }
        
        Set<String> extensionSet = Arrays.stream(extensions)
                .map(ext -> ext.toLowerCase())
                .collect(Collectors.toSet());
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                       .filter(path -> {
                           if (extensionSet.isEmpty()) return true;
                           String fileName = path.getFileName().toString().toLowerCase();
                           return extensionSet.stream().anyMatch(fileName::endsWith);
                       })
                       .collect(Collectors.toList());
        }
    }
    
    /**
     * Get file size in bytes
     */
    public static long getFileSize(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        return Files.size(filePath);
    }
    
    /**
     * Get directory size in bytes
     */
    public static long getDirectorySize(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return 0;
        }
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                       .mapToLong(path -> {
                           try {
                               return Files.size(path);
                           } catch (IOException e) {
                               return 0;
                           }
                       })
                       .sum();
        }
    }
    
    /**
     * Check if file exists
     */
    public static boolean fileExists(Path filePath) {
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }
    
    /**
     * Check if directory exists
     */
    public static boolean directoryExists(Path directory) {
        return Files.exists(directory) && Files.isDirectory(directory);
    }
    
    /**
     * Create directories if they don't exist
     */
    public static void createDirectoriesIfNotExists(Path directory) throws IOException {
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }
    
    /**
     * Create backup of file with timestamp
     */
    public static Path createBackup(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = filePath.getFileName().toString();
        String backupFileName = fileName + "_backup_" + timestamp;
        
        Path backupPath = Paths.get(BACKUP_DIRECTORY, backupFileName);
        copyFile(filePath, backupPath);
        
        return backupPath;
    }
    
    /**
     * Find files by name pattern
     */
    public static List<Path> findFilesByPattern(Path directory, String pattern) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return Collections.emptyList();
        }
        
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                       .filter(path -> matcher.matches(path.getFileName()))
                       .collect(Collectors.toList());
        }
    }
    
    /**
     * Get file creation time
     */
    public static java.time.LocalDateTime getFileCreationTime(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        return java.time.LocalDateTime.ofInstant(
            attrs.creationTime().toInstant(), 
            java.time.ZoneId.systemDefault()
        );
    }
    
    /**
     * Get file last modified time
     */
    public static java.time.LocalDateTime getFileModifiedTime(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        return java.time.LocalDateTime.ofInstant(
            attrs.lastModifiedTime().toInstant(), 
            java.time.ZoneId.systemDefault()
        );
    }
    
    /**
     * Compress files into ZIP archive
     */
    public static void createZipArchive(List<Path> files, Path zipFilePath) throws IOException {
        createDirectoriesIfNotExists(zipFilePath.getParent());
        
        try (ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(zipFilePath, StandardOpenOption.CREATE, 
                                    StandardOpenOption.TRUNCATE_EXISTING))) {
            
            for (Path file : files) {
                if (Files.exists(file) && Files.isRegularFile(file)) {
                    ZipEntry entry = new ZipEntry(file.getFileName().toString());
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                }
            }
        }
    }
    
    /**
     * Extract ZIP archive
     */
    public static void extractZipArchive(Path zipFilePath, Path destinationDirectory) throws IOException {
        if (!Files.exists(zipFilePath)) {
            throw new FileNotFoundException("ZIP file not found: " + zipFilePath);
        }
        
        createDirectoriesIfNotExists(destinationDirectory);
        
        try (ZipInputStream zis = new ZipInputStream(
                Files.newInputStream(zipFilePath, StandardOpenOption.READ))) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outputPath = destinationDirectory.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    createDirectoriesIfNotExists(outputPath);
                } else {
                    createDirectoriesIfNotExists(outputPath.getParent());
                    Files.copy(zis, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Read file with custom buffer size for large files
     */
    public static void readLargeFile(Path filePath, int bufferSize, 
                                   java.util.function.Consumer<String> lineProcessor) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineProcessor.accept(line);
            }
        }
    }
    
    /**
     * Write large amounts of data efficiently
     */
    public static void writeLargeFile(Path filePath, Stream<String> lines) throws IOException {
        createDirectoriesIfNotExists(filePath.getParent());
        
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            
            lines.forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    throw new RuntimeException("Error writing line", e);
                }
            });
        }
    }
    
    /**
     * Get temporary file path
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(Paths.get(TEMP_DIRECTORY), prefix, suffix);
    }
    
    /**
     * Clean up temporary files older than specified days
     */
    public static void cleanupTempFiles(int daysOld) throws IOException {
        Path tempDir = Paths.get(TEMP_DIRECTORY);
        if (!Files.exists(tempDir)) {
            return;
        }
        
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        
        try (Stream<Path> paths = Files.list(tempDir)) {
            paths.filter(Files::isRegularFile)
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
                     } catch (IOException e) {
                         System.err.println("Failed to delete temp file: " + path);
                     }
                 });
        }
    }
    
    /**
     * Get file information
     */
    public static FileInfo getFileInfo(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        
        return new FileInfo(
            filePath.toString(),
            attrs.size(),
            attrs.isRegularFile(),
            attrs.isDirectory(),
            java.time.LocalDateTime.ofInstant(attrs.creationTime().toInstant(), 
                                            java.time.ZoneId.systemDefault()),
            java.time.LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), 
                                            java.time.ZoneId.systemDefault())
        );
    }
    
    /**
     * File information class
     */
    public static class FileInfo {
        private final String path;
        private final long size;
        private final boolean isFile;
        private final boolean isDirectory;
        private final java.time.LocalDateTime creationTime;
        private final java.time.LocalDateTime modifiedTime;
        
        public FileInfo(String path, long size, boolean isFile, boolean isDirectory,
                       java.time.LocalDateTime creationTime, java.time.LocalDateTime modifiedTime) {
            this.path = path;
            this.size = size;
            this.isFile = isFile;
            this.isDirectory = isDirectory;
            this.creationTime = creationTime;
            this.modifiedTime = modifiedTime;
        }
        
        // Getters
        public String getPath() { return path; }
        public long getSize() { return size; }
        public boolean isFile() { return isFile; }
        public boolean isDirectory() { return isDirectory; }
        public java.time.LocalDateTime getCreationTime() { return creationTime; }
        public java.time.LocalDateTime getModifiedTime() { return modifiedTime; }
        
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
        
        @Override
        public String toString() {
            return String.format("FileInfo{path='%s', size=%s, isFile=%s, isDirectory=%s, " +
                               "created=%s, modified=%s}",
                               path, getFormattedSize(), isFile, isDirectory, 
                               creationTime, modifiedTime);
        }
    }
}