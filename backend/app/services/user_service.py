from sqlalchemy.orm import Session

from app.core.security import create_access_token
from app.models.user import User


def create_dummy_users(db: Session) -> None:
    """
    Create dummy users for testing purposes.
    Creates 3 users with different roles and JWT tokens.
    Does nothing if users already exist to prevent duplicates.
    """
    # Check if users already exist
    existing_users = (
        db.query(User).filter(User.name.in_(["Jessica", "Sara", "Mervi"])).count()
    )

    if existing_users > 0:
        return  # Users already exist, don't create duplicates

    # Create dummy users
    dummy_users = [
        {"name": "Jessica", "role": "educator", "user_id": 1, "classes": ["Class A"]},
        {"name": "Sara", "role": "parent", "user_id": 2, "classes": ["Class A"]},
        {"name": "Mervi", "role": "super_educator", "user_id": 3, "classes": ["*"]},
    ]

    for user_data in dummy_users:
        # Create JWT token
        jwt_payload = {
            "sub": user_data["name"],
            "role": user_data["role"],
            "user_id": user_data["user_id"],
            "classes": user_data["classes"],
        }
        jwt_token = create_access_token(jwt_payload)

        # Create user with JWT token and classes
        user = User(
            name=user_data["name"], 
            role=user_data["role"], 
            jwt_token=jwt_token,
            classes=user_data["classes"]
        )

        db.add(user)

    db.commit()


def insert_dummy_users(db: Session) -> None:
    """
    Insert dummy users into the database.
    Creates 3 users with different roles and JWT tokens.
    Prevents duplicates by checking if users already exist.
    """
    # Check if users already exist
    existing_users = (
        db.query(User).filter(User.name.in_(["Jessica", "Sara", "Mervi"])).count()
    )

    if existing_users > 0:
        return  # Users already exist, don't create duplicates

    # Create dummy users
    dummy_users = [
        {"name": "Jessica", "role": "educator", "user_id": 1, "classes": ["Class A"]},
        {"name": "Sara", "role": "parent", "user_id": 2, "classes": ["Class A"]},
        {"name": "Mervi", "role": "super_educator", "user_id": 3, "classes": ["*"]},
    ]

    for user_data in dummy_users:
        # Create JWT token
        jwt_payload = {
            "sub": user_data["name"],
            "role": user_data["role"],
            "user_id": user_data["user_id"],
            "classes": user_data["classes"],
        }
        jwt_token = create_access_token(jwt_payload)

        # Create user with JWT token and classes
        user = User(
            name=user_data["name"], 
            role=user_data["role"], 
            jwt_token=jwt_token,
            classes=user_data["classes"]
        )

        db.add(user)

    db.commit()
