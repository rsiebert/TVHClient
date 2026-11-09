package org.tvheadend.tvhclient.util

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Creates the pool that runs the blocking HTSP work: opening a connection, running the handshake,
 * downloading channel icons and driving the player connections.
 *
 * Every connection handler and both player view models used to create their own pool of ten
 * threads, so a live connection plus a player could hold dozens of threads that were mostly idle.
 * The pool is now created once and handed out by [org.tvheadend.tvhclient.AppGraph], which also lets
 * a test install a pool it controls.
 *
 * The threads are daemons and idle ones are released, so an idle application is not kept alive by
 * the pool.
 */
object HtspExecutor {

    const val DEFAULT_CORE_POOL_SIZE = 4
    private const val DEFAULT_IDLE_TIMEOUT_SECONDS = 30L

    fun create(
        corePoolSize: Int = DEFAULT_CORE_POOL_SIZE,
        idleTimeoutSeconds: Long = DEFAULT_IDLE_TIMEOUT_SECONDS
    ): ScheduledExecutorService =
            ScheduledThreadPoolExecutor(corePoolSize) { runnable ->
                Thread(runnable, "htsp-worker").apply { isDaemon = true }
            }.apply {
                setKeepAliveTime(idleTimeoutSeconds, TimeUnit.SECONDS)
                allowCoreThreadTimeOut(true)
            }
}
