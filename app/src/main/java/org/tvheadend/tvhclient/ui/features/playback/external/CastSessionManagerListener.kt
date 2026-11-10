package org.tvheadend.tvhclient.ui.features.playback.external

import android.app.Activity

import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

import timber.log.Timber

/**
 * Invalidates the activity's options menu when a Cast session starts, resumes or ends, so that the
 * per-item `cast` actions appear and disappear with the session.
 */
class CastSessionManagerListener(private val activity: Activity) : SessionManagerListener<CastSession> {

    override fun onSessionEnded(session: CastSession, error: Int) {
        Timber.d("Cast session ended with error $error")
        activity.invalidateOptionsMenu()
    }

    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
        Timber.d("Cast session resumed, was suspended $wasSuspended")
        activity.invalidateOptionsMenu()
    }

    override fun onSessionStarted(session: CastSession, sessionId: String) {
        Timber.d("Cast session started with id $sessionId")
        activity.invalidateOptionsMenu()
    }

    override fun onSessionStarting(session: CastSession) {
        Timber.d("Cast session is starting")
    }

    override fun onSessionStartFailed(session: CastSession, error: Int) {
        Timber.d("Cast session failed with error $error")
    }

    override fun onSessionEnding(session: CastSession) {
        Timber.d("Cast session is ending")
    }

    override fun onSessionResuming(session: CastSession, sessionId: String) {
        Timber.d("Cast session is resuming with id $sessionId")
    }

    override fun onSessionResumeFailed(session: CastSession, error: Int) {
        Timber.d("Cast session resume failed with error $error")
    }

    override fun onSessionSuspended(session: CastSession, reason: Int) {
        Timber.d("Cast session suspended with reason $reason")
    }
}
