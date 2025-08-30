#!/bin/bash

# OrgoLink Auth Development Startup Script

echo "🚀 Starting OrgoLink Auth Development Environment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Start development databases
echo "📦 Starting development databases..."
docker-compose -f docker-compose.dev.yml up -d

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
until docker exec orgolink-auth-postgres-dev pg_isready -U orgolink -d orgolink_auth > /dev/null 2>&1; do
    echo "   PostgreSQL is starting up..."
    sleep 2
done

echo "✅ PostgreSQL is ready!"

# Wait for Redis to be ready
echo "⏳ Waiting for Redis to be ready..."
until docker exec orgolink-auth-redis-dev redis-cli ping > /dev/null 2>&1; do
    echo "   Redis is starting up..."
    sleep 1
done

echo "✅ Redis is ready!"

echo ""
echo "🎉 Development environment is ready!"
echo ""
echo "📋 Available services:"
echo "   • PostgreSQL: localhost:5432 (orgolink/orgolink123)"
echo "   • Redis: localhost:6379"
echo "   • pgAdmin: http://localhost:5050 (admin@orgolink.dev/admin123)"
echo ""
echo "🔧 To start your Spring Boot application:"
echo "   ./gradlew bootRun --args='--spring.profiles.active=dev'"
echo ""
echo "🛑 To stop the development environment:"
echo "   docker-compose -f docker-compose.dev.yml down"
echo ""
