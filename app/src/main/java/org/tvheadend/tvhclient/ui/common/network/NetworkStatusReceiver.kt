package org.tvheadend.tvhclient.ui.common.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import timber.log.Timber

class NetworkStatusReceiver() : BroadcastReceiver() {

    val isNetworkAvailable: MutableLiveData<Boolean> = MutableLiveData()

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Network availability changed to ${isConnectionAvailable(context)}")
        isNetworkAvailable.postValue(isConnectionAvailable(context))
    }
}
