package org.tvheadend.api

import org.tvheadend.htsp.HtspMessage

interface ServerResponseListener {

    fun handleResponse(response: HtspMessage)
}