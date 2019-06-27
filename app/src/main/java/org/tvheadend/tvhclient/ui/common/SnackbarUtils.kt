package org.tvheadend.tvhclient.ui.common

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_CONTENT
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver.Companion.SNACKBAR_DURATION
import org.tvheadend.tvhclient.util.extensions.config
import timber.log.Timber

fun showSnackbarMessage(activity: AppCompatActivity, intent: Intent) {
    val msg = intent.getStringExtra(SNACKBAR_CONTENT)
    val duration = intent.getIntExtra(SNACKBAR_DURATION, Snackbar.LENGTH_SHORT)
    val view: View? = activity.findViewById(android.R.id.content)
    view?.let {
        Timber.d("Showing snackbar message $msg")
        val snackbar = Snackbar.make(view, msg, duration)
        snackbar.config(activity)
        snackbar.show()
    }
}
