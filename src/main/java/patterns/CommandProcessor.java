// File location: src/main/java/patterns/CommandProcessor.java

package patterns;

import models.*;
import services.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Command pattern implementation for processing system operations
 * Provides undo/redo functionality, command queuing, and batch processing
 */
public class CommandProcessor {
    
    private final Queue<Command> commandHistory;
    private final Stack<Command> undoStack;
    private final Stack<Command> redoStack;
    private final ExecutorService executorService;
    private final Map<String, CommandFactory> commandFactories;
    private final CommandValidator validator;
    private final int maxHistorySize;
    
    public CommandProcessor() {
        this(1000); // Default history size
    }
    
    public CommandProcessor(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        this.commandHistory = new LinkedList<>();
        this.undoStack = new Stack<>();
        this.redoStack = new Stack<>();
        this.executorService = Executors.newFixedThreadPool(4);
        this.commandFactories = new ConcurrentHashMap<>();
        this.validator = new CommandValidator();
        
        registerDefaultCommands();
    }
    
    /**
     * Execute a command synchronously
     */
    public CommandResult execute(Command command) {
        ValidationResult validation = validator.validate(command);
        if (!validation.isValid()) {
            return new CommandResult(false, "Command validation failed: " + 
                                   String.join(", ", validation.getErrors()), null);
        }
        
        try {
            Object result = command.execute();
            
            // Add to history and undo stack if the command is undoable
            recordCommand(command);
            
            return new CommandResult(true, "Command executed successfully", result);
            
        } catch (Exception e) {
            return new CommandResult(false, "Command execution failed: " + e.getMessage(), null);
        }
    }
    
    /**
     * Execute a command asynchronously
     */
    public CompletableFuture<CommandResult> executeAsync(Command command) {
        return CompletableFuture.supplyAsync(() -> execute(command), executorService);
    }
    
