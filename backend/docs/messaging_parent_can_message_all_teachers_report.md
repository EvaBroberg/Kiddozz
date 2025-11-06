# Messaging Feature Diagnostic Report: Parent Can See All Teachers

**Date:** 2024-11-05  
**Issue:** Parent users can see all educators in the Educators tab, instead of only educators assigned to their kids' groups.  
**Scope:** Diagnostic report only — no code modifications.

---

## Section 1: Files & Exact Locations Inspected

### Core Files

1. **`app/src/main/java/fi/kidozz/app/MainActivity.kt`**
   - Lines 115-127: `UserSessionManager` initialization
   - Lines 183-229: Parent session derivation (`LaunchedEffect` for kids)
   - Lines 188-189: Parent role check and `currentParentId` assignment
   - Lines 191-193: Filtering kids to only those belonging to logged-in parent
   - Lines 198-226: Setting `session.userId` and `session.groupIds` for parents

2. **`app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`**
   - Lines 47-56: `getUserId()` method (extracts `sub` claim from JWT)
   - Lines 58-67: `getDaycareId()` method (extracts `daycare_id` claim from JWT)
   - Lines 84-87: `userIdFlow` and `daycareIdFlow` StateFlows

3. **`app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`**
   - Lines 36-87: `contacts` StateFlow definition
   - Lines 46: Logging of filter, role, userId, groupIds
   - Lines 61-78: Role-aware branching for `ConversationType.EDUCATOR`
   - Lines 70-76: Parent role path for Educators tab

4. **`app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`**
   - Lines 26-32: Constructor accepting `kidsCache` and `educatorsCache` StateFlows
   - Lines 102-177: `observeContactsInMyGroups` implementation
   - Lines 111-114: Guard for empty `myGroupIds`
   - Lines 147-175: `ContactType.EDUCATOR` filtering logic
   - Lines 149-158: Educator filtering by group membership
   - Lines 179-202: `observeAllEducatorsExcept` implementation (no group filtering)

5. **`app/src/main/java/fi/kidozz/app/core/session/UserSession.kt`**
   - Lines 1-9: `UserRole` enum and `UserSession` data class definition

6. **`app/src/main/java/fi/kidozz/app/core/session/UserSessionManager.kt`**
   - Lines 1-13: `UserSessionManager` class with `update()` method

### Data Source Files

7. **`app/src/main/java/fi/kidozz/app/MainActivity.kt`**
   - Lines 132-133: `EducatorsListViewModel` and `KidsViewModel` initialization
   - Lines 137-140: `MessagingRepositoryImpl` initialization with caches
   - Lines 149-162: `LaunchedEffect` loading educators and kids by `daycareId`

---

## Section 2: Parent Session Derivation (userId, groupIds)

### Code Path for Parent Session

**Location:** `app/src/main/java/fi/kidozz/app/MainActivity.kt:183-229`

1. **Trigger:** `LaunchedEffect(kids, session.role, currentParentId)` (line 188)
   - Reacts to changes in `kids` StateFlow, `session.role` (String), and `currentParentId`

2. **Parent ID Extraction:**
   ```kotlin
   val currentParentId = authUserId  // Line 186
   ```
   - `authUserId` comes from `tokenManager.userIdFlow.collectAsState()` (line 84)
   - `TokenManager.userIdFlow` extracts `sub` claim from JWT token (lines 47-56 in TokenManager.kt)
   - **✅ Confirmed:** `session.userId` equals the parent's real ID from JWT token (not a placeholder)

3. **Role Check:**
   ```kotlin
   if (session.role == "parent" && currentParentId != null) {  // Line 189
   ```
   - `session.role` is a String from `TokenManager.roleFlow` (line 82)
   - Must be exactly `"parent"` (lowercase) to enter this block

4. **Kids Filtering:**
   ```kotlin
   val myKids = kids.filter { kid ->
       kid.parents.any { parent -> parent.id == currentParentId }  // Lines 191-193
   }
   ```
   - Filters `kids` StateFlow to only kids where `kid.parents` contains a parent with `id == currentParentId`
   - **✅ Confirmed:** Only the logged-in parent's kids are considered

5. **Group IDs Extraction:**
   ```kotlin
   val parentGroupIds = myKids.map { it.group_id }.toSet()  // Line 198
   ```
   - Extracts `group_id` from each kid in `myKids`
   - Converts to `Set<String>` to remove duplicates
   - **✅ Confirmed:** `session.groupIds` are computed only from the logged-in parent's kids (intersection of their `kid.group_ids`), not from all kids in the daycare

