package org.tvheadend.tvhclient.ui.features.dvr.series_recordings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.SeriesRecording
import javax.inject.Inject

class SeriesRecordingViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val recordings: LiveData<List<SeriesRecording>>?
    val numberOfRecordings: LiveData<Int>

    init {
        MainApplication.getComponent().inject(this)
        recordings = appRepository.seriesRecordingData.getLiveDataItems()
        numberOfRecordings = appRepository.seriesRecordingData.getLiveDataItemCount()
    }

    fun getRecordingById(id: String): LiveData<SeriesRecording>? {
        return appRepository.seriesRecordingData.getLiveDataItemById(id)
    }

    fun getRecordingByIdSync(id: String): SeriesRecording? {
        return if (!id.isEmpty()) {
            appRepository.seriesRecordingData.getItemById(id)
        } else {
            SeriesRecording()
        }
    }
}
