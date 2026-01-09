# Android Dummy Authentication Flow Audit

**Date:** 2025-01-XX  
**Task:** T1b - Audit Android dummy user & role selection flow  
**Status:** Complete

---

## 1) Scope & How You Searched

### Search Commands Executed

1. **RoleSelectionScreen search:**
   ```bash
   grep -ri "RoleSelectionScreen" app/
   ```
   - Found 7 matches in:
     - `app/src/main/java/fi/kidozz/app/MainActivity.kt` (lines 346, 380)
     - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt` (lines 27, 272, 274)
     - `app/src/main/java/fi/kidozz/app/navigation/KiddozzNavigation.kt.old` (line 30, 46)

2. **Dev-login endpoint search:**
   ```bash
   grep -ri "devLogin\|dev-login\|/dev-login" app/
   ```
   - Found 14 matches in:
     - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt` (lines 73, 108, 143)
     - `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt` (lines 45, 64)
     - `app/src/main/java/fi/kidozz/app/data/api/AuthApiService.kt` (line 26)
     - `app/src/main/java/fi/kidozz/app/data/models/DevLoginRequest.kt` (line 7)

3. **Educator/parent ID search:**
   ```bash
   grep -ri "educator_id\|parent_id" app/
   ```
   - Found 6 matches in:
     - `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt` (lines 47, 66)
     - `app/src/main/java/fi/kidozz/app/data/models/DevLoginRequest.kt` (lines 8, 9)
     - `app/src/main/java/fi/kidozz/app/data/api/ParentsApiService.kt` (line 18)

4. **Authorization header search:**
   ```bash
   grep -ri "access_token\|Bearer\|Authorization" app/
   ```
   - Found 13 matches in:
     - `app/src/main/java/fi/kidozz/app/MainActivity.kt` (line 129)
     - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt` (lines 82, 117, 152)
     - `app/src/main/java/fi/kidozz/app/features/messaging/data/ws/MessagingSseClient.kt` (lines 27, 29)
     - `app/src/main/java/fi/kidozz/app/data/repository/KidsRepository.kt` (lines 75, 105)
     - `app/src/main/java/fi/kidozz/app/data/api/KidsApiService.kt` (lines 33, 40)
     - `app/src/main/java/fi/kidozz/app/data/models/DevLoginRequest.kt` (line 14)

5. **Token/JWT search:**
   ```bash
   grep -ri "Token\|jwt" app/ --files-with-matches
   ```
   - Found 21 files, key files:
     - `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`
     - `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt`
     - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt`

6. **SharedPreferences/DataStore/TokenManager/Auth search:**
   ```bash
   grep -ri "SharedPreferences\|DataStore\|TokenManager\|Auth" app/ --files-with-matches
   ```
   - Found 20 files, key files:
     - `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`
     - `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt`
     - `app/src/main/java/fi/kidozz/app/ui/components/LogoutButton.kt`

7. **Hardcoded user names search:**
   ```bash
   grep -ri "Jessica\|Sara\|Mervi" app/
   ```
   - Found 46 matches in:
     - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt` (lines 68, 94, 98, 103, 129, 133, 138, 164, 168)
     - `app/src/main/java/fi/kidozz/app/features/dashboard/EducatorViewModel.kt` (line 49)
     - Test files (multiple)

8. **Navigation search:**
   ```bash
   grep -ri "navigate(\|NavHost\|startDestination" app/ --files-with-matches
   ```
   - Found 5 files, key file:
     - `app/src/main/java/fi/kidozz/app/MainActivity.kt`

9. **Daycare ID search:**
   ```bash
   grep -ri "default-daycare-id\|daycare.*id" app/ -i
   ```
   - Found 110 matches, key locations:
     - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt` (line 64)
     - `app/src/main/java/fi/kidozz/app/MainActivity.kt` (lines 402, 409, 424)
     - `app/src/main/java/fi/kidozz/app/features/dashboard/EducatorViewModel.kt` (line 49)

10. **Login state search:**
    ```bash
    grep -ri "isLoggedIn\|loggedIn\|session\.isLoggedIn" app/
    ```
    - Found 14 matches in:
      - `app/src/main/java/fi/kidozz/app/MainActivity.kt` (lines 37, 83, 85, 94, 95, 318, 328, 337)
      - `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt` (line 132)
      - `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt` (line 91)