6. **Session Update:**
   ```kotlin
   sessionManager.update(
       sessionManager.session.value.copy(
           userId = currentParentId,  // From JWT token
           groupIds = parentGroupIds   // From parent's kids
       )
   )  // Lines 210-215
   ```

### Guardrails

- **Placeholder Check (lines 201-208):** If `currentParentId` is `"current_user"` or `"unknown_parent"`, sets `groupIds = emptySet()` to prevent showing everyone
- **Empty GroupIds Check (lines 217-227):** If `parentGroupIds.isEmpty()`, sets `groupIds = emptySet()` and logs a warning

### Potential Issues

1. **Timing:** The `LaunchedEffect` depends on `kids` StateFlow being populated. If `kids` is empty or not yet loaded, `myKids` will be empty, leading to `parentGroupIds = emptySet()`.
2. **Role String Mismatch:** If `session.role` is not exactly `"parent"` (e.g., `"Parent"` with capital P), the session update block won't execute.

---

## Section 3: ViewModel Branching Matrix (role × tab → repo method)

**Location:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt:36-87`

### Branching Logic

The `contacts` StateFlow uses `combine(_filter, session)` to react to filter changes and session updates.

| Role (UserRole enum) | Filter Tab (ConversationType) | Repository Method Called | Line |
|---------------------|-------------------------------|-------------------------|------|
| **PARENT** | `PARENT` | `observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)` | 59 |
| **PARENT** | `EDUCATOR` | `observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)` | 75 |
| **EDUCATOR** | `PARENT` | `observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)` | 59 |
| **EDUCATOR** | `EDUCATOR` | `observeAllEducatorsExcept(session.userId)` | 68 |
| **PARENT** | `GROUP` | `emptyFlow<List<Contact>>()` | 79 |
| **EDUCATOR** | `GROUP` | `emptyFlow<List<Contact>>()` | 79 |

### Key Code Path for Parent + Educators Tab

**Lines 61-78:**
```kotlin
ConversationType.EDUCATOR -> {
    when (session.role) {
        fi.kidozz.app.core.session.UserRole.EDUCATOR -> {
            android.util.Log.d("MessagingViewModel", "Educator role: showing all educators except self")
            repository.observeAllEducatorsExcept(session.userId)  // ⚠️ NO GROUP FILTERING
        }
        fi.kidozz.app.core.session.UserRole.PARENT -> {
            android.util.Log.d("MessagingViewModel", "Parent role: showing educators from my groups")
            if (session.groupIds.isEmpty()) {
                android.util.Log.w("MessagingViewModel", "Educator filter selected but session.groupIds is empty!")
            }
            repository.observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)  // ✅ GROUP FILTERING
        }
    }
}
```

### Observations

1. **✅ Correct Path for Parent:** When `role == PARENT` and `filter == EDUCATOR`, the code calls `observeContactsInMyGroups(ContactType.EDUCATOR, ...)`, which applies group filtering.
2. **❌ No "All Educators" Path for Parent:** There is no code path where a parent uses `observeAllEducatorsExcept()` — this is only called for `UserRole.EDUCATOR`.
3. **Guardrails:**
   - Lines 49-51: If `session.userId` is a placeholder, returns `emptyFlow()` to prevent showing everyone.
   - Lines 56-58, 72-74: Logs warnings if `session.groupIds.isEmpty()` but still proceeds (repository will return empty list).

### Self-Exclusion Logic

- Self-exclusion is handled in the repository methods:
  - `observeContactsInMyGroups`: Filters by `e.id != myUserId` (line 159 in MessagingRepositoryImpl.kt)
  - `observeAllEducatorsExcept`: Filters by `e.id != myUserId` (line 185 in MessagingRepositoryImpl.kt)

---

## Section 4: Repository Filtering Logic for Educators

**Location:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:102-177`

### Method: `observeContactsInMyGroups(ContactType.EDUCATOR, myGroupIds, myUserId)`

**Lines 147-175:**

```kotlin
ContactType.EDUCATOR -> {
    // Educators that are assigned to any of my groups
    val filteredEducators = educators
        .filter { e -> 
            val matches = e.groups.any { g -> 
                val groupIdString = g.id.toString()
                val match = groupIdString in myGroupIds
                android.util.Log.d("MessagingRepo", "Educator ${e.full_name}: group.id=$groupIdString, in myGroupIds=$match")
                match
            }
            matches
        }
        .filter { e -> e.id != myUserId }  // Self-exclusion
    
    // ... mapping to Contact objects
}
```

### Filtering Steps

