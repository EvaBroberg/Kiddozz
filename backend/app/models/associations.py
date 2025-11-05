from sqlalchemy import Column, ForeignKey, Integer, Table, UniqueConstraint

from app.core.database import Base

# Association table for many-to-many relationship between educators and groups
# Constraints:
# - NOT NULL on both columns (enforced by migration)
# - UNIQUE(educator_id, group_id) to prevent duplicates (enforced by migration)
# - Constraint trigger ensures every educator has at least one group at commit time
educator_groups = Table(
    "educator_groups",
    Base.metadata,
    Column(
        "educator_id",
        Integer,
        ForeignKey("educators.id", ondelete="CASCADE"),
        nullable=False,
    ),
    Column(
        "group_id", Integer, ForeignKey("groups.id", ondelete="CASCADE"), nullable=False
    ),
    UniqueConstraint("educator_id", "group_id", name="uq_educator_groups_pair"),
)

# Association table for many-to-many relationship between parents and kids
parent_kids = Table(
    "parent_kids",
    Base.metadata,
    Column("parent_id", Integer, ForeignKey("parents.id", ondelete="CASCADE")),
    Column("kid_id", Integer, ForeignKey("kids.id", ondelete="CASCADE")),
)
