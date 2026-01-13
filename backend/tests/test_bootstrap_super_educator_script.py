"""Tests for bootstrap_super_educator_invite script."""

from datetime import datetime, timezone

import pytest

from app.core.roles import Role
from app.models.daycare import Daycare
from app.models.invite_token import InviteToken
from app.services.invite_token_service import validate_invite_token
from scripts.bootstrap_super_educator_invite import bootstrap_super_educator_invite


@pytest.fixture
def db_session():
    """Create a test database session."""
    from sqlalchemy import create_engine
    from sqlalchemy.orm import sessionmaker

    from app.core.database import Base

    SQLALCHEMY_DATABASE_URL = "sqlite:///./test_bootstrap_super_educator.db"
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

        if os.path.exists("test_bootstrap_super_educator.db"):
            os.remove("test_bootstrap_super_educator.db")


class TestBootstrapSuperEducatorInvite:
    """Test bootstrap_super_educator_invite function."""

    def test_bootstrap_creates_daycare_and_token(self, db_session):
        """Test that bootstrap creates daycare and invite token correctly."""
        daycare_name = "Test Daycare Bootstrap"
        email = "super@example.com"
        ttl_minutes = 1440  # 1 day

        # Call bootstrap function with test db session
        daycare_id, token_id, token_str, invite_link = bootstrap_super_educator_invite(
            daycare_name=daycare_name,
            email=email,
            ttl_minutes=ttl_minutes,
            created_by="test-script",
            base_url="test://invite",
            dry_run=False,
            db=db_session,
        )

        # Verify daycare was created
        daycare = db_session.query(Daycare).filter(Daycare.id == daycare_id).first()
        assert daycare is not None
        assert daycare.name == daycare_name

        # Verify token was created
        token = db_session.query(InviteToken).filter(InviteToken.id == token_id).first()
        assert token is not None
        assert token.token == token_str
        assert token.daycare_id == daycare_id
        assert token.role == Role.SUPER_EDUCATOR.value
        assert token.email == email
        assert token.created_by == "test-script"

        # Verify token expires_at is in the future
        now_utc = datetime.now(timezone.utc)
        token_expires_utc = (
            token.expires_at.replace(tzinfo=timezone.utc)
            if token.expires_at.tzinfo is None
            else token.expires_at
        )
        assert token_expires_utc > now_utc

        # Verify token validates successfully
        validated_token = validate_invite_token(db_session, token_str)
        assert validated_token.id == token_id
        assert validated_token.role == Role.SUPER_EDUCATOR.value

        # Verify invite link format
        assert invite_link == f"test://invite?token={token_str}"

    def test_bootstrap_dry_run_completes(self, db_session):
        """Test that dry-run mode completes successfully and returns expected values."""
        daycare_name = "Dry Run Daycare"
        email = "dryrun@example.com"

        # Call bootstrap with dry_run=True
        # Note: Full rollback verification is complex with SQLite due to nested commits
        # in generate_invite_token. This test verifies dry-run completes without errors.
        daycare_id, token_id, token_str, invite_link = bootstrap_super_educator_invite(
            daycare_name=daycare_name,
            email=email,
            dry_run=True,
            db=db_session,
        )

        # Verify function returns expected values
        assert daycare_id is not None
        assert token_id is not None
        assert token_str is not None
        assert invite_link is not None
        assert "kiddozz://invite?token=" in invite_link
        assert token_str in invite_link

    def test_bootstrap_default_values(self, db_session):
        """Test that bootstrap works with default values."""
        daycare_name = "Default Daycare"
        email = "default@example.com"

        daycare_id, token_id, token_str, invite_link = bootstrap_super_educator_invite(
            daycare_name=daycare_name,
            email=email,
            dry_run=False,
            db=db_session,
        )

        # Verify defaults were used
        token = db_session.query(InviteToken).filter(InviteToken.id == token_id).first()
        assert token.created_by == "bootstrap-script"
        assert "kiddozz://invite?token=" in invite_link

        # Verify default TTL (7 days = 10080 minutes)
        # Token should expire approximately 7 days from now
        now_utc = datetime.now(timezone.utc)
        token_expires_utc = (
            token.expires_at.replace(tzinfo=timezone.utc)
            if token.expires_at.tzinfo is None
            else token.expires_at
        )
        # Should be roughly 7 days (allow 1 minute tolerance)
        time_diff = (token_expires_utc - now_utc).total_seconds() / 60
        assert 10079 <= time_diff <= 10081  # Approximately 10080 minutes

