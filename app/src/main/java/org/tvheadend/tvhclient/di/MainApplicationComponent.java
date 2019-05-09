package org.tvheadend.tvhclient.di;

import android.content.Context;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.data.service.HtspIntentService;
import org.tvheadend.tvhclient.data.service.HtspService;
import org.tvheadend.tvhclient.di.modules.MainApplicationModule;
import org.tvheadend.tvhclient.di.modules.RepositoryModule;
import org.tvheadend.tvhclient.di.modules.SharedPreferencesModule;
import org.tvheadend.tvhclient.ui.common.MenuUtils;
import org.tvheadend.tvhclient.ui.features.MainActivity;
import org.tvheadend.tvhclient.ui.features.MainViewModel;
import org.tvheadend.tvhclient.ui.features.channels.BaseChannelViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.ui.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.ui.features.information.StatusViewModel;
import org.tvheadend.tvhclient.ui.features.navigation.NavigationViewModel;
import org.tvheadend.tvhclient.ui.features.playback.external.ExternalPlayerViewModel;
import org.tvheadend.tvhclient.ui.features.playback.internal.PlayerViewModel;
import org.tvheadend.tvhclient.ui.features.programs.ProgramViewModel;
import org.tvheadend.tvhclient.ui.features.settings.SettingsViewModel;
import org.tvheadend.tvhclient.util.MigrateUtils;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = {
        MainApplicationModule.class,
        SharedPreferencesModule.class,
        RepositoryModule.class})
@Singleton
public interface MainApplicationComponent {

    Context context();

    SharedPreferences sharedPreferences();

    AppRepository appRepository();

    void inject(MainApplication mainApplication);

    void inject(MainViewModel mainViewModel);

    void inject(NavigationViewModel navigationViewModel);

    void inject(StatusViewModel statusViewModel);

    void inject(SettingsViewModel settingsViewModel);

    void inject(BaseChannelViewModel baseChannelViewModel);

    void inject(ProgramViewModel programViewModel);

    void inject(RecordingViewModel recordingViewModel);

    void inject(TimerRecordingViewModel timerRecordingViewModel);

    void inject(SeriesRecordingViewModel seriesRecordingViewModel);

    void inject(PlayerViewModel playerViewModel);

    void inject(ExternalPlayerViewModel externalPlayerViewModel);

    void inject(MainActivity mainActivity);

    void inject(MenuUtils menuUtils);

    void inject(MigrateUtils migrateUtils);

    void inject(HtspService htspService);

    void inject(HtspIntentService htspIntentService);
}
