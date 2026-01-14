package fi.kidozz.app.data.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Role is no longer persisted - it comes from server (/auth/me)
    // Keep roleFlow for backwards compatibility during transition, but it's not authoritative
    private val _roleFlow = MutableStateFlow<String?>(null)
    val roleFlow: StateFlow<String?> = _roleFlow

    private val _tokenFlow = MutableStateFlow<String?>(prefs.getString("token", null))
    val tokenFlow: StateFlow<String?> = _tokenFlow

    /**
     * Decode JWT token and extract claims.
     * Note: This only decodes the payload without verification.
     * For production, use a proper JWT library with verification.
     */
    private fun decodeJwtPayload(token: String): Map<String, Any>? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            val json = JSONObject(decodedString)
            
            // Convert JSONObject to Map
            val map = mutableMapOf<String, Any>()
            json.keys().forEach { key ->
                map[key] = json.get(key)
            }
            map
        } catch (e: Exception) {
            Log.e("TokenManager", "Failed to decode JWT: ${e.message}")
            null
        }
    }

    /**
     * Extract user ID from JWT token (sub claim).
     */
    fun getUserId(): String? {
        val token = getToken()
        if (token == null) return null
        
        val payload = decodeJwtPayload(token) ?: return null
        return payload["sub"]?.toString()
    }

    /**
     * Extract daycare ID from JWT token.
     */
    fun getDaycareId(): String? {
        val token = getToken()
        if (token == null) return null
        
        val payload = decodeJwtPayload(token) ?: return null
        return payload["daycare_id"]?.toString()
    }

    private val _userIdFlow = MutableStateFlow<String?>(null)
    val userIdFlow: StateFlow<String?> = _userIdFlow

    private val _daycareIdFlow = MutableStateFlow<String?>(null)
    val daycareIdFlow: StateFlow<String?> = _daycareIdFlow

    private fun updateDerivedClaims() {
        val token = _tokenFlow.value
        if (token == null) {
            _userIdFlow.value = null
            _daycareIdFlow.value = null
        } else {
            val payload = decodeJwtPayload(token)
            _userIdFlow.value = payload?.get("sub")?.toString()
            _daycareIdFlow.value = payload?.get("daycare_id")?.toString()
        }
    }

    fun saveToken(token: String) {
        val current = _tokenFlow.value
        if (current != token) {
            prefs.edit().putString("token", token).apply()
            _tokenFlow.value = token
            updateDerivedClaims() // Update userId and daycareId when token changes
            Log.d("TokenManagerDebug", "saveToken('$token') – changed from '$current'")
        } else {
            Log.d("TokenManagerDebug", "saveToken('$token') – skipped duplicate")
        }
    }

    // Role persistence removed - role is now server-authoritative via /auth/me
    // These methods are kept for backwards compatibility but do not persist to SharedPreferences
    @Deprecated("Role is now server-authoritative. Use /auth/me endpoint instead.", ReplaceWith(""))
    fun saveRole(role: String) {
        // No-op: role is no longer persisted
        Log.d("TokenManager", "saveRole() called but ignored - role is server-authoritative")
    }

    @Deprecated("Role is now server-authoritative. Use /auth/me endpoint instead.", ReplaceWith(""))
    fun clearRole() {
        // No-op: role is no longer persisted
        _roleFlow.value = null
        Log.d("TokenManager", "clearRole() called - clearing in-memory role only")
    }

    @Deprecated("Role is now server-authoritative. Use /auth/me endpoint instead.", ReplaceWith(""))
    fun getRole(): String? {
        // Return in-memory role only (not from SharedPreferences)
        return _roleFlow.value
    }

    fun isLoggedIn(): Boolean {
        val token = prefs.getString("token", null)
        return !token.isNullOrEmpty()
    }

    fun clearAll() {
        val hadToken = _tokenFlow.value
        prefs.edit().clear().apply()

        // Clear in-memory role (not persisted, but clear for consistency)
        _roleFlow.value = null
        if (hadToken != null) {
            _tokenFlow.value = null
            Log.d("TokenManagerDebug", "clearAll() reset token from '$hadToken'")
        }
    }

    fun getToken(): String? {
        val token = prefs.getString("token", null)
        val current = _tokenFlow.value
        if (current != token) {
            _tokenFlow.value = token
            updateDerivedClaims() // Update userId and daycareId when token changes
            Log.d("TokenManagerDebug", "getToken() -> '$token' (updated from '$current')")
        } else {
            Log.d("TokenManagerDebug", "getToken() -> '$token' (no change)")
        }
        return token
    }

    fun clearToken() {
        prefs.edit().remove("token").apply()
        _tokenFlow.value = null
        updateDerivedClaims() // Clear derived claims
        Log.d("TokenManagerDebug", "clearToken() called")
    }

    init {
        // Initialize derived claims from existing token
        updateDerivedClaims()
    }
}