package org.tvheadend.tvhclient.ui.recordings.timer_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.model.TimerRecording;
import org.tvheadend.tvhclient.data.DataRepository;

import java.util.List;

public class TimerRecordingViewModel extends AndroidViewModel {

    private final DataRepository repository;
    private LiveData<List<TimerRecording>> recordings;

    public TimerRecordingViewModel(Application application) {
        super(application);
        repository = DataRepository.getInstance(application);
        recordings = repository.getTimerRecordings();
    }

    public LiveData<List<TimerRecording>> getRecordings() {
        return recordings;
    }

    public LiveData<TimerRecording> getRecording(String id) {
        return repository.getTimerRecording(id);
    }

    public TimerRecording getRecordingSync(String id) {
        return repository.getTimerRecordingFromDatabase(id);
    }
}
