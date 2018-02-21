package org.tvheadend.tvhclient.ui.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ServerStatusRepository;

import java.util.Date;
import java.util.List;

public class ChannelViewModel extends AndroidViewModel {

    private LiveData<List<Channel>> channelsByTimeAndTag;
    private final LiveData<List<Channel>> channels;

    private final ChannelAndProgramRepository channelRepository;
    private final ServerStatusRepository serverRepository;
    private final ConfigRepository configRepository;

    private long currentTime;
    private LiveData<ServerStatus> serverStatus;

    public ChannelViewModel(Application application) {
        super(application);

        channelRepository = new ChannelAndProgramRepository(application);
        serverRepository = new ServerStatusRepository(application);
        configRepository = new ConfigRepository(application);

        currentTime = new Date().getTime();

        channels = channelRepository.getAllChannels();
        channelsByTimeAndTag = channelRepository.getAllChannelsByTimeAndTag();
        serverStatus = serverRepository.loadServerStatus();
    }

    LiveData<List<Channel>> getAllChannelsByTime() {
        return channelsByTimeAndTag;
    }

    public LiveData<List<Channel>> getAllChannels() {
        return channels;
    }

    void setCurrentTime(long time) {
        channelRepository.updateChannelTime(time);
    }

    long getCurrentTime() {
        return currentTime;
    }

    public LiveData<ServerStatus> getServerStatus() {
        return serverStatus;
    }

    ChannelTag getSelectedChannelTag() {
        return configRepository.getSelectedChannelTag();
    }

    void setSelectedChannelTag(int id) {
        configRepository.setSelectedChannelTag(id);
    }
}
