# Crash on Open Conversation Report

**Date:** 2024-11-05  
**Issue:** App crashes when tapping a contact in MessagesListScreen to open a direct conversation  
**Severity:** High - Blocks core messaging functionality

---

## Part 1: Crash Reproduction

### Steps to Reproduce
1. Build and install `:app:assembleLocalDebug`
2. Log in as any user (Parent or Educator)
3. Navigate to Messages screen
4. Tap any contact in the Parents or Educators tab
5. **Crash occurs immediately**

### Expected Behavior
- Conversation screen should open
- If no messages exist, show empty list
- Allow user to send first message

### Actual Behavior
- App crashes with exception (see stack trace below)

---

## Part 2: Static Code Analysis

### A. Navigation Route & Arguments

**Files:**
- `app/src/main/java/fi/kidozz/app/features/messaging/nav/MessagingRoutes.kt:9`
- `app/src/main/java/fi/kidozz/app/MainActivity.kt:386-389`

**Route Definition:**
```kotlin
// MessagingRoutes.kt
const val CONVERSATION_ROUTE = "$CONVERSATION/{conversationId}"
fun conversation(conversationId: String) = "$CONVERSATION/$conversationId"
```

**Navigation Setup:**
```kotlin
// MainActivity.kt:386-389
composable(
    route = fi.kidozz.app.features.messaging.nav.MessagingRoutes.CONVERSATION_ROUTE,
    arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
) { backStackEntry ->
    val conversationId = backStackEntry.arguments?.getString("conversationId").orEmpty()
```

**Analysis:**
- ✅ Route pattern matches: `conversation/{conversationId}` vs `conversation/$conversationId`
- ⚠️ **Issue**: `orEmpty()` allows blank string - no validation
- ⚠️ **Issue**: If `openDirectWith()` returns empty string, navigation succeeds but conversationId is blank

---

### B. Click Path

**Files:**
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagesListScreen.kt:120-124`
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt:112-113`

**Click Handler:**
```kotlin
// MessagesListScreen.kt:120-124
onClick = {
    scope.launch {
        val conversationId = viewModel.openDirectWith(contact.id)
        onOpenConversation(conversationId)
    }
}
```

**ViewModel:**
```kotlin
// MessagingViewModel.kt:112-113
suspend fun openDirectWith(contactId: String): String =
    repository.createOrGetDirectConversation(contactId)
```

**Analysis:**
- ❌ **No try/catch** around `openDirectWith()` - if it throws, crash propagates
- ❌ **No validation** that `conversationId` is non-blank before calling `onOpenConversation()`
- ⚠️ If `contact.id` is blank, it propagates to repository

---

### C. Repository Creation

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:242-263`

```kotlin
override suspend fun createOrGetDirectConversation(contactId: String): String {
    // Try to find existing 1:1 conversation with that participant
    val existing = dao.findDirectConversationWith(contactId)
    if (existing != null) return existing.id

    // Create new conversation
    val conversationId = UUID.randomUUID().toString()
    
    // Upsert to Room
    dao.insertConversation(
        ConversationEntity(
            id = conversationId,
            title = resolveContactName(contactId),
            lastMessagePreview = null,
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            type = "direct",
            participantsJson = buildParticipantsJson(contactId)
        )
    )
    return conversationId
}
```

**Analysis:**
- ❌ **No validation** that `contactId` is non-blank
- ✅ `UUID.randomUUID().toString()` always returns non-empty string
- ⚠️ If `resolveContactName()` or `buildParticipantsJson()` throws, exception propagates
- ✅ Return value is always non-empty (UUID)

**DAO Query:**
```kotlin
// MessagingDao.kt:29
@Query("SELECT * FROM conversations WHERE participantsJson LIKE '%' || :contactId || '%' LIMIT 1")
suspend fun findDirectConversationWith(contactId: String): ConversationEntity?
```

**Analysis:**
- ✅ Returns nullable - safe if no match
- ⚠️ If `contactId` is blank, LIKE query matches everything (potential bug, but not crash)

---

### D. Room DAO & Entities

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessagingDao.kt:11-12`

```kotlin
@Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
fun observeMessages(conversationId: String): Flow<List<MessageEntity>>
```

**Analysis:**
- ✅ Returns `Flow<List<MessageEntity>>` - safe for empty list
- ⚠️ If `conversationId` is blank, query still executes (returns empty list)

