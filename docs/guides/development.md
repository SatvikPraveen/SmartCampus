# SmartCampus Development Guide

**Location:** `docs/guides/development.md`

## Overview

This guide provides comprehensive information for developers working on the SmartCampus project. It covers coding standards, development workflows, testing strategies, and contribution guidelines.

## Development Environment Setup

### Required Tools

#### Core Development Tools

- **Java Development Kit (JDK) 17+**

  - Features used: Records, Pattern Matching, Text Blocks, Sealed Classes
  - Download: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)

- **Apache Maven 3.8+**

  - Build automation and dependency management
  - Configuration: `pom.xml`

- **Git**
  - Version control system
  - Required for contribution workflow

#### Recommended IDE Setup

##### IntelliJ IDEA (Recommended)

1. **Project Setup**

   - Open project directory
   - Set Project SDK to Java 17+
   - Enable annotation processing
   - Install recommended plugins:
     - SonarLint
     - CheckStyle-IDEA
     - SpotBugs
     - Maven Helper

2. **Code Style Configuration**

   ```xml
   <!-- .editorconfig file -->
   root = true

   [*.java]
   indent_style = space
   indent_size = 4
   end_of_line = lf
   charset = utf-8
   trim_trailing_whitespace = true
   insert_final_newline = true
   max_line_length = 120
   ```

3. **Live Templates**
   - Create custom templates for common patterns
   - Example: Logger declaration, Builder pattern setup

##### Eclipse Setup

1. **Import Project**
   - File → Import → Existing Maven Projects
   - Select SmartCampus directory
2. **Configure Java Build Path**
   - Right-click project → Properties → Java Build Path
   - Ensure JRE version is 17+
3. **Code Formatter**
   - Import Eclipse formatter configuration
   - Window → Preferences → Java → Code Style → Formatter

##### Visual Studio Code

1. **Extensions**

   ```json
   {
     "recommendations": [
       "vscjava.vscode-java-pack",
       "vscjava.vscode-maven",
       "sonarsource.sonarlint-vscode",
       "shengchen.vscode-checkstyle"
     ]
   }
   ```

### Development Workflow

#### Branch Strategy

We follow GitFlow branching model:

```bash
main
├── develop
│   ├── feature/student-management
│   ├── feature/grade-calculation
│   └── feature/reporting-system
├── release/v1.0.0
└── hotfix/critical-security-fix
```

#### Branch Naming Convention

- `feature/descriptive-name` - New features
- `bugfix/issue-description` - Bug fixes
- `hotfix/critical-issue` - Critical production fixes
- `release/version-number` - Release preparation
- `chore/maintenance-task` - Maintenance tasks

#### Commit Message Format

```bash
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, semicolons, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**

```bash
feat(enrollment): add batch enrollment processing

Implement concurrent batch processing for student enrollments
using CompletableFuture and custom thread pool.

Closes #123
```

```bash
fix(grade): resolve GPA calculation precision issue

Fix floating-point precision errors in GPA calculations
by using BigDecimal for monetary calculations.

Fixes #456
```

## Code Standards and Conventions

### Java Coding Standards

#### Naming Conventions

```java
// Classes: PascalCase
public class StudentService { }

// Interfaces: PascalCase with descriptive names
public interface Enrollable { }
public interface CrudOperations<T, ID> { }

// Methods: camelCase with verb-noun format
public void enrollStudent(Student student, Course course) { }
public List<Student> findStudentsByMajor(String major) { }

// Variables: camelCase
private String studentId;
private List<Course> enrolledCourses;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_ENROLLMENT_ATTEMPTS = 3;
private static final String DEFAULT_SEMESTER = "FALL";

