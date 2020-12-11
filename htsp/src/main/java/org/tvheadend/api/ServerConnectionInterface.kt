package org.tvheadend.api

import org.tvheadend.htsp.HtspMessage

internal interface ServerConnectionInterface {
    fun addMessageListener(listener: ServerMessageListener)
    fun removeMessageListener(listener: ServerMessageListener)

    // synchronized, non blocking connect
    fun openConnection()
    val isNotConnected: Boolean
    val isAuthenticated: Boolean

    // synchronized, blocking auth
    fun authenticate()
    fun sendMessage(message: HtspMessage)
    fun sendMessage(message: HtspMessage, listener: ServerResponseListener?)
    fun closeConnection()
}