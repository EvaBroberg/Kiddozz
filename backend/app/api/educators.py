from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.educator import Educator
from app.schemas.educators import EducatorOut
from app.utils.daycare_resolver import resolve_daycare_id

router = APIRouter()


@router.get("/educators", response_model=List[EducatorOut])
def list_educators(
    daycare_id: Optional[str] = Query(
        None, description="Daycare scope (required in dev/staging if no JWT)"
    ),
    group: Optional[str] = Query(None, description="Filter by group name"),
    search: Optional[str] = Query(None, description="Search by name"),
    db: Session = Depends(get_db),
):
    # In prod, daycare_id should come from JWT; for now allow a query param in non-prod.
    if settings.environment == "production" and not daycare_id:
        raise HTTPException(status_code=400, detail="daycare_id is required")

    # Resolve daycare_id for local development
    if daycare_id:
        daycare_id = resolve_daycare_id(db, daycare_id)

    q = db.query(Educator)
    if daycare_id:
        q = q.filter(Educator.daycare_id == daycare_id)

    if search:
        q = q.filter(Educator.full_name.ilike(f"%{search}%"))

    educators = q.all()

    if group:
        educators = [e for e in educators if any(g.name == group for g in e.groups)]
    return educators
