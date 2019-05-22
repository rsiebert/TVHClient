package org.tvheadend.tvhclient.ui.features

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import javax.inject.Inject

class MainViewModel : ViewModel() {

    @Inject
    lateinit var appRepository: AppRepository

    val connection: Connection
    val serverStatus: ServerStatus
    val connections: LiveData<List<Connection>>
    val connectionCount: LiveData<Int>

    init {
        MainApplication.component.inject(this)
        connections = appRepository.connectionData.getLiveDataItems()
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem
    }

    fun setConnectionSyncRequired() {
        Timber.d("Updating active connection to request a full sync")
        connection.isSyncRequired = true
        connection.lastUpdate = 0
        appRepository.connectionData.updateItem(connection)
    }
}