#!/bin/bash

# Location: scripts/database/setup-db.sh
# SmartCampus Database Setup Script
# This script sets up the database for different environments

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Default configuration
DEFAULT_DB_HOST="localhost"
DEFAULT_DB_PORT="5432"
DEFAULT_DB_NAME="smartcampus"
DEFAULT_DB_USER="smartcampus_user"
DEFAULT_ADMIN_USER="postgres"

# Functions
print_banner() {
    echo -e "${BLUE}"
    echo "===================================="
    echo "  SmartCampus Database Setup"
    echo "===================================="
    echo -e "${NC}"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    echo "Usage: $0 [OPTIONS] ENVIRONMENT"
    echo ""
    echo "Environments:"
    echo "  dev          Development database"
    echo "  test         Test database"
    echo "  staging      Staging database"
    echo "  prod         Production database"
    echo ""
    echo "Options:"
    echo "  --host HOST          Database host (default: $DEFAULT_DB_HOST)"
    echo "  --port PORT          Database port (default: $DEFAULT_DB_PORT)"
    echo "  --admin-user USER    Admin user (default: $DEFAULT_ADMIN_USER)"
    echo "  --admin-pass PASS    Admin password (prompt if not provided)"
    echo "  --db-name NAME       Database name (default: smartcampus_ENV)"
    echo "  --db-user USER       Application user (default: smartcampus_user_ENV)"
    echo "  --db-pass PASS       Application password (generated if not provided)"
    echo "  --drop-existing      Drop existing database if exists"
    echo "  --skip-sample-data   Skip loading sample data"
    echo "  --ssl                Enable SSL connection"
    echo "  -v, --verbose        Enable verbose output"
    echo "  -h, --help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 dev"
    echo "  $0 prod --host db.example.com --ssl"
    echo "  $0 test --drop-existing --skip-sample-data"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if psql is available
    if ! command -v psql &> /dev/null; then
        log_error "PostgreSQL client (psql) is not installed"
        log_info "Install it with:"
        log_info "  Ubuntu/Debian: sudo apt-get install postgresql-client"
        log_info "  CentOS/RHEL: sudo yum install postgresql"
        log_info "  macOS: brew install postgresql"
        exit 1
    fi
    
    # Check if openssl is available for password generation
    if ! command -v openssl &> /dev/null; then
        log_warning "OpenSSL not found - password generation may not work"
    fi
    
    log_success "Prerequisites check passed"
}

generate_password() {
    if command -v openssl &> /dev/null; then
        openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
    else
        # Fallback method
        date +%s | sha256sum | base64 | head -c 25
    fi
}

test_connection() {
    local host=$1
    local port=$2
    local admin_user=$3
    local admin_pass=$4
    local ssl_option=$5
    
    log_info "Testing database connection to $host:$port..."
    
    export PGPASSWORD="$admin_pass"
    
    if psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "SELECT version();" > /dev/null 2>&1; then
        log_success "Database connection successful"
        return 0
    else
        log_error "Cannot connect to database server"
        log_info "Please check:"
        log_info "  - Database server is running"
        log_info "  - Host and port are correct"
        log_info "  - Admin credentials are valid"
        log_info "  - Firewall settings allow connection"
        return 1
    fi
}

create_database() {
    local host=$1
    local port=$2
    local admin_user=$3
    local admin_pass=$4
    local db_name=$5
    local ssl_option=$6
    local drop_existing=$7
    
    export PGPASSWORD="$admin_pass"
    
    # Check if database exists
    local db_exists=$(psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -tAc "SELECT 1 FROM pg_database WHERE datname='$db_name';" 2>/dev/null || echo "0")
    
    if [ "$db_exists" = "1" ]; then
        if [ "$drop_existing" = true ]; then
            log_warning "Dropping existing database: $db_name"
            
            # Terminate existing connections
            psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "
                SELECT pg_terminate_backend(pg_stat_activity.pid)
                FROM pg_stat_activity
                WHERE pg_stat_activity.datname = '$db_name'
                  AND pid <> pg_backend_pid();
            " > /dev/null 2>&1
            
            # Drop database
            psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "DROP DATABASE IF EXISTS $db_name;"
            
            log_success "Database dropped: $db_name"
        else
            log_warning "Database already exists: $db_name"
            return 0
        fi
    fi
    
    # Create database
    log_info "Creating database: $db_name"
    psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "
        CREATE DATABASE $db_name
        WITH ENCODING='UTF8'
        LC_COLLATE='en_US.UTF-8'
        LC_CTYPE='en_US.UTF-8'
        TEMPLATE=template0;
    "
    
    log_success "Database created: $db_name"
}

