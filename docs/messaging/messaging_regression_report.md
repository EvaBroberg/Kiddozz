# Messaging Feature Regression Report

**Date:** 2024-11-05  
**Branch:** `feature/messaging-v1`  
**Status:** Build Failing - Compilation Errors

---

## Executive Summary

The Android app fails to compile due to **field name mismatches** between `MessageDto` (updated to match new backend API) and the mapper function `MessageDto.toDomain()` in `MessagingRepositoryImpl.kt`. The mapper references camelCase properties (`conversationId`, `senderId`, `imageUrl`, `createdAt`, `status`) that don't exist in the DTO, which uses snake_case (`conversation_id`, `sender_id`, `image_url`, `created_at`) and lacks a `status` field.

**Root Cause:** During the "standard messaging" implementation, `MessageDto` was updated to match the new backend API schema, but the mapper extension function was not updated to use the new field names.

---

## 1. Build Errors Overview

### Compilation Errors

```
> Task :app:compileLocalDebugKotlin FAILED
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:335:35 Unresolved reference 'conversationId'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:336:29 Unresolved reference 'senderId'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:338:29 Unresolved reference 'imageUrl'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:339:30 Unresolved reference 'createdAt'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:340:27 Unresolved reference 'senderId'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:341:33 Unresolved reference 'status'.
```

### Error Analysis

| Line | Symbol | Expected Provider | Actual State |
|------|--------|-------------------|--------------|
| 335 | `conversationId` | `MessageDto.conversation_id` | DTO has `conversation_id` (snake_case) |
| 336 | `senderId` | `MessageDto.sender_id` | DTO has `sender_id` (snake_case) |
| 338 | `imageUrl` | `MessageDto.image_url` | DTO has `image_url` (snake_case) |
| 339 | `createdAt` | `MessageDto.created_at` | DTO has `created_at` (snake_case, String type) |
| 340 | `senderId` | `MessageDto.sender_id` | DTO has `sender_id` (snake_case) |
| 341 | `status` | `MessageDto.status` | **DTO has no `status` field** |

---

## 2. DTO vs Mapper Comparison

### MessageDto Definition

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessageDto.kt`

```kotlin
data class MessageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("conversationId")
    val conversation_id: String,  // ⚠️ Property name is snake_case
    @SerializedName("senderId")
    val sender_id: String,  // ⚠️ Property name is snake_case
    @SerializedName("senderType")
    val sender_type: String,  // ⚠️ Property name is snake_case
    @SerializedName("body")
    val body: String?,
    @SerializedName("imageUrl")
    val image_url: String? = null,  // ⚠️ Property name is snake_case
    @SerializedName("createdAt")
    val created_at: String  // ⚠️ Property name is snake_case, type is String (ISO 8601)
    // ❌ NO 'status' field
)
```

### Mapper Function (Failing Code)

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:332-349`

```kotlin
private fun fi.kidozz.app.features.messaging.data.api.MessageDto.toDomain(): Message {
    return Message(
        id = this.id,
        conversationId = this.conversationId,  // ❌ Should be: this.conversation_id
        senderId = this.senderId,  // ❌ Should be: this.sender_id
        body = this.body,
        imageUrl = this.imageUrl,  // ❌ Should be: this.image_url
        createdAt = this.createdAt,  // ❌ Should be: this.created_at (and parse String to Long)
        isMine = this.senderId == "current_user",  // ❌ Should be: this.sender_id
        status = when (this.status.lowercase()) {  // ❌ 'status' field doesn't exist in DTO
            "pending" -> DeliveryStatus.PENDING
            "sent" -> DeliveryStatus.SENT
            "delivered" -> DeliveryStatus.DELIVERED
            "read" -> DeliveryStatus.READ
            else -> DeliveryStatus.SENT
        }
    )
}
```

### Field Mapping Table

