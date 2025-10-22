#!/bin/bash

# ELK Stack Deployment Script for Podman
# This script sets up and manages the ELK stack for the Delight Account Service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ELK_COMPOSE_FILE="elk-podman-compose.yaml"
LOG_DIR="logs"
ELK_CONFIG_DIR="elk-config"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Podman is installed
check_podman() {
    if ! command -v podman &> /dev/null; then
        print_error "Podman is not installed. Please install Podman first."
        exit 1
    fi
    print_success "Podman is available"
}

# Function to check if podman-compose is available
check_podman_compose() {
    if ! command -v podman-compose &> /dev/null; then
        print_warning "podman-compose not found. Installing podman-compose..."
        # Install podman-compose if not available
        if command -v pip3 &> /dev/null; then
            pip3 install podman-compose
        else
            print_error "pip3 not found. Please install podman-compose manually."
            exit 1
        fi
    fi
    print_success "podman-compose is available"
}

# Function to create necessary directories
setup_directories() {
    print_status "Setting up directories..."
    
    # Create log directory if it doesn't exist
    if [ ! -d "$LOG_DIR" ]; then
        mkdir -p "$LOG_DIR"
        print_success "Created log directory: $LOG_DIR"
    fi
    
    # Create ELK config directories
    mkdir -p "$ELK_CONFIG_DIR/logstash/pipeline"
    mkdir -p "$ELK_CONFIG_DIR/logstash/config"
    mkdir -p "$ELK_CONFIG_DIR/filebeat"
    
    print_success "Directory structure ready"
}

# Function to start ELK stack
start_elk() {
    print_status "Starting ELK stack..."
    
    # Check if containers are already running
    if podman ps --format "table {{.Names}}" | grep -q "delight-elasticsearch\|delight-logstash\|delight-kibana\|delight-filebeat"; then
        print_warning "Some ELK containers are already running. Stopping them first..."
        stop_elk
    fi
    
    # Start the ELK stack
    podman-compose -f "$ELK_COMPOSE_FILE" up -d
    
    print_success "ELK stack started"
    print_status "Waiting for services to be ready..."
    
    # Wait for Elasticsearch to be ready
    print_status "Waiting for Elasticsearch..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
            print_success "Elasticsearch is ready"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        print_error "Elasticsearch failed to start within 60 seconds"
        exit 1
    fi
    
    # Wait for Kibana to be ready
    print_status "Waiting for Kibana..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:5601/api/status > /dev/null 2>&1; then
            print_success "Kibana is ready"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        print_warning "Kibana may not be fully ready yet"
    fi
    
    print_success "ELK stack is running!"
    print_status "Access points:"
    echo "  - Elasticsearch: http://localhost:9200"
    echo "  - Kibana: http://localhost:5601"
    echo "  - Logstash: http://localhost:9600"
}

# Function to stop ELK stack
stop_elk() {
    print_status "Stopping ELK stack..."
    podman-compose -f "$ELK_COMPOSE_FILE" down
    print_success "ELK stack stopped"
}

# Function to show ELK stack status
status_elk() {
    print_status "ELK stack status:"
    podman-compose -f "$ELK_COMPOSE_FILE" ps
}

# Function to show logs
logs_elk() {
    print_status "ELK stack logs:"
    podman-compose -f "$ELK_COMPOSE_FILE" logs -f
}

# Function to clean up ELK stack
cleanup_elk() {
    print_status "Cleaning up ELK stack..."
    podman-compose -f "$ELK_COMPOSE_FILE" down -v
    print_success "ELK stack cleaned up"
}

# Function to show help
show_help() {
    echo "ELK Stack Management Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start     Start the ELK stack"
    echo "  stop      Stop the ELK stack"
    echo "  restart   Restart the ELK stack"
    echo "  status    Show ELK stack status"
    echo "  logs      Show ELK stack logs"
    echo "  cleanup   Stop and remove all containers and volumes"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start"
    echo "  $0 status"
    echo "  $0 logs"
}

# Main script logic
case "${1:-help}" in
    start)
        check_podman
        check_podman_compose
        setup_directories
        start_elk
        ;;
    stop)
        stop_elk
        ;;
    restart)
        stop_elk
        sleep 5
        start_elk
        ;;
    status)
        status_elk
        ;;
    logs)
        logs_elk
        ;;
    cleanup)
        cleanup_elk
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
