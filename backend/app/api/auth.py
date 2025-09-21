from datetime import timedelta
from typing import Any, Dict

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.security import create_access_token
from app.models.educator import Educator
from app.models.parent import Parent
from app.schemas.auth import DevLoginRequest, TokenResponse
from app.utils.daycare_resolver import resolve_daycare_id

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
        "user_id": user_id,
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

    # Define allowed roles
    ALLOWED_ROLES = {"parent", "educator", "super_educator"}
    role = request.role.lower().strip()

    # Validate role
    if role not in ALLOWED_ROLES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid role. Allowed: {sorted(ALLOWED_ROLES)}",
        )

    # Create token data
    token_data = {
        "user_id": str(request.user_id),
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
        role = "educator" if edu.role == "educator" else "super_educator"
        sub = str(edu.id)
        daycare_id = resolve_daycare_id(db, str(edu.daycare_id))
        groups = [g.name for g in edu.groups]

    if payload.parent_id:
        par = db.query(Parent).get(payload.parent_id)
        if not par:
            raise HTTPException(status_code=404, detail="Parent not found")
        role = "parent"
        sub = str(par.id)
        daycare_id = resolve_daycare_id(db, str(par.daycare_id))
        # parents don't have groups directly, but you can derive via their kids if you wish; leave empty for now or compute later
        groups = []

    token = create_access_token(
        data={"sub": sub, "role": role, "daycare_id": daycare_id, "groups": groups}
    )
    return {"access_token": token, "token_type": "bearer"}


@router.get("/me")
def get_current_user_info(
    current_user: Dict[str, Any] = Depends(get_current_user),
) -> Dict[str, Any]:
    """Get current user information from JWT token."""
    return {
        "user_id": current_user.get("user_id"),
        "role": current_user.get("role"),
        "exp": current_user.get("exp"),
    }
