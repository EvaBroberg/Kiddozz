package fi.kidozz.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Guardian(
    val name: String,
    val email: String,
    val phone: String,
    val relationship: String
) : Parcelable

@Parcelize
data class Kid(
    val id: String,
    val name: String,
    val age: Int,
    val className: String
) : Parcelable
