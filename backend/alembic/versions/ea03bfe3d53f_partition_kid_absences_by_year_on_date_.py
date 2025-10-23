"""partition kid_absences by year on date v2

Revision ID: ea03bfe3d53f
Revises: c2d8ca2d669f
Create Date: 2025-10-22 13:46:32.790125

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import text


# revision identifiers, used by Alembic.
revision = 'ea03bfe3d53f'
down_revision = 'c2d8ca2d669f'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Check if already partitioned
    conn = op.get_bind()
    result = conn.execute(text("""
        SELECT EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public' AND c.relname = 'kid_absences'
            AND c.relkind = 'p'
        )
    """))
    is_partitioned = result.scalar()
    
    if is_partitioned:
        print("kid_absences is already partitioned, skipping migration")
        return
    
    # Get existing data years
    result = conn.execute(text("""
        SELECT DISTINCT EXTRACT(YEAR FROM date)::int as year
        FROM kid_absences
        ORDER BY year
    """))
    existing_years = [row[0] for row in result.fetchall()]
    
    # Add current and next year if not present
    from datetime import datetime
    current_year = datetime.now().year
    next_year = current_year + 1
    
    all_years = list(set(existing_years + [current_year, next_year]))
    all_years.sort()
    
    print(f"Creating partitions for years: {all_years}")
    
    # Create new partitioned table manually (not using LIKE to avoid constraint issues)
    op.execute(text("""
        CREATE TABLE kid_absences_new (
            id uuid NOT NULL,
            kid_id integer NOT NULL,
            date date NOT NULL,
            reason absence_reason NOT NULL,
            note character varying(500),
            created_at timestamp without time zone NOT NULL DEFAULT now(),
            updated_at timestamp without time zone NOT NULL DEFAULT now()
        ) PARTITION BY RANGE (date)
    """))
    
    # Add primary key that includes partition key
    op.execute(text("""
        ALTER TABLE kid_absences_new ADD PRIMARY KEY (id, date)
    """))
    
    # Add unique constraint
    op.execute(text("""
        ALTER TABLE kid_absences_new ADD CONSTRAINT uq_kid_absences_new_kid_date UNIQUE (kid_id, date)
    """))
    
    # Add foreign key
    op.execute(text("""
        ALTER TABLE kid_absences_new ADD CONSTRAINT kid_absences_kid_id_fkey 
        FOREIGN KEY (kid_id) REFERENCES kids(id) ON DELETE CASCADE
    """))
    
    # Create partitions for each year
    for year in all_years:
        start_date = f"{year}-01-01"
        end_date = f"{year + 1}-01-01"
        
        op.execute(text(f"""
            CREATE TABLE kid_absences_{year} PARTITION OF kid_absences_new
            FOR VALUES FROM ('{start_date}') TO ('{end_date}')
        """))
        
        # Create indexes on each partition
        op.execute(text(f"""
            CREATE INDEX idx_kid_absences_{year}_kid_date 
            ON kid_absences_{year} (kid_id, date)
        """))
        
        op.execute(text(f"""
            CREATE INDEX idx_kid_absences_{year}_date 
            ON kid_absences_{year} (date)
        """))
        
        print(f"Created partition kid_absences_{year} for {start_date} to {end_date}")
    
    # Copy data from old table to new partitioned table
    for year in existing_years:
        start_date = f"{year}-01-01"
        end_date = f"{year + 1}-01-01"
        
        op.execute(text(f"""
            INSERT INTO kid_absences_new (id, kid_id, date, reason, note, created_at, updated_at)
            SELECT id, kid_id, date, reason, note, created_at, updated_at FROM kid_absences
            WHERE date >= '{start_date}' AND date < '{end_date}'
        """))
        
        print(f"Copied data for year {year}")
    
    # Drop old table and rename new one
    op.drop_table('kid_absences')
    op.execute(text("ALTER TABLE kid_absences_new RENAME TO kid_absences"))
    
    # Create view for future archival support
    op.execute(text("""
        CREATE VIEW v_kid_absences_all AS
        SELECT * FROM kid_absences
    """))
    
    print("Partitioning migration completed successfully")


def downgrade() -> None:
    # Check if partitioned
    conn = op.get_bind()
    result = conn.execute(text("""
        SELECT EXISTS (
            SELECT 1 FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public' AND c.relname = 'kid_absences'
            AND c.relkind = 'p'
        )
    """))
    is_partitioned = result.scalar()
    
    if not is_partitioned:
        print("kid_absences is not partitioned, skipping downgrade")
        return
    
    # Get all partition names
    result = conn.execute(text("""
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE tablename LIKE 'kid_absences_%'
        AND schemaname = 'public'
    """))
    partitions = result.fetchall()
    
    # Create non-partitioned table with same schema
    op.execute(text("""
        CREATE TABLE kid_absences_old (
            id uuid NOT NULL,
            kid_id integer NOT NULL,
            date date NOT NULL,
            reason absence_reason NOT NULL,
            note character varying(500),
            created_at timestamp without time zone NOT NULL DEFAULT now(),
            updated_at timestamp without time zone NOT NULL DEFAULT now()
        )
    """))
    
    # Add original primary key (just id)
    op.execute(text("""
        ALTER TABLE kid_absences_old ADD PRIMARY KEY (id)
    """))
    
    # Add unique constraint
    op.execute(text("""
        ALTER TABLE kid_absences_old ADD CONSTRAINT uq_kid_absences_kid_date UNIQUE (kid_id, date)
    """))
    
    # Add foreign key
    op.execute(text("""
        ALTER TABLE kid_absences_old ADD CONSTRAINT kid_absences_kid_id_fkey 
        FOREIGN KEY (kid_id) REFERENCES kids(id) ON DELETE CASCADE
    """))
    
    # Copy all data from partitions
    for schema, table in partitions:
        op.execute(text(f"""
            INSERT INTO kid_absences_old
            SELECT * FROM {schema}.{table}
        """))
    
    # Drop partitioned table and partitions
    op.drop_table('kid_absences')
    for schema, table in partitions:
        op.drop_table(table)
    
    # Rename old table back
    op.execute(text("ALTER TABLE kid_absences_old RENAME TO kid_absences"))
    
    # Drop view
    op.execute(text("DROP VIEW IF EXISTS v_kid_absences_all"))
    
    print("Downgrade completed successfully")
