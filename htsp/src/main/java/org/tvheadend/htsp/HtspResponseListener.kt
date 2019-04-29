package org.tvheadend.htsp

interface HtspResponseListener {

    fun handleResponse(response: HtspMessage)
}