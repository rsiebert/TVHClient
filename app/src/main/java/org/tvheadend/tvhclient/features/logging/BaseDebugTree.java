package org.tvheadend.tvhclient.features.logging;

import timber.log.Timber;

abstract class BaseDebugTree extends Timber.DebugTree {

    @Override
    protected String createStackElementTag(StackTraceElement element) {
        return String.format("[C:%s] [M:%s] [L:%s] ",
                super.createStackElementTag(element),
                element.getMethodName(),
                element.getLineNumber());
    }
}
