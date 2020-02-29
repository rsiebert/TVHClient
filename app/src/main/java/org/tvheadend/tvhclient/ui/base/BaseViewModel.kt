package org.tvheadend.tvhclient.ui.base

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.interfaces.NetworkStatusInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity
import org.tvheadend.tvhclient.util.livedata.Event
import javax.inject.Inject

open class BaseViewModel(application: Application) : AndroidViewModel(application), SnackbarMessageInterface, NetworkStatusInterface {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    var connectionToServerAvailableLiveData = MutableLiveData<Boolean>() // TODO rename
        private set

    /**
     * Contains an intent with the snackbar message and other information.
     * The value gets set by the {@link SnackbarMessageReceiver}
     */
    var snackbarMessageLiveData = MutableLiveData<Event<Intent>>()
        private set

    /**
     * Contains the current network status.
     * The value gets set by the {@link NetworkStatusReceiver}
     */
    var networkStatusLiveData = MutableLiveData<NetworkStatus>()
        private set

    var connection: Connection
    var connectionCount: LiveData<Int>  // TODO rename
    var connectionLiveData: LiveData<Connection>

    /**
     * Contains the live data information that the application is unlocked or not
     */
    var isUnlockedLiveData = appRepository.getIsUnlockedLiveData()
        private set

    /**
     * Contains the information that the application is unlocked or not
     */
    var isUnlocked = appRepository.getIsUnlocked()
        private set

    var htspVersion: Int
    var removeFragmentWhenSearchIsDone = false

    var searchQuery = MutableLiveData("") // TODO rename
    var searchViewHasFocus = false

    val isSearchActive: Boolean
        get() = !searchQuery.value.isNullOrEmpty()

    init {
        inject()
        connection = appRepository.connectionData.activeItem
        htspVersion = appRepository.serverStatusData.activeItem.htspVersion

        networkStatusLiveData.value = NetworkStatus.NETWORK_UNKNOWN
        connectionToServerAvailableLiveData.value = false

        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem
    }

    private fun inject() {
        MainApplication.component.inject(this)
    }

    fun updateConnectionAndRestartApplication(context: Context?, isSyncRequired: Boolean = true) {
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

    fun startSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun clearSearchQuery() {
        searchQuery.value = ""
    }

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }

    override fun setNetworkStatus(status: NetworkStatus) {
        networkStatusLiveData.value = status
    }

    override fun getNetworkStatus(): NetworkStatus? = networkStatusLiveData.value

    fun setConnectionToServerAvailable(available: Boolean) {
        connectionToServerAvailableLiveData.value = available
    }
}