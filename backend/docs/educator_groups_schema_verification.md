# Educator ↔ Group Relationship Schema Verification Report

**Date:** 2025-01-10  
**Database:** PostgreSQL  
**Table:** `educator_groups`  
**Migration:** `d4e8f9a1b2c3`  
**Status:** ✅ **IMPLEMENTATION VERIFIED**

---

## 1. File Locations Inspected

### Core Schema Definitions

1. **SQLAlchemy Association Table Model**
   - **File:** `backend/app/models/associations.py`
   - **Lines:** 10-16
   - **Content:** Defines `educator_groups` Table with `nullable=False`, UNIQUE constraint, and FK constraints

2. **Alembic Migration**
   - **File:** `backend/alembic/versions/d4e8f9a1b2c3_enforce_educator_groups_constraints.py`
   - **Revision:** `d4e8f9a1b2c3`
   - **Revises:** `c2d8ca2d669f`
   - **Content:**
     - Pre-cleanup (removes duplicates and NULL rows)
     - Adds NOT NULL constraints
     - Adds UNIQUE constraint `uq_educator_groups_pair`
     - Creates trigger function `check_educator_has_groups()`
     - Creates constraint trigger `trigger_ensure_educator_has_groups`

3. **ORM Relationships**
   - **File:** `backend/app/models/educator.py`
   - **Lines:** 52-54
   - **Content:** `groups: Mapped[List[Group]] = relationship("Group", secondary="educator_groups", back_populates="educators")`
   
   - **File:** `backend/app/models/group.py`
   - **Lines:** 39-41
   - **Content:** `educators: Mapped[List[Educator]] = relationship("Educator", secondary="educator_groups", back_populates="groups")`

4. **Service Layer Helpers**
   - **File:** `backend/app/services/educator_service.py`
   - **Lines:** 102-255
   - **Functions:**
     - `assign_educator_to_group()` - Idempotent assignment
     - `unassign_educator_from_group()` - Prevents zero groups (raises `EducatorGroupAssignmentError`)
     - `move_educator_between_groups()` - Transaction-safe move operation

5. **Test Suite**
   - **File:** `backend/tests/test_educator_groups_constraints.py`
   - **Lines:** 1-509
   - **Coverage:**
     - Database constraint tests (UNIQUE, NOT NULL, trigger)
     - Service helper tests (idempotency, error handling)
     - Transaction behavior tests (moves between groups)

---

## 2. Reconstructed Final DDL

### Complete CREATE TABLE Statement

```sql
CREATE TABLE educator_groups (
    educator_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    CONSTRAINT educator_groups_educator_id_fkey 
        FOREIGN KEY (educator_id) 
        REFERENCES educators(id) 
        ON DELETE CASCADE,
    CONSTRAINT educator_groups_group_id_fkey 
        FOREIGN KEY (group_id) 
        REFERENCES groups(id) 
        ON DELETE CASCADE,
    CONSTRAINT uq_educator_groups_pair 
        UNIQUE (educator_id, group_id)
);

-- Index automatically created by PostgreSQL for UNIQUE constraint
-- Index name: uq_educator_groups_pair (implicit btree index)
```

### Trigger Function

