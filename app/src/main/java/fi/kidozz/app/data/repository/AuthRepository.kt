package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.AuthApiService
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.data.models.AcceptInviteRequest
import fi.kidozz.app.data.models.AcceptInviteResponse
import fi.kidozz.app.data.models.DevLoginRequest
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.models.Parent
import fi.kidozz.app.data.models.TokenResponse
import fi.kidozz.app.data.models.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    
    /**
     * Get current user information from server (server-authoritative role).
     * Returns role, user_id, and daycare_id from backend.
     */
    suspend fun getCurrentUserInfo(): Result<UserInfo> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.getCurrentUserInfo()
            if (response.isSuccessful) {
                val userInfo = response.body()
                if (userInfo != null) {
                    Result.success(userInfo)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("Failed to get user info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEducators(daycareId: String, search: String? = null): Result<List<Educator>> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.getEducators(daycareId, search)
            if (response.isSuccessful) {
                val educators = response.body() ?: emptyList()
                Result.success(educators)
            } else {
                Result.failure(Exception("Failed to fetch educators: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getParents(daycareId: String, search: String? = null): Result<List<Parent>> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.getParents(daycareId, search)
            if (response.isSuccessful) {
                val parents = response.body() ?: emptyList()
                Result.success(parents)
            } else {
                Result.failure(Exception("Failed to fetch parents: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun devLoginAsEducator(educatorId: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val request = DevLoginRequest(educator_id = educatorId)
            val response = authApiService.devLogin(request)
            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    Result.success(tokenResponse)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("Failed to login as educator: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun devLoginAsParent(parentId: String): Result<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val request = DevLoginRequest(parent_id = parentId)
            val response = authApiService.devLogin(request)
            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    Result.success(tokenResponse)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("Failed to login as parent: ${response.code()}"))
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
    
    /**
     * Accept an invite token and create a user account.
     * Returns the access token response on success.
     */
    suspend fun acceptInvite(token: String, name: String, phoneNum: String? = null): Result<AcceptInviteResponse> = withContext(Dispatchers.IO) {
        try {
            val request = AcceptInviteRequest(token = token, name = name, phone_num = phoneNum)
            val response = authApiService.acceptInvite(request)
            if (response.isSuccessful) {
                val inviteResponse = response.body()
                if (inviteResponse != null) {
                    Result.success(inviteResponse)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to accept invite: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
