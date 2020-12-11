package org.tvheadend.tvhclient.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import java.lang.ref.WeakReference

sealed class SyncStateResult : Parcelable {
    @Parcelize
    data class Connecting(val reason: ConnectionStateResult): SyncStateResult(), Parcelable
    @Parcelize
    data class Syncing(val state: SyncState): SyncStateResult(), Parcelable
    @Parcelize
    data class Authenticating(val reason: AuthenticationStateResult): SyncStateResult(), Parcelable
}

sealed class SyncState : Parcelable {
    @Parcelize
    data class Started(val message: String = "") : SyncState(), Parcelable
    @Parcelize
    data class InProgress(val message: String = "") : SyncState(), Parcelable
    @Parcelize
    data class Done(val message: String = "") : SyncState(), Parcelable
}

class SyncStateReceiver(callback: Listener) : BroadcastReceiver() {

    private val callback: WeakReference<Listener> = WeakReference(callback)

    /**
     * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
     * The main message is sent via the "message" extra. Any details about the state is given
     * via the "details" extra.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (callback.get() != null) {
            (callback.get() as Listener).onSyncStateChanged(
                    intent.getParcelableExtra(STATE) as SyncStateResult)
        }
    }

    interface Listener {
        /**
         * Interface method that is called then a local broadcast was received
         * with the connection and synchronization state including any text messages
         *
         * @param result The current connection and synchronization state
         */
        fun onSyncStateChanged(result: SyncStateResult)
    }

    companion object {

        const val ACTION = "service_status"
        const val STATE = "state"
        const val MESSAGE = "message"
        const val DETAILS = "details"
    }
}
