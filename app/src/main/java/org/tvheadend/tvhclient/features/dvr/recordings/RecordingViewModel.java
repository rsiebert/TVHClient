package org.tvheadend.tvhclient.features.dvr.recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;

import java.util.List;

public class RecordingViewModel extends AndroidViewModel {

    private final RecordingRepository repository;
    private LiveData<List<Recording>> completedRecordings = new MutableLiveData<>();
    private LiveData<List<Recording>> scheduledRecordings = new MutableLiveData<>();
    private LiveData<List<Recording>> failedRecordings = new MutableLiveData<>();
    private LiveData<List<Recording>> removedRecordings = new MutableLiveData<>();
    private Recording recording;

    public RecordingViewModel(Application application) {
        super(application);
        repository = new RecordingRepository(application);
        completedRecordings = repository.getAllCompletedRecordings();
        scheduledRecordings = repository.getAllScheduledRecordings();
        failedRecordings = repository.getAllFailedRecordings();
        removedRecordings = repository.getAllRemovedRecordings();
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

    LiveData<Recording> getRecordingById(int id) {
        return repository.getRecordingById(id);
    }

    public Recording getRecordingByIdSync(int dvrId) {
        if (recording == null) {
            if (dvrId > 0) {
                recording = repository.getRecordingByIdSync(dvrId);
            } else {
                recording = new Recording();
            }
        }
        return recording;
    }

    public LiveData<Integer> getNumberOfCompletedRecordings() {
        return repository.getNumberOfCompletedRecordings();
    }

    public LiveData<Integer> getNumberOfScheduledRecordings() {
        return repository.getNumberOfScheduledRecordings();
    }

    public LiveData<Integer> getNumberOfFailedRecordings() {
        return repository.getNumberOfFailedRecordings();
    }

    public LiveData<Integer> getNumberOfRemovedRecordings() {
        return repository.getNumberOfRemovedRecordings();
    }
}
