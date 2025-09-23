from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.models.kid import AttendanceStatus, Kid
from app.schemas.kid import KidOut
from app.utils.daycare_resolver import resolve_daycare_id

router = APIRouter()


class AttendanceUpdateRequest(BaseModel):
    attendance: str


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


@router.patch("/kids/{kid_id}/attendance")
def update_attendance(
    kid_id: int,
    request: AttendanceUpdateRequest,
    db: Session = Depends(get_db),
):
    # Validate attendance value
    try:
        attendance_status = AttendanceStatus(request.attendance)
    except ValueError:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid attendance status: {request.attendance}. Must be one of: {[status.value for status in AttendanceStatus]}",
        )

    # Find the kid
    kid = db.query(Kid).filter(Kid.id == kid_id).first()
    if not kid:
        raise HTTPException(status_code=404, detail="Kid not found")

    # Update attendance
    kid.attendance = attendance_status
    db.commit()
    db.refresh(kid)

    return {
        "message": "Attendance updated successfully",
        "attendance": kid.attendance.value,
    }
