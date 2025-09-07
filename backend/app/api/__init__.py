from fastapi import APIRouter

from app.api.events import router as events_router

api_router = APIRouter()
api_router.include_router(events_router, prefix="/events", tags=["events"])
