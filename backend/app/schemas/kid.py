from datetime import date
from typing import List, Optional, Union

from pydantic import BaseModel


class TrustedAdultOut(BaseModel):
    name: str
    email: Optional[str] = None
    phone_num: Optional[str] = None
    address: Optional[str] = None


class KidOut(BaseModel):
    id: Union[str, int]
    full_name: str
    dob: date
    group_id: Union[str, int]
    daycare_id: Union[str, int]
    trusted_adults: List[TrustedAdultOut] = []

    class Config:
        from_attributes = True
