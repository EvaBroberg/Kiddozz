from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING, List

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from .daycare import Daycare
    from .educator import Educator
    from .kid import Kid


class Group(Base):
    """Group model for managing daycare groups."""

    __tablename__ = "groups"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
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
    daycare: Mapped[Daycare] = relationship("Daycare", back_populates="groups")
    educators: Mapped[List[Educator]] = relationship(
        "Educator", secondary="educator_groups", back_populates="groups"
    )
    kids: Mapped[List[Kid]] = relationship("Kid", back_populates="group")

    def __repr__(self):
        return f"<Group(id={self.id}, name='{self.name}', daycare_id='{self.daycare_id}')>"
