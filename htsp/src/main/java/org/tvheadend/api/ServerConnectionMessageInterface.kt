package org.tvheadend.api

interface ServerConnectionMessageInterface<T, M> {
    fun sendMessage(message: M)
    fun sendMessage(message: M, listener: ServerResponseListener<T>?)
}