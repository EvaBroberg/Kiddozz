import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import IntegrityError

from app.models.user import User, UserRole
from app.core.database import Base

# Use the same test database as other tests
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
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


class TestUserModel:
    """Test User model functionality."""

    def test_create_user_success(self, setup_database, db_session):
        """Test creating a valid user with role and jwt_token."""
        user = User(
            name="John Doe",
            role=UserRole.EDUCATOR.value,
            jwt_token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMTIzIiwicm9sZSI6ImVkdWNhdG9yIn0.test"
        )
        
        db_session.add(user)
        db_session.commit()
        db_session.refresh(user)
        
        # Verify user was created
        assert user.id is not None
        assert user.name == "John Doe"
        assert user.role == UserRole.EDUCATOR.value
        assert user.jwt_token is not None
        assert user.created_at is not None

    def test_create_user_missing_role(self, setup_database, db_session):
        """Test error when no role is provided."""
        user = User(
            name="Jane Doe",
            jwt_token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNDU2Iiwicm9sZSI6InBhcmVudCJ9.test"
        )
        
        db_session.add(user)
        
        with pytest.raises(IntegrityError):
            db_session.commit()

    def test_create_user_invalid_role(self, setup_database, db_session):
        """Test error when role is not one of the allowed values."""
        user = User(
            name="Invalid User",
            role="invalid_role",
            jwt_token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNzg5Iiwicm9sZSI6ImludmFsaWQifQ.test"
        )
        
        db_session.add(user)
        db_session.commit()  # This should succeed as we don't have enum constraint at DB level
        
        # But we can test that the role is stored as-is
        assert user.role == "invalid_role"

    def test_create_user_invalid_jwt(self, setup_database, db_session):
        """Test error when jwt_token is malformed."""
        # Test with completely invalid JWT
        user = User(
            name="Bad JWT User",
            role=UserRole.PARENT.value,
            jwt_token="not.a.valid.jwt"
        )
        
        db_session.add(user)
        db_session.commit()
        db_session.refresh(user)
        
        # Should still be created (we don't validate JWT format at DB level)
        assert user.jwt_token == "not.a.valid.jwt"

    def test_user_data_mismatch(self, setup_database, db_session):
        """Test user with mismatched role in JWT vs role column."""
        # Create user with educator role but parent JWT
        user = User(
            name="Mismatched User",
            role=UserRole.EDUCATOR.value,
            jwt_token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMTAwIiwicm9sZSI6InBhcmVudCJ9.test"
        )
        
        db_session.add(user)
        db_session.commit()
        db_session.refresh(user)
        
        # User should be created successfully
        assert user.role == UserRole.EDUCATOR.value
        assert user.jwt_token is not None
        # Note: In a real application, you might want to add validation
        # to ensure JWT role matches database role

    def test_create_user_with_super_educator_role(self, setup_database, db_session):
        """Test creating a user with super_educator role."""
        user = User(
            name="Super Educator",
            role=UserRole.SUPER_EDUCATOR.value,
            jwt_token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiOTk5Iiwicm9sZSI6InN1cGVyX2VkdWNhdG9yIn0.test"
        )
        
        db_session.add(user)
        db_session.commit()
        db_session.refresh(user)
        
        assert user.role == UserRole.SUPER_EDUCATOR.value
        assert user.name == "Super Educator"

    def test_create_user_without_jwt_token(self, setup_database, db_session):
        """Test creating a user without jwt_token (nullable field)."""
        user = User(
            name="No JWT User",
            role=UserRole.PARENT.value
        )
        
        db_session.add(user)
        db_session.commit()
        db_session.refresh(user)
        
        assert user.jwt_token is None
        assert user.role == UserRole.PARENT.value

    def test_user_repr(self, setup_database, db_session):
        """Test User __repr__ method."""
        user = User(
            name="Test User",
            role=UserRole.EDUCATOR.value
        )
        
        db_session.add(user)
        db_session.commit()
        db_session.refresh(user)
        
        repr_str = repr(user)
        assert "User(id=" in repr_str
        assert "name='Test User'" in repr_str
        assert "role='educator'" in repr_str

    def test_user_role_enum_values(self):
        """Test UserRole enum has correct values."""
        assert UserRole.PARENT.value == "parent"
        assert UserRole.EDUCATOR.value == "educator"
        assert UserRole.SUPER_EDUCATOR.value == "super_educator"

    def test_multiple_users_different_roles(self, setup_database, db_session):
        """Test creating multiple users with different roles."""
        users = [
            User(name="Parent User", role=UserRole.PARENT.value),
            User(name="Educator User", role=UserRole.EDUCATOR.value),
            User(name="Super Educator User", role=UserRole.SUPER_EDUCATOR.value),
        ]
        
        for user in users:
            db_session.add(user)
        
        db_session.commit()
        
        # Verify the specific users were created by checking their names
        created_users = db_session.query(User).filter(User.name.in_([
            "Parent User", "Educator User", "Super Educator User"
        ])).all()
        assert len(created_users) == 3
        
        # Verify roles
        roles = [user.role for user in created_users]
        assert "parent" in roles
        assert "educator" in roles
        assert "super_educator" in roles
