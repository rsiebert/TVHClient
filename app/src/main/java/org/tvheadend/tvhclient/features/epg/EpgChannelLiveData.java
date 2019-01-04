package org.tvheadend.tvhclient.features.epg;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.v4.util.Pair;

import java.util.Set;

public class EpgChannelLiveData extends MediatorLiveData<Pair<Integer, Set<Integer>>> {

    public EpgChannelLiveData(LiveData<Integer> selectedChannelSortOrder,
                              LiveData<Set<Integer>> selectedChannelTagIds) {

        addSource(selectedChannelSortOrder, order ->
                setValue(Pair.create(order, selectedChannelTagIds.getValue()))
        );
        addSource(selectedChannelTagIds, integers ->
                setValue(Pair.create(selectedChannelSortOrder.getValue(), integers))
        );
    }
}
