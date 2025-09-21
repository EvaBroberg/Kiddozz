import uuid

from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.daycare import Daycare


def resolve_daycare_id(db: Session, daycare_id: str) -> str:
    """Resolve daycare ID, mapping 'default-daycare-id' to a real UUID in local and test environments."""
    if settings.app_env in ("local", "test") and daycare_id == "default-daycare-id":
        daycare = db.query(Daycare).first()
        if not daycare:
            daycare = Daycare(id=str(uuid.uuid4()), name="Local Dev Daycare")
            db.add(daycare)
            db.commit()
            db.refresh(daycare)
        return daycare.id
    return daycare_id
