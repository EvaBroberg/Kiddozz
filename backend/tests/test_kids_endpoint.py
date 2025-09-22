import pytest

from tests.conftest import client


@pytest.fixture
def client_fixture():
    """Test client fixture."""
    return client


def test_list_kids_returns_data(client_fixture, seeded_daycare_id):
    res = client_fixture.get(f"/api/v1/kids?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, list)
    assert all("id" in kid for kid in data)
    assert all("full_name" in kid for kid in data)


def test_list_kids_filters_by_group(client_fixture, seeded_daycare_id):
    res = client_fixture.get(
        f"/api/v1/kids?daycare_id={seeded_daycare_id}&group_id=GroupA"
    )
    assert res.status_code == 200
    data = res.json()
    assert all(kid["group_id"] == "GroupA" for kid in data)


def test_list_kids_requires_daycare_id(client_fixture):
    res = client_fixture.get("/api/v1/kids")
    assert res.status_code == 422  # Validation error for missing required parameter


def test_list_kids_with_real_daycare_id(client_fixture, seeded_daycare_id):
    # Test with a real daycare ID to ensure the endpoint works
    res = client_fixture.get(f"/api/v1/kids?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, list)
    # Should have some kids from the seeded data
    assert len(data) > 0
