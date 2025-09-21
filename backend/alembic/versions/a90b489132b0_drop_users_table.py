"""Drop users table

Revision ID: a90b489132b0
Revises: 7290bdad5686
Create Date: 2025-09-21 22:16:59.879634

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'a90b489132b0'
down_revision = '7290bdad5686'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Drop the users table
    op.drop_table("users")


def downgrade() -> None:
    # Recreate the users table with the original schema
    op.create_table(
        "users",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("role", sa.Enum("parent", "educator", "super_educator", name="userrole"), nullable=False),
        sa.Column("jwt_token", sa.String(length=512), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=True),
        sa.Column("groups", sa.JSON(), nullable=False),
        sa.PrimaryKeyConstraint("id")
    )
