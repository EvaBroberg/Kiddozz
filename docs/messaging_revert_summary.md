# Messaging Stack Revert Summary

**Date:** 2024-11-05  
**Branch:** `revert/messaging-to-last-good`  
**Status:** ✅ Complete

---

## Actions Taken

### 1. Safety Snapshot
- Created branch: `snapshot/messaging-stack-attempt`
- Committed all messaging stack changes for reference

### 2. Revert Branch
- Created branch: `revert/messaging-to-last-good`
- Reset to last known good commit: `db893da` (Fix messaging role source of truth)

### 3. Files Removed
- ✅ `backend/alembic/versions/f1a2b3c4d5e6_add_messaging_tables.py` - Migration deleted
- ✅ `backend/app/api/messaging.py` - API endpoints deleted
- ✅ `backend/app/models/messaging.py` - Models deleted
- ✅ `backend/app/schemas/messaging.py` - Schemas deleted
- ✅ `backend/app/services/messaging_service.py` - Service deleted
- ✅ `backend/tests/test_messaging.py` - Tests deleted
- ✅ `backend/docs/manual_checks/messaging_e2e.md` - Manual checks deleted
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/data/db/Migration1To2.kt` - Room migration deleted

### 4. Files Reverted to Last Good State
- ✅ `backend/app/models/__init__.py` - Removed messaging imports
- ✅ `backend/app/main.py` - Removed messaging router registration
- ✅ `backend/alembic/env.py` - Removed messaging model imports
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessagingApiService.kt` - Old endpoints (not /messaging/)
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessageEntity.kt` - No senderRole field
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessagingDatabase.kt` - Version 1 (not 2)
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessagingDao.kt` - Old query (no parentheses fix)
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt` - No sessionManager/daycareId params, local-only conversations
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt` - Inbox only shows GROUP (not direct)
- ✅ `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagesListScreen.kt` - Shows contacts only (not mixing inbox)
- ✅ `app/src/main/java/fi/kidozz/app/MainActivity.kt` - No sessionManager/daycareId in messagingRepository constructor

### 5. Known-Good Fixes Preserved
- ✅ `MainActivity.kt`: Session role comes from `TokenManager.roleFlow` (reactive)
- ✅ `MainActivity.kt`: For educator: `session.userId = educator.id`, `groupIds = educator.groups.map { id }`
- ✅ `MainActivity.kt`: For parent: `session.userId = authUserId`, `groupIds` computed only from that parent's kids
- ✅ `MessagingRepositoryImpl.observeContactsInMyGroups()`: Correct group filtering and self-exclusion
- ✅ `MessagingViewModel`: Role-aware branching:
  - Educators tab → educator role sees all educators except self; parent role sees only educators in groupIds
  - Parents tab → both roles see only parents in groupIds

---

## Verification

### Backend
- ✅ No messaging endpoints in `backend/app/api/`
- ✅ No messaging models in `backend/app/models/`
- ✅ No messaging migration in `backend/alembic/versions/`
- ✅ No messaging imports in `backend/app/main.py`
- ✅ No messaging imports in `backend/app/models/__init__.py`
- ✅ No messaging imports in `backend/alembic/env.py`

### Android
- ✅ `MessagingDatabase` is version 1 (not 2)
- ✅ `MessageEntity` has no `senderRole` field
- ✅ No Room migrations exist
- ✅ `MessagingApiService` has old endpoints (not `/messaging/`)
- ✅ `MessagingRepositoryImpl` has no `sessionManager` or `daycareId` parameters
- ✅ `createOrGetDirectConversation()` creates local conversations (not server)
- ✅ `sendMessage()` uses placeholder `"current_user"` (expected in last good state)
- ✅ `syncInitial()` is stubbed (not implemented)
- ✅ `MessagingViewModel.inbox` only shows GROUP conversations (not direct)
- ✅ `MessagesListScreen` shows contacts only (not mixing inbox)

### Build Status
- ✅ Android build successful: `./gradlew :app:assembleLocalDebug`
- ✅ No compilation errors
- ✅ No lint errors

---

## Next Steps

1. **Milestone 1**: Lock contacts correctness (verify existing tests)
2. **Milestone 2**: Identity hardening & logging guardrails
3. **Milestone 3**: Backend messaging MVP (feature-flagged)
4. **Milestone 4**: Android integration with backend messaging (feature-flagged)

See `docs/messaging_rollout_plan.md` for detailed rollout plan.

---

## Branches

- `snapshot/messaging-stack-attempt`: Contains the full messaging stack attempt (for reference)
- `revert/messaging-to-last-good`: Current working branch with stable contacts behavior

---

## Commit

```
dd22596 Plan: staged messaging rollout; revert to stable contacts; add tasks & docs
```

**Pushed to:** `origin/revert/messaging-to-last-good`

