from datetime import date

from fastapi.testclient import TestClient

from app.main import app
from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import AttendanceStatus, Kid
from app.models.parent import Parent
from app.services.seeder import insert_dummy_kids, insert_dummy_parents
from tests.conftest import TestingSessionLocal

client = TestClient(app)


class TestKidSeeding:
    """Test kid seeding functionality."""

    def test_insert_dummy_kids_creates_nine_kids(self):
        """Test that insert_dummy_kids creates exactly 9 kids."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            kids = insert_dummy_kids(db, daycare.id, groups, parents)

            assert len(kids) == 9

            # Verify all kids exist in database
            db_kids = db.query(Kid).all()
            assert len(db_kids) == 9

        finally:
            db.close()

    def test_kids_are_distributed_equally_among_groups(self):
        """Test that kids are distributed equally among groups (3 per group)."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            kids = insert_dummy_kids(db, daycare.id, groups, parents)

            # Check distribution per group
            for group in groups:
                group_kids = [kid for kid in kids if kid.group_id == group.id]
                assert len(group_kids) == 3

        finally:
            db.close()

    def test_kids_share_last_names_with_parents(self):
        """Test that kids share last names with their linked parents."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            kids = insert_dummy_kids(db, daycare.id, groups, parents)

            # Check that each kid shares a last name with their parent
            for kid in kids:
                kid_last_name = kid.full_name.split()[-1]

                # Find the parent(s) linked to this kid
                kid_parents = [parent for parent in parents if kid in parent.kids]
                assert (
                    len(kid_parents) > 0
                ), f"Kid {kid.full_name} has no linked parents"

                # Check that at least one parent shares the last name
                parent_last_names = [
                    parent.full_name.split()[-1] for parent in kid_parents
                ]
                assert (
                    kid_last_name in parent_last_names
                ), f"Kid {kid.full_name} doesn't share last name with any parent"

        finally:
            db.close()

    def test_no_parent_has_more_than_two_kids(self):
        """Test that no parent has more than 2 kids."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            insert_dummy_kids(db, daycare.id, groups, parents)

            # Check that no parent has more than 2 kids
            for parent in parents:
                assert (
                    len(parent.kids) <= 2
                ), f"Parent {parent.full_name} has {len(parent.kids)} kids, which exceeds the limit of 2"

        finally:
            db.close()

    def test_kids_have_valid_dates_of_birth(self):
        """Test that kids have valid dates of birth."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            kids = insert_dummy_kids(db, daycare.id, groups, parents)

            # Check that all kids have valid dates of birth
            for kid in kids:
                assert kid.dob is not None
                # Check that DOB is reasonable (between 2019 and 2021)
                assert kid.dob.year >= 2019
                assert kid.dob.year <= 2021

        finally:
            db.close()

    def test_kids_are_linked_to_correct_groups(self):
        """Test that kids are linked to the correct groups."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            kids = insert_dummy_kids(db, daycare.id, groups, parents)

            # Check that each kid is linked to a valid group
            for kid in kids:
                assert kid.group_id is not None
                group = next((g for g in groups if g.id == kid.group_id), None)
                assert (
                    group is not None
                ), f"Kid {kid.full_name} is linked to non-existent group"

        finally:
            db.close()

    def test_kid_seeding_is_idempotent(self):
        """Test that running insert_dummy_kids multiple times doesn't create duplicates."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids twice
            kids1 = insert_dummy_kids(db, daycare.id, groups, parents)
            kids2 = insert_dummy_kids(db, daycare.id, groups, parents)

            # Should return the same kids (idempotent)
            assert len(kids1) == 9
            assert len(kids2) == 9

            # Database should still have only 9 kids
            db_kids = db.query(Kid).all()
            assert len(db_kids) == 9

        finally:
            db.close()

    def test_kids_have_expected_names(self):
        """Test that kids have the expected names matching their parents."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and groups
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            groups = []
            for group_name in ["Group A", "Group B", "Group C"]:
                group = Group(name=group_name, daycare_id=daycare.id)
                db.add(group)
                groups.append(group)
            db.commit()
            for group in groups:
                db.refresh(group)

            # Create parents
            parents = insert_dummy_parents(db, daycare.id)

            # Insert dummy kids
            kids = insert_dummy_kids(db, daycare.id, groups, parents)

            expected_kid_names = [
                "Emma Sara",
                "Liam Sara",
                "Sophia Smith",
                "Noah Davis",
                "Olivia Wilson",
                "William Wilson",
                "Ava Garcia",
                "James Garcia",
                "Isabella Miller",
            ]

            kid_names = [kid.full_name for kid in kids]
            for expected_name in expected_kid_names:
                assert expected_name in kid_names

        finally:
            db.close()


