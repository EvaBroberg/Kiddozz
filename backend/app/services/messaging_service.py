"""Messaging service for conversations, messages, and push notifications."""

import hashlib
import json
from typing import List, Optional, Tuple
from uuid import UUID

from sqlalchemy import desc
from sqlalchemy.orm import Session

from app.models.messaging import (
    Conversation,
    ConversationParticipant,
    ConversationType,
    Message,
    PushToken,
    UserType,
)


def compute_direct_key_hash(
    participant_a: Tuple[str, str], participant_b: Tuple[str, str]
) -> str:
    """
    Compute canonical hash for direct conversation (order-invariant).

    Args:
        participant_a: (user_type, user_id)
        participant_b: (user_type, user_id)

    Returns:
        SHA256 hash of sorted JSON representation
    """
    # Ensure consistent ordering
    sorted_participants = sorted(
        [participant_a, participant_b], key=lambda x: (x[0], x[1])
    )
    key_json = json.dumps(sorted_participants, sort_keys=True)
    return hashlib.sha256(key_json.encode()).hexdigest()


def get_or_create_direct_conversation(
    db: Session,
    daycare_id: str,
    participant_a: Tuple[str, str],  # (user_type, user_id)
    participant_b: Tuple[str, str],  # (user_type, user_id)
) -> Conversation:
    """
    Get or create a direct conversation between two participants.
    Ensures canonicalization using a hash of sorted participant IDs.

    Args:
        db: Database session
        daycare_id: Daycare ID
        participant_a: (user_type, user_id) for first participant
        participant_b: (user_type, user_id) for second participant

    Returns:
        Conversation object (existing or newly created)

    Raises:
        ValueError: If participant_a == participant_b (cannot message self)
    """
    import logging

    logger = logging.getLogger(__name__)

    logger.info("=" * 80)
    logger.info("get_or_create_direct_conversation called")
    logger.info("  daycare_id: %s", daycare_id)
    logger.info("  participant_a: %s", participant_a)
    logger.info("  participant_b: %s", participant_b)
    logger.info("=" * 80)

    if participant_a == participant_b:
        logger.error("Self-conversation attempt detected!")
        raise ValueError("Cannot create a direct conversation with oneself.")

    # Compute canonical hash
    logger.info("Computing direct_key_hash...")
    try:
        direct_key_hash = compute_direct_key_hash(participant_a, participant_b)
        logger.info("  direct_key_hash: %s", direct_key_hash)
    except Exception as e:
        logger.exception("Failed to compute direct_key_hash: %s", e)
        raise

    logger.info("=" * 80)
    logger.info("Looking up existing direct conversation via ORM")
    logger.info("  daycare_id: %s", daycare_id)
    logger.info("  direct_key_hash: %s", direct_key_hash)
    logger.info("=" * 80)

    # Use plain SQLAlchemy enum handling: compare against ConversationType.DIRECT
    conversation = (
        db.query(Conversation)
        .filter(
            Conversation.daycare_id == daycare_id,
            Conversation.type == ConversationType.DIRECT,
            Conversation.direct_key_hash == direct_key_hash,
        )
        .first()
    )

    if conversation:
        logger.info(
            "Found existing conversation: id=%s, type=%s",
            conversation.id,
            conversation.type,
        )
        # Check if participants already exist
        existing_participants = {
            (p.user_type.value, p.user_id)
            for p in conversation.participants
        }
        participant_a_key = (participant_a[0].lower().strip(), str(participant_a[1]).strip())
        participant_b_key = (participant_b[0].lower().strip(), str(participant_b[1]).strip())
        
        if participant_a_key in existing_participants and participant_b_key in existing_participants:
            logger.info("All participants already exist, skipping participant creation")
            db.refresh(conversation)
            return conversation
        else:
            logger.info("Some participants missing, will add them")
    else:
        logger.info("No existing conversation found. Creating new one via ORM.")
        conversation = Conversation(
            type=ConversationType.DIRECT,
            daycare_id=daycare_id,
            direct_key_hash=direct_key_hash,
            title=None,
        )
        db.add(conversation)
        db.flush()  # assign ID from DB
        logger.info(
            "New conversation created (pending commit): id=%s, type=%s",
            conversation.id,
            conversation.type,
        )

    # Add participants (only if they don't exist)
    logger.info("=" * 80)
    logger.info("ADDING PARTICIPANTS")
    logger.info("  participant_a: %s", participant_a)
    logger.info("  participant_b: %s", participant_b)
    logger.info("=" * 80)

    # Normalize and validate participant_a
    user_type_a = participant_a[0].lower().strip()
    user_id_a = str(participant_a[1]).strip()
    if user_type_a not in ("parent", "educator"):
        raise ValueError(
            f"Invalid user_type for participant_a: '{participant_a[0]}' (normalized: '{user_type_a}')"
        )
    
    # Check if participant_a already exists
    existing_a = (
        db.query(ConversationParticipant)
        .filter(
            ConversationParticipant.conversation_id == conversation.id,
            ConversationParticipant.user_type == UserType(user_type_a),
            ConversationParticipant.user_id == user_id_a,
        )
        .first()
    )
    
    if not existing_a:
        try:
            logger.info("Creating ConversationParticipant for participant_a...")
            participant_a_obj = ConversationParticipant(
                conversation_id=conversation.id,
                user_type=UserType(user_type_a),
                user_id=user_id_a,
            )
            logger.info("  participant_a_obj: %s", participant_a_obj)
            db.add(participant_a_obj)
            logger.info("db.add(participant_a_obj) succeeded")
        except Exception as e:
            logger.exception("Failed to add participant_a: %s", e)
            raise
    else:
        logger.info("Participant_a already exists, skipping")

    # Normalize and validate participant_b
    user_type_b = participant_b[0].lower().strip()
    user_id_b = str(participant_b[1]).strip()
    if user_type_b not in ("parent", "educator"):
        raise ValueError(
            f"Invalid user_type for participant_b: '{participant_b[0]}' (normalized: '{user_type_b}')"
        )
    
    # Check if participant_b already exists
    existing_b = (
        db.query(ConversationParticipant)
        .filter(
            ConversationParticipant.conversation_id == conversation.id,
            ConversationParticipant.user_type == UserType(user_type_b),
            ConversationParticipant.user_id == user_id_b,
        )
        .first()
    )
    
    if not existing_b:
        try:
            logger.info("Creating ConversationParticipant for participant_b...")
            participant_b_obj = ConversationParticipant(
                conversation_id=conversation.id,
                user_type=UserType(user_type_b),
                user_id=user_id_b,
            )
            logger.info("  participant_b_obj: %s", participant_b_obj)
            db.add(participant_b_obj)
            logger.info("db.add(participant_b_obj) succeeded")
        except Exception as e:
            logger.exception("Failed to add participant_b: %s", e)
            raise
    else:
        logger.info("Participant_b already exists, skipping")

    try:
        logger.info("Calling db.commit()...")
        db.commit()
        logger.info("db.commit() succeeded")
    except Exception as e:
        logger.exception("db.commit() FAILED: %s", e)
        db.rollback()
        raise

    try:
        logger.info("Calling db.refresh(conversation)...")
        db.refresh(conversation)
        logger.info("db.refresh() succeeded. Final conversation: %s", conversation)
        logger.info("=" * 80)
        logger.info("get_or_create_direct_conversation SUCCESS")
        logger.info("=" * 80)
    except Exception as e:
        logger.exception("db.refresh() FAILED: %s", e)
        raise

    return conversation