11. **Logout/clear search:**
    ```bash
    grep -ri "clearAll\|clearToken\|clearRole" app/
    ```
    - Found 19 matches in:
      - `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt` (lines 111, 137, 165)
      - `app/src/main/java/fi/kidozz/app/ui/components/LogoutButton.kt` (line 17)
      - `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt` (line 88)

### Keywords Searched

- `RoleSelectionScreen`
- `devLogin`, `dev-login`, `/dev-login`
- `educator_id`, `parent_id`
- `access_token`, `Bearer`, `Authorization`
- `Token`, `jwt`
- `SharedPreferences`, `DataStore`, `TokenManager`, `Auth`
- `navigate(`, `NavHost`, `startDestination`
- `Jessica`, `Sara`, `Mervi` (hardcoded user names)
- `default-daycare-id` (hardcoded daycare ID)
- `isLoggedIn`, `loggedIn`, `session.isLoggedIn`
- `clearAll`, `clearToken`, `clearRole`

---

## 2) Files Using Hardcoded Users or Dummy Auth

### 2.1 RoleSelectionScreen.kt

**File:** `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt`

**Hardcoded Values:**
- **Line 64:** `val daycareId = "default-daycare-id"` - Hardcoded daycare ID
- **Line 68:** `authRepository.getEducators(daycareId, "Jessica")` - Hardcoded educator name
- **Line 94:** `errorMessage = "Jessica not found"` - Error message for hardcoded name
- **Line 98:** `errorMessage = "Failed to fetch Jessica: ${exception.message}"` - Error message
- **Line 103:** `authRepository.getParents(daycareId, "Sara")` - Hardcoded parent name
- **Line 129:** `errorMessage = "Sara not found"` - Error message for hardcoded name
- **Line 133:** `errorMessage = "Failed to fetch Sara: ${exception.message}"` - Error message
- **Line 138:** `authRepository.getEducators(daycareId, "Mervi")` - Hardcoded super educator name
- **Line 164:** `errorMessage = "Mervi not found"` - Error message for hardcoded name
- **Line 168:** `errorMessage = "Failed to fetch Mervi: ${exception.message}"` - Error message

**What it does:**
- Searches for users by hardcoded names ("Jessica", "Sara", "Mervi")
- Calls `/api/v1/auth/dev-login` with the found user's ID
- Saves token and role to TokenManager
- Navigates to appropriate dashboard based on role

### 2.2 EducatorViewModel.kt

**File:** `app/src/main/java/fi/kidozz/app/features/dashboard/EducatorViewModel.kt`

**Hardcoded Values:**
- **Line 49:** `educatorRepository.getEducatorByName(daycareId, "Jessica")` - Fallback to hardcoded name when ID lookup fails

**What it does:**
- Falls back to searching for "Jessica" if educator ID from token is not found
- Used as backward compatibility fallback

### 2.3 MainActivity.kt

