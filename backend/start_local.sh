#!/bin/bash

# ===========================================
# Kiddozz Backend - Local Development
# ===========================================

# Set environment
export APP_ENV=local
export MESSAGING_BACKEND=true

# Print banner
echo "🚀 Starting Kiddozz Backend - LOCAL Environment"
echo "================================================"
echo "Environment: LOCAL"
echo "APP_ENV: $APP_ENV"

# Load environment variables to show DATABASE_URL
source .env.local 2>/dev/null || echo "⚠️  Warning: .env.local not found, using defaults"
echo "DATABASE_URL: ${DATABASE_URL:-'Using default from config'}"
echo "================================================"
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

echo ""
echo "🌐 Starting FastAPI server on http://0.0.0.0:8000"
echo "📱 Android emulator should connect to: http://10.0.2.2:8000"
echo "🔄 Auto-reload enabled for development"
echo ""

# Start the server
poetry run uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
