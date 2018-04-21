package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.RecordingRepository;

import java.util.List;

public class ChannelViewModel extends AndroidViewModel {

    private final RecordingRepository recordingRepository;
    private final ChannelAndProgramRepository channelRepository;
    private final ConfigRepository configRepository;
    private LiveData<List<Channel>> channels;
    private LiveData<List<Recording>> recordings;
    private LiveData<ServerStatus> serverStatus;

    public ChannelViewModel(Application application) {
        super(application);
        channelRepository = new ChannelAndProgramRepository(application);
        configRepository = new ConfigRepository(application);
        recordingRepository = new RecordingRepository(application);
    }

    LiveData<List<Recording>> getAllRecordings() {
        if (recordings == null) {
            recordings = new MutableLiveData<>();
            recordings = recordingRepository.getAllRecordings();
        }
        return recordings;
    }

    public LiveData<ServerStatus> getServerStatus() {
        if (serverStatus == null) {
            serverStatus = new MutableLiveData<>();
            serverStatus = configRepository.loadServerStatus();
        }
        return serverStatus;
    }

    List<Channel> getAllChannelsByTimeAndTagSync(long currentTime, int channelTagId) {
        return channelRepository.getAllChannelsByTimeAndTagSync(currentTime, channelTagId);
    }

    Program getProgramByIdSync(int eventId) {
        return channelRepository.getProgramByIdSync(eventId);
    }

    Recording getRecordingByEventIdSync(int eventId) {
        return recordingRepository.getRecordingByEventIdSync(eventId);
    }

    ChannelTag getChannelTagByIdSync(int channelTagId) {
        return channelRepository.getChannelTagByIdSync(channelTagId);
    }

    public LiveData<Integer> getNumberOfChannels() {
        return channelRepository.getNumberOfChannels();
    }
}
