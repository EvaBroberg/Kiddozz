package fi.kidozz.app.core.session

enum class UserRole { PARENT, EDUCATOR }

data class UserSession(
    val userId: String,
    val role: UserRole,
    val groupIds: Set<String>
)