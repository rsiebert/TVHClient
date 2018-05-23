package org.tvheadend.tvhclient.injection.modules;

import android.content.Context;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.source.ChannelData;
import org.tvheadend.tvhclient.data.source.ChannelTagData;
import org.tvheadend.tvhclient.data.source.ConnectionData;
import org.tvheadend.tvhclient.data.source.MiscData;
import org.tvheadend.tvhclient.data.source.ProgramData;
import org.tvheadend.tvhclient.data.source.RecordingData;
import org.tvheadend.tvhclient.data.source.SeriesRecordingData;
import org.tvheadend.tvhclient.data.source.ServerProfileData;
import org.tvheadend.tvhclient.data.source.ServerStatusData;
import org.tvheadend.tvhclient.data.source.TagAndChannelData;
import org.tvheadend.tvhclient.data.source.TimerRecordingData;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    private final AppRoomDatabase appRoomDatabase;

    public RepositoryModule(Context context) {
        appRoomDatabase = AppRoomDatabase.getInstance(context);
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
                new ChannelData(db, context),
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
