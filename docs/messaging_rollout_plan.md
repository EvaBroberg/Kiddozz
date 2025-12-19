# Messaging Feature Staged Rollout Plan

**Date:** 2024-11-05  
**Status:** Planning  
**Goal:** Implement server-side messaging with canonical conversation IDs, ensuring DMs sent by Sara (parent) to Jessica (educator) appear for Jessica after login, and vice-versa.

---

## Overview

This plan breaks down the messaging feature implementation into four incremental milestones, each with clear acceptance criteria, tests, and verification steps. Each milestone builds on the previous one, ensuring stability at each stage.

---

## Milestone 1: Lock Contacts Correctness (No Server Messaging)

**Goal:** Ensure contact filtering is correct and stable before adding server-side messaging.

### Current State
- Contact tabs exist: Parents, Educators, Groups
- Parents tab: Shows only parents in `session.groupIds` (self excluded)
- Educators tab:
  - If educator: Shows all educators except self
  - If parent: Shows only educators in `session.groupIds`
- Session management: `session.role`, `session.userId`, `session.groupIds` are correctly derived from auth and data

### Tasks
1. Verify existing unit tests cover the contact matrix:
   - ✅ Parent + Parents tab
   - ✅ Parent + Educators tab
   - ✅ Educator + Parents tab
   - ✅ Educator + Educators tab (self excluded)
2. Add any missing test cases
3. Manual verification:
   - Login as Parent (id 10): Verify Parents tab shows only parents sharing group 7 (exclude self)
   - Login as Parent (id 10): Verify Educators tab shows only educators in group 7
   - Login as Educator (id 27): Verify Educators tab shows all educators except 27
   - Login as Educator (id 27): Verify Parents tab shows only parents from educator's groups

### Acceptance Criteria
- [ ] All unit tests pass
- [ ] Manual verification confirms correct contact scoping
- [ ] Screenshots/documentation of UI scope
- [ ] No server messaging dependencies present

### Tests to Run
```bash
# Android unit tests
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingRepositoryImplTest*"
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingViewModelTest*"

# Backend tests (should not include messaging tests)
cd backend && pytest tests/ -v
```

### Deliverable
- Green test suite
- Screenshots confirming UI scope
- Documentation of contact filtering behavior

---

## Milestone 2: Identity Hardening & Logging Guardrails

**Goal:** Ensure no placeholder IDs leak into flows and add diagnostic logging.

### Tasks
1. Audit all messaging-related code for placeholder IDs:
   - Search for `"current_user"`, `"unknown_parent"`, `"unknown"` in messaging code
   - Ensure `session.userId` is always used (never placeholders)
   - Ensure `session.role` comes from `TokenManager.roleFlow` (reactive)
2. Add concise debug logging:
   - `MainActivity.kt`: Log session updates (role, userId, daycareId, groupIds)
   - `MessagingViewModel.kt`: Log before contacts selection (role, filter, userId, groupIds)
   - `MessagingRepositoryImpl.kt`: Log contact filtering steps (groupIds, matches, exclusions)
3. Add unit tests for role switching:
   - Test: Start with role=EDUCATOR, switch to role=PARENT
   - Assert: ViewModel branching flips correctly
   - Assert: Contacts flow updates reactively

### Acceptance Criteria
- [ ] No placeholder IDs in messaging flows
- [ ] All session updates logged
- [ ] Role switching test passes
- [ ] Logs confirm correct branching

### Tests to Add
```kotlin
// app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt
@Test
fun role_switching_from_educator_to_parent_updates_contacts() = runTest {
    // Start with role=EDUCATOR
    // Switch to role=PARENT
    // Assert contacts flow updates correctly
}
```

### Deliverable
- Clean codebase (no placeholders)
- Diagnostic logging in place
- Role switching test passing

---

## Milestone 3: Backend Messaging MVP (Server-Side Only, Behind Feature Flag)

**Goal:** Implement minimal backend messaging API without Android integration.

### Tasks
1. Create Alembic migration for messaging tables:
   - `conversations` table (UUID, type, daycare_id, direct_key_hash, title, created_at)
   - `conversation_participants` table (conversation_id, user_type, user_id)
   - `messages` table (UUID, conversation_id, sender_type, sender_id, body, image_url, created_at)
   - Direct conversation canonicalization via `direct_key_hash`
2. Create SQLAlchemy models:
   - `Conversation`, `ConversationParticipant`, `Message`
3. Create service functions:
   - `get_or_create_direct_conversation()` - canonical conversation ID
   - `list_conversations_for_user()` - user's conversations
   - `list_messages()` - paginated messages
   - `append_message()` - create message
   - `verify_user_is_participant()` - permission check
4. Create API endpoints (behind feature flag):
   - `POST /api/messaging/conversations/direct` - create/get direct conversation
   - `GET /api/messaging/conversations` - list conversations
   - `GET /api/messaging/conversations/{id}/messages` - get messages
   - `POST /api/messaging/conversations/{id}/messages` - send message
   - `POST /api/messaging/conversations/{id}/read` - mark as read
