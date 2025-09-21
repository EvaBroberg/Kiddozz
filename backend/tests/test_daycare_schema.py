import pytest
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole
from app.models.group import Group
from app.models.kid import Kid
from app.models.parent import Parent
from app.services.seeder import clear_daycare_data, seed_daycare_data

# Use the same test database as other tests
SQLALCHEMY_DATABASE_URL = "sqlite:///./test.db"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture
def db_session():
    """Create a test database session."""
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def seeded_data(db_session):
    """Seed the database with test data and clean up after."""
    # Clear any existing data
    clear_daycare_data(db_session)

    # Seed new data
    seed_daycare_data(db_session)

    # Also seed educators
    from app.services.educator_service import insert_dummy_educators

    insert_dummy_educators(db_session)

    yield db_session

    # Clean up after test
    clear_daycare_data(db_session)


class TestDaycareSchema:
    """Test the multi-daycare database schema and relationships."""

    def test_daycare_creation(self, db_session):
        """Test creating a daycare."""
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        assert daycare.id is not None
        assert daycare.name == "Test Daycare"
        assert daycare.created_at is not None
        assert daycare.updated_at is not None

    def test_group_creation_with_daycare(self, db_session):
        """Test creating a group that belongs to a daycare."""
        # Create daycare first
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create group
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add(group)
        db_session.commit()
        db_session.refresh(group)

        assert group.id is not None
        assert group.name == "Test Group"
        assert group.daycare_id == daycare.id
        assert group.daycare == daycare

    def test_educator_creation_with_daycare(self, db_session):
        """Test creating an educator that belongs to a daycare."""
        # Create daycare first
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create educator
        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        assert educator.id is not None
        assert educator.full_name == "Test Educator"
        assert educator.daycare_id == daycare.id
        assert educator.daycare == daycare

    def test_parent_creation_with_daycare(self, db_session):
        """Test creating a parent that belongs to a daycare."""
        # Create daycare first
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create parent
        parent = Parent(
            full_name="Test Parent",
            email="parent@example.com",
            phone_num="+1234567890",
            daycare_id=daycare.id,
        )
        db_session.add(parent)
        db_session.commit()
        db_session.refresh(parent)

        assert parent.id is not None
        assert parent.full_name == "Test Parent"
        assert parent.daycare_id == daycare.id
        assert parent.daycare == daycare

    def test_kid_creation_with_relationships(self, db_session):
        """Test creating a kid with daycare and group relationships."""
        # Create daycare
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create group
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add(group)
        db_session.commit()
        db_session.refresh(group)

        # Create kid
        from datetime import date

        kid = Kid(
            full_name="Test Kid",
            dob=date(2020, 1, 1),
            daycare_id=daycare.id,
            group_id=group.id,
            trusted_adults=[
                {
                    "name": "Trusted Adult",
                    "email": "trusted@example.com",
                    "phone": "+1234567890",
                    "address": "123 Test St",
                }
            ],
        )
        db_session.add(kid)
        db_session.commit()
        db_session.refresh(kid)

        assert kid.id is not None
        assert kid.full_name == "Test Kid"
        assert kid.daycare_id == daycare.id
        assert kid.group_id == group.id
        assert kid.daycare == daycare
        assert kid.group == group
        assert len(kid.trusted_adults) == 1
        assert kid.trusted_adults[0]["name"] == "Trusted Adult"

    def test_educator_group_relationship(self, db_session):
        """Test many-to-many relationship between educators and groups."""
        # Create daycare
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create groups
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([group1, group2])
        db_session.commit()

        # Create educator
        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        # Assign educator to groups
        educator.groups.append(group1)
        educator.groups.append(group2)
        db_session.commit()

        # Verify relationships
        assert len(educator.groups) == 2
        assert group1 in educator.groups
        assert group2 in educator.groups
        assert educator in group1.educators
        assert educator in group2.educators

    def test_parent_kid_relationship(self, db_session):
        """Test many-to-many relationship between parents and kids."""
        # Create daycare
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create group
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add(group)
        db_session.commit()
        db_session.refresh(group)

        # Create parent
        parent = Parent(
            full_name="Test Parent",
            email="parent@example.com",
            phone_num="+1234567890",
            daycare_id=daycare.id,
        )
        db_session.add(parent)
        db_session.commit()
        db_session.refresh(parent)

        # Create kids
        from datetime import date

        kid1 = Kid(
            full_name="Kid 1",
            dob=date(2020, 1, 1),
            daycare_id=daycare.id,
            group_id=group.id,
        )
        kid2 = Kid(
            full_name="Kid 2",
            dob=date(2020, 2, 1),
            daycare_id=daycare.id,
            group_id=group.id,
        )
        db_session.add_all([kid1, kid2])
        db_session.commit()

        # Link parent to kids
        parent.kids.append(kid1)
        parent.kids.append(kid2)
        db_session.commit()

        # Verify relationships
        assert len(parent.kids) == 2
        assert kid1 in parent.kids
        assert kid2 in parent.kids
        assert parent in kid1.parents
        assert parent in kid2.parents

    def test_cascade_delete_daycare(self, db_session):
        """Test that deleting a daycare cascades to all related records."""
        # Create daycare with related data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create group
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add(group)
        db_session.commit()
        db_session.refresh(group)

        # Create educator
        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db_session.add(educator)
        db_session.commit()
        db_session.refresh(educator)

        # Create parent
        parent = Parent(
            full_name="Test Parent",
            email="parent@example.com",
            phone_num="+1234567890",
            daycare_id=daycare.id,
        )
        db_session.add(parent)
        db_session.commit()
        db_session.refresh(parent)

        # Create kid
        from datetime import date

        kid = Kid(
            full_name="Test Kid",
            dob=date(2020, 1, 1),
            daycare_id=daycare.id,
            group_id=group.id,
        )
        db_session.add(kid)
        db_session.commit()
        db_session.refresh(kid)

        # Link relationships
        educator.groups.append(group)
        parent.kids.append(kid)
        db_session.commit()

        # Delete daycare
        db_session.delete(daycare)
        db_session.commit()

        # Verify all related records are deleted
        assert db_session.query(Daycare).filter_by(id=daycare.id).first() is None
        assert db_session.query(Group).filter_by(daycare_id=daycare.id).first() is None
        assert (
            db_session.query(Educator).filter_by(daycare_id=daycare.id).first() is None
        )
        assert db_session.query(Parent).filter_by(daycare_id=daycare.id).first() is None
        assert db_session.query(Kid).filter_by(daycare_id=daycare.id).first() is None

    def test_seeded_data_structure(self, seeded_data):
        """Test that seeded data has correct structure and relationships."""
        # Get all data
        daycare = seeded_data.query(Daycare).first()
        groups = seeded_data.query(Group).all()
        educators = seeded_data.query(Educator).all()
        parents = seeded_data.query(Parent).all()
        kids = seeded_data.query(Kid).all()

        # Verify counts
        assert daycare is not None
        assert len(groups) == 3
        assert len(educators) == 4  # 3 regular + 1 super-educator
        assert len(parents) == 3
        assert len(kids) == 9

        # Verify daycare
        assert daycare.name == "Happy Kids Daycare"

        # Verify groups
        group_names = [group.name for group in groups]
        assert "Group A" in group_names
        assert "Group B" in group_names
        assert "Group C" in group_names

        # Verify educators
        educator_names = [educator.full_name for educator in educators]
        assert "Anna Johnson" in educator_names
        assert "Mark Smith" in educator_names
        assert "Sarah Davis" in educator_names
        assert "Lisa Wilson" in educator_names

        # Verify Lisa Wilson is super-educator
        lisa = next(e for e in educators if e.full_name == "Lisa Wilson")
        assert lisa.role == EducatorRole.SUPER_EDUCATOR.value
        assert len(lisa.groups) == 3  # Assigned to all groups

        # Verify other educators are assigned to groups
        anna = next(e for e in educators if e.full_name == "Anna Johnson")
        mark = next(e for e in educators if e.full_name == "Mark Smith")
        sarah = next(e for e in educators if e.full_name == "Sarah Davis")

        assert len(anna.groups) == 1
        assert len(mark.groups) == 1
        assert len(sarah.groups) == 1

        # Verify parents
        parent_names = [parent.full_name for parent in parents]
        assert "Sara" in parent_names
        assert "Laura" in parent_names
        assert "Angela" in parent_names

        # Verify kids are distributed across groups
        kids_by_group = {}
        for kid in kids:
            group_name = next(g.name for g in groups if g.id == kid.group_id)
            if group_name not in kids_by_group:
                kids_by_group[group_name] = []
            kids_by_group[group_name].append(kid)

        assert len(kids_by_group["Group A"]) == 3
        assert len(kids_by_group["Group B"]) == 3
        assert len(kids_by_group["Group C"]) == 3

        # Verify each parent is linked to at least one kid
        for parent in parents:
            assert len(parent.kids) > 0

        # Verify each kid is linked to at least one parent
        for kid in kids:
            assert len(kid.parents) > 0

    def test_daycare_filtering(self, seeded_data):
        """Test that queries can be filtered by daycare_id for security."""
        # Get the daycare
        daycare = seeded_data.query(Daycare).first()

        # Test filtering kids by daycare
        kids_in_daycare = seeded_data.query(Kid).filter_by(daycare_id=daycare.id).all()
        assert len(kids_in_daycare) == 9

        # Test filtering educators by daycare
        educators_in_daycare = (
            seeded_data.query(Educator).filter_by(daycare_id=daycare.id).all()
        )
        assert len(educators_in_daycare) == 4

        # Test filtering parents by daycare
        parents_in_daycare = (
            seeded_data.query(Parent).filter_by(daycare_id=daycare.id).all()
        )
        assert len(parents_in_daycare) == 3

        # Test filtering groups by daycare
        groups_in_daycare = (
            seeded_data.query(Group).filter_by(daycare_id=daycare.id).all()
        )
        assert len(groups_in_daycare) == 3

    def test_trusted_adults_json_storage(self, seeded_data):
        """Test that trusted_adults JSON data is stored and retrieved correctly."""
        kids_with_trusted_adults = (
            seeded_data.query(Kid).filter(Kid.trusted_adults.isnot(None)).all()
        )

        assert len(kids_with_trusted_adults) > 0

        for kid in kids_with_trusted_adults:
            assert kid.trusted_adults is not None
            assert isinstance(kid.trusted_adults, list)
            if len(kid.trusted_adults) > 0:
                trusted_adult = kid.trusted_adults[0]
                assert "name" in trusted_adult
                assert "email" in trusted_adult
                assert "phone" in trusted_adult
                assert "address" in trusted_adult

    def test_duplicate_email_constraints_within_table(self, db_session):
        """Test that email uniqueness constraints work within the same table."""
        # Create daycare
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        # Create first educator with email
        educator1 = Educator(
            full_name="Test Educator 1",
            email="duplicate@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db_session.add(educator1)
        db_session.commit()

        # Try to create second educator with same email - should fail
        educator2 = Educator(
            full_name="Test Educator 2",
            email="duplicate@example.com",  # Same email
            phone_num="+1234567891",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db_session.add(educator2)

        with pytest.raises(Exception):  # Should raise integrity error
            db_session.commit()

    def test_foreign_key_constraints(self, db_session):
        """Test that foreign key constraints work properly."""
        # Enable foreign key constraints for SQLite
        db_session.execute(text("PRAGMA foreign_keys=ON"))

        # Try to create group without valid daycare_id
        group = Group(name="Orphan Group", daycare_id="invalid-uuid")
        db_session.add(group)

        with pytest.raises(Exception):  # Should raise foreign key constraint error
            db_session.commit()
