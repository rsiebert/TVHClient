package org.tvheadend.tvhclient.data.service.htsp

interface HtspConnectionStateListener {

    fun onAuthenticationStateChange(state: HtspConnection.AuthenticationState)

    fun onConnectionStateChange(state: HtspConnection.ConnectionState)
}
