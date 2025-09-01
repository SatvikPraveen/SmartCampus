# SmartCampus Best Practices Guide

**Location:** `docs/examples/best-practices.md`

## Overview

This guide outlines the best practices, coding standards, and design principles used in the SmartCampus project. Following these practices ensures maintainable, scalable, and robust code that aligns with modern Java development standards.

## Table of Contents

1. [Code Organization and Structure](#code-organization-and-structure)
2. [Java Best Practices](#java-best-practices)
3. [Object-Oriented Design Principles](#object-oriented-design-principles)
4. [Functional Programming Guidelines](#functional-programming-guidelines)
5. [Error Handling and Exceptions](#error-handling-and-exceptions)
6. [Testing Best Practices](#testing-best-practices)
7. [Performance Optimization](#performance-optimization)
8. [Security Best Practices](#security-best-practices)
9. [Documentation Standards](#documentation-standards)
10. [Database and Data Access](#database-and-data-access)
11. [Concurrent Programming](#concurrent-programming)
12. [Logging and Monitoring](#logging-and-monitoring)

## Code Organization and Structure

### Package Structure Best Practices

```java
// ✅ GOOD: Clear, purpose-driven package structure
com.smartcampus.models.entities     // Domain entities
com.smartcampus.models.dto          // Data Transfer Objects
com.smartcampus.services.core       // Core business services
com.smartcampus.services.impl       // Service implementations
com.smartcampus.repositories.jpa    // JPA repositories
com.smartcampus.utils.validation    // Validation utilities
com.smartcampus.config.security     // Security configuration

// ❌ BAD: Generic, unclear package names
com.smartcampus.stuff
com.smartcampus.misc
com.smartcampus.things
com.smartcampus.utils.all
```

### Class Organization

```java
// ✅ GOOD: Well-organized class structure
public class StudentService {
    // 1. Constants (static final fields)
    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);
    private static final int MAX_ENROLLMENT_ATTEMPTS = 3;

    // 2. Instance fields (dependencies first, then state)
    private final StudentRepository repository;
    private final ValidationUtil validator;
    private final NotificationService notificationService;

    // 3. Constructors
    public StudentService(StudentRepository repository,
                         ValidationUtil validator,
                         NotificationService notificationService) {
        this.repository = requireNonNull(repository, "Repository cannot be null");
        this.validator = requireNonNull(validator, "Validator cannot be null");
        this.notificationService = requireNonNull(notificationService, "NotificationService cannot be null");
    }

    // 4. Public methods (API)
    public Student createStudent(StudentCreateRequest request) {
        // Implementation
    }

    // 5. Package-private methods (for testing)
    Student validateAndCreateStudent(StudentCreateRequest request) {
        // Implementation
    }

    // 6. Private methods (internal logic)
    private void sendWelcomeNotification(Student student) {
        // Implementation
    }

    // 7. Static utility methods (if any)
    public static String generateStudentId(String prefix, int sequence) {
        return String.format("%s%06d", prefix, sequence);
    }
}
```

### Naming Conventions

```java
// ✅ GOOD: Descriptive, intention-revealing names
public class EnrollmentService {

    // Clear method names that describe what they do
    public CompletableFuture<Enrollment> enrollStudentInCourseAsync(String studentId, String courseId) { }
    public List<Student> findStudentsEligibleForHonorsList() { }
    public boolean hasStudentMetPrerequisites(String studentId, String courseId) { }

    // Clear variable names
    private final Map<String, List<EnrollmentRequest>> pendingEnrollmentsByStudent = new HashMap<>();
    private final Duration enrollmentProcessingTimeout = Duration.ofMinutes(5);

    // Clear constant names
    private static final int DEFAULT_MAX_CONCURRENT_ENROLLMENTS = 10;
    private static final String ENROLLMENT_SUCCESS_MESSAGE = "Enrollment completed successfully";
}

// ❌ BAD: Abbreviated, unclear names
public class EnrSrv {
    public CompletableFuture<Enrollment> enrStdAsync(String sid, String cid) { }
    public List<Student> findStdsForHL() { }
    public boolean chkPrereqs(String sid, String cid) { }

    private final Map<String, List<EnrollmentRequest>> pendEnrs = new HashMap<>();
    private final Duration procTmout = Duration.ofMinutes(5);

    private static final int MAX_CONC_ENR = 10;
    private static final String SUCCESS_MSG = "OK";
}
```

## Java Best Practices

### Use Modern Java Features Effectively

#### Records for Data Transfer Objects

```java
// ✅ GOOD: Using records for immutable DTOs
public record StudentSummaryDTO(
    String id,
    String fullName,
    String email,
    String major,
    double gpa,
    LocalDate enrollmentDate
) {
    // Compact constructor for validation
    public StudentSummaryDTO {
        requireNonNull(id, "Student ID cannot be null");
        requireNonNull(fullName, "Full name cannot be null");
        requireNonNull(email, "Email cannot be null");

        if (gpa < 0.0 || gpa > 4.0) {
            throw new IllegalArgumentException("GPA must be between 0.0 and 4.0");
        }
    }

    // Derived properties
    public boolean isHonorStudent() {
        return gpa >= 3.5;
    }
}

// ❌ BAD: Mutable DTO with boilerplate
public class StudentSummaryDTO {
    private String id;
    private String fullName;
    private String email;
    private String major;
    private double gpa;
    private LocalDate enrollmentDate;

    // Constructors, getters, setters, equals, hashCode, toString...
}
```

#### Pattern Matching and Switch Expressions

```java
// ✅ GOOD: Modern switch expressions with pattern matching
public String getStatusMessage(EnrollmentStatus status) {
    return switch (status) {
        case ENROLLED -> "Student is currently enrolled in the course";
        case COMPLETED -> "Student has successfully completed the course";
        case WITHDRAWN -> "Student has withdrawn from the course";
        case FAILED -> "Student did not meet the course requirements";
        case PENDING -> "Enrollment is pending approval";
        case WAITLISTED -> "Student is on the waitlist for this course";
    };
}

public double calculateLateFee(Grade grade) {
    return switch (grade) {
        case Grade g when g.isSubmittedLate() && g.getDaysLate() <= 3 -> 10.0;
        case Grade g when g.isSubmittedLate() && g.getDaysLate() <= 7 -> 25.0;
        case Grade g when g.isSubmittedLate() -> 50.0;
        default -> 0.0;
    };
}

// ❌ BAD: Old-style switch statements
public String getStatusMessage(EnrollmentStatus status) {
    String message;
    switch (status) {
        case ENROLLED:
            message = "Student is currently enrolled in the course";
            break;
        case COMPLETED:
            message = "Student has successfully completed the course";
            break;
        case WITHDRAWN:
            message = "Student has withdrawn from the course";
            break;
        // ... more cases
        default:
            message = "Unknown status";
    }
    return message;
}
```

#### Text Blocks for Multi-line Strings

```java
// ✅ GOOD: Using text blocks for SQL and templates
public class StudentRepository {

    private static final String FIND_STUDENTS_WITH_HONORS_QUERY = """
        SELECT s.id, s.first_name, s.last_name, s.email, s.gpa
        FROM students s
        WHERE s.gpa >= 3.5
          AND s.status = 'ACTIVE'
          AND s.enrollment_year >= ?
        ORDER BY s.gpa DESC, s.last_name ASC
        """;

    private static final String EMAIL_TEMPLATE = """
        Dear %s,

        Congratulations! You have been placed on the Dean's List for
        exceptional academic performance.

        Your current GPA: %.2f
        Semester: %s %d

        Keep up the excellent work!

        Best regards,
        Academic Affairs Office
        """;
}

// ❌ BAD: Concatenated strings
private static final String FIND_STUDENTS_WITH_HONORS_QUERY =
    "SELECT s.id, s.first_name, s.last_name, s.email, s.gpa " +
    "FROM students s " +
    "WHERE s.gpa >= 3.5 " +
    "AND s.status = 'ACTIVE' " +
    "AND s.enrollment_year >= ? " +
    "ORDER BY s.gpa DESC, s.last_name ASC";
```

### Effective Use of Optionals

```java
// ✅ GOOD: Proper Optional usage
public class StudentService {

    public Optional<Student> findStudentById(String id) {
        return repository.findById(id);
    }

    public Student getStudentOrThrow(String id) {
        return findStudentById(id)
            .orElseThrow(() -> new StudentNotFoundException(id));
    }

    public void processStudent(String studentId) {
        findStudentById(studentId)
            .filter(Student::isActive)
            .ifPresentOrElse(
                this::processActiveStudent,
                () -> log.warn("Student {} is not active or not found", studentId)
            );
    }

    public String getStudentDisplayName(String studentId) {
        return findStudentById(studentId)
            .map(Student::getFullName)
            .orElse("Unknown Student");
    }
}

// ❌ BAD: Misusing Optionals
public class StudentService {

    // Don't use Optional for fields
    private Optional<StudentRepository> repository;

    // Don't use Optional for collections (use empty collections instead)
    public Optional<List<Student>> findStudentsByMajor(String major) {
        List<Student> students = repository.findByMajor(major);
        return students.isEmpty() ? Optional.empty() : Optional.of(students);
    }

    // Don't call get() without checking
    public Student getStudentById(String id) {
        return repository.findById(id).get(); // Dangerous!
    }
}
```

### Immutability and Defensive Programming

```java
// ✅ GOOD: Immutable objects with defensive copying
public final class Course {
    private final String courseId;
    private final String title;
    private final int credits;
    private final List<String> prerequisites;
    private final Schedule schedule;

    public Course(String courseId, String title, int credits,
                  List<String> prerequisites, Schedule schedule) {
        this.courseId = requireNonNull(courseId, "Course ID cannot be null");
        this.title = requireNonNull(title, "Title cannot be null");
        this.credits = validateCredits(credits);
        this.prerequisites = prerequisites != null ?
            Collections.unmodifiableList(new ArrayList<>(prerequisites)) :
            Collections.emptyList();
        this.schedule = schedule;
    }

    public List<String> getPrerequisites() {
        return prerequisites; // Already immutable
    }

    public Course withTitle(String newTitle) {
        return new Course(courseId, newTitle, credits, prerequisites, schedule);
    }

    private static int validateCredits(int credits) {
        if (credits < 1 || credits > 6) {
            throw new IllegalArgumentException("Credits must be between 1 and 6");
        }
        return credits;
    }
}

// ❌ BAD: Mutable object exposing internal state
public class Course {
    private String courseId;
    private String title;
    private int credits;
    private List<String> prerequisites;

    // Exposes mutable internal state
    public List<String> getPrerequisites() {
        return prerequisites;
    }

    // No validation
    public void setCredits(int credits) {
        this.credits = credits;
    }
}
```

## Object-Oriented Design Principles

### Single Responsibility Principle (SRP)

```java
// ✅ GOOD: Each class has a single responsibility
@Service
public class StudentEnrollmentService {
    // Only handles enrollment logic
    public Enrollment enrollStudent(String studentId, String courseId) { }
    public void withdrawStudent(String studentId, String courseId) { }
    public List<Enrollment> getStudentEnrollments(String studentId) { }
}

@Service
public class StudentNotificationService {
    // Only handles student notifications
    public void sendEnrollmentConfirmation(Student student, Course course) { }
    public void sendGradeNotification(Student student, Grade grade) { }
    public void sendWelcomeEmail(Student student) { }
}

@Component
public class StudentValidator {
    // Only handles student validation
    public void validateStudentData(Student student) { }
    public void validateEnrollmentEligibility(String studentId, String courseId) { }
}

// ❌ BAD: Class trying to do everything
@Service
public class StudentService {
    // Enrollment logic
    public Enrollment enrollStudent(String studentId, String courseId) { }

    // Email sending
    public void sendEmail(String to, String subject, String body) { }

    // File operations
    public void exportStudentData(String filename) { }

    // Database operations
    public void backupStudentData() { }

    // Report generation
    public byte[] generateStudentReport() { }
}
```

### Open/Closed Principle (OCP)

```java
// ✅ GOOD: Open for extension, closed for modification
public abstract class GradeCalculator {

    public final double calculateFinalGrade(List<Grade> grades) {
        validateGrades(grades);
        return computeFinalGrade(grades);
    }

    protected abstract double computeFinalGrade(List<Grade> grades);

    private void validateGrades(List<Grade> grades) {
        if (grades == null || grades.isEmpty()) {
            throw new IllegalArgumentException("Grades list cannot be empty");
        }
    }
}

@Component
public class WeightedGradeCalculator extends GradeCalculator {
    @Override
    protected double computeFinalGrade(List<Grade> grades) {
        return grades.stream()
            .mapToDouble(grade -> grade.getPoints() * grade.getWeight())
            .sum() / getTotalWeight(grades);
    }
}

@Component
public class AverageGradeCalculator extends GradeCalculator {
    @Override
    protected double computeFinalGrade(List<Grade> grades) {
        return grades.stream()
            .mapToDouble(Grade::getPoints)
            .average()
            .orElse(0.0);
    }
}

// ❌ BAD: Modifying existing code for new functionality
public class GradeCalculator {
    public double calculateFinalGrade(List<Grade> grades, String calculationType) {
        switch (calculationType) {
            case "WEIGHTED":
                // Weighted calculation logic
                break;
            case "AVERAGE":
                // Average calculation logic
                break;
            case "MEDIAN": // New requirement - modifying existing code
                // Median calculation logic
                break;
            default:
                throw new IllegalArgumentException("Unknown calculation type");
        }
    }
}
```

### Dependency Injection and Inversion of Control

```java
// ✅ GOOD: Proper dependency injection
@Service
public class EnrollmentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final PrerequisiteChecker prerequisiteChecker;

    public EnrollmentService(StudentRepository studentRepository,
                           CourseRepository courseRepository,
                           EnrollmentRepository enrollmentRepository,
                           NotificationService notificationService,
                           PrerequisiteChecker prerequisiteChecker) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
        this.prerequisiteChecker = prerequisiteChecker;
    }

    public Enrollment enrollStudent(String studentId, String courseId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new StudentNotFoundException(studentId));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException(courseId));

        prerequisiteChecker.validatePrerequisites(student, course);

        Enrollment enrollment = createEnrollment(student, course);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        notificationService.sendEnrollmentConfirmation(student, course);

        return savedEnrollment;
    }
}

// ❌ BAD: Direct instantiation and tight coupling
@Service
public class EnrollmentService {

    private StudentRepository studentRepository = new JpaStudentRepository();
    private CourseRepository courseRepository = new JpaCourseRepository();
    private EmailService emailService = new SmtpEmailService();

    public Enrollment enrollStudent(String studentId, String courseId) {
        // Hard to test, tightly coupled
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            throw new StudentNotFoundException(studentId);
        }

        // Direct instantiation makes testing difficult
        PrerequisiteChecker checker = new DefaultPrerequisiteChecker();
        checker.check(student, courseId);

        // Concrete email implementation
        emailService.sendEmail(student.getEmail(), "Enrollment Confirmation", "...");
    }
}
```

## Functional Programming Guidelines

### Effective Use of Streams

```java
// ✅ GOOD: Clear, readable stream operations
@Service
public class StudentAnalyticsService {

    public List<StudentSummary> getTopPerformingStudents(int count) {
        return studentRepository.findAll()
            .stream()
            .filter(Student::isActive)
            .filter(student -> student.getGPA() >= 3.0)
            .sorted(Comparator
                .comparing(Student::getGPA)
                .reversed()
                .thenComparing(Student::getLastName))
            .limit(count)
            .map(this::createStudentSummary)
            .collect(Collectors.toList());
    }

    public Map<String, Double> getAverageGPAByMajor() {
        return studentRepository.findAll()
            .stream()
            .filter(Student::isActive)
            .collect(Collectors.groupingBy(
                Student::getMajor,
                Collectors.averagingDouble(Student::getGPA)
            ));
    }

    public Optional<Student> findStudentWithHighestGPA(String major) {
        return studentRepository.findAll()
            .stream()
            .filter(student -> major.equals(student.getMajor()))
            .filter(Student::isActive)
            .max(Comparator.comparing(Student::getGPA));
    }

    // Break complex operations into smaller, named methods
    public List<Course> getRecommendedCoursesForStudent(String studentId) {
        Student student = getStudent(studentId);

        return courseRepository.findAll()
            .stream()
            .filter(course -> isEligibleForCourse(student, course))
            .filter(course -> matchesStudentInterests(student, course))
            .filter(Course::hasAvailableSeats)
            .sorted(Comparator.comparing(this::calculateCourseRelevanceScore).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    private boolean isEligibleForCourse(Student student, Course course) {
        return course.getPrerequisites()
            .stream()
            .allMatch(prereq -> student.hasCompletedCourse(prereq));
    }
}

// ❌ BAD: Complex, hard-to-read stream operations
public List<Student> getStudents() {
    return studentRepository.findAll().stream()
        .filter(s -> s.getStatus() == StudentStatus.ACTIVE && s.getGPA() >= 2.0 &&
                    s.getEnrollmentYear() >= 2020 && s.getMajor() != null &&
                    !s.getMajor().isEmpty() && s.getEmail() != null &&
                    s.getEmail().contains("@") && s.getLastName() != null)
        .sorted((s1, s2) -> {
            int gpaComp = Double.compare(s2.getGPA(), s1.getGPA());
            if (gpaComp == 0) {
                int yearComp = Integer.compare(s2.getEnrollmentYear(), s1.getEnrollmentYear());
                if (yearComp == 0) {
                    return s1.getLastName().compareToIgnoreCase(s2.getLastName());
                }
                return yearComp;
            }
            return gpaComp;
        })
        .collect(Collectors.toList());
}
```

### Method References and Lambda Expressions

```java
// ✅ GOOD: Appropriate use of method references and lambdas
public class CourseService {

    public List<String> getCourseIds() {
        return courses.stream()
            .map(Course::getCourseId)  // Method reference for simple property access
            .collect(Collectors.toList());
    }

    public List<Course> getActiveCoursesOrderedByTitle() {
        return courses.stream()
            .filter(Course::isActive)  // Method reference for boolean methods
            .sorted(Comparator.comparing(Course::getTitle))  // Method reference with comparing
            .collect(Collectors.toList());
    }

    public void notifyStudentsOfCancellation(String courseId) {
        getEnrolledStudents(courseId)
            .forEach(this::sendCancellationNotification);  // Method reference to instance method
    }

    // Use lambda when the logic is more complex
    public List<Course> getCoursesWithHighDemand(double threshold) {
        return courses.stream()
            .filter(course -> {
                double demandRatio = (double) course.getCurrentEnrollment() / course.getMaxEnrollment();
                return demandRatio >= threshold;
            })
            .collect(Collectors.toList());
    }
}

// ❌ BAD: Verbose lambda expressions where method references would be clearer
public List<String> getCourseIds() {
    return courses.stream()
        .map(course -> course.getCourseId())  // Verbose lambda
        .collect(Collectors.toList());
}

public List<Course> getActiveCoursesOrderedByTitle() {
    return courses.stream()
        .filter(course -> course.isActive())  // Verbose lambda
        .sorted((c1, c2) -> c1.getTitle().compareTo(c2.getTitle()))  // Verbose comparator
        .collect(Collectors.toList());
}
```

### Custom Collectors and Functional Utilities

```java
// ✅ GOOD: Custom collectors for reusable operations
public class CustomCollectors {

    // Custom collector for calculating weighted average
    public static Collector<Grade, ?, Double> toWeightedAverage() {
        return Collector.of(
            () -> new double[2], // [totalWeightedScore, totalWeight]
            (acc, grade) -> {
                acc[0] += grade.getScore() * grade.getWeight();
                acc[1] += grade.getWeight();
            },
            (acc1, acc2) -> new double[]{acc1[0] + acc2[0], acc1[1] + acc2[1]},
            acc -> acc[1] == 0 ? 0.0 : acc[0] / acc[1]
        );
    }

    // Custom collector for grouping by multiple keys
    public static <T> Collector<T, ?, Map<String, Map<String, List<T>>>>
            groupingByMajorAndYear(Function<T, String> majorExtractor,
                                 Function<T, String> yearExtractor) {
        return Collectors.groupingBy(
            majorExtractor,
            Collectors.groupingBy(yearExtractor)
        );
    }

    // Usage example
    public void demonstrateCustomCollectors() {
        // Calculate weighted GPA
        double weightedGPA = grades.stream()
            .collect(CustomCollectors.toWeightedAverage());

        // Group students by major and year
        Map<String, Map<String, List<Student>>> groupedStudents = students.stream()
            .collect(CustomCollectors.groupingByMajorAndYear(
                Student::getMajor,
                student -> String.valueOf(student.getEnrollmentYear())
            ));
    }
}

// Functional utilities for common operations
public class FunctionalUtils {

    // Predicate compositions
    public static final Predicate<Student> IS_HONOR_STUDENT =
        student -> student.getGPA() >= 3.5;

    public static final Predicate<Student> IS_SENIOR_STUDENT =
        student -> student.getEnrollmentYear() <= LocalDate.now().getYear() - 3;

    public static final Predicate<Student> IS_ACTIVE =
        student -> student.getStatus() == StudentStatus.ACTIVE;

    // Function compositions
    public static final Function<Student, String> STUDENT_DISPLAY_NAME =
        student -> String.format("%s (%s)", student.getFullName(), student.getId());

    public static final Function<Course, String> COURSE_DESCRIPTION =
        course -> String.format("%s - %s (%d credits)",
            course.getCourseId(), course.getTitle(), course.getCredits());

    // Utility methods for chaining predicates
    public static <T> Predicate<T> and(Predicate<T>... predicates) {
        return Arrays.stream(predicates)
            .reduce(Predicate::and)
            .orElse(x -> true);
    }

    public static <T> Predicate<T> or(Predicate<T>... predicates) {
        return Arrays.stream(predicates)
            .reduce(Predicate::or)
            .orElse(x -> false);
    }

    // Usage example
    public List<Student> findQualifiedStudents() {
        return students.stream()
            .filter(and(IS_ACTIVE, IS_HONOR_STUDENT, IS_SENIOR_STUDENT))
            .collect(Collectors.toList());
    }
}
```

## Error Handling and Exceptions

### Exception Hierarchy Design

```java
// ✅ GOOD: Well-structured exception hierarchy
public abstract class SmartCampusException extends Exception {
    private final String errorCode;
    private final Map<String, Object> context;

    protected SmartCampusException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    protected SmartCampusException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public SmartCampusException addContext(String key, Object value) {
        context.put(key, value);
        return this;
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
}

// Domain-specific exceptions
public class StudentNotFoundException extends SmartCampusException {
    public StudentNotFoundException(String studentId) {
        super("STUDENT_NOT_FOUND", "Student with ID " + studentId + " not found");
        addContext("studentId", studentId);
    }
}

public class EnrollmentException extends SmartCampusException {
    public EnrollmentException(String message, String studentId, String courseId) {
        super("ENROLLMENT_ERROR", message);
        addContext("studentId", studentId);
        addContext("courseId", courseId);
    }

    public EnrollmentException(String message, String studentId, String courseId, Throwable cause) {
        super("ENROLLMENT_ERROR", message, cause);
        addContext("studentId", studentId);
        addContext("courseId", courseId);
    }
}

// Specific enrollment exceptions
public class PrerequisiteNotMetException extends EnrollmentException {
    public PrerequisiteNotMetException(String studentId, String courseId, List<String> missingPrereqs) {
        super("Prerequisites not met for course enrollment", studentId, courseId);
        addContext("missingPrerequisites", missingPrereqs);
    }
}

public class CourseCapacityExceededException extends EnrollmentException {
    public CourseCapacityExceededException(String studentId, String courseId, int capacity, int enrolled) {
        super("Course has reached maximum capacity", studentId, courseId);
        addContext("maxCapacity", capacity);
        addContext("currentEnrollment", enrolled);
    }
}

// ❌ BAD: Generic exceptions without context
public class StudentException extends Exception {
    public StudentException(String message) {
        super(message);
    }
}
```

### Proper Exception Handling

```java
// ✅ GOOD: Comprehensive error handling
@Service
public class EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

    public EnrollmentResult enrollStudent(String studentId, String courseId) {
        try {
            logger.info("Starting enrollment process: studentId={}, courseId={}", studentId, courseId);

            // Validate input parameters
            validateEnrollmentRequest(studentId, courseId);

            // Load entities with proper error handling
            Student student = loadStudent(studentId);
            Course course = loadCourse(courseId);

            // Business rule validations
            validateEnrollmentEligibility(student, course);

            // Process enrollment
            Enrollment enrollment = processEnrollment(student, course);

            // Send notifications with error recovery
            sendNotifications(student, course, enrollment);

            logger.info("Enrollment completed successfully: enrollmentId={}", enrollment.getId());
            return EnrollmentResult.success(enrollment);

        } catch (ValidationException e) {
            logger.warn("Validation failed for enrollment: studentId={}, courseId={}, error={}",
                studentId, courseId, e.getMessage());
            return EnrollmentResult.failure("VALIDATION_ERROR", e.getMessage());

        } catch (StudentNotFoundException | CourseNotFoundException e) {
            logger.error("Entity not found during enrollment: studentId={}, courseId={}, error={}",
                studentId, courseId, e.getMessage());
            return EnrollmentResult.failure(e.getErrorCode(), e.getMessage());

        } catch (EnrollmentException e) {
            logger.error("Enrollment business rule violation: studentId={}, courseId={}, error={}, context={}",
                studentId, courseId, e.getMessage(), e.getContext());
            return EnrollmentResult.failure(e.getErrorCode(), e.getMessage());

        } catch (DataAccessException e) {
            logger.error("Database error during enrollment: studentId={}, courseId={}",
                studentId, courseId, e);
            return EnrollmentResult.failure("DATABASE_ERROR", "Unable to process enrollment due to system error");

        } catch (Exception e) {
            logger.error("Unexpected error during enrollment: studentId={}, courseId={}",
                studentId, courseId, e);
            return EnrollmentResult.failure("SYSTEM_ERROR", "An unexpected error occurred");
        }
    }

    private Student loadStudent(String studentId) {
        return studentRepository.findById(studentId)
            .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private Course loadCourse(String courseId) {
        return courseRepository.findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private void validateEnrollmentRequest(String studentId, String courseId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new ValidationException("Student ID cannot be null or empty");
        }
        if (courseId == null || courseId.trim().isEmpty()) {
            throw new ValidationException("Course ID cannot be null or empty");
        }
    }

    private void validateEnrollmentEligibility(Student student, Course course) {
        // Check if student is active
        if (!student.isActive()) {
            throw new EnrollmentException("Student is not active", student.getId(), course.getCourseId())
                .addContext("studentStatus", student.getStatus());
        }

        // Check prerequisites
        List<String> missingPrereqs = findMissingPrerequisites(student, course);
        if (!missingPrereqs.isEmpty()) {
            throw new PrerequisiteNotMetException(student.getId(), course.getCourseId(), missingPrereqs);
        }

        // Check course capacity
        if (!course.hasAvailableSeats()) {
            throw new CourseCapacityExceededException(
                student.getId(),
                course.getCourseId(),
                course.getMaxEnrollment(),
                course.getCurrentEnrollment()
            );
        }
    }

    private void sendNotifications(Student student, Course course, Enrollment enrollment) {
        try {
            notificationService.sendEnrollmentConfirmation(student, course, enrollment);
        } catch (Exception e) {
            // Log but don't fail the enrollment for notification errors
            logger.warn("Failed to send enrollment notification: studentId={}, courseId={}, error={}",
                student.getId(), course.getCourseId(), e.getMessage());
        }
    }
}

// ❌ BAD: Poor exception handling
@Service
public class EnrollmentService {

    public Enrollment enrollStudent(String studentId, String courseId) {
        try {
            Student student = studentRepository.findById(studentId).get(); // Dangerous!
            Course course = courseRepository.findById(courseId).get(); // Dangerous!

            if (student.getGPA() < 2.0) {
                throw new RuntimeException("GPA too low"); // Generic exception
            }

            if (course.getCurrentEnrollment() >= course.getMaxEnrollment()) {
                throw new Exception("Course full"); // Checked exception for control flow
            }

            return enrollmentRepository.save(new Enrollment(student, course));

        } catch (Exception e) {
            e.printStackTrace(); // Poor logging
            throw new RuntimeException("Something went wrong"); // Loss of original context
        }
    }
}
```

### Result Objects for Better Error Handling

```java
// ✅ GOOD: Result objects for handling success/failure scenarios
public sealed interface EnrollmentResult
    permits EnrollmentResult.Success, EnrollmentResult.Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    record Success(Enrollment enrollment) implements EnrollmentResult {
        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure(String errorCode, String message, Map<String, Object> context)
        implements EnrollmentResult {

        public Failure(String errorCode, String message) {
            this(errorCode, message, Collections.emptyMap());
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    static Success success(Enrollment enrollment) {
        return new Success(enrollment);
    }

    static Failure failure(String errorCode, String message) {
        return new Failure(errorCode, message);
    }

    static Failure failure(String errorCode, String message, Map<String, Object> context) {
        return new Failure(errorCode, message, context);
    }
}

// Usage example
public void handleEnrollmentResult(EnrollmentResult result) {
    switch (result) {
        case EnrollmentResult.Success success -> {
            System.out.println("Enrollment successful: " + success.enrollment().getId());
            sendConfirmationEmail(success.enrollment());
        }
        case EnrollmentResult.Failure failure -> {
            System.err.println("Enrollment failed: " + failure.message());
            logFailureDetails(failure.errorCode(), failure.context());
            notifyAdministrator(failure);
        }
    }
}
```

## Testing Best Practices

### Unit Testing Guidelines

```java
// ✅ GOOD: Well-structured unit tests
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private ValidationUtil validator;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StudentService studentService;

    @Nested
    @DisplayName("Student Creation Tests")
    class StudentCreationTests {

        @Test
        @DisplayName("Should create student with valid data")
        void shouldCreateStudentWithValidData() {
            // Given
            Student inputStudent = createValidStudent();
            Student savedStudent = createValidStudentWithId();

            when(validator.validate(inputStudent)).thenReturn(ValidationResult.valid());
            when(repository.save(inputStudent)).thenReturn(savedStudent);

            // When
            Student result = studentService.createStudent(inputStudent);

            // Then
            assertThat(result)
                .isNotNull()
                .extracting(Student::getId, Student::getFullName, Student::getEmail)
                .containsExactly("STU001", "John Doe", "john.doe@test.edu");

            verify(validator).validate(inputStudent);
            verify(repository).save(inputStudent);
            verify(notificationService).sendWelcomeEmail(savedStudent);
        }

        @Test
        @DisplayName("Should throw ValidationException when student data is invalid")
        void shouldThrowValidationExceptionWhenStudentDataIsInvalid() {
            // Given
            Student invalidStudent = createInvalidStudent();
            ValidationResult validationResult = ValidationResult.invalid("Invalid email format");

            when(validator.validate(invalidStudent)).thenReturn(validationResult);

            // When & Then
            assertThatThrownBy(() -> studentService.createStudent(invalidStudent))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid email format");

            verify(validator).validate(invalidStudent);
            verify(repository, never()).save(any());
            verify(notificationService, never()).sendWelcomeEmail(any());
        }

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            Student validStudent = createValidStudent();

            when(validator.validate(validStudent)).thenReturn(ValidationResult.valid());
            when(repository.save(validStudent)).thenThrow(new DataAccessException("Database error"));

            // When & Then
            assertThatThrownBy(() -> studentService.createStudent(validStudent))
                .isInstanceOf(StudentCreationException.class)
                .hasMessage("Failed to create student")
                .hasCauseInstanceOf(DataAccessException.class);

            verify(notificationService, never()).sendWelcomeEmail(any());
        }

        @ParameterizedTest
        @DisplayName("Should validate GPA ranges correctly")
        @ValueSource(doubles = {-1.0, -0.1, 4.1, 5.0, Double.MAX_VALUE})
        void shouldValidateGPARangesCorrectly(double invalidGPA) {
            // Given
            Student studentWithInvalidGPA = createStudentWithGPA(invalidGPA);
            ValidationResult validationResult = ValidationResult.invalid("GPA must be between 0.0 and 4.0");

            when(validator.validate(studentWithInvalidGPA)).thenReturn(validationResult);

            // When & Then
            assertThatThrownBy(() -> studentService.createStudent(studentWithInvalidGPA))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("GPA must be between 0.0 and 4.0");
        }

        @ParameterizedTest
        @DisplayName("Should accept valid GPA values")
        @ValueSource(doubles = {0.0, 2.5, 3.5, 4.0})
        void shouldAcceptValidGPAValues(double validGPA) {
            // Given
            Student studentWithValidGPA = createStudentWithGPA(validGPA);
            Student savedStudent = createStudentWithGPAAndId(validGPA, "STU001");

            when(validator.validate(studentWithValidGPA)).thenReturn(ValidationResult.valid());
            when(repository.save(studentWithValidGPA)).thenReturn(savedStudent);

            // When
            Student result = studentService.createStudent(studentWithValidGPA);

            // Then
            assertThat(result.getGPA()).isEqualTo(validGPA);
            verify(repository).save(studentWithValidGPA);
        }
    }

    @Nested
    @DisplayName("Student Search Tests")
    class StudentSearchTests {

        @Test
        @DisplayName("Should find students by major")
        void shouldFindStudentsByMajor() {
            // Given
            String major = "Computer Science";
            List<Student> expectedStudents = createStudentsWithMajor(major, 3);

            when(repository.findByMajor(major)).thenReturn(expectedStudents);

            // When
            List<Student> result = studentService.findStudentsByMajor(major);

            // Then
            assertThat(result)
                .hasSize(3)
                .extracting(Student::getMajor)
                .containsOnly(major);

            verify(repository).findByMajor(major);
        }

        @Test
        @DisplayName("Should return empty list when no students found for major")
        void shouldReturnEmptyListWhenNoStudentsFoundForMajor() {
            // Given
            String major = "Nonexistent Major";

            when(repository.findByMajor(major)).thenReturn(Collections.emptyList());

            // When
            List<Student> result = studentService.findStudentsByMajor(major);

            // Then
            assertThat(result).isEmpty();
            verify(repository).findByMajor(major);
        }
    }

    // Test data builders
    private Student createValidStudent() {
        return new StudentBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@test.edu")
            .setMajor("Computer Science")
            .setGPA(3.5)
            .build();
    }

    private Student createValidStudentWithId() {
        return new StudentBuilder()
            .setId("STU001")
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@test.edu")
            .setMajor("Computer Science")
            .setGPA(3.5)
            .build();
    }

    private Student createInvalidStudent() {
        return new StudentBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("invalid-email")
            .setMajor("Computer Science")
            .setGPA(3.5)
            .build();
    }

    private Student createStudentWithGPA(double gpa) {
        return new StudentBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@test.edu")
            .setMajor("Computer Science")
            .setGPA(gpa)
            .build();
    }
}

// ❌ BAD: Poor test structure
class StudentServiceTest {

    @Test
    void testStudent() { // Unclear test name
        StudentService service = new StudentService(); // No mocking
        Student student = new Student(); // No proper test data setup
        student.setName("John"); // Using setters

        Student result = service.createStudent(student); // No exception handling in test

        assert result != null; // Poor assertion
        System.out.println("Test passed"); // Console output in tests
    }

    @Test
    void testBadStudent() { // Testing multiple scenarios in one test
        StudentService service = new StudentService();

        // Test 1: Null student
        try {
            service.createStudent(null);
            fail("Should have thrown exception");
        } catch (Exception e) {
            // Expected
        }

        // Test 2: Invalid email
        Student student = new Student();
        student.setEmail("bad-email");
        try {
            service.createStudent(student);
            fail("Should have thrown exception");
        } catch (Exception e) {
            // Expected
        }

        // Test 3: Valid student
        student.setEmail("good@email.com");
        Student result = service.createStudent(student);
        assert result != null;
    }
}
```

### Integration Testing

```java
// ✅ GOOD: Comprehensive integration tests
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@Transactional
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should complete full student lifecycle")
    void shouldCompleteFullStudentLifecycle() {
        // Given - Create a student
        Student student = new StudentBuilder()
            .setFirstName("Integration")
            .setLastName("Test")
            .setEmail("integration.test@test.edu")
            .setMajor("Computer Science")
            .setGPA(3.7)
            .build();

        // When - Create student
        Student createdStudent = studentService.createStudent(student);
        entityManager.flush();

        // Then - Verify student creation
        assertThat(createdStudent.getId()).isNotNull();

        // When - Create a course
        Course course = new CourseBuilder()
            .setCourseId("CS101")
            .setTitle("Introduction to Programming")
            .setCredits(3)
            .setMaxEnrollment(30)
            .build();

        Course createdCourse = courseService.createCourse(course);
        entityManager.flush();

        // When - Enroll student in course
        Enrollment enrollment = enrollmentService.enrollStudent(
            createdStudent.getId(),
            createdCourse.getCourseId()
        );
        entityManager.flush();

        // Then - Verify enrollment
        assertThat(enrollment)
            .isNotNull()
            .extracting(Enrollment::getStudentId, Enrollment::getCourseId, Enrollment::getStatus)
            .containsExactly(createdStudent.getId(), createdCourse.getCourseId(), EnrollmentStatus.ENROLLED);

        // Verify student-course relationship
        List<Course> studentCourses = studentService.getStudentCourses(createdStudent.getId());
        assertThat(studentCourses)
            .hasSize(1)
            .extracting(Course::getCourseId)
            .containsExactly("CS101");

        // When - Update student GPA
        Student updatedStudent = new StudentBuilder()
            .from(createdStudent)
            .setGPA(3.9)
            .build();

        Student savedUpdatedStudent = studentService.updateStudent(updatedStudent);
        entityManager.flush();

        // Then - Verify update
        assertThat(savedUpdatedStudent.getGPA()).isEqualTo(3.9);

        // Verify data consistency
        Student retrievedStudent = studentService.findStudentById(createdStudent.getId())
            .orElseThrow();
        assertThat(retrievedStudent.getGPA()).isEqualTo(3.9);
    }

    @Test
    @DisplayName("Should handle concurrent enrollments correctly")
    void shouldHandleConcurrentEnrollmentsCorrectly() throws InterruptedException {
        // Given - Create course with limited capacity
        Course limitedCourse = new CourseBuilder()
            .setCourseId("LIMITED101")
            .setTitle("Limited Capacity Course")
            .setCredits(3)
            .setMaxEnrollment(2)
            .build();

        courseService.createCourse(limitedCourse);

        // Create multiple students
        List<Student> students = IntStream.range(1, 6)
            .mapToObj(i -> new StudentBuilder()
                .setFirstName("Student" + i)
                .setLastName("Test")
                .setEmail("student" + i + "@test.edu")
                .setMajor("Computer Science")
                .setGPA(3.0)
                .build())
            .map(studentService::createStudent)
            .collect(Collectors.toList());

        entityManager.flush();

        // When - Attempt concurrent enrollments
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(students.size());
        List<Future<EnrollmentResult>> futures = new ArrayList<>();

        for (Student student : students) {
            Future<EnrollmentResult> future = executor.submit(() -> {
                try {
                    Enrollment enrollment = enrollmentService.enrollStudent(
                        student.getId(),
                        limitedCourse.getCourseId()
                    );
                    return EnrollmentResult.success(enrollment);
                } catch (Exception e) {
                    return EnrollmentResult.failure("ENROLLMENT_FAILED", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - Verify only 2 students enrolled (capacity limit)
        List<EnrollmentResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    return EnrollmentResult.failure("FUTURE_ERROR", e.getMessage());
                }
            })
            .collect(Collectors.toList());

        long successfulEnrollments = results.stream()
            .mapToLong(result -> result.isSuccess() ? 1 : 0)
            .sum();

        assertThat(successfulEnrollments).isEqualTo(2);

        // Verify course enrollment count
        Course updatedCourse = courseService.findById(limitedCourse.getCourseId())
            .orElseThrow();
        assertThat(updatedCourse.getCurrentEnrollment()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should roll back transaction on enrollment failure")
    @Rollback
    void shouldRollBackTransactionOnEnrollmentFailure() {
        // Given - Create student and course
        Student student = studentService.createStudent(createValidStudent());
        Course course = courseService.createCourse(createValidCourse());
        entityManager.flush();

        // Simulate a service that will fail after partial operations
        // This would typically involve mocking a dependency to throw an exception

        // When - Attempt enrollment that should fail
        assertThatThrownBy(() -> {
            // Simulate complex enrollment process that fails partway through
            enrollmentService.enrollStudentWithComplexValidation(student.getId(), course.getCourseId());
        }).isInstanceOf(EnrollmentException.class);

        // Then - Verify rollback occurred (no partial data saved)
        List<Enrollment> enrollments = enrollmentService.findEnrollmentsByStudent(student.getId());
        assertThat(enrollments).isEmpty();
    }

    // Helper methods
    private Student createValidStudent() {
        return new StudentBuilder()
            .setFirstName("Test")
            .setLastName("Student")
            .setEmail("test.student@test.edu")
            .setMajor("Computer Science")
            .setGPA(3.0)
            .build();
    }

    private Course createValidCourse() {
        return new CourseBuilder()
            .setCourseId("TEST101")
            .setTitle("Test Course")
            .setCredits(3)
            .setMaxEnrollment(30)
            .build();
    }
}
```

## Performance Optimization

### Database Query Optimization

```java
// ✅ GOOD: Optimized database queries
@Repository
public class OptimizedStudentRepository {

    private final EntityManager entityManager;

    // Use pagination for large result sets
    public Page<Student> findStudentsByMajor(String major, Pageable pageable) {
        String countQuery = "SELECT COUNT(s) FROM Student s WHERE s.major = :major";
        String selectQuery = """
            SELECT s FROM Student s
            LEFT JOIN FETCH s.enrollments e
            LEFT JOIN FETCH e.course c
            WHERE s.major = :major
            ORDER BY s.lastName, s.firstName
            """;

        // Get total count
        Long total = entityManager.createQuery(countQuery, Long.class)
            .setParameter("major", major)
            .getSingleResult();

        // Get page data with fetch joins
        List<Student> students = entityManager.createQuery(selectQuery, Student.class)
            .setParameter("major", major)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        return new PageImpl<>(students, pageable, total);
    }

    // Use projections for read-only data
    @Query("""
        SELECT new com.smartcampus.dto.StudentSummaryDTO(
            s.id, s.firstName, s.lastName, s.email, s.major, s.gpa
        )
        FROM Student s
        WHERE s.gpa >= :minGPA
        ORDER BY s.gpa DESC
        """)
    List<StudentSummaryDTO> findStudentSummaries(@Param("minGPA") double minGPA);

    // Batch operations for better performance
    @Modifying
    @Query("UPDATE Student s SET s.status = :status WHERE s.id IN :studentIds")
    int updateStudentStatusBatch(@Param("studentIds") List<String> studentIds,
                                @Param("status") StudentStatus status);

    // Use native queries for complex operations
    @Query(value = """
        SELECT s.major,
               AVG(s.gpa) as avg_gpa,
               COUNT(*) as student_count,
               PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY s.gpa) as median_gpa
        FROM students s
        WHERE s.status = 'ACTIVE'
        GROUP BY s.major
        HAVING COUNT(*) >= :minStudentCount
        ORDER BY avg_gpa DESC
        """, nativeQuery = true)
    List<Object[]> findMajorStatistics(@Param("minStudentCount") int minStudentCount);

    // Efficient exists query
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END FROM Student s WHERE s.email = :email")
    boolean existsByEmail(@Param("email") String email);
}

// ❌ BAD: Inefficient queries
@Repository
public class InfficientStudentRepository {

    // N+1 query problem
    public List<Student> findStudentsWithCourses() {
        List<Student> students = findAll(); // Query 1
        for (Student student : students) {
            student.getEnrollments().size(); // Query 2, 3, 4, ... N+1
        }
        return students;
    }

    // Loading all data when only subset needed
    public List<String> getAllStudentEmails() {
        return findAll().stream() // Loads all student data
            .map(Student::getEmail)
            .collect(Collectors.toList());
    }

    // Inefficient counting
    public boolean hasStudentsInMajor(String major) {
        return !findByMajor(major).isEmpty(); // Loads all data just to check existence
    }
}
```

### Caching Strategies

```java
// ✅ GOOD: Strategic caching implementation
@Service
public class CachedStudentService {

    private final StudentRepository repository;
    private final CacheManager cacheManager;

    // Cache frequently accessed single entities
    @Cacheable(value = "students", key = "#id", unless = "#result == null")
    public Optional<Student> findStudentById(String id) {
        return repository.findById(id);
    }

    // Cache collections with appropriate key
    @Cacheable(value = "studentsByMajor", key = "#major")
    public List<StudentSummaryDTO> findStudentSummariesByMajor(String major) {
        return repository.findStudentSummariesByMajor(major);
    }

    // Evict cache on updates
    @CacheEvict(value = {"students", "studentsByMajor"},
                key = "#student.id",
                beforeInvocation = false)
    public Student updateStudent(Student student) {
        return repository.save(student);
    }

    // Conditional caching based on data size
    @Cacheable(value = "majorStatistics",
               key = "#root.methodName",
               condition = "#result.size() <= 50")
    public List<MajorStatistics> getMajorStatistics() {
        return repository.calculateMajorStatistics();
    }

    // Cache with TTL using @CacheEvict scheduled task
    @Scheduled(fixedRate = 300000) // 5 minutes
    @CacheEvict(value = "studentsByMajor", allEntries = true)
    public void evictStudentsByMajorCache() {
        // Automatic cache refresh
    }

    // Manual cache management for complex scenarios
    public List<Student> findStudentsWithSmartCaching(String major, boolean forceRefresh) {
        Cache cache = cacheManager.getCache("complexStudentQueries");
        String cacheKey = "students-major-" + major;

        if (!forceRefresh) {
            List<Student> cached = cache.get(cacheKey, List.class);
            if (cached != null) {
                return cached;
            }
        }

        List<Student> students = repository.findByMajorWithEnrollments(major);
        cache.put(cacheKey, students);

        return students;
    }
}

// Cache configuration
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats());

        cacheManager.setCacheNames(Arrays.asList(
            "students", "courses", "enrollments",
            "studentsByMajor", "majorStatistics"
        ));

        return cacheManager;
    }

    @Bean
    public CacheMetricsRegistrar cacheMetricsRegistrar(MeterRegistry meterRegistry) {
        return new CacheMetricsRegistrar(meterRegistry);
    }
}
```

### Asynchronous Processing

```java
// ✅ GOOD: Effective asynchronous processing
@Service
public class AsyncEnrollmentService {

    private final EnrollmentRepository repository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Async("enrollmentTaskExecutor")
    public CompletableFuture<Enrollment> processEnrollmentAsync(String studentId, String courseId) {
        try {
            // Heavy enrollment processing
            Enrollment enrollment = processEnrollment(studentId, courseId);

            // Trigger async notifications (fire-and-forget)
            CompletableFuture.runAsync(() ->
                notificationService.sendEnrollmentNotification(enrollment),
                taskExecutor);

            // Async audit logging
            CompletableFuture.runAsync(() ->
                auditService.logEnrollment(enrollment),
                taskExecutor);

            return CompletableFuture.completedFuture(enrollment);

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<EnrollmentResult>> processBatchEnrollments(
            List<EnrollmentRequest> requests) {

        // Process requests in parallel with controlled concurrency
        List<CompletableFuture<EnrollmentResult>> futures = requests.stream()
            .map(request -> processEnrollmentAsync(request.getStudentId(), request.getCourseId())
                .thenApply(EnrollmentResult::success)
                .exceptionally(throwable -> EnrollmentResult.failure(throwable.getMessage())))
            .collect(Collectors.toList());

        // Combine all results
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    // Circuit breaker for external service calls
    @CircuitBreaker(name = "external-grade-service", fallbackMethod = "fallbackGradeCalculation")
    @Async
    public CompletableFuture<Double> calculateGPAAsync(String studentId) {
        return externalGradeService.calculateGPA(studentId);
    }

    public CompletableFuture<Double> fallbackGradeCalculation(String studentId, Exception ex) {
        // Fallback to local calculation
        return CompletableFuture.supplyAsync(() ->
            localGradeService.calculateGPA(studentId));
    }
}

// Async configuration
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "enrollmentTaskExecutor")
    public TaskExecutor enrollmentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Enrollment-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationTaskExecutor")
    public TaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }
}
```

This comprehensive best practices guide provides detailed examples and guidelines for writing high-quality, maintainable Java code in the SmartCampus project. Following these practices ensures consistency, reliability, and scalability across the entire codebase.
