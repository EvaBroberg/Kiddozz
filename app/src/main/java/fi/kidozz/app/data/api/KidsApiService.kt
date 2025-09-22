package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.Kid
import retrofit2.http.GET
import retrofit2.http.Query

interface KidsApiService {
    @GET("/api/v1/kids")
    suspend fun getKids(
        @Query("daycare_id") daycareId: String,
        @Query("group_id") groupId: String? = null
    ): List<Kid>
}
