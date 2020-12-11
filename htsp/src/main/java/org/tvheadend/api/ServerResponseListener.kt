package org.tvheadend.api

interface ServerResponseListener<T> {

    fun handleResponse(response: T)
}