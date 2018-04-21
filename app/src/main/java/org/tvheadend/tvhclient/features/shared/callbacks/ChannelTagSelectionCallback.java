package org.tvheadend.tvhclient.features.shared.callbacks;

public interface ChannelTagSelectionCallback {

    /**
     * Called when the user has chosen a new channel tag from the list.
     *
     * @param id The id of the selected channel tag
     */
    void onChannelTagIdSelected(int id);
}
