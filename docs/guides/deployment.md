# SmartCampus Deployment Guide

**Location:** `docs/guides/deployment.md`

## Overview

This guide covers the deployment strategies and procedures for the SmartCampus university management system across different environments including development, testing, staging, and production.

## Deployment Strategies

### 1. Local Development Deployment

#### Prerequisites

- Java 17+ JDK
- Maven 3.8+
- Git
- Docker (optional)

#### Quick Start

```bash
# Clone and navigate to project
git clone https://github.com/your-org/smartcampus.git
cd smartcampus

# Build the application
mvn clean package

# Run locally
java -jar target/smartcampus-1.0.0.jar

# Or using Maven
mvn spring-boot:run
```

#### Development with Hot Reload

```bash
# Enable development mode with auto-restart
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or use development profile
export SPRING_PROFILES_ACTIVE=dev
java -jar target/smartcampus-1.0.0.jar
```

### 2. Docker Deployment

#### Single Container Deployment

```bash
# Build Docker image
docker build -t smartcampus:latest .

# Run container
docker run -d \
  --name smartcampus-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  smartcampus:latest

# View logs
docker logs -f smartcampus-app
```

#### Multi-Container with Docker Compose

```bash
# Start all services (app + database)
docker-compose up -d

# View service status
docker-compose ps

# View aggregated logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### 3. Production Deployment

#### Traditional Server Deployment

##### System Requirements

- **OS**: Linux (Ubuntu 20.04 LTS recommended), Windows Server 2019+, or macOS
- **CPU**: Minimum 2 cores, Recommended 4+ cores
- **Memory**: Minimum 4GB RAM, Recommended 8GB+
- **Storage**: Minimum 20GB, Recommended 50GB+ SSD
- **Network**: Stable internet connection with appropriate firewall configuration

##### Installation Steps

```bash
# 1. Create application user
sudo useradd -r -s /bin/false smartcampus
sudo mkdir -p /opt/smartcampus
sudo chown smartcampus:smartcampus /opt/smartcampus

# 2. Deploy application
sudo -u smartcampus cp smartcampus-1.0.0.jar /opt/smartcampus/
sudo -u smartcampus cp application-prod.properties /opt/smartcampus/

# 3. Create systemd service
sudo cp smartcampus.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable smartcampus
sudo systemctl start smartcampus

# 4. Verify deployment
sudo systemctl status smartcampus
curl http://localhost:8080/health
```

##### SystemD Service Configuration

```ini
# /etc/systemd/system/smartcampus.service
[Unit]
Description=SmartCampus University Management System
After=network.target

[Service]
Type=simple
User=smartcampus
WorkingDirectory=/opt/smartcampus
ExecStart=/usr/bin/java -jar /opt/smartcampus/smartcampus-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=smartcampus

# Environment variables
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=JAVA_OPTS=-Xmx2g -Xms512m

# Security settings
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/smartcampus/logs

[Install]
WantedBy=multi-user.target
```

## Environment Configurations

### 1. Development Environment

#### Configuration Properties

```properties
# application-dev.properties
spring.profiles.active=dev

# Database configuration (H2 for development)
spring.datasource.url=jdbc:h2:mem:smartcampus_dev
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true

# Logging configuration
logging.level.com.smartcampus=DEBUG
logging.level.org.springframework=INFO

# Development-specific settings
app.cache.enabled=false
app.security.jwt.secret=dev-secret-key-change-in-production
app.concurrent.thread-pool-size=5
```

#### Docker Development Setup

```yaml
# docker-compose.dev.yml
version: "3.8"

services:
  smartcampus-app:
    build:
      context: .
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
      - "5005:5005" # Debug port
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    volumes:
      - ./src:/app/src
      - ./target:/app/target
    networks:
      - smartcampus-dev

  postgres-dev:
    image: postgres:13
    environment:
      POSTGRES_DB: smartcampus_dev
      POSTGRES_USER: dev_user
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
    networks:
      - smartcampus-dev

volumes:
  postgres_dev_data:

