# Educator ↔ Group Relationship Schema Report

**Date:** 2025-01-10  
**Database:** PostgreSQL  
**Table:** `educator_groups`

---

## Section 1: Found Files & Locations

### Primary Schema Definitions

1. **ORM Model (SQLAlchemy)**
   - `backend/app/models/associations.py` (lines 5-11)
     - Defines the `educator_groups` Table object using SQLAlchemy's declarative `Table()` constructor
   - `backend/app/models/educator.py` (lines 52-54)
     - Defines the relationship: `groups: Mapped[List[Group]] = relationship("Group", secondary="educator_groups", back_populates="educators")`
   - `backend/app/models/group.py` (lines 39-41)
     - Defines the reverse relationship: `educators: Mapped[List[Educator]] = relationship("Educator", secondary="educator_groups", back_populates="groups")`

2. **Database Migrations**
   - `backend/alembic/versions/7290bdad5686_add_multi_daycare_schema_with_.py` (lines 49-54)
     - **Original migration** that created the `educator_groups` table
     - Revision: `7290bdad5686`
     - Create date: 2025-09-20 21:29:49
   - `backend/alembic/versions/d4e8f9a1b2c3_enforce_educator_groups_constraints.py`
     - **Enforcement migration** that adds NOT NULL, UNIQUE, and constraint trigger
     - Revision: `d4e8f9a1b2c3`
     - Revises: `c2d8ca2d669f`
     - Create date: 2025-01-10

3. **Usage in Application Code**
   - `backend/app/services/educator_service.py`
     - Lines 89-96: Uses ORM relationships: `educator.groups.append(group)`
     - Lines 107-255: Service helpers: `assign_educator_to_group()`, `unassign_educator_from_group()`, `move_educator_between_groups()`
   - `backend/app/services/seeder.py` (line 343)
     - Direct SQL: `DELETE FROM educator_groups` (for cleanup)
   - `backend/tests/test_daycare_schema.py` (lines 203-206)
     - Test usage: `educator.groups.append(group1)`
   - `backend/tests/test_educator_groups_constraints.py`
     - Comprehensive tests for constraints and service helpers

### Related Tables

- **`educators`** table: Referenced by `educator_id` FK
  - Primary key: `id` (Integer)
  - Defined in: `backend/app/models/educator.py`
  - Migration: `backend/alembic/versions/09bfd1d87bd1_add_educators_table.py`

- **`groups`** table: Referenced by `group_id` FK
  - Primary key: `id` (Integer)
  - Defined in: `backend/app/models/group.py`
  - Migration: `backend/alembic/versions/7290bdad5686_add_multi_daycare_schema_with_.py`

---

## Section 2: Current DDL (After Migration d4e8f9a1b2c3)

### CREATE TABLE Statement

```sql
CREATE TABLE educator_groups (
    educator_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    FOREIGN KEY (educator_id) REFERENCES educators(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_educator_groups_pair UNIQUE (educator_id, group_id)
);

-- Constraint trigger function
CREATE OR REPLACE FUNCTION check_educator_has_groups()
RETURNS TRIGGER AS $$
-- Ensures every educator has at least one group at commit time
$$ LANGUAGE plpgsql;

-- Constraint trigger (DEFERRABLE INITIALLY DEFERRED)
CREATE CONSTRAINT TRIGGER trigger_ensure_educator_has_groups
AFTER DELETE OR UPDATE ON educator_groups
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION check_educator_has_groups();
```

### Detailed Column Specifications

| Column Name | Data Type | Nullable | Default | Constraints |
|-------------|-----------|----------|---------|-------------|
| `educator_id` | `INTEGER` | **NO** | `NULL` | FK to `educators.id` (ON DELETE CASCADE), NOT NULL |
| `group_id` | `INTEGER` | **NO** | `NULL` | FK to `groups.id` (ON DELETE CASCADE), NOT NULL |

### Keys & Constraints

- **Primary Key:** ❌ **NONE** (composite primary key not defined)
- **Foreign Keys:**
  - `educator_id` → `educators.id` (ON DELETE CASCADE)
  - `group_id` → `groups.id` (ON DELETE CASCADE)