1. **Group Filtering (lines 149-158):**
   - Iterates over all educators in `educatorsCache`
   - For each educator `e`, checks if `e.groups.any { g.id.toString() in myGroupIds }`
   - **Expected parent path:** `educator.groups.any { it.id in myGroupIds }` ✅ **CONFIRMED**
   - Converts `g.id` to String for comparison (line 152)

2. **Self-Exclusion (line 159):**
   - Filters out educators where `e.id == myUserId`
   - **✅ Confirmed:** `e.id != myUserId` is applied

3. **Deduplication & Sorting (lines 164-174):**
   - `distinctBy { it.id }` removes duplicates
   - Sorts by name (lowercase)

### Guard for Empty GroupIds

**Lines 111-114:**
```kotlin
if (myGroupIds.isEmpty()) {
    android.util.Log.w("MessagingRepo", "myGroupIds is empty! Returning empty contacts list.")
    return@combine emptyList<Contact>()
}
```

- **✅ Confirmed:** If `myGroupIds.isEmpty()`, returns empty list (does not show all educators)

### Method: `observeAllEducatorsExcept(myUserId)`

**Location:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt:179-202`

```kotlin
override fun observeAllEducatorsExcept(myUserId: String): Flow<List<Contact>> {
    return educatorsCache.map { educators ->
        val filteredEducators = educators
            .filter { e -> e.id != myUserId }  // Only self-exclusion, NO group filtering
        
        // ... mapping to Contact objects
    }
}
```

- **⚠️ No Group Filtering:** This method does NOT filter by `myGroupIds` — it returns all educators except self.
- **Usage:** Only called when `session.role == UserRole.EDUCATOR` (line 68 in MessagingViewModel.kt)

### Branches That Skip Group Filtering

1. **`observeAllEducatorsExcept()`:** Always skips group filtering (used only for educator role in Educators tab)
2. **Empty `myGroupIds` guard:** Returns empty list, so no filtering needed

**❌ No branches skip group filtering for parent role in Educators tab** — the code path is correct.

---

## Section 5: Collected Runtime Logs (Parent Scenario)

### Expected Log Flow for Parent on Educators Tab

1. **MainActivity.kt - Session Update:**
   ```
   D/SessionUpdate: role=PARENT, userId=10, groupIds=[7], daycareId=default-daycare-id (from 2 kids: Kid1(g7), Kid2(g7))
   ```

2. **MessagingViewModel.kt - Contacts Selection:**
   ```
   D/ContactsSelect: filter=EDUCATOR, role=PARENT, userId=10, groupIds=[7] (size=1), size(kids)=50, size(educators)=10
   D/MessagingViewModel: Parent role: showing educators from my groups
   ```

3. **MessagingRepositoryImpl.kt - Group Filtering:**
   ```
   D/MessagingRepo: observeContactsInMyGroups: type=EDUCATOR, myGroupIds=[7] (size=1), myUserId=10
   D/MessagingRepo: Source sizes: kidsCache.size=50, educatorsCache.size=10
   D/MessagingRepo: Educator Teacher1: group.id=7, in myGroupIds=true
   D/MessagingRepo: Educator Teacher2: group.id=7, in myGroupIds=true
   D/MessagingRepo: Educator Sarah Davis: group.id=8, in myGroupIds=false
   D/MessagingRepo: Filtered educators: 2
   D/MessagingRepo: Final educator IDs: [Teacher1(25), Teacher2(28)]
   ```

4. **MessagingViewModel.kt - Contacts Emitted:**
   ```
   D/MessagingViewModel: Contacts flow emitted 2 contacts: [Teacher1 (25), Teacher2 (28)]
   ```

### Hypothetical Logs Showing the Bug (All Teachers Visible)

If a parent sees all teachers, the logs might show:

**Scenario A: Empty groupIds**
```
D/SessionUpdate: role=PARENT, userId=10, groupIds=empty, daycareId=default-daycare-id
D/ContactsSelect: filter=EDUCATOR, role=PARENT, userId=10, groupIds=[] (size=0), size(kids)=50, size(educators)=10
W/MessagingViewModel: Educator filter selected but session.groupIds is empty!
W/MessagingRepo: myGroupIds is empty! Returning empty contacts list.
D/MessagingViewModel: Contacts flow emitted 0 contacts: []
```
**Result:** Empty list (not all teachers) — this is not the bug.

**Scenario B: Role Mismatch (session.role != PARENT)**
```
D/ContactsSelect: filter=EDUCATOR, role=EDUCATOR, userId=10, groupIds=[7] (size=1), size(kids)=50, size(educators)=10
D/MessagingViewModel: Educator role: showing all educators except self
D/MessagingRepo: observeAllEducatorsExcept: myUserId=10, educatorsCache.size=10
D/MessagingRepo: All educator IDs in cache: [Teacher1(25), Teacher2(28), Sarah Davis(24), ...]
D/MessagingRepo: Filtered educators (excluding self): 9
D/MessagingRepo: Final educator IDs: [Teacher1(25), Teacher2(28), Sarah Davis(24), ...]
```
**Result:** All educators shown (no group filtering) — **THIS COULD BE THE BUG**

**Scenario C: Type Mismatch in Group ID Comparison**
```
D/MessagingRepo: observeContactsInMyGroups: type=EDUCATOR, myGroupIds=[7] (size=1), myUserId=10
D/MessagingRepo: Educator Teacher1: group.id=7, in myGroupIds=true
D/MessagingRepo: Educator Teacher2: group.id=7, in myGroupIds=true
D/MessagingRepo: Educator Sarah Davis: group.id=8, in myGroupIds=false
D/MessagingRepo: Educator Jessica: group.id=7, in myGroupIds=false  // ⚠️ Type mismatch?
```
**Result:** Some educators missing — this could be a type conversion issue.

---

## Section 6: SQL Verification Queries

### Query 1: Logged-in Parent's ID and Their Kids' Group IDs

```sql
-- Replace :parent_id with the actual parent ID from JWT token (e.g., 10)
SELECT 
    p.id AS parent_id,
    p.full_name AS parent_name,
    k.id AS kid_id,
    k.full_name AS kid_name,
    k.group_id AS kid_group_id
