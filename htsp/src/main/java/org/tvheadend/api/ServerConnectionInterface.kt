package org.tvheadend.api

internal interface ServerConnectionInterface<T> {
    fun addMessageListener(listener: ServerMessageListener<T>)
    fun removeMessageListener(listener: ServerMessageListener<T>)

    // synchronized, non blocking connect
    fun openConnection()
    val isNotConnected: Boolean
    val isAuthenticated: Boolean

    // synchronized, blocking auth
    fun authenticate()
    fun sendMessage(message: T)
    fun sendMessage(message: T, listener: ServerResponseListener<T>?)
    fun closeConnection()
}