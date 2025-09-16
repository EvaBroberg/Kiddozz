package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DummyUser(
    val id: Int,
    val name: String,
    val role: String,
    val token: String
) : Parcelable

@Parcelize
data class DummyUsersResponse(
    val users: List<DummyUser>
) : Parcelable
