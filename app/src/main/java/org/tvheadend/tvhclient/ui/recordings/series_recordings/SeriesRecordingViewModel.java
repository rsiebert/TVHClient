package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.TextUtils;

import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;

import java.util.List;

public class SeriesRecordingViewModel extends AndroidViewModel {

    private final RecordingRepository repository;
    private LiveData<List<SeriesRecording>> recordings = new MutableLiveData<>();
    private SeriesRecording recording;

    public SeriesRecordingViewModel(Application application) {
        super(application);
        repository = new RecordingRepository(application);
        recordings = repository.getAllSeriesRecordings();
    }

    public LiveData<List<SeriesRecording>> getRecordings() {
        return recordings;
    }

    LiveData<SeriesRecording> getRecordingById(String id) {
        return repository.getSeriesRecordingById(id);
    }

    public SeriesRecording getRecordingByIdSync(String id) {
        if (recording == null) {
            if (!TextUtils.isEmpty(id)) {
                recording = repository.getSeriesRecordingByIdSync(id);
            } else {
                recording = new SeriesRecording();
            }
        }
        return recording;
    }
}
