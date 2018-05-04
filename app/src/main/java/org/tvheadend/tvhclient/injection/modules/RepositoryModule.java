package org.tvheadend.tvhclient.injection.modules;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.local.db.AppRoomDatabase;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.repository.ChannelData;
import org.tvheadend.tvhclient.data.repository.ChannelTagData;
import org.tvheadend.tvhclient.data.repository.ConnectionData;
import org.tvheadend.tvhclient.data.repository.ProgramData;
import org.tvheadend.tvhclient.data.repository.RecordingData;
import org.tvheadend.tvhclient.data.repository.SeriesRecordingData;
import org.tvheadend.tvhclient.data.repository.TimerRecordingData;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    private final AppRoomDatabase appRoomDatabase;

    public RepositoryModule(Context context) {
        appRoomDatabase = Room.databaseBuilder(context, AppRoomDatabase.class, "tvhclient").build();
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
                new ChannelTagData(db));
    }
}
