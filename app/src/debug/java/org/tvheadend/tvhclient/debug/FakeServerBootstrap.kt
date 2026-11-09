package org.tvheadend.tvhclient.debug

import android.content.Context
import kotlinx.coroutines.delay
import org.tvheadend.data.entity.Connection
import org.tvheadend.htsp.FakeTvheadendServer
import org.tvheadend.tvhclient.MainApplication
import timber.log.Timber

/**
 * Debug-only bootstrap that starts the in-process [FakeTvheadendServer] and, when the app has
 * no connections yet, adds a demo connection pointing at it. Called from
 * [MainApplication.onCreate] via the build-type specific twin in the release source set.
 *
 * This makes a fresh debug install show channels, EPG data and recordings without a real
 * TVHeadend server. Existing connections are never touched, so testing against a real server
 * keeps working once you have added one (delete the demo connection if you want it gone).
 */
object FakeServerBootstrap {

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

        seedDemoConnectionIfNone(app, fakeServer)
    }

    private suspend fun seedDemoConnectionIfNone(app: MainApplication, fakeServer: FakeTvheadendServer) {
        try {
            val repository = app.appRepository
            if (repository.connectionData.getItems().isNotEmpty()) {
                Timber.d("A connection already exists, not adding the demo connection")
                return
            }

            val connection = Connection(
                name = "Demo (fake server)",
                hostname = FakeTvheadendServer.DEFAULT_HOST,
                port = fakeServer.boundPort,
                username = "demo",
                password = "demo",
                isActive = true,
                streamingPort = fakeServer.boundPort,
                lastUpdate = 0,
                isSyncRequired = true,
                serverUrl = fakeServer.url,
                streamingUrl = fakeServer.url,
            )
            repository.connectionData.addItem(connection)

            // addItem inserts on a background dispatcher. Wait (briefly) until the row exists so
            // that ConnectionService.onCreate() picks it up as the active connection when the
            // app's normal start-up connect kicks in. delay(), not Thread.sleep(): we are already on
            // a background dispatcher here, and blocking the thread would only tie one up.
            val deadline = System.currentTimeMillis() + 2_000
            while (System.currentTimeMillis() < deadline) {
                if (repository.connectionData.getItems().isNotEmpty()) {
                    break
                }
                delay(25)
            }
            Timber.i("Added demo connection to the fake TVHeadend server at ${fakeServer.url}")
        } catch (e: Exception) {
            Timber.e(e, "Could not add the demo connection")
        }
    }
}