networks:
  smartcampus-dev:
```

### 2. Testing Environment

#### Configuration Properties

```properties
# application-test.properties
spring.profiles.active=test

# Test database configuration
spring.datasource.url=jdbc:h2:mem:smartcampus_test
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Testing-specific settings
app.cache.enabled=true
app.cache.size=100
logging.level.com.smartcampus=INFO

# Disable external integrations in tests
app.email.enabled=false
app.sms.enabled=false
```

#### Continuous Integration Configuration

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: test_password
          POSTGRES_DB: smartcampus_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: "17"
          distribution: "temurin"

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Run tests
        run: mvn clean test -Pci
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/smartcampus_test
          DATABASE_USERNAME: postgres
          DATABASE_PASSWORD: test_password

      - name: Generate test report
        run: mvn surefire-report:report

      - name: Upload coverage reports
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml
```

### 3. Staging Environment

#### Configuration Properties

```properties
# application-staging.properties
spring.profiles.active=staging

# Staging database configuration
spring.datasource.url=jdbc:postgresql://staging-db:5432/smartcampus_staging
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Connection pool configuration
spring.datasource.hikari.maximum-pool-size=15
spring.datasource.hikari.minimum-idle=5

# Caching configuration
app.cache.enabled=true
app.cache.size=5000
app.cache.ttl=3600

# Security configuration
app.security.jwt.secret=${JWT_SECRET}
app.security.jwt.expiration=86400

# Monitoring and logging
management.endpoints.web.exposure.include=health,metrics,info
logging.level.com.smartcampus=INFO
```

#### Docker Compose for Staging

```yaml
# docker-compose.staging.yml
version: "3.8"

services:
  smartcampus-app:
    image: smartcampus:${APP_VERSION}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - DATABASE_USERNAME=${DATABASE_USERNAME}
      - DATABASE_PASSWORD=${DATABASE_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres-staging
    networks:
      - smartcampus-staging
    restart: unless-stopped

  postgres-staging:
    image: postgres:13
    environment:
      POSTGRES_DB: smartcampus_staging
      POSTGRES_USER: ${DATABASE_USERNAME}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - postgres_staging_data:/var/lib/postgresql/data
      - ./scripts/database/init-staging.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - smartcampus-staging
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/staging.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/ssl/certs
    depends_on:
      - smartcampus-app
    networks:
      - smartcampus-staging
    restart: unless-stopped

volumes:
  postgres_staging_data:

networks:
  smartcampus-staging:
```

### 4. Production Environment

#### Configuration Properties

```properties
# application-prod.properties
spring.profiles.active=prod

# Production database configuration
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Connection pool optimization for production
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# JPA/Hibernate configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.jdbc.batch_size=20

# Security configuration
app.security.jwt.secret=${JWT_SECRET}
app.security.jwt.expiration=3600
app.security.password.strength=STRONG

# Performance settings
app.cache.enabled=true
app.cache.size=10000
app.cache.ttl=1800
app.concurrent.thread-pool-size=20

# Monitoring and actuator
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=when-authorized

# Logging configuration for production
logging.level.com.smartcampus=INFO
logging.level.org.springframework.security=WARN
logging.pattern.file=%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n
logging.file.name=/opt/smartcampus/logs/application.log
logging.file.max-size=100MB
logging.file.max-history=30
```

## Database Setup and Migration

### 1. Database Schema Initialization

#### PostgreSQL Setup

```sql
-- Create database and user
CREATE DATABASE smartcampus_prod;
CREATE USER smartcampus_user WITH ENCRYPTED PASSWORD 'secure_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE smartcampus_prod TO smartcampus_user;
GRANT ALL ON SCHEMA public TO smartcampus_user;
GRANT ALL ON ALL TABLES IN SCHEMA public TO smartcampus_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO smartcampus_user;

-- Create application schema
\connect smartcampus_prod
CREATE SCHEMA IF NOT EXISTS smartcampus;
GRANT ALL ON SCHEMA smartcampus TO smartcampus_user;
```

