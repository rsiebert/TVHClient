package org.tvheadend.tvhclient.ui.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.*
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer.Companion.MENU_CHANNELS
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val navigationMenuId: MutableLiveData<Int> = MutableLiveData()

    val activeServerStatus: ServerStatus
    val activeConnection: Connection

    val connections: LiveData<List<Connection>>
    val connectionCount: LiveData<Int>
    val channelCount: LiveData<Int>
    val programCount: LiveData<Int>
    val timerRecordingCount: LiveData<Int>
    val seriesRecordingCount: LiveData<Int>
    val completedRecordingCount: LiveData<Int>
    val scheduledRecordingCount: LiveData<Int>
    val failedRecordingCount: LiveData<Int>
    val removedRecordingCount: LiveData<Int>

    val subscriptions: LiveData<List<Subscription>>
    val inputs: LiveData<List<Input>>

    private val channelSortOrder: Int

    init {
        MainApplication.getComponent().inject(this)
        navigationMenuId.value = MENU_CHANNELS

        // Init the active connection and server status that is needed throughout the app
        activeServerStatus = appRepository.serverStatusData.activeItem
        activeConnection = appRepository.connectionData.activeItem
        connections = appRepository.connectionData.getLiveDataItems()

        // Initialize the item counts which are required by the navigation drawer and status screen
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        channelCount = appRepository.channelData.getLiveDataItemCount()
        programCount = appRepository.programData.getLiveDataItemCount()
        timerRecordingCount = appRepository.timerRecordingData.getLiveDataItemCount()
        seriesRecordingCount = appRepository.seriesRecordingData.getLiveDataItemCount()
        completedRecordingCount = appRepository.recordingData.getLiveDataCountByType("completed")
        scheduledRecordingCount = appRepository.recordingData.getLiveDataCountByType("scheduled")
        failedRecordingCount = appRepository.recordingData.getLiveDataCountByType("failed")
        removedRecordingCount = appRepository.recordingData.getLiveDataCountByType("removed")

        // These are required by the status screen
        subscriptions = appRepository.subscriptionData.getLiveDataItems()
        inputs = appRepository.inputData.getLiveDataItems()

        val defaultChannelSortOrder = application.resources.getString(R.string.pref_default_channel_sort_order)
        channelSortOrder = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(application).getString("channel_sort_order", defaultChannelSortOrder)
                ?: defaultChannelSortOrder)
    }

    fun setSelectedNavigationMenuId(id: Int) {
        navigationMenuId.value = id
    }

    fun updateConnection(id: Int): Boolean {
        val connection = appRepository.connectionData.getItemById(id)
        return if (connection != null) {
            connection.isActive = true
            appRepository.connectionData.updateItem(connection)
            true
        } else {
            false
        }
    }

    fun getChannelList(): List<Channel> {
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(activeServerStatus.recordingServerProfileId)
    }

    fun getRecordingById(id: Int): Recording? {
        return appRepository.recordingData.getItemByEventId(id)
    }

    fun getProgramById(id: Int): Program? {
        return appRepository.programData.getItemById(id)
    }

    fun getChannelById(id: Int): Channel? {
        return appRepository.channelData.getItemById(id)
    }
}