package org.tvheadend.tvhclient.injection;

import android.content.Context;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.EpgSyncHandler;
import org.tvheadend.tvhclient.data.service.EpgSyncIntentService;
import org.tvheadend.tvhclient.data.service.EpgSyncService;
import org.tvheadend.tvhclient.data.service.EpgSyncTask;
import org.tvheadend.tvhclient.features.MainActivity;
import org.tvheadend.tvhclient.features.channels.ChannelViewModel;
import org.tvheadend.tvhclient.features.download.DownloadRecordingManager;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.features.epg.EpgViewModel;
import org.tvheadend.tvhclient.features.epg.EpgViewPagerFragment;
import org.tvheadend.tvhclient.features.streaming.external.BasePlaybackActivity;
import org.tvheadend.tvhclient.features.streaming.internal.HtspPlaybackActivity;
import org.tvheadend.tvhclient.features.programs.ProgramViewModel;
import org.tvheadend.tvhclient.features.settings.BasePreferenceFragment;
import org.tvheadend.tvhclient.features.settings.ConnectionViewModel;
import org.tvheadend.tvhclient.features.settings.SettingsListConnectionsFragment;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.utils.MenuUtils;
import org.tvheadend.tvhclient.features.startup.StartupFragment;
import org.tvheadend.tvhclient.injection.modules.EpgSyncHandlerModule;
import org.tvheadend.tvhclient.injection.modules.MainApplicationModule;
import org.tvheadend.tvhclient.injection.modules.RepositoryModule;
import org.tvheadend.tvhclient.injection.modules.SharedPreferencesModule;
import org.tvheadend.tvhclient.utils.MigrateUtils;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {
        MainApplicationModule.class,
        SharedPreferencesModule.class,
        RepositoryModule.class,
        EpgSyncHandlerModule.class})
@Singleton
public interface MainApplicationComponent {

    Context context();

    SharedPreferences sharedPreferences();

    AppRepository appRepository();

    EpgSyncHandler epgSyncHandler();

    void inject(MainApplication mainApplication);

    void inject(RecordingViewModel recordingViewModel);

    void inject(BaseFragment baseFragment);

    void inject(BasePreferenceFragment basePreferenceFragment);

    void inject(ProgramViewModel programViewModel);

    void inject(TimerRecordingViewModel timerRecordingViewModel);

    void inject(SeriesRecordingViewModel seriesRecordingViewModel);

    void inject(BasePlaybackActivity basePlayActivity);

    void inject(ChannelViewModel channelViewModel);

    void inject(MenuUtils menuUtils);

    void inject(SettingsListConnectionsFragment settingsListConnectionsFragment);

    void inject(ConnectionViewModel connectionViewModel);

    void inject(EpgSyncService epgSyncService);

    void inject(MigrateUtils migrateUtils);

    void inject(DownloadRecordingManager downloadRecordingManager);

    void inject(EpgSyncTask epgSyncTask);

    void inject(MainActivity mainActivity);

    void inject(StartupFragment startupFragment);

    void inject(EpgViewModel epgViewModel);

    void inject(EpgViewPagerFragment epgViewPagerFragment);

    void inject(EpgSyncIntentService epgSyncIntentService);

    void inject(HtspPlaybackActivity htspPlaybackActivity);
}
