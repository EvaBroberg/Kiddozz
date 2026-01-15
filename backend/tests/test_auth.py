import os
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from jose import jwt

from app.core.security import create_access_token, decode_access_token
from app.main import app

client = TestClient(app)


@pytest.fixture
def client_fixture():
    """Test client fixture."""
    return client


class TestJWTSecurity:
    """Test JWT token creation and decoding functionality."""

    def test_create_access_token_generates_valid_jwt(self):
        """Test that create_access_token generates a valid JWT with correct claims."""
        # Test data
        test_data = {"sub": "test-user-123", "role": "educator"}

        # Create token
        token = create_access_token(test_data)

        # Verify token is a string
        assert isinstance(token, str)
        assert len(token) > 0

        # Decode without verification to check structure
        from app.core.config import settings

        decoded = jwt.decode(
            token, settings.secret_key, options={"verify_signature": False}
        )

        # Verify required claims are present
        assert "sub" in decoded
        assert "role" in decoded
        assert "exp" in decoded

        # Verify claim values
        assert decoded["sub"] == "test-user-123"
        assert decoded["role"] == "educator"

        # Verify exp is in the future
        exp_timestamp = decoded["exp"]
        exp_datetime = datetime.fromtimestamp(exp_timestamp, tz=timezone.utc)
        assert exp_datetime > datetime.now(timezone.utc)

    def test_create_access_token_with_custom_expiry(self):
        """Test that create_access_token works with custom expiry time."""
        test_data = {"user_id": "test-user", "role": "parent"}
        custom_expiry = timedelta(minutes=5)

        token = create_access_token(test_data, expires_delta=custom_expiry)

        # Decode and verify expiry
        from app.core.config import settings

        decoded = jwt.decode(
            token, settings.secret_key, options={"verify_signature": False}
        )
        exp_timestamp = decoded["exp"]
        exp_datetime = datetime.fromtimestamp(exp_timestamp, tz=timezone.utc)

        # Verify that the expiry is in the future and reasonable
        now = datetime.now(timezone.utc)
        assert exp_datetime > now  # Should be in the future
        assert exp_datetime < now + timedelta(minutes=10)  # Should be within 10 minutes

    def test_decode_access_token_valid_token(self):
        """Test that decode_access_token correctly extracts claims from valid token."""
        test_data = {"sub": "test-user-456", "role": "parent"}

        token = create_access_token(test_data)
        decoded = decode_access_token(token)

        # Verify all claims are correctly extracted
        assert decoded["sub"] == "test-user-456"
        assert decoded["role"] == "parent"
        assert "exp" in decoded

    def test_decode_access_token_invalid_token_fails(self):
        """Test that decode_access_token fails with invalid token."""
        # Test with malformed token
        with pytest.raises(Exception):  # Should raise HTTPException
            decode_access_token("invalid.token.here")

        # Test with modified token
        valid_token = create_access_token({"user_id": "test", "role": "educator"})
        modified_token = valid_token[:-5] + "xxxxx"  # Modify last part

        with pytest.raises(Exception):  # Should raise HTTPException
            decode_access_token(modified_token)

    def test_decode_access_token_expired_token_fails(self):
        """Test that decode_access_token fails with expired token."""
        # Create token with past expiry
        past_time = datetime.now(timezone.utc) - timedelta(hours=1)
        expired_data = {"user_id": "test-user", "role": "educator", "exp": past_time}

        # Manually create expired token
        from app.core.config import settings

        expired_token = jwt.encode(
            expired_data, settings.secret_key, algorithm=settings.algorithm
        )

        with pytest.raises(Exception):  # Should raise HTTPException
            decode_access_token(expired_token)


