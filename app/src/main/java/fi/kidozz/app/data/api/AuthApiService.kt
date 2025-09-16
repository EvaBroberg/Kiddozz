package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.DummyUsersResponse
import retrofit2.Response
import retrofit2.http.GET

interface AuthApiService {
    @GET("auth/dummy-users")
    suspend fun getDummyUsers(): Response<DummyUsersResponse>
}
