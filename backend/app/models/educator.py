from enum import Enum
from typing import Optional

from sqlalchemy import DateTime, Integer, String, func
from sqlalchemy import Enum as SQLEnum
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


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
    group: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    email: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    phone_num: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)
    jwt_token: Mapped[Optional[str]] = mapped_column(String(512), nullable=True)
    created: Mapped[DateTime] = mapped_column(DateTime, default=func.now(), nullable=False)
    updated: Mapped[DateTime] = mapped_column(
        DateTime, default=func.now(), onupdate=func.now(), nullable=False
    )

    def __repr__(self):
        return f"<Educator(id={self.id}, full_name='{self.full_name}', role='{self.role}', email='{self.email}')>"
