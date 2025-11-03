from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import create_access_token
from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole
from app.models.group import Group


def insert_dummy_educators(db: Session) -> None:
    """
    Insert dummy educators into the database.
    Creates 4 educators with different roles and group assignments.
    Prevents duplicates by checking if educators already exist.
    """
    # Check if educators already exist
    existing_educators = (
        db.query(Educator).filter(Educator.full_name.in_(["Jessica", "Mervi"])).count()
    )

    if existing_educators > 0:
        return  # Educators already exist, don't create duplicates

    # Get or create a default daycare
    daycare = db.query(Daycare).first()
    if not daycare:
        daycare = Daycare(name="Happy Kids Daycare")
        db.add(daycare)
        db.commit()
        db.refresh(daycare)

    # Get or create groups
    groups = db.query(Group).filter(Group.daycare_id == daycare.id).all()
    if not groups:
        # Create groups if they don't exist
        group_names = ["Group A", "Group B", "Group C"]
        groups = []
        for group_name in group_names:
            group = Group(name=group_name, daycare_id=daycare.id)
            db.add(group)
            groups.append(group)
        db.commit()
        for group in groups:
            db.refresh(group)

    # Create dummy educators
    dummy_educators = [
        {
            "full_name": "Jessica",
            "email": "jessica@daycare.com",
            "phone_num": "+1234567890",
            "role": EducatorRole.EDUCATOR.value,
            "group_names": ["Group A"],
        },
        {
            "full_name": "Mervi",
            "email": "mervi@daycare.com",
            "phone_num": "+1234567891",
            "role": EducatorRole.SUPER_EDUCATOR.value,
            "group_names": ["Group A", "Group B", "Group C"],
        },
    ]

    educators = []
    for educator_data in dummy_educators:
        # Create JWT token
        jwt_payload = {
            "sub": educator_data["full_name"],
            "role": educator_data["role"],
            "user_id": len(educators) + 1,
            "daycare_id": daycare.id,
        }
        jwt_token = create_access_token(jwt_payload)

        educator = Educator(
            full_name=educator_data["full_name"],
            email=educator_data["email"],
            phone_num=educator_data["phone_num"],
            role=educator_data["role"],
            jwt_token=jwt_token,
            daycare_id=daycare.id,
        )
        db.add(educator)
        educators.append(educator)

    db.commit()
    for educator in educators:
        db.refresh(educator)

    # Assign educators to groups
    for educator_data, educator in zip(dummy_educators, educators):
        for group_name in educator_data["group_names"]:
            group = next((g for g in groups if g.name == group_name), None)
            if group:
                educator.groups.append(group)

    db.commit()
    print("✅ Seeded educators successfully!")


class EducatorGroupAssignmentError(Exception):
    """Raised when an educator group assignment operation violates business rules."""
    pass


def assign_educator_to_group(db: Session, educator_id: int, group_id: int) -> None:
    """
    Assign an educator to a group (idempotent).
    
    If the educator is already assigned to the group, this is a no-op.
    Raises ValueError if educator or group doesn't exist.
    
    Args:
        db: Database session
        educator_id: ID of the educator
        group_id: ID of the group
        
    Raises:
        ValueError: If educator or group doesn't exist
        IntegrityError: If database constraint is violated (shouldn't happen with UNIQUE)
    """
    # Verify educator exists
    educator = db.query(Educator).filter(Educator.id == educator_id).first()
    if not educator:
        raise ValueError(f"Educator with id {educator_id} not found")
    
    # Verify group exists
    group = db.query(Group).filter(Group.id == group_id).first()
    if not group:
        raise ValueError(f"Group with id {group_id} not found")
    
    # Check if already assigned (idempotent)
    if group in educator.groups:
        return  # Already assigned, no-op
    
    # Assign (UNIQUE constraint will prevent duplicates)
    educator.groups.append(group)
    try:
        db.commit()
    except IntegrityError as e:
        db.rollback()
        # UNIQUE constraint violation means duplicate (shouldn't happen if check above works)
        raise ValueError(f"Educator {educator_id} is already assigned to group {group_id}") from e


def unassign_educator_from_group(db: Session, educator_id: int, group_id: int) -> None:
    """
    Unassign an educator from a group.
    
    Raises EducatorGroupAssignmentError if this would leave the educator with zero groups.
    This matches the database trigger error message.
    
    Args:
        db: Database session
        educator_id: ID of the educator
        group_id: ID of the group
        
    Raises:
        ValueError: If educator or group doesn't exist
        EducatorGroupAssignmentError: If unassigning would leave educator with zero groups
    """
    # Verify educator exists
    educator = db.query(Educator).filter(Educator.id == educator_id).first()
    if not educator:
        raise ValueError(f"Educator with id {educator_id} not found")
    
    # Verify group exists
    group = db.query(Group).filter(Group.id == group_id).first()
    if not group:
        raise ValueError(f"Group with id {group_id} not found")
    
    # Check if assigned
    if group not in educator.groups:
        return  # Not assigned, no-op
    
    # Check if this would leave educator with zero groups
    if len(educator.groups) == 1:
        raise EducatorGroupAssignmentError(
            f"Educator {educator_id} must belong to at least one group. Cannot delete the last group assignment."
        )
    
    # Unassign
    educator.groups.remove(group)
    try:
        db.commit()
    except Exception as e:
        db.rollback()
        # The database trigger should also catch this, but we check in Python for better error messages
        error_msg = str(e)
        if "must belong to at least one group" in error_msg:
            raise EducatorGroupAssignmentError(error_msg) from e
        raise


def move_educator_between_groups(
    db: Session, educator_id: int, from_group_id: int, to_group_id: int
) -> None:
    """
    Move an educator from one group to another in a single transaction.
    
    This ensures the database trigger never fires because the educator always has
    at least one group assignment at commit time.
    
    Args:
        db: Database session
        educator_id: ID of the educator
        from_group_id: ID of the group to remove
        to_group_id: ID of the group to add
        
    Raises:
        ValueError: If educator or groups don't exist, or if educator isn't in from_group
        IntegrityError: If database constraint is violated
    """
    # Verify educator exists
    educator = db.query(Educator).filter(Educator.id == educator_id).first()
    if not educator:
        raise ValueError(f"Educator with id {educator_id} not found")
    
    # Verify groups exist
    from_group = db.query(Group).filter(Group.id == from_group_id).first()
    if not from_group:
        raise ValueError(f"Group with id {from_group_id} not found")
    
    to_group = db.query(Group).filter(Group.id == to_group_id).first()
    if not to_group:
        raise ValueError(f"Group with id {to_group_id} not found")
    
    # Check if educator is in from_group
    if from_group not in educator.groups:
        raise ValueError(
            f"Educator {educator_id} is not assigned to group {from_group_id}"
        )
    
    # Check if already in to_group (move to same group is no-op)
    if to_group in educator.groups:
        # If only one group, this would be a duplicate, so just return
        if len(educator.groups) == 1:
            return
        # Otherwise, remove from_group and keep to_group
        educator.groups.remove(from_group)
    else:
        # Move: add new group first, then remove old group
        # This ensures the educator always has at least one group at commit time
        educator.groups.append(to_group)
        educator.groups.remove(from_group)
    
    try:
        db.commit()
    except IntegrityError as e:
        db.rollback()
        error_msg = str(e)
        if "uq_educator_groups_pair" in error_msg or "unique constraint" in error_msg.lower():
            raise ValueError(f"Educator {educator_id} is already assigned to group {to_group_id}") from e
        raise
