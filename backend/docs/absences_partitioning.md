# Kid Absences Partitioning

## What & Why

The `kid_absences` table is partitioned by year on the `date` column for performance and archival purposes. This allows:

- **Performance**: Queries on recent data are faster as they only scan relevant partitions
- **Archival**: Old data can be moved to archive schema without affecting current operations
- **Maintenance**: Individual years can be backed up, restored, or dropped independently

## How It Works

### Table Structure

- **Parent table**: `kid_absences` (partitioned by RANGE on `date`)
- **Child partitions**: `kid_absences_YYYY` (one per year)
- **Unique constraint**: `(kid_id, date)` enforced at parent level
- **Indexes**: Each partition has indexes on `(kid_id, date)` and `(date)`

### Automatic Partition Creation

The application automatically ensures partitions exist for:
- Current year
- Next year

This is controlled by the `ENABLE_ABSENCE_PARTITIONS` environment variable (default: `true`).

### Data Routing

When inserting data:
- PostgreSQL automatically routes rows to the correct partition based on the `date` value
- No application code changes required
- All existing queries work unchanged

## Archival Procedure

### Archive Last Year's Data

```bash
# Archive data for 2025
python -m app.scripts.archive_absences --year 2025
```

This will:
1. Detach the `kid_absences_2025` partition from the parent table
2. Move it to the `archive` schema as `archive.kid_absences_2025`
3. Provide instructions for backup and cleanup

### Backup and Cleanup (Optional)

After archiving, you can:

```bash
# Create backup
pg_dump -t archive.kid_absences_2025 | gzip > kid_absences_2025.sql.gz

# Upload to S3
aws s3 cp kid_absences_2025.sql.gz s3://your-bucket/archives/

# Drop from database (after confirming backup)
psql -c "DROP TABLE archive.kid_absences_2025;"
```

## Rollback

### Downgrade Migration

To rollback the partitioning:

```bash
alembic downgrade -1
```

This will:
1. Create a non-partitioned table with the same schema
2. Copy all data from partitions back to the single table
3. Drop all partitions and the parent table
4. Rename the new table back to `kid_absences`

### Reattaching Archived Data

If you need to reattach archived data:

```sql
-- Move back to public schema
ALTER TABLE archive.kid_absences_2025 SET SCHEMA public;

-- Reattach to parent table
ALTER TABLE kid_absences ATTACH PARTITION kid_absences_2025
FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
```

## FAQ

### What happens if a partition is missing?

- **For reads**: Queries will return no data for that year
- **For writes**: PostgreSQL will raise an error if trying to insert data for a year without a partition
- **Solution**: Run the startup helper or create the partition manually

### How to create missing partitions?

```python
from app.db.partitioning import ensure_absence_partition_for_year
from app.core.database import engine

# Create partition for specific year
ensure_absence_partition_for_year(engine, 2027)
```

### How to check existing partitions?

```sql
-- List all partitions
SELECT tablename, schemaname 
FROM pg_tables 
WHERE tablename LIKE 'kid_absences_%'
ORDER BY tablename;

-- Check partition constraints
SELECT schemaname, tablename, pg_get_expr(c.relpartbound, c.oid) as constraint
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relname LIKE 'kid_absences_%'
AND c.relkind = 'r';
```

### Performance Impact

- **Inserts**: Slightly faster (direct to partition)
- **Queries on date ranges**: Much faster (partition pruning)
- **Queries without date filter**: Same performance as before
- **Storage**: No change in total storage requirements

## Environment Variables

- `ENABLE_ABSENCE_PARTITIONS`: Enable/disable automatic partition creation (default: `true`)
- `DATABASE_URL`: Database connection string (required)

## Monitoring

### Check Partition Status

```sql
-- Verify parent table is partitioned
SELECT relname, relkind 
FROM pg_class 
WHERE relname = 'kid_absences' AND relkind = 'p';

-- Check partition sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE tablename LIKE 'kid_absences_%'
ORDER BY tablename;
```

### Application Logs

The application logs partition creation during startup:

```
🔄 Ensuring absence partitions...
Created partition kid_absences_2025 for 2025-01-01 to 2026-01-01
Created partition kid_absences_2026 for 2026-01-01 to 2027-01-01
✅ Absence partitions ensured successfully
```

## Troubleshooting

### Migration Fails

If the partitioning migration fails:

1. Check if table is already partitioned
2. Verify database permissions
3. Check for data type mismatches
4. Review migration logs

### Partition Missing

If a partition is missing:

1. Check application logs for errors
2. Verify `ENABLE_ABSENCE_PARTITIONS` is set to `true`
3. Manually create the partition using the helper function
4. Check database connectivity

### Data Not Found

If data appears to be missing:

1. Check if it was archived
2. Verify the correct year partition exists
3. Check for timezone issues in date filtering
4. Query the archive schema if applicable