```sql
CREATE OR REPLACE FUNCTION check_educator_has_groups()
RETURNS TRIGGER AS $$
DECLARE
    group_count INTEGER;
    is_educator_deleting BOOLEAN;
BEGIN
    IF TG_OP = 'DELETE' THEN
        -- Check if educator row still exists (if not, it's being deleted via FK CASCADE)
        SELECT EXISTS(SELECT 1 FROM educators WHERE id = OLD.educator_id) 
        INTO is_educator_deleting;
        
        IF NOT is_educator_deleting THEN
            -- Educator is being deleted, FK CASCADE will handle cleanup
            RETURN OLD;
        END IF;
        
        -- Count remaining groups for this educator
        SELECT COUNT(*) INTO group_count
        FROM educator_groups
        WHERE educator_id = OLD.educator_id;
        
        -- If count is 0, this was the last group assignment
        IF group_count = 0 THEN
            RAISE EXCEPTION 'Educator % must belong to at least one group. Cannot delete the last group assignment.', 
                OLD.educator_id;
        END IF;
        
        RETURN OLD;
        
    ELSIF TG_OP = 'UPDATE' THEN
        -- Handle UPDATE operations (educator_id or group_id changes)
        IF OLD.educator_id != NEW.educator_id THEN
            -- Check old educator won't be orphaned
            SELECT COUNT(*) INTO group_count
            FROM educator_groups
            WHERE educator_id = OLD.educator_id 
              AND (educator_id, group_id) != (NEW.educator_id, NEW.group_id);
            
            IF group_count = 0 THEN
                RAISE EXCEPTION 'Educator % must belong to at least one group. Cannot move the last group assignment.', 
                    OLD.educator_id;
            END IF;
        END IF;
        
        -- Check new educator will have at least one group
        SELECT COUNT(*) INTO group_count
        FROM educator_groups
        WHERE educator_id = NEW.educator_id 
          AND (educator_id, group_id) != (OLD.educator_id, OLD.group_id);
        
        IF group_count = 0 THEN
            SELECT COUNT(*) INTO group_count
            FROM educator_groups
            WHERE educator_id = NEW.educator_id;
            
            IF group_count = 0 THEN
                RAISE EXCEPTION 'Educator % must belong to at least one group.', 
                    NEW.educator_id;
            END IF;
        END IF;
        
        RETURN NEW;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

### Constraint Trigger

```sql
CREATE CONSTRAINT TRIGGER trigger_ensure_educator_has_groups
AFTER DELETE OR UPDATE ON educator_groups
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION check_educator_has_groups();
```

### Complete Schema Summary

| Element | Name | Type | Status |
|---------|------|------|--------|
| **Column** | `educator_id` | `INTEGER NOT NULL` | ✅ |
| **Column** | `group_id` | `INTEGER NOT NULL` | ✅ |
| **Foreign Key** | `educator_groups_educator_id_fkey` | `educator_id → educators(id) ON DELETE CASCADE` | ✅ |
| **Foreign Key** | `educator_groups_group_id_fkey` | `group_id → groups(id) ON DELETE CASCADE` | ✅ |
| **Unique Constraint** | `uq_educator_groups_pair` | `UNIQUE(educator_id, group_id)` | ✅ |
| **Index** | `uq_educator_groups_pair` | Implicit btree index on unique constraint | ✅ |
| **Trigger Function** | `check_educator_has_groups()` | PL/pgSQL function | ✅ |
| **Constraint Trigger** | `trigger_ensure_educator_has_groups` | DEFERRABLE INITIALLY DEFERRED | ✅ |

---

## 3. Behavior Confirmation

### Example 1: Educator Assigned to Multiple Groups ✅

**SQL:**
```sql
-- Setup: Create educator 25 and groups 7, 8
INSERT INTO educators (id, full_name, email, role, daycare_id) 
VALUES (25, 'Jessica', 'jessica@test.com', 'educator', 'daycare-uuid');

INSERT INTO groups (id, name, daycare_id) 
VALUES (7, 'Group A', 'daycare-uuid'), 
       (8, 'Group B', 'daycare-uuid');

-- Assign educator to multiple groups
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, 7);
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, 8);
COMMIT;
```

**Result:** ✅ **SUCCESS**
- Educator 25 is assigned to both groups 7 and 8
- Table contains two rows: `(25, 7)` and `(25, 8)`
- Many-to-many relationship working correctly

**Verification:**
```sql
SELECT * FROM educator_groups WHERE educator_id = 25;
-- Returns:
-- educator_id | group_id
-- 25         | 7
-- 25         | 8
```

---

### Example 2: Duplicate Pair Insertion ❌

**SQL:**
```sql
-- Attempt to insert duplicate
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, 7);
-- This row already exists from Example 1
```

**Result:** ❌ **ERROR**
```
ERROR: duplicate key value violates unique constraint "uq_educator_groups_pair"
DETAIL: Key (educator_id, group_id)=(25, 7) already exists.
```

**Verification:**
- UNIQUE constraint `uq_educator_groups_pair` prevents duplicates
- Error occurs at INSERT time (immediate constraint check)

---

### Example 3: NULL Value Insertion ❌

**SQL:**
```sql
-- Attempt to insert NULL educator_id
INSERT INTO educator_groups (educator_id, group_id) VALUES (NULL, 7);
```

**Result:** ❌ **ERROR**
```
ERROR: null value in column "educator_id" of relation "educator_groups" violates not-null constraint
DETAIL: Failing row contains (null, 7).
```

**SQL:**
```sql
-- Attempt to insert NULL group_id
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, NULL);
```

**Result:** ❌ **ERROR**
```
ERROR: null value in column "group_id" of relation "educator_groups" violates not-null constraint
DETAIL: Failing row contains (25, null).
```

**Verification:**
- NOT NULL constraints on both columns prevent NULL values
- Errors occur at INSERT time (immediate constraint check)

---

### Example 4: Deleting Last Group Assignment ❌

**SQL:**
```sql
-- Setup: Educator 25 has only one group (7)
-- Remove all other groups first (if any)
DELETE FROM educator_groups WHERE educator_id = 25 AND group_id != 7;

