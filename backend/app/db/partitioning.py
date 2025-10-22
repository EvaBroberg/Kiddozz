"""
Database partitioning utilities for kid_absences table.

This module provides functions to manage yearly partitions for the kid_absences table,
ensuring that current and next year partitions exist for optimal performance.
"""

from sqlalchemy import create_engine, text
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


def ensure_absence_partition_for_year(engine, year: int) -> bool:
    """
    Ensure that a partition exists for the given year.
    
    Args:
        engine: SQLAlchemy engine instance
        year: Year to create partition for
        
    Returns:
        bool: True if partition was created or already exists, False on error
    """
    try:
        with engine.connect() as conn:
            # Check if partition already exists
            result = conn.execute(text("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public' AND c.relname = :table_name
                )
            """), {"table_name": f"kid_absences_{year}"})
            
            if result.scalar():
                logger.info(f"Partition kid_absences_{year} already exists")
                return True
            
            # Check if parent table is partitioned
            result = conn.execute(text("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public' AND c.relname = 'kid_absences'
                    AND c.relkind = 'p'
                )
            """))
            
            if not result.scalar():
                logger.warning("kid_absences table is not partitioned, skipping partition creation")
                return False
            
            # Create partition
            start_date = f"{year}-01-01"
            end_date = f"{year + 1}-01-01"
            
            conn.execute(text(f"""
                CREATE TABLE kid_absences_{year} PARTITION OF kid_absences
                FOR VALUES FROM ('{start_date}') TO ('{end_date}')
            """))
            
            # Create indexes on the partition
            conn.execute(text(f"""
                CREATE INDEX idx_kid_absences_{year}_kid_date 
                ON kid_absences_{year} (kid_id, date)
            """))
            
            conn.execute(text(f"""
                CREATE INDEX idx_kid_absences_{year}_date 
                ON kid_absences_{year} (date)
            """))
            
            conn.commit()
            logger.info(f"Created partition kid_absences_{year} for {start_date} to {end_date}")
            return True
            
    except Exception as e:
        logger.error(f"Error creating partition for year {year}: {e}")
        return False


def ensure_current_and_next_year_partitions(engine) -> bool:
    """
    Ensure that partitions exist for the current year and next year.
    
    Args:
        engine: SQLAlchemy engine instance
        
    Returns:
        bool: True if all partitions were created or already exist, False on error
    """
    current_year = datetime.now().year
    next_year = current_year + 1
    
    logger.info(f"Ensuring partitions for years {current_year} and {next_year}")
    
    current_success = ensure_absence_partition_for_year(engine, current_year)
    next_success = ensure_absence_partition_for_year(engine, next_year)
    
    return current_success and next_success


def get_existing_partition_years(engine) -> list:
    """
    Get list of years that have existing partitions.
    
    Args:
        engine: SQLAlchemy engine instance
        
    Returns:
        list: List of years with existing partitions
    """
    try:
        with engine.connect() as conn:
            result = conn.execute(text("""
                SELECT tablename
                FROM pg_tables
                WHERE tablename LIKE 'kid_absences_%'
                AND schemaname = 'public'
                ORDER BY tablename
            """))
            
            years = []
            for row in result.fetchall():
                table_name = row[0]
                # Extract year from table name (kid_absences_YYYY)
                year_str = table_name.replace('kid_absences_', '')
                try:
                    years.append(int(year_str))
                except ValueError:
                    continue
            
            return sorted(years)
            
    except Exception as e:
        logger.error(f"Error getting existing partition years: {e}")
        return []
