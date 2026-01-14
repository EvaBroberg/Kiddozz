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
    generate_invite_token,
    revoke_invite_token,
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


class TestGenerateInviteToken:
    """Test generate_invite_token function."""

    def test_generate_token(self, db_session, test_daycare):
        """Test that a token can be generated."""
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="parent",
            email="newuser@example.com",
            ttl_minutes=60 * 24,  # 1 day
        )

        assert token_obj.id is not None
        assert token_obj.token == token_str
        assert token_obj.daycare_id == test_daycare.id
        assert token_obj.role == "parent"
        assert token_obj.email == "newuser@example.com"
        assert token_obj.used_at is None
        assert token_obj.revoked_at is None
        # Ensure expires_at is in the future (timezone-aware comparison)
        now_utc = datetime.now(timezone.utc)
        assert token_obj.expires_at.replace(tzinfo=timezone.utc) > now_utc

    def test_generate_token_with_created_by(self, db_session, test_daycare):
        """Test generating a token with created_by field."""
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="educator",
            email="educator@example.com",
            created_by="admin-user-123",
        )

        assert token_obj.created_by == "admin-user-123"

    def test_generate_token_invalid_role(self, db_session, test_daycare):
        """Test that generating with invalid role raises ValueError."""
        with pytest.raises(ValueError, match="Invalid role"):
            generate_invite_token(
                db_session,
                daycare_id=test_daycare.id,
                role="invalid_role",
                email="test@example.com",
            )

    def test_generate_token_invalid_ttl(self, db_session, test_daycare):
        """Test that generating with non-positive TTL raises ValueError."""
        with pytest.raises(ValueError, match="ttl_minutes must be positive"):
            generate_invite_token(
                db_session,
                daycare_id=test_daycare.id,
                role="parent",
                email="test@example.com",
                ttl_minutes=0,
            )

        with pytest.raises(ValueError, match="ttl_minutes must be positive"):
            generate_invite_token(
                db_session,
                daycare_id=test_daycare.id,
                role="parent",
                email="test@example.com",
                ttl_minutes=-1,
            )

    def test_generate_token_ttl_enforced(self, db_session, test_daycare):
        """Test that TTL is properly enforced in expiration."""
        # Generate token with default TTL (7 days)
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="parent",
            email="test@example.com",
        )

        # Token should be valid immediately
        validate_invite_token(db_session, token_str)

        # Manually expire the token by setting expires_at to past
        # Ensure timezone-aware datetime
        token_obj.expires_at = datetime.now(timezone.utc) - timedelta(minutes=1)
        db_session.commit()
        db_session.refresh(token_obj)

        # Validation should fail with ExpiredTokenError
        with pytest.raises(ExpiredTokenError, match="has expired"):
            validate_invite_token(db_session, token_str)

    def test_token_randomness(self, db_session, test_daycare):
        """Test that generated tokens are unique (randomness check)."""
        tokens = set()
        for _ in range(50):
            token_obj, token_str = generate_invite_token(
                db_session,
                daycare_id=test_daycare.id,
                role="parent",
                email=f"user{_}@example.com",
            )
            assert token_str not in tokens, "Token collision detected"
            tokens.add(token_str)


class TestRevokeInviteToken:
    """Test revoke_invite_token function."""

    def test_revoke_token(self, db_session, test_daycare):
        """Test that a token can be revoked."""
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="parent",
            email="test@example.com",
        )

        # Token should be valid before revocation
        validate_invite_token(db_session, token_str)

        # Revoke the token
        revoked_token = revoke_invite_token(
            db_session, token_str, revoked_by="admin-123"
        )

        assert revoked_token.id == token_obj.id
        assert revoked_token.revoked_at is not None
        # Ensure timezone-aware comparison
        now_utc = datetime.now(timezone.utc)
        revoked_at_utc = (
            revoked_token.revoked_at.replace(tzinfo=timezone.utc)
            if revoked_token.revoked_at.tzinfo is None
            else revoked_token.revoked_at
        )
        assert revoked_at_utc <= now_utc

        # Token should fail validation after revocation
        with pytest.raises(RevokedTokenError, match="has been revoked"):
            validate_invite_token(db_session, token_str)

    def test_revoke_nonexistent_token(self, db_session):
        """Test that revoking a non-existent token raises InvalidTokenError."""
        with pytest.raises(InvalidTokenError, match="does not exist"):
            revoke_invite_token(db_session, "non-existent-token")

    def test_revoke_used_token_rejected(self, db_session, test_daycare):
        """Test that revoking a used token is rejected."""
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="parent",
            email="test@example.com",
        )

        # Consume the token first
        consume_invite_token(db_session, token_str)

        # Attempting to revoke should fail
        with pytest.raises(TokenAlreadyUsedError, match="already been used"):
            revoke_invite_token(db_session, token_str)


class TestInviteTokenLifecycle:
    """Test complete lifecycle: generate -> validate -> revoke -> validate."""

    def test_generate_validate_revoke_flow(self, db_session, test_daycare):
        """Test the complete lifecycle flow."""
        # 1. Generate token
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="educator",
            email="educator@example.com",
        )

        # 2. Validate succeeds
        validated_token = validate_invite_token(db_session, token_str)
        assert validated_token.id == token_obj.id

        # 3. Revoke token
        revoked_token = revoke_invite_token(db_session, token_str)
        assert revoked_token.revoked_at is not None

        # 4. Validate fails as revoked
        with pytest.raises(RevokedTokenError, match="has been revoked"):
            validate_invite_token(db_session, token_str)


class TestInviteTokenLogging:
    """Test that logging works correctly and doesn't leak tokens."""

    def test_generate_logs_lifecycle(self, db_session, test_daycare, caplog):
        """Test that token generation is logged without full token."""
        with caplog.at_level("INFO"):
            token_obj, token_str = generate_invite_token(
                db_session,
                daycare_id=test_daycare.id,
                role="parent",
                email="test@example.com",
            )

        # Check that log contains generation message
        assert "Invite token generated" in caplog.text
        assert f"id={token_obj.id}" in caplog.text
        assert f"token_prefix='{token_str[:6]}...'" in caplog.text

        # Ensure full token is NOT in logs
        assert token_str not in caplog.text

    def test_revoke_logs_lifecycle(self, db_session, test_daycare, caplog):
        """Test that token revocation is logged without full token."""
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="parent",
            email="test@example.com",
        )

        with caplog.at_level("INFO"):
            revoke_invite_token(db_session, token_str, revoked_by="admin-123")

        # Check that log contains revocation message
        assert "Invite token revoked" in caplog.text
        assert f"id={token_obj.id}" in caplog.text
        assert f"token_prefix='{token_str[:6]}...'" in caplog.text

        # Ensure full token is NOT in logs
        assert token_str not in caplog.text

    def test_consume_logs_lifecycle(self, db_session, test_daycare, caplog):
        """Test that token consumption is logged without full token."""
        token_obj, token_str = generate_invite_token(
            db_session,
            daycare_id=test_daycare.id,
            role="parent",
            email="test@example.com",
        )

        with caplog.at_level("INFO"):
            consume_invite_token(db_session, token_str)

        # Check that log contains consumption message
        assert "Invite token consumed" in caplog.text
        assert f"id={token_obj.id}" in caplog.text
        assert f"token_prefix='{token_str[:6]}...'" in caplog.text

        # Ensure full token is NOT in logs
        assert token_str not in caplog.text
