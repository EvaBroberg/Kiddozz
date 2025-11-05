# Messaging Runtime Diagnostics Report

**Date:** 2025-01-10  
**Scope:** Android Messaging Feature - Session Management & Contact Filtering  
**Status:** 🔍 Diagnostic Analysis Only (No Code Changes)

---

## Section 1: Files & Locations Inspected

### Core Session Management

1. **UserSession Model**
   - **File:** `app/src/main/java/fi/kidozz/app/core/session/UserSession.kt`
   - **Lines:** 1-9
   - **Content:** Defines `UserSession` data class with `userId: String`, `role: UserRole`, `groupIds: Set<String>`

2. **UserSessionManager**
   - **File:** `app/src/main/java/fi/kidozz/app/core/session/UserSessionManager.kt`
   - **Lines:** 1-13
   - **Content:** Manages `UserSession` as a `StateFlow` with `update()` method

3. **MainActivity - Session Initialization & Updates**
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
   - **Lines:** 114-162 (Session initialization and educator updates)
   - **Lines:** 165-212 (Parent session updates)
   - **Lines:** 152-163 (Educator session update logic - **CRITICAL: userId not set**)

### Messaging Components

4. **MessagingViewModel**
   - **File:** `app/src/main/java/fi/kidozz/app/features/messaging/ui/MessagingViewModel.kt`
   - **Lines:** 36-65 (contacts flow selection logic)
   - **Lines:** 38-63 (filter → repository mapping)

5. **MessagingRepositoryImpl**
   - **File:** `app/src/main/java/fi/kidozz/app/features/messaging/data/repo/MessagingRepositoryImpl.kt`
   - **Lines:** 102-173 (observeContactsInMyGroups implementation)
   - **Lines:** 106-173 (filtering logic for PARENT and EDUCATOR types)

6. **Data Sources**
   - **File:** `app/src/main/java/fi/kidozz/app/features/dashboard/EducatorViewModel.kt`
   - **Lines:** 25-46 (loadCurrentEducatorByDaycare - hardcoded to "Jessica")
   - **File:** `app/src/main/java/fi/kidozz/app/features/dashboard/EducatorsListViewModel.kt`
   - **Lines:** 25-38 (load method - populates educators StateFlow)
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
   - **Lines:** 135-137 (messagingRepository construction with kids and educators StateFlows)

### Data Models

7. **Educator Model**
   - **File:** `app/src/main/java/fi/kidozz/app/data/models/Educator.kt`
   - **Lines:** 16-25
   - **Key:** `id: String`, `groups: List<Group>`

8. **Kid Model**
   - **File:** `app/src/main/java/fi/kidozz/app/data/models/Kid.kt`
   - **Lines:** 8-19
   - **Key:** `id: String`, `group_id: String`, `parents: List<Parent>`

9. **Parent Model**
   - **File:** `app/src/main/java/fi/kidozz/app/data/models/Parent.kt`
   - **Lines:** 7-12
   - **Key:** `id: String`

---

## Section 2: Auth & Session Derivation

### For Educators

**Current Implementation:**

1. **Session Initialization** (`MainActivity.kt:114-125`)
   ```kotlin
   val sessionManager = remember {
       UserSessionManager(
           UserSession(
               userId = "current_user", // ❌ TODO: Get from auth
               role = if (session.role in listOf("educator", "super_educator")) 
                   UserRole.EDUCATOR else UserRole.PARENT,
               groupIds = emptySet() // Will be updated when educator loads
           )
       )
   }
   ```
   - **Issue:** `userId` is initialized to `"current_user"` placeholder, never updated for educators

2. **Educator Data Loading** (`MainActivity.kt:142-150`)
   ```kotlin
   LaunchedEffect(daycareId) {
       if (daycareId.isNotBlank()) {
           educatorsListViewModel.load(daycareId)
           educatorViewModel.loadCurrentEducatorByDaycare(daycareId)
           kidsViewModel.loadKids(daycareId)
       }
   }
   ```
   - Loads educators list and current educator (hardcoded to "Jessica" in `EducatorViewModel.kt:33`)

