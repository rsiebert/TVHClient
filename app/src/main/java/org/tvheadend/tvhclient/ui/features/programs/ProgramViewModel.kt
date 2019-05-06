package org.tvheadend.tvhclient.ui.features.programs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Program
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.SearchResultProgram
import javax.inject.Inject

class ProgramViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository

    val recordings: LiveData<List<Recording>>?

    init {
        MainApplication.getComponent().inject(this)
        recordings = appRepository.recordingData.getLiveDataItems()
    }

    fun getProgramsByChannelFromTime(channelId: Int, time: Long): LiveData<List<Program>> {
        return appRepository.programData.getLiveDataItemByChannelIdAndTime(channelId, time)
    }

    fun getProgramsFromTime(time: Long): LiveData<List<SearchResultProgram>> {
        return appRepository.programData.getLiveDataItemsFromTime(time)
    }

    fun getProgramByIdSync(eventId: Int): Program? {
        return appRepository.programData.getItemById(eventId)
    }

    fun getRecordingsByChannelId(channelId: Int): LiveData<List<Recording>> {
        return appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
    }
}
