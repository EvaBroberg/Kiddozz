"""Test daycare tenant creation and user linkage."""

import pytest

from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole


@pytest.fixture
def db_session():
    """Create a test database session."""
    from sqlalchemy import create_engine, text
    from sqlalchemy.orm import sessionmaker
    
    SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
    engine = create_engine(
        SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
    )
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


class TestDaycareTenantLinkage:
    """Test that daycare can be created and users can be linked to it."""

    def test_create_daycare_and_link_educator(self, db_session):
        """Test creating a daycare and linking an educator to it."""
        # Create a daycare
        daycare = Daycare(name="Test Tenant Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)
        
        # Assert daycare.id exists
        assert daycare.id is not None, "Daycare ID should be generated"
        assert isinstance(daycare.id, str), "Daycare ID should be a string (UUID)"
        assert len(daycare.id) > 0, "Daycare ID should not be empty"
        
        # Create an educator linked to the daycare
        educator = Educator(
            full_name="Test Educator",
            email=f"test-{daycare.id[:8]}@example.com",  # Unique email
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)
        
        # Assert educator.daycare_id == daycare.id
        assert educator.daycare_id == daycare.id, (
            f"Educator daycare_id ({educator.daycare_id}) should match daycare.id ({daycare.id})"
        )
        
        # Verify relationship loads (if relationship exists)
        # Refresh to ensure relationship is loaded
        db_session.refresh(educator)
        if hasattr(educator, 'daycare') and educator.daycare:
            assert educator.daycare.id == daycare.id, (
                f"Educator.daycare.id ({educator.daycare.id}) should match daycare.id ({daycare.id})"
            )
            assert educator.daycare.name == daycare.name, (
                f"Educator.daycare.name ({educator.daycare.name}) should match daycare.name ({daycare.name})"
            )
        
        # Verify from daycare side (if relationship exists)
        db_session.refresh(daycare)
        if hasattr(daycare, 'educators') and daycare.educators:
            educator_ids = [e.id for e in daycare.educators]
            assert educator.id in educator_ids, (
                f"Educator {educator.id} should be in daycare.educators list"
            )

