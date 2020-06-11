package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.util.livedata.Event
import timber.log.Timber
import javax.inject.Inject

class SettingsViewModel(application: Application) : AndroidViewModel(application), SnackbarMessageInterface {

    @Inject
    lateinit var appRepository: AppRepository

    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)

    /**
     * The currently active connection. It is also used to hold the current
     * data when a new connection is added or an existing is edited
     */
    var connectionToEdit: Connection

    /**
     * Contains the id of the connection that shall be edited otherwise -1.
     */
    var connectionIdToBeEdited: Int = -1

    /**
     * The number of available connections as live data
     */
    var connectionCountLiveData: LiveData<Int>

    /**
     * Currently active connection as live data
     */
    var activeConnectionLiveData: LiveData<Connection>

    /**
     * Contains the list of all available connections as live data
     */
    var connectionListLiveData: LiveData<List<Connection>>

    /**
     *  Contains the currently active server information like the selected playback and recording profile ids or the name and disc space information.
     *  This variable should be updated whenever the @{link currentServerStatusLiveData} variable changes to have the latest values
     */
    var currentServerStatus: ServerStatus

    /**
     *  Live data status of the active server status. The activity need to observe it so that whenever the user changes
     *  the profile or clears the database the {@see currentServerStatus} variable can be updated.
     *  In this way the other setting screens will always have access to the latest values.
     */
    var currentServerStatusLiveData: LiveData<ServerStatus>

    /**
     * Contains the live data information if the application is unlocked or not.
     */
    var isUnlockedLiveData: LiveData<Boolean>
        private set

    /**
     * Contains the information if the application is unlocked or not
     */
    var isUnlocked = false
        private set

    /**
     * Contains a string with the name of the fragment that shall be shown
     */
    private val navigationMenuIdLiveData = MutableLiveData(Event("default"))

    /**
     * Contains an intent with the snackbar message and other information.
     * The value gets set by the {@link SnackbarMessageReceiver}
     */
    var snackbarMessageLiveData = MutableLiveData<Event<Intent>>()
        private set

    init {
        inject()
        isUnlocked = appRepository.getIsUnlocked()
        isUnlockedLiveData = appRepository.getIsUnlockedLiveData()
        connectionToEdit = appRepository.connectionData.activeItem
        activeConnectionLiveData = appRepository.connectionData.liveDataActiveItem
        connectionCountLiveData = appRepository.connectionData.getLiveDataItemCount()
        connectionListLiveData = appRepository.connectionData.getLiveDataItems()
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

    fun getSeriesRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(currentServerStatus.seriesRecordingServerProfileId)
    }

    fun getTimerRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(currentServerStatus.timerRecordingServerProfileId)
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
        appRepository.connectionData.addItem(connectionToEdit)
    }

    fun loadConnectionById(id: Int) {
        connectionToEdit = appRepository.connectionData.getItemById(id) ?: Connection()
    }

    fun updateConnection(connection: Connection) {
        appRepository.connectionData.updateItem(connection)
    }

    fun updateConnection() {
        appRepository.connectionData.updateItem(connectionToEdit)
    }

    fun removeConnection(connection: Connection) {
        appRepository.connectionData.removeItem(connection)
    }

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }
}
