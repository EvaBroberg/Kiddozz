from datetime import timedelta
from typing import Any, Dict

from fastapi import APIRouter, Depends, Query

from app.core.deps import get_current_user
from app.core.security import create_access_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/switch-role")
def switch_role(
    role: str = Query(..., description="Role to switch to", pattern="^(parent|educator)$")
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
        data=token_data,
        expires_delta=timedelta(hours=24)
    )
    
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user_id": user_id,
        "role": role,
        "expires_in": 24 * 60 * 60  # 24 hours in seconds
    }


@router.get("/me")
def get_current_user_info(current_user: Dict[str, Any] = Depends(get_current_user)) -> Dict[str, Any]:
    """Get current user information from JWT token."""
    return {
        "user_id": current_user.get("user_id"),
        "role": current_user.get("role"),
        "exp": current_user.get("exp")
    }
