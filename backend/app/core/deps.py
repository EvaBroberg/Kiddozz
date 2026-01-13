from typing import Any, Dict, Union

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer

from app.core.roles import Role
from app.core.security import decode_access_token

# OAuth2 scheme for token extraction
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/token")


def get_current_user(token: str = Depends(oauth2_scheme)) -> Dict[str, Any]:
    """
    Get the current user from the JWT token.

    Validates that the token contains a valid role and returns the payload.
    Raises 401 if role is missing or invalid.
    """
    try:
        payload = decode_access_token(token)
        user_id = payload.get("sub")  # Changed from "user_id" to "sub"
        if user_id is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authentication credentials",
            )

        # Validate role is present and valid
        role_str = payload.get("role")
        if not role_str:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Role not found in token",
            )

        if not Role.is_valid(role_str):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Invalid role in token: {role_str}",
            )

        return payload
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Could not validate credentials: {str(e)}",
        )


def require_role(role: Union[Role, str]):
    """
    Create a dependency that requires a specific role.

    Args:
        role: Role enum or role string (normalized to Role enum)

    Returns:
        Dependency function that checks role
    """
    # Normalize to Role enum
    if isinstance(role, str):
        try:
            role_enum = Role.from_str(role)
        except ValueError as e:
            raise ValueError(f"Invalid role for require_role: {role}") from e
    else:
        role_enum = role

    def role_checker(
        current_user: Dict[str, Any] = Depends(get_current_user),
    ) -> Dict[str, Any]:
        user_role_str = current_user.get("role")
        try:
            user_role = Role.from_str(user_role_str)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Invalid role in token: {user_role_str}",
            )

        if user_role != role_enum:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Access denied. Required role: {role_enum.value}, current role: {user_role.value}",
            )
        return current_user

    return role_checker


def require_any_role(*roles: Union[Role, str]):
    """
    Create a dependency that requires any of the specified roles.

    Args:
        *roles: Role enums or role strings (normalized to Role enums)

    Returns:
        Dependency function that checks if user has one of the roles
    """
    # Normalize all to Role enums
    role_enums = []
    for role in roles:
        if isinstance(role, str):
            try:
                role_enums.append(Role.from_str(role))
            except ValueError as e:
                raise ValueError(f"Invalid role for require_any_role: {role}") from e
        else:
            role_enums.append(role)

    def _require_any(
        current_user: Dict[str, Any] = Depends(get_current_user),
    ) -> Dict[str, Any]:
        user_role_str = current_user.get("role")
        try:
            user_role = Role.from_str(user_role_str)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Invalid role in token: {user_role_str}",
            )

        if user_role not in role_enums:
            allowed_values = [r.value for r in role_enums]
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Requires one of roles: {allowed_values}, current role: {user_role.value}",
            )
        return current_user

    return _require_any
