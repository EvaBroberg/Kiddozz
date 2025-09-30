"""add_unique_constraint_kid_absences

Revision ID: 8b08245951e7
Revises: 0faed39f155f
Create Date: 2025-09-30 10:50:59.148760

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '8b08245951e7'
down_revision = '0faed39f155f'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Add unique constraint on kid_absences table
    op.create_unique_constraint(
        "uq_kid_absence_kid_date",
        "kid_absences",
        ["kid_id", "date"]
    )


def downgrade() -> None:
    # Remove unique constraint
    op.drop_constraint(
        "uq_kid_absence_kid_date",
        "kid_absences",
        type_="unique"
    )
