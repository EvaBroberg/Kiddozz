"""enforce educator_groups constraints

Revision ID: d4e8f9a1b2c3
Revises: c2d8ca2d669f
Create Date: 2025-01-10 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy import text

# revision identifiers, used by Alembic.
revision = 'd4e8f9a1b2c3'
down_revision = 'c2d8ca2d669f'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Step 1: Pre-cleanup (idempotent)
    # Delete duplicate pairs keeping one (lowest educator_id, group_id)
    conn = op.get_bind()
    
    # Delete duplicates using row_number()
    conn.execute(text("""
        WITH ranked AS (
            SELECT 
                educator_id, 
                group_id,
                row_number() OVER (PARTITION BY educator_id, group_id ORDER BY educator_id, group_id) as rn
            FROM educator_groups
            WHERE educator_id IS NOT NULL AND group_id IS NOT NULL
        )
        DELETE FROM educator_groups
        WHERE (educator_id, group_id) IN (
            SELECT educator_id, group_id FROM ranked WHERE rn > 1
        )
    """))
    
    # Delete rows with any NULLs
    conn.execute(text("""
        DELETE FROM educator_groups
        WHERE educator_id IS NULL OR group_id IS NULL
    """))
    
    conn.commit()
    
    # Step 2: Nullability + uniqueness
    # Set NOT NULL constraints
    op.alter_column('educator_groups', 'educator_id', nullable=False)
    op.alter_column('educator_groups', 'group_id', nullable=False)
    
    # Add UNIQUE constraint
    op.create_unique_constraint(
        'uq_educator_groups_pair',
        'educator_groups',
        ['educator_id', 'group_id']
    )
    
    # Step 3: "At least one group per educator" constraint trigger
    # Create the function that checks if educator has at least one group
    conn.execute(text("""
        CREATE OR REPLACE FUNCTION check_educator_has_groups()
        RETURNS TRIGGER AS $$
        DECLARE
            group_count INTEGER;
            is_educator_deleting BOOLEAN;
        BEGIN
            -- If we're deleting the educator itself (via FK CASCADE), skip check
            -- Check if the educator row is being deleted by looking at pg_trigger_depth
            -- If depth > 1, we're likely in a CASCADE delete
            -- However, a simpler approach: check if the educator still exists
            -- If the educator is being deleted, its row won't exist at commit time
            -- So we check: if educator doesn't exist, skip validation
            
            IF TG_OP = 'DELETE' THEN
                -- Check if educator row still exists (if not, it's being deleted via FK CASCADE)
                -- Since trigger is DEFERRABLE INITIALLY DEFERRED, this check happens at COMMIT time
                -- If educator was deleted, its row won't exist, so skip validation
                SELECT EXISTS(SELECT 1 FROM educators WHERE id = OLD.educator_id) INTO is_educator_deleting;
                
                IF NOT is_educator_deleting THEN
                    -- Educator is being deleted, FK CASCADE will handle cleanup
                    -- Skip validation to allow the CASCADE delete
                    RETURN OLD;
                END IF;
                
                -- Count remaining groups for this educator
                -- Since trigger is AFTER DELETE, the row being deleted is already gone from the table
                SELECT COUNT(*) INTO group_count
                FROM educator_groups
                WHERE educator_id = OLD.educator_id;
                
                -- If count is 0, this was the last group assignment
                IF group_count = 0 THEN
                    RAISE EXCEPTION 'Educator % must belong to at least one group. Cannot delete the last group assignment.', OLD.educator_id;
                END IF;
                
                RETURN OLD;
                
            ELSIF TG_OP = 'UPDATE' THEN
                -- For UPDATE, check both old and new educator_id
                -- If educator_id changed, check the old one being "orphaned"
                IF OLD.educator_id != NEW.educator_id THEN
                    SELECT COUNT(*) INTO group_count
                    FROM educator_groups
                    WHERE educator_id = OLD.educator_id AND (educator_id, group_id) != (NEW.educator_id, NEW.group_id);
                    
                    IF group_count = 0 THEN
                        RAISE EXCEPTION 'Educator % must belong to at least one group. Cannot move the last group assignment.', OLD.educator_id;
                    END IF;
                END IF;
                
                -- Check the new educator_id will have at least one group
                SELECT COUNT(*) INTO group_count
                FROM educator_groups
                WHERE educator_id = NEW.educator_id AND (educator_id, group_id) != (OLD.educator_id, OLD.group_id);
                
                IF group_count = 0 THEN
                    -- This shouldn't happen if we're moving between groups in one transaction
                    -- But if updating to a new educator, ensure they have groups
                    SELECT COUNT(*) INTO group_count
                    FROM educator_groups
                    WHERE educator_id = NEW.educator_id;
                    
                    IF group_count = 0 THEN
                        RAISE EXCEPTION 'Educator % must belong to at least one group.', NEW.educator_id;
                    END IF;
                END IF;
                
                RETURN NEW;
            END IF;
            
            RETURN NULL;
        END;
        $$ LANGUAGE plpgsql;
    """))
    
    # Create the constraint trigger (DEFERRABLE INITIALLY DEFERRED)
    conn.execute(text("""
        CREATE CONSTRAINT TRIGGER trigger_ensure_educator_has_groups
        AFTER DELETE OR UPDATE ON educator_groups
        DEFERRABLE INITIALLY DEFERRED
        FOR EACH ROW
        EXECUTE FUNCTION check_educator_has_groups();
    """))
    
    conn.commit()


def downgrade() -> None:
    conn = op.get_bind()
    
    # Drop the trigger
    conn.execute(text("DROP TRIGGER IF EXISTS trigger_ensure_educator_has_groups ON educator_groups"))
    
    # Drop the function
    conn.execute(text("DROP FUNCTION IF EXISTS check_educator_has_groups()"))
    
    conn.commit()
    
    # Drop unique constraint
    op.drop_constraint('uq_educator_groups_pair', 'educator_groups', type_='unique')
    
    # Set columns back to nullable
    op.alter_column('educator_groups', 'educator_id', nullable=True)
    op.alter_column('educator_groups', 'group_id', nullable=True)

