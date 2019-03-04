package org.tvheadend.tvhclient.ui.features.playback.external

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.data.service.htsp.HtspConnection
import org.tvheadend.tvhclient.data.service.htsp.HtspConnectionStateListener
import org.tvheadend.tvhclient.data.service.htsp.HtspMessage
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Inject

class ExternalPlayerViewModel(application: Application) : AndroidViewModel(application), HtspConnectionStateListener {

    @Inject
    lateinit var context: Context
    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var appRepository: AppRepository

    // Connection related
    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    private val htspConnection: HtspConnection = HtspConnection(this, null)

    var connection: Connection? = null
    var serverStatus: ServerStatus
    var channel: Channel? = null
    var recording: Recording? = null
    private var path = ""
    private var ticket = ""

    // Observable fields
    var isTicketReceived: MutableLiveData<Boolean> = MutableLiveData()
    var isConnected: MutableLiveData<Boolean> = MutableLiveData()

    init {
        Timber.d("Initializing view model")
        MainApplication.getComponent().inject(this)

        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem

        Timber.d("Starting connection")
        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }
    }

    override fun onConnectionStateChange(state: HtspConnection.ConnectionState) {
        when (state) {
            HtspConnection.ConnectionState.FAILED..HtspConnection.ConnectionState.FAILED_EXCEPTION_OPENING_SOCKET -> {
                Timber.d("Connection failed")
                isConnected.postValue(false)
            }
            else -> {
                Timber.d("Connected, initializing or idle")
            }
        }
    }

    override fun onAuthenticationStateChange(state: HtspConnection.AuthenticationState) {
        when (state) {
            HtspConnection.AuthenticationState.FAILED,
            HtspConnection.AuthenticationState.FAILED_BAD_CREDENTIALS -> {
                Timber.d("Authorization failed")
                isConnected.postValue(false)
            }
            HtspConnection.AuthenticationState.AUTHENTICATED -> {
                Timber.d("Authenticated, starting player")
                isConnected.postValue(true)
            }
            else -> {
                Timber.d("Initializing or authenticating")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing view model")
        execService.shutdown()
        htspConnection.closeConnection()
    }

    fun requestTicketFromServer(bundle: Bundle?) {
        val channelId = bundle?.getInt("channelId") ?: 0
        val dvrId = bundle?.getInt("dvrId") ?: 0

        val request = HtspMessage()
        request["method"] = "getTicket"
        if (channelId > 0) {
            channel = appRepository.channelData.getItemById(channelId)
            request["channelId"] = channelId
        }
        if (dvrId > 0) {
            recording = appRepository.recordingData.getItemById(dvrId)
            request["dvrId"] = dvrId
        }
        htspConnection.sendMessage(request) { response ->
            if (response != null) {
                Timber.d("Received response for ticket request")
                path = response.getString("path")
                ticket = response.getString("ticket")
                isTicketReceived.postValue(true)
            } else {
                Timber.d("No response for ticket request received")
            }
        }
    }

    fun getServerUrl(convertHostnameToAddress: Boolean = false): String {
        // Convert the hostname to the IP address only when required.
        // This is usually required when a channel or recording shall
        // be played on a chromecast
        var hostname = connection?.hostname
        if (convertHostnameToAddress && !TextUtils.isEmpty(connection?.hostname)) {
            Timber.d("Convert hostname ${connection?.hostname} to IP address")
            try {
                hostname = ConvertHostnameToAddressTask(connection?.hostname ?: "").execute().get()
            } catch (e: InterruptedException) {
                Timber.d("Could not execute task to get ip address from ${connection?.hostname}", e)
            } catch (e: ExecutionException) {
                Timber.d("Could not execute task to get ip address from ${connection?.hostname}", e)
            }
        } else {
            Timber.d("Hostname ${connection?.hostname} to IP address conversion not required")
        }

        var baseUrl = "http://$hostname"
        if (connection?.streamingPort != 80 && connection?.streamingPort != 443) {
            baseUrl = "http://$hostname:${connection?.streamingPort}"
        }

        if (!TextUtils.isEmpty(serverStatus.webroot)) {
            baseUrl += serverStatus.webroot
        }
        return baseUrl
    }

    fun getPlaybackUrl(convertHostname: Boolean = false, profileId: Int = 0): String {
        val serverProfile = if (profileId > 0) appRepository.serverProfileData.getItemById(profileId) else appRepository.serverProfileData.getItemById(serverStatus.httpPlaybackServerProfileId)
        return "${getServerUrl(convertHostname)}$path?ticket=$ticket&profile=${serverProfile?.name}"
    }
}
