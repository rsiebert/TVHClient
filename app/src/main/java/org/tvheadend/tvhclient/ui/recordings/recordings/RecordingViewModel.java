package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.DataRepository;
import org.tvheadend.tvhclient.data.model.Recording;

import java.util.List;

public class RecordingViewModel extends AndroidViewModel {

    private final DataRepository repository;
    private LiveData<List<Recording>> completedRecordings;
    private LiveData<List<Recording>> scheduledRecordings;
    private LiveData<List<Recording>> failedRecordings;
    private LiveData<List<Recording>> removedRecordings;

    public RecordingViewModel(Application application) {
        super(application);
        repository = DataRepository.getInstance(application);
        completedRecordings = repository.getCompletedRecordings();
        scheduledRecordings = repository.getScheduledRecordings();
        failedRecordings = repository.getFailedRecordings();
        removedRecordings = repository.getRemovedRecordings();
    }

    public LiveData<List<Recording>> getCompletedRecordings() {
        return completedRecordings;
    }

    public LiveData<List<Recording>> getScheduledRecordings() {
        return scheduledRecordings;
    }

    public LiveData<List<Recording>> getFailedRecordings() {
        return failedRecordings;
    }

    public LiveData<List<Recording>> getRemovedRecordings() {
        return removedRecordings;
    }

    public LiveData<Recording> getRecording(int id) {
        return repository.getRecording(id);
    }
}
