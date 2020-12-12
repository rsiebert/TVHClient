package org.tvheadend.tvhclient.service.http

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import org.json.JSONObject
import org.tvheadend.api.*
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.service.ConnectionService

class HttpServiceHandler(val context: Context, val appRepository: AppRepository, val connection: Connection) : ConnectionService.ServiceInterface, ServerConnectionStateListener, ServerMessageListener<JSONObject>, ServerRequestInterface {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun onStartCommand(intent: Intent?): Int {
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

    override fun onMessage(response: JSONObject) {
        TODO("Not yet implemented")
    }

    override fun getDiscSpace() {
        TODO("Not yet implemented")
    }

    override fun getSystemTime() {
        TODO("Not yet implemented")
    }

    override fun getChannel(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getEvent(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getEvents(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getEpgQuery(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun addDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun updateDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun cancelDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun deleteDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun stopDvrEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun addAutorecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun updateAutorecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun deleteAutorecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun addTimerrecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun updateTimerrecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun deleteTimerrecEntry(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getTicket(intent: Intent) {
        TODO("Not yet implemented")
    }

    override fun getProfiles() {
        TODO("Not yet implemented")
    }

    override fun getDvrConfigs() {
        TODO("Not yet implemented")
    }

    override fun getSubscriptions() {
        TODO("Not yet implemented")
    }

    override fun getInputs() {
        TODO("Not yet implemented")
    }

    override fun getMoreEvents(intent: Intent) {
        TODO("Not yet implemented")
    }
}
