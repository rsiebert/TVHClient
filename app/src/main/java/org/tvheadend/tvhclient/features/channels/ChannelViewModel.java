package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class ChannelViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Channel>> channels = new MutableLiveData<>();
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private long selectedTime;
    private int channelTagId;
    private int channelSortOrder;
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();

    public ChannelViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        channelTagId = appRepository.getServerStatusData().getActiveItem().getChannelTagId();
        selectedTime = new Date().getTime();
        channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        channelUpdateTask = () -> {
            long currentTime = new Date().getTime();
            if (selectedTime < currentTime) {
                selectedTime = currentTime;
            }
            List<Channel> list = appRepository.getChannelData().getItemsByTimeAndTag(selectedTime, channelTagId);
            channels.setValue(list);

            channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
        };
        channelUpdateHandler.post(channelUpdateTask);
    }

    LiveData<List<Recording>> getAllRecordings() {
        return appRepository.getRecordingData().getLiveDataItems();
    }

    public LiveData<ServerStatus> getServerStatus() {
        return appRepository.getServerStatusData().getLiveDataActiveItem();
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
        ServerStatus serverStatus = appRepository.getServerStatusData().getActiveItem();
        serverStatus.setChannelTagId(channelTagId);
        appRepository.getServerStatusData().updateItem(serverStatus);
        channelUpdateHandler.post(channelUpdateTask);
    }

    public void checkAndUpdateChannels() {
        Timber.d("Checking if channels need to be updated");
        boolean updateChannels = false;

        int newChannelTagId = appRepository.getServerStatusData().getActiveItem().getChannelTagId();
        if (channelTagId != newChannelTagId) {
            Timber.d("Channel tag has changed from " + channelTagId + " to " + newChannelTagId);
            channelTagId = newChannelTagId;
            updateChannels = true;
        }
        int newChannelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
        if (channelSortOrder != newChannelSortOrder) {
            Timber.d("Sort order has changed from " + channelSortOrder + " to " + newChannelSortOrder);
            channelSortOrder = newChannelSortOrder;
            updateChannels = true;
        }
        if (updateChannels) {
            channelUpdateHandler.post(channelUpdateTask);
        }
    }
}
