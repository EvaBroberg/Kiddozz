# Kiddozz Backend API

A FastAPI-based backend service for managing events and images with PostgreSQL database and AWS S3 integration.

## Features

- **Event Management**: Create, read, update, and delete events
- **Image Management**: Upload and manage images for events using AWS S3
- **Pre-signed URLs**: Secure image upload/download using S3 pre-signed URLs
- **Database**: PostgreSQL with SQLAlchemy ORM
- **Migrations**: Alembic for database schema management
- **API Documentation**: Automatic OpenAPI/Swagger documentation

## Tech Stack

- **FastAPI**: Modern, fast web framework for building APIs
- **PostgreSQL**: Relational database
- **SQLAlchemy**: Python SQL toolkit and ORM
- **Alembic**: Database migration tool
- **AWS S3**: Object storage for images
- **Boto3**: AWS SDK for Python
- **Pydantic**: Data validation using Python type annotations

## Project Structure

```
backend/
├── app/
│   ├── api/                 # API routes
│   │   ├── __init__.py
│   │   └── events.py        # Event-related endpoints
│   ├── core/                # Core configuration
│   │   ├── __init__.py
│   │   ├── config.py        # Application settings
│   │   └── database.py      # Database configuration
│   ├── models/              # Database models and schemas
│   │   ├── __init__.py
│   │   ├── event.py         # SQLAlchemy models
│   │   └── schemas.py       # Pydantic schemas
│   ├── services/            # Business logic
│   │   ├── __init__.py
│   │   ├── event_service.py # Event business logic
│   │   └── s3_service.py    # S3 operations
│   ├── utils/               # Utility functions
│   │   └── __init__.py
│   └── main.py              # FastAPI application
├── alembic/                 # Database migrations
│   ├── versions/
│   ├── env.py
│   └── script.py.mako
├── alembic.ini              # Alembic configuration
├── requirements.txt         # Python dependencies
├── env.example              # Environment variables template
└── README.md               # This file
```

## Setup Instructions

### 1. Prerequisites

- Python 3.8+
- PostgreSQL 12+
- AWS Account (for S3)
- Git

### 2. Clone and Install

```bash
# Navigate to backend directory
cd backend

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### 3. Database Setup

```bash
# Create PostgreSQL database
createdb kiddozz_demo

# Or using psql:
psql -U postgres
CREATE DATABASE kiddozz_demo;
```

### 4. Environment Configuration

The project supports multiple environments with separate configuration files:

```bash
# Environment-specific configuration files
.env.local      # Local development
.env.staging    # Staging environment  
.env.prod       # Production environment

# Copy environment template for reference
cp env.example .env
```

Each environment file should contain the appropriate settings for that environment. The startup scripts will automatically load the correct configuration based on the `APP_ENV` variable.

Required environment variables:
```env
# Database
DATABASE_URL=postgresql://username:password@localhost:5432/kiddozz_demo
DB_HOST=localhost
DB_PORT=5432
DB_NAME=kiddozz_demo
DB_USER=your_username
DB_PASSWORD=your_password

# AWS S3
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=us-east-1
S3_BUCKET_NAME=kiddozz-images

# Application
SECRET_KEY=your-secret-key-here
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30

# CORS
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
```

### 5. Database Migrations

```bash
# Initialize Alembic (if not already done)
alembic init alembic

# Create initial migration
alembic revision --autogenerate -m "Initial migration"

# Apply migrations
alembic upgrade head
```

### 6. Run the Application

#### Quick Start with Environment Scripts

The easiest way to run the backend is using the provided startup scripts:

```bash
# Local development (with auto-reload)
./start_local.sh

# Staging environment
./start_staging.sh

# Production environment
./start_prod.sh
```

Each script will:
- Set the appropriate `APP_ENV` environment variable
- Run database migrations automatically
- Display the environment and DATABASE_URL being used
- Start the FastAPI server with the correct configuration

#### Quick Command (Recommended)

For even faster startup, you can use the `run` command after setting it up:

```bash
# First time setup (add to your ~/.zshrc)
echo 'run() {
  if [ "$1" = "local" ]; then
    ./start_local.sh
  elif [ "$1" = "staging" ]; then
    ./start_staging.sh
  elif [ "$1" = "prod" ]; then
    ./start_prod.sh
  else
    echo "Usage: run {local|staging|prod}"
  fi
}' >> ~/.zshrc

# Reload your shell configuration
source ~/.zshrc

