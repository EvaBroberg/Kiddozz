package fi.kidozz.app.core.auth

/**
 * Maps server-authoritative role strings to app role enum.
 * 
 * This is a pure function for role mapping, making it testable.
 * 
 * @param serverRole Role string from backend (e.g., "parent", "educator", "super_educator")
 * @return Normalized role string for routing, or null if invalid
 */
fun mapServerRoleToAppRole(serverRole: String?): String? {
    if (serverRole == null) return null
    
    return when (serverRole.lowercase().trim()) {
        "parent" -> "parent"
        "educator" -> "educator"
        "super_educator" -> "super_educator"
        else -> null
    }
}