**File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`

**Hardcoded Values:**
- **Line 402:** `daycareId = daycareId ?: "default-daycare-id"` - Fallback daycare ID
- **Line 409:** `daycareId = daycareId ?: "default-daycare-id"` - Fallback daycare ID
- **Line 424:** `daycareId = daycareId ?: "default-daycare-id"` - Fallback daycare ID

**What it does:**
- Uses "default-daycare-id" as fallback when daycare ID is not found in token
- Passes this to ViewModels for data loading

### 2.4 AuthRepository.kt

**File:** `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt`

**Hardcoded Values:**
- **Line 47:** `val request = DevLoginRequest(educator_id = educatorId)` - Creates dev-login request
- **Line 66:** `val request = DevLoginRequest(parent_id = parentId)` - Creates dev-login request

**What it does:**
- Wraps calls to `/api/v1/auth/dev-login` endpoint
- No hardcoded IDs here, but facilitates dummy auth flow

### 2.5 AuthApiService.kt

**File:** `app/src/main/java/fi/kidozz/app/data/api/AuthApiService.kt`

**Hardcoded Values:**
- **Line 26:** `@POST("api/v1/auth/dev-login")` - Dev-login endpoint definition

**What it does:**
- Defines Retrofit interface for dev-login API call

---

## 3) RoleSelectionScreen Audit

### 3.1 Definition Location

**File:** `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt`  
**Lines:** 27-268 (main composable), 272-281 (preview)

### 3.2 How It's Reached

**Entry Points:**

1. **MainActivity navigation (logged out state):**
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
   - **Lines:** 337-354
   - **Condition:** `session.isLoggedIn == false || session.role == null`
   - **Route:** `Routes.ROLE_SELECTION` ("role_selection")
   - **Start Destination:** When logged out, NavHost uses `Routes.ROLE_SELECTION` as start destination (line 343)

2. **Production build bypass:**
   - **File:** `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt`
   - **Lines:** 39-44
   - **Condition:** `if (!BuildConfig.DEBUG)`
   - **Behavior:** In release builds, automatically calls `onEducatorViewClick()` and returns early (never shows UI)

3. **Within logged-in NavHost:**
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
   - **Lines:** 379-386
   - **Route:** `Routes.ROLE_SELECTION` is also defined in the logged-in NavHost (but shouldn't be reachable normally)

### 3.3 Actions Available

**Three buttons:**

1. **"Educator View" button:**
   - **Line:** 225-233
   - **onClick:** Calls `handleRoleSelection("educator")`
   - **Flow:**
     - Searches for educator named "Jessica" (line 68)
     - Gets first result (line 71)
     - Calls `authRepository.devLoginAsEducator(educator.id)` (line 73)
     - On success: saves token (line 82), saves role "educator" (line 85), calls `onEducatorViewClick()` (line 87)

2. **"Parent View" button:**
   - **Line:** 236-244
   - **onClick:** Calls `handleRoleSelection("parent")`
   - **Flow:**
     - Searches for parent named "Sara" (line 103)
     - Gets first result (line 106)
     - Calls `authRepository.devLoginAsParent(parent.id)` (line 108)
     - On success: saves token (line 117), saves role "parent" (line 120), calls `onParentViewClick()` (line 122)

3. **"Super-Educator View" button:**
   - **Line:** 247-255
   - **onClick:** Calls `handleRoleSelection("super_educator")`
   - **Flow:**
     - Searches for educator named "Mervi" (line 138)
     - Gets first result (line 141)
     - Calls `authRepository.devLoginAsEducator(educator.id)` (line 143)
     - On success: saves token (line 152), saves role "super_educator" (line 155), calls `onSuperEducatorViewClick()` (line 157)

### 3.4 What Each Action Does

**All actions follow this pattern:**

1. **Search for user by name:**
   - Educator: `authRepository.getEducators(daycareId, "Jessica"|"Mervi")`
   - Parent: `authRepository.getParents(daycareId, "Sara")`
   - Uses hardcoded `daycareId = "default-daycare-id"` (line 64)

2. **Get first matching user:**
   - `educators.firstOrNull()` or `parents.firstOrNull()`
   - If not found, shows error message

3. **Call dev-login API:**
   - Educator: `authRepository.devLoginAsEducator(educator.id)`
   - Parent: `authRepository.devLoginAsParent(parent.id)`
   - This calls `POST /api/v1/auth/dev-login` with `educator_id` or `parent_id`

4. **Save token:**
   - `authRepository.loginWithToken(tokenResponse.access_token)` (lines 82, 117, 152)
   - This calls `tokenManager.saveToken(token)` which stores in SharedPreferences

5. **Save role:**
   - `tokenManager.saveRole("educator"|"parent"|"super_educator")` (lines 85, 120, 155)
   - Only saves if role changed (checks current role first)

6. **Navigate:**
   - Educator: `onEducatorViewClick()` → navigates to `Routes.EDU_GRAPH`
   - Parent: `onParentViewClick()` → navigates to `"parent_dashboard"`
   - Super Educator: `onSuperEducatorViewClick()` → navigates to `Routes.EDU_GRAPH`

---

## 4) Navigation & Role Routing

### 4.1 App Entry Point

**File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`  
**Entry:** `onCreate()` method (line 68)

### 4.2 Navigation Decision Logic

