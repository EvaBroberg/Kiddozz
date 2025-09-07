from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


# Event Schemas
class EventBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    date: datetime
    start_time: Optional[str] = Field(
        None, pattern=r"^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"
    )
    location: Optional[str] = Field(None, max_length=255)
    is_past: bool = False


class EventCreate(EventBase):
    pass


class EventUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=255)
    description: Optional[str] = None
    date: Optional[datetime] = None
    start_time: Optional[str] = Field(
        None, pattern=r"^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"
    )
    location: Optional[str] = Field(None, max_length=255)
    is_past: Optional[bool] = None


class Event(EventBase):
    id: int
    created_at: datetime
    updated_at: Optional[datetime] = None
    images: List["EventImage"] = []

    class Config:
        from_attributes = True


# Event Image Schemas
class EventImageBase(BaseModel):
    file_name: str = Field(..., min_length=1, max_length=255)
    file_size: Optional[int] = None
    mime_type: Optional[str] = Field(None, max_length=100)


class EventImageCreate(EventImageBase):
    pass


class EventImage(EventImageBase):
    id: int
    event_id: int
    image_url: str
    s3_key: Optional[str] = None
    is_uploaded: bool = False
    created_at: datetime

    class Config:
        from_attributes = True


# S3 Pre-signed URL Schemas
class PresignedUrlRequest(BaseModel):
    file_name: str = Field(..., min_length=1, max_length=255)
    file_size: int = Field(..., gt=0)
    mime_type: str = Field(..., min_length=1, max_length=100)
    event_id: int


class PresignedUrlResponse(BaseModel):
    upload_url: str
    s3_key: str
    expires_in: int


# Response Schemas
class EventWithImages(Event):
    images: List[EventImage] = []


# Update forward references
Event.model_rebuild()
EventImage.model_rebuild()
