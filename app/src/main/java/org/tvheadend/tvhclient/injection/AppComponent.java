package org.tvheadend.tvhclient.injection;

import android.content.Context;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.features.MainActivity;
import org.tvheadend.tvhclient.features.channels.ChannelViewModel;
import org.tvheadend.tvhclient.features.download.DownloadRecordingManager;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.features.playback.BasePlaybackActivity;
import org.tvheadend.tvhclient.features.programs.ProgramViewModel;
import org.tvheadend.tvhclient.features.settings.BasePreferenceFragment;
import org.tvheadend.tvhclient.features.settings.ConnectionViewModel;
import org.tvheadend.tvhclient.features.settings.SettingsListConnectionsFragment;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.features.shared.MenuUtils;
import org.tvheadend.tvhclient.features.startup.StartupFragment;
import org.tvheadend.tvhclient.injection.modules.AppModule;
import org.tvheadend.tvhclient.injection.modules.RepositoryModule;
import org.tvheadend.tvhclient.injection.modules.SharedPreferencesModule;
import org.tvheadend.tvhclient.utils.MigrateUtils;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {
        AppModule.class,
        SharedPreferencesModule.class,
        RepositoryModule.class,})
@Singleton
public interface AppComponent {
    Context context();

    SharedPreferences sharedPreferences();

    AppRepository appRepository();

    void inject(MainApplication mainApplication);

    void inject(MainActivity mainActivity);

    void inject(RecordingViewModel recordingViewModel);

    void inject(BaseFragment baseFragment);

    void inject(BasePreferenceFragment basePreferenceFragment);

    void inject(ProgramViewModel programViewModel);

    void inject(TimerRecordingViewModel timerRecordingViewModel);

    void inject(SeriesRecordingViewModel seriesRecordingViewModel);

    void inject(BasePlaybackActivity basePlayActivity);

    void inject(StartupFragment startupFragment);

    void inject(ChannelViewModel channelViewModel);

    void inject(MenuUtils menuUtils);

    void inject(SettingsListConnectionsFragment settingsListConnectionsFragment);

    void inject(ConnectionViewModel connectionViewModel);

    void inject(EpgSyncService epgSyncService);

    void inject(MigrateUtils migrateUtils);

    void inject(DownloadRecordingManager downloadRecordingManager);
}
