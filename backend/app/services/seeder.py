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

    # Create parents using the new function
    parents = insert_dummy_parents(db, daycare.id)

    # Create kids using the new function
    kids = insert_dummy_kids(db, daycare.id, groups, parents)

    print("✅ Seeded daycare data (without educators) successfully!")
    print(f"   - Daycare: {daycare.name} (ID: {daycare.id})")
    print(f"   - Groups: {len(groups)} created")
    print(f"   - Parents: {len(parents)} created")
    print(f"   - Kids: {len(kids)} created")


def insert_dummy_parents(db: Session, daycare_id: str) -> list[Parent]:
    """
    Insert dummy parents into the database.
    Creates 6 parents with proper last names for family consistency.
    Returns the list of created parents.
    """
    # Check if parents already exist
    existing_parents = (
        db.query(Parent)
        .filter(
            Parent.email.in_(
                [
                    "sara@example.com",
                    "laura.smith@example.com",
                    "angela.davis@example.com",
                    "michael.wilson@example.com",
                    "emma.garcia@example.com",
                    "david.miller@example.com",
                ]
            )
        )
        .all()
    )

    if len(existing_parents) == 6:
        print("Parents already exist, skipping seeding.")
        return existing_parents

    parents_data = [
        {
            "full_name": "Sara",
            "email": "sara@example.com",
            "phone_num": "+1987654321",
        },
        {
            "full_name": "Laura Smith",
            "email": "laura.smith@example.com",
            "phone_num": "+1987654322",
        },
        {
            "full_name": "Angela Davis",
            "email": "angela.davis@example.com",
            "phone_num": "+1987654323",
        },
        {
            "full_name": "Michael Wilson",
            "email": "michael.wilson@example.com",
            "phone_num": "+1987654324",
        },
        {
            "full_name": "Emma Garcia",
            "email": "emma.garcia@example.com",
            "phone_num": "+1987654325",
        },
        {
            "full_name": "David Miller",
            "email": "david.miller@example.com",
            "phone_num": "+1987654326",
        },
    ]

    parents = []
    for parent_data in parents_data:
        # Check if parent already exists by email
        if db.query(Parent).filter(Parent.email == parent_data["email"]).first():
            continue

        parent = Parent(
            full_name=parent_data["full_name"],
            email=parent_data["email"],
            phone_num=parent_data["phone_num"],
            daycare_id=daycare_id,
        )
        db.add(parent)
        parents.append(parent)

    db.commit()
    for parent in parents:
        db.refresh(parent)

    # Get all parents for this daycare
    all_parents = db.query(Parent).filter(Parent.daycare_id == daycare_id).all()
    print(f"✅ Seeded {len(all_parents)} parents successfully!")
    return all_parents


def insert_dummy_kids(
    db: Session, daycare_id: str, groups: list[Group], parents: list[Parent]
) -> list[Kid]:
    """
    Insert dummy kids into the database.
    Creates 9 kids (3 per group) with proper parent relationships and last name matching.
    Returns the list of created kids.
    """
    # Check if kids already exist
    existing_kids = (
        db.query(Kid)
        .filter(
            Kid.full_name.in_(
                [
                    "Emma Sara",
                    "Liam Johnson",
                    "Sophia Smith",
                    "Noah Davis",
                    "Olivia Wilson",
                    "William Wilson",
                    "Ava Garcia",
                    "James Garcia",
                    "Isabella Miller",
                ]
            )
        )
        .all()
    )

    if len(existing_kids) == 9:
        print("Kids already exist, skipping seeding.")
        return existing_kids

    kids_data = [
        # Group A kids (3 kids, 2 parents)
        {
            "full_name": "Emma Sara",
            "dob": date(2020, 3, 15),
            "group_index": 0,
            "parent_index": 0,  # Sara
            "trusted_adults": [
                {
                    "name": "Grandma Sara",
                    "email": "grandma@example.com",
                    "phone": "+1555000001",
                    "address": "123 Main St, City",
                }
            ],
        },
        {
            "full_name": "Liam Sara",
            "dob": date(2020, 7, 22),
            "group_index": 0,
            "parent_index": 0,  # Sara
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
            "full_name": "Sophia Smith",
            "dob": date(2020, 11, 8),
            "group_index": 0,
            "parent_index": 1,  # Laura Smith
            "trusted_adults": [],
        },
        # Group B kids (3 kids, 2 parents)
        {
            "full_name": "Noah Davis",
            "dob": date(2019, 5, 12),
            "group_index": 1,
            "parent_index": 2,  # Angela Davis
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
            "parent_index": 3,  # Michael Wilson
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
            "full_name": "William Wilson",
            "dob": date(2019, 12, 3),
            "group_index": 1,
            "parent_index": 3,  # Michael Wilson
            "trusted_adults": [],
        },
        # Group C kids (3 kids, 2 parents)
        {
            "full_name": "Ava Garcia",
            "dob": date(2021, 1, 18),
            "group_index": 2,
            "parent_index": 4,  # Emma Garcia
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
            "full_name": "James Garcia",
            "dob": date(2021, 4, 25),
            "group_index": 2,
            "parent_index": 4,  # Emma Garcia
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
            "full_name": "Isabella Miller",
            "dob": date(2021, 8, 14),
            "group_index": 2,
            "parent_index": 5,  # David Miller
            "trusted_adults": [],
        },
    ]

    kids = []
    for kid_data in kids_data:
        # Check if kid already exists by name
        if db.query(Kid).filter(Kid.full_name == kid_data["full_name"]).first():
            continue

        # Validate parent index is valid
        parent_index = kid_data["parent_index"]
        if parent_index >= len(parents):
            raise ValueError(
                f"Invalid parent_index {parent_index} for kid {kid_data['full_name']}"
            )

        kid = Kid(
            full_name=kid_data["full_name"],
            dob=kid_data["dob"],
            daycare_id=daycare_id,
            group_id=groups[kid_data["group_index"]].id,
            trusted_adults=kid_data["trusted_adults"],
        )
        db.add(kid)
        kids.append(kid)

    db.commit()
    for kid in kids:
        db.refresh(kid)

    # Link parents to kids - ensure every kid has at least one parent
    for i, kid in enumerate(kids):
        parent_index = kids_data[i]["parent_index"]
        if parent_index < len(parents):
            parent = parents[parent_index]
            parent.kids.append(kid)
        else:
            raise ValueError(
                f"Kid {kid.full_name} has invalid parent_index {parent_index}"
            )

    db.commit()

    # Final validation: ensure all kids have at least one parent
    for kid in kids:
        if not kid.parents or len(kid.parents) == 0:
            raise ValueError(
                f"Kid {kid.full_name} was created without any parent links"
            )

    # Get all kids for this daycare
    all_kids = db.query(Kid).filter(Kid.daycare_id == daycare_id).all()
    print(f"✅ Seeded {len(all_kids)} kids successfully!")
    return all_kids


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
