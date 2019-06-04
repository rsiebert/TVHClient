package org.tvheadend.tvhclient.util.logging

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.tvheadend.tvhclient.BuildConfig

class CrashlyticsTree : BaseDebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.INFO || priority == Log.DEBUG) {
            return
        }
        if (Fabric.isInitialized()) {
            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority)
            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag)
            Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message)
            Crashlytics.setString("Git commit", BuildConfig.GIT_SHA)
            Crashlytics.setString("Build time", BuildConfig.BUILD_TIME)

            if (t == null) {
                Crashlytics.logException(Exception(message))
            } else {
                Crashlytics.logException(t)
            }
        }
    }

    companion object {

        private const val CRASHLYTICS_KEY_PRIORITY = "priority"
        private const val CRASHLYTICS_KEY_TAG = "tag"
        private const val CRASHLYTICS_KEY_MESSAGE = "message"
    }
}
