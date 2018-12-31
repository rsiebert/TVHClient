package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.features.shared.models.BaseChannelViewModel;

import java.util.Date;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class ChannelViewModel extends BaseChannelViewModel {

    private final MutableLiveData<List<Channel>> channels = new MutableLiveData<>();
    private Runnable channelUpdateTask;
    private final Handler channelUpdateHandler = new Handler();

    public ChannelViewModel(Application application) {
        super(application);

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        channelUpdateTask = () -> {
            long currentTime = new Date().getTime();
            if (selectedTime < currentTime) {
                selectedTime = currentTime;
            }
            Timber.d("Loading channels from update task");
            List<Channel> list = appRepository.getChannelData().getItemsByTimeAndTags(selectedTime, channelTagIds);
            Timber.d("Loaded channels from update task");
            channels.setValue(list);

            channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
            Timber.d("Done loading channels from update task");
        };
        channelUpdateHandler.post(channelUpdateTask);
    }

    @NonNull
    public MutableLiveData<List<Channel>> getChannels() {
        return channels;
    }

    public LiveData<ServerStatus> getServerStatus() {
        return appRepository.getServerStatusData().getLiveDataActiveItem();
    }

    public LiveData<Integer> getNumberOfChannels() {
        return appRepository.getChannelData().getLiveDataItemCount();
    }

    @Override
    protected void setSelectedTime(long selectedTime) {
        super.setSelectedTime(selectedTime);
        channelUpdateHandler.post(channelUpdateTask);
    }

    @Override
    public void setChannelTagIds(Set<Integer> channelTagIds) {
        super.setChannelTagIds(channelTagIds);
        channelUpdateHandler.post(channelUpdateTask);
    }

    public void checkAndUpdateChannels() {
        Timber.d("Checking if channels need to be updated");
        if (isUpdateOfChannelsRequired()) {
            channelUpdateHandler.post(channelUpdateTask);
        }
        Timber.d("Done checking if channels need to be updated");
    }
}