**File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`  
**Lines:** 327-499

**Decision Tree:**

```
MainActivity.onCreate()
└─> setContent { KiddozzTheme { ... } }
    └─> TokenManager initialization (line 78)
        └─> Collect token and role flows (lines 79-82)
            └─> Compute session state (lines 85-87)
                └─> when { ... } (line 327)
                    ├─> session.isLoggedIn == null
                    │   └─> Show LoadingScreen (lines 328-335)
                    │
                    ├─> session.isLoggedIn == false || session.role == null
                    │   └─> Show RoleSelectionScreen (lines 337-354)
                    │       └─> NavHost with startDestination = Routes.ROLE_SELECTION (line 343)
                    │
                    └─> else (logged in)
                        └─> Show main app with bottom nav (lines 357-497)
                            └─> Determine startDestination by role (lines 369-373)
                                ├─> "educator" or "super_educator" → Routes.EDU_GRAPH
                                ├─> "parent" → "parent_dashboard"
                                └─> else → Routes.ROLE_SELECTION
```

### 4.3 Role Storage & Reading

**Where role is stored:**
- **File:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`
- **Storage:** Android SharedPreferences (key: "role")
- **Line 12:** `context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)`
- **Line 103:** `prefs.edit().putString("role", canonical).apply()`
- **StateFlow:** `_roleFlow` (line 14) - reactive state

**Where role is read:**
- **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- **Line 79:** `val role by tokenManager.roleFlow.collectAsState(initial = tokenManager.getRole())`
- **Line 85:** Used to compute `SessionState(isLoggedIn = loggedIn, role = role)`
- **Line 360:** Used to determine bottom navigation: `when (session.role?.lowercase())`
- **Line 369:** Used to determine start destination: `when (session.role?.lowercase())`

**Role values:**
- Stored as: `"educator"`, `"parent"`, `"super_educator"` (lowercase, canonicalized)
- Saved in: `RoleSelectionScreen.kt` lines 85, 120, 155
- Read from: `TokenManager.getRole()` or `tokenManager.roleFlow`

### 4.4 Screen Routing by Role

**Educator/Super Educator:**
- **Start Destination:** `Routes.EDU_GRAPH` ("educator_graph")
- **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- **Line 370:** `"educator", "super_educator" -> Routes.EDU_GRAPH`
- **EDU_GRAPH navigation:**
  - **Line 388-427:** Nested navigation graph
  - **Start:** `Routes.KIDS_OVERVIEW` ("kids_overview") (line 389)
  - **Routes:**
    - `Routes.KIDS_OVERVIEW` → `EducatorDashboardScreen` (line 392)
    - `Routes.CALENDAR` → `EducatorCalendarScreen` (line 412)
- **Bottom Navigation:** `EducatorBottomNavigation` (line 361)

**Parent:**
- **Start Destination:** `"parent_dashboard"` (hardcoded string, not in Routes object)
- **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- **Line 371:** `"parent" -> "parent_dashboard"`
- **Route Definition:**
  - **Line 474:** `composable("parent_dashboard")` → `ParentDashboardScreen`
- **Bottom Navigation:** `ParentBottomNavigation` (line 363)

**Unknown/No Role:**
- **Start Destination:** `Routes.ROLE_SELECTION` (line 372)
- Falls back to role selection screen

### 4.5 Navigation Callbacks

**From RoleSelectionScreen:**
- **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- **Lines 348-350:** Callbacks passed to RoleSelectionScreen
  - `onEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) }`
  - `onParentViewClick = { navController.navigate("parent_dashboard") }`
  - `onSuperEducatorViewClick = { navController.navigate(Routes.EDU_GRAPH) }`

**Note:** These callbacks are called AFTER token/role are saved, so MainActivity recomposition will show the correct screen based on new session state.

---

## 5) Auth State Management

### 5.1 Token Storage

