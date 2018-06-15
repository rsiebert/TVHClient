package org.tvheadend.tvhclient.injection.modules;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MainApplicationModule {

    private final Context appContext;

    public MainApplicationModule(@NonNull Context context) {
        this.appContext = context;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return appContext;
    }
}
