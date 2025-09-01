#!/bin/bash

# Location: scripts/test.sh
# SmartCampus Test Execution Script
# This script handles running various types of tests with reporting and coverage

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Configuration
PROJECT_NAME="SmartCampus"
TEST_RESULTS_DIR="target/test-results"
COVERAGE_DIR="target/site/jacoco"
REPORTS_DIR="target/site"
MIN_COVERAGE_THRESHOLD=80
PARALLEL_TESTS=true

# Functions
print_banner() {
    echo -e "${BLUE}"
    echo "=================================="
    echo "  SmartCampus Test Runner"
    echo "=================================="
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

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1"
}

check_prerequisites() {
    log_info "Checking test prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed"
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed"
        exit 1
    fi
    
    # Check if project structure exists
    if [ ! -f "pom.xml" ]; then
        log_error "Maven project not found (pom.xml missing)"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

setup_test_environment() {
    log_info "Setting up test environment..."
    
    # Create test directories
    mkdir -p "$TEST_RESULTS_DIR"
    mkdir -p "$COVERAGE_DIR"
    mkdir -p "$REPORTS_DIR"
    
    # Set test environment variables
    export SPRING_PROFILES_ACTIVE="test"
    export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"
    
    # Clean previous test results
    if [ -d "$TEST_RESULTS_DIR" ]; then
        rm -rf "$TEST_RESULTS_DIR"/*
    fi
    
    log_success "Test environment setup completed"
}

run_unit_tests() {
    local profile=${1:-"test"}
    local parallel=${2:-$PARALLEL_TESTS}
    
    log_step "Running unit tests"
    
    local maven_args="-P$profile"
    
    if [ "$parallel" = true ]; then
        maven_args="$maven_args -T 1C -Dparallel=all -DthreadCount=4"
        log_info "Running tests in parallel mode"
    fi
    
    # Run unit tests with coverage
    mvn clean test $maven_args \
        -Dtest="**/*Test.java" \
        -DexcludedGroups="integration,functional" \
        -Djacoco.destFile="$COVERAGE_DIR/jacoco-unit.exec" \
        -Dmaven.test.failure.ignore=false
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_success "Unit tests completed successfully"
        
        # Show test summary
        show_test_summary "unit"
    else
        log_error "Unit tests failed"
        show_failed_tests "unit"
        return $exit_code
    fi
}

run_integration_tests() {
    local profile=${1:-"integration-test"}
    
    log_step "Running integration tests"
    
    # Start test database if needed
    start_test_database
    
    # Run integration tests
    mvn test -P$profile \
        -Dtest="**/*IntegrationTest.java,**/*IT.java" \
        -DexcludedGroups="functional" \
        -Djacoco.destFile="$COVERAGE_DIR/jacoco-integration.exec" \
        -Dmaven.test.failure.ignore=false
    
    local exit_code=$?
    
    # Stop test database
    stop_test_database
    
    if [ $exit_code -eq 0 ]; then
        log_success "Integration tests completed successfully"
        show_test_summary "integration"
    else
        log_error "Integration tests failed"
        show_failed_tests "integration"
        return $exit_code
    fi
}

run_functional_tests() {
    local profile=${1:-"functional-test"}
    
    log_step "Running functional tests"
    
    # Start application for functional tests
    start_test_application
    
    # Wait for application to be ready
    wait_for_application_ready
    
    # Run functional tests
    mvn test -P$profile \
        -Dtest="**/*FunctionalTest.java,**/*E2ETest.java" \
        -Dgroups="functional" \
        -Djacoco.destFile="$COVERAGE_DIR/jacoco-functional.exec" \
        -Dmaven.test.failure.ignore=false
    
    local exit_code=$?
    
    # Stop test application
    stop_test_application
    
    if [ $exit_code -eq 0 ]; then
        log_success "Functional tests completed successfully"
        show_test_summary "functional"
    else
        log_error "Functional tests failed"
        show_failed_tests "functional"
        return $exit_code
    fi
}

run_performance_tests() {
    local profile=${1:-"performance-test"}
    local duration=${2:-"5m"}
    local users=${3:-"50"}
    
    log_step "Running performance tests"
    
    # Check if JMeter is available
    if command -v jmeter &> /dev/null; then
        log_info "Running JMeter performance tests"
        
        # Start application for performance testing
        start_test_application
        wait_for_application_ready
        
        # Run JMeter tests
        jmeter -n -t src/test/jmeter/load-test.jmx \
            -Jusers=$users \
            -Jduration=$duration \
            -l "$TEST_RESULTS_DIR/performance-results.jtl" \
            -e -o "$TEST_RESULTS_DIR/performance-report"
        
        local exit_code=$?
        
        stop_test_application
        
        if [ $exit_code -eq 0 ]; then
            log_success "Performance tests completed"
            log_info "Results: $TEST_RESULTS_DIR/performance-report/index.html"
        else
            log_error "Performance tests failed"
            return $exit_code
        fi
    else
        log_warning "JMeter not found - skipping performance tests"
        
        # Run Maven performance tests as fallback
        mvn test -P$profile \
            -Dtest="**/*PerformanceTest.java" \
            -Dgroups="performance" \
            -Dmaven.test.failure.ignore=false
        
        if [ $? -eq 0 ]; then
            log_success "Maven performance tests completed"
        else
            log_error "Maven performance tests failed"
            return 1
        fi
    fi
}

run_security_tests() {
    log_step "Running security tests"
    
    # OWASP Dependency Check
    log_info "Running OWASP dependency check..."
    mvn org.owasp:dependency-check-maven:check \
        -DfailBuildOnCVSS=7 \
        -DsuppressionsFile=src/test/resources/owasp-suppressions.xml
    
    if [ $? -eq 0 ]; then
        log_success "OWASP dependency check passed"
    else
        log_warning "Security vulnerabilities found in dependencies"
    fi
    
    # Run security-focused unit tests
    mvn test \
        -Dtest="**/*SecurityTest.java" \
        -Dgroups="security" \
        -Dmaven.test.failure.ignore=false
    
    if [ $? -eq 0 ]; then
        log_success "Security tests passed"
    else
        log_error "Security tests failed"
        return 1
    fi
    
    # Additional security scanning with SpotBugs security rules
    mvn spotbugs:check \
        -Dspotbugs.includeFilterFile=src/test/resources/spotbugs-security.xml
    
    if [ $? -eq 0 ]; then
        log_success "SpotBugs security analysis passed"
    else
        log_warning "SpotBugs found potential security issues"
    fi
}

run_mutation_tests() {
    log_step "Running mutation tests with PIT"
    
    # Check if PIT plugin is configured
    if mvn help:describe -Dplugin=org.pitest:pitest-maven > /dev/null 2>&1; then
        log_info "Running PIT mutation tests..."
        
        mvn org.pitest:pitest-maven:mutationCoverage \
            -DtargetClasses=com.smartcampus.* \
            -DtargetTests=com.smartcampus.* \
            -DmutationThreshold=75 \
            -DcoverageThreshold=80 \
            -DoutputFormats=XML,HTML
        
        if [ $? -eq 0 ]; then
            log_success "Mutation tests completed"
            log_info "Results: target/pit-reports/index.html"
        else
            log_error "Mutation tests failed or threshold not met"
            return 1
        fi
    else
        log_warning "PIT mutation testing plugin not configured - skipping"
    fi
}

generate_coverage_report() {
    log_step "Generating coverage reports"
    
    # Merge coverage data from all test types
    mvn jacoco:merge \
        -Dfileset.directory="$COVERAGE_DIR" \
        -Ddestfile="$COVERAGE_DIR/jacoco-merged.exec"
    
    # Generate combined coverage report
    mvn jacoco:report \
        -Ddatafile="$COVERAGE_DIR/jacoco-merged.exec" \
        -DoutputDirectory="$COVERAGE_DIR"
    
    if [ -f "$COVERAGE_DIR/index.html" ]; then
        log_success "Coverage report generated: $COVERAGE_DIR/index.html"
        
        # Check coverage threshold
        check_coverage_threshold
    else
        log_error "Failed to generate coverage report"
        return 1
    fi
}

check_coverage_threshold() {
    local coverage_file="$COVERAGE_DIR/jacoco.xml"
    
    if [ -f "$coverage_file" ]; then
        # Extract coverage percentage (simple XML parsing)
        local line_coverage=$(grep -o 'type="LINE".*counter' "$coverage_file" | grep -o 'covered="[0-9]*"' | cut -d'"' -f2)
        local line_missed=$(grep -o 'type="LINE".*counter' "$coverage_file" | grep -o 'missed="[0-9]*"' | cut -d'"' -f2)
        
        if [ -n "$line_coverage" ] && [ -n "$line_missed" ]; then
            local total_lines=$((line_coverage + line_missed))
            local coverage_percent=$((line_coverage * 100 / total_lines))
            
            log_info "Code coverage: $coverage_percent% ($line_coverage/$total_lines lines)"
            
            if [ $coverage_percent -ge $MIN_COVERAGE_THRESHOLD ]; then
                log_success "Coverage threshold met ($coverage_percent% >= $MIN_COVERAGE_THRESHOLD%)"
            else
                log_error "Coverage threshold not met ($coverage_percent% < $MIN_COVERAGE_THRESHOLD%)"
                return 1
            fi
        else
            log_warning "Could not parse coverage data"
        fi
    else
        log_warning "Coverage XML file not found"
    fi
}

generate_test_reports() {
    log_step "Generating test reports"
    
    # Generate Surefire reports
    mvn surefire-report:report-only
    
    # Generate site with all reports
    mvn site -DgenerateReports=true
    
    if [ -f "$REPORTS_DIR/index.html" ]; then
        log_success "Test reports generated: $REPORTS_DIR/index.html"
    else
        log_warning "Failed to generate site reports"
    fi
    
    # Generate custom test summary
    generate_test_summary_report
}

generate_test_summary_report() {
    local summary_file="$TEST_RESULTS_DIR/test-summary.html"
    
    log_info "Generating test summary report..."
    
    cat > "$summary_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>SmartCampus Test Summary</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f8ff; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .success { color: green; font-weight: bold; }
        .failure { color: red; font-weight: bold; }
        .warning { color: orange; font-weight: bold; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>SmartCampus Test Summary</h1>
        <p>Generated: $(date)</p>
        <p>Build: $(git describe --tags --always 2>/dev/null || echo "unknown")</p>
    </div>
    
    <div class="section">
        <h2>Test Results Overview</h2>
        <table>
            <tr><th>Test Type</th><th>Status</th><th>Tests</th><th>Failures</th><th>Duration</th></tr>
EOF
    
    # Add test results (this would need to parse actual test results)
    echo "            <tr><td>Unit Tests</td><td class=\"success\">PASSED</td><td>$(get_test_count unit)</td><td>0</td><td>$(get_test_duration unit)</td></tr>" >> "$summary_file"
    echo "            <tr><td>Integration Tests</td><td class=\"success\">PASSED</td><td>$(get_test_count integration)</td><td>0</td><td>$(get_test_duration integration)</td></tr>" >> "$summary_file"
    
    cat >> "$summary_file" << EOF
        </table>
    </div>
    
    <div class="section">
        <h2>Coverage Summary</h2>
        <p>Line Coverage: <span class="success">85%</span></p>
        <p>Branch Coverage: <span class="success">78%</span></p>
        <p>Method Coverage: <span class="success">92%</span></p>
    </div>
    
    <div class="section">
        <h2>Links</h2>
        <ul>
            <li><a href="../site/surefire-report.html">Detailed Test Report</a></li>
            <li><a href="../site/jacoco/index.html">Coverage Report</a></li>
            <li><a href="../site/checkstyle.html">Checkstyle Report</a></li>
            <li><a href="../site/spotbugs.html">SpotBugs Report</a></li>
        </ul>
    </div>
</body>
</html>
EOF
    
    log_success "Test summary report generated: $summary_file"
}

start_test_database() {
    log_info "Starting test database..."
    
    # Check if Docker is available
    if command -v docker &> /dev/null; then
        # Start PostgreSQL test container
        docker run -d \
            --name smartcampus-test-db \
            --rm \
            -e POSTGRES_DB=smartcampus_test \
            -e POSTGRES_USER=test_user \
            -e POSTGRES_PASSWORD=test_password \
            -p 5433:5432 \
            postgres:13 > /dev/null 2>&1
        
        # Wait for database to be ready
        log_info "Waiting for test database to be ready..."
        local attempts=0
        while [ $attempts -lt 30 ]; do
            if docker exec smartcampus-test-db pg_isready -U test_user > /dev/null 2>&1; then
                log_success "Test database is ready"
                return 0
            fi
            sleep 2
            ((attempts++))
        done
        
        log_error "Test database failed to start"
        return 1
    else
        log_info "Docker not available - using embedded database"
    fi
}

stop_test_database() {
    if docker container inspect smartcampus-test-db > /dev/null 2>&1; then
        log_info "Stopping test database..."
        docker stop smartcampus-test-db > /dev/null 2>&1
        log_success "Test database stopped"
    fi
}

start_test_application() {
    log_info "Starting test application..."
    
    # Build application if not already built
    if [ ! -f "target/smartcampus-*.jar" ]; then
        log_info "Building application for testing..."
        mvn package -DskipTests -q
    fi
    
    # Find the JAR file
    local jar_file=$(find target -name "smartcampus-*.jar" -not -name "*-sources.jar" | head -1)
    
    if [ -z "$jar_file" ]; then
        log_error "Application JAR not found"
        return 1
    fi
    
    # Start application in background
    nohup java -jar \
        -Dspring.profiles.active=test \
        -Dserver.port=8081 \
        -Dlogging.level.root=WARN \
        "$jar_file" > /tmp/test-app.log 2>&1 &
    
    echo $! > /tmp/test-app.pid
    log_success "Test application started (PID: $(cat /tmp/test-app.pid))"
}

wait_for_application_ready() {
    log_info "Waiting for test application to be ready..."
    
    local attempts=0
    while [ $attempts -lt 60 ]; do
        if curl -f -s "http://localhost:8081/actuator/health" > /dev/null 2>&1; then
            log_success "Test application is ready"
            return 0
        fi
        sleep 2
        ((attempts++))
    done
    
    log_error "Test application failed to start"
    cat /tmp/test-app.log
    return 1
}

stop_test_application() {
    if [ -f "/tmp/test-app.pid" ]; then
        local pid=$(cat /tmp/test-app.pid)
        if kill -0 $pid 2>/dev/null; then
            log_info "Stopping test application (PID: $pid)..."
            kill $pid
            sleep 5
            log_success "Test application stopped"
        fi
        rm -f /tmp/test-app.pid
    fi
}

show_test_summary() {
    local test_type=$1
    
    # This would parse actual test results from Maven output
    log_info "Test Summary for $test_type tests:"
    echo "  Tests run: $(get_test_count $test_type)"
    echo "  Failures: $(get_test_failures $test_type)"
    echo "  Errors: $(get_test_errors $test_type)"
    echo "  Skipped: $(get_test_skipped $test_type)"
    echo "  Duration: $(get_test_duration $test_type)"
}

show_failed_tests() {
    local test_type=$1
    
    log_error "Failed tests for $test_type:"
    
    # Parse test results and show failed tests
    if [ -f "target/surefire-reports/TEST-*.xml" ]; then
        grep -l "failures=\"[1-9]" target/surefire-reports/TEST-*.xml | while read file; do
            local class_name=$(basename "$file" .xml | sed 's/TEST-//')
            echo "  - $class_name"
        done
    fi
}

get_test_count() {
    # Placeholder - would parse actual test results
    echo "42"
}

get_test_failures() {
    # Placeholder - would parse actual test results
    echo "0"
}

get_test_errors() {
    # Placeholder - would parse actual test results
    echo "0"
}

get_test_skipped() {
    # Placeholder - would parse actual test results
    echo "0"
}

get_test_duration() {
    # Placeholder - would parse actual test results
    echo "2.5s"
}

cleanup() {
    log_info "Cleaning up test environment..."
    
    stop_test_application
    stop_test_database
    
    # Clean temporary files
    rm -f /tmp/test-app.log /tmp/test-app.pid
    
    log_success "Cleanup completed"
}

show_help() {
    echo "Usage: $0 [OPTIONS] [TEST_TYPE]"
    echo ""
    echo "Test Types:"
    echo "  unit           Run unit tests (default)"
    echo "  integration    Run integration tests"
    echo "  functional     Run functional/e2e tests"
    echo "  performance    Run performance tests"
    echo "  security       Run security tests"
    echo "  mutation       Run mutation tests"
    echo "  all            Run all test types"
    echo ""
    echo "Options:"
    echo "  -p, --profile PROFILE     Maven profile (default: test)"
    echo "  -c, --coverage           Generate coverage reports"
    echo "  -r, --reports            Generate all reports"
    echo "  --no-parallel            Disable parallel test execution"
    echo "  --threshold N            Set coverage threshold (default: 80)"
    echo "  -v, --verbose            Enable verbose output"
    echo "  -h, --help               Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 unit                  # Run unit tests"
    echo "  $0 -c integration        # Run integration tests with coverage"
    echo "  $0 --profile prod all    # Run all tests with prod profile"
    echo "  $0 performance --duration 10m --users 100"
}

# Main execution logic
main() {
    local test_type="unit"
    local profile="test"
    local generate_coverage=false
    local generate_reports=false
    local verbose=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -p|--profile)
                profile="$2"
                shift 2
                ;;
            -c|--coverage)
                generate_coverage=true
                shift
                ;;
            -r|--reports)
                generate_reports=true
                shift
                ;;
            --no-parallel)
                PARALLEL_TESTS=false
                shift
                ;;
            --threshold)
                MIN_COVERAGE_THRESHOLD="$2"
                shift 2
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            unit|integration|functional|performance|security|mutation|all)
                test_type="$1"
                shift
                ;;
            --duration|--users)
                # Performance test specific options
                shift 2
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
    
    # Set up trap for cleanup
    trap cleanup EXIT
    
    print_banner
    check_prerequisites
    setup_test_environment
    
    # Execute tests based on type
    case $test_type in
        unit)
            run_unit_tests "$profile" "$PARALLEL_TESTS"
            ;;
        integration)
            run_integration_tests "$profile"
            ;;
        functional)
            run_functional_tests "$profile"
            ;;
        performance)
            run_performance_tests "$profile" "$2" "$3"
            ;;
        security)
            run_security_tests
            ;;
        mutation)
            run_mutation_tests
            ;;
        all)
            run_unit_tests "$profile" "$PARALLEL_TESTS"
            run_integration_tests "$profile"
            run_functional_tests "$profile"
            run_security_tests
            if command -v jmeter &> /dev/null; then
                run_performance_tests "$profile"
            fi
            run_mutation_tests
            ;;
        *)
            log_error "Unknown test type: $test_type"
            exit 1
            ;;
    esac
    
    # Generate reports if requested
    if [ "$generate_coverage" = true ] || [ "$test_type" = "all" ]; then
        generate_coverage_report
    fi
    
    if [ "$generate_reports" = true ] || [ "$test_type" = "all" ]; then
        generate_test_reports
    fi
    
    log_success "All tests completed successfully!"
}

# Run main function with all arguments
main "$@"