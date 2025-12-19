"""Messaging API endpoints for conversations, messages, and push notifications."""

import asyncio
import json
import os
from typing import Dict, List, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import StreamingResponse
from sqlalchemy import not_
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import get_current_user
from app.models.educator import Educator
from app.models.parent import Parent
from app.schemas.messaging import (
    ConversationOut,
    CreateDirectConversationRequest,
    CreateDirectConversationResponse,
    MessageOut,
    MessagingEvent,
    RegisterPushTokenRequest,
    SendMessageRequest,
)
from app.services.messaging_service import (
    append_message,
    get_or_create_direct_conversation,
    get_push_tokens_for_user,
    list_conversations_for_user,
    list_messages,
    verify_participant,
)
from app.services.messaging_service import (
    register_push_token as register_push_token_service,
)

# Feature flag
MESSAGING_BACKEND_ENABLED = os.getenv("MESSAGING_BACKEND", "false").lower() == "true"

router = APIRouter(prefix="/messaging", tags=["messaging"])


def get_user_type_and_id_from_token(current_user: Dict) -> tuple[str, str]:
    """
    Extract user_type and user_id from JWT token.

    Args:
        current_user: JWT payload from get_current_user

    Returns:
        (user_type, user_id) tuple
    """
    role = current_user.get("role", "").lower()
    user_id = current_user.get("sub")

    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User ID not found in token",
        )

    # Map role to user_type
    if role in ("educator", "super_educator"):
        user_type = "educator"
    elif role == "parent":
        user_type = "parent"
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid role: {role}",
        )

    return user_type, str(user_id)


def resolve_participant_name(
    db: Session,
    user_type: str,
    user_id: str,
) -> str:
    """
    Resolve participant name from user_type and user_id.

    Args:
        db: Database session
        user_type: 'parent' or 'educator'
        user_id: User ID

    Returns:
        Full name of the participant
    """
    if user_type == "parent":
        parent = db.query(Parent).filter(Parent.id == user_id).first()
        return parent.full_name if parent else f"Parent {user_id}"
    elif user_type == "educator":
        educator = db.query(Educator).filter(Educator.id == user_id).first()
        return educator.full_name if educator else f"Educator {user_id}"
    return f"User {user_id}"


def conversation_to_out(
    db: Session,
    conversation,
    current_user_type: str,
    current_user_id: str,
) -> ConversationOut:
    """
    Convert Conversation model to ConversationOut schema.

    Args:
        db: Database session
        conversation: Conversation model
        current_user_type: Current user type
        current_user_id: Current user ID

    Returns:
        ConversationOut schema
    """
    # Get participants
    participants = []
    for participant in conversation.participants:
        name = resolve_participant_name(
            db, participant.user_type.value, participant.user_id
        )
        participants.append(
            {
                "id": participant.user_id,
                "name": name,
                "avatar_url": None,
                "role": participant.user_type.value,
            }
        )

    # Get last message preview and timestamp
    last_message_preview = None
    last_timestamp = int(conversation.created_at.timestamp() * 1000)
    if conversation.messages:
        last_message = conversation.messages[-1]
        last_message_preview = last_message.body or "[Image]"
        last_timestamp = int(last_message.created_at.timestamp() * 1000)

    # Count unread (simplified: assume all messages are read for now)
    unread_count = 0

    return ConversationOut(
        id=conversation.id,
        type=conversation.type.value,  # Enum value matches database (lowercase)
        daycare_id=conversation.daycare_id,
        title=conversation.title,
        last_message_preview=last_message_preview,
        last_timestamp=last_timestamp,
        unread_count=unread_count,
        participants=participants,
    )


