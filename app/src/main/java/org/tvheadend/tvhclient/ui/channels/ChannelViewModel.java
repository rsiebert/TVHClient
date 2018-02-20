package org.tvheadend.tvhclient.ui.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;
import org.tvheadend.tvhclient.data.repository.ServerStatusRepository;

import java.util.Date;
import java.util.List;

public class ChannelViewModel extends AndroidViewModel {

    private final LiveData<List<Channel>> channels;
    private final ChannelAndProgramRepository repository;
    private final ServerStatusRepository serverRepository;
    private final ConfigRepository configRepository;
    private MutableLiveData<List<Channel>> channelsByTime = new MutableLiveData<>();
    private long showProgramsFromTime;
    private int selectedChannelTagId;

    public ChannelViewModel(Application application) {
        super(application);
        repository = new ChannelAndProgramRepository(application);
        serverRepository =  new ServerStatusRepository(application);
        configRepository =  new ConfigRepository(application);

        showProgramsFromTime = new Date().getTime();
        selectedChannelTagId = 0;

        channels = repository.getAllChannels();
        reloadChannels();
    }

    public LiveData<List<Channel>> getChannelsByTime() {
        return channelsByTime;
    }

    public LiveData<List<Channel>> getAllChannels() {
        return channels;
    }

    public void setTime(long time) {
        this.showProgramsFromTime = time;
        reloadChannels();
    }

    public long getTime() {
        return this.showProgramsFromTime;
    }

    private void reloadChannels() {
        List<Channel> newChannels = repository.getAllChannelsByTimeSync(showProgramsFromTime, selectedChannelTagId);
        channelsByTime.postValue(newChannels);
    }

    public void setTag(int id) {
        ChannelTag channelTag = getSelectedChannelTag();
        if (channelTag != null) {
            this.selectedChannelTagId = id;
            reloadChannels();
        }
    }

    public LiveData<ServerStatus> getServerStatus() {
        return serverRepository.loadServerStatus();
    }

    ChannelTag getSelectedChannelTag() {
        return configRepository.getSelectedChannelTag();
    }

    void setSelectedChannelTag(int id) {
        configRepository.setSelectedChannelTag(id);
    }
}
