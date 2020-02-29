package org.tvheadend.tvhclient.ui.common.interfaces

import org.tvheadend.tvhclient.ui.common.NetworkStatus

interface NetworkStatusInterface {
    fun setNetworkStatus(status: NetworkStatus)
    fun getNetworkStatus(): NetworkStatus?
}