#### Database Migration Strategy

```bash
# 1. Create backup before migration
pg_dump -h localhost -U smartcampus_user -d smartcampus_prod > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Run Flyway migrations
mvn flyway:migrate -Pflyway-prod

# 3. Verify migration
mvn flyway:info -Pflyway-prod
```

#### Flyway Configuration

```properties
# flyway.conf
flyway.url=jdbc:postgresql://localhost:5432/smartcampus_prod
flyway.user=smartcampus_user
flyway.password=${FLYWAY_PASSWORD}
flyway.schemas=smartcampus
flyway.locations=classpath:db/migration
flyway.baselineOnMigrate=true
flyway.validateOnMigrate=true
```

### 2. Data Migration and Seeding

#### Production Data Seeding

```java
@Component
@Profile("prod")
public class ProductionDataSeeder implements ApplicationRunner {

    private final UniversityService universityService;
    private final DepartmentService departmentService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (shouldSeedData()) {
            seedUniversityData();
            seedDepartmentData();
            seedDefaultUsers();
        }
    }

    private void seedUniversityData() {
        University university = new UniversityBuilder()
            .setName("SmartCampus University")
            .setType(UniversityType.PUBLIC)
            .setEstablishedDate(LocalDate.of(1950, 1, 1))
            .build();

        universityService.createUniversity(university);
    }
}
```

## Load Balancing and High Availability

### 1. Nginx Load Balancer Configuration

```nginx
# /etc/nginx/sites-available/smartcampus
upstream smartcampus_backend {
    least_conn;
    server smartcampus-app1:8080 weight=3 max_fails=3 fail_timeout=30s;
    server smartcampus-app2:8080 weight=3 max_fails=3 fail_timeout=30s;
    server smartcampus-app3:8080 weight=2 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name smartcampus.example.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name smartcampus.example.com;

    # SSL Configuration
    ssl_certificate /etc/ssl/certs/smartcampus.crt;
    ssl_certificate_key /etc/ssl/private/smartcampus.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript;

    # Main application proxy
    location / {
        proxy_pass http://smartcampus_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        # Health check
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://smartcampus_backend/actuator/health;
        access_log off;
    }

    # Static content caching
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### 2. Application-Level High Availability

#### Health Checks Configuration

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        Health.Builder status = Health.up();

        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(1)) {
                status.down().withDetail("database", "Connection invalid");
            } else {
                status.withDetail("database", "Available");
            }
        } catch (SQLException e) {
            status.down().withDetail("database", "Error: " + e.getMessage());
        }

        // Check Redis connectivity
        try {
            redisTemplate.opsForValue().get("health-check");
            status.withDetail("redis", "Available");
        } catch (Exception e) {
            status.withDetail("redis", "Error: " + e.getMessage());
        }

        return status.build();
    }
}
```

#### Circuit Breaker Pattern

```java
@Service
public class ExternalServiceClient {

    private final CircuitBreaker circuitBreaker;

    public ExternalServiceClient() {
        this.circuitBreaker = CircuitBreaker.ofDefaults("externalService");
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String callExternalService(String data) {
        return circuitBreaker.executeSupplier(() -> {
            // External service call
            return restTemplate.postForObject("/api/external", data, String.class);
        });
    }
}
```

## Monitoring and Observability

### 1. Application Monitoring

#### Prometheus Metrics Configuration

```java
@Configuration
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
public class MonitoringConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configurer() {
        return registry -> registry.config()
            .commonTags("application", "smartcampus")
            .commonTags("version", getClass().getPackage().getImplementationVersion());
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

#### Custom Metrics

```java
@Service
public class StudentService {

    private final Counter studentCreationCounter;
    private final Timer enrollmentProcessingTime;
    private final Gauge activeStudentsGauge;

    public StudentService(MeterRegistry meterRegistry) {
        this.studentCreationCounter = Counter.builder("student.created.total")
            .description("Total number of students created")
            .register(meterRegistry);

        this.enrollmentProcessingTime = Timer.builder("enrollment.processing.time")
            .description("Time taken to process enrollment")
            .register(meterRegistry);

        this.activeStudentsGauge = Gauge.builder("students.active.count")
            .description("Number of active students")
            .register(meterRegistry, this, StudentService::getActiveStudentCount);
    }

