package org.tvheadend.tvhclient.service.http

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.service.ConnectionService

class HttpServiceHandler(val context: Context, val appRepository: AppRepository, val connection: Connection) : ConnectionService.ServiceInterface {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun onStartCommand(intent: Intent?): Int {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }
}
