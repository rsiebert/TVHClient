package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

public class ChannelViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Channel>> channels = new MutableLiveData<>();
    private final String allChannelsSelectedText;
    private final String multipleChannelTagsSelectedText;
    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    protected long selectedTime;
    protected Set<Integer> channelTagIds;
    private int channelSortOrder;
    private boolean showGenreColors;
    protected Runnable channelUpdateTask;
    protected final Handler channelUpdateHandler = new Handler();

    public ChannelViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        allChannelsSelectedText = application.getString(R.string.all_channels);
        multipleChannelTagsSelectedText = application.getString(R.string.multiple_channel_tags);

        channelTagIds = appRepository.getChannelTagData().getSelectedChannelTagIds();
        selectedTime = new Date().getTime();
        channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
        showGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);

        // Initiate a timer that will update the view model data every minute
        // so that the progress bars will be displayed correctly
        channelUpdateTask = () -> {
            long currentTime = new Date().getTime();
            if (selectedTime < currentTime) {
                selectedTime = currentTime;
            }
            List<Channel> list = appRepository.getChannelData().getItemsByTimeAndTags(selectedTime, channelTagIds);
            channels.setValue(list);

            channelUpdateHandler.postDelayed(channelUpdateTask, 60000);
        };
        channelUpdateHandler.post(channelUpdateTask);
    }

    public LiveData<List<Recording>> getAllRecordings() {
        return appRepository.getRecordingData().getLiveDataItems();
    }

    public LiveData<ServerStatus> getServerStatus() {
        return appRepository.getServerStatusData().getLiveDataActiveItem();
    }

    public String getSelectedChannelTagName() {
        if (channelTagIds.size() == 1) {
            return appRepository.getChannelTagData().getItemById(channelTagIds.iterator().next()).getTagName();
        } else if (channelTagIds.size() == 0) {
            return allChannelsSelectedText;
        } else {
            return multipleChannelTagsSelectedText;
        }
    }

    public LiveData<Integer> getNumberOfChannels() {
        return appRepository.getChannelData().getLiveDataItemCount();
    }

    @NonNull
    public MutableLiveData<List<Channel>> getChannels() {
        return channels;
    }

    long getSelectedTime() {
        return selectedTime;
    }

    void setSelectedTime(long selectedTime) {
        this.selectedTime = selectedTime;
        channelUpdateHandler.post(channelUpdateTask);
    }

    public Set<Integer> getChannelTagIds() {
        return channelTagIds;
    }

    public void setChannelTagIds(Set<Integer> channelTagIds) {
        this.channelTagIds = channelTagIds;

        List<ChannelTag> channelTags = appRepository.getChannelTagData().getItems();
        for (ChannelTag channelTag : channelTags) {
            channelTag.setIsSelected(0);
            if (channelTagIds.contains(channelTag.getTagId())) {
                channelTag.setIsSelected(1);
            }
            appRepository.getChannelTagData().updateItem(channelTag);
        }
        channelUpdateHandler.post(channelUpdateTask);
    }

    public void checkAndUpdateChannels() {
        Timber.d("Checking if channels need to be updated");
        boolean updateChannels = false;

        Set<Integer> newChannelTagIds = appRepository.getChannelTagData().getSelectedChannelTagIds();
        if (channelTagIds != newChannelTagIds) {
            channelTagIds = newChannelTagIds;
            updateChannels = true;
        }
        int newChannelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0"));
        if (channelSortOrder != newChannelSortOrder) {
            Timber.d("Sort order has changed from " + channelSortOrder + " to " + newChannelSortOrder);
            channelSortOrder = newChannelSortOrder;
            updateChannels = true;
        }
        boolean newShowGenreColors = sharedPreferences.getBoolean("genre_colors_for_channels_enabled", false);
        if (showGenreColors != newShowGenreColors) {
            Timber.d("Channel genre color has changed from " + showGenreColors + " to " + newShowGenreColors);
            showGenreColors = newShowGenreColors;
            updateChannels = true;
        }
        if (updateChannels) {
            channelUpdateHandler.post(channelUpdateTask);
        }
    }
}