    /**
     * Execute multiple commands as a batch
     */
    public List<CommandResult> executeBatch(List<Command> commands) {
        List<CommandResult> results = new ArrayList<>();
        
        for (Command command : commands) {
            CommandResult result = execute(command);
            results.add(result);
            
            // Stop on first failure if command is critical
            if (!result.isSuccess() && command instanceof CriticalCommand) {
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Execute multiple commands asynchronously
     */
    public CompletableFuture<List<CommandResult>> executeBatchAsync(List<Command> commands) {
        List<CompletableFuture<CommandResult>> futures = commands.stream()
            .map(this::executeAsync)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
    }
    
    /**
     * Undo the last executed command
     */
    public CommandResult undo() {
        if (undoStack.isEmpty()) {
            return new CommandResult(false, "No commands to undo", null);
        }
        
        Command command = undoStack.pop();
        if (!(command instanceof UndoableCommand)) {
            return new CommandResult(false, "Command is not undoable", null);
        }
        
        try {
            Object result = ((UndoableCommand) command).undo();
            redoStack.push(command);
            return new CommandResult(true, "Command undone successfully", result);
        } catch (Exception e) {
            undoStack.push(command); // Put it back
            return new CommandResult(false, "Undo failed: " + e.getMessage(), null);
        }
    }
    
    /**
     * Redo the last undone command
     */
    public CommandResult redo() {
        if (redoStack.isEmpty()) {
            return new CommandResult(false, "No commands to redo", null);
        }
        
        Command command = redoStack.pop();
        try {
            Object result = command.execute();
            undoStack.push(command);
            return new CommandResult(true, "Command redone successfully", result);
        } catch (Exception e) {
            redoStack.push(command); // Put it back
            return new CommandResult(false, "Redo failed: " + e.getMessage(), null);
        }
    }
    
    /**
     * Record command in history and undo stack
     */
    private void recordCommand(Command command) {
        commandHistory.offer(command);
        
        // Maintain history size
        while (commandHistory.size() > maxHistorySize) {
            commandHistory.poll();
        }
        
        // Add to undo stack if undoable
        if (command instanceof UndoableCommand) {
            undoStack.push(command);
            redoStack.clear(); // Clear redo stack when new command is executed
        }
    }
    
    /**
     * Create command using registered factory
     */
    public Command createCommand(String commandType, Map<String, Object> parameters) {
        CommandFactory factory = commandFactories.get(commandType);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown command type: " + commandType);
        }
        return factory.createCommand(parameters);
    }
    
    /**
     * Register command factory
     */
    public void registerCommandFactory(String commandType, CommandFactory factory) {
        commandFactories.put(commandType, factory);
    }
    
    /**
     * Register default command factories
     */
    private void registerDefaultCommands() {
        // Student commands
        registerCommandFactory("CREATE_STUDENT", params -> 
            new CreateStudentCommand((Student) params.get("student")));
        registerCommandFactory("UPDATE_STUDENT", params -> 
            new UpdateStudentCommand((Student) params.get("student")));
        registerCommandFactory("DELETE_STUDENT", params -> 
            new DeleteStudentCommand((String) params.get("studentId")));
        
        // Course commands
        registerCommandFactory("CREATE_COURSE", params -> 
            new CreateCourseCommand((Course) params.get("course")));
        registerCommandFactory("UPDATE_COURSE", params -> 
            new UpdateCourseCommand((Course) params.get("course")));
        registerCommandFactory("DELETE_COURSE", params -> 
            new DeleteCourseCommand((String) params.get("courseCode")));
        
        // Enrollment commands
        registerCommandFactory("ENROLL_STUDENT", params -> 
            new EnrollStudentCommand((Student) params.get("student"), 
                                   (Course) params.get("course")));
        registerCommandFactory("UNENROLL_STUDENT", params -> 
            new UnenrollStudentCommand((Student) params.get("student"), 
                                     (Course) params.get("course")));
        
        // Grade commands
        registerCommandFactory("ASSIGN_GRADE", params -> 
            new AssignGradeCommand((Student) params.get("student"), 
                                 (Course) params.get("course"), 
                                 (Grade) params.get("grade")));
        
        // System commands
        registerCommandFactory("BACKUP_DATA", params -> 
            new BackupDataCommand());
        registerCommandFactory("RESTORE_DATA", params -> 
            new RestoreDataCommand((String) params.get("backupPath")));
    }
    
    /**
     * Get command history
     */
    public List<Command> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }
    
    /**
     * Get command statistics
     */
    public CommandStatistics getStatistics() {
        Map<String, Integer> commandCounts = new HashMap<>();
        int successfulCommands = 0;
        int failedCommands = 0;
        
        for (Command command : commandHistory) {
            String type = command.getClass().getSimpleName();
            commandCounts.merge(type, 1, Integer::sum);
            
            if (command instanceof StatefulCommand) {
                if (((StatefulCommand) command).wasSuccessful()) {
                    successfulCommands++;
                } else {
                    failedCommands++;
                }
            }
        }
        
        return new CommandStatistics(
            commandHistory.size(),
            successfulCommands,
            failedCommands,
            undoStack.size(),
            redoStack.size(),
            commandCounts
        );
    }
    
    /**
     * Clear all history and stacks
     */
    public void clearHistory() {
        commandHistory.clear();
        undoStack.clear();
        redoStack.clear();
    }
    
    /**
     * Shutdown command processor
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
    
    // Command interfaces and base classes
    
    public interface Command {
        Object execute() throws Exception;
        String getDescription();
        Date getTimestamp();
    }
    
    public interface UndoableCommand extends Command {
        Object undo() throws Exception;
    }
    
    public interface CriticalCommand extends Command {
        // Marker interface for commands that should stop batch execution on failure
    }
    
    public interface StatefulCommand extends Command {
        boolean wasSuccessful();
        Exception getLastError();
    }
    
    public abstract class BaseCommand implements Command, StatefulCommand {
        protected final Date timestamp;
        protected boolean successful = false;
        protected Exception lastError;
        
        public BaseCommand() {
            this.timestamp = new Date();
        }
        
        @Override
        public Date getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean wasSuccessful() {
            return successful;
        }
        
        @Override
        public Exception getLastError() {
            return lastError;
        }
        
        protected void setResult(boolean successful, Exception error) {
            this.successful = successful;
            this.lastError = error;
        }
    }
    
    // Concrete command implementations for SmartCampus operations
    
    public class CreateStudentCommand extends BaseCommand implements UndoableCommand {
        private final Student student;
        private StudentService studentService;
        
        public CreateStudentCommand(Student student) {
            this.student = student;
            this.studentService = ServiceFactory.getDefaultFactory().createStudentService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                Student created = studentService.createStudent(student);
                setResult(true, null);
                return created;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            studentService.deleteStudent(student.getId());
            return null;
        }
        
        @Override
        public String getDescription() {
            return "Create student: " + student.getName();
        }
    }
    
    public class UpdateStudentCommand extends BaseCommand implements UndoableCommand {
        private final Student newStudent;
        private Student originalStudent;
        private StudentService studentService;
        
        public UpdateStudentCommand(Student student) {
            this.newStudent = student;
            this.studentService = ServiceFactory.getDefaultFactory().createStudentService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                // Store original for undo
                originalStudent = studentService.getStudentById(newStudent.getId());
                Student updated = studentService.updateStudent(newStudent);
                setResult(true, null);
                return updated;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            studentService.updateStudent(originalStudent);
            return originalStudent;
        }
        
        @Override
        public String getDescription() {
            return "Update student: " + newStudent.getName();
        }
    }
    
    public class DeleteStudentCommand extends BaseCommand implements UndoableCommand {
        private final String studentId;
        private Student deletedStudent;
        private StudentService studentService;
        
        public DeleteStudentCommand(String studentId) {
            this.studentId = studentId;
            this.studentService = ServiceFactory.getDefaultFactory().createStudentService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                // Store for undo
                deletedStudent = studentService.getStudentById(studentId);
                studentService.deleteStudent(studentId);
                setResult(true, null);
                return null;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            studentService.createStudent(deletedStudent);
            return deletedStudent;
        }
        
        @Override
        public String getDescription() {
            return "Delete student: " + studentId;
        }
    }
    
    public class CreateCourseCommand extends BaseCommand implements UndoableCommand {
        private final Course course;
        private CourseService courseService;
        
        public CreateCourseCommand(Course course) {
            this.course = course;
            this.courseService = ServiceFactory.getDefaultFactory().createCourseService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                Course created = courseService.createCourse(course);
                setResult(true, null);
                return created;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            courseService.deleteCourse(course.getCourseCode());
            return null;
        }
        
        @Override
        public String getDescription() {
            return "Create course: " + course.getName();
        }
    }
    
    public class UpdateCourseCommand extends BaseCommand implements UndoableCommand {
        private final Course newCourse;
        private Course originalCourse;
        private CourseService courseService;
        
        public UpdateCourseCommand(Course course) {
            this.newCourse = course;
            this.courseService = ServiceFactory.getDefaultFactory().createCourseService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                originalCourse = courseService.getCourseByCode(newCourse.getCourseCode());
                Course updated = courseService.updateCourse(newCourse);
                setResult(true, null);
                return updated;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            courseService.updateCourse(originalCourse);
            return originalCourse;
        }
        
        @Override
        public String getDescription() {
            return "Update course: " + newCourse.getName();
        }
    }
    
    public class DeleteCourseCommand extends BaseCommand implements UndoableCommand {
        private final String courseCode;
        private Course deletedCourse;
        private CourseService courseService;
        
        public DeleteCourseCommand(String courseCode) {
            this.courseCode = courseCode;
            this.courseService = ServiceFactory.getDefaultFactory().createCourseService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                deletedCourse = courseService.getCourseByCode(courseCode);
                courseService.deleteCourse(courseCode);
                setResult(true, null);
                return null;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            courseService.createCourse(deletedCourse);
            return deletedCourse;
        }
        
        @Override
        public String getDescription() {
            return "Delete course: " + courseCode;
        }
    }
    
    public class EnrollStudentCommand extends BaseCommand implements UndoableCommand {
        private final Student student;
        private final Course course;
        private EnrollmentService enrollmentService;
        
        public EnrollStudentCommand(Student student, Course course) {
            this.student = student;
            this.course = course;
            this.enrollmentService = ServiceFactory.getDefaultFactory().createEnrollmentService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                boolean result = enrollmentService.enrollStudent(student, course);
                setResult(result, null);
                return result;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            enrollmentService.unenrollStudent(student, course);
            return null;
        }
        
        @Override
        public String getDescription() {
            return "Enroll " + student.getName() + " in " + course.getName();
        }
    }
    
    public class UnenrollStudentCommand extends BaseCommand implements UndoableCommand {
        private final Student student;
        private final Course course;
        private EnrollmentService enrollmentService;
        
        public UnenrollStudentCommand(Student student, Course course) {
            this.student = student;
            this.course = course;
            this.enrollmentService = ServiceFactory.getDefaultFactory().createEnrollmentService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                boolean result = enrollmentService.unenrollStudent(student, course);
                setResult(result, null);
                return result;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            enrollmentService.enrollStudent(student, course);
            return null;
        }
        
        @Override
        public String getDescription() {
            return "Unenroll " + student.getName() + " from " + course.getName();
        }
    }
    
    public class AssignGradeCommand extends BaseCommand implements UndoableCommand {
        private final Student student;
        private final Course course;
        private final Grade newGrade;
        private Grade previousGrade;
        private GradeService gradeService;
        
        public AssignGradeCommand(Student student, Course course, Grade grade) {
            this.student = student;
            this.course = course;
            this.newGrade = grade;
            this.gradeService = ServiceFactory.getDefaultFactory().createGradeService();
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                // Store previous grade for undo
                previousGrade = gradeService.getGrade(student, course);
                Grade assigned = gradeService.assignGrade(student, course, newGrade);
                setResult(true, null);
                return assigned;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public Object undo() throws Exception {
            if (previousGrade != null) {
                gradeService.assignGrade(student, course, previousGrade);
                return previousGrade;
            } else {
                gradeService.removeGrade(student, course);
                return null;
            }
        }
        
        @Override
        public String getDescription() {
            return "Assign grade " + newGrade.getLetterGrade() + " to " + 
                   student.getName() + " for " + course.getName();
        }
    }
    
    public class BackupDataCommand extends BaseCommand implements CriticalCommand {
        @Override
        public Object execute() throws Exception {
            try {
                // Simulate backup operation
                Thread.sleep(1000); // Simulate time-consuming operation
                setResult(true, null);
                return "Backup completed successfully";
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public String getDescription() {
            return "Backup system data";
        }
    }
    
    public class RestoreDataCommand extends BaseCommand implements CriticalCommand {
        private final String backupPath;
        
        public RestoreDataCommand(String backupPath) {
            this.backupPath = backupPath;
        }
        
        @Override
        public Object execute() throws Exception {
            try {
                // Simulate restore operation
                Thread.sleep(2000); // Simulate time-consuming operation
                setResult(true, null);
                return "Restore completed successfully from " + backupPath;
            } catch (Exception e) {
                setResult(false, e);
                throw e;
            }
        }
        
        @Override
        public String getDescription() {
            return "Restore system data from " + backupPath;
        }
    }
    
    // Supporting classes
    
    @FunctionalInterface
    public interface CommandFactory {
        Command createCommand(Map<String, Object> parameters);
    }
    
    public static class CommandResult {
        private final boolean success;
        private final String message;
        private final Object result;
        
        public CommandResult(boolean success, String message, Object result) {
            this.success = success;
            this.message = message;
            this.result = result;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getResult() { return result; }
        
        @Override
        public String toString() {
            return String.format("CommandResult{success=%s, message='%s'}", success, message);
        }
    }
    
    public static class CommandStatistics {
        private final int totalCommands;
        private final int successfulCommands;
        private final int failedCommands;
        private final int undoStackSize;
        private final int redoStackSize;
        private final Map<String, Integer> commandCounts;
        
        public CommandStatistics(int totalCommands, int successfulCommands, int failedCommands,
                               int undoStackSize, int redoStackSize, Map<String, Integer> commandCounts) {
            this.totalCommands = totalCommands;
            this.successfulCommands = successfulCommands;
            this.failedCommands = failedCommands;
            this.undoStackSize = undoStackSize;
            this.redoStackSize = redoStackSize;
            this.commandCounts = commandCounts;
        }
        
        // Getters
        public int getTotalCommands() { return totalCommands; }
        public int getSuccessfulCommands() { return successfulCommands; }
        public int getFailedCommands() { return failedCommands; }
        public int getUndoStackSize() { return undoStackSize; }
        public int getRedoStackSize() { return redoStackSize; }
        public Map<String, Integer> getCommandCounts() { return commandCounts; }
        
        public double getSuccessRate() {
            return totalCommands > 0 ? (double) successfulCommands / totalCommands * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("CommandStats{total=%d, success=%.1f%%, undo=%d, redo=%d}",
                               totalCommands, getSuccessRate(), undoStackSize, redoStackSize);
        }
    }
    
    public static class CommandValidator {
        public ValidationResult validate(Command command) {
            List<String> errors = new ArrayList<>();
            
            if (command == null) {
                errors.add("Command cannot be null");
            } else {
                if (command.getDescription() == null || command.getDescription().trim().isEmpty()) {
                    errors.add("Command must have a description");
                }
                
                if (command.getTimestamp() == null) {
                    errors.add("Command must have a timestamp");
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
        }
    }
}