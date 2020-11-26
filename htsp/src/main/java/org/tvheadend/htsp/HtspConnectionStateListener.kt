package org.tvheadend.htsp

interface HtspConnectionStateListener {

    fun onAuthenticationStateChange(result: AuthenticationStateResult)

    fun onConnectionStateChange(result: ConnectionStateResult)
}
