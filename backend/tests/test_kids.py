from fastapi.testclient import TestClient

from app.main import app
from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import Kid
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
                "Emma Johnson",
                "Liam Johnson",
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
