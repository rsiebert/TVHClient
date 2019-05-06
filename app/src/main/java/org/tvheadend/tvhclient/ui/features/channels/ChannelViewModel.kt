package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import org.tvheadend.tvhclient.domain.entity.Channel
import timber.log.Timber

class ChannelViewModel(application: Application) : BaseChannelViewModel(application) {

    val channels: LiveData<List<Channel>>

    init {

        val trigger = ChannelLiveData(selectedTime, channelSortOrder, selectedChannelTagIds)
        channels = Transformations.switchMap(trigger) { value ->
            Timber.d("Loading channels because one of the three triggers have changed")

            val first = value.first
            val second = value.second
            val third = value.third

            if (first == null) {
                Timber.d("Skipping loading of channels because selected time is not set")
                return@switchMap null
            }
            if (second == null) {
                Timber.d("Skipping loading of channels because channel sort order is not set")
                return@switchMap null
            }
            if (third == null) {
                Timber.d("Skipping loading of channels because selected channel tag ids are not set")
                return@switchMap null
            }

            return@switchMap appRepository.channelData.getAllChannelsByTime(first, second, third)
        }
    }

    internal inner class ChannelLiveData(selectedTime: LiveData<Long>,
                                         selectedChannelSortOrder: LiveData<Int>,
                                         selectedChannelTagIds: LiveData<List<Int>?>) : MediatorLiveData<Triple<Long?, Int?, List<Int>?>>() {
        init {
            addSource(selectedTime) { time ->
                value = Triple(time, selectedChannelSortOrder.value, selectedChannelTagIds.value)
            }
            addSource(selectedChannelSortOrder) { order ->
                value = Triple(selectedTime.value, order, selectedChannelTagIds.value)
            }
            addSource(selectedChannelTagIds) { integers ->
                value = Triple(selectedTime.value, selectedChannelSortOrder.value, integers)
            }
        }
    }
}
