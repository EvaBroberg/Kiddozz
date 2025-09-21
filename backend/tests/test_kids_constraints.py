from datetime import date

from fastapi import HTTPException

from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import Kid
from app.models.parent import Parent
from app.services.kid_service import (
    create_kid,
    ensure_kid_has_parents,
    get_kids_by_parent,
    validate_kid_parent_relationship,
)
from tests.conftest import TestingSessionLocal


class TestKidConstraints:
    """Test kid-parent relationship constraints and validation."""

    def test_create_kid_without_parent_fails(self):
        """Test that creating a kid without linking a parent fails."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Try to create a kid without any parent IDs
            try:
                create_kid(
                    db=db,
                    full_name="Test Kid",
                    dob="2020-01-01",
                    daycare_id=daycare.id,
                    group_id=group.id,
                    parent_ids=[],  # Empty parent list
                )
                assert False, "Expected HTTPException but none was raised"
            except HTTPException as e:
                assert e.status_code == 400
                assert "Kid must be linked to at least one parent" in e.detail

        finally:
            db.close()

    def test_create_kid_with_none_parent_ids_fails(self):
        """Test that creating a kid with None parent_ids fails."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Try to create a kid with None parent IDs
            try:
                create_kid(
                    db=db,
                    full_name="Test Kid",
                    dob="2020-01-01",
                    daycare_id=daycare.id,
                    group_id=group.id,
                    parent_ids=None,  # None parent list
                )
                assert False, "Expected HTTPException but none was raised"
            except HTTPException as e:
                assert e.status_code == 400
                assert "Kid must be linked to at least one parent" in e.detail

        finally:
            db.close()

    def test_create_kid_with_parent_succeeds(self):
        """Test that creating a kid linked to an existing parent succeeds."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare, group, and parent
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="parent@test.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid linked to the parent
            kid = create_kid(
                db=db,
                full_name="Test Kid",
                dob="2020-01-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent.id],
            )

            # Verify the kid was created and linked to the parent
            assert kid.id is not None
            assert kid.full_name == "Test Kid"
            assert len(kid.parents) == 1
            assert kid.parents[0].id == parent.id
            assert parent.kids[0].id == kid.id

        finally:
            db.close()

    def test_parent_and_kid_share_same_daycare(self):
        """Test that both parent and kid belong to the same daycare."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create two daycares
            daycare1 = Daycare(name="Daycare 1")
            daycare2 = Daycare(name="Daycare 2")
            db.add(daycare1)
            db.add(daycare2)
            db.commit()
            db.refresh(daycare1)
            db.refresh(daycare2)

            # Create groups for each daycare
            group1 = Group(name="Group 1", daycare_id=daycare1.id)
            group2 = Group(name="Group 2", daycare_id=daycare2.id)
            db.add(group1)
            db.add(group2)
            db.commit()
            db.refresh(group1)
            db.refresh(group2)

            # Create parent in daycare1
            parent = Parent(
                full_name="Test Parent",
                email="parent@test.com",
                phone_num="+1234567890",
                daycare_id=daycare1.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Try to create kid in daycare2 but link to parent in daycare1
            try:
                create_kid(
                    db=db,
                    full_name="Test Kid",
                    dob="2020-01-01",
                    daycare_id=daycare2.id,  # Different daycare
                    group_id=group2.id,
                    parent_ids=[parent.id],  # Parent from daycare1
                )
                assert False, "Expected HTTPException but none was raised"
            except HTTPException as e:
                assert e.status_code == 400
                assert "don't belong to the same daycare" in e.detail

        finally:
            db.close()

    def test_create_kid_with_multiple_parents_succeeds(self):
        """Test that creating a kid linked to multiple parents succeeds."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create two parents
            parent1 = Parent(
                full_name="Parent 1",
                email="parent1@test.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            parent2 = Parent(
                full_name="Parent 2",
                email="parent2@test.com",
                phone_num="+1234567891",
                daycare_id=daycare.id,
            )
            db.add(parent1)
            db.add(parent2)
            db.commit()
            db.refresh(parent1)
            db.refresh(parent2)

            # Create a kid linked to both parents
            kid = create_kid(
                db=db,
                full_name="Test Kid",
                dob="2020-01-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent1.id, parent2.id],
            )

            # Verify the kid was created and linked to both parents
            assert kid.id is not None
            assert len(kid.parents) == 2
            parent_ids = [p.id for p in kid.parents]
            assert parent1.id in parent_ids
            assert parent2.id in parent_ids

        finally:
            db.close()

    def test_validate_kid_parent_relationship(self):
        """Test the validate_kid_parent_relationship function."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create a parent
            parent = Parent(
                full_name="Test Parent",
                email="parent@test.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid with parent
            kid_with_parent = create_kid(
                db=db,
                full_name="Kid With Parent",
                dob="2020-01-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent.id],
            )

            # Create a kid without parent (manually to bypass validation)
            kid_without_parent = Kid(
                full_name="Kid Without Parent",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
            )
            db.add(kid_without_parent)
            db.commit()
            db.refresh(kid_without_parent)

            # Test validation
            assert validate_kid_parent_relationship(db, kid_with_parent.id) is True
            assert validate_kid_parent_relationship(db, kid_without_parent.id) is False
            assert (
                validate_kid_parent_relationship(db, 99999) is False
            )  # Non-existent kid

        finally:
            db.close()

    def test_ensure_kid_has_parents(self):
        """Test the ensure_kid_has_parents function."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create a parent
            parent = Parent(
                full_name="Test Parent",
                email="parent@test.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            # Create a kid with parent
            kid_with_parent = create_kid(
                db=db,
                full_name="Kid With Parent",
                dob="2020-01-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent.id],
            )

            # Create a kid without parent (manually to bypass validation)
            kid_without_parent = Kid(
                full_name="Kid Without Parent",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
            )
            db.add(kid_without_parent)
            db.commit()
            db.refresh(kid_without_parent)

            # Test ensure_kid_has_parents
            ensure_kid_has_parents(db, kid_with_parent)  # Should not raise

            try:
                ensure_kid_has_parents(db, kid_without_parent)
                assert False, "Expected HTTPException but none was raised"
            except HTTPException as e:
                assert e.status_code == 400
                assert "Kid must be linked to at least one parent" in e.detail

        finally:
            db.close()

    def test_get_kids_by_parent(self):
        """Test getting kids by parent ID."""
        db = TestingSessionLocal()
        try:
            # Clear existing data
            db.query(Kid).delete()
            db.query(Parent).delete()
            db.query(Group).delete()
            db.query(Daycare).delete()
            db.commit()

            # Create a daycare and group
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Test Group", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            # Create two parents
            parent1 = Parent(
                full_name="Parent 1",
                email="parent1@test.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            parent2 = Parent(
                full_name="Parent 2",
                email="parent2@test.com",
                phone_num="+1234567891",
                daycare_id=daycare.id,
            )
            db.add(parent1)
            db.add(parent2)
            db.commit()
            db.refresh(parent1)
            db.refresh(parent2)

            # Create kids for each parent
            create_kid(
                db=db,
                full_name="Kid 1",
                dob="2020-01-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent1.id],
            )
            create_kid(
                db=db,
                full_name="Kid 2",
                dob="2020-02-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent1.id],
            )
            create_kid(
                db=db,
                full_name="Kid 3",
                dob="2020-03-01",
                daycare_id=daycare.id,
                group_id=group.id,
                parent_ids=[parent2.id],
            )

            # Test getting kids by parent
            parent1_kids = get_kids_by_parent(db, parent1.id)
            parent2_kids = get_kids_by_parent(db, parent2.id)
            non_existent_kids = get_kids_by_parent(db, 99999)

            assert len(parent1_kids) == 2
            assert len(parent2_kids) == 1
            assert len(non_existent_kids) == 0

            kid_names_1 = [kid.full_name for kid in parent1_kids]
            assert "Kid 1" in kid_names_1
            assert "Kid 2" in kid_names_1

            kid_names_2 = [kid.full_name for kid in parent2_kids]
            assert "Kid 3" in kid_names_2

        finally:
            db.close()
