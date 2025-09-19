#!/bin/bash

# ===========================================
# Kiddozz Backend - Staging Environment
# ===========================================

# Set environment
export APP_ENV=staging

# Print banner
echo "🚀 Starting Kiddozz Backend - STAGING Environment"
echo "================================================="
echo "Environment: STAGING"
echo "APP_ENV: $APP_ENV"

# Load environment variables to show DATABASE_URL
source .env.staging 2>/dev/null || echo "⚠️  Warning: .env.staging not found, using defaults"
echo "DATABASE_URL: ${DATABASE_URL:-'Using default from config'}"
echo "================================================="
echo ""

# Run database migrations
echo "🔄 Running database migrations..."
poetry run alembic upgrade head

if [ $? -eq 0 ]; then
    echo "✅ Database migrations completed successfully"
else
    echo "❌ Database migrations failed"
    exit 1
fi

# Determine port
PORT=${PORT:-8001}
echo ""
echo "🌐 Starting FastAPI server on http://0.0.0.0:$PORT"
echo "🔧 Staging environment - no auto-reload"
echo ""

# Start the server
poetry run uvicorn app.main:app --host 0.0.0.0 --port $PORT
