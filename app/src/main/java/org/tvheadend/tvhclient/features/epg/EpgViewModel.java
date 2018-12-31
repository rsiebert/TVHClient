package org.tvheadend.tvhclient.features.epg;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.EpgChannel;
import org.tvheadend.tvhclient.data.entity.EpgProgram;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.shared.models.BaseChannelViewModel;

import java.util.Date;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class EpgViewModel extends BaseChannelViewModel {

    private MutableLiveData<List<EpgChannel>> channels;
    private Runnable epgChannelUpdateTask;
    private final Handler epgChannelUpdateHandler = new Handler();

    private int verticalOffset = 0;
    private int verticalPosition = 0;

    public EpgViewModel(Application application) {
        super(application);

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        epgChannelUpdateTask = () -> {
            long currentTime = new Date().getTime();
            if (selectedTime < currentTime) {
                selectedTime = currentTime;
            }
            Timber.d("Loading channels from epg update task");
            channels.setValue(appRepository.getChannelData().getChannelNamesByTag(channelTagIds));
            Timber.d("Loaded channels from epg update task");
        };
    }

    @NonNull
    MutableLiveData<List<EpgChannel>> getChannelSubsets() {
        if (channels == null) {
            channels = new MutableLiveData<>();
            epgChannelUpdateHandler.post(epgChannelUpdateTask);
        }
        return channels;
    }

    LiveData<List<Recording>> getRecordingsByChannel(int channelId) {
        return appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
    }

    List<EpgProgram> getProgramsByChannelAndBetweenTimeSync(int channelId, long startTime, long endTime) {
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

    @Override
    protected void setSelectedTime(long selectedTime) {
        super.setSelectedTime(selectedTime);
        epgChannelUpdateHandler.post(epgChannelUpdateTask);
    }

    @Override
    public void setChannelTagIds(Set<Integer> channelTagIds) {
        super.setChannelTagIds(channelTagIds);
        epgChannelUpdateHandler.post(epgChannelUpdateTask);
    }

    void checkAndUpdateChannels() {
        Timber.d("Checking if channels need to be updated");
        if (isUpdateOfChannelsRequired()) {
            epgChannelUpdateHandler.post(epgChannelUpdateTask);
        }
        Timber.d("Done checking if channels need to be updated");
    }
}
