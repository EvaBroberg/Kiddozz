"""Central role definitions for server-authoritative role management."""

from enum import Enum


class Role(str, Enum):
    """
    User role enumeration.

    Values match existing token/API strings to maintain backwards compatibility:
    - "parent" -> PARENT
    - "educator" -> EDUCATOR
    - "super_educator" -> SUPER_EDUCATOR
    """

    PARENT = "parent"
    EDUCATOR = "educator"
    SUPER_EDUCATOR = "super_educator"

    @classmethod
    def from_str(cls, value: str) -> "Role":
        """
        Parse a role string into a Role enum.

        Args:
            value: Role string (case-insensitive, whitespace trimmed)

        Returns:
            Role enum value

        Raises:
            ValueError: If the role string is invalid
        """
        if not value:
            raise ValueError("Role cannot be empty")
        normalized = value.lower().strip()
        try:
            return cls(normalized)
        except ValueError:
            valid_roles = [r.value for r in cls]
            raise ValueError(
                f"Invalid role '{value}'. Valid roles: {valid_roles}"
            ) from None

    @classmethod
    def is_valid(cls, value: str) -> bool:
        """
        Check if a string is a valid role.

        Args:
            value: Role string to validate

        Returns:
            True if valid, False otherwise
        """
        if not value:
            return False
        try:
            cls.from_str(value)
            return True
        except ValueError:
            return False
