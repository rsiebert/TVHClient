package org.tvheadend.tvhclient.data.service.htsp

interface HtspMessageListener {

    fun onMessage(response: HtspMessage)
}
