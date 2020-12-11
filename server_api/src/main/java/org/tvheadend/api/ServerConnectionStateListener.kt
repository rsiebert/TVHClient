package org.tvheadend.api

interface ServerConnectionStateListener {

    fun onAuthenticationStateChange(result: AuthenticationStateResult)

    fun onConnectionStateChange(result: ConnectionStateResult)
}
