import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Dict

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.roles import Role
from app.core.security import create_access_token
from app.models.educator import Educator, EducatorRole
from app.models.parent import Parent
from app.schemas.auth import AcceptInviteRequest, AcceptInviteResponse, DevLoginRequest, TokenResponse
from app.services.invite_token_service import (
    ExpiredTokenError,
    InvalidTokenError,
    RevokedTokenError,
    TokenAlreadyUsedError,
    validate_invite_token,
)
from app.utils.daycare_resolver import resolve_daycare_id

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/auth", tags=["auth"])


class TestTokenRequest(BaseModel):
    role: str
    user_id: int = 1


@router.post("/switch-role")
def switch_role(
    role: str = Query(
        ...,
        description="Role to switch to",
        pattern="^(parent|educator|super_educator)$",
    )
) -> Dict[str, Any]:
    """
    Switch user role (staging only).
    This endpoint is for testing purposes and should be removed in production.
    """
    # In staging, we'll use a test user
    user_id = "test-user"

    # Create token data
    token_data = {
        "sub": user_id,
        "role": role,
    }

    # Create access token with 24 hour expiry for testing
    access_token = create_access_token(
        data=token_data, expires_delta=timedelta(hours=24)
    )

    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user_id": user_id,
        "role": role,
        "expires_in": 24 * 60 * 60,  # 24 hours in seconds
    }


@router.post("/test-token")
def test_token(request: TestTokenRequest) -> Dict[str, Any]:
    """
    Generate a test token for the specified role and user_id.
    Only available in staging environment.
    """
    # Check if environment is staging
    if settings.environment != "staging":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Endpoint not available in this environment",
        )

    # Validate role using Role enum
    try:
        role_enum = Role.from_str(request.role)
        role = role_enum.value
    except ValueError as e:
        valid_roles = [r.value for r in Role]
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid role. Allowed: {valid_roles}",
        ) from e

    # Create token data
    token_data = {
        "sub": str(request.user_id),
        "role": role,
    }

    # Create access token
    access_token = create_access_token(data=token_data)

    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user_id": request.user_id,
        "role": role,
    }


@router.post("/dev-login", response_model=TokenResponse)
def dev_login(payload: DevLoginRequest, db: Session = Depends(get_db)):
    # Import settings at the beginning
    from app.core.config import settings

    # Disabled in production
    if settings.environment == "production":
        raise HTTPException(
            status_code=403, detail="Dev login is disabled in production"
        )

    if (payload.educator_id and payload.parent_id) or (
        not payload.educator_id and not payload.parent_id
    ):
        raise HTTPException(
            status_code=400, detail="Provide exactly one of educator_id or parent_id"
        )

    role = None
    sub = None
    daycare_id = None
    groups = []

    if payload.educator_id:
        edu = db.query(Educator).get(payload.educator_id)
        if not edu:
            raise HTTPException(status_code=404, detail="Educator not found")
        # Map EducatorRole to Role enum
        if edu.role == "educator":
            role = Role.EDUCATOR.value
        else:
            role = Role.SUPER_EDUCATOR.value
        sub = str(edu.id)
        daycare_id = resolve_daycare_id(db, str(edu.daycare_id))
        groups = [g.name for g in edu.groups]

    if payload.parent_id:
        par = db.query(Parent).get(payload.parent_id)
        if not par:
            raise HTTPException(status_code=404, detail="Parent not found")
        role = Role.PARENT.value
        sub = str(par.id)
        daycare_id = resolve_daycare_id(db, str(par.daycare_id))
        # parents don't have groups directly, but you can derive via their kids if you wish; leave empty for now or compute later
        groups = []

    token = create_access_token(
        data={"sub": sub, "role": role, "daycare_id": daycare_id, "groups": groups}
    )

    # Debug: Print secret key being used
    print(f"Dev-login using secret key: {settings.secret_key}")
    print(f"Dev-login environment: {settings.app_env}")

    return {"access_token": token, "token_type": "bearer"}


