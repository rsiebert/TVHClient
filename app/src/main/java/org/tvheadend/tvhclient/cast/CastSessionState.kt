package org.tvheadend.tvhclient.cast

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Process-wide Cast availability state.
 *
 * The [CastPlaybackService] owns the CastPlayer and is the only place where "is a Cast session
 * available" can be observed (via `onDeviceInfoChanged` with `PLAYBACK_TYPE_REMOTE`). It publishes
 * that here so screens that only have a `Context` - the menu builders and the play-vs-cast
 * decisions - can read it without connecting a `MediaController` everywhere.
 *
 * Replaces the legacy `context.getCastSession() != null` check.
 */
object CastSessionState {

    @Volatile
    var isCastAvailable: Boolean = false
        private set

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /** Updates the flag and notifies listeners only when the value actually changes. */
    fun setCastAvailable(available: Boolean) {
        val changed = isCastAvailable != available
        isCastAvailable = available
        if (changed) {
            listeners.forEach { it() }
        }
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}
