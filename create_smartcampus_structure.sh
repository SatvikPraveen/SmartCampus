#!/bin/bash

# SmartCampus Project Structure Creation Script
# This script creates the complete folder structure and empty files for the SmartCampus project

echo "🏗️  Creating SmartCampus Project Structure..."
echo "=========================================="

# Create main project directory
mkdir -p SmartCampus
cd SmartCampus

# Create main source structure
echo "📁 Creating main source directories..."
mkdir -p src/main/java/app
mkdir -p src/main/java/models
mkdir -p src/main/java/interfaces
mkdir -p src/main/java/services
mkdir -p src/main/java/repositories
mkdir -p src/main/java/concurrent
mkdir -p src/main/java/io
mkdir -p src/main/java/patterns
mkdir -p src/main/java/reflection
mkdir -p src/main/java/annotations
mkdir -p src/main/java/functional
mkdir -p src/main/java/utils
mkdir -p src/main/java/exceptions
mkdir -p src/main/java/enums
mkdir -p src/main/java/events
mkdir -p src/main/java/cache
mkdir -p src/main/java/security

# Create resources structure
echo "📁 Creating resources directories..."
mkdir -p src/main/resources/data
mkdir -p src/main/resources/config
mkdir -p src/main/resources/templates
mkdir -p src/main/resources/sql/migrations

# Create test structure
echo "📁 Creating test directories..."
mkdir -p src/test/java/unit/models
mkdir -p src/test/java/unit/services
mkdir -p src/test/java/unit/utils
mkdir -p src/test/java/unit/concurrent
mkdir -p src/test/java/integration
mkdir -p src/test/java/functional
mkdir -p src/test/resources/test-data
mkdir -p src/test/resources/fixtures

# Create documentation structure
echo "📁 Creating documentation directories..."
mkdir -p docs/api
mkdir -p docs/design
mkdir -p docs/guides
mkdir -p docs/examples

# Create scripts structure
echo "📁 Creating scripts directories..."
mkdir -p scripts/database

# Create CI/CD structure
echo "📁 Creating CI/CD directories..."
mkdir -p .github/workflows

# Create Docker structure
echo "📁 Creating Docker directories..."
mkdir -p docker

echo "📄 Creating Java source files..."

# APP FILES
touch src/main/java/app/App.java

# MODEL FILES
touch src/main/java/models/User.java
touch src/main/java/models/Student.java
touch src/main/java/models/Professor.java
touch src/main/java/models/Admin.java
touch src/main/java/models/Course.java
touch src/main/java/models/Department.java
touch src/main/java/models/Enrollment.java
touch src/main/java/models/Grade.java
touch src/main/java/models/University.java

# INTERFACE FILES
touch src/main/java/interfaces/Enrollable.java
touch src/main/java/interfaces/Reportable.java
touch src/main/java/interfaces/Searchable.java
touch src/main/java/interfaces/Auditable.java
touch src/main/java/interfaces/Repository.java
touch src/main/java/interfaces/CrudOperations.java
touch src/main/java/interfaces/EventListener.java

# SERVICE FILES
touch src/main/java/services/AuthService.java
touch src/main/java/services/StudentService.java
touch src/main/java/services/ProfessorService.java
touch src/main/java/services/CourseService.java
touch src/main/java/services/DepartmentService.java
touch src/main/java/services/EnrollmentService.java
touch src/main/java/services/GradeService.java
touch src/main/java/services/ReportService.java
touch src/main/java/services/SearchService.java
touch src/main/java/services/NotificationService.java

# REPOSITORY FILES
touch src/main/java/repositories/BaseRepository.java
touch src/main/java/repositories/StudentRepository.java
touch src/main/java/repositories/ProfessorRepository.java
touch src/main/java/repositories/CourseRepository.java
touch src/main/java/repositories/DepartmentRepository.java
touch src/main/java/repositories/EnrollmentRepository.java

# CONCURRENT FILES
touch src/main/java/concurrent/EnrollmentProcessor.java
touch src/main/java/concurrent/DataSyncManager.java
touch src/main/java/concurrent/BatchProcessor.java
touch src/main/java/concurrent/ConcurrentGradeCalculator.java
touch src/main/java/concurrent/AsyncNotificationSender.java

