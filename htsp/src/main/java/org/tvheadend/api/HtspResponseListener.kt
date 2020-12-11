package org.tvheadend.api

import org.tvheadend.htsp.HtspMessage

interface HtspResponseListener {

    fun handleResponse(response: HtspMessage)
}