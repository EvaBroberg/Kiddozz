# Task 04: Android Messaging Integration

**Milestone:** 4  
**Status:** Pending  
**Owner:** TBD  
**Goal:** Wire Android app to backend messaging API with feature flag control.

---

## Current State

- Backend messaging API exists (feature-flagged)
- Android app has local-only messaging (Room database)
- Contact filtering is correct and stable
- Session management is hardened

---

## Steps

### 1. Add Feature Flag

Create `app/src/main/java/fi/kidozz/app/core/config/FeatureFlags.kt`:

```kotlin
object FeatureFlags {
    val ENABLE_SERVER_MESSAGING = BuildConfig.ENABLE_SERVER_MESSAGING ?: false
}
```

Add to `app/build.gradle.kts`:

```kotlin
buildConfigField("Boolean", "ENABLE_SERVER_MESSAGING", "false")
```

### 2. Update MessagingApiService

Update `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessagingApiService.kt`:

```kotlin
interface MessagingApiService {
    @POST("messaging/conversations/direct")
    suspend fun createDirectConversation(
        @Body request: CreateDirectConversationRequest
    ): Response<CreateDirectConversationResponse>

    @GET("messaging/conversations")
    suspend fun getConversations(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<ConversationDto>>

    // ... (other endpoints)
}
```

### 3. Update Room Models

