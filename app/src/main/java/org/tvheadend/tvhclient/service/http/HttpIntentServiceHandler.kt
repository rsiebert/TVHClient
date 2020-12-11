package org.tvheadend.tvhclient.service.http

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.service.ConnectionIntentService

class HttpIntentServiceHandler(val context: Context, val appRepository: AppRepository, val connection: Connection) : ConnectionIntentService.ServiceInterface, ServerConnectionStateListener {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun onHandleWork(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        TODO("Not yet implemented")
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        TODO("Not yet implemented")
    }
}
