#!/bin/bash

# Location: scripts/deploy.sh
# SmartCampus Deployment Script
# This script handles deployment to different environments

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
DEFAULT_PORT="8080"
HEALTH_CHECK_ENDPOINT="/actuator/health"
MAX_HEALTH_CHECK_ATTEMPTS=30
HEALTH_CHECK_INTERVAL=10

# Functions
print_banner() {
    echo -e "${BLUE}"
    echo "===================================="
    echo "  SmartCampus Deployment Script"
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

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1"
}

check_prerequisites() {
    local environment=$1
    
    log_info "Checking deployment prerequisites for environment: $environment"
    
    case $environment in
        local|dev)
            # Check Java
            if ! command -v java &> /dev/null; then
                log_error "Java is not installed"
                exit 1
            fi
            log_success "Java found"
            ;;
        docker)
            # Check Docker
            if ! command -v docker &> /dev/null; then
                log_error "Docker is not installed"
                exit 1
            fi
            
            if ! docker info &> /dev/null; then
                log_error "Docker daemon is not running"
                exit 1
            fi
            log_success "Docker is available"
            ;;
        k8s|kubernetes)
            # Check kubectl
            if ! command -v kubectl &> /dev/null; then
                log_error "kubectl is not installed"
                exit 1
            fi
            
            if ! kubectl cluster-info &> /dev/null; then
                log_error "Cannot connect to Kubernetes cluster"
                exit 1
            fi
            log_success "Kubernetes cluster is accessible"
            ;;
        aws)
            # Check AWS CLI
            if ! command -v aws &> /dev/null; then
                log_error "AWS CLI is not installed"
                exit 1
            fi
            
            if ! aws sts get-caller-identity &> /dev/null; then
                log_error "AWS credentials not configured"
                exit 1
            fi
            log_success "AWS CLI is configured"
            ;;
        *)
            log_warning "Unknown environment: $environment"
            ;;
    esac
}

wait_for_health_check() {
    local host=${1:-"localhost"}
    local port=${2:-$DEFAULT_PORT}
    local endpoint=${3:-$HEALTH_CHECK_ENDPOINT}
    
    log_info "Waiting for application health check at http://$host:$port$endpoint"
    
    local attempt=1
    while [ $attempt -le $MAX_HEALTH_CHECK_ATTEMPTS ]; do
        if curl -f -s "http://$host:$port$endpoint" > /dev/null 2>&1; then
            log_success "Application is healthy after $attempt attempts"
            return 0
        fi
        
        echo -n "."
        sleep $HEALTH_CHECK_INTERVAL
        ((attempt++))
    done
    
    echo ""
    log_error "Application health check failed after $MAX_HEALTH_CHECK_ATTEMPTS attempts"
    return 1
}

deploy_local() {
    local jar_file=$1
    local profile=${2:-"dev"}
    local port=${3:-$DEFAULT_PORT}
    
    log_step "Deploying to local environment"
    
    if [ ! -f "$jar_file" ]; then
        log_error "JAR file not found: $jar_file"
        exit 1
    fi
    
    # Check if port is already in use
    if lsof -i :$port > /dev/null 2>&1; then
        log_warning "Port $port is already in use"
        read -p "Do you want to kill the existing process? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            local pid=$(lsof -t -i :$port)
            kill $pid
            sleep 5
            log_info "Killed process on port $port"
        else
            log_error "Deployment cancelled"
            exit 1
        fi
    fi
    
    # Create logs directory
    mkdir -p logs
    
    # Start application
    log_info "Starting application with profile: $profile"
    nohup java -jar \
        -Dspring.profiles.active=$profile \
        -Dserver.port=$port \
        -Dlogging.file.name=logs/application.log \
        "$jar_file" \
        > logs/startup.log 2>&1 &
    
    local app_pid=$!
    echo $app_pid > .app.pid
    
    log_info "Application started with PID: $app_pid"
    
    # Wait for health check
    if wait_for_health_check "localhost" $port; then
        log_success "Local deployment completed successfully"
        log_info "Application is running at: http://localhost:$port"
        log_info "Health check: http://localhost:$port$HEALTH_CHECK_ENDPOINT"
        log_info "Logs: tail -f logs/application.log"
    else
        log_error "Deployment failed - application is not healthy"
        exit 1
    fi
}

