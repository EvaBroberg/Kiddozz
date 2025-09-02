package fi.kidozz.app.data.models

import java.util.UUID

data class Guardian(
    val name: String,
    val email: String,
    val phone: String,
    val relationship: String
)

data class Kid(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var attendanceStatus: String = "OUT",
    val profileImageUrl: String? = null,
    val allergies: List<String> = emptyList(),
    val needToKnow: String = "",
    val primaryGuardian: Guardian,
    val secondaryGuardian: Guardian? = null,
    val authorizedPickups: List<Guardian> = emptyList(),
    val address: String = ""
)
