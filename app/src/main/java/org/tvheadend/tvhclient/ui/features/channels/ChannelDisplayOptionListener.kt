package org.tvheadend.tvhclient.ui.features.channels

interface ChannelDisplayOptionListener {

    /**
     * Called when the user has chosen a new channel sort order from the list.
     *
     * @param id The id of the new sort order
     */
    fun onChannelSortOrderSelected(id: Int)

    /**
     * Called when the user has chosen one or more channel tags from the list.
     *
     * @param ids The ids of the selected channel tags
     */
    fun onChannelTagIdsSelected(ids: Set<Int>)

    /**
     * Called when a new time was selected from the time selection dialog.
     * The position in the list represented the offset in hours.
     * 0 is the current time, 1 is 1 hour ahead, 2 is 2 hours ahead and so on
     *
     * @param which The position in the list
     */
    fun onTimeSelected(which: Int)
}
