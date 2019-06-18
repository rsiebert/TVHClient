package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.Snackbar
import org.tvheadend.tvhclient.R

fun Snackbar.config(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(12, 12, 12, 12)
        this.view.layoutParams = params
        this.view.background = context.getDrawable(R.drawable.snackbar_background)
        ViewCompat.setElevation(this.view, 6f)
    }
}