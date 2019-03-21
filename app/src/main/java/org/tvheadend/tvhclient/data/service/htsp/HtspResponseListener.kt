package org.tvheadend.tvhclient.data.service.htsp

interface HtspResponseListener {

    fun handleResponse(response: HtspMessage)
}