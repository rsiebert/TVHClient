package org.tvheadend.tvhclient.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import timber.log.Timber

/**
 * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
 * Any string in the given extra will be shown as a snackbar.
 */
class SnackbarMessageReceiver(private val viewModel: SnackbarMessageInterface) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received new snackbar message")
        viewModel.setSnackbarMessage(intent)
    }

    companion object {

        const val SNACKBAR_ACTION = "snackbar_message"
        const val SNACKBAR_CONTENT = "content"
        const val SNACKBAR_DURATION = "duration"
    }
}
