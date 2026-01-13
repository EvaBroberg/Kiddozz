#!/usr/bin/env python3
"""
Bootstrap script to create a daycare and generate a super educator invite token.

This script creates a new daycare (tenant) and generates an invite token
for a super educator role. The invite link is printed for sharing.
"""

import argparse
import os
import sys

# Add the app directory to the Python path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.database import SessionLocal
from app.core.roles import Role
from app.models.daycare import Daycare
from app.services.invite_token_service import (
    generate_invite_token,
    validate_invite_token,
)


def bootstrap_super_educator_invite(
    daycare_name: str,
    email: str,
    ttl_minutes: int = 10080,  # 7 days default
    created_by: str = "bootstrap-script",
    base_url: str = "kiddozz://invite",
    dry_run: bool = False,
    db=None,
):
    """
    Create a daycare and generate a super educator invite token.

    Args:
        daycare_name: Name of the daycare to create
        email: Email address for the super educator
        ttl_minutes: Time-to-live for the invite token (default: 7 days)
        created_by: Identifier of who created the token (default: "bootstrap-script")
        base_url: Base URL for the invite link (default: "kiddozz://invite")
        dry_run: If True, rollback all changes at the end
        db: Optional database session (for testing). If None, creates a new session.

    Returns:
        Tuple of (daycare_id, token_id, token_str, invite_link)
    """
    should_close_db = False
    if db is None:
        db = SessionLocal()
        should_close_db = True
    try:
        # 1. Create daycare
        daycare = Daycare(name=daycare_name)
        db.add(daycare)
        db.flush()  # Flush to get ID without committing
        db.refresh(daycare)
        print(f"✅ Created Daycare: id={daycare.id}, name='{daycare.name}'")

        # 2. Generate invite token for SUPER_EDUCATOR
        # Note: generate_invite_token commits internally
        token_obj, token_str = generate_invite_token(
            db=db,
            daycare_id=daycare.id,
            role=Role.SUPER_EDUCATOR.value,
            email=email,
            ttl_minutes=ttl_minutes,
            created_by=created_by,
        )
        print(
            f"✅ Generated invite token: id={token_obj.id}, "
            f"role='{token_obj.role}', email='{token_obj.email}', "
            f"expires_at='{token_obj.expires_at.isoformat()}'"
        )

        # 3. Validate the token to prove it works
        validated_token = validate_invite_token(db, token_str)
        print(f"✅ Token validation successful: id={validated_token.id}")

        # 4. Construct invite link
        invite_link = f"{base_url}?token={token_str}"

        # 5. Print summary
        print("\n" + "=" * 60)
        print("📋 BOOTSTRAP SUMMARY")
        print("=" * 60)
        print(f"Daycare ID: {daycare.id}")
        print(f"Daycare Name: {daycare.name}")
        print(f"Token ID: {token_obj.id}")
        print(f"Token Created At: {token_obj.created_at.isoformat()}")
        print(f"Token Expires At: {token_obj.expires_at.isoformat()}")
        print(f"Super Educator Email: {email}")
        print(f"\n🔗 Invite Link:")
        print(f"   {invite_link}")
        print("=" * 60)

        # Save IDs before potential rollback/expunge
        saved_daycare_id = daycare.id
        saved_token_id = token_obj.id

        if dry_run:
            print("\n⚠️  DRY RUN MODE: Rolling back all changes...")
            # Note: Full rollback may not be possible with SQLite due to nested commits
            # in generate_invite_token, but we attempt to rollback what we can
            try:
                db.rollback()
                db.expunge_all()
                print("✅ Dry run complete (attempted rollback)")
            except Exception as e:
                print(f"⚠️  Dry run rollback warning: {e}")
                print("   (Some changes may persist due to nested commits)")
        else:
            db.commit()
            print("\n✅ Changes committed to database")

        return saved_daycare_id, saved_token_id, token_str, invite_link

    except Exception as e:
        db.rollback()
        print(f"\n❌ Error: {e}")
        raise
    finally:
        if should_close_db:
            db.close()


def main():
    """CLI entry point for the bootstrap script."""
    parser = argparse.ArgumentParser(
        description="Bootstrap a daycare and generate a super educator invite token"
    )
    parser.add_argument(
        "--daycare-name",
        required=True,
        help="Name of the daycare to create",
    )
    parser.add_argument(
        "--email",
        required=True,
        help="Email address for the super educator",
    )
    parser.add_argument(
        "--ttl-minutes",
        type=int,
        default=10080,  # 7 days
        help="Time-to-live for the invite token in minutes (default: 10080 = 7 days)",
    )
    parser.add_argument(
        "--created-by",
        default="bootstrap-script",
        help="Identifier of who created the token (default: 'bootstrap-script')",
    )
    parser.add_argument(
        "--base-url",
        default="kiddozz://invite",
        help="Base URL for the invite link (default: 'kiddozz://invite')",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Perform a dry run (rollback all changes at the end)",
    )

    args = parser.parse_args()

    try:
        bootstrap_super_educator_invite(
            daycare_name=args.daycare_name,
            email=args.email,
            ttl_minutes=args.ttl_minutes,
            created_by=args.created_by,
            base_url=args.base_url,
            dry_run=args.dry_run,
        )
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ Bootstrap failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()

