package org.tvheadend.tvhclient.ui.recordings.timer_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.TextUtils;

import org.tvheadend.tvhclient.data.repository.RecordingRepository;
import org.tvheadend.tvhclient.data.entity.TimerRecording;

import java.util.List;

public class TimerRecordingViewModel extends AndroidViewModel {

    private final RecordingRepository repository;
    private LiveData<List<TimerRecording>> recordings = new MutableLiveData<>();
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
}