| Domain Model Field | Mapper References | DTO Actual Field | Status |
|-------------------|-------------------|------------------|--------|
| `conversationId` | `this.conversationId` | `this.conversation_id` | ❌ Mismatch |
| `senderId` | `this.senderId` | `this.sender_id` | ❌ Mismatch |
| `imageUrl` | `this.imageUrl` | `this.image_url` | ❌ Mismatch |
| `createdAt` (Long) | `this.createdAt` | `this.created_at` (String) | ❌ Mismatch + Type mismatch |
| `status` | `this.status` | **Field doesn't exist** | ❌ Missing |
| `isMine` | Uses `this.senderId` | Should use `this.sender_id` | ❌ Mismatch |

---

## 3. API Contract vs Backend Availability

### MessagingApiService Endpoints

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessagingApiService.kt`

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

    @GET("messaging/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageDto>>

    @POST("messaging/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body message: SendMessageRequest
    ): Response<MessageDto>

    @POST("messaging/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("conversationId") conversationId: String
    ): Response<Unit>

    @POST("messaging/push/register")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): Response<Unit>
}
```

### Backend Availability

**Status:** Backend messaging endpoints exist but are **behind feature flag** `MESSAGING_BACKEND`.

**File:** `backend/app/main.py:23-25`

```python
# Register messaging router (behind feature flag)
if os.getenv("MESSAGING_BACKEND", "false").lower() == "true":
    from app.api import messaging
    app.include_router(messaging.router, prefix="/api", tags=["messaging"])
```

**Current State:**
- ✅ Backend endpoints are implemented (`backend/app/api/messaging.py`)
- ✅ Backend models exist (`backend/app/models/messaging.py`)
- ✅ Backend migration exists (`backend/alembic/versions/7b3f1700ae94_add_messaging_tables.py`)
- ⚠️ **Endpoints are not mounted** (feature flag defaults to `false`)
- ⚠️ Android code assumes endpoints exist (no feature flag checks in repository)

### Dead Interface Analysis

The Android `MessagingApiService` declares endpoints that:
1. **Exist in backend code** but are **not mounted** (feature flag off)
2. **Return DTOs** that match the backend schema (snake_case properties)
3. **Are called unconditionally** in `MessagingRepositoryImpl` (no feature flag checks)

**Impact:** If `MESSAGING_BACKEND=false`, API calls will return 404, but the code will still attempt to parse responses.

---

## 4. Room Schema vs Domain/DTO

### MessageEntity

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessageEntity.kt`

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val imageUrl: String? = null,
    val createdAt: Long,
    val isMine: Boolean,
    val status: String = "sent" // pending | sent | delivered | read
    // ❌ Missing: senderRole field (mentioned in requirements but not implemented)
)
```

### ConversationEntity

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/db/ConversationEntity.kt`

```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val type: String,              // "parent" | "educator" | "group"
    val participantsJson: String   // JSON array of participants
)
```

### Domain Models

**Message Domain Model:**

```kotlin
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val imageUrl: String? = null,
    val createdAt: Long,
    val isMine: Boolean,
    val status: DeliveryStatus = DeliveryStatus.SENT
)
```

**Conversation Domain Model:**

```kotlin
data class Conversation(
    val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val type: ConversationType,
    val participants: List<Participant>
)
```

### Schema Alignment Issues

| Entity Field | Domain Field | DTO Field | Status |
|--------------|--------------|-----------|--------|
| `MessageEntity.senderId` | `Message.senderId` | `MessageDto.sender_id` | ✅ Aligned (via mapper) |
| `MessageEntity.status` | `Message.status` (enum) | **Missing in DTO** | ⚠️ DTO lacks status |
| `MessageEntity.senderRole` | **Missing** | `MessageDto.sender_type` | ❌ Entity missing field |
| `ConversationEntity.type` | `Conversation.type` (enum) | `ConversationDto.type` (String) | ⚠️ Type mismatch |

### Migration Status

**File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessagingDatabase.kt`

