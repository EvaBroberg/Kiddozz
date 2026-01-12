# T3a — Audit Existing Tenant/Daycare Schema

## 1) Existing Tables Related to Daycare/Tenant

### `daycares` Table

**File:** `backend/app/models/daycare.py` (lines 20-51)

**Primary Key:**
- `id`: `UUID(as_uuid=False)` - String UUID, primary key, auto-generated via `uuid4()`

**Columns:**
- `id`: `UUID(as_uuid=False)`, primary key, default=`lambda: str(uuid4())`
- `name`: `String(200)`, nullable=False
- `created_at`: `DateTime`, nullable=False, default=`func.now()`
- `updated_at`: `DateTime`, nullable=False, default=`func.now()`, onupdate=`func.now()`

**Migration:** `backend/alembic/versions/7290bdad5686_add_multi_daycare_schema_with_.py` (lines 21-27)
- Created: 2025-09-20
- Revision: `7290bdad5686`

**Relationships:**
- `groups`: One-to-many with `Group` (cascade delete)
- `educators`: One-to-many with `Educator` (cascade delete)
- `parents`: One-to-many with `Parent` (cascade delete)
- `kids`: One-to-many with `Kid` (cascade delete)

**Notes:**
- No unique constraints on `name` (multiple daycares can have the same name)
- No `tenant_id` column exists - `daycare` IS the tenant concept

## 2) User-like Entities

### Educator

**File:** `backend/app/models/educator.py` (lines 25-57)

**Daycare Reference:**
- `daycare_id`: `UUID(as_uuid=False)`, nullable=False, ForeignKey to `daycares.id` with CASCADE delete
- **Line 38-42:** `daycare_id: Mapped[str] = mapped_column(UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False)`

**Relationship:**
- `daycare: Mapped[Daycare]` - relationship back to Daycare (line 51)

**Unique Constraint:**
- `uq_educators_daycare_email`: Unique constraint on `(daycare_id, email)` - ensures email uniqueness per daycare
- **Migration:** `backend/alembic/versions/3009d0ea1444_add_allergies_and_need_to_know_columns_.py` (line 79)

**JWT Token:**
- `daycare_id` is included in JWT token claims (see `backend/app/api/auth.py` line 144)
- Token structure: `{"sub": user_id, "role": role, "daycare_id": daycare_id, "groups": groups}`

### Parent

**File:** `backend/app/models/parent.py` (lines 17-45)

**Daycare Reference:**
- `daycare_id`: `UUID(as_uuid=False)`, nullable=False, ForeignKey to `daycares.id` with CASCADE delete
- **Line 26-29:** `daycare_id: Mapped[str] = mapped_column(UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False)`

**Relationship:**
- `daycare: Mapped[Daycare]` - relationship back to Daycare (line 39)

**Unique Constraint:**
- `email`: Unique globally (line 24), NOT scoped to daycare
- **Note:** This is a potential issue - email uniqueness is global, not per-daycare

**JWT Token:**
- `daycare_id` is included in JWT token claims (see `backend/app/api/auth.py` line 144)

### Kid (Child)

**File:** `backend/app/models/kid.py` (lines 44-93)

**Daycare Reference:**
- `daycare_id`: `UUID(as_uuid=False)`, nullable=False, ForeignKey to `daycares.id` with CASCADE delete
- **Line 52-56:** `daycare_id: Mapped[str] = mapped_column(UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False)`

**Relationship:**
- `daycare: Mapped[Daycare]` - relationship back to Daycare (line 82)

**Additional FK:**
- `group_id`: ForeignKey to `groups.id` (groups also belong to daycare)

### Group

**File:** `backend/app/models/group.py` (lines 18-47)

**Daycare Reference:**
- `daycare_id`: `UUID(as_uuid=False)`, nullable=False, ForeignKey to `daycares.id` with CASCADE delete
- **Line 25-29:** `daycare_id: Mapped[str] = mapped_column(UUID(as_uuid=False), ForeignKey("daycares.id", ondelete="CASCADE"), nullable=False)`

