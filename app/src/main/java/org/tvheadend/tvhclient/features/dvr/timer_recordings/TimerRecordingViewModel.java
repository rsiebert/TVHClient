package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.text.TextUtils;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class TimerRecordingViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    private TimerRecording recording;

    public TimerRecordingViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
    }

    public LiveData<List<TimerRecording>> getRecordings() {
        return appRepository.getTimerRecordingData().getLiveDataItems();
    }

    LiveData<TimerRecording> getRecordingById(String id) {
        return appRepository.getTimerRecordingData().getLiveDataItemById(id);
    }

    TimerRecording getRecordingByIdSync(String id) {
        if (recording == null) {
            if (!TextUtils.isEmpty(id)) {
                recording = appRepository.getTimerRecordingData().getItemById(id);
            } else {
                recording = new TimerRecording();
            }
        }
        return recording;
    }

    public LiveData<Integer> getNumberOfRecordings() {
        return appRepository.getTimerRecordingData().getLiveDataItemCount();
    }
}
