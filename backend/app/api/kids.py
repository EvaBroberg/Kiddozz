from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import get_current_user
from app.models.kid import AttendanceStatus, Kid
from app.models.parent import Parent
from app.schemas.kid import KidOut, KidUpdate
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


@router.patch("/kids/{kid_id}", response_model=KidOut)
def update_kid(
    kid_id: int,
    kid_update: KidUpdate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Update a kid's information with parent authorization for sensitive fields."""
    # Find the kid
    kid = db.query(Kid).filter(Kid.id == kid_id).first()
    if not kid:
        raise HTTPException(status_code=404, detail="Kid not found")

    user_role = current_user.get("role")
    user_id = current_user.get("sub")  # JWT sub field contains the user ID

    # Check if user is a parent and if they're linked to this kid
    is_linked_parent = False
    if user_role == "parent" and user_id:
        parent = db.query(Parent).filter(Parent.id == int(user_id)).first()
        if parent and kid in parent.kids:
            is_linked_parent = True

    # Update basic fields (allowed for all authenticated users)
    if kid_update.full_name is not None:
        kid.full_name = kid_update.full_name
    if kid_update.dob is not None:
        kid.dob = kid_update.dob
    if kid_update.group_id is not None:
        kid.group_id = kid_update.group_id
    if kid_update.daycare_id is not None:
        kid.daycare_id = kid_update.daycare_id
    if kid_update.trusted_adults is not None:
        kid.trusted_adults = kid_update.trusted_adults
    if kid_update.attendance is not None:
        kid.attendance = kid_update.attendance

    # Update sensitive fields only if user is a linked parent
    if user_role == "parent":
        if not is_linked_parent:
            raise HTTPException(
                status_code=403,
                detail="Only parents linked to this kid can update allergies and need_to_know fields",
            )
        # Parent is linked, allow updates to sensitive fields
        if kid_update.allergies is not None:
            kid.allergies = kid_update.allergies
        if kid_update.need_to_know is not None:
            kid.need_to_know = kid_update.need_to_know
    else:
        # Non-parent users (educators, super_educators) cannot update sensitive fields
        # These fields will be ignored even if provided in the request
        pass

    db.commit()
    db.refresh(kid)

    return kid