    @Timed(name = "student.creation.time", description = "Time taken to create a student")
    public Student createStudent(Student student) {
        studentCreationCounter.increment();
        return repository.save(student);
    }
}
```

### 2. Log Management

#### Structured Logging with Logback

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="prod">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/smartcampus.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/smartcampus.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <arguments/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### 3. APM Integration

#### Application Performance Monitoring

```java
@Configuration
public class APMConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.apm.enabled", havingValue = "true")
    public ElasticApmAgent elasticApmAgent() {
        return ElasticApmAgent.attach();
    }
}
```

## Security Hardening

### 1. Application Security

#### Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Disable for API
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/professor/**").hasAnyRole("PROFESSOR", "ADMIN")
                .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "PROFESSOR", "ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### 2. Infrastructure Security

#### Firewall Configuration

```bash
# UFW (Ubuntu Firewall) rules
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow from 10.0.0.0/8 to any port 8080  # Internal network only
sudo ufw enable
```

#### SSL/TLS Configuration

```bash
# Generate SSL certificate using Let's Encrypt
sudo certbot --nginx -d smartcampus.example.com

# Set up automatic renewal
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

## Backup and Disaster Recovery

### 1. Database Backup Strategy

#### Automated Backup Script

```bash
#!/bin/bash
# backup.sh

DB_NAME="smartcampus_prod"
DB_USER="smartcampus_user"
BACKUP_DIR="/opt/backups/database"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/smartcampus_${TIMESTAMP}.sql"

# Create backup directory if it doesn't exist
mkdir -p ${BACKUP_DIR}

# Create database backup
pg_dump -h localhost -U ${DB_USER} -d ${DB_NAME} > ${BACKUP_FILE}

# Compress the backup
gzip ${BACKUP_FILE}

# Remove backups older than 30 days
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -delete

# Upload to cloud storage (example with AWS S3)
aws s3 cp ${BACKUP_FILE}.gz s3://smartcampus-backups/database/

echo "Backup completed: ${BACKUP_FILE}.gz"
```

#### Automated Backup with Cron

```bash
# Add to crontab
0 2 * * * /opt/smartcampus/scripts/backup.sh >> /var/log/smartcampus-backup.log 2>&1
```

### 2. Application Backup

#### File System Backup

```bash
#!/bin/bash
# app-backup.sh

APP_DIR="/opt/smartcampus"
BACKUP_DIR="/opt/backups/application"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create backup
tar -czf ${BACKUP_DIR}/smartcampus-app_${TIMESTAMP}.tar.gz \
    ${APP_DIR}/*.jar \
    ${APP_DIR}/*.properties \
    ${APP_DIR}/logs \
    ${APP_DIR}/config

# Upload to cloud storage
aws s3 cp ${BACKUP_DIR}/smartcampus-app_${TIMESTAMP}.tar.gz \
    s3://smartcampus-backups/application/

# Cleanup old backups
find ${BACKUP_DIR} -name "*.tar.gz" -mtime +7 -delete
```

### 3. Disaster Recovery Plan

#### Recovery Procedure

```bash
# 1. Stop the application
sudo systemctl stop smartcampus

# 2. Restore database
gunzip -c smartcampus_20240315_020001.sql.gz | psql -h localhost -U smartcampus_user -d smartcampus_prod

# 3. Restore application files
cd /opt/smartcampus
tar -xzf /opt/backups/application/smartcampus-app_20240315_020001.tar.gz

# 4. Update configuration if needed
sudo nano application-prod.properties

# 5. Start the application
sudo systemctl start smartcampus

# 6. Verify recovery
curl http://localhost:8080/actuator/health
sudo systemctl status smartcampus
```

## Performance Tuning

### 1. JVM Tuning

#### Production JVM Arguments

