package org.tvheadend.api

import org.tvheadend.htsp.HtspMessage

interface ServerMessageListener {

    fun onMessage(response: HtspMessage)
}
