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
        prefs.edit().putString("token", token).apply()
        _tokenFlow.value = token
        Log.d("TokenManagerDebug", "saveToken('$token')")
    }

    fun saveRole(role: String) {
        val canonical = role.trim().lowercase()
        prefs.edit().putString("role", canonical).apply()
        _roleFlow.value = canonical
        Log.d("TokenManagerDebug", "saveRole('$canonical')")
    }

    fun getRole(): String? {
        val r = prefs.getString("role", null)
        _roleFlow.value = r
        Log.d("TokenManagerDebug", "getRole() -> '$r'")
        return r
    }

    fun isLoggedIn(): Boolean {
        val token = prefs.getString("token", null)
        return !token.isNullOrEmpty()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _roleFlow.value = null
        _tokenFlow.value = null
        Log.d("TokenManagerDebug", "clearAll() called")
    }

    fun getToken(): String? {
        val token = prefs.getString("token", null)
        _tokenFlow.value = token
        return token
    }

    fun clearToken() {
        prefs.edit().remove("token").apply()
        _tokenFlow.value = null
        Log.d("TokenManagerDebug", "clearToken() called")
    }
}