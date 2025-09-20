import pytest
from sqlalchemy import create_engine
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import sessionmaker

from app.models.educator import Educator, EducatorRole

# Use the same test database as other tests
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture
def db_session():
    """Create a test database session."""
    session = TestingSessionLocal()

    try:
        yield session
    finally:
        session.close()


class TestEducatorModel:
    """Test Educator model functionality."""

    def test_create_educator_success(self, db_session):
        """Test creating a valid educator."""
        educator = Educator(
            full_name="John Doe",
            role=EducatorRole.EDUCATOR.value,
            email="john.doe@example.com",
            phone_num="+1234567890",
            group="Class A"
        )

        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        # Verify educator was created
        assert educator.id is not None
        assert educator.full_name == "John Doe"
        assert educator.role == EducatorRole.EDUCATOR.value
        assert educator.email == "john.doe@example.com"
        assert educator.phone_num == "+1234567890"
        assert educator.group == "Class A"
        assert educator.created is not None
        assert educator.updated is not None

    def test_create_educator_with_minimal_fields(self, db_session):
        """Test creating an educator with only required fields."""
        educator = Educator(
            full_name="Jane Smith",
            role=EducatorRole.SUPER_EDUCATOR.value,
            email="jane.smith@example.com"
        )

        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        # Verify educator was created with defaults
        assert educator.id is not None
        assert educator.full_name == "Jane Smith"
        assert educator.role == EducatorRole.SUPER_EDUCATOR.value
        assert educator.email == "jane.smith@example.com"
        assert educator.phone_num is None
        assert educator.group is None
        assert educator.jwt_token is None
        assert educator.created is not None
        assert educator.updated is not None

    def test_create_educator_duplicate_email_fails(self, db_session):
        """Test that creating educators with duplicate emails fails."""
        # Create first educator
        educator1 = Educator(
            full_name="First Educator",
            role=EducatorRole.EDUCATOR.value,
            email="duplicate@example.com"
        )
        db_session.add(educator1)
        db_session.commit()

        # Try to create second educator with same email
        educator2 = Educator(
            full_name="Second Educator",
            role=EducatorRole.EDUCATOR.value,
            email="duplicate@example.com"
        )
        db_session.add(educator2)

        with pytest.raises(IntegrityError):
            db_session.commit()

    def test_create_educator_missing_required_fields(self, db_session):
        """Test that creating educators without required fields fails."""
        # Test missing full_name
        with pytest.raises(IntegrityError):
            educator = Educator(
                role=EducatorRole.EDUCATOR.value,
                email="test@example.com"
            )
            db_session.add(educator)
            db_session.commit()

    def test_create_educator_missing_email(self, db_session):
        """Test that creating educators without email fails."""
        with pytest.raises(IntegrityError):
            educator = Educator(
                full_name="Test Educator",
                role=EducatorRole.EDUCATOR.value
            )
            db_session.add(educator)
            db_session.commit()

    def test_create_educator_missing_role(self, db_session):
        """Test that creating educators without role fails."""
        with pytest.raises(IntegrityError):
            educator = Educator(
                full_name="Test Educator",
                email="test@example.com"
            )
            db_session.add(educator)
            db_session.commit()

    def test_educator_timestamps_auto_set(self, db_session):
        """Test that created and updated timestamps are automatically set."""
        educator = Educator(
            full_name="Timestamp Test",
            role=EducatorRole.EDUCATOR.value,
            email="timestamp@example.com"
        )

        # Before adding to database, timestamps should be None
        assert educator.created is None
        assert educator.updated is None

        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        # After commit, timestamps should be set
        assert educator.created is not None
        assert educator.updated is not None
        assert educator.created == educator.updated  # Should be equal on creation

    def test_educator_updated_timestamp_changes(self, db_session):
        """Test that updated timestamp can be manually updated."""
        educator = Educator(
            full_name="Update Test",
            role=EducatorRole.EDUCATOR.value,
            email="update@example.com"
        )

        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        original_updated = educator.updated

        # Manually update the timestamp to test the field works
        from datetime import datetime
        educator.updated = datetime.now()
        db_session.commit()
        db_session.refresh(educator)

        # Updated timestamp should be different
        assert educator.updated != original_updated

    def test_educator_role_enum_values(self, db_session):
        """Test that EducatorRole enum values are correctly stored."""
        educator1 = Educator(
            full_name="Regular Educator",
            role=EducatorRole.EDUCATOR.value,
            email="regular@example.com"
        )
        educator2 = Educator(
            full_name="Super Educator",
            role=EducatorRole.SUPER_EDUCATOR.value,
            email="super@example.com"
        )

        db_session.add_all([educator1, educator2])
        db_session.commit()

        # Verify roles were stored correctly
        assert (
            db_session.query(Educator)
            .filter_by(email="regular@example.com")
            .first()
            .role
            == EducatorRole.EDUCATOR.value
        )
        assert (
            db_session.query(Educator)
            .filter_by(email="super@example.com")
            .first()
            .role
            == EducatorRole.SUPER_EDUCATOR.value
        )

    def test_educator_repr(self, db_session):
        """Test the __repr__ method of the Educator model."""
        educator = Educator(
            full_name="Repr Test",
            role=EducatorRole.EDUCATOR.value,
            email="repr@example.com"
        )
        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        expected_repr = f"<Educator(id={educator.id}, full_name='Repr Test', role='educator', email='repr@example.com')>"
        assert repr(educator) == expected_repr

    def test_educator_with_jwt_token(self, db_session):
        """Test creating an educator with JWT token."""
        educator = Educator(
            full_name="JWT Test",
            role=EducatorRole.EDUCATOR.value,
            email="jwt@example.com",
            jwt_token="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
        )

        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        assert educator.jwt_token == "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"

    def test_multiple_educators_different_roles(self, db_session):
        """Test creating multiple educators with different roles."""
        educators = [
            Educator(
                full_name="Educator 1",
                role=EducatorRole.EDUCATOR.value,
                email="educator1@example.com",
                group="Class A"
            ),
            Educator(
                full_name="Educator 2",
                role=EducatorRole.SUPER_EDUCATOR.value,
                email="educator2@example.com",
                group="Class B"
            ),
            Educator(
                full_name="Educator 3",
                role=EducatorRole.EDUCATOR.value,
                email="educator3@example.com",
                group="Class A"
            )
        ]

        db_session.add_all(educators)
        db_session.commit()

        created_educators = (
            db_session.query(Educator)
            .filter(
                Educator.email.in_([
                    "educator1@example.com",
                    "educator2@example.com",
                    "educator3@example.com"
                ])
            )
            .all()
        )
        assert len(created_educators) == 3

        # Verify roles
        roles = [educator.role for educator in created_educators]
        assert "educator" in roles
        assert "super_educator" in roles
