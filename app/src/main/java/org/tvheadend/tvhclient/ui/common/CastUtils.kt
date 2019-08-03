package org.tvheadend.tvhclient.ui.common

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import timber.log.Timber

fun getCastSession(context: Context): CastSession? {
    val castContext = getCastContext(context)
    if (castContext != null) {
        try {
            return castContext.sessionManager.currentCastSession
        } catch (e: IllegalStateException) {
            Timber.e("Could not get current cast session")
        }
    }
    return null
}

fun getCastContext(context: Context): CastContext? {
    val playServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (playServicesAvailable == ConnectionResult.SUCCESS) {
        try {
            return CastContext.getSharedInstance(context)
        } catch (e: RuntimeException) {
            Timber.e(e, "Could not get cast context")
        }
    }
    return null
}