"""Pydantic schemas for messaging API."""

from datetime import datetime
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, Field


class ParticipantOut(BaseModel):
    """Participant in a conversation."""

    id: str
    name: str
    avatar_url: Optional[str] = None
    role: str  # 'parent' or 'educator'

    class Config:
        from_attributes = True


class MessageOut(BaseModel):
    """Message output schema."""

    id: UUID
    conversation_id: UUID = Field(..., alias="conversationId")
    sender_id: str = Field(..., alias="senderId")
    sender_type: str = Field(..., alias="senderType")
    body: Optional[str] = None
    image_url: Optional[str] = Field(None, alias="imageUrl")
    created_at: datetime = Field(..., alias="createdAt")

    class Config:
        from_attributes = True
        populate_by_name = True


class ConversationOut(BaseModel):
    """Conversation output schema."""

    id: UUID
    type: str  # 'direct' or 'group'
    daycare_id: str = Field(..., alias="daycareId")
    title: Optional[str] = None
    last_message_preview: Optional[str] = Field(None, alias="lastMessagePreview")
    last_timestamp: int = Field(
        ..., alias="lastTimestamp"
    )  # Unix timestamp in milliseconds
    unread_count: int = Field(0, alias="unreadCount")
    participants: List[ParticipantOut]

    class Config:
        from_attributes = True
        populate_by_name = True


class CreateDirectConversationRequest(BaseModel):
    """Request to create or get a direct conversation."""

    with_user_type: str = Field(..., alias="withUserType")
    with_user_id: str = Field(..., alias="withUserId")

    class Config:
        populate_by_name = True


class CreateDirectConversationResponse(BaseModel):
    """Response for creating a direct conversation."""

    conversation_id: UUID = Field(..., alias="conversationId")
    participants: List[ParticipantOut]

    class Config:
        populate_by_name = True


class SendMessageRequest(BaseModel):
    """Request to send a message."""

    body: Optional[str] = None
    image_url: Optional[str] = Field(None, alias="imageUrl")
    client_message_id: Optional[str] = Field(None, alias="clientMessageId")

    class Config:
        populate_by_name = True


class RegisterPushTokenRequest(BaseModel):
    """Request to register a push token."""

    token: str


class MessagingEvent(BaseModel):
    """SSE event envelope for messaging events."""

    type: str
    conversation_id: UUID = Field(..., alias="conversationId")
    message: MessageOut

    class Config:
        populate_by_name = True
