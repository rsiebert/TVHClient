package org.tvheadend.tvhclient.ui.common

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_CONTENT
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_DURATION
import timber.log.Timber

fun showSnackbarMessage(activity: AppCompatActivity, intent: Intent) {
    val msg = intent.getStringExtra(SNACKBAR_CONTENT)
    val duration = intent.getIntExtra(SNACKBAR_DURATION, Snackbar.LENGTH_SHORT)
    val view: View? = activity.findViewById(android.R.id.content)

    if (view != null && !msg.isNullOrEmpty()) {
        Timber.d("Showing snackbar message $msg")
        Snackbar.make(view, msg, duration).show()
    }
}
