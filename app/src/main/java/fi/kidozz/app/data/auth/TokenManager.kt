package fi.kidozz.app.data.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _roleFlow = MutableStateFlow<String?>(prefs.getString("role", null))
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

    fun saveRole(role: String) {
        val canonical = role.trim().lowercase()
        val current = _roleFlow.value
        if (current != canonical) {
            prefs.edit().putString("role", canonical).apply()
            _roleFlow.value = canonical
            Log.d("TokenManagerDebug", "saveRole('$canonical') – changed from '$current'")
        } else {
            Log.d("TokenManagerDebug", "saveRole('$canonical') – skipped duplicate")
        }
    }

    fun clearRole() {
        val hadRole = _roleFlow.value
        if (hadRole != null) {
            prefs.edit().remove("role").apply()
            _roleFlow.value = null
            Log.d("TokenManagerDebug", "clearRole() reset role from '$hadRole'")
        }
    }

    fun getRole(): String? {
        val r = prefs.getString("role", null)
        val current = _roleFlow.value
        if (current != r) {
            _roleFlow.value = r
            Log.d("TokenManagerDebug", "getRole() -> '$r' (updated from '$current')")
        } else {
            Log.d("TokenManagerDebug", "getRole() -> '$r' (no change)")
        }
        return r
    }

    fun isLoggedIn(): Boolean {
        val token = prefs.getString("token", null)
        return !token.isNullOrEmpty()
    }

    fun clearAll() {
        val hadRole = _roleFlow.value
        val hadToken = _tokenFlow.value
        prefs.edit().clear().apply()

        if (hadRole != null) {
            _roleFlow.value = null
            Log.d("TokenManagerDebug", "clearAll() reset role from '$hadRole'")
        }
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