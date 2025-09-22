from typing import List, Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.models.kid import Kid
from app.schemas.kid import KidOut
from app.utils.daycare_resolver import resolve_daycare_id

router = APIRouter()


@router.get("/kids", response_model=List[KidOut])
def list_kids(
    daycare_id: str = Query(...),
    group_id: Optional[str] = Query(None),
    db: Session = Depends(get_db),
):
    # Resolve daycare_id for local/test development
    daycare_id = resolve_daycare_id(db, daycare_id)

    query = db.query(Kid).filter(Kid.daycare_id == daycare_id)
    if group_id:
        query = query.filter(Kid.group_id == group_id)
    return query.all()
