package org.tvheadend.tvhclient.features.shared.callbacks;

public interface ChannelSortOrderSelectionCallback {

    /**
     * Called when the user has chosen a new channel sort order from the list.
     *
     * @param id The id of the new sort order
     */
    void onChannelSortOrderSelected(int id);
}
