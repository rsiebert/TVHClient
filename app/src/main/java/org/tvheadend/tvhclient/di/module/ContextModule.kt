package org.tvheadend.tvhclient.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("unused")
@Module
class ContextModule(private val appContext: Context) {

    @Provides
    @Singleton
    internal fun provideContext(): Context {
        return appContext
    }
}
