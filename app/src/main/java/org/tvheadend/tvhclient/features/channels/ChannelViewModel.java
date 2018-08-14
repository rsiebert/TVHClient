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

    private MutableLiveData<List<Channel>> channels = new MutableLiveData<>();
    private LiveData<ServerStatus> serverStatus = new MutableLiveData<>();
    @Inject
    protected AppRepository appRepository;

    private long selectedTime;
    private int channelTagId;
    private int channelSortOrder;
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();

    public ChannelViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        channelTagId = 0;
        selectedTime = new Date().getTime();
        channelSortOrder = 0;

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
        channelUpdateHandler.post(channelUpdateTask);
    }

    LiveData<List<Recording>> getAllRecordings() {
        return appRepository.getRecordingData().getLiveDataItems();
    }

    public LiveData<ServerStatus> getServerStatus() {
        Connection connection = appRepository.getConnectionData().getActiveItem();
        return appRepository.getServerStatusData().getLiveDataItemById(connection.getId());
    }

    ChannelTag getChannelTag() {
        return appRepository.getChannelTagData().getItemById(channelTagId);
    }

    public LiveData<Integer> getNumberOfChannels() {
        return appRepository.getChannelData().getLiveDataItemCount();
    }

    @NonNull
    public MutableLiveData<List<Channel>> getChannels() {
        return channels;
    }

    public void setChannelSortOrder(int sortOrder) {
        if (channelSortOrder != sortOrder) {
            channelSortOrder = sortOrder;
            channelUpdateHandler.post(channelUpdateTask);
        }
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
