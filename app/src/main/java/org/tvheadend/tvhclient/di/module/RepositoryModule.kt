package org.tvheadend.tvhclient.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.repository.AppRepository
import org.tvheadend.data.source.*
import javax.inject.Singleton

@Module
class RepositoryModule {

    @Singleton
    @Provides
    internal fun providesAppRoomDatabase(context: Context): AppRoomDatabase {
        return AppRoomDatabase.getInstance(context)!!
    }

    @Singleton
    @Provides
    internal fun providesAppRepository(db: AppRoomDatabase): AppRepository {
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
