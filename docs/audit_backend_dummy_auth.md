# Backend Dummy Authentication Audit

**Date:** 2025-01-XX  
**Task:** T1a - Audit backend dummy authentication  
**Status:** Complete

---

## 1) Scope & How You Searched

### Search Commands Executed

1. **Dev-login endpoint search:**
   ```bash
   grep -ri "dev-login\|/dev-login\|DevLogin\|dev login" backend/
   ```
   - Found 34 matches across test files and `backend/app/api/auth.py`

2. **Dummy/mock/test user search:**
   ```bash
   grep -ri "dummy\|seed\|mock\|test user\|impersonate\|bypass" backend/
   ```
   - Found 205 matches, primarily in:
     - `backend/app/services/seeder.py` (dummy parents/kids creation)
     - `backend/app/services/educator_service.py` (dummy educators)
     - Test files using seeded data

3. **JWT/token/secret search:**
   ```bash
   grep -ri "JWT\|token\|sign\|encode\|secret\|HS256\|RS256" backend/
   ```
   - Found 730 matches across:
     - `backend/app/core/security.py` (token creation/verification)
     - `backend/app/core/deps.py` (token extraction/validation)
     - `backend/app/api/auth.py` (token minting)
     - Various API endpoints using tokens

4. **Environment/DEBUG flags search:**
   ```bash
   grep -ri "SKIP_AUTH\|DISABLE_AUTH\|BYPASS_AUTH\|ENV.*DEV\|DEBUG\|development\|local" backend/
   ```
   - Found 183 matches, primarily:
     - `backend/app/core/config.py` (environment configuration)
     - `backend/app/api/auth.py` (production checks)
     - `backend/app/utils/daycare_resolver.py` (local/test shortcuts)

5. **Switch-role/test-token endpoints:**
   ```bash
   grep -ri "switch-role\|test-token\|/switch-role\|/test-token" backend/
   ```
   - Found 20 matches in `backend/app/api/auth.py` and test files

### Keywords Searched

- `dev-login`, `/dev-login`, `DevLogin`, `dev login`
- `dummy`, `seed`, `mock`, `test user`, `impersonate`, `bypass`
- `JWT`, `token`, `sign`, `encode`, `secret`, `HS256`, `RS256`
- `Auth`, `Authorization`, `Bearer`, `middleware`, `guard`
- `ENV`, `DEBUG`, `DEV`, `development`, `local`
- `switch-role`, `test-token`
- `default-daycare-id` (local dev shortcut)

---

## 2) Endpoints Involved in Dev/Dummy Auth

### 2.1 POST `/api/v1/auth/dev-login`

**File:** `backend/app/api/auth.py`  
**Lines:** 101-151

**Request Body:**
```python
class DevLoginRequest(BaseModel):
    educator_id: Optional[str] = None
    parent_id: Optional[str] = None
```
- Must provide exactly one of `educator_id` or `parent_id`

**Response:**
```python
class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
```

**Protection:**
- **Line 107:** Checks `settings.environment == "production"`
- If production, returns `403 Forbidden` with message "Dev login is disabled in production"
- **No authentication required** - anyone can call this endpoint in non-production

**Flow:**
1. Validates exactly one of `educator_id` or `parent_id` provided (lines 112-117)
2. Queries database for Educator or Parent by ID (lines 125-141)
3. Extracts role, daycare_id, groups from database record
4. Calls `resolve_daycare_id()` to map "default-daycare-id" in local/test (line 130, 139)
5. Creates JWT token via `create_access_token()` (line 143-145)
6. Returns token in response (line 151)

**Token Claims Created:**
- `sub`: User ID (string)
- `role`: "educator", "super_educator", or "parent"
- `daycare_id`: UUID string
- `groups`: List of group names (for educators)
- `exp`: Expiration timestamp (default 30 minutes from `settings.access_token_expire_minutes`)

