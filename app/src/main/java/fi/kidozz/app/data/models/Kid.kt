package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Kid(
    val id: String,
    val full_name: String,
    val dob: String,
    val group_id: String,
    val daycare_id: String,
    val trusted_adults: List<TrustedAdult>
) : Parcelable

@Parcelize
data class TrustedAdult(
    val name: String,
    val email: String?,
    val phone_num: String?,
    val address: String?
) : Parcelable