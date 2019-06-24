package org.tvheadend.tvhclient.ui.features

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.getNetworkStatus
import timber.log.Timber

class MainViewModel(application: Application) : BaseViewModel(application) {

    val connection: Connection = appRepository.connectionData.activeItem
    val connectionCountLiveData: LiveData<Int> = appRepository.connectionData.getLiveDataItemCount()
    val serverStatus: ServerStatus = appRepository.serverStatusData.activeItem

    var networkStatus: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.NETWORK_UNKNOWN)
    var showSnackbar: MutableLiveData<Intent> = MutableLiveData()

    fun setConnectionSyncRequired() {
        Timber.d("Updating active connection to request a full sync")
        connection.isSyncRequired = true
        connection.lastUpdate = 0
        appRepository.connectionData.updateItem(connection)
    }

    fun setNetworkIsAvailable(isAvailable: Boolean) {
        Timber.d("Updating network status to $isAvailable")
        networkStatus.value = getNetworkStatus(networkStatus.value, isAvailable)
    }
}