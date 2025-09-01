#!/bin/bash

# Location: scripts/database/migrate.sh
# SmartCampus Database Migration Script
# This script handles database schema migrations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
MIGRATIONS_DIR="src/main/resources/sql/migrations"
MIGRATIONS_TABLE="schema_migrations"

# Functions
print_banner() {
    echo -e "${BLUE}"
    echo "===================================="
    echo "  SmartCampus Database Migrations"
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
    echo "Usage: $0 [OPTIONS] COMMAND"
    echo ""
    echo "Commands:"
    echo "  migrate       Apply pending migrations (default)"
    echo "  status        Show migration status"
    echo "  rollback      Rollback last migration"
    echo "  reset         Reset database (drop all tables)"
    echo "  create NAME   Create new migration file"
    echo "  validate      Validate all migrations"
    echo ""
    echo "Options:"
    echo "  --host HOST          Database host (default: localhost)"
    echo "  --port PORT          Database port (default: 5432)"
    echo "  --database DB        Database name (required)"
    echo "  --username USER      Database username (required)"
    echo "  --password PASS      Database password (prompt if not provided)"
    echo "  --ssl               Enable SSL connection"
    echo "  --dry-run           Show what would be done without executing"
    echo "  --force             Force migration even if validation fails"
    echo "  -v, --verbose       Enable verbose output"
    echo "  -h, --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 migrate --database smartcampus_dev --username app_user"
    echo "  $0 status --database smartcampus_prod --username app_user"
    echo "  $0 create add_student_gpa_index"
    echo "  $0 rollback --database smartcampus_test --username test_user"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if psql is available
    if ! command -v psql &> /dev/null; then
        log_error "PostgreSQL client (psql) is not installed"
        exit 1
    fi
    
    # Check if migrations directory exists
    if [ ! -d "$MIGRATIONS_DIR" ]; then
        log_info "Creating migrations directory: $MIGRATIONS_DIR"
        mkdir -p "$MIGRATIONS_DIR"
    fi
    
    log_success "Prerequisites check passed"
}

test_connection() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    
    log_info "Testing database connection..."
    
    export PGPASSWORD="$password"
    
    if psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option -c "SELECT version();" > /dev/null 2>&1; then
        log_success "Database connection successful"
        return 0
    else
        log_error "Cannot connect to database"
        return 1
    fi
}

ensure_migrations_table() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    
    export PGPASSWORD="$password"
    
    log_info "Ensuring migrations table exists..."
    
    psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option << EOF
CREATE TABLE IF NOT EXISTS $MIGRATIONS_TABLE (
    version VARCHAR(255) PRIMARY KEY,
    description VARCHAR(500),
    checksum VARCHAR(64),
    applied_by VARCHAR(100),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms INTEGER
);

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_migrations_applied_at ON $MIGRATIONS_TABLE(applied_at);
EOF
    
    log_success "Migrations table ready"
}

get_applied_migrations() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    
    export PGPASSWORD="$password"
    
    psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option -tAc "
        SELECT version FROM $MIGRATIONS_TABLE ORDER BY version;
    " 2>/dev/null || echo ""
}

get_pending_migrations() {
    local applied_migrations="$1"
    
    local pending_migrations=()
    
    for migration_file in $(ls "$MIGRATIONS_DIR"/V*.sql 2>/dev/null | sort); do
        local version=$(basename "$migration_file" | sed 's/V\(.*\)__.*/\1/')
        
        if ! echo "$applied_migrations" | grep -q "^$version$"; then
            pending_migrations+=("$migration_file")
        fi
    done
    
    printf '%s\n' "${pending_migrations[@]}"
}

calculate_checksum() {
    local file=$1
    
    if command -v sha256sum &> /dev/null; then
        sha256sum "$file" | cut -d' ' -f1
    elif command -v shasum &> /dev/null; then
        shasum -a 256 "$file" | cut -d' ' -f1
    else
        # Fallback to a simple checksum
        cksum "$file" | cut -d' ' -f1
    fi
}

