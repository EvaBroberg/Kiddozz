"""InviteToken model for invitation-based onboarding."""

from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING, Optional

from sqlalchemy import DateTime, ForeignKey, Integer, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from .daycare import Daycare


class InviteToken(Base):
    """InviteToken model for managing invitation tokens."""

    __tablename__ = "invite_tokens"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    token: Mapped[str] = mapped_column(String(255), unique=True, nullable=False, index=True)
    daycare_id: Mapped[str] = mapped_column(
        UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False
    )
    role: Mapped[str] = mapped_column(String(20), nullable=False)  # Role enum value
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    used_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    revoked_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    created_by: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)  # JWT sub
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=func.now(), nullable=False
    )

    # Relationships
    daycare: Mapped["Daycare"] = relationship("Daycare", back_populates="invite_tokens")

    def __repr__(self):
        return f"<InviteToken(id={self.id}, token='{self.token[:8]}...', role='{self.role}', email='{self.email}')>"

