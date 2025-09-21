from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.parent import Parent
from app.schemas.parents import ParentOut
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
