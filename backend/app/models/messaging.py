"""Messaging models for conversations, participants, messages, and push tokens."""

import enum
from datetime import datetime
from typing import List, Optional
from uuid import UUID, uuid4

from sqlalchemy import (
    CheckConstraint,
    Column,
    DateTime,
    Enum,
    ForeignKey,
    Index,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import UUID as PostgresUUID
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.core.database import Base


class ConversationType(str, enum.Enum):
    """Conversation type enum.
    
    Values must match PostgreSQL enum which uses lowercase.
    """

    DIRECT = "direct"
    GROUP = "group"


class UserType(str, enum.Enum):
    """User type enum for participants and senders."""

    PARENT = "parent"
    EDUCATOR = "educator"


class Conversation(Base):
    """Conversation model for direct and group chats."""

    __tablename__ = "conversations"

    id = Column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4, index=True)
    type = Column(Enum(ConversationType, values_callable=lambda obj: [e.value for e in obj]), nullable=False, index=True)
    daycare_id = Column(Text, nullable=False, index=True)
    title = Column(Text, nullable=True)  # For group conversations
    direct_key_hash = Column(Text, nullable=True, index=True)  # For direct conversation canonicalization
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    participants = relationship(
        "ConversationParticipant",
        back_populates="conversation",
        cascade="all, delete-orphan",
    )
    messages = relationship(
        "Message",
        back_populates="conversation",
        cascade="all, delete-orphan",
        order_by="Message.created_at",
    )

    __table_args__ = (
        Index("ix_conversations_daycare_type_hash", "daycare_id", "type", "direct_key_hash"),
    )

    def __repr__(self):
        return f"<Conversation(id={self.id}, type='{self.type}', title='{self.title}')>"


class ConversationParticipant(Base):
    """Participant in a conversation."""

    __tablename__ = "conversation_participants"

    conversation_id = Column(
        PostgresUUID(as_uuid=True),
        ForeignKey("conversations.id", ondelete="CASCADE"),
        primary_key=True,
        nullable=False,
    )
    user_type = Column(Enum(UserType, values_callable=lambda obj: [e.value for e in obj]), primary_key=True, nullable=False, index=True)
    user_id = Column(Text, primary_key=True, nullable=False, index=True)

    conversation = relationship("Conversation", back_populates="participants")

    __table_args__ = (
        Index("ix_conversation_participants_user", "user_type", "user_id"),
    )

    def __repr__(self):
        return f"<ConversationParticipant(conversation_id={self.conversation_id}, user_type='{self.user_type}', user_id='{self.user_id}')>"


class Message(Base):
    """Message in a conversation."""

    __tablename__ = "messages"

    id = Column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4, index=True)
    conversation_id = Column(
        PostgresUUID(as_uuid=True),
        ForeignKey("conversations.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    sender_type = Column(Enum(UserType, values_callable=lambda obj: [e.value for e in obj]), nullable=False, index=True)
    sender_id = Column(Text, nullable=False, index=True)
    body = Column(Text, nullable=True)
    image_url = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False, index=True)

    conversation = relationship("Conversation", back_populates="messages")

    __table_args__ = (
        Index("ix_messages_conversation_created", "conversation_id", "created_at"),
    )

    def __repr__(self):
        return f"<Message(id={self.id}, conversation_id={self.conversation_id}, sender='{self.sender_type}:{self.sender_id}')>"


class PushToken(Base):
    """FCM push token for a user."""

    __tablename__ = "push_tokens"

    user_type = Column(Enum(UserType, values_callable=lambda obj: [e.value for e in obj]), primary_key=True, nullable=False, index=True)
    user_id = Column(Text, primary_key=True, nullable=False, index=True)
    token = Column(Text, primary_key=True, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    __table_args__ = (
        Index("ix_push_tokens_user", "user_type", "user_id"),
    )

    def __repr__(self):
        return f"<PushToken(user_type='{self.user_type}', user_id='{self.user_id}', token='{self.token[:20]}...')>"