```kotlin
@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,  // ⚠️ Still version 1, no migration for senderRole
    exportSchema = false
)
```

**Missing Migration:** No migration exists to add `senderRole` to `MessageEntity` (migration 1→2 was removed during revert).

---

## 5. ViewModel/Repository Call Graph

### openDirectWith() Flow

```
MessagingViewModel.openDirectWith(contactId)
  └─> MessagingRepository.createOrGetDirectConversation(contactId)
      └─> MessagingRepositoryImpl.createOrGetDirectConversation(contactId)
          ├─> dao.findDirectConversationWith(contactId)  // Local lookup
          └─> [If not found] Create local ConversationEntity
              └─> dao.insertConversation(...)  // Local-only, no server call
```

**Status:** ⚠️ **Local-only implementation** - Does not call `apiService.createDirectConversation()` even though the endpoint exists.

### sendMessage() Flow

```
MessagingViewModel.send(conversationId, text, image)
  └─> MessagingRepository.sendMessage(conversationId, text, image)
      └─> MessagingRepositoryImpl.sendMessage(conversationId, text, imageBytes)
          ├─> Create Message (domain) with status=PENDING
          ├─> dao.insertMessage(message.toEntity())  // Insert as pending
          ├─> apiService.sendMessage(conversationId, SendMessageRequest(...))  // ⚠️ Calls server
          ├─> response.body()?.toDomain()  // ❌ FAILS HERE - mapper uses wrong field names
          └─> dao.insertMessage(sentMessage.toEntity())  // Upsert with SENT status
```

**Status:** ❌ **Fails at mapper** - `toDomain()` extension function uses wrong field names.

### syncInitial() Flow

```
MessagingViewModel.init
  └─> repository.syncInitial()
      └─> MessagingRepositoryImpl.syncInitial()
          ├─> apiService.getConversations()  // ⚠️ Calls server
          ├─> response.body() ?: emptyList()  // Gets List<ConversationDto>
          └─> [Stub] // Convert and insert conversations - NOT IMPLEMENTED
```

**Status:** ⚠️ **Stub implementation** - Fetches conversations but doesn't insert them into Room.

### observeInbox() Flow

```
MessagingViewModel.inbox
  └─> repository.observeInbox(filter)
      └─> MessagingRepositoryImpl.observeInbox(filter)
          └─> dao.observeConversationsByType(filter?.name?.lowercase())
              └─> [Returns Flow<List<ConversationEntity>>]
                  └─> entities.map { it.toDomain() }
```

**Filtering Logic:**

```kotlin
when (filter) {
    ConversationType.GROUP -> repository.observeInbox(ConversationType.GROUP)
    else -> flowOf(emptyList())  // ⚠️ PARENT and EDUCATOR filters return empty
}
```

**Status:** ⚠️ **Filters out direct conversations** - Only GROUP conversations are shown in inbox. PARENT and EDUCATOR tabs show empty inbox.

---

## 6. Feature Flags & BuildConfig Check

### BuildConfig.MESSAGING_ANDROID

**File:** `app/build.gradle.kts:38-56`

```kotlin
productFlavors {
    create("local") {
        buildConfigField("Boolean", "MESSAGING_ANDROID", "true")
    }
    create("staging") {
        buildConfigField("Boolean", "MESSAGING_ANDROID", "true")
    }
    create("prod") {
        buildConfigField("Boolean", "MESSAGING_ANDROID", "false")
    }
}
```

### FeatureFlags Usage

**File:** `app/src/main/java/fi/kidozz/app/core/config/FeatureFlags.kt`

```kotlin
object FeatureFlags {
    val MESSAGING_ANDROID: Boolean = BuildConfig.MESSAGING_ANDROID
}
```

### Feature Flag Checks in Code

**Search Results:** `grep -r "MESSAGING_ANDROID\|FeatureFlags" app/src/main/java`

