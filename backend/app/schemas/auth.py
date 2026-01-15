from typing import Optional

from pydantic import BaseModel


class AcceptInviteRequest(BaseModel):
    token: str
    name: str  # Required for both Educator and Parent
    phone_num: Optional[str] = None  # Required for Parent, optional for Educator


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class AcceptInviteResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    role: str
    daycare_id: str
