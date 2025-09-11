from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import api_router, health
from app.core.config import settings
from app.core.database import Base, engine

# Create database tables
Base.metadata.create_all(bind=engine)

# Create FastAPI app
app = FastAPI(
    title=settings.project_name,
    description="Kiddozz API for managing events and images",
    version="1.0.0",
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register routes
app.include_router(health.router)

# Include API routes
app.include_router(api_router, prefix=settings.api_v1_str)


@app.get("/")
def read_root():
    """Root endpoint"""
    return {"message": "Welcome to Kiddozz API", "version": "1.0.0", "docs": "/docs"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