def list_conversations_for_user(
    db: Session,
    daycare_id: str,
    user_type: str,
    user_id: str,
    cursor: Optional[str] = None,
    limit: int = 50,
) -> List[Conversation]:
    """
    List conversations where the user is a participant.

    Args:
        db: Database session
        daycare_id: Daycare ID
        user_type: User type ('parent' or 'educator')
        user_id: User ID
        cursor: Optional cursor for pagination (conversation ID)
        limit: Maximum number of conversations to return

    Returns:
        List of Conversation objects, ordered by most recent message
    """
    query = (
        db.query(Conversation)
        .join(ConversationParticipant)
        .filter(
            Conversation.daycare_id == daycare_id,
            ConversationParticipant.user_type == UserType(user_type),
            ConversationParticipant.user_id == user_id,
        )
    )

    if cursor:
        try:
            cursor_uuid = UUID(cursor)
            # Filter by cursor (simple ID-based pagination)
            query = query.filter(Conversation.id != cursor_uuid)
        except ValueError:
            pass  # Invalid cursor, ignore

    # Get conversations
    conversations = query.order_by(desc(Conversation.created_at)).limit(limit).all()

    # Sort by last message timestamp (eager load messages for sorting)
    for conv in conversations:
        # Load messages relationship
        _ = conv.messages

    # Sort by last message timestamp
    def get_last_timestamp(conv: Conversation) -> float:
        if conv.messages:
            return conv.messages[-1].created_at.timestamp()
        return conv.created_at.timestamp()

    return sorted(conversations, key=get_last_timestamp, reverse=True)


