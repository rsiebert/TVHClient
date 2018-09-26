package org.tvheadend.tvhclient.features.logging;

import com.crashlytics.android.Crashlytics;

public class ReleaseTree extends BaseDebugTree {

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        // Logs the given message when a crash occurred.
        // In addition to writing to the next crash report, it will also
        // write to the LogCat using android.util.Log.println(priority, tag, msg)
        Crashlytics.log(priority, tag, message);
    }
}
