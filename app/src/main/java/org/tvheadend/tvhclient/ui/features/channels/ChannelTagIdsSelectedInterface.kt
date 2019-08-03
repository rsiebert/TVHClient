package org.tvheadend.tvhclient.ui.features.channels

interface ChannelTagIdsSelectedInterface {

    /**
     * Called when the user has chosen one or more channel tags from the list.
     *
     * @param ids The ids of the selected channel tags
     */
    fun onChannelTagIdsSelected(ids: Set<Int>)
}
