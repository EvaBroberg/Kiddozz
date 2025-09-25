from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.parent import Parent
from app.schemas.kid import KidOut
from app.schemas.parents import ParentOut
from app.services.kid_service import get_kids_for_parent
from app.utils.daycare_resolver import resolve_daycare_id

router = APIRouter()


@router.get("/parents", response_model=List[ParentOut])
def list_parents(
    daycare_id: Optional[str] = Query(
        None, description="Daycare scope (required in prod if no JWT)"
    ),
    search: Optional[str] = Query(None, description="Search by name/email"),
    db: Session = Depends(get_db),
):
    if settings.environment == "production" and not daycare_id:
        raise HTTPException(status_code=400, detail="daycare_id is required")

    # Resolve daycare_id for local development
    if daycare_id:
        daycare_id = resolve_daycare_id(db, daycare_id)

    q = db.query(Parent)
    if daycare_id:
        q = q.filter(Parent.daycare_id == daycare_id)

    if search:
        q = q.filter(
            (Parent.full_name.ilike(f"%{search}%"))
            | (Parent.email.ilike(f"%{search}%"))
        )

    return q.all()


@router.get("/parents/{parent_id}/kids", response_model=List[KidOut])
def get_kids_for_parent_endpoint(
    parent_id: int,
    db: Session = Depends(get_db),
):
    """Get all kids linked to a specific parent."""
    # Verify parent exists
    parent = db.query(Parent).filter(Parent.id == parent_id).first()
    if not parent:
        raise HTTPException(status_code=404, detail="Parent not found")

    # Get kids for this parent
    kids = get_kids_for_parent(db, parent_id)
    return kids
