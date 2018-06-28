package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Connection;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class ChannelViewModel extends AndroidViewModel {

    private MutableLiveData<List<Channel>> channels;
    private LiveData<List<Recording>> recordings;
    private LiveData<ServerStatus> serverStatus;
    @Inject
    protected AppRepository appRepository;

    private long selectedTime;
    private int channelTagId;
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();

    public ChannelViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        channelTagId = 0;
        selectedTime = new Date().getTime();

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        channelUpdateTask = new Runnable() {
            public void run() {
                long currentTime = new Date().getTime();
                if (selectedTime < currentTime) {
                    selectedTime = currentTime;
                }
                List<Channel> list = appRepository.getChannelData().getItemsByTimeAndTag(selectedTime, channelTagId);
                channels.setValue(list);

                channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
            }
        };
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

    ChannelTag getChannelTag() {
        return appRepository.getChannelTagData().getItemById(channelTagId);
    }

    public LiveData<Integer> getNumberOfChannels() {
        return appRepository.getChannelData().getLiveDataItemCount();
    }

    @NonNull
    public MutableLiveData<List<Channel>> getChannels() {
        if (channels == null) {
            channels = new MutableLiveData<>();
            channelUpdateHandler.post(channelUpdateTask);
        }
        return channels;
    }

    public long getSelectedTime() {
        return selectedTime;
    }

    public void setSelectedTime(long selectedTime) {
        this.selectedTime = selectedTime;
        channelUpdateHandler.post(channelUpdateTask);
    }

    public int getChannelTagId() {
        return channelTagId;
    }

    public void setChannelTagId(int channelTagId) {
        this.channelTagId = channelTagId;
        channelUpdateHandler.post(channelUpdateTask);
    }
}