# I/O FILES
touch src/main/java/io/FileUtil.java
touch src/main/java/io/CsvProcessor.java
touch src/main/java/io/JsonProcessor.java
touch src/main/java/io/ConfigManager.java
touch src/main/java/io/DatabaseManager.java
touch src/main/java/io/BackupManager.java
touch src/main/java/io/LogManager.java

# PATTERN FILES
touch src/main/java/patterns/StudentBuilder.java
touch src/main/java/patterns/CourseBuilder.java
touch src/main/java/patterns/UniversityFactory.java
touch src/main/java/patterns/ServiceFactory.java
touch src/main/java/patterns/EventManager.java
touch src/main/java/patterns/DatabaseConnection.java
touch src/main/java/patterns/CommandProcessor.java
touch src/main/java/patterns/AdapterService.java

# REFLECTION FILES
touch src/main/java/reflection/ModelInspector.java
touch src/main/java/reflection/AnnotationProcessor.java
touch src/main/java/reflection/DynamicProxy.java
touch src/main/java/reflection/ConfigurationLoader.java

# ANNOTATION FILES
touch src/main/java/annotations/Entity.java
touch src/main/java/annotations/Validator.java
touch src/main/java/annotations/Cacheable.java
touch src/main/java/annotations/Audited.java
touch src/main/java/annotations/Async.java

# FUNCTIONAL FILES
touch src/main/java/functional/Predicates.java
touch src/main/java/functional/Functions.java
touch src/main/java/functional/Collectors.java
touch src/main/java/functional/StreamUtils.java

# UTILITY FILES
touch src/main/java/utils/ValidationUtil.java
touch src/main/java/utils/InputUtil.java
touch src/main/java/utils/DateUtil.java
touch src/main/java/utils/StringUtil.java
touch src/main/java/utils/CollectionUtil.java
touch src/main/java/utils/CacheUtil.java
touch src/main/java/utils/SecurityUtil.java
touch src/main/java/utils/MathUtil.java

# EXCEPTION FILES
touch src/main/java/exceptions/InvalidInputException.java
touch src/main/java/exceptions/UserNotFoundException.java
touch src/main/java/exceptions/EnrollmentException.java
touch src/main/java/exceptions/DatabaseException.java
touch src/main/java/exceptions/AuthenticationException.java
touch src/main/java/exceptions/ValidationException.java
touch src/main/java/exceptions/SystemException.java

# ENUM FILES
touch src/main/java/enums/UserRole.java
touch src/main/java/enums/CourseStatus.java
touch src/main/java/enums/EnrollmentStatus.java
touch src/main/java/enums/GradeLevel.java
touch src/main/java/enums/Semester.java
touch src/main/java/enums/Priority.java

# EVENT FILES
touch src/main/java/events/Event.java
touch src/main/java/events/StudentEnrolledEvent.java
touch src/main/java/events/GradeUpdatedEvent.java
touch src/main/java/events/CourseCreatedEvent.java
touch src/main/java/events/EventBus.java

# CACHE FILES
touch src/main/java/cache/CacheManager.java
touch src/main/java/cache/LRUCache.java
touch src/main/java/cache/CacheStrategy.java

# SECURITY FILES
touch src/main/java/security/SecurityManager.java
touch src/main/java/security/PasswordEncoder.java
touch src/main/java/security/TokenManager.java
touch src/main/java/security/RoleBasedAccess.java

echo "📄 Creating resource files..."

# RESOURCE DATA FILES
touch src/main/resources/data/students.csv
touch src/main/resources/data/professors.csv
touch src/main/resources/data/courses.csv
touch src/main/resources/data/departments.csv
touch src/main/resources/data/enrollments.csv
touch src/main/resources/data/grades.csv

# RESOURCE CONFIG FILES
touch src/main/resources/config/application.properties
touch src/main/resources/config/database.properties
touch src/main/resources/config/logging.properties

# RESOURCE TEMPLATE FILES
touch src/main/resources/templates/report-template.html
touch src/main/resources/templates/email-template.html

# RESOURCE SQL FILES
touch src/main/resources/sql/schema.sql
touch src/main/resources/sql/test-data.sql
touch src/main/resources/sql/migrations/V1__initial_schema.sql
touch src/main/resources/sql/migrations/V2__add_grades_table.sql

echo "📄 Creating test files..."

# UNIT TEST FILES - MODELS
touch src/test/java/unit/models/StudentTest.java
touch src/test/java/unit/models/ProfessorTest.java
touch src/test/java/unit/models/CourseTest.java
touch src/test/java/unit/models/DepartmentTest.java

