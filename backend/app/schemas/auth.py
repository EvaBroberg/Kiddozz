from typing import Optional

from pydantic import BaseModel


class DevLoginRequest(BaseModel):
    educator_id: Optional[str] = None
    parent_id: Optional[str] = None


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
