from datetime import date, timedelta

import pytest
from fastapi.testclient import TestClient
from freezegun import freeze_time

from app.main import app
from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import AbsenceReason, AttendanceStatus, Kid, KidAbsence
from app.models.parent import Parent
from tests.conftest import TestingSessionLocal

client = TestClient(app)


class TestAbsenceSystem:
    """Test the absence system with daily attendance logic."""

    def test_absence_applies_today(self, clean_db):
        """Test that absence for today's date shows up in attendance."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Create absence for today
            absence = KidAbsence(
                kid_id=kid.id, date=date.today(), reason=AbsenceReason.SICK
            )
            db.add(absence)
            db.commit()

            # Test GET /kids endpoint
            response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert response.status_code == 200

            kids_data = response.json()
            assert len(kids_data) == 1
            assert kids_data[0]["attendance"] == "sick"

        finally:
            db.close()

    def test_absence_does_not_apply_future(self, clean_db):
        """Test that absence for future date doesn't affect today's attendance."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Create absence for tomorrow
            tomorrow = date.today() + timedelta(days=1)
            absence = KidAbsence(
                kid_id=kid.id, date=tomorrow, reason=AbsenceReason.HOLIDAY
            )
            db.add(absence)
            db.commit()

            # Test GET /kids endpoint
            response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert response.status_code == 200

            kids_data = response.json()
            assert len(kids_data) == 1
            assert (
                kids_data[0]["attendance"] == "out"
            )  # Should show original attendance

        finally:
            db.close()

    def test_absence_reverts_next_day(self, clean_db):
        """Test that absence only applies on the specific date."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Create absence for today
            absence = KidAbsence(
                kid_id=kid.id, date=date.today(), reason=AbsenceReason.SICK
            )
            db.add(absence)
            db.commit()

            # Test with tomorrow's date
            tomorrow = date.today() + timedelta(days=1)
            with freeze_time(tomorrow):
                response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
                assert response.status_code == 200

                kids_data = response.json()
                assert len(kids_data) == 1
                assert kids_data[0]["attendance"] == "out"  # Should revert to original

        finally:
            db.close()

    def test_absence_reason_restricted(self, clean_db):
        """Test that only 'sick' and 'holiday' reasons are allowed for absences."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Test with invalid reason
            invalid_absence = {
                "date": date.today().isoformat(),
                "reason": "out",  # Invalid reason
            }

            # Get parent JWT token
            login_response = client.post(
                "/api/v1/auth/dev-login", json={"parent_id": str(parent.id)}
            )
            assert login_response.status_code == 200
            token = login_response.json()["access_token"]

            # Test POST absence with invalid reason
            response = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=invalid_absence,
                headers={"Authorization": f"Bearer {token}"},
            )
            assert response.status_code == 422  # Validation error

        finally:
            db.close()

    def test_absence_teacher_cannot_create(self, clean_db):
        """Test that educators cannot create absences."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create educator
            from app.models.educator import Educator

            educator = Educator(
                full_name="Test Educator",
                email="educator@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
                role="educator",
            )
            db.add(educator)
            db.commit()
            db.refresh(educator)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Get educator JWT token
            login_response = client.post(
                "/api/v1/auth/dev-login", json={"educator_id": str(educator.id)}
            )
            assert login_response.status_code == 200
            token = login_response.json()["access_token"]

            # Test POST absence as educator
            absence_data = {"date": date.today().isoformat(), "reason": "sick"}

            response = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=absence_data,
                headers={"Authorization": f"Bearer {token}"},
            )
            assert response.status_code == 403
            assert "Only parents can create absences" in response.json()["detail"]

        finally:
            db.close()

    def test_absence_parent_can_only_update_own_kids(self, clean_db):
        """Test that parents can only create absences for their own kids."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create two parents
            parent1 = Parent(
                full_name="Parent 1",
                email="parent1@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            parent2 = Parent(
                full_name="Parent 2",
                email="parent2@example.com",
                phone_num="+1234567891",
                daycare_id=daycare.id,
            )
            db.add_all([parent1, parent2])
            db.commit()
            db.refresh(parent1)
            db.refresh(parent2)

            # Create kid linked only to parent1
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

            # Link only parent1 to kid
            parent1.kids.append(kid)
            db.commit()

            # Get parent2 JWT token
            login_response = client.post(
                "/api/v1/auth/dev-login", json={"parent_id": str(parent2.id)}
            )
            assert login_response.status_code == 200
            token = login_response.json()["access_token"]

            # Test POST absence as unrelated parent
            absence_data = {"date": date.today().isoformat(), "reason": "sick"}

            response = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=absence_data,
                headers={"Authorization": f"Bearer {token}"},
            )
            assert response.status_code == 403
            assert "not authorized" in response.json()["detail"]

        finally:
            db.close()

    def test_teacher_override_in_care(self, clean_db):
        """Test that teacher can override absence with in-care status."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Create absence for today
            absence = KidAbsence(
                kid_id=kid.id, date=date.today(), reason=AbsenceReason.SICK
            )
            db.add(absence)
            db.commit()

            # Teacher overrides attendance to in-care
            response = client.patch(
                f"/api/v1/kids/{kid.id}/attendance", json={"attendance": "in-care"}
            )
            assert response.status_code == 200

            # Check that attendance is now in-care
            response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert response.status_code == 200

            kids_data = response.json()
            assert len(kids_data) == 1
            assert kids_data[0]["attendance"] == "in-care"

        finally:
            db.close()

    def test_absence_unique_per_day(self, clean_db):
        """Test that only one absence per kid per day is allowed."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Create first absence
            absence1 = KidAbsence(
                kid_id=kid.id, date=date.today(), reason=AbsenceReason.SICK
            )
            db.add(absence1)
            db.commit()

            # Try to create second absence for same day
            absence2 = KidAbsence(
                kid_id=kid.id, date=date.today(), reason=AbsenceReason.HOLIDAY
            )
            db.add(absence2)

            # This should raise an integrity error
            with pytest.raises(Exception):  # SQLAlchemy integrity error
                db.commit()

        finally:
            db.close()

    def test_multiple_day_absence_holiday(self, clean_db):
        """Test that absence for multiple consecutive days (tomorrow until next week)
        changes attendance on each indicated date."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

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

            # Create absences for multiple consecutive days (tomorrow until next week)
            # Let's say tomorrow is Monday, and we're absent until Friday (5 days)
            tomorrow = date.today() + timedelta(days=1)
            next_week = tomorrow + timedelta(days=4)  # 5 consecutive days

            # Create absences for each day
            absence_dates = []
            current_date = tomorrow
            while current_date <= next_week:
                absence = KidAbsence(
                    kid_id=kid.id, date=current_date, reason=AbsenceReason.HOLIDAY
                )
                db.add(absence)
                absence_dates.append(current_date)
                current_date += timedelta(days=1)

            db.commit()

            # Test that today's attendance is still "out" (no absence for today)
            response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert response.status_code == 200
            kids_data = response.json()
            assert len(kids_data) == 1
            assert kids_data[0]["attendance"] == "out"

            # Test attendance for each day of absence using freeze_time
            for absence_date in absence_dates:
                with freeze_time(absence_date):
                    response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
                    assert response.status_code == 200
                    kids_data = response.json()
                    assert len(kids_data) == 1
                    assert kids_data[0]["attendance"] == "holiday"
                    print(f"✓ Attendance on {absence_date} is 'holiday'")

            # Test that attendance reverts to "out" after the absence period
            day_after_absence = next_week + timedelta(days=1)
            with freeze_time(day_after_absence):
                response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
                assert response.status_code == 200
                kids_data = response.json()
                assert len(kids_data) == 1
                assert kids_data[0]["attendance"] == "out"
                print(f"✓ Attendance on {day_after_absence} reverts to 'out'")

        finally:
            db.close()

    def test_multiple_day_absence_sick(self, clean_db):
        """Test that absence for multiple consecutive days with 'sick' reason
        changes attendance on each indicated date."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.IN_CARE,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Create absences for 3 consecutive days starting tomorrow
            tomorrow = date.today() + timedelta(days=1)
            end_date = tomorrow + timedelta(days=2)  # 3 consecutive days

            # Create absences for each day
            absence_dates = []
            current_date = tomorrow
            while current_date <= end_date:
                absence = KidAbsence(
                    kid_id=kid.id, date=current_date, reason=AbsenceReason.SICK
                )
                db.add(absence)
                absence_dates.append(current_date)
                current_date += timedelta(days=1)

            db.commit()

            # Test that today's attendance is still "in-care" (no absence for today)
            response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert response.status_code == 200
            kids_data = response.json()
            assert len(kids_data) == 1
            assert kids_data[0]["attendance"] == "in-care"

            # Test attendance for each day of absence using freeze_time
            for absence_date in absence_dates:
                with freeze_time(absence_date):
                    response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
                    assert response.status_code == 200
                    kids_data = response.json()
                    assert len(kids_data) == 1
                    assert kids_data[0]["attendance"] == "sick"
                    print(f"✓ Attendance on {absence_date} is 'sick'")

            # Test that attendance reverts to "in-care" after the absence period
            day_after_absence = end_date + timedelta(days=1)
            with freeze_time(day_after_absence):
                response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
                assert response.status_code == 200
                kids_data = response.json()
                assert len(kids_data) == 1
                assert kids_data[0]["attendance"] == "in-care"
                print(f"✓ Attendance on {day_after_absence} reverts to 'in-care'")

        finally:
            db.close()


