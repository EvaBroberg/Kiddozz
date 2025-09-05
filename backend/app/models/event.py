from sqlalchemy import Column, Integer, String, DateTime, Text, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.core.database import Base


class Event(Base):
    __tablename__ = "events"
    
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text)
    date = Column(DateTime, nullable=False)
    start_time = Column(String(10))  # HH:MM format
    location = Column(String(255))
    is_past = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    # Relationship with images
    images = relationship("EventImage", back_populates="event", cascade="all, delete-orphan")


class EventImage(Base):
    __tablename__ = "event_images"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.id", ondelete="CASCADE"))
    file_name = Column(String, nullable=False)
    s3_key = Column(String, unique=True, nullable=False)
    status = Column(String, default="pending")  # pending | approved
    created_at = Column(DateTime, server_default=func.now())

    event = relationship("Event", back_populates="images")
