#!/bin/bash

# ===========================================
# Kiddozz Backend - Production Environment
# ===========================================

# Set environment
export APP_ENV=prod

# Print banner
echo "🚀 Starting Kiddozz Backend - PRODUCTION Environment"
echo "===================================================="
echo "Environment: PRODUCTION"
echo "APP_ENV: $APP_ENV"

# Load environment variables to show DATABASE_URL
source .env.prod 2>/dev/null || echo "⚠️  Warning: .env.prod not found, using defaults"
echo "DATABASE_URL: ${DATABASE_URL:-'Using default from config'}"
echo "===================================================="
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
PORT=${PORT:-8002}
echo ""
echo "🌐 Starting FastAPI server on http://0.0.0.0:$PORT"
echo "🏭 Production environment - no auto-reload"
echo ""

# Start the server
poetry run uvicorn app.main:app --host 0.0.0.0 --port $PORT
