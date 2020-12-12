package org.tvheadend.tvhclient.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.JobIntentService
import androidx.preference.PreferenceManager
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.htsp.HtspIntentServiceHandler
import org.tvheadend.tvhclient.service.http.HttpIntentServiceHandler
import timber.log.Timber
import javax.inject.Inject

class ConnectionIntentService : JobIntentService() {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var connection: Connection
    private lateinit var serviceHandler: ServiceInterface

    init {
        Timber.d("Starting service")
        MainApplication.component.inject(this)
        connection = appRepository.connectionData.activeItem

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        when (sharedPreferences.getString("connection_type", context.resources.getString(R.string.pref_default_connection_type))) {
            "htsp" -> serviceHandler = HtspIntentServiceHandler(context, appRepository, connection)
            "http" -> serviceHandler = HttpIntentServiceHandler(context, appRepository, connection)
        }
    }

    override fun onHandleWork(intent: Intent) {
        serviceHandler.onHandleWork(intent)
    }

    override fun onDestroy() {
        Timber.d("Stopping service")
        serviceHandler.onDestroy()
    }

    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, ConnectionIntentService::class.java, 1, work)
        }
    }

    interface ServiceInterface {
        fun onHandleWork(intent: Intent)
        fun onDestroy()
    }
}
