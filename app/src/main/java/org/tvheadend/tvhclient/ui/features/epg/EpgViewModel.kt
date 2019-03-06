package org.tvheadend.tvhclient.ui.features.epg

import android.app.Application
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import org.tvheadend.tvhclient.domain.entity.EpgChannel
import org.tvheadend.tvhclient.domain.entity.EpgProgram
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.ui.features.channels.BaseChannelViewModel
import timber.log.Timber

class EpgViewModel(application: Application) : BaseChannelViewModel(application) {

    val epgChannels: LiveData<List<EpgChannel>>

    var verticalScrollOffset = 0
    var verticalScrollPosition = 0

    init {
        val trigger = EpgChannelLiveData(channelSortOrder, selectedChannelTagIds)
        epgChannels = Transformations.switchMap(trigger) { value ->
            Timber.d("Loading epg channels because one of the two triggers have changed")

            val first = value.first
            val second = value.second

            if (first == null) {
                Timber.d("Skipping loading of epg channels because channel sort order is not set")
                return@switchMap null
            }
            if (second == null) {
                Timber.d("Skipping loading of epg channels because selected channel tag ids are not set")
                return@switchMap null
            }

            return@switchMap appRepository.channelData.getAllEpgChannels(first, second)
        }
    }

    fun getRecordingsByChannel(channelId: Int): LiveData<List<Recording>> {
        return appRepository.recordingData.getLiveDataItemsByChannelId(channelId)
    }

    fun getProgramsByChannelAndBetweenTimeSync(channelId: Int, startTime: Long, endTime: Long): List<EpgProgram> {
        return appRepository.programData.getItemByChannelIdAndBetweenTime(channelId, startTime, endTime)
    }

    internal inner class EpgChannelLiveData(selectedChannelSortOrder: LiveData<Int>,
                                            selectedChannelTagIds: LiveData<List<Int>?>) : MediatorLiveData<Pair<Int, List<Int>?>>() {

        init {
            addSource(selectedChannelSortOrder) { order ->
                value = Pair.create(order, selectedChannelTagIds.value)
            }
            addSource(selectedChannelTagIds) { integers ->
                value = Pair.create(selectedChannelSortOrder.value, integers)
            }
        }
    }
}
