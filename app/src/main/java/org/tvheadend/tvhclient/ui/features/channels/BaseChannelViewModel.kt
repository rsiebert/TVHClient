package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import org.tvheadend.data.entity.ChannelTag
import org.tvheadend.data.entity.Program
import org.tvheadend.data.entity.Recording
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber
import java.util.*

open class BaseChannelViewModel(application: Application) : BaseViewModel(application) {

    var showAllChannelTags = MutableLiveData<Boolean>()
    val channelTags = MediatorLiveData<List<ChannelTag>>()
    val recordings: LiveData<List<Recording>> = appRepository.recordingData.getLiveDataItems()
    val selectedChannelTagIds: LiveData<List<Int>?> = appRepository.channelTagData.liveDataSelectedItemIds
    val channelCount: LiveData<Int> = appRepository.channelData.getLiveDataItemCount()
    val selectedTime = MutableLiveData(Date().time)
    val defaultChannelSortOrder: String = appContext.resources.getString(R.string.pref_default_channel_sort_order)

    init {
        showAllChannelTags.value = sharedPreferences.getBoolean("empty_channel_tags_enabled", appContext.resources.getBoolean(R.bool.pref_default_empty_channel_tags_enabled))
        // Reload the channel tag list depending on the preference
        channelTags.addSource(showAllChannelTags) { allTags ->
            channelTags.value = appRepository.channelTagData.getNonEmptyItems(allTags)
        }
        // Reload the channel tag list when the tags have changed to
        // get the updated selection status which is saved for each tag
        channelTags.addSource(appRepository.channelTagData.getLiveDataItems()) {
            channelTags.value = appRepository.channelTagData.getNonEmptyItems(showAllChannelTags.value!!)
        }
    }

    fun setSelectedTime(time: Long) {
        if (selectedTime.value != time) {
            selectedTime.value = time
        }
    }

    fun setSelectedChannelTagIds(ids: Set<Int>) {
        val tagValues = channelTags.value ?: HashSet<Int>()
        if (!tagValues.toTypedArray().contentEquals(ids.toTypedArray())) {
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

    fun getRecordingById(id: Int): Recording? {
        return appRepository.recordingData.getItemByEventId(id)
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    fun getProgramById(id: Int): Program? {
        return appRepository.programData.getItemById(id)
    }
}
