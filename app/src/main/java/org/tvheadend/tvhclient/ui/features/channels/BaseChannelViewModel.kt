package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.ChannelTag
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import timber.log.Timber
import java.util.*
import javax.inject.Inject

open class BaseChannelViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    val channelTags: LiveData<List<ChannelTag>>
    val allRecordings: LiveData<List<Recording>>
    val serverStatus: LiveData<ServerStatus>
    val selectedChannelTagIds: LiveData<List<Int>?>

    val selectedTime = MutableLiveData<Long>()
    val channelSortOrder = MutableLiveData<Int>()

    init {
        // TODO
        MainApplication.getComponent().inject(this)

        serverStatus = appRepository.serverStatusData.liveDataActiveItem
        channelTags = appRepository.channelTagData.getLiveDataItems()
        allRecordings = appRepository.recordingData.getLiveDataItems()
        selectedChannelTagIds = appRepository.channelTagData.liveDataSelectedItemIds

        Timber.d("Loading time, sort order and channel tags ids from database")
        selectedTime.postValue(Date().time)

        val defaultChannelSortOrder = application.resources.getString(R.string.pref_default_channel_sort_order)
        channelSortOrder.postValue(Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder))
    }

    fun setSelectedTime(time: Long) {
        if (selectedTime.value != time) {
            Timber.d("Saving newly selected time")
            selectedTime.value = time
        }
    }

    fun setChannelSortOrder(order: Int) {
        if (channelSortOrder.value != order) {
            Timber.d("Saving newly selected channel sort order")
            channelSortOrder.value = order
        }
    }

    fun setSelectedChannelTagIds(ids: Set<Int>) {
        val tagValues = channelTags.value ?: HashSet<Int>()
        if (!Arrays.equals(tagValues.toTypedArray(), ids.toTypedArray())) {
            Timber.d("Updating database with newly selected channel tag ids")
            appRepository.channelTagData.updateSelectedChannelTags(ids)
        }
    }

    fun getSelectedChannelTagName(context: Context): String {
        if (selectedChannelTagIds.value == null || channelTags.value == null) {
            Timber.d("No channel tags or selected tag id values exist")
            return context.getString(R.string.unknown)
        }

        Timber.d("Returning name of the selected channel tag")
        val selectedTagIds = selectedChannelTagIds.value ?: ArrayList()
        if (selectedTagIds.size == 1) {
            channelTags.value?.forEach {
                if (selectedTagIds.contains(it.tagId)) {
                    return it.tagName ?: context.getString(R.string.unknown)
                }
            }
            return context.getString(R.string.unknown)
        } else return if (selectedTagIds.isEmpty()) {
            context.getString(R.string.all_channels)
        } else {
            context.getString(R.string.multiple_channel_tags)
        }
    }
}
