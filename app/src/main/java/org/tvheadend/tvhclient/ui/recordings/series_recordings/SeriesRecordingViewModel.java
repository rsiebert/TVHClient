package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.model.SeriesRecording;
import org.tvheadend.tvhclient.data.DataRepository;

import java.util.List;

public class SeriesRecordingViewModel extends AndroidViewModel {

    private final DataRepository repository;
    private LiveData<List<SeriesRecording>> recordings;

    public SeriesRecordingViewModel(Application application) {
        super(application);
        repository = DataRepository.getInstance(application);
        recordings = repository.getSeriesRecordings();
    }

    public LiveData<List<SeriesRecording>> getRecordings() {
        return recordings;
    }

    public LiveData<SeriesRecording> getRecording(String id) {
        return repository.getSeriesRecording(id);
    }

    public SeriesRecording getRecordingSync(String id) {
        return repository.getSeriesRecordingFromDatabase(id);
    }
}
