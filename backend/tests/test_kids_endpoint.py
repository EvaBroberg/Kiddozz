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


def test_kids_include_allergies_and_need_to_know_fields(client_fixture, seeded_daycare_id):
    """Test that kids API response includes allergies and need_to_know fields."""
    res = client_fixture.get(f"/api/v1/kids?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, list)
    
    # Check that all kids have the new fields (they should be null by default)
    for kid in data:
        assert "allergies" in kid
        assert "need_to_know" in kid
        # These should be null for existing kids since they were added as nullable
        assert kid["allergies"] is None
        assert kid["need_to_know"] is None


def test_kids_with_allergies_and_need_to_know_values(client_fixture, seeded_daycare_id):
    """Test that kids can be created/updated with allergies and need_to_know values."""
    from datetime import date
    from sqlalchemy.orm import Session
    from tests.conftest import TestingSessionLocal
    from app.models.kid import Kid, AttendanceStatus
    from app.models.daycare import Daycare
    from app.models.group import Group
    
    # Create a test kid with allergies and need_to_know
    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()
        
        if daycare and group:
            test_kid = Kid(
                full_name="Test Kid with Allergies",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
                allergies="Peanuts, dairy, shellfish",
                need_to_know="Has asthma, needs inhaler nearby"
            )
            db.add(test_kid)
            db.commit()
            db.refresh(test_kid)
            
            # Test that the kid was created with the values
            assert test_kid.allergies == "Peanuts, dairy, shellfish"
            assert test_kid.need_to_know == "Has asthma, needs inhaler nearby"
            
            # Test that the API returns these values
            res = client_fixture.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert res.status_code == 200
            data = res.json()
            
            # Find our test kid in the response
            test_kid_data = next((kid for kid in data if kid["id"] == test_kid.id), None)
            assert test_kid_data is not None
            assert test_kid_data["allergies"] == "Peanuts, dairy, shellfish"
            assert test_kid_data["need_to_know"] == "Has asthma, needs inhaler nearby"
            
            # Clean up
            db.delete(test_kid)
            db.commit()
            
    finally:
        db.close()
