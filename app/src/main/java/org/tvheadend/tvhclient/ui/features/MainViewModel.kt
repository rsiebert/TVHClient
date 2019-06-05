package org.tvheadend.tvhclient.ui.features

import android.content.Context
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.common.callbacks.NetworkStatusListener
import org.tvheadend.tvhclient.ui.common.network.NetworkStatusReceiver
import timber.log.Timber
import javax.inject.Inject

class MainViewModel : ViewModel(), NetworkStatusListener {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository

    val connection: Connection
    val serverStatus: ServerStatus

    val connectionCountLiveData: LiveData<Int>
    val isNetworkAvailableLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

    var isNetworkAvailable: Boolean = false
    private var networkStatusReceiver: NetworkStatusReceiver

    init {
        Timber.d("Initializing")
        MainApplication.component.inject(this)

        connectionCountLiveData = appRepository.connectionData.getLiveDataItemCount()
        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem

        networkStatusReceiver = NetworkStatusReceiver(this)
        appContext.registerReceiver(networkStatusReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    override fun onCleared() {
        appContext.unregisterReceiver(networkStatusReceiver)
        super.onCleared()
    }

    fun setConnectionSyncRequired() {
        Timber.d("Updating active connection to request a full sync")
        connection.isSyncRequired = true
        connection.lastUpdate = 0
        appRepository.connectionData.updateItem(connection)
    }

    override fun onNetworkStatusChanged(isAvailable: Boolean) {
        Timber.d("Network status changed, network is available $isAvailable")
        isNetworkAvailableLiveData.postValue(isAvailable)
    }
}