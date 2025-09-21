package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Parent(
    val id: String,
    val full_name: String,
    val email: String?,
    val phone_num: String?
) : Parcelable
