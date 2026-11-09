package org.tvheadend.tvhclient.ui.features.playback.external

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import org.tvheadend.api.ServerResponseListener
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.htsp.*
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.playback.PlaybackLogic
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ScheduledExecutorService

class ExternalPlayerViewModel(application: Application) : BaseViewModel(application), ServerConnectionStateListener {

    // Connection related
    private val execService: ScheduledExecutorService = AppGraphProvider.require().htspExecutor
    private val htspConnection: HtspConnection

    var channel: Channel? = null
    var recording: Recording? = null
    private var path = ""
    private var ticket = ""

    // Observable fields
    var isTicketReceived: MutableLiveData<Boolean> = MutableLiveData()
    var isConnected: MutableLiveData<Boolean> = MutableLiveData()

    init {
        Timber.d("Initializing")
        val connectionTimeout = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(application).getString("connection_timeout", application.resources.getString(R.string.pref_default_connection_timeout))!!) * 1000
        val htspConnectionData = HtspConnectionData(
                connection.username,
                connection.password,
                connection.serverUrl,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                connectionTimeout
        )
        htspConnection = HtspConnection(htspConnectionData, this, null)

        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        when (result) {
            is AuthenticationStateResult.Idle -> {}
            is AuthenticationStateResult.Authenticating -> {
                Timber.d("Authenticating")
            }
            is AuthenticationStateResult.Authenticated -> {
                Timber.d("Authenticated, starting player")
                isConnected.postValue(true)
            }
            is AuthenticationStateResult.Failed -> {
                Timber.w("Authorization with the server failed")
                isConnected.postValue(false)
            }
        }
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        when (result) {
            is ConnectionStateResult.Failed -> {
                Timber.w("Connection to the server failed")
                isConnected.postValue(false)
            }
            else -> {
                Timber.d("Connected, initializing or idle")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing view model")
        // The executor is shared and application scoped, so it is not shut down here.
        htspConnection.closeConnection()
    }

    suspend fun requestTicketFromServer(bundle: Bundle?) {
        val channelId = bundle?.getInt("channelId") ?: 0
        val dvrId = bundle?.getInt("dvrId") ?: 0

        val request = HtspMessage()
        request["method"] = "getTicket"
        if (channelId > 0) {
            channel = appRepository.channelData.getItemById(channelId)
            Timber.d("Requesting ticket for channel id $channelId")
            request["channelId"] = channelId
        }
        if (dvrId > 0) {
            recording = appRepository.recordingData.getItemById(dvrId)
            Timber.d("Requesting ticket for recording id $dvrId")
            request["dvrId"] = dvrId
        }
        htspConnection.sendMessage(request, object : ServerResponseListener<HtspMessage> {
            override fun handleResponse(response: HtspMessage) {
                path = response.getString("path", "")
                ticket = response.getString("ticket", "")
                // The ticket is a credential valid until it expires, so its value is never logged.
                Timber.d("Received response for ticket request, path is $path, ticket received ${ticket.isNotEmpty()}")
                isTicketReceived.postValue(true)
            }
        })
    }

    suspend fun getServerUrl(convertHostnameToAddress: Boolean = false): String {
        // Convert the hostname to the IP address only when required.
        // This is usually required when a channel or recording shall
        // be played on a chromecast
        val uri = Uri.parse(connection.streamingUrl)
        var hostname = uri.host
        if (convertHostnameToAddress) {
            Timber.d("Convert hostname $hostname to IP address")
            withContext(Dispatchers.IO) {
                try {
                    if (!hostname.isNullOrEmpty()) {
                        Timber.d("Trying to convert hostname $hostname to an address")
                        hostname = InetAddress.getByName(hostname).hostAddress
                    }
                } catch (e: UnknownHostException) {
                    Timber.w(e, "Could not resolve hostname $hostname, using the hostname as fallback")
                }
            }
        } else {
            Timber.d("Hostname $hostname to IP address conversion not required")
        }

        var baseUrl = "${uri.scheme.toString().lowercase()}://$hostname"
        if (uri.port != 80 && uri.port != 443) {
            baseUrl = "${uri.scheme}://$hostname:${uri.port}"
        }
        if (!uri.path.isNullOrEmpty()) {
            baseUrl += uri.path
        }

        Timber.d("Original url was ${connection.streamingUrl}, converted url is $baseUrl")

        return baseUrl
    }

    private suspend fun getHttpProfile(): ServerProfile? {
        val serverStatus = appRepository.serverStatusData.getActiveItem()
        return appRepository.serverProfileData.getItemById(serverStatus.httpPlaybackServerProfileId)
    }

    suspend fun getServerStatus(): ServerStatus {
        return appRepository.serverStatusData.getActiveItem()
    }

    suspend fun getPlaybackUrl(convertHostname: Boolean = false, profileId: Int = 0): String {
        // If the server status is null, then use the default id of zero which will
        // return a null server profile. In this case use the default profile 'pass'

        val defaultProfile = getHttpProfile()
        val defaultProfileName = defaultProfile?.name ?: "pass"

        // Get the playback profile for the given id. In case no profile is returned, use the default name
        val serverProfile = appRepository.serverProfileData.getItemById(profileId)
        return PlaybackLogic.playbackUrl(
            baseUrl = getServerUrl(convertHostname),
            path = path,
            ticket = ticket,
            profileName = serverProfile?.name ?: defaultProfileName
        )
    }
}
