package org.tvheadend.api

interface HtspConnectionStateListener {

    fun onAuthenticationStateChange(result: AuthenticationStateResult)

    fun onConnectionStateChange(result: ConnectionStateResult)
}
