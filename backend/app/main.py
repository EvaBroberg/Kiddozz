import os

from fastapi import FastAPI

from app.api import auth, educators, events, health, parents
from app.core.database import SessionLocal
from app.services.educator_service import insert_dummy_educators
from app.services.user_service import insert_dummy_users

app = FastAPI(title="Kiddozz Backend API", version="1.0.0")

# Register routers
app.include_router(health.router)
app.include_router(auth.router, prefix="/api/v1")  # Keep existing API v1 routes
app.include_router(
    auth.router
)  # Add auth routes without prefix for Android compatibility
app.include_router(events.router, prefix="/api/v1/events")
app.include_router(educators.router, prefix="/api/v1", tags=["educators"])
app.include_router(parents.router, prefix="/api/v1", tags=["parents"])


@app.on_event("startup")
def startup_event():
    """Run database migrations and insert dummy users on application startup."""
    import os
    import subprocess
    import sys

    # Log the current environment
    app_env = os.getenv("APP_ENV", "not set")
    print(f"🌍 APP_ENV = {app_env}")

    # Run database migrations first
    try:
        print("🔄 Running database migrations...")
        result = subprocess.run(
            [sys.executable, "-m", "alembic", "upgrade", "head"],
            capture_output=True,
            text=True,
            cwd=os.getcwd(),
        )

        if result.returncode == 0:
            print("✅ Database migrations completed successfully")
        else:
            print(f"❌ Database migration failed: {result.stderr}")
            # Don't exit here, let the app start and handle DB errors gracefully
    except Exception as e:
        print(f"⚠️  Could not run migrations: {e}")
        # Don't exit here, let the app start and handle DB errors gracefully

    # Insert dummy users
    try:
        print("👥 Inserting dummy users...")
        db = SessionLocal()
        try:
            insert_dummy_users(db)
            print("✅ Dummy users inserted successfully")
        finally:
            db.close()
    except Exception as e:
        print(f"⚠️  Could not insert dummy users: {e}")
        # Don't exit here, let the app start and handle DB errors gracefully

    # Insert dummy educators
    try:
        print("👨‍🏫 Inserting dummy educators...")
        db = SessionLocal()
        try:
            insert_dummy_educators(db)
            print("✅ Dummy educators inserted successfully")
        finally:
            db.close()
    except Exception as e:
        print(f"⚠️  Could not insert dummy educators: {e}")
        # Don't exit here, let the app start and handle DB errors gracefully


@app.get("/")
def read_root():
    return {"message": "Welcome to Kiddozz API", "version": "1.0.0", "docs": "/docs"}


if __name__ == "__main__":
    import uvicorn

    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("app.main:app", host="0.0.0.0", port=port, reload=True)
