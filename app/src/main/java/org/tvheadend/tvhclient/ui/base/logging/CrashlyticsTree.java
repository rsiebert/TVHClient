package org.tvheadend.tvhclient.ui.base.logging;

import androidx.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.tvheadend.tvhclient.BuildConfig;

import io.fabric.sdk.android.Fabric;

public class CrashlyticsTree extends BaseDebugTree {

    private static final String CRASHLYTICS_KEY_PRIORITY = "priority";
    private static final String CRASHLYTICS_KEY_TAG = "tag";
    private static final String CRASHLYTICS_KEY_MESSAGE = "message";

    @Override
    protected void log(int priority, @Nullable String tag, @Nullable String message, @Nullable Throwable t) {
        if (priority == Log.VERBOSE || priority == Log.INFO) {
            return;
        }
        if (Fabric.isInitialized()) {
            Crashlytics.setInt(CRASHLYTICS_KEY_PRIORITY, priority);
            Crashlytics.setString(CRASHLYTICS_KEY_TAG, tag);
            Crashlytics.setString(CRASHLYTICS_KEY_MESSAGE, message);
            Crashlytics.setString("Git commit", BuildConfig.GIT_SHA);
            Crashlytics.setString("Build time", BuildConfig.BUILD_TIME);

            if (t == null) {
                Crashlytics.logException(new Exception(message));
            } else {
                Crashlytics.logException(t);
            }
        }
    }
}