```bash
# /opt/smartcampus/start.sh
JAVA_OPTS="-server \
  -Xms2g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+UseCompressedOops \
  -XX:+UseCompressedClassPointers \
  -XX:+DisableExplicitGC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/opt/smartcampus/logs/heap_dump.hprof \
  -XX:+PrintGCDetails \
  -XX:+PrintGCTimeStamps \
  -Xloggc:/opt/smartcampus/logs/gc.log \
  -XX:+UseGCLogFileRotation \
  -XX:NumberOfGCLogFiles=5 \
  -XX:GCLogFileSize=10M"

java $JAVA_OPTS -jar smartcampus-1.0.0.jar
```

### 2. Database Performance Tuning

#### PostgreSQL Configuration

```sql
-- postgresql.conf optimizations
shared_buffers = '1GB'
effective_cache_size = '3GB'
maintenance_work_mem = '256MB'
work_mem = '16MB'
wal_buffers = '16MB'
checkpoint_completion_target = 0.9
random_page_cost = 1.1
effective_io_concurrency = 200
max_connections = 100
```

#### Database Indexing Strategy

```sql
-- Create performance indexes
CREATE INDEX CONCURRENTLY idx_students_major ON students(major);
CREATE INDEX CONCURRENTLY idx_students_enrollment_year ON students(enrollment_year);
CREATE INDEX CONCURRENTLY idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX CONCURRENTLY idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX CONCURRENTLY idx_enrollments_status ON enrollments(status);
CREATE INDEX CONCURRENTLY idx_grades_student_course ON grades(student_id, course_id);
CREATE INDEX CONCURRENTLY idx_courses_department ON courses(department_id);
CREATE INDEX CONCURRENTLY idx_courses_semester_year ON courses(semester, year);

-- Create composite indexes for common query patterns
CREATE INDEX CONCURRENTLY idx_students_major_gpa ON students(major, gpa DESC);
CREATE INDEX CONCURRENTLY idx_enrollments_status_date ON enrollments(status, enrollment_date);
```

### 3. Application Performance Optimization

#### Connection Pool Tuning

```properties
# Optimized connection pool settings for production
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=15
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.initialization-fail-timeout=1
```

#### Caching Configuration

```java
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats());
        return cacheManager;
    }

    @Bean
    public CacheMetricsRegistrar cacheMetricsRegistrar(MeterRegistry meterRegistry) {
        return new CacheMetricsRegistrar(meterRegistry);
    }
}
```

## Deployment Automation

### 1. CI/CD Pipeline

#### GitHub Actions Deployment Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]
    tags: ["v*"]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: "17"
          distribution: "temurin"

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run tests
        run: mvn clean test

      - name: Generate test report
        run: mvn surefire-report:report

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: "17"
          distribution: "temurin"

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Build application
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: |
          docker build -t smartcampus:${{ github.sha }} .
          docker tag smartcampus:${{ github.sha }} smartcampus:latest

      - name: Login to Docker Registry
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Push Docker image
        run: |
          docker tag smartcampus:latest ghcr.io/${{ github.repository }}:latest
          docker tag smartcampus:latest ghcr.io/${{ github.repository }}:${{ github.sha }}
          docker push ghcr.io/${{ github.repository }}:latest
          docker push ghcr.io/${{ github.repository }}:${{ github.sha }}

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy to staging
        run: |
          echo "Deploying to staging environment..."
          # Add staging deployment commands here

  deploy-production:
    needs: [build, deploy-staging]
    runs-on: ubuntu-latest
    environment: production
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - name: Deploy to production
        run: |
          echo "Deploying to production environment..."
          # Add production deployment commands here
```

### 2. Blue-Green Deployment

#### Blue-Green Deployment Script

```bash
#!/bin/bash
# blue-green-deploy.sh

set -e

DOCKER_IMAGE="ghcr.io/your-org/smartcampus:$1"
CURRENT_COLOR=$(docker ps --filter "name=smartcampus" --format "{{.Names}}" | grep -o -E "(blue|green)" | head -1)

