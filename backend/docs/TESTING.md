# Testing Guide

## Database policy for tests

- **CI (GitHub Actions)** always runs backend tests against **PostgreSQL** by starting a PG service and setting `DATABASE_URL` accordingly; migrations run before pytest.
- **Local developers** may run tests on SQLite (default local `DATABASE_URL`) for speed.
- Tests that depend on PG-only features (partitions, `pg_tables`, `ALTER ... PARTITION`) must be guarded with:
  - `if engine.dialect.name != "postgresql": pytest.skip(...)`
- A session-scoped fixture prints the active test DB: see `tests/conftest.py`.

This policy prevents flaky results and keeps Postgres-specific features validated in CI.

## Running Tests

### Local Development
```bash
cd backend
poetry run pytest -q
```

### CI Environment
Tests automatically run against PostgreSQL with proper migrations.

## Test Database Detection

The test suite automatically detects and prints the active database:
```
[TEST DB] dialect=postgresql url=postgresql+psycopg2://...
```

## PostgreSQL-Only Features

Some tests require PostgreSQL-specific features:
- Table partitioning
- System catalog queries (`pg_tables`, `pg_class`)
- Partition DDL operations (`ALTER TABLE ... DETACH PARTITION`)

These tests are automatically skipped on SQLite and only run in CI.
