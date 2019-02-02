package org.tvheadend.tvhclient.features.epg;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import org.tvheadend.tvhclient.data.entity.EpgChannel;
import org.tvheadend.tvhclient.data.entity.EpgProgram;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.channels.BaseChannelViewModel;

import java.util.List;

import timber.log.Timber;

public class EpgViewModel extends BaseChannelViewModel {

    private final LiveData<List<EpgChannel>> epgChannels;

    private int verticalOffset = 0;
    private int verticalPosition = 0;

    public EpgViewModel(Application application) {
        super(application);

        EpgChannelLiveData trigger = new EpgChannelLiveData(channelSortOrder, selectedChannelTagIds);
        epgChannels = Transformations.switchMap(trigger, value -> {
            Timber.d("Loading epg channels because one of the two triggers have changed");

            if (value.first == null) {
                Timber.d("Skipping loading of epg channels because channel sort order is not set");
                return null;
            }
            if (value.second == null) {
                Timber.d("Skipping loading of epg channels because selected channel tag ids are not set");
                return null;
            }

            return appRepository.getChannelData().getAllEpgChannels(value.first, value.second);
        });
    }

    LiveData<List<Recording>> getRecordingsByChannel(int channelId) {
        return appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
    }

    List<EpgProgram> getProgramsByChannelAndBetweenTimeSync(int channelId, long startTime, long endTime) {
        return appRepository.getProgramData().getItemByChannelIdAndBetweenTime(channelId, startTime, endTime);
    }

    LiveData<List<EpgChannel>> getEpgChannels() {
        return epgChannels;
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
