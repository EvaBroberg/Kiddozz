"""Service for managing invite tokens (validation and consumption)."""

import logging
import secrets
from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple

from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.roles import Role
from app.models.invite_token import InviteToken

logger = logging.getLogger(__name__)


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


def generate_invite_token(
    db: Session,
    daycare_id: str,
    role: str,
    email: str,
    ttl_minutes: int = 60 * 24 * 7,  # 7 days default
    created_by: Optional[str] = None,
) -> Tuple[InviteToken, str]:
    """
    Generate a new invite token.

    Args:
        db: Database session
        daycare_id: UUID of the daycare (tenant)
        role: Role string (must be valid Role enum value)
        email: Email address for the invite
        ttl_minutes: Time-to-live in minutes (default: 7 days)
        created_by: Optional JWT sub of the creator

    Returns:
        Tuple of (InviteToken object, raw_token_string)

    Raises:
        ValueError: If role is invalid or ttl_minutes is non-positive
    """
    # Validate role
    try:
        role_enum = Role.from_str(role)
    except ValueError as e:
        raise ValueError(f"Invalid role: {role}") from e

    if ttl_minutes <= 0:
        raise ValueError("ttl_minutes must be positive")

    # Generate cryptographically random token (32 bytes = 256 bits of entropy)
    max_retries = 5
    for attempt in range(max_retries):
        token_str = secrets.token_urlsafe(32)

        # Calculate expiration
        now = datetime.now(timezone.utc)
        expires_at = now + timedelta(minutes=ttl_minutes)

        # Create token record
        token = InviteToken(
            token=token_str,
            daycare_id=daycare_id,
            role=role_enum.value,
            email=email,
            expires_at=expires_at,
            created_by=created_by,
        )

        try:
            db.add(token)
            db.commit()
            db.refresh(token)

            # Log generation (never log full token)
            logger.info(
                f"Invite token generated: id={token.id}, "
                f"token_prefix='{token_str[:6]}...', role='{role_enum.value}', "
                f"email='{email}', daycare_id='{daycare_id[:8]}...', "
                f"expires_at='{expires_at.isoformat()}'"
            )

            return token, token_str

        except IntegrityError:
            # Token collision (very rare but handle it)
            db.rollback()
            if attempt == max_retries - 1:
                logger.error(
                    f"Failed to generate unique token after {max_retries} attempts"
                )
                raise ValueError(
                    "Failed to generate unique token after multiple attempts"
                )
            # Retry with new token
            continue

    # Should never reach here, but satisfy type checker
    raise RuntimeError("Token generation failed unexpectedly")


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
        logger.warning(
            f"Token validation failed: token not found (prefix='{token_str[:6]}...')"
        )
        raise InvalidTokenError(f"Token '{token_str}' does not exist")

    # Check if token is expired
    # Handle both timezone-aware and timezone-naive datetimes
    now = datetime.now(timezone.utc) if token.expires_at.tzinfo else datetime.now()
    if token.expires_at < now:
        logger.warning(
            f"Token validation failed: expired (id={token.id}, prefix='{token_str[:6]}...')"
        )
        raise ExpiredTokenError(f"Token '{token_str}' has expired")

    # Check if token has been revoked
    if token.revoked_at is not None:
        logger.warning(
            f"Token validation failed: revoked (id={token.id}, prefix='{token_str[:6]}...')"
        )
        raise RevokedTokenError(f"Token '{token_str}' has been revoked")

    # Check if token has already been used
    if token.used_at is not None:
        logger.warning(
            f"Token validation failed: already used (id={token.id}, prefix='{token_str[:6]}...')"
        )
        raise TokenAlreadyUsedError(f"Token '{token_str}' has already been used")

    # Validate role is a valid Role enum value
    try:
        Role.from_str(token.role)
    except ValueError:
        logger.warning(
            f"Token validation failed: invalid role (id={token.id}, prefix='{token_str[:6]}...', role='{token.role}')"
        )
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

        # Log consumption (never log full token)
        logger.info(
            f"Invite token consumed: id={token.id}, token_prefix='{token_str[:6]}...', "
            f"role='{token.role}', email='{token.email}'"
        )

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

        # Log consumption (never log full token)
        logger.info(
            f"Invite token consumed: id={token.id}, token_prefix='{token_str[:6]}...', "
            f"role='{token.role}', email='{token.email}'"
        )

        return token


def revoke_invite_token(
    db: Session, token_str: str, revoked_by: Optional[str] = None
) -> InviteToken:
    """
    Revoke an invite token.

    Args:
        db: Database session
        token_str: Token string to revoke
        revoked_by: Optional identifier of who revoked the token (for logging)

    Returns:
        InviteToken object that was revoked

    Raises:
        InvalidTokenError: If token does not exist
        TokenAlreadyUsedError: If token has already been used (cannot revoke used tokens)
    """
    token = db.query(InviteToken).filter(InviteToken.token == token_str).first()

    if not token:
        raise InvalidTokenError(f"Token '{token_str}' does not exist")

    # Cannot revoke a token that has already been used
    if token.used_at is not None:
        logger.warning(
            f"Token revocation rejected: already used (id={token.id}, prefix='{token_str[:6]}...')"
        )
        raise TokenAlreadyUsedError(
            f"Token '{token_str}' has already been used and cannot be revoked"
        )

    # Set revocation timestamp
    now = datetime.now(timezone.utc)
    token.revoked_at = now
    db.commit()
    db.refresh(token)

    # Log revocation (never log full token)
    revoked_by_str = revoked_by or "N/A"
    logger.info(
        f"Invite token revoked: id={token.id}, token_prefix='{token_str[:6]}...', "
        f"role='{token.role}', email='{token.email}', "
        f"revoked_by='{revoked_by_str}'"
    )

    return token
