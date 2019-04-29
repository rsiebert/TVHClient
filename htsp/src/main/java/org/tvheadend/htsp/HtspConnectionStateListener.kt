package org.tvheadend.htsp

interface HtspConnectionStateListener {

    fun onAuthenticationStateChange(state: HtspConnection.AuthenticationState)

    fun onConnectionStateChange(state: HtspConnection.ConnectionState)
}
