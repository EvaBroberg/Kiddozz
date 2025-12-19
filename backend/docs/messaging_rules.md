# Messaging Feature Rules & Implementation

**Date:** 2025-01-10  
**Status:** ✅ **IMPLEMENTED**

---

## Overview

The messaging feature enforces strict group-based filtering to ensure users only see contacts who share at least one group with them. The implementation differs based on user role (Parent vs Educator).

---

## Role-Specific Rules

### Educator Role (`educator` / `super_educator`)

#### Educators Tab
- **Rule:** Show ALL educators in the daycare except yourself
- **Implementation:** `observeAllEducatorsExcept(session.userId)`
- **Source:** `educatorsCache` (all educators for the daycare)
- **Filter:** Exclude `session.userId` only
- **Group filtering:** None (shows all educators regardless of groups)

#### Parents Tab
- **Rule:** Show only parents of kids in the educator's assigned groups
- **Implementation:** `observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)`
- **Source:** `kidsCache` → filter by `kid.group_id in session.groupIds` → extract parents
- **Filter:** 
  1. Filter kids where `kid.group_id in session.groupIds`
  2. Extract parents from those kids
  3. Exclude `session.userId`
  4. Deduplicate by parent ID
  5. Sort by name

### Parent Role (`parent`)

#### Parents Tab
- **Rule:** Show only parents who share your groupIds (parents of kids in the same groups as your kids)
- **Implementation:** `observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)`
- **Source:** `kidsCache` → filter by `kid.group_id in session.groupIds` → extract parents
- **Filter:**
  1. Filter kids where `kid.group_id in session.groupIds`
  2. Extract parents from those kids
  3. Exclude `session.userId`
  4. Deduplicate by parent ID
  5. Sort by name

#### Educators Tab
- **Rule:** Show only educators assigned to your groupIds
- **Implementation:** `observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)`
- **Source:** `educatorsCache` → filter by `educator.groups.any { it.id in session.groupIds }`
- **Filter:**
  1. Filter educators where `educator.groups.any { it.id in session.groupIds }`
  2. Exclude `session.userId`
  3. Deduplicate by educator ID
  4. Sort by name

---

## Session Management

### Session Derivation

**Location:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`

#### For Educators

1. **userId:**
   - **Source:** `educator.id` from database (not from token, as token may have different format)
   - **Code:** `MainActivity.kt:171` → `val educatorUserId = educator.id`
   - **Updated:** When `currentEducator` loads (`MainActivity.kt:167-180`)

2. **groupIds:**
   - **Source:** `educator.groups.map { it.id.toString() }.toSet()`
   - **Code:** `MainActivity.kt:172` → `val educatorGroupIds = educator.groups.map { it.id.toString() }.toSet()`
   - **Updated:** When `currentEducator` loads (`MainActivity.kt:167-180`)

3. **daycareId:**
   - **Source:** JWT token claim `daycare_id`
   - **Code:** `MainActivity.kt:145` → `val daycareId = authDaycareId ?: ...`
   - **Extracted by:** `TokenManager.daycareIdFlow` (`TokenManager.kt:61-67`)

#### For Parents

1. **userId:**
   - **Source:** JWT token claim `sub` (user ID)
   - **Code:** `MainActivity.kt:187` → `val currentParentId = authUserId`
   - **Extracted by:** `TokenManager.userIdFlow` (`TokenManager.kt:50-56`)

2. **groupIds:**
   - **Source:** Computed from parent's kids only
   - **Code:** `MainActivity.kt:192-199`
   - **Logic:**
     ```kotlin
     val myKids = kids.filter { kid ->
         kid.parents.any { parent -> parent.id == currentParentId }
     }
     val parentGroupIds = myKids.mapNotNull { it.group_id?.toString() }.toSet()
     ```
   - **Updated:** When `kids` StateFlow emits (`MainActivity.kt:189-229`)

3. **daycareId:**
   - **Source:** JWT token claim `daycare_id`
   - **Code:** `MainActivity.kt:145` → `val daycareId = authDaycareId ?: ...`
   - **Extracted by:** `TokenManager.daycareIdFlow` (`TokenManager.kt:61-67`)

### JWT Token Claims

**Location:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`

