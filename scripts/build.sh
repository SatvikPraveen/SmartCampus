#!/bin/bash

# Location: scripts/build.sh
# SmartCampus Build Script
# This script handles building, testing, and packaging the SmartCampus application

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="SmartCampus"
BUILD_DIR="target"
JAR_NAME="smartcampus"
DOCKER_IMAGE="smartcampus"
PROFILES="dev,test,prod"

# Functions
print_banner() {
    echo -e "${BLUE}"
    echo "=================================="
    echo "  SmartCampus Build Script"
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

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "?(1\.)?\K\d+' | head -1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        log_error "Java 17 or higher is required. Found version: $JAVA_VERSION"
        exit 1
    fi
    
    log_success "Java $JAVA_VERSION found"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    MVN_VERSION=$(mvn -version | grep "Apache Maven" | cut -d' ' -f3)
    log_success "Maven $MVN_VERSION found"
    
    # Check Git
    if ! command -v git &> /dev/null; then
        log_warning "Git is not installed - version information will not be available"
    else
        GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
        GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
        log_success "Git found - Branch: $GIT_BRANCH, Commit: $GIT_COMMIT"
    fi
}

clean_build() {
    log_info "Cleaning previous build artifacts..."
    
    if [ -d "$BUILD_DIR" ]; then
        rm -rf $BUILD_DIR
        log_success "Cleaned $BUILD_DIR directory"
    fi
    
    # Clean Maven artifacts
    mvn clean -q
    log_success "Maven clean completed"
}

run_tests() {
    local test_profile=${1:-"test"}
    
    log_info "Running tests with profile: $test_profile"
    
    # Unit tests
    log_info "Running unit tests..."
    mvn test -P$test_profile -Dtest="**/*Test.java" -DfailIfNoTests=false
    
    if [ $? -eq 0 ]; then
        log_success "Unit tests passed"
    else
        log_error "Unit tests failed"
        exit 1
    fi
    
    # Integration tests
    log_info "Running integration tests..."
    mvn test -P$test_profile -Dtest="**/*IntegrationTest.java" -DfailIfNoTests=false
    
    if [ $? -eq 0 ]; then
        log_success "Integration tests passed"
    else
        log_error "Integration tests failed"
        exit 1
    fi
    
    # Generate test reports
    mvn surefire-report:report -q
    log_info "Test reports generated in target/site/surefire-report.html"
}

run_quality_checks() {
    log_info "Running code quality checks..."
    
    # Checkstyle
    if mvn checkstyle:check -q; then
        log_success "Checkstyle passed"
    else
        log_warning "Checkstyle issues found - check target/site/checkstyle.html"
    fi
    
    # SpotBugs
    if mvn spotbugs:check -q; then
        log_success "SpotBugs analysis passed"
    else
        log_warning "SpotBugs issues found - check target/site/spotbugs.html"
    fi
    
    # PMD
    if mvn pmd:check -q; then
        log_success "PMD analysis passed"
    else
        log_warning "PMD issues found - check target/site/pmd.html"
    fi
}

build_application() {
    local profile=${1:-"dev"}
    
    log_info "Building application with profile: $profile"
    
    # Set build properties
    BUILD_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    BUILD_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    
    # Build with Maven
    mvn package -P$profile \
        -DskipTests=${SKIP_TESTS:-false} \
        -Dbuild.time="$BUILD_TIME" \
        -Dbuild.version="$BUILD_VERSION" \
        -Dgit.commit="$GIT_COMMIT" \
        -Dgit.branch="$GIT_BRANCH"
    
    if [ $? -eq 0 ]; then
        log_success "Application built successfully"
        
        # Show build artifacts
        if [ -f "$BUILD_DIR/$JAR_NAME-$BUILD_VERSION.jar" ]; then
            JAR_SIZE=$(du -h "$BUILD_DIR/$JAR_NAME-$BUILD_VERSION.jar" | cut -f1)
            log_info "JAR file: $JAR_NAME-$BUILD_VERSION.jar ($JAR_SIZE)"
        fi
    else
        log_error "Build failed"
        exit 1
    fi
}

generate_documentation() {
    log_info "Generating documentation..."
    
    # JavaDoc
    mvn javadoc:javadoc -q
    if [ $? -eq 0 ]; then
        log_success "JavaDoc generated in target/site/apidocs/"
    else
        log_warning "JavaDoc generation failed"
    fi
    
    # Site documentation
    mvn site -q
    if [ $? -eq 0 ]; then
        log_success "Site documentation generated in target/site/"
    else
        log_warning "Site documentation generation failed"
    fi
}

build_docker_image() {
    local version=${1:-"latest"}
    
    if ! command -v docker &> /dev/null; then
        log_warning "Docker not found - skipping Docker image build"
        return 0
    fi
    
    log_info "Building Docker image: $DOCKER_IMAGE:$version"
    
    # Build Docker image
    docker build -t $DOCKER_IMAGE:$version .
    
    if [ $? -eq 0 ]; then
        log_success "Docker image built: $DOCKER_IMAGE:$version"
        
        # Show image size
        IMAGE_SIZE=$(docker images $DOCKER_IMAGE:$version --format "table {{.Size}}" | tail -1)
        log_info "Docker image size: $IMAGE_SIZE"
        
        # Tag as latest if not already
        if [ "$version" != "latest" ]; then
            docker tag $DOCKER_IMAGE:$version $DOCKER_IMAGE:latest
            log_info "Tagged as $DOCKER_IMAGE:latest"
        fi
    else
        log_error "Docker image build failed"
        exit 1
    fi
}