- **Unique Constraints:** ✅ **uq_educator_groups_pair** (`UNIQUE(educator_id, group_id)`)
- **Indexes:** ✅ **Implicit index on UNIQUE constraint** (PostgreSQL creates this automatically)
- **Check Constraints:** ❌ **NONE**
- **Triggers:** ✅ **trigger_ensure_educator_has_groups** (constraint trigger, DEFERRABLE INITIALLY DEFERRED)

### Notes on Nullability

Both columns are **NOT NULL** (enforced by migration `d4e8f9a1b2c3`). This means:
- A row **cannot** have `NULL` for `educator_id` or `group_id`
- All rows must have valid foreign key references

---

## Section 3: How "Educator in Multiple Groups" is Represented

### Pattern: Multiple Rows for One Educator

The relationship is modeled as a **many-to-many join table** using the **multiple rows pattern**:

- **One educator** → **Multiple groups**: One row per group assignment
- Example: If educator ID `25` is assigned to groups `7` and `8`, the table contains:
  ```
  educator_id | group_id
  ------------+----------
  25         | 7
  25         | 8
  ```

### Visual Example

If educator "Jessica" (ID: 25) belongs to "Group A" (ID: 7) and "Group B" (ID: 8):

```sql
SELECT * FROM educator_groups WHERE educator_id = 25;
-- Returns 2 rows:
-- educator_id | group_id
-- 25         | 7
-- 25         | 8
```

### SQLAlchemy ORM Usage

The application code uses SQLAlchemy's relationship API (never inserts raw rows):

```python
# Assign educator to multiple groups
educator.groups.append(group1)
educator.groups.append(group2)
db.commit()
```

This generates INSERT statements into `educator_groups` automatically.

---

## Section 4: Current Constraints & What They Allow

### What the Current Schema Allows

1. **Duplicate Pairs: ✅ YES**
   - No UNIQUE constraint on `(educator_id, group_id)`
   - The same educator can be "assigned" to the same group multiple times
   - Example: You could have:
     ```
     educator_id | group_id
     ------------+----------
     25         | 7
     25         | 7  ← Duplicate!
     ```

2. **NULL Values: ✅ YES**
   - Both `educator_id` and `group_id` are nullable
   - Allows rows with `NULL` values:
     - `(NULL, 7)` → orphaned group assignment
     - `(25, NULL)` → orphaned educator assignment
     - `(NULL, NULL)` → completely orphaned row

3. **Zero Groups: ✅ YES** (at DB level)
   - No CHECK constraint or trigger enforces "at least one group"
   - An educator can have **zero rows** in `educator_groups`
   - Example: Newly created educator with no assignments → 0 groups

4. **ON DELETE CASCADE Behavior:**
   - **If an educator is deleted:** All rows in `educator_groups` with that `educator_id` are automatically deleted
   - **If a group is deleted:** All rows in `educator_groups` with that `group_id` are automatically deleted
   - **Prevents orphaned FK references** (if the FK columns are non-NULL)

### What the Current Schema Prevents

- ❌ **Prevents duplicate pairs:** No (duplicates allowed)
- ❌ **Prevents NULL values:** No (both columns nullable)
- ❌ **Prevents zero groups:** No (no DB-level enforcement)
- ✅ **Prevents orphaned FKs (when non-NULL):** Yes (via ON DELETE CASCADE, if FKs are non-NULL)

---

## Section 5: Gaps vs Desired Rules

### Desired Policy

1. ✅ An educator may belong to multiple groups (already supported via multiple rows)
2. ✅ It must be easy to add/remove/change group links (already supported via ORM)
3. ❌ Every educator must belong to at least one group (no NULL, and no "zero rows" case)

### Comparison: Current vs Desired

| Requirement | Current State | Gap |
|------------|---------------|-----|
| **Multiple groups per educator** | ✅ Supported (multiple rows) | None |
| **Easy add/remove/change** | ✅ Supported (ORM `append`/`remove`) | None |
| **No duplicates** | ❌ Not enforced | **MISSING: UNIQUE(educator_id, group_id)** |
| **No NULL values** | ❌ Both columns nullable | **MISSING: NOT NULL constraints** |
| **At least one group per educator** | ❌ Not enforced | **MISSING: DB-level constraint or app logic** |