**Debug Output:**
- Lines 148-149: Prints secret key and environment to console (security risk)

### 2.2 POST `/api/v1/auth/switch-role`

**File:** `backend/app/api/auth.py`  
**Lines:** 25-57

**Request:**
- Query parameter: `role` (must match pattern `^(parent|educator|super_educator)$`)
- **No authentication required**

**Response:**
```python
{
    "access_token": str,
    "token_type": "bearer",
    "user_id": "test-user",
    "role": str,
    "expires_in": 86400  # 24 hours
}
```

**Protection:**
- **No environment check** - available in all environments
- **No authentication required**
- Comment says "staging only" but no enforcement (line 34)

**Flow:**
1. Hardcodes `user_id = "test-user"` (line 38)
2. Creates token with provided role (lines 41-48)
3. Uses 24-hour expiry (line 48)

**Token Claims:**
- `sub`: "test-user" (hardcoded)
- `role`: From query parameter
- `exp`: 24 hours from now

### 2.3 POST `/api/v1/auth/test-token`

**File:** `backend/app/api/auth.py`  
**Lines:** 60-98

**Request Body:**
```python
class TestTokenRequest(BaseModel):
    role: str
    user_id: int = 1
```

**Response:**
```python
{
    "access_token": str,
    "token_type": "bearer",
    "user_id": int,
    "role": str
}
```

**Protection:**
- **Line 67:** Checks `settings.environment != "staging"`
- If not staging, returns `404 Not Found`
- **No authentication required** in staging

**Flow:**
1. Validates role is in allowed set: `{"parent", "educator", "super_educator"}` (lines 74-82)
2. Creates token with provided `user_id` and `role` (lines 85-91)
3. Uses default token expiry (30 minutes)

**Token Claims:**
- `sub`: String of `request.user_id`
- `role`: From request body
- `exp`: Default expiry (30 minutes)

---

## 3) Auth Shortcuts / Bypasses

### 3.1 Dev-Login Endpoint Bypass

**Location:** `backend/app/api/auth.py:107-110`

**Condition:**
```python
if settings.environment == "production":
    raise HTTPException(status_code=403, detail="Dev login is disabled in production")
```

**What is bypassed:**
- Password authentication
- Email verification
- User registration flow
- Any identity verification

**Risk if removed:**
- All test suites that use `/dev-login` would break
- Android app's current login flow would break (uses hardcoded user IDs)
- Local development would require manual token generation

**Files that depend on this:**
- `backend/tests/test_auth.py` (multiple tests)
- `backend/tests/test_identity_endpoints.py` (lines 37, 52)
- `backend/tests/test_kids_absences.py` (lines 130, 242, 247, 319, 389, 752, 833, 918, 1002, 1067)
- `backend/tests/test_absence_partitions.py` (lines 130, 242)
- Android app: `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt` (lines 73, 108, 143)

### 3.2 Switch-Role Endpoint (No Protection)

**Location:** `backend/app/api/auth.py:25-57`

**Condition:**
- **No environment check**
- **No authentication required**

**What is bypassed:**
- All authentication
- User identity verification
- Role validation (only pattern matching)

**Risk if removed:**
- Test files using `/switch-role` would break:
  - `backend/tests/test_auth.py` (lines 125, 151, 168, 174, 181, 199, 236, 254, 287, 471)

### 3.3 Test-Token Endpoint (Staging Only)

**Location:** `backend/app/api/auth.py:67-71`

**Condition:**
```python
if settings.environment != "staging":
    raise HTTPException(status_code=404, detail="Endpoint not available in this environment")
```

**What is bypassed:**
- Password authentication
- User existence verification (creates token for any user_id)
- Role validation (only checks against allowed list)

**Risk if removed:**
- Test files using `/test-token` would break:
  - `backend/tests/test_auth.py` (lines 329-471)

### 3.4 Daycare ID Resolution Shortcut

