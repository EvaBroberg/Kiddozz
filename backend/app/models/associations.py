from sqlalchemy import Column, ForeignKey, Integer, Table

from app.core.database import Base

# Association table for many-to-many relationship between educators and groups
educator_groups = Table(
    "educator_groups",
    Base.metadata,
    Column("educator_id", Integer, ForeignKey("educators.id", ondelete="CASCADE")),
    Column("group_id", Integer, ForeignKey("groups.id", ondelete="CASCADE")),
)

# Association table for many-to-many relationship between parents and kids
parent_kids = Table(
    "parent_kids",
    Base.metadata,
    Column("parent_id", Integer, ForeignKey("parents.id", ondelete="CASCADE")),
    Column("kid_id", Integer, ForeignKey("kids.id", ondelete="CASCADE")),
)