### Detailed Gap Analysis

#### Gap 1: Missing UNIQUE Constraint

**Problem:** Duplicate `(educator_id, group_id)` pairs can exist.

**Impact:**
- Wastes storage
- Can cause confusion in queries (e.g., `COUNT(*)` returns inflated counts)
- Application logic must manually deduplicate

**Solution Needed:**
```sql
ALTER TABLE educator_groups
ADD CONSTRAINT uq_educator_group UNIQUE (educator_id, group_id);
```

#### Gap 2: Missing NOT NULL Constraints

**Problem:** Both columns are nullable, allowing orphaned rows.

**Impact:**
- `(NULL, 7)` → meaningless "someone assigned to group 7, but who?"
- `(25, NULL)` → meaningless "educator 25 assigned to some group, but which?"
- `(NULL, NULL)` → completely meaningless row

**Solution Needed:**
```sql
ALTER TABLE educator_groups
ALTER COLUMN educator_id SET NOT NULL,
ALTER COLUMN group_id SET NOT NULL;
```

#### Gap 3: Missing "At Least One Group" Constraint

**Problem:** No DB-level enforcement that each educator has ≥1 group.

**Impact:**
- Educators can exist with zero group assignments
- Must be handled in application logic or with a CHECK constraint / trigger

**Solution Options:**

**Option A: Application-level validation**
- Validate in service layer before committing
- Simpler but not DB-enforced

**Option B: CHECK constraint with subquery (PostgreSQL)**
- More complex, may impact performance

**Option C: Database trigger**
- Trigger on INSERT/UPDATE to ensure educator always has ≥1 group
- More robust but adds complexity

**Option D: Partial unique index + NOT NULL (if we want "exactly one group")**
- Not applicable here (we want "at least one", not "exactly one")

---

## Section 5.5: Decisions & Constraints (Implemented Solution)

### Migration: `d4e8f9a1b2c3_enforce_educator_groups_constraints`

**Implemented:** January 2025

#### Decision: Database-Level Enforcement

We chose **Option C: Database trigger** (with application-level validation as a safety net) to enforce the "at least one group per educator" rule. This provides:

1. **DB-level guarantee**: No application can bypass the rule (even direct SQL)
2. **Transaction safety**: DEFERRABLE INITIALLY DEFERRED ensures the check happens at commit time, allowing moves between groups in one transaction
3. **Clear error messages**: The trigger raises PostgreSQL exceptions that are caught and translated by service helpers

#### Constraints Implemented

1. **NOT NULL on both columns**
   - Prevents orphaned rows with NULL values
   - Enforced via `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL`

2. **UNIQUE(educator_id, group_id)**
   - Prevents duplicate pairs
   - Named constraint: `uq_educator_groups_pair`
   - PostgreSQL automatically creates an index on this constraint

3. **Constraint trigger: "at least one group per educator"**
   - Function: `check_educator_has_groups()`
   - Trigger: `trigger_ensure_educator_has_groups` (DEFERRABLE INITIALLY DEFERRED)
   - Fires: AFTER DELETE OR UPDATE
   - Behavior:
     - Checks if educator would have zero groups at commit time
     - Allows moves between groups (add new, then remove old)
     - Skips check if educator row is being deleted (FK CASCADE handles cleanup)
     - Raises exception: `"Educator {id} must belong to at least one group. Cannot delete the last group assignment."`

#### Service Helpers

Located in `backend/app/services/educator_service.py`:

- **`assign_educator_to_group(educator_id, group_id)`**
  - Idempotent: no-op if already assigned
  - Handles UNIQUE constraint violations gracefully

- **`unassign_educator_from_group(educator_id, group_id)`**
  - Raises `EducatorGroupAssignmentError` if this would leave educator with zero groups
  - Error message matches the database trigger message

- **`move_educator_between_groups(educator_id, from_group_id, to_group_id)`**
  - Single transaction: adds new group first, then removes old group
  - Ensures educator always has ≥1 group at commit time
  - Prevents trigger from firing

#### How to "Move" an Educator Between Groups

**Transaction order matters!**