run_security_scan() {
    log_info "Running security scans..."
    
    # OWASP Dependency Check
    if mvn org.owasp:dependency-check-maven:check -q; then
        log_success "OWASP dependency check passed"
    else
        log_warning "Security vulnerabilities found - check target/site/dependency-check-report.html"
    fi
    
    # Snyk scan if available
    if command -v snyk &> /dev/null; then
        log_info "Running Snyk security scan..."
        snyk test --severity-threshold=high
        if [ $? -eq 0 ]; then
            log_success "Snyk security scan passed"
        else
            log_warning "High severity vulnerabilities found"
        fi
    fi
}

create_build_info() {
    local build_info_file="$BUILD_DIR/build-info.json"
    
    log_info "Creating build information file..."
    
    cat > $build_info_file << EOF
{
  "project": "$PROJECT_NAME",
  "version": "$BUILD_VERSION",
  "buildTime": "$BUILD_TIME",
  "gitCommit": "$GIT_COMMIT",
  "gitBranch": "$GIT_BRANCH",
  "javaVersion": "$JAVA_VERSION",
  "mavenVersion": "$MVN_VERSION",
  "buildHost": "$(hostname)",
  "buildUser": "$(whoami)",
  "profile": "$PROFILE"
}
EOF
    
    log_success "Build info created: $build_info_file"
}

package_artifacts() {
    local version=${1:-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)}
    local package_name="$JAR_NAME-$version-dist"
    
    log_info "Packaging distribution artifacts..."
    
    mkdir -p $BUILD_DIR/dist
    
    # Copy main artifacts
    cp $BUILD_DIR/$JAR_NAME-$version.jar $BUILD_DIR/dist/
    
    # Copy configuration files
    cp -r src/main/resources/config $BUILD_DIR/dist/ 2>/dev/null || true
    
    # Copy scripts
    mkdir -p $BUILD_DIR/dist/bin
    cp scripts/*.sh $BUILD_DIR/dist/bin/ 2>/dev/null || true
    
    # Copy documentation
    cp -r target/site $BUILD_DIR/dist/docs 2>/dev/null || true
    cp README.md $BUILD_DIR/dist/ 2>/dev/null || true
    cp CHANGELOG.md $BUILD_DIR/dist/ 2>/dev/null || true
    
    # Create distribution archive
    cd $BUILD_DIR
    tar -czf $package_name.tar.gz dist/
    zip -rq $package_name.zip dist/
    cd ..
    
    log_success "Distribution packages created:"
    log_info "  - $BUILD_DIR/$package_name.tar.gz"
    log_info "  - $BUILD_DIR/$package_name.zip"
}

show_help() {
    echo "Usage: $0 [OPTIONS] [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  build          Build the application (default)"
    echo "  clean          Clean build artifacts"
    echo "  test           Run tests only"
    echo "  package        Build and package for distribution"
    echo "  docker         Build Docker image"
    echo "  docs           Generate documentation"
    echo "  security       Run security scans"
    echo "  all            Run all build steps"
    echo ""
    echo "Options:"
    echo "  -p, --profile PROFILE    Maven profile to use (default: dev)"
    echo "  -s, --skip-tests         Skip running tests"
    echo "  -q, --quiet             Quiet output"
    echo "  -v, --version VERSION   Set version for Docker image"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                      # Build with dev profile"
    echo "  $0 -p prod package      # Build and package for production"
    echo "  $0 --skip-tests docker  # Build Docker image without tests"
    echo "  $0 all                  # Run complete build pipeline"
}

# Main execution logic
main() {
    local command="build"
    local profile="dev"
    local skip_tests=false
    local quiet=false
    local docker_version="latest"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -p|--profile)
                profile="$2"
                shift 2
                ;;
            -s|--skip-tests)
                skip_tests=true
                shift
                ;;
            -q|--quiet)
                quiet=true
                shift
                ;;
            -v|--version)
                docker_version="$2"
                shift 2
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            build|clean|test|package|docker|docs|security|all)
                command="$1"
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Set environment variables
    export SKIP_TESTS=$skip_tests
    export PROFILE=$profile
    
    if [ "$quiet" = true ]; then
        exec > /dev/null 2>&1
    fi
    
    print_banner
    check_prerequisites
    
    # Execute command
    case $command in
        clean)
            clean_build
            ;;
        test)
            run_tests $profile
            ;;
        build)
            clean_build
            if [ "$skip_tests" != true ]; then
                run_tests $profile
            fi
            run_quality_checks
            build_application $profile
            create_build_info
            ;;
        package)
            clean_build
            if [ "$skip_tests" != true ]; then
                run_tests $profile
            fi
            run_quality_checks
            build_application $profile
            generate_documentation
            create_build_info
            package_artifacts
            ;;
        docker)
            if [ "$skip_tests" != true ]; then
                run_tests $profile
            fi
            build_application $profile
            build_docker_image $docker_version
            ;;
        docs)
            generate_documentation
            ;;
        security)
            run_security_scan
            ;;
        all)
            clean_build
            run_tests $profile
            run_quality_checks
            build_application $profile
            generate_documentation
            run_security_scan
            create_build_info
            build_docker_image $docker_version
            package_artifacts
            ;;
        *)
            log_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
    
    log_success "Build completed successfully!"
}

# Run main function with all arguments
main "$@"