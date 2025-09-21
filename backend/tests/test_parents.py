from fastapi.testclient import TestClient

from app.main import app
from app.models.daycare import Daycare
from app.models.parent import Parent
from app.services.seeder import insert_dummy_parents
from tests.conftest import TestingSessionLocal

client = TestClient(app)


class TestParentSeeding:
    """Test parent seeding functionality."""

    def test_insert_dummy_parents_creates_six_parents(self):
        """Test that insert_dummy_parents creates exactly 6 parents."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Parent).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Insert dummy parents
            parents = insert_dummy_parents(db, daycare.id)

            assert len(parents) == 6

            # Verify all parents exist in database
            db_parents = db.query(Parent).all()
            assert len(db_parents) == 6

        finally:
            db.close()

    def test_parents_have_proper_last_names(self):
        """Test that parents have proper last names for family consistency."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Parent).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Insert dummy parents
            parents = insert_dummy_parents(db, daycare.id)

            # Check that all parents have last names
            expected_last_names = [
                "Johnson",
                "Smith",
                "Davis",
                "Wilson",
                "Garcia",
                "Miller",
            ]
            parent_last_names = [parent.full_name.split()[-1] for parent in parents]

            for last_name in expected_last_names:
                assert last_name in parent_last_names

            # Verify no duplicate last names
            assert len(set(parent_last_names)) == len(parent_last_names)

        finally:
            db.close()

    def test_parents_have_valid_contact_info(self):
        """Test that parents have valid email and phone numbers."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Parent).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Insert dummy parents
            parents = insert_dummy_parents(db, daycare.id)

            for parent in parents:
                # Check email format
                assert "@" in parent.email
                assert "." in parent.email

                # Check phone number format
                assert parent.phone_num.startswith("+")
                assert len(parent.phone_num) >= 10

        finally:
            db.close()

    def test_parents_are_linked_to_daycare(self):
        """Test that all parents are properly linked to the daycare."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Parent).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Insert dummy parents
            parents = insert_dummy_parents(db, daycare.id)

            for parent in parents:
                assert parent.daycare_id == daycare.id
                assert parent.daycare == daycare

        finally:
            db.close()

    def test_parent_seeding_is_idempotent(self):
        """Test that running insert_dummy_parents multiple times doesn't create duplicates."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Parent).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Insert dummy parents twice
            parents1 = insert_dummy_parents(db, daycare.id)
            parents2 = insert_dummy_parents(db, daycare.id)

            # Should return the same parents (idempotent)
            assert len(parents1) == 6
            assert len(parents2) == 6

            # Database should still have only 6 parents
            db_parents = db.query(Parent).all()
            assert len(db_parents) == 6

        finally:
            db.close()

    def test_parent_names_are_realistic(self):
        """Test that parent names are realistic and properly formatted."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Parent).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            # Insert dummy parents
            parents = insert_dummy_parents(db, daycare.id)

            expected_names = [
                "Sara Johnson",
                "Laura Smith",
                "Angela Davis",
                "Michael Wilson",
                "Emma Garcia",
                "David Miller",
            ]

            parent_names = [parent.full_name for parent in parents]
            for expected_name in expected_names:
                assert expected_name in parent_names

        finally:
            db.close()
