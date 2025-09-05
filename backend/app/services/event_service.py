from sqlalchemy.orm import Session
from sqlalchemy import and_, or_
from app.models.event import Event, EventImage
from app.models.schemas import EventCreate, EventUpdate, EventImageCreate
from app.services.s3_service import s3_service
from typing import List, Optional
from datetime import datetime


class EventService:
    def __init__(self, db: Session):
        self.db = db
    
    def create_event(self, event_data: EventCreate) -> Event:
        """Create a new event"""
        db_event = Event(
            title=event_data.title,
            description=event_data.description,
            date=event_data.date,
            start_time=event_data.start_time,
            location=event_data.location,
            is_past=event_data.is_past
        )
        self.db.add(db_event)
        self.db.commit()
        self.db.refresh(db_event)
        return db_event
    
    def get_event(self, event_id: int) -> Optional[Event]:
        """Get event by ID"""
        return self.db.query(Event).filter(Event.id == event_id).first()
    
    def get_events(
        self, 
        skip: int = 0, 
        limit: int = 100, 
        upcoming_only: bool = False,
        past_only: bool = False
    ) -> List[Event]:
        """Get events with optional filtering"""
        query = self.db.query(Event)
        
        if upcoming_only:
            query = query.filter(Event.is_past == False)
        elif past_only:
            query = query.filter(Event.is_past == True)
        
        return query.offset(skip).limit(limit).all()
    
    def update_event(self, event_id: int, event_data: EventUpdate) -> Optional[Event]:
        """Update an event"""
        db_event = self.get_event(event_id)
        if not db_event:
            return None
        
        update_data = event_data.dict(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_event, field, value)
        
        self.db.commit()
        self.db.refresh(db_event)
        return db_event
    
    def delete_event(self, event_id: int) -> bool:
        """Delete an event and its images"""
        db_event = self.get_event(event_id)
        if not db_event:
            return False
        
        # Delete associated images from S3
        for image in db_event.images:
            if image.s3_key:
                s3_service.delete_object(image.s3_key)
        
        self.db.delete(db_event)
        self.db.commit()
        return True
    
    def add_image_to_event(
        self, 
        event_id: int, 
        image_data: EventImageCreate,
        s3_key: str
    ) -> Optional[EventImage]:
        """Add an image to an event"""
        db_event = self.get_event(event_id)
        if not db_event:
            return None
        
        # Generate S3 URL
        image_url = s3_service.get_object_url(s3_key)
        
        db_image = EventImage(
            event_id=event_id,
            image_url=image_url,
            s3_key=s3_key,
            file_name=image_data.file_name,
            file_size=image_data.file_size,
            mime_type=image_data.mime_type,
            is_uploaded=True
        )
        
        self.db.add(db_image)
        self.db.commit()
        self.db.refresh(db_image)
        return db_image
    
    def get_event_images(self, event_id: int) -> List[EventImage]:
        """Get all images for an event"""
        return self.db.query(EventImage).filter(EventImage.event_id == event_id).all()
    
    def delete_event_image(self, image_id: int) -> bool:
        """Delete an event image"""
        db_image = self.db.query(EventImage).filter(EventImage.id == image_id).first()
        if not db_image:
            return False
        
        # Delete from S3
        if db_image.s3_key:
            s3_service.delete_object(db_image.s3_key)
        
        self.db.delete(db_image)
        self.db.commit()
        return True
    
    def generate_presigned_upload_url(
        self, 
        event_id: int, 
        file_name: str, 
        file_size: int, 
        mime_type: str
    ) -> dict:
        """Generate pre-signed URL for uploading image to event"""
        # Verify event exists
        if not self.get_event(event_id):
            raise ValueError("Event not found")
        
        return s3_service.generate_presigned_upload_url(
            file_name=file_name,
            mime_type=mime_type,
            event_id=event_id
        )
    
    def confirm_image_upload(self, event_id: int, s3_key: str, image_data: EventImageCreate) -> EventImage:
        """Confirm image upload and create database record"""
        return self.add_image_to_event(event_id, image_data, s3_key)
