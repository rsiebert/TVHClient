package org.tvheadend.tvhclient.ui.features.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import javax.inject.Inject

class SettingsViewModel : ViewModel() {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val allConnections: LiveData<List<Connection>>
    val serverStatus: ServerStatus
    val connection: Connection

    val activeConnectionId: Int
        get() {
            return appRepository.connectionData.activeItem.id
        }

    var connectionHasChanged: Boolean
        get() = sharedPreferences.getBoolean("connection_value_changed", false)
        set(change) = sharedPreferences.edit().putBoolean("connection_value_changed", change).apply()

    init {
        MainApplication.component.inject(this)
        allConnections = appRepository.connectionData.getLiveDataItems()
        serverStatus = appRepository.serverStatusData.activeItem
        connection = appRepository.connectionData.activeItem
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
        val connection = appRepository.connectionData.activeItem
        if (connection.id >= 0) {
            connection.isSyncRequired = true
            connection.lastUpdate = 0
            updateConnection(connection)
        }
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

    fun getConnectionById(id: Int): Connection {
        return appRepository.connectionData.getItemById(id) ?: Connection()
    }

    fun addConnection(connection: Connection) {
        appRepository.connectionData.addItem(connection)
    }

    fun updateConnection(connection: Connection) {
        appRepository.connectionData.updateItem(connection)
    }

    fun removeConnection(connection: Connection) {
        appRepository.connectionData.removeItem(connection)
    }
}
