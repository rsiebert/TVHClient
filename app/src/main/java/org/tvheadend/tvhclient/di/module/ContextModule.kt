package org.tvheadend.tvhclient.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tvheadend.data.di.FeatureScope

@Suppress("unused")
@Module
class ContextModule(private val appContext: Context) {

    @Provides
    @FeatureScope
    internal fun provideContext(): Context {
        return appContext
    }
}