3. **Session Update for Educators** (`MainActivity.kt:152-163`)
   ```kotlin
   LaunchedEffect(currentEducator, session.role) {
       val educator = currentEducator
       if (educator != null && session.role in listOf("educator", "super_educator")) {
           val educatorGroupIds = educator.groups.map { it.id.toString() }.toSet()
           sessionManager.update(
               sessionManager.session.value.copy(groupIds = educatorGroupIds)
               // ❌ MISSING: userId = educator.id
           )
           Log.d("UserSession", "Updated session with educator groups: $educatorGroupIds (from educator ${educator.full_name})")
       }
   }
   ```
   - **Issue:** Only updates `groupIds`, not `userId`
   - **Result:** `session.userId` remains `"current_user"` for educators
   - **Expected:** `session.userId = educator.id` (e.g., `"27"` for Jessica)

**Code Path:**
- `EducatorViewModel.loadCurrentEducatorByDaycare()` → `EducatorRepository.getEducatorByName("default-daycare-id", "Jessica")`
- Returns `Educator` with `id: String` (e.g., `"27"`)
- `MainActivity.kt:154-162` → Updates only `groupIds`, not `userId`

**Evidence:**
- Line 159: `sessionManager.session.value.copy(groupIds = educatorGroupIds)` - no `userId` parameter
- Line 117: Initial `userId = "current_user"` - never changed for educators

### For Parents

**Current Implementation:**

1. **Session Initialization** (`MainActivity.kt:114-125`)
   - Same as educators: `userId = "current_user"` initially

2. **Parent ID Derivation** (`MainActivity.kt:168-170`)
   ```kotlin
   // TODO: Get from TokenManager claims or /me endpoint - for now accept from manual flow
   // For testing: parent ID 10 (from the scenario)
   val currentParentId = "10" // TODO: Extract from JWT token or auth flow
   ```
   - **Issue:** Hardcoded to `"10"` instead of extracting from JWT token or auth flow

3. **Session Update for Parents** (`MainActivity.kt:172-212`)
   ```kotlin
   LaunchedEffect(kids, session.role, currentParentId) {
       if (session.role == "parent") {
           val myKids = kids.filter { kid ->
               kid.parents.any { parent -> parent.id == currentParentId }
           }
           
           val parentGroupIds = myKids.mapNotNull { it.group_id?.toString() }.toSet()
           
           if (parentGroupIds.isNotEmpty()) {
               sessionManager.update(
                   sessionManager.session.value.copy(
                       userId = currentParentId, // ✅ Set to "10"
                       groupIds = parentGroupIds
                   )
               )
           }
       }
   }
   ```
   - **Status:** ✅ `userId` is set correctly to `currentParentId` ("10")
   - **Status:** ✅ `groupIds` computed from only that parent's kids

**Code Path:**
- `KidsViewModel.kids` → StateFlow of all kids
- Filter to `myKids` where `kid.parents.any { it.id == currentParentId }`
- Extract `group_id` from `myKids` → `parentGroupIds`
- Update session with `userId = currentParentId` and `groupIds = parentGroupIds`

**Evidence:**
- Line 194-198: Both `userId` and `groupIds` are updated correctly for parents
- Line 170: `currentParentId` is hardcoded, should come from auth

---

## Section 3: Messaging Flow Selection Matrix

### Flow Selection Logic (`MessagingViewModel.kt:36-65`)

The `contacts` StateFlow uses `combine(_filter, session)` to select the appropriate repository function:

| Role | Filter Tab | Repository Function | Cache Used | Filters Applied |
|------|------------|-------------------|------------|-----------------|
| **Parent** | Parents | `observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)` | `kidsCache` | 1. Filter kids where `kid.group_id in session.groupIds`<br>2. Extract parents from those kids<br>3. Exclude `session.userId`<br>4. Deduplicate by parent ID<br>5. Sort by name |
| **Parent** | Educators | `observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)` | `educatorsCache` | 1. Filter educators where `educator.groups.any { it.id in session.groupIds }`<br>2. Exclude `session.userId`<br>3. Deduplicate by educator ID<br>4. Sort by name |
| **Educator** | Parents | `observeContactsInMyGroups(ContactType.PARENT, session.groupIds, session.userId)` | `kidsCache` | 1. Filter kids where `kid.group_id in session.groupIds`<br>2. Extract parents from those kids<br>3. Exclude `session.userId`<br>4. Deduplicate by parent ID<br>5. Sort by name |
| **Educator** | Educators | `observeContactsInMyGroups(ContactType.EDUCATOR, session.groupIds, session.userId)` | `educatorsCache` | 1. Filter educators where `educator.groups.any { it.id in session.groupIds }`<br>2. Exclude `session.userId`<br>3. Deduplicate by educator ID<br>4. Sort by name |

