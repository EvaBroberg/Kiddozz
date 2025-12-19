"""Tests for messaging API and service."""

import os
from unittest.mock import patch
from uuid import UUID

import pytest

from app.models.messaging import (
    Conversation,
    Message,
    PushToken,
    UserType,
)
from app.services.messaging_service import (
    append_message,
    compute_direct_key_hash,
    get_or_create_direct_conversation,
    list_messages,
    verify_participant,
)
from tests.conftest import TestingSessionLocal, client


@pytest.fixture
def db_session():
    """Create a test database session."""
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


class TestDirectConversationCanonicalization:
    """Test direct conversation canonicalization."""

    def test_hash_order_invariant(self):
        """Test that hash([A, B]) == hash([B, A])."""
        participant_a = ("parent", "10")
        participant_b = ("educator", "27")

        hash_ab = compute_direct_key_hash(participant_a, participant_b)
        hash_ba = compute_direct_key_hash(participant_b, participant_a)

        assert hash_ab == hash_ba, "Hash should be order-invariant"


class TestMessagingService:
    """Test messaging service functions."""

    def test_get_or_create_direct_conversation_returns_same_id(self, db_session):
        """Test that both participants get the same conversation ID."""
        daycare_id = "test-daycare"
        participant_a = ("parent", "10")
        participant_b = ("educator", "27")

        # Create conversation as participant A
        conv_a = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=participant_a,
            participant_b=participant_b,
        )

        # Get conversation as participant B (should return same ID)
        conv_b = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=participant_b,
            participant_b=participant_a,
        )

        assert (
            conv_a.id == conv_b.id
        ), "Both participants should get the same conversation ID"
        assert len(conv_a.participants) == 2, "Conversation should have 2 participants"

    def test_post_message_as_sara_get_as_jessica(self, db_session):
        """Test that message posted by Sara is visible to Jessica."""
        daycare_id = "test-daycare"
        sara = ("parent", "10")
        jessica = ("educator", "27")

        # Create conversation
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=sara,
            participant_b=jessica,
        )

        # Sara sends a message
        append_message(
            db=db_session,
            conversation_id=conversation.id,
            sender_type="parent",
            sender_id="10",
            body="Hello Jessica!",
        )

        # Jessica gets messages
        messages = list_messages(
            db=db_session,
            conversation_id=conversation.id,
        )

        assert len(messages) == 1, "Should have one message"
        assert messages[0].body == "Hello Jessica!", "Message body should match"
        assert messages[0].sender_id == "10", "Sender should be Sara"

    def test_non_participant_cannot_read(self, db_session):
        """Test that non-participant cannot read conversation."""
        daycare_id = "test-daycare"
        sara = ("parent", "10")
        jessica = ("educator", "27")

        # Create conversation between Sara and Jessica
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=sara,
            participant_b=jessica,
        )

        # Other user is not a participant
        is_participant = verify_participant(
            db=db_session,
            conversation_id=conversation.id,
            user_type="parent",
            user_id="11",
        )

        assert not is_participant, "Other user should not be a participant"


