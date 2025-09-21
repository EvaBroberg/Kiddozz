from datetime import date
from typing import List, Optional

from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.models.kid import Kid
from app.models.parent import Parent


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
