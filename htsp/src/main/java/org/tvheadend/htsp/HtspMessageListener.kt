package org.tvheadend.htsp

interface HtspMessageListener {

    fun onMessage(response: HtspMessage)
}
