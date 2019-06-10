package org.tvheadend.tvhclient.ui.features

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import javax.inject.Inject

class MainViewModel : ViewModel() {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository

    val connection: Connection
    val connectionCountLiveData: LiveData<Int>
    val serverStatus: ServerStatus

    var isNetworkAvailableLiveData: MutableLiveData<Boolean>
    var isNetworkAvailable: Boolean = false
        set(available) {
            Timber.d("Setting network availability (isNetworkAvailable) to $available")
            field = available
        }

    init {
        Timber.d("Initializing")
        MainApplication.component.inject(this)

        connectionCountLiveData = appRepository.connectionData.getLiveDataItemCount()
        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem
        isNetworkAvailableLiveData = appRepository.isNetworkAvailable
    }

    fun setConnectionSyncRequired() {
        Timber.d("Updating active connection to request a full sync")
        connection.isSyncRequired = true
        connection.lastUpdate = 0
        appRepository.connectionData.updateItem(connection)
    }
}