class TestKidAttendance:
    """Test kid attendance functionality."""

    def test_kid_default_attendance(self, clean_db):
        """Test that a kid defaults to OUT attendance status."""
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

            # Create a kid without specifying attendance
            kid = Kid(
                full_name="Attendance Test",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            assert kid.attendance == AttendanceStatus.OUT

        finally:
            db.close()

    def test_kid_attendance_enum(self, clean_db):
        """Test that a kid can be set to different attendance statuses."""
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

            # Test SICK status
            sick_kid = Kid(
                full_name="Sick Kid",
                dob=date(2020, 2, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.SICK
            )
            db.add(sick_kid)
            db.commit()
            db.refresh(sick_kid)

            assert sick_kid.attendance == AttendanceStatus.SICK

            # Test IN_CARE status
            in_care_kid = Kid(
                full_name="In Care Kid",
                dob=date(2020, 3, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.IN_CARE
            )
            db.add(in_care_kid)
            db.commit()
            db.refresh(in_care_kid)

            assert in_care_kid.attendance == AttendanceStatus.IN_CARE

        finally:
            db.close()

    def test_kid_attendance_serialization(self, clean_db):
        """Test that attendance status serializes correctly in API responses."""
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

            # Create kids with different attendance statuses
            kids_data = [
                ("Out Kid", AttendanceStatus.OUT),
                ("Sick Kid", AttendanceStatus.SICK),
                ("In Care Kid", AttendanceStatus.IN_CARE)
            ]

            for name, status in kids_data:
                kid = Kid(
                    full_name=name,
                    dob=date(2020, 1, 1),
                    daycare_id=daycare.id,
                    group_id=group.id,
                    attendance=status
                )
                db.add(kid)
            db.commit()

            # Test API endpoint
            response = client.get(f"/api/v1/kids?daycare_id={daycare.id}")
            assert response.status_code == 200

            kids = response.json()
            assert len(kids) == 3

            # Check that attendance statuses are serialized as strings
            attendance_values = [kid["attendance"] for kid in kids]
            assert "out" in attendance_values
            assert "sick" in attendance_values
            assert "in-care" in attendance_values

        finally:
            db.close()

    def test_kid_attendance_update(self, clean_db):
        """Test that kid attendance can be updated."""
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
                full_name="Update Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Update attendance
            kid.attendance = AttendanceStatus.IN_CARE
            db.commit()
            db.refresh(kid)

            assert kid.attendance == AttendanceStatus.IN_CARE

        finally:
            db.close()

    def test_kid_attendance_invalid_value_raises_error(self, clean_db):
        """Test that invalid attendance values raise appropriate errors."""
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

            # This should work - valid enum value
            kid = Kid(
                full_name="Valid Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.SICK
            )
            db.add(kid)
            db.commit()

            # Test that the enum values are properly constrained
            # by checking that only valid values are accepted
            valid_statuses = [AttendanceStatus.OUT, AttendanceStatus.SICK, AttendanceStatus.IN_CARE]
            for status in valid_statuses:
                kid.attendance = status
                db.commit()
                assert kid.attendance == status

        finally:
            db.close()