# UNIT TEST FILES - SERVICES
touch src/test/java/unit/services/StudentServiceTest.java
touch src/test/java/unit/services/CourseServiceTest.java
touch src/test/java/unit/services/EnrollmentServiceTest.java
touch src/test/java/unit/services/AuthServiceTest.java

# UNIT TEST FILES - UTILS
touch src/test/java/unit/utils/ValidationUtilTest.java
touch src/test/java/unit/utils/DateUtilTest.java
touch src/test/java/unit/utils/CollectionUtilTest.java

# UNIT TEST FILES - CONCURRENT
touch src/test/java/unit/concurrent/EnrollmentProcessorTest.java
touch src/test/java/unit/concurrent/DataSyncManagerTest.java

# INTEGRATION TEST FILES
touch src/test/java/integration/DatabaseIntegrationTest.java
touch src/test/java/integration/FileIOIntegrationTest.java
touch src/test/java/integration/EndToEndTest.java
touch src/test/java/integration/PerformanceTest.java

# FUNCTIONAL TEST FILES
touch src/test/java/functional/EnrollmentFlowTest.java
touch src/test/java/functional/GradingFlowTest.java
touch src/test/java/functional/ReportingFlowTest.java

# TEST RESOURCE FILES
touch src/test/resources/test-data/test-students.csv
touch src/test/resources/test-data/test-courses.csv
touch src/test/resources/test-data/test-config.properties
touch src/test/resources/fixtures/sample-reports.json
touch src/test/resources/fixtures/mock-responses.json

echo "📄 Creating documentation files..."

# DOCUMENTATION FILES
touch docs/design/architecture.md
touch docs/design/design-patterns.md
touch docs/design/class-diagrams.puml
touch docs/guides/getting-started.md
touch docs/guides/development.md
touch docs/guides/deployment.md
touch docs/examples/usage-examples.md
touch docs/examples/best-practices.md

echo "📄 Creating script files..."

# SCRIPT FILES
touch scripts/build.sh
touch scripts/deploy.sh
touch scripts/test.sh
touch scripts/database/setup-db.sh
touch scripts/database/migrate.sh

echo "📄 Creating CI/CD files..."

# CI/CD FILES
touch .github/workflows/ci.yml
touch .github/workflows/tests.yml
touch .github/workflows/release.yml

echo "📄 Creating Docker files..."

# DOCKER FILES
touch docker/Dockerfile
touch docker/docker-compose.yml
touch docker/docker-compose.dev.yml

echo "📄 Creating root configuration files..."

# ROOT CONFIGURATION FILES
touch .gitignore
touch .editorconfig
touch pom.xml
touch README.md
touch CONTRIBUTING.md
touch LICENSE
touch CHANGELOG.md

echo "✅ Making script files executable..."

# Make script files executable
chmod +x scripts/*.sh
chmod +x scripts/database/*.sh

echo ""
echo "🎉 SUCCESS! SmartCampus project structure created successfully!"
echo ""
echo "📊 Project Statistics:"
echo "   📁 Directories created: $(find . -type d | wc -l)"
echo "   📄 Files created: $(find . -type f | wc -l)"
echo ""
echo "📂 Project structure:"
echo "   ├── src/main/java/ ($(find src/main/java -name "*.java" | wc -l) Java files)"
echo "   ├── src/test/java/ ($(find src/test/java -name "*.java" | wc -l) test files)"
echo "   ├── src/main/resources/ ($(find src/main/resources -type f | wc -l) resource files)"
echo "   ├── docs/ ($(find docs -type f | wc -l) documentation files)"
echo "   ├── scripts/ ($(find scripts -name "*.sh" | wc -l) shell scripts)"
echo "   └── Configuration files ($(ls -1 *.* | wc -l) files)"
echo ""
echo "🚀 Next steps:"
echo "   1. cd SmartCampus"
echo "   2. Start implementing your Java files"
echo "   3. Initialize git repository: git init"
echo "   4. Add your files: git add ."
echo "   5. Make initial commit: git commit -m 'Initial project structure'"
echo ""
echo "💡 Tip: Use 'tree' command to visualize the structure:"
echo "   tree -I 'target|*.class' SmartCampus"
echo ""
echo "Happy coding! 🖥️✨"