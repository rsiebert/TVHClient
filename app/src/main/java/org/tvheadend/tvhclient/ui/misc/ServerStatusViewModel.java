package org.tvheadend.tvhclient.ui.misc;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;

public class ServerStatusViewModel extends AndroidViewModel {

    private LiveData<ServerStatus> serverStatus;
    private ConfigRepository configRepository;

    public ServerStatusViewModel(@NonNull Application application) {
        super(application);
        configRepository = new ConfigRepository(application);
        serverStatus = configRepository.loadServerStatus();
    }

    public LiveData<ServerStatus> getServerStatus() {
        return serverStatus;
    }

    public ChannelTag getSelectedChannelTag() {
        return configRepository.getSelectedChannelTag();
    }

    public void setSelectedChannelTag(int id) {
        configRepository.setSelectedChannelTag(id);
    }
}
