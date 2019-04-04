package org.tvheadend.tvhclient.ui.common

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import timber.log.Timber

fun Context.sendSnackbarMessage(resId: Int) {
    this.sendSnackbarMessage(this.getString(resId))
}

fun Context.sendSnackbarMessage(msg: String) {
    Timber.d("Sending broadcast to show snackbar message $msg")
    val intent = Intent(SnackbarMessageReceiver.ACTION)
    intent.putExtra(SnackbarMessageReceiver.CONTENT, msg)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}