### Key Observations

1. **Same logic for both roles:** The ViewModel doesn't branch by role - it uses the same `observeContactsInMyGroups` function for both parents and educators
2. **Filtering depends on `session.groupIds`:** Both roles only see contacts from their own groups
3. **Self-exclusion depends on `session.userId`:** The repository filters out `session.userId` from the contact list

### Data Sources

**Kids Cache:**
- **Source:** `KidsViewModel.kids` (StateFlow<List<Kid>>)
- **Populated by:** `KidsRepository.loadKids(daycareId)` → `KidsApiService.getKids(daycareId)`
- **Location:** `MainActivity.kt:137` → `messagingRepository = MessagingRepositoryImpl(..., kidsViewModel.kids, ...)`

**Educators Cache:**
- **Source:** `EducatorsListViewModel.educators` (StateFlow<List<Educator>>)
- **Populated by:** `EducatorsListViewModel.load(daycareId)` → `EducatorRepository.getEducators(daycareId, search)`
- **Location:** `MainActivity.kt:137` → `messagingRepository = MessagingRepositoryImpl(..., educatorsListViewModel.educators)`

**Daycare ID:**
- **Hardcoded:** `"default-daycare-id"` (`MainActivity.kt:143`)
- **Used for:** All API calls (kids, educators, groups)

---

## Section 4: Findings for Jessica(27)/Sarah(24)/Sara(10) Scenario

### Scenario Setup

**Logged in as:** Jessica (educator, id=27)
- Jessica is assigned to group 7
- Expected: Educators tab should show only educators from group 7 (excluding Jessica)
- Expected: Parents tab should show parents whose kids are in group 7

**Issues Observed:**

1. **Educators tab shows all educators including Jessica** ❌
2. **Sarah Davis (id=24) is missing from Educators tab** ❌
3. **Sara Johnson (parent id=10) is missing from Parents tab** ❌

### Root Cause Analysis

#### Issue 1: Jessica appears in Educators tab (self not excluded)

**Suspected Cause:** `session.userId` is `"current_user"` instead of `"27"`

**Evidence:**
- `MainActivity.kt:117`: Session initialized with `userId = "current_user"`
- `MainActivity.kt:158-159`: Only `groupIds` updated, not `userId`
- `MessagingRepositoryImpl.kt:156`: Filters with `.filter { e -> e.id != myUserId }`
- **Result:** `"current_user" != "27"`, so Jessica (id="27") is not filtered out

**Code Path:**
```
MainActivity.kt:117 → userId = "current_user"
MainActivity.kt:158 → sessionManager.update(session.copy(groupIds = ...)) // userId not changed
MessagingRepositoryImpl.kt:156 → educators.filter { e.id != "current_user" } // "27" != "current_user" → Jessica not excluded
```

**Fix Required:**
- `MainActivity.kt:158` should also set `userId = educator.id`

#### Issue 2: Sarah Davis (id=24) missing from Educators tab

**Possible Causes:**

1. **Sarah not in educatorsCache:**
   - `EducatorsListViewModel.load("default-daycare-id")` may not return Sarah
   - Check: Is Sarah returned by `/api/v1/educators?daycare_id=default-daycare-id`?

2. **Sarah's groups don't match session.groupIds:**
   - If Jessica's `session.groupIds = {"7"}` and Sarah is not assigned to group 7
   - `MessagingRepositoryImpl.kt:148-150`: `e.groups.any { g.id.toString() in myGroupIds }`
   - If Sarah is assigned to group 8, she won't match

3. **Sarah's group ID type mismatch:**
   - If `educator.groups[].id` is `Int` but converted to `String` incorrectly
   - `MessagingRepositoryImpl.kt:149`: `g.id.toString()` should handle this

