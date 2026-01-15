import os
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

# Set test environment BEFORE importing app
os.environ["APP_ENV"] = "test"
os.environ["ENVIRONMENT"] = "test"
os.environ["SECRET_KEY"] = "test-secret-key"
# Ensure routers registered behind feature flags are included in the test app.
# (Messaging tests expect /api/messaging/* to exist; otherwise they 404 at routing.)
os.environ["MESSAGING_BACKEND"] = "true"

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


@pytest.fixture
def auth_token_via_invite():
    """
    Helper fixture that creates and accepts an invite, returning a JWT access token.

    This is intended to gradually migrate tests away from dev-only auth shortcuts.
    """
    from app.core.roles import Role
    from app.models.daycare import Daycare
    from app.services.invite_token_service import generate_invite_token

    def _factory(
        *,
        role: str,
        daycare_id: str | None = None,
        name: str = "Test User",
        phone_num: str | None = None,
        email: str | None = None,
    ) -> dict:
        db = TestingSessionLocal()
        try:
            if daycare_id is None:
                daycare = Daycare(name=f"Test Daycare {uuid4().hex[:8]}")
                db.add(daycare)
                db.commit()
                db.refresh(daycare)
                daycare_id = str(daycare.id)

            if email is None:
                email = f"test+{uuid4().hex}@example.com"

            role_str = Role.from_str(role).value
            if role_str == Role.PARENT.value and phone_num is None:
                phone_num = "+358000000000"

            token_row, token_str = generate_invite_token(
                db,
                daycare_id=daycare_id,
                role=role_str,
                email=email,
                ttl_minutes=60,
                created_by="test-fixture",
            )

            # SQLite returns timezone-naive datetimes even for timezone=True columns.
            # The current validate logic compares naive expires_at to local naive now,
            # so ensure expires_at is set in local time to avoid false-expired tokens.
            if token_row.expires_at.tzinfo is None:
                from datetime import datetime, timedelta

                token_row.expires_at = datetime.now() + timedelta(minutes=60)
                db.commit()

            payload = {"token": token_str, "name": name}
            if phone_num is not None:
                payload["phone_num"] = phone_num

            res = client.post("/api/v1/auth/accept-invite", json=payload)
            assert res.status_code == 200, res.text
            data = res.json()

            return {
                "access_token": data["access_token"],
                "token_type": data["token_type"],
                "user_id": data["user_id"],
                "role": data["role"],
                "daycare_id": data["daycare_id"],
                "email": email,
            }
        finally:
            db.close()

    return _factory
