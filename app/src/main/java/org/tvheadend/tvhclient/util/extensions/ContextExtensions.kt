package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.service.SyncStateReceiver
import org.tvheadend.tvhclient.service.SyncStateResult
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import timber.log.Timber

fun Context.sendSnackbarMessage(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
    this.sendSnackbarMessage(this.getString(resId), duration)
}

fun Context.sendSnackbarMessage(msg: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Timber.d("Sending broadcast to show snackbar message $msg")
    val intent = Intent(SnackbarMessageReceiver.SNACKBAR_ACTION)
    intent.putExtra(SnackbarMessageReceiver.SNACKBAR_CONTENT, msg)
    intent.putExtra(SnackbarMessageReceiver.SNACKBAR_DURATION, duration)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}

fun Context.sendSyncStateMessage(state: SyncStateResult, message: String = "", details: String = "") {

    val intent = Intent(SyncStateReceiver.ACTION)
    intent.putExtra(SyncStateReceiver.STATE, state)
    if (message.isNotEmpty()) {
        intent.putExtra(SyncStateReceiver.MESSAGE, message)
    }
    if (details.isNotEmpty()) {
        intent.putExtra(SyncStateReceiver.DETAILS, details)
    }
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}