package org.tvheadend.tvhclient.features.epg;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.core.util.Pair;

import java.util.List;

class EpgChannelLiveData extends MediatorLiveData<Pair<Integer, List<Integer>>> {

    EpgChannelLiveData(LiveData<Integer> selectedChannelSortOrder,
                              LiveData<List<Integer>> selectedChannelTagIds) {

        addSource(selectedChannelSortOrder, order ->
                setValue(Pair.create(order, selectedChannelTagIds.getValue()))
        );
        addSource(selectedChannelTagIds, integers ->
                setValue(Pair.create(selectedChannelSortOrder.getValue(), integers))
        );
    }
}