if [ "$CURRENT_COLOR" = "blue" ]; then
    NEW_COLOR="green"
    OLD_COLOR="blue"
else
    NEW_COLOR="blue"
    OLD_COLOR="green"
fi

echo "Current deployment: $CURRENT_COLOR"
echo "New deployment: $NEW_COLOR"
echo "Docker image: $DOCKER_IMAGE"

# Pull new image
docker pull $DOCKER_IMAGE

# Start new deployment
echo "Starting $NEW_COLOR deployment..."
docker run -d \
    --name smartcampus-$NEW_COLOR \
    --network smartcampus-network \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DATABASE_URL=$DATABASE_URL \
    -e DATABASE_USERNAME=$DATABASE_USERNAME \
    -e DATABASE_PASSWORD=$DATABASE_PASSWORD \
    -e JWT_SECRET=$JWT_SECRET \
    $DOCKER_IMAGE

# Wait for health check
echo "Waiting for $NEW_COLOR deployment to be ready..."
for i in {1..30}; do
    if curl -f http://smartcampus-$NEW_COLOR:8080/actuator/health > /dev/null 2>&1; then
        echo "$NEW_COLOR deployment is healthy"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 10
done

# Check if deployment is healthy
if ! curl -f http://smartcampus-$NEW_COLOR:8080/actuator/health > /dev/null 2>&1; then
    echo "ERROR: $NEW_COLOR deployment failed health check"
    docker logs smartcampus-$NEW_COLOR
    docker rm -f smartcampus-$NEW_COLOR
    exit 1
fi

# Update load balancer to point to new deployment
echo "Updating load balancer configuration..."
sed -i "s/smartcampus-$OLD_COLOR/smartcampus-$NEW_COLOR/g" /etc/nginx/sites-available/smartcampus
nginx -t && systemctl reload nginx

# Wait a moment for connections to migrate
sleep 30

# Stop old deployment
if [ ! -z "$OLD_COLOR" ] && docker ps -q -f name=smartcampus-$OLD_COLOR; then
    echo "Stopping $OLD_COLOR deployment..."
    docker stop smartcampus-$OLD_COLOR
    docker rm smartcampus-$OLD_COLOR
fi

echo "Blue-green deployment completed successfully!"
echo "Active deployment: $NEW_COLOR"
```

### 3. Rolling Deployment

#### Rolling Update Script

```bash
#!/bin/bash
# rolling-deploy.sh

set -e

DOCKER_IMAGE="ghcr.io/your-org/smartcampus:$1"
INSTANCES=3

echo "Starting rolling deployment with image: $DOCKER_IMAGE"

# Pull new image on all nodes
docker pull $DOCKER_IMAGE

# Update instances one by one
for i in $(seq 1 $INSTANCES); do
    INSTANCE_NAME="smartcampus-app$i"

    echo "Updating instance: $INSTANCE_NAME"

    # Remove from load balancer
    echo "Removing $INSTANCE_NAME from load balancer..."
    # This would depend on your load balancer configuration

    # Stop old instance
    docker stop $INSTANCE_NAME || true
    docker rm $INSTANCE_NAME || true

    # Start new instance
    docker run -d \
        --name $INSTANCE_NAME \
        --network smartcampus-network \
        -e SPRING_PROFILES_ACTIVE=prod \
        -e DATABASE_URL=$DATABASE_URL \
        -e DATABASE_USERNAME=$DATABASE_USERNAME \
        -e DATABASE_PASSWORD=$DATABASE_PASSWORD \
        -e JWT_SECRET=$JWT_SECRET \
        $DOCKER_IMAGE

    # Wait for health check
    echo "Waiting for $INSTANCE_NAME to be ready..."
    for j in {1..30}; do
        if curl -f http://$INSTANCE_NAME:8080/actuator/health > /dev/null 2>&1; then
            echo "$INSTANCE_NAME is healthy"
            break
        fi
        sleep 10
    done

    # Check if instance is healthy
    if ! curl -f http://$INSTANCE_NAME:8080/actuator/health > /dev/null 2>&1; then
        echo "ERROR: $INSTANCE_NAME failed health check"
        exit 1
    fi

    # Add back to load balancer
    echo "Adding $INSTANCE_NAME back to load balancer..."
    # This would depend on your load balancer configuration

    echo "Instance $INSTANCE_NAME updated successfully"

    # Wait before updating next instance
    if [ $i -lt $INSTANCES ]; then
        echo "Waiting 30 seconds before updating next instance..."
        sleep 30
    fi
