# SmartCampus Design Patterns Documentation

**Location:** `docs/design/design-patterns.md`

## Overview

SmartCampus leverages several well-established design patterns to create a maintainable, scalable, and robust university management system. This document outlines the implementation and rationale for each pattern used in the project.

## Creational Patterns

### 1. Builder Pattern

**Implementation**: `StudentBuilder.java`, `CourseBuilder.java`

**Purpose**: Construct complex objects step by step, especially useful for objects with many optional parameters.

**Example Usage**:

```java
Student student = new StudentBuilder()
    .setId("STU001")
    .setFirstName("John")
    .setLastName("Doe")
    .setEmail("john.doe@university.edu")
    .setMajor("Computer Science")
    .setEnrollmentYear(2024)
    .build();

Course course = new CourseBuilder()
    .setCourseId("CS101")
    .setTitle("Introduction to Programming")
    .setCredits(3)
    .setProfessorId("PROF001")
    .setMaxEnrollment(50)
    .setSemester(Semester.FALL)
    .build();
```

**Benefits**:

- Immutable object creation
- Flexible construction process
- Better readability for complex objects
- Validation during construction

### 2. Factory Pattern

**Implementation**: `UniversityFactory.java`, `ServiceFactory.java`

**Purpose**: Create objects without specifying their exact classes, promoting loose coupling.

**Example Usage**:

```java
// Abstract Factory for Services
ServiceFactory factory = ServiceFactory.getInstance();
StudentService studentService = factory.createStudentService();
CourseService courseService = factory.createCourseService();

// University Factory
University university = UniversityFactory.createUniversity(
    "Harvard University",
    UniversityType.PRIVATE
);
```

**Benefits**:

- Centralized object creation logic
- Easy to extend with new implementations
- Promotes dependency injection
- Configuration-driven instantiation

### 3. Singleton Pattern

**Implementation**: `DatabaseConnection.java`

**Purpose**: Ensure only one instance of database connection manager exists throughout the application lifecycle.

**Example Usage**:

```java
public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private DataSource dataSource;

    private DatabaseConnection() {
        initializeDataSource();
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }
}
```

**Benefits**:

- Resource optimization
- Global access point
- Thread-safe implementation
- Lazy initialization

## Structural Patterns

### 4. Adapter Pattern

**Implementation**: `AdapterService.java`

**Purpose**: Allow incompatible interfaces to work together, particularly useful for integrating with external systems.

**Example Usage**:

```java
public class ExternalSystemAdapter implements UniversitySystem {
    private ExternalLegacySystem legacySystem;

    public ExternalSystemAdapter(ExternalLegacySystem legacySystem) {
        this.legacySystem = legacySystem;
    }

    @Override
    public List<Student> getStudents() {
        // Convert legacy format to our format
        return legacySystem.retrieveStudentData()
            .stream()
            .map(this::convertLegacyStudent)
            .collect(Collectors.toList());
    }
}
```

**Benefits**:

- Integration with legacy systems
- Interface compatibility
- Code reusability
- Minimal changes to existing code

### 5. Repository Pattern

**Implementation**: `BaseRepository.java`, specific repository classes

**Purpose**: Encapsulate data access logic and provide a uniform interface for data operations.

**Example Usage**:

```java
public interface Repository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
    boolean existsById(ID id);
}

public class StudentRepository extends BaseRepository<Student, String> {
    public List<Student> findByMajor(String major) {
        return findAll().stream()
            .filter(student -> major.equals(student.getMajor()))
            .collect(Collectors.toList());
    }
}
```

**Benefits**:

- Data access abstraction
- Testability through mocking
- Centralized query logic
- Database independence

## Behavioral Patterns

### 6. Observer Pattern

**Implementation**: `EventManager.java`, event classes

**Purpose**: Define a subscription mechanism to notify multiple objects about events that happen to the object they're observing.

**Example Usage**:

```java
public class EventManager {
    private Map<Class<? extends Event>, List<EventListener>> listeners = new HashMap<>();

    public void subscribe(Class<? extends Event> eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void notify(Event event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            eventListeners.forEach(listener -> listener.handle(event));
        }
    }
}

// Usage
eventManager.subscribe(StudentEnrolledEvent.class, new NotificationService());
eventManager.notify(new StudentEnrolledEvent(student, course));
```

**Benefits**:

- Loose coupling between components
- Dynamic relationships
- Event-driven architecture
- Scalable notification system

### 7. Command Pattern

**Implementation**: `CommandProcessor.java`

**Purpose**: Encapsulate requests as objects, allowing for parameterization, queuing, and logging of requests.

**Example Usage**:

```java
public interface Command {
    void execute();
    void undo();
}

public class EnrollStudentCommand implements Command {
    private Student student;
    private Course course;
    private EnrollmentService enrollmentService;

    @Override
    public void execute() {
        enrollmentService.enroll(student, course);
    }

    @Override
    public void undo() {
        enrollmentService.unenroll(student, course);
    }
}

public class CommandProcessor {
    private Stack<Command> history = new Stack<>();

    public void execute(Command command) {
        command.execute();
        history.push(command);
    }

    public void undo() {
        if (!history.isEmpty()) {
            history.pop().undo();
        }
    }
}
```

