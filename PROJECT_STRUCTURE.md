.
├── .DS_Store
├── .editorconfig
├── .env
├── .github
│   └── workflows
│       ├── ci.yml
│       ├── release.yml
│       └── tests.yml
├── .gitignore
├── CHANGELOG.md
├── CONTRIBUTING.md
├── create_smartcampus_structure.sh
├── docker
│   ├── docker-compose.dev.yml
│   ├── docker-compose.yml
│   └── Dockerfile
├── docker-compose.override.yml
├── docker-compose.yml
├── Dockerfile
├── docs
│   ├── api
│   ├── design
│   │   ├── architecture.md
│   │   ├── class-diagrams.puml
│   │   └── design-patterns.md
│   ├── examples
│   │   ├── best-practices.md
│   │   └── usage-examples.md
│   └── guides
│       ├── deployment.md
│       ├── development.md
│       └── getting-started.md
├── LICENSE
├── pom.xml
├── PROJECT_STRUCTURE.md
├── README.md
├── scripts
│   ├── build.sh
│   ├── database
│   │   ├── migrate.sh
│   │   └── setup-db.sh
│   ├── deploy.sh
│   └── test.sh
└── src
    ├── main
    │   ├── java
    │   │   ├── annotations
    │   │   │   ├── Async.java
    │   │   │   ├── Audited.java
    │   │   │   ├── Cacheable.java
    │   │   │   ├── Entity.java
    │   │   │   └── Validator.java
    │   │   ├── app
    │   │   │   └── App.java
    │   │   ├── cache
    │   │   │   ├── CacheManager.java
    │   │   │   ├── CacheStrategy.java
    │   │   │   └── LRUCache.java
    │   │   ├── com
    │   │   │   └── smartcampus
    │   │   │       └── SmartCampusApplication.java
    │   │   ├── concurrent
    │   │   │   ├── AsyncNotificationSender.java
    │   │   │   ├── BatchProcessor.java
    │   │   │   ├── ConcurrentGradeCalculator.java
    │   │   │   ├── DataSyncManager.java
    │   │   │   └── EnrollmentProcessor.java
    │   │   ├── enums
    │   │   │   ├── CourseStatus.java
    │   │   │   ├── EnrollmentStatus.java
    │   │   │   ├── GradeLevel.java
    │   │   │   ├── Priority.java
    │   │   │   ├── Semester.java
    │   │   │   └── UserRole.java
    │   │   ├── events
    │   │   │   ├── CourseCreatedEvent.java
    │   │   │   ├── Event.java
    │   │   │   ├── EventBus.java
    │   │   │   ├── GradeUpdatedEvent.java
    │   │   │   └── StudentEnrolledEvent.java
    │   │   ├── exceptions
    │   │   │   ├── AuthenticationException.java
    │   │   │   ├── DatabaseException.java
    │   │   │   ├── EnrollmentException.java
    │   │   │   ├── InvalidInputException.java
    │   │   │   ├── SystemException.java
    │   │   │   ├── UserNotFoundException.java
    │   │   │   └── ValidationException.java
    │   │   ├── functional
    │   │   │   ├── Collectors.java
    │   │   │   ├── Functions.java
    │   │   │   ├── Predicates.java
    │   │   │   └── StreamUtils.java
    │   │   ├── interfaces
    │   │   │   ├── Auditable.java
    │   │   │   ├── CrudOperations.java
    │   │   │   ├── Enrollable.java
    │   │   │   ├── EventListener.java
    │   │   │   ├── Reportable.java
    │   │   │   ├── Repository.java
    │   │   │   └── Searchable.java
    │   │   ├── io
    │   │   │   ├── BackupManager.java
    │   │   │   ├── ConfigManager.java
    │   │   │   ├── CsvProcessor.java
    │   │   │   ├── DatabaseManager.java
    │   │   │   ├── FileUtil.java
    │   │   │   ├── JsonProcessor.java
    │   │   │   └── LogManager.java
    │   │   ├── models
    │   │   │   ├── Admin.java
    │   │   │   ├── Course.java
    │   │   │   ├── Department.java
    │   │   │   ├── Enrollment.java
    │   │   │   ├── Grade.java
    │   │   │   ├── Professor.java
    │   │   │   ├── Student.java
    │   │   │   ├── University.java
    │   │   │   └── User.java
    │   │   ├── patterns
    │   │   │   ├── AdapterService.java
    │   │   │   ├── CommandProcessor.java
    │   │   │   ├── CourseBuilder.java
    │   │   │   ├── DatabaseConnection.java
    │   │   │   ├── EventManager.java
    │   │   │   ├── ServiceFactory.java
    │   │   │   ├── StudentBuilder.java
    │   │   │   └── UniversityFactory.java
    │   │   ├── reflection
    │   │   │   ├── AnnotationProcessor.java
    │   │   │   ├── ConfigurationLoader.java
    │   │   │   ├── DynamicProxy.java
    │   │   │   └── ModelInspector.java
    │   │   ├── repositories
    │   │   │   ├── BaseRepository.java
    │   │   │   ├── CourseRepository.java
    │   │   │   ├── DepartmentRepository.java
    │   │   │   ├── EnrollmentRepository.java
    │   │   │   ├── ProfessorRepository.java
    │   │   │   └── StudentRepository.java
    │   │   ├── security
    │   │   │   ├── PasswordEncoder.java
    │   │   │   ├── RoleBasedAccess.java
    │   │   │   ├── SecurityManager.java
    │   │   │   └── TokenManager.java
    │   │   ├── services
    │   │   │   ├── AuthService.java
    │   │   │   ├── CourseService.java
    │   │   │   ├── DepartmentService.java
    │   │   │   ├── EnrollmentService.java
    │   │   │   ├── GradeService.java
    │   │   │   ├── NotificationService.java
    │   │   │   ├── ProfessorService.java
    │   │   │   ├── ReportService.java
    │   │   │   ├── SearchService.java
    │   │   │   └── StudentService.java
    │   │   └── utils
    │   │       ├── CacheUtil.java
    │   │       ├── CollectionUtil.java
    │   │       ├── DateUtil.java
    │   │       ├── InputUtil.java
    │   │       ├── MathUtil.java
    │   │       ├── SecurityUtil.java
    │   │       ├── StringUtil.java
    │   │       └── ValidationUtil.java
    │   └── resources
    │       ├── application-dev.yml
    │       ├── application.yml
    │       ├── config
    │       │   ├── application.properties
    │       │   ├── database.properties
    │       │   └── logging.properties
    │       ├── data
    │       │   ├── courses.csv
    │       │   ├── departments.csv
    │       │   ├── enrollments.csv
    │       │   ├── grades.csv
    │       │   ├── professors.csv
    │       │   └── students.csv
    │       ├── sql
    │       │   ├── migrations
    │       │   │   ├── V1__initial_schema.sql
    │       │   │   └── V2__add_grades_table.sql
    │       │   ├── schema.sql
    │       │   └── test-data.sql
    │       └── templates
    │           ├── email-template.html
    │           └── report-template.html
    └── test
        ├── java
        │   ├── functional
        │   │   ├── EnrollmentFlowTest.java
        │   │   ├── GradingFlowTest.java
        │   │   └── ReportingFlowTest.java
        │   ├── integration
        │   │   ├── DatabaseIntegrationTest.java
        │   │   ├── EndToEndTest.java
        │   │   ├── FileIOIntegrationTest.java
        │   │   └── PerformanceTest.java
        │   └── unit
        │       ├── concurrent
        │       │   ├── DataSyncManagerTest.java
        │       │   └── EnrollmentProcessorTest.java
        │       ├── models
        │       │   ├── CourseTest.java
        │       │   ├── DepartmentTest.java
        │       │   ├── ProfessorTest.java
        │       │   └── StudentTest.java
        │       ├── services
        │       │   ├── AuthServiceTest.java
        │       │   ├── CourseServiceTest.java
        │       │   ├── EnrollmentServiceTest.java
        │       │   └── StudentServiceTest.java
        │       └── utils
        │           ├── CollectionUtilTest.java
        │           ├── DateUtilTest.java
        │           └── ValidationUtilTest.java
        └── resources
            ├── fixtures
            │   ├── mock-responses.json
            │   └── sample-reports.json
            └── test-data
                ├── test-config.properties
                ├── test-courses.csv
                └── test-students.csv

51 directories, 175 files