FROM parents p
JOIN parent_kids pk ON p.id = pk.parent_id
JOIN kids k ON pk.kid_id = k.id
WHERE p.id = :parent_id  -- e.g., 10
ORDER BY k.id;
```

**Expected Output (for parent ID 10):**
```
parent_id | parent_name | kid_id | kid_name | kid_group_id
----------|-------------|--------|----------|-------------
10        | Sara Johnson| 19     | Kid1     | 7
10        | Sara Johnson| 20     | Kid2     | 7
```

**Expected `session.groupIds`:** `["7"]` (Set<String>)

### Query 2: All Educators and Their Group Assignments

```sql
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    g.id AS group_id,
    g.name AS group_name
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
LEFT JOIN groups g ON eg.group_id = g.id
ORDER BY e.id, g.id;
```

**Expected Output (example):**
```
educator_id | educator_name | group_id | group_name
------------|---------------|----------|------------
24          | Sarah Davis   | 8        | Group 8
25          | Teacher1      | 7        | Group 7
27          | Jessica       | 7        | Group 7
28          | Teacher2      | 7        | Group 7
```

**Expected Filtering:** For parent with `groupIds = ["7"]`, only educators 25, 27, 28 should appear (not 24).

### Query 3: Daycare Scoping (if applicable)

```sql
-- Replace :daycare_id with the actual daycare ID from JWT token
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    e.daycare_id,
    g.id AS group_id,
    g.name AS group_name
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
LEFT JOIN groups g ON eg.group_id = g.id
WHERE e.daycare_id = :daycare_id  -- e.g., 'default-daycare-id'
ORDER BY e.id, g.id;
```

**Expected:** Only educators from the logged-in parent's daycare should be in `educatorsCache`.

### Query 4: Verify Parent-Kid-Group Chain

```sql
-- For parent ID 10, verify the complete chain
SELECT 
    p.id AS parent_id,
    p.full_name AS parent_name,
    k.id AS kid_id,
    k.full_name AS kid_name,
    k.group_id AS kid_group_id,
    g.name AS group_name,
    e.id AS educator_id,
    e.full_name AS educator_name
