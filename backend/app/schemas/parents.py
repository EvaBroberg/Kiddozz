from typing import Optional, Union

from pydantic import BaseModel


class ParentOut(BaseModel):
    id: Union[str, int]
    full_name: str
    email: Optional[str] = None
    phone_num: Optional[str] = None

    class Config:
        from_attributes = True