**Benefits**:

- Decoupling of invoker and receiver
- Undo functionality
- Macro commands
- Logging and auditing

### 8. Strategy Pattern

**Implementation**: `CacheStrategy.java` and related classes

**Purpose**: Define a family of algorithms, encapsulate each one, and make them interchangeable.

**Example Usage**:

```java
public interface CacheStrategy {
    void put(String key, Object value);
    Object get(String key);
    void evict(String key);
}

public class LRUCacheStrategy implements CacheStrategy {
    // LRU implementation
}

public class FIFOCacheStrategy implements CacheStrategy {
    // FIFO implementation
}

public class CacheManager {
    private CacheStrategy strategy;

    public CacheManager(CacheStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(CacheStrategy strategy) {
        this.strategy = strategy;
    }
}
```

**Benefits**:

- Algorithm flexibility
- Runtime strategy switching
- Easy testing of different approaches
- Open/Closed principle compliance

## Functional Patterns

### 9. Function Composition

**Implementation**: `Functions.java`, `StreamUtils.java`

**Purpose**: Combine simple functions to create more complex operations.

**Example Usage**:

```java
public class Functions {
    public static final Function<Student, String> GET_FULL_NAME =
        student -> student.getFirstName() + " " + student.getLastName();

    public static final Predicate<Student> IS_HONOR_STUDENT =
        student -> student.getGPA() >= 3.5;

    public static final Function<Student, StudentDTO> TO_DTO =
        student -> new StudentDTO(
            student.getId(),
            GET_FULL_NAME.apply(student),
            student.getMajor()
        );
}

// Usage with method chaining
List<StudentDTO> honorStudents = students.stream()
    .filter(Functions.IS_HONOR_STUDENT)
    .map(Functions.TO_DTO)
    .collect(Collectors.toList());
```

**Benefits**:

- Code reusability
- Functional composition
- Readable operations
- Testable functions

### 10. Null Object Pattern

**Implementation**: Used in various service classes

**Purpose**: Provide a default object that exhibits neutral behavior, eliminating null checks.

**Example Usage**:

```java
public class NullStudent extends Student {
    private static final NullStudent INSTANCE = new NullStudent();

    public static Student getInstance() {
        return INSTANCE;
    }

    @Override
    public String getFullName() {
        return "Unknown Student";
    }

    @Override
    public boolean isValid() {
        return false;
    }
}

public class StudentService {
    public Student findById(String id) {
        Student student = repository.findById(id);
        return student != null ? student : NullStudent.getInstance();
    }
}
```

**Benefits**:

- Eliminates null pointer exceptions
- Simplifies client code
- Consistent behavior
- Reduces conditional logic

## Pattern Integration

### Combined Pattern Usage

Many components use multiple patterns together for maximum effectiveness:

```java
// Builder + Factory + Strategy
Course course = CourseFactory.createBuilder()
    .setGradingStrategy(new StandardGradingStrategy())
    .setEnrollmentStrategy(new FirstComeFirstServeStrategy())
    .build();

// Observer + Command + Strategy
eventManager.subscribe(GradeUpdatedEvent.class,
    new CommandEventListener(new UpdateTranscriptCommand()));
```

## Anti-Patterns Avoided

### 1. God Object

- **Problem**: Classes that know too much or do too much
- **Solution**: Single Responsibility Principle, service layer separation

### 2. Spaghetti Code

- **Problem**: Unstructured, difficult-to-follow code
- **Solution**: Clear architecture layers, design patterns

### 3. Magic Numbers/Strings

- **Problem**: Hard-coded values without context
- **Solution**: Enums, constants, configuration files

### 4. Copy-Paste Programming

- **Problem**: Code duplication
- **Solution**: DRY principle, utility classes, inheritance

## Pattern Selection Guidelines

### When to Use Each Pattern

1. **Builder**: Complex objects with many parameters
2. **Factory**: Object creation with varying implementations
3. **Singleton**: Single global instance needed
4. **Observer**: Event-driven architecture
5. **Command**: Undo functionality, operation queuing
6. **Strategy**: Algorithm variations
7. **Repository**: Data access abstraction
8. **Adapter**: Legacy system integration

### Performance Considerations

- **Singleton**: Memory efficient but potential concurrency issues
- **Observer**: Event propagation overhead
- **Command**: Memory usage for command history
- **Factory**: Additional abstraction layer overhead

## Testing Strategies

### Pattern-Specific Testing

```java
// Testing Builder Pattern
@Test
public void testStudentBuilder() {
    Student student = new StudentBuilder()
        .setFirstName("John")
        .setLastName("Doe")
        .build();

    assertEquals("John Doe", student.getFullName());
}

// Testing Observer Pattern
@Test
public void testEventNotification() {
    TestListener listener = new TestListener();
    eventManager.subscribe(TestEvent.class, listener);

    eventManager.notify(new TestEvent());

    assertTrue(listener.wasNotified());
}
```

## Conclusion

The design patterns implemented in SmartCampus provide a solid foundation for maintainable, scalable software architecture. Each pattern serves a specific purpose and contributes to the overall system's flexibility and robustness. The combination of creational, structural, and behavioral patterns ensures that the system can evolve with changing requirements while maintaining code quality and developer productivity.
