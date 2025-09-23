from __future__ import annotations

from datetime import date, datetime
from enum import Enum as PyEnum
from typing import TYPE_CHECKING, Any, Dict, List, Optional

from sqlalchemy import JSON, Date, DateTime, Enum, ForeignKey, Integer, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class AttendanceStatus(PyEnum):
    SICK = "sick"
    OUT = "out"
    IN_CARE = "in-care"

if TYPE_CHECKING:
    from .daycare import Daycare
    from .group import Group
    from .parent import Parent


class Kid(Base):
    """Kid model for managing children information."""

    __tablename__ = "kids"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    full_name: Mapped[str] = mapped_column(String(100), nullable=False)
    dob: Mapped[date] = mapped_column(Date, nullable=False)
    daycare_id: Mapped[str] = mapped_column(
        UUID(as_uuid=False),
        ForeignKey("daycares.id", ondelete="CASCADE"),
        nullable=False,
    )
    group_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("groups.id", ondelete="CASCADE"), nullable=False
    )
    trusted_adults: Mapped[Optional[List[Dict[str, Any]]]] = mapped_column(
        JSON, nullable=True
    )
    attendance: Mapped[AttendanceStatus] = mapped_column(
        Enum(AttendanceStatus, name="attendance_status", values_callable=lambda obj: [e.value for e in obj]), 
        nullable=False, 
        server_default="out"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), onupdate=func.now(), nullable=False
    )

    # Relationships
    daycare: Mapped[Daycare] = relationship("Daycare", back_populates="kids")
    group: Mapped[Group] = relationship("Group", back_populates="kids")
    parents: Mapped[List[Parent]] = relationship(
        "Parent", secondary="parent_kids", back_populates="kids"
    )

    def __repr__(self):
        return f"<Kid(id={self.id}, full_name='{self.full_name}', dob='{self.dob}')>"