**Location:** `backend/app/utils/daycare_resolver.py:11-18`

**Condition:**
```python
if settings.app_env in ("local", "test") and daycare_id == "default-daycare-id":
    # Maps to first daycare in database or creates one
```

**What is bypassed:**
- Proper daycare ID validation
- Tenant isolation (allows "default-daycare-id" to map to any daycare)

**Risk if removed:**
- Android app uses "default-daycare-id" hardcoded (see `MainActivity.kt`)
- Test fixtures use "default-daycare-id" (see `backend/tests/conftest.py:104`)
- Local development would require actual daycare UUIDs

**Files using this:**
- `backend/app/api/auth.py:130,139` (dev-login)
- `backend/app/api/kids.py:34` (list_kids)
- `backend/app/api/educators.py:30` (list_educators)
- `backend/app/api/parents.py:30` (list_parents)

### 3.5 Production Daycare ID Requirement (Relaxed in Dev)

**Location:** `backend/app/api/educators.py:25-26` and `backend/app/api/parents.py:25-26`

**Condition:**
```python
if settings.environment == "production" and not daycare_id:
    raise HTTPException(status_code=400, detail="daycare_id is required")
```

**What is bypassed:**
- In non-production, `daycare_id` is optional
- Allows querying without daycare scope (security risk)

**Risk if removed:**
- Would enforce daycare scoping in all environments
- Might break tests that don't provide daycare_id

### 3.6 No Auth Required on Some Endpoints

**Endpoints that don't require authentication:**
- `GET /api/v1/kids` - No `Depends(get_current_user)` (line 27-47 in `backend/app/api/kids.py`)
- `GET /api/v1/educators` - No `Depends(get_current_user)` (line 15-43 in `backend/app/api/educators.py`)
- `GET /api/v1/parents` - No `Depends(get_current_user)` (line 17-42 in `backend/app/api/parents.py`)

**What is bypassed:**
- User authentication
- Tenant isolation (relies on daycare_id query param)

**Risk if removed:**
- Would require all clients to authenticate
- Android app currently calls these without tokens in some flows

---

## 4) Config Flags / Environment Variables Affecting Auth

### 4.1 `ENVIRONMENT` / `settings.environment`

**Location:** `backend/app/core/config.py:23`

**Default:** `"development"` (from `os.getenv("ENVIRONMENT", "development")`)

**Usage:**
- `backend/app/api/auth.py:67` - `/test-token` endpoint availability
- `backend/app/api/auth.py:107` - `/dev-login` endpoint availability
- `backend/app/api/educators.py:25` - Daycare ID requirement
- `backend/app/api/parents.py:25` - Daycare ID requirement

**Behavior:**
- `"production"`: Disables `/dev-login`, requires daycare_id on some endpoints
- `"staging"`: Enables `/test-token`, allows `/dev-login`
- `"development"` (default): Allows all dev endpoints

### 4.2 `APP_ENV` / `settings.app_env`

**Location:** `backend/app/core/config.py:22`

**Default:** `"local"` (from `os.getenv("APP_ENV", "local")`)

**Usage:**
- `backend/app/utils/daycare_resolver.py:11` - Enables "default-daycare-id" mapping

**Behavior:**
- `"local"` or `"test"`: Maps "default-daycare-id" to first daycare in DB
- Other values: No special mapping

### 4.3 `SECRET_KEY` / `settings.secret_key`

**Location:** `backend/app/core/config.py:43-44`

**Default:** `"your-secret-key-here-change-in-production"`

**Usage:**
- `backend/app/core/security.py:27` - JWT signing key
- `backend/app/core/security.py:41` - JWT verification key

**Security Risk:**
- Default key is hardcoded and weak
- Used for all JWT signing/verification
- Printed to console in dev-login (line 148 of `backend/app/api/auth.py`)

### 4.4 `access_token_expire_minutes` / `settings.access_token_expire_minutes`

