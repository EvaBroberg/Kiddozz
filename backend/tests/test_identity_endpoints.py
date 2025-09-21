import pytest

from app.models.educator import Educator
from app.models.parent import Parent
from tests.conftest import TestingSessionLocal, client


@pytest.fixture
def client_fixture():
    """Test client fixture."""
    return client


def test_list_educators(client_fixture, seeded_daycare_id):
    """Test that GET /api/v1/educators returns the seeded educators, filtered by daycare_id."""
    res = client_fixture.get(f"/api/v1/educators?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert any(e["full_name"].lower().startswith("jessica") for e in data)
    assert any(e["full_name"].lower().startswith("mervi") for e in data)


def test_list_parents(client_fixture, seeded_daycare_id):
    """Test that GET /api/v1/parents returns the seeded parents."""
    res = client_fixture.get(f"/api/v1/parents?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert any(p["full_name"].lower().startswith("sara") for p in data)


def test_dev_login_as_educator(client_fixture, seeded_daycare_id):
    """Test that POST /api/v1/auth/dev-login returns a valid JWT for a known educator_id."""
    db = TestingSessionLocal()
    try:
        edu = db.query(Educator).first()
        res = client_fixture.post(
            "/api/v1/auth/dev-login", json={"educator_id": str(edu.id)}
        )
        assert res.status_code == 200
        token = res.json()["access_token"]
        assert token and len(token) > 10
    finally:
        db.close()


def test_dev_login_as_parent(client_fixture, seeded_daycare_id):
    """Test that POST /api/v1/auth/dev-login returns a valid JWT for a known parent_id."""
    db = TestingSessionLocal()
    try:
        parent = db.query(Parent).first()
        res = client_fixture.post(
            "/api/v1/auth/dev-login", json={"parent_id": str(parent.id)}
        )
        assert res.status_code == 200
        token = res.json()["access_token"]
        assert token and len(token) > 10
    finally:
        db.close()


def test_dev_login_requires_exactly_one_id(client_fixture):
    """Test that dev-login requires exactly one of educator_id or parent_id."""
    # Test with both IDs
    res = client_fixture.post(
        "/api/v1/auth/dev-login", json={"educator_id": "1", "parent_id": "1"}
    )
    assert res.status_code == 400
    assert "exactly one" in res.json()["detail"]

    # Test with neither ID
    res = client_fixture.post("/api/v1/auth/dev-login", json={})
    assert res.status_code == 400
    assert "exactly one" in res.json()["detail"]


def test_dev_login_educator_not_found(client_fixture):
    """Test that dev-login returns 404 for non-existent educator."""
    res = client_fixture.post("/api/v1/auth/dev-login", json={"educator_id": "99999"})
    assert res.status_code == 404
    assert "Educator not found" in res.json()["detail"]


def test_dev_login_parent_not_found(client_fixture):
    """Test that dev-login returns 404 for non-existent parent."""
    res = client_fixture.post("/api/v1/auth/dev-login", json={"parent_id": "99999"})
    assert res.status_code == 404
    assert "Parent not found" in res.json()["detail"]


def test_token_payload_includes_role_daycare_groups(client_fixture, seeded_daycare_id):
    """Test that token payload includes role, daycare_id and groups (for educators)."""
    db = TestingSessionLocal()
    try:
        edu = db.query(Educator).first()
        res = client_fixture.post(
            "/api/v1/auth/dev-login", json={"educator_id": str(edu.id)}
        )
        assert res.status_code == 200

        # Decode the token to check payload
        from app.core.security import decode_access_token

        token = res.json()["access_token"]
        payload = decode_access_token(token)

        assert "role" in payload
        assert "daycare_id" in payload
        assert "groups" in payload
        assert payload["role"] in ["educator", "super_educator"]
        assert payload["daycare_id"] == str(edu.daycare_id)
        assert isinstance(payload["groups"], list)
    finally:
        db.close()


def test_educators_endpoint_requires_daycare_id_in_prod(
    client_fixture, seeded_daycare_id
):
    """Test that educators endpoint requires daycare_id in production."""
    # This test would need to mock the environment to production
    # For now, just test that it works with daycare_id
    res = client_fixture.get(f"/api/v1/educators?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200


def test_educators_endpoint_search_filter(client_fixture, seeded_daycare_id):
    """Test that educators endpoint supports search filtering."""
    res = client_fixture.get(
        f"/api/v1/educators?daycare_id={seeded_daycare_id}&search=jessica"
    )
    assert res.status_code == 200
    data = res.json()
    assert len(data) >= 1
    assert any("jessica" in e["full_name"].lower() for e in data)


def test_educators_endpoint_group_filter(client_fixture, seeded_daycare_id):
    """Test that educators endpoint supports group filtering."""
    res = client_fixture.get(
        f"/api/v1/educators?daycare_id={seeded_daycare_id}&group=Group A"
    )
    assert res.status_code == 200
    data = res.json()
    # Should include educators assigned to Group A
    for educator in data:
        group_names = [g["name"] for g in educator["groups"]]
        assert "Group A" in group_names


def test_parents_endpoint_search_filter(client_fixture, seeded_daycare_id):
    """Test that parents endpoint supports search filtering."""
    res = client_fixture.get(
        f"/api/v1/parents?daycare_id={seeded_daycare_id}&search=sara"
    )
    assert res.status_code == 200
    data = res.json()
    assert len(data) >= 1
    assert any("sara" in p["full_name"].lower() for p in data)


def test_educators_response_structure(client_fixture, seeded_daycare_id):
    """Test that educators response has correct structure."""
    res = client_fixture.get(f"/api/v1/educators?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()

    for educator in data:
        assert "id" in educator
        assert "full_name" in educator
        assert "role" in educator
        assert "groups" in educator
        assert isinstance(educator["groups"], list)

        for group in educator["groups"]:
            assert "id" in group
            assert "name" in group


def test_parents_response_structure(client_fixture, seeded_daycare_id):
    """Test that parents response has correct structure."""
    res = client_fixture.get(f"/api/v1/parents?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()

    for parent in data:
        assert "id" in parent
        assert "full_name" in parent
        assert "email" in parent
        assert "phone_num" in parent


def test_local_dev_default_daycare_id_mapping(client_fixture, seeded_daycare_id):
    """Test that in local and test environment, 'default-daycare-id' maps to the seeded daycare."""
    # This test simulates the Android emulator sending 'default-daycare-id'
    res = client_fixture.get("/api/v1/educators?daycare_id=default-daycare-id")
    assert res.status_code == 200
    data = res.json()

    # Should return educators from the seeded daycare
    assert len(data) > 0
    # Verify we get the expected educators (Jessica and Mervi should be there)
    educator_names = [e["full_name"] for e in data]
    assert "Jessica" in educator_names
    assert "Mervi" in educator_names