// Packages: lowercase with dots
com.smartcampus.services
com.smartcampus.models.entities
```

#### Class Structure Order

```java
public class StudentService {
    // 1. Static fields
    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);

    // 2. Instance fields
    private final StudentRepository repository;
    private final EnrollmentService enrollmentService;

    // 3. Constructors
    public StudentService(StudentRepository repository, EnrollmentService enrollmentService) {
        this.repository = repository;
        this.enrollmentService = enrollmentService;
    }

    // 4. Public methods
    public Student createStudent(StudentBuilder builder) {
        // Implementation
    }

    // 5. Package-private methods
    void validateStudent(Student student) {
        // Implementation
    }

    // 6. Private methods
    private boolean isValidEmail(String email) {
        // Implementation
    }

    // 7. Static methods
    public static StudentBuilder builder() {
        return new StudentBuilder();
    }
}
```

#### Modern Java Features Usage

##### Records (Java 14+)

```java
// Use records for DTOs and value objects
public record StudentDTO(
    String id,
    String fullName,
    String email,
    String major,
    double gpa
) {
    // Compact constructor for validation
    public StudentDTO {
        Objects.requireNonNull(id, "Student ID cannot be null");
        if (gpa < 0.0 || gpa > 4.0) {
            throw new IllegalArgumentException("GPA must be between 0.0 and 4.0");
        }
    }
}
```

##### Pattern Matching (Java 17+)

```java
public String getStatusMessage(EnrollmentStatus status) {
    return switch (status) {
        case ENROLLED -> "Student is currently enrolled";
        case COMPLETED -> "Student has completed the course";
        case WITHDRAWN -> "Student has withdrawn from the course";
        case FAILED -> "Student did not meet course requirements";
        case PENDING -> "Enrollment is pending approval";
    };
}
```

##### Sealed Classes (Java 17+)

```java
public sealed abstract class Event
    permits StudentEnrolledEvent, GradeUpdatedEvent, CourseCreatedEvent {

    private final String eventId;
    private final LocalDateTime timestamp;

    protected Event(String eventId) {
        this.eventId = eventId;
        this.timestamp = LocalDateTime.now();
    }
}
```

##### Text Blocks (Java 15+)

```java
private static final String SQL_FIND_STUDENTS_BY_MAJOR = """
    SELECT s.id, s.first_name, s.last_name, s.email, s.gpa
    FROM students s
    WHERE s.major = ?
      AND s.status = 'ACTIVE'
    ORDER BY s.last_name, s.first_name
    """;
```

### Functional Programming Practices

#### Stream API Usage

```java
// Good: Readable and functional
public List<StudentDTO> getHonorStudents(String major) {
    return students.stream()
        .filter(student -> major.equals(student.getMajor()))
        .filter(student -> student.getGPA() >= 3.5)
        .map(this::convertToDTO)
        .sorted(Comparator.comparing(StudentDTO::fullName))
        .collect(Collectors.toList());
}

// Avoid: Overly complex streams
// Break down complex operations into smaller, named methods
public List<EnrollmentSummary> getComplexEnrollmentReport() {
    return students.stream()
        .filter(this::isActiveStudent)
        .map(this::getStudentEnrollments)
        .filter(this::hasValidEnrollments)
        .map(this::createEnrollmentSummary)
        .collect(Collectors.toList());
}
```

#### Method References and Lambda Usage

```java
// Method references when possible
students.stream()
    .map(Student::getFullName)  // Instead of s -> s.getFullName()
    .forEach(System.out::println);

// Lambda for complex logic
students.stream()
    .filter(s -> s.getGPA() > 3.0 && s.getEnrollmentYear() >= 2020)
    .collect(Collectors.groupingBy(
        Student::getMajor,
        Collectors.counting()
    ));
```

### Error Handling Best Practices

#### Custom Exception Hierarchy

```java
// Base application exception
public abstract class SmartCampusException extends Exception {
    private final String errorCode;

    protected SmartCampusException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected SmartCampusException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

// Specific exceptions
public class StudentNotFoundException extends SmartCampusException {
    public StudentNotFoundException(String studentId) {
        super("STUDENT_NOT_FOUND", "Student with ID " + studentId + " not found");
    }
}

public class EnrollmentException extends SmartCampusException {
    public EnrollmentException(String message, Throwable cause) {
        super("ENROLLMENT_ERROR", message, cause);
    }
}
```

#### Exception Handling Patterns

```java
// Use Optional for null handling
public Optional<Student> findStudentById(String id) {
    try {
        Student student = repository.findById(id);
        return Optional.ofNullable(student);
    } catch (DataAccessException e) {
        logger.error("Database error while finding student: {}", id, e);
        return Optional.empty();
    }
}

// Fail-fast validation
public void enrollStudent(String studentId, String courseId) {
    Objects.requireNonNull(studentId, "Student ID cannot be null");
    Objects.requireNonNull(courseId, "Course ID cannot be null");

    Student student = findStudentById(studentId)
        .orElseThrow(() -> new StudentNotFoundException(studentId));

    Course course = findCourseById(courseId)
        .orElseThrow(() -> new CourseNotFoundException(courseId));

    // Proceed with enrollment logic
}
```

### Documentation Standards

#### JavaDoc Guidelines

```java
/**
 * Service class for managing student operations and enrollment workflows.
 *
 * <p>This service provides comprehensive student management capabilities including:
 * <ul>
 *   <li>Student registration and profile management</li>
 *   <li>Course enrollment and withdrawal</li>
 *   <li>GPA calculation and academic standing</li>
 *   <li>Transcript generation and reporting</li>
 * </ul>
 *
 * <p>All operations are thread-safe and support concurrent access.
 *
 * @author SmartCampus Development Team
 * @version 1.0
 * @since 1.0
 */
public class StudentService {