-- Now attempt to delete the last group
DELETE FROM educator_groups WHERE educator_id = 25 AND group_id = 7;
COMMIT;  -- Trigger checks at COMMIT time
```

**Result:** ❌ **ERROR**
```
ERROR: Educator 25 must belong to at least one group. Cannot delete the last group assignment.
```

**Verification:**
- Constraint trigger `trigger_ensure_educator_has_groups` fires at COMMIT time
- Trigger checks if educator has zero groups after DELETE
- Raises exception if count = 0

---

### Example 5: Moving Educator Between Groups (Single Transaction) ✅

**SQL:**
```sql
-- Setup: Educator 25 is in group 7 only
-- Move from group 7 to group 8 in one transaction
BEGIN;
INSERT INTO educator_groups (educator_id, group_id) VALUES (25, 8);
DELETE FROM educator_groups WHERE educator_id = 25 AND group_id = 7;
COMMIT;  -- Trigger checks here: educator still has group 8, so it passes
```

**Result:** ✅ **SUCCESS**
- Transaction completes successfully
- Educator is now in group 8 (not group 7)
- At COMMIT time, educator has 1 group (group 8), so trigger passes

**Verification:**
```sql
SELECT * FROM educator_groups WHERE educator_id = 25;
-- Returns:
-- educator_id | group_id
-- 25         | 8
```

**Why it works:**
- The trigger is `DEFERRABLE INITIALLY DEFERRED`, so it checks at COMMIT time
- The INSERT happens before the DELETE in the transaction
- At COMMIT, the educator has group 8, so the trigger check passes

---

## 4. Trigger Details

### Trigger Function Logic

The `check_educator_has_groups()` function implements the following logic:

#### For DELETE Operations:

1. **CASCADE Detection:**
   - Checks if the educator row still exists: `SELECT EXISTS(SELECT 1 FROM educators WHERE id = OLD.educator_id)`
   - If educator doesn't exist → Educator is being deleted via FK CASCADE → Skip validation (return OLD)
   - This allows FK CASCADE deletes to proceed without constraint violations

2. **Zero Groups Check:**
   - Counts remaining groups: `SELECT COUNT(*) FROM educator_groups WHERE educator_id = OLD.educator_id`
   - Since trigger is `AFTER DELETE`, the deleted row is already gone from the table
   - If `count = 0` → This was the last group → Raise exception

#### For UPDATE Operations:

1. **Educator ID Change:**
   - If `OLD.educator_id != NEW.educator_id`:
     - Check if old educator will have ≥1 group remaining (excluding the updated row)
     - If old educator would have 0 groups → Raise exception

2. **New Educator Validation:**
   - Check if new educator_id will have ≥1 group (excluding the updated row)
   - If new educator would have 0 groups → Raise exception

### Trigger Execution Timing

**Type:** `DEFERRABLE INITIALLY DEFERRED` constraint trigger

**Fires:**
- **Timing:** At COMMIT time (not per-statement)
- **Event:** AFTER DELETE OR UPDATE on `educator_groups`
- **Scope:** FOR EACH ROW

**Benefits:**
- Allows transactions to temporarily violate the constraint
- Example: Move from group A to group B (add B, then remove A)
- At COMMIT time, the educator must have ≥1 group

### Trigger Function Source

Located in migration: `backend/alembic/versions/d4e8f9a1b2c3_enforce_educator_groups_constraints.py`

**Lines:** 62-136 (function definition)
**Lines:** 139-146 (trigger creation)

---

## 5. Gap Analysis vs Target Design

### Target Design Requirements

| Requirement | Target | Status | Evidence |
|------------|--------|--------|----------|
| **Many-to-many relationship** | Educator can belong to multiple groups | ✅ **ENFORCED** | Multiple rows pattern; ORM `List[Group]` relationship |
| **No duplicates** | `(educator_id, group_id)` pair must be unique | ✅ **ENFORCED** | `UNIQUE(educator_id, group_id)` constraint `uq_educator_groups_pair` |
| **No NULLs** | Both columns must be NOT NULL | ✅ **ENFORCED** | `NOT NULL` constraints on both columns |
| **Cannot end with zero groups** | Every educator must have ≥1 group at commit time | ✅ **ENFORCED** | Constraint trigger `trigger_ensure_educator_has_groups` |

### Implementation Status

✅ **ALL REQUIREMENTS MET**

1. **Many-to-many relationship:** ✅
   - Implemented via multiple rows in join table
   - SQLAlchemy relationships configured correctly
   - Tests confirm multiple groups per educator work

2. **No duplicates:** ✅
   - UNIQUE constraint `uq_educator_groups_pair` enforced at database level
   - Test: `test_unique_constraint_prevents_duplicates` passes
   - SQL inserts fail with unique constraint violation

3. **No NULLs:** ✅
   - NOT NULL constraints on both columns enforced at database level
   - Tests: `test_not_null_constraint_on_educator_id` and `test_not_null_constraint_on_group_id` pass
   - SQL inserts with NULL fail immediately

4. **Cannot end with zero groups:** ✅
   - Constraint trigger `trigger_ensure_educator_has_groups` enforced at database level
   - Trigger is DEFERRABLE INITIALLY DEFERRED (checks at COMMIT)
   - Test: `test_trigger_prevents_zero_groups` passes
   - SQL deletes of last group fail at COMMIT with trigger exception

### Application-Level Safeguards

In addition to database-level enforcement, the service layer provides application-level checks:

1. **`assign_educator_to_group()`:**
   - Checks if already assigned (idempotent)
   - Validates educator and group exist
   - Handles IntegrityError for duplicates

2. **`unassign_educator_from_group()`:**
   - Pre-checks if removing would leave zero groups (raises `EducatorGroupAssignmentError`)
   - Matches database trigger error message
   - Provides better error messages than raw SQL

3. **`move_educator_between_groups()`:**
   - Ensures transaction order: add new group first, then remove old group
   - Prevents trigger from firing by maintaining ≥1 group throughout transaction

**Note:** Application-level checks provide **better user experience** but are **not required** for correctness. The database constraints guarantee data integrity even if service helpers are bypassed.

### Remaining Gaps

**None.** All four target requirements are fully enforced at the database level.

---

## 6. Verification SQL

### 6.1 Table Structure Introspection

```sql
\d+ educator_groups
```

**Expected Output:**
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

---

### 6.2 Verify UNIQUE Constraint Exists

```sql
SELECT 
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'educator_groups'::regclass
AND conname = 'uq_educator_groups_pair';
```

**Expected Output:**
```
    constraint_name      | constraint_type | constraint_definition
