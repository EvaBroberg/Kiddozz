#!/usr/bin/env python3
"""
Script to ensure next year's partition exists.

This script can be run manually or scheduled to ensure partitions are created
for the next year before it arrives.
"""

import os
import sys
from datetime import datetime

# Add the app directory to the Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.db.partitioning import ensure_current_and_next_year_partitions
from app.core.database import engine


def main():
    """Ensure current and next year partitions exist."""
    current_year = datetime.now().year
    next_year = current_year + 1
    
    print(f"Ensuring partitions for years {current_year} and {next_year}")
    
    success = ensure_current_and_next_year_partitions(engine)
    
    if success:
        print("✅ Partitions ensured successfully")
        sys.exit(0)
    else:
        print("❌ Failed to ensure partitions")
        sys.exit(1)


if __name__ == "__main__":
    main()
