package org.tvheadend.tvhclient.util

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * The single background pool for the blocking HTSP work: opening a connection, running the
 * handshake, downloading channel icons and driving the player connections.
 *
 * Every connection handler and both player view models used to create their own pool of ten
 * threads, so a live connection plus a player could hold dozens of threads that were mostly idle.
 *
 * The pool is application scoped and is deliberately never shut down. The connection handler
 * previously shut its pool down in `onDestroy`, which is not possible once the pool is shared; the
 * threads are daemons and the pool releases idle ones again.
 */
object HtspExecutor {

    private const val CORE_POOL_SIZE = 4
    private const val IDLE_TIMEOUT_SECONDS = 30L

    val service: ScheduledExecutorService =
            ScheduledThreadPoolExecutor(CORE_POOL_SIZE) { runnable ->
                Thread(runnable, "htsp-worker").apply { isDaemon = true }
            }.apply {
                // Let idle threads go away so a connection that is not doing anything does not keep
                // the whole pool alive.
                setKeepAliveTime(IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                allowCoreThreadTimeOut(true)
            }
}
