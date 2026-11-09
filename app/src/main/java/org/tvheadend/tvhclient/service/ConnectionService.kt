package org.tvheadend.tvhclient.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.htsp.HtspServiceHandler
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class ConnectionService : Service() {

    companion object {
        /** Opens the connection to the active server. */
        const val ACTION_CONNECT = "connect"

        /**
         * Rebinds the running service to the currently active connection. The current handler is
         * torn down and a new one is created for the active connection, so the application can
         * switch servers without being restarted.
         */
        const val ACTION_REBIND = "rebind"
    }

    private lateinit var appRepository: AppRepository
    private lateinit var execService: ScheduledExecutorService

    /**
     * Serializes command handling on a single background thread. A single thread (rather than the
     * shared, multi-threaded HTSP pool) keeps the commands in the order they arrive, which the
     * handler relies on, while also keeping the active-connection database read out of
     * [onCreate]/[onStartCommand] so the main thread is never blocked on it.
     */
    private val commandExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "htsp-service-command").apply { isDaemon = true }
    }

    /** Only ever touched on [commandExecutor]. */
    private var serviceHandler: ServiceInterface? = null

    override fun onCreate() {
        Timber.d("Starting service")
        val application = application as MainApplication
        appRepository = application.appRepository
        execService = application.htspExecutor
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        commandExecutor.execute { handleCommand(intent) }
        return START_NOT_STICKY
    }

    private fun handleCommand(intent: Intent?) {
        if (intent?.action == ACTION_REBIND) {
            rebindServiceHandler()
            serviceHandler?.onStartCommand(Intent(ACTION_CONNECT))
            return
        }
        (serviceHandler ?: createServiceHandler()).onStartCommand(intent)
    }

    private fun createServiceHandler(): ServiceInterface {
        // Runs on commandExecutor, so the (blocking) read of the active connection never blocks
        // the main thread.
        val connection: Connection = runBlocking { appRepository.connectionData.getActiveItem() }
        return HtspServiceHandler(this, appRepository, connection, execService)
                .also { serviceHandler = it }
    }

    /**
     * Replaces the current handler with a new one bound to the active connection. Called when the
     * user switches servers so the already running application reconnects in place instead of
     * being restarted.
     */
    private fun rebindServiceHandler() {
        Timber.d("Rebinding the service handler to the active connection")
        serviceHandler?.onDestroy()
        serviceHandler = null
        createServiceHandler()
    }

    override fun onDestroy() {
        // Let any in-flight command finish, then tear the handler down on the same thread so its
        // state is not touched concurrently.
        commandExecutor.execute { serviceHandler?.onDestroy() }
        commandExecutor.shutdown()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    interface ServiceInterface {
        fun onStartCommand(intent: Intent?): Int
        fun onDestroy()
    }
}
