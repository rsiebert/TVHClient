package org.tvheadend.tvhclient.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.htsp.HtspServiceHandler
import timber.log.Timber
import javax.inject.Inject

class ConnectionService : Service() {

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private lateinit var connection: Connection
    private lateinit var serviceHandler: ServiceInterface

    override fun onCreate() {
        Timber.d("Starting service")
        MainApplication.component.inject(this)

        connection = appRepository.connectionData.activeItem
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