**Storage Mechanism:**
- **File:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`
- **Type:** Android SharedPreferences
- **Key:** `"token"`
- **Line 12:** `context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)`
- **Line 90:** `prefs.edit().putString("token", token).apply()`

**StateFlow:**
- **Line 17:** `private val _tokenFlow = MutableStateFlow<String?>(prefs.getString("token", null))`
- **Line 18:** `val tokenFlow: StateFlow<String?> = _tokenFlow`
- Reactive state that updates when token is saved/cleared

**Token Saving:**
- **Function:** `saveToken(token: String)` (line 87)
- **Called from:**
  - `AuthRepository.loginWithToken()` (line 84) → called from RoleSelectionScreen (lines 82, 117, 152)
- **What it does:**
  - Saves to SharedPreferences (line 90)
  - Updates `_tokenFlow` (line 91)
  - Calls `updateDerivedClaims()` to extract userId and daycareId from JWT (line 92)

**Token Reading:**
- **Function:** `getToken(): String?` (line 152)
- **Returns:** Token from SharedPreferences or null
- **Also updates:** `_tokenFlow` if value changed (lines 154-161)

### 5.2 "Logged In" Determination

**Method:**
- **File:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`
- **Function:** `isLoggedIn(): Boolean` (line 132)
- **Implementation:**
  ```kotlin
  fun isLoggedIn(): Boolean {
      val token = prefs.getString("token", null)
      return !token.isNullOrEmpty()
  }
  ```
- **Logic:** Simply checks if token exists (non-null and non-empty)
- **No validation:** Does NOT verify token signature, expiry, or call `/me` endpoint

