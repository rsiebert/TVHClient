package org.tvheadend.tvhclient.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.tvheadend.tvhclient.data.db.AppRoomDatabase
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.repository.data_source.*
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
                ChannelData(db),
                ProgramData(db),
                RecordingData(db),
                SeriesRecordingData(db),
                TimerRecordingData(db),
                ConnectionData(db),
                ChannelTagData(db),
                ServerStatusData(db),
                ServerProfileData(db),
                TagAndChannelData(db),
                MiscData(db),
                SubscriptionData(db),
                InputData(db))
    }
}