    /**
     * Enrolls a student in the specified course.
     *
     * <p>This method validates prerequisites, checks course capacity,
     * and processes the enrollment request asynchronously.
     *
     * @param studentId the unique identifier of the student, must not be null
     * @param courseId the unique identifier of the course, must not be null
     * @return a CompletableFuture that completes with the enrollment record
     * @throws StudentNotFoundException if the student does not exist
     * @throws CourseNotFoundException if the course does not exist
     * @throws EnrollmentException if enrollment fails due to business rules
     * @throws IllegalArgumentException if any parameter is null
     *
     * @see #withdrawStudent(String, String)
     * @see EnrollmentService#processEnrollment(Student, Course)
     */
    public CompletableFuture<Enrollment> enrollStudent(String studentId, String courseId) {
        // Implementation
    }
}
```

#### Code Comments

```java
// Use comments sparingly - prefer self-documenting code
public class GradeCalculator {

    // Explain "why" not "what"
    // Using BigDecimal to avoid floating-point precision issues in financial calculations
    private static final BigDecimal PRECISION_SCALE = new BigDecimal("0.01");

    public double calculateWeightedGPA(List<Grade> grades) {
        // Group grades by course to handle multiple assignments per course
        Map<String, List<Grade>> gradesByCourse = grades.stream()
            .collect(Collectors.groupingBy(Grade::getCourseId));

        // Calculate final grade for each course, then compute overall GPA
        return gradesByCourse.entrySet().stream()
            .mapToDouble(this::calculateCourseGPA)
            .average()
            .orElse(0.0);
    }
}
```

## Testing Strategy

### Test Structure

```bash
src/test/java/
├── unit/                    # Unit tests (fast, isolated)
│   ├── models/             # Domain model tests
│   ├── services/           # Service layer tests
│   └── utils/              # Utility class tests
├── integration/            # Integration tests (slower, with external dependencies)
│   ├── repository/         # Database integration tests
│   └── service/            # Service integration tests
└── functional/             # End-to-end functional tests
    └── workflows/          # Complete business workflow tests
```

### Unit Testing with JUnit 5

#### Basic Test Structure

```java
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private StudentService studentService;

    @Test
    @DisplayName("Should create student with valid data")
    void shouldCreateStudentWithValidData() {
        // Given
        Student student = new StudentBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@university.edu")
            .setMajor("Computer Science")
            .build();

        when(repository.save(any(Student.class))).thenReturn(student);

        // When
        Student createdStudent = studentService.createStudent(student);

        // Then
        assertThat(createdStudent).isNotNull();
        assertThat(createdStudent.getFullName()).isEqualTo("John Doe");
        verify(repository).save(student);
    }

    @Test
    @DisplayName("Should throw exception for invalid email")
    void shouldThrowExceptionForInvalidEmail() {
        // Given
        Student studentWithInvalidEmail = new StudentBuilder()
            .setFirstName("Jane")
            .setLastName("Doe")
            .setEmail("invalid-email")
            .build();

        // When & Then
        assertThatThrownBy(() -> studentService.createStudent(studentWithInvalidEmail))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Invalid email format");
    }

    @ParameterizedTest
    @DisplayName("Should validate GPA ranges")
    @ValueSource(doubles = {-1.0, 4.1, 5.0})
    void shouldRejectInvalidGPA(double invalidGPA) {
        // Given
        Student studentWithInvalidGPA = new StudentBuilder()
            .setGPA(invalidGPA)
            .build();

        // When & Then
        assertThatThrownBy(() -> studentService.createStudent(studentWithInvalidGPA))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("GPA must be between 0.0 and 4.0");
    }
}
```

#### Testing with Streams and Functional Code

```java
@Test
@DisplayName("Should filter and transform students correctly")
void shouldFilterAndTransformStudents() {
    // Given
    List<Student> students = Arrays.asList(
        createStudent("John", "Doe", "Computer Science", 3.8),
        createStudent("Jane", "Smith", "Mathematics", 3.9),
        createStudent("Bob", "Johnson", "Computer Science", 3.2)
    );

    when(repository.findAll()).thenReturn(students);

    // When
    List<StudentDTO> honorStudents = studentService.getHonorStudentsByMajor("Computer Science");

    // Then
    assertThat(honorStudents)
        .hasSize(1)
        .extracting(StudentDTO::fullName)
        .containsExactly("John Doe");
}

