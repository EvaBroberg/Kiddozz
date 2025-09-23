from pydantic import BaseModel


class GroupOut(BaseModel):
    id: int
    name: str
    daycare_id: str

    class Config:
        from_attributes = True