class TestRegisterPushTokenEndpoint:
    """Test the register_push_token API endpoint."""

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_register_push_token_as_parent(self, db_session, make_token):
        """Test registering a push token as a parent."""
        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id="test-daycare")
        headers = {"Authorization": f"Bearer {token}"}

        # Register push token
        response = client.post(
            "/api/messaging/push/register",
            headers=headers,
            json={"token": "test-fcm-token-12345"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert "token_id" in data

        # Verify token was saved in database
        push_token = (
            db_session.query(PushToken)
            .filter(
                PushToken.user_type == UserType.PARENT,
                PushToken.user_id == "10",
                PushToken.token == "test-fcm-token-12345",
            )
            .first()
        )
        assert push_token is not None, "Push token should be saved in database"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_register_push_token_as_educator(self, db_session, make_token):
        """Test registering a push token as an educator."""
        # Create educator token
        token = make_token(user_id="27", role="educator", daycare_id="test-daycare")
        headers = {"Authorization": f"Bearer {token}"}

        # Register push token
        response = client.post(
            "/api/messaging/push/register",
            headers=headers,
            json={"token": "test-fcm-token-educator-67890"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert "token_id" in data

        # Verify token was saved in database
        push_token = (
            db_session.query(PushToken)
            .filter(
                PushToken.user_type == UserType.EDUCATOR,
                PushToken.user_id == "27",
                PushToken.token == "test-fcm-token-educator-67890",
            )
            .first()
        )
        assert push_token is not None, "Push token should be saved in database"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_register_push_token_updates_existing(self, db_session, make_token):
        """Test that registering the same token again updates the existing record."""
        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id="test-daycare")
        headers = {"Authorization": f"Bearer {token}"}

        # Register push token first time
        response1 = client.post(
            "/api/messaging/push/register",
            headers=headers,
            json={"token": "test-fcm-token-duplicate"},
        )
        assert response1.status_code == 200

        # Get the first token's created_at
        push_token1 = (
            db_session.query(PushToken)
            .filter(
                PushToken.user_type == UserType.PARENT,
                PushToken.user_id == "10",
                PushToken.token == "test-fcm-token-duplicate",
            )
            .first()
        )
        first_created_at = push_token1.created_at

        # Register same token again
        response2 = client.post(
            "/api/messaging/push/register",
            headers=headers,
            json={"token": "test-fcm-token-duplicate"},
        )
        assert response2.status_code == 200

        # Verify it's the same token (not a duplicate)
        push_tokens = (
            db_session.query(PushToken)
            .filter(
                PushToken.user_type == UserType.PARENT,
                PushToken.user_id == "10",
                PushToken.token == "test-fcm-token-duplicate",
            )
            .all()
        )
        assert len(push_tokens) == 1, "Should only have one token, not duplicates"
        assert (
            push_tokens[0].created_at == first_created_at
        ), "created_at should not change"
        # updated_at should be updated (but we can't easily test timing, so just verify it exists)
        assert push_tokens[0].updated_at is not None

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_register_push_token_without_auth_fails(self):
        """Test that registering a push token without authentication fails."""
        # Try to register without token
        response = client.post(
            "/api/messaging/push/register",
            json={"token": "test-fcm-token-12345"},
        )

        assert response.status_code == 401

    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", False)
    def test_register_push_token_when_messaging_disabled_returns_404(self, make_token):
        """Test that endpoint returns 404 when messaging backend is disabled."""
        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id="test-daycare")
        headers = {"Authorization": f"Bearer {token}"}

        # Try to register push token
        response = client.post(
            "/api/messaging/push/register",
            headers=headers,
            json={"token": "test-fcm-token-12345"},
        )

        assert response.status_code == 404
        assert "Messaging API not enabled" in response.json()["detail"]


class TestSendMessageEndpoint:
    """Test the send_message API endpoint."""

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_send_message_with_client_message_id_uses_provided_id(
        self, db_session, make_token
    ):
        """Test that sending a message with client_message_id uses the provided ID."""
        # Create conversation (participants are already added by get_or_create_direct_conversation)
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)
        headers = {"Authorization": f"Bearer {token}"}

        # Send message with client_message_id
        client_message_id = "550e8400-e29b-41d4-a716-446655440000"
        response = client.post(
            f"/api/messaging/conversations/{conversation.id}/messages",
            headers=headers,
            json={
                "body": "Test message",
                "clientMessageId": client_message_id,
            },
        )

        assert response.status_code == 200
        data = response.json()
        assert (
            data["id"] == client_message_id
        ), "Message ID should match client_message_id"
        assert data["body"] == "Test message"

        # Verify message in database has the provided ID
        message = (
            db_session.query(Message)
            .filter(Message.id == UUID(client_message_id))
            .first()
        )
        assert message is not None, "Message should be saved with provided ID"
        assert str(message.id) == client_message_id

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_send_message_without_client_message_id_generates_uuid(
        self, db_session, make_token
    ):
        """Test that sending a message without client_message_id generates a UUID."""
        # Create conversation (participants are already added by get_or_create_direct_conversation)
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)
        headers = {"Authorization": f"Bearer {token}"}

        # Send message without client_message_id
        response = client.post(
            f"/api/messaging/conversations/{conversation.id}/messages",
            headers=headers,
            json={"body": "Test message"},
        )

        assert response.status_code == 200
        data = response.json()
        # Verify it's a valid UUID
        message_id = UUID(data["id"])
        assert message_id is not None, "Message ID should be a valid UUID"
        assert data["body"] == "Test message"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_send_message_with_invalid_client_message_id_returns_400(
        self, db_session, make_token
    ):
        """Test that sending a message with invalid client_message_id format returns 400."""
        # Create conversation (participants are already added by get_or_create_direct_conversation)
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)
        headers = {"Authorization": f"Bearer {token}"}

        # Send message with invalid client_message_id
        response = client.post(
            f"/api/messaging/conversations/{conversation.id}/messages",
            headers=headers,
            json={
                "body": "Test message",
                "clientMessageId": "not-a-valid-uuid",
            },
        )

        assert response.status_code == 400
        assert "Invalid client_message_id format" in response.json()["detail"]

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_send_message_uses_client_message_id_when_provided(
        self, db_session, make_token
    ):
        """Test that sending a message with client_message_id uses the provided ID in response and DB."""
        # Create conversation (participants are already added by get_or_create_direct_conversation)
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create parent token
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)
        headers = {"Authorization": f"Bearer {token}"}

        # Send message with client_message_id (using valid UUID format)
        client_message_id = "12345678-1234-1234-1234-123456789abc"
        response = client.post(
            f"/api/messaging/conversations/{conversation.id}/messages",
            headers=headers,
            json={
                "body": "Test message with client ID",
                "clientMessageId": client_message_id,
            },
        )

        # Assert response JSON has the client_message_id as the message id
        assert response.status_code == 200
        data = response.json()
        assert (
            data["id"] == client_message_id
        ), "Response JSON should have id matching client_message_id"
        assert data["body"] == "Test message with client ID"

        # Verify the stored message in DB has the provided ID
        message = (
            db_session.query(Message)
            .filter(Message.id == UUID(client_message_id))
            .first()
        )
        assert (
            message is not None
        ), "Message should be saved in database with provided ID"
        assert (
            str(message.id) == client_message_id
        ), "Database message ID should match client_message_id"
        assert message.body == "Test message with client ID"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_append_message_service_with_message_id(self, db_session):
        """Test that append_message service function accepts message_id parameter."""
        # Create conversation
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create message with provided ID
        message_id = UUID("550e8400-e29b-41d4-a716-446655440001")
        message = append_message(
            db=db_session,
            conversation_id=conversation.id,
            sender_type="parent",
            sender_id="10",
            body="Test message",
            message_id=message_id,
        )

        assert message.id == message_id, "Message should have the provided ID"
        assert message.body == "Test message"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_append_message_service_without_message_id_generates_uuid(self, db_session):
        """Test that append_message service function generates UUID when message_id is not provided."""
        # Create conversation
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create message without provided ID
        message = append_message(
            db=db_session,
            conversation_id=conversation.id,
            sender_type="parent",
            sender_id="10",
            body="Test message",
        )

        assert message.id is not None, "Message should have a generated UUID"
        assert isinstance(message.id, UUID), "Message ID should be a UUID"
        assert message.body == "Test message"


