from __future__ import annotations

import uuid
from datetime import date, datetime
from enum import Enum as PyEnum
from typing import TYPE_CHECKING, Any, Dict, List, Optional

from sqlalchemy import (
    JSON,
    Date,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class AttendanceStatus(PyEnum):
    SICK = "sick"
    OUT = "out"
    IN_CARE = "in-care"
    HOLIDAY = "holiday"


class AbsenceReason(PyEnum):
    SICK = "sick"
    HOLIDAY = "holiday"


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
        Enum(
            AttendanceStatus,
            name="attendance_status",
            values_callable=lambda obj: [e.value for e in obj],
        ),
        nullable=False,
        server_default="out",
    )
    allergies: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    need_to_know: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
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
    absences: Mapped[List[KidAbsence]] = relationship(
        "KidAbsence", back_populates="kid", cascade="all, delete-orphan"
    )

    def __repr__(self):
        return f"<Kid(id={self.id}, full_name='{self.full_name}', dob='{self.dob}')>"


class KidAbsence(Base):
    """Kid absence model for tracking daily absences."""

    __tablename__ = "kid_absences"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=False), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    kid_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("kids.id", ondelete="CASCADE"), nullable=False
    )
    date: Mapped[date] = mapped_column(Date, nullable=False)
    reason: Mapped[AbsenceReason] = mapped_column(
        Enum(
            AbsenceReason,
            name="absence_reason",
            values_callable=lambda obj: [e.value for e in obj],
        ),
        nullable=False,
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), onupdate=func.now(), nullable=False
    )

    # Relationships
    kid: Mapped[Kid] = relationship("Kid", back_populates="absences")

    __table_args__ = (
        UniqueConstraint("kid_id", "date", name="uq_kid_absences_kid_date"),
    )

    def __repr__(self):
        return f"<KidAbsence(id={self.id}, kid_id={self.kid_id}, date='{self.date}', reason='{self.reason}')>"