**Location:** `backend/app/core/config.py:47`

**Default:** `30` minutes

**Usage:**
- `backend/app/core/security.py:20` - Default token expiry
- Overridden in `/switch-role` to 24 hours (line 48 of `backend/app/api/auth.py`)

### 4.5 `algorithm` / `settings.algorithm`

**Location:** `backend/app/core/config.py:46`

**Default:** `"HS256"`

**Usage:**
- `backend/app/core/security.py:27` - JWT signing algorithm
- `backend/app/core/security.py:41` - JWT verification algorithm

---

## 5) JWT Minting & Verification in Dev

### 5.1 Token Creation

**Function:** `create_access_token()`  
**File:** `backend/app/core/security.py`  
**Lines:** 10-34

**Algorithm:** HS256 (symmetric, single secret key)

**Key Source:**
- `settings.secret_key` (line 27)
- Default: `"your-secret-key-here-change-in-production"` (from `backend/app/core/config.py:44`)
- Can be overridden via `SECRET_KEY` environment variable

**Claims Included:**
- All claims from `data` parameter (copied, line 14)
- `exp`: Expiration timestamp (line 23)
  - If `expires_delta` provided: `datetime.now(timezone.utc) + expires_delta`
  - Otherwise: `datetime.now(timezone.utc) + timedelta(minutes=settings.access_token_expire_minutes)`

**Token Creation Points:**

1. **Dev-login endpoint:**
   - File: `backend/app/api/auth.py:143-145`
   - Claims: `{"sub": user_id, "role": role, "daycare_id": daycare_id, "groups": groups}`
   - Expiry: Default (30 minutes)

2. **Switch-role endpoint:**
   - File: `backend/app/api/auth.py:47-48`
   - Claims: `{"sub": "test-user", "role": role}`
   - Expiry: 24 hours (hardcoded)

3. **Test-token endpoint:**
   - File: `backend/app/api/auth.py:91`
   - Claims: `{"sub": str(user_id), "role": role}`
   - Expiry: Default (30 minutes)

### 5.2 Token Verification

**Function:** `decode_access_token()`  
**File:** `backend/app/core/security.py`  
**Lines:** 37-53

**Algorithm:** HS256 (must match signing algorithm)

**Key Source:**
- `settings.secret_key` (line 41)
- Same key used for signing

**Verification Process:**
1. Calls `jwt.decode()` with token, secret_key, and algorithm (lines 40-42)
2. Returns decoded payload if valid
3. Raises `HTTPException 401` if JWTError (invalid signature, expired, etc.)

**Token Verification Points:**

1. **OAuth2PasswordBearer extraction:**
   - File: `backend/app/core/deps.py:9`
   - Extracts token from `Authorization: Bearer <token>` header
   - Token URL: `"auth/token"` (not used, just for OAuth2 scheme)

2. **get_current_user dependency:**
   - File: `backend/app/core/deps.py:12-29`
   - Calls `decode_access_token(token)` (line 15)
   - Validates `sub` claim exists (lines 16-21)
   - Returns full payload as `Dict[str, Any]`

3. **Endpoints using get_current_user:**
   - `GET /api/v1/auth/me` - `backend/app/api/auth.py:156`
   - `GET /api/v1/auth/me/educator` - `backend/app/api/auth.py:169`
   - `POST /api/v1/auth/logout` - `backend/app/api/auth.py:190`
   - `PATCH /api/v1/kids/{kid_id}/attendance` - `backend/app/api/kids.py:50`
   - `POST /api/v1/kids/{kid_id}/absences` - `backend/app/api/kids.py:108`
   - All messaging endpoints - `backend/app/api/messaging.py` (multiple)

**Token TTL:**
- Default: 30 minutes (`settings.access_token_expire_minutes`)
- Switch-role: 24 hours (hardcoded)
- Can be customized per token via `expires_delta` parameter