done

echo "Rolling deployment completed successfully!"
```

## Troubleshooting

### 1. Common Deployment Issues

#### Application Won't Start

```bash
# Check application logs
sudo journalctl -u smartcampus -f

# Check Docker container logs
docker logs smartcampus-app -f

# Check system resources
free -h
df -h
top

# Check network connectivity
netstat -tlnp | grep 8080
curl -I http://localhost:8080/actuator/health
```

#### Database Connection Issues

```bash
# Test database connectivity
psql -h localhost -U smartcampus_user -d smartcampus_prod -c "SELECT 1;"

# Check database status
sudo systemctl status postgresql

# Monitor database connections
psql -h localhost -U smartcampus_user -d smartcampus_prod -c "
SELECT count(*) as active_connections
FROM pg_stat_activity
WHERE state = 'active';"

# Check connection pool status (if using monitoring)
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

#### Memory Issues

```bash
# Check JVM memory usage
jstat -gc $(pgrep java)

# Generate heap dump for analysis
jcmd $(pgrep java) GC.run_finalization
jcmd $(pgrep java) VM.gc
jmap -dump:format=b,file=heapdump.hprof $(pgrep java)

# Analyze heap dump (using Eclipse MAT or similar)
# Download and analyze heapdump.hprof
```

### 2. Performance Issues

#### Slow Database Queries

```sql
-- Enable query logging temporarily
ALTER SYSTEM SET log_min_duration_statement = 1000; -- Log queries > 1 second
SELECT pg_reload_conf();

-- Check slow queries
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Check for missing indexes
SELECT schemaname, tablename, attname, n_distinct, correlation
FROM pg_stats
WHERE schemaname = 'public'
ORDER BY n_distinct DESC;
```

#### High CPU Usage

```bash
# Check Java thread dump
jstack $(pgrep java) > thread_dump.txt

# Monitor CPU usage by thread
top -H -p $(pgrep java)

# Check application metrics
curl http://localhost:8080/actuator/metrics/process.cpu.usage
curl http://localhost:8080/actuator/metrics/jvm.threads.live
```

### 3. Recovery Procedures

#### Database Recovery

```bash
# Stop application
sudo systemctl stop smartcampus

# Restore from latest backup
LATEST_BACKUP=$(ls -t /opt/backups/database/smartcampus_*.sql.gz | head -1)
gunzip -c $LATEST_BACKUP | psql -h localhost -U smartcampus_user -d smartcampus_prod

# Restart application
sudo systemctl start smartcampus

# Verify functionality
curl http://localhost:8080/actuator/health
```

#### Application Recovery

```bash
# Rollback to previous version
docker stop smartcampus-app
docker run -d \
    --name smartcampus-app \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=prod \
    ghcr.io/your-org/smartcampus:previous-stable-tag

# Or restore from backup
cd /opt/smartcampus
tar -xzf /opt/backups/application/smartcampus-app_latest.tar.gz
sudo systemctl restart smartcampus
```

## Maintenance Procedures

### 1. Regular Maintenance Tasks

#### Weekly Maintenance Checklist

