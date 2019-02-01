package org.tvheadend.tvhclient.features.dvr.recordings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class RecordingViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    private Recording recording;
    private LiveData<List<Recording>> completedRecordings;
    private LiveData<List<Recording>> scheduledRecordings;
    private LiveData<List<Recording>> failedRecordings;
    private LiveData<List<Recording>> removedRecordings;
    private LiveData<Integer> completedRecordingCount;
    private LiveData<Integer> scheduledRecordingCount;
    private LiveData<Integer> failedRecordingCount;
    private LiveData<Integer> removedRecordingCount;

    public RecordingViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        completedRecordings = appRepository.getRecordingData().getLiveDataItemsByType("completed");
        scheduledRecordings = appRepository.getRecordingData().getLiveDataItemsByType("scheduled");
        failedRecordings = appRepository.getRecordingData().getLiveDataItemsByType("failed");
        removedRecordings = appRepository.getRecordingData().getLiveDataItemsByType("removed");

        completedRecordingCount = appRepository.getRecordingData().getLiveDataCountByType("completed");
        scheduledRecordingCount = appRepository.getRecordingData().getLiveDataCountByType("scheduled");
        failedRecordingCount = appRepository.getRecordingData().getLiveDataCountByType("failed");
        removedRecordingCount = appRepository.getRecordingData().getLiveDataCountByType("removed");
    }

    LiveData<List<Recording>> getCompletedRecordings() {
        return completedRecordings;
    }

    public LiveData<List<Recording>> getScheduledRecordings() {
        return scheduledRecordings;
    }

    LiveData<List<Recording>> getFailedRecordings() {
        return failedRecordings;
    }

    LiveData<List<Recording>> getRemovedRecordings() {
        return removedRecordings;
    }

    LiveData<Recording> getRecordingById(int id) {
        return appRepository.getRecordingData().getLiveDataItemById(id);
    }

    public LiveData<Integer> getNumberOfCompletedRecordings() {
        return completedRecordingCount;
    }

    public LiveData<Integer> getNumberOfScheduledRecordings() {
        return scheduledRecordingCount;
    }

    public LiveData<Integer> getNumberOfFailedRecordings() {
        return failedRecordingCount;
    }

    public LiveData<Integer> getNumberOfRemovedRecordings() {
        return removedRecordingCount;
    }

    Recording getRecordingByIdSync(int dvrId) {
        if (recording == null) {
            if (dvrId > 0) {
                recording = appRepository.getRecordingData().getItemById(dvrId);
            } else {
                recording = new Recording();
            }
        }
        return recording;
    }
}
