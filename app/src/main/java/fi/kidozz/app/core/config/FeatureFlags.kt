package fi.kidozz.app.core.config

import fi.kidozz.app.BuildConfig
import android.util.Log

object FeatureFlags {
    val MESSAGING_ANDROID: Boolean = BuildConfig.MESSAGING_ANDROID

    init {
        Log.d("FeatureFlags", "MESSAGING_ANDROID=$MESSAGING_ANDROID (buildType=${BuildConfig.BUILD_TYPE})")
    }
}

enum class AuthMode {
    DEV,
    INVITE
}

object AuthConfig {
    /**
     * Authentication mode: DEV (default) or INVITE.
     * 
     * Currently defaults to DEV. Can be overridden via BuildConfig.AUTH_MODE
     * in build variants later (e.g., buildConfigField("String", "AUTH_MODE", "\"INVITE\"")).
     */
    val authMode: AuthMode = AuthMode.DEV
}




