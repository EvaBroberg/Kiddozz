from datetime import date

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.models.daycare import Daycare
from app.models.group import Group
from app.models.kid import Kid
from app.models.parent import Parent


def seed_daycare_data(db: Session) -> None:
    """
    Seed the database with dummy daycare data for testing.

    Creates:
    - 1 daycare: "Happy Kids Daycare"
    - 3 groups: "Group A", "Group B", "Group C"
    - 3 parents linked to kids
    - 9 kids (3 per group) with trusted adults
    """

    # Create daycare
    daycare = Daycare(name="Happy Kids Daycare")
    db.add(daycare)
    db.commit()
    db.refresh(daycare)

    # Create groups
    groups_data = [{"name": "Group A"}, {"name": "Group B"}, {"name": "Group C"}]

    groups = []
    for group_data in groups_data:
        group = Group(name=group_data["name"], daycare_id=daycare.id)
        db.add(group)
        groups.append(group)

    db.commit()
    for group in groups:
        db.refresh(group)

    # Create parents
    parents_data = [
        {"full_name": "Sara", "email": "sara@example.com", "phone_num": "+1987654321"},
        {
            "full_name": "Laura",
            "email": "laura@example.com",
            "phone_num": "+1987654322",
        },
        {
            "full_name": "Angela",
            "email": "angela@example.com",
            "phone_num": "+1987654323",
        },
    ]

    parents = []
    for parent_data in parents_data:
        parent = Parent(
            full_name=parent_data["full_name"],
            email=parent_data["email"],
            phone_num=parent_data["phone_num"],
            daycare_id=daycare.id,
        )
        db.add(parent)
        parents.append(parent)

    db.commit()
    for parent in parents:
        db.refresh(parent)

    # Create kids
    kids_data = [
        # Group A kids
        {
            "full_name": "Emma Johnson",
            "dob": date(2020, 3, 15),
            "group_index": 0,
            "parent_index": 0,
            "trusted_adults": [
                {
                    "name": "Grandma Johnson",
                    "email": "grandma@example.com",
                    "phone": "+1555000001",
                    "address": "123 Main St, City",
                }
            ],
        },
        {
            "full_name": "Liam Smith",
            "dob": date(2020, 7, 22),
            "group_index": 0,
            "parent_index": 0,
            "trusted_adults": [
                {
                    "name": "Uncle Mike",
                    "email": "mike@example.com",
                    "phone": "+1555000002",
                    "address": "456 Oak Ave, City",
                }
            ],
        },
        {
            "full_name": "Sophia Brown",
            "dob": date(2020, 11, 8),
            "group_index": 0,
            "parent_index": 0,
            "trusted_adults": [],
        },
        # Group B kids
        {
            "full_name": "Noah Davis",
            "dob": date(2019, 5, 12),
            "group_index": 1,
            "parent_index": 1,
            "trusted_adults": [
                {
                    "name": "Aunt Sarah",
                    "email": "sarah@example.com",
                    "phone": "+1555000003",
                    "address": "789 Pine St, City",
                }
            ],
        },
        {
            "full_name": "Olivia Wilson",
            "dob": date(2019, 9, 30),
            "group_index": 1,
            "parent_index": 1,
            "trusted_adults": [
                {
                    "name": "Family Friend Tom",
                    "email": "tom@example.com",
                    "phone": "+1555000004",
                    "address": "321 Elm St, City",
                }
            ],
        },
        {
            "full_name": "William Miller",
            "dob": date(2019, 12, 3),
            "group_index": 1,
            "parent_index": 1,
            "trusted_adults": [],
        },
        # Group C kids
        {
            "full_name": "Ava Garcia",
            "dob": date(2021, 1, 18),
            "group_index": 2,
            "parent_index": 2,
            "trusted_adults": [
                {
                    "name": "Neighbor Lisa",
                    "email": "lisa@example.com",
                    "phone": "+1555000005",
                    "address": "654 Maple Dr, City",
                }
            ],
        },
        {
            "full_name": "James Rodriguez",
            "dob": date(2021, 4, 25),
            "group_index": 2,
            "parent_index": 2,
            "trusted_adults": [
                {
                    "name": "Cousin Alex",
                    "email": "alex@example.com",
                    "phone": "+1555000006",
                    "address": "987 Cedar Ln, City",
                }
            ],
        },
        {
            "full_name": "Isabella Martinez",
            "dob": date(2021, 8, 14),
            "group_index": 2,
            "parent_index": 2,
            "trusted_adults": [],
        },
    ]

    kids = []
    for kid_data in kids_data:
        kid = Kid(
            full_name=kid_data["full_name"],
            dob=kid_data["dob"],
            daycare_id=daycare.id,
            group_id=groups[kid_data["group_index"]].id,
            trusted_adults=kid_data["trusted_adults"],
        )
        db.add(kid)
        kids.append(kid)

    db.commit()
    for kid in kids:
        db.refresh(kid)

    # Link parents to kids
    for i, kid in enumerate(kids):
        parent_index = kids_data[i]["parent_index"]
        parent = parents[parent_index]
        parent.kids.append(kid)

    db.commit()

    print("✅ Seeded daycare data (without educators) successfully!")
    print(f"   - Daycare: {daycare.name} (ID: {daycare.id})")
    print(f"   - Groups: {len(groups)} created")
    print(f"   - Parents: {len(parents)} created")
    print(f"   - Kids: {len(kids)} created")


def clear_daycare_data(db: Session) -> None:
    """Clear all daycare-related data from the database."""

    # Delete in reverse order of dependencies
    db.execute(text("DELETE FROM parent_kids"))
    db.execute(text("DELETE FROM educator_groups"))
    db.execute(text("DELETE FROM kids"))
    db.execute(text("DELETE FROM parents"))
    db.execute(text("DELETE FROM educators"))
    db.execute(text("DELETE FROM groups"))
    db.execute(text("DELETE FROM daycares"))

    db.commit()
    print("✅ Cleared all daycare data")
