from __future__ import annotations

from enum import Enum
from typing import TYPE_CHECKING, List, Optional

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy import Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from .daycare import Daycare
    from .group import Group


class EducatorRole(str, Enum):
    """Educator role enumeration."""

    EDUCATOR = "educator"
    SUPER_EDUCATOR = "super_educator"


class Educator(Base):
    """Educator model for managing educator information."""

    __tablename__ = "educators"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    full_name: Mapped[str] = mapped_column(String(100), nullable=False)
    role: Mapped[str] = mapped_column(
        SQLEnum("educator", "super_educator", name="educatorrole"), nullable=False
    )
    email: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    phone_num: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)
    jwt_token: Mapped[Optional[str]] = mapped_column(String(512), nullable=True)
    daycare_id: Mapped[str] = mapped_column(
        UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False
    )
    created_at: Mapped[DateTime] = mapped_column(DateTime, default=func.now(), nullable=False)
    updated_at: Mapped[DateTime] = mapped_column(
        DateTime, default=func.now(), onupdate=func.now(), nullable=False
    )

    # Relationships
    daycare: Mapped[Daycare] = relationship("Daycare", back_populates="educators")
    groups: Mapped[List[Group]] = relationship(
        "Group", secondary="educator_groups", back_populates="educators"
    )

    def __repr__(self):
        return f"<Educator(id={self.id}, full_name='{self.full_name}', role='{self.role}', email='{self.email}')>"
