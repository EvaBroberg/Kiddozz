-- Manual Verification SQL for Messaging Feature
-- Run these queries to verify the data used by the messaging feature
-- Expected scenario: Jessica (id=27), Sarah (id=24), Sara Johnson (parent id=10), kids 19/20 in group 7

-- 1. Verify Jessica (id=27) exists and her group assignments
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    e.email,
    eg.group_id,
    g.name AS group_name,
    e.daycare_id
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
LEFT JOIN groups g ON eg.group_id = g.id
WHERE e.id = 27
   OR e.full_name = 'Jessica'
ORDER BY eg.group_id;

-- Expected: Should show Jessica (id=27) assigned to group 7 (or group with id=7)

-- 2. Verify Sarah Davis (id=24) exists and her group assignments
SELECT 
    e.id AS educator_id,
    e.full_name AS educator_name,
    e.email,
    eg.group_id,
    g.name AS group_name,
    e.daycare_id
FROM educators e
LEFT JOIN educator_groups eg ON e.id = eg.educator_id
LEFT JOIN groups g ON eg.group_id = g.id
WHERE e.id = 24
   OR e.full_name LIKE '%Sarah%' OR e.full_name LIKE '%Davis%'
ORDER BY eg.group_id;

-- Expected: Should show Sarah Davis (id=24) and her group assignments

-- 3. Verify Sara Johnson (parent id=10) and her kids (19, 20) in group 7
SELECT 
    p.id AS parent_id,
    p.full_name AS parent_name,
    p.email AS parent_email,
    k.id AS kid_id,
    k.full_name AS kid_name,
    k.group_id AS kid_group_id,
    g.name AS group_name,
    k.daycare_id
FROM parents p
INNER JOIN parent_kids pk ON p.id = pk.parent_id
INNER JOIN kids k ON pk.kid_id = k.id
LEFT JOIN groups g ON k.group_id = g.id
WHERE p.id = 10
   OR p.full_name LIKE '%Sara%' OR p.full_name LIKE '%Johnson%'
   OR k.id IN (19, 20)
ORDER BY k.id;

-- Expected: Should show parent 10 (Sara Johnson) with kids 19 and 20 both in group 7

-- 4. List all educators assigned to group 7
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

-- Expected: Should show all educators assigned to group 7 (should include Jessica id=27, and possibly others like 25, 28)

-- 5. List all kids in group 7 and their parents
SELECT 
    k.id AS kid_id,
    k.full_name AS kid_name,
    k.group_id,
    p.id AS parent_id,
    p.full_name AS parent_name,
    p.email AS parent_email
FROM kids k
LEFT JOIN parent_kids pk ON k.id = pk.kid_id
LEFT JOIN parents p ON pk.parent_id = p.id
WHERE k.group_id = 7
ORDER BY k.id, p.id;

-- Expected: Should show all kids in group 7, including kids 19 and 20 with parent 10 (Sara Johnson)

-- 6. Verify daycare ID consistency for all relevant entities
SELECT 'educator' AS entity_type, id::text, full_name, daycare_id::text FROM educators WHERE id IN (27, 24)
UNION ALL
SELECT 'parent' AS entity_type, id::text, full_name, daycare_id::text FROM parents WHERE id = 10
UNION ALL
SELECT 'kid' AS entity_type, id::text, full_name, daycare_id::text FROM kids WHERE id IN (19, 20)
UNION ALL
SELECT 'group' AS entity_type, id::text, name, daycare_id::text FROM groups WHERE id = 7;

-- Expected: All entities should have the same daycare_id (the one used in the Android app)

-- 7. Count educators by daycare
SELECT 
    daycare_id,
    COUNT(*) AS educator_count
FROM educators
GROUP BY daycare_id
ORDER BY daycare_id;

-- 8. Count kids by group
SELECT 
    group_id,
    COUNT(*) AS kid_count
FROM kids
WHERE group_id IS NOT NULL
GROUP BY group_id
ORDER BY group_id;

-- 9. Count parents linked to kids in group 7
SELECT 
    COUNT(DISTINCT p.id) AS parent_count
FROM parents p
INNER JOIN parent_kids pk ON p.id = pk.parent_id
INNER JOIN kids k ON pk.kid_id = k.id
WHERE k.group_id = 7;

-- Expected: Should show count of distinct parents who have kids in group 7

-- 10. Verify educator_groups constraints (should have no duplicates, no NULLs)
SELECT 
    'Total rows' AS metric,
    COUNT(*)::text AS value
FROM educator_groups
UNION ALL
SELECT 
    'Distinct pairs' AS metric,
    COUNT(DISTINCT (educator_id, group_id))::text AS value
FROM educator_groups
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
WHERE group_id IS NULL;

-- Expected: 
-- - Total rows = Distinct pairs (no duplicates)
-- - Rows with NULL educator_id = 0
-- - Rows with NULL group_id = 0

