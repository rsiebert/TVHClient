package org.tvheadend.tvhclient.ui.common.callbacks

interface NetworkStatusListener {

    /**
     * Called by a broadcast receiver when the network status has changed.
     * This method is called in the activity that listens to the broadcast.
     * The activity will then inform any fragments via this methods.
     *
     * @param isAvailable True is network is available, false otherwise
     */
    fun onNetworkStatusChanged(isAvailable: Boolean)
}
