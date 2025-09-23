from datetime import date
from typing import List, Optional, Union

from pydantic import BaseModel

from app.models.kid import AttendanceStatus


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


class KidCreate(KidBase):
    pass


class KidUpdate(BaseModel):
    full_name: Optional[str] = None
    dob: Optional[date] = None
    group_id: Optional[Union[str, int]] = None
    daycare_id: Optional[Union[str, int]] = None
    trusted_adults: Optional[List[TrustedAdultOut]] = None
    attendance: Optional[AttendanceStatus] = None


class KidOut(KidBase):
    id: Union[str, int]

    class Config:
        from_attributes = True
