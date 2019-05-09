package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import javax.inject.Inject

class RecordingViewModel : ViewModel() {

    @Inject
    lateinit var appContext: Context
    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val completedRecordings: LiveData<List<Recording>>
    val scheduledRecordings: LiveData<List<Recording>>
    val failedRecordings: LiveData<List<Recording>>
    val removedRecordings: LiveData<List<Recording>>

    var recording = Recording()
    var recordingProfileNameId: Int = 0

    init {
        MainApplication.getComponent().inject(this)

        completedRecordings = appRepository.recordingData.getLiveDataItemsByType("completed")
        scheduledRecordings = appRepository.recordingData.getLiveDataItemsByType("scheduled")
        failedRecordings = appRepository.recordingData.getLiveDataItemsByType("failed")
        removedRecordings = appRepository.recordingData.getLiveDataItemsByType("removed")
    }

    fun getRecordingById(id: Int): LiveData<Recording>? {
        return appRepository.recordingData.getLiveDataItemById(id)
    }

    fun loadRecordingByIdSync(id: Int) {
        recording = appRepository.recordingData.getItemById(id)
    }

    fun getChannelList(): List<Channel> {
        val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }
}
