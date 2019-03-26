package org.tvheadend.tvhclient.util.logging

import timber.log.Timber

abstract class BaseDebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String {
        return String.format("[C:%s] [M:%s] [L:%s] ",
                super.createStackElementTag(element),
                element.methodName,
                element.lineNumber)
    }
}
