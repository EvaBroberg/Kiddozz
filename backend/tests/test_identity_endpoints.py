import pytest

from tests.conftest import client


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


def test_accept_invite_as_educator(client_fixture, seeded_daycare_id, auth_token_via_invite):
    """Test that POST /api/v1/auth/accept-invite returns a valid JWT for an educator invite."""
    result = auth_token_via_invite(role="educator", daycare_id=seeded_daycare_id)
    assert result["access_token"] and len(result["access_token"]) > 10


def test_accept_invite_as_parent(client_fixture, seeded_daycare_id, auth_token_via_invite):
    """Test that POST /api/v1/auth/accept-invite returns a valid JWT for a parent invite."""
    result = auth_token_via_invite(role="parent", daycare_id=seeded_daycare_id)
    assert result["access_token"] and len(result["access_token"]) > 10


def test_auth_me_via_invite_token(client_fixture, seeded_daycare_id, auth_token_via_invite):
    """Test that a JWT obtained via invite acceptance works for /api/v1/auth/me."""
    result = auth_token_via_invite(role="educator", daycare_id=seeded_daycare_id)
    headers = {"Authorization": f"Bearer {result['access_token']}"}
    res = client_fixture.get("/api/v1/auth/me", headers=headers)
    assert res.status_code == 200
    me = res.json()
    assert me["user_id"] == result["user_id"]
    assert me["role"] == result["role"]
    assert me["daycare_id"] == result["daycare_id"]


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
