package org.tvheadend.tvhclient.features.dvr.timer_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.text.TextUtils;

import org.tvheadend.tvhclient.data.entity.TimerRecording;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;

import java.util.List;

public class TimerRecordingViewModel extends AndroidViewModel {

    private final RecordingRepository repository;
    private LiveData<List<TimerRecording>> recordings;
    private TimerRecording recording;

    public TimerRecordingViewModel(Application application) {
        super(application);
        repository = new RecordingRepository(application);
        recordings = repository.getAllTimerRecordings();
    }

    public LiveData<List<TimerRecording>> getRecordings() {
        return recordings;
    }

    LiveData<TimerRecording> getRecordingById(String id) {
        return repository.getTimerRecordingById(id);
    }

    TimerRecording getRecordingByIdSync(String id) {
        if (recording == null) {
            if (!TextUtils.isEmpty(id)) {
                recording = repository.getTimerRecordingByIdSync(id);
            } else {
                recording = new TimerRecording();
            }
        }
        return recording;
    }

    public LiveData<Integer> getNumberOfRecordings() {
        return repository.getNumberOfTimerRecordings();
    }
}
