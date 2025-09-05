from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List
import os
from app.core.database import get_db
from app.models.schemas import (
    Event as EventSchema, EventCreate, EventUpdate, EventWithImages,
    PresignedUrlRequest, PresignedUrlResponse,
    EventImage as EventImageSchema, EventImageCreate
)
from app.models.event import Event, EventImage
from app.services.event_service import EventService
from app.services.s3_service import create_presigned_url

router = APIRouter()


@router.post("/", response_model=EventSchema, status_code=status.HTTP_201_CREATED)
def create_event(
    event: EventCreate,
    db: Session = Depends(get_db)
):
    """Create a new event"""
    event_service = EventService(db)
    return event_service.create_event(event)


@router.get("/", response_model=List[EventWithImages])
def get_events(
    skip: int = 0,
    limit: int = 100,
    upcoming_only: bool = False,
    past_only: bool = False,
    db: Session = Depends(get_db)
):
    """Get all events with optional filtering"""
    event_service = EventService(db)
    return event_service.get_events(
        skip=skip,
        limit=limit,
        upcoming_only=upcoming_only,
        past_only=past_only
    )


@router.get("/upcoming", response_model=List[EventWithImages])
def get_upcoming_events(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db)
):
    """Get upcoming events only"""
    event_service = EventService(db)
    return event_service.get_events(
        skip=skip,
        limit=limit,
        upcoming_only=True
    )


@router.get("/past", response_model=List[EventWithImages])
def get_past_events(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db)
):
    """Get past events only"""
    event_service = EventService(db)
    return event_service.get_events(
        skip=skip,
        limit=limit,
        past_only=True
    )


@router.get("/{event_id}", response_model=EventWithImages)
def get_event(
    event_id: int,
    db: Session = Depends(get_db)
):
    """Get a specific event by ID"""
    event_service = EventService(db)
    event = event_service.get_event(event_id)
    if not event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )
    return event


@router.put("/{event_id}", response_model=EventSchema)
def update_event(
    event_id: int,
    event: EventUpdate,
    db: Session = Depends(get_db)
):
    """Update an event"""
    event_service = EventService(db)
    updated_event = event_service.update_event(event_id, event)
    if not updated_event:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )
    return updated_event


@router.delete("/{event_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_event(
    event_id: int,
    db: Session = Depends(get_db)
):
    """Delete an event"""
    event_service = EventService(db)
    if not event_service.delete_event(event_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )


@router.post("/{event_id}/images/presigned-url", response_model=PresignedUrlResponse)
def generate_presigned_upload_url(
    event_id: int,
    request: PresignedUrlRequest,
    db: Session = Depends(get_db)
):
    """Generate pre-signed URL for uploading image to event"""
    event_service = EventService(db)
    try:
        result = event_service.generate_presigned_upload_url(
            event_id=event_id,
            file_name=request.file_name,
            file_size=request.file_size,
            mime_type=request.mime_type
        )
        return PresignedUrlResponse(**result)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating pre-signed URL: {str(e)}"
        )


@router.post("/{event_id}/images/confirm", response_model=EventImageSchema)
def confirm_image_upload(
    event_id: int,
    s3_key: str,
    image_data: EventImageCreate,
    db: Session = Depends(get_db)
):
    """Confirm image upload and create database record"""
    event_service = EventService(db)
    try:
        return event_service.confirm_image_upload(event_id, s3_key, image_data)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e)
        )


@router.get("/{event_id}/images", response_model=List[EventImageSchema])
def get_event_images(
    event_id: int,
    db: Session = Depends(get_db)
):
    """Get all images for an event"""
    event_service = EventService(db)
    if not event_service.get_event(event_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Event not found"
        )
    return event_service.get_event_images(event_id)


@router.delete("/images/{image_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_event_image(
    image_id: int,
    db: Session = Depends(get_db)
):
    """Delete an event image"""
    event_service = EventService(db)
    if not event_service.delete_event_image(image_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Image not found"
        )


@router.post("/{event_id}/images")
def get_upload_url(event_id: int, filename: str, db: Session = Depends(get_db)):
    """Get presigned URL for uploading image to event"""
    # Ensure event exists
    event = db.query(Event).filter(Event.id == event_id).first()
    if not event:
        return {"error": "Event not found"}

    # Create DB record
    key = f"daycares/demo/events/{event_id}/images/{filename}"
    image = EventImage(event_id=event_id, file_name=filename, s3_key=key)
    db.add(image)
    db.commit()
    db.refresh(image)

    # Generate presigned URL (use dummy URL for testing if AWS not configured)
    try:
        url = create_presigned_url(os.getenv("AWS_BUCKET_NAME"), key)
    except:
        # Fallback to dummy URL for testing
        url = f"https://s3.amazonaws.com/bucket/{key}"
    
    return {"upload_url": url, "image_id": image.id}