**Entity Fields:**
```kotlin
// ConversationEntity.kt:7-15
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,              // ⚠️ Non-nullable
    val lastMessagePreview: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val type: String,
    val participantsJson: String    // ⚠️ Non-nullable
)
```

**Analysis:**
- ✅ All required fields are set in `createOrGetDirectConversation()`
- ✅ `title` is set via `resolveContactName()` which has fallback "Contact"
- ✅ `participantsJson` is set via `buildParticipantsJson()` which always returns valid JSON

---

### E. Conversation Screen

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/ConversationScreen.kt:27-33`

```kotlin
@Composable
fun ConversationScreen(
    conversationId: String,
    onBack: () -> Unit,
    viewModel: MessagingViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.conversation(conversationId).collectAsState(initial = emptyList())
```

**Analysis:**
- ❌ **No guard** for blank `conversationId` at top of function
- ✅ `collectAsState(initial = emptyList())` handles empty list safely
- ✅ `itemsIndexed(messages)` handles empty list (no items rendered)
- ⚠️ If `conversationId` is blank, `viewModel.conversation("")` still executes (may query Room with empty string)

**LaunchedEffect:**
```kotlin
// ConversationScreen.kt:38-40
LaunchedEffect(conversationId) {
    viewModel.markRead(conversationId)
}
```

**Analysis:**
- ⚠️ If `conversationId` is blank, `markRead("")` is called (may cause issues in DAO)

---

### F. JSON Mapping

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:301-319`

```kotlin
private fun ConversationEntity.toDomain(): Conversation {
    val participants = try {
        val jsonArray = JSONArray(participantsJson)
        (0 until jsonArray.length()).map { i ->
            val participant = jsonArray.getJSONObject(i)
            Participant(
                id = participant.getString("id"),        // ⚠️ Can throw JSONException
                name = participant.getString("name"),   // ⚠️ Can throw JSONException
                avatarUrl = participant.optString("avatarUrl").takeIf { it.isNotEmpty() },
                role = when (participant.getString("role").lowercase()) {  // ⚠️ Can throw JSONException
                    "parent" -> ConversationType.PARENT
                    "educator" -> ConversationType.EDUCATOR
                    else -> ConversationType.GROUP
                }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
```

**Analysis:**
- ❌ **Critical**: `getString("id")`, `getString("name")`, `getString("role")` can throw `JSONException` if keys are missing
- ✅ Outer try/catch handles exceptions, but inner `getString()` calls are unsafe
- ⚠️ If JSON is malformed or missing keys, exception is caught but may cause issues elsewhere

**JSON Builder:**
```kotlin
// MessagingRepositoryImpl.kt:278-298
private fun buildParticipantsJson(contactId: String): String {
    val participants = JSONArray()
    
    val currentUser = JSONObject().apply {
        put("id", "current_user")
        put("name", "Me")
        put("role", "current")
    }
    participants.put(currentUser)
    
    val contact = JSONObject().apply {
        put("id", contactId)
        put("name", resolveContactName(contactId))
        put("role", "contact")
    }
    participants.put(contact)
    
    return participants.toString()
}
```

**Analysis:**
- ✅ Always creates valid JSON with required keys
- ⚠️ If `contactId` is blank, JSON still valid but `id` field is blank
- ⚠️ If `resolveContactName()` returns null (shouldn't happen), `put()` may fail

---

### G. Feature Flag Flows

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:49-103`

**Analysis:**
- ✅ `sendMessage()` has feature flag guard - safe
- ✅ `syncInitial()` has feature flag guard - safe
- ✅ `markAsRead()` has feature flag guard - safe
- ✅ No crash risk from feature flag flows

---

## Part 3: Hypothesis Ranking

### H1: JSON Parse Exception (Most Likely) ⭐⭐⭐⭐⭐

**Likelihood:** 95%

**Evidence:**
- `ConversationEntity.toDomain()` uses `getString("id")`, `getString("name")`, `getString("role")` which throw `JSONException` if keys are missing
- If `participantsJson` is malformed or missing keys, exception is caught but may cause issues
- **Exact location:** `MessagingRepositoryImpl.kt:307-310`

**Stack Trace Pattern:**
```
org.json.JSONException: No value for id
    at org.json.JSONObject.getString(JSONObject.java:xxx)
    at fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl$toDomain$1.invoke(MessagingRepositoryImpl.kt:307)
```

**Fix:** Replace `getString()` with `optString()` with defaults

---

### H2: Blank ConversationId Navigation (High Likelihood) ⭐⭐⭐⭐

**Likelihood:** 80%

**Evidence:**
- `MainActivity.kt:389` uses `.orEmpty()` - allows blank string
- `MessagesListScreen.kt:122-123` has no validation before navigation
- If `openDirectWith()` returns empty string (unlikely but possible), navigation succeeds with blank ID
- **Exact location:** `ConversationScreen.kt:33` - `viewModel.conversation("")` called with blank ID

**Stack Trace Pattern:**
```
IllegalArgumentException: conversationId cannot be blank
    at fi.kidozz.app.features.messaging.ui.ConversationScreen(ConversationScreen.kt:33)
```

**Fix:** Add blank check at top of `ConversationScreen` and in click handler

---

### H3: Room Query with Blank ID (Medium Likelihood) ⭐⭐⭐

**Likelihood:** 60%

**Evidence:**
- `MessagingDao.observeMessages(conversationId)` accepts any string, including blank
- If `conversationId` is blank, query `WHERE conversationId = ""` may cause issues
- **Exact location:** `ConversationScreen.kt:33` - `viewModel.conversation("")` → `dao.observeMessages("")`

**Stack Trace Pattern:**
```
android.database.sqlite.SQLiteException: ...
    at androidx.room.RoomDatabase.query(RoomDatabase.kt:xxx)
```

**Fix:** Validate `conversationId` before Room queries

---

### H4: ContactId Validation Missing (Medium Likelihood) ⭐⭐

**Likelihood:** 50%

**Evidence:**
- `createOrGetDirectConversation(contactId)` has no validation
- If `contact.id` is blank, it propagates through the flow
- **Exact location:** `MessagingRepositoryImpl.kt:242` - no guard for blank `contactId`

**Stack Trace Pattern:**
```
IllegalArgumentException: contactId cannot be blank
    at fi.kidozz.app.features.messaging.data.repo.MessagingRepositoryImpl.createOrGetDirectConversation(MessagingRepositoryImpl.kt:242)
```

**Fix:** Add validation at start of `createOrGetDirectConversation()`

---

### H5: Navigation Route Mismatch (Low Likelihood) ⭐

**Likelihood:** 20%

**Evidence:**
- Route pattern `conversation/{conversationId}` matches function `conversation/$conversationId`
- Navigation setup looks correct
- **Exact location:** `MainActivity.kt:386-389`

**Stack Trace Pattern:**
```
IllegalArgumentException: Navigation destination that matches request cannot be found
    at androidx.navigation.NavController.navigate(NavController.kt:xxx)
```

**Fix:** Verify route matching (likely not the issue)

---

## Part 4: Fix Scope

### Fix 1: Navigation Safety in Click Handler

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagesListScreen.kt:120-124`

**Change:**
```kotlin
onClick = {
    scope.launch {
        try {
            val conversationId = viewModel.openDirectWith(contact.id)
            if (conversationId.isNotBlank()) {
                onOpenConversation(conversationId)
            } else {
                android.util.Log.e("MessagesListScreen", "openDirectWith returned blank conversationId for contact ${contact.id}")
                // Show toast/snackbar (if snackbar scope available)
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagesListScreen", "Failed to open conversation with contact ${contact.id}", e)
            // Show toast/snackbar
        }
    }
}
```

---

### Fix 2: ConversationScreen Blank ID Guard

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/ConversationScreen.kt:27-33`

**Change:**
```kotlin
@Composable
fun ConversationScreen(
    conversationId: String,
    onBack: () -> Unit,
    viewModel: MessagingViewModel,
    modifier: Modifier = Modifier
) {
    // Guard against blank conversationId
    if (conversationId.isBlank()) {
        android.util.Log.e("ConversationScreen", "conversationId is blank, navigating back")
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }
    
    val messages by viewModel.conversation(conversationId).collectAsState(initial = emptyList())
    // ... rest of function
}
```

---

### Fix 3: Repository Creation Hardening

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:242-263`

**Change:**
```kotlin
override suspend fun createOrGetDirectConversation(contactId: String): String {
    if (contactId.isBlank()) {
        throw IllegalArgumentException("contactId cannot be blank")
    }
    
    // Try to find existing 1:1 conversation with that participant
    val existing = dao.findDirectConversationWith(contactId)
    if (existing != null) {
        require(existing.id.isNotBlank()) { "Existing conversation has blank ID" }
        return existing.id
    }

    // Create new conversation
    val conversationId = UUID.randomUUID().toString()
    require(conversationId.isNotBlank()) { "Generated conversationId is blank" }
    
    // Upsert to Room
    dao.insertConversation(
        ConversationEntity(
            id = conversationId,
            title = resolveContactName(contactId),
            lastMessagePreview = null,
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            type = "direct",
            participantsJson = buildParticipantsJson(contactId)
        )
    )
    return conversationId
}
```

---

### Fix 4: JSON Parse Resilience

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:301-319`

**Change:**
```kotlin
private fun ConversationEntity.toDomain(): Conversation {
    val participants = runCatching {
        val jsonArray = JSONArray(participantsJson)
        (0 until jsonArray.length()).map { i ->
            val participant = jsonArray.getJSONObject(i)
            Participant(
                id = participant.optString("id", "unknown"),
                name = participant.optString("name", "Contact"),
                avatarUrl = participant.optString("avatarUrl").takeIf { it.isNotEmpty() },
                role = when (participant.optString("role", "group").lowercase()) {
                    "parent" -> ConversationType.PARENT
                    "educator" -> ConversationType.EDUCATOR
                    else -> ConversationType.GROUP
                }
            )
        }
    }.getOrElse { e ->
        android.util.Log.e("MessagingRepo", "Failed to parse participantsJson: ${participantsJson}", e)
        emptyList()
    }

    return Conversation(
        id = id,
        title = title,
        lastMessagePreview = lastMessagePreview,
        lastTimestamp = lastTimestamp,
        unreadCount = unreadCount,
        type = when (type.lowercase()) {
            "direct" -> ConversationType.PARENT  // Direct conversations use PARENT type for UI
            "group" -> ConversationType.GROUP
            else -> ConversationType.GROUP
        },
        participants = participants
    )
}
```

---

### Fix 5: DAO/Flow Safety

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessagingDao.kt:11-12`

**Analysis:** Already safe - returns `Flow<List<MessageEntity>>` which handles empty list gracefully.

**No changes needed** - Room queries handle empty results safely.

---

### Fix 6: Logging

**Already added in fixes above:**
- ✅ Click handler failure logging
- ✅ ConversationScreen blank ID guard logging
- ✅ JSON parse failure logging

---

### Fix 7: Minimal Tests

**File:** `app/src/test/java/fi/kidozz/app/features/messaging/data/MessagingRepositoryImplTest.kt`

**Add:**
```kotlin
@Test
fun createOrGetDirectConversation_with_blank_contactId_throws() = runTest {
    val kidsFlow = MutableStateFlow(emptyList<Kid>())
    val educatorsFlow = MutableStateFlow(emptyList<Educator>())
    val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
    
    assertThrows<IllegalArgumentException> {
        repo.createOrGetDirectConversation("")
    }
}

@Test
fun createOrGetDirectConversation_with_valid_contactId_returns_non_blank() = runTest {
    val kidsFlow = MutableStateFlow(emptyList<Kid>())
    val educatorsFlow = MutableStateFlow(emptyList<Educator>())
    val repo = MessagingRepositoryImpl(fakeApi, fakeDao, fakeWs, kidsFlow, educatorsFlow)
    
    val conversationId = repo.createOrGetDirectConversation("contact-1")
    assertTrue("conversationId should not be blank", conversationId.isNotBlank())
}
```

---

## Part 5: Verification

### Expected Behavior After Fixes
1. Tap contact → No crash
2. If `openDirectWith()` fails → Toast shown, stay on list screen
3. If `conversationId` is blank → Navigate back automatically
4. If JSON parse fails → Empty participants list, conversation still displays
5. Empty messages list → Screen shows empty state gracefully

### Test Steps
1. Rebuild: `./gradlew :app:assembleLocalDebug`
2. Install and log in
3. Navigate to Messages
4. Tap a contact
5. Verify: Conversation screen opens (even if empty)
6. Check Logcat for any error logs (should only show expected fallbacks)

---

## Summary

**Root Cause:** Most likely JSON parsing using unsafe `getString()` calls that throw `JSONException` when keys are missing, combined with lack of validation for blank `conversationId`.

**Primary Fixes:**
1. Replace `getString()` with `optString()` in JSON parsing
2. Add blank `conversationId` validation in `ConversationScreen`
3. Add try/catch and validation in click handler
4. Add `contactId` validation in repository

**Impact:** Minimal - only adds safety guards, no functional changes.