private Student createStudent(String firstName, String lastName, String major, double gpa) {
    return new StudentBuilder()
        .setFirstName(firstName)
        .setLastName(lastName)
        .setMajor(major)
        .setGPA(gpa)
        .build();
}
```

### Integration Testing

#### Repository Integration Tests

```java
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class StudentRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentRepository repository;

    @Test
    @DisplayName("Should find students by major")
    void shouldFindStudentsByMajor() {
        // Given
        Student csStudent = createAndPersistStudent("John", "Doe", "Computer Science");
        Student mathStudent = createAndPersistStudent("Jane", "Smith", "Mathematics");
        entityManager.flush();

        // When
        List<Student> csStudents = repository.findByMajor("Computer Science");

        // Then
        assertThat(csStudents)
            .hasSize(1)
            .extracting(Student::getFullName)
            .containsExactly("John Doe");
    }

    private Student createAndPersistStudent(String firstName, String lastName, String major) {
        Student student = new StudentBuilder()
            .setFirstName(firstName)
            .setLastName(lastName)
            .setMajor(major)
            .build();
        return entityManager.persistAndFlush(student);
    }
}
```

#### Service Integration Tests

```java
@SpringBootTest
@Transactional
@TestPropertySource(locations = "classpath:application-integration-test.properties")
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Test
    @DisplayName("Should complete full enrollment workflow")
    void shouldCompleteFullEnrollmentWorkflow() {
        // Given
        Student student = studentService.createStudent(
            new StudentBuilder()
                .setFirstName("John")
                .setLastName("Doe")
                .setMajor("Computer Science")
                .build()
        );

        Course course = courseService.createCourse(
            new CourseBuilder()
                .setCourseId("CS101")
                .setTitle("Introduction to Programming")
                .setCredits(3)
                .build()
        );

        // When
        Enrollment enrollment = studentService.enrollInCourse(student.getId(), course.getCourseId())
            .join(); // Wait for async completion

        // Then
        assertThat(enrollment).isNotNull();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);

        List<Course> studentCourses = studentService.getStudentCourses(student.getId());
        assertThat(studentCourses).hasSize(1);
        assertThat(studentCourses.get(0).getCourseId()).isEqualTo("CS101");
    }
}
```

### Performance Testing

#### Concurrency Tests

```java
@Test
@DisplayName("Should handle concurrent enrollments correctly")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
void shouldHandleConcurrentEnrollments() throws InterruptedException {
    // Given
    int numberOfThreads = 100;
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    List<Future<Enrollment>> futures = new ArrayList<>();

    Course popularCourse = createCourseWithCapacity(50);
    List<Student> students = createStudents(numberOfThreads);

    // When
    for (Student student : students) {
        Future<Enrollment> future = executor.submit(() -> {
            try {
                return enrollmentService.enrollStudent(student.getId(), popularCourse.getCourseId());
            } finally {
                latch.countDown();
            }
        });
        futures.add(future);
    }

    latch.await();
    executor.shutdown();

    // Then
    long successfulEnrollments = futures.stream()
        .mapToLong(future -> {
            try {
                future.get();
                return 1;
            } catch (Exception e) {
                return 0;
            }
        })
        .sum();

    assertThat(successfulEnrollments).isEqualTo(50); // Course capacity
}
```

### Test Data Management

#### Test Data Builders

```java
public class TestDataBuilder {

    public static StudentBuilder aStudent() {
        return new StudentBuilder()
            .setId(UUID.randomUUID().toString())
            .setFirstName("John")
            .setLastName("Doe")
            .setEmail("john.doe@test.edu")
            .setMajor("Computer Science")
            .setEnrollmentYear(2024)
            .setGPA(3.5);
    }

    public static CourseBuilder aCourse() {
        return new CourseBuilder()
            .setCourseId("CS" + ThreadLocalRandom.current().nextInt(100, 999))
            .setTitle("Test Course")
            .setCredits(3)
            .setMaxEnrollment(30)
            .setSemester(Semester.FALL);
    }