5. Add backend tests:
   - Direct conversation canonicalization (order-invariant hash)
   - Message send/receive
   - Permission checks

### Acceptance Criteria
- [ ] Alembic migration creates tables correctly
- [ ] Backend tests pass
- [ ] cURL tests show Sara↔Jessica can create/list canonical direct conversation
- [ ] cURL tests show messages can be sent/received
- [ ] Feature flag controls endpoint availability
- [ ] Android app does not call these endpoints yet

### Tests to Run
```bash
# Backend tests
cd backend && pytest tests/test_messaging.py -v

# cURL verification (see docs/manual_checks/messaging_e2e.md)
curl -X POST http://localhost:8000/api/messaging/conversations/direct ...
```

### Deliverable
- Backend messaging API (feature-flagged)
- Passing backend tests
- cURL verification script

---

## Milestone 4: Android Integration with Backend Messaging (Feature Flag Off by Default)

**Goal:** Wire Android app to backend messaging API with feature flag control.

### Tasks
1. Update `MessagingApiService.kt`:
   - Add `createDirectConversation()` endpoint
   - Update `getConversations()` to use new endpoint
   - Update `getMessages()` to use new endpoint
   - Update `sendMessage()` to use new endpoint
2. Update `MessagingRepositoryImpl.kt`:
   - `createOrGetDirectConversation()`: Call server API, persist server conversation ID
   - `sendMessage()`: Use real `session.userId` and `session.role`, call server API, upsert response
   - `syncInitial()`: Fetch conversations and messages from server, upsert to Room
3. Update Room models:
   - Add `senderRole` column to `MessageEntity` (migration 1→2)
   - Ensure `ConversationEntity` stores server conversation ID (UUID)
4. Update `MessagingViewModel.kt`:
   - `inbox`: Include direct conversations for PARENT/EDUCATOR filters
5. Update `MessagesListScreen.kt`:
   - Show existing conversations first, then contacts
6. Add feature flag:
   - `ENABLE_SERVER_MESSAGING` (default: false)
   - Controls whether Android calls server endpoints
   - When false: Falls back to local-only behavior
7. Add tests:
   - Repository tests for server integration
   - ViewModel tests for DM visibility
   - End-to-end test: Sara sends to Jessica, Jessica sees after sync

### Acceptance Criteria
- [ ] Feature flag controls server integration
- [ ] `createOrGetDirectConversation()` returns server conversation ID
- [ ] `sendMessage()` uses real userId and senderRole
- [ ] `syncInitial()` fetches and stores conversations/messages
- [ ] Inbox shows both direct and group conversations
- [ ] Unit tests pass
- [ ] Manual test: Sara→Jessica message appears for Jessica after login/sync

### Tests to Run
```bash
# Android unit tests
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingRepositoryImplTest*"
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingViewModelTest*"

# Manual acceptance test
# 1. Login as Parent 10 (Sara)
# 2. Open Messages → Educators tab
# 3. Click Jessica → Send "Hello"
# 4. Logout
# 5. Login as Educator 27 (Jessica)
# 6. Open Messages → Educators tab
# 7. Should see conversation with Sara
# 8. Open conversation → Should see "Hello" message
```

### Deliverable
- Android app integrated with backend (feature-flagged)
- Passing tests
- Manual acceptance test passing

---

## Checklist

### Milestone 1: Contacts Correctness
- [ ] Dev: Verify existing tests
- [ ] Dev: Add missing test cases
- [ ] Tests: All unit tests pass
- [ ] Manual QA: Verify contact scoping for Parent and Educator roles

### Milestone 2: Identity Hardening
- [ ] Dev: Remove placeholder IDs
- [ ] Dev: Add diagnostic logging
- [ ] Dev: Add role switching test
- [ ] Tests: Role switching test passes
- [ ] Manual QA: Verify logs show correct session values

### Milestone 3: Backend MVP
- [ ] Dev: Create Alembic migration
- [ ] Dev: Create SQLAlchemy models
- [ ] Dev: Create service functions
- [ ] Dev: Create API endpoints (feature-flagged)
- [ ] Tests: Backend tests pass
- [ ] Manual QA: cURL tests pass

### Milestone 4: Android Integration
- [ ] Dev: Update API client
- [ ] Dev: Update repository
- [ ] Dev: Update Room models/migration
- [ ] Dev: Update ViewModel
- [ ] Dev: Update UI
- [ ] Dev: Add feature flag
- [ ] Tests: Android tests pass
- [ ] Manual QA: Sara→Jessica scenario passes

---

## Risk Mitigation

1. **Feature Flag**: Server messaging is behind a feature flag, allowing gradual rollout
2. **Incremental Milestones**: Each milestone is independently verifiable
3. **Tests First**: Tests are added before implementation
4. **Rollback Plan**: Each milestone can be reverted independently if issues arise

---

## Notes

- Keep existing contact filtering logic intact throughout all milestones
- Session management fixes (from earlier work) must remain unchanged
- Backend messaging API can be developed and tested independently of Android
- Android integration should be opt-in via feature flag until fully tested

