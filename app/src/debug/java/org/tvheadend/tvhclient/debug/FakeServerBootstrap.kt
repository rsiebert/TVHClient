package org.tvheadend.tvhclient.debug

import android.content.Context
import kotlinx.coroutines.delay
import org.tvheadend.data.entity.Connection
import org.tvheadend.htsp.FakeTvheadendServer
import org.tvheadend.tvhclient.MainApplication
import timber.log.Timber

/**
 * Debug-only bootstrap that starts the in-process [FakeTvheadendServer] and adds the demo
 * connections pointing at it. Called from [MainApplication.onCreate] via the build-type specific
 * twin in the release source set.
 *
 * It seeds two connections to the *same* fake server (see [DEMO_CONNECTIONS]): the first is the
 * active one, the second is left inactive. Having more than one makes the multi-connection flows -
 * the connection list, "switch server", and per-connection state - testable out of the box without
 * a second real server.
 *
 * Unlike the previous "only when the app has no connections at all" behaviour, each demo connection
 * is now added independently whenever a connection with its name is missing. That way an existing
 * debug install (which already has the first demo connection, or a real one) still picks up the
 * second. A connection is only ever identified by its [DemoConnection.name], so user-added
 * connections are never touched. To get rid of a demo connection the entry has to be removed from
 * [DEMO_CONNECTIONS] - deleting it in the UI alone is not enough, it would reappear on the next
 * launch.
 */
object FakeServerBootstrap {

    /**
     * The demo connections that should exist. They all point at the same in-process fake server;
     * only [DemoConnection.isActive] differs.
     */
    private val DEMO_CONNECTIONS = listOf(
        DemoConnection(name = "Demo (fake server)", isActive = true),
        DemoConnection(name = "Demo 2 (fake server)", isActive = false),
    )

    @Volatile
    private var server: FakeTvheadendServer? = null

    /**
     * Suspending so it can run inside [MainApplication]'s migration coroutine and never blocks the
     * main thread. It must run after the migration, because it depends on the migrated connection
     * data. Tests drive it from their own coroutine so it runs inline.
     */
    suspend fun start(context: Context) {
        if (server != null) {
            return
        }
        val app = context.applicationContext as? MainApplication ?: return

        val fakeServer = try {
            FakeTvheadendServer().start()
        } catch (e: Exception) {
            Timber.e(e, "Could not start the fake TVHeadend server")
            return
        }
        server = fakeServer

        seedDemoConnections(app, fakeServer)
    }

    private suspend fun seedDemoConnections(app: MainApplication, fakeServer: FakeTvheadendServer) {
        try {
            val repository = app.appRepository
            val existingNames = repository.connectionData.getItems().mapNotNull { it.name }
            val missing = DEMO_CONNECTIONS.filter { it.name !in existingNames }
            if (missing.isEmpty()) {
                Timber.d("All demo connections already exist, not adding any")
                return
            }

            missing.forEach { repository.connectionData.addItem(it.toConnection(fakeServer)) }

            // addItem inserts on a background dispatcher. Wait (briefly) until the rows exist so
            // that ConnectionService.onCreate() picks up the active connection when the app's
            // normal start-up connect kicks in. delay(), not Thread.sleep(): we are already on a
            // background dispatcher here, and blocking the thread would only tie one up.
            val deadline = System.currentTimeMillis() + 2_000
            while (System.currentTimeMillis() < deadline) {
                val names = repository.connectionData.getItems().mapNotNull { it.name }.toSet()
                if (DEMO_CONNECTIONS.all { it.name in names }) {
                    break
                }
                delay(25)
            }
            Timber.i(
                "Added ${missing.size} demo connection(s) to the fake TVHeadend server at ${fakeServer.url}"
            )
        } catch (e: Exception) {
            Timber.e(e, "Could not add the demo connections")
        }
    }

    /**
     * Template for one of the seeded connections. Everything except [name] and [isActive] is
     * identical, because they all point at the same [FakeTvheadendServer].
     */
    private data class DemoConnection(val name: String, val isActive: Boolean) {
        fun toConnection(fakeServer: FakeTvheadendServer) = Connection(
            name = name,
            hostname = FakeTvheadendServer.DEFAULT_HOST,
            port = fakeServer.boundPort,
            username = "demo",
            password = "demo",
            isActive = isActive,
            streamingPort = fakeServer.boundPort,
            lastUpdate = 0,
            isSyncRequired = true,
            serverUrl = fakeServer.url,
            streamingUrl = fakeServer.url,
        )
    }
}
