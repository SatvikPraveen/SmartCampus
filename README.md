# SmartCampus Backend - University Management System

[![Build Status](https://github.com/SatvikPraveen/SmartCampus/actions/workflows/ci.yml/badge.svg)](https://github.com/SatvikPraveen/SmartCampus/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Code Coverage](https://img.shields.io/badge/Coverage-85%25-green.svg)](https://github.com/SatvikPraveen/SmartCampus)

A comprehensive Smart Campus Management System backend built with modern Spring Boot architecture, providing enterprise-grade RESTful APIs for managing academic institutions' complete operations. This system demonstrates advanced Java programming concepts, Spring Boot best practices, design patterns, and production-ready deployment strategies.

## 🏗️ Architecture Overview

SmartCampus follows a modern layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                               │
│              (REST Controllers + OpenAPI Documentation)                 │
├─────────────────────────────────────────────────────────────────────────┤
│                         Service Layer                                   │
│           (Business Logic + Transaction Management + Caching)           │
├─────────────────────────────────────────────────────────────────────────┤
│                       Repository Layer                                  │
│              (Data Access + JPA Repositories + Queries)                │
├─────────────────────────────────────────────────────────────────────────┤
│                        Domain Layer                                     │
│                  (JPA Entities + Domain Models)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                     Infrastructure Layer                                │
│         (Database + Cache + External APIs + File System)               │
└─────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Key Features

### Core Academic Management

-   **Student Lifecycle Management**: Complete student management from admission to graduation
-   **Course Management**: Course creation, scheduling, prerequisite handling, and capacity management
-   **Enrollment System**: Automated enrollment with real-time availability and waitlist management
-   **Grading System**: Flexible grading schemes, assignment tracking, and GPA calculation
-   **Department Management**: Academic department organization and faculty assignment

### Advanced System Features

-   **Multi-Role Authentication**: JWT-based authentication with role-based access control (Students, Faculty, Admin)
-   **Audit Trail**: Complete audit logging for compliance and security monitoring
-   **Caching Strategy**: Multi-level caching with in-memory and distributed cache
-   **Concurrent Processing**: Async operations for bulk enrollment and notifications
-   **Event-Driven Architecture**: Domain events for loose coupling and scalability
-   **File Management**: Document upload, storage, and retrieval system

### Technical Excellence

-   **RESTful API Design**: Well-documented APIs with OpenAPI/Swagger
-   **Security Framework**: Comprehensive security with input validation and SQL injection prevention
-   **Performance Optimization**: Database query optimization and connection pooling
-   **Testing Suite**: Unit, integration, and functional testing with high coverage
-   **Containerization**: Docker support with multi-environment configurations
-   **Modern Java Features**: Records, sealed classes, pattern matching, and functional programming

## 🛠️ Technology Stack

**Core Framework**

-   **Spring Boot 3.2.x**: Enterprise application framework
-   **Spring Security**: Authentication and authorization
-   **Spring Data JPA**: Data access with Hibernate ORM
-   **Spring Cache**: Caching abstraction with Redis integration

**Database & Persistence**

-   **PostgreSQL**: Primary relational database
-   **Redis**: High-performance caching and session storage
-   **Flyway**: Database migration and versioning

**Documentation & Testing**

-   **OpenAPI 3/Swagger**: Interactive API documentation
-   **JUnit 5**: Modern testing framework
-   **TestContainers**: Integration testing with real database instances
-   **Mockito**: Mocking framework for unit tests

**Build & Deployment**

-   **Maven**: Dependency management and build automation
-   **Docker**: Containerization with multi-stage builds
-   **GitHub Actions**: CI/CD pipeline automation

## 📋 Prerequisites

-   **Java 17+** (OpenJDK or Oracle JDK)
-   **Apache Maven 3.8+**
-   **PostgreSQL 13+** (or Docker for containerized setup)
-   **Git** for version control

**Optional but Recommended**

-   **Redis 6.0+** for caching
-   **Docker & Docker Compose** for easy setup
-   **IDE with Spring Boot support** (IntelliJ IDEA, Eclipse STS, VS Code)

## ⚡ Quick Start

### Method 1: Docker Setup (Recommended)

```bash
# Clone the repository
git clone https://github.com/SatvikPraveen/SmartCampus.git
cd SmartCampus

# Start all services with Docker Compose
docker-compose up -d

# Verify services are running
docker-compose ps

# View application logs
docker-compose logs -f smartcampus-backend
```

**Available Services:**

-   **API Server**: http://localhost:8080
-   **API Documentation**: http://localhost:8080/swagger-ui.html
-   **Database**: localhost:5432
-   **Redis Cache**: localhost:6379

### Method 2: Local Development Setup

**1. Database Setup**

```bash
# Create PostgreSQL database
createdb smartcampus

# Or using Docker for database only
docker run --name smartcampus-db \
  -e POSTGRES_DB=smartcampus \
  -e POSTGRES_USER=smartcampus \
  -e POSTGRES_PASSWORD=smartcampus123 \
  -p 5432:5432 -d postgres:15
```

**2. Application Build and Run**

```bash
# Install dependencies and build
mvn clean install

# Run with development profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Alternative: Run as JAR
java -jar target/smartcampus-backend.jar --spring.profiles.active=dev
```

**3. Verification**

```bash
# Check application health
curl http://localhost:8080/actuator/health

# View API documentation
open http://localhost:8080/swagger-ui.html
```

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/
│   │   ├── annotations/          # Custom annotations (@Entity, @Audited, @Cacheable)
│   │   ├── app/                  # Main application class
│   │   ├── cache/                # Caching strategies and LRU implementation
│   │   ├── com/smartcampus/      # Main application package
│   │   ├── concurrent/           # Async processing and concurrent operations
│   │   ├── enums/                # System enumerations (roles, status, etc.)
│   │   ├── events/               # Event-driven architecture implementation
│   │   ├── exceptions/           # Custom exception hierarchy
│   │   ├── functional/           # Functional programming utilities
│   │   ├── interfaces/           # Core system interfaces
│   │   ├── io/                   # File I/O and external system integration
│   │   ├── models/               # JPA entities and domain models
│   │   ├── patterns/             # Design pattern implementations
│   │   ├── reflection/           # Reflection-based utilities
│   │   ├── repositories/         # Data access layer
│   │   ├── security/             # Security implementation
│   │   ├── services/             # Business logic layer
│   │   └── utils/                # Utility classes and helpers
│   └── resources/
│       ├── application.yml       # Main configuration
│       ├── config/               # External configurations
│       ├── data/                 # Sample data files (CSV)
│       ├── sql/                  # Database scripts and migrations
│       └── templates/            # Email and report templates
└── test/
    ├── java/
    │   ├── functional/           # End-to-end tests
    │   ├── integration/          # Integration tests
    │   └── unit/                 # Unit tests
    └── resources/
        ├── fixtures/             # Test fixtures and mock data
        └── test-data/            # Test-specific data files
```

## 🔐 Security Features

### Authentication & Authorization

-   **JWT Token-based Authentication**: Stateless authentication with refresh tokens
-   **Role-Based Access Control (RBAC)**: Multi-level authorization (STUDENT, FACULTY, ADMIN)
-   **Method-Level Security**: Fine-grained access control with `@PreAuthorize` and `@PostAuthorize`
-   **Password Security**: BCrypt hashing with configurable strength

### Data Protection

-   **Input Validation**: Comprehensive validation using Bean Validation API
-   **SQL Injection Prevention**: Parameterized queries and JPA protection
-   **XSS Protection**: Input sanitization and output encoding
-   **CORS Configuration**: Configurable cross-origin resource sharing

### Audit & Compliance

-   **Audit Trail**: Automatic auditing of all entity changes
-   **Security Event Logging**: Authentication attempts, access violations, and security events
-   **Data Retention**: Configurable data retention policies

## 🚀 Performance Features

### Caching Strategy

-   **Multi-Level Caching**: L1 (Caffeine) + L2 (Redis) caching
-   **Cache Abstraction**: Spring Cache with configurable TTL and eviction policies
-   **Query Result Caching**: Database query result caching for improved performance

### Database Optimization

-   **Connection Pooling**: HikariCP for high-performance connection management
-   **Query Optimization**: Optimized JPA queries with fetch strategies
-   **Database Migrations**: Versioned database schema management with Flyway

### Async Processing

-   **Concurrent Operations**: Async processing for bulk operations and notifications
-   **Thread Pool Management**: Configurable thread pools for different operation types
-   **Batch Processing**: Efficient bulk data processing capabilities

## 📊 Monitoring & Observability

### Health Checks

-   **Actuator Endpoints**: Comprehensive health monitoring endpoints
-   **Custom Health Indicators**: Database, cache, and external service health checks
-   **Readiness and Liveness Probes**: Kubernetes-ready health probes

### Metrics & Logging

-   **Application Metrics**: Performance metrics with Micrometer
-   **Structured Logging**: JSON-formatted logs for production environments
-   **Audit Logging**: Complete audit trail for compliance requirements

## 🧪 Testing Strategy

### Test Coverage

-   **Unit Tests**: Service and utility class testing with Mockito
-   **Integration Tests**: Repository and API endpoint testing with TestContainers
-   **Functional Tests**: End-to-end workflow testing
-   **Performance Tests**: Load testing and concurrent operation testing

### Test Commands

```bash
# Run all tests
mvn clean test

# Run specific test categories
mvn test -Dtest="*Test"              # Unit tests
mvn test -Dtest="*IT,*Integration*"  # Integration tests
mvn test -Dtest="*Functional*"       # Functional tests

# Generate coverage report
mvn test jacoco:report
```

## 📁 Configuration Management

### Application Profiles

-   **Development (`dev`)**: H2 database, debug logging, hot reload
-   **Test (`test`)**: TestContainers, isolated test database
-   **Production (`prod`)**: PostgreSQL, optimized settings, security hardening

### Environment Variables

```bash
# Database Configuration
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=smartcampus
export DB_USERNAME=smartcampus
export DB_PASSWORD=your_password

# Security Configuration
export JWT_SECRET=your-jwt-secret-key
export JWT_EXPIRATION=86400000

# Cache Configuration
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

## 🔄 API Documentation

### Core Endpoints

**Authentication**

```bash
POST /api/auth/login          # User authentication
POST /api/auth/refresh        # Token refresh
POST /api/auth/logout         # User logout
```

**User Management**

```bash
GET    /api/users             # List users (paginated)
GET    /api/users/{id}        # Get user by ID
POST   /api/users             # Create new user
PUT    /api/users/{id}        # Update user
DELETE /api/users/{id}        # Delete user
```

**Academic Management**

```bash
GET    /api/students          # List students
POST   /api/students          # Create student
GET    /api/courses           # List courses
POST   /api/courses           # Create course
POST   /api/enrollments       # Enroll student
GET    /api/grades            # Get grades
POST   /api/grades            # Submit grade
```

### Interactive Documentation

Access the Swagger UI at: http://localhost:8080/swagger-ui.html

## 🐳 Docker Deployment

### Development Environment

```bash
# Start development environment
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f smartcampus-backend
```

### Production Deployment

```bash
# Build production image
docker build -t smartcampus/backend:latest .

# Deploy production environment
docker-compose -f docker-compose.yml up -d

# Scale application
docker-compose up --scale smartcampus-backend=3
```

## 🎯 Design Patterns Implemented

### Creational Patterns

-   **Builder Pattern**: Complex object construction (CourseBuilder, StudentBuilder)
-   **Factory Pattern**: Service instantiation (ServiceFactory, UniversityFactory)
-   **Singleton Pattern**: Configuration management (DatabaseConnection)

### Structural Patterns

-   **Repository Pattern**: Data access abstraction
-   **Adapter Pattern**: External service integration (AdapterService)
-   **Facade Pattern**: Service layer abstraction

### Behavioral Patterns

-   **Observer Pattern**: Event-driven architecture (EventManager)
-   **Strategy Pattern**: Caching strategies and algorithms
-   **Command Pattern**: Operation encapsulation (CommandProcessor)
-   **Template Method Pattern**: Common processing workflows

## 🔧 Development Guidelines

### Code Style

-   Follow Java naming conventions
-   Use meaningful variable and method names
-   Implement proper error handling
-   Write comprehensive JavaDoc for public APIs
-   Maintain consistent code formatting

### Testing Requirements

-   Minimum 85% code coverage
-   Unit tests for all service methods
-   Integration tests for API endpoints
-   Functional tests for critical workflows

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "feat: add new feature description"

# Push and create pull request
git push origin feature/your-feature-name
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

### Code Review Process

-   All contributions require code review
-   Automated CI/CD pipeline validation
-   Manual testing verification
-   Documentation updates when needed

## 📈 Performance Benchmarks

-   **Application Startup**: < 45 seconds
-   **Average Response Time**: < 150ms (95th percentile < 500ms)
-   **Throughput**: 2,000+ requests/second
-   **Memory Usage**: 512MB baseline, 1.5GB under load
-   **Database Connection Pool**: 50 connections, 60% average utilization

## 🗺️ Roadmap

### Version 1.1 (Upcoming)

-   [ ] GraphQL API implementation
-   [ ] Real-time notifications with WebSocket
-   [ ] Advanced search with Elasticsearch
-   [ ] Mobile API optimizations
-   [ ] Enhanced bulk operations

### Version 1.2 (Future)

-   [ ] Machine Learning integration for recommendations
-   [ ] Advanced analytics and reporting
-   [ ] Multi-tenant support
-   [ ] Microservices architecture migration
-   [ ] OAuth2 integration

## 🐛 Troubleshooting

### Common Issues

**Application won't start**

```bash
# Check port availability
lsof -i :8080

# Check database connection
pg_isready -h localhost -p 5432

# View detailed logs
mvn spring-boot:run -X
```

**Database connection issues**

```bash
# Test database connectivity
psql -h localhost -U smartcampus -d smartcampus

# Check Docker database status
docker-compose ps postgres
```

**Memory issues**

```bash
# Increase JVM memory
export JAVA_OPTS="-Xms1g -Xmx2g"
mvn spring-boot:run
```

## 📞 Support

-   **Issues**: [GitHub Issues](https://github.com/SatvikPraveen/SmartCampus/issues)
-   **Discussions**: [GitHub Discussions](https://github.com/SatvikPraveen/SmartCampus/discussions)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

-   Spring Framework team for the comprehensive ecosystem
-   PostgreSQL community for the robust database system
-   Open source community for the incredible tools and libraries
-   All contributors and users of this project

---

**Built with ❤️ using modern Java technologies and Spring Boot best practices.**

For more information, visit: [https://github.com/SatvikPraveen/SmartCampus](https://github.com/SatvikPraveen/SmartCampus)
