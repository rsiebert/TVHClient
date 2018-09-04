package org.tvheadend.tvhclient.features.programs;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class ProgramViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;

    public ProgramViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
    }

    LiveData<List<Program>> getProgramsByChannelFromTime(int channelId, long time) {
        return appRepository.getProgramData().getLiveDataItemByChannelIdAndTime(channelId, time);
    }

    Program getProgramByIdSync(int eventId) {
        return appRepository.getProgramData().getItemById(eventId);
    }

    LiveData<List<Recording>> getRecordingsByChannelId(int channelId) {
        return appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
    }
}
