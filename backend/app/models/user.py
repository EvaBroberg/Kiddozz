from enum import Enum

from sqlalchemy import Column, DateTime, Integer, String, func

from app.core.database import Base


class UserRole(str, Enum):
    """User role enumeration."""

    PARENT = "parent"
    EDUCATOR = "educator"
    SUPER_EDUCATOR = "super_educator"


class User(Base):
    """User model for authentication and authorization."""

    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False)  # Will store the enum value as string
    created_at = Column(DateTime, default=func.now(), nullable=False)
    jwt_token = Column(String, nullable=True)  # Temporary for testing

    def __repr__(self):
        return f"<User(id={self.id}, name='{self.name}', role='{self.role}')>"
