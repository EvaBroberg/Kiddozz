"""Tests for accept-invite endpoint."""

import pytest

from app.core.roles import Role
from app.models.daycare import Daycare
from app.models.educator import Educator
from app.models.parent import Parent
from app.services.invite_token_service import generate_invite_token
from tests.conftest import TestingSessionLocal, client


@pytest.fixture
def db_session():
    """Create a test database session."""
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def test_daycare(db_session) -> Daycare:
    """Create a test daycare."""
    daycare = Daycare(name="Test Daycare")
    db_session.add(daycare)
    db_session.commit()
    db_session.refresh(daycare)
    return daycare


class TestAcceptSuperEducatorInvite:
    """Test accepting SUPER_EDUCATOR invite."""

    def test_accept_super_educator_invite_success(self, db_session, test_daycare):
        """Test successfully accepting a SUPER_EDUCATOR invite."""
        # Generate invite token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.SUPER_EDUCATOR,
            email="super@example.com",
            ttl_minutes=60 * 24,  # 1 day
        )

        # Accept invite
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "Super Educator", "phone_num": "+1234567890"},
        )

        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["token_type"] == "bearer"
        assert data["role"] == "super_educator"
        assert data["daycare_id"] == test_daycare.id
        assert "user_id" in data

        # Verify token was consumed
        db_session.refresh(token_obj)
        assert token_obj.used_at is not None

        # Verify educator was created
        educator = db_session.query(Educator).filter(Educator.email == "super@example.com").first()
        assert educator is not None
        assert educator.full_name == "Super Educator"
        assert educator.role == "super_educator"
        assert educator.daycare_id == test_daycare.id
        assert str(educator.id) == data["user_id"]

    def test_accept_educator_invite_success(self, db_session, test_daycare):
        """Test successfully accepting an EDUCATOR invite."""
        # Generate invite token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.EDUCATOR,
            email="educator@example.com",
            ttl_minutes=60 * 24,
        )

        # Accept invite
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "Regular Educator"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["role"] == "educator"
        assert data["daycare_id"] == test_daycare.id

        # Verify educator was created
        educator = db_session.query(Educator).filter(Educator.email == "educator@example.com").first()
        assert educator is not None
        assert educator.role == "educator"

    def test_accept_parent_invite_success(self, db_session, test_daycare):
        """Test successfully accepting a PARENT invite."""
        # Generate invite token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.PARENT,
            email="parent@example.com",
            ttl_minutes=60 * 24,
        )

        # Accept invite
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={
                "token": token_str,
                "name": "Test Parent",
                "phone_num": "+1987654321",
            },
        )

        assert response.status_code == 200
        data = response.json()
        assert data["role"] == "parent"
        assert data["daycare_id"] == test_daycare.id

        # Verify parent was created
        parent = db_session.query(Parent).filter(Parent.email == "parent@example.com").first()
        assert parent is not None
        assert parent.full_name == "Test Parent"
        assert parent.phone_num == "+1987654321"

    def test_accept_parent_invite_missing_phone_fails(self, db_session, test_daycare):
        """Test that accepting PARENT invite without phone_num fails and token is not consumed."""
        # Generate invite token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.PARENT,
            email="parent2@example.com",
            ttl_minutes=60 * 24,
        )

        # Accept invite without phone_num
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "Test Parent"},
        )

        assert response.status_code == 422
        assert "phone_num is required" in response.json()["detail"]
        
        # Verify token was NOT consumed
        db_session.refresh(token_obj)
        assert token_obj.used_at is None

    def test_accept_invite_missing_name_fails(self, db_session, test_daycare):
        """Test that accepting invite without name fails and token is not consumed."""
        # Generate invite token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.SUPER_EDUCATOR,
            email="noname@example.com",
            ttl_minutes=60 * 24,
        )

        # Accept invite without name
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": ""},  # Empty name
        )

        assert response.status_code == 422
        assert "name is required" in response.json()["detail"]
        
        # Verify token was NOT consumed
        db_session.refresh(token_obj)
        assert token_obj.used_at is None


class TestAcceptInviteTokenReuse:
    """Test that token reuse is rejected."""

    def test_reuse_token_rejected(self, db_session, test_daycare):
        """Test that using the same token twice is rejected."""
        # Generate invite token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.SUPER_EDUCATOR,
            email="reuse@example.com",
            ttl_minutes=60 * 24,
        )

        # First acceptance should succeed
        response1 = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "First User"},
        )
        assert response1.status_code == 200

        # Second acceptance should fail
        response2 = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "Second User"},
        )
        assert response2.status_code == 400
        detail_lower = response2.json()["detail"].lower()
        assert "already" in detail_lower and "used" in detail_lower


class TestAcceptInviteExpiredToken:
    """Test that expired tokens are rejected."""

    def test_expired_token_rejected(self, db_session, test_daycare):
        """Test that accepting an expired token fails."""
        from datetime import datetime, timedelta, timezone

        # Generate invite token with very short TTL
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.SUPER_EDUCATOR,
            email="expired@example.com",
            ttl_minutes=1,  # 1 minute
        )

        # Manually expire the token
        token_obj.expires_at = datetime.now(timezone.utc) - timedelta(minutes=1)
        db_session.add(token_obj)
        db_session.commit()
        db_session.refresh(token_obj)

        # Attempt to accept expired token
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "Expired User"},
        )

        assert response.status_code == 400
        assert "expired" in response.json()["detail"].lower()


class TestAcceptInviteDuplicateEmail:
    """Test duplicate email handling."""

    def test_duplicate_educator_email_rejected(self, db_session, test_daycare):
        """Test that accepting invite with duplicate educator email fails."""
        # Create existing educator
        existing_educator = Educator(
            full_name="Existing Educator",
            email="duplicate@example.com",
            role="educator",
            daycare_id=test_daycare.id,
        )
        db_session.add(existing_educator)
        db_session.commit()

        # Generate invite token with same email
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.EDUCATOR,
            email="duplicate@example.com",
            ttl_minutes=60 * 24,
        )

        # Attempt to accept invite
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "New Educator"},
        )

        assert response.status_code == 409
        assert "already registered" in response.json()["detail"].lower()

        # Verify token was NOT consumed
        db_session.refresh(token_obj)
        assert token_obj.used_at is None

    def test_duplicate_parent_email_rejected(self, db_session, test_daycare):
        """Test that accepting invite with duplicate parent email fails."""
        # Create existing parent
        existing_parent = Parent(
            full_name="Existing Parent",
            email="duplicate.parent@example.com",
            phone_num="+1111111111",
            daycare_id=test_daycare.id,
        )
        db_session.add(existing_parent)
        db_session.commit()

        # Generate invite token with same email
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role=Role.PARENT,
            email="duplicate.parent@example.com",
            ttl_minutes=60 * 24,
        )

        # Attempt to accept invite
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": token_str, "name": "New Parent", "phone_num": "+2222222222"},
        )

        assert response.status_code == 409
        assert "already registered" in response.json()["detail"].lower()

        # Verify token was NOT consumed
        db_session.refresh(token_obj)
        assert token_obj.used_at is None

    def test_invalid_token_rejected(self, db_session):
        """Test that accepting an invalid token fails."""
        response = client.post(
            "/api/v1/auth/accept-invite",
            json={"token": "invalid-token-123", "name": "Test User"},
        )

        assert response.status_code == 400
        assert "does not exist" in response.json()["detail"].lower()

