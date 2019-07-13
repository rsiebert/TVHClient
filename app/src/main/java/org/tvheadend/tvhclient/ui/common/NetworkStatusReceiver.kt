package org.tvheadend.tvhclient.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tvheadend.tvhclient.data.repository.AppRepository
import timber.log.Timber

class NetworkStatusReceiver(private val appRepository: AppRepository) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isAvailable = isConnectionAvailable(context)
        Timber.d("Network availability is $isAvailable")
        val networkIsAvailable = getNetworkStatus(appRepository.getNetworkStatus().value, isAvailable)
        appRepository.setNetworkStatus(networkIsAvailable)
    }
}
