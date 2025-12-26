.PHONY: help build docker-build docker-up docker-down clean run stop logs ps

# Variables
PROJECT_NAME=sensor-reactive
DOCKER_IMAGE=$(PROJECT_NAME):latest
DOCKER_CONTAINER=$(PROJECT_NAME)-app

# Default target
help:
	@echo "=== Sensor Reactive System - Available Commands ==="
	@echo ""
	@echo "Build & Development:"
	@echo "  make build          - Build Maven project (compile and package)"
	@echo "  make clean          - Clean build artifacts and Docker images"
	@echo "  make run            - Run application locally (requires Java 17+ and PostgreSQL)"
	@echo ""
	@echo "Docker Management:"
	@echo "  make docker-build   - Build Docker image"
	@echo "  make docker-up      - Start application with Docker Compose"
	@echo "  make docker-down    - Stop and remove Docker containers"
	@echo "  make docker-logs    - View Docker container logs"
	@echo "  make ps             - List running Docker containers"
	@echo ""
	@echo "Utilities:"
	@echo "  make help           - Show this help message"
	@echo ""

# Build Maven project
build:
	@echo "Building Maven project..."
	mvn clean package -DskipTests
	@echo "Build completed successfully!"

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t $(DOCKER_IMAGE) .
	@echo "Docker image built successfully!"

# Start Docker Compose
docker-up:
	@echo "Starting Docker Compose..."
	docker-compose up -d
	@echo "Waiting for services to be healthy..."
	@sleep 5
	@docker-compose ps
	@echo "Docker services started! Access application at http://localhost:8080"

# Stop Docker Compose
docker-down:
	@echo "Stopping Docker Compose..."
	docker-compose down -v
	@echo "Docker services stopped!"

# View Docker logs
docker-logs:
	docker-compose logs -f app

# List running containers
ps:
	docker-compose ps

# Stop running containers
stop:
	@echo "Stopping containers..."
	docker-compose stop
	@echo "Containers stopped!"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	docker-compose down -v 2>/dev/null || true
	docker rmi $(DOCKER_IMAGE) 2>/dev/null || true
	rm -rf target/
	@echo "Clean completed!"

# Run application locally
run:
	@echo "Starting PostgreSQL (if using Docker)..."
	docker-compose up -d postgres
	@echo "Waiting for PostgreSQL..."
	@sleep 3
	@echo "Starting Spring Boot application..."
	mvn spring-boot:run

# Check if PostgreSQL is running
check-db:
	@echo "Checking PostgreSQL connection..."
	docker-compose exec postgres pg_isready -U postgres || echo "PostgreSQL is not running"

# Test sensor endpoints
test-endpoints:
	@echo "Testing sensor endpoints..."
	@echo "\n1. Testing Server Stream (single sensor):"
	@curl -i "http://localhost:8080/api/sensors/stream?sensorId=1&limit=3"
	@echo "\n\n2. Testing Client Stream:"
	@curl -i "http://localhost:8080/api/client/sensors?sensorId=1&limit=3"

# View application logs
logs:
	docker-compose logs -f app

# Health check
health:
	@echo "Checking application health..."
	curl -s http://localhost:8080/actuator/health | python -m json.tool || echo "Application is not responding"

# Reset database
reset-db:
	@echo "Resetting database..."
	docker-compose down -v
	docker-compose up -d postgres
	@echo "Database reset completed!"
