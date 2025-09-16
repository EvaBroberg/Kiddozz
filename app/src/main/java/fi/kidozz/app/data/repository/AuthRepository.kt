package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.AuthApiService
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.data.models.DummyUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    
    suspend fun getDummyUsers(): Result<List<DummyUser>> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.getDummyUsers()
            if (response.isSuccessful) {
                val users = response.body()?.users ?: emptyList()
                Result.success(users)
            } else {
                Result.failure(Exception("Failed to fetch dummy users: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun loginWithToken(token: String) {
        tokenManager.saveToken(token)
    }
    
    fun logout() {
        tokenManager.clearToken()
    }
    
    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
    
    fun getCurrentToken(): String? {
        return tokenManager.getToken()
    }
}
