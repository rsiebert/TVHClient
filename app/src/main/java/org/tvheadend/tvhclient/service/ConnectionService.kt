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
        return serviceHandler.onStartCommand(intent)
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
