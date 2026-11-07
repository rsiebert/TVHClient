package org.tvheadend.tvhclient.debug

import android.content.Context

/**
 * Release variant twin of the debug [FakeServerBootstrap]. The fake TVHeadend server is not
 * shipped in release builds, so this does nothing. Keeping the symbol in both build types
 * lets [org.tvheadend.tvhclient.MainApplication] call it unconditionally.
 */
object FakeServerBootstrap {

    fun start(context: Context) {
        // No-op in release builds.
    }
}
