package org.tvheadend.tvhclient.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import kotlinx.coroutines.runBlocking
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.htsp.HtspServiceHandler
import timber.log.Timber

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
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var connection: Connection
    private lateinit var serviceHandler: ServiceInterface

    override fun onCreate() {
        Timber.d("Starting service")
        val application = application as MainApplication
        appRepository = application.appRepository
        sharedPreferences = application.appSharedPreferences

        // onStartCommand() needs the handler (and thus the connection) to be ready right after
        // onCreate(), so this one read is bridged back to blocking. It is the documented boundary
        // for runBlocking outside the htsp handlers; the database read itself still happens off the
        // main thread via the data source's withContext(Dispatchers.IO).
        connection = runBlocking { appRepository.connectionData.getActiveItem() }
        serviceHandler = HtspServiceHandler(this, appRepository, connection)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REBIND) {
            rebindServiceHandler()
            return serviceHandler.onStartCommand(Intent(ACTION_CONNECT))
        }
        return serviceHandler.onStartCommand(intent)
    }

    /**
     * Replaces the current handler with a new one bound to the active connection. Called when the
     * user switches servers so the already running application reconnects in place instead of
     * being restarted.
     */
    private fun rebindServiceHandler() {
        Timber.d("Rebinding the service handler to the active connection")
        serviceHandler.onDestroy()
        connection = runBlocking { appRepository.connectionData.getActiveItem() }
        serviceHandler = HtspServiceHandler(this, appRepository, connection)
    }

    override fun onDestroy() {
        serviceHandler.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    interface ServiceInterface {
        fun onStartCommand(intent: Intent?): Int
        fun onDestroy()
    }
}
