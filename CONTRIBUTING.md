# Location: CONTRIBUTING.md

# Contributing to SmartCampus Backend

First off, thank you for considering contributing to SmartCampus Backend! It's people like you that make SmartCampus such a great tool for educational institutions worldwide.

## 🌟 Ways to Contribute

There are many ways you can contribute to this project:

-   **Bug Reports**: Report bugs and issues
-   **Feature Requests**: Suggest new features or enhancements
-   **Code Contributions**: Submit bug fixes, new features, or improvements
-   **Documentation**: Improve documentation, write tutorials, or create examples
-   **Testing**: Help test new features and report issues
-   **Community Support**: Help other users in discussions and issues

## 📋 Table of Contents

-   [Code of Conduct](#code-of-conduct)
-   [Getting Started](#getting-started)
-   [Development Workflow](#development-workflow)
-   [Coding Standards](#coding-standards)
-   [Testing Guidelines](#testing-guidelines)
-   [Documentation Guidelines](#documentation-guidelines)
-   [Submitting Changes](#submitting-changes)
-   [Community Guidelines](#community-guidelines)

## 📜 Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to [conduct@smartcampus.com](mailto:conduct@smartcampus.com).

### Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, sex characteristics, gender identity and expression, level of experience, education, socio-economic status, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Our Standards

Examples of behavior that contributes to creating a positive environment include:

-   Using welcoming and inclusive language
-   Being respectful of differing viewpoints and experiences
-   Gracefully accepting constructive criticism
-   Focusing on what is best for the community
-   Showing empathy towards other community members

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

-   **Java 17+**: Download from [OpenJDK](https://openjdk.java.net/)
-   **Maven 3.8+**: Download from [Maven](https://maven.apache.org/)
-   **Git**: Download from [Git](https://git-scm.com/)
-   **PostgreSQL 13+**: For database (or use Docker)
-   **Docker**: For containerized development (optional but recommended)
-   **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Fork and Clone

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:

    ```bash
    git clone https://github.com/YOUR_USERNAME/smartcampus-backend.git
    cd smartcampus-backend
    ```

3. **Add the original repository as upstream**:

    ```bash
    git remote add upstream https://github.com/smartcampus/smartcampus-backend.git
    ```

4. **Verify your remotes**:
    ```bash
    git remote -v
    ```

### Setting Up Development Environment

1. **Install dependencies**:

    ```bash
    mvn clean install
    ```

2. **Set up the database**:

    ```bash
    # Using Docker (recommended)
    docker-compose up -d postgres redis

    # Or install PostgreSQL locally and create database
    createdb smartcampus
    ```

3. **Run the application**:

    ```bash
    mvn spring-boot:run -Dspring.profiles.active=dev
    ```

4. **Verify setup**:
    - API: http://localhost:8080
    - Swagger UI: http://localhost:8080/swagger-ui.html
    - Health Check: http://localhost:8080/actuator/health

## 🔄 Development Workflow

### Branch Naming Convention

Use descriptive branch names with the following prefixes:

-   `feature/` - New features (e.g., `feature/user-authentication`)
-   `bugfix/` - Bug fixes (e.g., `bugfix/login-validation`)
-   `hotfix/` - Critical fixes (e.g., `hotfix/security-vulnerability`)
-   `docs/` - Documentation changes (e.g., `docs/api-documentation`)
-   `refactor/` - Code refactoring (e.g., `refactor/service-layer`)
-   `test/` - Test improvements (e.g., `test/integration-tests`)

### Workflow Steps

1. **Create a new branch** for your work:

    ```bash
    git checkout -b feature/your-feature-name
    ```

2. **Keep your branch up to date**:

    ```bash
    git fetch upstream
    git rebase upstream/main
    ```

3. **Make your changes** following our coding standards

4. **Test your changes** thoroughly:

    ```bash
    mvn test
    mvn verify
    ```

5. **Commit your changes** with meaningful messages:

    ```bash
    git add .
    git commit -m "feat: add user authentication feature"
    ```

6. **Push your branch**:

    ```bash
    git push origin feature/your-feature-name
    ```

7. **Create a Pull Request** on GitHub

### Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Types:**

-   `feat` - New features
-   `fix` - Bug fixes
-   `docs` - Documentation only changes
-   `style` - Changes that don't affect the meaning of the code
-   `refactor` - Code refactoring
-   `perf` - Performance improvements
-   `test` - Adding missing tests or correcting existing tests
-   `chore` - Changes to the build process or auxiliary tools

**Examples:**

```bash
feat: add JWT-based authentication
fix: resolve null pointer exception in user service
docs: update API documentation for user endpoints
test: add integration tests for course enrollment
```

## 💻 Coding Standards

### Java Code Standards

We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with some modifications:

#### General Guidelines

-   **Indentation**: 4 spaces (no tabs)
-   **Line Length**: Maximum 120 characters
-   **Encoding**: UTF-8
-   **File Structure**: Follow standard Maven project structure

#### Naming Conventions

-   **Classes**: PascalCase (e.g., `UserService`, `CourseController`)
-   **Methods**: camelCase (e.g., `findUserById`, `createCourse`)
-   **Variables**: camelCase (e.g., `userId`, `courseName`)
-   **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`)
-   **Packages**: lowercase with dots (e.g., `com.smartcampus.service`)

#### Code Organization

```java
// Class structure order:
1. Static imports
2. Non-static imports
3. Class declaration
4. Static fields
5. Instance fields
6. Constructors
7. Static methods
8. Instance methods
9. Inner classes
```

#### Best Practices

-   **Use meaningful names** for variables, methods, and classes
-   **Write self-documenting code** with clear logic flow
-   **Add JavaDoc** for public APIs and complex methods
-   **Keep methods small** (preferably under 20 lines)
-   **Use dependency injection** instead of static dependencies
-   **Handle exceptions appropriately** with proper error messages
-   **Validate input parameters** in public methods

#### Example Code Style

```java
package com.smartcampus.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user operations.
 */
@Service
public class UserService {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Finds a user by their ID.
     *
     * @param userId the unique identifier of the user
     * @return an optional containing the user if found
     * @throws IllegalArgumentException if userId is null or empty
     */
    public Optional<User> findUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        return userRepository.findById(userId);
    }
}
```

### Spring Boot Best Practices

-   **Use constructor injection** over field injection
-   **Implement proper exception handling** with `@ControllerAdvice`
-   **Use DTOs** for API requests/responses
-   **Implement proper validation** with Bean Validation annotations
-   **Use profiles** for environment-specific configurations
-   **Implement proper logging** with SLF4J
-   **Use transactions** appropriately with `@Transactional`

## 🧪 Testing Guidelines

### Test Structure

We use a comprehensive testing strategy with multiple levels:

#### Unit Tests

-   Test individual methods and classes in isolation
-   Use mocking for dependencies
-   Fast execution (< 1 second per test)
-   Located in `src/test/java`
-   Naming: `*Test.java`

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should find user by valid ID")
    void shouldFindUserByValidId() {
        // Given
        String userId = "user123";
        User expectedUser = new User(userId, "john@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.findUserById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
    }
}
```

#### Integration Tests

-   Test interaction between components
-   Use TestContainers for real database testing
-   Test complete request/response cycles
-   Naming: `*IntegrationTest.java` or `*IT.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        UserCreateRequest request = new UserCreateRequest("john@example.com", "password123");

        // When
        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/users", request, UserResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getEmail()).isEqualTo("john@example.com");
    }
}
```

### Testing Best Practices

-   **Write tests first** (TDD approach when possible)
-   **Use descriptive test names** that explain what is being tested
-   **Follow the AAA pattern** (Arrange, Act, Assert)
-   **Use meaningful assertions** with proper error messages
-   **Test edge cases** and error conditions
-   **Keep tests independent** and idempotent
-   **Use test data builders** for complex object creation
-   **Mock external dependencies** in unit tests
-   **Use real implementations** in integration tests

### Test Coverage Requirements

-   **Minimum coverage**: 80% overall
-   **Critical paths**: 95% coverage required
-   **New features**: Must have comprehensive tests
-   **Bug fixes**: Must include regression tests

Run coverage reports:

```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

## 📝 Documentation Guidelines

### API Documentation

We use OpenAPI 3 (Swagger) for API documentation:

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with the provided information")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "User information") @Valid @RequestBody UserCreateRequest request) {
        // Implementation
    }
}
```

### Code Documentation

-   **JavaDoc for public APIs**: All public classes, methods, and fields
-   **Inline comments**: For complex logic and business rules
-   **README updates**: For new features or changes
-   **Architecture decisions**: Document in ADR format

### Documentation Standards

-   **Clear and concise**: Use simple language
-   **Keep it updated**: Update docs with code changes
-   **Include examples**: Provide usage examples
-   **Link related content**: Cross-reference related documentation

## 📤 Submitting Changes

### Pull Request Process

1. **Ensure your code follows** all guidelines mentioned above
2. **Update documentation** for any new or changed functionality
3. **Add/update tests** for your changes
4. **Ensure all tests pass**:
    ```bash
    mvn clean verify
    ```
5. **Create a detailed Pull Request** with:
    - Clear title and description
    - Reference to related issues
    - Screenshots (if applicable)
    - Breaking changes (if any)

### Pull Request Template

```markdown
## Description

Brief description of the changes made.

## Related Issues

-   Fixes #123
-   Related to #456

## Type of Change

-   [ ] Bug fix (non-breaking change which fixes an issue)
-   [ ] New feature (non-breaking change which adds functionality)
-   [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
-   [ ] Documentation update

## Testing

-   [ ] Unit tests added/updated
-   [ ] Integration tests added/updated
-   [ ] Manual testing completed

## Checklist

-   [ ] My code follows the project's coding standards
-   [ ] I have performed a self-review of my code
-   [ ] I have added tests that prove my fix is effective or that my feature works
-   [ ] New and existing unit tests pass locally with my changes
-   [ ] I have updated the documentation accordingly

## Screenshots (if applicable)

Add screenshots to help explain your changes.

## Additional Notes

Any additional information that reviewers should know.
```

### Review Process

1. **Automated checks** must pass (CI/CD pipeline)
2. **At least 2 approvals** from maintainers required
3. **All conversations resolved** before merging
4. **Squash and merge** is the preferred merge strategy

## 🤝 Community Guidelines

### Communication Channels

-   **GitHub Issues**: Bug reports and feature requests
-   **GitHub Discussions**: General discussions and questions
-   **Email**: [dev-team@smartcampus.com](mailto:dev-team@smartcampus.com) for sensitive matters
-   **Discord**: [SmartCampus Discord Server](https://discord.gg/smartcampus) for real-time chat

### Getting Help

If you need help with:

-   **Development setup**: Check the README.md or ask in discussions
-   **Code review**: Tag reviewers in your PR
-   **Bug reports**: Create a detailed issue with reproduction steps
-   **Feature requests**: Discuss in issues before implementation

### Recognition

We value all contributions and will:

-   **Credit contributors** in release notes
-   **Add contributors** to the README
-   **Highlight significant contributions** in community updates
-   **Provide mentorship** for new contributors

## 📞 Questions?

If you have questions about contributing, please:

1. Check existing documentation and issues
2. Search GitHub discussions
3. Create a new discussion or issue
4. Contact the maintainers directly

Thank you for contributing to SmartCampus Backend! 🎉

---

**Remember**: The best contributions come from understanding the problem you're solving and the impact it will have on users. When in doubt, ask questions and collaborate with the community.

_Happy coding!_ 💻✨
