package org.tvheadend.tvhclient.features.channels;

import android.app.Application;
import android.content.SharedPreferences;

import org.tvheadend.tvhclient.MainApplication;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.data.entity.ChannelTag;
import org.tvheadend.tvhclient.data.entity.Recording;
import org.tvheadend.tvhclient.data.entity.ServerStatus;
import org.tvheadend.tvhclient.data.repository.AppRepository;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import timber.log.Timber;

public class BaseChannelViewModel extends AndroidViewModel {

    @Inject
    protected AppRepository appRepository;
    @Inject
    protected SharedPreferences sharedPreferences;

    private final String allChannelsSelectedText;
    private final String multipleChannelTagsSelectedText;
    private final String unknownChannelTagText;

    private final LiveData<List<ChannelTag>> channelTags;
    protected final LiveData<List<Recording>> recordings;
    protected final LiveData<ServerStatus> serverStatus;
    protected final LiveData<List<Integer>> selectedChannelTagIds;

    final MutableLiveData<Long> selectedTime = new MutableLiveData<>();
    protected final MutableLiveData<Integer> channelSortOrder = new MutableLiveData<>();

    public BaseChannelViewModel(@NonNull Application application) {
        super(application);
        MainApplication.getComponent().inject(this);

        allChannelsSelectedText = application.getString(R.string.all_channels);
        multipleChannelTagsSelectedText = application.getString(R.string.multiple_channel_tags);
        unknownChannelTagText = application.getString(R.string.unknown);

        serverStatus = appRepository.getServerStatusData().getLiveDataActiveItem();
        channelTags = appRepository.getChannelTagData().getLiveDataItems();
        recordings = appRepository.getRecordingData().getLiveDataItems();
        selectedChannelTagIds = appRepository.getChannelTagData().getLiveDataSelectedItemIds();

        Timber.d("Loading time, sort order and channel tags ids from database");
        selectedTime.postValue(new Date().getTime());
        //noinspection ConstantConditions
        channelSortOrder.postValue(Integer.valueOf(sharedPreferences.getString("channel_sort_order", "0")));
    }

    public LiveData<List<ChannelTag>> getChannelTags() {
        return channelTags;
    }

    public LiveData<ServerStatus> getServerStatus() {
        return serverStatus;
    }

    public LiveData<List<Recording>> getAllRecordings() {
        return recordings;
    }

    LiveData<Long> getSelectedTime() {
        return selectedTime;
    }

    public void setSelectedTime(long time) {
        if (selectedTime.getValue() != null && selectedTime.getValue() != time) {
            Timber.d("Saving newly selected time");
            selectedTime.setValue(time);
        }
    }

    public void setChannelSortOrder(int order) {
        if (channelSortOrder.getValue() != null && channelSortOrder.getValue() != order) {
            Timber.d("Saving newly selected channel sort order");
            channelSortOrder.setValue(order);
        }
    }

    public void setSelectedChannelTagIds(Set<Integer> ids) {
        if (channelTags.getValue() != null) {
            if (!Arrays.equals(channelTags.getValue().toArray(), ids.toArray())) {
                Timber.d("Updating database with newly selected channel tag ids");
                appRepository.getChannelTagData().updateSelectedChannelTags(ids);
            }
        }
    }

    public String getSelectedChannelTagName() {
        if (selectedChannelTagIds.getValue() == null || channelTags.getValue() == null) {
            Timber.d("No channel tags or selected tag id values exist");
            return unknownChannelTagText;
        }

        Timber.d("Returning name of the selected channel tag");
        final List<Integer> selectedTagIds = selectedChannelTagIds.getValue();
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