create_user() {
    local host=$1
    local port=$2
    local admin_user=$3
    local admin_pass=$4
    local db_name=$5
    local db_user=$6
    local db_pass=$7
    local ssl_option=$8
    
    export PGPASSWORD="$admin_pass"
    
    # Check if user exists
    local user_exists=$(psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -tAc "SELECT 1 FROM pg_user WHERE usename='$db_user';" 2>/dev/null || echo "0")
    
    if [ "$user_exists" = "1" ]; then
        log_info "Updating existing user: $db_user"
        psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "
            ALTER USER $db_user WITH PASSWORD '$db_pass';
        "
    else
        log_info "Creating database user: $db_user"
        psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "
            CREATE USER $db_user WITH PASSWORD '$db_pass';
        "
    fi
    
    # Grant permissions
    log_info "Granting permissions to user: $db_user"
    psql -h "$host" -p "$port" -U "$admin_user" -d postgres $ssl_option -c "
        GRANT ALL PRIVILEGES ON DATABASE $db_name TO $db_user;
        GRANT ALL ON SCHEMA public TO $db_user;
    "
    
    # Connect to the database and grant additional permissions
    export PGPASSWORD="$admin_pass"
    psql -h "$host" -p "$port" -U "$admin_user" -d "$db_name" $ssl_option -c "
        GRANT ALL ON ALL TABLES IN SCHEMA public TO $db_user;
        GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO $db_user;
        GRANT ALL ON ALL FUNCTIONS IN SCHEMA public TO $db_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $db_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $db_user;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO $db_user;
    "
    
    log_success "User configured: $db_user"
}

create_schema() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_pass=$4
    local db_name=$5
    local ssl_option=$6
    
    export PGPASSWORD="$db_pass"
    
    log_info "Creating database schema..."
    
    # Check if schema.sql exists
    local schema_file="src/main/resources/sql/schema.sql"
    if [ ! -f "$schema_file" ]; then
        log_warning "Schema file not found: $schema_file"
        log_info "Creating basic schema..."
        create_basic_schema "$host" "$port" "$db_user" "$db_name" "$ssl_option"
    else
        log_info "Executing schema file: $schema_file"
        psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -f "$schema_file"
    fi
    
    log_success "Database schema created"
}

create_basic_schema() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_name=$4
    local ssl_option=$5
    
    export PGPASSWORD="$DB_PASS"
    
    # Create basic SmartCampus schema
    psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option << 'EOF'
-- Create SmartCampus Tables

-- Users table (base for students, professors, admins)
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    user_role VARCHAR(20) NOT NULL CHECK (user_role IN ('STUDENT', 'PROFESSOR', 'ADMIN', 'STAFF')),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students table
CREATE TABLE students (
    id VARCHAR(50) PRIMARY KEY REFERENCES users(id),
    student_number VARCHAR(20) UNIQUE,
    major VARCHAR(100),
    enrollment_year INTEGER,
    gpa DECIMAL(3,2) DEFAULT 0.00 CHECK (gpa >= 0.00 AND gpa <= 4.00),
    credits_completed INTEGER DEFAULT 0,
    graduation_date DATE
);

-- Professors table
CREATE TABLE professors (
    id VARCHAR(50) PRIMARY KEY REFERENCES users(id),
    employee_id VARCHAR(20) UNIQUE,
    department_id VARCHAR(50),
    title VARCHAR(100),
    salary DECIMAL(12,2),
    hire_date DATE,
    office_location VARCHAR(100)
);

