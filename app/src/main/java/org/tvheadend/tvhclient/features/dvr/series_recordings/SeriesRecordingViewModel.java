package org.tvheadend.tvhclient.features.dvr.series_recordings;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.text.TextUtils;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.SeriesRecording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class SeriesRecordingViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    private LiveData<List<SeriesRecording>> recordings;
    private SeriesRecording recording;

    public SeriesRecordingViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        recordings = appRepository.getSeriesRecordingData().getLiveDataItems();
    }

    public LiveData<List<SeriesRecording>> getRecordings() {
        return recordings;
    }

    LiveData<SeriesRecording> getRecordingById(String id) {
        return appRepository.getSeriesRecordingData().getLiveDataItemById(id);
    }

    public SeriesRecording getRecordingByIdSync(String id) {
        if (recording == null) {
            if (!TextUtils.isEmpty(id)) {
                recording = appRepository.getSeriesRecordingData().getItemById(id);
            } else {
                recording = new SeriesRecording();
            }
        }
        return recording;
    }

    public LiveData<Integer> getNumberOfRecordings() {
        return appRepository.getSeriesRecordingData().getLiveDataItemCount();
    }
}