def test_duplicate_absence_returns_400():
    """Test that submitting duplicate absence returns 400 Bad Request."""
    db = TestingSessionLocal()
    try:
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db.add(daycare)
        db.commit()
        db.refresh(daycare)

        group = Group(name="Group A", daycare_id=daycare.id)
        db.add(group)
        db.commit()
        db.refresh(group)

        parent = Parent(
            full_name="Test Parent",
            email="test@example.com",
            phone_num="+1234567890",
            daycare_id=daycare.id,
        )
        db.add(parent)
        db.commit()
        db.refresh(parent)

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

        # Get parent JWT token
        login_response = client.post(
            "/api/v1/auth/dev-login", json={"parent_id": str(parent.id)}
        )
        assert login_response.status_code == 200
        token = login_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Create first absence
        absence_data = {
            "date": date.today().isoformat(),
            "reason": "sick",
        }

        response1 = client.post(
            f"/api/v1/kids/{kid.id}/absences",
            json=absence_data,
            headers=headers,
        )
        assert response1.status_code == 200
        print("✓ First absence created successfully")

        # Try to create duplicate absence
        response2 = client.post(
            f"/api/v1/kids/{kid.id}/absences",
            json=absence_data,
            headers=headers,
        )
        assert response2.status_code == 400
        assert "Absence already reported for this date" in response2.json()["detail"]
        print("✓ Duplicate absence returns 400 with proper message")

        # Verify only one absence exists in database
        absences = db.query(KidAbsence).filter(KidAbsence.kid_id == kid.id).all()
        assert len(absences) == 1
        print("✓ Database contains only one absence record")

    finally:
        db.close()


