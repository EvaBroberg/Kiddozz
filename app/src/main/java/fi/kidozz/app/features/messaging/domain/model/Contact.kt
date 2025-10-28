package fi.kidozz.app.features.messaging.domain.model

enum class ContactType { PARENT, EDUCATOR }

data class Contact(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val type: ContactType
)