```
app/src/main/java/fi/kidozz/app/core/config/FeatureFlags.kt
  object FeatureFlags {
    val MESSAGING_ANDROID: Boolean = BuildConfig.MESSAGING_ANDROID
  }
```

**Status:** ❌ **Feature flag is defined but never used** - No code checks `FeatureFlags.MESSAGING_ANDROID` before calling messaging endpoints.

**Impact:** Even if `MESSAGING_ANDROID=false`, the code will still attempt to call server endpoints and parse responses.

---

## 7. What Changed vs What Remains

### Files Created/Modified During "Standard Messaging" Implementation

#### Backend (All Present)

| File | Status | Notes |
|------|--------|-------|
| `backend/app/models/messaging.py` | ✅ Created | Models exist |
| `backend/app/services/messaging_service.py` | ✅ Created | Service functions exist |
| `backend/app/api/messaging.py` | ✅ Created | API endpoints exist |
| `backend/app/schemas/messaging.py` | ✅ Created | Pydantic schemas exist |
| `backend/alembic/versions/7b3f1700ae94_add_messaging_tables.py` | ✅ Created | Migration exists |
| `backend/alembic/versions/8a9b1c2d3e4f_merge_heads_for_messaging.py` | ✅ Created | Merge migration exists |
| `backend/tests/test_messaging.py` | ✅ Created | Tests exist |
| `backend/app/main.py` | ✅ Modified | Router registration (feature-flagged) |
| `backend/app/models/__init__.py` | ✅ Modified | Imports messaging models |
| `backend/alembic/env.py` | ✅ Modified | Imports messaging models |

#### Android (Partially Reverted)

| File | Status | Notes |
|------|--------|-------|
| `app/src/main/java/.../api/MessagingApiService.kt` | ✅ Updated | New endpoints, new DTOs |
| `app/src/main/java/.../api/MessageDto.kt` | ✅ Updated | Snake_case fields, no status |
| `app/src/main/java/.../data/repo/MessagingRepositoryImpl.kt` | ⚠️ **Partially updated** | Mapper not updated |
| `app/src/main/java/.../data/db/MessageEntity.kt` | ❌ **Not updated** | Missing senderRole |
| `app/src/main/java/.../data/db/MessagingDatabase.kt` | ❌ **Not updated** | Still version 1 |
| `app/src/main/java/.../data/db/Migration1To2.kt` | ❌ **Removed** | Was deleted during revert |
| `app/src/main/java/.../core/config/FeatureFlags.kt` | ✅ Created | Feature flag exists |
| `app/build.gradle.kts` | ✅ Modified | MESSAGING_ANDROID flag added |

### Dangling References

1. **Mapper Extension Function** (`MessagingRepositoryImpl.kt:332-349`)
   - References old camelCase field names
   - References non-existent `status` field
   - **Status:** ❌ Causes compilation errors

2. **API Service Calls** (`MessagingRepositoryImpl.kt:66, 91`)
   - Calls `apiService.sendMessage()` and `apiService.getConversations()`
   - No feature flag checks
   - **Status:** ⚠️ Will fail at runtime if backend flag is off

3. **createOrGetDirectConversation** (`MessagingRepositoryImpl.kt:204-225`)
   - Creates local-only conversations
   - Does not call `apiService.createDirectConversation()`
   - **Status:** ⚠️ Inconsistent with API service interface

---

## 8. Root-Cause Hypotheses (Ranked)

### H1: MessageDto.toDomain() Mapper Uses Wrong Field Names ⭐⭐⭐⭐⭐

**Likelihood:** 100% (Confirmed by compilation errors)

