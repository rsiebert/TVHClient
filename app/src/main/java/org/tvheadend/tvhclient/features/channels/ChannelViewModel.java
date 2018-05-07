package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.List;

import javax.inject.Inject;

public class ChannelViewModel extends AndroidViewModel {

    private LiveData<List<Recording>> recordings;
    private LiveData<ServerStatus> serverStatus;
    @Inject
    protected AppRepository appRepository;

    public ChannelViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);
    }

    LiveData<List<Recording>> getAllRecordings() {
        if (recordings == null) {
            recordings = new MutableLiveData<>();
            recordings = appRepository.getRecordingData().getLiveDataItems();
        }
        return recordings;
    }

    public LiveData<ServerStatus> getServerStatus() {
        if (serverStatus == null) {
            serverStatus = new MutableLiveData<>();
            Connection connection = appRepository.getConnectionData().getActiveItem();
            serverStatus = appRepository.getServerStatusData().getLiveDataItemById(connection.getId());
        }
        return serverStatus;
    }

    ChannelTag getChannelTagByIdSync(int channelTagId) {
        return appRepository.getChannelTagData().getItemById(channelTagId);
    }

    public LiveData<Integer> getNumberOfChannels() {
        return appRepository.getChannelData().getLiveDataItemCount();
    }
}