@router.post("/conversations/direct", response_model=CreateDirectConversationResponse)
def create_direct_conversation(
    request: CreateDirectConversationRequest,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Create or get a direct conversation with another user.
    Returns canonical conversation ID (same for both participants).
    """
    import logging

    logger = logging.getLogger(__name__)

    # VERSION MARKER - 2025-12-10-10:35
    logger.info("=" * 80)
    logger.info("VERSION 2025-12-10-10:35 - create_direct_conversation called")
    logger.info("=" * 80)

    # Log raw request body
    try:
        request_dict = {
            "withUserType": request.with_user_type,
            "withUserId": request.with_user_id,
        }
        logger.info("INCOMING REQUEST BODY (raw): %s", json.dumps(request_dict))
    except Exception as e:
        logger.error("Failed to log request body: %s", e)

    if not MESSAGING_BACKEND_ENABLED:
        logger.error("Messaging backend not enabled!")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    # Get current user info
    logger.info("Extracting user info from JWT token...")
    try:
        user_type, user_id = get_user_type_and_id_from_token(current_user)
        logger.info(
            "Extracted from token - user_type: %s, user_id: %s", user_type, user_id
        )
    except Exception as e:
        logger.exception("Failed to extract user_type/user_id from token: %s", e)
        raise

    daycare_id = current_user.get("daycare_id")
    logger.info("daycare_id from token: %s", daycare_id)
    logger.info("Full current_user dict keys: %s", list(current_user.keys()))

    if not daycare_id:
        logger.error(
            "daycare_id not found in token! current_user keys: %s",
            list(current_user.keys()),
        )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="daycare_id not found in token",
        )

    # Log all extracted values
    logger.info("=" * 80)
    logger.info("EXTRACTED VALUES:")
    logger.info("  user_type: %s", user_type)
    logger.info("  user_id: %s", user_id)
    logger.info("  daycare_id: %s", daycare_id)
    logger.info("  request.with_user_type: %s", request.with_user_type)
    logger.info("  request.with_user_id: %s", request.with_user_id)
    logger.info("=" * 80)

    # Get or create conversation
    participant_a = (user_type, user_id)
    participant_b = (request.with_user_type, request.with_user_id)

    logger.info("Participant tuples:")
    logger.info("  participant_a: %s", participant_a)
    logger.info("  participant_b: %s", participant_b)

    # Single-line context log for debugging 500 errors from Android
    logger.error(
        "CREATE_DIRECT context: daycare_id=%s user_type=%s user_id=%s with_user_type=%s with_user_id=%s",
        daycare_id,
        user_type,
        user_id,
        request.with_user_type,
        request.with_user_id,
    )

    try:
        logger.info("Calling get_or_create_direct_conversation...")
        conversation = get_or_create_direct_conversation(
            db=db,
            daycare_id=daycare_id,
            participant_a=participant_a,
            participant_b=participant_b,
        )
        logger.info(
            "get_or_create_direct_conversation returned successfully. Conversation ID: %s",
            conversation.id,
        )
    except ValueError as e:
        logger.warning("create_direct_conversation: bad request: %s", e)
        logger.exception("ValueError stack trace:")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        logger.exception(
            "create_direct_conversation: UNEXPECTED ERROR - Full stack trace:"
        )
        logger.error("Exception type: %s", type(e).__name__)
        logger.error("Exception message: %s", str(e))
        # Include error details in the response for easier debugging from the Android client.
        # This is mainly for local/dev; you can later switch back to a generic message if needed.
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Internal messaging error: {type(e).__name__}: {str(e)}",
        )

    # Build participants list matching ParticipantDto format
    participants = []
    for participant in conversation.participants:
        name = resolve_participant_name(
            db, participant.user_type.value, participant.user_id
        )
        participants.append(
            {
                "id": participant.user_id,
                "name": name,
                "avatar_url": None,
                "role": participant.user_type.value,
            }
        )

    return CreateDirectConversationResponse(
        conversation_id=conversation.id,
        participants=participants,
    )


@router.get("/conversations", response_model=List[ConversationOut])
def get_conversations(
    cursor: Optional[str] = Query(None, description="Cursor for pagination"),
    limit: int = Query(
        50, ge=1, le=100, description="Number of conversations to return"
    ),
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    List conversations where the current user is a participant.
    Returns both direct and group conversations.
    """
    if not MESSAGING_BACKEND_ENABLED:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    user_type, user_id = get_user_type_and_id_from_token(current_user)
    daycare_id = current_user.get("daycare_id")

    if not daycare_id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="daycare_id not found in token",
        )

    conversations = list_conversations_for_user(
        db=db,
        daycare_id=daycare_id,
        user_type=user_type,
        user_id=user_id,
        cursor=cursor,
        limit=limit,
    )

    # Convert to output schema
    return [conversation_to_out(db, conv, user_type, user_id) for conv in conversations]


