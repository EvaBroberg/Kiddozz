#!/usr/bin/env python3
"""
Archive script for kid_absences partitions.

This script detaches a partition for a specific year and moves it to the archive schema.
"""

import argparse
import os
import sys
from datetime import datetime
from sqlalchemy import create_engine, text
import logging

# Add the app directory to the Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.database import engine

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def create_archive_schema(conn):
    """Create archive schema if it doesn't exist."""
    try:
        conn.execute(text("CREATE SCHEMA IF NOT EXISTS archive"))
        logger.info("Archive schema created or already exists")
        return True
    except Exception as e:
        logger.error(f"Error creating archive schema: {e}")
        return False


def archive_absences_for_year(year: int) -> bool:
    """
    Archive absences for a specific year.
    
    Args:
        year: Year to archive
        
    Returns:
        bool: True if successful, False otherwise
    """
    try:
        with engine.connect() as conn:
            # Check if partition exists
            result = conn.execute(text("""
                SELECT EXISTS (
                    SELECT 1 FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = 'public' AND c.relname = :table_name
                )
            """), {"table_name": f"kid_absences_{year}"})
            
            if not result.scalar():
                logger.info(f"Partition kid_absences_{year} does not exist, nothing to archive")
                return True
            
            # Create archive schema
            if not create_archive_schema(conn):
                return False
            
            # Detach partition from parent table
            logger.info(f"Detaching partition kid_absences_{year} from parent table")
            conn.execute(text(f"""
                ALTER TABLE kid_absences DETACH PARTITION kid_absences_{year}
            """))
            
            # Move partition to archive schema
            logger.info(f"Moving partition to archive schema")
            conn.execute(text(f"""
                ALTER TABLE kid_absences_{year} SET SCHEMA archive
            """))
            
            conn.commit()
            
            logger.info(f"Successfully archived kid_absences_{year} to archive.kid_absences_{year}")
            
            # Print next steps
            print("\n" + "="*60)
            print("ARCHIVE COMPLETED SUCCESSFULLY")
            print("="*60)
            print(f"Partition kid_absences_{year} has been moved to archive.kid_absences_{year}")
            print("\nNext steps (optional):")
            print(f"1. Create backup: pg_dump -t archive.kid_absences_{year} | gzip > kid_absences_{year}.sql.gz")
            print(f"2. Upload to S3: aws s3 cp kid_absences_{year}.sql.gz s3://your-bucket/archives/")
            print(f"3. Drop from database: DROP TABLE archive.kid_absences_{year};")
            print("="*60)
            
            return True
            
    except Exception as e:
        logger.error(f"Error archiving absences for year {year}: {e}")
        return False


def main():
    """Main CLI function."""
    parser = argparse.ArgumentParser(description="Archive kid_absences partitions")
    parser.add_argument(
        "--year", 
        type=int, 
        default=datetime.now().year - 1,
        help="Year to archive (defaults to last year)"
    )
    
    args = parser.parse_args()
    
    logger.info(f"Starting archive process for year {args.year}")
    
    success = archive_absences_for_year(args.year)
    
    if success:
        logger.info("Archive process completed successfully")
        sys.exit(0)
    else:
        logger.error("Archive process failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
