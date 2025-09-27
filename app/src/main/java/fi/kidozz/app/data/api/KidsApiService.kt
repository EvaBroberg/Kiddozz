package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.AbsenceReasonsResponse
import fi.kidozz.app.data.models.Kid
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface KidsApiService {
    @GET("/api/v1/kids")
    suspend fun getKids(
        @Query("daycare_id") daycareId: String,
        @Query("group_id") groupId: String? = null
    ): List<Kid>
    
    @PATCH("/api/v1/kids/{id}/attendance")
    suspend fun updateAttendance(
        @Path("id") id: String,
        @Body status: Map<String, String>
    ): Response<Unit>
    
    @GET("/api/v1/kids/absence-reasons")
    suspend fun getAbsenceReasons(): Response<AbsenceReasonsResponse>
}
