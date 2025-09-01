// File location: src/main/java/io/BackupManager.java

package io;

import models.*;
import repositories.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Manages data backup and restore operations for the SmartCampus system
 * Provides comprehensive backup solutions including incremental and full backups
 */
public class BackupManager {
    
    private static final String BACKUP_BASE_DIR = "backup";
    private static final String FULL_BACKUP_DIR = "full";
    private static final String INCREMENTAL_BACKUP_DIR = "incremental";
    private static final String DATABASE_BACKUP_DIR = "database";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DatabaseManager databaseManager;
    private final ExecutorService executorService;
    
    public BackupManager(StudentRepository studentRepository,
                        ProfessorRepository professorRepository,
                        CourseRepository courseRepository,
                        DepartmentRepository departmentRepository,
                        EnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.professorRepository = professorRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.databaseManager = DatabaseManager.getInstance();
        this.executorService = Executors.newFixedThreadPool(4);
        
        initializeBackupDirectories();
    }
    
    /**
     * Initialize backup directories
     */
    private void initializeBackupDirectories() {
        try {
            Files.createDirectories(Paths.get(BACKUP_BASE_DIR, FULL_BACKUP_DIR));
            Files.createDirectories(Paths.get(BACKUP_BASE_DIR, INCREMENTAL_BACKUP_DIR));
            Files.createDirectories(Paths.get(BACKUP_BASE_DIR, DATABASE_BACKUP_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize backup directories", e);
        }
    }
    
    /**
     * Create full backup of all data
     */
    public CompletableFuture<BackupResult> createFullBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                String backupName = "full_backup_" + timestamp;
                Path backupDir = Paths.get(BACKUP_BASE_DIR, FULL_BACKUP_DIR, backupName);
                Files.createDirectories(backupDir);
                
                long startTime = System.currentTimeMillis();
                
                // Create concurrent backup tasks for each entity type
                List<CompletableFuture<BackupEntityResult>> backupTasks = Arrays.asList(
                    backupStudentsAsync(backupDir),
                    backupProfessorsAsync(backupDir),
                    backupCoursesAsync(backupDir),
                    backupDepartmentsAsync(backupDir),
                    backupEnrollmentsAsync(backupDir)
                );
                
                // Wait for all backups to complete
                CompletableFuture.allOf(backupTasks.toArray(new CompletableFuture[0])).join();
                
                // Collect results
                Map<String, BackupEntityResult> entityResults = new HashMap<>();
                for (CompletableFuture<BackupEntityResult> task : backupTasks) {
                    BackupEntityResult result = task.join();
                    entityResults.put(result.getEntityType(), result);
                }
                
                // Create backup metadata
                BackupMetadata metadata = new BackupMetadata(
                    backupName,
                    BackupType.FULL,
                    timestamp,
                    entityResults,
                    System.currentTimeMillis() - startTime
                );
                
                // Save metadata
                Path metadataFile = backupDir.resolve("backup_metadata.json");
                JsonProcessor.writeToJsonFile(metadata, metadataFile);
                
                // Create compressed archive
                Path archivePath = createBackupArchive(backupDir, backupName);
                
                // Cleanup uncompressed backup directory
                FileUtil.deleteDirectoryRecursively(backupDir);
                
                return new BackupResult(true, "Full backup completed successfully", 
                                      archivePath, metadata);
                
            } catch (Exception e) {
                return new BackupResult(false, "Full backup failed: " + e.getMessage(), 
                                      null, null);
            }
        }, executorService);
    }
    
    /**
     * Create incremental backup (only changed data since last backup)
     */
    public CompletableFuture<BackupResult> createIncrementalBackup(Date sinceDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                String backupName = "incremental_backup_" + timestamp;
                Path backupDir = Paths.get(BACKUP_BASE_DIR, INCREMENTAL_BACKUP_DIR, backupName);
                Files.createDirectories(backupDir);
                
                long startTime = System.currentTimeMillis();
                
                // Create incremental backup tasks
                List<CompletableFuture<BackupEntityResult>> backupTasks = Arrays.asList(
                    backupStudentsIncrementalAsync(backupDir, sinceDate),
                    backupEnrollmentsIncrementalAsync(backupDir, sinceDate)
                    // Add other incremental backups as needed
                );
                
                // Wait for all backups to complete
                CompletableFuture.allOf(backupTasks.toArray(new CompletableFuture[0])).join();
                
                // Collect results
                Map<String, BackupEntityResult> entityResults = new HashMap<>();
                for (CompletableFuture<BackupEntityResult> task : backupTasks) {
                    BackupEntityResult result = task.join();
                    entityResults.put(result.getEntityType(), result);
                }
                
                // Create backup metadata
                BackupMetadata metadata = new BackupMetadata(
                    backupName,
                    BackupType.INCREMENTAL,
                    timestamp,
                    entityResults,
                    System.currentTimeMillis() - startTime
                );
                metadata.setBasedOnDate(sinceDate);
                
                // Save metadata
                Path metadataFile = backupDir.resolve("backup_metadata.json");
                JsonProcessor.writeToJsonFile(metadata, metadataFile);
                
                // Create compressed archive
                Path archivePath = createBackupArchive(backupDir, backupName);
                
                // Cleanup uncompressed backup directory
                FileUtil.deleteDirectoryRecursively(backupDir);
                
                return new BackupResult(true, "Incremental backup completed successfully", 
                                      archivePath, metadata);
                
            } catch (Exception e) {
                return new BackupResult(false, "Incremental backup failed: " + e.getMessage(), 
                                      null, null);
            }
        }, executorService);
    }
    
    /**
     * Create database backup
     */
    public CompletableFuture<BackupResult> createDatabaseBackup() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                String backupName = "db_backup_" + timestamp + ".sql";
                Path backupPath = Paths.get(BACKUP_BASE_DIR, DATABASE_BACKUP_DIR, backupName);
                
                long startTime = System.currentTimeMillis();
                
                // Create database backup
                databaseManager.backupDatabase(backupPath.toString());
                
                long duration = System.currentTimeMillis() - startTime;
                long fileSize = Files.size(backupPath);
                
                BackupMetadata metadata = new BackupMetadata(
                    backupName,
                    BackupType.DATABASE,
                    timestamp,
                    new HashMap<>(),
                    duration
                );
                
                return new BackupResult(true, 
                    String.format("Database backup completed successfully (%d bytes, %d ms)", 
                                fileSize, duration), 
                    backupPath, metadata);
                
            } catch (Exception e) {
                return new BackupResult(false, "Database backup failed: " + e.getMessage(), 
                                      null, null);
            }
        }, executorService);
    }
    
    /**
     * Restore data from backup
     */
    public CompletableFuture<RestoreResult> restoreFromBackup(Path backupPath, RestoreOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Extract backup if it's compressed
                Path extractedDir = extractBackupArchive(backupPath);
                
                // Load backup metadata
                Path metadataFile = extractedDir.resolve("backup_metadata.json");
                BackupMetadata metadata = JsonProcessor.readFromJsonFile(metadataFile, BackupMetadata.class);
                
                Map<String, RestoreEntityResult> restoreResults = new HashMap<>();
                
                // Restore each entity type based on options
                if (options.isRestoreStudents()) {
                    RestoreEntityResult result = restoreStudents(extractedDir);
                    restoreResults.put("students", result);
                }
                
                if (options.isRestoreProfessors()) {
                    RestoreEntityResult result = restoreProfessors(extractedDir);
                    restoreResults.put("professors", result);
                }
                
                if (options.isRestoreCourses()) {
                    RestoreEntityResult result = restoreCourses(extractedDir);
                    restoreResults.put("courses", result);
                }
                
                if (options.isRestoreDepartments()) {
                    RestoreEntityResult result = restoreDepartments(extractedDir);
                    restoreResults.put("departments", result);
                }
                
                if (options.isRestoreEnrollments()) {
                    RestoreEntityResult result = restoreEnrollments(extractedDir);
                    restoreResults.put("enrollments", result);
                }
                
                // Cleanup extracted directory
                FileUtil.deleteDirectoryRecursively(extractedDir);
                
                long duration = System.currentTimeMillis() - startTime;
                
                return new RestoreResult(true, "Restore completed successfully", 
                                       restoreResults, duration, metadata);
                
            } catch (Exception e) {
                return new RestoreResult(false, "Restore failed: " + e.getMessage(), 
                                       new HashMap<>(), 0, null);
            }
        }, executorService);
    }
    
    /**
     * List available backups
     */
    public List<BackupInfo> listAvailableBackups() throws IOException {
        List<BackupInfo> backups = new ArrayList<>();
        
        // List full backups
        Path fullBackupDir = Paths.get(BACKUP_BASE_DIR, FULL_BACKUP_DIR);
        if (Files.exists(fullBackupDir)) {
            backups.addAll(listBackupsInDirectory(fullBackupDir, BackupType.FULL));
        }
        
        // List incremental backups
        Path incrementalBackupDir = Paths.get(BACKUP_BASE_DIR, INCREMENTAL_BACKUP_DIR);
        if (Files.exists(incrementalBackupDir)) {
            backups.addAll(listBackupsInDirectory(incrementalBackupDir, BackupType.INCREMENTAL));
        }
        
        // List database backups
        Path databaseBackupDir = Paths.get(BACKUP_BASE_DIR, DATABASE_BACKUP_DIR);
        if (Files.exists(databaseBackupDir)) {
            backups.addAll(listBackupsInDirectory(databaseBackupDir, BackupType.DATABASE));
        }
        
        // Sort by creation date (newest first)
        backups.sort((b1, b2) -> b2.getCreationDate().compareTo(b1.getCreationDate()));
        
        return backups;
    }
    
    /**
     * Delete old backups based on retention policy
     */
    public BackupCleanupResult cleanupOldBackups(int retentionDays) {
        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
        int deletedCount = 0;
        long freedSpace = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            List<BackupInfo> allBackups = listAvailableBackups();
            
            for (BackupInfo backup : allBackups) {
                if (backup.getCreationDate().getTime() < cutoffTime) {
                    try {
                        long fileSize = Files.size(backup.getPath());
                        Files.delete(backup.getPath());
                        deletedCount++;
                        freedSpace += fileSize;
                    } catch (IOException e) {
                        errors.add("Failed to delete backup: " + backup.getName() + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            errors.add("Failed to list backups: " + e.getMessage());
        }
        
        return new BackupCleanupResult(deletedCount, freedSpace, errors);
    }
    
    /**
     * Verify backup integrity
     */
    public BackupVerificationResult verifyBackup(Path backupPath) {
        try {
            List<String> issues = new ArrayList<>();
            
            // Check if file exists and is readable
            if (!Files.exists(backupPath)) {
                issues.add("Backup file does not exist");
                return new BackupVerificationResult(false, issues);
            }
            
            if (!Files.isReadable(backupPath)) {
                issues.add("Backup file is not readable");
                return new BackupVerificationResult(false, issues);
            }
            
            // Try to extract and verify structure
            Path tempDir = Files.createTempDirectory("backup_verify");
            try {
                Path extractedDir = extractBackupArchive(backupPath, tempDir);
                
                // Check for metadata file
                Path metadataFile = extractedDir.resolve("backup_metadata.json");
                if (!Files.exists(metadataFile)) {
                    issues.add("Backup metadata file missing");
                } else {
                    // Try to parse metadata
                    try {
                        JsonProcessor.readFromJsonFile(metadataFile, BackupMetadata.class);
                    } catch (Exception e) {
                        issues.add("Invalid backup metadata: " + e.getMessage());
                    }
                }
                
                // Check for expected data files
                String[] expectedFiles = {"students.json", "professors.json", "courses.json", 
                                        "departments.json", "enrollments.json"};
                for (String file : expectedFiles) {
                    Path dataFile = extractedDir.resolve(file);
                    if (!Files.exists(dataFile)) {
                        issues.add("Missing data file: " + file);
                    } else {
                        // Try to parse JSON structure
                        try {
                            JsonProcessor.readFromJsonFile(dataFile, Object.class);
                        } catch (Exception e) {
                            issues.add("Invalid JSON in file " + file + ": " + e.getMessage());
                        }
                    }
                }
                
            } finally {
                FileUtil.deleteDirectoryRecursively(tempDir);
            }
            
            return new BackupVerificationResult(issues.isEmpty(), issues);
            
        } catch (Exception e) {
            return new BackupVerificationResult(false, 
                Arrays.asList("Verification failed: " + e.getMessage()));
        }
    }
    
    // Private helper methods for backup operations
    
    private CompletableFuture<BackupEntityResult> backupStudentsAsync(Path backupDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Student> students = studentRepository.findAll();
                Path studentsFile = backupDir.resolve("students.json");
                JsonProcessor.writeStudents(students, studentsFile);
                return new BackupEntityResult("students", true, students.size(), 
                                            Files.size(studentsFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("students", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    private CompletableFuture<BackupEntityResult> backupProfessorsAsync(Path backupDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Professor> professors = professorRepository.findAll();
                Path professorsFile = backupDir.resolve("professors.json");
                JsonProcessor.writeProfessors(professors, professorsFile);
                return new BackupEntityResult("professors", true, professors.size(), 
                                            Files.size(professorsFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("professors", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    private CompletableFuture<BackupEntityResult> backupCoursesAsync(Path backupDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Course> courses = courseRepository.findAll();
                Path coursesFile = backupDir.resolve("courses.json");
                JsonProcessor.writeCourses(courses, coursesFile);
                return new BackupEntityResult("courses", true, courses.size(), 
                                            Files.size(coursesFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("courses", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    private CompletableFuture<BackupEntityResult> backupDepartmentsAsync(Path backupDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Department> departments = departmentRepository.findAll();
                Path departmentsFile = backupDir.resolve("departments.json");
                JsonProcessor.writeDepartments(departments, departmentsFile);
                return new BackupEntityResult("departments", true, departments.size(), 
                                            Files.size(departmentsFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("departments", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    private CompletableFuture<BackupEntityResult> backupEnrollmentsAsync(Path backupDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Enrollment> enrollments = enrollmentRepository.findAll();
                Path enrollmentsFile = backupDir.resolve("enrollments.json");
                JsonProcessor.writeEnrollments(enrollments, enrollmentsFile);
                return new BackupEntityResult("enrollments", true, enrollments.size(), 
                                            Files.size(enrollmentsFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("enrollments", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    private CompletableFuture<BackupEntityResult> backupStudentsIncrementalAsync(Path backupDir, Date sinceDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get students enrolled since the given date
                List<Student> students = studentRepository.findByEnrollmentDateBetween(sinceDate, new Date());
                Path studentsFile = backupDir.resolve("students.json");
                JsonProcessor.writeStudents(students, studentsFile);
                return new BackupEntityResult("students", true, students.size(), 
                                            Files.size(studentsFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("students", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    private CompletableFuture<BackupEntityResult> backupEnrollmentsIncrementalAsync(Path backupDir, Date sinceDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get enrollments since the given date
                List<Enrollment> enrollments = enrollmentRepository.findByEnrollmentDateBetween(sinceDate, new Date());
                Path enrollmentsFile = backupDir.resolve("enrollments.json");
                JsonProcessor.writeEnrollments(enrollments, enrollmentsFile);
                return new BackupEntityResult("enrollments", true, enrollments.size(), 
                                            Files.size(enrollmentsFile), null);
            } catch (Exception e) {
                return new BackupEntityResult("enrollments", false, 0, 0, e.getMessage());
            }
        }, executorService);
    }
    
    // Private helper methods for restore operations
    
    private RestoreEntityResult restoreStudents(Path backupDir) {
        try {
            Path studentsFile = backupDir.resolve("students.json");
            if (Files.exists(studentsFile)) {
                List<Student> students = JsonProcessor.readStudents(studentsFile);
                for (Student student : students) {
                    studentRepository.save(student);
                }
                return new RestoreEntityResult("students", true, students.size(), null);
            } else {
                return new RestoreEntityResult("students", false, 0, "Students file not found in backup");
            }
        } catch (Exception e) {
            return new RestoreEntityResult("students", false, 0, e.getMessage());
        }
    }
    
    private RestoreEntityResult restoreProfessors(Path backupDir) {
        try {
            Path professorsFile = backupDir.resolve("professors.json");
            if (Files.exists(professorsFile)) {
                List<Professor> professors = JsonProcessor.readProfessors(professorsFile);
                for (Professor professor : professors) {
                    professorRepository.save(professor);
                }
                return new RestoreEntityResult("professors", true, professors.size(), null);
            } else {
                return new RestoreEntityResult("professors", false, 0, "Professors file not found in backup");
            }
        } catch (Exception e) {
            return new RestoreEntityResult("professors", false, 0, e.getMessage());
        }
    }
    
    private RestoreEntityResult restoreCourses(Path backupDir) {
        try {
            Path coursesFile = backupDir.resolve("courses.json");
            if (Files.exists(coursesFile)) {
                List<Course> courses = JsonProcessor.readCourses(coursesFile);
                for (Course course : courses) {
                    courseRepository.save(course);
                }
                return new RestoreEntityResult("courses", true, courses.size(), null);
            } else {
                return new RestoreEntityResult("courses", false, 0, "Courses file not found in backup");
            }
        } catch (Exception e) {
            return new RestoreEntityResult("courses", false, 0, e.getMessage());
        }
    }
    
    private RestoreEntityResult restoreDepartments(Path backupDir) {
        try {
            Path departmentsFile = backupDir.resolve("departments.json");
            if (Files.exists(departmentsFile)) {
                List<Department> departments = JsonProcessor.readDepartments(departmentsFile);
                for (Department department : departments) {
                    departmentRepository.save(department);
                }
                return new RestoreEntityResult("departments", true, departments.size(), null);
            } else {
                return new RestoreEntityResult("departments", false, 0, "Departments file not found in backup");
            }
        } catch (Exception e) {
            return new RestoreEntityResult("departments", false, 0, e.getMessage());
        }
    }
    
    private RestoreEntityResult restoreEnrollments(Path backupDir) {
        try {
            Path enrollmentsFile = backupDir.resolve("enrollments.json");
            if (Files.exists(enrollmentsFile)) {
                List<Enrollment> enrollments = JsonProcessor.readEnrollments(enrollmentsFile);
                for (Enrollment enrollment : enrollments) {
                    enrollmentRepository.save(enrollment);
                }
                return new RestoreEntityResult("enrollments", true, enrollments.size(), null);
            } else {
                return new RestoreEntityResult("enrollments", false, 0, "Enrollments file not found in backup");
            }
        } catch (Exception e) {
            return new RestoreEntityResult("enrollments", false, 0, e.getMessage());
        }
    }
    
    // Archive and extraction methods
    
    private Path createBackupArchive(Path backupDir, String backupName) throws IOException {
        Path archivePath = backupDir.getParent().resolve(backupName + ".zip");
        
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archivePath))) {
            Files.walk(backupDir)
                 .filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         String entryName = backupDir.relativize(file).toString();
                         ZipEntry entry = new ZipEntry(entryName);
                         zos.putNextEntry(entry);
                         Files.copy(file, zos);
                         zos.closeEntry();
                     } catch (IOException e) {
                         throw new RuntimeException("Failed to add file to archive: " + file, e);
                     }
                 });
        }
        
        return archivePath;
    }
    
    private Path extractBackupArchive(Path archivePath) throws IOException {
        Path tempDir = Files.createTempDirectory("backup_extract");
        return extractBackupArchive(archivePath, tempDir);
    }
    
    private Path extractBackupArchive(Path archivePath, Path extractDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outputPath = extractDir.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    Files.copy(zis, outputPath);
                }
                zis.closeEntry();
            }
        }
        
        return extractDir;
    }
    
    private List<BackupInfo> listBackupsInDirectory(Path directory, BackupType type) throws IOException {
        List<BackupInfo> backups = new ArrayList<>();
        
        try (var files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                 .forEach(file -> {
                     try {
                         backups.add(new BackupInfo(
                             file.getFileName().toString(),
                             file,
                             type,
                             new Date(Files.getLastModifiedTime(file).toMillis()),
                             Files.size(file)
                         ));
                     } catch (IOException e) {
                         System.err.println("Error reading backup file info: " + file);
                     }
                 });
        }
        
        return backups;
    }
    
    /**
     * Shutdown backup manager
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes for backup operations
    
    public enum BackupType {
        FULL, INCREMENTAL, DATABASE
    }
    
    public static class BackupResult {
        private final boolean success;
        private final String message;
        private final Path backupPath;
        private final BackupMetadata metadata;
        
        public BackupResult(boolean success, String message, Path backupPath, BackupMetadata metadata) {
            this.success = success;
            this.message = message;
            this.backupPath = backupPath;
            this.metadata = metadata;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Path getBackupPath() { return backupPath; }
        public BackupMetadata getMetadata() { return metadata; }
    }
    
    public static class RestoreResult {
        private final boolean success;
        private final String message;
        private final Map<String, RestoreEntityResult> entityResults;
        private final long duration;
        private final BackupMetadata originalMetadata;
        
        public RestoreResult(boolean success, String message, 
                           Map<String, RestoreEntityResult> entityResults,
                           long duration, BackupMetadata originalMetadata) {
            this.success = success;
            this.message = message;
            this.entityResults = entityResults;
            this.duration = duration;
            this.originalMetadata = originalMetadata;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, RestoreEntityResult> getEntityResults() { return entityResults; }
        public long getDuration() { return duration; }
        public BackupMetadata getOriginalMetadata() { return originalMetadata; }
    }
    
    public static class BackupEntityResult {
        private final String entityType;
        private final boolean success;
        private final int recordCount;
        private final long fileSize;
        private final String error;
        
        public BackupEntityResult(String entityType, boolean success, int recordCount, 
                                long fileSize, String error) {
            this.entityType = entityType;
            this.success = success;
            this.recordCount = recordCount;
            this.fileSize = fileSize;
            this.error = error;
        }
        
        // Getters
        public String getEntityType() { return entityType; }
        public boolean isSuccess() { return success; }
        public int getRecordCount() { return recordCount; }
        public long getFileSize() { return fileSize; }
        public String getError() { return error; }
    }
    
    public static class RestoreEntityResult {
        private final String entityType;
        private final boolean success;
        private final int recordCount;
        private final String error;
        
        public RestoreEntityResult(String entityType, boolean success, int recordCount, String error) {
            this.entityType = entityType;
            this.success = success;
            this.recordCount = recordCount;
            this.error = error;
        }
        
        // Getters
        public String getEntityType() { return entityType; }
        public boolean isSuccess() { return success; }
        public int getRecordCount() { return recordCount; }
        public String getError() { return error; }
    }
    
    public static class BackupMetadata {
        private String name;
        private BackupType type;
        private String timestamp;
        private Map<String, BackupEntityResult> entityResults;
        private long duration;
        private Date basedOnDate; // For incremental backups
        
        public BackupMetadata(String name, BackupType type, String timestamp,
                            Map<String, BackupEntityResult> entityResults, long duration) {
            this.name = name;
            this.type = type;
            this.timestamp = timestamp;
            this.entityResults = entityResults;
            this.duration = duration;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public BackupType getType() { return type; }
        public void setType(BackupType type) { this.type = type; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public Map<String, BackupEntityResult> getEntityResults() { return entityResults; }
        public void setEntityResults(Map<String, BackupEntityResult> entityResults) { 
            this.entityResults = entityResults; 
        }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public Date getBasedOnDate() { return basedOnDate; }
        public void setBasedOnDate(Date basedOnDate) { this.basedOnDate = basedOnDate; }
    }
    
    public static class BackupInfo {
        private final String name;
        private final Path path;
        private final BackupType type;
        private final Date creationDate;
        private final long size;
        
        public BackupInfo(String name, Path path, BackupType type, Date creationDate, long size) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.creationDate = creationDate;
            this.size = size;
        }
        
        // Getters
        public String getName() { return name; }
        public Path getPath() { return path; }
        public BackupType getType() { return type; }
        public Date getCreationDate() { return creationDate; }
        public long getSize() { return size; }
        
        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    public static class RestoreOptions {
        private boolean restoreStudents = true;
        private boolean restoreProfessors = true;
        private boolean restoreCourses = true;
        private boolean restoreDepartments = true;
        private boolean restoreEnrollments = true;
        private boolean overwriteExisting = false;
        
        // Getters and setters
        public boolean isRestoreStudents() { return restoreStudents; }
        public void setRestoreStudents(boolean restoreStudents) { this.restoreStudents = restoreStudents; }
        
        public boolean isRestoreProfessors() { return restoreProfessors; }
        public void setRestoreProfessors(boolean restoreProfessors) { this.restoreProfessors = restoreProfessors; }
        
        public boolean isRestoreCourses() { return restoreCourses; }
        public void setRestoreCourses(boolean restoreCourses) { this.restoreCourses = restoreCourses; }
        
        public boolean isRestoreDepartments() { return restoreDepartments; }
        public void setRestoreDepartments(boolean restoreDepartments) { this.restoreDepartments = restoreDepartments; }
        
        public boolean isRestoreEnrollments() { return restoreEnrollments; }
        public void setRestoreEnrollments(boolean restoreEnrollments) { this.restoreEnrollments = restoreEnrollments; }
        
        public boolean isOverwriteExisting() { return overwriteExisting; }
        public void setOverwriteExisting(boolean overwriteExisting) { this.overwriteExisting = overwriteExisting; }
    }
    
    public static class BackupCleanupResult {
        private final int deletedCount;
        private final long freedSpace;
        private final List<String> errors;
        
        public BackupCleanupResult(int deletedCount, long freedSpace, List<String> errors) {
            this.deletedCount = deletedCount;
            this.freedSpace = freedSpace;
            this.errors = errors;
        }
        
        // Getters
        public int getDeletedCount() { return deletedCount; }
        public long getFreedSpace() { return freedSpace; }
        public List<String> getErrors() { return errors; }
        
        public String getFormattedFreedSpace() {
            if (freedSpace < 1024) return freedSpace + " B";
            if (freedSpace < 1024 * 1024) return String.format("%.1f KB", freedSpace / 1024.0);
            if (freedSpace < 1024 * 1024 * 1024) return String.format("%.1f MB", freedSpace / (1024.0 * 1024));
            return String.format("%.1f GB", freedSpace / (1024.0 * 1024 * 1024));
        }
    }
    
    public static class BackupVerificationResult {
        private final boolean valid;
        private final List<String> issues;
        
        public BackupVerificationResult(boolean valid, List<String> issues) {
            this.valid = valid;
            this.issues = issues;
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public List<String> getIssues() { return issues; }
        public boolean hasIssues() { return !issues.isEmpty(); }
    }
}