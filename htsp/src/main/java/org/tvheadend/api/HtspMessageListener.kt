package org.tvheadend.api

import org.tvheadend.htsp.HtspMessage

interface HtspMessageListener {

    fun onMessage(response: HtspMessage)
}
