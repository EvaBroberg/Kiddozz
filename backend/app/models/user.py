from enum import Enum
from typing import List

from sqlalchemy import JSON, Column, DateTime, Integer, String, func
from sqlalchemy import Enum as SQLEnum
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class UserRole(str, Enum):
    """User role enumeration."""

    PARENT = "parent"
    EDUCATOR = "educator"
    SUPER_EDUCATOR = "super_educator"


class User(Base):
    """User model for authentication and authorization."""

    __tablename__ = "users"

    id = Column(Integer, primary_key=True)
    name = Column(String(100), nullable=False)
    role = Column(
        SQLEnum("parent", "educator", "super_educator", name="userrole"), nullable=False
    )
    jwt_token = Column(String(512), nullable=True)
    created_at = Column(DateTime, default=func.now())
    
    # Groups field - JSON type for both PostgreSQL and SQLite
    groups: Mapped[List[str]] = mapped_column(JSON, default=list, nullable=False)

    def __repr__(self):
        return f"<User(id={self.id}, name='{self.name}', role='{self.role}', groups={self.groups})>"
