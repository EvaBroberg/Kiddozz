"""add attendance column to kids table

Revision ID: e01caaa97916
Revises: a90b489132b0
Create Date: 2025-09-23 21:09:29.747300

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


# revision identifiers, used by Alembic.
revision = 'e01caaa97916'
down_revision = 'a90b489132b0'
branch_labels = None
depends_on = None


def upgrade() -> None:
    attendance_enum = postgresql.ENUM('sick', 'out', 'in-care', name='attendance_status')
    attendance_enum.create(op.get_bind(), checkfirst=True)
    op.add_column('kids', sa.Column('attendance', sa.Enum('sick', 'out', 'in-care', name='attendance_status'), nullable=False, server_default='out'))


def downgrade() -> None:
    op.drop_column('kids', 'attendance')
    attendance_enum = postgresql.ENUM('sick', 'out', 'in-care', name='attendance_status')
    attendance_enum.drop(op.get_bind(), checkfirst=True)
