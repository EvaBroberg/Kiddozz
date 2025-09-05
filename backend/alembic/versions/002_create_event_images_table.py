"""Create event_images table

Revision ID: 002
Revises: 001
Create Date: 2024-01-15 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '002'
down_revision = '001'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create event_images table
    op.create_table('event_images',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('event_id', sa.Integer(), nullable=True),
        sa.Column('file_name', sa.String(), nullable=False),
        sa.Column('s3_key', sa.String(), nullable=False),
        sa.Column('status', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['event_id'], ['events.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('s3_key')
    )
    op.create_index(op.f('ix_event_images_id'), 'event_images', ['id'], unique=False)


def downgrade() -> None:
    # Drop event_images table
    op.drop_index(op.f('ix_event_images_id'), table_name='event_images')
    op.drop_table('event_images')
