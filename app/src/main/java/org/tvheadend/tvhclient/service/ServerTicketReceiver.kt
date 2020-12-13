package org.tvheadend.tvhclient.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.lang.ref.WeakReference

class ServerTicketReceiver(callback: Listener) : BroadcastReceiver() {

    private val callback: WeakReference<Listener> = WeakReference(callback)

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "message" extra. Any details about the state is given
     * via the "details" extra.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (callback.get() != null) {
            (callback.get() as Listener).onServerTicketReceived(intent)
        }
    }

    interface Listener {
        fun onServerTicketReceived(intent: Intent)
    }

    companion object {

        const val ACTION = "server_ticket"
        const val PATH = "path"
        const val TICKET = "ticket"
    }
}