**Justification:**
- `MessageDto` was updated to use snake_case properties (`conversation_id`, `sender_id`, `image_url`, `created_at`)
- Mapper extension function still references camelCase properties (`conversationId`, `senderId`, `imageUrl`, `createdAt`)
- Mapper references non-existent `status` field

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:332-349`
- Lines 335, 336, 338, 339, 340, 341

**Fix Required:**
```kotlin
// Change:
conversationId = this.conversationId
// To:
conversationId = this.conversation_id

// Change:
createdAt = this.createdAt
// To:
createdAt = parseIso8601ToLong(this.created_at)  // Also needs type conversion

// Remove:
status = when (this.status.lowercase()) { ... }
// Replace with:
status = DeliveryStatus.SENT  // Default, since DTO has no status field
```

---

### H2: MessageDto Missing Status Field ⭐⭐⭐⭐

**Likelihood:** 95% (Confirmed by DTO definition)

**Justification:**
- Backend `MessageOut` schema doesn't include `status` field
- Android `MessageDto` was updated to match backend but mapper still expects `status`
- Domain model `Message` requires `status: DeliveryStatus`

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessageDto.kt:5-20`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:341`

**Fix Required:**
- Either add `status` field to `MessageDto` (if backend provides it)
- Or default to `DeliveryStatus.SENT` in mapper (if backend doesn't provide it)

---

### H3: MessageDto.created_at Type Mismatch (String vs Long) ⭐⭐⭐

**Likelihood:** 90% (Confirmed by DTO definition)

**Justification:**
- `MessageDto.created_at` is `String` (ISO 8601 datetime)
- Domain model `Message.createdAt` is `Long` (Unix timestamp in milliseconds)
- Mapper doesn't convert String to Long

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessageDto.kt:19`
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:339`

**Fix Required:**
- Add ISO 8601 to Long conversion in mapper
- Or change DTO to use Long (if backend provides Unix timestamp)

---

### H4: No Feature Flag Checks in Repository ⭐⭐⭐

**Likelihood:** 85% (Confirmed by code search)

**Justification:**
- `FeatureFlags.MESSAGING_ANDROID` is defined but never checked
- Repository calls server endpoints unconditionally
- If backend flag is off, API calls will return 404

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:66, 91`
- No feature flag checks before `apiService.sendMessage()` or `apiService.getConversations()`

**Fix Required:**
- Add `if (FeatureFlags.MESSAGING_ANDROID)` checks before API calls
- Fall back to local-only behavior when flag is off

---

### H5: Inbox Filters Out Direct Conversations ⭐⭐

**Likelihood:** 80% (Confirmed by ViewModel code)

**Justification:**
- `MessagingViewModel.inbox` only shows GROUP conversations
- PARENT and EDUCATOR filters return empty list
- Even if direct conversations existed, they wouldn't be shown

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt:27-34`

**Fix Required:**
- Update `inbox` flow to include direct conversations for PARENT/EDUCATOR filters
- Or show conversations in contacts list (current UI approach)

---

### H6: createOrGetDirectConversation Doesn't Call Server ⭐⭐

**Likelihood:** 75% (Confirmed by implementation)

**Justification:**
- `MessagingRepositoryImpl.createOrGetDirectConversation()` creates local-only conversations
- `apiService.createDirectConversation()` exists but is never called
- Inconsistent with API service interface

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:204-225`

**Fix Required:**
- Call `apiService.createDirectConversation()` when feature flag is on
- Fall back to local-only when flag is off

---

### H7: MessageEntity Missing senderRole Field ⭐

**Likelihood:** 60% (Mentioned in requirements but not implemented)

**Justification:**
- Requirements mention adding `senderRole` to `MessageEntity`
- Migration 1→2 was removed during revert
- `MessageDto` has `sender_type` but entity doesn't store it

**Exact Locations:**
- `app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessageEntity.kt:7-16`
- No `senderRole` field

**Fix Required:**
- Add `senderRole: String` to `MessageEntity`
- Create migration 1→2
- Update `Message.toEntity()` to include `senderRole`

---

## 9. Appendix: Raw Logs & Code Snippets

