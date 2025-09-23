package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.Educator
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface EducatorApiService {
    @GET("/api/v1/auth/me/educator")
    suspend fun getCurrentEducator(@Header("Authorization") token: String): Response<Educator>
    
    @GET("/api/v1/educators")
    suspend fun getEducators(
        @Query("daycare_id") daycareId: String,
        @Query("search") search: String? = null
    ): List<Educator>
}
