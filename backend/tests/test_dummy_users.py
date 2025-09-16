import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.core.database import Base
from app.core.security import decode_access_token
from app.models.user import User, UserRole
from app.services.user_service import create_dummy_users

# Use the same test database as other tests
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="module")
def setup_database():
    """Create test database tables"""
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


@pytest.fixture
def db_session():
    """Create a test database session."""
    session = TestingSessionLocal()

    try:
        yield session
    finally:
        session.close()


class TestDummyUsers:
    """Test dummy users functionality."""

    def test_dummy_users_created(self, setup_database, db_session):
        """Test that 3 dummy users are created with correct roles."""
        # Create dummy users
        create_dummy_users(db_session)

        # Query all users
        users = db_session.query(User).all()

        # Should have 3 users
        assert len(users) == 3

        # Check specific users exist with correct roles
        jessica = db_session.query(User).filter(User.name == "Jessica").first()
        sara = db_session.query(User).filter(User.name == "Sara").first()
        mervi = db_session.query(User).filter(User.name == "Mervi").first()

        assert jessica is not None
        assert jessica.role == UserRole.EDUCATOR.value

        assert sara is not None
        assert sara.role == UserRole.PARENT.value

        assert mervi is not None
        assert mervi.role == UserRole.SUPER_EDUCATOR.value

    def test_dummy_jwts_valid(self, setup_database, db_session):
        """Test that JWT tokens are valid and contain correct role claims."""
        # Create dummy users
        create_dummy_users(db_session)

        # Get all users
        users = db_session.query(User).all()

        for user in users:
            # Verify JWT token exists
            assert user.jwt_token is not None

            # Decode JWT token
            decoded_payload = decode_access_token(user.jwt_token)

            # Verify JWT contains correct claims
            assert "sub" in decoded_payload
            assert "role" in decoded_payload
            assert "user_id" in decoded_payload

            # Verify role in JWT matches database role
            assert decoded_payload["role"] == user.role
            assert decoded_payload["sub"] == user.name

    def test_dummy_users_not_duplicated(self, setup_database, db_session):
        """Test that calling create_dummy_users twice doesn't create duplicates."""
        # Create dummy users first time
        create_dummy_users(db_session)

        # Count users after first creation
        first_count = db_session.query(User).count()
        assert first_count == 3

        # Create dummy users second time
        create_dummy_users(db_session)

        # Count users after second creation
        second_count = db_session.query(User).count()

        # Should still be 3 users (no duplicates)
        assert second_count == 3
        assert first_count == second_count

        # Verify specific users still exist
        jessica = db_session.query(User).filter(User.name == "Jessica").first()
        sara = db_session.query(User).filter(User.name == "Sara").first()
        mervi = db_session.query(User).filter(User.name == "Mervi").first()

        assert jessica is not None
        assert sara is not None
        assert mervi is not None
