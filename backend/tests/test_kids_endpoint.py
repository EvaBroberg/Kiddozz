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


def test_kids_include_allergies_and_need_to_know_fields(
    client_fixture, seeded_daycare_id
):
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


def test_kids_include_parents_and_trusted_adults_fields(
    client_fixture, seeded_daycare_id
):
    """Test that kids API response includes both parents and trusted_adults fields."""
    res = client_fixture.get(f"/api/v1/kids?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, list)

    # Check that all kids have both fields
    for kid in data:
        assert "parents" in kid
        assert "trusted_adults" in kid
        # parents should be a list
        assert isinstance(kid["parents"], list)
        # trusted_adults should be a list (can be empty)
        assert isinstance(kid["trusted_adults"], list)


def test_seeded_kids_have_parents(client_fixture, seeded_daycare_id):
    """Test that seeded kids have at least one parent."""
    res = client_fixture.get(f"/api/v1/kids?daycare_id={seeded_daycare_id}")
    assert res.status_code == 200
    data = res.json()
    assert isinstance(data, list)
    assert len(data) > 0

    # Check that at least one kid has parents
    kids_with_parents = [kid for kid in data if len(kid["parents"]) > 0]
    assert len(kids_with_parents) > 0, "At least one seeded kid should have parents"

    # Verify parent structure
    for kid in kids_with_parents:
        for parent in kid["parents"]:
            assert "id" in parent
            assert "full_name" in parent
            assert "email" in parent
            assert "phone_num" in parent


def test_kids_with_allergies_and_need_to_know_values(client_fixture, seeded_daycare_id):
    """Test that kids can be created/updated with allergies and need_to_know values."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from tests.conftest import TestingSessionLocal

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
                need_to_know="Has asthma, needs inhaler nearby",
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
            test_kid_data = next(
                (kid for kid in data if kid["id"] == test_kid.id), None
            )
            assert test_kid_data is not None
            assert test_kid_data["allergies"] == "Peanuts, dairy, shellfish"
            assert test_kid_data["need_to_know"] == "Has asthma, needs inhaler nearby"

            # Clean up
            db.delete(test_kid)
            db.commit()

    finally:
        db.close()


def test_update_kid_basic_functionality(client_fixture, seeded_daycare_id, make_token):
    """Test that the PATCH endpoint can update allergies and need_to_know fields."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from app.models.parent import Parent
    from tests.conftest import TestingSessionLocal

    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()

        if daycare and group:
            # Create a parent
            parent = Parent(
                full_name="Test Parent",
                email="test.parent@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid and link to parent
            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Create a token for the parent
            token = make_token(str(parent.id), "parent", daycare_id=daycare.id)
            headers = {"Authorization": f"Bearer {token}"}

            # Test updating allergies and need_to_know
            update_data = {
                "allergies": "Peanuts, dairy",
                "need_to_know": "Has asthma, needs inhaler",
            }

            response = client_fixture.patch(
                f"/api/v1/kids/{kid.id}", json=update_data, headers=headers
            )

            assert response.status_code == 200
            data = response.json()
            assert data["allergies"] == "Peanuts, dairy"
            assert data["need_to_know"] == "Has asthma, needs inhaler"

            # Clean up
            db.delete(kid)
            db.delete(parent)
            db.commit()

    finally:
        db.close()


def test_update_kid_as_linked_parent_success(
    client_fixture, seeded_daycare_id, make_token
):
    """Test that a parent linked to a kid can update allergies and need_to_know."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from app.models.parent import Parent
    from tests.conftest import TestingSessionLocal

    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()

        if daycare and group:
            # Create a parent
            parent = Parent(
                full_name="Test Parent",
                email="test.parent@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid and link to parent
            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Create token for parent
            token = make_token(str(parent.id), "parent", daycare_id=daycare.id)
            headers = {"Authorization": f"Bearer {token}"}

            # Update kid with allergies and need_to_know
            update_data = {
                "allergies": "Peanuts, dairy",
                "need_to_know": "Has asthma, needs inhaler",
            }

            response = client_fixture.patch(
                f"/api/v1/kids/{kid.id}", json=update_data, headers=headers
            )

            assert response.status_code == 200
            data = response.json()
            assert data["allergies"] == "Peanuts, dairy"
            assert data["need_to_know"] == "Has asthma, needs inhaler"

            # Clean up
            db.delete(kid)
            db.delete(parent)
            db.commit()

    finally:
        db.close()


def test_update_kid_as_unlinked_parent_forbidden(
    client_fixture, seeded_daycare_id, make_token
):
    """Test that a parent not linked to a kid cannot update allergies and need_to_know."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from app.models.parent import Parent
    from tests.conftest import TestingSessionLocal

    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()

        if daycare and group:
            # Create a parent
            parent = Parent(
                full_name="Test Parent",
                email="test.parent@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid (not linked to parent)
            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Create token for parent
            token = make_token(str(parent.id), "parent", daycare_id=daycare.id)
            headers = {"Authorization": f"Bearer {token}"}

            # Try to update kid with allergies and need_to_know
            update_data = {
                "allergies": "Peanuts, dairy",
                "need_to_know": "Has asthma, needs inhaler",
            }

            response = client_fixture.patch(
                f"/api/v1/kids/{kid.id}", json=update_data, headers=headers
            )

            assert response.status_code == 403
            assert "Only parents linked to this kid" in response.json()["detail"]

            # Clean up
            db.delete(kid)
            db.delete(parent)
            db.commit()

    finally:
        db.close()


def test_update_kid_as_educator_ignores_sensitive_fields(
    client_fixture, seeded_daycare_id, make_token
):
    """Test that educators cannot update allergies and need_to_know fields."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.educator import Educator
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from tests.conftest import TestingSessionLocal

    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()

        if daycare and group:
            # Create an educator
            educator = Educator(
                full_name="Test Educator",
                email="test.educator@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
                role="educator",
            )
            db.add(educator)
            db.commit()
            db.refresh(educator)

            # Create a kid
            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
                allergies="Original allergies",
                need_to_know="Original need to know",
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Create token for educator
            token = make_token(str(educator.id), "educator", daycare_id=daycare.id)
            headers = {"Authorization": f"Bearer {token}"}

            # Try to update kid with allergies and need_to_know
            update_data = {
                "full_name": "Updated Kid Name",
                "allergies": "New allergies",
                "need_to_know": "New need to know",
            }

            response = client_fixture.patch(
                f"/api/v1/kids/{kid.id}", json=update_data, headers=headers
            )

            assert response.status_code == 200
            data = response.json()
            # Name should be updated
            assert data["full_name"] == "Updated Kid Name"
            # Sensitive fields should remain unchanged
            assert data["allergies"] == "Original allergies"
            assert data["need_to_know"] == "Original need to know"

            # Clean up
            db.delete(kid)
            db.delete(educator)
            db.commit()

    finally:
        db.close()


def test_update_kid_as_super_educator_ignores_sensitive_fields(
    client_fixture, seeded_daycare_id, make_token
):
    """Test that super educators cannot update allergies and need_to_know fields."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.educator import Educator
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from tests.conftest import TestingSessionLocal

    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()

        if daycare and group:
            # Create a super educator
            super_educator = Educator(
                full_name="Test Super Educator",
                email="test.super@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
                role="super_educator",
            )
            db.add(super_educator)
            db.commit()
            db.refresh(super_educator)

            # Create a kid
            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
                allergies="Original allergies",
                need_to_know="Original need to know",
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Create token for super educator
            token = make_token(
                str(super_educator.id), "super_educator", daycare_id=daycare.id
            )
            headers = {"Authorization": f"Bearer {token}"}

            # Try to update kid with allergies and need_to_know
            update_data = {
                "full_name": "Updated Kid Name",
                "allergies": "New allergies",
                "need_to_know": "New need to know",
            }

            response = client_fixture.patch(
                f"/api/v1/kids/{kid.id}", json=update_data, headers=headers
            )

            assert response.status_code == 200
            data = response.json()
            # Name should be updated
            assert data["full_name"] == "Updated Kid Name"
            # Sensitive fields should remain unchanged
            assert data["allergies"] == "Original allergies"
            assert data["need_to_know"] == "Original need to know"

            # Clean up
            db.delete(kid)
            db.delete(super_educator)
            db.commit()

    finally:
        db.close()


def test_update_kid_parent_can_update_null_fields(
    client_fixture, seeded_daycare_id, make_token
):
    """Test that a linked parent can set allergies and need_to_know to null."""
    from datetime import date

    from app.models.daycare import Daycare
    from app.models.group import Group
    from app.models.kid import AttendanceStatus, Kid
    from app.models.parent import Parent
    from tests.conftest import TestingSessionLocal

    db = TestingSessionLocal()
    try:
        # Get the first daycare and group
        daycare = db.query(Daycare).first()
        group = db.query(Group).first()

        if daycare and group:
            # Create a parent
            parent = Parent(
                full_name="Test Parent",
                email="test.parent@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid with existing allergies and need_to_know
            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
                allergies="Existing allergies",
                need_to_know="Existing need to know",
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Create token for parent
            token = make_token(str(parent.id), "parent", daycare_id=daycare.id)
            headers = {"Authorization": f"Bearer {token}"}

            # Update kid to set allergies and need_to_know to null
            update_data = {"allergies": "", "need_to_know": ""}

            response = client_fixture.patch(
                f"/api/v1/kids/{kid.id}", json=update_data, headers=headers
            )

            assert response.status_code == 200
            data = response.json()
            assert data["allergies"] == ""
            assert data["need_to_know"] == ""

            # Clean up
            db.delete(kid)
            db.delete(parent)
            db.commit()

    finally:
        db.close()


def test_get_absence_reasons_returns_200():
    """Test that /kids/absence-reasons returns a 200 status code."""
    response = client.get("/api/v1/kids/absence-reasons")
    assert response.status_code == 200


def test_get_absence_reasons_returns_expected_values():
    """Test that the response contains all expected values from the enum."""
    response = client.get("/api/v1/kids/absence-reasons")
    assert response.status_code == 200

    data = response.json()
    assert "absence_reasons" in data
    assert isinstance(data["absence_reasons"], list)

    # Check that we get the expected values from AbsenceReason enum
    expected_reasons = ["sick", "holiday"]
    assert set(data["absence_reasons"]) == set(expected_reasons)
    assert len(data["absence_reasons"]) == len(expected_reasons)


def test_get_absence_reasons_enum_sync():
    """Test that if the enum changes, the endpoint reflects it automatically."""
    from app.models.kid import AbsenceReason

    response = client.get("/api/v1/kids/absence-reasons")
    assert response.status_code == 200

    data = response.json()
    api_reasons = set(data["absence_reasons"])
    enum_reasons = set(r.value for r in AbsenceReason)

    # The API should return exactly what the enum contains
    assert api_reasons == enum_reasons
