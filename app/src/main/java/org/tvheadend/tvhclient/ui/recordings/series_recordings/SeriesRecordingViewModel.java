package org.tvheadend.tvhclient.ui.recordings.series_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.data.DataRepository;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;

import java.util.List;

public class SeriesRecordingViewModel extends AndroidViewModel {

    private final DataRepository repository;
    private LiveData<List<SeriesRecording>> recordings = new MutableLiveData<>();

    public SeriesRecordingViewModel(Application application) {
        super(application);
        repository = new DataRepository(application);
        recordings = repository.getAllSeriesRecordings();
    }

    public LiveData<List<SeriesRecording>> getRecordings() {
        return recordings;
    }

    LiveData<SeriesRecording> getRecording(String id) {
        return repository.getSeriesRecording(id);
    }
}