# Now you can simply run:
run local      # Start local development server
run staging    # Start staging server
run prod       # Start production server
```

**Note**: After adding the function to `~/.zshrc`, restart your terminal or run `source ~/.zshrc` to make the `run` command available.

#### Manual Startup

```bash
# Development server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Or using Python
python -m app.main
```

The API will be available at:
- **API**: http://localhost:8000
- **Documentation**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API Endpoints

### Events

- `GET /api/v1/events/` - Get all events
- `GET /api/v1/events/upcoming` - Get upcoming events
- `GET /api/v1/events/past` - Get past events
- `GET /api/v1/events/{event_id}` - Get specific event
- `POST /api/v1/events/` - Create new event
- `PUT /api/v1/events/{event_id}` - Update event
- `DELETE /api/v1/events/{event_id}` - Delete event

### Images

- `GET /api/v1/events/{event_id}/images` - Get event images
- `POST /api/v1/events/{event_id}/images/presigned-url` - Get upload URL
- `POST /api/v1/events/{event_id}/images/confirm` - Confirm upload
- `DELETE /api/v1/events/images/{image_id}` - Delete image

### Identity Management

- `GET /api/v1/educators` - List educators (requires `daycare_id` in production)
- `GET /api/v1/parents` - List parents (requires `daycare_id` in production)
- `POST /api/v1/auth/dev-login` - Development login endpoint (disabled in production)

#### Query Parameters

**Educators endpoint:**
- `daycare_id` (required in production) - Filter by daycare
- `group` (optional) - Filter by group name
- `search` (optional) - Search by educator name

**Parents endpoint:**
- `daycare_id` (required in production) - Filter by daycare
- `search` (optional) - Search by parent name or email

**Dev-login endpoint:**
- `educator_id` (optional) - Login as educator
- `parent_id` (optional) - Login as parent

**Note:** The dev-login endpoint is only available in development and staging environments. It is disabled in production for security reasons.

## Usage Examples

### Create an Event

```bash
curl -X POST "http://localhost:8000/api/v1/events/" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Festival",
    "description": "Join us for a fun-filled day!",
    "date": "2024-05-10T10:00:00",
    "start_time": "10:00",
    "location": "Main Hall"
  }'
```

### Get Pre-signed Upload URL

```bash
curl -X POST "http://localhost:8000/api/v1/events/1/images/presigned-url" \
  -H "Content-Type: application/json" \
  -d '{
    "file_name": "festival.jpg",
    "file_size": 1024000,
    "mime_type": "image/jpeg",
    "event_id": 1
  }'
```

### Upload Image to S3

```bash
# Use the presigned URL from previous response
curl -X PUT "PRESIGNED_URL_FROM_RESPONSE" \
  -H "Content-Type: image/jpeg" \
  --data-binary @festival.jpg
```

### Confirm Image Upload

```bash
curl -X POST "http://localhost:8000/api/v1/events/1/images/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "s3_key": "events/1/uuid-filename.jpg",
    "file_name": "festival.jpg",
    "file_size": 1024000,
    "mime_type": "image/jpeg"
  }'
```

### List Educators

```bash
# Get all educators for a daycare
curl "http://localhost:8000/api/v1/educators?daycare_id=your-daycare-id"

# Search for educators by name
curl "http://localhost:8000/api/v1/educators?daycare_id=your-daycare-id&search=jessica"

# Filter by group
curl "http://localhost:8000/api/v1/educators?daycare_id=your-daycare-id&group=Group%20A"
```

### List Parents

```bash
# Get all parents for a daycare
curl "http://localhost:8000/api/v1/parents?daycare_id=your-daycare-id"

# Search for parents by name or email
curl "http://localhost:8000/api/v1/parents?daycare_id=your-daycare-id&search=sara"
```

### Development Login

```bash
# Login as educator
curl -X POST "http://localhost:8000/api/v1/auth/dev-login" \
  -H "Content-Type: application/json" \
  -d '{"educator_id": "27"}'

# Login as parent
curl -X POST "http://localhost:8000/api/v1/auth/dev-login" \
  -H "Content-Type: application/json" \
  -d '{"parent_id": "10"}'
```

## Development

### Running Tests

```bash
# Install test dependencies
pip install pytest pytest-asyncio httpx

# Run tests
pytest
```

### Database Migrations

Migrations are managed with **Alembic** to keep the database schema in sync with the SQLAlchemy models in `app/models/`.

- Create a new migration (after changing models):
  alembic revision --autogenerate -m "Describe your change"

- Apply migrations:
  alembic upgrade head

- Rollback the last migration:
  alembic downgrade -1

Always review the generated migration script in alembic/versions/. Migration scripts must be committed to Git so all environments (local, CI, production) stay consistent.

### Code Formatting

```bash
# Install formatting tools
pip install black isort

# Format code
black app/
isort app/
```

## Deployment

### Using Docker

```bash
# Build image
docker build -t kiddozz-backend .

# Run container
docker run -p 8000:8000 --env-file .env kiddozz-backend
```

### Environment Variables for Production

Make sure to set these in your production environment:
- `DATABASE_URL`: Production PostgreSQL URL
- `AWS_ACCESS_KEY_ID`: Production AWS credentials
- `AWS_SECRET_ACCESS_KEY`: Production AWS credentials
- `S3_BUCKET_NAME`: Production S3 bucket
- `SECRET_KEY`: Strong secret key for JWT tokens
- `ALLOWED_ORIGINS`: Production frontend URLs

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Check PostgreSQL is running
   - Verify database credentials in `.env`
   - Ensure database exists

2. **S3 Upload Fails**
   - Verify AWS credentials
   - Check S3 bucket exists and is accessible
   - Verify bucket permissions

3. **CORS Issues**
   - Add your frontend URL to `ALLOWED_ORIGINS`
   - Check CORS middleware configuration

### Logs

```bash
# View application logs
tail -f logs/app.log

# Database logs
tail -f logs/db.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