```bash
#!/bin/bash
# weekly-maintenance.sh

echo "=== Weekly Maintenance Report $(date) ===" >> /var/log/maintenance.log

# 1. Check disk space
echo "Disk Usage:" >> /var/log/maintenance.log
df -h >> /var/log/maintenance.log

# 2. Check system load
echo "System Load:" >> /var/log/maintenance.log
uptime >> /var/log/maintenance.log

# 3. Check application health
echo "Application Health:" >> /var/log/maintenance.log
curl -s http://localhost:8080/actuator/health | jq . >> /var/log/maintenance.log

# 4. Check database connections
echo "Database Connections:" >> /var/log/maintenance.log
psql -h localhost -U smartcampus_user -d smartcampus_prod -t -c "
SELECT count(*) as total_connections,
       count(*) filter (where state = 'active') as active_connections,
       count(*) filter (where state = 'idle') as idle_connections
FROM pg_stat_activity;" >> /var/log/maintenance.log

# 5. Rotate logs
find /opt/smartcampus/logs -name "*.log" -size +100M -exec gzip {} \;
find /opt/smartcampus/logs -name "*.gz" -mtime +30 -delete

# 6. Update system packages (if needed)
apt list --upgradable >> /var/log/maintenance.log

echo "=== Maintenance Complete ===" >> /var/log/maintenance.log
```

#### Monthly Maintenance Checklist

```bash
#!/bin/bash
# monthly-maintenance.sh

# 1. Database maintenance
echo "Running database maintenance..."
psql -h localhost -U smartcampus_user -d smartcampus_prod << EOF
VACUUM ANALYZE;
REINDEX DATABASE smartcampus_prod;
UPDATE pg_stat_statements_reset();
EOF

# 2. SSL certificate renewal check
certbot certificates

# 3. Security updates
apt update && apt list --upgradable | grep -i security

# 4. Backup verification
LATEST_BACKUP=$(ls -t /opt/backups/database/smartcampus_*.sql.gz | head -1)
echo "Testing backup restore capability..."
# Test restore to temporary database
# createdb smartcampus_test
# gunzip -c $LATEST_BACKUP | psql -d smartcampus_test
# dropdb smartcampus_test

# 5. Performance analysis
echo "Generating performance report..."
psql -h localhost -U smartcampus_user -d smartcampus_prod -c "
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_stat_user_tables.n_tup_ins as inserts,
    pg_stat_user_tables.n_tup_upd as updates,
    pg_stat_user_tables.n_tup_del as deletes
FROM pg_tables
LEFT JOIN pg_stat_user_tables ON pg_tables.tablename = pg_stat_user_tables.relname
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
" > /var/log/database-performance-$(date +%Y%m).log
```

### 2. Update Procedures

#### Application Update Process

```bash
#!/bin/bash
# update-application.sh

NEW_VERSION=$1
if [ -z "$NEW_VERSION" ]; then
    echo "Usage: $0 <version>"
    exit 1
fi

echo "Starting application update to version: $NEW_VERSION"

# 1. Create backup
./scripts/backup.sh

# 2. Download new version
docker pull ghcr.io/your-org/smartcampus:$NEW_VERSION

# 3. Run database migrations if needed
mvn flyway:migrate -Pflyway-prod

# 4. Deploy new version using blue-green deployment
./scripts/blue-green-deploy.sh $NEW_VERSION

# 5. Verify deployment
sleep 30
curl -f http://localhost:8080/actuator/health || {
    echo "Health check failed, rolling back..."
    ./scripts/rollback.sh
    exit 1
}

# 6. Run smoke tests
curl -f http://localhost:8080/api/health/detailed || {
    echo "Smoke tests failed, rolling back..."
    ./scripts/rollback.sh
    exit 1
}

echo "Application update completed successfully!"
```

## Conclusion

This deployment guide provides comprehensive procedures for deploying and maintaining the SmartCampus application across different environments. Key takeaways include:

1. **Environment-specific configurations** ensure optimal performance for each deployment target
2. **Automated deployment pipelines** reduce manual errors and improve deployment reliability
3. **Monitoring and observability** enable proactive issue detection and resolution
4. **Backup and disaster recovery** procedures protect against data loss
5. **Performance optimization** ensures the application scales with user demand
6. **Security hardening** protects against common vulnerabilities

Regular review and updates of these procedures ensure they remain current with evolving infrastructure and security requirements. Always test deployment procedures in non-production environments before applying them to production systems.
