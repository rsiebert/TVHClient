package org.tvheadend.tvhclient.features.programs;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;

import java.util.List;

import timber.log.Timber;

public class ProgramViewModel extends AndroidViewModel {

    private final ChannelAndProgramRepository channelAndProgramRepository;
    private final RecordingRepository recordingRepository;

    public ProgramViewModel(Application application) {
        super(application);
        channelAndProgramRepository = new ChannelAndProgramRepository(application);
        recordingRepository = new RecordingRepository(application);
    }

    LiveData<List<Program>> getProgramsByChannelFromTime(int channelId, long time) {
        return channelAndProgramRepository.getProgramsByChannelFromTime(channelId, time);
    }

    Program getProgramByIdSync(int eventId) {
        Timber.d("getProgramByIdSync() called with: eventId = [" + eventId + "]");
        return channelAndProgramRepository.getProgramByIdSync(eventId);
    }

    LiveData<List<Recording>> getRecordingsByChannelId(int channelId) {
        return recordingRepository.getAllRecordingsByChannelId(channelId);
    }

    Recording getRecordingsById(int dvrId) {
        return recordingRepository.getRecordingByIdSync(dvrId);
    }
}
