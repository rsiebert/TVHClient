package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import timber.log.Timber

fun Context.sendSnackbarMessage(resId: Int) {
    this.sendSnackbarMessage(this.getString(resId))
}

fun Context.sendSnackbarMessage(msg: String) {
    Timber.d("Sending broadcast to show snackbar message $msg")
    val intent = Intent(SnackbarMessageReceiver.SNACKBAR_ACTION)
    intent.putExtra(SnackbarMessageReceiver.SNACKBAR_CONTENT, msg)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}