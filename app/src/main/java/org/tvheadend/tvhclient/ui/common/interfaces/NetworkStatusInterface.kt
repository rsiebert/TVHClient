package org.tvheadend.tvhclient.ui.common.interfaces

import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.ui.common.NetworkStatus

interface NetworkStatusInterface {
    fun setNetworkStatus(status: NetworkStatus)
    fun getNetworkStatus(): LiveData<NetworkStatus>
}