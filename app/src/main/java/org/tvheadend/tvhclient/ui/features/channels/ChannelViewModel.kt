package org.tvheadend.tvhclient.ui.features.channels

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.domain.entity.Channel
import timber.log.Timber

class ChannelViewModel(application: Application) : BaseChannelViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    val channels: LiveData<List<Channel>>
    var showGenreColor = MutableLiveData<Boolean>()
    var showNextProgramTitle = MutableLiveData<Boolean>()
    var showProgressBar = MutableLiveData<Boolean>()
    var showProgramSubtitle = MutableLiveData<Boolean>()
    val showChannelName = MutableLiveData<Boolean>()
    val showChannelNumber = MutableLiveData<Boolean>()
    private val channelSortOrder = MutableLiveData<Int>()

    init {
        Timber.d("Initializing")
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

        onSharedPreferenceChanged(sharedPreferences, "channel_sort_order")
        onSharedPreferenceChanged(sharedPreferences, "channel_name_enabled")
        onSharedPreferenceChanged(sharedPreferences, "channel_number_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_progressbar_enabled")
        onSharedPreferenceChanged(sharedPreferences, "program_subtitle_enabled")
        onSharedPreferenceChanged(sharedPreferences, "next_program_title_enabled")
        onSharedPreferenceChanged(sharedPreferences, "genre_colors_for_channels_enabled")

        Timber.d("Registering shared preference change listener")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        Timber.d("Unregistering shared preference change listener")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "channel_sort_order" -> channelSortOrder.value = Integer.valueOf(sharedPreferences.getString("channel_sort_order", appContext.resources.getString(R.string.pref_default_channel_sort_order)) ?: appContext.resources.getString(R.string.pref_default_channel_sort_order))
            "channel_name_enabled" -> showChannelName.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_channel_name_enabled))
            "channel_number_enabled" -> showChannelNumber.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_channel_number_enabled))
            "program_subtitle_enabled" -> showProgramSubtitle.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_program_subtitle_enabled))
            "program_progressbar_enabled" -> showProgressBar.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_program_progressbar_enabled))
            "next_program_title_enabled" -> showNextProgramTitle.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_next_program_title_enabled))
            "genre_colors_for_channels_enabled" -> showGenreColor.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_genre_colors_for_channels_enabled))
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