-- Departments table
CREATE TABLE departments (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    head_of_department VARCHAR(50),
    location VARCHAR(100),
    phone_number VARCHAR(20),
    email VARCHAR(255),
    budget DECIMAL(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Courses table
CREATE TABLE courses (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    credits INTEGER NOT NULL CHECK (credits > 0),
    department_id VARCHAR(50) REFERENCES departments(id),
    professor_id VARCHAR(50) REFERENCES professors(id),
    max_enrollment INTEGER DEFAULT 30,
    current_enrollment INTEGER DEFAULT 0,
    semester VARCHAR(20) CHECK (semester IN ('SPRING', 'SUMMER', 'FALL', 'WINTER')),
    year INTEGER,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELLED')),
    schedule JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Course Prerequisites table
CREATE TABLE course_prerequisites (
    course_id VARCHAR(50) REFERENCES courses(id),
    prerequisite_course_id VARCHAR(50) REFERENCES courses(id),
    PRIMARY KEY (course_id, prerequisite_course_id)
);

-- Enrollments table
CREATE TABLE enrollments (
    id VARCHAR(50) PRIMARY KEY,
    student_id VARCHAR(50) REFERENCES students(id),
    course_id VARCHAR(50) REFERENCES courses(id),
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED', 'COMPLETED', 'WITHDRAWN', 'FAILED', 'PENDING', 'WAITLISTED')),
    final_grade DECIMAL(5,2),
    completion_date TIMESTAMP,
    UNIQUE(student_id, course_id)
);

-- Grades table
CREATE TABLE grades (
    id VARCHAR(50) PRIMARY KEY,
    student_id VARCHAR(50) REFERENCES students(id),
    course_id VARCHAR(50) REFERENCES courses(id),
    assignment_name VARCHAR(200),
    points DECIMAL(5,2) NOT NULL,
    max_points DECIMAL(5,2) NOT NULL,
    grade_type VARCHAR(50) CHECK (grade_type IN ('ASSIGNMENT', 'EXAM', 'QUIZ', 'PROJECT', 'PARTICIPATION', 'FINAL')),
    submission_date TIMESTAMP,
    graded_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(user_role);
CREATE INDEX idx_students_major ON students(major);
CREATE INDEX idx_students_year ON students(enrollment_year);
CREATE INDEX idx_courses_department ON courses(department_id);
CREATE INDEX idx_courses_professor ON courses(professor_id);
CREATE INDEX idx_courses_semester ON courses(semester, year);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE INDEX idx_grades_student_course ON grades(student_id, course_id);
CREATE INDEX idx_grades_type ON grades(grade_type);

-- Create triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to automatically update current_enrollment
CREATE OR REPLACE FUNCTION update_course_enrollment()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status = 'ENROLLED' THEN
            UPDATE courses SET current_enrollment = current_enrollment + 1 
            WHERE id = NEW.course_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.status != NEW.status THEN
            IF OLD.status = 'ENROLLED' AND NEW.status != 'ENROLLED' THEN
                UPDATE courses SET current_enrollment = current_enrollment - 1 
                WHERE id = NEW.course_id;
            ELSIF OLD.status != 'ENROLLED' AND NEW.status = 'ENROLLED' THEN
                UPDATE courses SET current_enrollment = current_enrollment + 1 
                WHERE id = NEW.course_id;
            END IF;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.status = 'ENROLLED' THEN
            UPDATE courses SET current_enrollment = current_enrollment - 1 
            WHERE id = OLD.course_id;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enrollment_count_trigger
    AFTER INSERT OR UPDATE OR DELETE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION update_course_enrollment();

-- Add foreign key constraints that were missed
ALTER TABLE professors ADD CONSTRAINT fk_professors_department 
    FOREIGN KEY (department_id) REFERENCES departments(id);

ALTER TABLE departments ADD CONSTRAINT fk_departments_head 
    FOREIGN KEY (head_of_department) REFERENCES professors(id);

-- Insert default data
INSERT INTO departments (id, name, description, location, phone_number, email) VALUES 
    ('DEPT001', 'Computer Science', 'Computer Science and Information Technology', 'Building A, Floor 3', '555-0101', 'cs@smartcampus.edu'),
    ('DEPT002', 'Mathematics', 'Mathematics and Statistics Department', 'Building B, Floor 2', '555-0102', 'math@smartcampus.edu'),
    ('DEPT003', 'Business Administration', 'School of Business', 'Building C, Floor 1', '555-0103', 'business@smartcampus.edu'),
    ('DEPT004', 'Engineering', 'Engineering Department', 'Building D, Floor 4', '555-0104', 'engineering@smartcampus.edu');

-- Grant sequences permission
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO CURRENT_USER;

EOF

    log_success "Basic schema created"
}

load_sample_data() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_pass=$4
    local db_name=$5
    local ssl_option=$6
    local environment=$7
    
    export PGPASSWORD="$db_pass"
    
    log_info "Loading sample data for $environment environment..."
    
    # Check if sample data files exist
    local data_dir="src/main/resources/data"
    if [ ! -d "$data_dir" ]; then
        log_warning "Sample data directory not found: $data_dir"
        log_info "Creating minimal sample data..."
        create_minimal_sample_data "$host" "$port" "$db_user" "$db_name" "$ssl_option" "$environment"
        return
    fi
    
    # Load data files in order
    local files=("departments.csv" "professors.csv" "students.csv" "courses.csv" "enrollments.csv" "grades.csv")
    
    for file in "${files[@]}"; do
        local file_path="$data_dir/$file"
        if [ -f "$file_path" ]; then
            log_info "Loading data from: $file"
            load_csv_data "$host" "$port" "$db_user" "$db_name" "$ssl_option" "$file_path"
        else
            log_warning "Sample data file not found: $file_path"
        fi
    done
    
    log_success "Sample data loaded"
}

load_csv_data() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_name=$4
    local ssl_option=$5
    local csv_file=$6
    
    export PGPASSWORD="$DB_PASS"
    
    # Determine table name from filename
    local table_name=$(basename "$csv_file" .csv)
    
    # Copy CSV data to database
    psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -c "
        \copy $table_name FROM '$csv_file' WITH CSV HEADER DELIMITER ','
    "
}

create_minimal_sample_data() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_name=$4
    local ssl_option=$5
    local environment=$6
    
    export PGPASSWORD="$DB_PASS"
    
    # Create minimal sample data for testing
    psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option << EOF
-- Insert sample users
INSERT INTO users (id, first_name, last_name, email, user_role) VALUES
    ('ADMIN001', 'System', 'Administrator', 'admin@smartcampus.edu', 'ADMIN'),
    ('PROF001', 'John', 'Smith', 'john.smith@smartcampus.edu', 'PROFESSOR'),
    ('PROF002', 'Jane', 'Davis', 'jane.davis@smartcampus.edu', 'PROFESSOR'),
    ('STU001', 'Alice', 'Johnson', 'alice.johnson@student.smartcampus.edu', 'STUDENT'),
    ('STU002', 'Bob', 'Wilson', 'bob.wilson@student.smartcampus.edu', 'STUDENT'),
    ('STU003', 'Carol', 'Brown', 'carol.brown@student.smartcampus.edu', 'STUDENT')
ON CONFLICT (id) DO NOTHING;

-- Insert sample professors
INSERT INTO professors (id, employee_id, department_id, title, hire_date) VALUES
    ('PROF001', 'EMP001', 'DEPT001', 'Associate Professor', '2020-08-15'),
    ('PROF002', 'EMP002', 'DEPT002', 'Assistant Professor', '2021-01-10')
ON CONFLICT (id) DO NOTHING;

-- Insert sample students
INSERT INTO students (id, student_number, major, enrollment_year, gpa) VALUES
    ('STU001', 'S2024001', 'Computer Science', 2024, 3.75),
    ('STU002', 'S2024002', 'Computer Science', 2024, 3.25),
    ('STU003', 'S2023001', 'Mathematics', 2023, 3.90)
ON CONFLICT (id) DO NOTHING;

-- Insert sample courses
INSERT INTO courses (id, course_id, title, credits, department_id, professor_id, max_enrollment, semester, year) VALUES
    ('COURSE001', 'CS101', 'Introduction to Programming', 3, 'DEPT001', 'PROF001', 30, 'FALL', 2024),
    ('COURSE002', 'CS201', 'Data Structures', 4, 'DEPT001', 'PROF001', 25, 'SPRING', 2025),
    ('COURSE003', 'MATH101', 'Calculus I', 4, 'DEPT002', 'PROF002', 40, 'FALL', 2024)
ON CONFLICT (id) DO NOTHING;

-- Insert sample enrollments (only for non-production environments)
EOF

    if [ "$environment" != "prod" ]; then
        psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option << EOF
INSERT INTO enrollments (id, student_id, course_id, status) VALUES
    ('ENR001', 'STU001', 'COURSE001', 'ENROLLED'),
    ('ENR002', 'STU002', 'COURSE001', 'ENROLLED'),
    ('ENR003', 'STU003', 'COURSE003', 'ENROLLED'),
    ('ENR004', 'STU001', 'COURSE003', 'COMPLETED')
ON CONFLICT (student_id, course_id) DO NOTHING;

-- Insert sample grades
INSERT INTO grades (id, student_id, course_id, assignment_name, points, max_points, grade_type) VALUES
    ('GRD001', 'STU001', 'COURSE001', 'Assignment 1', 85.0, 100.0, 'ASSIGNMENT'),
    ('GRD002', 'STU002', 'COURSE001', 'Assignment 1', 78.0, 100.0, 'ASSIGNMENT'),
    ('GRD003', 'STU003', 'COURSE003', 'Midterm Exam', 92.0, 100.0, 'EXAM'),
    ('GRD004', 'STU001', 'COURSE003', 'Final Exam', 88.0, 100.0, 'FINAL')
ON CONFLICT (id) DO NOTHING;
EOF
    fi
    
    log_success "Minimal sample data created"
}

