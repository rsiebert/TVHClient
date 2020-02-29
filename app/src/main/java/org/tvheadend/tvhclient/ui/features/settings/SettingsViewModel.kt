package org.tvheadend.tvhclient.ui.features.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.service.HtspService
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.ui.features.startup.SplashActivity
import org.tvheadend.tvhclient.util.livedata.Event
import timber.log.Timber
import javax.inject.Inject

class SettingsViewModel : ViewModel(), SnackbarMessageInterface {

    @Inject
    lateinit var appContext: Context // TODO rename
    @Inject
    lateinit var appRepository: AppRepository // TODO rename
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    var connection: Connection
    var connectionIdToBeEdited: Int = -1

    /**
     * The number of available connections as live data
     */
    var connectionCount: LiveData<Int>  // TODO rename

    /**
     * Currently active connection as live data
     */
    var connectionLiveData:  LiveData<Connection>

    /**
     * Contains the list of all available connections as live data
     */
    var allConnectionsLiveData: LiveData<List<Connection>>

    /**
     *  The active server status. Contains some server information
     *  and the selected playback and recording profile ids
     */
    var currentServerStatus: ServerStatus

    /**
     *  Live data status of the active server status. Observing this is required to get updated
     *  about any changes to the profile name or ids e.g. in case the database has been cleared.
     */
    var currentServerStatusLiveData: LiveData<ServerStatus>

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

    /**
     * Contains a string with the name of the fragment that shall be shown
     */
    private val navigationMenuIdLiveData = MutableLiveData<Event<String>>(Event("default"))

    /**
     * Contains an intent with the snackbar message and other information.
     * The value gets set by the {@link SnackbarMessageReceiver}
     */
    var snackbarMessageLiveData = MutableLiveData<Event<Intent>>()
        private set

    init {
        inject()
        connection = appRepository.connectionData.activeItem
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        connectionLiveData = appRepository.connectionData.liveDataActiveItem
        allConnectionsLiveData = appRepository.connectionData.getLiveDataItems()
        currentServerStatus = appRepository.serverStatusData.activeItem
        currentServerStatusLiveData = appRepository.serverStatusData.liveDataActiveItem
    }

    private fun inject() {
        MainApplication.component.inject(this)
    }

    fun getNavigationMenuId(): LiveData<Event<String>> = navigationMenuIdLiveData

    fun setNavigationMenuId(id: String) {
        Timber.d("Received new navigation id $id")
        navigationMenuIdLiveData.value = Event(id)
    }

    fun getChannelList(): List<Channel> {
        val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder)
                ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    /**
     * Updates the connection with the information that a new sync is required.
     */
    fun setSyncRequiredForActiveConnection() {
        Timber.d("Updating active connection to request a full sync")
        appRepository.connectionData.setSyncRequiredForActiveConnection()
    }

    /**
     * Clear the database contents, when done the callback
     * is triggered which will restart the application
     */
    fun clearDatabase(callback: MiscDataSource.DatabaseClearedCallback) {
        appRepository.miscData.clearDatabase(callback)
    }

    fun updateServerStatus(serverStatus: ServerStatus) {
        appRepository.serverStatusData.updateItem(serverStatus)
    }

    fun getHtspProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(currentServerStatus.htspPlaybackServerProfileId)
    }

    fun getHtspProfiles(): List<ServerProfile> {
        val profiles = appRepository.serverProfileData.htspPlaybackProfiles
        Timber.d("Loaded ${profiles.size} Htsp profiles")
        return profiles
    }

    fun getHttpProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(currentServerStatus.httpPlaybackServerProfileId)
    }

    fun getHttpProfiles(): List<ServerProfile> {
        val profiles = appRepository.serverProfileData.httpPlaybackProfiles
        Timber.d("Loaded ${profiles.size} Http profiles")
        return profiles
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(currentServerStatus.recordingServerProfileId)
    }

    fun getRecordingProfiles(): List<ServerProfile> {
        val profiles = appRepository.serverProfileData.recordingProfiles
        Timber.d("Loaded ${profiles.size} recording profiles")
        return profiles
    }

    fun getCastingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(currentServerStatus.castingServerProfileId)
    }

    fun addConnection() {
        appRepository.connectionData.addItem(connection)
    }

    fun loadConnectionById(id: Int) {
        connection = appRepository.connectionData.getItemById(id) ?: Connection()
    }

    fun createNewConnection() {
        connection = Connection()
    }

    fun updateConnection(connection: Connection) {
        appRepository.connectionData.updateItem(connection)
    }

    fun updateConnection() {
        appRepository.connectionData.updateItem(connection)
    }

    fun removeConnection(connection: Connection) {
        appRepository.connectionData.removeItem(connection)
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

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }
}
