package fi.kidozz.app.core.config

import fi.kidozz.app.BuildConfig
import android.util.Log

object FeatureFlags {
    val MESSAGING_ANDROID: Boolean = BuildConfig.MESSAGING_ANDROID

    init {
        Log.d("FeatureFlags", "MESSAGING_ANDROID=$MESSAGING_ANDROID (buildType=${BuildConfig.BUILD_TYPE})")
    }
}




