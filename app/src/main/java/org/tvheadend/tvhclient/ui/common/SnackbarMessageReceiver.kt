package org.tvheadend.tvhclient.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tvheadend.tvhclient.ui.features.MainViewModel
import timber.log.Timber

/**
 * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
 * Any string in the given extra will be shown as a snackbar.
 */
class SnackbarMessageReceiver(private val mainViewModel: MainViewModel) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received new snackbar message")
        mainViewModel.showSnackbar.value = intent
    }

    companion object {

        const val ACTION = "snackbar_message"
        const val CONTENT = "content"
        const val DURATION = "duration"
    }
}
