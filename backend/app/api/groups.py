from typing import List

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.models.group import Group
from app.schemas.groups import GroupOut
from app.utils.daycare_resolver import resolve_daycare_id

router = APIRouter()


@router.get("/groups", response_model=List[GroupOut])
def list_groups(
    daycare_id: str = Query(..., description="Daycare scope"),
    db: Session = Depends(get_db),
):
    """List groups for a specific daycare."""
    # Resolve daycare_id for local development
    daycare_id = resolve_daycare_id(db, daycare_id)

    groups = db.query(Group).filter(Group.daycare_id == daycare_id).all()
    return groups