apply_migration() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    local migration_file=$7
    local dry_run=$8
    
    export PGPASSWORD="$password"
    
    local version=$(basename "$migration_file" | sed 's/V\(.*\)__.*/\1/')
    local description=$(basename "$migration_file" | sed 's/V.*__\(.*\)\.sql/\1/' | tr '_' ' ')
    local checksum=$(calculate_checksum "$migration_file")
    
    log_info "Applying migration: V$version - $description"
    
    if [ "$dry_run" = true ]; then
        log_info "[DRY RUN] Would apply: $migration_file"
        return 0
    fi
    
    # Record start time
    local start_time=$(date +%s%3N)
    
    # Apply migration in a transaction
    if psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option -v ON_ERROR_STOP=1 << EOF
BEGIN;

-- Apply the migration
\i $migration_file

-- Record the migration
INSERT INTO $MIGRATIONS_TABLE (version, description, checksum, applied_by, applied_at, execution_time_ms)
VALUES ('$version', '$description', '$checksum', '$username', CURRENT_TIMESTAMP, $(( $(date +%s%3N) - start_time )));

COMMIT;
EOF
    then
        local end_time=$(date +%s%3N)
        local duration=$((end_time - start_time))
        log_success "Migration V$version completed in ${duration}ms"
        return 0
    else
        log_error "Migration V$version failed"
        return 1
    fi
}

migrate() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    local dry_run=$7
    
    log_info "Starting database migration..."
    
    # Get applied and pending migrations
    local applied_migrations=$(get_applied_migrations "$host" "$port" "$database" "$username" "$password" "$ssl_option")
    local pending_migrations=($(get_pending_migrations "$applied_migrations"))
    
    if [ ${#pending_migrations[@]} -eq 0 ]; then
        log_success "No pending migrations found"
        return 0
    fi
    
    log_info "Found ${#pending_migrations[@]} pending migration(s)"
    
    # Apply each pending migration
    for migration_file in "${pending_migrations[@]}"; do
        if ! apply_migration "$host" "$port" "$database" "$username" "$password" "$ssl_option" "$migration_file" "$dry_run"; then
            log_error "Migration failed, stopping"
            return 1
        fi
    done
    
    log_success "All migrations applied successfully"
}

show_migration_status() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    
    export PGPASSWORD="$password"
    
    log_info "Migration Status:"
    
    # Get applied migrations with details
    echo -e "\n${BLUE}Applied Migrations:${NC}"
    psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option << EOF
\x off
\pset border 2
\pset format aligned

SELECT 
    version,
    LEFT(description, 40) as description,
    applied_by,
    applied_at::timestamp(0),
    execution_time_ms as "time_ms"
FROM $MIGRATIONS_TABLE 
ORDER BY version;
EOF
    
    # Get pending migrations
    local applied_migrations=$(get_applied_migrations "$host" "$port" "$database" "$username" "$password" "$ssl_option")
    local pending_migrations=($(get_pending_migrations "$applied_migrations"))
    
    echo -e "\n${BLUE}Pending Migrations:${NC}"
    if [ ${#pending_migrations[@]} -eq 0 ]; then
        echo "  None"
    else
        for migration_file in "${pending_migrations[@]}"; do
            local version=$(basename "$migration_file" | sed 's/V\(.*\)__.*/\1/')
            local description=$(basename "$migration_file" | sed 's/V.*__\(.*\)\.sql/\1/' | tr '_' ' ')
            echo "  V$version - $description"
        done
    fi
    
    echo
}

rollback_migration() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    local dry_run=$7
    
    export PGPASSWORD="$password"
    
    log_warning "Rolling back last migration..."
    
    # Get the last applied migration
    local last_migration=$(psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option -tAc "
        SELECT version FROM $MIGRATIONS_TABLE ORDER BY applied_at DESC LIMIT 1;
    " 2>/dev/null)
    
    if [ -z "$last_migration" ]; then
        log_info "No migrations to rollback"
        return 0
    fi
    
    # Check if rollback file exists
    local rollback_file="$MIGRATIONS_DIR/R${last_migration}__rollback.sql"
    
    if [ ! -f "$rollback_file" ]; then
        log_error "Rollback file not found: $rollback_file"
        log_info "Manual rollback required for migration V$last_migration"
        return 1
    fi
    
    log_info "Rolling back migration V$last_migration"
    
    if [ "$dry_run" = true ]; then
        log_info "[DRY RUN] Would rollback using: $rollback_file"
        return 0
    fi
    
    # Apply rollback in a transaction
    if psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option -v ON_ERROR_STOP=1 << EOF
BEGIN;

-- Apply the rollback
\i $rollback_file

-- Remove migration record
DELETE FROM $MIGRATIONS_TABLE WHERE version = '$last_migration';

COMMIT;
EOF
    then
        log_success "Migration V$last_migration rolled back successfully"
    else
        log_error "Rollback failed"
        return 1
    fi
}

reset_database() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    local dry_run=$7
    
    export PGPASSWORD="$password"
    
    log_warning "Resetting database (dropping all tables)..."
    
    if [ "$dry_run" = true ]; then
        log_info "[DRY RUN] Would drop all tables and reset migrations"
        return 0
    fi
    
    # Confirm reset
    echo -n "Are you sure you want to reset the database? This will DROP ALL TABLES! (yes/no): "
    read -r confirmation
    
    if [ "$confirmation" != "yes" ]; then
        log_info "Reset cancelled"
        return 0
    fi
    
    # Drop all tables
    psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option << EOF
-- Disable foreign key checks temporarily
SET session_replication_role = replica;

-- Drop all tables
DO \$\$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) 
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';
    END LOOP;
END \$\$;

-- Drop all sequences
DO \$\$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = current_schema()) 
    LOOP
        EXECUTE 'DROP SEQUENCE IF EXISTS ' || quote_ident(r.sequence_name) || ' CASCADE';
    END LOOP;