run_migrations() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_pass=$4
    local db_name=$5
    local ssl_option=$6
    
    export PGPASSWORD="$db_pass"
    
    log_info "Running database migrations..."
    
    local migrations_dir="src/main/resources/sql/migrations"
    if [ ! -d "$migrations_dir" ]; then
        log_info "No migrations directory found - skipping"
        return 0
    fi
    
    # Create migrations table if it doesn't exist
    psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option << EOF
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(255) PRIMARY KEY,
    description VARCHAR(500),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
EOF
    
    # Run migration files in order
    for migration_file in $(ls "$migrations_dir"/V*.sql 2>/dev/null | sort); do
        local version=$(basename "$migration_file" | sed 's/V\(.*\)__.*/\1/')
        local description=$(basename "$migration_file" | sed 's/V.*__\(.*\)\.sql/\1/')
        
        # Check if migration already applied
        local applied=$(psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -tAc "SELECT 1 FROM schema_migrations WHERE version='$version';" 2>/dev/null || echo "0")
        
        if [ "$applied" = "1" ]; then
            log_info "Migration $version already applied - skipping"
            continue
        fi
        
        log_info "Applying migration: $version - $description"
        
        # Run migration
        if psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -f "$migration_file"; then
            # Record successful migration
            psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -c "
                INSERT INTO schema_migrations (version, description) 
                VALUES ('$version', '$description');
            "
            log_success "Migration $version completed"
        else
            log_error "Migration $version failed"
            exit 1
        fi
    done
    
    log_success "All migrations completed"
}

