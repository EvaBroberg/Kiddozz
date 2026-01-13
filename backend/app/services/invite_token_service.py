"""Service for managing invite tokens (validation and consumption)."""

from datetime import datetime, timezone

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.core.roles import Role
from app.models.invite_token import InviteToken


class InviteTokenError(Exception):
    """Base exception for invite token errors."""

    pass


class InvalidTokenError(InviteTokenError):
    """Token does not exist."""

    pass


class ExpiredTokenError(InviteTokenError):
    """Token has expired."""

    pass


class RevokedTokenError(InviteTokenError):
    """Token has been revoked."""

    pass


class TokenAlreadyUsedError(InviteTokenError):
    """Token has already been used."""

    pass


def validate_invite_token(db: Session, token_str: str) -> InviteToken:
    """
    Validate an invite token without consuming it.

    Args:
        db: Database session
        token_str: Token string to validate

    Returns:
        InviteToken object if valid

    Raises:
        InvalidTokenError: If token does not exist
        ExpiredTokenError: If token has expired
        RevokedTokenError: If token has been revoked
        TokenAlreadyUsedError: If token has already been used
    """
    token = db.query(InviteToken).filter(InviteToken.token == token_str).first()

    if not token:
        raise InvalidTokenError(f"Token '{token_str}' does not exist")

    # Check if token is expired
    # Handle both timezone-aware and timezone-naive datetimes
    now = datetime.now(timezone.utc) if token.expires_at.tzinfo else datetime.now()
    if token.expires_at < now:
        raise ExpiredTokenError(f"Token '{token_str}' has expired")

    # Check if token has been revoked
    if token.revoked_at is not None:
        raise RevokedTokenError(f"Token '{token_str}' has been revoked")

    # Check if token has already been used
    if token.used_at is not None:
        raise TokenAlreadyUsedError(f"Token '{token_str}' has already been used")

    # Validate role is a valid Role enum value
    try:
        Role.from_str(token.role)
    except ValueError:
        raise InvalidTokenError(f"Token '{token_str}' has invalid role: '{token.role}'")

    return token


def consume_invite_token(db: Session, token_str: str) -> InviteToken:
    """
    Atomically consume an invite token (mark as used).

    This function ensures single-use by atomically updating the token.
    Uses UPDATE ... WHERE used_at IS NULL pattern for atomic consumption
    that works reliably on both PostgreSQL and SQLite.

    Args:
        db: Database session
        token_str: Token string to consume

    Returns:
        InviteToken object that was consumed

    Raises:
        InvalidTokenError: If token does not exist
        ExpiredTokenError: If token has expired
        RevokedTokenError: If token has been revoked
        TokenAlreadyUsedError: If token has already been used (race condition)
    """
    bind = db.bind
    dialect = bind.dialect.name

    # Get token first to check timezone and existence
    token_check = db.query(InviteToken).filter(InviteToken.token == token_str).first()
    if not token_check:
        raise InvalidTokenError(f"Token '{token_str}' does not exist")

    # Use timezone-aware now if token has timezone, otherwise naive
    now = (
        datetime.now(timezone.utc) if token_check.expires_at.tzinfo else datetime.now()
    )

    # Use atomic UPDATE pattern for both PostgreSQL and SQLite
    # This ensures single-use even with concurrent requests
    if dialect == "postgresql":
        # PostgreSQL: Use UPDATE ... WHERE ... RETURNING * for atomic consumption
        result = db.execute(
            text(
                """
                UPDATE invite_tokens
                SET used_at = :now
                WHERE token = :token_str
                  AND used_at IS NULL
                  AND revoked_at IS NULL
                  AND expires_at > :now
                RETURNING *
            """
            ),
            {"token_str": token_str, "now": now},
        )
        row = result.fetchone()

        if not row:
            # Update affected 0 rows - token is invalid/reused/expired/revoked
            # Validate to get specific error message
            validate_invite_token(db, token_str)
            # If validation didn't raise, it means race condition (token was consumed between checks)
            raise TokenAlreadyUsedError(f"Token '{token_str}' was already consumed")

        # Refresh the token object from the database
        token = db.query(InviteToken).filter(InviteToken.token == token_str).first()
        db.commit()
        return token
    else:
        # SQLite: Use UPDATE ... WHERE ... then SELECT (no RETURNING support)
        result = db.execute(
            text(
                """
                UPDATE invite_tokens
                SET used_at = :now
                WHERE token = :token_str
                  AND used_at IS NULL
                  AND revoked_at IS NULL
                  AND expires_at > :now
            """
            ),
            {"token_str": token_str, "now": now},
        )

        # Check if update affected exactly 1 row
        if result.rowcount == 0:
            # Update affected 0 rows - token is invalid/reused/expired/revoked
            # Validate to get specific error message
            validate_invite_token(db, token_str)
            # If validation didn't raise, it means race condition (token was consumed between checks)
            raise TokenAlreadyUsedError(f"Token '{token_str}' was already consumed")

        # Update succeeded - fetch the updated token
        token = db.query(InviteToken).filter(InviteToken.token == token_str).first()
        db.commit()
        db.refresh(token)
        return token