class TestAuthEndpoints:
    """Test authentication endpoints."""

    def test_get_me_with_valid_token(self, auth_token_via_invite):
        """Test /auth/me returns user info with valid token."""
        auth = auth_token_via_invite(role="educator")
        token = auth["access_token"]

        # Test /me endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/auth/me", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify user info
        assert data["user_id"] == auth["user_id"]
        assert data["role"] == "educator"
        assert "exp" in data

    def test_get_me_with_parent_token(self, auth_token_via_invite):
        """Test /auth/me returns parent info with parent token."""
        auth = auth_token_via_invite(role="parent", phone_num="+358000000000")
        token = auth["access_token"]

        # Test /me endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/auth/me", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify user info
        assert data["user_id"] == auth["user_id"]
        assert data["role"] == "parent"
        assert "exp" in data

    def test_get_me_without_token_fails(self):
        """Test /auth/me fails without token."""
        response = client.get("/api/v1/auth/me")

        assert response.status_code == 401
        assert "Not authenticated" in response.json()["detail"]

    def test_get_me_with_invalid_token_fails(self):
        """Test /auth/me fails with invalid token."""
        headers = {"Authorization": "Bearer invalid.token.here"}
        response = client.get("/api/v1/auth/me", headers=headers)

        assert response.status_code == 401
        assert "Invalid authentication credentials" in response.json()["detail"]


class TestRoleBasedAccessControl:
    """Test role-based access control middleware."""

    def test_educator_only_endpoint_with_educator_token_succeeds(self, auth_token_via_invite):
        """Test educator-only endpoint allows educator access."""
        auth = auth_token_via_invite(role="educator")
        token = auth["access_token"]

        # Access protected endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/events/educator-only", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify response
        assert data["message"] == "Educator-only endpoint"
        assert "user" in data
        assert data["user"]["role"] == "educator"

    def test_educator_only_endpoint_with_parent_token_fails(self, auth_token_via_invite):
        """Test educator-only endpoint denies parent access."""
        auth = auth_token_via_invite(role="parent", phone_num="+358000000000")
        token = auth["access_token"]

        # Access protected endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/events/educator-only", headers=headers)

        assert response.status_code == 403
        data = response.json()

        # Verify error message
        assert "Requires one of roles:" in data["detail"]
        assert "educator" in data["detail"]
        assert "super_educator" in data["detail"]

    def test_educator_only_endpoint_without_token_fails(self):
        """Test educator-only endpoint fails without token."""
        response = client.get("/api/v1/events/educator-only")

        assert response.status_code == 401
        assert "Not authenticated" in response.json()["detail"]

    def test_educator_only_endpoint_with_invalid_token_fails(self):
        """Test educator-only endpoint fails with invalid token."""
        headers = {"Authorization": "Bearer invalid.token.here"}
        response = client.get("/api/v1/events/educator-only", headers=headers)

        assert response.status_code == 401
        assert "Invalid authentication credentials" in response.json()["detail"]

    def test_educator_only_endpoint_with_super_educator_token_succeeds(self, auth_token_via_invite):
        """Test educator-only endpoint allows super_educator access."""
        auth = auth_token_via_invite(role="super_educator")
        token = auth["access_token"]

        # Access protected endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/events/educator-only", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify response
        assert data["message"] == "Educator-only endpoint"
        assert "user" in data
        assert data["user"]["role"] == "super_educator"


class TestTokenExpiry:
    """Test token expiry functionality."""

    @patch.dict(os.environ, {"ACCESS_TOKEN_EXPIRE_MINUTES": "1"})
    def test_token_expiry_with_short_expiry(self):
        """Test token expiry with short expiry time."""
        # This test would require time manipulation to be fully effective
        # For now, we'll just test that the token is created with the correct expiry
        test_data = {"user_id": "test-user", "role": "educator"}
        token = create_access_token(test_data)

        # Decode and verify expiry is set
        from app.core.config import settings

        decoded = jwt.decode(
            token, settings.secret_key, options={"verify_signature": False}
        )
        assert "exp" in decoded

        # Verify token is valid (not expired yet)
        decoded_valid = decode_access_token(token)
        assert decoded_valid["user_id"] == "test-user"
        assert decoded_valid["role"] == "educator"


def test_logout(client_fixture, auth_token_via_invite):
    """Test logout endpoint returns success message."""
    auth = auth_token_via_invite(role="educator")
    token = auth["access_token"]

    # Test logout endpoint
    headers = {"Authorization": f"Bearer {token}"}
    response = client_fixture.post("/api/v1/auth/logout", headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert data["message"] == "Logged out successfully"
