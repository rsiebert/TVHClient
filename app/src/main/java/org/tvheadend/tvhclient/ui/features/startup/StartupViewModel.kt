package org.tvheadend.tvhclient.ui.features.startup

import androidx.core.util.Pair
import androidx.lifecycle.*
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import javax.inject.Inject

open class StartupViewModel : ViewModel() {

    @Inject
    lateinit var appRepository: AppRepository

    private var connectionCount: LiveData<Int>
    private var connectionLiveData: LiveData<Connection>
    var connectionStatus: LiveData<Pair<Int, Boolean>>

    init {
        inject()
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem

        connectionStatus = ConnectionStatusLiveData(connectionCount, connectionLiveData).switchMap { value ->
            val count = value.first ?: 0
            val connection = value.second
            return@switchMap MutableLiveData(Pair(count, connection?.isActive ?: false))
        }
    }

    internal class ConnectionStatusLiveData(connectionCount: LiveData<Int>,
                                                  activeConnection: LiveData<Connection>) : MediatorLiveData<Pair<Int, Connection>>() {
        init {
            addSource(connectionCount) { count ->
                value = Pair.create(count, activeConnection.value)
            }
            addSource(activeConnection) { connection ->
                value = Pair.create(connectionCount.value, connection)
            }
        }
    }

    private fun inject() {
        MainApplication.component.inject(this)
    }
}