**Usage:**
- **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- **Line 83:** `val loggedIn = !token.isNullOrEmpty()` (duplicate logic, doesn't use `isLoggedIn()`)
- **Line 85:** `SessionState(isLoggedIn = loggedIn, role = role)`
- **Line 337:** `session.isLoggedIn == false || session.role == null` → show RoleSelectionScreen

### 5.3 Logout Behavior

**Logout Function:**
- **File:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`
- **Function:** `clearAll()` (line 137)
- **Implementation:**
  ```kotlin
  fun clearAll() {
      val hadRole = _roleFlow.value
      val hadToken = _tokenFlow.value
      prefs.edit().clear().apply()  // Clears ALL SharedPreferences, not just token/role
      
      if (hadRole != null) {
          _roleFlow.value = null
      }
      if (hadToken != null) {
          _tokenFlow.value = null
      }
  }
  ```
- **What it clears:**
  - All SharedPreferences (line 140) - **WARNING:** Clears everything, not just auth data
  - `_roleFlow` (set to null)
  - `_tokenFlow` (set to null)
  - Also clears `_userIdFlow` and `_daycareIdFlow` via `updateDerivedClaims()` (called implicitly)

**Logout Trigger:**
- **File:** `app/src/main/java/fi/kidozz/app/ui/components/LogoutButton.kt`
- **Line 17:** `tokenManager.clearAll()` called on button click
- **No explicit navigation:** Comment says "Navigation will be handled automatically by MainActivity recomposition" (line 19)

**Post-Logout Navigation:**
- **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt`
- **Lines 94-108:** `LaunchedEffect` watches `session.isLoggedIn` and `session.role`
- **When cleared:**
  - `session.isLoggedIn` becomes `false` (token is null)
  - `session.role` becomes `null` (role is cleared)
  - **Line 95:** Condition `session.isLoggedIn == false || session.role == null` becomes true
  - **Lines 99-102:** Clears NavController back stack (pops until empty)
  - **Line 337:** `when` statement shows RoleSelectionScreen
  - **App does NOT exit:** User stays in app, sees role selection screen

**Alternative Logout:**
- **File:** `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt`
- **Function:** `logout()` (line 87)
- **Implementation:** `tokenManager.clearToken()` (only clears token, not role)
- **Not used:** LogoutButton uses `clearAll()` instead

---

## 6) Call Flow Map

### 6.1 Cold Start Flow

```
App Launch
└─> MainActivity.onCreate()
    └─> setContent { KiddozzTheme { ... } }
        └─> TokenManager(context) initialized (line 78)
            ├─> Reads token from SharedPreferences
            ├─> Reads role from SharedPreferences
            └─> Initializes StateFlows
        └─> Collect tokenFlow and roleFlow (lines 79-82)
            └─> Compute loggedIn = !token.isNullOrEmpty() (line 83)
            └─> Create SessionState(isLoggedIn, role) (lines 85-87)
            └─> when (session) { ... } (line 327)
                │
                ├─> session.isLoggedIn == null
                │   └─> Show LoadingScreen (CircularProgressIndicator)
                │
                ├─> session.isLoggedIn == false || session.role == null
                │   └─> Show RoleSelectionScreen
                │       └─> NavHost(startDestination = Routes.ROLE_SELECTION)
                │
                └─> else (logged in)
                    └─> Show main app
                        └─> Determine startDestination by role:
                            ├─> "educator"/"super_educator" → Routes.EDU_GRAPH
                            ├─> "parent" → "parent_dashboard"
                            └─> else → Routes.ROLE_SELECTION
```

### 6.2 Role Selection → Login Flow

```
User clicks "Educator View" button
└─> handleRoleSelection("educator") (line 56)
    └─> coroutineScope.launch { ... }
        └─> daycareId = "default-daycare-id" (hardcoded, line 64)
        └─> authRepository.getEducators(daycareId, "Jessica") (line 68)
            └─> GET /api/v1/educators?daycare_id=default-daycare-id&search=Jessica
            └─> Result.fold(
                ├─> onSuccess: educators.firstOrNull()
                │   └─> if educator != null:
                │       └─> authRepository.devLoginAsEducator(educator.id) (line 73)
                │           └─> POST /api/v1/auth/dev-login { educator_id: "..." }
                │           └─> Result.fold(
                │               ├─> onSuccess: tokenResponse
                │               │   ├─> authRepository.loginWithToken(tokenResponse.access_token) (line 82)
                │               │   │   └─> tokenManager.saveToken(token) (line 84)
                │               │   │       ├─> Save to SharedPreferences (key: "token")
                │               │   │       ├─> Update _tokenFlow
                │               │   │       └─> updateDerivedClaims() (extract userId, daycareId from JWT)
                │               │   ├─> tokenManager.saveRole("educator") (line 85)
                │               │   │   ├─> Save to SharedPreferences (key: "role")
                │               │   │   └─> Update _roleFlow
                │               │   └─> onEducatorViewClick() (line 87)
                │               │       └─> navController.navigate(Routes.EDU_GRAPH)
                │               └─> onFailure: Show error message
                └─> onFailure: Show error message
```

**MainActivity recomposition after token/role saved:**
```
TokenManager.tokenFlow emits new value
└─> MainActivity recomposes
    └─> loggedIn = !token.isNullOrEmpty() → true
    └─> session = SessionState(isLoggedIn = true, role = "educator")
    └─> when (session) → else branch (logged in)
        └─> Show main app with EducatorBottomNavigation
            └─> startDestination = Routes.EDU_GRAPH (line 370)
            └─> NavHost navigates to EDU_GRAPH
                └─> EDU_GRAPH startDestination = Routes.KIDS_OVERVIEW
                    └─> Shows EducatorDashboardScreen
```

### 6.3 Logout Flow

```
User clicks LogoutButton
└─> LogoutButton.onClick (line 16)
    └─> tokenManager.clearAll() (line 17)
        ├─> prefs.edit().clear().apply() (clears ALL SharedPreferences)
        ├─> _roleFlow.value = null
        ├─> _tokenFlow.value = null
        └─> updateDerivedClaims() (clears userIdFlow, daycareIdFlow)
        
TokenManager flows emit null
└─> MainActivity recomposes
    └─> loggedIn = !token.isNullOrEmpty() → false
    └─> role = null (from roleFlow)
    └─> session = SessionState(isLoggedIn = false, role = null)
    └─> LaunchedEffect(session.isLoggedIn, session.role) triggers (line 94)
        └─> if (session.isLoggedIn == false || session.role == null) (line 95)
            └─> Clear NavController back stack (lines 99-102)
                └─> while (navController.popBackStack()) { ... }
    └─> when (session) → session.isLoggedIn == false branch (line 337)
        └─> Show RoleSelectionScreen
            └─> NavHost(startDestination = Routes.ROLE_SELECTION)
                └─> User sees role selection buttons again
```

**App does NOT exit:** User remains in app, sees RoleSelectionScreen.

---

## 7) Completeness Checklist

### 7.1 RoleSelectionScreen References

- ✅ **Found all references to RoleSelectionScreen:**
  - Definition: `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt:27`
  - Usage in MainActivity (logged out): `app/src/main/java/fi/kidozz/app/MainActivity.kt:346`
  - Usage in MainActivity (logged in NavHost): `app/src/main/java/fi/kidozz/app/MainActivity.kt:380`
  - Preview: `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt:272`
  - Old navigation file: `app/src/main/java/fi/kidozz/app/navigation/KiddozzNavigation.kt.old:30,46`

### 7.2 Dev-Login Calls from Android

- ✅ **Found all dev-login calls:**
  - API definition: `app/src/main/java/fi/kidozz/app/data/api/AuthApiService.kt:26`
  - Repository wrapper: `app/src/main/java/fi/kidozz/app/data/repository/AuthRepository.kt:45,64`
  - Usage (educator): `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt:73,143`
  - Usage (parent): `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt:108`
  - Request model: `app/src/main/java/fi/kidozz/app/data/models/DevLoginRequest.kt:7-10`

### 7.3 Role Decision and Storage

- ✅ **Found where role is decided:**
  - Saved in: `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt:85,120,155`
  - Storage: `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt:99-109`
  - Read in: `app/src/main/java/fi/kidozz/app/MainActivity.kt:79`
  - Used for navigation: `app/src/main/java/fi/kidozz/app/MainActivity.kt:360,369`

### 7.4 Token Persistence and Reading

- ✅ **Found where token is persisted:**
  - Storage: `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt:87-97`
  - Mechanism: SharedPreferences (key: "token")
  - StateFlow: `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt:17-18`

- ✅ **Found where token is read:**
  - Direct read: `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt:152-163`
  - Reactive read: `app/src/main/java/fi/kidozz/app/MainActivity.kt:80` (collectAsState)
  - Used for auth header: `app/src/main/java/fi/kidozz/app/MainActivity.kt:126,129`

### 7.5 Uncertainties / Gaps

**Uncertain:**
- **No token validation:** Token is never verified against backend `/me` endpoint
- **No token expiry check:** JWT expiry claim is decoded but not checked
- **No refresh token:** Only access token stored, no refresh mechanism

**Missing (expected but not found):**
- No password storage (expected - using dev-login)
- No email/password login flow (expected - using dev-login)
- No invitation link handling (expected - not implemented yet)
- No deep link handling for registration (expected - not implemented yet)

**Potential Issues:**
- `TokenManager.clearAll()` clears ALL SharedPreferences, not just auth data (line 140)
- Hardcoded "default-daycare-id" used as fallback in multiple places
- No validation that token is still valid (could be expired)
- Role is stored separately from token (could get out of sync)

---

## Summary: What Screen Shows on App Launch and Why?

### Cold Start (No Token)

1. **MainActivity.onCreate()** initializes TokenManager
2. TokenManager reads SharedPreferences - finds no token
3. `loggedIn = false`, `role = null`
4. `session = SessionState(isLoggedIn = false, role = null)`
5. **when statement** (line 327) → `session.isLoggedIn == false` branch (line 337)
6. **Shows:** RoleSelectionScreen
7. **NavHost:** `startDestination = Routes.ROLE_SELECTION` (line 343)

### Cold Start (With Saved Token)

1. **MainActivity.onCreate()** initializes TokenManager
2. TokenManager reads SharedPreferences - finds token and role
3. `loggedIn = true`, `role = "educator"` (or "parent", "super_educator")
4. `session = SessionState(isLoggedIn = true, role = "educator")`
5. **when statement** → `else` branch (line 357)
6. **Determines startDestination:**
   - If role = "educator" or "super_educator" → `Routes.EDU_GRAPH`
   - If role = "parent" → `"parent_dashboard"`
7. **Shows:** Main app with appropriate bottom navigation and start screen

---

## Summary: Where Are Dummy Users Defined and How Are They Used?

### Dummy Users

**Not defined in Android app** - they exist in the backend database (seeded via `insert_dummy_educators()` and `insert_dummy_parents()`).

**Android app searches for them by name:**
- **"Jessica"** - Educator (searched in `RoleSelectionScreen.kt:68`)
- **"Sara"** - Parent (searched in `RoleSelectionScreen.kt:103`)
- **"Mervi"** - Super Educator (searched in `RoleSelectionScreen.kt:138`)

**How they're used:**
1. User clicks role button (e.g., "Educator View")
2. App searches: `GET /api/v1/educators?daycare_id=default-daycare-id&search=Jessica`
3. Backend returns list of educators matching "Jessica"
4. App takes first result: `educators.firstOrNull()`
5. App calls dev-login with that educator's ID: `POST /api/v1/auth/dev-login { educator_id: "..." }`
6. Backend returns JWT token
7. App saves token and role
8. App navigates to educator dashboard

**Fallback usage:**
- `EducatorViewModel.kt:49` - Falls back to searching "Jessica" if educator ID from token is not found

---

## Summary: Exactly How Does Role Selection Translate into Navigation?

### Step-by-Step Flow

1. **User clicks button** (e.g., "Educator View")
   - **File:** `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt:225`
   - **Action:** `handleRoleSelection("educator")` (line 56)

2. **Search and login:**
   - Search for "Jessica" (line 68)
   - Call dev-login with educator ID (line 73)
   - Save token (line 82)
   - Save role "educator" (line 85)

3. **Immediate navigation callback:**
   - `onEducatorViewClick()` called (line 87)
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt:348`
   - **Action:** `navController.navigate(Routes.EDU_GRAPH)` (line 348)

4. **MainActivity recomposition (reactive):**
   - TokenManager.tokenFlow emits new token
   - TokenManager.roleFlow emits "educator"
   - MainActivity recomposes (line 80-82)
   - `loggedIn = true`, `role = "educator"`
   - `session = SessionState(isLoggedIn = true, role = "educator")`

5. **Navigation decision:**
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt:369`
   - `when (session.role?.lowercase())` → `"educator"` matches
   - `startDestination = Routes.EDU_GRAPH` (line 370)

6. **NavHost shows:**
   - **File:** `app/src/main/java/fi/kidozz/app/MainActivity.kt:388-427`
   - EDU_GRAPH nested navigation
   - Start: `Routes.KIDS_OVERVIEW` (line 389)
   - Screen: `EducatorDashboardScreen` (line 392)
   - Bottom nav: `EducatorBottomNavigation` (line 361)

**For Parent:**
- Same flow, but:
  - Searches "Sara" (line 103)
  - Saves role "parent" (line 120)
  - Navigates to `"parent_dashboard"` (line 122, 349)
  - MainActivity: `startDestination = "parent_dashboard"` (line 371)
  - Shows: `ParentDashboardScreen` (line 475)
  - Bottom nav: `ParentBottomNavigation` (line 363)

---

## Summary: Where Is Token Stored, and How Does Logout Behave?

### Token Storage

**Location:** Android SharedPreferences  
**File:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt`  
**Key:** `"token"`  
**SharedPreferences name:** `"user_prefs"` (line 12)  
**Storage method:** `prefs.edit().putString("token", token).apply()` (line 90)

**Reactive state:**
- `_tokenFlow: MutableStateFlow<String?>` (line 17)
- Emits when token is saved or cleared
- MainActivity collects this flow (line 80)

### Logout Behavior

**Function:** `TokenManager.clearAll()`  
**File:** `app/src/main/java/fi/kidozz/app/data/auth/TokenManager.kt:137`

**What it does:**
1. Clears ALL SharedPreferences (line 140) - **WARNING:** Not just auth data
2. Sets `_roleFlow.value = null`
3. Sets `_tokenFlow.value = null`
4. Implicitly clears `_userIdFlow` and `_daycareIdFlow` via `updateDerivedClaims()`

**Trigger:**
- `LogoutButton.onClick()` (line 17 in `LogoutButton.kt`)

**Post-logout:**
1. TokenManager flows emit `null`
2. MainActivity recomposes
3. `loggedIn = false`, `role = null`
4. `LaunchedEffect` clears NavController back stack (lines 94-108)
5. `when` statement shows RoleSelectionScreen (line 337)
6. **App does NOT exit** - user sees role selection screen again

**No backend logout call:** Logout is purely client-side (clears local token/role).

---

**End of Audit**

