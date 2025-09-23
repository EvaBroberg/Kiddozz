from datetime import date

from fastapi.testclient import TestClient

from app.main import app
from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import AttendanceStatus, Kid
from tests.conftest import TestingSessionLocal

client = TestClient(app)


class TestKidsAttendanceUpdate:
    """Test kids attendance update functionality."""

    def test_update_attendance_success(self, clean_db):
        """Test successful attendance update."""
        db = TestingSessionLocal()
        try:
            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create a kid
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

            # Update attendance to IN_CARE
            response = client.patch(
                f"/api/v1/kids/{kid.id}/attendance", json={"attendance": "in-care"}
            )

            assert response.status_code == 200
            data = response.json()
            assert data["message"] == "Attendance updated successfully"
            assert data["attendance"] == "in-care"

            # Verify the change in database
            db.refresh(kid)
            assert kid.attendance == AttendanceStatus.IN_CARE

        finally:
            db.close()

    def test_update_attendance_invalid_status(self, clean_db):
        """Test attendance update with invalid status."""
        db = TestingSessionLocal()
        try:
            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create a kid
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

            # Try to update with invalid status
            response = client.patch(
                f"/api/v1/kids/{kid.id}/attendance",
                json={"attendance": "invalid-status"},
            )

            assert response.status_code == 400
            data = response.json()
            assert "Invalid attendance status" in data["detail"]

        finally:
            db.close()

    def test_update_attendance_kid_not_found(self, clean_db):
        """Test attendance update for non-existent kid."""
        response = client.patch(
            "/api/v1/kids/999/attendance", json={"attendance": "in-care"}
        )

        assert response.status_code == 404
        data = response.json()
        assert data["detail"] == "Kid not found"

    def test_update_attendance_all_statuses(self, clean_db):
        """Test updating to all valid attendance statuses."""
        db = TestingSessionLocal()
        try:
            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create a kid
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

            # Test all valid statuses
            valid_statuses = ["out", "sick", "in-care"]

            for status in valid_statuses:
                response = client.patch(
                    f"/api/v1/kids/{kid.id}/attendance", json={"attendance": status}
                )

                assert response.status_code == 200
                data = response.json()
                assert data["attendance"] == status

                # Verify in database
                db.refresh(kid)
                assert kid.attendance.value == status

        finally:
            db.close()

    def test_update_attendance_missing_field(self, clean_db):
        """Test attendance update with missing attendance field."""
        db = TestingSessionLocal()
        try:
            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create a kid
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

            # Try to update without attendance field
            response = client.patch(f"/api/v1/kids/{kid.id}/attendance", json={})

            assert response.status_code == 422  # Validation error

        finally:
            db.close()
