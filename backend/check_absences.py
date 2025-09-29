#!/usr/bin/env python3

from app.core.database import engine
from sqlalchemy import text

try:
    with engine.connect() as conn:
        # Get all absences
        result = conn.execute(text("SELECT * FROM kid_absences ORDER BY created_at DESC"))
        absences = result.fetchall()
        print(f"Found {len(absences)} absences:")
        for absence in absences:
            print(f"  ID: {absence[0]}, Kid ID: {absence[1]}, Date: {absence[2]}, Reason: {absence[3]}")
        
except Exception as e:
    print(f"Database error: {e}")
