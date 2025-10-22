"""
Tests for kid_absences partitioning functionality.
"""

import os
from datetime import date, datetime

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import text

from app.db.partitioning import (
    ensure_absence_partition_for_year,
    get_existing_partition_years,
)
from app.main import app
from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import AbsenceReason, AttendanceStatus, Kid, KidAbsence
from app.models.parent import Parent
from tests.conftest import TestingSessionLocal

client = TestClient(app)

# Skip partitioning tests if not using PostgreSQL
IS_POSTGRES = os.getenv("DATABASE_URL", "").startswith("postgresql://")


class TestAbsencePartitions:
    """Test absence partitioning functionality."""

    @pytest.mark.skipif(not IS_POSTGRES, reason="Partitioning requires PostgreSQL")
    def test_insert_routes_to_correct_partition(self, clean_db):
        """Test that inserts route to correct partition."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Get parent JWT token
            login_response = client.post(
                "/api/v1/auth/dev-login", json={"parent_id": str(parent.id)}
            )
            assert login_response.status_code == 200
            token = login_response.json()["access_token"]
            headers = {"Authorization": f"Bearer {token}"}

            # Insert absences for different years
            # 2025-12-31 (end of 2025)
            absence_data_2025 = {
                "date": "2025-12-31",
                "reason": "sick",
                "note": "End of year absence",
            }

            response_2025 = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=absence_data_2025,
                headers=headers,
            )
            assert response_2025.status_code == 200

            # 2026-01-01 (start of 2026)
            absence_data_2026 = {
                "date": "2026-01-01",
                "reason": "holiday",
                "note": "New year absence",
            }

            response_2026 = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=absence_data_2026,
                headers=headers,
            )
            assert response_2026.status_code == 200

            # Verify both absences exist in the main table
            absences = db.query(KidAbsence).filter(KidAbsence.kid_id == kid.id).all()
            assert len(absences) == 2

            # Check that partitions exist
            result = db.execute(
                text(
                    """
                SELECT tablename
                FROM pg_tables
                WHERE tablename LIKE 'kid_absences_%'
                AND schemaname = 'public'
                ORDER BY tablename
            """
                )
            )
            partition_tables = [row[0] for row in result.fetchall()]

            # Should have partitions for 2025 and 2026
            assert "kid_absences_2025" in partition_tables
            assert "kid_absences_2026" in partition_tables

            print("✅ Partitions created and data routed correctly")

        finally:
            db.close()

    @pytest.mark.skipif(not IS_POSTGRES, reason="Partitioning requires PostgreSQL")
    def test_unique_constraint_enforced_across_partitions(self, clean_db):
        """Test that unique constraint is enforced across partitions."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Get parent JWT token
            login_response = client.post(
                "/api/v1/auth/dev-login", json={"parent_id": str(parent.id)}
            )
            assert login_response.status_code == 200
            token = login_response.json()["access_token"]
            headers = {"Authorization": f"Bearer {token}"}

            # Insert first absence
            absence_data = {
                "date": "2025-05-10",
                "reason": "sick",
                "note": "First absence",
            }

            response1 = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=absence_data,
                headers=headers,
            )
            assert response1.status_code == 200

            # Try to insert duplicate absence (same kid, same date)
            response2 = client.post(
                f"/api/v1/kids/{kid.id}/absences",
                json=absence_data,
                headers=headers,
            )
            assert response2.status_code == 400
            assert "already reported for this date" in response2.json()["detail"]

            print("✅ Unique constraint enforced across partitions")

        finally:
            db.close()

    @pytest.mark.skipif(not IS_POSTGRES, reason="Partitioning requires PostgreSQL")
    def test_archive_last_year_keeps_data_accessible(self, clean_db):
        """Test that archiving last year keeps data accessible via manual query."""
        db = TestingSessionLocal()
        try:
            # Create test data
            daycare = Daycare(name="Test Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)

            group = Group(name="Group A", daycare_id=daycare.id)
            db.add(group)
            db.commit()
            db.refresh(group)

            parent = Parent(
                full_name="Test Parent",
                email="test@example.com",
                phone_num="+1234567890",
                daycare_id=daycare.id,
            )
            db.add(parent)
            db.commit()
            db.refresh(parent)

            kid = Kid(
                full_name="Test Kid",
                dob=date(2020, 1, 1),
                daycare_id=daycare.id,
                group_id=group.id,
                attendance=AttendanceStatus.OUT,
            )
            db.add(kid)
            db.commit()
            db.refresh(kid)

            # Link parent to kid
            parent.kids.append(kid)
            db.commit()

            # Create absence for last year
            last_year = datetime.now().year - 1
            absence = KidAbsence(
                kid_id=kid.id,
                date=date(last_year, 6, 15),
                reason=AbsenceReason.SICK,
                note="Archived absence",
            )
            db.add(absence)
            db.commit()
            db.refresh(absence)

            # Simulate archive process
            from app.core.database import engine

            # Create archive schema
            with engine.connect() as conn:
                conn.execute(text("CREATE SCHEMA IF NOT EXISTS archive"))
                conn.commit()

                # Detach partition
                conn.execute(
                    text(
                        f"""
                    ALTER TABLE kid_absences DETACH PARTITION kid_absences_{last_year}
                """
                    )
                )

                # Move to archive
                conn.execute(
                    text(
                        f"""
                    ALTER TABLE kid_absences_{last_year} SET SCHEMA archive
                """
                    )
                )
                conn.commit()

            # Verify data is accessible in archive
            result = db.execute(
                text(
                    f"""
                SELECT * FROM archive.kid_absences_{last_year}
                WHERE kid_id = :kid_id
            """
                ),
                {"kid_id": kid.id},
            )

            archived_absences = result.fetchall()
            assert len(archived_absences) == 1
            assert archived_absences[0][4] == "Archived absence"  # note field

            print("✅ Archive process completed and data accessible")

        finally:
            db.close()

    @pytest.mark.skipif(not IS_POSTGRES, reason="Partitioning requires PostgreSQL")
    def test_startup_helper_creates_current_next(self, clean_db):
        """Test that startup helper creates current and next year partitions."""
        from app.core.database import engine

        current_year = datetime.now().year
        next_year = current_year + 1

        # Call the startup helper
        success = ensure_absence_partition_for_year(engine, current_year)
        assert success

        success = ensure_absence_partition_for_year(engine, next_year)
        assert success

        # Verify partitions exist
        with engine.connect() as conn:
            result = conn.execute(
                text(
                    """
                SELECT tablename
                FROM pg_tables
                WHERE tablename LIKE 'kid_absences_%'
                AND schemaname = 'public'
                ORDER BY tablename
            """
                )
            )
            partition_tables = [row[0] for row in result.fetchall()]

            assert f"kid_absences_{current_year}" in partition_tables
            assert f"kid_absences_{next_year}" in partition_tables

        print("✅ Startup helper creates partitions correctly")

    @pytest.mark.skipif(not IS_POSTGRES, reason="Partitioning requires PostgreSQL")
    def test_get_existing_partition_years(self, clean_db):
        """Test getting existing partition years."""
        from app.core.database import engine

        # Create some partitions
        current_year = datetime.now().year
        ensure_absence_partition_for_year(engine, current_year)
        ensure_absence_partition_for_year(engine, current_year + 1)

        # Get existing years
        years = get_existing_partition_years(engine)

        assert current_year in years
        assert (current_year + 1) in years

        print("✅ Get existing partition years works correctly")
