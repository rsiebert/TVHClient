package org.tvheadend.tvhclient.di.module

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import org.tvheadend.data.di.FeatureScope

@Suppress("unused")
@Module
class SharedPreferencesModule {

    @Provides
    @FeatureScope
    internal fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
