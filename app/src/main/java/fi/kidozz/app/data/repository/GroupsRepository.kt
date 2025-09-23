package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.GroupsApiService
import fi.kidozz.app.data.models.Group

class GroupsRepository(private val api: GroupsApiService) {
    suspend fun getGroups(daycareId: String): List<Group> {
        return api.getGroups(daycareId)
    }
}