**Relationship:**
- `daycare: Mapped[Daycare]` - relationship back to Daycare (line 38)

**Unique Constraint:**
- `uq_groups_daycare_name`: Unique constraint on `(daycare_id, name)` - ensures group name uniqueness per daycare
- **Migration:** `backend/alembic/versions/3009d0ea1444_add_allergies_and_need_to_know_columns_.py` (line 69)

### Conversation (Messaging)

**File:** `backend/app/models/messaging.py` (lines 38-80)

**Daycare Reference:**
- `daycare_id`: `Text`, nullable=False, indexed
- **Line 49:** `daycare_id = Column(Text, nullable=False, index=True)`
- **Note:** Uses `Text` type, NOT a ForeignKey to `daycares.id` - this is a data integrity issue

**Migration:** `backend/alembic/versions/7b3f1700ae94_add_messaging_tables.py` (line 29)
- Created conversations table with `daycare_id` as `Text` (not FK)

**Index:**
- `ix_conversations_daycare_id`: Index on `daycare_id` (line 35)
- `ix_conversations_daycare_type_hash`: Composite index on `(daycare_id, type, direct_key_hash)` (line 38)

**Missing:**
- No ForeignKey constraint to `daycares.id` - conversations can reference non-existent daycares

### Event

**File:** `backend/app/models/event.py` (lines 8-38)

**Daycare Reference:**
- **NONE** - Events table does NOT have a `daycare_id` column
- Events are global, not scoped to daycare

## 3) Seeds / Fixtures

### Seeder Service

**File:** `backend/app/services/seeder.py`

**Function:** `seed_daycare_data(db: Session)` (lines 12-53)
- Creates 1 daycare: "Happy Kids Daycare" (line 24)
- Creates 3 groups linked to daycare (lines 30-40)
- Creates parents linked to daycare via `insert_dummy_parents()` (line 43)
- Creates kids linked to daycare via `insert_dummy_kids()` (line 46)
- **Returns:** Daycare ID is returned implicitly via the created `Daycare` object

**Function:** `insert_dummy_parents(db: Session, daycare_id: str)` (lines 55-138)
- Creates 6 parents with `daycare_id` parameter (line 126)
- All parents are linked to the same daycare

**Function:** `insert_dummy_kids(db: Session, daycare_id: str, groups: list[Group], parents: list[Parent])` (lines 141-335)
- Creates 9 kids with `daycare_id` parameter (line 301)
- All kids are linked to the same daycare

### Educator Service

**File:** `backend/app/services/educator_service.py`

**Function:** `insert_dummy_educators(db: Session)` (lines 10-101)
- Gets or creates default daycare (lines 24-30)
- Creates educators linked to daycare (line 130 in auth.py shows educator.daycare_id is used)

### Test Fixtures

**File:** `backend/tests/conftest.py`

**Fixture:** `seeded_daycare_id()` (lines 77-94)
- Seeds database via `seed_daycare_data(db)` (line 87)
- Seeds educators via `insert_dummy_educators(db)` (line 88)
- Returns the first daycare's ID as string (line 92)

### "default-daycare-id" Resolution

**File:** `backend/app/utils/daycare_resolver.py` (lines 9-19)

**Function:** `resolve_daycare_id(db: Session, daycare_id: str) -> str`
- **Purpose:** Maps synthetic `"default-daycare-id"` string to actual UUID in local/test environments
- **Logic:**
  - If `APP_ENV` is `"local"` or `"test"` AND `daycare_id == "default-daycare-id"`:
    - Query for first daycare in database
    - If none exists, create one: `Daycare(id=str(uuid.uuid4()), name="Local Dev Daycare")`
    - Return the actual UUID
  - Otherwise, return `daycare_id` as-is

**Usage:**
- Used in `backend/app/api/auth.py`:
  - Line 130: `daycare_id = resolve_daycare_id(db, str(edu.daycare_id))`
  - Line 139: `daycare_id = resolve_daycare_id(db, str(par.daycare_id))`

**Test Usage:**
- `backend/tests/test_identity_endpoints.py` (line 192-195): Tests that `"default-daycare-id"` maps to seeded daycare

