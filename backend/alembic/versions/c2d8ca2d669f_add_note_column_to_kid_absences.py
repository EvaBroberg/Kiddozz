"""add note column to kid_absences

Revision ID: c2d8ca2d669f
Revises: 8b08245951e7
Create Date: 2025-10-22 10:25:01.963014

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'c2d8ca2d669f'
down_revision = '8b08245951e7'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column('kid_absences', sa.Column('note', sa.String(length=500), nullable=True))


def downgrade() -> None:
    op.drop_column('kid_absences', 'note')