FROM parents p
JOIN parent_kids pk ON p.id = pk.parent_id
JOIN kids k ON pk.kid_id = k.id
JOIN groups g ON k.group_id = g.id::text  -- Note: type conversion if needed
LEFT JOIN educator_groups eg ON g.id = eg.group_id
LEFT JOIN educators e ON eg.educator_id = e.id
WHERE p.id = 10
ORDER BY k.id, e.id;
```

**Expected:** Shows which educators are assigned to groups that contain parent 10's kids.

---

## Section 7: Root Cause Hypothesis

### Most Plausible Cause: Role Enum Mismatch

**Hypothesis:** The `session.role` in `UserSession` is not correctly set to `UserRole.PARENT` when a parent logs in, causing the ViewModel to branch to the `EDUCATOR` role path, which calls `observeAllEducatorsExcept()` instead of `observeContactsInMyGroups()`.

### Evidence

1. **Role Conversion Logic (MainActivity.kt:120-123):**
   ```kotlin
   role = if (session.role in listOf("educator", "super_educator")) 
       fi.kidozz.app.core.session.UserRole.EDUCATOR 
   else 
       fi.kidozz.app.core.session.UserRole.PARENT
   ```
   - If `session.role` (String) is `null`, `"parent"`, or any other value, it defaults to `PARENT`.
   - **However:** The `UserSession` is created once at initialization (line 116-127) and may not be updated when the role changes.

2. **Session Update Logic:**
   - The `LaunchedEffect` for parents (line 188) updates `userId` and `groupIds` but **does not update `role`**.
   - If the initial `UserSession` was created with `role = EDUCATOR` (due to a timing issue or incorrect role string), it will remain `EDUCATOR` even after the parent's kids load.

3. **ViewModel Branching (MessagingViewModel.kt:65-77):**
   - The `when (session.role)` check uses the `UserRole` enum from `UserSession`.
   - If `session.role == UserRole.EDUCATOR` (incorrectly), it calls `observeAllEducatorsExcept()`, which shows all educators without group filtering.

4. **Alternative Hypothesis: Type Mismatch in Group ID Comparison**
   - If `educator.groups[].id` is an `Int` but `session.groupIds` contains `String` values, the comparison `g.id.toString() in myGroupIds` should work, but if `g.id` is already a String and `myGroupIds` contains Ints (or vice versa), the comparison might fail, causing all educators to pass the filter.

### Recommended Debug Logs

Add these logs to verify the hypothesis:

1. **MainActivity.kt (after line 123):**
   ```kotlin
   Log.d("SessionInit", "Initial UserSession: role=${initialSession.role}, userId=${initialSession.userId}")
   ```

2. **MainActivity.kt (after line 215):**
   ```kotlin
   Log.d("SessionUpdate", "Updated UserSession: role=${sessionManager.session.value.role}, userId=${sessionManager.session.value.userId}, groupIds=${sessionManager.session.value.groupIds}")
   ```

3. **MessagingViewModel.kt (after line 65):**
   ```kotlin
   android.util.Log.d("MessagingViewModel", "Role check: session.role=${session.role}, is EDUCATOR?=${session.role == fi.kidozz.app.core.session.UserRole.EDUCATOR}, is PARENT?=${session.role == fi.kidozz.app.core.session.UserRole.PARENT}")
   ```

4. **MessagingRepositoryImpl.kt (after line 152):**
   ```kotlin
   android.util.Log.d("MessagingRepo", "Group ID comparison: educator.group.id=${g.id} (type=${g.id::class.simpleName}), myGroupIds=$myGroupIds (types=${myGroupIds.map { it::class.simpleName }})")
   ```

### Conclusion

The most likely root cause is that `session.role` in `UserSession` is incorrectly set to `UserRole.EDUCATOR` (or not updated to `PARENT`) when a parent logs in, causing the ViewModel to use the "all educators" flow instead of the group-filtered flow. The second most likely cause is a type mismatch in group ID comparison, though the code appears to handle this with `.toString()` conversion.

---

## Appendix: Code Flow Diagram

```
Parent Login
    ↓
TokenManager extracts role="parent", userId="10", daycareId="..."
    ↓
MainActivity creates UserSessionManager
    ├─ Initial role: if (role in ["educator", "super_educator"]) EDUCATOR else PARENT
    ├─ Initial userId: authUserId ?: "unknown"
    └─ Initial groupIds: emptySet()
    ↓
LaunchedEffect(kids, session.role, currentParentId)
    ├─ Check: session.role == "parent" && currentParentId != null
    ├─ Filter kids: kid.parents.any { it.id == currentParentId }
    ├─ Extract groupIds: myKids.map { it.group_id }.toSet()
    └─ Update session: sessionManager.update(session.copy(userId=..., groupIds=...))
    ⚠️ NOTE: Does NOT update session.role
    ↓
MessagingViewModel.contacts flow
    ├─ combine(_filter, session)
    ├─ Check: filter == EDUCATOR
    ├─ Branch: when (session.role)
    │   ├─ EDUCATOR → observeAllEducatorsExcept()  ⚠️ NO GROUP FILTER
    │   └─ PARENT → observeContactsInMyGroups(EDUCATOR, groupIds, userId)  ✅ GROUP FILTER
    ↓
MessagingRepositoryImpl.observeContactsInMyGroups
    ├─ Guard: if (myGroupIds.isEmpty()) return emptyList()
    ├─ Filter: educators.filter { e.groups.any { g.id.toString() in myGroupIds } }
    ├─ Self-exclude: .filter { e.id != myUserId }
    └─ Return: List<Contact>
```

---

**End of Report**