def test_unique_absence_returns_200():
    """Test that submitting unique absence returns 200 OK."""
    db = TestingSessionLocal()
    try:
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db.add(daycare)
        db.commit()
        db.refresh(daycare)

        group = Group(name="Group A", daycare_id=daycare.id)
        db.add(group)
        db.commit()
        db.refresh(group)

        parent = Parent(
            full_name="Test Parent",
            email="test@example.com",
            phone_num="+1234567890",
            daycare_id=daycare.id,
        )
        db.add(parent)
        db.commit()
        db.refresh(parent)

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

        # Get parent JWT token
        login_response = client.post(
            "/api/v1/auth/dev-login", json={"parent_id": str(parent.id)}
        )
        assert login_response.status_code == 200
        token = login_response.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        # Create absence for today
        absence_data_today = {
            "date": date.today().isoformat(),
            "reason": "sick",
        }

        response1 = client.post(
            f"/api/v1/kids/{kid.id}/absences",
            json=absence_data_today,
            headers=headers,
        )
        assert response1.status_code == 200
        print("✓ First absence created successfully")

        # Create absence for tomorrow (should be unique)
        absence_data_tomorrow = {
            "date": (date.today() + timedelta(days=1)).isoformat(),
            "reason": "holiday",
        }

        response2 = client.post(
            f"/api/v1/kids/{kid.id}/absences",
            json=absence_data_tomorrow,
            headers=headers,
        )
        assert response2.status_code == 200
        print("✓ Second absence (different date) created successfully")

        # Verify both absences exist in database
        absences = db.query(KidAbsence).filter(KidAbsence.kid_id == kid.id).all()
        assert len(absences) == 2
        print("✓ Database contains both absence records")

    finally:
        db.close()
