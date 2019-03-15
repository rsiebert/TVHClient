package org.tvheadend.tvhclient.ui.base.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.google.android.material.snackbar.Snackbar

import java.lang.ref.WeakReference

/**
 * This receiver handles the data that was given from an intent via the LocalBroadcastManager.
 * Any string in the given extra will be shown as a snackbar.
 */
class SnackbarMessageReceiver(activity: Activity) : BroadcastReceiver() {
    private val activity: WeakReference<Activity> = WeakReference(activity)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(CONTENT)) {
            val activity = this.activity.get()
            if (activity != null && activity.currentFocus != null) {
                val msg = intent.getStringExtra(CONTENT)
                val duration = intent.getIntExtra(DURATION, Snackbar.LENGTH_SHORT)
                Snackbar.make(activity.currentFocus!!, msg, duration).show()
            }
        }
    }

    companion object {

        const val ACTION = "snackbar_message"
        const val CONTENT = "content"
        const val DURATION = "duration"
    }
}
