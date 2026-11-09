package org.tvheadend.tvhclient.ui.features.startup

import androidx.core.util.Pair
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.MainApplication

open class StartupViewModel : ViewModel() {

    lateinit var appRepository: AppRepository

    private var connectionCount: LiveData<Int>
    private var connectionLiveData: LiveData<Connection>
    var connectionStatus: LiveData<Pair<Int, Boolean>>

    init {
        appRepository = AppGraphProvider.require().appRepository
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem

        val statusLiveData = ConnectionStatusLiveData(connectionCount, connectionLiveData).switchMap { value ->
            val count = value.first ?: 0
            val connection = value.second
            return@switchMap MutableLiveData(Pair(count, connection?.isActive ?: false))
        }

        // On a real device the database migration runs off the main thread in MainApplication. The
        // startup screen must not report any status - and thereby let the UI query the database -
        // before that migration has finished, so gate the status on the migration job. Tests use a
        // plain [org.tvheadend.tvhclient.AppGraph] (not [MainApplication]) and skip the gate.
        val migrationJob = (AppGraphProvider.require() as? MainApplication)?.migrationJob
        connectionStatus = if (migrationJob == null) {
            statusLiveData
        } else {
            val migrationComplete = MutableLiveData(false)
            viewModelScope.launch {
                migrationJob.join()
                migrationComplete.value = true
            }
            migrationComplete.switchMap { complete ->
                if (complete) statusLiveData else MutableLiveData<Pair<Int, Boolean>>()
            }
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

}