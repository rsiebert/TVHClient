package org.tvheadend.tvhclient.ui.recordings.recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.data.AppDatabase;
import org.tvheadend.tvhclient.data.dao.RecordingDao;
import org.tvheadend.tvhclient.data.entity.Recording;

import java.util.List;

public class RecordingViewModel extends AndroidViewModel {

    private final RecordingDao dao;
    private LiveData<List<Recording>> completedRecordings = new MutableLiveData<>();
    private LiveData<List<Recording>> scheduledRecordings = new MutableLiveData<>();
    private LiveData<List<Recording>> failedRecordings = new MutableLiveData<>();
    private LiveData<List<Recording>> removedRecordings = new MutableLiveData<>();

    public RecordingViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application.getApplicationContext());
        dao = db.recordingDao();
        completedRecordings = dao.loadAllCompletedRecordings();
        scheduledRecordings = dao.loadAllScheduledRecordings();
        failedRecordings = dao.loadAllFailedRecordings();
        removedRecordings = dao.loadAllRemovedRecordings();
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
        return dao.loadRecording(id);
    }
}
