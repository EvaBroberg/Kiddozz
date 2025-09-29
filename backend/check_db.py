#!/usr/bin/env python3

from app.core.database import engine
from sqlalchemy import text

try:
    with engine.connect() as conn:
        # Check if kid_absences table exists
        result = conn.execute(text("SELECT COUNT(*) FROM kid_absences"))
        count = result.scalar()
        print(f"Absences in DB: {count}")
        
        # Check table structure
        result = conn.execute(text("SELECT column_name FROM information_schema.columns WHERE table_name = 'kid_absences' ORDER BY ordinal_position"))
        columns = [row[0] for row in result]
        print(f"kid_absences columns: {columns}")
        
except Exception as e:
    print(f"Database error: {e}")