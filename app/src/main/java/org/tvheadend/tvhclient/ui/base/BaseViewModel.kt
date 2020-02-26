package org.tvheadend.tvhclient.ui.base

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.repository.AppRepository
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.util.livedata.Event
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

    var connectionToServerAvailable: LiveData<Boolean>
    var networkStatus: LiveData<NetworkStatus>
    var showSnackbar: LiveData<Event<Intent>>
    var isUnlocked: LiveData<Boolean>
    var htspVersion: Int
    var removeFragmentWhenSearchIsDone = false

    var searchQuery = MutableLiveData("")
    var searchViewHasFocus = false

    val isSearchActive: Boolean
        get() = !searchQuery.value.isNullOrEmpty()

    init {
        inject()
        connection = appRepository.connectionData.activeItem
        networkStatus = appRepository.getNetworkStatus()
        showSnackbar = appRepository.getSnackbarMessage()
        isUnlocked = appRepository.getIsUnlockedLiveData()
        htspVersion = appRepository.serverStatusData.activeItem.htspVersion

        connectionToServerAvailable = appRepository.getConnectionToServerAvailable()

        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem
    }

    private fun inject() {
        MainApplication.component.inject(this)
    }

    fun setConnectionToServerIsAvailable(isAvailable: Boolean) {
        Timber.d("Updating connection to server is available to $isAvailable")
        appRepository.setConnectionToServerAvailable(isAvailable)
    }

    fun updateConnectionAndRestartApplication(context: Context?, isSyncRequired: Boolean = true) {
        appRepository.updateConnectionAndRestartApplication(context, isSyncRequired)
    }

    fun startSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun clearSearchQuery() {
        searchQuery.value = ""
    }
}