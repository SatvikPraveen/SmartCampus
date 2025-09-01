// File location: src/main/java/concurrent/DataSyncManager.java

package concurrent;

import repositories.*;
import models.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Manages data synchronization operations using CompletableFuture
 * Provides asynchronous data operations and cache management
 */
public class DataSyncManager {
    
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean syncInProgress;
    private final AtomicLong lastSyncTime;
    private final ConcurrentHashMap<String, Object> syncCache;
    
    public DataSyncManager(StudentRepository studentRepository,
                          ProfessorRepository professorRepository,
                          CourseRepository courseRepository,
                          DepartmentRepository departmentRepository,
                          EnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.professorRepository = professorRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.syncInProgress = new AtomicBoolean(false);
        this.lastSyncTime = new AtomicLong(0);
        this.syncCache = new ConcurrentHashMap<>();
        
        // Schedule periodic sync operations
        schedulePeriodicSync();
    }
    
    /**
     * Synchronize all data repositories asynchronously
     */
    public CompletableFuture<SyncResult> syncAllDataAsync() {
        if (!syncInProgress.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(
                new SyncResult(false, "Sync already in progress", new Date()));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                Map<String, Integer> syncCounts = new HashMap<>();
                
                // Sync departments first (dependencies)
                syncCounts.put("departments", syncDepartments());
                
                // Sync users in parallel
                CompletableFuture<Integer> studentSync = 
                    CompletableFuture.supplyAsync(this::syncStudents, executorService);
                CompletableFuture<Integer> professorSync = 
                    CompletableFuture.supplyAsync(this::syncProfessors, executorService);
                
                // Wait for user sync completion
                CompletableFuture.allOf(studentSync, professorSync).join();
                syncCounts.put("students", studentSync.join());
                syncCounts.put("professors", professorSync.join());
                
                // Sync courses (depends on departments and professors)
                syncCounts.put("courses", syncCourses());
                
                // Sync enrollments (depends on students and courses)
                syncCounts.put("enrollments", syncEnrollments());
                
                long endTime = System.currentTimeMillis();
                lastSyncTime.set(endTime);
                
                return new SyncResult(true, 
                    String.format("Sync completed in %d ms. Counts: %s", 
                                endTime - startTime, syncCounts), 
                    new Date());
                    
            } catch (Exception e) {
                return new SyncResult(false, "Sync failed: " + e.getMessage(), new Date());
            } finally {
                syncInProgress.set(false);
            }
        }, executorService);
    }
    
    /**
     * Sync data with external source asynchronously
     */
    public CompletableFuture<SyncResult> syncFromExternalSourceAsync(
            ExternalDataSource dataSource) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate external data fetch
                ExternalData externalData = fetchExternalData(dataSource);
                
                // Process data in parallel
                List<CompletableFuture<Integer>> syncTasks = Arrays.asList(
                    CompletableFuture.supplyAsync(() -> 
                        processExternalStudents(externalData.getStudents()), executorService),
                    CompletableFuture.supplyAsync(() -> 
                        processExternalProfessors(externalData.getProfessors()), executorService),
                    CompletableFuture.supplyAsync(() -> 
                        processExternalCourses(externalData.getCourses()), executorService)
                );
                
                // Wait for all tasks to complete
                CompletableFuture.allOf(syncTasks.toArray(new CompletableFuture[0])).join();
                
                int totalProcessed = syncTasks.stream()
                    .mapToInt(CompletableFuture::join)
                    .sum();
                
                return new SyncResult(true, 
                    String.format("External sync completed. Processed %d records", totalProcessed),
                    new Date());
                    
            } catch (Exception e) {
                return new SyncResult(false, 
                    "External sync failed: " + e.getMessage(), new Date());
            }
        }, executorService);
    }
    
    /**
     * Asynchronous cache refresh operation
     */
    public CompletableFuture<Void> refreshCacheAsync() {
        return CompletableFuture.runAsync(() -> {
            // Clear existing cache
            syncCache.clear();
            
            // Populate cache with frequently accessed data
            List<CompletableFuture<Void>> cacheTasks = Arrays.asList(
                CompletableFuture.runAsync(() -> 
                    cacheStudentData(), executorService),
                CompletableFuture.runAsync(() -> 
                    cacheCourseData(), executorService),
                CompletableFuture.runAsync(() -> 
                    cacheDepartmentData(), executorService),
                CompletableFuture.runAsync(() -> 
                    cacheEnrollmentData(), executorService)
            );
            
            CompletableFuture.allOf(cacheTasks.toArray(new CompletableFuture[0])).join();
        }, executorService);
    }
    
    /**
     * Asynchronous data validation
     */
    public CompletableFuture<ValidationResult> validateDataIntegrityAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> validationErrors = new ArrayList<>();
            
            // Validate in parallel
            List<CompletableFuture<List<String>>> validationTasks = Arrays.asList(
                CompletableFuture.supplyAsync(this::validateStudentData, executorService),
                CompletableFuture.supplyAsync(this::validateProfessorData, executorService),
                CompletableFuture.supplyAsync(this::validateCourseData, executorService),
                CompletableFuture.supplyAsync(this::validateEnrollmentData, executorService)
            );
            
            // Collect all validation errors
            for (CompletableFuture<List<String>> task : validationTasks) {
                validationErrors.addAll(task.join());
            }
            
            return new ValidationResult(validationErrors.isEmpty(), validationErrors);
        }, executorService);
    }
    
    /**
     * Asynchronous backup operation
     */
    public CompletableFuture<BackupResult> createBackupAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Create backup data snapshots in parallel
                CompletableFuture<List<Student>> studentBackup = 
                    CompletableFuture.supplyAsync(() -> 
                        new ArrayList<>(studentRepository.findAll()), executorService);
                        
                CompletableFuture<List<Professor>> professorBackup = 
                    CompletableFuture.supplyAsync(() -> 
                        new ArrayList<>(professorRepository.findAll()), executorService);
                        
                CompletableFuture<List<Course>> courseBackup = 
                    CompletableFuture.supplyAsync(() -> 
                        new ArrayList<>(courseRepository.findAll()), executorService);
                        
                CompletableFuture<List<Department>> departmentBackup = 
                    CompletableFuture.supplyAsync(() -> 
                        new ArrayList<>(departmentRepository.findAll()), executorService);
                        
                CompletableFuture<List<Enrollment>> enrollmentBackup = 
                    CompletableFuture.supplyAsync(() -> 
                        new ArrayList<>(enrollmentRepository.findAll()), executorService);
                
                // Wait for all backups to complete
                CompletableFuture.allOf(studentBackup, professorBackup, courseBackup, 
                                      departmentBackup, enrollmentBackup).join();
                
                // Create backup package
                BackupData backupData = new BackupData(
                    studentBackup.join(),
                    professorBackup.join(),
                    courseBackup.join(),
                    departmentBackup.join(),
                    enrollmentBackup.join(),
                    new Date()
                );
                
                long endTime = System.currentTimeMillis();
                
                return new BackupResult(true, 
                    String.format("Backup completed in %d ms", endTime - startTime),
                    backupData);
                    
            } catch (Exception e) {
                return new BackupResult(false, "Backup failed: " + e.getMessage(), null);
            }
        }, executorService);
    }
    
    /**
     * Schedule periodic synchronization
     */
    private void schedulePeriodicSync() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (!syncInProgress.get()) {
                syncAllDataAsync().whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Scheduled sync failed: " + throwable.getMessage());
                    }
                });
            }
        }, 1, 6, TimeUnit.HOURS); // Sync every 6 hours
    }
    
    // Private sync methods
    
    private int syncDepartments() {
        // Simulate department sync
        return departmentRepository.findAll().size();
    }
    
    private int syncStudents() {
        // Simulate student sync
        return studentRepository.findAll().size();
    }
    
    private int syncProfessors() {
        // Simulate professor sync
        return professorRepository.findAll().size();
    }
    
    private int syncCourses() {
        // Simulate course sync
        return courseRepository.findAll().size();
    }
    
    private int syncEnrollments() {
        // Simulate enrollment sync
        return enrollmentRepository.findAll().size();
    }
    
    // Cache methods
    
    private void cacheStudentData() {
        List<Student> students = studentRepository.findAll();
        syncCache.put("students_count", students.size());
        syncCache.put("students_by_department", 
                     studentRepository.groupByDepartment());
    }
    
    private void cacheCourseData() {
        List<Course> courses = courseRepository.findAll();
        syncCache.put("courses_count", courses.size());
        syncCache.put("courses_by_department", 
                     courseRepository.groupByDepartment());
    }
    
    private void cacheDepartmentData() {
        List<Department> departments = departmentRepository.findAll();
        syncCache.put("departments_count", departments.size());
        syncCache.put("department_stats", 
                     departmentRepository.getDepartmentStatistics());
    }
    
    private void cacheEnrollmentData() {
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        syncCache.put("enrollments_count", enrollments.size());
        syncCache.put("enrollment_stats", 
                     enrollmentRepository.getEnrollmentStatistics());
    }
    
    // Validation methods
    
    private List<String> validateStudentData() {
        List<String> errors = new ArrayList<>();
        List<Student> students = studentRepository.findAll();
        
        for (Student student : students) {
            if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
                errors.add("Student " + student.getId() + " has no email");
            }
            if (student.getDepartment() == null) {
                errors.add("Student " + student.getId() + " has no department");
            }
        }
        
        return errors;
    }
    
    private List<String> validateProfessorData() {
        List<String> errors = new ArrayList<>();
        List<Professor> professors = professorRepository.findAll();
        
        for (Professor professor : professors) {
            if (professor.getEmail() == null || professor.getEmail().trim().isEmpty()) {
                errors.add("Professor " + professor.getId() + " has no email");
            }
            if (professor.getDepartment() == null) {
                errors.add("Professor " + professor.getId() + " has no department");
            }
        }
        
        return errors;
    }
    
    private List<String> validateCourseData() {
        List<String> errors = new ArrayList<>();
        List<Course> courses = courseRepository.findAll();
        
        for (Course course : courses) {
            if (course.getDepartment() == null) {
                errors.add("Course " + course.getCourseCode() + " has no department");
            }
            if (course.getProfessor() == null) {
                errors.add("Course " + course.getCourseCode() + " has no professor");
            }
            if (course.getCapacity() <= 0) {
                errors.add("Course " + course.getCourseCode() + " has invalid capacity");
            }
        }
        
        return errors;
    }
    
    private List<String> validateEnrollmentData() {
        List<String> errors = new ArrayList<>();
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudent() == null) {
                errors.add("Enrollment " + enrollment.getEnrollmentId() + " has no student");
            }
            if (enrollment.getCourse() == null) {
                errors.add("Enrollment " + enrollment.getEnrollmentId() + " has no course");
            }
        }
        
        return errors;
    }
    
    // External data processing methods
    
    private ExternalData fetchExternalData(ExternalDataSource dataSource) {
        // Simulate external data fetch
        return new ExternalData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
    
    private int processExternalStudents(List<Student> students) {
        // Process external students
        return students.size();
    }
    
    private int processExternalProfessors(List<Professor> professors) {
        // Process external professors
        return professors.size();
    }
    
    private int processExternalCourses(List<Course> courses) {
        // Process external courses
        return courses.size();
    }
    
    // Shutdown method
    
    public void shutdown() {
        executorService.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Getter methods
    
    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }
    
    public long getLastSyncTime() {
        return lastSyncTime.get();
    }
    
    public Object getCachedData(String key) {
        return syncCache.get(key);
    }
    
    // Inner classes
    
    public static class SyncResult {
        private final boolean success;
        private final String message;
        private final Date timestamp;
        
        public SyncResult(boolean success, String message, Date timestamp) {
            this.success = success;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Date getTimestamp() { return timestamp; }
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
    }
    
    public static class BackupResult {
        private final boolean success;
        private final String message;
        private final BackupData backupData;
        
        public BackupResult(boolean success, String message, BackupData backupData) {
            this.success = success;
            this.message = message;
            this.backupData = backupData;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BackupData getBackupData() { return backupData; }
    }
    
    public static class BackupData {
        private final List<Student> students;
        private final List<Professor> professors;
        private final List<Course> courses;
        private final List<Department> departments;
        private final List<Enrollment> enrollments;
        private final Date backupTime;
        
        public BackupData(List<Student> students, List<Professor> professors,
                         List<Course> courses, List<Department> departments,
                         List<Enrollment> enrollments, Date backupTime) {
            this.students = students;
            this.professors = professors;
            this.courses = courses;
            this.departments = departments;
            this.enrollments = enrollments;
            this.backupTime = backupTime;
        }
        
        // Getters
        public List<Student> getStudents() { return students; }
        public List<Professor> getProfessors() { return professors; }
        public List<Course> getCourses() { return courses; }
        public List<Department> getDepartments() { return departments; }
        public List<Enrollment> getEnrollments() { return enrollments; }
        public Date getBackupTime() { return backupTime; }
    }
    
    public static class ExternalDataSource {
        private final String sourceName;
        private final String connectionString;
        
        public ExternalDataSource(String sourceName, String connectionString) {
            this.sourceName = sourceName;
            this.connectionString = connectionString;
        }
        
        public String getSourceName() { return sourceName; }
        public String getConnectionString() { return connectionString; }
    }
    
    public static class ExternalData {
        private final List<Student> students;
        private final List<Professor> professors;
        private final List<Course> courses;
        
        public ExternalData(List<Student> students, List<Professor> professors, 
                           List<Course> courses) {
            this.students = students;
            this.professors = professors;
            this.courses = courses;
        }
        
        public List<Student> getStudents() { return students; }
        public List<Professor> getProfessors() { return professors; }
        public List<Course> getCourses() { return courses; }
    }
}