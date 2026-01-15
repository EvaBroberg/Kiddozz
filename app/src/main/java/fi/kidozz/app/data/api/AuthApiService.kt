package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.AcceptInviteRequest
import fi.kidozz.app.data.models.AcceptInviteResponse
import fi.kidozz.app.data.models.DevLoginRequest
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Parent
import fi.kidozz.app.data.models.TokenResponse
import fi.kidozz.app.data.models.UserInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

interface AuthApiService {
    @GET("api/v1/educators")
    suspend fun getEducators(
        @Query("daycare_id") daycareId: String,
        @Query("search") search: String? = null
    ): Response<List<Educator>>
    
    @GET("api/v1/parents")
    suspend fun getParents(
        @Query("daycare_id") daycareId: String,
        @Query("search") search: String? = null
    ): Response<List<Parent>>
    
    @POST("api/v1/auth/dev-login")
    suspend fun devLogin(@Body request: DevLoginRequest): Response<TokenResponse>
    
    @GET("api/v1/auth/me")
    suspend fun getCurrentUserInfo(): Response<UserInfo>
    
    @POST("api/v1/auth/accept-invite")
    suspend fun acceptInvite(@Body request: AcceptInviteRequest): Response<AcceptInviteResponse>
}