### Build Error Log

```
> Task :app:compileLocalDebugKotlin FAILED
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:335:35 Unresolved reference 'conversationId'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:336:29 Unresolved reference 'senderId'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:338:29 Unresolved reference 'imageUrl'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:339:30 Unresolved reference 'createdAt'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:340:27 Unresolved reference 'senderId'.
e: file:///Users/ievabroberg/AndroidStudioProjects/Kiddozz/app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:341:33 Unresolved reference 'status'.

FAILURE: Build failed with an exception.
```

### MessageDto Definition (Current)

```kotlin
// app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessageDto.kt
data class MessageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("conversationId")
    val conversation_id: String,  // snake_case property
    @SerializedName("senderId")
    val sender_id: String,  // snake_case property
    @SerializedName("senderType")
    val sender_type: String,  // snake_case property
    @SerializedName("body")
    val body: String?,
    @SerializedName("imageUrl")
    val image_url: String? = null,  // snake_case property
    @SerializedName("createdAt")
    val created_at: String  // snake_case property, String type (ISO 8601)
    // NO 'status' field
)
```

### Mapper Function (Failing Code)

```kotlin
// app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:332-349
private fun fi.kidozz.app.features.messaging.data.api.MessageDto.toDomain(): Message {
    return Message(
        id = this.id,
        conversationId = this.conversationId,  // ❌ Should be: this.conversation_id
        senderId = this.senderId,  // ❌ Should be: this.sender_id
        body = this.body,
        imageUrl = this.imageUrl,  // ❌ Should be: this.image_url
        createdAt = this.createdAt,  // ❌ Should be: this.created_at (and parse String to Long)
        isMine = this.senderId == "current_user",  // ❌ Should be: this.sender_id
        status = when (this.status.lowercase()) {  // ❌ 'status' field doesn't exist
            "pending" -> DeliveryStatus.PENDING
            "sent" -> DeliveryStatus.SENT
            "delivered" -> DeliveryStatus.DELIVERED
            "read" -> DeliveryStatus.READ
            else -> DeliveryStatus.SENT
        }
    )
}
```

### MessageEntity Definition

```kotlin
// app/src/main/java/fi/kidozz/app/features/messaging/data/db/MessageEntity.kt
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String?,
    val imageUrl: String? = null,
    val createdAt: Long,
    val isMine: Boolean,
    val status: String = "sent"  // pending | sent | delivered | read
    // Missing: senderRole field
)
```

### ConversationEntity Definition

```kotlin
// app/src/main/java/fi/kidozz/app/features/messaging/data/db/ConversationEntity.kt
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessagePreview: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val type: String,              // "parent" | "educator" | "group"
    val participantsJson: String   // JSON array of participants
)
```

### MessagingApiService Signatures

```kotlin
// app/src/main/java/fi/kidozz/app/features/messaging/data/api/MessagingApiService.kt
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

    @GET("messaging/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageDto>>

    @POST("messaging/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body message: SendMessageRequest
    ): Response<MessageDto>

    @POST("messaging/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("conversationId") conversationId: String
    ): Response<Unit>

    @POST("messaging/push/register")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): Response<Unit>
}
```

---

## Summary

The compilation failure is caused by **field name mismatches** between `MessageDto` (snake_case) and the mapper function (camelCase). The mapper also references a non-existent `status` field and doesn't handle the `created_at` String-to-Long conversion.

**Immediate Fix Required:**
1. Update `MessageDto.toDomain()` to use snake_case property names
2. Remove `status` field reference (or default to `DeliveryStatus.SENT`)
3. Add ISO 8601 to Long conversion for `created_at`

**Secondary Issues:**
4. Add feature flag checks before API calls
5. Update `inbox` flow to show direct conversations
6. Implement `createOrGetDirectConversation()` to call server when flag is on
7. Add `senderRole` field to `MessageEntity` (if required)