**Conclusion:**
- `"default-daycare-id"` is a **synthetic placeholder** used by Android app in dev
- It is **NOT persisted** in the database
- It is **resolved at runtime** to the first daycare's UUID in local/test environments
- In production, actual UUIDs must be used

## 4) Conclusions

### Does a daycare table already exist?

**YES** - The `daycares` table exists.

**Evidence:**
- Model: `backend/app/models/daycare.py` (lines 20-51)
- Migration: `backend/alembic/versions/7290bdad5686_add_multi_daycare_schema_with_.py` (created 2025-09-20)
- Table name: `daycares`
- Primary key: `id` (UUID as string)

### Is it already acting as tenant?

**YES** - Daycare is already functioning as a tenant.

**Evidence:**
1. **All user entities reference daycare:**
   - `Educator.daycare_id` (FK, NOT NULL)
   - `Parent.daycare_id` (FK, NOT NULL)
   - `Kid.daycare_id` (FK, NOT NULL)
   - `Group.daycare_id` (FK, NOT NULL)

2. **Unique constraints are scoped to daycare:**
   - `uq_educators_daycare_email`: Email unique per daycare
   - `uq_groups_daycare_name`: Group name unique per daycare

3. **JWT tokens include daycare_id:**
   - Token claims: `{"sub": user_id, "role": role, "daycare_id": daycare_id, "groups": groups}`
   - See `backend/app/api/auth.py` line 144

4. **Cascade deletes enforce tenant boundaries:**
   - All FKs use `ondelete="CASCADE"` - deleting a daycare removes all related entities

5. **Data isolation:**
   - All queries can be filtered by `daycare_id`
   - Relationships enforce that users belong to exactly one daycare

**Exception:**
- `Conversation.daycare_id` is `Text` type without FK constraint (data integrity issue, but still scoped to daycare)
- `Event` table has NO `daycare_id` - events are global (potential multi-tenancy issue)

### What is missing to satisfy "User belongs to tenant"?

**Mostly complete, but with gaps:**

#### ✅ Already Satisfied:

1. **Schema:** All user entities (`Educator`, `Parent`, `Kid`) have `daycare_id` FK (NOT NULL)
2. **Constraints:** Unique constraints are daycare-scoped where appropriate
3. **JWT:** `daycare_id` is included in token claims
4. **Relationships:** SQLAlchemy relationships enforce referential integrity

#### ❌ Missing or Incomplete:

1. **Conversation FK Constraint:**
   - `Conversation.daycare_id` is `Text` without FK to `daycares.id`
   - **File:** `backend/app/models/messaging.py` line 49
   - **Risk:** Can reference non-existent daycares, no cascade delete
   - **Fix needed:** Add FK constraint in migration

2. **Event Multi-tenancy:**
   - `Event` table has NO `daycare_id` column
   - **File:** `backend/app/models/event.py` (lines 8-38)
   - **Risk:** Events are global, not tenant-scoped
   - **Fix needed:** Add `daycare_id` FK to events table if events should be tenant-scoped

3. **Parent Email Uniqueness:**
   - `Parent.email` is globally unique, not daycare-scoped
   - **File:** `backend/app/models/parent.py` line 24
   - **Risk:** Same email cannot be used in different daycares
   - **Fix needed:** Change to composite unique constraint `(daycare_id, email)` if parents can reuse emails across daycares

4. **API Endpoint Filtering:**
   - Some endpoints may not enforce daycare filtering from JWT
   - **Note:** Need to audit API endpoints separately to verify all queries filter by `daycare_id` from token

### Recommendation

**REUSE existing `daycares` table** - it is already functioning as a tenant table.

**Action Items:**
1. Fix `Conversation.daycare_id` to be a proper FK (migration needed)
2. Decide if `Event` should be tenant-scoped (add `daycare_id` if yes)
3. Decide if `Parent.email` should be globally unique or per-daycare (change constraint if needed)
4. Audit API endpoints to ensure all queries filter by `daycare_id` from JWT token

**No new tenant table needed** - the existing `daycares` table serves this purpose.

