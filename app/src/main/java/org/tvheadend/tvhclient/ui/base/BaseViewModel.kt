package org.tvheadend.tvhclient.ui.base

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.util.Pair
import androidx.lifecycle.*
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.ui.common.Event
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity
import timber.log.Timber
import javax.inject.Inject

open class BaseViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    var connection: Connection
    var connectionCount: LiveData<Int>
    var connectionLiveData: LiveData<Connection>
    var connectionStatus: LiveData<Pair<Int, Boolean>>

    var connectionToServerAvailable: LiveData<Boolean>
    var networkStatus: LiveData<NetworkStatus>
    var showSnackbar: LiveData<Event<Intent>>
    var isUnlocked: LiveData<Boolean>
    var htspVersion: Int

    init {
        inject()
        connection = appRepository.connectionData.activeItem
        networkStatus = appRepository.getNetworkStatus()
        showSnackbar = appRepository.getSnackbarMessage()
        isUnlocked = appRepository.getIsUnlocked()
        htspVersion = appRepository.serverStatusData.activeItem.htspVersion

        connectionToServerAvailable = appRepository.getConnectionToServerAvailable()

        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem

        connectionStatus = Transformations.switchMap(ConnectionStatusLiveData(connectionCount, connectionLiveData)) { value ->
            val count = value.first ?: 0
            val connection = value.second
            return@switchMap MutableLiveData(Pair(count, connection != null))
        }
    }

    internal inner class ConnectionStatusLiveData(connectionCount: LiveData<Int>,
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

    fun setConnectionToServerIsAvailable(isAvailable: Boolean) {
        Timber.d("Updating connection to server is available to $isAvailable")
        appRepository.setConnectionToServerAvailable(isAvailable)
    }

    fun updateConnectionAndRestartApplication(context: Context?, isSyncRequired: Boolean = true) {
        Timber.d("Restart of application requested")
        context?.let {
            if (isSyncRequired) {
                appRepository.connectionData.setSyncRequiredForActiveConnection()
            }
            context.stopService(Intent(context, HtspService::class.java))
            val intent = Intent(context, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}