package org.tvheadend.tvhclient.ui.common.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.ui.common.callbacks.NetworkStatusListener

import java.lang.ref.WeakReference

class NetworkStatusReceiver(callback: NetworkStatusListener) : BroadcastReceiver() {

    private val callback: WeakReference<NetworkStatusListener> = WeakReference(callback)

    override fun onReceive(context: Context, intent: Intent) {
        if (MainApplication.isActivityVisible()) {
            (callback.get() as NetworkStatusListener).onNetworkStatusChanged(isConnectionAvailable(context))
        }
    }
}
