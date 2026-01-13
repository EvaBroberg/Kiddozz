#!/usr/bin/env python3
"""
Script to create a daycare and link a user (Educator) to it.

This script demonstrates tenant (daycare) creation and user linkage.
It creates a new Daycare record and an Educator linked to that daycare.
"""

import os
import sys

# Add the app directory to the Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.database import SessionLocal
from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole


def main():
    """Create a daycare and link an educator to it."""
    db = SessionLocal()
    try:
        # Create a new daycare
        daycare = Daycare(name="Test Daycare Tenant")
        db.add(daycare)
        db.commit()
        db.refresh(daycare)
        
        print(f"✅ Created Daycare: id={daycare.id}, name='{daycare.name}'")
        
        # Create an educator linked to the daycare
        educator = Educator(
            full_name="Test Educator",
            email=f"test-educator-{daycare.id[:8]}@example.com",  # Unique email
            role=EducatorRole.EDUCATOR.value,
            daycare_id=daycare.id,
        )
        db.add(educator)
        db.commit()
        db.refresh(educator)
        
        print(f"✅ Created Educator: id={educator.id}, name='{educator.full_name}', email='{educator.email}'")
        
        # Verify linkage: read back and assert
        educator_check = db.query(Educator).filter(Educator.id == educator.id).first()
        if not educator_check:
            raise ValueError(f"Educator {educator.id} not found after creation")
        
        if educator_check.daycare_id != daycare.id:
            raise ValueError(
                f"Linkage mismatch: educator.daycare_id={educator_check.daycare_id} != daycare.id={daycare.id}"
            )
        
        # Verify relationship loads (if available)
        if hasattr(educator_check, 'daycare') and educator_check.daycare:
            if educator_check.daycare.id != daycare.id:
                raise ValueError(
                    f"Relationship mismatch: educator.daycare.id={educator_check.daycare.id} != daycare.id={daycare.id}"
                )
            print(f"✅ Relationship verified: educator.daycare.name='{educator_check.daycare.name}'")
        
        print(f"\n✅ Success! Daycare and Educator are properly linked.")
        print(f"   Daycare ID: {daycare.id}")
        print(f"   Educator ID: {educator.id}")
        print(f"   Educator daycare_id: {educator.daycare_id}")
        
    except Exception as e:
        db.rollback()
        print(f"❌ Error: {e}")
        sys.exit(1)
    finally:
        db.close()


if __name__ == "__main__":
    main()

