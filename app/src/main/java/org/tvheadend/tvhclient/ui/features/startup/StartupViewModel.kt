package org.tvheadend.tvhclient.ui.features.startup

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.util.Pair
import androidx.lifecycle.*
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Connection
import timber.log.Timber
import javax.inject.Inject

open class StartupViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private var connectionCount: LiveData<Int>
    private var connectionLiveData: LiveData<Connection>
    var connectionStatus: LiveData<Pair<Int, Boolean>>

    init {
        inject()
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem

        connectionStatus = Transformations.switchMap(ConnectionStatusLiveData(connectionCount, connectionLiveData)) { value ->
            val count = value.first ?: 0
            val connection = value.second
            return@switchMap MutableLiveData(Pair(count, connection?.isActive ?: false))
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