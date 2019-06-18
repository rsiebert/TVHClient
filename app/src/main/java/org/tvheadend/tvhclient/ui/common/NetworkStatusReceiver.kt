package org.tvheadend.tvhclient.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tvheadend.tvhclient.ui.common.getNetworkStatus
import org.tvheadend.tvhclient.ui.common.isConnectionAvailable
import org.tvheadend.tvhclient.ui.features.MainViewModel
import timber.log.Timber

class NetworkStatusReceiver(private val mainViewModel: MainViewModel) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isAvailable = isConnectionAvailable(context)
        Timber.d("Network availability is $isAvailable")
        mainViewModel.networkStatus.value = getNetworkStatus(mainViewModel.networkStatus.value, isAvailable)
    }
}
