package org.tvheadend.htsp

internal interface HtspConnectionInterface {
    fun addMessageListener(listener: HtspMessageListener)
    fun removeMessageListener(listener: HtspMessageListener)

    // synchronized, non blocking connect
    fun openConnection()
    val isNotConnected: Boolean
    val isAuthenticated: Boolean

    // synchronized, blocking auth
    fun authenticate()
    fun sendMessage(message: HtspMessage)
    fun sendMessage(message: HtspMessage, listener: HtspResponseListener?)
    fun closeConnection()
}