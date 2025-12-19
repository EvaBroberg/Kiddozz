# Task 02: Session Identity Guardrails

**Milestone:** 2  
**Status:** Pending  
**Owner:** TBD  
**Goal:** Ensure no placeholder IDs leak into flows and add diagnostic logging.

---

## Current State

- Session management exists: `UserSessionManager`, `UserSession`
- `session.role` comes from `TokenManager.roleFlow` (reactive)
- `session.userId` and `session.groupIds` are derived from auth and data
- Some placeholder IDs may still exist in messaging flows

---

## Steps

### 1. Audit for Placeholder IDs

Search for placeholder IDs in messaging code:

```bash
# Search for placeholders
grep -r "current_user" app/src/main/java/fi/kidozz/app/features/messaging/
grep -r "unknown_parent" app/src/main/java/fi/kidozz/app/features/messaging/
grep -r "unknown" app/src/main/java/fi/kidozz/app/features/messaging/
```

**Files to check:**
- `MessagingRepositoryImpl.kt`:
  - [ ] `sendMessage()` - Ensure uses `session.userId`, not `"current_user"`
  - [ ] `createOrGetDirectConversation()` - Ensure uses `session.userId`, not `"current_user"`
  - [ ] `buildParticipantsJson()` - Ensure uses `session.userId`, not `"current_user"`
  - [ ] `MessageDto.toDomain()` - Ensure uses `session.userId` for `isMine` check

- `MainActivity.kt`:
  - [ ] Ensure `session.userId` is never `"current_user"` or `"unknown_parent"`
  - [ ] Ensure `session.role` comes from `TokenManager.roleFlow` (reactive)

### 2. Remove Placeholder IDs

Replace all placeholder IDs with real values from session:

```kotlin
// BEFORE
senderId = "current_user" // TODO: Get from auth

// AFTER
val session = sessionManager.session.value
senderId = session.userId
```

### 3. Add Diagnostic Logging

Add concise debug logging at key points:

**MainActivity.kt:**
```kotlin
// On session update
Log.d("SessionUpdate", "role=${session.role}, userId=${session.userId}, groupIds=${session.groupIds}, daycareId=$daycareId")
```

**MessagingViewModel.kt:**
```kotlin
// Before contacts selection
Log.d("ContactsSelect", "filter=$filter, role=${session.role}, userId=${session.userId}, groupIds=${session.groupIds}")
```

**MessagingRepositoryImpl.kt:**
```kotlin
// During contact filtering
Log.d("ContactFilter", "myGroupIds=$myGroupIds, myUserId=$myUserId, matches=${matches.size}")
```

### 4. Add Role Switching Test

Add unit test for role switching:

```kotlin
// app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt
@Test
fun role_switching_from_educator_to_parent_updates_contacts() = runTest {
    // GIVEN: Start with role=EDUCATOR
    val sessionManager = UserSessionManager(
        UserSession(userId = "27", role = UserRole.EDUCATOR, groupIds = setOf("7"))
    )
    val viewModel = MessagingViewModel(repo, sessionManager)
    
    // WHEN: Select Educators tab
    viewModel.setFilter(ConversationType.EDUCATOR)
    val contactsAsEducator = viewModel.contacts.first()
    
    // THEN: Should see all educators except self
    assertTrue("Should see all educators", contactsAsEducator.size > 0)
    
    // WHEN: Switch role to PARENT
    sessionManager.update(
        sessionManager.session.value.copy(role = UserRole.PARENT, userId = "10", groupIds = setOf("7"))
    )
    
    // THEN: Should see only educators in group 7
    val contactsAsParent = viewModel.contacts.first { it.size != contactsAsEducator.size }
    assertTrue("Should see only group-filtered educators", contactsAsParent.size <= contactsAsEducator.size)
}
```

### 5. Run Tests

```bash
# Android unit tests
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingViewModelTest*role_switching*"
```

### 6. Verify Logs

1. **Login as Parent (id 10):**
   - Check logs for `SessionUpdate`: Should show `role=PARENT, userId=10, groupIds={7}`
   - Check logs for `ContactsSelect`: Should show correct filter, role, userId, groupIds

2. **Login as Educator (id 27):**
   - Check logs for `SessionUpdate`: Should show `role=EDUCATOR, userId=27, groupIds={7}`
   - Check logs for `ContactsSelect`: Should show correct filter, role, userId, groupIds

---

## Acceptance Criteria

- [ ] No placeholder IDs in messaging flows
- [ ] All session updates logged
- [ ] Role switching test passes
- [ ] Logs confirm correct branching
- [ ] No `"current_user"` or `"unknown_parent"` in codebase

---

## Files to Modify

- `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`
- `app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt`

---

## Deliverable

- Clean codebase (no placeholders)
- Diagnostic logging in place
- Role switching test passing
- Log verification script

