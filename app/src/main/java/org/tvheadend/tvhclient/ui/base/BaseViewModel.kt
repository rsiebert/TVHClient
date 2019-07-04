package org.tvheadend.tvhclient.ui.base

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.getNetworkStatus
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

    var connectionCount: LiveData<Int>
    // TODO make this live data
    var connection: Connection
    // TODO make this live data
    val serverStatus: ServerStatus

    var connectionToServerAvailable = MutableLiveData(false)
    var networkStatus: MutableLiveData<NetworkStatus>
    var showSnackbar: LiveData<Intent>
    var isUnlocked: LiveData<Boolean>

    init {
        inject()
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connection = appRepository.connectionData.activeItem
        serverStatus = appRepository.serverStatusData.activeItem
        networkStatus = appRepository.networkStatus
        showSnackbar = appRepository.snackbarMessage
        isUnlocked = appRepository.isUnlocked
    }

    private fun inject() {
        MainApplication.component.inject(this)
    }

    fun setNetworkIsAvailable(isAvailable: Boolean) {
        Timber.d("Updating network status to $isAvailable")
        networkStatus.value = getNetworkStatus(networkStatus.value, isAvailable)
    }

    fun setConnectionToServerIsAvailable(isAvailable: Boolean) {
        Timber.d("Updating connection to server is available to $isAvailable")
        connectionToServerAvailable.value = isAvailable
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