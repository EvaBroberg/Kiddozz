import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.core.database import Base
from app.core.security import decode_access_token
from app.models.user import User, UserRole
from app.services.user_service import insert_dummy_users

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


class TestDummyUsersDB:
    """Test dummy users database functionality."""

    def test_insert_dummy_users_creates_three_users(self, setup_database, db_session):
        """Test that insert_dummy_users creates exactly 3 users."""
        # Insert dummy users
        insert_dummy_users(db_session)

        # Query all users
        users = db_session.query(User).all()

        # Should have exactly 3 users
        assert len(users) == 3

        # Check specific users exist
        jessica = db_session.query(User).filter(User.name == "Jessica").first()
        sara = db_session.query(User).filter(User.name == "Sara").first()
        mervi = db_session.query(User).filter(User.name == "Mervi").first()

        assert jessica is not None
        assert sara is not None
        assert mervi is not None

    def test_dummy_users_have_correct_roles(self, setup_database, db_session):
        """Test that dummy users have the correct roles."""
        # Insert dummy users
        insert_dummy_users(db_session)

        # Check Jessica (educator)
        jessica = db_session.query(User).filter(User.name == "Jessica").first()
        assert jessica.role == UserRole.EDUCATOR.value

        # Check Sara (parent)
        sara = db_session.query(User).filter(User.name == "Sara").first()
        assert sara.role == UserRole.PARENT.value

        # Check Mervi (super_educator)
        mervi = db_session.query(User).filter(User.name == "Mervi").first()
        assert mervi.role == UserRole.SUPER_EDUCATOR.value

    def test_dummy_users_jwt_tokens_decode_correctly(self, setup_database, db_session):
        """Test that JWT tokens decode correctly with expected claims."""
        # Insert dummy users
        insert_dummy_users(db_session)

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

            # Verify user_id is correct
            expected_user_id = {"Jessica": 1, "Sara": 2, "Mervi": 3}[user.name]
            assert decoded_payload["user_id"] == expected_user_id

    def test_duplicate_insertion_does_not_create_extra_rows(
        self, setup_database, db_session
    ):
        """Test that calling insert_dummy_users twice doesn't create duplicates."""
        # Insert dummy users first time
        insert_dummy_users(db_session)

        # Count users after first insertion
        first_count = db_session.query(User).count()
        assert first_count == 3

        # Insert dummy users second time
        insert_dummy_users(db_session)

        # Count users after second insertion
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

    def test_dummy_users_created_on_startup(self, setup_database, db_session):
        """Test that dummy users are created when the startup event is triggered."""
        # Simulate startup by calling insert_dummy_users
        insert_dummy_users(db_session)

        # Verify users exist
        users = db_session.query(User).all()
        assert len(users) == 3

        # Verify all expected users are present
        names = [user.name for user in users]
        assert "Jessica" in names
        assert "Sara" in names
        assert "Mervi" in names

        # Verify all users have JWT tokens
        for user in users:
            assert user.jwt_token is not None
            assert len(user.jwt_token) > 0
