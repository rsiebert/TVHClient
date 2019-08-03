package org.tvheadend.tvhclient.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.lang.ref.WeakReference

class SyncStateReceiver(callback: Listener) : BroadcastReceiver() {

    private val callback: WeakReference<Listener> = WeakReference(callback)

    enum class State {
        CLOSED,
        CONNECTING,
        CONNECTED,
        SYNC_STARTED,
        SYNC_IN_PROGRESS,
        SYNC_DONE,
        IDLE,
        FAILED
    }

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "message" extra. Any details about the state is given
     * via the "details" extra.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (callback.get() != null) {
            (callback.get() as Listener).onSyncStateChanged(
                    intent.getSerializableExtra(STATE) as State,
                    intent.getStringExtra(MESSAGE) ?: "",
                    intent.getStringExtra(DETAILS) ?: "")
        }
    }

    interface Listener {
        /**
         * Interface method that is called then a local broadcast was received
         * with the connection and synchronization state including any text messages
         *
         * @param state   The current connection and synchronization state
         * @param message Main text message describing the state
         * @param details Additional text message to describe any details
         */
        fun onSyncStateChanged(state: State, message: String, details: String)
    }

    companion object {

        const val ACTION = "service_status"
        const val STATE = "state"
        const val MESSAGE = "message"
        const val DETAILS = "details"
    }
}