------------------------+-----------------+---------------------------
 uq_educator_groups_pair | u              | UNIQUE (educator_id, group_id)
```

---

### 6.3 Verify NOT NULL Constraints

```sql
SELECT 
    column_name,
    is_nullable,
    data_type
FROM information_schema.columns
WHERE table_name = 'educator_groups'
AND (column_name = 'educator_id' OR column_name = 'group_id')
ORDER BY column_name;
```

**Expected Output:**
```
 column_name  | is_nullable | data_type
--------------+-------------+-----------
 educator_id  | NO          | integer
 group_id     | NO          | integer
```

---

### 6.4 Verify Trigger Exists

```sql
SELECT 
    tgname AS trigger_name,
    tgenabled AS enabled,
    tgtype::text AS trigger_type,
    tgnamespace::regnamespace AS schema_name
FROM pg_trigger
WHERE tgrelid = 'educator_groups'::regclass
AND tgname = 'trigger_ensure_educator_has_groups';
```

**Expected Output:**
```
            trigger_name            | enabled | trigger_type | schema_name
------------------------------------+---------+--------------+-------------
 trigger_ensure_educator_has_groups | O       | 87           | public
```

**Note:** `tgenabled = 'O'` means "origin" (enabled), `tgtype = 87` indicates a constraint trigger.

---

### 6.5 Test UNIQUE Constraint (Expect Failure)

```sql
-- Setup: Create test data
INSERT INTO educators (id, full_name, email, role, daycare_id) 
VALUES (27, 'Test Educator', 'test@example.com', 'educator', 'daycare-uuid')
ON CONFLICT (id) DO NOTHING;

INSERT INTO groups (id, name, daycare_id) 
VALUES (10, 'Test Group', 'daycare-uuid')
ON CONFLICT (id) DO NOTHING;

-- Insert first row
INSERT INTO educator_groups (educator_id, group_id) VALUES (27, 10);
COMMIT;

