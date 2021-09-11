package org.tvheadend.tvhclient.ui.features.playback.external

import kotlinx.coroutines.CoroutineScope
import org.tvheadend.tvhclient.util.extensions.executeAsyncTask
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException

class ConvertHostnameToAddressTask(lifecycleScope: CoroutineScope, private val hostname: String) {

    private var convertedHostname = hostname
    init {
        lifecycleScope.executeAsyncTask(onPreExecute = {
            // ... runs in Main Thread
            Timber.d("onPreExecute")
        }, doInBackground = {
            Timber.d("doInBackground")
            try {
                convertedHostname = InetAddress.getByName(hostname).hostAddress
            } catch (e: UnknownHostException) {
                Timber.d(e, "Could not get ip address from $hostname, using hostname as fallback")
            }
        }, onPostExecute = {
            // runs in Main Thread
            Timber.d("onPostExecute")
            convertedHostname
        })
    }
}