The JWT token contains the following claims:
- `sub`: User ID (educator ID or parent ID)
- `role`: User role (`educator`, `super_educator`, `parent`)
- `daycare_id`: Daycare ID (UUID string)

**Extraction:**
- `getUserId()`: Extracts `sub` claim
- `getDaycareId()`: Extracts `daycare_id` claim
- `userIdFlow`: Reactive `StateFlow<String?>` for user ID
- `daycareIdFlow`: Reactive `StateFlow<String?>` for daycare ID

---

## Data Sources

### Kids Cache

**Location:** `KidsViewModel.kids` (StateFlow<List<Kid>>)

**Populated by:**
- `KidsViewModel.loadKids(daycareId)` → `KidsRepository.loadKids(daycareId)` → `KidsApiService.getKids(daycareId)`

**Used in:**
- `MessagingRepositoryImpl.observeContactsInMyGroups(ContactType.PARENT, ...)` - to find parents of kids in shared groups

### Educators Cache

**Location:** `EducatorsListViewModel.educators` (StateFlow<List<Educator>>)

**Populated by:**
- `EducatorsListViewModel.load(daycareId)` → `EducatorRepository.getEducators(daycareId, search)` → `EducatorApiService.getEducators(daycareId, search)`

**Used in:**
- `MessagingRepositoryImpl.observeContactsInMyGroups(ContactType.EDUCATOR, ...)` - to find educators in shared groups
- `MessagingRepositoryImpl.observeAllEducatorsExcept(...)` - to show all educators except self (for educator role)

---

## Implementation Details

### Repository Methods

**Location:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`

1. **`observeContactsInMyGroups(type, myGroupIds, myUserId)`**
   - Filters contacts by shared groups
   - Excludes self (`myUserId`)
   - Works for both PARENT and EDUCATOR contact types

2. **`observeAllEducatorsExcept(myUserId)`**
   - Returns all educators in daycare except self
   - No group filtering (shows all educators)
   - Used only for educator role in Educators tab

### ViewModel Flow Selection

**Location:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`

**Contacts Flow Logic** (`MessagingViewModel.kt:36-73`):

```kotlin
when (filter) {
    ConversationType.PARENT -> {
        // Both roles: Show only parents who share my groupIds
        repository.observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)
    }
    ConversationType.EDUCATOR -> {
        when (session.role) {
            UserRole.EDUCATOR -> {
                // Educator: Show ALL educators except self
                repository.observeAllEducatorsExcept(session.userId)
            }
            UserRole.PARENT -> {
                // Parent: Show only educators from my groups
                repository.observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)
            }
        }
    }
    ConversationType.GROUP -> emptyFlow() // Groups use inbox, not contacts
}
```

---

## Guardrails

### Placeholder User ID Detection

**Location:** `MessagingViewModel.kt:41-43`

If `session.userId` is a placeholder (`"current_user"` or `"unknown_parent"`), the contacts flow returns empty to prevent showing everyone.

### Empty Group IDs Check

**Location:** `MessagingRepositoryImpl.kt:110-112`

