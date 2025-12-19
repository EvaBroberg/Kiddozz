# Task 03: Backend Messaging MVP

**Milestone:** 3  
**Status:** Pending  
**Owner:** TBD  
**Goal:** Implement minimal backend messaging API without Android integration.

---

## Current State

- No backend messaging endpoints exist
- No messaging tables in database
- Android app has local-only messaging (Room database)

---

## Steps

### 1. Create Alembic Migration

Create migration file: `backend/alembic/versions/XXXX_add_messaging_tables.py`

**Tables to create:**
- `conversations`:
  - `id` UUID PK
  - `type` TEXT CHECK IN ('direct','group') NOT NULL
  - `daycare_id` TEXT NOT NULL
  - `title` TEXT NULL (for group only)
  - `direct_key_hash` TEXT NULL (for canonicalization)
  - `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
  - UNIQUE (daycare_id, type, direct_key_hash)

- `conversation_participants`:
  - `conversation_id` UUID FK conversations(id) ON DELETE CASCADE
  - `user_type` TEXT CHECK IN ('parent','educator') NOT NULL
  - `user_id` TEXT NOT NULL
  - PRIMARY KEY (conversation_id, user_type, user_id)
  - Index on (user_type, user_id)

- `messages`:
  - `id` UUID PK
  - `conversation_id` UUID FK conversations(id) ON DELETE CASCADE
  - `sender_type` TEXT CHECK IN ('parent','educator') NOT NULL
  - `sender_id` TEXT NOT NULL
  - `body` TEXT NULL
  - `image_url` TEXT NULL
  - `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
  - Index on (conversation_id, created_at)

### 2. Create SQLAlchemy Models

Create `backend/app/models/messaging.py`:

```python
from sqlalchemy import Column, String, Text, ForeignKey, CheckConstraint, Index
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from sqlalchemy import DateTime

from app.core.database import Base

class Conversation(Base):
    __tablename__ = "conversations"
    # ... (see implementation)

class ConversationParticipant(Base):
    __tablename__ = "conversation_participants"
    # ... (see implementation)

class Message(Base):
    __tablename__ = "messages"
    # ... (see implementation)
```

### 3. Create Service Functions

Create `backend/app/services/messaging_service.py`:

```python
def compute_direct_key_hash(participant_a: Tuple[str, str], participant_b: Tuple[str, str]) -> str:
    """Compute canonical hash for direct conversation (order-invariant)."""
    # ... (see implementation)

def get_or_create_direct_conversation(...) -> Conversation:
    """Get or create direct conversation (canonical ID for both participants)."""
    # ... (see implementation)

def list_conversations_for_user(...) -> List[Conversation]:
    """List conversations where user is a participant."""
    # ... (see implementation)

def list_messages(...) -> List[Message]:
    """List messages in conversation with pagination."""
    # ... (see implementation)

def append_message(...) -> Message:
    """Append message to conversation."""
    # ... (see implementation)

def verify_user_is_participant(...) -> bool:
    """Verify user is a participant in conversation."""
    # ... (see implementation)
```

### 4. Create API Endpoints (Feature-Flagged)

Create `backend/app/api/messaging.py`:

```python
# Feature flag check
ENABLE_MESSAGING_API = os.getenv("ENABLE_MESSAGING_API", "false").lower() == "true"

@router.post("/conversations/direct", response_model=CreateDirectConversationResponse)
def create_direct_conversation(...):
    if not ENABLE_MESSAGING_API:
        raise HTTPException(404, "Messaging API not enabled")
    # ... (see implementation)

@router.get("/conversations", response_model=List[ConversationOut])
def get_conversations(...):
    if not ENABLE_MESSAGING_API:
        raise HTTPException(404, "Messaging API not enabled")
    # ... (see implementation)

# ... (other endpoints)
```

### 5. Register Router (Feature-Flagged)

Update `backend/app/main.py`:

```python
if os.getenv("ENABLE_MESSAGING_API", "false").lower() == "true":
    from app.api import messaging
    app.include_router(messaging.router, prefix="/api/messaging", tags=["messaging"])
```

### 6. Create Pydantic Schemas

Create `backend/app/schemas/messaging.py`:

```python
class ConversationOut(BaseModel):
    # ... (see implementation)

class CreateDirectConversationRequest(BaseModel):
    # ... (see implementation)

class MessageOut(BaseModel):
    # ... (see implementation)

class SendMessageRequest(BaseModel):
    # ... (see implementation)
```

### 7. Add Backend Tests

Create `backend/tests/test_messaging.py`:

```python
class TestDirectConversationCanonicalization:
    def test_hash_order_invariant(self):
        """Test that hash([A, B]) == hash([B, A])."""
        # ... (see implementation)

class TestMessagingService:
    def test_get_or_create_direct_conversation_returns_same_id(self, db_session):
        """Test that both participants get the same conversation ID."""
        # ... (see implementation)

    def test_post_message_as_sara_get_as_jessica(self, db_session):
        """Test that message posted by Sara is visible to Jessica."""
        # ... (see implementation)

    def test_non_participant_cannot_read(self, db_session):
        """Test that non-participant cannot read conversation."""
        # ... (see implementation)
```

### 8. Run Tests

```bash
# Backend tests
cd backend && pytest tests/test_messaging.py -v

# Run migration
cd backend && alembic upgrade head
```

### 9. cURL Verification

See `backend/docs/manual_checks/messaging_e2e.md` for cURL commands.

**Key tests:**
1. Login as Sara (Parent 10) → Create direct conversation with Jessica (Educator 27)
2. Send message as Sara → "Hello"
3. Login as Jessica (Educator 27) → Get conversations → Should see conversation with Sara
4. Get messages as Jessica → Should see "Hello" message

---

## Acceptance Criteria

- [ ] Alembic migration creates tables correctly
- [ ] Backend tests pass
- [ ] cURL tests show Sara↔Jessica can create/list canonical direct conversation
- [ ] cURL tests show messages can be sent/received
- [ ] Feature flag controls endpoint availability
- [ ] Android app does not call these endpoints yet

---

## Files to Create

- `backend/alembic/versions/XXXX_add_messaging_tables.py`
- `backend/app/models/messaging.py`
- `backend/app/services/messaging_service.py`
- `backend/app/api/messaging.py`
- `backend/app/schemas/messaging.py`
- `backend/tests/test_messaging.py`

---

## Files to Modify

- `backend/app/models/__init__.py` - Add messaging models
- `backend/app/main.py` - Register messaging router (feature-flagged)
- `backend/alembic/env.py` - Import messaging models

---

## Deliverable

- Backend messaging API (feature-flagged)
- Passing backend tests
- cURL verification script
- Migration applied successfully