**Most Likely Cause:** Sarah is not assigned to group 7 (Jessica's group), OR Sarah is not in the educators list returned by the API

**Code Path to Verify:**
```
EducatorsListViewModel.load() → API call → educatorsCache
MessagingRepositoryImpl.kt:146 → educators.filter { e.groups.any { g.id.toString() in {"7"} } }
```

#### Issue 3: Sara Johnson (parent id=10) missing from Parents tab

**Possible Causes:**

1. **Kids 19 and 20 not in kidsCache:**
   - `KidsViewModel.loadKids("default-daycare-id")` may not return kids 19, 20
   - Check: Are kids 19, 20 returned by `/api/v1/kids?daycare_id=default-daycare-id`?

2. **Kids 19, 20 have wrong group_id:**
   - If `kid.group_id != "7"`, they won't match Jessica's `session.groupIds = {"7"}`
   - `MessagingRepositoryImpl.kt:120-122`: `kid.group_id?.toString() in myGroupIds`

3. **Kids 19, 20 not linked to parent id=10:**
   - If `kid.parents` doesn't include parent with `id="10"`, Sara won't be extracted
   - `MessagingRepositoryImpl.kt:130`: `kidsInMyGroups.flatMap { k -> k.parents }`

4. **Type mismatch in parent ID:**
   - If parent `id` is `Int` but stored as `String`, or vice versa
   - `MessagingRepositoryImpl.kt:131`: `p.id != myUserId` (both are String)

**Most Likely Cause:** Kids 19, 20 are not in the kids list returned by the API, OR they have `group_id != "7"`, OR they're not linked to parent id="10"

**Code Path to Verify:**
```
KidsViewModel.loadKids() → API call → kidsCache
MessagingRepositoryImpl.kt:120 → kids.filter { kid.group_id?.toString() in {"7"} }
MessagingRepositoryImpl.kt:130 → kidsInMyGroups.flatMap { k.parents }
MessagingRepositoryImpl.kt:131 → parents.filter { p.id != "current_user" } // Should be "27" but is "current_user"
```

**Note:** Even if kids 19, 20 are correct, Sara (id="10") might be excluded if `session.userId` is incorrectly set (though this shouldn't affect educators viewing parents)

---

## Section 5: Suggested Minimal Probes

### Probe 1: Session Values at Messages Screen Open

**Location:** `MainActivity.kt` (after session updates)

**Log Statement:**
```kotlin
LaunchedEffect(sessionManager.session.value) {
    val s = sessionManager.session.value
    Log.d("DIAGNOSTIC", "Session: role=${s.role}, userId=${s.userId}, groupIds=${s.groupIds}")
}
```

**Expected Output for Jessica (id=27):**
```
Session: role=EDUCATOR, userId=27, groupIds=[7]
```

**Actual Expected Output (Bug Present):**
```
Session: role=EDUCATOR, userId=current_user, groupIds=[7]
```

### Probe 2: MessagingViewModel Filter Selection

**Location:** `MessagingViewModel.kt:36-65` (in `contacts` flow)

**Existing Log:** Already present at line 38
```kotlin
android.util.Log.d("MessagingViewModel", "Filter=$filter, role=${session.role}, userId=${session.userId}, groupIds=${session.groupIds} (size=${session.groupIds.size})")
```

**Expected Output:**
```
Filter=EDUCATOR, role=EDUCATOR, userId=27, groupIds=[7] (size=1)
```

**Actual Expected Output (Bug Present):**
```
Filter=EDUCATOR, role=EDUCATOR, userId=current_user, groupIds=[7] (size=1)
```

### Probe 3: Repository Filtering Inputs/Outputs

**Location:** `MessagingRepositoryImpl.kt:102-173` (in `observeContactsInMyGroups`)

**Existing Logs:** Already present at lines 107, 115, 123, 126, 151, 158

**Add Additional Log:**
```kotlin
// After filtering educators
android.util.Log.d("DIAGNOSTIC", "Repository: type=$type, myGroupIds=$myGroupIds, myUserId=$myUserId")
android.util.Log.d("DIAGNOSTIC", "Repository: educatorsCache.size=${educators.size}, kidsCache.size=${kids.size}")
android.util.Log.d("DIAGNOSTIC", "Repository: All educator IDs=${educators.map { "${it.full_name}(${it.id})" }}")
android.util.Log.d("DIAGNOSTIC", "Repository: Educator groups=${educators.map { "${it.full_name}: ${it.groups.map { g -> g.id.toString() }}" }}")
```

**Expected Output for Educators Tab:**
```
Repository: type=EDUCATOR, myGroupIds=[7], myUserId=27
Repository: educatorsCache.size=5, kidsCache.size=20
Repository: All educator IDs=[Jessica(27), Sarah(24), ...]
Repository: Educator groups=[Jessica(27): [7], Sarah(24): [8], ...]
Repository: Filtered educators: 2 (should exclude Jessica)
```

**Actual Expected Output (Bug Present):**
```
Repository: type=EDUCATOR, myGroupIds=[7], myUserId=current_user
Repository: All educator IDs=[Jessica(27), Sarah(24), ...]
Repository: Educator groups=[Jessica(27): [7], Sarah(24): [8], ...]
Repository: Filtered educators: 3 (includes Jessica because "27" != "current_user")
```

### Probe 4: Kids/Parents Extraction for Parents Tab

**Location:** `MessagingRepositoryImpl.kt:118-141`

**Add Log:**
```kotlin
android.util.Log.d("DIAGNOSTIC", "Parents Tab: Kids in my groups=${kidsInMyGroups.map { "${it.full_name}(g${it.group_id}, parents=${it.parents.map { p -> p.id }})" }}")
android.util.Log.d("DIAGNOSTIC", "Parents Tab: Extracted parents=${kidsInMyGroups.flatMap { it.parents }.map { "${it.full_name}(${it.id})" }}")
android.util.Log.d("DIAGNOSTIC", "Parents Tab: After self-exclusion=${kidsInMyGroups.flatMap { it.parents }.filter { it.id != myUserId }.map { "${it.full_name}(${it.id})" }}")
```

**Expected Output:**
```
Parents Tab: Kids in my groups=[Kid1(g7, parents=[10]), Kid2(g7, parents=[10])]
Parents Tab: Extracted parents=[Sara Johnson(10), Sara Johnson(10)]
Parents Tab: After self-exclusion=[Sara Johnson(10)]
```

---

## Section 6: SQL to Verify DB Facts

### Verify Jessica (id=27) Group Assignment

```sql
-- Check Jessica exists and her group assignments
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    e.email,
    eg.group_id,
    g.name AS group_name
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
LEFT JOIN groups g ON eg.group_id = g.id
WHERE e.id = 27
   OR e.full_name = 'Jessica';
```

**Expected Result:**
```
educator_id | educator_name | email          | group_id | group_name
------------+---------------+----------------+----------+------------
27          | Jessica       | jessica@...    | 7        | Group A (or similar)
```

### Verify Sarah Davis (id=24) Group Assignment

```sql
-- Check Sarah exists and her group assignments
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    e.email,
    eg.group_id,
    g.name AS group_name
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
LEFT JOIN groups g ON eg.group_id = g.id
WHERE e.id = 24
   OR e.full_name LIKE '%Sarah%' OR e.full_name LIKE '%Davis%';
```

**Expected Result:** Should show Sarah's group assignments. If she's in group 7, she should appear for Jessica. If she's in group 8, she won't appear.

### Verify Sara Johnson (parent id=10) and Kids 19, 20

```sql
-- Check Sara Johnson and her kids
SELECT 
    p.id AS parent_id,
    p.full_name AS parent_name,
    p.email AS parent_email,
    k.id AS kid_id,
    k.full_name AS kid_name,
    k.group_id AS kid_group_id,
    g.name AS group_name
FROM parents p
INNER JOIN parent_kids pk ON p.id = pk.parent_id
INNER JOIN kids k ON pk.kid_id = k.id
LEFT JOIN groups g ON k.group_id = g.id
WHERE p.id = 10
   OR p.full_name LIKE '%Sara%' OR p.full_name LIKE '%Johnson%'
ORDER BY k.id;
```

**Expected Result:**
```
parent_id | parent_name | kid_id | kid_name | kid_group_id | group_name
----------+-------------+--------+----------+--------------+------------
10        | Sara Johnson| 19     | Kid19    | 7            | Group A
10        | Sara Johnson| 20     | Kid20    | 7            | Group A
```

### Verify All Educators in Group 7

```sql
-- List all educators assigned to group 7
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    e.email,
    eg.group_id,
    g.name AS group_name
FROM educators e
INNER JOIN educator_groups eg ON e.id = eg.educator_id
INNER JOIN groups g ON eg.group_id = g.id
WHERE eg.group_id = 7
ORDER BY e.id;
```

**Expected Result:** Should show all educators assigned to group 7 (should include Jessica id=27, and possibly Sarah id=24 if she's also in group 7)

### Verify All Kids in Group 7

```sql
-- List all kids in group 7 and their parents
SELECT 
    k.id AS kid_id,
    k.full_name AS kid_name,
    k.group_id,
    p.id AS parent_id,
    p.full_name AS parent_name
FROM kids k
LEFT JOIN parent_kids pk ON k.id = pk.kid_id
LEFT JOIN parents p ON pk.parent_id = p.id
WHERE k.group_id = 7
ORDER BY k.id, p.id;
```

**Expected Result:** Should show all kids in group 7, including kids 19 and 20 with parent id=10 (Sara Johnson)

### Verify Daycare ID Consistency

```sql
-- Check daycare_id for all relevant entities
SELECT 'educator' AS entity_type, id, full_name, daycare_id::text FROM educators WHERE id IN (27, 24)
UNION ALL
SELECT 'parent' AS entity_type, id::text, full_name, daycare_id::text FROM parents WHERE id = 10
UNION ALL
SELECT 'kid' AS entity_type, id::text, full_name, daycare_id::text FROM kids WHERE id IN (19, 20)
UNION ALL
SELECT 'group' AS entity_type, id::text, name, daycare_id::text FROM groups WHERE id = 7;
```

**Expected Result:** All entities should have the same `daycare_id` (the one used in the Android app: `"default-daycare-id"` or actual UUID)

---

## Section 7: Summary & Root Causes

### Primary Root Cause: Educator userId Never Set

**Location:** `MainActivity.kt:158-159`

**Issue:** When updating session for educators, only `groupIds` is updated, not `userId`. The session remains with `userId = "current_user"` instead of the actual educator ID.

**Impact:**
- Self-exclusion fails: `e.id != "current_user"` evaluates to `true` for Jessica (id="27"), so she's not filtered out
- All educators appear in the list (if they're in the correct groups)

**Fix Required:**
```kotlin
sessionManager.update(
    sessionManager.session.value.copy(
        userId = educator.id,  // ✅ Add this
        groupIds = educatorGroupIds
    )
)
```

### Secondary Issues

1. **Parent ID Hardcoded:** `MainActivity.kt:170` - `currentParentId = "10"` should come from JWT token
2. **Educator Loading Hardcoded:** `EducatorViewModel.kt:33` - Always loads "Jessica" instead of current educator from auth
3. **Daycare ID Hardcoded:** `MainActivity.kt:143` - `"default-daycare-id"` may not match actual daycare

### Data Availability Issues (Requires Runtime Verification)

1. **Sarah Davis (id=24) Missing:**
   - May not be in group 7 (Jessica's group)
   - May not be returned by `/api/v1/educators?daycare_id=...`
   - Verify with SQL query in Section 6

2. **Sara Johnson (parent id=10) Missing:**
   - Kids 19, 20 may not be in group 7
   - Kids 19, 20 may not be linked to parent id=10
   - Kids 19, 20 may not be returned by `/api/v1/kids?daycare_id=...`
   - Verify with SQL query in Section 6

---

## Appendix: TODOs for Fixes

### Critical Fixes (Required for Basic Functionality)

1. **Set educator userId in session** (`MainActivity.kt:158`)
   - Change: `sessionManager.update(session.copy(userId = educator.id, groupIds = ...))`
   - Impact: Fixes self-exclusion in Educators tab

2. **Extract parent ID from JWT token** (`MainActivity.kt:170`)
   - Change: Replace `val currentParentId = "10"` with token claim extraction
   - Impact: Ensures correct parent session

3. **Load current educator from auth** (`EducatorViewModel.kt:33`)
   - Change: Replace hardcoded "Jessica" with current educator from JWT/token
   - Impact: Works for any educator, not just Jessica

### Secondary Fixes (Improve Robustness)

4. **Extract daycare ID from JWT token** (`MainActivity.kt:143`)
   - Change: Replace `"default-daycare-id"` with token claim
   - Impact: Works for multi-daycare scenarios

5. **Add error handling for missing data**
   - Location: `MessagingRepositoryImpl.kt`
   - Impact: Better user feedback when data is missing

6. **Add logging for debugging**
   - See Section 5 for suggested log points
   - Impact: Easier diagnosis of future issues

---

**Report End**