@router.get(
    "/conversations/{conversation_id}/messages", response_model=List[MessageOut]
)
def get_messages(
    conversation_id: UUID,
    cursor: Optional[str] = Query(None, description="Cursor for pagination"),
    limit: int = Query(50, ge=1, le=100, description="Number of messages to return"),
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    List messages in a conversation.
    Requires user to be a participant.
    """
    if not MESSAGING_BACKEND_ENABLED:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    user_type, user_id = get_user_type_and_id_from_token(current_user)

    # Verify participant
    if not verify_participant(db, conversation_id, user_type, user_id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User is not a participant in this conversation",
        )

    messages = list_messages(
        db=db,
        conversation_id=conversation_id,
        cursor=cursor,
        limit=limit,
    )

    return [
        MessageOut(
            id=msg.id,
            conversation_id=msg.conversation_id,
            sender_id=msg.sender_id,
            sender_type=msg.sender_type.value,
            body=msg.body,
            image_url=msg.image_url,
            created_at=msg.created_at,
        )
        for msg in messages
    ]


@router.post("/conversations/{conversation_id}/messages", response_model=MessageOut)
def send_message(
    conversation_id: UUID,
    request: SendMessageRequest,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Send a message to a conversation.
    Requires user to be a participant.
    """
    if not MESSAGING_BACKEND_ENABLED:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    user_type, user_id = get_user_type_and_id_from_token(current_user)

    # Verify participant
    if not verify_participant(db, conversation_id, user_type, user_id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User is not a participant in this conversation",
        )

    # Validate request
    if not request.body and not request.image_url:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Either body or image_url must be provided",
        )

    # Convert client_message_id to UUID if provided
    message_id = None
    if request.client_message_id:
        try:
            message_id = UUID(request.client_message_id)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid client_message_id format. Must be a valid UUID.",
            )

    # Append message
    message = append_message(
        db=db,
        conversation_id=conversation_id,
        sender_type=user_type,
        sender_id=user_id,
        body=request.body,
        image_url=request.image_url,
        message_id=message_id,
    )

    # Create MessageOut DTO
    message_out = MessageOut(
        id=message.id,
        conversation_id=message.conversation_id,
        sender_id=message.sender_id,
        sender_type=message.sender_type.value,
        body=message.body,
        image_url=message.image_url,
        created_at=message.created_at,
    )

    # Publish event to SSE streams
    publish_message_event(
        conversation_id=conversation_id,
        message=message_out,
    )

    # Send FCM push notifications (async, fire-and-forget)
    send_fcm_notification_async(
        db=db,
        conversation_id=conversation_id,
        sender_type=user_type,
        sender_id=user_id,
        body=request.body,
    )

    return message_out


