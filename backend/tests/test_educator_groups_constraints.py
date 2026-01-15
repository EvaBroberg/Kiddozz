"""Tests for educator_groups table constraints and service helpers."""

import pytest
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError

from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole
from app.models.group import Group
from app.services.educator_service import (
    EducatorGroupAssignmentError,
    assign_educator_to_group,
    move_educator_between_groups,
    unassign_educator_from_group,
)


class TestEducatorGroupsConstraints:
    """Test database-level constraints on educator_groups table."""

    def test_unique_constraint_prevents_duplicates(self, db_session):
        """Test that UNIQUE(educator_id, group_id) prevents duplicate pairs."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add_all([educator, group])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group)

        # First assignment should succeed
        educator.groups.append(group)
        db_session.commit()

        # Second assignment should fail with UNIQUE constraint violation
        try:
            educator.groups.append(group)
            db_session.commit()
            pytest.fail("Expected IntegrityError for duplicate assignment")
        except IntegrityError as e:
            assert (
                "uq_educator_groups_pair" in str(e)
                or "unique constraint" in str(e).lower()
            )
            db_session.rollback()

    def test_not_null_constraint_on_educator_id(self, db_session):
        """Test that educator_id cannot be NULL."""
        conn = db_session.connection()
        try:
            conn.execute(
                text(
                    "INSERT INTO educator_groups (educator_id, group_id) VALUES (NULL, 1)"
                )
            )
            db_session.commit()
            pytest.fail("Expected IntegrityError for NULL educator_id")
        except IntegrityError:
            db_session.rollback()

    def test_not_null_constraint_on_group_id(self, db_session):
        """Test that group_id cannot be NULL."""
        conn = db_session.connection()
        try:
            conn.execute(
                text(
                    "INSERT INTO educator_groups (educator_id, group_id) VALUES (1, NULL)"
                )
            )
            db_session.commit()
            pytest.fail("Expected IntegrityError for NULL group_id")
        except IntegrityError:
            db_session.rollback()

    def test_trigger_prevents_zero_groups(self, db_session):
        """Test that constraint trigger prevents removing last group assignment."""
        # This constraint is enforced via a database trigger in PostgreSQL.
        # Our default local test DB is SQLite, which does not run those triggers.
        if db_session.get_bind().dialect.name != "postgresql":
            pytest.skip("Trigger enforcement requires PostgreSQL")

        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add_all([educator, group])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group)

        # Assign educator to group (now has exactly 1 group)
        educator.groups.append(group)
        db_session.commit()

        # Try to remove the only group - should fail
        try:
            educator.groups.remove(group)
            db_session.commit()
            pytest.fail("Expected constraint trigger to prevent removing last group")
        except Exception as e:
            error_msg = str(e)
            assert "must belong to at least one group" in error_msg
            db_session.rollback()

    def test_trigger_allows_multiple_groups(self, db_session):
        """Test that educator can have multiple groups."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([educator, group1, group2])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group1)
        db_session.refresh(group2)

        # Assign to first group
        educator.groups.append(group1)
        db_session.commit()

        # Assign to second group
        educator.groups.append(group2)
        db_session.commit()

        # Verify educator has 2 groups
        assert len(educator.groups) == 2

        # Remove one group (should succeed since educator still has another)
        educator.groups.remove(group1)
        db_session.commit()

        # Verify educator still has 1 group
        assert len(educator.groups) == 1
        assert group2 in educator.groups

    def test_move_between_groups_succeeds(self, db_session):
        """Test that moving educator between groups in one transaction succeeds."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([educator, group1, group2])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group1)
        db_session.refresh(group2)

        # Assign to first group
        educator.groups.append(group1)
        db_session.commit()

        # Move from group1 to group2 in one transaction
        # Add group2 first, then remove group1 (ensures always has >= 1 group)
        educator.groups.append(group2)
        educator.groups.remove(group1)
        db_session.commit()

        # Verify educator is in group2 but not group1
        assert len(educator.groups) == 1
        assert group2 in educator.groups
        assert group1 not in educator.groups

    def test_cascade_delete_removes_join_rows(self, db_session):
        """Test that deleting an educator removes their group assignments."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add_all([educator, group])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group)

        # Assign educator to group
        educator.groups.append(group)
        db_session.commit()

        # Delete educator
        db_session.delete(educator)
        db_session.commit()

        # Verify educator_groups row was deleted (CASCADE)
        conn = db_session.connection()
        result = conn.execute(
            text("SELECT COUNT(*) FROM educator_groups WHERE educator_id = :edu_id"),
            {"edu_id": educator.id},
        )
        count = result.scalar()
        assert count == 0