**Additional Claims:**
- No `aud` (audience) claim
- No `iss` (issuer) claim
- No `iat` (issued at) claim (only `exp`)
- No `nbf` (not before) claim

---

## 6) Call Flow Map

### 6.1 Dev-Login Flow

```
1. HTTP POST /api/v1/auth/dev-login
   └─> backend/app/api/auth.py:102 (dev_login function)
       ├─> Check: settings.environment != "production" (line 107)
       │   └─> If production: return 403
       ├─> Validate: exactly one of educator_id or parent_id (lines 112-117)
       ├─> Query database for Educator or Parent (lines 125-141)
       │   ├─> If educator: Extract role, daycare_id, groups
       │   └─> If parent: Extract role, daycare_id
       ├─> resolve_daycare_id() (lines 130, 139)
       │   └─> backend/app/utils/daycare_resolver.py:9
       │       └─> If local/test and "default-daycare-id": map to first daycare
       ├─> create_access_token() (line 143)
       │   └─> backend/app/core/security.py:10
       │       ├─> Add exp claim (30 min default)
       │       ├─> jwt.encode() with settings.secret_key, HS256
       │       └─> Return JWT string
       └─> Return {"access_token": token, "token_type": "bearer"}
```

### 6.2 Normal Endpoint Flow (Authenticated)

```
1. HTTP Request with Authorization: Bearer <token>
   └─> FastAPI routing
       └─> Endpoint with Depends(get_current_user)
           └─> backend/app/core/deps.py:12 (get_current_user)
               ├─> OAuth2PasswordBearer extracts token from header (line 9)
               ├─> decode_access_token(token) (line 15)
               │   └─> backend/app/core/security.py:37
               │       ├─> jwt.decode() with settings.secret_key, HS256
               │       ├─> Validate signature and expiry
               │       └─> Return payload Dict
               ├─> Validate "sub" claim exists (lines 16-21)
               └─> Return payload as current_user Dict
                   └─> Endpoint receives current_user with claims:
                       - sub (user_id)
                       - role
                       - daycare_id
                       - groups (if educator)
                       - exp
```

### 6.3 Unauthenticated Endpoint Flow

```
1. HTTP Request (no Authorization header)
   └─> FastAPI routing
       └─> Endpoint without Depends(get_current_user)
           ├─> GET /api/v1/kids?daycare_id=...
           ├─> GET /api/v1/educators?daycare_id=...
           └─> GET /api/v1/parents?daycare_id=...
               └─> resolve_daycare_id() if daycare_id provided
                   └─> Query database directly (no user context)
```

---

## 7) Audit Completeness Checklist

### 7.1 Dev-Login References

- ✅ **Found all references to `/dev-login`:**
  - Endpoint definition: `backend/app/api/auth.py:101`
  - Schema: `backend/app/schemas/auth.py:6-8`
  - Test files: 10+ test files using this endpoint
  - Documentation: `backend/README.md:240, 257, 337, 342`

### 7.2 Token Creation Points

- ✅ **Found all token creation points:**
  - `create_access_token()` function: `backend/app/core/security.py:10`
  - Dev-login: `backend/app/api/auth.py:143`
  - Switch-role: `backend/app/api/auth.py:47`
  - Test-token: `backend/app/api/auth.py:91`
  - No other direct JWT creation found

### 7.3 Auth Middleware/Guards

- ✅ **Found all auth middleware/guards:**
  - `get_current_user`: `backend/app/core/deps.py:12`
  - `require_role()`: `backend/app/core/deps.py:32`
  - `require_any_role()`: `backend/app/core/deps.py:49`
  - `OAuth2PasswordBearer`: `backend/app/core/deps.py:9`
  - No other auth middleware found

### 7.4 Environment Flags Affecting Auth

