package org.tvheadend.tvhclient.features.dvr.series_recordings;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.text.TextUtils;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class SeriesRecordingViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    private SeriesRecording recording;
    private LiveData<List<SeriesRecording>> recordings;
    private LiveData<Integer> recordingCount;

    public SeriesRecordingViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
        recordings = appRepository.getSeriesRecordingData().getLiveDataItems();
        recordingCount = appRepository.getSeriesRecordingData().getLiveDataItemCount();
    }

    public LiveData<List<SeriesRecording>> getRecordings() {
        return recordings;
    }

    LiveData<SeriesRecording> getRecordingById(String id) {
        return appRepository.getSeriesRecordingData().getLiveDataItemById(id);
    }

    public LiveData<Integer> getNumberOfRecordings() {
        return recordingCount;
    }

    SeriesRecording getRecordingByIdSync(String id) {
        if (recording == null) {
            if (!TextUtils.isEmpty(id)) {
                recording = appRepository.getSeriesRecordingData().getItemById(id);
            } else {
                recording = new SeriesRecording();
            }
        }
        return recording;
    }
}
