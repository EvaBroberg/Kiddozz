from typing import List

from pydantic import BaseModel


class UserBase(BaseModel):
    name: str
    role: str
    groups: List[str] = []


class UserCreate(UserBase):
    pass


class UserUpdate(BaseModel):
    name: str = None
    role: str = None
    groups: List[str] = None


class User(UserBase):
    id: int
    jwt_token: str = None

    class Config:
        from_attributes = True
