package org.tvheadend.tvhclient.features.dvr.recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class RecordingViewModel extends AndroidViewModel {

    private Recording recording;

    @Inject
    protected AppRepository appRepository;

    public RecordingViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
    }

    public LiveData<List<Recording>> getCompletedRecordings() {
        return appRepository.getRecordingData().getLiveDataItemsByType("completed");
    }

    public LiveData<List<Recording>> getScheduledRecordings() {
        return appRepository.getRecordingData().getLiveDataItemsByType("scheduled");
    }

    public LiveData<List<Recording>> getFailedRecordings() {
        return appRepository.getRecordingData().getLiveDataItemsByType("failed");
    }

    public LiveData<List<Recording>> getRemovedRecordings() {
        return appRepository.getRecordingData().getLiveDataItemsByType("removed");
    }

    LiveData<Recording> getRecordingById(int id) {
        return appRepository.getRecordingData().getLiveDataItemById(id);
    }

    public Recording getRecordingByIdSync(int dvrId) {
        if (recording == null) {
            if (dvrId > 0) {
                recording = appRepository.getRecordingData().getItemById(dvrId);
            } else {
                recording = new Recording();
            }
        }
        return recording;
    }

    public LiveData<Integer> getNumberOfCompletedRecordings() {
        return appRepository.getRecordingData().getLiveDataCountByType("completed");
    }

    public LiveData<Integer> getNumberOfScheduledRecordings() {
        return appRepository.getRecordingData().getLiveDataCountByType("scheduled");
    }

    public LiveData<Integer> getNumberOfFailedRecordings() {
        return appRepository.getRecordingData().getLiveDataCountByType("failed");
    }

    public LiveData<Integer> getNumberOfRemovedRecordings() {
        return appRepository.getRecordingData().getLiveDataCountByType("removed");
    }
}