class TestSSEEvents:
    """Test SSE event structure and publishing."""

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_sse_event_has_stable_envelope_structure(self, db_session):
        """Test that SSE events have a stable envelope with type, conversation_id, and message."""
        import asyncio
        import json

        from app.api.messaging import _event_streams, publish_message_event
        from app.schemas.messaging import MessageOut

        # Create conversation
        daycare_id = "test-daycare"
        conversation = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "27"),
        )

        # Create a message
        message = append_message(
            db=db_session,
            conversation_id=conversation.id,
            sender_type="parent",
            sender_id="10",
            body="Test SSE message",
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

        # Set up a test queue to capture events
        test_queue = asyncio.Queue()
        test_user_key = "parent:10"
        _event_streams[test_user_key] = test_queue

        try:
            # Publish the event
            publish_message_event(
                conversation_id=conversation.id,
                message=message_out,
            )

            # Get the event from the queue (non-blocking)
            event_json = None
            try:
                event_json = test_queue.get_nowait()
            except asyncio.QueueEmpty:
                pytest.fail("Event was not published to queue")

            # Parse the JSON
            event_data = json.loads(event_json)

            # Assert event structure
            assert "type" in event_data, "Event should have 'type' field"
            assert (
                event_data["type"] == "message.created"
            ), "Event type should be 'message.created'"

            assert (
                "conversationId" in event_data
            ), "Event should have 'conversationId' field (camelCase)"
            assert event_data["conversationId"] == str(
                conversation.id
            ), "conversationId should match"

            assert "message" in event_data, "Event should have 'message' field"
            message_obj = event_data["message"]
            assert isinstance(message_obj, dict), "message should be an object"
            assert "id" in message_obj, "message should have 'id' field"
            assert message_obj["id"] == str(message.id), "message.id should match"
            assert (
                "conversationId" in message_obj
            ), "message should have 'conversationId' field"
            assert message_obj["conversationId"] == str(
                conversation.id
            ), "message.conversationId should match"
            assert "body" in message_obj, "message should have 'body' field"
            assert (
                message_obj["body"] == "Test SSE message"
            ), "message.body should match"
        finally:
            # Cleanup
            if test_user_key in _event_streams:
                del _event_streams[test_user_key]


class TestCreateDirectConversationEndpoint:
    """Test POST /api/messaging/conversations/direct endpoint."""

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_creates_new_direct_conversation(self, db_session, make_token):
        """Test 1: creates new direct conversation between two different users."""
        # Create users in database
        from uuid import uuid4

        from app.models.daycare import Daycare
        from app.models.educator import Educator, EducatorRole
        from app.models.parent import Parent

        daycare_id = str(uuid4())
        daycare = Daycare(id=daycare_id, name="Test Daycare")
        db_session.add(daycare)

        parent = Parent(
            id="10",
            full_name="Sara Johnson",
            email="sara@test.com",
            phone_num="123456789",
            daycare_id=daycare_id,
        )
        educator = Educator(
            id="20",
            full_name="Jessica Teacher",
            email="jessica@test.com",
            role=EducatorRole.EDUCATOR,
            daycare_id=daycare_id,
        )
        db_session.add(parent)
        db_session.add(educator)
        db_session.commit()

        # Auth as parent
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)

        # POST to create conversation with educator
        response = client.post(
            "/api/messaging/conversations/direct",
            headers={"Authorization": f"Bearer {token}"},
            json={
                "withUserType": "educator",
                "withUserId": "20",
            },
        )

        # Assert response
        assert response.status_code in (
            200,
            201,
        ), f"Expected 200/201, got {response.status_code}: {response.text}"
        data = response.json()
        assert "conversationId" in data, "Response should have conversationId"
        assert "participants" in data, "Response should have participants"
        assert len(data["participants"]) == 2, "Should have exactly 2 participants"

        # Verify participant IDs
        participant_ids = {p["id"] for p in data["participants"]}
        assert "10" in participant_ids, "Parent should be a participant"
        assert "20" in participant_ids, "Educator should be a participant"

        # Verify database state

        conversation = (
            db_session.query(Conversation)
            .filter(Conversation.id == UUID(data["conversationId"]))
            .first()
        )
        assert conversation is not None, "Conversation should exist in database"
        assert (
            str(conversation.type) == "direct" or conversation.type.value == "direct"
        ), "Conversation type should be direct"
        assert (
            len(conversation.participants) == 2
        ), "Conversation should have 2 participants"

        participant_user_ids = {p.user_id for p in conversation.participants}
        assert "10" in participant_user_ids, "Parent should be in participants"
        assert "20" in participant_user_ids, "Educator should be in participants"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_reuses_existing_conversation(self, db_session, make_token):
        """Test 2: reuses existing conversation when called again."""
        from uuid import uuid4

        from app.models.daycare import Daycare
        from app.models.educator import Educator, EducatorRole
        from app.models.parent import Parent

        daycare_id = str(uuid4())
        daycare = Daycare(id=daycare_id, name="Test Daycare")
        db_session.add(daycare)

        parent = Parent(
            id="10",
            full_name="Sara Johnson",
            email="sara@test.com",
            phone_num="123456789",
            daycare_id=daycare_id,
        )
        educator = Educator(
            id="20",
            full_name="Jessica Teacher",
            email="jessica@test.com",
            role=EducatorRole.EDUCATOR,
            daycare_id=daycare_id,
        )
        db_session.add(parent)
        db_session.add(educator)
        db_session.commit()

        # Pre-create a direct conversation
        existing_conv = get_or_create_direct_conversation(
            db=db_session,
            daycare_id=daycare_id,
            participant_a=("parent", "10"),
            participant_b=("educator", "20"),
        )
        existing_conv_id = str(existing_conv.id)
        db_session.commit()

        # Auth as parent and call endpoint again
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)

        response = client.post(
            "/api/messaging/conversations/direct",
            headers={"Authorization": f"Bearer {token}"},
            json={
                "withUserType": "educator",
                "withUserId": "20",
            },
        )

        # Assert response
        assert response.status_code in (
            200,
            201,
        ), f"Expected 200/201, got {response.status_code}: {response.text}"
        data = response.json()
        returned_conv_id = data["conversationId"]

        # Should return the same conversation ID
        assert (
            returned_conv_id == existing_conv_id
        ), "Should return existing conversation ID, not create duplicate"

        # Verify only one conversation exists
        conversations = (
            db_session.query(Conversation)
            .filter(
                Conversation.daycare_id == daycare_id,
                Conversation.type == "direct",
            )
            .all()
        )
        assert (
            len(conversations) == 1
        ), "Should have exactly one conversation, not duplicates"

    @patch.dict(os.environ, {"MESSAGING_BACKEND": "true"})
    @patch("app.api.messaging.MESSAGING_BACKEND_ENABLED", True)
    def test_self_conversation_is_rejected(self, db_session, make_token):
        """Test 3: self-conversation is rejected with 400."""
        from uuid import uuid4

        from app.models.daycare import Daycare
        from app.models.parent import Parent

        daycare_id = str(uuid4())
        daycare = Daycare(id=daycare_id, name="Test Daycare")
        db_session.add(daycare)

        parent = Parent(
            id="10",
            full_name="Sara Johnson",
            email="sara@test.com",
            phone_num="123456789",
            daycare_id=daycare_id,
        )
        db_session.add(parent)
        db_session.commit()

        # Auth as user 10
        token = make_token(user_id="10", role="parent", daycare_id=daycare_id)

        # Try to create conversation with self
        response = client.post(
            "/api/messaging/conversations/direct",
            headers={"Authorization": f"Bearer {token}"},
            json={
                "withUserType": "parent",
                "withUserId": "10",  # Same as authenticated user
            },
        )

        # Assert 400 error
        assert (
            response.status_code == 400
        ), f"Expected 400, got {response.status_code}: {response.text}"
        data = response.json()
        assert "detail" in data, "Error response should have detail"
        assert (
            "oneself" in data["detail"].lower() or "self" in data["detail"].lower()
        ), f"Error message should mention self-conversation: {data['detail']}"
