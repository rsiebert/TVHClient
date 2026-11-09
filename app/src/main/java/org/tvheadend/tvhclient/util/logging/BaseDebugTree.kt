package org.tvheadend.tvhclient.util.logging

import timber.log.Timber

abstract class BaseDebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String {
        return String.format("[C:%s] [M:%s] [L:%s] ",
                super.createStackElementTag(element),
                element.methodName,
                element.lineNumber)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Logcat is readable by anything attached to the device and gets pasted into bug reports,
        // so the same values that are redacted for the log file are redacted here too.
        super.log(priority, tag, LogRedaction.apply(message), t)
    }
}