    // Usage in tests
    @Test
    void testWithCustomStudent() {
        Student student = aStudent()
            .setMajor("Mathematics")
            .setGPA(4.0)
            .build();

        // Test logic here
    }
}
```

## Debugging and Profiling

### Logging Best Practices

#### Structured Logging

```java
@Service
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    public Student createStudent(Student student) {
        logger.info("Creating student: id={}, name={}, major={}",
            student.getId(),
            student.getFullName(),
            student.getMajor());

        try {
            Student createdStudent = repository.save(student);

            logger.info("Successfully created student: id={}, name={}",
                createdStudent.getId(),
                createdStudent.getFullName());

            return createdStudent;

        } catch (Exception e) {
            logger.error("Failed to create student: id={}, name={}, error={}",
                student.getId(),
                student.getFullName(),
                e.getMessage(),
                e);
            throw new StudentCreationException("Failed to create student", e);
        }
    }
}
```

#### Performance Monitoring

```java
@Component
public class PerformanceMonitor {

    private static final Logger perfLogger = LoggerFactory.getLogger("PERFORMANCE");

    @EventListener
    public void handleMethodExecution(MethodExecutionEvent event) {
        if (event.getExecutionTime() > Duration.ofMillis(500)) {
            perfLogger.warn("Slow method execution: method={}, duration={}ms, args={}",
                event.getMethodName(),
                event.getExecutionTime().toMillis(),
                Arrays.toString(event.getArguments()));
        }
    }
}
```

### Debugging Tools and Techniques

#### Debug Configuration

```java
// Enable debug logging for specific packages
# In application-dev.properties
logging.level.com.smartcampus.services=DEBUG
logging.level.com.smartcampus.concurrent=TRACE
logging.level.org.springframework.transaction=DEBUG

# Profile-specific configuration
spring.profiles.active=dev

# JVM debugging options
-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
```

#### Memory Profiling

```bash
# Enable memory profiling
java -XX:+UseG1GC \
     -XX:+PrintGCDetails \
     -XX:+PrintGCTimeStamps \
     -Xloggc:gc.log \
     -jar smartcampus.jar

# Heap dump on OutOfMemoryError
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/tmp/heapdump.hprof \
     -jar smartcampus.jar
```

## Performance Optimization

### Database Optimization

#### Query Optimization

```java
// Use pagination for large result sets
public Page<Student> findStudentsByMajor(String major, Pageable pageable) {
    return repository.findByMajor(major, pageable);
}

// Use projections to limit data transfer
public interface StudentSummary {
    String getId();
    String getFullName();
    String getMajor();
    Double getGPA();
}

public List<StudentSummary> findStudentSummaries() {
    return repository.findAllBy();
}

// Batch operations
@Transactional
public void createStudents(List<Student> students) {
    int batchSize = 50;
    for (int i = 0; i < students.size(); i++) {
        repository.save(students.get(i));
        if (i % batchSize == 0 && i > 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
```

#### Connection Pool Configuration

```properties
# Database connection pool settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.leak-detection-threshold=60000
```

### Concurrent Processing Optimization

#### Thread Pool Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SmartCampus-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
```

#### Caching Strategy

```java
@Service
@CacheConfig(cacheNames = "students")
public class StudentService {

    @Cacheable(key = "#id")
    public Optional<Student> findStudentById(String id) {
        return repository.findById(id);
    }

    @CacheEvict(key = "#student.id")
    public Student updateStudent(Student student) {
        return repository.save(student);
    }

    @CacheEvict(allEntries = true)
    public void refreshStudentCache() {
        // Method to refresh entire cache
    }
}
```

## Security Considerations

### Input Validation

```java
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public static void validateStudent(Student student) {
        requireNonNull(student, "Student cannot be null");
        validateEmail(student.getEmail());
        validateGPA(student.getGPA());
        validateName(student.getFirstName(), "First name");
        validateName(student.getLastName(), "Last name");
    }

    private static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email format");
        }
    }

    private static void validateGPA(double gpa) {
        if (gpa < 0.0 || gpa > 4.0) {
            throw new ValidationException("GPA must be between 0.0 and 4.0");
        }
    }
}
```

### SQL Injection Prevention

```java
// Always use parameterized queries
@Repository
public class StudentRepository {

    // Good: Parameterized query
    public List<Student> findByMajor(String major) {
        String sql = "SELECT * FROM students WHERE major = ?";
        return jdbcTemplate.query(sql, new Object[]{major}, studentRowMapper);
    }

    // Bad: String concatenation (vulnerable to SQL injection)
    // public List<Student> findByMajor(String major) {
    //     String sql = "SELECT * FROM students WHERE major = '" + major + "'";
    //     return jdbcTemplate.query(sql, studentRowMapper);
    // }
}
```

## Deployment and DevOps

### Build Configuration

#### Maven Profiles

```xml
<!-- pom.xml profiles for different environments -->
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>dev</spring.profiles.active>
            <maven.test.skip>false</maven.test.skip>
        </properties>
    </profile>

    <profile>
        <id>test</id>
        <properties>
            <spring.profiles.active>test</spring.profiles.active>
            <maven.test.skip>false</maven.test.skip>
        </properties>
    </profile>

    <profile>
        <id>prod</id>
        <properties>
            <spring.profiles.active>prod</spring.profiles.active>
            <maven.test.skip>true</maven.test.skip>
        </properties>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <executable>true</executable>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

#### Docker Configuration

```dockerfile
# Multi-stage Docker build
FROM openjdk:17-jdk-slim as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/target/smartcampus-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Monitoring and Observability

#### Health Checks

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("validationTimeout", "1000ms")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withException(e)
                .build();
        }

        return Health.down()
            .withDetail("database", "Connection invalid")
            .build();
    }
}
```

#### Metrics Collection

```java
@Service
public class StudentService {

