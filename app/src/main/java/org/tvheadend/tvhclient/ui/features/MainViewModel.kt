package org.tvheadend.tvhclient.ui.features

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.features.navigation.NavigationDrawer.MENU_CHANNELS
import timber.log.Timber
import javax.inject.Inject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val navigationMenuId: MutableLiveData<Int> = MutableLiveData()

    val activeServerStatus: ServerStatus
    val activeConnection: Connection

    val connections: LiveData<List<Connection>>
    val channelCount: LiveData<Int>
    val programCount: LiveData<Int>
    val timerRecordingCount: LiveData<Int>
    val seriesRecordingCount: LiveData<Int>
    val completedRecordingCount: LiveData<Int>
    val scheduledRecordingCount: LiveData<Int>
    val failedRecordingCount: LiveData<Int>
    val removedRecordingCount: LiveData<Int>

    init {
        MainApplication.getComponent().inject(this)
        Timber.d("Initializing")
        navigationMenuId.value = MENU_CHANNELS

        activeServerStatus = appRepository.serverStatusData.activeItem
        activeConnection = appRepository.connectionData.activeItem

        connections = appRepository.connectionData.getLiveDataItems()
        channelCount = appRepository.channelData.getLiveDataItemCount()
        programCount = appRepository.programData.getLiveDataItemCount()
        timerRecordingCount = appRepository.timerRecordingData.getLiveDataItemCount()
        seriesRecordingCount = appRepository.seriesRecordingData.getLiveDataItemCount()
        completedRecordingCount = appRepository.recordingData.getLiveDataCountByType("completed")
        scheduledRecordingCount = appRepository.recordingData.getLiveDataCountByType("scheduled")
        failedRecordingCount = appRepository.recordingData.getLiveDataCountByType("failed")
        removedRecordingCount = appRepository.recordingData.getLiveDataCountByType("removed")
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
}