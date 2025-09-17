import os
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient
from jose import jwt

from app.core.security import create_access_token, decode_access_token
from app.main import app
from app.services.user_service import insert_dummy_users

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
        test_data = {"user_id": "test-user-123", "role": "educator"}

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
        assert "user_id" in decoded
        assert "role" in decoded
        assert "exp" in decoded

        # Verify claim values
        assert decoded["user_id"] == "test-user-123"
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
        test_data = {"user_id": "test-user-456", "role": "parent"}

        token = create_access_token(test_data)
        decoded = decode_access_token(token)

        # Verify all claims are correctly extracted
        assert decoded["user_id"] == "test-user-456"
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

    def test_switch_role_educator_returns_valid_jwt(self):
        """Test switching to educator role returns valid JWT."""
        response = client.post("/api/v1/auth/switch-role?role=educator")

        assert response.status_code == 200
        data = response.json()

        # Verify response structure
        assert "access_token" in data
        assert "token_type" in data
        assert "user_id" in data
        assert "role" in data
        assert "expires_in" in data

        # Verify values
        assert data["token_type"] == "bearer"
        assert data["user_id"] == "test-user"
        assert data["role"] == "educator"
        assert data["expires_in"] == 24 * 60 * 60  # 24 hours

        # Verify token is valid
        token = data["access_token"]
        decoded = decode_access_token(token)
        assert decoded["user_id"] == "test-user"
        assert decoded["role"] == "educator"

    def test_switch_role_parent_returns_valid_jwt(self):
        """Test switching to parent role returns valid JWT."""
        response = client.post("/api/v1/auth/switch-role?role=parent")

        assert response.status_code == 200
        data = response.json()

        # Verify response structure
        assert "access_token" in data
        assert data["role"] == "parent"

        # Verify token is valid
        token = data["access_token"]
        decoded = decode_access_token(token)
        assert decoded["user_id"] == "test-user"
        assert decoded["role"] == "parent"

    def test_switch_role_invalid_role_fails(self):
        """Test switching to invalid role fails."""
        response = client.post("/api/v1/auth/switch-role?role=invalid")

        assert response.status_code == 422  # Validation error

    def test_switch_role_missing_role_fails(self):
        """Test switching without role parameter fails."""
        response = client.post("/api/v1/auth/switch-role")

        assert response.status_code == 422  # Validation error

    def test_get_me_with_valid_token(self):
        """Test /auth/me returns user info with valid token."""
        # Get educator token
        switch_response = client.post("/api/v1/auth/switch-role?role=educator")
        token = switch_response.json()["access_token"]

        # Test /me endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/auth/me", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify user info
        assert data["user_id"] == "test-user"
        assert data["role"] == "educator"
        assert "exp" in data

    def test_get_me_with_parent_token(self):
        """Test /auth/me returns parent info with parent token."""
        # Get parent token
        switch_response = client.post("/api/v1/auth/switch-role?role=parent")
        token = switch_response.json()["access_token"]

        # Test /me endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/auth/me", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify user info
        assert data["user_id"] == "test-user"
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

    def test_educator_only_endpoint_with_educator_token_succeeds(self):
        """Test educator-only endpoint allows educator access."""
        # Get educator token
        switch_response = client.post("/api/v1/auth/switch-role?role=educator")
        token = switch_response.json()["access_token"]

        # Access protected endpoint
        headers = {"Authorization": f"Bearer {token}"}
        response = client.get("/api/v1/events/educator-only", headers=headers)

        assert response.status_code == 200
        data = response.json()

        # Verify response
        assert data["message"] == "Educator-only endpoint"
        assert "user" in data
        assert data["user"]["role"] == "educator"

    def test_educator_only_endpoint_with_parent_token_fails(self):
        """Test educator-only endpoint denies parent access."""
        # Get parent token
        switch_response = client.post("/api/v1/auth/switch-role?role=parent")
        token = switch_response.json()["access_token"]

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

    def test_educator_only_endpoint_with_super_educator_token_succeeds(self):
        """Test educator-only endpoint allows super_educator access."""
        # Get super_educator token
        switch_response = client.post("/api/v1/auth/switch-role?role=super_educator")
        token = switch_response.json()["access_token"]

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


