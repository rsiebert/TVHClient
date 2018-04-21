package org.tvheadend.tvhclient.features.logging;

import android.util.Log;

public class ReleaseTree extends BaseDebugTree {

    @Override
    protected boolean isLoggable(String tag, int priority) {
        return !(priority == Log.INFO || priority == Log.VERBOSE);
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (isLoggable(tag, priority)) {
            switch (priority) {
                case Log.DEBUG:
                    Log.d(tag, message, t);
                    break;
                case Log.WARN:
                    Log.w(tag, message, t);
                    break;
                case Log.ERROR:
                    Log.e(tag, message, t);
                    break;
            }
        }
    }
}