def list_messages(
    db: Session,
    conversation_id: UUID,
    cursor: Optional[str] = None,
    limit: int = 50,
) -> List[Message]:
    """
    List messages in a conversation with pagination.

    Args:
        db: Database session
        conversation_id: Conversation ID
        cursor: Optional cursor for pagination (message ID)
        limit: Maximum number of messages to return

    Returns:
        List of Message objects, ordered by created_at ASC
    """
    query = db.query(Message).filter(Message.conversation_id == conversation_id)

    if cursor:
        try:
            cursor_uuid = UUID(cursor)
            cursor_msg = db.query(Message).filter(Message.id == cursor_uuid).first()
            if cursor_msg:
                query = query.filter(Message.created_at > cursor_msg.created_at)
        except ValueError:
            pass  # Invalid cursor, ignore

    return query.order_by(Message.created_at.asc()).limit(limit).all()


def append_message(
    db: Session,
    conversation_id: UUID,
    sender_type: str,
    sender_id: str,
    body: Optional[str] = None,
    image_url: Optional[str] = None,
    message_id: Optional[UUID] = None,
) -> Message:
    """
    Append a message to a conversation.

    Args:
        db: Database session
        conversation_id: Conversation ID
        sender_type: Sender type ('parent' or 'educator')
        sender_id: Sender ID
        body: Message body (optional)
        image_url: Image URL (optional)
        message_id: Optional message ID (if provided by client)

    Returns:
        Created Message object

    Raises:
        ValueError: If conversation doesn't exist
    """
    # Verify conversation exists
    conversation = (
        db.query(Conversation).filter(Conversation.id == conversation_id).first()
    )
    if not conversation:
        raise ValueError(f"Conversation {conversation_id} not found")

    # Create message
    # If message_id is provided, use it; otherwise let database generate UUID via default
    message_kwargs = {
        "conversation_id": conversation_id,
        "sender_type": UserType(sender_type),
        "sender_id": sender_id,
        "body": body,
        "image_url": image_url,
    }
    if message_id is not None:
        message_kwargs["id"] = message_id

    message = Message(**message_kwargs)
    db.add(message)
    db.commit()
    db.refresh(message)
    return message


def verify_participant(
    db: Session,
    conversation_id: UUID,
    user_type: str,
    user_id: str,
) -> bool:
    """
    Verify if a user is a participant in a conversation.

    Args:
        db: Database session
        conversation_id: Conversation ID
        user_type: User type ('parent' or 'educator')
        user_id: User ID

    Returns:
        True if user is a participant, False otherwise
    """
    participant = (
        db.query(ConversationParticipant)
        .filter(
            ConversationParticipant.conversation_id == conversation_id,
            ConversationParticipant.user_type == UserType(user_type),
            ConversationParticipant.user_id == user_id,
        )
        .first()
    )
    return participant is not None


def register_push_token(
    db: Session,
    user_type: str,
    user_id: str,
    token: str,
) -> PushToken:
    """
    Register or update a push token for a user.

    Args:
        db: Database session
        user_type: User type ('parent' or 'educator')
        user_id: User ID
        token: FCM token

    Returns:
        PushToken object
    """
    # Try to find existing token
    push_token = (
        db.query(PushToken)
        .filter(
            PushToken.user_type == UserType(user_type),
            PushToken.user_id == user_id,
            PushToken.token == token,
        )
        .first()
    )

    if push_token:
        # Update updated_at
        from datetime import datetime

        push_token.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(push_token)
        return push_token

    # Create new token
    push_token = PushToken(
        user_type=UserType(user_type),
        user_id=user_id,
        token=token,
    )
    db.add(push_token)
    db.commit()
    db.refresh(push_token)
    return push_token


def get_push_tokens_for_user(
    db: Session,
    user_type: str,
    user_id: str,
) -> List[PushToken]:
    """
    Get all push tokens for a user.

    Args:
        db: Database session
        user_type: User type ('parent' or 'educator')
        user_id: User ID

    Returns:
        List of PushToken objects
    """
    return (
        db.query(PushToken)
        .filter(
            PushToken.user_type == UserType(user_type),
            PushToken.user_id == user_id,
        )
        .all()
    )
