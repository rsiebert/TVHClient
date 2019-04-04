package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Recording
import javax.inject.Inject

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val completedRecordings: LiveData<List<Recording>>
    val scheduledRecordings: LiveData<List<Recording>>
    val failedRecordings: LiveData<List<Recording>>
    val removedRecordings: LiveData<List<Recording>>

    val numberOfCompletedRecordings: LiveData<Int>
    val numberOfScheduledRecordings: LiveData<Int>
    val numberOfFailedRecordings: LiveData<Int>
    val numberOfRemovedRecordings: LiveData<Int>

    var recording = Recording()
    var recordingProfileNameId: Int = 0

    init {
        MainApplication.getComponent().inject(this)

        completedRecordings = appRepository.recordingData.getLiveDataItemsByType("completed")
        scheduledRecordings = appRepository.recordingData.getLiveDataItemsByType("scheduled")
        failedRecordings = appRepository.recordingData.getLiveDataItemsByType("failed")
        removedRecordings = appRepository.recordingData.getLiveDataItemsByType("removed")

        numberOfCompletedRecordings = appRepository.recordingData.getLiveDataCountByType("completed")
        numberOfScheduledRecordings = appRepository.recordingData.getLiveDataCountByType("scheduled")
        numberOfFailedRecordings = appRepository.recordingData.getLiveDataCountByType("failed")
        numberOfRemovedRecordings = appRepository.recordingData.getLiveDataCountByType("removed")
    }

    fun getRecordingById(id: Int): LiveData<Recording>? {
        return appRepository.recordingData.getLiveDataItemById(id)
    }

    fun loadRecordingByIdSync(id: Int) {
        recording = appRepository.recordingData.getItemById(id)
    }
}
