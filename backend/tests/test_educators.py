from fastapi.testclient import TestClient

from app.main import app
from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole
from app.models.group import Group
from app.services.educator_service import insert_dummy_educators
from tests.conftest import TestingSessionLocal

client = TestClient(app)


class TestEducatorSeeding:
    """Test educator seeding functionality."""

    def test_insert_dummy_educators_creates_two_educators(self):
        """Test that insert_dummy_educators creates exactly 2 educators."""
        db = TestingSessionLocal()
        try:
            # Clear any existing educators first
            db.query(Educator).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Verify 2 educators were created
            educators = db.query(Educator).all()
            assert len(educators) == 2

            # Verify the expected names exist
            educator_names = [e.full_name for e in educators]
            expected_names = ["Jessica", "Mervi"]
            for name in expected_names:
                assert name in educator_names
        finally:
            db.close()

    def test_super_educator_has_all_groups(self):
        """Test that super educator is assigned to all groups."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators (this will create daycare and groups)
            insert_dummy_educators(db)

            # Find the super educator
            super_educator = (
                db.query(Educator)
                .filter(Educator.role == EducatorRole.SUPER_EDUCATOR.value)
                .first()
            )
            assert super_educator is not None
            assert super_educator.full_name == "Mervi"

            # Verify super educator has all groups
            group_names = [group.name for group in super_educator.groups]
            expected_groups = ["Group A", "Group B", "Group C"]
            for group_name in expected_groups:
                assert group_name in group_names

            # Verify super educator has exactly 3 groups
            assert len(super_educator.groups) == 3
        finally:
            db.close()

    def test_regular_educators_have_one_group_each(self):
        """Test that regular educators each belong to only one group."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get regular educators (not super educators)
            regular_educators = (
                db.query(Educator)
                .filter(Educator.role == EducatorRole.EDUCATOR.value)
                .all()
            )

            # Verify each regular educator has exactly one group
            for educator in regular_educators:
                assert len(educator.groups) == 1

            # Verify each educator is assigned to a different group
            assigned_groups = [
                educator.groups[0].name for educator in regular_educators
            ]
            assert len(set(assigned_groups)) == len(regular_educators)  # All unique
        finally:
            db.close()

    def test_educator_email_formats_are_valid(self):
        """Test that educator email formats are valid."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get all educators
            educators = db.query(Educator).all()

            # Verify email formats
            for educator in educators:
                assert educator.email is not None
                assert "@" in educator.email
                assert "." in educator.email
                assert educator.email.endswith("@daycare.com")
        finally:
            db.close()

    def test_educator_phone_formats_are_valid(self):
        """Test that educator phone number formats are valid."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get all educators
            educators = db.query(Educator).all()

            # Verify phone number formats
            for educator in educators:
                assert educator.phone_num is not None
                assert educator.phone_num.startswith("+")
                assert len(educator.phone_num) >= 10  # Reasonable phone number length
                assert educator.phone_num[1:].isdigit()  # All digits after +
        finally:
            db.close()

    def test_educator_jwt_tokens_are_non_empty(self):
        """Test that educator JWT tokens are non-empty strings."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get all educators
            educators = db.query(Educator).all()

            # Verify JWT tokens
            for educator in educators:
                assert educator.jwt_token is not None
                assert isinstance(educator.jwt_token, str)
                assert len(educator.jwt_token) > 0
        finally:
            db.close()

    def test_educator_seeding_is_idempotent(self):
        """Test that running insert_dummy_educators multiple times doesn't create duplicates."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators first time
            insert_dummy_educators(db)
            first_count = db.query(Educator).count()

            # Insert dummy educators second time
            insert_dummy_educators(db)
            second_count = db.query(Educator).count()

            # Verify no duplicates were created
            assert first_count == second_count
            assert first_count == 2
        finally:
            db.close()

    def test_educator_roles_are_correct(self):
        """Test that educator roles are correctly assigned."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get educators by role
            regular_educators = (
                db.query(Educator)
                .filter(Educator.role == EducatorRole.EDUCATOR.value)
                .all()
            )
            super_educators = (
                db.query(Educator)
                .filter(Educator.role == EducatorRole.SUPER_EDUCATOR.value)
                .all()
            )

            # Verify role distribution
            assert len(regular_educators) == 1
            assert len(super_educators) == 1

            # Verify specific educators have correct roles
            educator_names_by_role = {
                educator.full_name: educator.role
                for educator in db.query(Educator).all()
            }

            assert educator_names_by_role["Jessica"] == EducatorRole.EDUCATOR.value
            assert educator_names_by_role["Mervi"] == EducatorRole.SUPER_EDUCATOR.value
        finally:
            db.close()

    def test_educator_has_required_fields(self):
        """Test that all educators have all required fields populated."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get all educators
            educators = db.query(Educator).all()

            # Verify all required fields are populated
            for educator in educators:
                assert educator.id is not None
                assert educator.full_name is not None
                assert educator.role is not None
                assert educator.email is not None
                assert educator.phone_num is not None
                assert educator.jwt_token is not None
                assert educator.daycare_id is not None
                assert educator.created_at is not None
                assert educator.updated_at is not None
        finally:
            db.close()

    def test_educator_group_assignments_are_correct(self):
        """Test that educators are assigned to the correct groups."""
        db = TestingSessionLocal()
        try:
            # Clear any existing data
            db.query(Educator).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Insert dummy educators
            insert_dummy_educators(db)

            # Get educators and their group assignments
            educators = db.query(Educator).all()
            educator_groups = {
                educator.full_name: [group.name for group in educator.groups]
                for educator in educators
            }

            # Verify specific group assignments
            assert "Group A" in educator_groups["Jessica"]
            assert "Group A" in educator_groups["Mervi"]
            assert "Group B" in educator_groups["Mervi"]
            assert "Group C" in educator_groups["Mervi"]

            # Verify each regular educator has only one group
            assert len(educator_groups["Jessica"]) == 1

            # Verify super educator has all groups
            assert len(educator_groups["Mervi"]) == 3
        finally:
            db.close()
