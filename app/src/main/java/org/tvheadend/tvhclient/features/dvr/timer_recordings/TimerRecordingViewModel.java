package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
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
    private LiveData<List<TimerRecording>> recordings;
    private LiveData<Integer> recordingCount;

    public TimerRecordingViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
        recordings = appRepository.getTimerRecordingData().getLiveDataItems();
        recordingCount = appRepository.getTimerRecordingData().getLiveDataItemCount();
    }

    public LiveData<List<TimerRecording>> getRecordings() {
        return recordings;
    }

    LiveData<TimerRecording> getRecordingById(String id) {
        return appRepository.getTimerRecordingData().getLiveDataItemById(id);
    }

    public LiveData<Integer> getNumberOfRecordings() {
        return recordingCount;
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
}
