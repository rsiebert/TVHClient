package org.tvheadend.tvhclient.ui.features.playback.external

import android.app.Application
import android.net.Uri
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutionException

class ExternalPlayerViewModel(application: Application) : BaseViewModel(application) {

    var channel: Channel? = null
    var recording: Recording? = null
    var path = ""
    var ticket = ""

    fun getServerUrl(convertHostnameToAddress: Boolean = false): String {
        // Convert the hostname to the IP address only when required.
        // This is usually required when a channel or recording shall
        // be played on a chromecast
        val uri = Uri.parse(connection.streamingUrl)
        var hostname = uri.host
        if (convertHostnameToAddress && !hostname.isNullOrEmpty()) {
            Timber.d("Convert hostname $hostname to IP address")
            try {
                hostname = ConvertHostnameToAddressTask(hostname).execute().get()
            } catch (e: InterruptedException) {
                Timber.d(e, "Could not execute task to get ip address from $hostname")
            } catch (e: ExecutionException) {
                Timber.d(e, "Could not execute task to get ip address from $hostname")
            }
        } else {
            Timber.d("Hostname $hostname to IP address conversion not required")
        }

        var baseUrl = "${uri.scheme.toString().toLowerCase(Locale.getDefault())}://$hostname"
        if (uri.port != 80 && uri.port != 443) {
            baseUrl = "${uri.scheme}://$hostname:${uri.port}"
        }
        if (!uri.path.isNullOrEmpty()) {
            baseUrl += uri.path
        }

        Timber.d("Original url was ${connection.streamingUrl}, converted url is $baseUrl")
        return baseUrl
    }

    private fun getHttpProfile(): ServerProfile? {
        val serverStatus = appRepository.serverStatusData.activeItem
        return appRepository.serverProfileData.getItemById(serverStatus.httpPlaybackServerProfileId)
    }

    fun getServerStatus(): ServerStatus {
        return appRepository.serverStatusData.activeItem
    }

    fun getPlaybackUrl(convertHostname: Boolean = false, profileId: Int = 0): String {
        // If the server status is null, then use the default id of zero which will
        // return a null server profile. In this case use the default profile 'pass'

        val defaultProfile = getHttpProfile()
        val defaultProfileName = defaultProfile?.name ?: "pass"

        // Get the playback profile for the given id. In case no profile is returned, use the default name
        val serverProfile = appRepository.serverProfileData.getItemById(profileId)
        return "${getServerUrl(convertHostname)}$path?ticket=$ticket&profile=${serverProfile?.name
                ?: defaultProfileName}"
    }
}
