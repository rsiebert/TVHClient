package org.tvheadend.tvhclient.di.modules;

import android.content.Context;

import androidx.annotation.NonNull;

import org.tvheadend.tvhclient.data.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.domain.repository.data_source.ChannelData;
import org.tvheadend.tvhclient.domain.repository.data_source.ChannelTagData;
import org.tvheadend.tvhclient.domain.repository.data_source.ConnectionData;
import org.tvheadend.tvhclient.domain.repository.data_source.InputData;
import org.tvheadend.tvhclient.domain.repository.data_source.MiscData;
import org.tvheadend.tvhclient.domain.repository.data_source.ProgramData;
import org.tvheadend.tvhclient.domain.repository.data_source.RecordingData;
import org.tvheadend.tvhclient.domain.repository.data_source.SeriesRecordingData;
import org.tvheadend.tvhclient.domain.repository.data_source.ServerProfileData;
import org.tvheadend.tvhclient.domain.repository.data_source.ServerStatusData;
import org.tvheadend.tvhclient.domain.repository.data_source.SubscriptionData;
import org.tvheadend.tvhclient.domain.repository.data_source.TagAndChannelData;
import org.tvheadend.tvhclient.domain.repository.data_source.TimerRecordingData;

import javax.inject.Singleton;

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
    AppRepository providesAppRepository(AppRoomDatabase db) {
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
                new MiscData(db),
                new SubscriptionData(db),
                new InputData(db));
    }
}
