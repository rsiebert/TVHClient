package org.tvheadend.tvhclient.features.epg;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class EpgViewModel extends AndroidViewModel {

    private MutableLiveData<List<ChannelSubset>> channels;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private long selectedTime;
    private int channelTagId;
    private int channelSortOrder;
    private final Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();
    private int verticalOffset = 0;
    private int verticalPosition = 0;

    public EpgViewModel(Application application) {
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
            channels.setValue(appRepository.getChannelData().getChannelNamesByTimeAndTag(channelTagId));
        };
    }

    ChannelTag getChannelTag() {
        return appRepository.getChannelTagData().getItemById(channelTagId);
    }

    @NonNull
    public MutableLiveData<List<ChannelSubset>> getChannels() {
        if (channels == null) {
            channels = new MutableLiveData<>();
            channelUpdateHandler.post(channelUpdateTask);
        }
        return channels;
    }

    LiveData<List<Recording>> getAllRecordings() {
        return appRepository.getRecordingData().getLiveDataItems();
    }

    LiveData<List<Recording>> getRecordingsByChannel(int channelId) {
        return appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
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

    List<Program> getProgramsByChannelAndBetweenTimeSync(int channelId, long startTime, long endTime) {
        return appRepository.getProgramData().getItemByChannelIdAndBetweenTime(channelId, startTime, endTime);
    }

    void setVerticalScrollOffset(int offset) {
        this.verticalOffset = offset;
    }

    int getVerticalScrollOffset() {
        return this.verticalOffset;
    }

    void setVerticalScrollPosition(int position) {
        this.verticalPosition = position;
    }

    int getVerticalScrollPosition() {
        return this.verticalPosition;
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
