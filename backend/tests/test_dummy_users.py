from unittest.mock import MagicMock, patch

from app.core.database import get_db
from app.models.user import User

# Use the global test client and database from conftest.py
from tests.conftest import TestingSessionLocal, app, client, override_get_db


def test_get_dummy_users_success_api_v1():
    """Test that /api/v1/auth/dummy-users returns 200 and includes the 3 users"""
    response = client.get("/api/v1/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()

    # Check response structure
    assert "users" in data
    assert len(data["users"]) == 3

    # Check that all expected users are present
    user_names = [user["name"] for user in data["users"]]
    assert "Jessica" in user_names
    assert "Sara" in user_names
    assert "Mervi" in user_names

    # Check user structure
    for user in data["users"]:
        assert "id" in user
        assert "name" in user
        assert "role" in user
        assert "token" in user
        assert user["id"] is not None
        assert user["name"] in ["Jessica", "Sara", "Mervi"]
        assert user["role"] in ["educator", "parent", "super_educator"]
        assert user["token"] is not None
        assert len(user["token"]) > 0


def test_get_dummy_users_success_no_prefix():
    """Test that /auth/dummy-users returns 200 and includes the 3 users (Android compatibility)"""
    response = client.get("/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()

    # Check response structure
    assert "users" in data
    assert len(data["users"]) == 3

    # Check that all expected users are present
    user_names = [user["name"] for user in data["users"]]
    assert "Jessica" in user_names
    assert "Sara" in user_names
    assert "Mervi" in user_names

    # Check user structure
    for user in data["users"]:
        assert "id" in user
        assert "name" in user
        assert "role" in user
        assert "token" in user
        assert user["id"] is not None
        assert user["name"] in ["Jessica", "Sara", "Mervi"]
        assert user["role"] in ["educator", "parent", "super_educator"]
        assert user["token"] is not None
        assert len(user["token"]) > 0


def test_both_routes_return_same_data():
    """Test that both /auth/dummy-users and /api/v1/auth/dummy-users return identical data"""
    response_no_prefix = client.get("/auth/dummy-users")
    response_with_prefix = client.get("/api/v1/auth/dummy-users")

    assert response_no_prefix.status_code == 200
    assert response_with_prefix.status_code == 200

    data_no_prefix = response_no_prefix.json()
    data_with_prefix = response_with_prefix.json()

    # Both should have same structure
    assert data_no_prefix.keys() == data_with_prefix.keys()
    assert len(data_no_prefix["users"]) == len(data_with_prefix["users"])

    # Sort users by name for comparison
    users_no_prefix = sorted(data_no_prefix["users"], key=lambda x: x["name"])
    users_with_prefix = sorted(data_with_prefix["users"], key=lambda x: x["name"])

    # Compare each user
    for user_no_prefix, user_with_prefix in zip(users_no_prefix, users_with_prefix):
        assert user_no_prefix["name"] == user_with_prefix["name"]
        assert user_no_prefix["role"] == user_with_prefix["role"]
        # IDs might be different due to database state, but names and roles should match


def test_get_dummy_users_no_duplicates():
    """Test that calling twice does not duplicate users"""
    # First call
    response1 = client.get("/api/v1/auth/dummy-users")
    assert response1.status_code == 200
    data1 = response1.json()

    # Second call
    response2 = client.get("/api/v1/auth/dummy-users")
    assert response2.status_code == 200
    data2 = response2.json()

    # Should have same number of users
    assert len(data1["users"]) == len(data2["users"]) == 3

    # User IDs should be the same (no duplicates created)
    user_ids_1 = [user["id"] for user in data1["users"]]
    user_ids_2 = [user["id"] for user in data2["users"]]
    assert sorted(user_ids_1) == sorted(user_ids_2)


def test_get_dummy_users_creates_users_when_empty():
    """Test that users are created when database is empty"""
    # Ensure database is empty
    db = TestingSessionLocal()
    db.query(User).delete()
    db.commit()
    db.close()

    # Call endpoint
    response = client.get("/api/v1/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()
    assert len(data["users"]) == 3

    # Verify users were actually created in database
    db = TestingSessionLocal()
    users = db.query(User).filter(User.name.in_(["Jessica", "Sara", "Mervi"])).all()
    assert len(users) == 3
    db.close()


def test_get_dummy_users_database_error():
    """Test error handling when DB is unavailable"""

    # Temporarily override the get_db dependency to simulate database error
    def mock_get_db_error():
        mock_db = MagicMock()
        mock_db.query.side_effect = Exception("Database connection failed")
        try:
            yield mock_db
        finally:
            pass

    app.dependency_overrides[get_db] = mock_get_db_error

    try:
        response = client.get("/api/v1/auth/dummy-users")
        assert response.status_code == 500
        data = response.json()
        assert data["detail"] == "Failed to fetch dummy users"
    finally:
        # Restore original dependency
        app.dependency_overrides[get_db] = override_get_db


def test_get_dummy_users_db_connection_failed():
    """Test specific DB connection error handling"""
    from sqlalchemy.exc import SQLAlchemyError

    # Temporarily override the get_db dependency to simulate SQLAlchemy error
    def mock_get_db_sqlalchemy_error():
        mock_db = MagicMock()
        mock_db.query.side_effect = SQLAlchemyError("Connection timeout")
        try:
            yield mock_db
        finally:
            pass

    app.dependency_overrides[get_db] = mock_get_db_sqlalchemy_error

    try:
        response = client.get("/auth/dummy-users")
        assert response.status_code == 500
        data = response.json()
        assert data["detail"] == "DB connection failed"
    finally:
        # Restore original dependency
        app.dependency_overrides[get_db] = override_get_db


def test_get_dummy_users_user_creation_error():
    """Test error handling when user creation fails"""
    with patch("app.services.user_service.insert_dummy_users") as mock_insert:
        # Mock user creation to fail
        mock_insert.side_effect = Exception("User creation failed")

        # Mock empty database query
        with patch("app.api.auth.User") as mock_user_model:
            mock_query = MagicMock()
            mock_query.filter.return_value.all.return_value = []
            mock_user_model.query = mock_query

            response = client.get("/api/v1/auth/dummy-users")

            assert response.status_code == 500
            data = response.json()
            assert data["detail"] == "DB connection failed"


def test_dummy_users_response_structure():
    """Test that response matches expected Pydantic model structure"""
    response = client.get("/api/v1/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()

    # Validate response structure matches DummyUsersResponse model
    assert isinstance(data, dict)
    assert "users" in data
    assert isinstance(data["users"], list)

    for user in data["users"]:
        # Each user should have the required fields
        required_fields = ["id", "name", "role", "token"]
        for field in required_fields:
            assert field in user

        # Field types should be correct
        assert isinstance(user["id"], int)
        assert isinstance(user["name"], str)
        assert isinstance(user["role"], str)
        assert isinstance(user["token"], str)


def test_dummy_users_roles_correct():
    """Test that users have the correct roles"""
    response = client.get("/api/v1/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()

    # Find each user and check their role
    users_by_name = {user["name"]: user for user in data["users"]}

    assert users_by_name["Jessica"]["role"] == "educator"
    assert users_by_name["Sara"]["role"] == "parent"
    assert users_by_name["Mervi"]["role"] == "super_educator"


def test_dummy_users_jwt_tokens_valid():
    """Test that JWT tokens are properly generated and not empty"""
    response = client.get("/api/v1/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()

    for user in data["users"]:
        token = user["token"]
        assert token is not None
        assert len(token) > 0
        # JWT tokens typically have 3 parts separated by dots
        assert len(token.split(".")) == 3


def test_dummy_users_groups_field():
    """Test that dummy users have correct groups field"""
    response = client.get("/api/v1/auth/dummy-users")

    assert response.status_code == 200
    data = response.json()

    # Check that all users have groups field
    for user in data["users"]:
        assert "groups" in user
        assert isinstance(user["groups"], list)

    # Check specific group assignments
    user_data = {user["name"]: user for user in data["users"]}
    
    # Jessica (educator) should have ClassA
    assert user_data["Jessica"]["groups"] == ["ClassA"]
    
    # Sara (parent) should have ClassA
    assert user_data["Sara"]["groups"] == ["ClassA"]
    
    # Mervi (super_educator) should have empty groups (access to all)
    assert user_data["Mervi"]["groups"] == []


def test_dummy_users_jwt_includes_groups():
    """Test that JWT tokens include groups in the payload"""
    from app.core.security import decode_access_token
    
    response = client.get("/api/v1/auth/dummy-users")
    assert response.status_code == 200
    data = response.json()

    for user in data["users"]:
        # Decode the JWT token
        payload = decode_access_token(user["token"])
        
        # Check that groups are included in JWT payload
        assert "groups" in payload
        assert isinstance(payload["groups"], list)
        assert payload["groups"] == user["groups"]


def test_dummy_users_multiple_groups():
    """Test edge case for user with multiple groups"""
    from app.core.security import create_access_token, decode_access_token
    from app.models.user import User
    
    # Create a test user with multiple groups
    db = TestingSessionLocal()
    try:
        # Create user with multiple groups
        jwt_payload = {
            "sub": "TestEducator",
            "role": "educator", 
            "user_id": 999,
            "groups": ["ClassA", "ClassB"]
        }
        jwt_token = create_access_token(jwt_payload)
        
        user = User(
            name="TestEducator",
            role="educator",
            jwt_token=jwt_token,
            groups=["ClassA", "ClassB"]
        )
        
        db.add(user)
        db.commit()
        
        # Verify the user was created with correct groups
        created_user = db.query(User).filter(User.name == "TestEducator").first()
        assert created_user is not None
        assert created_user.groups == ["ClassA", "ClassB"]
        
        # Verify JWT includes groups
        payload = decode_access_token(created_user.jwt_token)
        assert payload["groups"] == ["ClassA", "ClassB"]
        
    finally:
        # Clean up
        db.query(User).filter(User.name == "TestEducator").delete()
        db.commit()
        db.close()
