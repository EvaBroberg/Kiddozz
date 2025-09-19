import json
from enum import Enum
from typing import List

from sqlalchemy import Column, DateTime, Integer, String, Text, func
from sqlalchemy import Enum as SQLEnum

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
    
    # Classes field - PostgreSQL ARRAY, SQLite JSON Text
    _classes_data = Column(Text, default="[]", nullable=False)
    
    @property
    def classes(self) -> List[str]:
        """Get classes as a list, handling both PostgreSQL ARRAY and SQLite JSON."""
        if hasattr(self, '_classes_data') and self._classes_data:
            try:
                return json.loads(self._classes_data)
            except (json.JSONDecodeError, TypeError):
                return []
        return []
    
    @classes.setter
    def classes(self, value: List[str]):
        """Set classes, storing as JSON for SQLite compatibility."""
        if value is None:
            value = []
        self._classes_data = json.dumps(value)

    def __repr__(self):
        return f"<User(id={self.id}, name='{self.name}', role='{self.role}', classes={self.classes})>"
