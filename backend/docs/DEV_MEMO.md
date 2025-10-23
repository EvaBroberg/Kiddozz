# Developer Memo

## Testing DB rule

- **CI backend tests**: PostgreSQL (service + DATABASE_URL + alembic upgrade).
- **Local dev tests**: SQLite allowed; Postgres-only tests must skip on non-PG.
- **If a test fails on SQLite** with `pg_tables` or partition DDL, it is missing the guard.

## Quick Reference

### Database Detection
```python
from app.core.database import engine
if engine.dialect.name != "postgresql":
    pytest.skip("Partition introspection requires PostgreSQL")
```

### Test Database Print
The test suite automatically prints the active database at session start:
```
[TEST DB] dialect=postgresql url=postgresql+psycopg2://...
```

### PostgreSQL-Only Features
- Table partitioning (`CREATE TABLE ... PARTITION OF`)
- System catalog queries (`pg_tables`, `pg_class`)
- Partition DDL (`ALTER TABLE ... DETACH PARTITION`)
- Schema operations (`CREATE SCHEMA`)

All tests using these features must be guarded with PostgreSQL checks.
