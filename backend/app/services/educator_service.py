from sqlalchemy.orm import Session

from app.core.security import create_access_token
from app.models.daycare import Daycare
from app.models.educator import Educator, EducatorRole
from app.models.group import Group


def insert_dummy_educators(db: Session) -> None:
    """
    Insert dummy educators into the database.
    Creates 4 educators with different roles and group assignments.
    Prevents duplicates by checking if educators already exist.
    """
    # Check if educators already exist
    existing_educators = (
        db.query(Educator)
        .filter(
            Educator.full_name.in_(
                ["Anna Johnson", "Mark Smith", "Sarah Davis", "Lisa Wilson"]
            )
        )
        .count()
    )

    if existing_educators > 0:
        return  # Educators already exist, don't create duplicates

    # Get or create a default daycare
    daycare = db.query(Daycare).first()
    if not daycare:
        daycare = Daycare(name="Happy Kids Daycare")
        db.add(daycare)
        db.commit()
        db.refresh(daycare)

    # Get or create groups
    groups = db.query(Group).filter(Group.daycare_id == daycare.id).all()
    if not groups:
        # Create groups if they don't exist
        group_names = ["Group A", "Group B", "Group C"]
        groups = []
        for group_name in group_names:
            group = Group(name=group_name, daycare_id=daycare.id)
            db.add(group)
            groups.append(group)
        db.commit()
        for group in groups:
            db.refresh(group)

    # Create dummy educators
    dummy_educators = [
        {
            "full_name": "Anna Johnson",
            "email": "anna.johnson@daycare.com",
            "phone_num": "+1234567890",
            "role": EducatorRole.EDUCATOR.value,
            "group_names": ["Group A"],
        },
        {
            "full_name": "Mark Smith",
            "email": "mark.smith@daycare.com",
            "phone_num": "+1234567891",
            "role": EducatorRole.EDUCATOR.value,
            "group_names": ["Group B"],
        },
        {
            "full_name": "Sarah Davis",
            "email": "sarah.davis@daycare.com",
            "phone_num": "+1234567892",
            "role": EducatorRole.EDUCATOR.value,
            "group_names": ["Group C"],
        },
        {
            "full_name": "Lisa Wilson",
            "email": "lisa.wilson@daycare.com",
            "phone_num": "+1234567893",
            "role": EducatorRole.SUPER_EDUCATOR.value,
            "group_names": ["Group A", "Group B", "Group C"],
        },
    ]

    educators = []
    for educator_data in dummy_educators:
        # Create JWT token
        jwt_payload = {
            "sub": educator_data["full_name"],
            "role": educator_data["role"],
            "user_id": len(educators) + 1,
            "daycare_id": daycare.id,
        }
        jwt_token = create_access_token(jwt_payload)

        educator = Educator(
            full_name=educator_data["full_name"],
            email=educator_data["email"],
            phone_num=educator_data["phone_num"],
            role=educator_data["role"],
            jwt_token=jwt_token,
            daycare_id=daycare.id,
        )
        db.add(educator)
        educators.append(educator)

    db.commit()
    for educator in educators:
        db.refresh(educator)

    # Assign educators to groups
    for educator_data, educator in zip(dummy_educators, educators):
        for group_name in educator_data["group_names"]:
            group = next((g for g in groups if g.name == group_name), None)
            if group:
                educator.groups.append(group)

    db.commit()
    print("✅ Seeded educators successfully!")