create_connection_config() {
    local environment=$1
    local host=$2
    local port=$3
    local db_name=$4
    local db_user=$5
    local db_pass=$6
    local ssl_option=$7
    
    local config_file="src/main/resources/config/database-$environment.properties"
    local config_dir=$(dirname "$config_file")
    
    # Create config directory if it doesn't exist
    mkdir -p "$config_dir"
    
    log_info "Creating database configuration: $config_file"
    
    # Determine JDBC URL
    local jdbc_url="jdbc:postgresql://$host:$port/$db_name"
    if [ "$ssl_option" = "--set=sslmode=require" ]; then
        jdbc_url="${jdbc_url}?sslmode=require"
    fi
    
    cat > "$config_file" << EOF
# SmartCampus Database Configuration - $environment
# Generated: $(date)

# Database connection
database.url=$jdbc_url
database.username=$db_user
database.password=$db_pass
database.driver=org.postgresql.Driver

# Connection pool settings
database.hikari.maximum-pool-size=20
database.hikari.minimum-idle=5
database.hikari.connection-timeout=30000
database.hikari.idle-timeout=600000
database.hikari.max-lifetime=1800000
database.hikari.leak-detection-threshold=60000

# JPA/Hibernate settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# Database migration
spring.flyway.enabled=true
spring.flyway.locations=classpath:sql/migrations
spring.flyway.baseline-on-migrate=true
EOF
    
    log_success "Configuration file created: $config_file"
    
    # Create environment-specific Spring profile
    local spring_config_file="src/main/resources/application-$environment.properties"
    
    if [ ! -f "$spring_config_file" ]; then
        log_info "Creating Spring profile configuration: $spring_config_file"
        
        cat > "$spring_config_file" << EOF
# SmartCampus Spring Configuration - $environment
# Generated: $(date)

# Include database configuration
spring.config.import=optional:classpath:config/database-$environment.properties

# Environment-specific settings
spring.profiles.active=$environment
logging.level.com.smartcampus=INFO

# Security settings
app.security.jwt.secret=change-this-in-production-$(openssl rand -hex 16)
app.security.jwt.expiration=3600

# Application settings
app.name=SmartCampus
app.version=1.0.0
app.environment=$environment
EOF
        
        log_success "Spring configuration created: $spring_config_file"
    fi
}