```python
# ✅ CORRECT: Add new group first, then remove old group
educator.groups.append(to_group)
educator.groups.remove(from_group)
db.commit()

# ❌ WRONG: Remove old group first (would trigger constraint violation)
educator.groups.remove(from_group)  # If this is the last group, trigger fires!
educator.groups.append(to_group)
db.commit()

# ✅ BEST: Use service helper (handles edge cases)
from app.services.educator_service import move_educator_between_groups
move_educator_between_groups(db, educator_id, from_group_id, to_group_id)
```

#### Example SQL (Direct Database Access)

```sql
-- Assign educator 25 to group 7
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, 7);

-- Move educator 25 from group 7 to group 8 (in one transaction)
BEGIN;
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, 8);
DELETE FROM educator_groups WHERE educator_id = 25 AND group_id = 7;
COMMIT;  -- Trigger checks here: educator still has ≥1 group, so it passes

-- ❌ This would fail:
DELETE FROM educator_groups WHERE educator_id = 25 AND group_id = 7;
-- Error: "Educator 25 must belong to at least one group. Cannot delete the last group assignment."
```

#### Example cURL (API Usage)

```bash
# Assign educator to group (would need API endpoint)
# POST /api/v1/educators/{educator_id}/groups/{group_id}

# Unassign educator from group (would fail if last group)
# DELETE /api/v1/educators/{educator_id}/groups/{group_id}

# Move educator between groups
# PUT /api/v1/educators/{educator_id}/groups/move
# Body: {"from_group_id": 7, "to_group_id": 8}
```

---

## Section 6: Verification SQL

### 6.1 Table Structure Introspection

```sql
-- PostgreSQL: Get detailed table structure
\d+ educator_groups
```

**Expected Output Structure (After Migration):**
```
                                    Table "public.educator_groups"
    Column     |  Type   | Collation | Nullable | Default | Storage | Stats target | Description 
---------------+---------+-----------+----------+---------+---------+--------------+-------------
 educator_id   | integer |           | no       | null    | plain   |              | 
 group_id      | integer |           | no       | null    | plain   |              | 
Indexes:
    "uq_educator_groups_pair" UNIQUE CONSTRAINT, btree (educator_id, group_id)
Foreign-key constraints:
    "educator_groups_educator_id_fkey" FOREIGN KEY (educator_id) REFERENCES educators(id) ON DELETE CASCADE
    "educator_groups_group_id_fkey" FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
Triggers:
    trigger_ensure_educator_has_groups AFTER DELETE OR UPDATE ON educator_groups DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_educator_has_groups()
```

### 6.2 Find Educator Assignments (Example: Jessica)

```sql
SELECT 
    eg.educator_id,
    e.full_name AS educator_name,
    eg.group_id,
    g.name AS group_name
FROM educator_groups eg
LEFT JOIN educators e ON eg.educator_id = e.id
LEFT JOIN groups g ON eg.group_id = g.id
WHERE e.full_name = 'Jessica'
ORDER BY eg.group_id;
```

**Expected:** Returns all groups assigned to Jessica (could be 0, 1, or many rows).

### 6.3 Detect Duplicate Pairs

```sql
SELECT 
    educator_id,
    group_id,
    COUNT(*) AS duplicate_count
FROM educator_groups
GROUP BY educator_id, group_id
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, educator_id, group_id;
```

**Expected:** Returns 0 rows (UNIQUE constraint `uq_educator_groups_pair` prevents duplicates).

**To verify the constraint exists:**
```sql
SELECT 
    conname AS constraint_name,
    contype AS constraint_type
FROM pg_constraint
WHERE conrelid = 'educator_groups'::regclass
AND conname = 'uq_educator_groups_pair';
-- Should return: uq_educator_groups_pair | u (unique constraint)
```

### 6.4 Detect NULL Values

```sql
SELECT 
    educator_id,
    group_id,
    CASE 
        WHEN educator_id IS NULL AND group_id IS NULL THEN 'Both NULL'
        WHEN educator_id IS NULL THEN 'educator_id is NULL'
        WHEN group_id IS NULL THEN 'group_id is NULL'
    END AS null_type
FROM educator_groups
WHERE educator_id IS NULL OR group_id IS NULL
ORDER BY null_type;
```

