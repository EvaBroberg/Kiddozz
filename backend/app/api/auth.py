import logging
from datetime import timedelta
from typing import Any, Dict, List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.security import create_access_token
from app.models.user import User

# Set up logging
logger = logging.getLogger(__name__)

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
    If users don't exist, creates them automatically.
    """
    logger.info("Dummy users endpoint called")

    try:
        # Query users from database with timeout handling
        try:
            users = (
                db.query(User).filter(User.name.in_(["Jessica", "Sara", "Mervi"])).all()
            )
            logger.info(f"Found {len(users)} existing dummy users")
        except SQLAlchemyError as db_error:
            logger.error(f"Database query failed: {str(db_error)}")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="DB connection failed",
            )

        # If no users exist, create them
        if not users:
            logger.info("No dummy users found, creating them")
            try:
                from app.services.user_service import insert_dummy_users

                insert_dummy_users(db)
                # Query again after creation
                users = (
                    db.query(User)
                    .filter(User.name.in_(["Jessica", "Sara", "Mervi"]))
                    .all()
                )
                logger.info(f"Created {len(users)} dummy users")
            except SQLAlchemyError as create_error:
                logger.error(f"Failed to create dummy users: {str(create_error)}")
                raise HTTPException(
                    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                    detail="DB connection failed",
                )

        # Convert to response format
        user_responses = []
        for user in users:
            user_responses.append(
                DummyUserResponse(
                    id=user.id, name=user.name, role=user.role, token=user.jwt_token
                )
            )

        logger.info("Dummy users fetched successfully")
        return DummyUsersResponse(users=user_responses)

    except HTTPException:
        # Re-raise HTTP exceptions as-is
        raise
    except Exception as e:
        # Log unexpected errors
        logger.error(f"Unexpected error in get_dummy_users: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to fetch dummy users",
        )
