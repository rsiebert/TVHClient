package org.tvheadend.tvhclient;

import android.app.Application;

import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class TestMainApplication extends Application implements TestLifecycleApplication {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void beforeTest(Method method) {
    }

    @Override
    public void prepareTest(Object test) {
    }

    @Override
    public void afterTest(Method method) {
    }
}