**Expected:** Returns 0 rows (NOT NULL constraints prevent NULL values).

**To verify NOT NULL constraints exist:**
```sql
SELECT 
    column_name,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'educator_groups'
AND (column_name = 'educator_id' OR column_name = 'group_id');
-- Should show: educator_id | NO, group_id | NO
```

### 6.5 Detect Educators with Zero Groups

```sql
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    COUNT(eg.group_id) AS group_count
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
GROUP BY e.id, e.full_name
HAVING COUNT(eg.group_id) = 0
ORDER BY e.id;
```

**Expected:** Returns 0 rows (constraint trigger prevents educators from having zero groups).

**To test the trigger manually:**
```sql
-- This should fail if educator 25 has only one group assignment:
BEGIN;
DELETE FROM educator_groups WHERE educator_id = 25 AND group_id = 7;
COMMIT;  -- Expected error: "Educator 25 must belong to at least one group. Cannot delete the last group assignment."
```

**To verify the trigger exists:**
```sql
SELECT 
    tgname AS trigger_name,
    tgenabled AS enabled,
    tgtype::text AS trigger_type
FROM pg_trigger
WHERE tgrelid = 'educator_groups'::regclass
AND tgname = 'trigger_ensure_educator_has_groups';
-- Should return the trigger details
```

### 6.6 Detect Groups with Zero Educators

```sql
SELECT 
    g.id AS group_id,
    g.name AS group_name,
    COUNT(eg.educator_id) AS educator_count
FROM groups g
LEFT JOIN educator_groups eg ON g.id = eg.group_id
GROUP BY g.id, g.name
HAVING COUNT(eg.educator_id) = 0
ORDER BY g.id;
```

**Expected:** Returns groups with no educators assigned (this may be acceptable, depending on business rules).

### 6.7 Summary Statistics

```sql
SELECT 
    'Total rows' AS metric,
    COUNT(*)::text AS value
FROM educator_groups
UNION ALL
SELECT 
    'Distinct educator_id' AS metric,
    COUNT(DISTINCT educator_id)::text AS value
FROM educator_groups
UNION ALL
SELECT 
    'Distinct group_id' AS metric,
    COUNT(DISTINCT group_id)::text AS value
FROM educator_groups
UNION ALL
SELECT 
    'Duplicate pairs' AS metric,
    (SELECT COUNT(*) FROM (
        SELECT educator_id, group_id
        FROM educator_groups
        GROUP BY educator_id, group_id
        HAVING COUNT(*) > 1
    ) AS duplicates)::text AS value
UNION ALL
SELECT 
    'Rows with NULL educator_id' AS metric,
    COUNT(*)::text AS value
FROM educator_groups
WHERE educator_id IS NULL
UNION ALL
SELECT 
    'Rows with NULL group_id' AS metric,
    COUNT(*)::text AS value
FROM educator_groups
WHERE group_id IS NULL
UNION ALL
SELECT 
    'Educators with zero groups' AS metric,
    (SELECT COUNT(*) FROM (
        SELECT e.id
        FROM educators e
        LEFT JOIN educator_groups eg ON e.id = eg.educator_id
        GROUP BY e.id
        HAVING COUNT(eg.group_id) = 0
    ) AS zero_groups)::text AS value;
```

---

## Summary

The `educator_groups` table is a **standard many-to-many join table** that currently:
- ✅ Supports multiple groups per educator (via multiple rows)
- ✅ Has proper ON DELETE CASCADE foreign keys
- ❌ **Lacks UNIQUE constraint** (allows duplicates)
- ❌ **Allows NULL values** (both columns nullable)
- ❌ **Does not enforce "at least one group"** rule

**Implementation Status (January 2025):**
✅ **COMPLETED** via migration `d4e8f9a1b2c3`:
1. ✅ Added `UNIQUE(educator_id, group_id)` constraint (`uq_educator_groups_pair`)
2. ✅ Added `NOT NULL` constraints to both columns
3. ✅ Added constraint trigger (`trigger_ensure_educator_has_groups`) to ensure every educator has ≥1 group
4. ✅ Added service helpers for safe CRUD operations
5. ✅ Added comprehensive tests

---

**Report End**

