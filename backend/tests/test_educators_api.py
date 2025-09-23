from fastapi.testclient import TestClient

from app.main import app
from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole
from app.models.group import Group
from tests.conftest import TestingSessionLocal

client = TestClient(app)


class TestEducatorsApi:
    """Test the educators API endpoint with search functionality."""

    def test_search_educators_returns_only_jessica(self, clean_db):
        """Test that searching for 'Jessica' returns only Jessica, not Mark Smith."""
        db = TestingSessionLocal()
        try:
            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Create a group
            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create two educators: Jessica and Mark Smith
            jessica = Educator(
                full_name="Jessica",
                email="jessica@test.com",
                phone_num="+1234567890",
                role=EducatorRole.EDUCATOR.value,
                daycare_id=daycare.id,
            )
            mark = Educator(
                full_name="Mark Smith",
                email="mark@test.com",
                phone_num="+1234567891",
                role=EducatorRole.EDUCATOR.value,
                daycare_id=daycare.id,
            )
            db.add_all([jessica, mark])
            db.commit()
            db.refresh(jessica)
            db.refresh(mark)

            # Test search for Jessica
            response = client.get(
                f"/api/v1/educators?daycare_id={daycare.id}&search=Jessica"
            )

            # Assert response status
            assert (
                response.status_code == 200
            ), f"Expected 200, got {response.status_code}: {response.text}"

            # Assert response JSON length is exactly 1
            educators = response.json()
            assert (
                len(educators) == 1
            ), f"Expected exactly 1 educator, got {len(educators)}: {educators}"

            # Assert only Jessica is returned
            jessica_result = educators[0]
            assert (
                jessica_result["full_name"] == "Jessica"
            ), f"Expected 'Jessica', got '{jessica_result['full_name']}'"
            assert jessica_result["email"] == "jessica@test.com"
            assert jessica_result["role"] == "educator"

            # Ensure Mark is not in the results
            mark_names = [edu["full_name"] for edu in educators]
            assert (
                "Mark Smith" not in mark_names
            ), f"Mark Smith should not appear when searching for Jessica, but got: {mark_names}"

        finally:
            db.close()

    def test_search_educators_returns_all_when_no_search(self, clean_db):
        """Test that calling /educators without search returns all educators."""
        db = TestingSessionLocal()
        try:
            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Create a group
            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create two educators: Jessica and Mark Smith
            jessica = Educator(
                full_name="Jessica",
                email="jessica@test.com",
                phone_num="+1234567890",
                role=EducatorRole.EDUCATOR.value,
                daycare_id=daycare.id,
            )
            mark = Educator(
                full_name="Mark Smith",
                email="mark@test.com",
                phone_num="+1234567891",
                role=EducatorRole.EDUCATOR.value,
                daycare_id=daycare.id,
            )
            db.add_all([jessica, mark])
            db.commit()
            db.refresh(jessica)
            db.refresh(mark)

            # Test without search parameter
            response = client.get(f"/api/v1/educators?daycare_id={daycare.id}")

            # Assert response status
            assert (
                response.status_code == 200
            ), f"Expected 200, got {response.status_code}: {response.text}"

            # Assert response JSON length is 2
            educators = response.json()
            assert (
                len(educators) == 2
            ), f"Expected exactly 2 educators, got {len(educators)}: {educators}"

            # Assert both educators are returned
            educator_names = [edu["full_name"] for edu in educators]
            assert (
                "Jessica" in educator_names
            ), f"Jessica should be in results: {educator_names}"
            assert (
                "Mark Smith" in educator_names
            ), f"Mark Smith should be in results: {educator_names}"

        finally:
            db.close()

    def test_search_educators_case_insensitive(self, clean_db):
        """Test that search is case insensitive."""
        db = TestingSessionLocal()
        try:
            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Create a group
            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create Jessica
            jessica = Educator(
                full_name="Jessica",
                email="jessica@test.com",
                phone_num="+1234567890",
                role=EducatorRole.EDUCATOR.value,
                daycare_id=daycare.id,
            )
            db.add(jessica)
            db.commit()
            db.refresh(jessica)

            # Test search with lowercase
            response = client.get(
                f"/api/v1/educators?daycare_id={daycare.id}&search=jessica"
            )

            # Assert response status
            assert (
                response.status_code == 200
            ), f"Expected 200, got {response.status_code}: {response.text}"

            # Assert response JSON length is exactly 1
            educators = response.json()
            assert (
                len(educators) == 1
            ), f"Expected exactly 1 educator, got {len(educators)}: {educators}"

            # Assert Jessica is returned
            jessica_result = educators[0]
            assert (
                jessica_result["full_name"] == "Jessica"
            ), f"Expected 'Jessica', got '{jessica_result['full_name']}'"

        finally:
            db.close()