deploy_docker() {
    local image_name=${1:-"smartcampus:latest"}
    local container_name=${2:-"smartcampus-app"}
    local port=${3:-$DEFAULT_PORT}
    local environment=${4:-"docker"}
    
    log_step "Deploying to Docker environment"
    
    # Check if image exists
    if ! docker image inspect "$image_name" > /dev/null 2>&1; then
        log_error "Docker image not found: $image_name"
        log_info "Build the image first: docker build -t $image_name ."
        exit 1
    fi
    
    # Stop and remove existing container
    if docker container inspect "$container_name" > /dev/null 2>&1; then
        log_info "Stopping existing container: $container_name"
        docker stop "$container_name"
        docker rm "$container_name"
    fi
    
    # Run new container
    log_info "Starting Docker container: $container_name"
    docker run -d \
        --name "$container_name" \
        --restart unless-stopped \
        -p "$port:8080" \
        -e SPRING_PROFILES_ACTIVE="$environment" \
        -e TZ="America/New_York" \
        -v smartcampus-data:/opt/smartcampus/data \
        -v smartcampus-logs:/opt/smartcampus/logs \
        --health-cmd="curl -f http://localhost:8080$HEALTH_CHECK_ENDPOINT || exit 1" \
        --health-interval=30s \
        --health-timeout=10s \
        --health-retries=3 \
        "$image_name"
    
    local container_id=$(docker ps -q -f name="$container_name")
    if [ -n "$container_id" ]; then
        log_success "Container started with ID: $container_id"
        
        # Wait for health check
        if wait_for_health_check "localhost" $port; then
            log_success "Docker deployment completed successfully"
            log_info "Application is running at: http://localhost:$port"
            log_info "Container logs: docker logs -f $container_name"
        else
            log_error "Deployment failed - application is not healthy"
            docker logs "$container_name"
            exit 1
        fi
    else
        log_error "Failed to start Docker container"
        exit 1
    fi
}

deploy_docker_compose() {
    local compose_file=${1:-"docker-compose.yml"}
    local environment=${2:-"docker"}
    
    log_step "Deploying with Docker Compose"
    
    if [ ! -f "$compose_file" ]; then
        log_error "Docker Compose file not found: $compose_file"
        exit 1
    fi
    
    # Set environment variables
    export ENVIRONMENT=$environment
    export APP_VERSION=$(git describe --tags --always 2>/dev/null || echo "latest")
    
    # Pull latest images
    log_info "Pulling latest images..."
    docker-compose -f "$compose_file" pull
    
    # Start services
    log_info "Starting services with Docker Compose..."
    docker-compose -f "$compose_file" up -d
    
    # Wait for services to be ready
    sleep 10
    
    # Check service health
    local app_container=$(docker-compose -f "$compose_file" ps -q smartcampus-app)
    if [ -n "$app_container" ]; then
        local app_port=$(docker port "$app_container" 8080 | cut -d: -f2)
        
        if wait_for_health_check "localhost" "${app_port:-8080}"; then
            log_success "Docker Compose deployment completed successfully"
            log_info "Services status:"
            docker-compose -f "$compose_file" ps
        else
            log_error "Deployment failed - application is not healthy"
            docker-compose -f "$compose_file" logs
            exit 1
        fi
    else
        log_error "Application container not found"
        exit 1
    fi
}

