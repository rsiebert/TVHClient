package org.tvheadend.tvhclient.ui.features.programs;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.domain.entity.Program;
import org.tvheadend.tvhclient.domain.entity.Recording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class ProgramViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    private LiveData<Integer> programCount;
    private LiveData<List<Recording>> recordings;

    public ProgramViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
        recordings = appRepository.getRecordingData().getLiveDataItems();
        programCount = appRepository.getProgramData().getLiveDataItemCount();
    }

    LiveData<List<Program>> getProgramsByChannelFromTime(int channelId, long time) {
        return appRepository.getProgramData().getLiveDataItemByChannelIdAndTime(channelId, time);
    }

    LiveData<List<Program>> getProgramsFromTime(long time) {
        return appRepository.getProgramData().getLiveDataItemsFromTime(time);
    }

    Program getProgramByIdSync(int eventId) {
        return appRepository.getProgramData().getItemById(eventId);
    }

    LiveData<List<Recording>> getRecordingsByChannelId(int channelId) {
        return appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
    }

    LiveData<List<Recording>> getRecordings() {
        return recordings;
    }

    public LiveData<Integer> getNumberOfPrograms() {
        return programCount;
    }
}