@router.get("/me")
def get_current_user_info(
    current_user: Dict[str, Any] = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Get current user information from JWT token.

    Returns server-authoritative user identity:
    - user_id (sub claim)
    - role (validated against Role enum)
    - daycare_id (if present in token)
    - exp (token expiration)
    """
    return {
        "user_id": current_user.get("sub"),
        "role": current_user.get("role"),
        "daycare_id": current_user.get("daycare_id"),
        "exp": current_user.get("exp"),
    }


@router.get("/me/educator")
def get_current_educator_info(
    current_user: Dict[str, Any] = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Get current educator information including groups."""
    role_str = current_user.get("role")
    try:
        role = Role.from_str(role_str)
    except ValueError:
        raise HTTPException(status_code=403, detail=f"Invalid role: {role_str}")

    if role not in [Role.EDUCATOR, Role.SUPER_EDUCATOR]:
        raise HTTPException(
            status_code=403, detail="This endpoint is only available for educators"
        )

    educator_id = current_user.get("sub")  # JWT sub field contains the educator ID
    if not educator_id:
        raise HTTPException(status_code=400, detail="Educator ID not found in token")

    educator = db.query(Educator).filter(Educator.id == educator_id).first()
    if not educator:
        raise HTTPException(status_code=404, detail="Educator not found")

    return educator


@router.post("/accept-invite", response_model=AcceptInviteResponse)
def accept_invite(
    payload: AcceptInviteRequest, db: Session = Depends(get_db)
) -> AcceptInviteResponse:
    """
    Accept an invite token and create a user account.

    Validates and consumes the invite token atomically, creates the appropriate
    user (Educator or Parent) linked to the daycare, and returns a JWT session.

    Args:
        payload: AcceptInviteRequest with token, name, and optional phone_num
        db: Database session

    Returns:
        AcceptInviteResponse with access_token, user_id, role, and daycare_id

    Raises:
        HTTPException 400: Invalid, expired, revoked, or already used token
        HTTPException 409: Email already exists (duplicate user)
    """
    token_str = payload.token

    # 1. Validate token (without consuming) to get specific error messages
    try:
        invite_token = validate_invite_token(db, token_str)
    except InvalidTokenError as e:
        logger.warning(f"Invite acceptance failed: invalid token (prefix='{token_str[:6]}...')")
        raise HTTPException(status_code=400, detail=str(e))
    except ExpiredTokenError as e:
        logger.warning(f"Invite acceptance failed: expired token (prefix='{token_str[:6]}...')")
        raise HTTPException(status_code=400, detail=str(e))
    except RevokedTokenError as e:
        logger.warning(f"Invite acceptance failed: revoked token (prefix='{token_str[:6]}...')")
        raise HTTPException(status_code=400, detail=str(e))
    except TokenAlreadyUsedError as e:
        logger.warning(f"Invite acceptance failed: already used token (prefix='{token_str[:6]}...')")
        raise HTTPException(status_code=400, detail=str(e))

    # 2. Validate required fields BEFORE consuming token
    role_enum = Role.from_str(invite_token.role)
    
    # Check for missing name
    if not payload.name or not payload.name.strip():
        logger.warning(f"Invite acceptance failed: missing name (prefix='{token_str[:6]}...')")
        raise HTTPException(status_code=422, detail="name is required")
    
    # Check for missing phone_num for Parent
    if role_enum == Role.PARENT:
        if not payload.phone_num or not payload.phone_num.strip():
            logger.warning(f"Invite acceptance failed: missing phone_num for parent (prefix='{token_str[:6]}...')")
            raise HTTPException(status_code=422, detail="phone_num is required for parent accounts")
    
    # 3. Check for duplicate email before consuming token
    if role_enum in [Role.EDUCATOR, Role.SUPER_EDUCATOR]:
        existing_educator = db.query(Educator).filter(Educator.email == invite_token.email).first()
        if existing_educator:
            logger.warning(
                f"Invite acceptance failed: email already exists (email='{invite_token.email}', role='{invite_token.role}')"
            )
            raise HTTPException(
                status_code=409,
                detail=f"Email '{invite_token.email}' is already registered as an educator",
            )
    elif role_enum == Role.PARENT:
        existing_parent = db.query(Parent).filter(Parent.email == invite_token.email).first()
        if existing_parent:
            logger.warning(
                f"Invite acceptance failed: email already exists (email='{invite_token.email}', role='{invite_token.role}')"
            )
            raise HTTPException(
                status_code=409,
                detail=f"Email '{invite_token.email}' is already registered as a parent",
            )

    # 4. Atomically consume token (within transaction, no commit yet)
    now = datetime.now(timezone.utc) if invite_token.expires_at.tzinfo else datetime.now()
    bind = db.bind
    dialect = bind.dialect.name

    if dialect == "postgresql":
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
            # Token was consumed between validation and consumption (race condition)
            raise HTTPException(status_code=400, detail="Token was already used")
    else:
        # SQLite
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
        if result.rowcount == 0:
            # Token was consumed between validation and consumption (race condition)
            raise HTTPException(status_code=400, detail="Token was already used")

    # Refresh token object
    db.refresh(invite_token)

    # 5. Create user record linked to daycare
    user_id = None
    role_value = None

    try:
        if role_enum in [Role.EDUCATOR, Role.SUPER_EDUCATOR]:
            # Map Role enum to EducatorRole enum value
            if role_enum == Role.SUPER_EDUCATOR:
                educator_role = EducatorRole.SUPER_EDUCATOR.value
            else:
                educator_role = EducatorRole.EDUCATOR.value

            educator = Educator(
                full_name=payload.name.strip(),
                email=invite_token.email,
                role=educator_role,
                daycare_id=invite_token.daycare_id,
                phone_num=payload.phone_num.strip() if payload.phone_num else None,  # Optional for Educator
            )
            db.add(educator)
            db.flush()  # Flush to get ID without committing
            db.refresh(educator)
            user_id = str(educator.id)
            role_value = role_enum.value

        elif role_enum == Role.PARENT:
            # phone_num already validated above
            parent = Parent(
                full_name=payload.name.strip(),
                email=invite_token.email,
                phone_num=payload.phone_num.strip(),
                daycare_id=invite_token.daycare_id,
            )
            db.add(parent)
            db.flush()  # Flush to get ID without committing
            db.refresh(parent)
            user_id = str(parent.id)
            role_value = Role.PARENT.value

        # 6. Commit transaction (token consumption + user creation)
        db.commit()

        # 7. Issue JWT
        token_data = {
            "sub": user_id,
            "role": role_value,
            "daycare_id": invite_token.daycare_id,
            "groups": [],  # Empty groups for new users
        }
        access_token = create_access_token(data=token_data)

        logger.info(
            f"Invite accepted successfully: user_id={user_id}, role='{role_value}', "
            f"email='{invite_token.email}', token_id={invite_token.id}"
        )

        return AcceptInviteResponse(
            access_token=access_token,
            token_type="bearer",
            user_id=user_id,
            role=role_value,
            daycare_id=invite_token.daycare_id,
        )

    except IntegrityError as e:
        db.rollback()
        # Check if it's a duplicate email error
        if "email" in str(e).lower() or "unique" in str(e).lower():
            logger.warning(
                f"Invite acceptance failed: duplicate email (email='{invite_token.email}')"
            )
            raise HTTPException(
                status_code=409,
                detail=f"Email '{invite_token.email}' is already registered",
            )
        # Other integrity errors
        logger.error(f"Invite acceptance failed: database integrity error: {e}")
        raise HTTPException(status_code=500, detail="Failed to create user account")
    except HTTPException:
        db.rollback()
        raise
    except Exception as e:
        db.rollback()
        logger.error(f"Invite acceptance failed: unexpected error: {e}")
        raise HTTPException(status_code=500, detail="Failed to accept invite")


@router.post("/logout")
def logout(current_user: Dict[str, Any] = Depends(get_current_user)) -> Dict[str, str]:
    """
    Stateless logout endpoint.
    Clears client-side session only.
    """
    return {"message": "Logged out successfully"}
