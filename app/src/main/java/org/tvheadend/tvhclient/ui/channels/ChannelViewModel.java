package org.tvheadend.tvhclient.ui.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.util.Log;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;
import org.tvheadend.tvhclient.data.repository.ConfigRepository;

import java.util.Date;
import java.util.List;

public class ChannelViewModel extends AndroidViewModel {
    private String TAG = getClass().getSimpleName();

    private final ChannelAndProgramRepository channelRepository;
    private final ConfigRepository configRepository;

    private long currentTime;

    public ChannelViewModel(Application application) {
        super(application);

        channelRepository = new ChannelAndProgramRepository(application);
        configRepository = new ConfigRepository(application);
        currentTime = new Date().getTime();
    }

    LiveData<List<Channel>> getAllChannelsByTime() {
        Log.d(TAG, "getAllChannelsByTime() called");
        return channelRepository.getAllChannelsByTimeAndTag();
    }

    public LiveData<List<Channel>> getAllChannels() {
        Log.d(TAG, "getAllChannels() called");
        return channelRepository.getAllChannels();
    }

    void setCurrentTime(long time) {
        Log.d(TAG, "setCurrentTime() called with: time = [" + time + "]");
        channelRepository.updateChannelTime(time);
    }

    long getCurrentTime() {
        return currentTime;
    }
}
