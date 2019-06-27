package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber

class SettingsViewModel(application: Application) : BaseViewModel(application) {

    val allConnections: LiveData<List<Connection>> = appRepository.connectionData.getLiveDataItems()
    val serverStatusLiveData: LiveData<ServerStatus> = appRepository.serverStatusData.liveDataActiveItem
    var connection = appRepository.connectionData.activeItem
    var isUnlocked = appRepository.isUnlocked

    val activeConnectionId: Int
        get() {
            return appRepository.connectionData.activeItem.id
        }

    var connectionHasChanged: Boolean = false

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
    fun clearDatabase(callback: DatabaseClearedCallback) {
        appRepository.miscData.clearDatabase(callback)
    }

    fun updateServerStatus(serverStatus: ServerStatus) {
        appRepository.serverStatusData.updateItem(serverStatus)
    }

    fun getProfileById(id: Int): ServerProfile? {
        return appRepository.serverProfileData.getItemById(id)
    }

    fun getHtspProfiles(): List<ServerProfile> {
        return appRepository.serverProfileData.htspPlaybackProfiles
    }

    fun getHttpProfiles(): List<ServerProfile> {
        return appRepository.serverProfileData.httpPlaybackProfiles
    }

    fun getRecordingProfiles(): List<ServerProfile> {
        return appRepository.serverProfileData.recordingProfiles
    }

    fun addConnection() {
        appRepository.connectionData.addItem(connection)
        // Save the information in the view model that a new connection is active.
        // This will then trigger a reconnect when the user leaves the connection list screen
        if (connection.isActive) {
            connectionHasChanged = true
        }
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