deploy_kubernetes() {
    local namespace=${1:-"smartcampus"}
    local manifest_dir=${2:-"k8s"}
    
    log_step "Deploying to Kubernetes"
    
    if [ ! -d "$manifest_dir" ]; then
        log_error "Kubernetes manifests directory not found: $manifest_dir"
        exit 1
    fi
    
    # Create namespace if it doesn't exist
    if ! kubectl get namespace "$namespace" > /dev/null 2>&1; then
        log_info "Creating namespace: $namespace"
        kubectl create namespace "$namespace"
    fi
    
    # Apply configurations in order
    local configs=("configmap" "secret" "deployment" "service" "ingress")
    
    for config in "${configs[@]}"; do
        local config_file="$manifest_dir/$config.yaml"
        if [ -f "$config_file" ]; then
            log_info "Applying $config configuration..."
            kubectl apply -f "$config_file" -n "$namespace"
        fi
    done
    
    # Wait for deployment rollout
    log_info "Waiting for deployment rollout..."
    kubectl rollout status deployment/smartcampus-app -n "$namespace" --timeout=600s
    
    # Check pod status
    local pod_status=$(kubectl get pods -n "$namespace" -l app=smartcampus-app -o jsonpath='{.items[0].status.phase}')
    if [ "$pod_status" = "Running" ]; then
        log_success "Kubernetes deployment completed successfully"
        
        # Show deployment info
        log_info "Deployment status:"
        kubectl get deployments -n "$namespace"
        kubectl get pods -n "$namespace"
        kubectl get services -n "$namespace"
    else
        log_error "Deployment failed - pod status: $pod_status"
        kubectl describe pods -n "$namespace" -l app=smartcampus-app
        exit 1
    fi
}

deploy_aws_ecs() {
    local cluster_name=${1:-"smartcampus-cluster"}
    local service_name=${2:-"smartcampus-service"}
    local task_definition=${3:-"smartcampus-task"}
    
    log_step "Deploying to AWS ECS"
    
    # Check if cluster exists
    if ! aws ecs describe-clusters --clusters "$cluster_name" > /dev/null 2>&1; then
        log_error "ECS cluster not found: $cluster_name"
        exit 1
    fi
    
    # Register new task definition
    log_info "Registering task definition..."
    local task_def_arn=$(aws ecs register-task-definition \
        --cli-input-json file://aws/task-definition.json \
        --query 'taskDefinition.taskDefinitionArn' \
        --output text)
    
    if [ -z "$task_def_arn" ]; then
        log_error "Failed to register task definition"
        exit 1
    fi
    
    log_info "Task definition registered: $task_def_arn"
    
    # Update service
    log_info "Updating ECS service..."
    aws ecs update-service \
        --cluster "$cluster_name" \
        --service "$service_name" \
        --task-definition "$task_def_arn" > /dev/null
    
    # Wait for service to stabilize
    log_info "Waiting for service to stabilize..."
    aws ecs wait services-stable \
        --cluster "$cluster_name" \
        --services "$service_name"
    
    # Check service status
    local service_status=$(aws ecs describe-services \
        --cluster "$cluster_name" \
        --services "$service_name" \
        --query 'services[0].status' \
        --output text)
    
    if [ "$service_status" = "ACTIVE" ]; then
        log_success "AWS ECS deployment completed successfully"
        
        # Show service info
        aws ecs describe-services \
            --cluster "$cluster_name" \
            --services "$service_name" \
            --query 'services[0].{Status:status,RunningCount:runningCount,DesiredCount:desiredCount}'
    else
        log_error "Deployment failed - service status: $service_status"
        exit 1
    fi
}

rollback_deployment() {
    local environment=$1
    local version=${2:-"previous"}
    
    log_step "Rolling back deployment for environment: $environment"
    
    case $environment in
        local)
            if [ -f ".app.pid" ]; then
                local pid=$(cat .app.pid)
                if kill -0 $pid 2>/dev/null; then
                    kill $pid
                    log_info "Stopped current application (PID: $pid)"
                fi
                rm .app.pid
            fi
            
            # Start previous version (implementation depends on versioning strategy)
            log_warning "Local rollback requires manual intervention"
            ;;
        docker)
            local container_name=${3:-"smartcampus-app"}
            docker stop "$container_name" 2>/dev/null || true
            docker rm "$container_name" 2>/dev/null || true
            
            # Deploy previous image version
            deploy_docker "smartcampus:$version" "$container_name"
            ;;
        kubernetes)
            local namespace=${3:-"smartcampus"}
            kubectl rollout undo deployment/smartcampus-app -n "$namespace"
            kubectl rollout status deployment/smartcampus-app -n "$namespace"
            log_success "Kubernetes rollback completed"
            ;;
        aws)
            local cluster_name=${3:-"smartcampus-cluster"}
            local service_name=${4:-"smartcampus-service"}
            
            # Get previous task definition
            local prev_task_def=$(aws ecs describe-services \
                --cluster "$cluster_name" \
                --services "$service_name" \
                --query 'services[0].deployments[1].taskDefinition' \
                --output text)
            
            if [ "$prev_task_def" != "None" ]; then
                aws ecs update-service \
                    --cluster "$cluster_name" \
                    --service "$service_name" \
                    --task-definition "$prev_task_def" > /dev/null
                
                aws ecs wait services-stable \
                    --cluster "$cluster_name" \
                    --services "$service_name"
                
                log_success "AWS ECS rollback completed"
            else
                log_error "No previous deployment found for rollback"
                exit 1
            fi
            ;;
        *)
            log_error "Rollback not supported for environment: $environment"
            exit 1
            ;;
    esac
}

