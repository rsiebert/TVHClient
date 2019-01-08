package org.tvheadend.tvhclient.features.channels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;

import org.tvheadend.tvhclient.utils.Triple;

import java.util.List;

class ChannelLiveData extends MediatorLiveData<Triple<Long, Integer, List<Integer>>> {

    ChannelLiveData(LiveData<Long> selectedTime,
                    LiveData<Integer> selectedChannelSortOrder,
                    LiveData<List<Integer>> selectedChannelTagIds) {

        addSource(selectedTime, time ->
                setValue(Triple.create(time, selectedChannelSortOrder.getValue(), selectedChannelTagIds.getValue()))
        );
        addSource(selectedChannelSortOrder, order ->
                setValue(Triple.create(selectedTime.getValue(), order, selectedChannelTagIds.getValue()))
        );
        addSource(selectedChannelTagIds, integers ->
                setValue(Triple.create(selectedTime.getValue(), selectedChannelSortOrder.getValue(), integers))
        );
    }
}
