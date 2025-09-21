package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DevLoginRequest(
    val educator_id: String? = null,
    val parent_id: String? = null
) : Parcelable

@Parcelize
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer"
) : Parcelable
