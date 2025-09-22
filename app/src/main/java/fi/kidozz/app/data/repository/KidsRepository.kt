package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.KidsApiService
import fi.kidozz.app.data.models.Kid

class KidsRepository(private val api: KidsApiService) {
    suspend fun fetchKids(daycareId: String, groupId: String? = null): List<Kid> {
        return api.getKids(daycareId, groupId)
    }
}
