package fi.kidozz.app.data.models

/**
 * Request body for POST /api/v1/auth/accept-invite
 */
data class AcceptInviteRequest(
    val token: String,
    val name: String,
    val phone_num: String? = null
)

