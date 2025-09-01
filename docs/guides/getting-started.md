# SmartCampus - Getting Started Guide

**Location:** `docs/guides/getting-started.md`

## Overview

SmartCampus is a comprehensive university management system built with Java 17+ and modern development practices. This guide will help you set up the development environment and get the application running on your local machine.

## Prerequisites

### Required Software

- **Java Development Kit (JDK) 17 or higher**

  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use [OpenJDK](https://openjdk.org/)
  - Verify installation: `java --version`

- **Apache Maven 3.8.0 or higher**

  - Download from [Maven Website](https://maven.apache.org/download.cgi)
  - Verify installation: `mvn --version`

- **Git**
  - Download from [Git Website](https://git-scm.com/downloads)
  - Verify installation: `git --version`

### Optional but Recommended

- **Docker & Docker Compose**

  - For containerized development and deployment
  - Download from [Docker Website](https://www.docker.com/get-started)

- **Integrated Development Environment (IDE)**

  - IntelliJ IDEA (recommended)
  - Eclipse with Java EE tools
  - Visual Studio Code with Java extensions

- **Database Management Tool**
  - DBeaver, HeidiSQL, or similar for database inspection

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-organization/smartcampus.git
cd smartcampus
```

### 2. Verify Java and Maven Setup

```bash
# Check Java version (should be 17+)
java --version

# Check Maven version (should be 3.8+)
mvn --version

# Verify JAVA_HOME is set correctly
echo $JAVA_HOME  # On Unix/macOS
echo %JAVA_HOME% # On Windows
```

### 3. Install Dependencies

```bash
# Download and install all Maven dependencies
mvn clean install

# Skip tests for faster initial setup
mvn clean install -DskipTests
```

### 4. Configure Database (Optional)

SmartCampus can run with in-memory data storage by default, but you can configure a persistent database:

#### H2 Database (Default - No setup required)

The application comes pre-configured with H2 in-memory database for immediate use.

#### MySQL Configuration (Optional)

1. Install MySQL Server
2. Create database:

   ```sql
   CREATE DATABASE smartcampus;
   CREATE USER 'smartcampus_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON smartcampus.* TO 'smartcampus_user'@'localhost';
   ```

3. Update `src/main/resources/config/database.properties`:

   ```properties
   database.url=jdbc:mysql://localhost:3306/smartcampus
   database.username=smartcampus_user
   database.password=your_password
   database.driver=com.mysql.cj.jdbc.Driver
   ```

#### PostgreSQL Configuration (Optional)

1. Install PostgreSQL
2. Create database:
   ```sql
   CREATE DATABASE smartcampus;
   CREATE USER smartcampus_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE smartcampus TO smartcampus_user;
   ```
3. Update `src/main/resources/config/database.properties`:
   ```properties
   database.url=jdbc:postgresql://localhost:5432/smartcampus
   database.username=smartcampus_user
   database.password=your_password
   database.driver=org.postgresql.Driver
   ```

## Running the Application

### Method 1: Using Maven

```bash
# Run the application
mvn exec:java -Dexec.mainClass="app.App"

# Or with Maven Spring Boot plugin (if applicable)
mvn spring-boot:run
```

### Method 2: Using Compiled JAR

```bash
# Compile the application
mvn clean package

# Run the compiled JAR
java -jar target/smartcampus-1.0.0.jar
```

### Method 3: Using Docker (Recommended for Production-like Environment)

```bash
# Build Docker image
docker build -t smartcampus:latest .

# Run with Docker Compose (includes database)
docker-compose up -d

# View logs
docker-compose logs -f smartcampus
```

### Method 4: Development Mode with Auto-reload

```bash
# Use Maven with file watching (if configured)
mvn compile exec:java -Dexec.mainClass="app.App" -Dexec.args="--dev-mode"
```

## Initial Setup

### 1. First Run Setup

When you first run the application, it will:

- Create necessary database tables (if using persistent database)
- Load sample data from CSV files in `src/main/resources/data/`
- Initialize default user accounts
- Set up default departments and courses

### 2. Default User Accounts

The application comes with pre-configured accounts for testing:

| Role      | Username   | Password   | Description              |
| --------- | ---------- | ---------- | ------------------------ |
| Admin     | admin      | admin123   | System administrator     |
| Professor | prof.smith | prof123    | Sample professor account |
| Student   | john.doe   | student123 | Sample student account   |

⚠️ **Security Note**: Change default passwords before deploying to production!

### 3. Sample Data

The application loads sample data including:

- 50+ sample students
- 20+ sample courses
- 10+ departments
- Sample enrollments and grades

## Application Structure Overview

```
SmartCampus/
├── src/main/java/
│   ├── app/                    # Main application entry point
│   ├── models/                 # Domain entities
│   ├── services/               # Business logic layer
│   ├── repositories/           # Data access layer
│   ├── utils/                  # Utility classes
│   └── ...
├── src/main/resources/
│   ├── data/                   # CSV data files
│   ├── config/                 # Configuration files
│   └── ...
├── src/test/                   # Test files
├── docs/                       # Documentation
├── docker/                     # Docker configuration
└── scripts/                    # Build and deployment scripts
```

## Basic Usage

### 1. Starting the Application

```bash
# Navigate to project directory
cd smartcampus

# Run the application
mvn exec:java -Dexec.mainClass="app.App"
```

### 2. Main Menu Navigation

The application presents a console-based menu system:

```
=== SmartCampus University Management System ===
1. Student Management
2. Professor Management
3. Course Management
4. Enrollment Management
5. Grade Management
6. Reports
7. System Administration
8. Exit
```

### 3. Common Operations

#### Student Operations:

- Add new student
- Update student information
- View student details
- Search students
- View student transcripts

#### Course Operations:

- Create new course
- Assign professor to course
- Set course capacity
- View course enrollment
- Generate course reports

#### Enrollment Operations:

- Enroll student in course
- Drop student from course
- View enrollment history
- Process waitlists

## Configuration

### Application Configuration

Edit `src/main/resources/config/application.properties`:

```properties
# Application Settings
app.name=SmartCampus
app.version=1.0.0
app.debug=false

# Performance Settings
app.cache.enabled=true
app.cache.size=1000
app.concurrent.thread-pool-size=10

# Security Settings
security.password.min-length=8
security.session.timeout=3600
security.jwt.secret=your-jwt-secret-here
```

### Logging Configuration

Edit `src/main/resources/config/logging.properties`:

```properties
# Root logger level
.level=INFO

# Console handler
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter

# File handler (optional)
# handlers=java.util.logging.FileHandler
# java.util.logging.FileHandler.pattern=logs/smartcampus.log
# java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter
```

## Troubleshooting

### Common Issues

#### 1. Java Version Compatibility

**Error**: `UnsupportedClassVersionError` or similar
**Solution**: Ensure you're using Java 17 or higher:

```bash
java --version
javac --version
```

#### 2. Maven Dependencies

**Error**: Dependencies not resolving
**Solution**:

```bash
# Clear Maven cache and reinstall
mvn dependency:purge-local-repository
mvn clean install
```

#### 3. Port Already in Use

**Error**: Port 8080 already in use
**Solution**:

- Kill the process using the port, or
- Change the port in configuration:
  ```properties
  server.port=8081
  ```

#### 4. Database Connection Issues

**Error**: Cannot connect to database
**Solution**:

1. Verify database server is running
2. Check connection properties
3. Verify user permissions
4. Test connection manually

#### 5. Memory Issues

**Error**: OutOfMemoryError
**Solution**:

```bash
# Increase JVM heap size
export MAVEN_OPTS="-Xmx2g -Xms512m"
mvn exec:java -Dexec.mainClass="app.App"

# Or when running JAR directly
java -Xmx2g -Xms512m -jar target/smartcampus-1.0.0.jar
```

### Getting Help

1. **Check the logs**: Look in the console output or log files for error details
2. **Verify configuration**: Ensure all configuration files are properly set up
3. **Check dependencies**: Run `mvn dependency:tree` to verify all dependencies are resolved
4. **Review documentation**: Check other files in the `docs/` directory
5. **Search issues**: Look through existing GitHub issues for similar problems

## Development Setup

### IDE Configuration

#### IntelliJ IDEA

1. Open the project directory
2. IntelliJ should automatically detect it as a Maven project
3. Wait for dependency resolution to complete
4. Set Project SDK to Java 17+
5. Configure code style (optional):
   - File > Settings > Editor > Code Style > Java
   - Import the project's code style if available

#### Eclipse

1. File > Import > Existing Maven Projects
2. Select the project directory
3. Eclipse will import and configure the project
4. Right-click project > Properties > Java Build Path
5. Verify JRE version is 17+

#### Visual Studio Code

1. Install Java Extension Pack
2. Open the project folder
3. VS Code should automatically detect the Maven project
4. Configure Java runtime in settings if needed

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=StudentServiceTest

# Run tests in specific package
mvn test -Dtest="com.smartcampus.services.*"

# Skip tests during build
mvn install -DskipTests
```

### Development Commands

```bash
# Compile without running tests
mvn compile

# Clean and compile
mvn clean compile

# Generate documentation
mvn javadoc:javadoc

# Check for dependency updates
mvn versions:display-dependency-updates

# Format code (if formatter plugin is configured)
mvn spotless:apply
```

## Next Steps

Once you have the application running:

1. **Explore the Features**: Try different menu options to understand the system capabilities
2. **Review the Code**: Examine the source code to understand the architecture
3. **Run the Tests**: Execute the test suite to ensure everything works correctly
4. **Read the Documentation**: Check out other files in the `docs/` directory
5. **Customize Configuration**: Adjust settings to match your requirements
6. **Add Sample Data**: Modify CSV files in `src/main/resources/data/` to add your own test data

## Support

For additional help:

- Check the [Development Guide](development.md) for detailed development instructions
- Review the [API Documentation](../api/) for code-level documentation
- See the [Architecture Documentation](../design/architecture.md) for system design details
- Look at [Best Practices](../examples/best-practices.md) for coding guidelines

Welcome to SmartCampus development! 🎓
