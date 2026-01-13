"""Tests for invite token validation and consumption."""

from datetime import datetime, timedelta, timezone

import pytest

from app.models.daycare import Daycare
from app.models.invite_token import InviteToken
from app.services.invite_token_service import (
    ExpiredTokenError,
    InvalidTokenError,
    RevokedTokenError,
    TokenAlreadyUsedError,
    consume_invite_token,
    validate_invite_token,
)


@pytest.fixture
def db_session():
    """Create a test database session."""
    from sqlalchemy import create_engine
    from sqlalchemy.orm import sessionmaker

    from app.core.database import Base

    SQLALCHEMY_DATABASE_URL = "sqlite:///./test_invite_token.db"
    engine = create_engine(
        SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
    )
    Base.metadata.create_all(bind=engine)
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        # Clean up test database
        import os

        if os.path.exists("test_invite_token.db"):
            os.remove("test_invite_token.db")


@pytest.fixture
def test_daycare(db_session):
    """Create a test daycare."""
    daycare = Daycare(name="Test Daycare")
    db_session.add(daycare)
    db_session.commit()
    db_session.refresh(daycare)
    return daycare


@pytest.fixture
def valid_token(db_session, test_daycare):
    """Create a valid invite token."""
    token = InviteToken(
        token="valid-token-123",
        daycare_id=test_daycare.id,
        role="parent",
        email="test@example.com",
        expires_at=datetime.now(timezone.utc) + timedelta(days=7),
    )
    db_session.add(token)
    db_session.commit()
    db_session.refresh(token)
    return token


class TestValidateInviteToken:
    """Test validate_invite_token function."""

    def test_validate_valid_token(self, db_session, valid_token):
        """Test that a valid token passes validation."""
        result = validate_invite_token(db_session, "valid-token-123")
        assert result.id == valid_token.id
        assert result.token == "valid-token-123"
        assert result.role == "parent"
        assert result.email == "test@example.com"

    def test_validate_invalid_token(self, db_session):
        """Test that a non-existent token raises InvalidTokenError."""
        with pytest.raises(InvalidTokenError, match="does not exist"):
            validate_invite_token(db_session, "non-existent-token")

    def test_validate_expired_token(self, db_session, test_daycare):
        """Test that an expired token raises ExpiredTokenError."""
        token = InviteToken(
            token="expired-token",
            daycare_id=test_daycare.id,
            role="educator",
            email="expired@example.com",
            expires_at=datetime.now(timezone.utc)
            - timedelta(days=1),  # Expired yesterday
        )
        db_session.add(token)
        db_session.commit()

        with pytest.raises(ExpiredTokenError, match="has expired"):
            validate_invite_token(db_session, "expired-token")

    def test_validate_revoked_token(self, db_session, test_daycare):
        """Test that a revoked token raises RevokedTokenError."""
        token = InviteToken(
            token="revoked-token",
            daycare_id=test_daycare.id,
            role="educator",
            email="revoked@example.com",
            expires_at=datetime.now(timezone.utc) + timedelta(days=7),
            revoked_at=datetime.now(timezone.utc),
        )
        db_session.add(token)
        db_session.commit()

        with pytest.raises(RevokedTokenError, match="has been revoked"):
            validate_invite_token(db_session, "revoked-token")

    def test_validate_used_token(self, db_session, test_daycare):
        """Test that a used token raises TokenAlreadyUsedError."""
        token = InviteToken(
            token="used-token",
            daycare_id=test_daycare.id,
            role="parent",
            email="used@example.com",
            expires_at=datetime.now(timezone.utc) + timedelta(days=7),
            used_at=datetime.now(timezone.utc),
        )
        db_session.add(token)
        db_session.commit()

        with pytest.raises(TokenAlreadyUsedError, match="has already been used"):
            validate_invite_token(db_session, "used-token")

    def test_validate_invalid_role(self, db_session, test_daycare):
        """Test that a token with invalid role raises InvalidTokenError."""
        token = InviteToken(
            token="invalid-role-token",
            daycare_id=test_daycare.id,
            role="invalid_role",
            email="invalid@example.com",
            expires_at=datetime.now(timezone.utc) + timedelta(days=7),
        )
        db_session.add(token)
        db_session.commit()

        with pytest.raises(InvalidTokenError, match="invalid role"):
            validate_invite_token(db_session, "invalid-role-token")


class TestConsumeInviteToken:
    """Test consume_invite_token function (atomic consumption)."""

    def test_consume_valid_token(self, db_session, valid_token):
        """Test that a valid token can be consumed once."""
        # Consume the token
        result = consume_invite_token(db_session, "valid-token-123")

        assert result.id == valid_token.id
        assert result.used_at is not None

        # Verify token is marked as used in database
        db_session.refresh(valid_token)
        assert valid_token.used_at is not None

    def test_consume_token_twice_rejected(self, db_session, valid_token):
        """Test that consuming a token twice is rejected."""
        # First consumption should succeed
        consume_invite_token(db_session, "valid-token-123")

        # Second consumption should fail
        with pytest.raises(
            TokenAlreadyUsedError, match="already been used|already consumed"
        ):
            consume_invite_token(db_session, "valid-token-123")

    def test_consume_expired_token_rejected(self, db_session, test_daycare):
        """Test that an expired token cannot be consumed."""
        token = InviteToken(
            token="expired-consume-token",
            daycare_id=test_daycare.id,
            role="educator",
            email="expired@example.com",
            expires_at=datetime.now(timezone.utc) - timedelta(days=1),
        )
        db_session.add(token)
        db_session.commit()

        with pytest.raises(ExpiredTokenError, match="has expired"):
            consume_invite_token(db_session, "expired-consume-token")

    def test_consume_revoked_token_rejected(self, db_session, test_daycare):
        """Test that a revoked token cannot be consumed."""
        token = InviteToken(
            token="revoked-consume-token",
            daycare_id=test_daycare.id,
            role="educator",
            email="revoked@example.com",
            expires_at=datetime.now(timezone.utc) + timedelta(days=7),
            revoked_at=datetime.now(timezone.utc),
        )
        db_session.add(token)
        db_session.commit()

        with pytest.raises(RevokedTokenError, match="has been revoked"):
            consume_invite_token(db_session, "revoked-consume-token")

    def test_consume_invalid_token_rejected(self, db_session):
        """Test that a non-existent token cannot be consumed."""
        with pytest.raises(InvalidTokenError, match="does not exist"):
            consume_invite_token(db_session, "non-existent-token")

    def test_consume_super_educator_token(self, db_session, test_daycare):
        """Test that super_educator role tokens can be consumed."""
        token = InviteToken(
            token="super-educator-token",
            daycare_id=test_daycare.id,
            role="super_educator",
            email="super@example.com",
            expires_at=datetime.now(timezone.utc) + timedelta(days=7),
        )
        db_session.add(token)
        db_session.commit()

        result = consume_invite_token(db_session, "super-educator-token")
        assert result.role == "super_educator"
        assert result.used_at is not None
