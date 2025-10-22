from datetime import date
from typing import List, Optional

from fastapi import HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.kid import Kid, KidAbsence
from app.models.parent import Parent
from app.schemas.kid import KidAbsenceCreate


def create_kid(
    db: Session,
    full_name: str,
    dob: str,
    daycare_id: str,
    group_id: int,
    parent_ids: List[int],
    trusted_adults: Optional[List[dict]] = None,
) -> Kid:
    """
    Create a new kid with validation to ensure at least one parent is linked.

    Args:
        db: Database session
        full_name: Kid's full name
        dob: Date of birth (YYYY-MM-DD format)
        group_id: ID of the group the kid belongs to
        parent_ids: List of parent IDs to link to the kid
        trusted_adults: Optional list of trusted adults
        daycare_id: ID of the daycare

    Returns:
        Created Kid object

    Raises:
        HTTPException: If validation fails
    """
    # Validate that at least one parent is provided
    if not parent_ids or len(parent_ids) == 0:
        raise HTTPException(
            status_code=400, detail="Kid must be linked to at least one parent"
        )

    # Validate that all parents exist and belong to the same daycare
    parents = (
        db.query(Parent)
        .filter(Parent.id.in_(parent_ids), Parent.daycare_id == daycare_id)
        .all()
    )

    if len(parents) != len(parent_ids):
        raise HTTPException(
            status_code=400,
            detail="One or more parent IDs are invalid or don't belong to the same daycare",
        )

    # Convert dob string to date object if needed
    if isinstance(dob, str):
        dob = date.fromisoformat(dob)

    # Create the kid
    kid = Kid(
        full_name=full_name,
        dob=dob,
        daycare_id=daycare_id,
        group_id=group_id,
        trusted_adults=trusted_adults or [],
    )

    db.add(kid)
    db.flush()  # Flush to get the kid ID

    # Link parents to the kid
    for parent in parents:
        parent.kids.append(kid)

    db.commit()
    db.refresh(kid)

    return kid


def get_kid_by_id(db: Session, kid_id: int) -> Optional[Kid]:
    """Get a kid by ID."""
    return db.query(Kid).filter(Kid.id == kid_id).first()


def get_kids_by_daycare(db: Session, daycare_id: str) -> List[Kid]:
    """Get all kids in a specific daycare."""
    return db.query(Kid).filter(Kid.daycare_id == daycare_id).all()


def get_kids_by_parent(db: Session, parent_id: int) -> List[Kid]:
    """Get all kids linked to a specific parent."""
    parent = db.query(Parent).filter(Parent.id == parent_id).first()
    if not parent:
        return []
    return parent.kids


def get_kids_for_parent(db: Session, parent_id: int) -> List[Kid]:
    """Get all kids linked to a specific parent (alias for get_kids_by_parent)."""
    return get_kids_by_parent(db, parent_id)


def validate_kid_parent_relationship(db: Session, kid_id: int) -> bool:
    """
    Validate that a kid has at least one parent linked.

    Args:
        db: Database session
        kid_id: ID of the kid to validate

    Returns:
        True if kid has at least one parent, False otherwise
    """
    kid = db.query(Kid).filter(Kid.id == kid_id).first()
    if not kid:
        return False

    return len(kid.parents) > 0


def ensure_kid_has_parents(db: Session, kid: Kid) -> None:
    """
    Ensure a kid has at least one parent linked. Raise exception if not.

    Args:
        db: Database session
        kid: Kid object to validate

    Raises:
        HTTPException: If kid has no parents
    """
    if not kid.parents or len(kid.parents) == 0:
        raise HTTPException(
            status_code=400, detail="Kid must be linked to at least one parent"
        )


def create_kid_absence(
    db: Session, kid_id: int, absence_data: KidAbsenceCreate, parent_id: int
) -> KidAbsence:
    """
    Create a new absence record for a kid.

    Args:
        db: Database session
        kid_id: ID of the kid
        absence_data: Absence data
        parent_id: ID of the parent creating the absence

    Returns:
        Created KidAbsence object

    Raises:
        HTTPException: If validation fails
    """
    # Verify kid exists and parent is linked to kid
    kid = db.query(Kid).filter(Kid.id == kid_id).first()
    if not kid:
        raise HTTPException(status_code=404, detail="Kid not found")

    # Check if parent is linked to this kid
    parent_linked = any(parent.id == parent_id for parent in kid.parents)
    if not parent_linked:
        raise HTTPException(
            status_code=403,
            detail="Parent not authorized to create absences for this kid",
        )

    # Check if absence already exists for this date
    existing_absence = (
        db.query(KidAbsence)
        .filter(KidAbsence.kid_id == kid_id, KidAbsence.date == absence_data.date)
        .first()
    )
    if existing_absence:
        raise HTTPException(
            status_code=400, detail="Absence already reported for this date"
        )

    # Create new absence
    try:
        absence = KidAbsence(
            kid_id=kid_id, 
            date=absence_data.date, 
            reason=absence_data.reason,
            note=absence_data.note
        )
        db.add(absence)
        db.commit()
        db.refresh(absence)
        return absence
    except IntegrityError as e:
        db.rollback()
        # Check if it's a unique constraint violation
        if "uq_kid_absence_kid_date" in str(e) or "duplicate key" in str(e).lower():
            raise HTTPException(
                status_code=400, detail="Absence already reported for this date"
            )
        # Re-raise other integrity errors
        raise e
    except Exception as e:
        db.rollback()
        raise e


def get_kid_absences(db: Session, kid_id: int, parent_id: int) -> List[KidAbsence]:
    """
    Get all absences for a kid.

    Args:
        db: Database session
        kid_id: ID of the kid
        parent_id: ID of the parent requesting absences

    Returns:
        List of KidAbsence objects

    Raises:
        HTTPException: If validation fails
    """
    # Verify kid exists and parent is linked to kid
    kid = db.query(Kid).filter(Kid.id == kid_id).first()
    if not kid:
        raise HTTPException(status_code=404, detail="Kid not found")

    # Check if parent is linked to this kid
    parent_linked = any(parent.id == parent_id for parent in kid.parents)
    if not parent_linked:
        raise HTTPException(
            status_code=403,
            detail="Parent not authorized to view absences for this kid",
        )

    return db.query(KidAbsence).filter(KidAbsence.kid_id == kid_id).all()


def get_effective_attendance(db: Session, kid: Kid, target_date: date = None) -> str:
    """
    Get effective attendance for a kid considering absences.

    Args:
        db: Database session
        kid: Kid object
        target_date: Date to check attendance for (defaults to today)

    Returns:
        Effective attendance status
    """
    if target_date is None:
        target_date = date.today()

    # Check if there's an absence for this date
    absence = (
        db.query(KidAbsence)
        .filter(KidAbsence.kid_id == kid.id, KidAbsence.date == target_date)
        .first()
    )

    # If there's an absence, return the absence reason
    # The base attendance field is only used when there's no absence
    if absence:
        return absence.reason.value
    else:
        return kid.attendance.value
