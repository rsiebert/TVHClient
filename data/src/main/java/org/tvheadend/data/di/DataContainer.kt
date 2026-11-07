package org.tvheadend.data.di

import android.content.Context
import org.tvheadend.data.AppRepository
import org.tvheadend.data.db.AppRoomDatabase
import org.tvheadend.data.source.ChannelDataSource
import org.tvheadend.data.source.ChannelTagDataSource
import org.tvheadend.data.source.ConnectionDataSource
import org.tvheadend.data.source.InputDataSource
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.data.source.ProgramDataSource
import org.tvheadend.data.source.RecordingDataSource
import org.tvheadend.data.source.SeriesRecordingDataSource
import org.tvheadend.data.source.ServerProfileDataSource
import org.tvheadend.data.source.ServerStatusDataSource
import org.tvheadend.data.source.SubscriptionDataSource
import org.tvheadend.data.source.TagAndChannelDataSource
import org.tvheadend.data.source.TimerRecordingDataSource

/**
 * Hand rolled replacement for the previous Dagger [org.tvheadend.data.di.RepositoryComponent].
 *
 * The graph it provides is tiny (a Room database and a single repository built from the
 * data sources), so a lazy singleton container is easier to follow and removes the Dagger
 * annotation processor from the build.
 */
class DataContainer(context: Context) {

    private val appContext = context.applicationContext

    val appDatabase: AppRoomDatabase by lazy { AppRoomDatabase.getInstance(appContext)!! }

    val appRepository: AppRepository by lazy {
        AppRepository(
                ChannelDataSource(appDatabase),
                ProgramDataSource(appDatabase),
                RecordingDataSource(appDatabase),
                SeriesRecordingDataSource(appDatabase),
                TimerRecordingDataSource(appDatabase),
                ConnectionDataSource(appDatabase),
                ChannelTagDataSource(appDatabase),
                ServerStatusDataSource(appDatabase),
                ServerProfileDataSource(appDatabase),
                TagAndChannelDataSource(appDatabase),
                MiscDataSource(appDatabase),
                SubscriptionDataSource(appDatabase),
                InputDataSource(appDatabase))
    }
}
