package org.tvheadend.tvhclient.injection;

import android.content.Context;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.repository.AppRepository;
import org.tvheadend.tvhclient.features.MainActivity;
import org.tvheadend.tvhclient.features.casting.BaseCastingActivity;
import org.tvheadend.tvhclient.features.dvr.recordings.RecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.series_recordings.SeriesRecordingViewModel;
import org.tvheadend.tvhclient.features.dvr.timer_recordings.TimerRecordingViewModel;
import org.tvheadend.tvhclient.features.playback.BasePlayActivity;
import org.tvheadend.tvhclient.features.programs.ProgramViewModel;
import org.tvheadend.tvhclient.features.settings.BasePreferenceFragment;
import org.tvheadend.tvhclient.features.shared.BaseFragment;
import org.tvheadend.tvhclient.injection.modules.AppModule;
import org.tvheadend.tvhclient.injection.modules.RepositoryModule;
import org.tvheadend.tvhclient.injection.modules.SharedPreferencesModule;

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

    void inject(BaseCastingActivity baseCastingActivity);

    void inject(BasePlayActivity basePlayActivity);
}
