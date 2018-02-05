package org.tvheadend.tvhclient.ui.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.repository.ChannelAndProgramRepository;

import java.util.Date;
import java.util.List;

public class ChannelViewModel extends AndroidViewModel {

    private final LiveData<List<Channel>> channels;
    private final ChannelAndProgramRepository repository;
    private MutableLiveData<List<Channel>> channelsByTime = new MutableLiveData<>();
    private long showProgramsFromTime;

    public ChannelViewModel(Application application) {
        super(application);
        repository = new ChannelAndProgramRepository(application);
        showProgramsFromTime = new Date().getTime();

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
        List<Channel> newChannels = repository.getAllChannelsByTimeSync(showProgramsFromTime);
        channelsByTime.postValue(newChannels);
    }
}
