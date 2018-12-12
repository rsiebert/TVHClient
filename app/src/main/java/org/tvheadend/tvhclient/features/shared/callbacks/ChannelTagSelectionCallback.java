package org.tvheadend.tvhclient.features.shared.callbacks;

import java.util.Set;

public interface ChannelTagSelectionCallback {

    /**
     * Called when the user has chosen one or more channel tags from the list.
     *
     * @param ids The ids of the selected channel tags
     */
    void onChannelTagIdsSelected(Set<Integer> ids);
}
