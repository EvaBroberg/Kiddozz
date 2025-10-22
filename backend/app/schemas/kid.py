from datetime import date, datetime
from typing import List, Optional, Union

from pydantic import BaseModel

from app.models.kid import AbsenceReason, AttendanceStatus
from app.schemas.parents import ParentOut


class TrustedAdultOut(BaseModel):
    name: str
    email: Optional[str] = None
    phone_num: Optional[str] = None
    address: Optional[str] = None


class KidBase(BaseModel):
    full_name: str
    dob: date
    group_id: Union[str, int]
    daycare_id: Union[str, int]
    trusted_adults: Optional[List[TrustedAdultOut]] = []
    attendance: AttendanceStatus = AttendanceStatus.OUT
    allergies: Optional[str] = None
    need_to_know: Optional[str] = None


class KidCreate(KidBase):
    pass


class KidUpdate(BaseModel):
    full_name: Optional[str] = None
    dob: Optional[date] = None
    group_id: Optional[Union[str, int]] = None
    daycare_id: Optional[Union[str, int]] = None
    trusted_adults: Optional[List[TrustedAdultOut]] = None
    attendance: Optional[AttendanceStatus] = None
    allergies: Optional[str] = None
    need_to_know: Optional[str] = None


class KidOut(KidBase):
    id: Union[str, int]
    parents: List[ParentOut] = []

    class Config:
        from_attributes = True


class KidAbsenceCreate(BaseModel):
    date: date
    reason: AbsenceReason
    note: Optional[str] = None


class KidAbsenceOut(BaseModel):
    id: str
    kid_id: int
    date: date
    reason: AbsenceReason
    note: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