Update `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessageEntity.kt`:

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderRole: String, // NEW: "parent" | "educator"
    val body: String?,
    val imageUrl: String? = null,
    val createdAt: Long,
    val isMine: Boolean,
    val status: String = "sent"
)
```

Create migration: `app/src/main/java/fi/kidozz/app/features/messaging/data/db/Migration1To2.kt`

Update `MessagingDatabase.kt`:
- Version: 1 → 2
- Add migration: `MIGRATION_1_2`

### 4. Update MessagingRepositoryImpl

Update `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`:

**Constructor:**
```kotlin
class MessagingRepositoryImpl(
    private val apiService: MessagingApiService,
    private val dao: MessagingDao,
    private val webSocketClient: MessagingWebSocketClient,
    val kidsCache: StateFlow<List<Kid>>,
    val educatorsCache: StateFlow<List<Educator>>,
    private val sessionManager: UserSessionManager,
    private val daycareId: String?,
    private val enableServerMessaging: Boolean = false // Feature flag
) : MessagingRepository
```

**createOrGetDirectConversation():**
```kotlin
override suspend fun createOrGetDirectConversation(contactId: String): String {
    if (!enableServerMessaging) {
        // Fallback to local-only behavior
        // ... (existing local-only logic)
    }
    
    // Call server API
    val request = CreateDirectConversationRequest(...)
    val response = apiService.createDirectConversation(request)
    // ... (upsert to Room)
}
```

**sendMessage():**
```kotlin
override suspend fun sendMessage(...): Message {
    val session = sessionManager.session.value
    val senderId = session.userId // Real userId, not placeholder
    val senderRole = when (session.role) {
        UserRole.PARENT -> "parent"
        UserRole.EDUCATOR -> "educator"
    }
    
    if (!enableServerMessaging) {
        // Fallback to local-only behavior
        // ... (existing local-only logic)
    }
    
    // Call server API
    val response = apiService.sendMessage(conversationId, SendMessageRequest(...))
    // ... (upsert to Room)
}
```

**syncInitial():**
```kotlin
override suspend fun syncInitial() {
    if (!enableServerMessaging) {
        return // No-op if feature flag is off
    }
    
    // Fetch conversations from server
    val conversations = apiService.getConversations().body() ?: emptyList()
    // ... (upsert to Room)
    
    // Fetch messages for each conversation
    for (conv in conversations) {
        val messages = apiService.getMessages(conv.id).body() ?: emptyList()
        // ... (upsert to Room)
    }
}
```

### 5. Update MessagingViewModel

Update `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`:

```kotlin
val inbox: StateFlow<List<Conversation>> =
    _filter.flatMapLatest { filter ->
        when (filter) {
            ConversationType.GROUP -> repository.observeInbox(ConversationType.GROUP)
            ConversationType.PARENT, ConversationType.EDUCATOR -> {
                if (enableServerMessaging) {
                    repository.observeInbox(filter) // Show direct conversations
                } else {
                    flowOf(emptyList()) // No direct conversations if feature flag is off
                }
            }
            else -> flowOf(emptyList())
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

### 6. Update MessagesListScreen

Update `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagesListScreen.kt`:

```kotlin
when (filter) {
    ConversationType.PARENT, ConversationType.EDUCATOR -> {
        // Show existing conversations first (if feature flag is on)
        if (enableServerMessaging && inbox.isNotEmpty()) {
            // ... (show conversations)
        }
        // Then show contacts (for starting new conversations)
        // ... (show contacts)
    }
    // ... (other filters)
}
```

### 7. Update MainActivity

Update `app/src/main/java/fi/kidozz/app/MainActivity.kt`:

```kotlin
val messagingRepository = MessagingRepositoryImpl(
    messagingApiService, messagingDao, messagingWebSocketClient,
    kidsViewModel.kids, educatorsListViewModel.educators,
    sessionManager, daycareId,
    enableServerMessaging = FeatureFlags.ENABLE_SERVER_MESSAGING
)
```

### 8. Add Tests

Update `app/src/test/java/fi/kidozz/app/features/messaging/data/MessagingRepositoryImplTest.kt`:

```kotlin
@Test
fun createOrGetDirectConversation_returns_server_id_when_feature_enabled() = runTest {
    // GIVEN: Feature flag enabled
    // WHEN: Create direct conversation
    // THEN: Should return server conversation ID
}

@Test
fun sendMessage_uses_real_userId_when_feature_enabled() = runTest {
    // GIVEN: Feature flag enabled
    // WHEN: Send message
    // THEN: Should use real userId from session
}

@Test
fun syncInitial_fetches_conversations_when_feature_enabled() = runTest {
    // GIVEN: Feature flag enabled
    // WHEN: Sync initial
    // THEN: Should fetch and store conversations/messages
}
```

Update `app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt`:

```kotlin
@Test
fun sara_sends_to_jessica_visible_after_sync() = runTest {
    // GIVEN: Sara sends message to Jessica
    // WHEN: Jessica syncs
    // THEN: Message should be visible
}
```

### 9. Run Tests

```bash
# Android unit tests
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingRepositoryImplTest*"
./gradlew :app:testLocalDebugUnitTest --tests "*MessagingViewModelTest*"
```

### 10. Manual Acceptance Test

1. **Enable feature flag:**
   - Set `ENABLE_SERVER_MESSAGING=true` in `app/build.gradle.kts`
   - Rebuild app

2. **Login as Parent 10 (Sara):**
   - Open Messages → Educators tab
   - Click Jessica (educator 27)
   - Send message: "Hello"

3. **Logout and Login as Educator 27 (Jessica):**
   - Open Messages → Educators tab
   - Should see conversation with Sara
   - Open conversation
   - Should see "Hello" message from Sara
   - Send reply: "Hi Sara!"

4. **Logout and Login as Parent 10 (Sara):**
   - Open Messages → Educators tab
   - Should see conversation with Jessica
   - Open conversation
   - Should see both messages: "Hello" (from Sara) and "Hi Sara!" (from Jessica)

---

## Acceptance Criteria

- [ ] Feature flag controls server integration
- [ ] `createOrGetDirectConversation()` returns server conversation ID (when enabled)
- [ ] `sendMessage()` uses real userId and senderRole (when enabled)
- [ ] `syncInitial()` fetches and stores conversations/messages (when enabled)
- [ ] Inbox shows both direct and group conversations (when enabled)
- [ ] Unit tests pass
- [ ] Manual test: Sara→Jessica message appears for Jessica after login/sync
- [ ] Feature flag off: Falls back to local-only behavior

---

## Files to Modify

- `app/src/main/java/fi/kidozz/app/core/config/FeatureFlags.kt` (new)
- `app/build.gradle.kts` - Add feature flag
- `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessagingApiService.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessageEntity.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/db/Migration1To2.kt` (new)
- `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessagingDatabase.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagesListScreen.kt`
- `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- `app/src/test/java/fi/kidozz/app/features/messaging/data/MessagingRepositoryImplTest.kt`
- `app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt`

---

## Deliverable

- Android app integrated with backend (feature-flagged)
- Passing tests
- Manual acceptance test passing
- Feature flag controls behavior correctly