- ✅ **Found all env flags:**
  - `ENVIRONMENT`: Controls dev-login and test-token availability
  - `APP_ENV`: Controls "default-daycare-id" mapping
  - `SECRET_KEY`: JWT signing key
  - `access_token_expire_minutes`: Token TTL (in Settings class)
  - No `SKIP_AUTH`, `DISABLE_AUTH`, or `BYPASS_AUTH` flags found

### 7.5 Uncertainties / Gaps

**Uncertain:**
- **No password hashing found:** Confirmed - no password fields in Educator or Parent models
- **No refresh token mechanism:** Confirmed - only access tokens with 30-min expiry
- **No token revocation:** Confirmed - stateless JWT system, no blacklist

**Missing (expected but not found):**
- No rate limiting on dev-login endpoint
- No audit logging of dev-login usage
- No IP whitelist for dev endpoints
- No request signing/validation beyond JWT

**Potential Issues:**
- Secret key printed to console in dev-login (security risk)
- Switch-role endpoint has no protection (comment says "staging only" but no enforcement)
- Default secret key is weak and hardcoded

---

## Summary: What Breaks If We Delete `/dev-login`?

### Immediate Breakage

1. **All test suites** using `/dev-login`:
   - `backend/tests/test_auth.py`
   - `backend/tests/test_identity_endpoints.py`
   - `backend/tests/test_kids_absences.py`
   - `backend/tests/test_absence_partitions.py`

2. **Android app login flow:**
   - `app/src/main/java/fi/kidozz/app/features/role/RoleSelectionScreen.kt`
   - Hardcoded calls to `devLoginAsEducator()` and `devLoginAsParent()`

3. **Local development workflow:**
   - Developers currently use `/dev-login` to get tokens for testing

### Required Replacements

1. **New authentication flow:**
   - Email/password login endpoint
   - Invitation-based registration
   - Password reset flow

2. **Test fixtures:**
   - Replace `/dev-login` calls with proper user creation + login
   - Or create test-only token generation utility

3. **Android app:**
   - Replace hardcoded user selection with proper login screen
   - Implement invitation link handling

---

## Summary: JWT Minting in Dev

### How JWTs Are Minted

1. **Function:** `create_access_token()` in `backend/app/core/security.py:10`
2. **Algorithm:** HS256 (symmetric)
3. **Key:** `settings.secret_key` (default: weak hardcoded string)
4. **Claims:** `sub`, `role`, `daycare_id`, `groups`, `exp`
5. **TTL:** 30 minutes default, 24 hours for switch-role

### What Claims Are Relied On

- **`sub`:** User ID (string) - used throughout codebase as `current_user.get("sub")`
- **`role`:** "parent", "educator", or "super_educator" - used for authorization
- **`daycare_id`:** UUID string - used for tenant isolation
- **`groups`:** List of group names (educators only) - used for filtering
- **`exp`:** Expiration timestamp - validated by JWT library

---

## Summary: Flags to Remove/Replace for Invite-Only Auth

### Flags to Remove

1. **`ENVIRONMENT` check for `/dev-login`:**
   - Currently: Disables in production
   - Action: Remove endpoint entirely

2. **`APP_ENV` check for "default-daycare-id":**
   - Currently: Maps to first daycare in local/test
   - Action: Remove shortcut, require real daycare IDs

3. **`ENVIRONMENT` check for `/test-token`:**
   - Currently: Only available in staging
   - Action: Remove endpoint entirely

4. **No protection on `/switch-role`:**
   - Currently: Available everywhere, no auth
   - Action: Remove endpoint entirely

### Flags to Keep/Modify

1. **`SECRET_KEY`:**
   - Keep: Still needed for JWT signing
   - Modify: Enforce strong key in production, remove default

2. **`access_token_expire_minutes`:**
   - Keep: Still needed for token expiry
   - Consider: Add refresh token mechanism

### New Flags Needed

1. **Invitation token expiry:** For invitation links
2. **Email service config:** For sending invitations
3. **Password reset token expiry:** For password resets

---

**End of Audit**

