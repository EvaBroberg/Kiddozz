"""Merge heads c1d78ecdf23f and 44326a409bb9

Revision ID: b4318fbb107a
Revises: 44326a409bb9
Create Date: 2025-09-19 20:59:18.474745

This is a merge migration to consolidate the migration history after
removing the redundant c1d78ecdf23f migration. No schema changes are needed
as the groups column functionality is already properly implemented in 44326a409bb9.
"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'b4318fbb107a'
down_revision = '44326a409bb9'
branch_labels = None
depends_on = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
