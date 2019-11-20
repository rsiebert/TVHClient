package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.tvhclient.R
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.common.Event
import timber.log.Timber

class SettingsViewModel(application: Application) : BaseViewModel(application) {

    var connectionIdToBeEdited: Int = -1
    val allConnections: LiveData<List<Connection>> = appRepository.connectionData.getLiveDataItems()
    var currentServerStatus = appRepository.serverStatusData.activeItem
    val currentServerStatusLiveData = appRepository.serverStatusData.liveDataActiveItem
    private val navigationMenuId = MutableLiveData<Event<String>>()

    init {
        navigationMenuId.value = Event("default")
    }

    fun getNavigationMenuId(): LiveData<Event<String>> = navigationMenuId

    fun setNavigationMenuId(id: String) {
        Timber.d("Received new navigation id $id")
        navigationMenuId.value = Event(id)
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
}
