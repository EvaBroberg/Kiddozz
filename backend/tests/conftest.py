import os

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from app.core.database import Base, get_db
from app.main import app

# Set test environment
os.environ["APP_ENV"] = "test"

# Create test database
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


@pytest.fixture(scope="session", autouse=True)
def setup_test_db():
    """Set up test database schema using SQLAlchemy metadata (SQLite-compatible)"""
    # Create all tables using SQLAlchemy metadata
    # This is more reliable for SQLite than running PostgreSQL-specific migrations
    Base.metadata.create_all(bind=engine)
    yield
    # Clean up test database after all tests
    if os.path.exists("test.db"):
        os.remove("test.db")


@pytest.fixture(scope="function", autouse=True)
def clean_db():
    """Clean database data before each test - automatically used by all tests"""
    # Clear all data but keep schema
    with engine.connect() as conn:
        # Get all table names
        result = conn.execute(
            text(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'alembic_%'"
            )
        )
        tables = [row[0] for row in result]

        # Clear all tables
        for table in tables:
            conn.execute(text(f"DELETE FROM {table}"))
        conn.commit()
    yield
