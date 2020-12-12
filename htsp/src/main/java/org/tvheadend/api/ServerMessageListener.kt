package org.tvheadend.api

interface ServerMessageListener<T> {

    fun onMessage(response: T)
}
