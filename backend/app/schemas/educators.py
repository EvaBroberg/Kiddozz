from typing import List, Optional, Union

from pydantic import BaseModel


class GroupOut(BaseModel):
    id: Union[str, int]
    name: str

    class Config:
        from_attributes = True


class EducatorOut(BaseModel):
    id: Union[str, int]
    full_name: str
    role: str
    email: Optional[str] = None
    phone_num: Optional[str] = None
    groups: List[GroupOut] = []

    class Config:
        from_attributes = True
