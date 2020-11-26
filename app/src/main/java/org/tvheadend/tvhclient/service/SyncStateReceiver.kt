package org.tvheadend.tvhclient.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tvheadend.htsp.ConnectionStateResult
import java.lang.ref.WeakReference

class SyncStateReceiver(callback: Listener) : BroadcastReceiver() {

    private val callback: WeakReference<Listener> = WeakReference(callback)

    sealed class SyncStateResult {
        data class Initializing(val state: ConnectionStateResult): SyncStateResult()
        data class Syncing(val state: SyncState): SyncStateResult()
        data class Failed(val message: String): SyncStateResult()
    }

    sealed class SyncState {
        data class Started(val message: String) : SyncState()
        data class InProgress(val message: String) : SyncState()
        data class Done(val message: String) : SyncState()
    }

    enum class State {
        IDLE,
        CLOSED,
        CONNECTING,
        CONNECTED,
        SYNC_STARTED,
        SYNC_IN_PROGRESS,
        SYNC_DONE,
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
                    intent.getSerializableExtra(STATE) as SyncStateResult,
                    intent.getStringExtra(MESSAGE) ?: "",
                    intent.getStringExtra(DETAILS) ?: "")
        }
    }

    interface Listener {
        /**
         * Interface method that is called then a local broadcast was received
         * with the connection and synchronization state including any text messages
         *
         * @param result   The current connection and synchronization state
         * @param message Main text message describing the state
         * @param details Additional text message to describe any details
         */
        fun onSyncStateChanged(result: SyncStateResult, message: String, details: String)
    }

    companion object {

        const val ACTION = "service_status"
        const val STATE = "state"
        const val MESSAGE = "message"
        const val DETAILS = "details"
    }
}