    private final MeterRegistry meterRegistry;
    private final Counter studentCreationCounter;
    private final Timer enrollmentTimer;

    public StudentService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.studentCreationCounter = Counter.builder("student.created")
            .description("Number of students created")
            .register(meterRegistry);
        this.enrollmentTimer = Timer.builder("enrollment.duration")
            .description("Time taken to process enrollment")
            .register(meterRegistry);
    }

    public Student createStudent(Student student) {
        return Timer.Sample.start(meterRegistry)
            .stop(enrollmentTimer, () -> {
                Student created = repository.save(student);
                studentCreationCounter.increment();
                return created;
            });
    }
}
```

## Contributing Guidelines

### Pull Request Process

1. **Create Feature Branch**

   ```bash
   git checkout -b feature/your-feature-name develop
   ```

2. **Make Changes**

   - Write code following the established conventions
   - Add comprehensive tests
   - Update documentation as needed

3. **Run Quality Checks**

   ```bash
   # Run tests
   mvn test

   # Check code quality
   mvn checkstyle:check
   mvn spotbugs:check

   # Run integration tests
   mvn test -Pintegration-test
   ```

4. **Commit Changes**

   ```bash
   git add .
   git commit -m "feat(student): add batch enrollment processing"
   ```

5. **Push and Create PR**

   ```bash
   git push origin feature/your-feature-name
   # Create pull request through GitHub interface
   ```

### Code Review Checklist

#### For Authors

- [ ] Code follows established conventions
- [ ] All tests pass
- [ ] Code coverage meets minimum threshold (80%)
- [ ] Documentation is updated
- [ ] No security vulnerabilities introduced
- [ ] Performance impact considered

#### For Reviewers

- [ ] Code is readable and maintainable
- [ ] Business logic is correct
- [ ] Edge cases are handled
- [ ] Tests are comprehensive
- [ ] Security best practices followed
- [ ] Performance considerations addressed

### Quality Gates

All changes must pass these quality gates:

1. **Unit Tests**: Minimum 80% coverage
2. **Integration Tests**: All critical workflows tested
3. **Code Quality**: No critical issues in SonarQube
4. **Security**: No high/critical security vulnerabilities
5. **Performance**: No significant performance degradation

## Troubleshooting Common Issues

### Development Environment Issues

#### Maven Dependency Conflicts

```bash
# Analyze dependency tree
mvn dependency:tree

# Force update snapshots
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

#### IDE Issues

```bash
# Refresh IDE project
# IntelliJ IDEA: File -> Reload Maven Projects
# Eclipse: Right-click project -> Maven -> Reload Projects
# VS Code: Command Palette -> Java: Reload Projects
```

### Runtime Issues

#### Memory Issues

```bash
# Increase heap size
export MAVEN_OPTS="-Xmx2g -Xms512m"

# Enable GC logging
java -XX:+PrintGC -XX:+PrintGCDetails -jar app.jar
```

#### Database Connection Issues

```properties
# Increase connection timeout
spring.datasource.hikari.connection-timeout=30000

# Enable connection validation
spring.datasource.hikari.validation-timeout=3000
spring.datasource.hikari.connection-test-query=SELECT 1
```

This development guide provides the foundation for maintaining high code quality and development efficiency in the SmartCampus project. Regular updates to this document ensure it remains current with best practices and project evolution.
