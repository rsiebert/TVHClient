package org.tvheadend.tvhclient.ui.features.playback.external

import android.os.AsyncTask
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException

internal class ConvertHostnameToAddressTask(private val hostname: String) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg voids: Void): String {
        return try {
            InetAddress.getByName(hostname).hostAddress
        } catch (e: UnknownHostException) {
            Timber.d(e, "Could not get ip address from $hostname, using hostname as fallback")
            hostname
        }
    }
}
