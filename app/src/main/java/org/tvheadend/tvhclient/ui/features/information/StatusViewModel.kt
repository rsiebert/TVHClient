package org.tvheadend.tvhclient.ui.features.information

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.domain.entity.Input
import org.tvheadend.tvhclient.domain.entity.Subscription
import timber.log.Timber
import javax.inject.Inject

class StatusViewModel : ViewModel() {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val connection: Connection
    val connectionCount: LiveData<Int>
    val channelCount: LiveData<Int>
    val programCount: LiveData<Int>
    val timerRecordingCount: LiveData<Int>
    val seriesRecordingCount: LiveData<Int>
    val completedRecordingCount: LiveData<Int>
    val scheduledRecordingCount: LiveData<Int>
    val runningRecordingCount: LiveData<Int>
    val failedRecordingCount: LiveData<Int>
    val removedRecordingCount: LiveData<Int>

    val subscriptions: LiveData<List<Subscription>>
    val inputs: LiveData<List<Input>>

    init {
        Timber.d("Initializing")
        MainApplication.getComponent().inject(this)

        connection = appRepository.connectionData.activeItem
        connectionCount = appRepository.connectionData.getLiveDataItemCount()
        channelCount = appRepository.channelData.getLiveDataItemCount()
        programCount = appRepository.programData.getLiveDataItemCount()
        timerRecordingCount = appRepository.timerRecordingData.getLiveDataItemCount()
        seriesRecordingCount = appRepository.seriesRecordingData.getLiveDataItemCount()
        completedRecordingCount = appRepository.recordingData.getLiveDataCountByType("completed")
        scheduledRecordingCount = appRepository.recordingData.getLiveDataCountByType("scheduled")
        runningRecordingCount = appRepository.recordingData.getLiveDataCountByType("running")
        failedRecordingCount = appRepository.recordingData.getLiveDataCountByType("failed")
        removedRecordingCount = appRepository.recordingData.getLiveDataCountByType("removed")

        subscriptions = appRepository.subscriptionData.getLiveDataItems()
        inputs = appRepository.inputData.getLiveDataItems()
    }

    fun getChannelById(id: Int): Channel? {
        return appRepository.channelData.getItemById(id)
    }
}