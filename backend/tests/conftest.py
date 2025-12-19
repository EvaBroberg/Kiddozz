import os

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

# Set test environment BEFORE importing app
os.environ["APP_ENV"] = "test"
os.environ["ENVIRONMENT"] = "test"
os.environ["SECRET_KEY"] = "test-secret-key"

from app.core.database import Base, get_db
from app.core.database import engine as app_engine
from app.core.security import create_access_token
from app.main import app

# Create test database
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
test_engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)


def override_get_db():
    try:
        db = TestingSessionLocal()
        yield db
    finally:
        db.close()


app.dependency_overrides[get_db] = override_get_db

client = TestClient(app)


@pytest.fixture(scope="session", autouse=True)
def _print_active_db():
    """Print the active database dialect and URL at test session start."""
    print(f"\n[TEST DB] dialect={app_engine.dialect.name} url={app_engine.url}")


@pytest.fixture(scope="session", autouse=True)
def setup_test_db():
    """Set up test database schema using SQLAlchemy metadata (SQLite-compatible)"""
    # Create all tables using SQLAlchemy metadata
    # This is more reliable for SQLite than running PostgreSQL-specific migrations
    Base.metadata.create_all(bind=test_engine)
    yield
    # Clean up test database after all tests
    if os.path.exists("test.db"):
        os.remove("test.db")


@pytest.fixture(scope="function", autouse=True)
def clean_db():
    """Clean database data before each test - automatically used by all tests"""
    # Clear all data but keep schema
    with test_engine.connect() as conn:
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


@pytest.fixture
def seeded_daycare_id():
    """Fixture that seeds the database and returns the daycare ID."""
    from app.models.daycare import Daycare
    from app.services.educator_service import insert_dummy_educators
    from app.services.seeder import seed_daycare_data

    db = TestingSessionLocal()
    try:
        # Seed the database
        seed_daycare_data(db)
        insert_dummy_educators(db)

        # Get the daycare ID
        daycare = db.query(Daycare).first()
        return str(daycare.id)
    finally:
        db.close()


@pytest.fixture
def make_token():
    """Helper fixture to create valid JWT tokens for testing."""

    def _make_token(
        user_id: str,
        role: str,
        daycare_id: str = "default-daycare-id",
        groups: list = None,
    ):
        if groups is None:
            groups = []
        data = {
            "sub": user_id,
            "role": role,
            "daycare_id": daycare_id,
            "groups": groups,
        }
        return create_access_token(data)

    return _make_token
