from datetime import timedelta
from typing import Any, Dict, List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.security import create_access_token
from app.models.user import User

router = APIRouter(prefix="/auth", tags=["auth"])


class TestTokenRequest(BaseModel):
    role: str
    user_id: int = 1


class DummyUserResponse(BaseModel):
    id: int
    name: str
    role: str
    token: str


class DummyUsersResponse(BaseModel):
    users: List[DummyUserResponse]


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


@router.get("/dummy-users", response_model=DummyUsersResponse)
def get_dummy_users(db: Session = Depends(get_db)) -> DummyUsersResponse:
    """
    Get dummy users with JWT tokens from the database.
    Returns users: Jessica (educator), Sara (parent), Mervi (super_educator).
    """
    # Query users from database
    users = db.query(User).filter(User.name.in_(["Jessica", "Sara", "Mervi"])).all()
    
    if not users:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Dummy users not found in database"
        )
    
    # Convert to response format
    user_responses = []
    for user in users:
        user_responses.append(DummyUserResponse(
            id=user.id,
            name=user.name,
            role=user.role,
            token=user.jwt_token
        ))
    
    return DummyUsersResponse(users=user_responses)
