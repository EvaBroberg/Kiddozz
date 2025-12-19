"""add_messaging_tables

Revision ID: 7b3f1700ae94
Revises: ea03bfe3d53f
Create Date: 2024-11-05 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '7b3f1700ae94'
down_revision = '8a9b1c2d3e4f'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create enum types
    op.execute("CREATE TYPE conversationtype AS ENUM ('direct', 'group')")
    op.execute("CREATE TYPE usertype AS ENUM ('parent', 'educator')")

    # Create conversations table
    op.create_table(
        'conversations',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('type', sa.Enum('direct', 'group', name='conversationtype'), nullable=False),
        sa.Column('daycare_id', sa.Text(), nullable=False),
        sa.Column('title', sa.Text(), nullable=True),
        sa.Column('direct_key_hash', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('NOW()'), nullable=False),
    )
    op.create_index('ix_conversations_id', 'conversations', ['id'])
    op.create_index('ix_conversations_daycare_id', 'conversations', ['daycare_id'])
    op.create_index('ix_conversations_type', 'conversations', ['type'])
    op.create_index('ix_conversations_direct_key_hash', 'conversations', ['direct_key_hash'])
    op.create_index('ix_conversations_daycare_type_hash', 'conversations', ['daycare_id', 'type', 'direct_key_hash'])

    # Unique constraint for direct conversations (partial index)
    op.execute("""
        CREATE UNIQUE INDEX uq_direct_conversation 
        ON conversations (daycare_id, type, direct_key_hash) 
        WHERE type = 'direct' AND direct_key_hash IS NOT NULL
    """)

    # Create conversation_participants table
    op.create_table(
        'conversation_participants',
        sa.Column('conversation_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_type', sa.Enum('parent', 'educator', name='usertype'), nullable=False),
        sa.Column('user_id', sa.Text(), nullable=False),
        sa.ForeignKeyConstraint(['conversation_id'], ['conversations.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('conversation_id', 'user_type', 'user_id')
    )
    op.create_index('ix_conversation_participants_user_type', 'conversation_participants', ['user_type'])
    op.create_index('ix_conversation_participants_user_id', 'conversation_participants', ['user_id'])
    op.create_index('ix_conversation_participants_user', 'conversation_participants', ['user_type', 'user_id'])

    # Create messages table
    op.create_table(
        'messages',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text('uuid_generate_v4()')),
        sa.Column('conversation_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('sender_type', sa.Enum('parent', 'educator', name='usertype'), nullable=False),
        sa.Column('sender_id', sa.Text(), nullable=False),
        sa.Column('body', sa.Text(), nullable=True),
        sa.Column('image_url', sa.Text(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('NOW()'), nullable=False),
        sa.ForeignKeyConstraint(['conversation_id'], ['conversations.id'], ondelete='CASCADE'),
    )
    op.create_index('ix_messages_id', 'messages', ['id'])
    op.create_index('ix_messages_conversation_id', 'messages', ['conversation_id'])
    op.create_index('ix_messages_sender_type', 'messages', ['sender_type'])
    op.create_index('ix_messages_created_at', 'messages', ['created_at'])
    op.create_index('ix_messages_conversation_created', 'messages', ['conversation_id', 'created_at'])

    # Create push_tokens table
    op.create_table(
        'push_tokens',
        sa.Column('user_type', sa.Enum('parent', 'educator', name='usertype'), nullable=False),
        sa.Column('user_id', sa.Text(), nullable=False),
        sa.Column('token', sa.Text(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('NOW()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('NOW()'), nullable=False),
        sa.PrimaryKeyConstraint('user_type', 'user_id', 'token')
    )
    op.create_index('ix_push_tokens_user', 'push_tokens', ['user_type', 'user_id'])


def downgrade() -> None:
    op.drop_table('push_tokens')
    op.drop_table('messages')
    op.drop_table('conversation_participants')
    op.drop_index('uq_direct_conversation', table_name='conversations')
    op.drop_table('conversations')
    op.execute('DROP TYPE usertype')
    op.execute('DROP TYPE conversationtype')

