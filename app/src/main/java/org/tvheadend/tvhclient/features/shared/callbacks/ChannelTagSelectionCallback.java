package org.tvheadend.tvhclient.features.shared.callbacks;

public interface ChannelTagSelectionCallback {

    /**
     * Called when the user has chosen a channel tag from the list.
     *
     * @param id The id of the selected channel tag
     */
    void onChannelTagIdSelected(int id);

    /**
     * Called when the user has chosen one or more channel tags from the list.
     *
     * @param ids The ids of the selected channel tags
     */
    void onMultipleChannelTagIdsSelected(Integer[] ids);
}
