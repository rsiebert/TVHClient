package org.tvheadend.tvhclient.ui.common.interfaces

interface ChannelTimeSelectedInterface {

    /**
     * Called when a new time was selected from the time selection dialog.
     * The position in the list represented the offset in hours.
     * 0 is the current time, 1 is 1 hour ahead, 2 is 2 hours ahead and so on
     *
     * @param which The position in the list
     */
    fun onTimeSelected(which: Int)
}
