package org.tvheadend.tvhclient.ui.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import org.tvheadend.tvhclient.data.DataRepository;
import org.tvheadend.tvhclient.data.model.Channel;

import java.util.Date;
import java.util.List;

public class ChannelViewModel extends AndroidViewModel {

    private final DataRepository repository;
    private LiveData<List<Channel>> channels;
    private long showProgramsFromTime;

    public ChannelViewModel(Application application) {
        super(application);
        repository = DataRepository.getInstance(application);
        channels = repository.getChannels();
        showProgramsFromTime = new Date().getTime();
    }

    public LiveData<List<Channel>> getChannels() {
        return channels;
    }

    public LiveData<Channel> getChannel(int id) {
        return repository.getChannel(id);
    }

    public Channel getChannelSync(int id) {
        return repository.getChannelFromDatabase(id);
    }

    public void setTime(long time) {
        this.showProgramsFromTime = time;
    }

    public long getTime() {
        return this.showProgramsFromTime;
    }
}
