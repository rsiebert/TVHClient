package org.tvheadend.tvhclient.ui.features

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.getNetworkStatus
import timber.log.Timber

class MainViewModel(application: Application) : BaseViewModel(application) {

    var connection = appRepository.connectionData.activeItem
    val connectionCountLiveData: LiveData<Int> = appRepository.connectionData.getLiveDataItemCount()
    val serverStatus = appRepository.serverStatusData.activeItem

    var networkStatus: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.NETWORK_UNKNOWN)
    var showSnackbar: MutableLiveData<Intent> = MutableLiveData()

    /**
     * Update the current active connection from the database in case
     * it has changed from the settings. The set the required properties
     * to trigger an initial sync after a reconnect.
     */
    fun setConnectionSyncRequired() {
        appRepository.connectionData.setSyncRequiredForActiveConnection()
    }

    fun setNetworkIsAvailable(isAvailable: Boolean) {
        Timber.d("Updating network status to $isAvailable")
        networkStatus.value = getNetworkStatus(networkStatus.value, isAvailable)
    }
}