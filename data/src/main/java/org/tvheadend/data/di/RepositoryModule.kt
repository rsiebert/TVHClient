package org.tvheadend.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tvheadend.data.AppRepository
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.source.*
import javax.inject.Singleton

@Module
class RepositoryModule(private val appContext: Context) {

    @Singleton
    @Provides
    fun providesAppRoomDatabase(): AppRoomDatabase {
        return AppRoomDatabase.getInstance(appContext)!!
    }

    @Singleton
    @Provides
    fun providesAppRepository(db: AppRoomDatabase): AppRepository {
        return AppRepository(
                ChannelDataSource(db),
                ProgramDataSource(db),
                RecordingDataSource(db),
                SeriesRecordingDataSource(db),
                TimerRecordingDataSource(db),
                ConnectionDataSource(db),
                ChannelTagDataSource(db),
                ServerStatusDataSource(db),
                ServerProfileDataSource(db),
                TagAndChannelDataSource(db),
                MiscDataSource(db),
                SubscriptionDataSource(db),
                InputDataSource(db))
    }
}