If `myGroupIds` is empty, the repository returns an empty list (doesn't show all contacts).

---

## Debugging

### Logging Points

1. **Session Updates** (`MainActivity.kt:179, 217, 227`)
   - Log: `SessionUpdate: role=<>, userId=<>, groupIds=<>, daycareId=<>`

2. **ViewModel Filter Selection** (`MessagingViewModel.kt:46`)
   - Log: `ContactsSelect: filter=<>, role=<>, userId=<>, groupIds=<>, size(kids)=<>, size(educators)=<>`

3. **Repository Filtering** (`MessagingRepositoryImpl.kt`)
   - Log: `observeContactsInMyGroups: type=<>, myGroupIds=<>, myUserId=<>`
   - Log: `Source sizes: kidsCache.size=<>, educatorsCache.size=<>`
   - Log: `Final parent IDs: [...]` or `Final educator IDs: [...]`

4. **All Educators Except** (`MessagingRepositoryImpl.kt:180-183`)
   - Log: `observeAllEducatorsExcept: myUserId=<>, educatorsCache.size=<>`
   - Log: `All educator IDs in cache: [...]`
   - Log: `Final educator IDs: [...]`

---

## Testing

### Unit Tests

**Location:** `app/src/test/java/fi/kidozz/app/features/messaging/`

1. **`MessagingRepositoryImplTest.kt`**
   - `observeAllEducatorsExcept_returns_all_educators_except_self()` - Verifies educator directory functionality
   - `educator_parents_tab_shows_only_parents_from_educator_groups()` - Verifies educator → parents filtering
   - `parent_session_derivation_multiple_groups_returns_only_parent_groups()` - Verifies parent group derivation
   - `self_exclusion_works_for_all_contact_types()` - Verifies self-exclusion in all flows

2. **`MessagingViewModelTest.kt`**
   - `educator_educators_tab_shows_all_educators_except_self()` - Verifies role × tab matrix
   - `educator_parents_tab_shows_only_parents_from_educator_groups()` - Verifies role × tab matrix
   - `parent_educators_tab_shows_only_educators_from_parent_groups()` - Verifies role × tab matrix
   - `parent_parents_tab_shows_only_parents_from_parent_groups()` - Verifies role × tab matrix

### Manual Verification

**Location:** `backend/docs/manual_checks/messaging_sanity.sql`

Run SQL queries to verify:
- Educator group assignments (Jessica id=27, Sarah id=24)
- Parent-kid relationships (Sara Johnson id=10, kids 19/20)
- Group memberships
- Daycare ID consistency

---

## Acceptance Criteria

### Login as Jessica (id=27)
- ✅ Educators tab: All educators except 27; 24 (Sarah) is visible
- ✅ Parents tab: Only parents of kids in group 7 (includes parent 10)

### Login as Parent 10
- ✅ Parents tab: Only parents who share group 7 (includes parent 11, excludes 10)
- ✅ Educators tab: Only educators assigned to group 7 (includes 25, 28; excludes others)

### Session Values
- ✅ Logs show correct `userId` and `groupIds` for each session
- ✅ No hardcoded IDs or daycare IDs
- ✅ `userId` comes from JWT token (for parents) or educator.id (for educators)
- ✅ `groupIds` computed correctly from user's actual group memberships

---

## Files Modified

1. **`app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`**
   - Added JWT decoding and `userId`/`daycareId` extraction

2. **`app/src/main/java/fi/kidozz/app/MainActivity.kt`**
   - Removed hardcoded IDs and daycare
   - Fixed educator session to set `userId` from `educator.id`
   - Fixed parent session to compute `groupIds` from only their kids
   - Added reactive session updates

3. **`app/src/main/java/fi/kidozz/app/features/messaging/domain/repo/MessagingRepository.kt`**
   - Added `observeAllEducatorsExcept()` method

4. **`app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`**
   - Implemented `observeAllEducatorsExcept()`
   - Added detailed logging

5. **`app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`**
   - Added role-aware branching for Educators tab
   - Added logging for cache sizes

6. **`app/src/main/java/fi/kidozz/app/features/dashboard/EducatorViewModel.kt`**
   - Added `loadCurrentEducatorById()` to load by ID from token
   - Deprecated `loadCurrentEducatorByDaycare()` (kept for backward compatibility)

7. **`app/src/test/java/fi/kidozz/app/features/messaging/data/MessagingRepositoryImplTest.kt`**
   - Added tests for `observeAllEducatorsExcept()`
   - Added tests for educator → parents filtering
   - Added tests for parent group derivation
   - Added tests for self-exclusion

8. **`app/src/test/java/fi/kidozz/app/features/messaging/ui/MessagingViewModelTest.kt`**
   - Added tests for role × tab matrix (4 combinations)

9. **`backend/docs/manual_checks/messaging_sanity.sql`**
   - Created SQL verification queries

---

**Document End**

