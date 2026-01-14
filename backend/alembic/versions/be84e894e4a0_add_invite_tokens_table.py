"""add_invite_tokens_table

Revision ID: be84e894e4a0
Revises: 7b3f1700ae94
Create Date: 2026-01-13 11:22:31.453878

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import text


# revision identifiers, used by Alembic.
revision = 'be84e894e4a0'
down_revision = '7b3f1700ae94'
branch_labels = None
depends_on = None


def upgrade() -> None:
    """Create invite_tokens table for invitation-based onboarding."""
    bind = op.get_bind()
    dialect = bind.dialect.name
    
    # Detect database dialect for proper timestamp defaults
    if dialect == "sqlite":
        created_default = sa.text("CURRENT_TIMESTAMP")
        now_func = sa.text("datetime('now')")
    else:
        created_default = sa.text("NOW()")
        now_func = sa.text("NOW()")
    
    # Create invite_tokens table
    # Use same UUID type as daycares table (UUID as string)
    if dialect == "postgresql":
        from sqlalchemy.dialects.postgresql import UUID
        daycare_id_type = UUID(as_uuid=False)
    else:
        # SQLite: use String for UUID
        daycare_id_type = sa.String(length=36)
    
    op.create_table(
        'invite_tokens',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('token', sa.String(length=255), nullable=False),
        sa.Column('daycare_id', daycare_id_type, nullable=False),
        sa.Column('role', sa.String(length=20), nullable=False),  # Role enum value
        sa.Column('email', sa.String(length=255), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('used_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('revoked_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('created_by', sa.String(length=255), nullable=True),  # JWT sub
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=created_default, nullable=False),
        sa.PrimaryKeyConstraint('id')
    )
    
    # Create unique constraint on token
    op.create_index('ix_invite_tokens_token', 'invite_tokens', ['token'], unique=True)
    
    # Create index on expires_at for efficient expiration queries
    op.create_index('ix_invite_tokens_expires_at', 'invite_tokens', ['expires_at'])
    
    # Create foreign key to daycares table
    # Note: SQLite doesn't enforce foreign keys by default, but we add it for PostgreSQL
    if dialect == "postgresql":
        op.create_foreign_key(
            'fk_invite_tokens_daycare_id',
            'invite_tokens',
            'daycares',
            ['daycare_id'],
            ['id'],
            ondelete='CASCADE'
        )
    
    # Add check constraint for role (if supported)
    if dialect == "postgresql":
        op.execute(text("""
            ALTER TABLE invite_tokens 
            ADD CONSTRAINT chk_invite_tokens_role 
            CHECK (role IN ('parent', 'educator', 'super_educator'))
        """))


def downgrade() -> None:
    """Drop invite_tokens table."""
    op.drop_table('invite_tokens')
