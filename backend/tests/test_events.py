import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.main import app
from app.core.database import get_db, Base
import os

# Test database URL (using SQLite for testing)
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def override_get_db():
    try:
        db = TestingSessionLocal()
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

client = TestClient(app)

@pytest.fixture(scope="module")
def setup_database():
    """Create test database tables"""
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)

def test_create_event(setup_database):
    """Test creating a new event"""
    event_data = {
        "title": "Test Event",
        "description": "A test event description",
        "date": "2024-01-15T10:00:00",
        "start_time": "10:00",
        "location": "Test Location"
    }
    
    response = client.post("/api/v1/events/", json=event_data)
    assert response.status_code == 201
    
    data = response.json()
    assert data["title"] == "Test Event"
    assert data["description"] == "A test event description"
    assert data["location"] == "Test Location"
    assert "id" in data

def test_get_events(setup_database):
    """Test retrieving all events"""
    response = client.get("/api/v1/events/")
    assert response.status_code == 200
    
    data = response.json()
    assert isinstance(data, list)

def test_get_event_by_id(setup_database):
    """Test retrieving a specific event by ID"""
    # First create an event
    event_data = {
        "title": "Test Event for ID",
        "description": "Test description",
        "date": "2024-01-15T10:00:00",
        "start_time": "10:00",
        "location": "Test Location"
    }
    
    create_response = client.post("/api/v1/events/", json=event_data)
    event_id = create_response.json()["id"]
    
    # Then retrieve it
    response = client.get(f"/api/v1/events/{event_id}")
    assert response.status_code == 200
    
    data = response.json()
    assert data["title"] == "Test Event for ID"
    assert data["id"] == event_id

def test_update_event(setup_database):
    """Test updating an event"""
    # First create an event
    event_data = {
        "title": "Original Title",
        "description": "Original description",
        "date": "2024-01-15T10:00:00",
        "start_time": "10:00",
        "location": "Original Location"
    }
    
    create_response = client.post("/api/v1/events/", json=event_data)
    event_id = create_response.json()["id"]
    
    # Update the event
    update_data = {
        "title": "Updated Title",
        "description": "Updated description",
        "location": "Updated Location"
    }
    
    response = client.put(f"/api/v1/events/{event_id}", json=update_data)
    assert response.status_code == 200
    
    data = response.json()
    assert data["title"] == "Updated Title"
    assert data["description"] == "Updated description"
    assert data["location"] == "Updated Location"

def test_delete_event(setup_database):
    """Test deleting an event"""
    # First create an event
    event_data = {
        "title": "Event to Delete",
        "description": "This event will be deleted",
        "date": "2024-01-15T10:00:00",
        "start_time": "10:00",
        "location": "Test Location"
    }
    
    create_response = client.post("/api/v1/events/", json=event_data)
    event_id = create_response.json()["id"]
    
    # Delete the event
    response = client.delete(f"/api/v1/events/{event_id}")
    assert response.status_code == 204
    
    # Verify it's deleted
    get_response = client.get(f"/api/v1/events/{event_id}")
    assert get_response.status_code == 404

def test_get_upload_url(setup_database):
    """Test getting upload URL for event image"""
    # First create an event
    event_data = {
        "title": "Event with Image",
        "description": "This event will have an image",
        "date": "2024-01-15T10:00:00",
        "start_time": "10:00",
        "location": "Test Location"
    }
    
    create_response = client.post("/api/v1/events/", json=event_data)
    event_id = create_response.json()["id"]
    
    # Get upload URL
    response = client.post(f"/api/v1/events/{event_id}/images?filename=test_image.jpg")
    assert response.status_code == 200
    
    data = response.json()
    assert "upload_url" in data
    assert "image_id" in data
    assert data["upload_url"].startswith("https://")

def test_get_upload_url_nonexistent_event(setup_database):
    """Test getting upload URL for non-existent event"""
    response = client.post("/api/v1/events/999/images?filename=test_image.jpg")
    assert response.status_code == 200
    assert "error" in response.json()
    assert response.json()["error"] == "Event not found"


# Async tests
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_healthcheck(setup_database):
    """Basic test to ensure the app starts and responds."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        response = await ac.get("/docs")  # docs route always exists
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_create_event(setup_database):
    """Test event creation without images (baseline)."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        payload = {
            "title": "Test Event",
            "date": "2025-09-10",
            "startTime": "10:00",
            "description": "This is a test event"
        }
        response = await ac.post("/api/v1/events/", json=payload)
    assert response.status_code == 200 or response.status_code == 201
    data = response.json()
    assert data["title"] == "Test Event"
    assert "id" in data
