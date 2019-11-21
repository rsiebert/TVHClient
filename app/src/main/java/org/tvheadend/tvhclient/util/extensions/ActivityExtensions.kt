package org.tvheadend.tvhclient.util.extensions

import android.app.Activity
import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import timber.log.Timber

fun Activity.showSnackbarMessage(intent: Intent) {
    val msg = intent.getStringExtra(SnackbarMessageReceiver.SNACKBAR_CONTENT)
    val duration = intent.getIntExtra(SnackbarMessageReceiver.SNACKBAR_DURATION, Snackbar.LENGTH_SHORT)
    val view: View? = this.findViewById(android.R.id.content)

    if (view != null && !msg.isNullOrEmpty()) {
        Timber.d("Showing snackbar message $msg")
        Snackbar.make(view, msg, duration).show()
    }
}