@router.post("/conversations/{conversation_id}/read")
def mark_conversation_as_read(
    conversation_id: UUID,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Mark a conversation as read (optional acknowledgment).
    """
    if not MESSAGING_BACKEND_ENABLED:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    user_type, user_id = get_user_type_and_id_from_token(current_user)

    # Verify participant
    if not verify_participant(db, conversation_id, user_type, user_id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User is not a participant in this conversation",
        )

    # TODO: Implement unread count tracking
    return {"status": "ok"}


@router.post("/push/register")
def register_push_token(
    request: RegisterPushTokenRequest,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Register or update a push token for the current user.
    """
    if not MESSAGING_BACKEND_ENABLED:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    user_type, user_id = get_user_type_and_id_from_token(current_user)

    push_token = register_push_token_service(
        db=db,
        user_type=user_type,
        user_id=user_id,
        token=request.token,
    )

    return {
        "status": "ok",
        "token_id": f"{push_token.user_type.value}:{push_token.user_id}:{push_token.token[:20]}...",
    }


# SSE event stream (simplified in-memory implementation)
# In production, use Redis Pub/Sub or similar
_event_streams: Dict[str, asyncio.Queue] = {}


async def event_stream_generator(user_key: str):
    """Generate SSE events for a user."""
    queue = asyncio.Queue()
    _event_streams[user_key] = queue

    try:
        while True:
            # Wait for event or timeout
            try:
                event_json = await asyncio.wait_for(queue.get(), timeout=30.0)
                # event_json is already a JSON string from MessagingEvent.model_dump_json()
                yield f"data: {event_json}\n\n"
            except asyncio.TimeoutError:
                # Send keepalive
                yield ": keepalive\n\n"
    finally:
        # Cleanup
        if user_key in _event_streams:
            del _event_streams[user_key]


def publish_message_event(conversation_id: UUID, message: MessageOut):
    """
    Publish a message event to all participants' streams.

    Args:
        conversation_id: UUID of the conversation
        message: MessageOut DTO containing the full message data
    """
    # Create typed event envelope
    event = MessagingEvent(
        type="message.created",
        conversation_id=conversation_id,
        message=message,
    )

    # Serialize to JSON once
    event_json = event.model_dump_json(by_alias=True)

    # Find all participants and publish to their streams
    # In production, query conversation participants from DB
    # For now, simplified: publish to all active streams (filtered by conversation_id on client)
    for user_key, queue in _event_streams.items():
        try:
            # Put the JSON string into the queue (will be parsed back to dict in event_stream_generator)
            queue.put_nowait(event_json)
        except asyncio.QueueFull:
            pass  # Skip if queue is full


@router.get("/events/stream")
async def get_event_stream(
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    SSE event stream for real-time message updates.
    Emits events when messages are created in conversations the user participates in.
    """
    if not MESSAGING_BACKEND_ENABLED:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Messaging API not enabled",
        )

    user_type, user_id = get_user_type_and_id_from_token(current_user)
    user_key = f"{user_type}:{user_id}"

    return StreamingResponse(
        event_stream_generator(user_key),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


def send_fcm_notification_async(
    db: Session,
    conversation_id: UUID,
    sender_type: str,
    sender_id: str,
    body: Optional[str],
):
    """
    Send FCM push notification to conversation participants (async, fire-and-forget).
    """
    import threading

    def _send():
        try:
            # Get conversation participants (excluding sender)
            from app.models.messaging import ConversationParticipant, UserType

            participants = (
                db.query(ConversationParticipant)
                .filter(
                    ConversationParticipant.conversation_id == conversation_id,
                    not_(
                        (ConversationParticipant.user_type == UserType(sender_type))
                        & (ConversationParticipant.user_id == sender_id)
                    ),
                )
                .all()
            )

            # Get sender name
            sender_name = resolve_participant_name(db, sender_type, sender_id)

            # Get FCM server key
            fcm_server_key = os.getenv("FCM_SERVER_KEY")
            if not fcm_server_key:
                return  # FCM not configured

            # Get push tokens for each participant
            for participant in participants:
                tokens = get_push_tokens_for_user(
                    db=db,
                    user_type=participant.user_type.value,
                    user_id=participant.user_id,
                )

                # Send FCM notification to each token
                for token_obj in tokens:
                    send_fcm_notification(
                        fcm_server_key=fcm_server_key,
                        token=token_obj.token,
                        conversation_id=str(conversation_id),
                        sender_name=sender_name,
                        preview=body[:100] if body else "[Image]",
                    )
        except Exception as e:
            # Log error but don't fail
            print(f"Error sending FCM notification: {e}")

    # Run in background thread
    thread = threading.Thread(target=_send, daemon=True)
    thread.start()


def send_fcm_notification(
    fcm_server_key: str,
    token: str,
    conversation_id: str,
    sender_name: str,
    preview: str,
):
    """
    Send FCM notification using HTTP v1 API.
    """
    import requests

    # For now, use legacy FCM API (simpler)
    # In production, use FCM HTTP v1 API with service account
    legacy_url = "https://fcm.googleapis.com/fcm/send"

    headers = {
        "Authorization": f"key={fcm_server_key}",
        "Content-Type": "application/json",
    }

    payload = {
        "to": token,
        "notification": {
            "title": sender_name,
            "body": preview,
        },
        "data": {
            "type": "message.created",
            "conversation_id": conversation_id,
        },
    }

    try:
        response = requests.post(legacy_url, json=payload, headers=headers, timeout=5)
        response.raise_for_status()
    except Exception as e:
        print(f"FCM notification failed: {e}")
