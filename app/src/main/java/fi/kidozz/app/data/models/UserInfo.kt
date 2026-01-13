package fi.kidozz.app.data.models

/**
 * Response from GET /api/v1/auth/me endpoint.
 * Contains server-authoritative user identity.
 */
data class UserInfo(
    val user_id: String,
    val role: String,
    val daycare_id: String? = null,
    val exp: Long? = null
)