END \$\$;

-- Re-enable foreign key checks
SET session_replication_role = DEFAULT;
EOF
    
    log_success "Database reset completed"
}

create_migration() {
    local migration_name=$1
    
    if [ -z "$migration_name" ]; then
        log_error "Migration name is required"
        return 1
    fi
    
    # Generate version number (timestamp)
    local version=$(date +%Y%m%d%H%M%S)
    local migration_file="$MIGRATIONS_DIR/V${version}__${migration_name}.sql"
    local rollback_file="$MIGRATIONS_DIR/R${version}__rollback.sql"
    
    # Create migration file
    cat > "$migration_file" << EOF
-- Migration: V${version}__${migration_name}
-- Description: ${migration_name//_/ }
-- Author: $(whoami)
-- Date: $(date)

-- Add your migration SQL here
-- Example:
-- CREATE TABLE new_table (
--     id SERIAL PRIMARY KEY,
--     name VARCHAR(100) NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- CREATE INDEX idx_new_table_name ON new_table(name);
EOF
    
    # Create rollback file
    cat > "$rollback_file" << EOF
-- Rollback: R${version}__rollback
-- Description: Rollback for ${migration_name//_/ }
-- Author: $(whoami)
-- Date: $(date)

-- Add your rollback SQL here
-- This should undo the changes made in V${version}__${migration_name}.sql
-- Example:
-- DROP INDEX IF EXISTS idx_new_table_name;
-- DROP TABLE IF EXISTS new_table;
EOF
    
    log_success "Migration files created:"
    log_info "  Migration: $migration_file"
    log_info "  Rollback:  $rollback_file"
    
    # Open files in editor if available
    if command -v "$EDITOR" &> /dev/null; then
        log_info "Opening migration file in $EDITOR"
        "$EDITOR" "$migration_file"
    elif command -v nano &> /dev/null; then
        log_info "Opening migration file in nano"
        nano "$migration_file"
    fi
}

validate_migrations() {
    local host=$1
    local port=$2
    local database=$3
    local username=$4
    local password=$5
    local ssl_option=$6
    
    export PGPASSWORD="$password"
    
    log_info "Validating migrations..."
    
    local validation_errors=0
    
    # Check migration file naming convention
    for migration_file in "$MIGRATIONS_DIR"/V*.sql; do
        if [ ! -f "$migration_file" ]; then
            continue
        fi
        
        local filename=$(basename "$migration_file")
        
        # Check naming convention
        if ! [[ "$filename" =~ ^V[0-9]+__.*\.sql$ ]]; then
            log_error "Invalid migration filename: $filename"
            log_info "Expected format: V{version}__{description}.sql"
            ((validation_errors++))
        fi
    done
    
    # Check for checksum mismatches in applied migrations
    local applied_migrations=$(get_applied_migrations "$host" "$port" "$database" "$username" "$password" "$ssl_option")
    
    for version in $applied_migrations; do
        local migration_file=$(ls "$MIGRATIONS_DIR"/V${version}__*.sql 2>/dev/null | head -1)
        
        if [ -f "$migration_file" ]; then
            local current_checksum=$(calculate_checksum "$migration_file")
            local stored_checksum=$(psql -h "$host" -p "$port" -d "$database" -U "$username" $ssl_option -tAc "
                SELECT checksum FROM $MIGRATIONS_TABLE WHERE version = '$version';
            " 2>/dev/null)
            
            if [ "$current_checksum" != "$stored_checksum" ]; then
                log_error "Checksum mismatch for migration V$version"
                log_info "File may have been modified after application"
                ((validation_errors++))
            fi
        else
            log_warning "Migration file not found for applied version V$version"
        fi
    done
    
    if [ $validation_errors -eq 0 ]; then
        log_success "All migrations are valid"
        return 0
    else
        log_error "Found $validation_errors validation error(s)"
        return 1
    fi
}

# Main execution logic
main() {
    local command="migrate"
    local host="localhost"
    local port="5432"
    local database=""
    local username=""
    local password=""
    local ssl_option=""
    local dry_run=false
    local force=false
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
            --database)
                database="$2"
                shift 2
                ;;
            --username)
                username="$2"
                shift 2
                ;;
            --password)
                password="$2"
                shift 2
                ;;
            --ssl)
                ssl_option="--set=sslmode=require"
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --force)
                force=true
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
            migrate|status|rollback|reset|validate)
                command="$1"
                shift
                ;;
            create)
                command="create"
                shift
                if [ $# -gt 0 ]; then
                    migration_name="$1"
                    shift
                fi
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    if [ "$verbose" = true ]; then
        set -x
    fi
    
    print_banner
    check_prerequisites
    
    # Handle create command separately (doesn't need database connection)
    if [ "$command" = "create" ]; then
        create_migration "$migration_name"
        exit 0
    fi
    
    # Validate required parameters
    if [ -z "$database" ]; then
        log_error "Database name is required"
        show_help
        exit 1
    fi
    
    if [ -z "$username" ]; then
        log_error "Username is required"
        show_help
        exit 1
    fi
    
    # Prompt for password if not provided
    if [ -z "$password" ]; then
        echo -n "Enter password for $username: "
        read -s password
        echo
        
        if [ -z "$password" ]; then
            log_error "Password is required"
            exit 1
        fi
    fi
    
    # Test database connection
    if ! test_connection "$host" "$port" "$database" "$username" "$password" "$ssl_option"; then
        exit 1
    fi
    
    # Ensure migrations table exists
    ensure_migrations_table "$host" "$port" "$database" "$username" "$password" "$ssl_option"
    
    # Validate migrations unless forced
    if [ "$force" != true ] && [ "$command" != "reset" ]; then
        if ! validate_migrations "$host" "$port" "$database" "$username" "$password" "$ssl_option"; then
            log_error "Validation failed. Use --force to proceed anyway."
            exit 1
        fi
    fi
    
    # Execute command
    case $command in
        migrate)
            migrate "$host" "$port" "$database" "$username" "$password" "$ssl_option" "$dry_run"
            ;;
        status)
            show_migration_status "$host" "$port" "$database" "$username" "$password" "$ssl_option"
            ;;
        rollback)
            rollback_migration "$host" "$port" "$database" "$username" "$password" "$ssl_option" "$dry_run"
            ;;
        reset)
            reset_database "$host" "$port" "$database" "$username" "$password" "$ssl_option" "$dry_run"
            ;;
        validate)
            # Already validated above
            log_success "Migration validation completed"
            ;;
        *)
            log_error "Unknown command: $command"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"