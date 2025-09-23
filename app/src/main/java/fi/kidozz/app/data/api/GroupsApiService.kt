package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.Group
import retrofit2.http.GET
import retrofit2.http.Query

interface GroupsApiService {
    @GET("/api/v1/groups")
    suspend fun getGroups(
        @Query("daycare_id") daycareId: String
    ): List<Group>
}
