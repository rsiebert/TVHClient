package org.tvheadend.tvhclient.ui.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Connection
import org.tvheadend.tvhclient.AppGraphProvider
import org.tvheadend.tvhclient.MainRepository
import org.tvheadend.tvhclient.MainRepository.Companion.UNLOCKER
import org.tvheadend.tvhclient.service.ConnectionService
import org.tvheadend.tvhclient.ui.common.NetworkStatus
import org.tvheadend.tvhclient.ui.common.interfaces.NetworkStatusInterface
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.util.billing.BillingFlowResult
import org.tvheadend.tvhclient.util.livedata.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

open class BaseViewModel(application: Application) : AndroidViewModel(application), SnackbarMessageInterface, NetworkStatusInterface {

    lateinit var appRepository: AppRepository

    private var mainRepository: MainRepository

    var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)

    var startupCompleteLiveData = MutableLiveData<Event<Boolean>>()
        private set

    var connectionToServerAvailableLiveData = MutableLiveData<Boolean>()
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
    var networkStatusLiveData = MutableLiveData<Event<NetworkStatus>>()
        private set

    var connection: Connection

    /**
     * The active connection once it has been loaded from the database. The [connection] field is
     * seeded with a default and updated when this LiveData emits, so the blocking read no longer
     * has to run on the main thread during [init].
     */
    val activeConnectionLiveData = MutableLiveData<Connection>()

    /**
     * The htsp version of the active server status once it has been loaded from the database.
     * Mirrors [htspVersion], which is seeded with a default until this emits.
     */
    val htspVersionLiveData = MutableLiveData<Int>()

    /**
     * Contains the live data information that the application is unlocked or not
     */
    var isUnlockedLiveData: LiveData<Boolean>
        private set

    /**
     * Contains the information that the application is unlocked or not
     */
    var isUnlocked = false

    var htspVersion: Int
    var removeFragmentWhenSearchIsDone = false

    var searchQueryLiveData = MutableLiveData("")
    var searchViewHasFocus = false

    val isSearchActive: Boolean
        get() = !searchQueryLiveData.value.isNullOrEmpty()

    val messages: LiveData<Int>
        get() = mainRepository.messages.asLiveData()

    val billingLifecycleObserver: LifecycleObserver
        get() = mainRepository.billingLifecycleObserver

    /**
     * Reports the outcome of every purchase flow, so a screen can tell the user what happened
     * instead of leaving them in front of a sheet that closed for an unknown reason.
     */
    val billingFlowResults: Flow<BillingFlowResult>
        get() = mainRepository.billingFlowResults

    /**
     * Starts the purchase flow for the given SKU. The activity is only passed on to the billing
     * library, which needs it to show the Play purchase sheet.
     */
    fun buySku(activity: Activity, sku: String) {
        mainRepository.buySku(activity, sku)
    }

    init {
        val graph = AppGraphProvider.require()
        appRepository = graph.appRepository
        startupCompleteLiveData.value = Event(false)

        mainRepository = graph.mainRepository

        Timber.d("Observing isUnlockedLiveData from main repository")
        isUnlockedLiveData = mainRepository.isPurchased(UNLOCKER).asLiveData()

        // Seed with defaults; the database reads happen off the main thread below and publish
        // their results through the LiveData, which consumers observe.
        connection = Connection().also { it.id = -1 }
        htspVersion = 13

        connectionToServerAvailableLiveData.value = false

        networkStatusLiveData.value = Event(NetworkStatus.NETWORK_UNKNOWN)

        // Follow the active connection and the server status in the database. Doing this through
        // the database LiveData keeps every observer up to date, also when the user switches
        // servers without the activity being restarted.
        viewModelScope.launch {
            appRepository.connectionData.liveDataActiveItem.asFlow().collect { loadedConnection ->
                connection = loadedConnection
                activeConnectionLiveData.value = loadedConnection
            }
        }
        viewModelScope.launch {
            appRepository.serverStatusData.liveDataActiveItem.asFlow().collect { loadedServerStatus ->
                htspVersion = loadedServerStatus.htspVersion
                htspVersionLiveData.value = loadedServerStatus.htspVersion
            }
        }
    }

    /**
     * Reconnects to the currently active server without recreating the activity.
     *
     * The connection service is rebound to the active connection, so switching servers or forcing
     * a reconnect keeps the running application (and its navigation state) alive instead of
     * restarting it with a [Intent.FLAG_ACTIVITY_CLEAR_TASK] relaunch.
     */
    fun updateConnectionAndRebind(context: Context?, isSyncRequired: Boolean = true) {
        val ctx = context ?: return
        viewModelScope.launch {
            if (isSyncRequired) {
                appRepository.connectionData.setSyncRequiredForActiveConnection()
            }
            // Rebinding the service in place tears down the old HTSP connection and opens one
            // against the active connection, so no activity restart is needed.
            val intent = Intent(ctx, ConnectionService::class.java)
            intent.action = ConnectionService.ACTION_REBIND
            ctx.startService(intent)
        }
    }

    fun startSearchQuery(query: String) {
        searchQueryLiveData.value = query
    }

    fun clearSearchQuery() {
        searchQueryLiveData.value = ""
    }

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }

    override fun setNetworkStatus(status: NetworkStatus) {
        networkStatusLiveData.value = Event(status)
    }

    override fun getNetworkStatus(): NetworkStatus? = networkStatusLiveData.value?.peekContent()

    fun setConnectionToServerAvailable(available: Boolean) {
        connectionToServerAvailableLiveData.value = available
    }

    fun setStartupComplete(isComplete: Boolean) {
        startupCompleteLiveData.value = Event(isComplete)
    }
}