stop_application() {
    local environment=$1
    
    log_step "Stopping application for environment: $environment"
    
    case $environment in
        local)
            if [ -f ".app.pid" ]; then
                local pid=$(cat .app.pid)
                if kill -0 $pid 2>/dev/null; then
                    kill $pid
                    log_success "Stopped application (PID: $pid)"
                    rm .app.pid
                else
                    log_warning "Application not running (PID: $pid)"
                fi
            else
                log_warning "No PID file found"
            fi
            ;;
        docker)
            local container_name=${2:-"smartcampus-app"}
            if docker container inspect "$container_name" > /dev/null 2>&1; then
                docker stop "$container_name"
                docker rm "$container_name"
                log_success "Stopped and removed Docker container: $container_name"
            else
                log_warning "Container not found: $container_name"
            fi
            ;;
        docker-compose)
            local compose_file=${2:-"docker-compose.yml"}
            docker-compose -f "$compose_file" down
            log_success "Stopped Docker Compose services"
            ;;
        kubernetes)
            local namespace=${2:-"smartcampus"}
            kubectl scale deployment smartcampus-app --replicas=0 -n "$namespace"
            log_success "Scaled down Kubernetes deployment"
            ;;
        aws)
            local cluster_name=${2:-"smartcampus-cluster"}
            local service_name=${3:-"smartcampus-service"}
            aws ecs update-service \
                --cluster "$cluster_name" \
                --service "$service_name" \
                --desired-count 0 > /dev/null
            log_success "Stopped AWS ECS service"
            ;;
        *)
            log_error "Stop not supported for environment: $environment"
            exit 1
            ;;
    esac
}

show_status() {
    local environment=$1
    
    log_info "Checking status for environment: $environment"
    
    case $environment in
        local)
            if [ -f ".app.pid" ]; then
                local pid=$(cat .app.pid)
                if kill -0 $pid 2>/dev/null; then
                    log_success "Application running (PID: $pid)"
                    
                    # Check if responding
                    if curl -f -s "http://localhost:$DEFAULT_PORT$HEALTH_CHECK_ENDPOINT" > /dev/null; then
                        log_success "Application is healthy"
                    else
                        log_warning "Application not responding to health checks"
                    fi
                else
                    log_warning "Application not running (stale PID: $pid)"
                fi
            else
                log_warning "No PID file found - application may not be running"
            fi
            ;;
        docker)
            local container_name=${2:-"smartcampus-app"}
            if docker container inspect "$container_name" > /dev/null 2>&1; then
                local container_status=$(docker container inspect "$container_name" --format '{{.State.Status}}')
                log_info "Container status: $container_status"
                
                if [ "$container_status" = "running" ]; then
                    local health_status=$(docker container inspect "$container_name" --format '{{.State.Health.Status}}' 2>/dev/null || echo "unknown")
                    log_info "Health status: $health_status"
                fi
            else
                log_warning "Container not found: $container_name"
            fi
            ;;
        kubernetes)
            local namespace=${2:-"smartcampus"}
            kubectl get deployments -n "$namespace"
            kubectl get pods -n "$namespace"
            kubectl get services -n "$namespace"
            ;;
        aws)
            local cluster_name=${2:-"smartcampus-cluster"}
            local service_name=${3:-"smartcampus-service"}
            aws ecs describe-services \
                --cluster "$cluster_name" \
                --services "$service_name" \
                --query 'services[0].{Status:status,RunningCount:runningCount,DesiredCount:desiredCount,TaskDefinition:taskDefinition}'
            ;;
        *)
            log_error "Status check not supported for environment: $environment"
            exit 1
            ;;
    esac
}

