package org.tvheadend.tvhclient

import org.tvheadend.data.AppRepository
import java.util.concurrent.ScheduledExecutorService

/**
 * The parts of the application's dependency graph that the ViewModels need.
 *
 * [MainApplication] is the only production implementation. It exists so that a ViewModel can be
 * constructed in a JVM test without a real `MainApplication`, whose `onCreate` performs Firebase,
 * Cast, Billing and database-migration work that has nothing to do with the ViewModel under test.
 */
interface AppGraph {

    val appRepository: AppRepository

    val mainRepository: MainRepository

    /**
     * The pool that runs the blocking HTSP work (opening a connection, the handshake, icon
     * downloads and the player connections). It is shared so a connection and a player do not each
     * hold their own set of idle threads, and so a test can supply its own pool.
     */
    val htspExecutor: ScheduledExecutorService
}

/**
 * Holds the [AppGraph] that the ViewModels resolve.
 *
 * In production [MainApplication.onCreate] installs itself here. Tests assign a lightweight graph
 * (a real [AppRepository] over an in-memory database) before constructing a ViewModel.
 */
object AppGraphProvider {

    @Volatile
    var graph: AppGraph? = null

    /** Returns the installed graph, falling back to the process-wide application instance. */
    fun require(): AppGraph = graph ?: MainApplication.instance
}
