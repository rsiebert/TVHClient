package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.features.shared.BaseChannelViewModel;

import java.util.List;

import timber.log.Timber;

public class ChannelViewModel extends BaseChannelViewModel {

    private final LiveData<List<Channel>> channels;
    private final LiveData<Integer> channelCount;

    public ChannelViewModel(Application application) {
        super(application);

        channelCount = appRepository.getChannelData().getLiveDataItemCount();

        ChannelLiveData trigger = new ChannelLiveData(selectedTime, channelSortOrder, selectedChannelTagIds);
        channels = Transformations.switchMap(trigger, value -> {
            Timber.d("Loading channels because one of the three triggers have changed");

            if (value.first == null) {
                Timber.d("Skipping loading of channels because selected time is not set");
                return null;
            }
            if (value.second == null) {
                Timber.d("Skipping loading of channels because channel sort order is not set");
                return null;
            }
            if (value.third == null) {
                Timber.d("Skipping loading of channels because selected channel tag ids are not set");
                return null;
            }

            return appRepository.getChannelData().getAllChannelsByTime(value.first, value.second, value.third);
        });
    }

    public LiveData<List<Channel>> getChannels() {
        return channels;
    }

    public LiveData<Integer> getNumberOfChannels() {
        return channelCount;
    }
}
