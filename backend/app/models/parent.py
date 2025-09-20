from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING, List

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from .daycare import Daycare
    from .kid import Kid


class Parent(Base):
    """Parent model for managing parent information."""

    __tablename__ = "parents"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    full_name: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    phone_num: Mapped[str] = mapped_column(String(20), nullable=False)
    daycare_id: Mapped[str] = mapped_column(
        UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), onupdate=func.now(), nullable=False
    )

    # Relationships
    daycare: Mapped[Daycare] = relationship("Daycare", back_populates="parents")
    kids: Mapped[List[Kid]] = relationship(
        "Kid", secondary="parent_kids", back_populates="parents"
    )

    def __repr__(self):
        return f"<Parent(id={self.id}, full_name='{self.full_name}', email='{self.email}')>"
