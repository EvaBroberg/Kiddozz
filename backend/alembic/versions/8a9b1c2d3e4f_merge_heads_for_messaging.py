"""merge heads for messaging

Revision ID: 8a9b1c2d3e4f
Revises: d4e8f9a1b2c3, ea03bfe3d53f
Create Date: 2024-11-05 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '8a9b1c2d3e4f'
down_revision = ('d4e8f9a1b2c3', 'ea03bfe3d53f')
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Merge migration - no schema changes
    pass


def downgrade() -> None:
    # Merge migration - no schema changes
    pass