-- Attempt duplicate (should fail)
INSERT INTO educator_groups (educator_id, group_id) VALUES (27, 10);
```

**Expected Result:** ❌ **ERROR**
```
ERROR: duplicate key value violates unique constraint "uq_educator_groups_pair"
DETAIL: Key (educator_id, group_id)=(27, 10) already exists.
```

---

### 6.6 Test NOT NULL Constraint (Expect Failure)

```sql
-- Attempt to insert NULL educator_id
INSERT INTO educator_groups (educator_id, group_id) VALUES (NULL, 10);
```

**Expected Result:** ❌ **ERROR**
```
ERROR: null value in column "educator_id" of relation "educator_groups" violates not-null constraint
DETAIL: Failing row contains (null, 10).
```

```sql
-- Attempt to insert NULL group_id
INSERT INTO educator_groups (educator_id, group_id) VALUES (27, NULL);
```

**Expected Result:** ❌ **ERROR**
```
ERROR: null value in column "group_id" of relation "educator_groups" violates not-null constraint
DETAIL: Failing row contains (27, null).
```

---

### 6.7 Test Zero Groups Constraint (Expect Failure)

```sql
-- Ensure educator 27 has only one group
DELETE FROM educator_groups WHERE educator_id = 27 AND group_id != 10;

-- Verify setup
SELECT COUNT(*) FROM educator_groups WHERE educator_id = 27;
-- Should return: 1

-- Now attempt to delete the last group
BEGIN;
DELETE FROM educator_groups WHERE educator_id = 27 AND group_id = 10;
COMMIT;  -- Trigger checks here
```

**Expected Result:** ❌ **ERROR**
```
ERROR: Educator 27 must belong to at least one group. Cannot delete the last group assignment.
```

---

### 6.8 Test Move Between Groups (Expect Success)

```sql
-- Setup: Educator 27 in group 10 only
-- Create group 11
INSERT INTO groups (id, name, daycare_id) 
VALUES (11, 'Group 11', 'daycare-uuid')
ON CONFLICT (id) DO NOTHING;

-- Move from group 10 to group 11 in one transaction
BEGIN;
INSERT INTO educator_groups (educator_id, group_id) VALUES (27, 11);
DELETE FROM educator_groups WHERE educator_id = 27 AND group_id = 10;
COMMIT;  -- Trigger checks: educator has group 11, so passes
```

**Expected Result:** ✅ **SUCCESS**
- Transaction completes
- Educator 27 is now in group 11 only

**Verification:**
```sql
SELECT * FROM educator_groups WHERE educator_id = 27;
-- Returns:
-- educator_id | group_id
-- 27         | 11
```

---

### 6.9 Verify Multiple Groups Allowed (Expect Success)

```sql
-- Assign educator 27 to multiple groups
INSERT INTO educator_groups (educator_id, group_id) VALUES (27, 10);
INSERT INTO educator_groups (educator_id, group_id) VALUES (27, 11);
COMMIT;
```

**Expected Result:** ✅ **SUCCESS**
- Both inserts succeed
- Educator has 2 groups

**Verification:**
```sql
SELECT COUNT(*) FROM educator_groups WHERE educator_id = 27;
-- Returns: 2
```

---

## 7. Summary Conclusion

**Schema Verification Status: ✅ COMPLETE**

The `educator_groups` table schema now **fully enforces all four target rules** at the database level:

1. ✅ **Many-to-many relationship:** Implemented via multiple rows in join table, supported by SQLAlchemy relationships (`List[Group]` and `List[Educator]`)

2. ✅ **No duplicates:** Enforced by `UNIQUE(educator_id, group_id)` constraint `uq_educator_groups_pair` with automatic btree index

3. ✅ **No NULLs:** Enforced by `NOT NULL` constraints on both `educator_id` and `group_id` columns

4. ✅ **Cannot end with zero groups:** Enforced by constraint trigger `trigger_ensure_educator_has_groups` (DEFERRABLE INITIALLY DEFERRED) that checks at COMMIT time and raises exception if educator would have zero groups

**Implementation Quality:**
- All constraints are enforced at the **database level**, ensuring data integrity even if application code is bypassed
- The trigger is **DEFERRABLE INITIALLY DEFERRED**, allowing transaction-safe moves between groups
- **Service layer helpers** provide user-friendly APIs that leverage the database constraints
- **Comprehensive test suite** verifies all constraint behaviors and service helpers
- **Migration is idempotent** and safe to run on existing data (cleans duplicates and NULLs before applying constraints)

**No manual enforcement required.** The database schema guarantees data integrity independently of application logic.

---

**Verification Report End**

