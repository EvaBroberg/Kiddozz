#!/bin/bash

# Kiddozz Backend Startup Script

echo "🚀 Starting Kiddozz Backend API..."

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python -m venv venv
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "📥 Installing dependencies..."
pip install -r requirements.txt

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "⚠️  .env file not found. Copying from env.example..."
    cp env.example .env
    echo "📝 Please edit .env file with your configuration before running again."
    exit 1
fi

# Check if database is accessible
echo "🔍 Checking database connection..."
python -c "
import os
from sqlalchemy import create_engine
from app.core.config import settings

try:
    engine = create_engine(settings.database_url)
    with engine.connect() as conn:
        print('✅ Database connection successful')
except Exception as e:
    print(f'❌ Database connection failed: {e}')
    print('Please check your database configuration in .env file')
    exit(1)
"

# Run database migrations
echo "🗄️  Running database migrations..."
alembic upgrade head

# Start the application
echo "🌟 Starting FastAPI server..."
echo "📖 API Documentation: http://localhost:8000/docs"
echo "🔗 API Base URL: http://localhost:8000"
echo ""

uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
