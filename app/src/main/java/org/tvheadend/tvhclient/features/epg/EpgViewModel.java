package org.tvheadend.tvhclient.features.epg;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class EpgViewModel extends AndroidViewModel {

    private MutableLiveData<List<ChannelSubset>> channels;
    private LiveData<List<Recording>> recordings;
    @Inject
    protected AppRepository appRepository;

    private long selectedTime;
    private int channelTagId;
    private Runnable channelUpdateTask;
    private final Handler handler = new Handler();
    private int verticalOffset = 0;
    private int verticalPosition = 0;

    public EpgViewModel(Application application) {
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
                channels.setValue(appRepository.getChannelData().getChannelNamesByTimeAndTag(channelTagId));
            }
        };
    }

    ChannelTag getChannelTag() {
        return appRepository.getChannelTagData().getItemById(channelTagId);
    }

    @NonNull
    public MutableLiveData<List<ChannelSubset>> getChannels() {
        if (channels == null) {
            channels = new MutableLiveData<>();
            handler.post(channelUpdateTask);
        }
        return channels;
    }

    LiveData<List<Recording>> getRecordingsByChannel(int channelId) {
        if (recordings == null) {
            recordings = new MutableLiveData<>();
            recordings = appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
        }
        return recordings;
    }

    public int getChannelTagId() {
        return channelTagId;
    }

    public void setChannelTagId(int channelTagId) {
        this.channelTagId = channelTagId;
        handler.post(channelUpdateTask);
    }

    LiveData<List<Program>> getProgramsByChannelAndBetweenTime(int channelId, long startTime, long endTime) {
        if (channelTagId == 0) {
            return appRepository.getProgramData().getLiveDataItemByChannelIdAndBetweenTime(channelId, startTime, endTime);
        } else {
            return appRepository.getProgramData().getLiveDataItemByChannelIdAndByTagAndBetweenTime(channelId, channelTagId, startTime, endTime);
        }
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
}
