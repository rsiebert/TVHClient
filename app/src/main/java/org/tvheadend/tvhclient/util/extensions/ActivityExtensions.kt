package org.tvheadend.tvhclient.util.extensions

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.ui.common.SnackbarMessageReceiver
import org.tvheadend.tvhclient.util.isLightTheme
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

/**
 * Enables edge-to-edge rendering for this activity and keeps the system bar icons contrasting with
 * the app background.
 *
 * The app picks its light or dark theme explicitly (see `getThemeId`), so the night mode
 * configuration alone is not a reliable signal for the icon colour; the selected theme is used
 * instead.
 */
fun Activity.setUpEdgeToEdge() {
    WindowCompat.enableEdgeToEdge(window)
    val useDarkIcons = isLightTheme(this)
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = useDarkIcons
        isAppearanceLightNavigationBars = useDarkIcons
    }
}