verify_setup() {
    local host=$1
    local port=$2
    local db_user=$3
    local db_pass=$4
    local db_name=$5
    local ssl_option=$6
    
    export PGPASSWORD="$db_pass"
    
    log_info "Verifying database setup..."
    
    # Test connection with application user
    if ! psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -c "SELECT current_user, current_database();" > /dev/null 2>&1; then
        log_error "Cannot connect with application user"
        return 1
    fi
    
    # Check if tables exist
    local table_count=$(psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';" 2>/dev/null || echo "0")
    
    if [ "$table_count" -gt 0 ]; then
        log_success "Database tables found: $table_count"
    else
        log_warning "No tables found - schema may not be created"
    fi
    
    # Check sample data (if not production)
    if [ "$ENVIRONMENT" != "prod" ]; then
        local user_count=$(psql -h "$host" -p "$port" -U "$db_user" -d "$db_name" $ssl_option -tAc "SELECT COUNT(*) FROM users;" 2>/dev/null || echo "0")
        log_info "Sample users found: $user_count"
    fi
    
    log_success "Database setup verification completed"
}

show_summary() {
    local environment=$1
    local host=$2
    local port=$3
    local db_name=$4
    local db_user=$5
    local db_pass=$6
    
    echo
    log_success "Database setup completed successfully!"
    echo
    echo -e "${BLUE}Database Information:${NC}"
    echo "  Environment: $environment"
    echo "  Host: $host"
    echo "  Port: $port"
    echo "  Database: $db_name"
    echo "  Username: $db_user"
    echo "  Password: $db_pass"
    echo
    echo -e "${BLUE}Connection String:${NC}"
    echo "  jdbc:postgresql://$host:$port/$db_name"
    echo
    echo -e "${BLUE}Test Connection:${NC}"
    echo "  psql -h $host -p $port -U $db_user -d $db_name"
    echo
    echo -e "${BLUE}Configuration Files:${NC}"
    echo "  src/main/resources/config/database-$environment.properties"
    echo "  src/main/resources/application-$environment.properties"
    echo
    
    if [ "$environment" != "prod" ]; then
        echo -e "${BLUE}Sample Users:${NC}"
        echo "  Admin: admin@smartcampus.edu"
        echo "  Professor: john.smith@smartcampus.edu"
        echo "  Student: alice.johnson@student.smartcampus.edu"
    fi
    echo
}

# Main execution logic
main() {
    local environment=""
    local host="$DEFAULT_DB_HOST"
    local port="$DEFAULT_DB_PORT"
    local admin_user="$DEFAULT_ADMIN_USER"
    local admin_pass=""
    local db_name=""
    local db_user=""
    local db_pass=""
    local drop_existing=false
    local skip_sample_data=false
    local ssl_option=""
    local verbose=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --host)
                host="$2"
                shift 2
                ;;
            --port)
                port="$2"
                shift 2
                ;;
            --admin-user)
                admin_user="$2"
                shift 2
                ;;
            --admin-pass)
                admin_pass="$2"
                shift 2
                ;;
            --db-name)
                db_name="$2"
                shift 2
                ;;
            --db-user)
                db_user="$2"
                shift 2
                ;;
            --db-pass)
                db_pass="$2"
                shift 2
                ;;
            --drop-existing)
                drop_existing=true
                shift
                ;;
            --skip-sample-data)
                skip_sample_data=true
                shift
                ;;
            --ssl)
                ssl_option="--set=sslmode=require"
                shift
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            dev|test|staging|prod)
                environment="$1"
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Validate required parameters
    if [ -z "$environment" ]; then
        log_error "Environment is required"
        show_help
        exit 1
    fi
    
    # Set defaults based on environment
    if [ -z "$db_name" ]; then
        db_name="${DEFAULT_DB_NAME}_${environment}"
    fi
    
    if [ -z "$db_user" ]; then
        db_user="${DEFAULT_DB_USER}_${environment}"
    fi
    
    if [ -z "$db_pass" ]; then
        db_pass=$(generate_password)
    fi
    
    # Prompt for admin password if not provided
    if [ -z "$admin_pass" ]; then
        echo -n "Enter admin password for $admin_user: "
        read -s admin_pass
        echo
        
        if [ -z "$admin_pass" ]; then
            log_error "Admin password is required"
            exit 1
        fi
    fi
    
    if [ "$verbose" = true ]; then
        set -x
    fi
    
    # Export environment for use in functions
    export ENVIRONMENT="$environment"
    export DB_PASS="$db_pass"
    
    print_banner
    check_prerequisites
    
    # Test database connection
    if ! test_connection "$host" "$port" "$admin_user" "$admin_pass" "$ssl_option"; then
        exit 1
    fi
    
    # Create database and user
    create_database "$host" "$port" "$admin_user" "$admin_pass" "$db_name" "$ssl_option" "$drop_existing"
    create_user "$host" "$port" "$admin_user" "$admin_pass" "$db_name" "$db_user" "$db_pass" "$ssl_option"
    
    # Create schema and run migrations
    create_schema "$host" "$port" "$db_user" "$db_pass" "$db_name" "$ssl_option"
    run_migrations "$host" "$port" "$db_user" "$db_pass" "$db_name" "$ssl_option"
    
    # Load sample data (except for production)
    if [ "$skip_sample_data" != true ] && [ "$environment" != "prod" ]; then
        load_sample_data "$host" "$port" "$db_user" "$db_pass" "$db_name" "$ssl_option" "$environment"
    fi
    
    # Create configuration files
    create_connection_config "$environment" "$host" "$port" "$db_name" "$db_user" "$db_pass" "$ssl_option"
    
    # Verify setup
    verify_setup "$host" "$port" "$db_user" "$db_pass" "$db_name" "$ssl_option"
    
    # Show summary
    show_summary "$environment" "$host" "$port" "$db_name" "$db_user" "$db_pass"
}

# Run main function with all arguments
main "$@"