show_logs() {
    local environment=$1
    local lines=${2:-100}
    
    log_info "Showing logs for environment: $environment (last $lines lines)"
    
    case $environment in
        local)
            if [ -f "logs/application.log" ]; then
                tail -n "$lines" logs/application.log
            else
                log_warning "Log file not found: logs/application.log"
            fi
            ;;
        docker)
            local container_name=${3:-"smartcampus-app"}
            docker logs --tail "$lines" "$container_name"
            ;;
        kubernetes)
            local namespace=${3:-"smartcampus"}
            kubectl logs deployment/smartcampus-app -n "$namespace" --tail="$lines"
            ;;
        aws)
            log_info "Use AWS CloudWatch to view ECS logs"
            log_info "aws logs tail /ecs/smartcampus --follow"
            ;;
        *)
            log_error "Log viewing not supported for environment: $environment"
            exit 1
            ;;
    esac
}

show_help() {
    echo "Usage: $0 [OPTIONS] COMMAND ENVIRONMENT [ARGS...]"
    echo ""
    echo "Commands:"
    echo "  deploy       Deploy application to specified environment"
    echo "  rollback     Rollback to previous version"
    echo "  stop         Stop running application"
    echo "  status       Show application status"
    echo "  logs         Show application logs"
    echo ""
    echo "Environments:"
    echo "  local        Deploy locally using JAR file"
    echo "  docker       Deploy using Docker container"
    echo "  compose      Deploy using Docker Compose"
    echo "  kubernetes   Deploy to Kubernetes cluster"
    echo "  aws          Deploy to AWS ECS"
    echo ""
    echo "Options:"
    echo "  -h, --help   Show this help message"
    echo "  -v, --verbose Enable verbose output"
    echo ""
    echo "Examples:"
    echo "  $0 deploy local target/smartcampus-1.0.0.jar prod 8080"
    echo "  $0 deploy docker smartcampus:latest smartcampus-app 8080"
    echo "  $0 deploy compose docker-compose.prod.yml production"
    echo "  $0 deploy kubernetes production k8s/production"
    echo "  $0 rollback docker previous"
    echo "  $0 status local"
    echo "  $0 logs docker 200 smartcampus-app"
    echo "  $0 stop kubernetes production"
}

# Main execution logic
main() {
    local command=""
    local environment=""
    local verbose=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            deploy|rollback|stop|status|logs)
                command="$1"
                shift
                ;;
            local|docker|compose|kubernetes|aws)
                environment="$1"
                shift
                break
                ;;
            *)
                if [ -z "$command" ]; then
                    log_error "Unknown command: $1"
                else
                    log_error "Unknown environment: $1"
                fi
                show_help
                exit 1
                ;;
        esac
    done
    
    if [ -z "$command" ] || [ -z "$environment" ]; then
        log_error "Command and environment are required"
        show_help
        exit 1
    fi
    
    if [ "$verbose" = true ]; then
        set -x
    fi
    
    print_banner
    check_prerequisites "$environment"
    
    # Execute command
    case $command in
        deploy)
            case $environment in
                local)
                    deploy_local "$@"
                    ;;
                docker)
                    deploy_docker "$@"
                    ;;
                compose)
                    deploy_docker_compose "$@"
                    ;;
                kubernetes)
                    deploy_kubernetes "$@"
                    ;;
                aws)
                    deploy_aws_ecs "$@"
                    ;;
            esac
            ;;
        rollback)
            rollback_deployment "$environment" "$@"
            ;;
        stop)
            stop_application "$environment" "$@"
            ;;
        status)
            show_status "$environment" "$@"
            ;;
        logs)
            show_logs "$environment" "$@"
            ;;
        *)
            log_error "Unknown command: $command"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"