package fi.kidozz.app.data.auth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _roleFlow = MutableStateFlow<String?>(prefs.getString("role", null))
    val roleFlow: StateFlow<String?> = _roleFlow

    private val _tokenFlow = MutableStateFlow<String?>(prefs.getString("token", null))
    val tokenFlow: StateFlow<String?> = _tokenFlow

    fun saveToken(token: String) {
        val current = _tokenFlow.value
        if (current != token) {
            prefs.edit().putString("token", token).apply()
            _tokenFlow.value = token
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
            Log.d("TokenManagerDebug", "getToken() -> '$token' (updated from '$current')")
        } else {
            Log.d("TokenManagerDebug", "getToken() -> '$token' (no change)")
        }
        return token
    }

    fun clearToken() {
        prefs.edit().remove("token").apply()
        _tokenFlow.value = null
        Log.d("TokenManagerDebug", "clearToken() called")
    }
}