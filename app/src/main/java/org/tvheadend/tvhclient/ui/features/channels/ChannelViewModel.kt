package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import org.tvheadend.data.entity.Channel
import org.tvheadend.tvhclient.R
import timber.log.Timber

class ChannelViewModel(application: Application) : BaseChannelViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    var selectedListPosition = 0
    var selectedTimeOffset = 0
    val channels: LiveData<List<Channel>>
    var showGenreColor = MutableLiveData<Boolean>()
    var showNextProgramTitle = MutableLiveData<Boolean>()
    var showProgressBar = MutableLiveData<Boolean>()
    var showProgramSubtitle = MutableLiveData<Boolean>()
    val showChannelName = MutableLiveData<Boolean>()
    val showChannelNumber = MutableLiveData<Boolean>()
    private val channelSortOrder = MutableLiveData<Int>()

    private val defaultShowChannelName = application.applicationContext.resources.getBoolean(R.bool.pref_default_channel_name_enabled)
    private val defaultShowChannelNumber = application.applicationContext.resources.getBoolean(R.bool.pref_default_channel_number_enabled)
    private val defaultShowProgramSubtitle = application.applicationContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled)
    private val defaultShowProgressBar = application.applicationContext.resources.getBoolean(R.bool.pref_default_program_progressbar_enabled)
    private val defaultShowNextProgramTitle = application.applicationContext.resources.getBoolean(R.bool.pref_default_next_program_title_enabled)
    private val defaultShowGenreColor = application.applicationContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled)
    private val defaultShowAllChannelTags = application.applicationContext.resources.getBoolean(R.bool.pref_default_empty_channel_tags_enabled)

    init {
        val trigger = ChannelLiveData(selectedTime, channelSortOrder, selectedChannelTagIds)
        channels = trigger.switchMap { value ->
            val time = value.first
            val sortOrder = value.second
            val tagIds = value.third

            if (time == null) {
                Timber.d("Not loading channels because the selected time is not set")
                return@switchMap null
            }
            if (sortOrder == null) {
                Timber.d("Not loading channels because no channel sort order is set")
                return@switchMap null
            }
            if (tagIds == null) {
                Timber.d("Not loading channels because no selected channel tag id is set")
                return@switchMap null
            }
            Timber.d("Loading channels because either the selected time, channel sort order or channel tag ids have changed")
            return@switchMap appRepository.channelData.getAllChannelsByTime(time, sortOrder, tagIds)
        }

        onSharedPreferenceChanged(sharedPreferences, "channel_sort_order")
        onSharedPreferenceChanged(sharedPreferences, "channel_name_enabled")
        onSharedPreferenceChanged(sharedPreferences, "channel_number_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_progressbar_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_subtitle_enabled")
        onSharedPreferenceChanged(sharedPreferences, "next_program_title_enabled")
        onSharedPreferenceChanged(sharedPreferences, "genre_colors_for_channels_enabled")
        onSharedPreferenceChanged(sharedPreferences, "empty_channel_tags_enabled")

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "channel_sort_order" -> channelSortOrder.value = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
            "channel_name_enabled" -> showChannelName.value = sharedPreferences.getBoolean(key, defaultShowChannelName)
            "channel_number_enabled" -> showChannelNumber.value = sharedPreferences.getBoolean(key, defaultShowChannelNumber)
            "program_subtitle_enabled" -> showProgramSubtitle.value = sharedPreferences.getBoolean(key, defaultShowProgramSubtitle)
            "program_progressbar_enabled" -> showProgressBar.value = sharedPreferences.getBoolean(key, defaultShowProgressBar)
            "next_program_title_enabled" -> showNextProgramTitle.value = sharedPreferences.getBoolean(key, defaultShowNextProgramTitle)
            "genre_colors_for_channels_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key, defaultShowGenreColor)
            "empty_channel_tags_enabled" -> showAllChannelTags.value = sharedPreferences.getBoolean(key, defaultShowAllChannelTags)
        }
    }

    internal class ChannelLiveData(selectedTime: LiveData<Long>,
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
