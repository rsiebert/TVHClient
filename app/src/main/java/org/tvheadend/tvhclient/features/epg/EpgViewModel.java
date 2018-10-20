package org.tvheadend.tvhclient.features.epg;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import org.tvheadend.tvhclient.data.entity.ChannelSubset;
import org.tvheadend.tvhclient.data.entity.Program;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.features.channels.ChannelViewModel;

import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class EpgViewModel extends ChannelViewModel {

    private MutableLiveData<List<ChannelSubset>> channels;

    private int verticalOffset = 0;
    private int verticalPosition = 0;
    private boolean showGenreColors;

    public EpgViewModel(Application application) {
        super(application);

        showGenreColors = sharedPreferences.getBoolean("genre_colors_for_program_guide_enabled", false);

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

    @NonNull
    public MutableLiveData<List<ChannelSubset>> getChannelSubsets() {
        if (channels == null) {
            channels = new MutableLiveData<>();
            channelUpdateHandler.post(channelUpdateTask);
        }
        return channels;
    }

    LiveData<List<Recording>> getRecordingsByChannel(int channelId) {
        return appRepository.getRecordingData().getLiveDataItemsByChannelId(channelId);
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
        super.checkAndUpdateChannels();

        boolean updateChannels = false;

        boolean newShowGenreColors = sharedPreferences.getBoolean("genre_colors_for_program_guide_enabled", false);
        if (showGenreColors != newShowGenreColors) {
            Timber.d("Epg genre color has changed from " + showGenreColors + " to " + newShowGenreColors);
            showGenreColors = newShowGenreColors;
            updateChannels = true;
        }
        if (updateChannels) {
            channelUpdateHandler.post(channelUpdateTask);
        }
    }
}