class TestEducatorGroupServiceHelpers:
    """Test service helper functions for educator group assignments."""

    def test_assign_educator_to_group_success(self, db_session):
        """Test that assign_educator_to_group successfully assigns an educator to a group."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add_all([educator, group])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group)

        # Assign
        assign_educator_to_group(db_session, educator.id, group.id)

        # Verify assignment
        assert group in educator.groups

    def test_assign_educator_to_group_idempotent(self, db_session):
        """Test that assign_educator_to_group is idempotent (no duplicates)."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add_all([educator, group])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group)

        # First assignment
        assign_educator_to_group(db_session, educator.id, group.id)
        assert len(educator.groups) == 1

        # Second assignment (should be no-op)
        assign_educator_to_group(db_session, educator.id, group.id)
        assert len(educator.groups) == 1  # Still only one

    def test_assign_educator_to_group_raises_on_not_found(self, db_session):
        """Test that assign_educator_to_group raises ValueError for non-existent entities."""
        with pytest.raises(ValueError, match="Educator with id 999 not found"):
            assign_educator_to_group(db_session, 999, 1)

        # Create educator but not group
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

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

        with pytest.raises(ValueError, match="Group with id 999 not found"):
            assign_educator_to_group(db_session, educator.id, 999)

    def test_unassign_educator_from_group_success(self, db_session):
        """Test that unassign_educator_from_group successfully removes an assignment."""
        # Create test data with 2 groups
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([educator, group1, group2])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group1)
        db_session.refresh(group2)

        # Assign to both groups
        educator.groups.append(group1)
        educator.groups.append(group2)
        db_session.commit()

        # Unassign from one group
        unassign_educator_from_group(db_session, educator.id, group1.id)

        # Verify educator still has the other group
        assert len(educator.groups) == 1
        assert group2 in educator.groups

    def test_unassign_educator_from_group_prevents_zero_groups(self, db_session):
        """Test that unassign_educator_from_group prevents leaving educator with zero groups."""
        # Create test data with 1 group
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group = Group(name="Test Group", daycare_id=daycare.id)
        db_session.add_all([educator, group])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group)

        # Assign to group
        educator.groups.append(group)
        db_session.commit()

        # Try to unassign (should fail)
        with pytest.raises(
            EducatorGroupAssignmentError,
            match="must belong to at least one group.*Cannot delete the last group assignment",
        ):
            unassign_educator_from_group(db_session, educator.id, group.id)

        # Verify assignment still exists
        assert group in educator.groups

    def test_unassign_educator_from_group_idempotent(self, db_session):
        """Test that unassign_educator_from_group is idempotent for non-assigned groups."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([educator, group1, group2])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group1)
        db_session.refresh(group2)

        # Assign to group1 only
        educator.groups.append(group1)
        db_session.commit()

        # Try to unassign from group2 (not assigned) - should be no-op
        unassign_educator_from_group(db_session, educator.id, group2.id)

        # Verify group1 assignment still exists
        assert group1 in educator.groups

    def test_move_educator_between_groups_success(self, db_session):
        """Test that move_educator_between_groups successfully moves an educator."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([educator, group1, group2])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group1)
        db_session.refresh(group2)

        # Assign to group1
        educator.groups.append(group1)
        db_session.commit()

        # Move from group1 to group2
        move_educator_between_groups(db_session, educator.id, group1.id, group2.id)

        # Verify move succeeded
        assert len(educator.groups) == 1
        assert group2 in educator.groups
        assert group1 not in educator.groups

    def test_move_educator_between_groups_raises_on_not_found(self, db_session):
        """Test that move_educator_between_groups raises ValueError for non-existent entities."""
        with pytest.raises(ValueError, match="Educator with id 999 not found"):
            move_educator_between_groups(db_session, 999, 1, 2)

        # Create educator but not groups
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

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

        with pytest.raises(ValueError, match="Group with id 1 not found"):
            move_educator_between_groups(db_session, educator.id, 1, 2)

    def test_move_educator_between_groups_raises_if_not_in_from_group(self, db_session):
        """Test that move_educator_between_groups raises if educator not in from_group."""
        # Create test data
        daycare = Daycare(name="Test Daycare")
        db_session.add(daycare)
        db_session.commit()
        db_session.refresh(daycare)

        educator = Educator(
            full_name="Test Educator",
            email="test@example.com",
            phone_num="+1234567890",
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        group1 = Group(name="Group 1", daycare_id=daycare.id)
        group2 = Group(name="Group 2", daycare_id=daycare.id)
        db_session.add_all([educator, group1, group2])
        db_session.commit()
        db_session.refresh(educator)
        db_session.refresh(group1)
        db_session.refresh(group2)

        # Don't assign educator to any group

        # Try to move - should fail
        with pytest.raises(
            ValueError,
            match=f"Educator {educator.id} is not assigned to group {group1.id}",
        ):
            move_educator_between_groups(db_session, educator.id, group1.id, group2.id)
