#!/bin/bash

# Setup script for booking-query-handler-postgres databases
# This script:
# 1. Starts PostgreSQL using Docker Compose
# 2. Creates the booking_projection database
# 3. Verifies the databases are ready

set -e

echo "========================================="
echo "Database Setup for booking-query-handler-postgres"
echo "========================================="

# Navigate to .docker directory
cd "$(dirname "$0")/../.docker"

# Start PostgreSQL
echo ""
echo "Starting PostgreSQL container..."
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo ""
echo "Waiting for PostgreSQL to be ready..."
sleep 5

# Check if PostgreSQL is ready
until docker exec postgres pg_isready -U postgres > /dev/null 2>&1; do
  echo "Waiting for PostgreSQL..."
  sleep 2
done

echo ""
echo "PostgreSQL is ready!"

# Create booking_projection database if it doesn't exist
echo ""
echo "Creating booking_projection database..."
docker exec postgres psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'booking_projection'" | grep -q 1 || \
  docker exec postgres psql -U postgres -c "CREATE DATABASE booking_projection;"

echo ""
echo "Verifying databases..."
docker exec postgres psql -U postgres -c "\l" | grep -E "(eventstore|booking_projection)"

echo ""
echo "========================================="
echo "✅ Database setup complete!"
echo "========================================="
echo ""
echo "Available databases:"
echo "  - eventstore (Event Store)"
echo "  - booking_projection (PostgreSQL Projection)"
echo ""
echo "You can now start the application:"
echo "  cd booking-query-handler-postgres"
echo "  mvn spring-boot:run"
echo ""
