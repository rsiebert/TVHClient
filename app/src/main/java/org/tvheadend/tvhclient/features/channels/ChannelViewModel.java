package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.Channel;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

// TODO move some stuff to the repo
// TODO check if sort order setting has changed
// TODO add comments

public class ChannelViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private final String allChannelsSelectedText;
    private final String multipleChannelTagsSelectedText;
    private final String unknownChannelTagText;

    private LiveData<List<Channel>> channels;
    private LiveData<List<ChannelTag>> channelTags;
    private LiveData<Integer> channelCount;
    private LiveData<List<Recording>> recordings;
    private LiveData<ServerStatus> serverStatus;

    private MutableLiveData<Long> selectedTime = new MutableLiveData<>();
    private MutableLiveData<Integer> channelSortOrder = new MutableLiveData<>();
    private MutableLiveData<Set<Integer>> selectedChannelTagIds = new MutableLiveData<>();

    public ChannelViewModel(Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        allChannelsSelectedText = application.getString(R.string.all_channels);
        multipleChannelTagsSelectedText = application.getString(R.string.multiple_channel_tags);
        unknownChannelTagText = application.getString(R.string.unknown);

        serverStatus = appRepository.getServerStatusData().getLiveDataActiveItem();
        channelTags = appRepository.getChannelTagData().getLiveDataItems();
        recordings = appRepository.getRecordingData().getLiveDataItems();
        channelCount = appRepository.getChannelData().getLiveDataItemCount();

        new Thread(() -> {
            Timber.d("Loading time, sort order and channel tags ids from database");
            selectedTime.postValue(new Date().getTime());
            channelSortOrder.postValue(Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0")));
            selectedChannelTagIds.postValue(appRepository.getChannelTagData().getSelectedChannelTagIds());
        }).start();

        ChannelLiveData trigger = new ChannelLiveData(selectedTime, channelSortOrder, selectedChannelTagIds);
        channels = Transformations.switchMap(trigger, value -> {
            Timber.d("Loading channels due to trigger changes");
            if (value.first == null || value.second == null || value.third == null) {
                Timber.d("At least one required trigger is null, skipping loading");
                return null;
            }
            Timber.d("Loading channels from repository");
            return appRepository.getChannelData().getAllChannelsByTime(value.first, value.second, value.third);
        });
    }

    public LiveData<List<Channel>> getChannels() {
        return channels;
    }

    void setSelectedChannelTagIds(Set<Integer> ids) {
        Timber.d("Saving newly selected channel tag ids");
        selectedChannelTagIds.setValue(ids);

        if (channelTags.getValue() != null) {
            if (!Arrays.equals(channelTags.getValue().toArray(), ids.toArray())) {
                Timber.d("Updating database with newly selected channel tag ids");
                appRepository.getChannelTagData().updateSelectedChannelTags(ids);
            }
        }
    }

    LiveData<List<ChannelTag>> getChannelTags() {
        return channelTags;
    }

    public LiveData<ServerStatus> getServerStatus() {
        return serverStatus;
    }

    public LiveData<Integer> getNumberOfChannels() {
        return channelCount;
    }

    LiveData<Long> getSelectedTime() {
        return selectedTime;
    }

    void setSelectedTime(long time) {
        if (selectedTime.getValue() != null && selectedTime.getValue() != time) {
            Timber.d("Saving newly selected time");
            selectedTime.setValue(time);
        }
    }

    LiveData<List<Recording>> getAllRecordings() {
        return recordings;
    }

    String getSelectedChannelTagName() {
        if (selectedChannelTagIds.getValue() == null || channelTags.getValue() == null) {
            Timber.d("No channel tags or selected tag id values exist");
            return unknownChannelTagText;
        }

        Timber.d("Returning name of the selected channel tag");
        final Set<Integer> selectedTagIds = selectedChannelTagIds.getValue();
        if (selectedTagIds.size() == 1) {
            for (ChannelTag tag : channelTags.getValue()) {
                if (selectedTagIds.contains(tag.getTagId())) {
                    return tag.getTagName();
                }
            }
            return unknownChannelTagText;
        } else if (selectedTagIds.size() == 0) {
            return allChannelsSelectedText;
        } else {
            return multipleChannelTagsSelectedText;
        }
    }
}