class TestTestTokenEndpoint:
    """Test the /auth/test-token endpoint."""

    def test_test_token_parent_role_in_staging(self):
        """Test issuing a parent token in staging environment."""
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "staging"

            response = client.post(
                "/api/v1/auth/test-token", json={"role": "parent", "user_id": 123}
            )

            assert response.status_code == 200
            data = response.json()

            # Verify response structure
            assert "access_token" in data
            assert data["token_type"] == "bearer"
            assert data["user_id"] == 123
            assert data["role"] == "parent"

            # Verify token is valid and has correct claims
            token = data["access_token"]
            decoded = decode_access_token(token)
            assert decoded["user_id"] == "123"
            assert decoded["role"] == "parent"

    def test_test_token_educator_role_in_staging(self):
        """Test issuing an educator token in staging environment."""
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "staging"

            response = client.post(
                "/api/v1/auth/test-token", json={"role": "educator", "user_id": 456}
            )

            assert response.status_code == 200
            data = response.json()

            # Verify response structure
            assert "access_token" in data
            assert data["token_type"] == "bearer"
            assert data["user_id"] == 456
            assert data["role"] == "educator"

            # Verify token is valid and has correct claims
            token = data["access_token"]
            decoded = decode_access_token(token)
            assert decoded["user_id"] == "456"
            assert decoded["role"] == "educator"

    def test_test_token_default_user_id(self):
        """Test issuing a token with default user_id."""
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "staging"

            response = client.post("/api/v1/auth/test-token", json={"role": "parent"})

            assert response.status_code == 200
            data = response.json()

            # Verify default user_id is used
            assert data["user_id"] == 1
            assert data["role"] == "parent"

            # Verify token has correct claims
            token = data["access_token"]
            decoded = decode_access_token(token)
            assert decoded["user_id"] == "1"
            assert decoded["role"] == "parent"

    def test_test_token_super_educator_role_in_staging(self):
        """Test issuing a super_educator token in staging environment."""
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "staging"

            response = client.post(
                "/api/v1/auth/test-token",
                json={"role": "super_educator", "user_id": 789},
            )

            assert response.status_code == 200
            data = response.json()

            # Verify response structure
            assert "access_token" in data
            assert data["token_type"] == "bearer"
            assert data["user_id"] == 789
            assert data["role"] == "super_educator"

            # Verify token is valid and has correct claims
            token = data["access_token"]
            decoded = decode_access_token(token)
            assert decoded["user_id"] == "789"
            assert decoded["role"] == "super_educator"

    def test_test_token_invalid_role(self):
        """Test issuing a token with invalid role."""
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "staging"

            response = client.post(
                "/api/v1/auth/test-token", json={"role": "hacker", "user_id": 123}
            )

            assert response.status_code == 400
            data = response.json()
            assert "Invalid role. Allowed:" in data["detail"]
            assert "parent" in data["detail"]
            assert "educator" in data["detail"]
            assert "super_educator" in data["detail"]

    def test_test_token_in_non_staging_environment(self):
        """Test that endpoint returns 404 in non-staging environment."""
        # Mock the settings to return "development" environment
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "development"

            response = client.post(
                "/api/v1/auth/test-token", json={"role": "parent", "user_id": 123}
            )

            assert response.status_code == 404
            data = response.json()
            assert "Endpoint not available in this environment" in data["detail"]

    def test_test_token_in_production_environment(self):
        """Test that endpoint returns 404 in production environment."""
        with patch("app.api.auth.settings") as mock_settings:
            mock_settings.environment = "production"

            response = client.post(
                "/api/v1/auth/test-token", json={"role": "parent", "user_id": 123}
            )

            assert response.status_code == 404
            data = response.json()
            assert "Endpoint not available in this environment" in data["detail"]


class TestDummyUsersEndpoint:
    """Test the /auth/dummy-users endpoint."""

    def test_get_dummy_users_returns_three_users(self, client_fixture):
        """Test that the endpoint returns exactly 3 users."""
        response = client_fixture.get("/api/v1/auth/dummy-users")

        assert response.status_code == 200
        data = response.json()

        assert "users" in data
        assert len(data["users"]) == 3

        # Check that we have the expected users
        user_names = [user["name"] for user in data["users"]]
        assert "Jessica" in user_names
        assert "Sara" in user_names
        assert "Mervi" in user_names

    def test_dummy_users_have_valid_jwt_tokens(self, client_fixture):
        """Test that each user has a valid JWT token."""
        response = client_fixture.get("/api/v1/auth/dummy-users")

        assert response.status_code == 200
        data = response.json()

        for user in data["users"]:
            assert "token" in user
            assert user["token"] is not None
            assert len(user["token"]) > 0

            # Verify token is a valid JWT format (has 3 parts separated by dots)
            token_parts = user["token"].split(".")
            assert len(token_parts) == 3

    def test_dummy_users_jwt_tokens_decode_correctly(
        self, client_fixture
    ):
        """Test that JWT tokens decode correctly and match database roles."""
        from app.core.security import decode_access_token

        response = client_fixture.get("/api/v1/auth/dummy-users")

        assert response.status_code == 200
        data = response.json()

        for user in data["users"]:
            # Decode the JWT token
            decoded_token = decode_access_token(user["token"])

            # Verify the decoded claims match the database values
            assert decoded_token["role"] == user["role"]
            assert decoded_token["sub"] == user["name"]  # 'sub' contains the name
            assert decoded_token["user_id"] == user["id"]
            assert "exp" in decoded_token  # Should have expiration

    def test_dummy_users_have_correct_roles(self, client_fixture):
        """Test that users have the correct roles."""
        response = client_fixture.get("/api/v1/auth/dummy-users")

        assert response.status_code == 200
        data = response.json()

        # Find users by name and check their roles
        users_by_name = {user["name"]: user for user in data["users"]}

        assert users_by_name["Jessica"]["role"] == "educator"
        assert users_by_name["Sara"]["role"] == "parent"
        assert users_by_name["Mervi"]["role"] == "super_educator"

    def test_dummy_users_have_valid_ids(self, client_fixture):
        """Test that users have valid integer IDs."""
        response = client_fixture.get("/api/v1/auth/dummy-users")

        assert response.status_code == 200
        data = response.json()

        for user in data["users"]:
            assert "id" in user
            assert isinstance(user["id"], int)
            assert user["id"] > 0

    def test_dummy_users_response_structure(self, client_fixture):
        """Test that the response has the correct structure."""
        response = client_fixture.get("/api/v1/auth/dummy-users")

        assert response.status_code == 200
        data = response.json()

        # Check top-level structure
        assert "users" in data
        assert isinstance(data["users"], list)

        # Check each user has required fields
        for user in data["users"]:
            required_fields = ["id", "name", "role", "token"]
            for field in required_fields:
                assert field in user
                assert user[field] is not None
