package org.tvheadend.tvhclient.injection.modules;

import android.content.Context;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.domain.data_sources.ChannelData;
import org.tvheadend.tvhclient.domain.data_sources.ChannelTagData;
import org.tvheadend.tvhclient.domain.data_sources.ConnectionData;
import org.tvheadend.tvhclient.domain.data_sources.MiscData;
import org.tvheadend.tvhclient.domain.data_sources.ProgramData;
import org.tvheadend.tvhclient.domain.data_sources.RecordingData;
import org.tvheadend.tvhclient.domain.data_sources.SeriesRecordingData;
import org.tvheadend.tvhclient.domain.data_sources.ServerProfileData;
import org.tvheadend.tvhclient.domain.data_sources.ServerStatusData;
import org.tvheadend.tvhclient.domain.data_sources.TagAndChannelData;
import org.tvheadend.tvhclient.domain.data_sources.TimerRecordingData;

import javax.inject.Singleton;

import androidx.annotation.NonNull;
import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    private final AppRoomDatabase appRoomDatabase;

    public RepositoryModule(Context context) {
        appRoomDatabase = AppRoomDatabase.Companion.getInstance(context);
    }

    @Singleton
    @NonNull
    @Provides
    AppRoomDatabase providesAppRoomDatabase() {
        return appRoomDatabase;
    }

    @Singleton
    @NonNull
    @Provides
    AppRepository providesAppRepository(AppRoomDatabase db, Context context) {
        return new AppRepository(
                new ChannelData(db),
                new ProgramData(db),
                new RecordingData(db),
                new SeriesRecordingData(db),
                new TimerRecordingData(db),
                new ConnectionData(db),
                new ChannelTagData(db),
                new ServerStatusData(db),
                new ServerProfileData(db),
                new TagAndChannelData(db),
                new MiscData(db));
    }
}
