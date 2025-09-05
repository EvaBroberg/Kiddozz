from fastapi import APIRouter
from app.api.events import router as events_router
from app.core.config import settings

api_router = APIRouter()
api_router.include_router(events_router, prefix="/events", tags=["events"])
