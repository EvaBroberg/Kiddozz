package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Group(
    val id: String,
    val name: String
) : Parcelable

@Parcelize
data class Educator(
    val id: String,
    val full_name: String,
    val role: String,
    val email: String?,
    val phone_num: String?,
    val groups: @RawValue List<Group>
) : Parcelable
