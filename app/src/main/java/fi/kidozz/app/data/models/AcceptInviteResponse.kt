package fi.kidozz.app.data.models

/**
 * Response from POST /api/v1/auth/accept-invite
 */
data class AcceptInviteResponse(
    val access_token: String,
    val token_type: String,
    val user_id: String,
    val role: String,
    val daycare_id: String
)

