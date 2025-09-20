from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING, List
from uuid import uuid4

from sqlalchemy import DateTime, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from .educator import Educator
    from .group import Group
    from .kid import Kid
    from .parent import Parent


class Daycare(Base):
    """Daycare model for managing multiple daycare centers."""

    __tablename__ = "daycares"

    id: Mapped[str] = mapped_column(
        UUID(as_uuid=False), primary_key=True, default=lambda: str(uuid4())
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, default=func.now(), onupdate=func.now(), nullable=False
    )

    # Relationships
    groups: Mapped[List["Group"]] = relationship(
        "Group", back_populates="daycare", cascade="all, delete-orphan"
    )
    educators: Mapped[List["Educator"]] = relationship(
        "Educator", back_populates="daycare", cascade="all, delete-orphan"
    )
    parents: Mapped[List["Parent"]] = relationship(
        "Parent", back_populates="daycare", cascade="all, delete-orphan"
    )
    kids: Mapped[List["Kid"]] = relationship(
        "Kid", back_populates="daycare", cascade="all, delete-orphan"
    )

    def __repr__(self):
        return f"<Daycare(id={self.id}, name='{self.name}')>"
