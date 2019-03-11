package org.tvheadend.tvhclient.ui.features.dvr.timer_recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.TimerRecording
import javax.inject.Inject

class TimerRecordingViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val recordings: LiveData<List<TimerRecording>>?
    val numberOfRecordings: LiveData<Int>

    init {
        MainApplication.getComponent().inject(this)
        recordings = appRepository.timerRecordingData.getLiveDataItems()
        numberOfRecordings = appRepository.timerRecordingData.getLiveDataItemCount()
    }

    fun getRecordingById(id: String): LiveData<TimerRecording>? {
        return appRepository.timerRecordingData.getLiveDataItemById(id)
    }

    fun getRecordingByIdSync(id: String): TimerRecording? {
        return if (!id.isEmpty()) {
            appRepository.timerRecordingData.getItemById(id)
        } else {
            TimerRecording()
        }
    }
}
